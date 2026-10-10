@echo off
cd /d D:\workspace\qianlvguoyu_saas\scripts
start /B "" "D:\Program Files\nodejs\node.exe" dev-server.mjs > D:\workspace\qianlvguoyu_saas\outputs\logs\dev-server.log 2>&1
echo Dev server started
