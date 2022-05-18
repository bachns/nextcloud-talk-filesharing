
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.FileEntity;
import org.apache.http.HttpResponse;
import java.util.Base64;
import java.io.File;


public class Main {
	
	private static String URI_SHARED = "https://DOMAIN_NAME/ocs/v2.php/apps/files_sharing/api/v1/shares";
	private static String URI_UPLOAD = "https://DOMAIN_NAME/remote.php/dav/files";
	private static String URI_FILE_SHARING = "https://DOMAIN_NAME/ocs/v2.php/apps/files_sharing/api/v1/shares";
	
	public static void main(String[] args) {
		String username = "bachns";
		String password = "123456";
		String groupIdToken = "1234abcd";
		String uploadingFile = "/Users/bachns/lab/file1.pdf";
		String cloudFile = uploadFile(uploadingFile, username, password);
		if (!cloudFile.isEmpty()) {
			shareFile(cloudFile, groupIdToken, username, password);
		}
	}
	
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
			if (response.getStatusLine().getStatusCode() == 200) {
				//Da ton tai
				return 1;
			}
			return 0;
		}catch (Exception ex) {
			System.err.println(ex.toString());
		}
		return -1; // Error
	}
	
	
	/*
	 * Thực hiện upload file lên cloud, đặt vào bên trong /Talk/
	 * Nếu tên file bị trùng sẽ thực hiện đổi tên file
	 * Trả về đường dẫn của file đã upload vào /Talk/
	 * 	ví dụ: /Talk/uploaded1.pdf, /Talk/uploaded2.jpg
	 */
	private static String uploadFile(String filePath, String username, String password) {
		//Kiểm tra tên file đã có trên cloud chưa.
		File file = new File(filePath);
		String fileName = file.getName();
		while (checkExists(fileName, username, password) == 1) {
			fileName = correctName(fileName);
		}
		
		String cloudFile = "/Talk/" + fileName;
		//fileName dung cho upload da OK!
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
	
	/*
	 * Thực hiện chia sẻ file vào cuộc hội thoại
	 * Thực hiện upload file lên cloud trong /Talk/
	 * Sau đó chia sẻ vào hội thoại
	 * 200: Chia sẻ mới, 403 chia sẻ lại
	 */
	private static void shareFile(String cloudFile, String groupIdToken, String username, String password) {
		try {
			CloseableHttpClient httpClient = HttpClients.createDefault();
			RequestBuilder requestBuilder = RequestBuilder.post(URI_FILE_SHARING);
			requestBuilder.addParameter("path", cloudFile);
			requestBuilder.addParameter("shareWith", groupIdToken);
			requestBuilder.addParameter("shareType", "10");
			requestBuilder.addHeader("Accept", "application/json");
			requestBuilder.addHeader("OCS-APIRequest", "true");
			requestBuilder.addHeader("Authorization", basicAuth64(username, password));
			HttpUriRequest request = requestBuilder.build();
			HttpResponse response = httpClient.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
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
}
