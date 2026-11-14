@echo off
cd /d "%~dp0"
if not exist out\com\dressapp\app\Main.class (
  echo Primero ejecuta compilar.bat
  pause
  exit /b 1
)
set "JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"

rem UTF-8 para que las tildes se vean bien. El <nul evita que chcp consuma la entrada.
chcp 65001 >nul <nul
"%JAVA%" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstdin.encoding=UTF-8 -cp out com.dressapp.app.Main
pause
