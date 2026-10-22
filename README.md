# Zune Pocket

A Zune-style music player for the **Retroid Pocket Classic**, designed for its physical buttons first and touch second.

It plays the music already on your device, from both internal storage and the SD card. Supported formats are the ones Android decodes itself: MP3, AAC/M4A, FLAC, OGG/Vorbis, Opus, WAV and ALAC. Playback continues in the background, with a notification and lock-screen controls, and the app picks up your last queue where you left off.

On first launch, choose **allow access to music** on the home screen. Album art comes from the files' embedded art or a `cover.jpg` / `folder.jpg` in the album folder. Albums without art get generated art.

**Settings → music source** switches to a built-in sample library with a silent player, for trying the UI without any music on the device.

## Install (no Play Store)

1. On the device, open this repo's **Releases** page and download the latest `zune-pocket-x.y.z.apk`.
2. Open the downloaded file. Android will ask you to allow installs from your browser or file manager the first time; allow it, then tap **Install**.
3. To update later, install the newer APK the same way. Your library, settings and queue are kept.

Or use [Obtainium](https://github.com/ImranR98/Obtainium): add this repo's URL and it will offer updates as new releases appear.

If you previously ran the app from Android Studio, uninstall that copy once first. It was signed with a different key, so Android won't let the release replace it.

## Publishing a release

Every tag starting with `v` builds a signed APK on GitHub and attaches it to a release:

```
git tag v0.3.0
git push origin v0.3.0
```

This needs four repository secrets (Settings → Secrets and variables → Actions): `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. For local release builds, put the same values in a `keystore.properties` file at the project root (`storeFile=`, `storePassword=`, `keyAlias=`, `keyPassword=`); it's git-ignored. **Back up the keystore.** Without it you can't publish updates that install over existing copies.

## Build from source

1. **Enable USB debugging on the device:** Settings → About device → tap *Build number* seven times. Then go to Settings → System → Developer options and turn on **USB debugging**. Plug it in and accept the "Allow USB debugging" prompt.
2. **Open the project:** in Android Studio, use *File → New → Project from Version Control* and paste this repo's URL, or *File → Open* this folder. Let Gradle sync finish. If it asks to install SDK Platform 35, say yes.
3. Pick the Retroid in the device dropdown and press **Run ▶**.

From the command line instead: `./gradlew installDebug`.

## Controls

| Button | Browsing | Now Playing |
|---|---|---|
| D-pad ↑ / ↓ | Move selection | Volume |
| D-pad ← / → | Switch pivot (artists ↔ albums ↔ …) | Previous / next track |
| A | Open / play | Play / pause |
| B | Back | Back |
| X | Go to Now Playing | Show the queue |
| Y | Context menu (play next, add to queue, go to artist…) | Shuffle, repeat, go to album/artist |
| L1 / R1 | Page up / down | Previous / next track |
| L2 / R2 | Previous / next letter | Seek −/+ 10 s |
| Start | Play / pause, anywhere | Play / pause |
| Select | Letter jump grid | Shuffle on/off |

**Album view:** the albums tab can show a list, large tiles (2 per row) or small tiles (3 per row). Change it in Settings → album view, or press Y on an album → "view as…". In tile view, ←/→ move between tiles and switch pivot at the edge of a row.

Hold any direction or shoulder button to repeat. If your Retroid is set to the swapped A/B layout, change **Settings → confirm button**.

Touch works too: tap to open, swipe sideways to change pivot or track, and tap the art in Now Playing to pause.

### Check your buttons first

Go to **Settings → key tester** and press every button. Each press shows the Android key code and the action it maps to. To leave the key tester, hold any button for 1.5 s or tap the screen. If a button shows **not mapped**, note its code. The mappings are all in `input/KeyBindings.kt`.

## Code tour

```
app/src/main/java/com/zunepocket/
  input/      KeyBindings (key → action), InputRouter (handler stack + auto-repeat)
  data/       models, MusicLibrary, MediaStoreMusicRepository + MediaStoreMapper (device music),
              FakeMusicRepository (sample data)
  player/     PlayerController, Media3PlayerController + PlaybackService (real audio),
              FakePlayerController (silent sample player)
  nav/        Screen back-stack entries (each owns its selection state), Navigator
  ui/         AppState, theme (Selawik font, accent colours), components, screens
```

Each screen tracks its own selected row and doesn't use Compose's D-pad focus system, so buttons and touch always agree. Every key press goes through `MainActivity.dispatchKeyEvent` → `InputRouter`. The router sends the resulting action to the top-most screen or overlay first, then to global handlers (Start, X, Back).

Unit tests: `./gradlew test`.

## Credits

The Selawik font is © Microsoft, licensed under the SIL Open Font License 1.1 (see `SELAWIK-LICENSE.txt`). This project isn't affiliated with Microsoft or the Zune brand.
