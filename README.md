# Hue Sort: Color Cascade

A lightweight, offline-first colour-sorting puzzle for Android, built with Kotlin and Jetpack Compose. Pour matching colours into the same tube, solve increasingly tricky boards, and use rewarded ads only for optional in-game helpers.

## Why this build is Play-friendly

- Puzzle category, **Everyone** content rating.
- No signup, login, chat, user-generated content, or social feed.
- No camera, contacts, location, storage, or background permissions.
- Only `INTERNET` and `ACCESS_NETWORK_STATE` are used, for ads.
- Targets **Android 16 (API 36)**, required for new Play submissions.
- No real cash, gift cards, crypto, betting, or "earn money" claims.
- Rewarded ads unlock only **hint**, **shuffle**, and **extra tube**.
- Interstitial ads appear only after completed levels, every three levels.

Instant approval cannot be guaranteed, but this is deliberately a low-policy-risk format.

## Game loop

- Each level starts with mixed tubes and two empty tubes.
- Tap one tube, then another to pour the top same-colour group.
- Fill every tube with a single colour to complete the level.
- Board count grows from 3 to 9 colours; tube capacity becomes 5 at higher levels.
- Every scramble is generated from a solved board using reverse legal pours, so it stays solvable.
- The board uses translucent 3D glass tubes, depth shading, and an animated ambient backdrop.

## Build

Requirements: JDK 17+, Android SDK with API 36 installed.

```bat
gradlew.bat test
gradlew.bat assembleDebug
gradlew.bat bundleRelease
```

Outputs:

- Debug APK: `app\build\outputs\apk\debug\app-debug.apk`
- Release AAB: `app\build\outputs\bundle\release\app-release.aab`

## Ads setup

`gradle.properties` currently uses Google's public **test ad unit IDs**. Keep them for local testing. Before uploading to Play:

1. Create your own AdMob app and ad units.
2. Replace `admobAppId`, `admobInterstitialId`, and `admobRewardedId` in `gradle.properties`.
3. Rebuild the release bundle.

Never commit AdMob credentials or your upload keystore. `keystore.properties`, `local.properties`, and `*.jks` are gitignored.

## Signing

A local upload keystore is generated during setup and ignored by Git. For a real Play Console account, generate and keep your own upload key; if you lose it, use Play App Signing recovery support.

## Privacy and disclosure

See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) and [PLAY_STORE_LISTING.md](PLAY_STORE_LISTING.md). Host the privacy policy at a public URL and paste that URL into Play Console before submitting.

Support: [info@omegaapi.com](mailto:info@omegaapi.com)

## Sponsor

[![Sponsor OmegaAPI](https://img.shields.io/badge/Sponsor-%E2%9D%A4-db61a2?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/omegaapi-india)

Built by Krishna Prakash Ojha (Leon) at OmegaAPI.
