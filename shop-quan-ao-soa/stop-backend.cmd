@echo off
title Shop Quan Ao SOA - Stop Backend Services
echo Dang dung toan bo cac dich vu backend...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\Stop-Local.ps1" %*
