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
| Charts (v1.5) | Vico |
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
- [ ] Quick steppers (+2.5 kg, +1 rep) and a numeric keypad that stays open between fields
- [ ] Exercise notes and workout notes
- [x] Finish workout: summary of duration, sets and total volume; discard with confirmation
- [x] Unit tests for "previous set" lookup and volume maths

Done when: you log two real sessions at the gym and the second one shows the first one's numbers.

## Phase 3: Routines and rest timer

Goal: start a planned session in one tap and rest on time.

- [ ] Create and edit routines: ordered exercises with target sets, rep range and rest time
- [ ] Start workout from a routine; target sets are created with "last time" values filled in
- [ ] "Save as routine" from a finished workout
- [ ] Rest timer that starts when a set is completed; per-exercise default, +/-15 s controls
- [ ] Foreground service with a countdown notification and vibration at zero, working with the screen off
- [ ] Notification permission request on Android 13+

Done when: a full Push/Pull/Legs week runs from routines with the phone locked between sets.

## Phase 4: History and PRs

Goal: see your past work and get rewarded for beating it.

- [ ] History list grouped by week, each card showing routine name, duration, volume and PR count
- [ ] Workout detail screen; edit or delete a past workout (PRs recomputed after edits)
- [ ] Month calendar with trained days highlighted
- [ ] PR detection on set completion: heaviest weight, most reps at a weight, best e1RM, best session volume
- [ ] PR badge in the logger and a short celebration on the finish summary
- [ ] Exercise detail screen: PR list and full history for that exercise
- [ ] Unit tests for PR detection, including edits and deletes

Done when: beating last week's top set shows a PR in the session and on the exercise screen.

## Phase 5: Export, backup and v1.0 release

Goal: your data is safe and the app is releasable.

- [ ] Export all data to JSON (full fidelity) and CSV (one row per set, Strong/Hevy style) via the system file picker
- [ ] Import from the app's own JSON, with a preview and replace-or-merge choice
- [ ] Android Auto Backup rules that include the database
- [ ] Settings: units, default rest time, default weight increment, export/import, app version
- [ ] App icon, empty states, dark theme check
- [ ] Release signing via GitHub Actions secrets; tag `v1.0.0` publishes a signed APK to GitHub Releases
- [ ] README with screenshots and install steps

Done when: you can uninstall, reinstall, import the export file, and all history is back.

## After v1

These come from the Should-have list and are tracked as separate milestones:

- v1.1: progression suggestions (double progression with a configurable increment)
- v1.2: per-exercise charts (e1RM, top set, volume) and weekly volume per muscle group
- v1.3: weekly session target with week-based streaks, training-day reminders, calendar heatmap
- v1.4: plate calculator, supersets, warm-up set generator

## Working agreement

- One branch and pull request per checklist item or small group of items; `main` stays releasable
- Each phase is a GitHub milestone; each checklist item is an issue
- Schema changes always ship with a Room migration and a migration test from Phase 1 onward
