# SecureRCS-Messenger

Privacy-first messenger that keeps all processing local, bridges Matrix/RCS and other services, and always warns users about the risks of each network.

## Was wird bereitgestellt?

- **Lokaler Dienst**: Keine externen Aufrufe – Nachrichten, Protokolle und Warnungen verbleiben lokal.
- **Matrix- und RCS-Basis**: Dedizierte Connectoren plus generische Third-Party-Connectoren, um weitere Dienste lokal einzubinden.
- **Risikohinweise**: Vor der Nutzung eines Dienstes muss der Nutzer die hinterlegten Risiken bestätigen (z. B. fehlende E2EE bei SMS/RCS, Metadatenabfluss bei proprietären Netzen).
- **Bridging**: Nachrichten können von einem Dienst an andere weitergereicht werden (z. B. Matrix → RCS/WhatsApp), solange der Nutzer die Risiken bestätigt hat.

## Nutzung (Demo)

1. Voraussetzungen: Python 3 (keine externen Abhängigkeiten).
2. Ausführen der Demo:

```bash
python messenger.py
```

Die Demo:

- zeigt alle bekannten Risiken,
- erzwingt eine Bestätigung bevor eine riskante Verbindung (z. B. WhatsApp) genutzt wird,
- sendet eine Matrix-Nachricht über die eingerichteten Bridges und protokolliert alles lokal.

Die komplette Historie (Inbox/Outbox/Audit) wird am Ende der Demo ausgegeben und verbleibt ausschließlich auf dem lokalen System.
