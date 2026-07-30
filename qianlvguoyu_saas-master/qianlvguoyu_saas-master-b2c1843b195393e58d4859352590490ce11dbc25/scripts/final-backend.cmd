@echo off
set SERVER_PORT=8080
set MYSQL_URL=jdbc:mysql://localhost:3308/gjjd_test?useUnicode=true&characterEncoding=utf8^&useSSL=false^&allowPublicKeyRetrieval=true^&serverTimezone=Asia/Shanghai
set MYSQL_USERNAME=root
set MYSQL_PASSWORD=guoyu_dev!513
set JWT_SECRET=dev-only-change-me-dev-only-change-me

start /B "" "D:\Program Files\Java\oracleJdk-21\bin\java.exe" -jar D:\workspace\qianlvguoyu_saas\services\platform-api\target\platform-api-0.1.0-SNAPSHOT.jar > D:\workspace\qianlvguoyu_saas\outputs\logs\platform-api-final.log 2>&1
ping -n 86400 127.0.0.1 > nul
