# HealthMate – Design Specification

HealthMate is a personal health & wellness tracker for Android, built as university coursework
(Mobile Application Development, Coursework 1). Users record their daily steps, calories burned
and water intake, then review, search, edit and delete their records. All data is stored locally
and works fully offline.

---

## 1. Technology & Architecture

| Area | Choice |
|---|---|
| Platform | Native Android |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (Model – View – ViewModel) |
| State management | ViewModel + Kotlin StateFlow (UI collects state; one-way data flow) |
| Local database | Room (SQLite). Lecturer confirmed any compatible embedded/local database may be used instead of sqflite. |
| Navigation | Navigation Compose |
| Images | Android Photo Picker (no permission needed); Coil for loading images |
| Async | Kotlin Coroutines + Flow |

### Package structure (feature-based)

```
com.example.healthmate
├── data
│   ├── local        → HealthRecord entity, HealthRecordDao, HealthMateDatabase, seed data
│   └── repository   → HealthRecordRepository
├── domain           → validation rules, filter/sort models, summary calculations
├── ui
│   ├── theme        → colours, typography, shapes
│   ├── components   → reusable composables (cards, buttons, dialogs, inputs, empty state)
│   ├── navigation   → nav graph and routes
│   ├── dashboard    → DashboardScreen + DashboardViewModel
│   ├── records      → AllRecordsScreen + AllRecordsViewModel
│   ├── addedit      → AddEditRecordScreen + AddEditRecordViewModel
│   ├── detail       → RecordDetailScreen + RecordDetailViewModel
│   └── search       → FilterBottomSheet, SearchResultsScreen + SearchViewModel
└── MainActivity
```

Rules:
- Screens never talk to the DAO directly: Screen → ViewModel → Repository → DAO.
- ViewModels expose a single immutable UI state (`StateFlow<UiState>`) plus event functions.
- The DAO returns `Flow` so every screen updates automatically when the database changes.

---

## 2. Database

Table name: **`health_records`**

| Field | Type | Rules |
|---|---|---|
| `id` | INTEGER | Primary key, auto-generated |
| `date` | TEXT | Format `yyyy-MM-dd` (sorts correctly as text). Unique – one record per date. |
| `steps` | INTEGER | Steps walked |
| `calories` | INTEGER | Calories burned (kcal) |
| `water` | INTEGER | Water intake in ml |
| `photo_path` | TEXT, nullable | **Extra feature.** Path to an optional selfie stored in the app's private storage. |
| `updated_at` | INTEGER, nullable | When the record was last saved or updated (epoch milliseconds). Shown on Record View (4.5). Null for seed records. Added in database version 2. |

Required DAO operations:
- **Create:** insert a record.
- **Read:** get all records (newest first), get by id, get by date, get records in a date range.
- **Update:** update a record.
- **Delete:** delete a record (also delete its photo file, if any).
- **Summary:** totals of steps, calories and water for a date range.
- **Search:** records between a start and end date, with sort order.

### Dummy data
On first launch, seed about **90 days** of realistic records ending yesterday, so the
1 Day / 7 Days / 30 Days / 3 Months filters show different totals.
Realistic ranges: steps 3,000–14,000; calories 150–700; water 1,200–3,200 ml.
Seed records have no photo. Seeding happens only once (when the database is first created).

---

## 3. Design System

The app should look like a modern, minimalist, professional commercial health app rather than
a basic student project.

### Colours
| Role | Colour |
|---|---|
| Background | White `#FFFFFF`; surfaces/cards white or very light grey `#F7F8FA` |
| Primary | Blue `#1E5EFF` – main buttons, selected filters, app bar accents |
| Secondary | Yellow `#FFC83D` – highlights, secondary accents, selected chips |
| Text | Near-black `#1A1C1E`; secondary text grey `#6B7280` |
| Steps stat | Green `#22A06B` |
| Calories stat | Red `#E5484D` |
| Water stat | Blue `#2E90FA` |
| Error | Red `#D92D20` |
| Success | Green `#12B76A` |

Keep the palette small. Stat colours are used only on icons, stat cards and indicators.

### Style
- Modern typography with clear hierarchy (large bold numbers on stat cards, clear section titles).
- Generous, consistent spacing (8 dp grid). Rounded corners (12–16 dp). Soft shadows, subtle borders.
- Material icons: steps = `DirectionsWalk`, calories = `LocalFireDepartment`, water = `WaterDrop`.
- **Primary buttons** (filled blue): Add New Record, Save Record, Update Record.
- **Secondary buttons** (outlined): All Records, Cancel, Reset, Edit.
- **Destructive actions** (red text/outline): Delete.
- No overcrowded screens, unnecessary decorations or excessive colours.

### Reusable components
`StatCard`, `RecordCard`, `PrimaryButton`, `SecondaryButton`, `PeriodFilterChips`,
`NumberInputField`, `DatePickerField`, `PhotoPickerBox`, `ConfirmDeleteDialog`, `EmptyState`,
`AppTopBar`.

---

## 4. Screens

### 4.1 Dashboard (Home)
- **Header:** HealthMate logo + app name on the left. Two clear buttons: **Add New Record** (primary)
  and **All Records** (secondary).
- **Greeting/date line:** e.g. "Today – 19 November 2026".
- **Summary section:** period chips **1 Day | 7 Days | 30 Days | 3 Months** (default 1 Day = today).
  Three stat cards: Total Steps, Total Calories Burned, Total Water Intake (with units and stat colours).
  If there is no record for today, show a friendly prompt to add one.
- **Search:** one search bar row with a **Filters** button that opens the Filter bottom sheet (4.6).
- **Recent Records:** the 5 most recent records as cards; tapping opens Record View.
  A "See all" link opens All Records.

### 4.2 All Records
- All records, **newest first**, as clean cards showing date, steps, calories, water and a photo
  thumbnail if available.
- Tap a card → Record View. Each card has Edit and Delete actions (Delete asks for confirmation).
- Empty state when there are no records, with an Add New Record button.

### 4.3 Add New Record
Form fields:
- **Date** (date picker; defaults to today)
- **Steps walked**
- **Calories burned (kcal)**
- **Water intake (ml)**
- **Selfie photo (optional)** – pick with the Photo Picker; show a preview; can be removed.
  The chosen image is copied into the app's private storage and its path saved in `photo_path`.

**Save Record** button (primary). On success: show a "Record saved" message, return to the previous
screen, and all screens refresh automatically.

### 4.4 Edit Record
- Same form as Add, pre-filled with the existing record.
- The user can change date, steps, calories, water, and replace or remove the selfie.
- **Update Record** button (primary). On success: "Record updated" message and screens refresh.

### 4.5 Record View (Detail)
- Shows the date (formatted nicely, e.g. "Wed, 19 Nov 2026"), selfie photo if available,
  steps, calories and water in stat cards, and when the record was last updated if available.
- Three actions: **Add New**, **Edit**, **Delete** (with confirmation dialog).

### 4.6 Filter bottom sheet
- **Date range:** start date and end date (end date cannot be before start date).
- **Sort by:** Newest first (default), Oldest first, Highest steps, Highest calories, Highest water.
- Buttons: **Apply** (primary) and **Reset** (secondary – clears all filters).
- Apply opens the Search Results screen.

### 4.7 Search Results
- Top: a summary of applied filters as chips (e.g. "1 Nov – 15 Nov 2026", "Highest steps"),
  with a way to change or clear them.
- Below: matching records as cards; tap → Record View; Edit and Delete available.
- If nothing matches: "No records found." with a Reset filters button.

---

## 5. Validation & Error Handling

| Field | Rule | Error message |
|---|---|---|
| Date | Required; cannot be in the future | "Please choose a date" / "Date can't be in the future" |
| Date | Only one record per date | "A record for this date already exists" + option to edit that record |
| Steps | Required; whole number 0–100,000 | "Enter steps between 0 and 100,000" |
| Calories | Required; whole number 0–10,000 | "Enter calories between 0 and 10,000" |
| Water | Required; whole number 0–10,000 ml | "Enter water between 0 and 10,000 ml" |
| Date range | End date not before start date | "End date must be after start date" |

- Errors appear under the related field; the Save/Update button stays disabled or shows errors when tapped.
- Use number keyboards for numeric fields.
- Show success messages (Snackbar) after save, update and delete.
- Delete always asks for confirmation: **Delete** (permanently removes the record) or **Cancel**.
- Handle empty states on every list screen.
- Database errors are caught and shown as a friendly message instead of crashing.

---

## 6. Navigation

```
Dashboard ──► Add New Record
    │──────► All Records ──► Record View ──► Edit Record
    │──────► Record View (from Recent Records)
    └──────► Filter sheet ──► Search Results ──► Record View
```
- Back navigation always returns to the previous screen.
- After Save/Update, return to the previous screen. After Delete from Record View, return to the list.

---

## 7. Testing

- **Unit tests:** form validation rules, summary calculations, ViewModel state changes.
- **Room DAO tests (instrumented):** insert, read, update, delete, date-range search and sort order.
- Test cases are documented in the report with: Test ID, Scenario, Input, Expected Result,
  Actual Result, Status.

---

## 8. Version Control

- Repository on GitHub (must be **public** for submission).
- Small, meaningful commits after each feature, spread across the development days.
- Commit messages describe the change, e.g. "Add Room entity and DAO for health records".

---

## 9. Build Plan (5 days)

| Day | Work |
|---|---|
| 1 | Project rules, Room database + repository + seed data, theme, reusable components |
| 2 | Navigation, Dashboard (summary filters, recent records, search bar) |
| 3 | Add / Edit form with validation and selfie, Record View, delete with confirmation |
| 4 | All Records, Filter bottom sheet, Search Results, empty states, auto refresh |
| 5 | Tests, bug fixes, app icon, signed release APK |
