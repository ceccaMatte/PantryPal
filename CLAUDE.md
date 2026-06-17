# CLAUDE.md — PantryPal Android Context

## Scopo del file

Questo file serve come contesto operativo per Claude Code quando lavora sul progetto Android **PantryPal**.

Il progetto è un'app Android nativa in Kotlin + Jetpack Compose per gestire la dispensa, tracciare scadenze, suggerire ricette e, nelle prossime fasi, gestire notifiche reali e scansione barcode tramite camera.

Claude Code deve usare questo file come guard rail: prima di modificare codice, leggere le sezioni sotto e rispettare architettura, scope e decisioni già prese.

---

# 1. Stato attuale del progetto

Il progetto è già stato portato avanti tramite diversi prompt incrementali.

## Già implementato

### Base Android / Architettura

- Package app: `com.example.pantrypal`.
- Kotlin + Jetpack Compose.
- Material 3.
- Single Activity architecture.
- Hilt.
- Navigation Compose con navigator/logica custom.
- Room.
- DataStore Preferences.
- Retrofit / network predisposto.
- WorkManager predisposto, ma notifiche reali ancora non completate.
- Feature-first + MVVM + UDF + Repository + UseCase selettivi.

### Dispensa locale

- Home con riepilogo dispensa.
- Dispensa con filtri storage.
- Aggiunta manuale alimento.
- Dettaglio alimento.
- Gestione lotti/scadenze.
- Aggregazione per `FoodCategory + expirationDate`.
- Profilo con impostazioni locali.
- Seed dati alimenti e alias ingredienti.

### Ricette e API

- Ricerca ricette tramite Spoonacular.
- Preferiti ricette persistenti in Room.
- Dettaglio ricetta.
- Matching ingredienti ricetta ↔ alimenti interni tramite mapping persistenti.
- Cache RAM sessione per ricette cercate.
- Refresh Home suggerimenti controllato.
- Open Food Facts / barcode lookup predisposto o parzialmente integrato, ma camera reale non ancora implementata.

### UI polish recente

- Prompt 3.3 in corso/completato per migliorare:
  - dark mode;
  - bottom nav più compatta;
  - card ricette;
  - dettaglio ricetta;
  - modal collegamenti;
  - profilo;
  - placeholder immagini;
  - blocco scadenze condiviso tra Add Manuale e Dettaglio Alimento.

---

# 2. Architettura da rispettare

L'app segue questa architettura:

```text
Compose UI
↓
ViewModel
↓
UseCase solo dove serve
↓
Repository
↓
Room / API / DataStore / WorkManager / NotificationManager
```

## Regole fondamentali

- La UI non accede mai direttamente a Room.
- La UI non accede mai direttamente alle API.
- La UI non accede direttamente a DataStore.
- La UI mostra `UiState` e invia `Event` al ViewModel.
- Il ViewModel espone `StateFlow<UiState>`.
- Gli eventi one-shot sono `Effect` tramite `Channel` o `SharedFlow`.
- I Repository sono l'unico punto di accesso ai dati.
- Gli UseCase si usano solo per logica non banale o multi-repository.
- Evitare refactor architetturali massivi se non richiesti.

## Pattern UDF

Ogni schermata deve seguire:

```text
UiState → UI → UserEvent → ViewModel → Repository/UseCase → nuovo UiState
```

---

# 3. Package attesi

Struttura logica consigliata/attuale:

```text
com.example.pantrypal
│
├── core
│   ├── database
│   ├── datastore
│   ├── network
│   ├── notification
│   ├── navigation
│   ├── designsystem
│   └── util
│
├── data
│   ├── pantry
│   ├── product
│   ├── recipe
│   └── settings
│
├── domain
│   ├── model
│   └── usecase
│
├── feature
│   ├── home
│   ├── pantry
│   ├── addfood
│   ├── recipes
│   └── profile
│
└── di
```

Non creare repository inutili se quelli esistenti bastano.

Repository previsti:

```text
PantryRepository
FoodRecognitionRepository / ProductRepository
RecipeRepository
SettingsRepository
NotificationRepository
```

---

# 4. Data model concettuale

PantryPal separa sempre tre concetti:

```text
Prodotto reale acquistato ≠ FoodCategory interna ≠ Ingrediente ricetta API
```

## FoodCategory

È l'alimento interno usato dall'app.

Esempi:

```text
Latte
Pasta
Olio
Pomodoro
Petto di pollo
Yogurt
```

La dispensa aggrega sempre per `FoodCategory`, non per barcode/prodotto commerciale.

## BarcodeProductLink

Rappresenta:

```text
barcode / prodotto reale → FoodCategory
```

Esempio:

```text
8001234567890 → Latte Parmalat Zymil 1L → Latte
```

Un barcode attivo punta a una sola FoodCategory.
Più barcode possono puntare alla stessa FoodCategory.

## RecipeIngredientLink

Rappresenta:

```text
ingrediente ricetta / alias → FoodCategory
```

È una relazione molti-a-molti.

Esempio:

```text
pasta → Pasta
spaghetti → Pasta
pollo → Petto di pollo
pollo → Cosce di pollo
olio d'oliva → Olio
```

## ExpiryLot

Rappresenta la quantità reale in dispensa aggregata per:

```text
FoodCategory + expirationDate
```

Esempio:

```text
Latte — 20/07 — ×2
```

Regola fondamentale:

```text
stessa categoria + stessa scadenza → incrementa quantity
```

---

# 5. Regole di persistenza

## Persistenza esplicita

Scrivere nel database solo quando il flow lo prevede chiaramente.

Esempi con conferma:

```text
Aggiunta Manuale → Salva
Dettaglio Alimento → Salva modifiche
```

Esempi auto-save:

```text
Like ricetta
Tema
Lingua
Nome utente
Soglie notifiche
```

## Stato UI temporaneo

Nel Dettaglio Ricetta, azioni come:

```text
Ce l'ho
Sposta in Da comprare
Scelta temporanea categoria per share
```

sono solo UI state e non scrivono su Room.

## Mapping persistenti

Mapping reali sono persistenti:

```text
barcode → FoodCategory
ingrediente ricetta → FoodCategory
```

ma devono essere creati solo dopo conferma esplicita dell'utente.

---

# 6. Ricette e availability

## Ricette preferite

- Solo le ricette preferite vengono salvate in Room.
- Il like salva la ricetta senza conferma.
- L'unlike rimuove la ricetta preferita e i suoi ingredienti salvati.
- I mapping globali `RecipeIngredientLinkEntity` non vengono eliminati togliendo il like.

## Ricette non preferite

- Restano in RAM durante la sessione.
- Non vengono persistite in Room.
- Spariscono alla chiusura dell'app/processo.

## Availability ricetta

Un ingrediente ricetta è “In dispensa” solo se:

```text
RecipeIngredient
→ normalizedName / externalIngredientId
→ RecipeIngredientLink attivi
→ FoodCategory collegate
→ almeno un ExpiryLot attivo con quantity > 0
```

Non basta un match testuale diretto.
Il matching testuale può suggerire collegamenti nel modal, ma non deve creare mapping da solo.

---

# 7. Cache RAM ricette

Esiste o deve esistere una cache RAM di sessione:

```text
SessionRecipeCache
```

Regole:

- Accumula risultati ricerca Ricette.
- Accumula risultati refresh Home.
- Merge per `externalId`.
- Non cancella risultati precedenti su nuova ricerca.
- Non viene svuotata cambiando tab.
- Non viene svuotata tornando in Home.
- Si resetta naturalmente solo alla chiusura del processo/app.

Le card senza ingredienti completi non devono mostrare counter fake `0/0`.
Mostrare “Da collegare” o nessun counter.

---

# 8. Home suggested recipes

La Home suggerisce ricette in base agli ingredienti presenti in dispensa.

Regola quota-safe attuale:

- Nessuna chiamata API automatica quando cambia la dispensa.
- Quando cambia il set delle FoodCategory attive, la Home imposta solo:

```kotlin
canMakeSuggestedRecipesQuery = true
```

- Il refresh Home chiama Spoonacular `findByIngredients` solo se:

```text
utente preme refresh
canMakeSuggestedRecipesQuery = true
isRefreshing = false
```

- Dopo refresh riuscito:

```text
merge risultati in SessionRecipeCache
canMakeSuggestedRecipesQuery = false
```

- Se flag false, refresh disabilitato o snackbar leggero.
- La tab Ricette NON usa questo flag: lì la ricerca dipende dalla query utente.

---

# 9. Navigazione e add flow

Bottom navigation principale:

```text
Home
Dispensa
FAB +
Ricette
Profilo
```

La bottom nav è visibile nelle schermate principali e secondarie standard:

```text
Home
Dispensa
Dettaglio Alimento
Ricette
Dettaglio Ricetta
Profilo
```

La bottom nav è nascosta durante tutto il flow di aggiunta alimento:

```text
AddChoice
Scan Barcode
ProductRecognized
Aggiunta Manuale
```

Il flow di aggiunta torna sempre alla schermata di origine.

Esempi:

```text
Home → + → Aggiunta Manuale → Salva → Home
Dispensa → + → Aggiunta Manuale → Salva → Dispensa
Ricette → + → Aggiunta Manuale → Salva → Ricette
Dettaglio Ricetta → + → Aggiunta Manuale → Salva → Dettaglio Ricetta
```

Back da Manual Add dopo Scan deve saltare la camera e tornare all'origine.

---

# 10. Barcode / Open Food Facts

API scelta per prodotti/barcode:

```text
Open Food Facts
```

Endpoint consigliato:

```text
GET https://world.openfoodfacts.org/api/v3.6/product/{barcode}.json
```

Request deve avere User-Agent identificabile, ad esempio:

```text
PantryPal/1.0 (student-project)
```

Campi logici utili:

```text
barcode
productName
genericName
brand
quantityLabel
imageUrl
rawCategoryTags
rawFoodGroupTags
```

## Flusso barcode corretto

```text
Scan barcode
↓
controlla BarcodeProductLink locale attivo
↓
se cache hit → Manual Add precompilata con categoria selezionata
↓
se cache miss e online → chiama Open Food Facts
↓
se trovato → ProductRecognized sheet
↓
utente conferma / modifica
↓
Manual Add
↓
Salva
↓
solo ora salva BarcodeProductLink
```

## Regole importanti

Non salvare `BarcodeProductLink`:

- appena la camera legge il barcode;
- appena Open Food Facts risponde;
- se barcode non riconosciuto;
- se prodotto non ha dati sufficienti;
- se utente non seleziona categoria;
- se flow è manuale puro.

Salvare `BarcodeProductLink` solo in `SaveAddedFoodUseCase` quando il form viene salvato correttamente.

---

# 11. CameraX + ML Kit Barcode Scanner — task futuro

Quando si implementa la camera reale, usare:

```text
CameraX + ML Kit Barcode Scanning
```

## Dipendenze attese

CameraX:

```text
camera-core
camera-camera2
camera-lifecycle
camera-view
```

ML Kit:

```text
barcode-scanning
```

Usare version catalog se presente.

## Manifest

```xml
<uses-permission android:name="android.permission.CAMERA" />

<uses-feature
    android:name="android.hardware.camera"
    android:required="false" />
```

## Scanner rules

- Camera posteriore.
- Fallback manuale sempre visibile.
- Gestire permesso camera runtime.
- Non crashare se permesso negato.
- Preview rilasciata correttamente al back/background.
- Analyzer ML Kit deve chiudere sempre `imageProxy`.
- Bloccare scan multipli.
- Dopo primo barcode valido, ignorare ulteriori letture finché processing non termina.

## Anti-duplicazione obbligatoria

Usare guardia tipo:

```kotlin
if (isProcessingBarcode) return
```

oppure equivalente sia nell'analyzer sia nel ViewModel.

Obiettivo:

```text
1 barcode letto = 1 ResolveBarcodeUseCase = 1 sheet/navigazione
```

---

# 12. Notifiche — task futuro separato

Le notifiche reali devono usare:

```text
WorkManager + NotificationManager
```

Devono dipendere da:

- dati Room;
- impostazioni DataStore;
- soglie fresh/long-life;
- permesso notifiche Android 13+.

Devono produrre una notifica riepilogativa unica giornaliera, circa alle 09:00.

Tap notifica:

```text
apre Dispensa
filtro Tutti
sezione In scadenza visibile
```

Non implementare notifiche insieme alla camera se il task corrente è solo barcode.

---

# 13. UI / design system

Stile generale:

- mobile-first;
- palette verde/crema;
- card arrotondate;
- layout morbido;
- bottom nav persistente;
- FAB centrale;
- dark mode coerente;
- placeholder immagini coerenti.

Usare `doc/ux_impl` e screenshot/mock come riferimento visivo.
Non fare pixel-perfect se richiede refactor eccessivo.
Priorità:

```text
coerenza
leggibilità
stabilità
aderenza ragionevole ai mock
```

---

# 14. Regole per Cloud Code / Claude Code

## Prima di modificare

- Leggere i file rilevanti.
- Cercare implementazioni già esistenti.
- Non duplicare classi o use case già presenti.
- Non cambiare schema Room se non richiesto esplicitamente.
- Non introdurre nuove API non previste.
- Non fare redesign totale.

## Durante il lavoro

- Procedere a piccoli step.
- Dopo ogni fase grande, eseguire build.
- Se build fallisce, fermarsi e sistemare prima di continuare.
- Non lasciare task a metà.
- Mantenere fallback manuale sempre funzionante.

## Comandi finali obbligatori

Da eseguire prima di dire “completato”:

```powershell
.\gradlew.bat :app:assembleDebug
```

poi:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Output finale richiesto

Alla fine riportare:

```text
Completato:
- elenco modifiche principali

File modificati:
- elenco file/aree principali

Build:
- assembleDebug: OK/KO
- testDebugUnitTest: OK/KO

Limitazioni:
- eventuali problemi rimasti

Manual QA:
- cosa è stato testato su telefono/emulatore
```

---

# 15. Stop rules

Se una fase rompe la build:

```text
fermarsi
sistemare la build
non procedere oltre
```

Se CameraX/ML Kit crea problemi bloccanti:

```text
mantenere fallback manuale funzionante
non rompere Add Food
segnalare chiaramente cosa manca
```

Se Open Food Facts fallisce o API key/config non è disponibile:

```text
mostrare errore leggero
non bloccare UI
manual insert sempre disponibile
non salvare dati parziali
```

Se una modifica richiede refactor enorme:

```text
non farlo automaticamente
scegli soluzione minima e stabile
segnala limitazione
```

---

# 16. Priorità prossime consigliate

Dopo Prompt 3.3 UI polish:

1. CameraX + ML Kit barcode scanner base.
2. Integrazione scanner con `ResolveBarcodeUseCase` e Open Food Facts.
3. Notifiche reali con WorkManager + NotificationManager.

Non unire camera e notifiche nello stesso task se non espressamente richiesto.
