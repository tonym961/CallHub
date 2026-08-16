# Privacy Policy — CallHub

_Last updated: 2026-08-16_

CallHub is a phone dialer with a unified call log (native + messenger calls). This
policy explains what data the app accesses and how it is handled.

## Summary

- **All data stays on your device.** CallHub has **no backend server** and does **not**
  send your calls, contacts, messages, or any personal data to us or to third parties.
- The only network activity is:
  - **SIP calls** (if you configure a SIP account) — traffic goes directly to *your* SIP
    provider, not to us.
  - **App updates** (sideload build only) — the app checks a version file and downloads
    the APK from this project's GitHub repository.

## Data the app accesses (and why)

| Data / permission | Why | Leaves device? |
|---|---|---|
| Call log (`READ_CALL_LOG`) | Show native calls in the unified log | No |
| Phone state / numbers | Detect call state, place calls | No |
| Contacts (`READ_CONTACTS`) | Show names/photos, T9 search, favorites | No |
| Notification access | Detect WhatsApp/Telegram/etc. call **notifications** to log them | No |
| Microphone (`RECORD_AUDIO`) | SIP call audio (only during SIP calls) | Only to your SIP server |
| Internet | SIP signaling/media; app update check | To your SIP server / GitHub |
| Default dialer role | Handle and place calls | No |

CallHub reads only the **metadata** of messenger call notifications (who/when/which app).
It does **not** read message contents and cannot record call audio for native calls.

## Storage

- The unified call log, notes, favorites, blocklist and settings are stored **locally**
  (app database / preferences).
- The **SIP account password is stored encrypted** (Android Keystore-backed
  EncryptedSharedPreferences).
- **Backups** you export are plain files saved **where you choose**; keep them safe, as
  they contain your settings (including the SIP password).

## Sharing

CallHub does not sell or share your data. There are no analytics or ad SDKs.

## Contact

info@iotatec.it
