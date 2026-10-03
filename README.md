# BrewBuddy

A personal Android app for tracking the thing Ryan Manzer cares about most: coffee.

> **Note:** This app was built for one specific person. If you are not Ryan Manzer, the tracked activities may hold limited appeal — unless you also obsess over brew ratios and grind sizes.

---

## What It Tracks

- **Roasters** — coffee roasting companies (name, location)
- **Coffee Bags** — individual purchases with origin, variety, process type, roast date, and region
- **Brews** — individual brewing sessions linked to a bag, capturing method, grind size, water temperature, brew ratio, and personal taste ratings with notes, plus ideas for the next brew that reappear when you next brew the same bag

## Insights

A second tab summarises what you have logged:

- **Stat tiles** — total brews, average rating, brews in the last 30 days
- **Rating over time** — a line chart for one coffee bag at a time
- **Best recipes by roaster** — pick a roaster to see its three best-rated combinations of water temperature, grind size, and ratio (a recipe needs at least five rated brews to qualify)
- **Average rating by roaster**, **by brew method**, and **top-rated bags** — bar charts, each showing how many brews sit behind the average

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
│   ├── Entities        # Room entities (Roaster, CoffeeBag, CoffeeBrew)
│   ├── CoffeeDao       # CRUD + Flow queries
│   ├── BrewInsights    # Pure summary functions for the Insights tab
│   └── AppDatabase     # Room singleton, version 6
├── ui/
│   ├── components/     # Cards, detail views, form inputs, and charts, one composable per file
│   ├── screens/        # CoffeeScreen, its forms, and InsightsScreen
│   ├── viewmodels/     # CoffeeViewModel, InsightsViewModel
│   ├── navigation/     # Sealed route definitions for the bottom nav bar
│   └── theme/          # Color schemes, typography, dark/light theming
├── MainActivity.kt
└── BrewBuddyApplication.kt
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

Room schema version 6 with one DAO covering three entities (Roaster, CoffeeBag, CoffeeBrew). Foreign keys enforce relational integrity with cascading deletes. Schema changes ship with explicit Room migrations, so existing data survives upgrades.

The package name, `applicationId`, and database file still use the app's original name, `manzertracker`. That is deliberate: changing them would make Android treat BrewBuddy as a new app and strand existing data.
