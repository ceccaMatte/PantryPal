# PantryPal — Decisioni applicative e use case

> Documento incrementale.  
> Scopo: fissare le decisioni già prese su comportamenti, persistenza e logica applicativa prima di tradurle nel contratto definitivo Repository/UseCase.

---

## 1. Principi generali

### 1.1 Persistenza esplicita

Le modifiche vengono salvate nel database solo quando il flow prevede chiaramente un’azione di conferma.

Esempi con conferma:

```text
Aggiunta Manuale → Salva
Dettaglio Alimento → Salva modifiche
```

Esempi senza conferma, quindi auto-save immediato:

```text
Like ricetta
Modifica impostazioni Profilo
Toggle tema
Cambio lingua
Cambio soglie notifiche
```

---

### 1.2 Stato UI temporaneo

Alcune modifiche servono solo alla UI e non devono arrivare al database.

Esempio principale:

```text
Dettaglio Ricetta:
spostare un ingrediente tra “In dispensa” e “Da comprare”
```

Questa modifica è solo temporanea, vale solo per la pagina corrente e per lo share intent.

---

### 1.3 Regola sui mapping

I mapping reali sono persistenti.

```text
barcode → alimento interno
ingrediente ricetta → alimento interno
```

Quando l’utente crea o corregge un mapping, questo viene salvato e riutilizzato in futuro.

---

## 2. Aggiunta alimento

### 2.1 Categoria obbligatoria

Nel form di Aggiunta Manuale il campo di ricerca non identifica da solo l’alimento.

```text
Campo ricerca = filtro/suggerimento
Badge selezionato = categoria interna effettiva
```

L’utente deve sempre selezionare un badge.

Se prova a salvare senza categoria selezionata:

```text
errore: seleziona un alimento
```

---

### 2.2 Creazione nuova categoria

L’utente può creare una nuova categoria interna.

Esempio:

```text
scrive: Latte intero
vede suggerimento: [Latte]
sceglie: + Crea “Latte intero”
risultato UI: [Latte intero ✓]
```

La nuova categoria non viene scritta subito nel DB.

Viene creata davvero solo quando l’utente preme:

```text
Salva
```

---

### 2.3 Data e quantità

La data di scadenza è sempre obbligatoria.

La quantità:

```text
default = 1
modificabile con stepper
```

---

### 2.4 Nessun prefill dalla pagina di origine

Se l’utente apre Aggiunta Manuale tramite FAB `+`, la pagina di origine non precompila la categoria.

Esempi:

```text
Home → + → Aggiunta Manuale
Dispensa → + → Aggiunta Manuale
Dettaglio Alimento → + → Aggiunta Manuale
```

In tutti i casi il form parte senza categoria selezionata, salvo dati provenienti da scan barcode.

---

### 2.5 Salvataggio alimento

Il salvataggio avviene solo al click su `Salva`.

Operazioni da eseguire:

```text
1. validare categoria selezionata
2. validare data scadenza
3. validare quantità
4. se categoria nuova → creare FoodCategory
5. creare o incrementare ExpiryLot
6. se il flow deriva da barcode riconosciuto → salvare BarcodeProductLink
7. tornare alla schermata di origine
```

Use case previsto:

```text
SaveAddedFoodUseCase
```

---

## 3. Scan barcode

### 3.1 Barcode già noto localmente

Se il barcode è già associato a una categoria interna:

```text
barcode → FoodCategory
```

allora Aggiunta Manuale viene aperta con:

```text
campo ricerca precompilato con il nome della categoria
badge della categoria già selezionato
```

Obiettivo: il barcode evita all’utente di cercare manualmente l’alimento.

---

### 3.2 Barcode riconosciuto da Open Food Facts

Se Open Food Facts riconosce il prodotto, ma non esiste ancora un’associazione locale:

```text
mostra ProductRecognized sheet
```

Poi, entrando in Aggiunta Manuale:

```text
campo ricerca precompilato con il nome prodotto da Open Food Facts
nessun badge selezionato, se non è stato trovato un match affidabile
```

L’utente deve selezionare una categoria esistente o crearne una nuova.

Al `Salva`, se i dati prodotto sono completi, viene creato il link:

```text
barcode → FoodCategory
```

---

### 3.3 Barcode riconosciuto ma non associabile

Se il prodotto viene riconosciuto da Open Food Facts, ma non è riconosciuto come alimento tracciato da PantryPal:

```text
mostra messaggio:
“Non riconosciuto come alimento della tua dispensa”
```

Poi:

```text
Inserisci manualmente
→ campo ricerca precompilato con nome prodotto API
→ nessun badge selezionato
```

Anche in questo caso, al salvataggio, il barcode link viene salvato solo se abbiamo dati prodotto completi e una categoria selezionata.

---

### 3.4 Barcode non riconosciuto

Se il barcode non viene riconosciuto:

```text
mostra warning:
“Barcode non riconosciuto. Aggiungilo manualmente alla tua dispensa.”
```

Poi:

```text
Aggiunta Manuale vuota
```

In questo caso non viene salvato alcun `BarcodeProductLink`, nemmeno se l’utente salva manualmente l’alimento.

Regola:

```text
niente link barcode parziali
o abbiamo dati prodotto completi
o trattiamo il flow come semplice inserimento manuale
```

Use case previsto:

```text
ResolveBarcodeUseCase
```

---

## 4. Dettaglio alimento e lotti

### 4.1 Lotti e scadenze

La dispensa è aggregata per:

```text
FoodCategory + expirationDate = ExpiryLot
```

Se viene aggiunta una quantità con stessa categoria e stessa scadenza:

```text
incrementa il lotto esistente
```

Non vengono creati duplicati.

---

### 4.2 Azioni da Dispensa

Regole da lista Dispensa:

```text
− con una sola scadenza → decremento diretto
− con più scadenze → vai a Dettaglio Alimento
+ → vai sempre a Dettaglio Alimento
```

Motivo: se la scadenza è ambigua, l’utente deve scegliere nel dettaglio.

---

### 4.3 Dettaglio Alimento come draft locale

Nel Dettaglio Alimento tutte le modifiche sono locali finché l’utente non preme:

```text
Salva modifiche
```

Modifiche locali:

```text
+ / − sui lotti
aggiunta nuova scadenza
modifica data scadenza
cambio luogo
cambio deperibilità
```

Il database viene aggiornato solo al salvataggio finale.

Se l’utente preme back senza salvare:

```text
le modifiche vengono perse
nessun modal di conferma
nessuna scrittura nel DB
```

---

### 4.4 Quantità a zero

Se un lotto arriva a quantità 0:

```text
viene rimosso dalla lista delle occorrenze attive
```

Se tutti i lotti arrivano a 0:

```text
la pagina Dettaglio Alimento resta visibile
la lista scadenze diventa vuota
```

Solo al salvataggio:

```text
i lotti a 0 vengono rimossi dal DB
l’alimento sparisce dalla lista Dispensa
FoodCategory resta nel database
```

---

### 4.5 Aggiungere nuova scadenza

Per aggiungere una nuova scadenza si usa un modal dedicato, coerente con l’inserimento alimento.

Il modal contiene:

```text
data scadenza obbligatoria
quantità default = 1
stepper quantità
Salva scadenza
```

Il `Salva scadenza` del modal conferma la riga nella UI del Dettaglio Alimento, ma non scrive ancora nel DB.

---

### 4.6 Modifica data scadenza

È possibile modificare la data di un lotto.

Regole:

```text
se la nuova data non esiste → aggiorna il lotto
se la nuova data esiste già → fonde le quantità
```

Esempio:

```text
20/07 ×2
21/07 ×1
```

modifico `20/07` in `21/07`:

```text
21/07 ×3
```

Use case previsto:

```text
SaveFoodDetailChangesUseCase
```

---

## 5. Ricette

### 5.1 Ricetta come contenuto

Una ricetta è un contenuto composto da:

```text
titolo
descrizione
immagine
tempo
porzioni
ingredienti con quantità
```

I dati della ricetta vengono salvati in Room solo se l’utente mette like.

---

### 5.2 Ricetta non preferita

Una ricetta non preferita:

```text
viene caricata da API
resta in RAM
non viene persistita in Room
si perde alla chiusura dell’app
```

Durante la sessione può esistere una cache RAM temporanea, ma non persistente.

Offline non è possibile aprire dati di ricette non preferite.

---

### 5.3 Ricetta preferita

Quando l’utente mette like:

```text
salva FavoriteRecipe
salva RecipeIngredient della ricetta
```

Quando l’utente toglie like:

```text
elimina FavoriteRecipe
elimina RecipeIngredient collegati
```

I mapping globali ingrediente → alimento interno non vengono eliminati.

Use case previsto:

```text
ToggleFavoriteRecipeUseCase
```

---

### 5.4 Ricetta preferita offline

Offline l’utente può vedere solo le ricette preferite salvate localmente.

Quando apre una ricetta preferita offline:

```text
legge dati ricetta da Room
ricalcola “In dispensa” / “Da comprare” usando la dispensa attuale
```

---

## 6. Mapping ingrediente ricetta → alimento interno

### 6.1 Mapping globale

Il mapping tra label ingrediente e alimento interno non appartiene alla singola ricetta.

È globale e persistente.

Esempi:

```text
pasta → Pasta
olio d’oliva → Olio
fried chicken → Pollo fritto
pollo → Petto di pollo
pollo → Cosce di pollo
```

Questo mapping è salvato in:

```text
RecipeIngredientLinkEntity
```

---

### 6.2 Ricetta salvata e mapping sono separati

La ricetta può non essere preferita e quindi non essere salvata.

Il mapping creato da quella ricetta invece viene salvato comunque.

Esempio:

```text
apro ricetta non preferita
collego “pasta” → Pasta
non metto like alla ricetta
chiudo app
```

Risultato:

```text
la ricetta sparisce
il mapping “pasta” → Pasta resta
```

---

### 6.3 Molti-a-molti

Lo stesso alias ingrediente può puntare a più categorie interne.

Esempio:

```text
pollo → Petto di pollo
pollo → Cosce di pollo
pollo → Pollo campese
```

L’ingrediente è considerato “In dispensa” se almeno una categoria collegata ha un lotto attivo.

---

### 6.4 Correzione mapping

Se l’utente corregge un mapping sbagliato:

```text
olio d’oliva → Salmone
```

e sceglie:

```text
olio d’oliva → Olio
```

il link sbagliato viene eliminato dal DB.

Non viene mantenuto con `isActive = false`.

Regola:

```text
mapping errato corretto dall’utente → delete
mapping corretto → insert/update
```

Use case previsto:

```text
LinkRecipeIngredientToFoodUseCase
```

---

## 7. Availability ricetta

### 7.1 Calcolo runtime

La divisione degli ingredienti tra:

```text
In dispensa
Da comprare
```

viene calcolata a runtime.

Flusso:

```text
RecipeIngredient originale
→ normalizedName
→ RecipeIngredientLink attivi
→ FoodCategory compatibili
→ ExpiryLot attivi
→ In dispensa / Da comprare
```

Non esiste più una tabella persistente `RecipeIngredientMatchEntity`.

Use case previsto:

```text
GetRecipeAvailabilityUseCase
```

---

### 7.2 Spostamenti temporanei nella UI

Nel Dettaglio Ricetta l’utente può spostare ingredienti tra:

```text
In dispensa
Da comprare
```

Questo spostamento:

```text
è solo visuale
non modifica il database
non modifica la dispensa
non modifica i mapping
non è persistente
```

Serve solo a sistemare la lista prima della condivisione.

Se l’utente esce e riapre la ricetta, la disponibilità viene ricalcolata dai dati reali.

---

### 7.3 Share lista spesa

Lo share intent usa lo stato visuale corrente del Dettaglio Ricetta.

Quindi considera anche gli spostamenti temporanei fatti dall’utente.

---

## 8. Home suggested recipes

### 8.1 Origine suggerimenti

La Home suggerisce ricette partendo dagli alimenti presenti in dispensa.

Regola semplice:

```text
prendi tutti i FoodCategory con almeno un ExpiryLot attivo
estrai i nomi
manda tutti i nomi all’API ricette
mostra i risultati restituiti dall’API
```

Non vengono introdotti filtri sofisticati.

Non esiste un flag `isStaple`.

Ingredienti come sale, olio o pepe vengono mandati all’API come tutti gli altri.

---

### 8.2 Stati

Stati previsti:

```text
online + dispensa con alimenti → ricerca API
online + dispensa vuota → stato vuoto
offline → stato vuoto/offline
API errore → stato vuoto/errore leggero
```

Offline non vengono mostrati suggerimenti dai preferiti locali.

---

### 8.3 Persistenza

Le ricette suggerite:

```text
non vengono salvate in Room
restano in RAM
vengono salvate solo se l’utente mette like
```

Use case previsto:

```text
GetHomeSuggestedRecipesUseCase
```

---

## 9. Home overview

### 9.1 Dati mostrati in Home

La Home mostra un riepilogo locale della dispensa:

```text
totale confezioni
conteggio confezioni Frigo
conteggio confezioni Freezer
conteggio confezioni Dispensa
alimenti in scadenza
ricette suggerite
```

Le ricette suggerite sono un blocco separato e opzionale, perché dipendono da rete/API.

---

### 9.2 Totale confezioni

Il totale confezioni è calcolato come:

```text
SUM(ExpiryLot.quantity)
```

Conta le confezioni, non le categorie alimentari.

Esempio:

```text
Latte ×2
Pasta ×1
Uova ×6
```

Totale:

```text
9 confezioni
```

---

### 9.3 Conteggi per luogo

I conteggi Frigo / Freezer / Dispensa sono calcolati come:

```text
somma delle quantity dei lotti
raggruppata per defaultStorageLocation della FoodCategory
```

Esempio:

```text
Latte FRIDGE ×2
Uova FRIDGE ×6
Pasta PANTRY ×1
```

Risultato:

```text
Frigo 8
Dispensa 1
Freezer 0
```

---

### 9.4 Alimenti in scadenza

La Home usa le stesse soglie salvate nelle impostazioni:

```text
FRESH → freshNotificationDays
LONG_LIFE → longLifeNotificationDays
```

Se le soglie non sono state impostate, si usano i default:

```text
FRESH → 2 giorni
LONG_LIFE → 7 giorni
```

Il toggle notifiche non influenza il calcolo degli alimenti in scadenza.

Anche con notifiche disabilitate, Home e Dispensa devono continuare a mostrare gli alimenti in scadenza.

---

### 9.5 Deduplica e moltiplicatore

Se più lotti dello stesso alimento sono in scadenza, la Home mostra una sola chip con moltiplicatore.

Esempio:

```text
Latte 20/07 ×1
Latte 21/07 ×2
```

Home:

```text
[Latte ×3]
```

Il moltiplicatore indica il totale delle confezioni in scadenza per quell’alimento.

La stessa regola vale per la sezione “In scadenza” della Dispensa:

```text
una card per alimento
moltiplicatore = quantità totale in scadenza
```

---

### 9.6 Ordinamento

Gli alimenti in scadenza sono ordinati per scadenza più vicina.

Esempio:

```text
Yogurt scade domani
Latte scade tra 2 giorni
Uova scadono tra 5 giorni
```

Ordine:

```text
Yogurt
Latte
Uova
```

---

### 9.7 Separazione da ricette suggerite

La Home usa due use case separati:

```text
GetHomeOverviewUseCase
GetHomeSuggestedRecipesUseCase
```

Motivo:

```text
la overview dispensa deve funzionare sempre da DB locale
le ricette suggerite dipendono da rete/API
```

Se le API ricette falliscono, la Home mostra comunque:

```text
totale confezioni
conteggi per luogo
alimenti in scadenza
```

e la sezione ricette mostra uno stato vuoto/offline/errore leggero.

Use case previsto:

```text
GetHomeOverviewUseCase
```

---

## 10. Profilo e Settings

### 10.1 Storage

Le impostazioni utente vengono salvate in DataStore, non in Room.

Campi previsti:

```text
username
language
theme
expirationNotificationsEnabled
freshNotificationDays
longLifeNotificationDays
pantryStorageFilter
```

Il filtro Dispensa persistente viene salvato nelle impostazioni.

---

### 10.2 Profilo senza bottone Salva

Il Profilo non ha un bottone di salvataggio.

Le modifiche sono auto-save:

```text
nome utente → salva on blur / fine editing
lingua → salva subito
tema → salva subito
toggle notifiche → salva subito
stepper giorni → salva subito
```

Non serve un use case dedicato per aggiornare tema, lingua, nome o soglie.

Per queste operazioni basta:

```text
ProfileViewModel → SettingsRepository
```

---

### 10.3 Nome utente

Se il nome utente non è definito o è vuoto, la Home mostra:

```text
Ciao!
```

Se è definito:

```text
Ciao, Matteo!
```

---

### 10.4 Soglie “in scadenza”

Le soglie servono sia per:

```text
notifiche reali
calcolo alimenti in scadenza in Home/Dispensa
```

Il toggle notifiche decide solo se inviare notifiche reali.

Non decide se un alimento è considerato “in scadenza”.

Default:

```text
FRESH → 2 giorni
LONG_LIFE → 7 giorni
```

Limiti stepper:

```text
freshNotificationDays: 1–7
longLifeNotificationDays: 1–30
```

Se notifiche OFF ma le soglie esistono, si usano le soglie salvate.

Se notifiche OFF e l’utente non le ha mai impostate, si usano i default.

---

### 10.5 Lingua

Il campo `language` viene salvato come preferenza futura.

Per MVP:

```text
lingua di default = italiano
UI effettiva = italiano
nessuna localizzazione reale multi-lingua
```

---

## 11. Notifiche reali e WorkManager

### 11.1 Scelta tecnica

Le notifiche di scadenza vengono gestite con:

```text
WorkManager periodico
NotificationManager
```

Non servono alarm esatti.

La notifica non deve arrivare al minuto preciso: è sufficiente un controllo giornaliero.

---

### 11.2 Worker giornaliero

Se le notifiche sono abilitate:

```text
WorkManager periodico
→ una volta al giorno
→ circa alle 09:00
→ secondo il fuso orario impostato sul telefono
```

Il controllo giornaliero invia una notifica solo se esistono alimenti in scadenza.

---

### 11.3 Tipo di notifica

Per MVP si usa una sola notifica riepilogativa.

Esempio:

```text
Hai 4 alimenti in scadenza
Latte ×2, Yogurt ×1, Uova ×1
```

Tap sulla notifica:

```text
Dispensa
Filtro = Tutti
Sezione “In scadenza” visibile
```

Non vengono inviate notifiche separate per ogni alimento.

---

### 11.4 Frequenza notifiche

Se ci sono alimenti in scadenza, la notifica può essere mostrata ogni giorno.

Se non ci sono alimenti in scadenza:

```text
nessuna notifica
```

Non esiste filtro anti-spam giornaliero.

Non viene salvato:

```text
lastExpirationNotificationDate
```

---

### 11.5 Toggle notifiche ON

Quando l’utente passa da OFF a ON:

```text
1. richiedi permesso notifiche se necessario
2. se concesso:
   - salva expirationNotificationsEnabled = true
   - schedula worker giornaliero
   - esegui subito un controllo notifiche
3. se negato:
   - toggle torna OFF
   - non schedulare worker
```

Il controllo immediato serve anche per testare le notifiche senza aspettare le 09:00.

Se l’utente spegne e riaccende le notifiche più volte, ogni passaggio OFF → ON deve eseguire un nuovo controllo immediato e notificare se necessario.

---

### 11.6 Toggle notifiche OFF

Quando l’utente disabilita le notifiche:

```text
expirationNotificationsEnabled = false
cancella worker periodico
non inviare notifiche
mantieni salvate le soglie
```

Home e Dispensa continuano comunque a usare le soglie per mostrare “In scadenza”.

---

### 11.7 Use case notifiche

Use case previsto:

```text
UpdateNotificationSettingsUseCase
```

Responsabilità:

```text
gestire toggle ON/OFF
richiedere/controllare permesso notifiche
schedulare/cancellare worker
lanciare controllo immediato quando OFF → ON
```

Use case previsto:

```text
RunExpirationNotificationCheckUseCase
```

Responsabilità:

```text
leggere settings e soglie
leggere alimenti in scadenza
costruire notifica riepilogo
inviare notifica se la lista non è vuota
non fare nulla se la lista è vuota
```

---

## 12. Use case emersi finora

Lista provvisoria aggiornata:

```text
SaveAddedFoodUseCase
ResolveBarcodeUseCase
SaveFoodDetailChangesUseCase

GetHomeOverviewUseCase
GetHomeSuggestedRecipesUseCase

ToggleFavoriteRecipeUseCase
GetRecipeAvailabilityUseCase
LinkRecipeIngredientToFoodUseCase

UpdateNotificationSettingsUseCase
RunExpirationNotificationCheckUseCase
```

Possibili use case ancora da valutare:

```text
FindOrCreateFoodCategoryUseCase
BuildShoppingListShareText
```

Note:

```text
FindOrCreateFoodCategoryUseCase può essere interno a SaveAddedFoodUseCase.
BuildShoppingListShareText può stare nel RecipeDetailViewModel se resta solo formattazione UI.
```

---

## 13. Punti ancora da definire

Da completare nei prossimi passaggi:

```text
Repository contract definitivo
Use case input/output
API integration
Matching runtime
UiState delle schermate
Implementation plan
```
