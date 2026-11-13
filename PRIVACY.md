# Privacy

ShrugTV has no server of its own and collects no data for itself. This page lists what is stored on this device and which services the app contacts directly. Each of them receives your IP address and the technical details of the request.

The same text is in the app under Settings → About ShrugTV → Privacy.

## Who is responsible

The operator named in the [imprint](app/src/oss/res/values/imprint.xml) (also shown in the app under Settings → About ShrugTV → Imprint), for the app as distributed from this repository. The app has no server of its own.

## Stored on this device

Your Twitch sign-in (access token), your settings, favourites, watch progress of videos, the watch statistics (which channels you watched, when and for how long; kept 25 months, in a database that is not part of backups, deleted on the Statistics page) and a local error log. Nothing of it leaves the device. Signing out removes the sign-in; clearing the app data in the system settings removes everything.

## Twitch

Streams, videos, chat, channel data, sign-in and live events come directly from Twitch Interactive, Inc. (id.twitch.tv, api.twitch.tv, gql.twitch.tv, usher.ttvnw.net, irc-ws.chat.twitch.tv, eventsub.wss.twitch.tv, static-cdn.jtvnw.net). Twitch processes this data under its own privacy policy (twitch.tv/p/legal/privacy-notice). Legal basis: Art. 6(1)(b) GDPR, the use of Twitch you ask for.

## Emote services

To show third-party emotes in the chat, the app loads emote lists and images from BetterTTV (api.betterttv.net, cdn.betterttv.net, sockets.betterttv.net), FrankerFaceZ (api.frankerfacez.com, cdn.frankerfacez.com) and 7TV (7tv.io, cdn.7tv.app, events.7tv.io). They learn which channel you are watching. Legal basis: Art. 6(1)(f) GDPR, the chat shown as on Twitch.

## Only when you switch it on

Chat history: recent-messages.robotty.de supplies the last chat messages of a channel. Home Assistant: the app connects to the MQTT broker you enter. Phone page: the app opens a small web page in your home network. All three are off until you enable them in the settings. Update check (public version): the app asks api.github.com for the newest release; GitHub sees your IP address. It is off until you enable it.

## Your rights

You have the right to access, rectification, erasure, restriction and objection (Art. 15 to 21 GDPR) and to lodge a complaint with a data protection supervisory authority. Since the app keeps your data only on your device, you exercise most of these rights yourself by signing out or clearing the app data. For data held by Twitch or the emote services, contact them.

## Proxy

The public edition has no proxy option; live stream playlists always come directly from Twitch.
