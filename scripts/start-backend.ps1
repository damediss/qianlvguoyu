$env:JAVA_HOME='D:\Program Files\Java\oracleJdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:SERVER_PORT='8080'
$env:MYSQL_URL='jdbc:mysql://192.168.1.200:3308/gjjd_test?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
$env:MYSQL_USERNAME='root'
$env:MYSQL_PASSWORD='guoyu_dev!513'
$env:JWT_SECRET='dev-only-change-me-dev-only-change-me'
Write-Output 'Starting platform-api on port 8080...'
java -jar D:\qianlvguoyu\qianlvguoyu_saas-git\services\platform-api\target\platform-api-0.1.0-SNAPSHOT.jar
