# iOS development setup

How to build and run Bisq Connect for iOS as a contributor, and what the simulator can and cannot do. Read this before opening `iosClient/iosClient.xcworkspace` for the first time.

---

## What you need

- A Mac with a current Xcode and the iOS SDK.
- Ruby 3+ and CocoaPods 1.15+ (`sudo gem install cocoapods`).
- The project's JDK via sdkman (`sdk env` in the repo root): the Xcode build runs Gradle to produce the Kotlin framework.

## First build

1. In the repo root run `./gradlew :apps:clientApp:podInstall` once, or `pod install` inside `iosClient/`. This generates the `Pods/` xcconfigs the Xcode project references.
2. Open `iosClient/iosClient.xcworkspace` (the workspace, not the `.xcodeproj`).
3. Pick the **iosClient Debug** scheme and a simulator, then run.

The Xcode build invokes Gradle for the shared Kotlin code, so the first build takes several minutes.

## Simulator is the default, with one gap

The simulator is the supported path for everyday development and needs no Apple Developer account. One thing does not work there: **Tor**. `embed_libtor.sh` skips the Tor framework on simulator builds because `LibTor.xcframework` only ships device binaries, so pairing with a node over a `.onion` address can only be exercised on a real device. Clearnet pairing with a local or LAN node works fine in the simulator.

## Running on a real device

The project signs automatically with the Bisq Apple team, and the bundle ID `network.bisq.mobile.ios` is registered to that team. Unless you are a member, Xcode cannot provision a device build out of the box. Two ways around it:

### Option A: your own Apple team (paid account)

1. Copy `iosClient/Configuration/Local.xcconfig.example` to `iosClient/Configuration/Local.xcconfig`. The copy is git-ignored.
2. Set `DEVELOPMENT_TEAM` to your team ID and `BUNDLE_ID` to a reverse-DNS ID you own. Both are needed: App IDs are unique across all Apple teams, so the default one cannot be provisioned by yours.
3. Run the **iosClient Debug** scheme on your device. Xcode registers the device and creates the development profile on first run.

Notes:

- A **paid** Apple Developer account is required. The app's entitlements include push notifications, which free personal teams cannot sign. Push will register against your own team's APNs sandbox, so pushes relayed by a Bisq node will not reach your build; everything else works.
- The override only affects the Debug configuration. Release builds are pinned to the Bisq team and bundle ID in `Config.xcconfig` regardless of what `Local.xcconfig` says, so a contributor cannot sign or archive a release by accident.
- Never commit `Local.xcconfig`, and do not change the team in the Xcode UI: that edits `project.pbxproj` and shows up as a diff. The team lives in `Configuration/Config.xcconfig`.

### Option B: join the Bisq Apple team

If you contribute regularly, ask a maintainer to add you to the Apple Developer team with the Developer role. Automatic signing then works with no local changes, including push. The same membership makes you an internal TestFlight tester, so this is also the path for testing release builds on a device without building them yourself.

## Troubleshooting

- `No such module 'ClientApp'` when archiving: you archived the Debug target. Releases use the **iosClient Release** scheme; see `docs/releases/iOS-Release-Guide.md`.
- Link errors mentioning `kniprot_cocoapods_Sentry0_*` after Kotlin changes: stale cinterop cache. Run `./gradlew :apps:clientApp:clean`, then Product > Clean Build Folder in Xcode, and rebuild.
- "The sandbox is not in sync with the Podfile.lock": run `pod install` in `iosClient/` again.
- Apple Silicon CocoaPods issues: see the note in the README's known issues.
