# 🇲🇾 Slotify — Master Technical & System Documentation

> **Application Name:** **Slotify**
> **Tagline:** *"Susun Waktu Anak, Tenang Sepanjang Hari"* / *"Slotify your day. Plan smarter, slot better."*
> **Platform:** Native Android (Kotlin 2.0.21 & Jetpack Compose, Min SDK 26, Target SDK 35)
> **Package Name / Application ID:** `my.slotify.app`
> **Architecture (current):** Single-module app, Compose screens with direct DAO access (see §2 and §13 for the target MVVM refactor)
> **Data Philosophy:** 🔒 Local-First, Privacy-Focused (user owns all data on-device; personal seed data never committed — see §11)
> **Map & Routing Engine:** OpenStreetMap via `osmdroid` + OSRM Driving API
> **UI Style:** Tactile Neumorphic Soft UI (Dual-Shadow Canvas Shaders + Pastel KPM Palettes)

---

## 📑 Table of Contents
1. [Executive Overview & Value Proposition](#-1-executive-overview--value-proposition)
2. [System Architecture & Tech Stack](#️-2-system-architecture--tech-stack)
3. [Tactile Neumorphic Design System](#-3-tactile-neumorphic-design-system)
4. [Family Operational Model](#-4-family-operational-model)
5. [KPM Academic & Syllabus Engine](#-5-kpm-academic--syllabus-engine)
6. [Logistics, Van Tracking & Parent Chained ETA](#️-6-logistics-van-tracking--parent-chained-eta)
7. [Gamified Star Vault & Parent PIN](#-7-gamified-star-vault--parent-pin)
8. [Canteen Allowance & Fee Management](#-8-canteen-allowance--fee-management)
9. [Room Database Schema & Migrations](#️-9-room-database-schema--migrations)
10. [Dual-Language Architecture (BM / ENG)](#-10-dual-language-architecture-bm--eng)
11. [Family Profile (Private Data Isolation)](#-11-family-profile-private-data-isolation)
12. [Build, Signing & Release](#-12-build-signing--release)
13. [Audit Findings & Fix Plan](#-13-audit-findings--fix-plan)

---

## 🎯 1. Executive Overview & Value Proposition

Malaysian primary school households with children attending **Sekolah Kebangsaan (SK)**, **Sekolah Rendah Agama (SRA/SRAI)**, and **Tadika / PASTI** face significant operational complexity daily:
- Dual school sessions (Morning SRA + Afternoon SK) with transit routines.
- Alternating weekly attire codes (KPM Official, Sports, Uniform Koku, Batik, Baju Melayu).
- Tracking multi-stream school van pickups alongside parent commuting routes.
- Daily bag packing, canteen pocket money, fee deadlines, and motivating kids through gamified rewards.

**Slotify** consolidates schedules, logistics, bag checklists, and gamified star rewards into a single tactile, child-friendly Android application.

---

## 🏛️ 2. System Architecture & Tech Stack

### 2.1 Current architecture (as built)

```mermaid
flowchart TD
    subgraph UI ["Presentation (Jetpack Compose)"]
        M["MainActivity (manual screen switch)"]
        A1["TodayDashboardScreen"]
        A2["TimetableScreen"]
        A3["LogisticsMapScreen"]
        A4["RewardVaultScreen"]
        A5["FinanceScreen"]
        A6["StudentProfileScreen"]
    end

    subgraph Core ["Core helpers (objects)"]
        B1["StarRewards (idempotent claims)"]
        B2["PinManager (PBKDF2 + lockout)"]
        B3["FeeRepository (monthly rollover)"]
        B4["GeofenceManager / OsrmRoutingService"]
        B5["FamilyProfile (demo or private)"]
    end

    subgraph Data ["Data (local-first)"]
        C1["Room: SlotifyDatabase (v6, exported schemas)"]
        C2["SharedPreferences (PIN, POIs, widget, bag flags)"]
        C3["WorkManager + AlarmManager"]
        C4["OSRM HTTP API / OSM tiles"]
    end

    M --> A1 & A2 & A3 & A4 & A5 & A6
    UI --> Core
    UI --> C1
    Core --> Data
```

There are **no ViewModels or use-case classes yet**: screens receive the `SlotifyDao` directly. The refactor to MVVM is tracked as **P4** in §13.

### 2.2 Technical Stack Matrix
- **Language:** Kotlin 2.0.21 (Coroutines & Flow)
- **UI Toolkit:** Jetpack Compose (Material3 + custom neumorphic modifiers)
- **Dependency Injection:** None (manual singletons via `SlotifyApplication` / `SlotifyDatabase.getDatabase`)
- **Local Persistence:** Room 2.6.1 + SQLite, schemas exported to `app/schemas/`
- **Background Tasks:** WorkManager 2.10.0 (widget refresh, geofence monitor) + AlarmManager (daily reminders)
- **Routing & Distance:** OSRM driving API + OkHttp 4.12
- **Map Rendering:** `osmdroid-android:6.1.20`

---

## 🎨 3. Tactile Neumorphic Design System

Neumorphism creates a realistic, extruded physical surface using dual light sources:
- **Specular Glow (Top-Left):** `#FFFFFF` with blur radius matching elevation.
- **Drop Shadow (Bottom-Right):** `#CBD5E1` with soft slate opacity.
- **Base Canvas:** `#EDF2F7` (Soft Pearl Light) / `#141820` (Dark Slate).

### 3.1 Color Palette Tokens (`SlotifyColors`)
| Token Name | Hex Code | Purpose |
|---|---|---|
| `BackgroundLight` | `#EDF2F7` | Main screen background canvas |
| `SurfaceCard` | `#EAF0F8` | Elevated tactile cards and buttons |
| `KpmBlue` | `#0284C7` | Bahasa Melayu & Primary KPM brand color |
| `ScienceGreen` | `#10B981` | Sains, safe geofence arrival, completed tasks |
| `MathAmber` | `#F59E0B` | Matematik, Waktu Rehat, due alerts |
| `IslamTeal` | `#0D9488` | Pendidikan Islam, Bahasa Arab, Jawi, PASTI |
| `EnglishRed` | `#EF4444` | Bahasa Inggeris & urgent alerts |
| `KokuPurple` | `#8B5CF6` | Unit Beruniform, Kokurikulum & Rewards |
| `VanPrimaryBlue` | `#2563EB` | Primary school-van route identifier |
| `VanSecondaryGold`| `#D97706` | Secondary school-van route identifier |
| `ParentGreen` | `#059669` | Parent evening pickup route identifier |

---

## 👨‍👩‍👧 4. Family Operational Model

The routine engine is built around **roles**, not specific people. The concrete names, schools, coordinates and times live only in the private family profile (§11).

```mermaid
sequenceDiagram
    autonumber
    actor Parent as 👨 Parent
    actor VanA as 🚐 Van A
    actor VanB as 🚐 Van B
    actor ChildA as 👦 Child A (dual session)
    actor ChildB as 📚 Child B (integrated)
    actor ChildC as 🧒 Child C (tadika)

    rect rgb(240, 245, 255)
    Note over Parent,ChildC: 🌅 Morning
    VanA->>ChildA: Home ➜ morning SRA session
    VanB->>ChildB: Home ➜ integrated school (full day)
    Parent->>Parent: Drop transit bags at daycare
    Parent->>ChildC: Drop at tadika ➜ work
    end

    rect rgb(255, 248, 240)
    Note over ChildA: ☀️ Midday shift
    ChildA->>ChildA: SRA ends ➜ transit daycare (lunch, change uniform)
    VanA->>ChildA: Daycare ➜ afternoon SK session
    end

    rect rgb(245, 255, 245)
    Note over Parent,ChildC: 🌆 Evening
    ChildB->>ChildB: School ends ➜ daycare
    Parent->>ChildC: Office ➜ pick up at tadika
    Parent->>ChildB: Pick up at daycare ➜ home
    VanA->>ChildA: SK ends ➜ home
    end
```

---

## 📚 5. KPM Academic & Syllabus Engine

### 5.1 Weekly Attire Resolver
- **Monday:** Official KPM Uniform (Kemeja Putih + Seluar Biru / Baju Kurung).
- **Tuesday:** Sports T-Shirt (Baju Rumah Sukan / PJK).
- **Wednesday:** Full Kokurikulum Uniform (Pengakap, TKRS, Puteri Islam, etc.).
- **Thursday:** Koku T-Shirt / Official School Batik.
- **Friday:** Baju Melayu Putih Lengkap (Samping & Songkok) / Baju Kurung.

### 5.2 School-day convention
`TimetableSlotEntity.dayOfWeek` uses **1 = Isnin … 5 = Jumaat**. Weekend handling is centralised in `core/time/SchoolDay.kt`:
- On Saturday/Sunday there is **no "today" timetable**.
- "Next school day" from Friday, Saturday or Sunday is **Monday** (used by the bag checklist).

### 5.3 Session inversion (Year 4 ➜ Year 5)
A student may have several `SchoolSessionEntity` rows (e.g. SRA pagi + SK petang). The timetable's inversion toggle cycles the active session; a year-level change swaps which session runs in the morning.

### 5.4 ICS import
`IcsParser` converts a school-issued `.ics` export into weekly `TimetableSlotEntity` rows (de-duplicated by day/start/subject). ICS files are supplied by the family profile from `assets/` so a specific class timetable is not committed.

---

## 🗺️ 6. Logistics, Van Tracking & Parent Chained ETA

- **Map:** osmdroid with OpenStreetMap tiles; © OpenStreetMap contributors attribution is shown on the map.
- **Routing:** OSRM driving API. On network failure the UI shows an explicit *"Anggaran luar talian"* (offline estimate) state — it never presents a fabricated route as live.
- **Routes:** three route slots — `PARENT_PICKUP`, `VAN_PRIMARY`, `VAN_SECONDARY`. Their labels, waypoints (POI ids), driver contacts and step notes come from the family profile.
- **Geofencing:** `GeofenceManager` evaluates distance to each POI with a per-target state machine (`NONE → APPROACHING → ENTERED → INSIDE → EXITED`), separate cooldowns per event type and persisted state. Background evaluation only runs if `ACCESS_BACKGROUND_LOCATION` has been granted.
- **Usage policies:** the public OSRM demo server and OSM tile servers are rate-limited community services. Bulk offline tile download is **not** offered (it violates the OSM tile usage policy); tiles are cached passively as they are viewed. A self-hosted OSRM instance is recommended before any wide distribution.

---

## 🌟 7. Gamified Star Vault & Parent PIN

### 7.1 Star earning — every award is idempotent
All star awards go through `StarRewards.claimOnce(dao, studentId, claimKey, stars)`, which inserts a row into `reward_claims` and credits the balance **in one transaction**. A second claim with the same key is a no-op, so toggling a checkbox or reopening a dialog can never mint extra stars.

| Action | Stars | Claim key (one-time scope) |
|---|---|---|
| Complete a homework task | task's `rewardStars` (5–10) | `homework:<taskId>` (once per task, ever) |
| Pack school bag (dashboard button or checklist) | `+5` | `bag:<studentId>:<date>` (once per day) |
| Daily mystery chest | `+15` | `chest:<studentId>:<date>` (once per day) |
| Convert RM 10 canteen savings | `+10` | `savings:<studentId>:<n>` — also deducts RM 10 from Tabung Maya |
| Record a PBD result TP5 / TP6 | `+15` | `pbd:<studentId>:<subject>:<period>` |
| Record a PBD result TP1–TP4 | `+5` | `pbd:<studentId>:<subject>:<period>` |

### 7.2 Redemption
Redemption uses a single conditional SQL update (`… SET starCoinsBalance = starCoinsBalance - :cost WHERE id = :id AND starCoinsBalance >= :cost`). The success dialog is only shown when that update affected a row.

### 7.3 Parent PIN (`core/security/PinManager.kt`)
- **No default PIN.** On first use the parent is asked to create a 4–6 digit PIN (entered twice).
- Stored as **PBKDF2-HMAC-SHA256** with a random 16-byte salt.
- Failed attempts and lockout deadline are **persisted**, so closing the dialog does not reset them. Lockout escalates: 3 failures ➜ 30 s, 6 ➜ 2 min, 9 ➜ 10 min, then 30 min per further 3 failures.
- The PIN can be changed from the Reward Vault (requires the current PIN).

### 7.4 Reward Catalog (defaults)
- **1 Hour Roblox / iPad:** `50 ⭐`
- **Buy Toy / Hot Wheels:** `200 ⭐`
- **Ice Cream Outing:** `30 ⭐`
- **Stay Up 30 Mins Later (Saturday Night):** `20 ⭐`

---

## 💰 8. Canteen Allowance & Fee Management

### 8.1 Daily canteen flow
- Each student has a `dailyCanteenBudget`. Spending is stored per student per day in `daily_spending`.
- **Tabung Maya** = Σ max(0, budget − spent) over all recorded days − RM 10 × (number of savings conversions). There is no synthetic starting balance.

### 8.2 Recurring fees
- A fee row belongs to one `billingMonth` (`yyyy-MM`). Its `seriesId` links the same fee across months and `isRecurring` marks monthly fees.
- When a month is opened for the first time, `FeeRepository.ensureMonth()` copies every recurring fee from the most recent earlier month into the new month as **unpaid** (`id = "<seriesId>_<yyyy-MM>"`).
- Seed fees are created for the **current** month at first launch (never a hard-coded month).

---

## 🗄️ 9. Room Database Schema & Migrations

- **Current version:** 6. Schemas are exported to `app/schemas/my.slotify.app.core.database.SlotifyDatabase/` and must be committed.
- **Migrations:** `SlotifyMigrations.MIGRATION_5_6` adds `reward_claims` and the fee `seriesId` / `isRecurring` columns. Destructive fallback is only allowed from pre-release versions 1–4.
- **Never use `OnConflictStrategy.REPLACE` on parent tables** (`students`, `school_sessions`): SQLite implements REPLACE as delete + insert, which fires `ON DELETE CASCADE` and wipes children. Parent writes use `@Upsert`; seeding uses `INSERT OR IGNORE`.

```mermaid
erDiagram
    STUDENT ||--o{ SCHOOL_SESSION : has
    SCHOOL_SESSION ||--o{ TIMETABLE_SLOT : contains
    STUDENT ||--o{ LOCATION_NODE : mapped_to
    STUDENT ||--o{ HOMEWORK_TASK : assigned
    STUDENT ||--o{ EXAM : sits
    STUDENT ||--o{ PBD_ASSESSMENT : assessed
    STUDENT ||--o{ DAILY_SPENDING : spends
    STUDENT ||--o{ REWARD_CLAIM : earns

    STUDENT {
        string id PK
        string fullName
        string nickname
        int starCoinsBalance
        double dailyCanteenBudget
    }
    SCHOOL_SESSION {
        string id PK
        string studentId FK
        string sessionTimeType
        string startTime
        string endTime
    }
    TIMETABLE_SLOT {
        string id PK
        string sessionId FK
        int dayOfWeek
        int periodIndex
        string subjectCode
    }
    COMMITMENT_FEE {
        string id PK
        string seriesId
        string billingMonth
        boolean isRecurring
        boolean isPaid
        double amountRm
    }
    REWARD_CLAIM {
        string claimKey PK
        string studentId
        int stars
        long claimedAt
    }
```

---

## 🌐 10. Dual-Language Architecture (BM / ENG)

- **In-app strings:** `SlotifyStrings.kt` exposes `strings.*` via `LocalAppLanguage`; switching language recomposes without restart.
- **Android resources:** `res/values/strings.xml` (BM) and `res/values-en/strings.xml` (EN) follow the **device** locale, not the in-app switch.
- **Known gap:** many BM literals are still hard-coded in Kotlin, and two systems coexist. Consolidation is tracked as **P5-3**.

---

## 🔐 11. Family Profile (Private Data Isolation)

All family-specific data — children's names, schools, classes, home/office coordinates, driver contacts, routines, fee templates, ICS timetables — is provided by a single object, `FamilyProfileData`, implementing `core/profile/FamilyProfile`.

| Source directory | Committed? | Used when |
|---|---|---|
| `app/family-profile/demo/` | ✅ Yes — fictional family | `app/family-profile/private/` does not exist (fresh clones, CI) |
| `app/family-profile/private/` | ❌ No — `.gitignore`d | Present on the family's own machine |

`app/build.gradle.kts` adds exactly one of these directories to the `main` source set (`java/` and `assets/`). Pass `-PslotifyProfile=demo` to force the demo profile even when a private one exists (useful to check the committed build). `FamilyProfileConsistencyTest` validates whichever profile is compiled in. To build with your own family:
1. Copy `app/family-profile/demo/` to `app/family-profile/private/`.
2. Edit `private/java/my/slotify/app/core/profile/FamilyProfileData.kt` and place any ICS files in `private/assets/`.
3. **Back the private folder up separately** — it is intentionally not in git.

Stable IDs (e.g. `student-…`, `session-…`) must not change between releases, because existing installs reference them.

---

## 📦 12. Build, Signing & Release

- **Signing:** release signing reads `keystore.properties` in the repo root (git-ignored; keys `storeFile`, `storePassword`, `keyAlias`, `keyPassword`) or the environment variables `SLOTIFY_KEYSTORE_FILE`, `SLOTIFY_KEYSTORE_PASSWORD`, `SLOTIFY_KEY_ALIAS`, `SLOTIFY_KEY_PASSWORD`. There are **no password fallbacks** in the build script; without credentials the release build is signed with the debug key and must not be published.
- **R8:** enabled for release. Libraries ship their own consumer rules, so `proguard-rules.pro` only keeps app-specific reflection targets.
- **Checks:** `./gradlew testDebugUnitTest lintDebug` must pass before tagging a release.

---

## 🩺 13. Audit Findings & Fix Plan

Audit date: **2026-11-14** (v1.1.0, DB v5). Baseline: 31/31 unit tests passing; `lintDebug` failing with 5 errors / 111 warnings.

Status legend: ✅ Done · 🔄 In progress · ⏳ Planned

### P1 — Security & privacy (critical)
| ID | Finding | Fix | Status |
|---|---|---|:---:|
| P1-1 | Children's names, schools, home/office coordinates & address, car plate, phone numbers and daily location routine hard-coded in source (and already on `origin/main`) | Move all family data into `FamilyProfile`; commit a fictional demo profile; git-ignore the private profile; purge history before the next push | 🔄 |
| P1-2 | Keystore password fallback `"slotify2026"` committed in `app/build.gradle.kts` | Read credentials from `keystore.properties` / env only | ✅ |
| P1-3 | PIN defaults to `1234`, `setPin()` never called, lockout held in `remember`, unsalted SHA-256 | First-run PIN setup, PBKDF2 + salt, persisted escalating lockout, change-PIN flow | ✅ |
| P1-4 | Unlimited star farming (homework re-toggle, daily chest, bag pack ×2, savings conversion never deducts) | `reward_claims` table + `StarRewards.claimOnce`; savings conversions deduct from Tabung | ✅ |
| P1-5 | Redemption success shown even when balance check fails | Atomic conditional SQL spend; show success only on success | ✅ |
| P1-6 | Widget receiver exported with a custom action any app can send | Explicit-component broadcast only; drop custom action from the intent filter | ✅ |

### P2 — Data safety
| ID | Finding | Fix | Status |
|---|---|---|:---:|
| P2-1 | `REPLACE` on `students` / `school_sessions` cascade-deletes children | `@Upsert` for parents; `INSERT OR IGNORE` for seeding | ✅ |
| P2-2 | `fallbackToDestructiveMigration()` + `exportSchema = false` | Export schemas, add `MIGRATION_5_6`, destructive only from v1–4 | ✅ |
| P2-3 | Seeder runs twice concurrently and re-`REPLACE`s homework on every launch (resets completion) | One seeding path, versioned, insert-or-ignore | ✅ |
| P2-4 | Lint errors: `MissingPermission` ×3 (notifications), `RemoteViewLayout` ×2 (widget `<View>`) | Permission checks before `notify`; widget-safe views | ✅ |

### P3 — Correctness
| ID | Finding | Fix | Status |
|---|---|---|:---:|
| P3-1 | Bag checklist built from **today's** slots | Pass next-school-day slots | ✅ |
| P3-2 | Saturday/Sunday mapped to Monday (dashboard, timetable, ICS, widget) | `SchoolDay` helper; explicit weekend handling | ✅ |
| P3-3 | Timetable "live" tracker uses a fake 08:15 clock | Real clock ticking each minute | ✅ |
| P3-4 | Fees seeded for `"2026-09"` only; monthly fees never roll over | `seriesId` / `isRecurring` + `FeeRepository.ensureMonth()` | ✅ |
| P3-5 | Fake `RM 25.00` starting savings | Removed | ✅ |
| P3-6 | "Rekod TP" button never shows its dialog | Wire `AddPbdAssessmentDialog` | ✅ |
| P3-7 | Exam dates parsed with device locale (`Okt` fails on EN devices) | Parse with `ms-MY` then English | ✅ |
| P3-8 | Geofence: shared cooldown suppresses "arrived"; exit fires "approaching"; state lost on process death | Per-event cooldown, exit handling, persisted state | ✅ |
| P3-9 | Background geofence / widget GPS without background-location permission | Skip location work unless permission granted; schedule-based fallback | ✅ |
| P3-10 | OSRM failure returns a fabricated route labelled "OSRM Live" | Return failure; show explicit offline-estimate state | ✅ |
| P3-11 | Van simulation fires every proximity alert at start | Alerts driven by remaining route distance; labelled as simulation | ✅ |
| P3-12 | `MapView` never receives lifecycle callbacks / `onDetach` (leaks) | Lifecycle observer + `onRelease` | ✅ |
| P3-13 | Location request asks only `FINE` (ignored on Android 12+); stale permission state | Request `FINE` + `COARSE`; re-evaluate on resume | ✅ |
| P3-14 | Alarm notifications hard-code child names; alarms lost after reboot | Build content from DB; `BOOT_COMPLETED` re-scheduler | ✅ |
| P3-15 | Selected student overwritten by stale async reloads | Track selected **id**, derive entity from the students Flow | ✅ |
| P3-16 | Bulk offline tile download violates OSM policy (and is blocked by osmdroid for OSM tiles) | Remove bulk download; rely on passive tile cache; add OSM attribution | ✅ |

### P4 — Architecture (next phase)
| ID | Finding | Fix | Status |
|---|---|---|:---:|
| P4-1 | No ViewModels; DAO and business logic in composables; 1,000–2,700-line files | Feature ViewModels + repositories exposing `StateFlow` | ⏳ |
| P4-2 | Navigation Compose declared but unused; Back always exits | `NavHost` + `BackHandler` for overlays | ⏳ |
| P4-3 | Logistics waypoint resolution duplicated 3×; overlays rebuilt every recomposition | Single route resolver; diff overlays | ⏳ |
| P4-4 | Money stored as `Double` | Store sen as `Long` (migration) | ⏳ |
| P4-5 | Theme / language not persisted | DataStore-backed settings | ⏳ |

### P5 — Quality
| ID | Finding | Fix | Status |
|---|---|---|:---:|
| P5-1 | Unit tests assert re-implemented copies of logic, not production code | Extract pure functions and test them; add Room migration test | 🔄 |
| P5-2 | ProGuard keeps all of Compose/osmdroid; `org.json` duplicated as a runtime dependency | Trim keep rules; `org.json` → `testImplementation` | ✅ |
| P5-3 | Two localisation systems + hundreds of hard-coded BM strings | Consolidate on one system | ⏳ |
| P5-4 | Touch targets < 48 dp, 7–9 sp text, missing toggle semantics | Accessibility pass | ⏳ |
| P5-5 | No CI, outdated dependencies, README version badge stale | GitHub Actions running tests + lint; dependency bump | ⏳ |

### Progress log
**2026-11-14 — P1, P2, P3 implemented.** Verified with `./gradlew testDebugUnitTest lintDebug` (private profile) and `./gradlew -PslotifyProfile=demo testDebugUnitTest assembleRelease`:
- Unit tests: **49/49 passing** (was 31). New suites test production code: `PinHasherTest`, `SchoolTimeTest` (school days, exam dates, ICS), `FinanceTabungTest` (Tabung balance, fee rollover, reward keys), `GeofenceManagerTest` (state machine), `OsrmRoutingServiceTest` (parsing, failure → null), `FamilyProfileConsistencyTest` (profile integrity).
- Lint: **0 errors** (was 5); 99 warnings remain (typos, unused resources, dependency versions).
- Release (R8) APK builds: ~2.4 MB with the trimmed keep rules.
- `SlotifyMigrationTest` (Room 5 → 6) compiles but needs a device: `./gradlew connectedDebugAndroidTest`.

Still open:
- **P1-1:** history rewritten locally on 2026-11-14 with `git filter-repo` (names, places, coordinates, phone/MyKid numbers replaced in every revision and commit message; ICS timetables, screenshots and UI dumps removed). **Force-push to GitHub still pending.**
- **P3-12 follow-up:** the map still lives inside a `LazyColumn` item; overlays are now released correctly, but moving it out is part of P4-3.
- **P3-9 follow-up:** background location is not requested; geofence/widget GPS stays off until a deliberate opt-in flow is built.

### Execution order
1. **P1 + P2** — privacy, security and data-loss fixes (blocking any further push).
2. **P3** — correctness bugs.
3. **P4** — architecture refactor, feature by feature (Rewards ➜ Finance ➜ Dashboard ➜ Timetable ➜ Logistics).
4. **P5** — quality and tooling.

### History purge (P1-1, manual step)
Personal data already exists in pushed commits. After the code fix is committed, rewrite history (e.g. `git filter-repo` with a replace-text file covering names, coordinates and phone numbers), force-push, and ask GitHub support to purge cached views if the repository was ever public. Rotate the release keystore if its password was ever used for a real key.
