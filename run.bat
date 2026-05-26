@echo off
REM Запуск приложения launcher1c
REM Требуется Java 21+ с модулями JavaFX
REM Скачайте JavaFX SDK с: https://gluonhq.com/products/javafx/

set JAVAFX_PATH=%JAVAFX_HOME%
if "%JAVAFX_PATH%"=="" set JAVAFX_PATH=lib\javafx

java --module-path "%JAVAFX_PATH%\lib" --add-modules javafx.controls -jar "app\build\libs\launcher1c-1.0.0.jar"
pause
