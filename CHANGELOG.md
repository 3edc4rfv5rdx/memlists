# Changelog

> N=new feature, E=error fix, F=fine-tune, R=refactor, I=infrastructure, T=tag

## Unreleased
- N Add the first memo creation flow with a dedicated editor screen, SQLite insert, and home refresh after save
- F Rename the changelog file to `CHANGELOG.md` and align script references with the uppercase filename
- N Add the first data-backed Lists home flow: SQLite loading, folder navigation, lock/completion indicators, and root or folder rendering
- F Remove leftover shell scripts from the old scheme and keep only the `xcalc`-style set
- N Add the first data-backed Memos home flow: SQLite loading, folder counts, folder switching, and memo card rendering
- I Switch the shell scripts to the `xcalc` workflow: `debug` increments `build_number.txt`, `release` does not, and the full script set now lives in the repo
- F Adapt install and release-upload scripts to the current MemLists APK outputs and `CHANGELOG.md`
- I Bootstrap Android project with Kotlin, Compose, `minSdk 28`, `targetSdk 36`, APK signing lookup, and shared build shell scripts
- N Add shared theme and localization JSON loaders, the initial SQLite schema helper, and the first navigation shell for welcome, memos, lists, and settings
- F Align the Gradle wrapper with `9.3.1` and fix the first compile issues until `:app:assembleDebug` succeeds
