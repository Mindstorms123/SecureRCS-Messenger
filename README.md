# SecureRCS-Messenger

Privacy-first messenger that keeps all processing local, bridges Matrix/RCS and other services, and always warns users about the risks of each network.

## What is provided?

- **Lokaler Dienst**: Keine externen Aufrufe – Nachrichten, Protokolle und Warnungen verbleiben lokal.
- **Matrix- und RCS-Basis**: Dedizierte Connectoren plus generische Third-Party-Connectoren, um weitere Dienste lokal einzubinden (E-Mail, Discord, Slack, Telegram, WhatsApp – alles lokal simuliert).
- **Risikohinweise**: Vor der Nutzung eines Dienstes muss der Nutzer die hinterlegten Risiken bestätigen (z. B. fehlende E2EE bei SMS/RCS, Metadatenabfluss bei proprietären Netzen).
- **Bridging**: Nachrichten können von einem Dienst an andere weitergereicht werden (z. B. Matrix → RCS/WhatsApp), solange der Nutzer die Risiken bestätigt hat.

## Usage (Demo)

1. Voraussetzungen: Python 3 (keine externen Abhängigkeiten).
2. Ausführen der Demo:

```bash
python messenger.py
```

Die Demo:

- zeigt alle bekannten Risiken,
- erzwingt eine Bestätigung bevor eine riskante Verbindung (z. B. WhatsApp) genutzt wird,
- sendet eine Matrix-Nachricht über die eingerichteten Bridges (RCS, E-Mail, Discord, Slack, Telegram, WhatsApp) und protokolliert alles lokal.

Die komplette Historie (Inbox/Outbox/Audit) wird am Ende der Demo ausgegeben und verbleibt ausschließlich auf dem lokalen System.

## Android-App (Android Studio)

Zusätzlich zur Python-Demo liegt nun ein komplettes Android-Studio-Projekt bei (`app`‑Modul, Jetpack Compose UI). Die App arbeitet rein lokal, zeigt alle Risiken an, erzwingt deren Bestätigung und erlaubt das Senden/Bridgen von Nachrichten zwischen Matrix, RCS und WhatsApp (lokale Simulation ohne Netzwerkzugriff).

### Öffnen und Bauen

1. Projektordner in Android Studio öffnen.
2. Android SDK 34 installieren/auswählen.
3. Sync ausführen und danach `app` auf ein Gerät oder Emulator deployen (`Run > Run 'app'`).

### Hinweise

- Mindest-SDK: 24, Ziel-SDK: 34.
- Compose-Material3 Oberfläche, keine externen Netzwerk- oder Backend-Abhängigkeiten.
- Lokal gespeicherte Historie (Inbox/Outbox) und Audit-Log sind nur pro App-Sitzung im Speicher vorhanden und verlassen das Gerät nicht.
