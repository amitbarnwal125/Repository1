# Swara: free and legal Indian music for Android

Swara is a native Android music player for discovering, streaming and downloading Indian and Bollywood-style music. It only uses music that is legally free to stream, and it only allows downloads where the licence and the provider's terms both permit offline copies.

<p align="center"><b>Install:</b> on your Android phone, open the latest release and download <code>Swara.apk</code><br>
<a href="https://github.com/amitbarnwal125/Repository1/releases/latest">github.com/amitbarnwal125/Repository1/releases/latest</a></p>

---

## 1. Where the music comes from (read this first)

| Provider | What it supplies | Stream | Download in app | Why |
|---|---|---|---|---|
| **Wikimedia Commons** (built in, no key) | Public-domain Indian film and 78-rpm recordings (1904–1950), Hindustani/Carnatic classical, bhajans and folk, and modern Bollywood-style instrumentals under CC BY / CC BY-SA / CC0 | ✅ | ✅ | Commons accepts only public-domain or freely licensed media. Those licences allow anyone to copy and store the files, provided the attribution (and, for BY-SA, the licence) stays with them. Swara reads each file's licence live from the Commons API and hides any file without a licence statement. |
| **Jamendo** (optional, your own free client ID) | Independent artists, filtered by tags such as bollywood, indian, hindi and sitar | ✅ non-commercial only | ❌ | Jamendo's [API terms](https://devportal.jamendo.com/api_terms_of_use) allow free non-commercial use with credit and a link back. They prohibit apps that cache content or offer offline access, so Swara never downloads Jamendo tracks. |
| **Your own files** | Audio the user picks with Android's file picker | ✅ | n/a (already on the phone) | Swara only reads what the user selects and never uploads it. The user is responsible for having the rights to it. |

### Why mainstream Bollywood hits aren't included

Current film music is owned by labels such as T-Series, Saregama, Sony Music India and Zee Music. JioSaavn, Gaana, Spotify, Apple Music and YouTube license that catalogue for their own apps only. None of them offers a free, licensed API that lets a third-party app stream full tracks or let users download them. For example, [JioSaavn's API policy](https://www.jiosaavn.com/corporate/api-policy) grants no public or developer access and explicitly bans streaming services, downloading and stream-ripping. The "unofficial JioSaavn APIs" on GitHub are scrapers, so Swara does **not** use them. It also doesn't scrape any site or bypass DRM.

To add real chart music in your own build, sign a deal with a licensed B2B music provider or with the labels. Then add a provider class next to `CommonsProvider`, and set `offlineAllowed = true` only where your contract allows offline copies (see §5).

### Demo catalogue

`data/remote/Catalog.kt` lists 45 hand-checked Commons files in four playlists: *Golden-Era Film & Gramophone*, *Sitar Grooves*, *Raga Mornings* and *Bhakti & Folk*. It also lists six live Commons categories that serve as browsable "albums", for example *Hindi Songs*, *Hindustani Music* and *Rabindra Sangeet*. Each track's licence and attribution are fetched from Commons at runtime and shown in the **Details & licence** sheet.

---

## 2. Features

- **Discover:** a Home screen with featured tracks, curated playlists, Commons collections (albums), artists, and "Popular on Jamendo" when Jamendo is enabled.
- **Search** across Commons and Jamendo with source filters. When offline, search covers your downloads and imported files.
- **Player:** play/pause, next/previous, seek bar, shuffle, repeat (off/all/one), and a queue (play next, add to queue, reorder, remove, clear).
- **Background playback** through a Media3 `MediaSessionService`, with lock-screen, notification, headset and Bluetooth controls, audio focus, and pause when headphones are unplugged.
- **Song details:** title, artist, album, art (or a generated cover), length, size, format, source, licence with links, attribution text, and whether offline use is allowed and why.
- **Downloads** use Android's DownloadManager: live progress, cancel, retry, a Wi-Fi-only option, a storage card showing used and free space, delete one or delete all, and an offline library. Files live in the app's private folder, so no storage permission is needed.
- **Playlists** you create yourself, plus **import** of your own audio files.
- **States:** loading spinners, empty states with next steps, friendly errors with a retry button, an offline banner, and an offline home screen that still plays your downloads.
- **Legal and privacy screens:** Music sources & licences, Privacy policy, and Open-source licences.

## 3. Tech stack and architecture

- Kotlin 2.0, Jetpack Compose with Material 3, and Navigation Compose
- AndroidX **Media3** (ExoPlayer and MediaSession) for playback
- **Room** for downloads, playlists and imported files; SharedPreferences for settings
- **OkHttp** for the APIs and **Coil** for cover art
- Android **DownloadManager** for downloads
- MVVM with a small manual DI container (`AppContainer`); no reflection-heavy frameworks

```
app/src/main/java/com/swara/music/
├─ SwaraApp.kt            AppContainer (manual dependency injection)
├─ MainActivity.kt
├─ data/
│  ├─ model/              Track, LicenseInfo, Collection, Loadable
│  ├─ remote/             CommonsProvider, JamendoProvider, Catalog, Http
│  ├─ local/              Room database, entities, DAOs
│  ├─ repo/               MusicRepository, DownloadRepository, LibraryRepository
│  ├─ Settings.kt, NetworkMonitor.kt
├─ playback/              PlaybackService (MediaSessionService), PlayerConnection
└─ ui/                    SwaraRoot (nav + scaffold), ViewModels, screens/, components/, theme/
```

Each provider turns its API response into a `Track` that carries a `LicenseInfo(offlineAllowed, offlineNote, attribution…)`. The UI never decides licensing. It only reads `track.canDownload`, and `DownloadRepository.start()` refuses any track without that permission.

## 4. Build and run

**Requirements:** Android Studio Ladybug (2024.2) or newer, JDK 17, and the Android SDK 35.

```bash
git clone -b bollywood-music-app https://github.com/amitbarnwal125/Repository1.git
cd Repository1/SwaraMusic
./gradlew assembleDebug          # debug build → app/build/outputs/apk/debug/
./gradlew installDebug           # install on a connected phone with USB debugging on
./gradlew assembleRelease        # minified release, signed with the demo key
```

Or open `SwaraMusic/` in Android Studio and press **Run**.

**To install the APK directly on a phone:** download `Swara.apk` from the Releases page on the phone, tap it, and when Android asks, allow your browser or file manager to *install unknown apps*.

**CI:** `.github/workflows/android.yml` builds the release APK on every push to the `bollywood-music-app` branch. It then runs an emulator smoke test (launch, play, download, queue, search, offline playback, lock screen) and publishes screenshots to the `ci-logs` branch. Add `[release]` to a commit message to publish a GitHub Release with the APK.

**Optional Jamendo setup:** create a free app at <https://devportal.jamendo.com>, then paste its client ID in **Settings → Music sources**. Jamendo's terms limit this to non-commercial use.

**Signing:** `app/demo-release.jks` (password `swarademo`) is committed only so that CI builds can update each other on a test phone. **Before distributing the app, generate your own keystore** and pass it in through `SIGNING_*` environment variables, or edit `signingConfigs`.

## 5. Adding a licensed provider

1. Create `data/remote/YourProvider.kt` that maps API results to `Track`.
2. In `LicenseInfo`, set `offlineAllowed` according to your contract, and put the reason in `offlineNote`.
3. Wire it into `MusicRepository` (featured, search, collections) and `AppContainer`.
4. Add a card to `SourcesScreen` that explains its permissions, and update the privacy policy if it receives user data.

## 6. Privacy

There are no accounts, ads, analytics or trackers. Downloads, playlists and settings stay in the app's private storage. Network requests go straight to Wikimedia (and to Jamendo if you enable it). The full policy is in the app under **Settings → Privacy policy**, and in [PRIVACY.md](PRIVACY.md).

## 7. Licences

- App code: MIT.
- Libraries: Apache 2.0 (AndroidX, Media3, OkHttp, Coil, Kotlin, Guava).
- Music: each track keeps its own licence, which is shown in the app.
