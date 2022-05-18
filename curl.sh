
export USER=bachns
export PASS=123456



curl --request GET --header 'Content-Type: application/json' --header 'Accept: application/json' --header 'OCS-APIRequest: true' --user $USER:$PASS https://DOMAIN_NAME/ocs/v2.php/apps/spreed/api/v4/room



curl --request PUT --header 'Content-Type: application/octet-stream' --header 'Accept: application/json' --header 'OCS-APIRequest: true' --user $USER:$PASS --upload-file /Users/bachns/lab/file1.pdf https://DOMAIN_NAME/remote.php/dav/files/bachns/Talk/file1.pdf



curl --request POST --header 'Accept: application/json' --header 'OCS-APIRequest: true' --user $USER:$PASS https://DOMAIN_NAME/ocs/v2.php/apps/files_sharing/api/v1/shares?shareType=10&shareWith=1234abcd&path=/Talk/file1.pdf
