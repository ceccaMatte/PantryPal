@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM ============================================================
REM PantryPal - clean build + reset dati app + install + launch
REM ============================================================
REM Modifica PROJECT_DIR se sposti il progetto.
set "PROJECT_DIR=D:\Universita\Terzo_Anno\mobile"
set "APP_ID=com.example.pantrypal"
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
set "APK=%PROJECT_DIR%\app\build\outputs\apk\debug\app-debug.apk"

echo.
echo ============================================
echo PantryPal - Build, reset, install, launch
echo ============================================
echo Project: %PROJECT_DIR%
echo App ID : %APP_ID%
echo.

if not exist "%PROJECT_DIR%\gradlew.bat" (
    echo ERRORE: gradlew.bat non trovato in "%PROJECT_DIR%".
    echo Controlla PROJECT_DIR dentro questo file .bat.
    pause
    exit /b 1
)

if not exist "%ADB%" (
    echo ERRORE: adb.exe non trovato in "%ADB%".
    echo Controlla che Android SDK platform-tools sia installato.
    pause
    exit /b 1
)

cd /d "%PROJECT_DIR%" || (
    echo ERRORE: impossibile entrare nella cartella progetto.
    pause
    exit /b 1
)

echo.
echo [1/6] Dispositivi collegati:
"%ADB%" devices -l
echo.
echo Se vedi "unauthorized", guarda il telefono e accetta il popup Debug USB.
echo Se non vedi dispositivi, avvia emulatore o collega telefono via USB.
echo.
pause

echo.
echo [2/6] Pulizia build Gradle...
call .\gradlew.bat clean
if errorlevel 1 (
    echo ERRORE durante gradle clean.
    pause
    exit /b 1
)

echo.
echo [3/6] Build debug APK...
call .\gradlew.bat :app:assembleDebug
if errorlevel 1 (
    echo ERRORE durante assembleDebug.
    pause
    exit /b 1
)

if not exist "%APK%" (
    echo ERRORE: APK non trovato in "%APK%".
    pause
    exit /b 1
)

echo.
echo [4/6] Reset dati app sul dispositivo...
echo Provo a cancellare dati/disinstallare l'app. Se non era installata, puoi ignorare eventuali messaggi.
"%ADB%" shell pm clear %APP_ID% >nul 2>nul
"%ADB%" uninstall %APP_ID% >nul 2>nul

echo.
echo [5/6] Installazione APK...
"%ADB%" install --no-streaming -r "%APK%"
if errorlevel 1 (
    echo ERRORE durante installazione APK.
    echo Se hai piu' dispositivi collegati, scollegane uno oppure usa Android Studio.
    pause
    exit /b 1
)

echo.
echo [6/6] Lancio app...
"%ADB%" shell monkey -p %APP_ID% -c android.intent.category.LAUNCHER 1
if errorlevel 1 (
    echo ERRORE durante lancio app.
    pause
    exit /b 1
)

echo.
echo FATTO: PantryPal compilata, dati puliti, installata e lanciata.
echo.
pause
endlocal
