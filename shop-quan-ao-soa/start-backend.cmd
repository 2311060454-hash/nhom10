@echo off
title Shop Quan Ao SOA - Backend Services
echo Dang khoi dong 8 microservices backend...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\Start-Local.ps1" %*
