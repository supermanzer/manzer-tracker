# ManzerTracker

A personal Android app for tracking the things Ryan Manzer cares about: coffee and fitness.

> **Note:** This app was built for one specific person. If you are not Ryan Manzer, the tracked activities may hold limited appeal — unless you also obsess over brew ratios and RPE scores.

---

## What It Tracks

### Coffee
- **Roasters** — coffee roasting companies (name, location)
- **Coffee Bags** — individual purchases with origin, variety, process type, roast date, and region
- **Brews** — individual brewing sessions linked to a bag, capturing method, grind size, water temperature, brew ratio, and personal taste ratings with notes

### Fitness
- **Exercises** — individual exercise definitions with category tags (Strength, Cardio, etc.)
- **Workout Plans** — custom routines with ordered exercise lists
- **Workout Sessions** — logged sessions with start/end times and notes
- **Sets** — per-exercise sets within a session, tracking weight, reps, and Rate of Perceived Exertion (RPE)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM with ViewModels and StateFlow |
| Database | Room (local, SQLite-backed) |
| Async | Kotlin Coroutines + Flow |
| Navigation | Jetpack Navigation Compose |
| Build | Gradle with KSP |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |

---

## Architecture

```
app/src/main/java/com/supermanzer/manzertracker/
├── data/
│   ├── Entities        # Room entities (Roaster, CoffeeBag, CoffeeBrew, Exercise, etc.)
│   ├── DAOs            # CoffeeDao, FitnessDao
│   └── AppDatabase     # Room singleton, version 3
├── ui/
│   ├── screens/        # CoffeeScreen, FitnessScreen + composable components
│   ├── viewmodels/     # CoffeeViewModel, FitnessViewModel
│   ├── navigation/     # Sealed route definitions, bottom nav bar
│   └── theme/          # Color schemes, typography, dark/light theming
├── MainActivity.kt
└── ManzerTrackerApplication.kt
```

Data flows reactively: Room emits `Flow` updates → ViewModels collect into `StateFlow` → Compose observes and recomposes.

---

## Running the App

1. Clone the repo and open in Android Studio.
2. Sync Gradle dependencies.
3. Run on a device or emulator running Android 8.0+.

No API keys, accounts, or network access required — all data is stored locally on-device.

---

## Database

Room schema version 3 with two DAOs covering eight entities across the Coffee and Fitness modules. Foreign keys enforce relational integrity with cascading deletes. Development builds use destructive migration; this is intentional.
