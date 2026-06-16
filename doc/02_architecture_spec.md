# PantryPal — Specifica architetturale

## 1. Scopo del documento

Questo documento fissa l'architettura tecnica da usare per sviluppare PantryPal, app Android nativa in Kotlin + Jetpack Compose.

L'obiettivo è avere una struttura ordinata, flessibile e veloce da implementare, evitando over-engineering.

Questa specifica deve essere usata come guard rail durante lo sviluppo: ogni nuova feature deve rispettare le scelte architetturali definite qui.

---

## 2. Stack tecnologico

### Linguaggio e UI

- Kotlin
- Jetpack Compose
- Material 3
- Single Activity architecture

### Persistenza locale

- Room per dati strutturati:
  - alimenti;
  - confezioni;
  - cache prodotti;
  - ricette preferite.
- DataStore Preferences per impostazioni utente:
  - nome utente;
  - tema;
  - lingua;
  - notifiche abilitate;
  - soglie notifiche.

### API esterne

- Open Food Facts per barcode/prodotti.
- Spoonacular per ricette.
- Edamam come fallback se Spoonacular non rientra nei limiti del piano gratuito.

### Background work e notifiche

- WorkManager per scheduling affidabile.
- NotificationManager per notifiche locali Android.

### Dependency Injection

- Hilt.
- 
---

## 3. Architettura scelta

L'architettura scelta è:

```text
Feature-first + MVVM + UDF + Repository + UseCase selettivi
```

Struttura logica:

```text
Compose UI
↓
ViewModel
↓
UseCase solo dove serve
↓
Repository
↓
Room / API / DataStore / WorkManager
```

Non si usa una Clean Architecture completa e rigida.

La priorità è mantenere:

- codice leggibile;
- sviluppo rapido;
- buona separazione delle responsabilità;
- facilità di modifica;
- niente astrazioni inutili.

---

## 4. Regole architetturali fondamentali

### 4.1 UI

La UI deve essere composta da funzioni Composable pure, per quanto possibile.

La UI:

- mostra lo stato ricevuto dal ViewModel;
- invia eventi utente al ViewModel;
- non accede direttamente a Room;
- non accede direttamente alle API;
- non contiene logica di business complessa;
- non contiene regole di persistenza.

Esempio corretto:

```kotlin
PantryScreen(
    state = state,
    onEvent = viewModel::onEvent
)
```

Esempio da evitare:

```kotlin
val db = Room.databaseBuilder(...)
```

all'interno di una Composable.

---

### 4.2 ViewModel

Il ViewModel gestisce lo stato della schermata.

Ogni ViewModel espone:

- uno `StateFlow<UiState>`;
- una funzione per ricevere eventi utente;
- eventuali effetti one-shot tramite `Channel` o `SharedFlow`.

Il ViewModel può contenere logica semplice di presentazione, ma non deve diventare il luogo dove vive tutta la logica dell'app.

Responsabilità del ViewModel:

- caricare dati per la schermata;
- trasformare dati in stato UI;
- reagire agli eventi utente;
- chiamare use case o repository;
- emettere effetti di navigazione, snackbar, share sheet.

---

### 4.3 UDF — Unidirectional Data Flow

Ogni schermata deve seguire questo flusso:

```text
UiState → UI → UserEvent → ViewModel → Repository/UseCase → nuovo UiState
```

Il dato scorre in una sola direzione.

Pattern consigliato:

```kotlin
data class PantryUiState(...)

sealed interface PantryEvent {
    data object OnAddClick : PantryEvent
    data class OnDecrementClick(val foodId: Long) : PantryEvent
}

sealed interface PantryEffect {
    data class Navigate(val route: AppRoute) : PantryEffect
    data class ShowMessage(val message: String) : PantryEffect
}
```

---

### 4.4 UseCase selettivi

I UseCase si usano solo quando esiste logica di business reale.

Servono per regole come:

- aggiungere o aggregare una confezione;
- decrementare una confezione;
- calcolare riepilogo Home;
- trovare alimenti in scadenza;
- confrontare ingredienti ricetta con dispensa;
- salvare una ricetta preferita;
- pulire la cache prodotti.

Non si creano UseCase per operazioni banali.

Esempi di UseCase utili:

```text
AddOrUpdatePackageUseCase
DecrementPackageUseCase
GetHomeOverviewUseCase
GetExpiringPackagesUseCase
MatchRecipeIngredientsUseCase
SaveFavoriteRecipeUseCase
CleanProductCacheUseCase
```

Esempi di UseCase da evitare:

```text
UpdateUsernameUseCase
ToggleThemeUseCase
ChangeLanguageUseCase
```

Queste operazioni possono stare direttamente in `SettingsRepository` e nel relativo ViewModel.

---

### 4.5 Repository

Il Repository è l'unico punto di accesso ai dati.

La UI e i ViewModel non devono conoscere direttamente:

- DAO Room;
- servizi API;
- DataStore;
- WorkManager;
- implementazioni concrete di cache.

Repository previsti:

```text
PantryRepository
ProductRepository
RecipeRepository
SettingsRepository
NotificationRepository
```

Responsabilità:

#### PantryRepository

Gestisce:

- alimenti;
- confezioni;
- aggregazioni;
- query dispensa;
- scadenze;
- conteggi.

#### ProductRepository

Gestisce:

- ricerca barcode su Open Food Facts;
- cache prodotto;
- fallback offline;
- pulizia cache.

#### RecipeRepository

Gestisce:

- ricerca ricette via Spoonacular/Edamam;
- salvataggio preferite in Room;
- lettura preferite offline.

#### SettingsRepository

Gestisce:

- nome utente;
- tema;
- lingua;
- notifiche abilitate;
- soglie notifiche.

#### NotificationRepository

Gestisce:

- scheduling notifiche;
- cancellazione notifiche;
- aggiornamento pianificazioni quando cambiano soglie/impostazioni.

---

## 5. Organizzazione package consigliata

Struttura base:

```text
com.example.pantrypal
│
├── MainActivity.kt
├── PantryPalApp.kt
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

---

## 6. Regole per i package

### core

Contiene elementi tecnici condivisi.

Esempi:

- database Room;
- client HTTP;
- DataStore;
- navigation custom;
- notifiche;
- componenti grafici comuni;
- utility generiche.

Non deve contenere logica specifica di una schermata.

---

### data

Contiene implementazioni concrete dei repository e sorgenti dati.

Esempi:

```text
data/pantry/PantryRepositoryImpl.kt
data/product/OpenFoodFactsApi.kt
data/recipe/SpoonacularApi.kt
data/settings/SettingsRepositoryImpl.kt
```

Qui possono vivere:

- DTO API;
- Entity Room;
- DAO;
- mapper necessari;
- repository implementation.

---

### domain

Contiene la logica di business condivisa.

Deve rimanere leggero.

Contiene:

- modelli domain se utili;
- UseCase importanti;
- enum condivisi.

Non deve diventare una copia artificiale di tutto il database.

---

### feature

Contiene le schermate utente.

Ogni feature contiene:

```text
Screen.kt
ViewModel.kt
UiState.kt
Event/Effect.kt
componenti locali
```

Esempio:

```text
feature/pantry
├── PantryScreen.kt
├── PantryViewModel.kt
├── PantryUiState.kt
├── PantryEvent.kt
├── PantryEffect.kt
└── components
```

---

### di

Contiene moduli Hilt.

Esempi:

```text
DatabaseModule.kt
NetworkModule.kt
RepositoryModule.kt
DataStoreModule.kt
NotificationModule.kt
```

---

## 7. Navigazione

L'app usa Navigation Compose, ma con una gestione logica custom della back queue.

Serve un componente dedicato:

```text
AppNavigator
```

Responsabilità:

- sapere qual è la pagina corrente;
- distinguere pagine principali e sotto-pagine;
- resettare la pila quando si entra in una pagina principale;
- impilare le sotto-pagine;
- gestire back custom;
- gestire eccezione add flow;
- gestire reset verso Home dopo salvataggio.

Regola:

```text
Le feature non manipolano direttamente la back queue.
```

Le feature emettono eventi di navigazione, il navigator applica le regole.

Esempio:

```kotlin
sealed interface AppRoute {
    data object Home : AppRoute
    data object Pantry : AppRoute
    data object Recipes : AppRoute
    data object Profile : AppRoute
    data object Scan : AppRoute
    data object ManualInsert : AppRoute
    data class FoodDetail(val foodId: Long) : AppRoute
    data class RecipeDetail(val recipeId: String) : AppRoute
}
```

---

## 8. Persistenza Room

Room è la sorgente di verità per:

- alimenti;
- confezioni;
- cache prodotti;
- ricette preferite.

Entità principali già previste:

```text
Alimento
Confezione
ProdottoCache
RicettaPreferita
```

Regola importante:

```text
Alimento senza confezioni attive resta nel database ma viene nascosto dalla Dispensa.
```

Motivi:

- autocomplete;
- reinserimento veloce;
- mapping barcode;
- memoria locale dell'utente.

Regola di aggregazione confezioni:

```text
stesso alimentoId + stessa dataScadenza → incrementa quantità
```

Non creare righe duplicate per la stessa scadenza.

---

## 9. API e strategia offline

L'app è local-first.

Questo significa:

- la dispensa funziona offline;
- le ricette preferite funzionano offline;
- l'autocomplete alimenti funziona offline;
- la cache barcode funziona offline se il prodotto è già noto;
- le chiamate API sono un arricchimento, non il cuore dell'app.

### Barcode

Prima scelta:

```text
Open Food Facts
```

Flusso:

```text
Scan barcode
↓
controlla cache locale
↓
se cache hit → precompila form
↓
se cache miss e online → chiama Open Food Facts
↓
se successo → precompila form e aggiorna cache
↓
se offline senza cache → mostra modal
```

### Ricette

Prima scelta:

```text
Spoonacular
```

Fallback:

```text
Edamam
```

In locale si salvano solo le ricette preferite.

---

## 10. Impostazioni

Le impostazioni usano DataStore Preferences, non Room.

Campi:

```text
username
language
theme
notificationsEnabled
freshFoodNotificationDays
longLifeFoodNotificationDays
```

Regola UI:

```text
Se notificationsEnabled = false, i campi soglia notifiche sono nascosti.
```

---

## 11. Notifiche

Le notifiche devono essere reali.

La progettazione dettagliata sarà definita in un file dedicato.

Scelta architetturale già fissata:

```text
WorkManager + NotificationManager
```

Il sistema notifiche dovrà dipendere da:

- dati Room;
- impostazioni DataStore;
- soglie diverse per fresco e lunga conservazione.

---

## 12. Design system

Creare un design system minimo in:

```text
core/designsystem
```

Contenuti:

- colori;
- typography;
- spacing;
- shape;
- componenti comuni.

Componenti comuni possibili:

```text
PantryCard
PantryTopBar
PantryBottomBar
Stepper
FoodChip
EmptyState
LoadingState
ErrorState
```

Non creare componenti generici troppo presto. Estrarre un componente solo quando è usato almeno in due punti o quando migliora chiaramente la leggibilità.

---

## 13. Milestone principali

### Milestone 1 — Setup progetto

Obiettivo: progetto Android compilabile con base tecnica pronta.

Include:

- progetto Kotlin + Compose;
- minSdk 28;
- Material 3;
- Hilt configurato;
- Room configurato;
- DataStore configurato;
- struttura package iniziale;
- tema base PantryPal.

Output atteso:

```text
L'app si avvia e mostra una schermata Home placeholder.
```

---

### Milestone 2 — Navigazione base

Obiettivo: implementare scheletro navigazione.

Include:

- bottom navigation;
- FAB centrale;
- schermate placeholder:
  - Home;
  - Dispensa;
  - Scan;
  - Inserimento manuale;
  - Dettaglio alimento;
  - Ricette;
  - Dettaglio ricetta;
  - Profilo;
- AppNavigator;
- back queue custom;
- eccezione add flow;
- reset verso Home dopo salvataggio.

Output atteso:

```text
Si può navigare in tutta l'app con le regole corrette, anche senza dati reali.
```

---

### Milestone 3 — Room e modello dispensa

Obiettivo: rendere funzionante il cuore locale dell'app.

Include:

- entità Alimento;
- entità Confezione;
- entità ProdottoCache;
- DAO;
- query aggregate;
- PantryRepository;
- AddOrUpdatePackageUseCase;
- DecrementPackageUseCase;
- GetHomeOverviewUseCase;
- GetExpiringPackagesUseCase.

Output atteso:

```text
Si possono aggiungere alimenti manualmente, vederli in Dispensa e Home, incrementare/decrementare quantità.
```

---

### Milestone 4 — UI Dispensa e Home reali

Obiettivo: collegare dati reali alla UI principale.

Include:

- Home con riepiloghi reali;
- conteggi per confezioni;
- card in scadenza;
- Dispensa con filtri luogo;
- sezione in scadenza sempre presente;
- stepper `− / ×N / +`;
- bottom sheet solo per decremento ambiguo;
- Dettaglio alimento.

Output atteso:

```text
La gestione dispensa è usabile end-to-end senza scan e senza API ricette.
```

---

### Milestone 5 — Scan barcode e Open Food Facts

Obiettivo: aggiungere inserimento tramite barcode.

Include:

- camera permission;
- scan barcode;
- integrazione Open Food Facts;
- cache prodotto;
- fallback offline;
- precompilazione form;
- modal offline senza cache.

Output atteso:

```text
Si può scansionare un prodotto e aggiungerlo alla dispensa con dati precompilati quando disponibili.
```

---

### Milestone 6 — Profilo e impostazioni

Obiettivo: rendere persistenti le preferenze utente.

Include:

- nome utente;
- lingua;
- tema;
- toggle notifiche;
- soglie separate fresco/lunga conservazione;
- auto-save;
- campi soglia nascosti se notifiche disabilitate.

Output atteso:

```text
Le impostazioni si salvano e influenzano Home/Dispensa, soprattutto le scadenze.
```

---

### Milestone 7 — Ricette

Obiettivo: implementare lista e dettaglio ricette.

Include:

- integrazione Spoonacular;
- predisposizione fallback Edamam;
- lista ricette;
- dettaglio ricetta;
- matching ingredienti/dispensa;
- like ricetta;
- salvataggio preferite in Room;
- preferite offline;
- share sheet contestuale.

Output atteso:

```text
L'utente può vedere ricette, aprirle, salvarle tra i preferiti e condividere cosa ha/cosa manca.
```

---

### Milestone 8 — Notifiche reali

Obiettivo: implementare notifiche locali pre-scadenza.

Include:

- WorkManager;
- NotificationManager;
- canale notifiche;
- rispetto impostazioni utente;
- soglie fresco/lunga conservazione;
- tap notifica → Dispensa.

Output atteso:

```text
L'app invia notifiche reali per prodotti in scadenza secondo le impostazioni utente.
```

---

### Milestone 9 — Rifinitura, test e consegna

Obiettivo: stabilizzare l'app.

Include:

- gestione errori;
- loading state;
- empty state;
- validazione form;
- test use case principali;
- test repository critici;
- pulizia UI;
- dati demo se utili;
- controllo permessi;
- controllo comportamento offline;
- README finale.

Output atteso:

```text
App pronta per demo/consegna.
```

---

## 14. Regole anti over-engineering

Non creare astrazioni solo “perché pulite”.

Evitare:

- interfacce repository se non servono nei test o per cambiare implementazione;
- mapper duplicati inutili;
- UseCase per ogni singolo click;
- moduli Gradle separati;
- domain model duplicato se identico alle entity;
- componenti UI generici creati troppo presto;
- navigazione distribuita dentro ogni schermata.

Preferire:

- codice chiaro;
- nomi espliciti;
- funzioni piccole;
- use case solo sulle regole importanti;
- repository come confine dati;
- UI stateless;
- ViewModel leggibili;
- milestone funzionanti una alla volta.

---

## 15. Regola finale

Ogni nuova feature deve rispettare questa catena:

```text
UI → ViewModel → UseCase se serve → Repository → Data source
```

Se una feature richiede di saltare uno strato, va motivato.

L'obiettivo non è fare l'architettura più teoricamente pura, ma costruire PantryPal in modo ordinato, modificabile e consegnabile.
