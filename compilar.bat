@echo off
cd /d "%~dp0"
set "JAVAC=javac"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC=%JAVA_HOME%\bin\javac.exe"

if exist out rmdir /s /q out
rem Bytecode de Java 8: asi corre con cualquier "java" 8 o superior que haya en el PATH.
"%JAVAC%" -source 8 -target 8 -Xlint:-options -d out -encoding UTF-8 -sourcepath src src\com\dressapp\app\Main.java
if errorlevel 1 (
  echo.
  echo La compilacion fallo. Necesitas un JDK 8 o superior: revisa que "javac -version" funcione
  echo o que la variable JAVA_HOME apunte a la carpeta del JDK.
  pause
  exit /b 1
)
echo Compilacion exitosa. Ahora ejecuta ejecutar.bat
pause
