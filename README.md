# Hướng dẫn sử dụng API để chia sẻ file vào Nextcloud Talk

## Tổng quan quá trình
1. Kiểm tra sự tồn tại của file trên cloud. Điều này là cần thiết vì nếu trùng tên, file được upload sẽ ghi đè lên file cũ.
2. Thực hiện upload file lên cloud và đặt vào trong thư mực /Talk.
3. Chia sẻ file đã upload vào trong cuộc hội thoại.

Tài liệu này sẽ hướng dẫn sử dụng nhanh lệnh **curl**, và sử dụng **code Java**. Username, password và token trong tài liệu này chỉ là ví dụ, cần thay đúng khi sử dụng.

## 1. Sử dụng lệnh curl
### 1.1. Đặt biến môi trường dùng cho xác thực
```bash
export USER=bachns
export PASS=123456
```

### 1.2. Lấy danh sách các cuộc hội thoại
```bash
curl --request GET --header 'Content-Type: application/json' --header 'Accept: application/json' --header 'OCS-APIRequest: true' --user $USER:$PASS https://DOMAIN_NAME/ocs/v2.php/apps/spreed/api/v4/room
```
Từ kết quả trả về ta có thể xác định được **token** của các cuộc hội thoại.

### 1.3. Upload tệp tin lên cloud 
```bash
curl --request PUT --header 'Content-Type: application/octet-stream' --header 'Accept: application/json' --header 'OCS-APIRequest: true' --user $USER:$PASS --upload-file /Users/bachns/lab/file1.pdf https://DOMAIN_NAME/remote.php/dav/files/bachns/Talk/file1.pdf

```

Lưu ý:
* Cần upload file lên thư mục /Talk và cần kiểm tra tên để tránh sự trùng lặp. Bởi khi trùng lặp thì file upload sẽ ghi đè lên file đang tồn tại trên cloud.
* Mặc định Nextcloud sẽ cấp mỗi tài khoản dung lượng 2 Gb (hoặc 10 Gb). Do vậy, nếu tài khoản thường xuyên upload dữ liệu thì cần liên hệ với quản trị viên để chuyển tài khoản sang chế độ upload Unlimited (không giới hạn).

### 1.4. Chia sẻ tệp tin vào hội thoại
```bash
curl --request POST --header 'Accept: application/json' --header 'OCS-APIRequest: true' --user $USER:$PASS https://DOMAIN_NAME/ocs/v2.php/apps/files_sharing/api/v1/shares?shareType=10&shareWith=1234abcd&path=/Talk/file1.pdf
```
Với **shareType** mặc định là 10, **shareWith** là token (8 kí tự) của cuộc hội thoại , và **path** là đường dẫn trên cloud của tệp tin muốn chia sẻ (bắt đầu với /Talk/). 

Xem tệp tin đầy đủ *curl.sh*.

## 2. Sử dụng java code

Dưới đây là đoạn code ví dụ sử dụng java để thực hiện quá trình chia sẻ tệp tin vào trong một cuộc hội thoại.
Code có kiểm tra trùng lặp để tránh việc ghi đè, việc đổi tên file sẽ được thực hiện khi phát hiện trùng lặp.

### 2.1. Phương thức main
```java
private static String URI_SHARED = "https://DOMAIN_NAME/ocs/v2.php/apps/files_sharing/api/v1/shares";
private static String URI_UPLOAD = "https://DOMAIN_NAME/remote.php/dav/files";
private static String URI_FILE_SHARING = "https://DOMAIN_NAME/ocs/v2.php/apps/files_sharing/api/v1/shares";

public static void main(String[] args) {
    String username = "bachns";
    String password = "123456";
    String token = "1234abcd";
    // Tệp tin cần upload
    String uploadingFile = "/Users/bachns/Desktop/imyapi.jpeg";
    // Upload xong có được đường dẫn cloud của tệp tin
    String cloudFile = uploadFile(uploadingFile, username, password);
    if (!cloudFile.isEmpty()) {
        // Thực hiện chia sẻ tệp tin cloud này vào trong cuộc hội thoại
        shareFile(cloudFile, token, username, password);
    }
}
```

### 2.2. Phương thức kiểm tra tên file đã có hay chưa?
```java
/*
* Kiểm tra sự tồn tại của file trên cloud trong thư mục /Talk/
* Trả về:
* 		1: Đã tồn tại trên cloud
*		0: Chưa tồn tại
*		-1: Xảy ra lỗi
*/
private static int checkExists(String name, String username, String password) {
    try {
        RequestBuilder requestBuilder = RequestBuilder.get(URI_SHARED);
        requestBuilder.addParameter("path", "/Talk/" + name);
        requestBuilder.addHeader("Accept", "application/json");
        requestBuilder.addHeader("OCS-APIRequest", "true");
        requestBuilder.addHeader("Authorization", basicAuth64(username, password));
        HttpUriRequest request = requestBuilder.build();
        CloseableHttpClient httpClient = HttpClients.createDefault();
        
        HttpResponse response = httpClient.execute(request);
        if (response.getStatusLine().getStatusCode() == 200)
            return 1;
            
        return 0;
    }catch (Exception ex) {
        System.err.println(ex.toString());
    }

    return -1; // Error
}
```

### 2.3. Phương thức upload file lên cloud
```java
/*
* Thực hiện upload file lên cloud, đặt vào bên trong /Talk/
* Nếu tên file bị trùng sẽ thực hiện đổi tên file.
* Trả về đường dẫn của file đã upload
*/
private static String uploadFile(String filePath, String username, String password) {
    //Kiểm tra tên file đã có trên cloud chưa.
    File file = new File(filePath);
    String fileName = file.getName();
    while (checkExists(fileName, username, password) == 1) {
        fileName = correctName(fileName);
    }
    String cloudFile = "/Talk/" + fileName;
    try {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        FileEntity entity = new FileEntity(new File(filePath));
        String uri = URI_UPLOAD + "/" + username + cloudFile;
        RequestBuilder requestBuilder = RequestBuilder.put(uri);
        requestBuilder.setEntity(entity);
        requestBuilder.addHeader("Content-Type", "application/octet-stream");
        requestBuilder.addHeader("Accept", "application/json");
        requestBuilder.addHeader("OCS-APIRequest", "true");
        requestBuilder.addHeader("Authorization", basicAuth64(username, password));
        HttpUriRequest request = requestBuilder.build();
        HttpResponse response = httpClient.execute(request);
        if (response.getStatusLine().getStatusCode() == 201) {
            System.out.println("Upload successfully: " + cloudFile);
            return cloudFile;
        } else {
            System.err.println("Upload failed");
        }
    } catch (Exception e) {
        System.err.println(e.toString());
    }
    return "";
}
```

### 2.4. Phương thức chia sẻ file trên cloud vào trong cuộc hội thoại
```java
/*
* Thực hiện chia sẻ cloud file vào cuộc hội thoại
* Cloud file là những file đã được upload lên cloud
* và đặt trong thưc mục /Talk. 
* Hàm cũng cần thêm token của cuội hội thoại được nhận chia sẻ
* 200: Chia sẻ mới, 403 chia sẻ lại
*/
private static void shareFile(String cloudFile, String token, String username, String password) {
    try {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        RequestBuilder requestBuilder = RequestBuilder.post(URI_FILE_SHARING);
        requestBuilder.addParameter("path", cloudFile);
        requestBuilder.addParameter("shareWith", token);
        requestBuilder.addParameter("shareType", "10");
        requestBuilder.addHeader("Accept", "application/json");
        requestBuilder.addHeader("OCS-APIRequest", "true");
        requestBuilder.addHeader("Authorization", basicAuth64(username, password));
        HttpUriRequest request = requestBuilder.build();

        HttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        
        //200: Chia sẻ mới, 403 chia sẻ lại
        if(statusCode == 200 || statusCode == 403) {
            System.out.println("Share successfully: " + cloudFile);
        }
        else {
            System.err.println("Share failed: " + statusCode);
        }

    } catch (Exception e) {
        System.err.println(e.toString());
    }

}
```

### 2.5. Các phương thức hỗ trợ khác (biên mã và đổi tên)
```java
/*
* Thực hiện biên mã username:password thành chuỗi base64
*/
public static String basicAuth64(String username, String password) {
    String auth = username + ":" + password;
    return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
}


/*
* Hiệu chỉnh tên file khi bị trùng tên
* Thực hiện đặt tên mới cho file bằng cách gắn thêm hậu tố -1, -2, -3
*/
public static String correctName(String name) {
    // Xác định dấu chấm để tách lấy phần tên và định dạng
    int dotIndex = name.lastIndexOf(".");
    if (dotIndex == -1)
        return name;
    // Lấy tên file và định dạng
    String baseName = name.substring(0, dotIndex);
    String ext = name.substring(dotIndex+1);
    int dashIndex = baseName.lastIndexOf("-");
    if (dashIndex != -1) {
        //Trường hợp đã có hậu tố -1 -2 -3 thì tăng thêm
        String suf = baseName.substring(dashIndex + 1);
        String nextSuf = String.valueOf(Integer.parseInt(suf) + 1);
        String newBaseName = baseName.substring(0, dashIndex) + "-" + nextSuf;
        return newBaseName + "." + ext;
    }
    //Trường hợp chưa có hậu tố thì gắn -1
    return baseName + "-1." + ext;
}
```

Xem tệp tin đầy đủ *sample.java* và *pom.xml*.