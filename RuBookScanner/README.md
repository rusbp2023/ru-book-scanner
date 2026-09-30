# Orosz Szókereső (könyv-szkenner)

Élő kamera-kép, közepén egy piros ponttal. Állítod a telefont úgy, hogy a
pötty egy orosz szón legyen egy könyvben/nyomtatott szövegben, megnyomod a
gombot, és az app (ML Kit cirill OCR, teljesen a telefonon, internet nélkül)
kiírja, mi az a szó. Onnan egy gombbal meg is oszthatod — ha az **Orosz
Szókártyák** app is telepítve van a telefonon, azonnal ki tudod választani a
megosztás-listából, és bekerül a szólistájába.

## Build ugyanúgy megy, mint a RuFlashcards-nál

1. Csomagold ki a kapott `RuBookScanner.zip`-et.
2. Hozz létre egy **új, üres** GitHub repót (pl. `ru-book-scanner`) — README,
   .gitignore, licenc nélkül.
3. A repó főoldalán: **Add file → Upload files**, húzd be az egész
   kicsomagolt `RuBookScanner` mappát (a mappa maga kerüljön be, ne csak a
   tartalma — pont úgy, mint korábban).
4. Mivel a `.github` mappa rejtett, ez most is lemarad a feltöltésből. Pótold
   kézzel: **Add file → Create new file**, fájlnév: `.github/workflows/build.yml`,
   és másold bele ezt a tartalmat:

```yaml
name: Build APK

on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

defaults:
  run:
    working-directory: RuBookScanner

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: 8.7

      - name: Build debug APK
        run: gradle assembleDebug

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-debug-apk
          path: RuBookScanner/app/build/outputs/apk/debug/app-debug.apk
```

5. Commit — ez elindítja a buildet. Nézd meg az **Actions** fülön.
6. Ha zöld pipa lesz, a futás alján az **Artifacts** résznél letöltheted az
   `app-debug-apk` csomagot (benne az `app-debug.apk`).
7. Telepítsd a telefonra, ugyanúgy, mint a másik appnál (Ismeretlen forrásból
   telepítés engedélyezése, ha kéri).

## Használat

1. Nyisd meg az appot, engedélyezd a kamera-hozzáférést, amikor kéri.
2. Célozd a telefont a könyv fölé úgy, hogy a piros pötty pontosan a
   kiválasztott szón legyen.
3. Nyomd meg az alsó 📷 gombot.
4. Pár másodperc múlva megjelenik a felismert szó egy kártyán, alatta
   **Megosztás** gombbal — ha megnyomod, a rendszer megosztás-menüjéből
   választhatod ki az Orosz Szókártyák appot (vagy bármi mást), és a szó
   átkerül oda.

## Ha pontatlan a felismerés

- Tartsd stabilan a telefont, kerüld az elmosódást.
- Jó, egyenletes megvilágítás sokat számít.
- Ha a szó túl kicsi a képen, menj közelebb — az OCR jobban működik nagyobb,
  élesebb betűkkel.
