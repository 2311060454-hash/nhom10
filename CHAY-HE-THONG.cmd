@echo off
chcp 65001 >nul
title KHOI DONG TOAN BO HE THONG SHOP QUAN AO (SOA)

echo ========================================================
echo   HE THONG QUAN LY SHOP QUAN AO - KIEN TRUC SOA
echo ========================================================
echo.
echo [1/3] Kiem tra va don dep cac phien chay cu...
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Test-Path '%~dp0shop-quan-ao-soa\.runtime\processes.json') { & '%~dp0shop-quan-ao-soa\scripts\Stop-Local.ps1' }"

echo.
echo [2/3] Dang khoi dong 8 Microservices Backend...
echo (Vui long cho giay lat de ca 8 service lan luot san sang...)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0shop-quan-ao-soa\scripts\Start-Local.ps1"
if %ERRORLEVEL% neq 0 (
    echo.
    echo [LOI] Khong the khoi dong Backend!
    echo Vui long kiem tra MySQL port 3310 da bat chua, hoac xem file log tai shop-quan-ao-soa\.runtime\
    pause
    exit /b 1
)

echo.
echo [3/3] Dang khoi dong Giao dien Frontend (React Vite)...
start "Shop Quan Ao - Frontend (Port 5173)" cmd /k "cd /d ""%~dp0shop-quan-ao-soa\frontend"" && npm run dev"

echo.
echo ========================================================
echo   TAT CA DICH VU DA DUOC KHOI DONG THANH CONG!
echo ========================================================
echo.
echo - Frontend:       http://localhost:5173
echo - API Gateway:    http://localhost:8080
echo - Mat khau chung: Password@123
echo - Tai khoan:
echo     + Admin (Quan tri):     admin@shop.local
echo     + Nhan vien (Kho/Ban):  staff@shop.local
echo     + Khach hang:           customer@shop.local
echo.
echo Dang tu dong mo trinh duyet...
timeout /t 3 /nobreak >nul
start http://localhost:5173

echo.
echo Nhan phim bat ky de dong cua so nay (Backend va Frontend van tiep tuc hoat dong).
echo De DUNG he thong, hay nhap dup chuot vao file: DUNG-HE-THONG.cmd
pause
