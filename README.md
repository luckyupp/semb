# Parkovaci dum nad Treapem (Java Swing)

Demonstracni aplikace pro predmet Datove struktury a algoritmy.

## Splneni zadani

- Vlastni implementace `Treap<K,V>` v `src/cz/andel/ds/`:
  - BST vlastnost podle klice
  - Heap vlastnost podle priority
  - rotace vlevo/vpravo pri vkladani a mazani
  - operace `insert`, `delete`, `find`, `contains`, `inOrder`, `predecessor`, `successor`
  - kontrola invariantu po kazde mutaci
- Aplikacni vrstva v `src/cz/andel/parking/service/`:
  - 1 podlazi = 1 instance Treapu
  - klic = cislo stani
  - data = informace o vozidle/uzivateli
- GUI v Java Swing (`ParkingGarageFrame`):
  - obsazeni mista
  - uvolneni mista
  - overeni obsazenosti
  - vypis obsazenych mist vzestupne
  - hledani nejblizsiho volneho mista
  - import CSV
  - spusteni povinnych scenaru 1-6
- Minimalni rozsah modelu:
  - 4 podlazi
  - 12 mist na podlazi
  - ukazkovy soubor `data/sample_occupancy.csv` obsahuje 22 obsazenych mist

## Spusteni (PowerShell)

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
java -cp out cz.andel.Main
```

## CSV format

Soubor musi mit radky:

```text
floor,spot,licensePlate,owner,vehicleType
```

Komentare jsou povoleny pomoci `#` na zacatku radku.

## Poznamka k scenari 6 (rotace)

V beznych operacich se priorita generuje nahodne.
Pro deterministickou ukazku rotaci v demonstracnim scenari je pouzit interni testovaci vstup s rucne zvolenymi prioritami.
