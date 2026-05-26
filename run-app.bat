@echo off
chcp 65001 >nul
echo ============================================
echo   Построитель команды запуска 1С
echo ============================================
echo.

REM Проверяем, есть ли JAR файл
if not exist "app\build\libs\launcher1c-1.0.0.jar" (
    echo ОШИБКА: JAR файл не найден!
    echo Сначала выполните: ./gradlew.bat jar
    pause
    exit /b 1
)

REM Пытаемся найти JavaFX
set JAVAFX_FOUND=0

if defined JAVAFX_HOME (
    if exist "%JAVAFX_HOME%\lib\javafx.controls.jar" (
        set JAVAFX_PATH=%JAVAFX_HOME%\lib
        set JAVAFX_FOUND=1
    )
)

if %JAVAFX_FOUND%==0 (
    if exist "lib\javafx-sdk-21.0.2\lib" (
        set JAVAFX_PATH=lib\javafx-sdk-21.0.2\lib
        set JAVAFX_FOUND=1
    )
)

if %JAVAFX_FOUND%==0 (
    echo JavaFX не найден!
    echo.
    echo Варианты решения:
    echo 1. Установите JAVAFX_HOME в переменную среды
    echo 2. Скачайте JavaFX SDK и положите в папку lib\javafx-sdk-21.0.2
    echo    https://gluonhq.com/products/javafx/
    echo.
    echo Или запустите через Gradle:
    echo   ./gradlew.bat run --no-configuration-cache
    echo.
    pause
    exit /b 1
)

echo Запуск приложения...
echo.

java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls -jar "app\build\libs\launcher1c-1.0.0.jar"

if errorlevel 1 (
    echo.
    echo ОШИБКА при запуске!
    pause
)
