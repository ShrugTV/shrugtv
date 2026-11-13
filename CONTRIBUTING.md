# Contributing

Thank you for helping with ShrugTV.

- **Bugs and ideas:** open an issue. Say which device (Fire TV, Google TV, Shield ...), the app version
  (Settings -> About ShrugTV) and what you did. Do not paste your Twitch access token or log lines that contain it.
- **Pull requests:** welcome. Keep a change small and focused, run `./gradlew :app:testDebugUnitTest` first, and put a
  line under "Unreleased" in `CHANGELOG.md` for anything a user can see. New texts go into `res/values/` (English)
  and `res/values-de/` (German); the other languages are filled in by the maintainer.
- **How changes land:** ShrugTV is developed in a separate working repository and published here as one commit per
  release. An accepted pull request is applied there by the maintainer and shows up in the next version; the pull
  request is then closed with a pointer to that version.
- **Licence:** by contributing you agree that your change is licensed under the Apache License 2.0 (see `LICENSE`).

Every user-visible feature has a setting and a short guide in the settings (`res/xml/settings.xml`); please keep it that way.
