# PracticeDyad

**Your personal training partner.** A native Android and iOS app for coaches and athletes to create, share, and track customized training plans with real-time progress monitoring.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)

---

## Features

### For Coaches 🏋️
- **Training Plans** — Create and organize workout units with custom exercises
- **Team Management** — Manage athletes and organizations in one dashboard
- **Plan Sharing** — Share plans with individual athletes or entire teams
- **Circuit Training** — Group exercises into circuits with rest periods
- **Progress Tracking** — Monitor athlete performance in real-time
- **PDF Export** — Generate downloadable training plan PDFs
- **Multi-language** — Full German (Deutsch) & English support

### For Athletes 🏃
- **Assigned Plans** — View shared training plans from your coaches
- **Workout Execution** — Execute workouts with guided exercise sequences
- **Progress Logging** — Track completed sessions and performance metrics
- **Flexible Scheduling** — Work on your schedule (specific days or rhythmic patterns)

### For Organizations 🏢
- **Team Organization** — Manage multiple coaches and athletes
- **Plan Library** — Centralized workout plan management
- **Reporting** — Monitor team-wide training progress

---

## Tech Stack

- **Platform:** Android (native), iOS (planned)
- **Language:** Kotlin (Android), Swift (iOS)
- **UI Framework:** Jetpack Compose (Android)
- **Architecture:** MVVM + Clean Architecture
- **Backend:** Firebase (Firestore, Authentication)
- **State Management:** MutableStateFlow, Hilt DI
- **Database:** Cloud Firestore (NoSQL)

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 11+
- Firebase project (Google Cloud Console access)
- Git + SSH key configured for GitHub

### Installation

1. **Clone the repository:**
   ```bash
   git clone git@github.com:BasketballStarsCode/PracticeDyad.git
   cd PracticeDyad
   ```

2. **Configure Firebase:**
   - Create a Firebase project at https://console.firebase.google.com
   - Enable **Cloud Firestore** (Region: Europe for EU compliance)
   - Enable **Email/Password Authentication**
   - Download `google-services.json` and place in `app/`
   - Publish security rules:
     ```json
     {
       "rules": {
         "users": {
           "{uid}": {
             ".read": "request.auth.uid == uid",
             ".write": "request.auth.uid == uid"
           }
         },
         "trainingPlans": {
           "{planId}": {
             ".read": "request.auth != null",
             ".write": "request.auth != null"
           }
         }
       }
     }
     ```

3. **Build & Run:**
   ```bash
   ./gradlew build
   # Open in Android Studio and run on emulator or device
   ```

### Development Workflow

- **Branch:** Always work on feature branches
- **Commit Style:** Descriptive, e.g., `feat: add circuit training grouping`
- **Testing:** Run on Android emulator before pushing
- **Push:** `git push origin feature-branch` → Create Pull Request

---

## Project Structure

```
app/
├── src/main/java/com/practicedyad/app/
│   ├── data/
│   │   ├── model/          # Data classes (TrainingPlan, WorkoutUnit, etc.)
│   │   └── repository/     # Firebase data access layer
│   ├── ui/
│   │   ├── screens/        # Composable screens (TrainingPlans, Profile, etc.)
│   │   ├── components/     # Reusable UI components
│   │   └── theme/          # Design tokens, strings, colors
│   ├── viewmodel/          # MVVM ViewModels
│   └── utils/              # Helpers (PdfExporter, etc.)
├── res/
│   ├── drawable/           # App icons and assets
│   └── values/             # Strings, colors, dimensions
└── AndroidManifest.xml
```

---

## Key Screens

| Screen | Role | Purpose |
|--------|------|---------|
| **Login/Registration** | Coach, Athlete, Org | User authentication & role selection |
| **Home/Dashboard** | All | Today's schedule, upcoming workouts |
| **Training Plans** | Coach | Create, edit, share plans |
| **Plan Preview** | All | View detailed plan structure |
| **Workout Execution** | Athlete | Execute a workout unit with timer |
| **Athletes** | Coach | Manage assigned athletes |
| **Profile** | All | View/edit user information |

---

## Known Issues & Roadmap

### Completed ✅
- Firestore data persistence (fixed - API was disabled)
- Localization (German + English text switching)
- Circuit Training exercise grouping
- PDF export functionality

### In Progress 🔄
- Firestore indexing for workout history queries
- Additional hardcoded string localization (ReactionGames, Settings)

### Planned 📋
- **iOS App** — React Native or Swift native port
- **Advanced Analytics** — Detailed progress reports
- **Gamification** — Leaderboards, achievements
- **Message Encryption** — End-to-end chat between coaches & athletes
- **Offline Mode** — Sync when back online

---

## Contributing

1. Fork this repository (under your own org for now)
2. Create a feature branch: `git checkout -b feat/amazing-feature`
3. Commit your changes: `git commit -m 'feat: add amazing feature'`
4. Push to branch: `git push origin feat/amazing-feature`
5. Open a Pull Request with a clear description

---

## Security & Privacy

- **GDPR Compliant** — Data stored in EU-region Firestore (eur3)
- **Authentication** — Firebase Email/Password (upgrade to OAuth planned)
- **Permissions** — Role-based access control (Coach, Athlete, Organization)
- **Data Encryption** — In-transit via HTTPS; at-rest via Firebase encryption

---

## License

This project is **proprietary** — all rights reserved. Unauthorized copying or modification is prohibited.

---



---

**Made with ❤️ for coaches and athletes who demand excellence.**
