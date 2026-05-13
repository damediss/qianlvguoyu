Set WshShell = CreateObject("WScript.Shell")
WshShell.CurrentDirectory = "D:\workspace\qianlvguoyu_saas"
Dim env
Set env = WshShell.Environment("PROCESS")
env("SERVER_PORT") = "8080"
env("MYSQL_URL") = "jdbc:mysql://localhost:3308/gjjd_test?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
env("MYSQL_USERNAME") = "root"
env("MYSQL_PASSWORD") = "guoyu_dev!513"
env("JWT_SECRET") = "dev-only-change-me-dev-only-change-me"
WshShell.Run """D:\Program Files\Java\oracleJdk-21\bin\java.exe"" -jar ""D:\workspace\qianlvguoyu_saas\services\platform-api\target\platform-api-0.1.0-SNAPSHOT.jar""", 0, False
