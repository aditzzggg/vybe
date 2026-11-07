# Vybe — Music & Video, Zero Ads

Vybe is an open-source Android streaming client that combines a video app and a music app
in one: trending videos, instant search, audio-only **Music Mode**, and playback that
keeps running when your screen is off.

## Features

- **Zero ads** — streams are resolved through community Piped instances, so no ad breaks
  and no pre-rolls, ever.
- **Background playback** — a foreground `MediaSessionService` keeps audio going with a
  system notification while the screen is off or the app is in the background.
- **Music Mode** — audio-only streaming that saves up to 90% of data.
- **Sponsor auto-skip** — when the instance provides SponsorBlock segments, Vybe skips
  in-video sponsors automatically.
- **Trending, search, related videos** — the full discovery loop.
- **Instance failover** — if one Piped instance is down, Vybe rotates to the next one
  automatically (also configurable in Settings).

## Download

Grab the latest APK from the [Releases](../../releases) page → download `app-release.apk` →
allow "install unknown apps" on your phone → install.

## Build it yourself

```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

Requires JDK 17+ and Android SDK 34. The release build is signed with the repository demo
keystore — **generate your own keystore before a real public release**.

## Legal & credits

- Vybe is a clean-room client: original brand, original UI, no third-party logos or assets.
- Content is fetched from third-party community-run instances. Availability and legality
  of those instances is their responsibility, not this app's.
- This project is for education. If you like a creator, support them on their official channel.
- Icons: Material Icons (Apache 2.0). Fonts: Roboto (Apache 2.0).
