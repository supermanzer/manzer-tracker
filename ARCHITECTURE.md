# BrewBuddy — Developer Architecture Guide

A personal Android application for tracking coffee: roasters, bags, and brews.
Built with Kotlin, Jetpack Compose, Room, and MVVM architecture.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack and Dependencies](#2-tech-stack-and-dependencies)
3. [Project Structure](#3-project-structure)
4. [Architecture Pattern (MVVM)](#4-architecture-pattern-mvvm)
5. [Data Layer — Room Database](#5-data-layer--room-database)
6. [ViewModel Layer](#6-viewmodel-layer)
7. [UI Layer — App Entry Point](#7-ui-layer--app-entry-point)
8. [UI Layer — Theming](#8-ui-layer--theming)
9. [UI Layer — Screens and Compose Patterns](#9-ui-layer--screens-and-compose-patterns)
10. [Dependency Injection (Manual)](#10-dependency-injection-manual)
11. [Data Flow: End to End](#11-data-flow-end-to-end)
12. [Best Practices Audit and Improvement Suggestions](#12-best-practices-audit-and-improvement-suggestions)

---

## 1. Project Overview

BrewBuddy is a single-activity, single-screen Android app that tracks a three-level coffee hierarchy:

```
Roasters → Coffee bags → Individual brews with brew parameters and ratings
```

Content is organized into tabs (Brews / Bags / Roasters). Adding and editing records is done via modal bottom sheets that slide up over the list.

> **History:** the app began life as "ManzerTracker" and also tracked fitness workouts. That feature was removed in schema version 5. The Kotlin package (`com.supermanzer.manzertracker`), `applicationId`, and database file name (`manzer_tracker_db`) keep the old name on purpose — see [5.4](#54-appdatabase-and-singleton-pattern).

---

## 2. Tech Stack and Dependencies

All dependency versions are centralized in `gradle/libs.versions.toml` (the Gradle version catalog). This is the modern replacement for hardcoding version strings in each `build.gradle` file.

| Library | Purpose |
|---|---|
| **Jetpack Compose + Material3** | Declarative UI toolkit. Replaces XML layouts entirely. |
| **Compose BOM** | Bill of Materials — ensures all Compose libraries use compatible versions without specifying each individually. |
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
├── BrewBuddyApplication.kt         # Application subclass — holds the DB singleton
├── MainActivity.kt                 # Single activity — applies the theme and hosts CoffeeScreen
│
├── data/                           # Room data layer
│   ├── AppDatabase.kt              # @Database declaration, migrations, singleton factory
│   ├── Converters.kt               # TypeConverters: Instant / LocalDate <-> Long
│   ├── CoffeeEntities.kt           # @Entity: Roaster, CoffeeBag, CoffeeBrew
│   └── CoffeeDao.kt                # @Dao: CRUD + Flow queries for coffee
│
└── ui/
    ├── theme/
    │   ├── Color.kt                # Color constants for the coffee theme
    │   ├── Type.kt                 # Typography scale
    │   └── Theme.kt                # BrewBuddyTheme composable — light and dark color schemes
    ├── viewmodels/
    │   └── CoffeeViewModel.kt      # State + actions for coffee data
    ├── components/                 # One composable per file
    │   ├── BrewItem.kt / BagItem.kt / RoasterItem.kt        # List cards
    │   ├── BrewDetail.kt / BagDetail.kt / RoasterDetail.kt  # Bottom sheet detail views
    │   ├── DetailSection.kt / DetailRow.kt                  # Building blocks for detail views
    │   └── DropdownField.kt / WheelNumberPicker.kt          # Reusable form inputs
    └── screens/
        ├── CoffeeScreen.kt         # Tab host, FAB, bottom sheet orchestration
        └── CoffeeForms.kt          # CoffeeBrewForm, RoasterForm, CoffeeBagForm
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
│  CoffeeViewModel                │
└───────────────┬─────────────────┘
                │ suspends / collects Flow
┌───────────────▼─────────────────┐
│       Data (Room DAOs)          │  SQL queries, returns Flow<T> or suspend fun
│  CoffeeDao                      │
└─────────────────────────────────┘
```

**Key principle:** UI never directly calls the DAO. UI never holds raw database objects in local `remember` state for mutation. Instead, the ViewModel is the single source of truth for what data looks like right now.

---

## 5. Data Layer — Room Database

### 5.1 Entities

Entities are plain Kotlin `data class` objects annotated with `@Entity`. Room maps each class to a database table.

The schema has three tables in a strict hierarchy:

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

### 5.2 TypeConverters

Room can only store primitive types (Int, Long, String, etc.) natively. `java.time.Instant` and `LocalDate` are objects, so they must be converted:

```kotlin
class Converters {
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    // LocalDate is stored as UTC-midnight epoch millis
    ...
}
```

Both types are stored as epoch milliseconds — a `Long`. The converters are registered on the database:

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

### 5.4 AppDatabase and Singleton Pattern

```kotlin
@Database(entities = [Roaster::class, CoffeeBag::class, CoffeeBrew::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coffeeDao(): CoffeeDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "manzer_tracker_db")
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
```

- `@Volatile` ensures the `Instance` variable is always read from main memory, not a thread-local CPU cache. This makes the null-check safe across threads.
- `synchronized(this)` prevents two threads from simultaneously creating two database instances (double-checked locking pattern).
- `addMigrations(...)` registers explicit `Migration` objects. There is no `fallbackToDestructiveMigration()`, so a version bump without a matching migration crashes at startup rather than silently wiping data.
- `exportSchema = false` suppresses Room's schema export file generation. Best practice is to set this to `true` and commit the schema files, which serve as a migration audit trail.

**Why the version went 4 → 5 when fitness was removed.** Room hashes the entity list into an *identity hash* stored in the database file. Removing entities changes that hash, so opening an existing version-4 file with the new code would fail with "Room cannot verify the data integrity". `MIGRATION_4_5` drops the five fitness tables (children first, so no foreign key is left dangling) and leaves the coffee tables untouched.

**Version 5 → 6** added `CoffeeBrew.nextBrewIdeas`, a nullable note on what to change next time. Adding a nullable column is the gentlest kind of migration: one `ALTER TABLE ... ADD COLUMN`, and existing rows simply read as `NULL`.

**Names that deliberately still say "manzertracker".** Android identifies an installed app by its `applicationId`, and Room identifies the database by its file name. Changing either would make the rebranded build look like a brand-new app with an empty database. The user-facing name comes from `app_name` in `strings.xml`, which is independent of both.

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

## 7. UI Layer — App Entry Point

With a single feature there is nothing to navigate between, so `MainActivity` hosts the one screen directly:

```kotlin
setContent {
    BrewBuddyTheme {
        CoffeeScreen()
    }
}
```

There is no `NavHost`, no bottom bar, and no Navigation Compose dependency. A bottom navigation bar with one destination is a Material anti-pattern — the guidelines call for three to five. In-screen movement between Brews, Bags, and Roasters is handled by the `TabRow` inside `CoffeeScreen` (see section 9).

If a second top-level destination is ever added, reintroduce `androidx.navigation:navigation-compose`, a sealed `Screen` class describing the routes, and a `NavHost` in `MainActivity`.

---

## 8. UI Layer — Theming

### 8.1 Material3 Color Scheme

The app defines two `ColorScheme` objects:

| Scheme | When applied |
|---|---|
| `CoffeeLightColorScheme` | System light mode |
| `CoffeeDarkColorScheme` | System dark mode |

`BrewBuddyTheme` selects between them:

```kotlin
@Composable
fun BrewBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) CoffeeDarkColorScheme else CoffeeLightColorScheme,
        typography = Typography,
        content = content
    )
}
```

### 8.2 Theme vs. Gradient Backgrounds

The `MaterialTheme.colorScheme.background` color provides the base. But `CoffeeScreen` **also** applies a `Brush.linearGradient` directly to a `Box` modifier:

```kotlin
val coffeeGradient = Brush.linearGradient(colors = listOf(CoffeeLightGradient1, CoffeeLightGradient2))

Box(modifier = Modifier.fillMaxSize().background(coffeeGradient)) { ... }
```

This is separate from the theme — the theme handles component colors (buttons, cards, text), while the gradient handles the raw screen backdrop. Cards use `alpha = 0.9f` on their surface color to let the gradient show through slightly.

---

## 9. UI Layer — Screens and Compose Patterns

### 9.1 Composable State in Screens

`CoffeeScreen` orchestrates everything on screen. Understanding its state variables is key:

```kotlin
var activeForm by remember { mutableStateOf(CoffeeFormType.NONE) }
var selectedBrew by remember { mutableStateOf<CoffeeBrew?>(null) }
var selectedTab by remember { mutableStateOf(CoffeeTab.BREWS) }
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

The new-brew form uses the same approach for "ideas from last brew": `CoffeeScreen` passes the full `brews` list in, and the form picks the most recent brew of the selected bag inside `remember(brew, selectedBag, previousBrews)`. The keys make the lookup re-run when the bag selection changes, and only then. If that brew recorded ideas, they are shown in a card under the bag dropdown.

This works fine for small datasets. The three `StateFlow`s are independent, so a roaster update will cause `roasters` to emit a new list, which causes recomposition of the brew list items even though brews didn't change.

### 9.6 LazyColumn

```kotlin
LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    items(brews, key = { it.id }) { brew -> BrewItem(...) }
}
```

`LazyColumn` is the Compose equivalent of `RecyclerView`. It only renders items that are currently visible, making it efficient for long lists. `items(list)` is a DSL function that maps each list element to a composable item. The `key` parameter gives each item a stable identity, so Compose can tell which items moved or changed and recompose only those.

### 9.7 Forms: Controlled Inputs

Forms use the "controlled input" pattern — each field's value is a `remember`'d state variable, and `onValueChange` updates it:

```kotlin
var ratio by remember { mutableStateOf(brew?.ratio ?: "1:15") }

OutlinedTextField(
    value = ratio,
    onValueChange = { ratio = it },
    label = { Text("Ratio (1:X)") }
)
```

The field's displayed value is always derived from state, never from the TextField's internal state. This makes it easy to pre-populate fields for editing (pass in the existing entity) and to validate or transform input before saving.

### 9.8 ExposedDropdownMenuBox

The "Select Coffee Bag" dropdown uses Material3's exposed dropdown:

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

### 9.9 Reusable Components

The `ui/components/` package provides `DropdownField<T>`, a generic wrapper around the exposed dropdown above for fields with a fixed set of choices. The brew form uses it for Brew Method (the `BrewMethod` enum's labels), Grind Size (1–30), and Rating (1–5, or None). It is *stateless* about the selection — the caller owns `selected` and receives `onSelected` — and only keeps its own open/closed flag. This is **state hoisting**.

`WheelNumberPicker` is a scrolling number wheel, which Compose does not ship. It is a `LazyColumn` with `rememberSnapFlingBehavior`, so a fling always settles with one number centred; `derivedStateOf` works out which item is nearest the centre, and `snapshotFlow` reports it to the caller. The brew form shows it in a dialog for Water Temp (150–212 °F).

It also provides two low-level composables used throughout detail views:

- `DetailSection(title, onEdit?, content)` — a labeled group with an optional edit icon
- `DetailRow(label, value, onEdit?)` — a single key/value pair row

These follow the **slot API pattern**: the `content` lambda accepts a `@Composable` block, letting callers inject arbitrary child composables.

---

## 10. Dependency Injection (Manual)

The app uses manual dependency injection rather than a framework like Hilt. Here is the chain:

1. `BrewBuddyApplication` (created by Android at app start) creates the `AppDatabase` lazily:
   ```kotlin
   val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
   ```
2. `CoffeeScreen` retrieves the application and its database:
   ```kotlin
   val database = (context.applicationContext as BrewBuddyApplication).database
   ```
3. The ViewModel is instantiated with the DAO:
   ```kotlin
   val viewModel: CoffeeViewModel = viewModel(factory = CoffeeViewModelFactory(database.coffeeDao()))
   ```

This works cleanly for a simple single-screen app. The downside is that the Screen composable is tightly coupled to `BrewBuddyApplication` — it knows how to resolve its own dependencies rather than receiving them from outside.

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

**A. Keep providing real migrations**

The database uses explicit `Migration` objects and no destructive fallback. Keep it that way: every `version` bump needs a matching migration, even a no-op one, or the app will crash on launch for existing installs.

```kotlin
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS workout_sets")
        ...
    }
}
Room.databaseBuilder(...)
    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
    .build()
```

---

### 12.2 Medium Priority

**B. Extract a Repository layer**

ViewModels currently call DAOs directly. Introducing a repository class between them yields several benefits: the ViewModel becomes testable without a real database, the data-access logic can be shared between ViewModels, and caching or offline-first logic can be added in one place.

```kotlin
class CoffeeRepository(private val coffeeDao: CoffeeDao) {
    val allRoasters = coffeeDao.getAllRoasters()
    suspend fun addRoaster(name: String, location: String?) =
        coffeeDao.insertRoaster(Roaster(name = name, location = location))
}
```

**C. Replace manual `ViewModelFactory` with Hilt**

The manual factory pattern requires boilerplate for every ViewModel. Hilt (Google's recommended DI framework for Android) generates the factory automatically and handles the `Application` / `Activity` / `Fragment` scope graph:

```kotlin
// With Hilt
@HiltViewModel
class CoffeeViewModel @Inject constructor(private val coffeeRepo: CoffeeRepository) : ViewModel()

// In composable — factory is auto-generated
val viewModel: CoffeeViewModel = hiltViewModel()
```

**D. Move UI state (form state, selected item) into ViewModel**

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

---

### 12.3 Lower Priority / Polish

**E. Enable `isMinifyEnabled = true` for release builds and configure ProGuard**

Code shrinking and obfuscation are disabled. For any app distributed outside personal use, enable them and test the release build.

**F. Export Room schema**

Change `exportSchema = false` to `exportSchema = true` and commit the generated JSON files to version control. These files act as a diff-able audit trail of every schema change and are required for Room's built-in migration testing utilities.

**G. Add `contentDescription` strings to resource file**

All `Icon` composables use hardcoded English strings for `contentDescription`. These should be moved to `strings.xml` for proper localization support and to make them accessible to screen readers.

**H. `BagDetail` is missing a brews count / brew history link**

The `BAG_DETAIL` bottom sheet shows bag metadata but provides no way to see which brews used that bag. A natural improvement would be a "Brews using this bag" section showing filtered brew items.

**I. No tests**

The project contains only the generated placeholder tests. Consider adding:
- Room DAO tests using `Room.inMemoryDatabaseBuilder` (run on device/emulator)
- ViewModel unit tests using `kotlinx-coroutines-test` and a fake DAO
- Compose UI tests for critical flows using `ComposeTestRule`

---

*This document reflects the codebase as of October 2026. Update it when the schema version increments, new screens are added, or DI is refactored.*
