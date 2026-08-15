<p align="center">
  <img src="branding/logo.png" width="140" alt="CallHub logo">
</p>

<h1 align="center">CallHub</h1>

<p align="center">Dialer Android con registro chiamate unificato — chiamate native + VoIP (WhatsApp, WhatsApp Business, Telegram e client di terze parti).</p>

---

## Cos'è

Un **dialer completo** per Android (Kotlin/Compose) che raccoglie in **un unico
registro** sia le chiamate **cellulari native** sia gli eventi delle chiamate
**VoIP** di WhatsApp, WhatsApp Business, Telegram e client Telegram di terze parti.

> **Solo metadati.** Registra *chi / quando / quale app / durata (stimata) / persa
> o ricevuta*. **Non** registra l'audio: su Android moderno non è possibile per
> un'app normale, né per le chiamate native né per quelle VoIP.

## Funzioni

- ☎️ **Dialer completo**: tastierino, chiamate in uscita, **schermata di chiamata
  in corso** (rispondi/rifiuta, muto, vivavoce, attesa, riaggancia), notifica di
  chiamata a schermo intero.
- 🧭 **Registro unificato**: timeline unica di chiamate native (dati completi) +
  VoIP (rilevate dalle notifiche), con app di origine, dedup e tap-to-call.
- 👤 **Rubrica** con **foto del contatto**, ricerca e **preferiti** (speed dial).
- ⌨️ **Ricerca T9** sul tastierino (digiti → trova contatti per nome/numero).
- 💬 **Rifiuta con messaggio** (SMS di risposta rapida) sulle chiamate in arrivo.
- 📶 **Dual-SIM**: scelta della SIM per la chiamata.
- 🛡️ **Anti-spam** a costo zero: `CallScreeningService` con blocklist locale +
  **blocco di sistema** (`BlockedNumberContract`), regole (anonimi, non-in-rubrica)
  e provider di reputazione *pluggable*.
- 📞 **SIP** (VoIP): account SIP integrato in Android Telecom (stesse schermate del
  dialer). Engine-agnostico — vedi note sotto.
- 🌍 **Multilingua**: Italiano, Inglese, Tedesco (per-app language).
- 🔄 **Auto-update** (solo sideload) via GitHub Releases.

## Due versioni (Gradle product flavors)

| | `full` (sideload) | `play` (Play Store) |
|---|---|---|
| applicationId | `it.iotatec.callhub.full` | `it.iotatec.callhub` |
| App VoIP monitorate | tutte, di default | opt-in per app + disclosure |
| Auto-update | ✅ da GitHub Releases | ❌ (aggiorna lo Store) |
| Permessi extra | `INTERNET`, `REQUEST_INSTALL_PACKAGES` | nessuno |

Le funzioni che le policy del Play Store limitano stanno **solo** nel flavor `full`.

```bash
./gradlew assembleFullDebug     # APK sideload (auto-update)
./gradlew assemblePlayDebug     # APK per lo Store
```

## Versioning

`MAJOR.MINOR.PATCH` — **PATCH** = bugfix, **MINOR** = nuove funzioni, **MAJOR** =
cambiamenti radicali. `versionCode = MAJOR*10000 + MINOR*100 + PATCH`.
Versione attuale: **1.0.0**.

## Perché due "mondi" di chiamate

| | Native (GSM/VoLTE) | VoIP (WhatsApp/Telegram/…) |
|---|---|---|
| API ufficiale | Sì (`CallLog`, `InCallService`, `TelephonyCallback`) | **Nessuna** |
| Come le vediamo | Lettura diretta del `CallLog` | **Solo** le *notifiche* dell'app |
| Numero | Di solito disponibile | Raramente (solo nome) |
| Durata | Esatta | Stimata |

La rilevazione VoIP usa un `NotificationListenerService` (permesso speciale
"Accesso alle notifiche").

## App VoIP supportate

WhatsApp `com.whatsapp` · WhatsApp Business `com.whatsapp.w4b` ·
Telegram `org.telegram.messenger` · Telegram X `org.thunderdog.challegram` ·
Nekogram `tw.nekomimi.nekogram` · Plus Messenger `org.telegram.plus`.
Aggiungerne uno = una voce in `CallSource.kt`.

## Architettura

```
ui/       Compose: dialer, in-call, recenti, contatti, impostazioni, nav a tab
dialer/   InCallService, CallScreeningService, CallManager, sync CallLog, anti-spam
voip/     NotificationListenerService + parser euristico delle notifiche
sip/      account SIP, ConnectionService Telecom, interfaccia SipEngine (+ stub)
data/     modello, DB Room (call_events), repository, contatti
update/   auto-updater (full = GitHub, play = no-op)
```

## SIP

Lo stack SIP di sistema (`android.net.sip`) è **rimosso da Android 12**. CallHub
integra Telecom (managed `ConnectionService`) ed espone un'interfaccia `SipEngine`:
per chiamate reali va innestato uno stack — **Linphone SDK** (GPLv3/commerciale) o
**PJSIP** (GPL/commerciale). L'engine attuale è uno stub.

## Roadmap (spunti da dialer open-source)

Fatti: ricerca T9, preferiti, rifiuta-con-SMS, blocco via `BlockedNumberContract`,
dual-SIM, **tastierino DTMF in chiamata**, **conferenza/merge**, **raggruppamento
cronologia**, **risposte rapide personalizzabili**.

Prossimi (spunti da **LineageOS/AOSP Dialer**, Apache-2.0): registrazione chiamate
(dove legale), temi/personalizzazione, backup/ripristino, importazione blocklist
community.

## Note legali

- **GDPR (IT/UE)**: per uso personale sul proprio dispositivo ok; monitorare
  chiamate/notifiche altrui ha implicazioni legali (GDPR + art. 617 c.p.).
- **Play Store**: dialer role + accesso notifiche richiedono core-functionality,
  disclosure e privacy policy; il flavor `full` (sideload) evita questi vincoli.

## Licenza

**GPLv3** (vedi [`LICENSE`](LICENSE)). Il progetto integra il **Linphone SDK**
(liblinphone, GPLv3): di conseguenza l'intera app — entrambi i flavor — è GPLv3.

## Sviluppo

Richiede JDK 17 e Android SDK (compileSdk 35). Apri in Android Studio, oppure imposta
`local.properties` con `sdk.dir=…` e usa `./gradlew`.
