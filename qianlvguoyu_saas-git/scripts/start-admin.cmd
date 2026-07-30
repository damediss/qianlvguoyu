@echo off
cd /d D:\workspace\qianlvguoyu_saas\apps\admin-web
set PATH=D:\Program Files\nodejs;%PATH%
start /B "" npm.cmd run dev > D:\workspace\qianlvguoyu_saas\outputs\logs\admin-web.log 2>&1
ping -n 999999 127.0.0.1 > nul
