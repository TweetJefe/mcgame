@echo off
setlocal
cd /d "%~dp0"
if exist "%USERPROFILE%\.jdks\ms-21.0.11\bin\java.exe" set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.11"
call gradlew.bat --console=plain :client-libgdx:run
if errorlevel 1 pause
