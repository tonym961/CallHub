# APK di prova — CallHub 1.0.0

Build **debug** pronte da installare per test.

| File | Flavor | Note |
|---|---|---|
| `CallHub-1.0.0-full-debug.apk` | sideload (`full`) | Tutte le funzioni, auto-update GitHub. **Consigliata per provare.** |
| `CallHub-1.0.0-play-debug.apk` | store (`play`) | Versione conservativa (VoIP opt-in, no auto-update). |

## Installazione (sideload)

1. Copia l'APK sul telefono Android.
2. Aprilo e consenti "Installa app sconosciute" se richiesto.
3. All'avvio: imposta CallHub come telefono predefinito e concedi l'accesso alle notifiche.

> Gli APK sono in `.gitignore` (non finiscono nel repo). Per l'**auto-update** del
> flavor `full`, carica l'APK come asset di una *GitHub Release* con tag `v1.0.0`.

> Nota: sono build **debug** (non firmate per release). Per la pubblicazione serve
> una build `release` firmata con un keystore.
