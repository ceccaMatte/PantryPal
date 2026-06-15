# PantryPal — Specifica funzionale

## 1. Obiettivo dell’app

PantryPal è un’app Android nativa per gestire la dispensa domestica, controllare le scadenze degli alimenti e suggerire ricette in base agli ingredienti disponibili.

L’app è local-first: i dati principali sono salvati in locale tramite Room. Non sono previsti login, onboarding o backend proprietario. L’app si apre direttamente sulla Home.

## 2. Stile grafico generale

L’interfaccia segue uno stile mobile-first, pulito e morbido:

* palette principale verde/crema;
* card arrotondate;
* layout chiaro e spazioso;
* bottom navigation persistente;
* FAB centrale per aggiungere alimenti;
* schermate secondarie con freccia back;
* immagini prodotto/ricetta gestite come placeholder o immagini recuperate da API.

## 3. Navigazione principale

La bottom navigation contiene quattro pagine principali:

* Home
* Dispensa
* Ricette
* Profilo

Al centro è presente un FAB `+`, separato dalle pagine principali, che apre la schermata Scan da qualunque pagina.

Le sotto-pagine sono:

* Scan
* Inserimento manuale
* Dettaglio alimento
* Dettaglio ricetta

## 4. Regole della back queue

La navigazione usa una pila logica.

Entrando in una pagina principale, la coda si resetta e contiene solo quella pagina.

Esempio:

```text
[home] → tap Ricette → [ricette]
[ricette] → tap Dispensa → [dispensa]
```

Entrando in una sotto-pagina, questa viene impilata sopra la pagina corrente.

Esempio:

```text
[ricette] → tap ricetta → [ricette, dettaglioRicetta]
back → [ricette]
```

Il salvataggio di un alimento o di una ricetta è un caso speciale: svuota la coda e porta alla Home.

```text
[dispensa, scan, manuale] → Salva Alimento → [home]
```

## 5. Eccezione catena di aggiunta alimento

Da qualunque pagina principale, premendo `+`, si entra nella schermata Scan.

Esempio:

```text
[dispensa] → + → [dispensa, scan]
```

Da Scan si può entrare in Inserimento manuale.

```text
[dispensa, scan] → Inserisci manualmente → [dispensa, scan, manuale]
```

Il back da Inserimento manuale deve tornare direttamente alla pagina d’origine, saltando Scan.

```text
[dispensa, scan, manuale] → back → [dispensa]
```

Lo stesso vale dopo uno scan riuscito: il form di inserimento è considerato parte del flusso di aggiunta, quindi il back dal form torna all’origine e non alla camera.

## 6. Home

La Home mostra:

* saluto utente;
* card “in scadenza”;
* riepilogo totale dispensa;
* conteggi per Frigo, Freezer e Dispensa;
* ricette suggerite.

Tutti i conteggi rappresentano confezioni, non categorie di alimento.

La card “in scadenza” mostra le confezioni in scadenza entro la soglia impostata nel Profilo. I tag mostrano i nomi dei prodotti deduplicati.

Azioni:

* tap sulla card “in scadenza” → Dispensa con sezione in scadenza visibile;
* tap su totale dispensa → Dispensa;
* tap su Frigo/Freezer/Dispensa → Dispensa filtrata per luogo;
* tap “Vedi” su una ricetta → Dettaglio ricetta.

## 7. Dispensa

La Dispensa mostra gli alimenti raggruppati.

I filtri disponibili sono solo:

* Tutti
* Frigo
* Freezer
* Dispensa

“In scadenza” non è un filtro. La sezione alimenti in scadenza è sempre presente in alto e rispetta il filtro luogo selezionato.

Ogni riga della lista rappresenta un alimento, non una singola confezione.

Ogni riga mostra:

* nome alimento;
* luogo;
* scadenza più vicina in evidenza;
* totale confezioni;
* stepper unico `− / ×N / +`.

Non esiste il bottone “Finito”.

### Decremento dalla riga Dispensa

Se l’alimento ha una sola scadenza, anche con quantità maggiore di 1, il tasto `−` decrementa direttamente di 1.

Esempio:

```text
Latte — 3 confezioni — stessa scadenza
tap − → Latte — 2 confezioni
```

Se l’alimento ha 2 o più scadenze diverse, il tasto `−` apre un bottom sheet di disambiguazione.

Il bottom sheet mostra le diverse scadenze e permette di indicare quante confezioni sono state consumate per ciascuna.

### Incremento dalla riga Dispensa

Il tasto `+` porta al Dettaglio alimento, dove l’utente può scegliere se aggiungere una confezione con una nuova scadenza o incrementare una scadenza esistente.

## 8. Dettaglio alimento

Il Dettaglio alimento mostra un singolo alimento e le sue confezioni raggruppate per data di scadenza.

Ogni riga-scadenza mostra:

* data di scadenza;
* quantità;
* stepper `− / ×N / +`.

Nel Dettaglio alimento il decremento e l’incremento agiscono sempre direttamente sulla singola scadenza, senza bottom sheet.

Quando una confezione arriva a quantità 0, sparisce dalla lista.

Quando un alimento non ha più confezioni attive, sparisce dalla Dispensa ma resta nel database.

L’entità alimento resta salvata per:

* velocizzare inserimenti futuri;
* supportare autocomplete;
* mantenere mapping barcode → alimento/categoria;
* conservare dati utili per scan e reinserimento.

## 9. Aggiunta alimento

Il FAB `+` apre la schermata Scan da qualunque pagina.

La schermata Scan permette:

* scansione barcode;
* inserimento manuale.

Il riconoscimento barcode usa Open Food Facts.

Regole:

* scan riuscito online → apertura form con nome precompilato;
* se il barcode è presente in cache → nome, luogo e deperibilità possono essere precompilati;
* offline + barcode presente in cache → apertura form con dati disponibili;
* offline + barcode non presente in cache → modal con scelta “Attiva connessione” / “Inserisci manualmente”.

La data di scadenza è sempre obbligatoria.

## 10. Inserimento manuale

Il form di inserimento manuale permette di inserire:

* nome alimento;
* presenza in dispensa;
* luogo: Frigo, Freezer, Dispensa;
* deperibilità: Fresco, Lunga conservazione;
* data di scadenza;
* numero confezioni.

Quando l’utente scrive il nome alimento, l’app mostra suggerimenti da alimenti già presenti nel database, anche se attualmente nascosti perché senza confezioni attive.

Il salvataggio alimento:

* crea o riusa l’alimento;
* crea o aggiorna la confezione;
* se esiste già una confezione con stesso alimento e stessa data, incrementa la quantità;
* aggiorna eventuale cache prodotto;
* svuota la back queue;
* torna alla Home.

## 11. Ricette

Le ricette vengono recuperate da API esterna.

Prima scelta:

* Spoonacular

Fallback se il piano gratuito non è sufficiente:

* Edamam

In locale vengono salvate solo le ricette preferite.

La lista Ricette mostra:

* risultati;
* preferiti;
* card ricetta;
* stato ingredienti presenti/mancanti;
* pulsante like.

Il like salva la ricetta in locale senza conferma.

## 12. Dettaglio ricetta

Il Dettaglio ricetta mostra:

* titolo;
* tempo di preparazione;
* porzioni;
* immagine o placeholder;
* ingredienti;
* ingredienti presenti in dispensa;
* ingredienti da comprare;
* pulsante condivisione.

Il confronto ingredienti/dispensa avviene tramite matching testuale normalizzato, da raffinare in fase implementativa.

La condivisione usa lo share sheet di sistema.

La lista condivisa è contestuale alla ricetta e contiene cosa è già disponibile e cosa manca.

Non esiste una lista spesa globale.

## 13. Profilo

Il Profilo contiene:

* nome utente;
* lingua;
* tema;
* notifiche pre-scadenza;
* soglia notifiche per alimenti freschi;
* soglia notifiche per lunga conservazione.

Tema, lingua e toggle notifiche sono in auto-save.

Il nome utente viene salvato all’uscita dal campo.

Se le notifiche sono disabilitate, i campi relativi alle soglie di scadenza sono nascosti.

Se le notifiche sono abilitate, l’utente può configurare soglie separate:

* notifica entro X giorni per alimenti freschi;
* notifica entro X giorni per alimenti a lunga conservazione.

La logica delle notifiche verrà approfondita in una specifica dedicata.

## 14. Notifiche

Le notifiche devono essere reali, non simulate.

La progettazione dettagliata delle notifiche verrà definita in una fase successiva.

La direzione prevista è l’uso di notifiche locali Android con scheduling affidabile.

## 15. Regole sui conteggi

Tutti i conteggi dell’app contano confezioni.

Esempi:

* totale “84 articoli”;
* contatori Frigo/Freezer/Dispensa;
* card “in scadenza”;
* riepiloghi nella Dispensa.

Gli alimenti/categorie non vengono contati come unità principali.

## 16. Regole di permanenza dati

Un alimento senza confezioni attive non viene mostrato nella Dispensa, ma non viene eliminato dal database.

Le confezioni con quantità 0 vengono rimosse o considerate non attive a livello applicativo.

L’alimento resta disponibile per:

* autocomplete;
* reinserimento rapido;
* mapping barcode;
* dati storici utili alla cache locale.

## 17. Ambiti fuori scope per ora

Per questa prima specifica restano fuori scope:

* architettura tecnica dettagliata;
* package structure;
* ViewModel/state management;
* schema Room definitivo;
* API client dettagliati;
* strategia notifiche completa;
* gestione errori avanzata;
* test;
* dependency injection;
* implementazione effettiva della navigation stack custom.

Questi aspetti verranno definiti nei file successivi.
