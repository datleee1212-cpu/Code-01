@echo off
chcp 65001 >nul
title Kiem tra may cho gia lap
cd /d "%~dp0"
set OUT=%~dp0KetQua-KiemTra.txt
echo Dang kiem tra, cho khoang 20 giay...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
 "$o=@();" ^
 "$cpu=Get-CimInstance Win32_Processor | Select-Object -First 1;" ^
 "$o+='CPU: '+$cpu.Name+' | Nhan: '+$cpu.NumberOfCores+' | Luong: '+$cpu.NumberOfLogicalProcessors;" ^
 "$o+='Ao hoa (VT) bat trong BIOS: '+$cpu.VirtualizationFirmwareEnabled;" ^
 "$ram=[math]::Round((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory/1GB,1);" ^
 "$o+='RAM: '+$ram+' GB';" ^
 "Get-CimInstance Win32_VideoController | ForEach-Object { $o+='Card do hoa: '+$_.Name };" ^
 "Get-PhysicalDisk | ForEach-Object { $o+='O dia: '+$_.FriendlyName+' ('+$_.MediaType+')' };" ^
 "$os=Get-CimInstance Win32_OperatingSystem; $o+='Windows: '+$os.Caption+' '+$os.Version;" ^
 "$hv=(Get-CimInstance Win32_ComputerSystem).HypervisorPresent; $o+='Hyper-V dang chay: '+$hv;" ^
 "try{$mi=(Get-ItemProperty 'HKLM:\SYSTEM\CurrentControlSet\Control\DeviceGuard\Scenarios\HypervisorEnforcedCodeIntegrity' -ErrorAction Stop).Enabled; $o+='Memory Integrity (Core isolation): '+$mi}catch{$o+='Memory Integrity: khong ro'};" ^
 "$free=[math]::Round((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1MB,1); $o+='RAM con trong luc nay: '+$free+' GB';" ^
 "$o | Out-File -Encoding UTF8 '%OUT%'; $o"
echo.
echo Da luu ket qua vao: %OUT%
echo Hay gui file KetQua-KiemTra.txt (hoac chup man hinh cua so nay) cho Claude.
notepad "%OUT%"
pause
