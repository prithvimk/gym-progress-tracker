# Gym Progress Tracker

Offline-first Android app (Kotlin, Jetpack Compose, Room). The roadmap and working agreement are in
[docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md); tick its checklist items as they land.

## Checks

Run before every push, the same as CI:

```
./gradlew testDebugUnitTest ktlintCheck lintDebug assembleDebug
```

Schema changes ship with a Room migration and a migration test.

## Phone test checklist after every build

After any build (a local build, or CI going green on a push or pull request), give the user a checklist of
what to test on their phone for that build:

- Say which build it is and how to install it: the `dev` release for `main`, or the `pr-<number>` build
  under Settings → App updates → Test a PR build.
- List concrete steps with what should happen, grouped by flow, as tickable `- [ ]` items.
- Cover what the change adds, and the existing flows it could break.
- If the change has nothing visible yet, say so plainly and list the regression checks instead.
