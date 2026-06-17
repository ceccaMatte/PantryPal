# PantryPal — Comandi Windows per compilare, installare e lanciare l’app

## 0. Percorsi usati

Progetto:

```powershell
D:\Universita\Terzo_Anno\mobile
```

Package app:

```powershell
com.example.pantrypal
```

APK debug:

```powershell
D:\Universita\Terzo_Anno\mobile\app\build\outputs\apk\debug\app-debug.apk
```

---

# 1. Configurare variabili d’ambiente Android SDK

## 1.1 Configurazione temporanea, valida solo nella PowerShell corrente

Usa questa se vuoi sistemare solo la sessione aperta:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\emulator;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:Path"
```

Dopo questo puoi usare direttamente:

```powershell
adb devices -l
```

invece del percorso completo:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices -l
```

---

## 1.2 Configurazione permanente per il tuo utente Windows

Usa questa se vuoi non dover riscrivere i percorsi ogni volta.

```powershell
[Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:LOCALAPPDATA\Android\Sdk", "User")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", "$env:LOCALAPPDATA\Android\Sdk", "User")
```

Poi aggiungi `platform-tools`, `emulator` e `cmdline-tools` al `PATH` utente:

```powershell
$androidSdk = "$env:LOCALAPPDATA\Android\Sdk"

$userPath = [Environment]::GetEnvironmentVariable("Path", "User")

$pathsToAdd = @(
    "$androidSdk\platform-tools",
    "$androidSdk\emulator",
    "$androidSdk\cmdline-tools\latest\bin"
)

foreach ($p in $pathsToAdd) {
    if ($userPath -notlike "*$p*") {
        $userPath = "$userPath;$p"
    }
}

[Environment]::SetEnvironmentVariable("Path", $userPath, "User")
```

Dopo averlo fatto:

1. chiudi PowerShell;
2. riapri PowerShell;
3. verifica:

```powershell
adb version
```

e poi:

```powershell
adb devices -l
```

---

# 2. Entrare nella cartella del progetto

```powershell
Set-Location "D:\Universita\Terzo_Anno\mobile"
```

Verifica di essere nella cartella corretta:

```powershell
Get-ChildItem
```

Dovresti vedere file/cartelle tipo:

```text
gradlew.bat
app
build.gradle.kts
settings.gradle.kts
```

---

# 3. Controllare dispositivi collegati

```powershell
adb devices -l
```

Esempio output:

```text
List of devices attached
emulator-5554        device product:sdk_gphone64_x86_64
R5CT123ABCX          device product:...
```

Il primo valore è l’ID dispositivo:

```text
emulator-5554
R5CT123ABCX
```

---

# 4. Gestire errore “more than one device/emulator”

Se ADB mostra:

```text
adb.exe: more than one device/emulator
```

vuol dire che hai più dispositivi collegati. Devi specificare quale usare con `-s`.

## 4.1 Scegliere emulatore

Esempio:

```powershell
$env:ADB_DEVICE_ID = "emulator-5554"
```

## 4.2 Scegliere telefono fisico

Esempio:

```powershell
$env:ADB_DEVICE_ID = "R5CT123ABCX"
```

Sostituisci `R5CT123ABCX` con l’ID reale mostrato da:

```powershell
adb devices -l
```

Da questo momento puoi usare:

```powershell
adb -s $env:ADB_DEVICE_ID devices -l
```

---

# 5. Compilare l’app

## 5.1 Build normale

```powershell
.\gradlew.bat :app:assembleDebug
```

Risultato atteso:

```text
BUILD SUCCESSFUL
```

---

## 5.2 Build pulita

Usa questa se vuoi cancellare cache/build precedenti:

```powershell
.\gradlew.bat clean
.\gradlew.bat :app:assembleDebug
```

---

# 6. Eseguire test unitari

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Risultato atteso:

```text
BUILD SUCCESSFUL
```

---

# 7. Installare e lanciare l’app

## Opzione A — Un solo dispositivo collegato

### 7.1 Installazione normale, mantenendo dati app

```powershell
adb install --no-streaming -r "D:\Universita\Terzo_Anno\mobile\app\build\outputs\apk\debug\app-debug.apk"
```

### 7.2 Lancio app

```powershell
adb shell monkey -p com.example.pantrypal -c android.intent.category.LAUNCHER 1
```

---

## Opzione B — Più dispositivi collegati

Prima scegli il dispositivo:

```powershell
adb devices -l
```

Poi imposta l’ID:

```powershell
$env:ADB_DEVICE_ID = "emulator-5554"
```

oppure:

```powershell
$env:ADB_DEVICE_ID = "ID_DEL_TUO_TELEFONO"
```

### 7.3 Installazione normale, mantenendo dati app

```powershell
adb -s $env:ADB_DEVICE_ID install --no-streaming -r "D:\Universita\Terzo_Anno\mobile\app\build\outputs\apk\debug\app-debug.apk"
```

### 7.4 Lancio app

```powershell
adb -s $env:ADB_DEVICE_ID shell monkey -p com.example.pantrypal -c android.intent.category.LAUNCHER 1
```

---

# 8. Installazione pulita, cancellando tutti i dati

Questa opzione elimina dispensa, preferiti e impostazioni salvate.

## 8.1 Con un solo dispositivo collegato

```powershell
adb uninstall com.example.pantrypal
```

Poi installa:

```powershell
adb install --no-streaming -r "D:\Universita\Terzo_Anno\mobile\app\build\outputs\apk\debug\app-debug.apk"
```

Poi lancia:

```powershell
adb shell monkey -p com.example.pantrypal -c android.intent.category.LAUNCHER 1
```

---

## 8.2 Con più dispositivi collegati

Imposta prima il dispositivo:

```powershell
adb devices -l
$env:ADB_DEVICE_ID = "emulator-5554"
```

Poi:

```powershell
adb -s $env:ADB_DEVICE_ID uninstall com.example.pantrypal
```

Installa:

```powershell
adb -s $env:ADB_DEVICE_ID install --no-streaming -r "D:\Universita\Terzo_Anno\mobile\app\build\outputs\apk\debug\app-debug.apk"
```

Lancia:

```powershell
adb -s $env:ADB_DEVICE_ID shell monkey -p com.example.pantrypal -c android.intent.category.LAUNCHER 1
```

---

# 9. Pulire solo i dati dell’app senza disinstallare

## Un solo dispositivo

```powershell
adb shell pm clear com.example.pantrypal
```

Poi rilancia:

```powershell
adb shell monkey -p com.example.pantrypal -c android.intent.category.LAUNCHER 1
```

## Più dispositivi

```powershell
adb -s $env:ADB_DEVICE_ID shell pm clear com.example.pantrypal
```

Poi rilancia:

```powershell
adb -s $env:ADB_DEVICE_ID shell monkey -p com.example.pantrypal -c android.intent.category.LAUNCHER 1
```

---

# 10. Sequenza completa consigliata

## 10.1 Sequenza completa pulita con un solo dispositivo

```powershell
Set-Location "D:\Universita\Terzo_Anno\mobile"

adb devices -l

.\gradlew.bat clean

.\gradlew.bat :app:assembleDebug

.\gradlew.bat :app:testDebugUnitTest

adb uninstall com.example.pantrypal

adb install --no-streaming -r "D:\Universita\Terzo_Anno\mobile\app\build\outputs\apk\debug\app-debug.apk"

adb shell monkey -p com.example.pantrypal -c android.intent.category.LAUNCHER 1
```

---

## 10.2 Sequenza completa pulita con più dispositivi

```powershell
Set-Location "D:\Universita\Terzo_Anno\mobile"

adb devices -l

$env:ADB_DEVICE_ID = "emulator-5554"

.\gradlew.bat clean

.\gradlew.bat :app:assembleDebug

.\gradlew.bat :app:testDebugUnitTest

adb -s $env:ADB_DEVICE_ID uninstall com.example.pantrypal

adb -s $env:ADB_DEVICE_ID install --no-streaming -r "D:\Universita\Terzo_Anno\mobile\app\build\outputs\apk\debug\app-debug.apk"

adb -s $env:ADB_DEVICE_ID shell monkey -p com.example.pantrypal -c android.intent.category.LAUNCHER 1
```

Sostituisci:

```powershell
$env:ADB_DEVICE_ID = "emulator-5554"
```

con l’ID reale del dispositivo che vuoi usare.

---

## 10.3 Sequenza veloce senza cancellare dati

```powershell
Set-Location "D:\Universita\Terzo_Anno\mobile"

adb devices -l

.\gradlew.bat :app:assembleDebug

adb install --no-streaming -r "D:\Universita\Terzo_Anno\mobile\app\build\outputs\apk\debug\app-debug.apk"

adb shell monkey -p com.example.pantrypal -c android.intent.category.LAUNCHER 1
```

Con più dispositivi:

```powershell
Set-Location "D:\Universita\Terzo_Anno\mobile"

adb devices -l

$env:ADB_DEVICE_ID = "emulator-5554"

.\gradlew.bat :app:assembleDebug

adb -s $env:ADB_DEVICE_ID install --no-streaming -r "D:\Universita\Terzo_Anno\mobile\app\build\outputs\apk\debug\app-debug.apk"

adb -s $env:ADB_DEVICE_ID shell monkey -p com.example.pantrypal -c android.intent.category.LAUNCHER 1
```

---

# 11. Problemi comuni

## ADB non riconosciuto

Errore:

```text
adb : The term 'adb' is not recognized
```

Soluzione temporanea:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\emulator;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:Path"
```

Poi:

```powershell
adb version
```

---

## Telefono unauthorized

Errore:

```text
unauthorized
```

Soluzione:

1. guarda il telefono;
2. accetta il popup “Consenti debug USB”;
3. rilancia:

```powershell
adb devices -l
```

---

## Più dispositivi collegati

Errore:

```text
more than one device/emulator
```

Soluzione:

```powershell
adb devices -l
$env:ADB_DEVICE_ID = "ID_DISPOSITIVO"
adb -s $env:ADB_DEVICE_ID install --no-streaming -r "D:\Universita\Terzo_Anno\mobile\app\build\outputs\apk\debug\app-debug.apk"
```

---

## Riavviare ADB

```powershell
adb kill-server
adb start-server
adb devices -l
```

---

# 12. API key ricette Spoonacular

Per testare le ricette reali serve la key in:

```text
D:\Universita\Terzo_Anno\mobile\local.properties
```

Esempio:

```properties
sdk.dir=C\:\\Users\\cecca\\AppData\\Local\\Android\\Sdk
SPOONACULAR_API_KEY=la_tua_chiave
```

Open Food Facts non richiede API key.

Dopo aver modificato `local.properties`, ricompila:

```powershell
.\gradlew.bat :app:assembleDebug
```
