# ManzerTracker — Developer Architecture Guide

A personal Android application for tracking coffee brews and fitness workouts.
Built with Kotlin, Jetpack Compose, Room, and MVVM architecture.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack and Dependencies](#2-tech-stack-and-dependencies)
3. [Project Structure](#3-project-structure)
4. [Architecture Pattern (MVVM)](#4-architecture-pattern-mvvm)
5. [Data Layer — Room Database](#5-data-layer--room-database)
6. [ViewModel Layer](#6-viewmodel-layer)
7. [UI Layer — Navigation](#7-ui-layer--navigation)
8. [UI Layer — Theming](#8-ui-layer--theming)
9. [UI Layer — Screens and Compose Patterns](#9-ui-layer--screens-and-compose-patterns)
10. [Dependency Injection (Manual)](#10-dependency-injection-manual)
11. [Data Flow: End to End](#11-data-flow-end-to-end)
12. [Best Practices Audit and Improvement Suggestions](#12-best-practices-audit-and-improvement-suggestions)

---

## 1. Project Overview

ManzerTracker is a single-activity Android app with two top-level features:

| Feature | What it tracks |
|---|---|
| **Coffee** | Roasters → Coffee bags → Individual brews with brew parameters and ratings |
| **Fitness** | Exercises → Workout plans → Workout sessions with per-exercise sets, weights, reps, and RPE |

The user navigates between features using a bottom navigation bar. Within each feature, content is organized into tabs (e.g. Brews / Bags / Roasters). Adding and editing records is done via modal bottom sheets that slide up over the list.

---

## 2. Tech Stack and Dependencies

All dependency versions are centralized in `gradle/libs.versions.toml` (the Gradle version catalog). This is the modern replacement for hardcoding version strings in each `build.gradle` file.

| Library | Purpose |
|---|---|
| **Jetpack Compose + Material3** | Declarative UI toolkit. Replaces XML layouts entirely. |
| **Compose BOM** | Bill of Materials — ensures all Compose libraries use compatible versions without specifying each individually. |
| **Navigation Compose** | Type-safe in-app routing between screens within Compose. |
| **Room** | SQLite ORM. Provides compile-time SQL validation, DAO interfaces, and reactive `Flow`-based queries. |
| **KSP (Kotlin Symbol Processing)** | Code generator used by Room to create DAO implementation classes at build time. Replaces the older KAPT. |
| **Lifecycle ViewModel Compose** | Provides the `viewModel()` composable function for retrieving ViewModels scoped to a composition. |

**Build tooling:**
- `compileSdk = 35`, `minSdk = 26` (Android 8.0+)
- `isMinifyEnabled = false` on the release build — code shrinking is disabled, which is fine for personal apps but should be enabled before any production release.

---

## 3. Project Structure

```
app/src/main/java/com/supermanzer/manzertracker/
├── ManzerTrackerApplication.kt     # Application subclass — holds the DB singleton
├── MainActivity.kt                 # Single activity — sets up nav host and bottom bar
│
├── data/                           # Room data layer
│   ├── AppDatabase.kt              # @Database declaration, singleton factory
│   ├── Converters.kt               # TypeConverter: Date <-> Long
│   ├── CoffeeEntities.kt           # @Entity: Roaster, CoffeeBag, CoffeeBrew
│   ├── CoffeeDao.kt                # @Dao: CRUD + Flow queries for coffee
│   ├── FitnessEntities.kt          # @Entity: Exercise, WorkoutPlan, WorkoutPlanExercise, WorkoutSession, WorkoutSet
│   └── FitnessDao.kt               # @Dao: CRUD + @Transaction methods for fitness
│
└── ui/
    ├── navigation/
    │   └── Screen.kt               # Sealed class defining routes and nav bar items
    ├── theme/
    │   ├── Color.kt                # All color constants for both themes
    │   ├── Type.kt                 # Typography scale
    │   └── Theme.kt                # ManzerTrackerTheme composable — 4 color schemes
    ├── viewmodels/
    │   ├── CoffeeViewModel.kt      # State + actions for coffee data
    │   └── FitnessViewModel.kt     # State + actions for fitness data
    └── screens/
        ├── CommonComponents.kt     # Shared composables: DetailSection, DetailRow
        ├── CoffeeScreen.kt         # Coffee tab host, FAB, bottom sheet orchestration
        ├── CoffeeComponents.kt     # BrewItem, BagItem, RoasterItem, *Detail composables
        ├── CoffeeForms.kt          # CoffeeBrewForm, RoasterForm, CoffeeBagForm
        ├── FitnessScreen.kt        # Fitness tab host, FAB, bottom sheet orchestration
        ├── FitnessComponents.kt    # WorkoutPlanItem, WorkoutSessionItem, *Detail composables
        └── FitnessForms.kt         # WorkoutSessionForm, ExerciseForm, WorkoutPlanForm
```

---

## 4. Architecture Pattern (MVVM)

The app follows **MVVM (Model-View-ViewModel)** with a clear separation of concerns across three layers:

```
┌─────────────────────────────────┐
│        UI (Compose)             │  Reads StateFlow, calls ViewModel functions
│  Screens / Components / Forms   │
└───────────────┬─────────────────┘
                │ observes / calls
┌───────────────▼─────────────────┐
│         ViewModel               │  Holds UI state as StateFlow, launches coroutines
│  CoffeeViewModel / Fitness...   │
└───────────────┬─────────────────┘
                │ suspends / collects Flow
┌───────────────▼─────────────────┐
│       Data (Room DAOs)          │  SQL queries, returns Flow<T> or suspend fun
│  CoffeeDao / FitnessDao         │
└─────────────────────────────────┘
```

**Key principle:** UI never directly calls the DAO. UI never holds raw database objects in local `remember` state for mutation. Instead, the ViewModel is the single source of truth for what data looks like right now.

---

## 5. Data Layer — Room Database

### 5.1 Entities

Entities are plain Kotlin `data class` objects annotated with `@Entity`. Room maps each class to a database table.

**Coffee domain (3 tables, hierarchical):**

```
Roaster ──(1:many)──> CoffeeBag ──(1:many)──> CoffeeBrew
```

```kotlin
@Entity(tableName = "roasters")
data class Roaster(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val location: String? = null
)
```

- `@PrimaryKey(autoGenerate = true)` tells Room to assign IDs automatically. Default of `0` means "new record" — Room replaces 0 with the real auto-incremented value.
- `val location: String? = null` — nullable fields become `NULL`-able columns.

`CoffeeBag` and `CoffeeBrew` use `@ForeignKey` to enforce referential integrity:

```kotlin
@Entity(
    tableName = "coffee_bags",
    foreignKeys = [
        ForeignKey(
            entity = Roaster::class,
            parentColumns = ["id"],
            childColumns = ["roasterId"],
            onDelete = ForeignKey.CASCADE   // deleting a Roaster deletes its bags
        )
    ],
    indices = [Index("roasterId")]          // index required to avoid a Room warning AND improves join performance
)
```

> **Why the `Index`?** SQLite requires an index on foreign key columns for efficient lookups. Without it, every query joining on `roasterId` does a full table scan. Room will emit a warning at build time if you omit it.

**Fitness domain (5 tables):**

```
Exercise  ──(many:many via WorkoutPlanExercise)──  WorkoutPlan
                                                        │
                                                   WorkoutSession
                                                        │
                                               WorkoutSet (per exercise)
```

`WorkoutPlanExercise` is a join/junction table with a **composite primary key**:

```kotlin
@Entity(
    tableName = "workout_plan_exercises",
    primaryKeys = ["planId", "exerciseId", "orderIndex"],  // composite PK
    ...
)
data class WorkoutPlanExercise(
    val planId: Long,
    val exerciseId: Long,
    val orderIndex: Int
)
```

`WorkoutSession.planId` uses `onDelete = ForeignKey.SET_NULL` — if a plan is deleted, existing sessions that used that plan retain their data but lose the plan association (the `planId` becomes `null`).

### 5.2 TypeConverters

Room can only store primitive types (Int, Long, String, etc.) natively. `java.util.Date` is an object, so it must be converted:

```kotlin
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}
```

`Date.time` is milliseconds since Unix epoch — a `Long`. This converter is registered on the database:

```kotlin
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase()
```

### 5.3 DAOs (Data Access Objects)

DAOs are Kotlin `interface`s annotated with `@Dao`. Room generates the implementation class at compile time via KSP.

**Reactive queries with Flow:**

```kotlin
@Query("SELECT * FROM roasters ORDER BY name ASC")
fun getAllRoasters(): Flow<List<Roaster>>
```

`Flow<T>` is a Kotlin coroutines concept. Think of it as a stream that automatically emits a new list every time the underlying table changes. The UI subscribes to this stream and redraws whenever new data arrives — no manual refresh needed.

Note that `Flow`-returning queries are **not** `suspend` functions. `suspend` is used only for one-shot write operations:

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertRoaster(roaster: Roaster): Long   // suspend, one-shot, returns the new row ID

@Query("SELECT * FROM roasters ORDER BY name ASC")
fun getAllRoasters(): Flow<List<Roaster>>            // NOT suspend — ongoing stream
```

**`@Transaction` methods in `FitnessDao`:**

For complex multi-step writes that must be atomic (either all succeed or all fail), the DAO provides `@Transaction` functions:

```kotlin
@Transaction
suspend fun updateWorkoutPlanWithExercises(plan: WorkoutPlan, exercises: List<Exercise>) {
    updateWorkoutPlan(plan)
    val currentEntries = getPlanExercisesSync(plan.id)
    val newEntries = exercises.mapIndexed { index, exercise ->
        WorkoutPlanExercise(plan.id, exercise.id, index)
    }
    val toDelete = currentEntries.filterNot { ce -> newEntries.any { ne -> ne.exerciseId == ce.exerciseId && ne.orderIndex == ce.orderIndex } }
    val toInsert = newEntries.filterNot { ne -> currentEntries.any { ce -> ce.exerciseId == ne.exerciseId && ce.orderIndex == ne.orderIndex } }
    if (toDelete.isNotEmpty()) deletePlanExercises(toDelete)
    if (toInsert.isNotEmpty()) insertPlanExercises(toInsert)
}
```

`@Transaction` wraps all operations in a single SQLite transaction. If any step throws, the whole transaction is rolled back. The "surgical update" approach (diff old vs new, delete/insert only what changed) avoids unnecessarily deleting and re-inserting all rows.

### 5.4 AppDatabase and Singleton Pattern

```kotlin
@Database(entities = [...], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coffeeDao(): CoffeeDao
    abstract fun fitnessDao(): FitnessDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "manzer_tracker_db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
```

- `@Volatile` ensures the `Instance` variable is always read from main memory, not a thread-local CPU cache. This makes the null-check safe across threads.
- `synchronized(this)` prevents two threads from simultaneously creating two database instances (double-checked locking pattern).
- `fallbackToDestructiveMigration()` means if the schema version increases without a migration defined, Room drops and recreates the database. Acceptable for personal use; **not** acceptable for production apps with user data that must be preserved.
- `exportSchema = false` suppresses Room's schema export file generation. Best practice is to set this to `true` and commit the schema files, which serve as a migration audit trail.

---

## 6. ViewModel Layer

### 6.1 Purpose

ViewModels bridge the data and UI layers. They:
- Convert `Flow<T>` from the DAO into `StateFlow<T>` that the UI can `collectAsState()`
- Launch coroutines for write operations so the UI stays responsive
- Survive configuration changes (screen rotation) — their lifecycle is scoped to the screen, not the `Activity`

### 6.2 StateFlow and `stateIn`

```kotlin
val allRoasters: StateFlow<List<Roaster>> = coffeeDao.getAllRoasters()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

Breaking this down:
- `coffeeDao.getAllRoasters()` returns `Flow<List<Roaster>>` — a cold stream from Room
- `.stateIn(...)` converts it to a `StateFlow` — a **hot** stream that always has a current value
- `viewModelScope` — the coroutine scope tied to this ViewModel's lifetime
- `SharingStarted.WhileSubscribed(5000)` — the upstream Flow is only active while there are active subscribers. The `5000` millisecond grace period means it stays alive for 5 seconds after the last subscriber leaves (e.g. screen rotation), preventing a restart if the UI comes back quickly
- `emptyList()` — the initial value before any database results arrive

### 6.3 Write Operations

```kotlin
fun addRoaster(name: String, location: String? = null) {
    viewModelScope.launch {
        coffeeDao.insertRoaster(Roaster(name = name, location = location))
    }
}
```

`viewModelScope.launch` starts a coroutine in the background. The `suspend` DAO function runs off the main thread (Room enforces this). The UI calls `viewModel.addRoaster(...)` and returns immediately — it never blocks.

### 6.4 ViewModelFactory

ViewModels cannot have constructor parameters by default — the Android framework creates them. To pass the DAO in, a factory is used:

```kotlin
class CoffeeViewModelFactory(private val coffeeDao: CoffeeDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CoffeeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CoffeeViewModel(coffeeDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

In the UI, this is used as:

```kotlin
val viewModel: CoffeeViewModel = viewModel(
    factory = CoffeeViewModelFactory(database.coffeeDao())
)
```

The `viewModel()` composable from `lifecycle-viewmodel-compose` handles ViewModel scoping to the current composition automatically.

---

## 7. UI Layer — Navigation

### 7.1 Screen Sealed Class

```kotlin
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Coffee : Screen("coffee", "Coffee", Icons.Default.Coffee)
    object Fitness : Screen("fitness", "Fitness", Icons.Default.FitnessCenter)
}

val items = listOf(Screen.Coffee, Screen.Fitness)
```

A `sealed class` restricts which subclasses can exist. `object` singletons are used because routes are stateless — there is exactly one Coffee screen, not parameterized instances. This pattern makes it easy to add new top-level destinations: add an `object` to `Screen` and add it to `items`.

### 7.2 NavHost Setup in MainActivity

```kotlin
val navController = rememberNavController()

Scaffold(
    bottomBar = {
        NavigationBar {
            items.forEach { screen ->
                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    ...
                )
            }
        }
    }
) { innerPadding ->
    NavHost(navController, startDestination = Screen.Coffee.route) {
        composable(Screen.Coffee.route) { CoffeeScreen() }
        composable(Screen.Fitness.route) { FitnessScreen() }
    }
}
```

The `navigate` block options deserve explanation:
- `popUpTo(startDestination) { saveState = true }` — when tapping a bottom nav item, pop back to the start of the graph so you don't accumulate a stack of screens. `saveState = true` preserves the back-stack state for that destination.
- `launchSingleTop = true` — if you're already on Coffee and tap Coffee again, don't create a second Coffee screen on the stack.
- `restoreState = true` — if you navigated away from Fitness and come back, restore its previous state (e.g. which tab was open).

### 7.3 Dynamic Theme Switching

A subtle but elegant detail: the theme changes based on which screen is active.

```kotlin
val isFitness = currentDestination?.hierarchy?.any { it.route == Screen.Fitness.route } == true

ManzerTrackerTheme(isFitness = isFitness) { ... }
```

This checks the navigation back stack hierarchy (not just the current route) so the theme is correct even during transitions.

---

## 8. UI Layer — Theming

### 8.1 Material3 Color Scheme

The app defines four `ColorScheme` objects:

| Scheme | When applied |
|---|---|
| `CoffeeLightColorScheme` | Coffee screen, system light mode |
| `CoffeeDarkColorScheme` | Coffee screen, system dark mode |
| `FitnessLightColorScheme` | Fitness screen, system light mode |
| `FitnessDarkColorScheme` | Fitness screen, system dark mode |

`ManzerTrackerTheme` selects the correct scheme via two booleans:

```kotlin
@Composable
fun ManzerTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isFitness: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isFitness) {
        if (darkTheme) FitnessDarkColorScheme else FitnessLightColorScheme
    } else {
        if (darkTheme) CoffeeDarkColorScheme else CoffeeLightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
```

### 8.2 Theme vs. Gradient Backgrounds

The `MaterialTheme.colorScheme.background` color provides the base. But both screens **also** apply a `Brush.linearGradient` directly to a `Box` modifier inside the screen:

```kotlin
val coffeeGradient = Brush.linearGradient(colors = listOf(CoffeeLightGradient1, CoffeeLightGradient2))

Box(modifier = Modifier.fillMaxSize().background(coffeeGradient)) { ... }
```

This is separate from the theme — the theme handles component colors (buttons, cards, text), while the gradient handles the raw screen backdrop. Cards use `alpha = 0.9f` on their surface color to let the gradient show through slightly.

Note that `ManzerTrackerTheme` is also called a second time inside each screen with `isFitness = true/false`. This is redundant given that `MainActivity` already wraps everything in the theme, but it functions as a safety net ensuring the correct theme is applied regardless of entry point.

---

## 9. UI Layer — Screens and Compose Patterns

### 9.1 Composable State in Screens

Both `CoffeeScreen` and `FitnessScreen` follow the same orchestration pattern. Understanding the state variables is key:

```kotlin
var activeForm by remember { mutableStateOf(CoffeeFormType.NONE) }
var selectedBrew by remember { mutableStateOf<CoffeeBrew?>(null) }
var selectedTab by remember { mutableStateOf(CoffeeTab.BREWS) }
var showFabMenu by remember { mutableStateOf(false) }
var showDeleteDialog by remember { mutableStateOf<Any?>(null) }
```

- `remember { mutableStateOf(...) }` — `remember` keeps state across recompositions. Without it, every recomposition would reset the value. `mutableStateOf` creates an observable value — when it changes, any composable reading it is automatically scheduled for recomposition.
- `by` is a Kotlin property delegate. It unwraps the `MutableState<T>` so you write `activeForm = X` instead of `activeForm.value = X`.

### 9.2 Form Type Enum as a State Machine

```kotlin
enum class CoffeeFormType {
    NONE, BREW, ROASTER, BAG, BREW_DETAIL, EDIT_BREW, EDIT_BAG, EDIT_ROASTER, BAG_DETAIL, ROASTER_DETAIL
}
```

`activeForm` acts as a simple state machine. When it's `NONE`, no bottom sheet is shown. When it changes to any other value, the `ModalBottomSheet` renders and a `when` expression selects which form to display inside it. This approach puts all the sheet logic in one place.

### 9.3 ModalBottomSheet

```kotlin
val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

if (activeForm != CoffeeFormType.NONE) {
    ModalBottomSheet(
        onDismissRequest = {
            activeForm = CoffeeFormType.NONE
            selectedBrew = null
        },
        sheetState = sheetState
    ) {
        when (activeForm) {
            CoffeeFormType.BREW -> CoffeeBrewForm(...)
            ...
        }
    }
}
```

- `rememberModalBottomSheetState(skipPartiallyExpanded = true)` — the sheet opens fully expanded immediately rather than stopping at a half-open position.
- `onDismissRequest` fires when the user swipes the sheet down or taps outside it. This resets `activeForm` to `NONE`, which removes the sheet from the composition.
- The sheet is conditionally included in the composition (`if (activeForm != NONE)`). When `activeForm` returns to `NONE`, Compose removes the sheet entirely from the tree — it is not just hidden.

### 9.4 collectAsState — Subscribing to StateFlow

```kotlin
val brews by viewModel.allBrews.collectAsState()
```

`collectAsState()` is a Compose extension that:
1. Subscribes to the `StateFlow` from the ViewModel
2. Returns the current value as Compose `State<T>`
3. Automatically unsubscribes when the composable leaves the composition
4. Triggers recomposition every time the flow emits a new value

`by` again delegates the property, so `brews` is `List<CoffeeBrew>` not `State<List<CoffeeBrew>>`.

### 9.5 In-Memory Relationship Resolution

The app does not use Room's `@Relation` feature. Instead, all three lists (`brews`, `bags`, `roasters`) are loaded independently, and the UI resolves relationships in-memory:

```kotlin
items(brews) { brew ->
    val bag = bags.find { it.id == brew.bagId }
    val roaster = roasters.find { it.id == bag?.roasterId }
    BrewItem(brew = brew, bag = bag, roaster = roaster, ...)
}
```

This works fine for small datasets. The three `StateFlow`s are independent, so a roaster update will cause `roasters` to emit a new list, which causes recomposition of the brew list items even though brews didn't change.

### 9.6 LaunchedEffect

`LaunchedEffect` runs a suspending block as a side effect when a composable enters composition or when its key changes:

```kotlin
LaunchedEffect(selectedPlan, session, planExercises) {
    if (session != null && sets.isEmpty()) {
        viewModel.getSetsForSession(session.id).collect { fetchedSets ->
            if (sets.isEmpty()) { sets = fetchedSets }
        }
    }
}
```

The keys `(selectedPlan, session, planExercises)` mean: re-run this block whenever any of those values change. This is used in `WorkoutSessionForm` to pre-populate sets when editing an existing session or when a plan is selected.

> **Caution:** In `WorkoutPlanForm`, the same `LaunchedEffect(initialExercises)` block is written three times. Only one copy is needed — duplicates are a bug left by the AI assistant.

### 9.7 LazyColumn

```kotlin
LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    items(brews) { brew -> BrewItem(...) }
}
```

`LazyColumn` is the Compose equivalent of `RecyclerView`. It only renders items that are currently visible, making it efficient for long lists. `items(list)` is a DSL function that maps each list element to a composable item. The `key` parameter (not used here) can be added to improve recomposition efficiency: `items(brews, key = { it.id }) { ... }`.

### 9.8 Forms: Controlled Inputs

Forms use the "controlled input" pattern — each field's value is a `remember`'d state variable, and `onValueChange` updates it:

```kotlin
var method by remember { mutableStateOf(brew?.method ?: "V60") }

OutlinedTextField(
    value = method,
    onValueChange = { method = it },
    label = { Text("Brew Method") }
)
```

The field's displayed value is always derived from state, never from the TextField's internal state. This makes it easy to pre-populate fields for editing (pass in the existing entity) and to validate or transform input before saving.

### 9.9 ExposedDropdownMenuBox

The "Select Coffee Bag" and "Select Workout Plan" dropdowns use Material3's exposed dropdown:

```kotlin
ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
    OutlinedTextField(
        value = selectedBag?.name ?: "Select Coffee Bag",
        readOnly = true,
        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        bags.forEach { bag ->
            DropdownMenuItem(text = { Text(bag.name) }, onClick = { selectedBag = bag; expanded = false })
        }
    }
}
```

`menuAnchor(MenuAnchorType.PrimaryNotEditable)` is required — it tells the dropdown menu where to anchor itself (to the TextField). `readOnly = true` prevents the system keyboard from appearing; selection only happens via the dropdown.

### 9.10 Reusable Components

`CommonComponents.kt` provides two low-level composables used throughout detail views:

- `DetailSection(title, onEdit?, content)` — a labeled group with an optional edit icon
- `DetailRow(label, value, onEdit?)` — a single key/value pair row

These follow the **slot API pattern**: the `content` lambda accepts a `@Composable` block, letting callers inject arbitrary child composables.

---

## 10. Dependency Injection (Manual)

The app uses manual dependency injection rather than a framework like Hilt. Here is the chain:

1. `ManzerTrackerApplication` (created by Android at app start) creates the `AppDatabase` lazily:
   ```kotlin
   val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
   ```
2. `CoffeeScreen` retrieves the application and its database:
   ```kotlin
   val database = (context.applicationContext as ManzerTrackerApplication).database
   ```
3. The ViewModel is instantiated with the DAO:
   ```kotlin
   val viewModel: CoffeeViewModel = viewModel(factory = CoffeeViewModelFactory(database.coffeeDao()))
   ```

This works cleanly for a simple two-screen app. The downside is that the Screen composable is tightly coupled to `ManzerTrackerApplication` — it knows how to resolve its own dependencies rather than receiving them from outside.

---

## 11. Data Flow: End to End

Here is the complete journey of a "user taps Save Brew" action:

```
1. User fills out CoffeeBrewForm and taps "Save Brew"
        │
2. Button onClick calls onSave(CoffeeBrew(...))
   (onSave lambda is passed in from CoffeeScreen)
        │
3. CoffeeScreen's lambda calls: viewModel.addBrew(brew)
        │
4. CoffeeViewModel.addBrew launches a coroutine:
   viewModelScope.launch { coffeeDao.insertBrew(brew) }
        │
5. Room executes INSERT on background thread
        │
6. Room's Flow for getAllBrews() emits a new List<CoffeeBrew>
        │
7. allBrews StateFlow in ViewModel emits the new list
        │
8. collectAsState() in CoffeeScreen receives new value
        │
9. Compose schedules recomposition
        │
10. LazyColumn re-renders with the new brew at the top
```

No manual refresh, no callbacks up the chain — the Flow/StateFlow pipeline handles propagation automatically.

---

## 12. Best Practices Audit and Improvement Suggestions

The following items range from quick wins to more significant refactors. They are grouped by priority.

---

### 12.1 High Priority

**A. Use `fallbackToDestructiveMigration` with caution — provide real migrations**

`fallbackToDestructiveMigration()` wipes the database whenever `version` increments. The database is currently at version 3, meaning it has already been wiped twice during development. For any app where data matters, define explicit `Migration` objects:

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN rpe REAL")
    }
}
Room.databaseBuilder(...)
    .addMigrations(MIGRATION_2_3)
    .build()
```

**B. Replace `java.util.Date` with `kotlinx.datetime` or `java.time`**

`java.util.Date` is a legacy class with confusing mutability and time-zone behavior. `minSdk = 26` means the entire `java.time` API is available without desugaring. Alternatively, `kotlinx-datetime` is idiomatic Kotlin. Either option results in cleaner, less error-prone date handling.

**C. Fix the triple `LaunchedEffect` in `WorkoutPlanForm`**

`WorkoutPlanForm.kt:325–343` contains three nearly identical `LaunchedEffect(initialExercises)` blocks. In Compose, only one `LaunchedEffect` with a given key is active at a time — having three with the same key means only the first one runs, and the others silently do nothing. Two of the three should be deleted.

**D. Add `key` to all `LazyColumn` `items` calls**

```kotlin
// Before
items(brews) { brew -> BrewItem(...) }

// After
items(brews, key = { it.id }) { brew -> BrewItem(...) }
```

Without a stable key, Compose cannot tell which items moved or changed when the list updates. It recomposes all visible items on any change. Providing the entity ID as a key allows Compose to recompose only the items that actually changed.

---

### 12.2 Medium Priority

**E. Extract a Repository layer**

ViewModels currently call DAOs directly. Introducing a repository class between them yields several benefits: the ViewModel becomes testable without a real database, the data-access logic can be shared between ViewModels, and caching or offline-first logic can be added in one place.

```kotlin
class CoffeeRepository(private val coffeeDao: CoffeeDao) {
    val allRoasters = coffeeDao.getAllRoasters()
    suspend fun addRoaster(name: String, location: String?) =
        coffeeDao.insertRoaster(Roaster(name = name, location = location))
}
```

**F. Replace manual `ViewModelFactory` with Hilt**

The manual factory pattern requires boilerplate for every ViewModel. Hilt (Google's recommended DI framework for Android) generates the factory automatically and handles the `Application` / `Activity` / `Fragment` scope graph:

```kotlin
// With Hilt
@HiltViewModel
class CoffeeViewModel @Inject constructor(private val coffeeRepo: CoffeeRepository) : ViewModel()

// In composable — factory is auto-generated
val viewModel: CoffeeViewModel = hiltViewModel()
```

**G. Move UI state (form state, selected item) into ViewModel**

Currently, the "which item is selected" and "which form is active" state lives in the Screen composable (`remember { mutableStateOf(...) }`). This state is lost on screen rotation. Moving it into the ViewModel (which survives rotation) provides a better user experience:

```kotlin
// In ViewModel
private val _activeForm = MutableStateFlow(CoffeeFormType.NONE)
val activeForm: StateFlow<CoffeeFormType> = _activeForm.asStateFlow()

fun showBrewDetail(brew: CoffeeBrew) {
    _selectedBrew.value = brew
    _activeForm.value = CoffeeFormType.BREW_DETAIL
}
```

**H. `WorkoutSessionForm` — fix the `LaunchedEffect` collect pattern**

The current code collects a Flow inside `LaunchedEffect` using `.collect { ... }`:

```kotlin
LaunchedEffect(selectedPlan, session, planExercises) {
    if (session != null && sets.isEmpty()) {
        viewModel.getSetsForSession(session.id).collect { fetchedSets ->
            if (sets.isEmpty()) { sets = fetchedSets }
        }
    }
}
```

This is fragile: `collect` is a suspend function that runs indefinitely, and the guard `if (sets.isEmpty())` only works for the first emission. The idiomatic approach is to use `collectAsState()` at the top level and react to the collected value, or use `.first()` to get exactly one value:

```kotlin
LaunchedEffect(session?.id) {
    if (session != null) {
        sets = viewModel.getSetsForSession(session.id).first()
    }
}
```

---

### 12.3 Lower Priority / Polish

**I. Enable `isMinifyEnabled = true` for release builds and configure ProGuard**

Code shrinking and obfuscation are disabled. For any app distributed outside personal use, enable them and test the release build.

**J. Export Room schema**

Change `exportSchema = false` to `exportSchema = true` and commit the generated JSON files to version control. These files act as a diff-able audit trail of every schema change and are required for Room's built-in migration testing utilities.

**K. Add `contentDescription` strings to resource file**

All `Icon` composables use hardcoded English strings for `contentDescription`. These should be moved to `strings.xml` for proper localization support and to make them accessible to screen readers.

**L. `BagDetail` is missing a brews count / brew history link**

The `BAG_DETAIL` bottom sheet shows bag metadata but provides no way to see which brews used that bag. A natural improvement would be a "Brews using this bag" section showing filtered brew items.

**M. Workout session requires a plan**

In `WorkoutSessionForm`, the Save button is `enabled = selectedPlan != null`. This prevents logging ad-hoc workouts without creating a plan first. The UI label "Select Workout Plan" implies it might be optional. Either allow plan-less sessions or update the UX to make the requirement clearer upfront.

**N. No tests**

The project contains only the generated placeholder tests. Consider adding:
- Room DAO tests using `Room.inMemoryDatabaseBuilder` (run on device/emulator)
- ViewModel unit tests using `kotlinx-coroutines-test` and a fake DAO
- Compose UI tests for critical flows using `ComposeTestRule`

---

*This document reflects the codebase as of April 2026. Update it when the schema version increments, new screens are added, or DI is refactored.*
