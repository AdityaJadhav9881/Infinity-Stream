@echo off
echo ============================================
echo  MusicFlow - Resume Session Script
echo ============================================
echo.

REM Check for device
echo Checking for connected devices...
"C:\Users\jadha\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices
echo.

REM Build
echo Building debug APK...
call gradlew.bat assembleDebug
echo.

REM Install
echo Installing APK...
"C:\Users\jadha\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
echo.

echo ============================================
echo  APK installed! Open MusicFlow on your phone.
echo ============================================
pause
