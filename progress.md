# Progress

## Done
- Read the full specification and extracted the initial architecture constraints.
- Stored project-specific workflow rules in local memory.
- Initialized a git repository in the current directory.
- Renamed the working branch to `main`.
- Added the Gradle wrapper and base Android project files.
- Configured Kotlin, Compose, `minSdk 28`, `targetSdk 36`, APK signing lookup, and version loading from `build_number.txt`.
- Added `changelog.md` and the first unreleased infrastructure entry.
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

## Next
- Replace the placeholder Lists home screen with real data-backed rendering.
- Start the Memos module data flow and filtering skeleton.
- Add memo creation and editing flow.
