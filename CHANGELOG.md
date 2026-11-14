# Changelog

All notable changes of ShrugTV, newest first. Versions follow [SemVer](https://semver.org).

## 2.3.0 – 2026-11-14

### New
- Statistics page: a new line "Clips watched" shows how many clips you watched in the period, how long they played, the average length and the channels most of them came from. A clip counts after 5 seconds (streams and recordings after a minute) and is no longer part of "Recordings" in the live-against-recordings ring, but still of the total time. It can be switched off under Settings → Home screen → Statistics page.
- Clips: the list grows while you scroll. The page shows one load of clips (Settings → Home screen → Clips → "Clips per load", 20 by default) and adds the next load two rows before the end. The queue is not limited to what is shown: with "Play the next clip" the clips go on through the whole pool (up to 500 clips, the best 50 of each channel).
- Clips: the title of a playing clip stays in the top left corner, with the channel and the place in the queue ("2 / 12"); it can be switched off under Settings → Home screen → Clips → "Show the clip title". With the bar open, the channel info shows the title.
- Clips in a queue: the buttons "Previous clip" and "Next clip" in the player bar go to the neighbours in the list the clip was opened from; at the ends the button is dimmed.
- Clips: a button "Recording" in the player bar jumps to the place in the recording the clip was cut from, as soon as that recording is available. If there is none (they expire after a few weeks), the button is dimmed and does nothing.
- Clips play one after the other: when a clip ends, the next one of the list it was opened from starts (the Clips page as chosen there, or the home row). Clips you opened before are left out of that queue; both can be changed under Settings → Home screen → Clips ("Play the next clip", "Clips I already opened in the queue"). Every clip that starts counts as opened.
- Link `shrugtv://clip/<slug>?queue=<slug>,<slug>` plays the clips one after the other, for Home Assistant or other apps.
- Clips show the chat that was running when they were recorded. A clip has no chat of its own, so the app looks up the recording it was cut from and plays its chat from the second where the clip starts. When that recording no longer exists (they expire after a few weeks) or chat of recordings is off, the chat panel stays away instead of showing an empty window.
- Clips: the sort option "Relative to channel size" compares each clip with the other clips of its own channel (views divided by what is usual there), so a very good clip of a small channel can rank next to one of a big channel. It is also a button on the Clips page.
- Clips: two more settings and a sort option. "Hide clips I already opened" leaves out clips you opened before (the last 500 are remembered), "Language of the clips" can keep only clips in your stream language, and "Shortest first" sorts by length.
- Link `shrugtv://clip/<slug>` plays one Twitch clip, like `shrugtv://stream/<login>` and `shrugtv://vod/<id>` do for streams and videos.
- Clips: a new page in the side bar (play symbol) and a home row show the best clips of the channels you follow in one list, so you do not have to open every channel. Settings → Home screen → Clips: whose clips (everyone you follow or favorites only), how far back (24 hours, 7 days, 30 days, year, all time), sort by views or newest, how many, and whether the row shows on the home screen; the page itself has the same choices.
- Statistics page: Settings → Home → "Statistics page" shapes it: the period it opens with, the time format (1 h 35 min, 1:35 h or 1.6 h), the comparison with the period before, each card on or off (figures, activity chart, time of day, top channels, top categories, live and recordings), the length of the top lists (3, 5, 8 or 10) and whether they rank by time or by number of sessions, the activity chart as bars or a line, marking the busiest day, the time of day as 24 hours or four parts of the day, live and recordings as a ring or a bar, and how long the data is kept (1, 2 or 5 years). Everything is also in the browser settings.
- Phone page and settings: the new texts (touchpad, chat tools, version tab, channel lists, stars, skin and followed-channels switches) are translated into all twelve languages.
- Phone page: a star on every channel card and next to the channel that is playing marks it as a favorite (the same favorites as the star on the TV). Favorites come first in the list of followed channels that Home Assistant gets, so the dashboard shows them first.
- Browser companion: the home screen rows can be sorted from the browser (settings page "Home screen rows": the list is in the current order, "Move up" per row, reset), and "Clear caches" runs from the browser too.
- Home Assistant (MQTT), followed channels: channels marked with the star in ShrugTV carry `favorite: true` in the list, so a dashboard can show them first.
- Home Assistant (MQTT): new switch "Followed channels" (off by default) publishes who you follow and who is live (game, title, viewers) as the topic `followed` with two sensors, "Live channels" and "Followed channels", whose attribute `channels` holds the list for a dynamic dashboard; updated every minute and when someone goes live.
- Phone page: it wears the colours of the skin picked on the TV (also your own skins and single colours) and follows live when you change them; the installed icon takes the colours too. Settings → Connections → Phone & browser → "TV skin on the phone page" (on by default) turns it off; the version tab explains how to put the page on the home screen of an iPhone or Android phone.
- Phone page: new "Version" tab with the installed version, edition, TV and Android, the result of the last update check, "Check now" and, in the public edition with "Install updates in the app" on, "Install update": the TV downloads the release, checks its SHA-256 and asks for the last OK on the TV; a red dot on the tab shows a newer version.
- Phone page, remote: touchpad (swipe = arrow keys, a long swipe keeps scrolling, tap = OK, two fingers = Back, with a small vibration), switchable to the key pad; new buttons "Replay" (live streams) and "Audio only"; "Recently watched" channels as quick picks.
- Phone page, chat: Twitch badges in front of the names, tap a message to answer it with @name, an emote panel with the emotes seen in the chat, and quick phrases you can add and remove (hold). Home Assistant (MQTT) can send `replay` and `audio_only` as well, and the state carries `audio_only`.
- Update check: with the check on, the home screen shows a small notice "Version X is available" at the start for a few seconds (where to find the card: Settings → App → Check now); it only informs and takes no focus.

### Changed
- Clips in a queue: a few seconds after a clip starts, the next one is loaded in the background (address looked up, the first seconds buffered), so the change to it is almost instant instead of a pause of about two seconds.
- Clips you opened before are now dimmed by default (before: shown normally). The choice is under Settings → Home screen → Clips.
- Clips: "Hide clips I already opened" is now the choice "Clips I already opened": show normally, dim them, or hide them. A switch that was on becomes "hide".
- Statistics page redone: cards on one screen without scrolling (time watched with the comparison, days, sessions, longest session), a new activity chart with one bar per day (per month for the last year) that marks the busiest day, the time of day, the top 5 channels and categories with ranks, and a ring for live against recordings. The periods sit at the top right.
- German texts: where people say the English word, the app does too: Button instead of Knopf, Replay instead of Nochmal, Cache, Backup, Badges, Dropped Frames, Nur Audio, Watchtime; "Zu Live springen" instead of "Zum Live-Rand", "Frühere Streams" instead of "Vergangene Übertragungen", "Zeitleiste", "Bildschirmschoner"; the group "Handy-Begleiter" is now "Handy & Browser" like its menu entry; the phone page says Edition, Schnellnachricht and "Chatten vom Handy".

### Fixed
- Watch time of a clip that was taken over from the preload (the next clip of a queue) was not counted, and a clip opened by link had no channel in the statistics.
- Clips page: after pressing "Favorites" (or any other choice) the old list stayed on screen while the new one loaded, and the focus jumped into the list, so it looked as if nothing worked. Now the list is replaced by "Loading clips …" at once, the focus stays on the button so the next one can be pressed, and the last four lists are kept, so switching back is instant. Loading is lighter too: 30 clips are asked per channel instead of 50.
- Chat of a recording: a page of messages asked for before a jump (seek, clip to its recording) and arriving after it no longer sends the following requests to the old position.
- Clips did not play: the player only knew HLS streams, but a clip is an MP4 file, so opening one gave a black screen. The player now picks the right kind by the address.
- Phone page: the favourite star in the channel lists sits at the right edge of the card instead of on top of the avatar.
- Home Assistant (MQTT), followed channels: the list no longer carries pictures, only names, and for those live game, title and viewers, so all channels fit under the limit of Home Assistant.
- Home Assistant (MQTT), followed channels: with many followed channels the live titles are kept; the pictures of the followed list are left out first.
- Phone page, chat: the Twitch badges now arrive in front of the names (the chat message did not carry which badges it had).
- Player: no crash on opening when the setting "answered message length" in the chat settings was changed with the slider on the TV (the number was read as text).
- Phone page: after an update of ShrugTV the browser no longer shows the old page; the page asks for its script and style with a checksum of the file.
- Repository: the GitHub build check uses current versions of its actions (checkout, setup-java, upload-artifact v5), which removes the Node 20 deprecation warnings.

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
