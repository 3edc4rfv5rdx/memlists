# Progress

## Done
- Read the full specification and extracted the initial architecture constraints.
- Stored project-specific workflow rules in local memory.
- Initialized a git repository in the current directory.
- Renamed the working branch to `main`.
- Added the Gradle wrapper and base Android project files.
- Configured Kotlin, Compose, `minSdk 28`, `targetSdk 36`, APK signing lookup, and version loading from `build_number.txt`.
- Added `CHANGELOG.md` and the first unreleased infrastructure entry.
- Added the Android manifest, launcher assets, and backup extraction placeholders.
- Added JSON assets for localization and theme definitions.
- Implemented the shared SQLite schema helper for the six required tables.
- Implemented the shared settings repository and initial app settings model.
- Implemented theme and localization loaders.
- Implemented shared Compose UI building blocks.
- Implemented the first app shell with welcome, memos home, lists home, and settings screens.
- Copied the base build shell scripts from `../weeker`.
- Updated the Gradle wrapper to `9.3.1`.
- Ran the first local build and fixed the initial compile and resource issues.
- Confirmed that `./gradlew :app:assembleDebug` completes successfully.
- Replaced the temporary shell scripts with the `xcalc` workflow and added the full script set.
- Updated the install and release-upload scripts for MemLists paths and outputs.
- Implemented the first data-backed Memos home flow with SQLite loading, sorting, folder counts, and folder switching.
- Confirmed again that `./gradlew :app:assembleDebug` completes successfully after the Memos changes.
- Implemented the first data-backed Lists home flow with SQLite loading, folder navigation, and list state indicators.
- Removed leftover shell scripts from the previous scheme and kept only the `xcalc`-style set.
- Confirmed again that `./gradlew :app:assembleDebug` completes successfully after the Lists changes.
- Renamed the changelog file to `CHANGELOG.md` and aligned script references.
- Implemented the first memo creation flow with a dedicated editor screen and SQLite insert.
- Confirmed again that `./gradlew :app:assembleDebug` completes successfully after the memo editor changes.
- Reworked the welcome and main memos navigation closer to the specification and removed the extra hero-style shortcuts.
- Updated all screens to the new `ScreenScaffold` navigation API and confirmed a clean rebuild.
- Installed the rebuilt debug APK to the emulator and confirmed that `MainActivity` is resumed.
- Reworked the first memo editor closer to the spec with top app bar save, date picker and clear button, tags controls, priority stepper, and the first reminder toggle.
- Confirmed again that `./gradlew :app:assembleDebug` completes successfully after the memo editor changes.
- Installed the rebuilt debug APK to the emulator and confirmed that `MainActivity` is displayed.
- Replaced the top app bar text save action with a save icon button and removed the visible `memo` wording from that action.
- Confirmed again that `./gradlew :app:assembleDebug` completes successfully after the save action change.

## Next
- Continue replacing provisional UI with spec-driven layouts and behavior.
- Extend memo editing beyond the first create flow and add reminder controls.
- Continue the Lists module toward the spec screens and actions.
