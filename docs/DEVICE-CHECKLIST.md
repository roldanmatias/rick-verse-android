# Things that need a device, a GitHub UI or a secret

Everything below is configured or written in code but **not verified**, because it cannot be done from this
environment. Tick them off before presenting the project; do not quote numbers you have not measured.

## On a device or emulator
- [ ] `./gradlew :app:connectedDebugAndroidTest` (Hilt e2e: list -> detail -> back, settings)
- [ ] Airplane mode: list and detail show saved data with the offline banner (Room cache)
- [ ] Cold start in airplane mode shows cached GraphQL data (SQLite normalized cache)
- [ ] Settings: switch REST <-> GraphQL resets to the first screen and top-bar chip changes (ADR 0009)
- [ ] Deep links: `adb shell am start -a android.intent.action.VIEW -d rickverse://character/1`
- [ ] Predictive back (Android 14+ developer option "Predictive back animations")
- [ ] Per-app language (Android 13+): Settings -> Apps -> RickVerse -> Language -> Espanol
- [ ] TalkBack walkthrough of list, detail, settings; font scale 2.0 and display size large
- [ ] Release variant smoke test (R8): `assembleRelease`, install, open list/detail on both data sources
- [ ] `./gradlew :benchmark:connectedBenchmarkAndroidTest` and `:app:generateBaselineProfile`; record cold-start
      medians (no profile vs profile) in `docs/benchmark.md`
- [ ] Device timings in `docs/benchmark.md` (5-run average per strategy, from Settings -> Network log)
- [ ] Layout Inspector recomposition counts on the cast list while typing in search

## Screenshots
- [ ] Re-record goldens on the CI OS (`recordRoborazziDebug`) if the verify job runs on Linux
