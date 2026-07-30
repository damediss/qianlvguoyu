$env:JAVA_HOME='D:\Program Files\Java\oracleJdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:SERVER_PORT='8080'
$env:MYSQL_URL='jdbc:mysql://localhost:3308/gjjd_test?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
$env:MYSQL_USERNAME='root'
$env:MYSQL_PASSWORD='guoyu_dev!513'
$env:JWT_SECRET='dev-only-change-me-dev-only-change-me'
$outFile='D:\workspace\qianlvguoyu_saas\outputs\logs\platform-api-console.log'
Write-Host 'Starting platform-api, logging to ' $outFile
& "$env:JAVA_HOME\bin\java.exe" -jar D:\workspace\qianlvguoyu_saas\services\platform-api\target\platform-api-0.1.0-SNAPSHOT.jar *> $outFile
