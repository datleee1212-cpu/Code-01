@echo off
chcp 65001 >nul
title AutoFarm - Cai dat qua day cap
cd /d "%~dp0"
echo ============================================================
echo   AUTOFARM - CAI DAT TU DONG QUA DAY CAP (Galaxy Tab S10+)
echo ============================================================
echo.

rem --- 1. Tai cong cu ADB cua Google (chi lan dau) ---
if not exist "platform-tools\adb.exe" (
  echo [1/6] Dang tai cong cu ADB cua Google...
  powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing https://dl.google.com/android/repository/platform-tools-latest-windows.zip -OutFile pt.zip; Expand-Archive pt.zip -DestinationPath . -Force; Remove-Item pt.zip"
) else (
  echo [1/6] Da co cong cu ADB.
)
set ADB="%~dp0platform-tools\adb.exe"
set PKG=com.code02.autofarm
set SVC=com.code02.autofarm/com.code02.autofarm.FarmService

rem --- 2. Cho may tinh bang ---
echo.
echo [2/6] Dang tim may tinh bang...
echo       Neu may tinh bang hien hop "Cho phep go loi USB?" hay bam CHO PHEP.
%ADB% start-server >nul 2>&1
%ADB% wait-for-device
for /f "tokens=*" %%m in ('%ADB% shell getprop ro.product.model') do set MODEL=%%m
echo       Da ket noi: %MODEL%

rem --- 3. Tai ban AutoFarm moi nhat ---
echo.
echo [3/6] Dang tai AutoFarm moi nhat...
powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing https://github.com/datleee1212-cpu/Code-01/releases/download/autofarm-latest/AutoFarm.apk -OutFile AutoFarm.apk"

rem --- 4. Cai dat (qua day cap thi Play Protect khong chan) ---
echo.
echo [4/6] Dang cai AutoFarm...
%ADB% install -r AutoFarm.apk > install.log 2>&1
findstr /c:"Success" install.log >nul
if errorlevel 1 (
  findstr /c:"INSTALL_FAILED_UPDATE_INCOMPATIBLE" install.log >nul
  if not errorlevel 1 (
    echo       Ban cu ky khoa khac - go ban cu roi cai lai (lan nay phai day lai bot^).
    %ADB% uninstall %PKG% >nul
    %ADB% install AutoFarm.apk
  ) else (
    type install.log
    echo       CAI DAT LOI. Chup man hinh cua so nay gui Claude.
    pause
    exit /b 1
  )
)
echo       Da cai xong.

rem --- 5. Bat Tro nang + tat toi uu pin ---
echo.
echo [5/6] Dang bat Tro nang va tat toi uu pin cho AutoFarm...
set CUR=
for /f "tokens=*" %%a in ('%ADB% shell settings get secure enabled_accessibility_services') do set CUR=%%a
set NEW=%SVC%
if not "%CUR%"=="" if not "%CUR%"=="null" (
  echo %CUR% | findstr /c:"%SVC%" >nul
  if errorlevel 1 (set NEW=%CUR%:%SVC%) else (set NEW=%CUR%)
)
%ADB% shell settings put secure enabled_accessibility_services "%NEW%"
%ADB% shell settings put secure accessibility_enabled 1
%ADB% shell dumpsys deviceidle whitelist +%PKG% >nul
%ADB% shell cmd appops set %PKG% RUN_ANY_IN_BACKGROUND allow >nul 2>&1
echo       Xong. Thanh noi AutoFarm se hien tren may tinh bang.

rem --- 6. Mo game ---
echo.
echo [6/6] Dang mo Clash of Clans...
%ADB% shell monkey -p com.supercell.clashofclans -c android.intent.category.LAUNCHER 1 >nul 2>&1

echo.
echo ============================================================
echo   XONG! Tren may tinh bang:
echo   1. Bam nut 📍 tren thanh noi de day bot (chi lam 1 lan).
echo   2. Bam 🔍 de kiem tra, roi bam ▶ de chay.
echo.
echo   Muon gui man hinh cho Claude: chay file ChupManHinh.bat
echo ============================================================
pause
