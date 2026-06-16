# PantryPal — Schema di navigazione

> Versione 1.1 — aggiornata con gestione aggiunta alimento, mapping alimenti e modifiche UX post data model

---

## 1. Struttura primaria

### Bottom Navigation

| Posizione | Label | Destinazione |
|---|---|---|
| 1 | Home | Schermata Home |
| 2 | Dispensa | Schermata Dispensa |
| 3 | **＋ FAB** | Apre `AddChoice` sheet |
| 4 | Ricette | Schermata Ricette |
| 5 | Profilo | Schermata Profilo |

La bottom navigation è visibile nelle schermate principali e nelle schermate secondarie standard:

- Home;
- Dispensa;
- Dettaglio Alimento;
- Gestisci collegamenti alimento;
- Ricette;
- Dettaglio Ricetta;
- Profilo.

La bottom navigation viene nascosta solo durante il flow di aggiunta alimento.

---

## 2. Regola speciale: flow di aggiunta alimento

Il flow di aggiunta alimento inizia quando l’utente preme il FAB `＋`.

Da quel momento la bottom navigation viene nascosta fino alla fine del flow.

### Schermate/sheet del flow aggiunta

La bottom nav è nascosta in:

- `AddChoice` sheet;
- Scan Barcode;
- `ProductRecognized` sheet;
- Aggiunta Manuale.

### Fine del flow

Il flow termina quando l’utente:

- salva l’alimento;
- annulla;
- usa back per tornare indietro.

Alla fine del flow si torna sempre alla schermata da cui il flow era partito.

Esempi:

```text
Home → ＋ → Aggiunta Manuale → Salva → Home
Dispensa → ＋ → Aggiunta Manuale → Salva → Dispensa
Dettaglio Alimento → ＋ → Aggiunta Manuale → Salva → Dettaglio Alimento
Ricette → ＋ → Aggiunta Manuale → Salva → Ricette
Dettaglio Ricetta → ＋ → Aggiunta Manuale → Salva → Dettaglio Ricetta
```

Questa regola sostituisce la vecchia regola “Salva alimento → Home”.

---

## 3. Regole generali di navigazione

### Pagine principali

Le pagine principali sono:

- Home;
- Dispensa;
- Ricette;
- Profilo.

Quando l’utente seleziona una pagina principale dalla bottom nav, lo stack logico viene resettato sulla pagina selezionata.

Esempio:

```text
Dispensa → Dettaglio Alimento → tap Ricette = [Ricette]
```

### Sotto-pagine

Le sotto-pagine vengono impilate sopra la schermata di origine.

Esempio:

```text
Dispensa → Dettaglio Alimento → Gestisci collegamenti alimento
```

Back da una sotto-pagina torna alla schermata precedente.

### Filtri Dispensa

I filtri della Dispensa sono filtri di luogo, non categorie alimentari.

Filtri disponibili:

- Tutti;
- Frigo;
- Freezer;
- Dispensa.

I filtri sono persistenti: sopravvivono alla navigazione tra tab e alle sessioni.

### Associazioni ingrediente ↔ alimento

Un’associazione creata da ricetta o da scan viene sempre ricordata per il futuro.

Non esiste la distinzione:

```text
Solo questa ricetta / Ricorda per sempre
```

Ogni associazione manuale è persistente finché non viene corretta o rimossa.

---

## 4. Schermate e flussi

## 4.1 Home

**Accesso:** bottom nav.

| Azione | Destinazione / Comportamento |
|---|---|
| Tap chip alimento in scadenza, es. `Latte` | Dispensa con filtro luogo `Tutti`, focus/evidenziazione su `Latte` nella sezione “In scadenza” |
| Tap stat Frigo | Dispensa con filtro `Frigo` |
| Tap stat Freezer | Dispensa con filtro `Freezer` |
| Tap stat Dispensa | Dispensa con filtro `Dispensa` |
| Tap “Vedi” su ricetta suggerita | Dettaglio Ricetta |
| FAB `＋` | `AddChoice` sheet, bottom nav nascosta |

### Nota focus chip in scadenza

Il focus della chip alimento deve avvenire nella sezione degli alimenti in scadenza, non nella lista globale della dispensa.

Esempio:

```text
Home → chip Latte → Dispensa/Tutti → evidenzia Latte nella rail/lista “In scadenza”
```

---

## 4.2 Dispensa

**Accesso:** bottom nav · Home stat tap · Home chip in scadenza · notifica riepilogo scadenze.

| Azione | Destinazione / Comportamento |
|---|---|
| Chip filtro luogo | Aggiorna lista inline e persiste il filtro |
| Tap card alimento | Dettaglio Alimento |
| Tap card rail “In scadenza” | Dettaglio Alimento |
| Stepper `−` con una sola scadenza | Decrementa inline, nessuna navigazione |
| Stepper `−` con più scadenze diverse | Dettaglio Alimento |
| Stepper `＋` | Dettaglio Alimento |
| FAB `＋` | `AddChoice` sheet, bottom nav nascosta |

### Regola decremento

Non esiste più un bottom sheet “quale scadenza?” dalla lista Dispensa.

Se una categoria ha più scadenze, la gestione avviene nel Dettaglio Alimento.

---

## 4.3 Dettaglio Alimento

**Accesso:** Dispensa card · Dispensa rail “In scadenza” · notifica alimento specifico.

| Azione | Destinazione / Comportamento |
|---|---|
| `← back` | Schermata precedente |
| Selettore deperibilità | Inline |
| Selettore luogo | Inline |
| Stepper `＋` su riga scadenza | Incrementa lotto inline |
| Stepper `−` su riga scadenza | Decrementa lotto inline |
| Quantità lotto arriva a `0` | Rimuove il lotto dagli stock attivi |
| `＋ Aggiungi scadenza` | Nuova riga scadenza inline |
| “Riconoscimento automatico” → Gestisci | Gestisci collegamenti alimento |
| Salva modifiche | Torna alla schermata precedente |
| FAB `＋` | `AddChoice` sheet, bottom nav nascosta |

### Regole rimosse

Il toggle “Presente in dispensa” viene rimosso.

La presenza deriva dai lotti:

```text
almeno un ExpiryLot con quantity > 0 → presente in dispensa
nessun ExpiryLot attivo → non presente
```

La `X` sulle righe scadenza viene rimossa.

Si usa solo lo stepper:

```text
− / ×N / +
```

Se il lotto arriva a quantità 0, viene rimosso dagli stock attivi. La categoria alimentare resta comunque nel database.

---

## 4.4 Gestisci collegamenti alimento

**Accesso:** Dettaglio Alimento → “Riconoscimento automatico” → Gestisci.

Questa pagina serve a gestire i collegamenti avanzati di una categoria interna.

Esempi:

- prodotti scansionati riconosciuti come questo alimento;
- nomi ingrediente ricetta riconosciuti come questo alimento.

| Azione | Destinazione / Comportamento |
|---|---|
| `← back` | Dettaglio Alimento |
| Rimuovi su prodotto scansionato | Disattiva/rimuove collegamento inline |
| Rimuovi su nome ricetta | Disattiva/rimuove collegamento inline |
| Sposta collegamento prodotto | Modal selezione alimento interno |
| Sposta nome ricetta | Modal selezione alimento interno |
| Campo “Aggiungi un nome…” + invio | Aggiunge nome ricetta collegato inline |
| `＋ Aggiungi barcode manualmente` | Aggiunge barcode collegato inline o via modal dedicato |

La pagina è una sezione avanzata, non una pagina principale.

---

## 4.5 Scan Barcode

**Accesso:** FAB `＋` → `AddChoice` → “Scansiona barcode”.

Durante Scan Barcode la bottom nav è nascosta.

| Azione | Destinazione / Comportamento |
|---|---|
| Barcode riconosciuto | `ProductRecognized` sheet |
| “Inserisci manualmente” fallback | Aggiunta Manuale |
| `← back` | Torna alla schermata da cui è stato premuto il FAB |

---

## 4.6 Aggiunta Manuale

**Accesso:**

| # | Percorso |
|---|---|
| 1 | FAB da qualsiasi schermata → `AddChoice` → “Inserisci manualmente” |
| 2 | Scan → “Inserisci manualmente” fallback |
| 3 | Scan → `ProductRecognized` → “Aggiungi o modifica” |

Durante Aggiunta Manuale la bottom nav è nascosta.

| Azione | Destinazione / Comportamento |
|---|---|
| Tap badge alimento suggerito | Precompila categoria inline |
| Badge “Crea nuovo” | Crea nuovo alimento interno inline |
| Salva | Torna alla schermata da cui è partito il flow di aggiunta |
| Annulla | Torna alla schermata da cui è partito il flow di aggiunta |
| `← back` | Torna alla schermata da cui è partito il flow di aggiunta, saltando eventuale Scan |

### Regola back nel flow scan/manuale

Se l’utente arriva ad Aggiunta Manuale passando da Scan, il back non deve riportare alla camera.

Esempio:

```text
Dispensa → ＋ → AddChoice → Scan → Inserisci manualmente → back → Dispensa
```

---

## 4.7 Ricette

**Accesso:** bottom nav.

| Azione | Destinazione / Comportamento |
|---|---|
| Tab “Risultati” / “Preferiti” | Inline, nessuna navigazione |
| `♥` su card ricetta | Toggle preferito inline |
| Tap card ricetta | Dettaglio Ricetta |
| FAB `＋` | `AddChoice` sheet, bottom nav nascosta |

---

## 4.8 Dettaglio Ricetta

**Accesso:** Ricette tap card · Home tap “Vedi” ricetta suggerita.

| Azione | Destinazione / Comportamento |
|---|---|
| `← back` | Schermata precedente |
| `♥` | Toggle preferito inline |
| Tap ingrediente “In dispensa” | Espansione inline con azioni temporanee e collegamento |
| Tap ingrediente “Da comprare” | Espansione inline con azioni temporanee |
| “Sposta in Da comprare” | Sposta ingrediente solo nella vista corrente |
| “Ce l’ho — segna in dispensa” | Sposta ingrediente solo nella vista corrente |
| “Cambia alimento collegato” | Modal dedicato “Collega ingrediente a un alimento” |
| “Condividi Lista Spesa” | Android share intent con lista basata sulla vista corrente |
| FAB `＋` | `AddChoice` sheet, bottom nav nascosta |

### Regola spostamento ingredienti

Spostare ingredienti tra:

```text
In dispensa
Da comprare
```

è solo una modifica temporanea della pagina.

Non modifica:

- dispensa reale;
- lotti/scadenze;
- categorie interne;
- mapping ingrediente ↔ alimento;
- database.

La modifica serve solo per sistemare la lista prima della condivisione.

Se l’utente esce e riapre il dettaglio ricetta, la vista viene ricostruita dal matching reale.

### Regola cambio alimento collegato

“Cambia alimento collegato” modifica davvero il mapping:

```text
nome ingrediente ricetta → FoodCategory interna
```

Questa azione apre un modal dedicato, non Aggiunta Manuale.

L’associazione viene sempre ricordata per il futuro.

---

## 4.9 Profilo

**Accesso:** bottom nav.

| Azione | Destinazione / Comportamento |
|---|---|
| Nome utente → modifica | Inline, nessuna navigazione |
| Lingua | Selezione inline, applica immediatamente |
| Tema | Toggle pill inline, applica immediatamente |
| Notifiche toggle ON/OFF | Inline; se ON espande i controlli giorni |
| Stepper giorni “Alimenti freschi” | Inline |
| Stepper giorni “Lunga conservazione” | Inline |
| FAB `＋` | `AddChoice` sheet, bottom nav nascosta |

---

## 5. Bottom sheet e modali

| Sheet / Modal | Trigger | Azioni principali |
|---|---|---|
| `AddChoice` | FAB `＋` | → Scan Barcode · → Aggiunta Manuale · Chiudi |
| `ProductRecognized` | Scan barcode riconosciuto | → Aggiunta Manuale · Cambia alimento riconosciuto · Chiudi → Scan |
| `IngredientLinked` | Dettaglio Ricetta → ingrediente | Mostra alimento collegato · Cambia alimento collegato · Rimuovi collegamento · Chiudi |
| `SelectFoodCategory` | Cambio associazione ingrediente/barcode | Cerca alimento · badge suggeriti · crea nuovo alimento · conferma |

### Sheet rimossi

I modali seguenti non fanno più parte del flusso:

- `MarkFinished`;
- `AddPackage`;
- bottom sheet “quale scadenza?” da Dispensa;
- modal “Solo questa ricetta / Ricorda per sempre”.

Incremento e decremento con ambiguità navigano o avvengono nel Dettaglio Alimento.

Le associazioni vengono sempre ricordate per il futuro.

---

## 6. Deep link da notifiche

Il comportamento dipende dal tipo di notifica, non dalla schermata corrente.

| Tipo notifica | Destinazione |
|---|---|
| Riepilogo scadenze | Dispensa con filtro luogo `Tutti` |
| Alimento specifico in scadenza | Dettaglio Alimento con `categoryId` |

---

## 7. Azioni di sistema Android

| Azione | Comportamento |
|---|---|
| Share intent “Condividi Lista Spesa” | Apre chooser Android con lista ingredienti in testo semplice |
| Back gesture / tasto back | Torna alla schermata precedente nello stack; dalla tab root nessuna azione |
| Notifica locale pre-scadenza | Deep link secondo tabella §6 |

---

## 8. Regole finali fissate

1. La bottom nav è nascosta solo nel flow di aggiunta alimento.
2. Salva/annulla/back da Aggiunta Manuale torna alla schermata di origine.
3. Il flow scan/manuale non permette navigazione tramite bottom nav.
4. Tap chip alimento in scadenza da Home porta a Dispensa/Tutti con focus nella sezione “In scadenza”.
5. In Dispensa, se il decremento è ambiguo, si naviga al Dettaglio Alimento.
6. Nel Dettaglio Alimento non esiste `X`, solo stepper `− / ×N / +`.
7. Il toggle “Presente in dispensa” è rimosso.
8. La presenza in dispensa deriva dai lotti attivi.
9. Nel Dettaglio Ricetta gli spostamenti tra “In dispensa” e “Da comprare” sono solo temporanei e visuali.
10. “Cambia alimento collegato” apre un modal dedicato e modifica il mapping persistente.
11. Le associazioni create da scan o ricetta sono sempre ricordate per il futuro.
12. Le notifiche usano deep link basati sul tipo di notifica.
