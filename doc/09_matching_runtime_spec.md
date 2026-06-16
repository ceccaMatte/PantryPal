# PantryPal — Matching Runtime Spec

> Versione 1.0  
> Scopo: definire come PantryPal normalizza testi, suggerisce alimenti interni e decide i match runtime per barcode, aggiunta manuale e ingredienti ricetta.

---

## 1. Obiettivo

Il matching serve a rispondere a tre domande pratiche:

```text
1. Questo barcode corrisponde a quale alimento interno?
2. Questo testo digitato dall’utente corrisponde a quale alimento interno?
3. Questo ingrediente ricetta corrisponde a quale alimento interno?
```

Regola generale:

```text
il matching può suggerire,
ma non deve creare mapping persistenti senza conferma esplicita dell’utente
```

Eccezioni:

```text
BarcodeProductLink viene salvato solo al Salva dell’aggiunta alimento
RecipeIngredientLink viene salvato solo quando l’utente collega esplicitamente un ingrediente a un alimento
```

---

## 2. Concetti coinvolti

### 2.1 Alimento interno

L’alimento interno è rappresentato da:

```text
FoodCategoryEntity
```

Esempi:

```text
Latte
Pasta
Olio
Petto di pollo
Yogurt
```

È la categoria logica usata da PantryPal per aggregare lotti/scadenze.

---

### 2.2 Prodotto barcode

Il prodotto barcode è un prodotto reale riconosciuto da Open Food Facts o da un link locale.

Esempio:

```text
barcode: 8001234567890
productName: Latte Parmalat Intero
brand: Parmalat
```

Il barcode non è l’alimento interno.

Il barcode deve essere collegato a un alimento interno:

```text
Latte Parmalat Intero → Latte
```

---

### 2.3 Ingrediente ricetta

L’ingrediente ricetta è una label testuale proveniente da API ricette o da una ricetta salvata.

Esempi:

```text
latte
pasta
olio d’oliva
fried chicken
chicken breast
```

L’ingrediente ricetta non è automaticamente un alimento interno.

Per essere considerato disponibile in dispensa deve avere un mapping persistente:

```text
RecipeIngredientLinkEntity
```

---

## 3. Normalizzazione testo

### 3.1 Funzione unica

Usare una funzione unica per normalizzare nomi alimento, query, alias ricetta e dati barcode.

Nome consigliato:

```kotlin
fun normalizeFoodText(value: String): String
```

---

### 3.2 Regole

La normalizzazione deve essere semplice e prevedibile.

Passaggi:

```text
1. trim
2. lowercase
3. rimozione accenti
4. sostituzione apostrofi tipografici con spazio
5. rimozione punteggiatura leggera
6. normalizzazione spazi multipli
```

Esempi:

```text
"Olio d’Oliva"        → "olio d oliva"
"  Latte   Intero "   → "latte intero"
"Pasta al Pomodoro!"  → "pasta al pomodoro"
"Crème fraîche"       → "creme fraiche"
```

---

### 3.3 Cosa NON fare

Per MVP non usare:

```text
NLP pesante
lemmatizzazione
stemming avanzato
machine learning
sinonimi dinamici generati da AI
ranking semantico complesso
```

Motivo:

```text
l’app deve essere semplice, prevedibile e implementabile in poco tempo
```

---

## 4. Tipi di match

### 4.1 Match esatto su nome normalizzato

Esempio:

```text
query: "latte"
FoodCategory.normalizedName: "latte"
```

Risultato:

```text
match forte
```

---

### 4.2 Match esatto su alias

Gli alias possono arrivare da:

```text
RecipeIngredientLinkEntity.normalizedAlias
eventuali seed alias iniziali
```

Esempio:

```text
query: "milk"
alias: "milk"
FoodCategory: Latte
```

Risultato:

```text
match forte
```

---

### 4.3 Match parziale su nome

Esempio:

```text
query: "latte"
FoodCategory.normalizedName: "latte intero"
```

Risultato:

```text
match medio
```

---

### 4.4 Match parziale su alias

Esempio:

```text
query: "chicken"
alias: "chicken breast"
FoodCategory: Petto di pollo
```

Risultato:

```text
match medio
```

---

### 4.5 Similarità testuale semplice

Può essere usata come fallback per ordinare suggerimenti.

Esempi implementabili:

```text
startsWith
contains
numero di token comuni
distanza Levenshtein solo su stringhe brevi
```

Non deve diventare una logica centrale.

---

## 5. Ranking suggerimenti alimento

### 5.1 Ordine generale

Quando bisogna suggerire badge alimento, ordinare così:

```text
1. match esatto su FoodCategory.normalizedName
2. match esatto su alias
3. nome categoria che inizia con query
4. alias che inizia con query
5. nome categoria che contiene query
6. alias che contiene query
7. token comuni
8. categorie usate di recente
9. ordine alfabetico
```

---

### 5.2 Limite risultati

Per MVP mostrare pochi suggerimenti.

Valore consigliato:

```text
max 5–8 badge suggeriti
```

Motivo:

```text
evitare UI affollata
mantenere scelta semplice
```

---

### 5.3 Categoria nuova

Se la query non è vuota, mostrare anche l’opzione:

```text
+ Crea “<query originale>”
```

Regole:

```text
non mostrare se esiste già match esatto su normalizedName
non creare nulla finché l’utente non preme Salva
```

Esempio:

```text
query: "Latte intero"
nessuna categoria esatta
→ mostra + Crea “Latte intero”
```

---

## 6. Matching in Aggiunta Manuale

### 6.1 Campo ricerca

Il campo ricerca serve a:

```text
filtrare categorie esistenti
mostrare badge suggeriti
permettere creazione nuova categoria
```

Non identifica da solo l’alimento.

Regola:

```text
il badge selezionato è la categoria effettiva
```

---

### 6.2 Salvataggio

Per salvare un alimento serve sempre:

```text
categoria selezionata
data scadenza
quantità valida
```

Se l’utente scrive nel campo ma non seleziona badge:

```text
errore inline:
“Seleziona un alimento”
```

---

### 6.3 Nuova categoria

Se l’utente sceglie:

```text
+ Crea “Latte intero”
```

il ViewModel imposta:

```text
pendingNewCategory
selectedCategory = nuova categoria temporanea
```

La categoria viene scritta in Room solo al click su:

```text
Salva
```

---

## 7. Matching barcode

### 7.1 Priorità

Quando viene scansionato un barcode:

```text
1. cerca BarcodeProductLink locale attivo
2. se trovato, usa la categoria collegata
3. se non trovato, chiama Open Food Facts
4. se Open Food Facts trova prodotto, prova a suggerire categorie
5. se Open Food Facts non trova prodotto, passa ad aggiunta manuale vuota
```

---

### 7.2 Barcode locale noto

Se esiste:

```text
BarcodeProductLink.barcode → FoodCategory
```

risultato:

```text
campo ricerca = nome categoria
badge categoria = selezionato
```

Questo è un match certo perché deriva da mapping persistente.

---

### 7.3 Barcode riconosciuto da Open Food Facts

Dati da usare per generare suggerimenti:

```text
productName
genericName
brand
rawCategoryTags
rawFoodGroupTags
```

Priorità consigliata:

```text
1. genericName
2. productName
3. rawCategoryTags
4. rawFoodGroupTags
5. brand solo come informazione secondaria
```

Nota:

```text
brand non deve guidare troppo il matching,
perché “Parmalat” non significa automaticamente “Latte”
```

---

### 7.4 Preselezione da Open Food Facts

Se il prodotto esterno produce un match forte con una FoodCategory, è possibile preselezionare il badge.

Esempio:

```text
productName: "Latte intero"
FoodCategory: "Latte"
```

Risultato:

```text
campo ricerca = "Latte intero"
badge "Latte" preselezionato, se il match è giudicato forte
```

Se il match è debole:

```text
campo ricerca precompilato
badge suggeriti
nessun badge selezionato
```

---

### 7.5 Salvataggio BarcodeProductLink

Il link barcode viene salvato solo quando l’utente preme:

```text
Salva
```

Condizioni:

```text
flow deriva da barcode riconosciuto
dati prodotto completi disponibili
categoria selezionata
```

Non salvare link barcode se:

```text
barcode non riconosciuto
prodotto senza dati sufficienti
utente non seleziona categoria
```

---

## 8. Matching ingredienti ricetta

### 8.1 Regola conservativa

Per decidere se un ingrediente ricetta è:

```text
In dispensa
```

non basta un match testuale forte con `FoodCategory`.

Serve un mapping persistente:

```text
RecipeIngredientLinkEntity
```

Regola finale:

```text
senza RecipeIngredientLink persistente,
l’ingrediente non viene considerato disponibile in dispensa
```

---

### 8.2 Esempio

Dati:

```text
ingrediente ricetta: "latte"
FoodCategory esistente: "Latte"
dispensa: Latte ×1
RecipeIngredientLink: assente
```

Risultato:

```text
"latte" va in Da comprare / Non collegato
```

L’utente può aprire il bottom sheet:

```text
Collega ingrediente a un alimento
```

e creare il mapping:

```text
latte → Latte
```

Dopo il collegamento, l’ingrediente potrà risultare:

```text
In dispensa
```

se `Latte` ha almeno un lotto attivo.

---

### 8.3 Perché scelta conservativa

Motivo:

```text
evitare falsi positivi sulle ricette
rendere esplicito il mapping ingrediente → alimento interno
mantenere comportamento prevedibile
```

Esempi ambigui:

```text
pollo → Petto di pollo / Cosce di pollo / Pollo fritto
olio → Olio d’oliva / Olio di semi
pasta → Pasta secca / Pasta fresca
```

---

### 8.4 Suggerimenti consentiti

La scelta conservativa vale per la disponibilità runtime.

È comunque possibile usare matching testuale per suggerire categorie nel bottom sheet di collegamento.

Esempio:

```text
ingrediente: "latte"
sheet suggerisce: [Latte]
```

Ma il link viene salvato solo se l’utente conferma.

---

### 8.5 Availability ricetta

Flusso per ogni ingrediente:

```text
1. normalizza originalName
2. cerca RecipeIngredientLink attivi per normalizedAlias / externalIngredientId
3. se non ci sono link:
   - ingrediente = Da comprare / Non collegato
4. se ci sono link:
   - recupera FoodCategory collegate
   - controlla ExpiryLot attivi
5. se almeno una FoodCategory collegata ha lotto attivo:
   - ingrediente = In dispensa
6. altrimenti:
   - ingrediente = Da comprare
```

---

### 8.6 Mapping molti-a-molti

Lo stesso alias può puntare a più categorie.

Esempio:

```text
pollo → Petto di pollo
pollo → Cosce di pollo
pollo → Pollo campese
```

L’ingrediente è “In dispensa” se almeno una categoria collegata ha quantità attiva.

---

### 8.7 Correzione mapping errato

Se l’utente corregge:

```text
olio d’oliva → Salmone
```

in:

```text
olio d’oliva → Olio
```

regola:

```text
elimina il link errato
salva il link corretto con origin = USER
```

Non usare:

```text
isActive = false
```

per mapping ingrediente errati corretti manualmente.

---

## 9. Seed e alias iniziali

### 9.1 Seed FoodCategory

Il seed iniziale deve contenere categorie frequenti.

Esempi:

```text
Latte
Pasta
Riso
Uova
Yogurt
Formaggio
Pane
Petto di pollo
Carne macinata
Pomodoro
Insalata
Mele
Banane
Olio
Burro
```

Ogni categoria seed deve avere:

```text
name
normalizedName
defaultStorageLocation
defaultPerishability
origin = SEED
```

---

### 9.2 Seed alias ricetta

Gli alias ricetta iniziali servono a evitare troppi collegamenti manuali.

Esempi:

```text
milk → Latte
latte → Latte
pasta → Pasta
rice → Riso
riso → Riso
eggs → Uova
uova → Uova
olive oil → Olio
olio d oliva → Olio
chicken breast → Petto di pollo
petto di pollo → Petto di pollo
```

Questi alias possono essere salvati come:

```text
RecipeIngredientLinkEntity
origin = SEED
isActive = true
```

---

### 9.3 Effetto del seed sulla regola conservativa

La regola conservativa resta valida.

Un ingrediente ricetta viene considerato disponibile solo se esiste un link persistente.

Il seed fornisce link persistenti iniziali.

Esempio:

```text
seed: latte → Latte
ingrediente ricetta: latte
dispensa: Latte ×1
```

Risultato:

```text
latte = In dispensa
```

Perché il link persistente esiste già.

---

## 10. Runtime vs persistenza

### 10.1 Matching runtime

Il matching runtime può:

```text
ordinare suggerimenti
precompilare campi
preselezionare badge in casi forti
mostrare candidate categories
```

---

### 10.2 Persistenza

Il matching runtime non deve:

```text
creare FoodCategory automaticamente
creare BarcodeProductLink automaticamente
creare RecipeIngredientLink automaticamente
modificare ExpiryLot
```

La persistenza avviene solo tramite:

```text
SaveAddedFoodUseCase
LinkRecipeIngredientToFoodUseCase
ToggleFavoriteRecipeUseCase
SaveFoodDetailChangesUseCase
```

---

## 11. Dove vive la logica

### 11.1 Normalizzazione

La normalizzazione deve stare in un modulo condiviso.

Percorso consigliato:

```text
core/util/TextNormalizer.kt
```

---

### 11.2 Matching categorie

La logica di suggerimento categorie può stare in:

```text
domain/matching/FoodCategoryMatcher.kt
```

Responsabilità:

```text
dato query + categorie + alias,
restituisce suggerimenti ordinati
```

---

### 11.3 Matching barcode

La coordinazione barcode sta in:

```text
ResolveBarcodeUseCase
```

Usa:

```text
PantryRepository
FoodRecognitionRepository
FoodCategoryMatcher
```

---

### 11.4 Matching ingredienti ricetta

La disponibilità ricetta sta in:

```text
GetRecipeAvailabilityUseCase
```

Usa:

```text
RecipeRepository
PantryRepository
TextNormalizer
```

Regola:

```text
availability solo da RecipeIngredientLink persistenti
```

---

## 12. Stati UI collegati

### 12.1 Aggiunta Manuale

Il matching aggiorna:

```text
searchQuery
categorySuggestions
selectedCategory
pendingNewCategory
categoryError
```

---

### 12.2 Scan barcode

Il matching produce:

```text
KnownLocalCategory
RecognizedExternalProduct
NotRecognized
NetworkError
```

---

### 12.3 Dettaglio Ricetta

Il matching produce:

```text
inPantry
toBuy
matchedCategories
ingredienti non collegati
```

Gli ingredienti non collegati possono mostrare una CTA:

```text
Collega alimento
```

---

### 12.4 Bottom sheet collegamento ingrediente

Il matching testuale è usato per suggerire badge.

Esempio:

```text
ingrediente: "chicken breast"
suggerimenti: [Petto di pollo]
```

Il mapping diventa persistente solo dopo conferma dell’utente.

---

## 13. Regole finali sintetiche

```text
Aggiunta Manuale:
    testo digitato suggerisce, badge selezionato decide

Barcode locale:
    mapping persistente decide e preseleziona

Barcode Open Food Facts:
    può suggerire/preselezionare se match forte,
    ma salva link solo al Salva

Ingrediente ricetta:
    availability solo da RecipeIngredientLink persistente

Ingrediente ricetta senza link:
    Da comprare / Non collegato,
    anche se il nome combacia con una FoodCategory

Bottom sheet collegamento ingrediente:
    può usare match testuale per suggerire,
    ma non salva finché l’utente non conferma

Seed:
    può creare link persistenti iniziali,
    quindi abilita availability automatica per casi comuni
```
