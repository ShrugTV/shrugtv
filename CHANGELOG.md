# Changelog

All notable changes of ShrugTV, newest first. Versions follow [SemVer](https://semver.org).

## 2.2.0 – 2026-11-13

### New
- Public edition: Settings → App → "Install updates in the app" (off by default) adds an "Install now" button to the update card: the app downloads the release, checks its SHA-256 and opens the system installer, where you confirm; it needs the system permission "Install unknown apps" for ShrugTV once. The private edition never has this permission.
- Update check: "Check now" shows a card for a newer version with the changes, a QR code to the release page and the buttons "Later" and "Skip this version"; the update texts are translated into all languages.

## 2.1.2 – 2026-11-13

### Fixed
- Repository: the GitHub build check (Android CI) runs again; `gradlew` was not marked executable, so every run failed at the first step.

## 2.1.1 – 2026-11-13

### New
- Install with the Downloader app on Fire TV, Android TV and Google TV: enter the code 4727814, no PC needed; the README has the steps and the code always loads the newest release.

## 2.1.0 – 2026-11-13

### New
- Public edition: Settings → About → "Check for updates" (off by default) asks the ShrugTV release page once a day whether a newer version exists and shows a notice with the link; "Check now" asks right away. Nothing is downloaded or installed.

## 2.0.0 – 2026-11-13

First public release, the state of the private development release 1.38.0.
