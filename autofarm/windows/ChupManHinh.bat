@echo off
chcp 65001 >nul
cd /d "%~dp0"
set ADB="%~dp0platform-tools\adb.exe"
if not exist "platform-tools\adb.exe" (
  echo Hay chay CaiDat-AutoFarm.bat truoc.
  pause
  exit /b 1
)
for /f "tokens=1-3 delims=/:. " %%a in ("%time%") do set T=%%a%%b%%c
set F=ManHinh-%date:~-4%%date:~3,2%%date:~0,2%-%T%.png
set F=%F: =0%
%ADB% exec-out screencap -p > "%F%"
echo Da luu: %CD%\%F%
echo Gui file anh nay cho Claude.
explorer /select,"%CD%\%F%"
pause
