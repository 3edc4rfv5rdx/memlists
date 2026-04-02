# Changelog

> N=new feature, E=error fix, F=fine-tune, R=refactor, I=infrastructure, T=tag

## Unreleased
- I Bootstrap Android project with Kotlin, Compose, `minSdk 28`, `targetSdk 36`, APK signing lookup, and shared build shell scripts
- N Add shared theme and localization JSON loaders, the initial SQLite schema helper, and the first navigation shell for welcome, memos, lists, and settings
- F Align the Gradle wrapper with `9.3.1` and fix the first compile issues until `:app:assembleDebug` succeeds
