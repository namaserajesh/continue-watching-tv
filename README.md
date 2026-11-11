# Continue Watching for Android TV

![Android TV 8+](https://img.shields.io/badge/Android%20TV-8.0%2B-3DDC84?logo=androidtv&logoColor=white)
![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![No INTERNET permission](https://img.shields.io/badge/network-none-2E7D32)
![License: MIT](https://img.shields.io/badge/license-MIT-blue)

One list of what you were watching, across every app on the TV, opened from its own tile or from a
[CleanYourTV](https://github.com/Gathin23/cleanyourtv) module card. It joins two sources:

1. **Watch Next rows** that streaming apps publish to Android TV's system table (Prime Video, JioHotstar,
   ZEE5 and others), with the app's own link back to the episode. Rows older than 90 days and titles
   past 95% are left out, because some apps leave rows behind for years.
2. **A now-playing history** this app records itself, for apps that publish no Watch Next rows at all.
   YouTube is the big one: it writes none, even with a Premium subscription. A title is saved once it
   has been playing for two checks in a row (30 seconds apart), which keeps out the previews that
   autoplay on the apps' home screens.

The app has no `INTERNET` permission, so nothing it reads can leave the TV. Rows show the app's own
banner instead of web posters for the same reason.

## Which TVs it works on

| Device | Status |
|---|---|
| Sony BRAVIA KD-55X80K, Android TV 12 | Tested, in daily use |
| Other Android TV and Google TV devices on Android 8.0 or later (TCL, Hisense, Philips, Xiaomi, Chromecast with Google TV, Nvidia Shield and others) | Should work: it uses only standard Android TV APIs. Not tested yet; reports are welcome |
| Fire TV | Not tested |
| Samsung (Tizen), LG (webOS), Roku | No: these TVs don't run Android apps |

What it shows depends on each app. Many apps never publish Watch Next rows, and a sideloaded app can
read only the rows an app marks `searchable`. CleanYourTV's
[watch-next notes](https://github.com/Gathin23/cleanyourtv/blob/main/docs/watch-next.md) and
[discussion #2341](https://github.com/Gathin23/cleanyourtv/discussions/2341) list what apps publish on
several brands. The now-playing history fills the gap for any app that reports what it plays to Android's
media controls, which is what the remote's play and pause buttons already rely on.

## Install

You need `adb` and network debugging turned on in the TV's developer options. Get the APK from
[Releases](../../releases/latest), or build it yourself (below), then:

```sh
adb connect <tv-ip>:5555
adb install continue-watching-v0.1.0.apk

# Read the Watch Next table (a runtime permission; there is no on-screen prompt for it).
adb shell pm grant local.continuewatching android.permission.READ_TV_LISTINGS

# Read what is playing (media sessions). The app overrides no notification callback.
adb shell cmd notification allow_listener local.continuewatching/local.continuewatching.NowPlayingListener
```

Open **Continue Watching** from the apps list. If the now-playing grant is missing, the screen says so
at the top and still shows the Watch Next rows.

If a second user profile is added or removed on the TV later, run `cmd notification disallow_listener`
with the same component, then `allow_listener` again: the listener can stay allowed without being bound.

Turn network debugging off when you are done.

## Check the signature

Every release APK on the Releases page is signed with the same certificate. Before installing a
download or an update, compare its certificate with this one (`apksigner` ships in the Android SDK's
build-tools):

```sh
apksigner verify --print-certs continue-watching-v0.1.0.apk
```

```text
Signer #1 certificate DN: C=US, O=Android, CN=Android Debug
Signer #1 certificate SHA-256 digest: 23784ccc26483560e91f114946defb0f2a00f2f2fe1175b0919bdfd596657abe
```

If the SHA-256 differs, the APK did not come from this repository's releases: don't install it, and
open an issue. The DN is the generic one every Android debug key carries, so only the digest
identifies this key.

## Permissions

| Permission | Why |
|---|---|
| `READ_TV_LISTINGS` | Read the Watch Next rows other apps publish |
| Notification access (`BIND_NOTIFICATION_LISTENER_SERVICE`) | Android's only way for an app to read media sessions, which is how the now-playing history learns titles |

No `INTERNET`, no `RECEIVE_BOOT_COMPLETED`, no accessibility service. `allowBackup` is off, and the
history (50 titles at most) stays in the app's private storage.

## Hide an app from the list

To keep an app out of both the list and the history, add its package names to `local.properties`
before building:

```properties
neverListed=com.example.one,com.example.two
```

`local.properties` is never committed, so the list stays on your machine.

## Build from source

Needs JDK 17 and the Android SDK (API 34).

```sh
export JAVA_HOME=/path/to/jdk-17
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

The release build is signed with the local debug key, as most sideloaded TV apps are. An APK you build
yourself therefore has a different certificate than the one on the Releases page (see
[Check the signature](#check-the-signature)); uninstall one before installing the other.

## With CleanYourTV

The app declares a CleanYourTV module card (`com.gathin.tvhome.category.DOCK_CARD`), so a CleanYourTV
home screen can show it in the dock. It works without CleanYourTV too, from any launcher's apps list.

## License

[MIT](LICENSE)
