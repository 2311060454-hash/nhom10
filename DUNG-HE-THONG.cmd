@echo off
chcp 65001 >nul
title DUNG TOAN BO HE THONG SHOP QUAN AO (SOA)

echo ========================================================
echo   DANG DUNG TOAN BO HE THONG SHOP QUAN AO...
echo ========================================================
echo.
echo [1/2] Dang tat cac Backend Microservices...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0shop-quan-ao-soa\scripts\Stop-Local.ps1"

echo.
echo [2/2] Dang tat Giao dien Frontend...
taskkill /fi "WINDOWTITLE eq Shop Quan Ao - Frontend*" /f /t >nul 2>&1
for /f "tokens=5" %%a in ('netstat -aon 2^>nul ^| findstr :5173 ^| findstr LISTENING') do taskkill /PID %%a /F >nul 2>&1

echo.
echo ========================================================
echo   [HOAN TAT] DA DUNG TOAN BO HE THONG AN TOAN!
echo ========================================================
timeout /t 3 /nobreak >nul
