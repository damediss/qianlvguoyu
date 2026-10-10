@echo off
cd /d D:\workspace\qianlvguoyu_saas\apps\admin-web
set PATH=D:\Program Files\nodejs;%PATH%
call npm.cmd run dev > D:\workspace\qianlvguoyu_saas\outputs\logs\admin-web.log 2>&1
echo Frontend started
