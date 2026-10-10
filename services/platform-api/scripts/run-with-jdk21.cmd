@echo off
setlocal
set "JAVA_HOME=C:\Users\admin\.jdks\ms-21.0.11"
set "NEWPATH=%JAVA_HOME%\bin;C:\Windows\system32;C:\Windows;C:\Windows\System32\Wbem;C:\Windows\System32\WindowsPowerShell\v1.0"
for %%P in ("%PATH:;=" "%") do (
    if /I not "%%~P"=="D:\Dev\Java\jdk-17\bin" if /I not "%%~P"=="D:\Dev\Java\jdk-17\jre\bin" if /I not "%%~P"=="C:\Program Files\Common Files\Oracle\Java\javapath" if not "%%~P"=="" set "NEWPATH=%NEWPATH%;%%~P"
)
set "PATH=%NEWPATH%"
cd /d D:\phpApp\qianlvguoyu_saas\services\platform-api
.\mvnw.cmd spring-boot:run
endlocal
