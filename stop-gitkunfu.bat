@echo off
setlocal
title gitKunfu

powershell -NoProfile -Command "$p = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue; if (-not $p) { exit 0 }; $p | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }"
if errorlevel 1 (
  echo [gitKunfu] Could not stop the server. Try closing the "gitKunfu Server" window manually.
  pause
  exit /b 1
)

echo [gitKunfu] Server stopped.
exit /b 0
