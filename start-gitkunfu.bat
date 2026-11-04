@echo off
setlocal
cd /d "%~dp0"
title gitKunfu

if not exist "backend\target\gitkunfu-server.jar" (
  echo [gitKunfu] Package not found: backend\target\gitkunfu-server.jar
  echo [gitKunfu] Build it first with: mvn -f backend\pom.xml package
  pause
  exit /b 1
)

powershell -NoProfile -Command "$c=New-Object Net.Sockets.TcpClient; try{$c.Connect('127.0.0.1',8080);$c.Close();exit 0}catch{exit 1}"
if not errorlevel 1 (
  echo [gitKunfu] Server is already running. Opening the browser...
  start "" "http://localhost:8080"
  exit /b 0
)

echo [gitKunfu] Starting server, please wait...
start "gitKunfu Server" /min java -jar "backend\target\gitkunfu-server.jar"

powershell -NoProfile -Command "$deadline=(Get-Date).AddSeconds(90); while((Get-Date) -lt $deadline){ $c=New-Object Net.Sockets.TcpClient; try{$c.Connect('127.0.0.1',8080);$c.Close();exit 0}catch{Start-Sleep -Milliseconds 800} }; exit 1"
if errorlevel 1 (
  echo [gitKunfu] Server did not start in time.
  echo [gitKunfu] Please check that MySQL is running and the database was initialized.
  echo [gitKunfu] See README.md for setup steps.
  pause
  exit /b 1
)

start "" "http://localhost:8080"
echo [gitKunfu] Ready: http://localhost:8080
exit /b 0
