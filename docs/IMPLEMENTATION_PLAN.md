# v1 Implementation Plan

v1 is a fully offline Android app that logs workouts, shows what you lifted last time, detects PRs, and exports your data. It has no backend. Each phase ends with something installable on your phone.

## Stack and conventions

| Area | Choice |
| --- | --- |
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Architecture | Single activity, Compose Navigation, ViewModel + StateFlow, repository layer |
| Storage | Room (SQLite), KSP for codegen |
| DI | Manual `AppContainer` (swap for Hilt later only if it gets painful) |
| Background | `kotlinx.coroutines`, a foreground service for the rest timer |
| Charts (v1.2) | Vico |
| Build | Gradle Kotlin DSL with a version catalog (`libs.versions.toml`) |
| CI | GitHub Actions: lint, unit tests, debug APK on every push |
| Min / target SDK | 26 (Android 8.0) / latest stable |
| Package | `io.github.prithvimk.gymtracker` |
| Units | Stored in kg; display unit is a setting (kg default) |
| IDs | UUID strings plus `createdAt` / `updatedAt` on every row, so sync can be added later |

## Data model

```
Exercise        id, name, muscleGroup, equipment, isCustom, archived, createdAt, updatedAt
Routine         id, name, notes, createdAt, updatedAt
RoutineExercise id, routineId, exerciseId, position, targetSets, repMin, repMax, restSeconds
Workout         id, routineId?, startedAt, endedAt?, notes, createdAt, updatedAt
WorkoutExercise id, workoutId, exerciseId, position, notes
WorkoutSet      id, workoutExerciseId, position, weightKg, reps, rpe?, type (WARMUP|WORKING|DROP|FAILURE), completedAt?
PersonalRecord  id, exerciseId, type (WEIGHT|REPS_AT_WEIGHT|E1RM|VOLUME), value, setId, achievedAt
```

Every table also has `createdAt` / `updatedAt`. Exercises are archived, never deleted.

Estimated 1RM uses Epley: `weight × (1 + reps / 30)`, counted only for working sets of 1 to 12 reps.

## Phase 0: Skeleton and CI

Goal: an empty app that builds locally and in CI.

- [x] Android Studio project with Compose, Material 3, version catalog, KSP
- [x] Package structure: `data/` (db, dao, repository), `domain/` (models, PR logic), `ui/` (screens, components, theme)
- [x] Bottom navigation shell: Workout, History, Exercises, Settings (placeholder screens)
- [x] GitHub Actions workflow: `./gradlew lint testDebugUnitTest assembleDebug`, upload debug APK as an artifact
- [x] `.editorconfig` and ktlint via Gradle plugin

Done when: CI is green and the debug APK installs and opens on your phone.

## Phase 1: Data layer and exercise library

Goal: everything persists, with a usable list of exercises.

- [x] Room entities, DAOs and the database for the model above, with `exportSchema = true` and schemas committed
- [x] Repositories exposing `Flow`s
- [x] Seed about 60 common exercises (barbell, dumbbell, machine, cable, bodyweight) from a bundled JSON on first launch (69 shipped; ids derive from each entry's key)
- [x] Exercises screen: search, filter by muscle group, create, edit and archive custom exercises
- [x] DAO tests with an in-memory database (Robolectric)

Done when: you can add a custom exercise, kill the app, and it is still there.

## Phase 2: Workout logging (the core loop)

Goal: log a real gym session faster than a notes app.

- [x] Start an empty workout; one active workout at a time, resumable after the app is killed
- [x] Add exercises, reorder them, remove them
- [x] Per set: weight, reps, optional RPE, set type, tap to mark complete
- [x] "Last time" column: the matching set from the previous session of that exercise, and new sets prefilled from it
- [x] Quick steppers (+2.5 kg, +1 rep) and a numeric keypad that stays open between fields
- [x] Exercise notes and workout notes
- [x] Finish workout: summary of duration, sets and total volume; discard with confirmation
- [x] Unit tests for "previous set" lookup and volume maths

Done when: you log two real sessions at the gym and the second one shows the first one's numbers.

## Phase 3: Routines and rest timer

Goal: start a planned session in one tap and rest on time.

- [x] Create and edit routines: ordered exercises with target sets, rep range and rest time (schema v2: workout exercises keep the routine's rep range and rest)
- [x] Start workout from a routine; target sets are created with "last time" values filled in
- [x] "Save as routine" from a finished workout
- [x] Rest timer that starts when a set is completed; per-exercise default, +/-15 s controls
- [x] Foreground service with a countdown notification and vibration at zero, working with the screen off (a `specialUse` service holding a wake lock until the rest ends)
- [x] Notification permission request on Android 13+

Done when: a full Push/Pull/Legs week runs from routines with the phone locked between sets.

## Phase 4: History and PRs

Goal: see your past work and get rewarded for beating it.

- [x] History list grouped by week, each card showing routine name, duration, volume and PR count
- [x] Workout detail screen; edit or delete a past workout (PRs recomputed after edits) (opened from a History card; editing reuses the logger, and a set ticked afterwards counts as done when the workout ended)
- [x] Month calendar with trained days highlighted (a List / Calendar switch on History; tapping a trained day lists that day's workouts)
- [x] PR detection on set completion: heaviest weight, most reps at a weight, best e1RM, best session volume (recomputed per exercise from its whole history after any set change; each session keeps only its best set per record type)
- [x] PR badge in the logger and a short celebration on the finish summary (session volume shows only on the summary, since it moves to each newly ticked set)
- [x] Exercise detail screen: PR list and full history for that exercise (opened from the Exercises tab; editing moved behind its edit button)
- [x] Unit tests for PR detection, including edits and deletes

Done when: beating last week's top set shows a PR in the session and on the exercise screen.

## Design pass (D1–D5, before Phase 5)

Goal: one coherent look, and a logger that gives the sets the screen. These phases come from the [design review](https://claude.ai/artifact/3AGT69E2zHpuw3dgz3YeHk) of Oct 2026; finding numbers (F01–F21) refer to it. They run before Phase 5 so that Phase 5's JSON export covers the one schema change here, and its README screenshots show the final design. D1 goes first, since every later phase uses its colours, formatters and shared components; D2–D5 can then go in any order.

### Design decisions

Settled while planning, to keep the picks from contradicting each other:

- **Record colours ignore dynamic colour.** Record gold is an app-defined extended colour family (record, onRecord, recordContainer, onRecordContainer) provided beside `MaterialTheme`, not the tertiary role, so it stays gold when Phase 5's dynamic colour toggle is on.
- **Dynamic colour is off from D1 on.** The Settings toggle to turn it back on arrives in Phase 5, so until then every phone shows the brand scheme.
- **One formatter for weights and durations.** Every weight on screen goes through it from D1 on, so Phase 5's kg/lb setting is a one-place change. The keypad's step labels ("+2.5 kg") read the weight increment, which becomes a setting in Phase 5.
- **RPE opt-in is a schema change.** It is stored per library exercise (schema v3), so it ships with a migration and a migration test, and lands before Phase 5's full-fidelity export.
- **Sparkline without a chart library.** The exercise detail's e1RM sparkline (D4) is drawn with `Canvas`; Vico comes with the full charts in v1.2.
- **Exercise detail opens under the current tab.** It gets a second route under History, so opening it from a workout's detail keeps History selected and Back returns there.
- **The mini bar gives way.** It hides while the keypad or the keyboard is up, and the Exercises tab's floating button moves above it.
- **Rep range in the routine editor (F15).** The collapsed row reads "3 × 6–8 · rest 3:00"; expanding it shows separate steppers for Sets, Reps from, Reps to and Rest.
- **Tapping a routine row opens the editor (F09),** as today; nothing changes until you save. The play button starts the routine, and the ⋮ menu holds Edit and Delete.
- **One title style on all tabs (F17):** a large title that collapses into a small top bar as the list scrolls, with actions on the right (History's calendar toggle) and Exercises' search bar under its title.
- **Reordering is drag and drop everywhere (F15).** A small Compose drag-and-drop list library powers the routine editor, and the logger gets a "Reorder exercises" menu entry that opens the same compact draggable list.
- **Roboto with tabular figures (F03).** No bundled display face for now; revisit a condensed one (about 100 KB) after D2.
- **Rest and elapsed time share one top-bar chip; tap the title to peek (F04 + F06, F22).** Outside rest the chip shows the elapsed time. During rest it shows only the countdown, and tapping the routine title spins it round to the elapsed time for 5 seconds. A split pill showing both was ruled out: at phone width it left one letter of the title.
- **Finishing can be undone.** Finish sits beside the chip and today ends the workout at once, so the finish summary gets a Resume workout button that reopens the session.

## Phase D1: Design foundations

Goal: the whole app in one green scheme, with records in gold and numbers that read unambiguously.

- [x] Full light and dark colour schemes generated from `#2E6B30`, defining every role including all surface containers; `dynamicColor` defaults to off (F01) (Theme Builder tonal mapping, primary pinned to the brand colour)
- [x] Record colour family as an extended colour; every PR mark (logger badge, History card, exercise detail, records card) moves off tertiary to gold (F02) (`MaterialTheme.recordColors`; the calendar's selected-day ring moved off tertiary too)
- [x] Shared formatter: finished durations in words ("58 min", "1 h 11 min"), clock format only for live timers; every weight goes through it (F03) (`ui/format`; durations round to the nearest minute, and set fields still edit raw kg until Phase 5's unit setting)
- [x] Tabular figures for timers, set fields, the Last column and totals (F03)
- [x] Shared components the later phases reuse: stat tile, gold record chip, gold number disc (`ui/components/RecordMarks.kt`; the chip is on History cards already)

Done when: no lavender, purple or pink is left on any screen in light or dark, and History shows "58 min" instead of "58:00".

## Phase D2: Workout logger

Goal: during a session the set table gets the screen, and the rest timer stays out of the way.

- [ ] Compact top bar: routine name and elapsed time, a filled Finish button, and a menu with Workout notes, Add exercises and Discard; the bottom footer goes away (F04)
- [x] Quiet set table: borderless filled fields, done rows with a light tint, and a filled done check with a 48 dp touch target (F05) (an undone set shows an empty outlined box, so done is not shown by colour alone)
- [x] PR mark: the set number sits on a gold disc in its usual place, read out as "Set 1, personal record" (F21) (followed by the record types; the exercise detail keeps its PR badge after the numbers)
- [ ] RPE column opt-in per exercise from its ⋮ menu, shown anyway when any set already has RPE (schema v3, migration and migration test) (F05)
- [ ] Rest countdown as a top-bar chip with an emptying ring, replacing the rest bar; tap for −15 / +15 / Skip; pulses in the last 10 seconds (F06)
- [ ] Peek at the elapsed time during rest: tapping the routine title spins the chip to the elapsed time in inverted colours, with a line that drains over 5 seconds before it spins back; tapping the title again flips it back sooner. Rest ending mid-peek still buzzes and leaves the chip on the elapsed time. Offered as a TalkBack action on the chip, whose label carries both times; a cross-fade replaces the spin when animations are off; a one-time hint on the first rest (F22)
- [ ] Resume workout on the finish summary (today's dialog, then D3's full screen): reopens the session (clears its end time) after a Finish tapped by mistake; offered only until the summary is dismissed. It ships with the top bar, since that is what puts Finish beside the chip
- [x] Keypad context line ("Bench press · Set 3 · Reps", last time on the right) with the hide button in it; step keys show their unit; "." greyed out for reps (F07) (Next now spans two keys where the hide key was)
- [ ] The workout detail's editor picks all of this up (it shares the logger's editor) but keeps its own top bar and Done button

Done when: on a Pixel 7, with the keypad open and a rest running, at least four set rows are visible.

## Phase D3: Start and finish

Goal: one tap to the right session, and a finish screen that shows progress.

- [ ] Workout tab: "Up next" card for the routine done least recently (never-done routines first, then routine order) with a Start button (F09)
- [ ] Routine rows with exercise count, last done, and typical duration (the average of past sessions, hidden until there is one), a play button and a ⋮ menu; "Start an empty workout" becomes a text button (F09)
- [ ] Full-screen finish summary: duration, volume and set tiles; the gold records card with its pop-in; Done as the main button and Save as routine as the secondary one; the note when the app ended an idle workout (F08)
- [ ] "Compared with last time" on the summary: volume change and each exercise's top set against the previous session of the same routine, hidden for empty workouts (F08)

Done when: finishing a routine shows its volume change against the last time you did it.

## Phase D4: History and exercises

Goal: past work and progress read at a glance.

- [ ] History cards: date line with a gold PR chip, routine name, one row of stats, and the first few exercises; the List / Calendar switch becomes a top-bar icon (F10)
- [ ] Calendar: trained days tinted in three steps by volume (thirds of the month's highest), selected day filled, today always ringed (F11)
- [ ] Workout detail: stat tiles, Delete moves to the menu, undone sets collapse to one line, exercise names open their detail (F12)
- [ ] Exercises: Material 3 search bar, muscle chips plus an Equipment chip, and each trained exercise's last top set and when; no monogram avatars (F13)
- [ ] Exercise detail: e1RM hero with its 30-day change and a sparkline, records as a 2×2 grid of tiles with dates, then history (F14)
- [ ] Exercise picker: chips with Recent first, numbered picks in tick order, and a tray of picks above the Add button (F16)

Done when: the Exercises list shows your last top set for every exercise you've trained.

## Phase D5: Routines and app frame

Goal: routines read like a written program, and a running workout is never out of sight.

- [ ] Routine editor: one summary line per exercise ("3 × 6–8 · rest 3:00"), one expanded at a time, drag to reorder, Delete in the menu (F15)
- [ ] Mini bar above the navigation bar on every other tab while a workout runs: routine, elapsed time, rest countdown, tap to resume (F17)
- [ ] Tab icons: dumbbell, history, list, sliders (vectors copied into `res/drawable`, not the extended icon library) (F17)
- [ ] One title style on all four tabs (F17)

Done when: you can check an exercise's history mid-session and see the rest countdown the whole time.

## Phase 5: Export, backup and v1.0 release

Goal: your data is safe and the app is releasable.

- [ ] Export all data to JSON (full fidelity) and CSV (one row per set, Strong/Hevy style) via the system file picker
- [ ] Import from the app's own JSON, with a preview and replace-or-merge choice
- [ ] Android Auto Backup rules that include the database
- [ ] Settings as a grouped list (F19): Appearance (theme: system, light or dark; dynamic colour toggle), Workout (units, default rest time, default weight increment), Your data (export, import, backup status), About (app version; the debug build's update card moves here). Stored with DataStore
- [ ] App icon
- [ ] Empty states with an icon, a line saying what will appear, and an action: History offers to start a workout, the Workout tab offers a new routine (F18)
- [ ] Dark theme and accessibility check (F20): the set table at 200% font size, no state shown by colour alone, animations respect the system's remove-animations setting, contrast of both schemes, TalkBack reading order in a set row
- [ ] Release signing via GitHub Actions secrets; tag `v1.0.0` publishes a signed APK to GitHub Releases
- [ ] README with screenshots (taken after the design pass) and install steps

Done when: you can uninstall, reinstall, import the export file, and all history is back.

## After v1

These come from the Should-have list. Each gets its own checklist section here when its turn comes:

- v1.1: progression suggestions (double progression with a configurable increment)
- v1.2: per-exercise charts (e1RM, top set, volume) and weekly volume per muscle group (growing out of D4's e1RM sparkline)
- v1.3: weekly session target with week-based streaks, training-day reminders, and a year heatmap (D4 already tints the month calendar by volume)
- v1.4: plate calculator, supersets, warm-up set generator

## Working agreement

- One branch and pull request per checklist item or small group of items; `main` stays releasable
- This file is the tracker: tick each checklist item in the pull request that lands it. No GitHub milestones or issues
- Schema changes always ship with a Room migration and a migration test from Phase 1 onward
