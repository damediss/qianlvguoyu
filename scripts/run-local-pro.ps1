# 本机联调 App（saasMode=dev）时启动 platform-api，默认连生产同库 gjjddbpro
# 否则 PHP itoken 验不过，且跟班班级花名册不在 gjjddb2026。
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = if (Test-Path 'D:\Dev\Java\jdk-21-x64') { 'D:\Dev\Java\jdk-21-x64' } else { $env:JAVA_HOME }
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:MYSQL_URL = 'jdbc:mysql://43.138.163.7:3308/gjjddbpro?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&connectTimeout=5000&socketTimeout=60000&tcpKeepAlive=true'
$env:MYSQL_USERNAME = 'gjjd'
if (-not $env:MYSQL_PASSWORD) {
  Write-Error '请先设置环境变量 MYSQL_PASSWORD'
}
$env:SERVER_PORT = '8080'
$env:FILE_OBJECT_KEY_PREFIX = 'dev'
Set-Location (Join-Path $PSScriptRoot '..\services\platform-api')
Write-Output "Starting platform-api -> gjjddbpro on :$($env:SERVER_PORT)"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Dserver.port=$($env:SERVER_PORT)"
