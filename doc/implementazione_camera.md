# PantryPal Prompt 4.1 — CameraX + ML Kit Barcode Scanner controllato

## Summary

Implementare la scansione barcode reale con fotocamera usando CameraX + ML Kit Barcode Scanning, integrandola nel flow di aggiunta alimento già esistente di PantryPal.

Obiettivo: sostituire/affiancare l’inserimento manuale del barcode con uno scanner reale, senza rompere il flow manuale già funzionante.

Questo prompt deve essere eseguito in modo incrementale e sicuro. Non lasciare il task a metà: ogni fase deve compilare prima di passare alla successiva.

---

# Regole fondamentali

## 1. Non rompere il fallback manuale

Il flow manuale deve restare sempre funzionante.

Se camera, permesso o ML Kit falliscono, l’utente deve poter premere:

```text
Inserisci manualmente
```

e arrivare ad Aggiunta Manuale come prima.

## 2. Nessuna persistenza automatica dal barcode

La scansione barcode e Open Food Facts possono solo:

```text
- riconoscere il barcode
- leggere prodotto esterno
- precompilare/suggerire la categoria
```

Non devono salvare subito `BarcodeProductLink`.

Il link barcode → FoodCategory viene salvato solo quando l’utente arriva al form e preme:

```text
Salva
```

Condizioni per salvare il link:

```text
- flow deriva da barcode riconosciuto
- dati prodotto completi disponibili
- utente ha selezionato una FoodCategory
- SaveAddedFoodUseCase completa il salvataggio
```

## 3. Evitare scan multipli

La camera può leggere lo stesso barcode molte volte in pochi millisecondi.

Dopo il primo barcode valido:

```text
- bloccare ulteriori letture
- non fare chiamate API duplicate
- non aprire più volte sheet/navigazioni
```

Usare uno stato tipo:

```kotlin
private var isProcessingBarcode = false
```

oppure equivalente nel ViewModel/analyzer.

## 4. Navigazione add-flow

Durante Scan Barcode, ProductRecognized e Aggiunta Manuale:

```text
bottom nav nascosta
```

Alla fine del flow, dopo Salva o Annulla/Back, si torna alla schermata da cui è partito il FAB.

Esempi:

```text
Home → + → Scan → Manuale → Salva → Home
Dispensa → + → Scan → Manuale → Salva → Dispensa
Ricette → + → Scan → Manuale → Salva → Ricette
Dettaglio Ricetta → + → Scan → Manuale → Salva → Dettaglio Ricetta
```

Back da Manual Add dopo Scan non deve tornare alla camera: deve tornare alla schermata origine.

## 5. Non implementare notifiche in questo prompt

Questo prompt riguarda solo:

```text
CameraX + ML Kit barcode scanner + integrazione flow barcode
```

Fuori scope:

```text
- notifiche reali
- WorkManager notifiche
- redesign UI
- nuove API
- refactor architetturale massivo
- modifiche schema Room non necessarie
```

---

# Fase 0 — Ispezione progetto

Prima di modificare codice:

1. Ispezionare struttura attuale:

   * `feature/addfood`
   * `core/navigation`
   * `data/product` o repository barcode/Open Food Facts
   * `domain/usecase/ResolveBarcodeUseCase`
   * `SaveAddedFoodUseCase`
   * `ProductRecognized` sheet se già esiste
   * `AddFoodUiState`
   * `AddFoodEvent`
   * `AddFoodEffect`

2. Verificare cosa è già implementato:

   * input manuale barcode mock;
   * Open Food Facts lookup;
   * cache locale barcode;
   * ProductRecognized sheet;
   * Manual Add precompilata da barcode;
   * salvataggio `BarcodeProductLink` su Save.

3. Non duplicare classi già esistenti. Estendere il flow attuale.

---

# Fase 1 — Dipendenze e Manifest

## Dipendenze

Aggiungere dipendenze CameraX e ML Kit in modo coerente con il sistema Gradle già usato dal progetto.

Usare version catalog se il progetto lo usa.

Dipendenze necessarie:

```text
CameraX:
- camera-core
- camera-camera2
- camera-lifecycle
- camera-view

ML Kit:
- barcode-scanning
```

Non fissare versioni a caso se esiste già version catalog. Usare versioni compatibili con compileSdk/AGP attuali.

## Manifest

Aggiungere permesso:

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

Aggiungere feature camera non obbligatoria:

```xml
<uses-feature
    android:name="android.hardware.camera"
    android:required="false" />
```

Motivo: l’app deve installarsi anche su emulatori/dispositivi senza camera.

## Checkpoint obbligatorio

Dopo Fase 1 eseguire:

```powershell
.\gradlew.bat :app:assembleDebug
```

Se fallisce, sistemare prima di procedere.

---

# Fase 2 — Stato UI, Eventi e Permission Flow

## Obiettivo

Gestire il permesso camera senza crash e senza bloccare l’utente.

## UiState

Estendere lo stato dello scan/add flow con campi equivalenti a:

```kotlin
data class ScanBarcodeUiState(
    val hasCameraPermission: Boolean = false,
    val isRequestingPermission: Boolean = false,
    val isCameraReady: Boolean = false,
    val isProcessingBarcode: Boolean = false,
    val scannedBarcode: String? = null,
    val errorMessage: String? = null,
    val showManualFallback: Boolean = true
)
```

Adattare ai nomi già presenti nel progetto.

## Eventi

Aggiungere eventi equivalenti:

```kotlin
sealed interface AddFoodEvent {
    data object OnRequestCameraPermissionClick : AddFoodEvent
    data class OnCameraPermissionResult(val granted: Boolean) : AddFoodEvent
    data class OnBarcodeDetected(val value: String) : AddFoodEvent
    data object OnManualInsertClick : AddFoodEvent
    data object OnRetryScanClick : AddFoodEvent
}
```

Non è obbligatorio usare esattamente questi nomi, ma servono eventi equivalenti.

## Effetti

Aggiungere effetti one-shot equivalenti:

```kotlin
sealed interface AddFoodEffect {
    data object RequestCameraPermission : AddFoodEffect
    data class ShowSnackbar(val message: String) : AddFoodEffect
}
```

La richiesta permesso può stare in Screen usando Activity Result API, ma l’intenzione deve passare da ViewModel/Effect.

## UI permesso

La schermata Scan deve gestire questi stati:

### Permesso non ancora concesso

Mostrare:

* titolo “Scansiona barcode”
* testo breve “Consenti l’accesso alla fotocamera per leggere il codice a barre”
* bottone “Consenti fotocamera”
* bottone secondario “Inserisci manualmente”

### Permesso negato

Mostrare:

* messaggio leggero
* bottone “Riprova”
* bottone “Inserisci manualmente”

### Permesso negato permanentemente

Mostrare:

* messaggio “Permesso fotocamera negato. Puoi abilitarlo dalle impostazioni.”
* bottone “Inserisci manualmente”

Non è obbligatorio aprire le impostazioni in questa fase, ma è accettabile se implementato in modo pulito.

## Checkpoint obbligatorio

Dopo Fase 2 eseguire:

```powershell
.\gradlew.bat :app:assembleDebug
```

Non procedere se la build fallisce.

---

# Fase 3 — CameraX Preview + ML Kit Analyzer isolato

## Obiettivo

Far funzionare la camera e leggere un barcode reale, senza ancora integrare Open Food Facts.

La Fase 3 deve produrre una schermata scanner funzionante che:

* mostra preview camera;
* rileva barcode;
* blocca letture duplicate;
* mostra/propaga il valore letto;
* permette fallback manuale.

## Implementazione consigliata

Creare o aggiornare:

```text
feature/addfood/scan/ScanBarcodeScreen.kt
feature/addfood/scan/BarcodeAnalyzer.kt
```

oppure usare la struttura feature già esistente.

## CameraX

Usare:

```kotlin
PreviewView
ProcessCameraProvider
Preview
ImageAnalysis
CameraSelector.DEFAULT_BACK_CAMERA
```

Bind al lifecycle della schermata.

Regole:

* usare camera posteriore;
* non usare flash obbligatorio;
* non introdurre overlay complessi;
* release automatico con lifecycle.

## Analyzer

Creare analyzer ML Kit:

```kotlin
class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer
```

Regole analyzer:

* chiudere sempre `imageProxy`;
* usare `InputImage.fromMediaImage`;
* leggere solo formati barcode comuni, se semplice:

  * EAN_13
  * EAN_8
  * UPC_A
  * UPC_E
  * QR_CODE opzionale ma non necessario;
* se `rawValue` non è nullo/vuoto, chiamare callback una sola volta.

Pseudo-regola:

```kotlin
if (hasDetectedBarcode.get()) {
    imageProxy.close()
    return
}

if (barcodeValue.isNotBlank()) {
    hasDetectedBarcode.set(true)
    onBarcodeDetected(barcodeValue)
}
```

## Validazione barcode

Prima di passare il valore al flow:

* trim;
* accettare solo stringhe non vuote;
* preferire codici numerici EAN/UPC;
* se valore non valido, ignorare e continuare scan.

Non bloccare il flusso per QR non alimentari.

## Anti-duplicazione

Implementare blocco a due livelli se possibile:

1. analyzer smette di emettere dopo il primo barcode;
2. ViewModel ignora `OnBarcodeDetected` se `isProcessingBarcode = true`.

## UI scanner

Mostrare:

* preview camera;
* piccolo testo “Inquadra il codice a barre”;
* eventuale frame/box centrale semplice;
* bottone “Inserisci manualmente” sempre disponibile;
* stato loading “Analisi barcode…” quando processing.

## Checkpoint obbligatorio

Dopo Fase 3:

```powershell
.\gradlew.bat :app:assembleDebug
```

Poi installare e provare su telefono reale.

Verifica manuale minima:

* apro Scan;
* concedo camera;
* vedo preview;
* inquadro barcode;
* il valore viene rilevato una sola volta;
* posso usare “Inserisci manualmente”.

Non integrare API finché la preview camera non funziona.

---

# Fase 4 — Collegare barcode rilevato a ResolveBarcodeUseCase

## Obiettivo

Quando lo scanner legge un barcode, usare il flow già previsto:

```text
barcode rilevato
↓
ResolveBarcodeUseCase
↓
controllo cache locale BarcodeProductLink
↓
se cache hit → apri Manual Add precompilata con categoria selezionata
↓
se cache miss → Open Food Facts
↓
se prodotto trovato → ProductRecognized sheet
↓
se not found/network/error → messaggio + fallback manuale
```

## Regole repository/use case

Non chiamare Open Food Facts direttamente dalla UI.

Flusso corretto:

```text
ScanBarcodeScreen
→ AddFoodViewModel event OnBarcodeDetected
→ ResolveBarcodeUseCase
→ ProductRepository / FoodRecognitionRepository / PantryRepository
→ UiState / Effect
```

## Stati attesi

### Barcode locale già noto

Se esiste `BarcodeProductLink` attivo:

```text
- aprire Aggiunta Manuale
- categoria interna già selezionata
- search field precompilato con nome categoria
- eventuali dati prodotto/local cache se disponibili
```

Non chiamare Open Food Facts.

### Barcode riconosciuto da Open Food Facts

Se API trova prodotto:

```text
- mostra ProductRecognized bottom sheet
- mostra nome prodotto
- brand se presente
- quantità commerciale se presente
- immagine se presente o placeholder
- CTA “Aggiungi o modifica”
- CTA “Inserisci manualmente” o “Non è questo”
```

Entrando in Manual Add:

* precompilare campo ricerca con nome prodotto/genericName;
* suggerire categorie;
* preselezionare categoria solo se il matching locale è forte;
* se match debole, nessun badge selezionato.

### Barcode non riconosciuto

Mostrare warning:

```text
Barcode non riconosciuto. Aggiungilo manualmente alla tua dispensa.
```

Poi permettere:

* “Inserisci manualmente”
* oppure apertura automatica Manual Add vuota, se già previsto dal flow.

Non salvare `BarcodeProductLink`.

### Network/API error

Mostrare snackbar/messaggio leggero:

* “Connessione non disponibile”
* “Servizio prodotto non disponibile”
* “Limite richieste raggiunto”

Lasciare fallback manuale.

Non restare bloccati su loading.

## Retry

Dopo errore:

* `isProcessingBarcode = false`;
* permettere retry scan;
* permettere manuale.

## Checkpoint obbligatorio

Dopo Fase 4:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

Non procedere se build/test falliscono.

---

# Fase 5 — Salvataggio BarcodeProductLink solo su Save

## Obiettivo

Assicurarsi che il link barcode venga scritto solo al salvataggio finale del form.

## Regola

Il ViewModel/AddFoodUiState deve trasportare il prodotto riconosciuto fino al save:

```kotlin
recognizedBarcodeProduct: RecognizedBarcodeProductUi?
```

o equivalente.

Quando l’utente preme `Salva`:

```text
SaveAddedFoodUseCase riceve:
- selectedCategoryId o pendingNewCategory
- expirationDate
- quantity
- recognizedBarcodeProduct, se presente
```

Il use case salva:

* FoodCategory se nuova;
* ExpiryLot;
* BarcodeProductLink solo se recognizedBarcodeProduct valido e categoria selezionata.

## Non fare

Non salvare barcode link:

* appena ML Kit legge il barcode;
* appena Open Food Facts risponde;
* se barcode non è riconosciuto;
* se utente entra manualmente senza prodotto riconosciuto;
* se utente non seleziona categoria.

## Checkpoint obbligatorio

Dopo Fase 5:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

---

# Fase 6 — UX polish scanner minimo

Solo dopo che funziona tutto:

Rifinire la UI dello scanner:

* titolo coerente;
* preview full width;
* frame centrale leggero;
* testo guida;
* bottone manuale visibile;
* loading non invasivo;
* dark mode leggibile;
* errore/snackbar coerente con design system.

Non fare redesign totale.

---

# Test Plan

## Unit test / ViewModel test

Aggiungere o aggiornare test dove possibile:

### Barcode detected

* `OnBarcodeDetected` con valore valido imposta `isProcessingBarcode = true`.
* secondo `OnBarcodeDetected` mentre processing viene ignorato.
* errore rete rimette `isProcessingBarcode = false`.

### Resolve barcode

* cache hit locale non chiama Open Food Facts.
* cache miss chiama Open Food Facts.
* prodotto trovato apre stato ProductRecognized.
* not found mostra fallback manuale.
* network error mostra messaggio e fallback.

### Save

* con recognized product + categoria selezionata → salva BarcodeProductLink.
* con barcode non riconosciuto → non salva BarcodeProductLink.
* senza categoria selezionata → errore validazione, non salva link.

Non serve testare CameraX con unit test puro se complicato. Testare CameraX manualmente su telefono.

---

# Manual QA obbligatoria su telefono reale

Provare almeno questi casi:

## Caso 1 — Permesso camera concesso

```text
Apri app
Tap +
Tap Scansiona barcode
Concedi permesso camera
Vedi preview
Inquadra barcode
Barcode letto una sola volta
```

## Caso 2 — Permesso camera negato

```text
Nega permesso
La schermata non crasha
Vedi fallback “Inserisci manualmente”
Manual Add funziona
```

## Caso 3 — Barcode riconosciuto da Open Food Facts

```text
Scanner legge barcode
ProductRecognized sheet appare
Nome prodotto visibile
Aggiungi o modifica apre Manual Add precompilata
Seleziona categoria
Inserisci data/quantità
Salva
Torna alla schermata origine
```

## Caso 4 — Barcode non riconosciuto

```text
Scanner legge barcode ignoto
Mostra warning
Manual Add resta disponibile
Nessun BarcodeProductLink viene salvato
```

## Caso 5 — Barcode già noto dopo primo salvataggio

```text
Scansiona un prodotto riconosciuto
Salva associandolo a una FoodCategory
Riscansiona lo stesso barcode
Deve usare cache locale
Non deve richiamare Open Food Facts
Manual Add si apre con categoria già selezionata
```

## Caso 6 — Back navigation

Verificare:

```text
Home → + → Scan → Manuale → back → Home
Dispensa → + → Scan → Manuale → back → Dispensa
Ricette → + → Scan → Manuale → back → Ricette
```

La bottom nav è nascosta durante Scan/Manual Add e torna visibile dopo uscita dal flow.

## Caso 7 — Anti duplicazione

Inquadrare lo stesso barcode per qualche secondo.

Risultato atteso:

```text
una sola lettura
una sola chiamata ResolveBarcodeUseCase
un solo sheet/navigazione
```

---

# Stop rules

Se una fase rompe la build:

```text
fermarsi
sistemare la build
non procedere alla fase successiva
```

Se CameraX o ML Kit crea un problema bloccante:

```text
mantenere fallback manuale funzionante
non rompere il flow Add Food
segnalare chiaramente nel riepilogo finale cosa manca
```

Se Open Food Facts non è configurata o la rete fallisce:

```text
mostrare errore leggero
non bloccare la UI
manual insert sempre disponibile
```

---

# Verifiche finali obbligatorie

Eseguire:

```powershell
.\gradlew.bat :app:assembleDebug
```

Poi:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Riportare nel riepilogo finale:

* fasi completate;
* file principali modificati;
* risultato build;
* risultato test;
* cosa è stato provato manualmente su telefono;
* eventuali limitazioni rimaste.

---

# Output finale richiesto

Alla fine rispondere con:

```text
Completato:
- Camera permission
- CameraX preview
- ML Kit analyzer
- Anti duplicate scan
- ResolveBarcodeUseCase integration
- ProductRecognized flow
- Save barcode link only on final Save
- Manual fallback preserved

Build:
- assembleDebug: OK/KO
- testDebugUnitTest: OK/KO

Manual QA:
- dispositivo usato
- permesso camera
- barcode riconosciuto
- barcode non riconosciuto
- back navigation
```
