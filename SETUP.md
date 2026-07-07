# PracticeDyad — Setup-Anleitung

## 1. Firebase-Projekt erstellen

1. Gehe zu https://console.firebase.google.com
2. Neues Projekt erstellen → Name: "PracticeDyad"
3. Folgende Dienste aktivieren:
   - **Authentication** → E-Mail/Passwort aktivieren
   - **Firestore Database** → Im Testmodus starten
   - **Storage** → Im Testmodus starten
   - **Cloud Messaging** (automatisch aktiv)

## 2. Android-App in Firebase registrieren

1. Im Firebase-Projekt: Zahnrad → Projekteinstellungen → App hinzufügen → Android
2. Package-Name: `com.practicedyad.app`
3. `google-services.json` herunterladen und in `app/` ersetzen

## 3. Firestore-Regeln (für Produktion anpassen)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## 4. Standard-Übungsdatenbank hochladen

Lade die Übungsdaten (JSON) in Firestore hoch:
- Collection: `exerciseTemplates`
- Felder: `nameDE`, `nameEN`, `descriptionDE`, `descriptionEN`, `category`, `isCustom: false`, `coachId: ""`

## 5. App öffnen in Android Studio

1. Android Studio → Open → `PracticeDyad/` Ordner
2. Gradle-Sync abwarten
3. Emulator oder echtes Android-Gerät (API 26+) auswählen
4. ▶ Run

## 6. App-Logo einbinden

- Das hochgeladene Logo (türkis-blauer Läufer) in verschiedene Auflösungen exportieren:
  - `res/mipmap-mdpi/ic_launcher.png` (48×48)
  - `res/mipmap-hdpi/ic_launcher.png` (72×72)
  - `res/mipmap-xhdpi/ic_launcher.png` (96×96)
  - `res/mipmap-xxhdpi/ic_launcher.png` (144×144)
  - `res/mipmap-xxxhdpi/ic_launcher.png` (192×192)
  - Runde Varianten: `ic_launcher_round.png` in gleichen Größen

## Projektstruktur

```
app/src/main/java/com/practicedyad/app/
├── MainActivity.kt              – App-Einstiegspunkt
├── PracticeDyadApplication.kt   – Hilt-App
├── data/
│   ├── model/Models.kt          – Alle Datenmodelle
│   ├── remote/FirebaseService.kt– Firebase-Operationen
│   └── repository/AppRepository.kt
├── di/AppModule.kt              – Dependency Injection
├── service/...MessagingService  – Push-Notifications
├── ui/
│   ├── theme/                   – Farben, Typografie, Theme
│   ├── navigation/              – NavGraph, Screens
│   ├── components/              – Gemeinsame UI-Komponenten
│   └── screens/
│       ├── auth/                – Login, Register, Rollenauswahl
│       ├── home/                – Startseite + Sidebar
│       ├── trainingplans/       – Trainingspläne ansehen + erstellen
│       ├── workout/             – Workout-Ausführung mit Timer
│       ├── athletes/            – Athletenverwaltung + Teams
│       ├── progress/            – Fortschrittsgraphen
│       ├── exercises/           – Übungsdatenbank + Editor
│       ├── chat/                – Nachrichtensystem
│       ├── settings/            – Einstellungen
│       └── organization/        – Organisationsverwaltung
├── utils/PdfExporter.kt         – PDF-Export
└── viewmodel/                   – ViewModels für alle Screens
```

## Features

- ✅ Rollenauswahl (Coach / Athlet*in / Organisation)
- ✅ Firebase Auth + Firestore
- ✅ Trainingspläne erstellen, teilen, bearbeiten
- ✅ Workout-Einheiten mit Übungen, Sätzen, Wiederholungen, Timer
- ✅ Gewicht- und Wiederholungs-Tracking
- ✅ Fortschrittsdiagramme (eigene Canvas-Implementierung)
- ✅ Kalender mit Trainingstagen (7-Tage-Streifen)
- ✅ Gelenkfigur-Editor mit Zeichentools
- ✅ PDF-Export für Trainingspläne
- ✅ Chat-System
- ✅ Push-Notifications (FCM)
- ✅ Dunkles/helles Design
- ✅ Deutsch/Englisch
- ✅ kg/lbs-Umrechnung
- ✅ Team-Verwaltung
- ✅ Mindestabstand zwischen Workouts
- ✅ Verbindungscode für Athlet*innen
