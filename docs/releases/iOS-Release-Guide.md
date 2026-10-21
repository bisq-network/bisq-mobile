# iOS Release Guide

Bisq Connect for iOS ships through two channels, both from the same Xcode archive:

| Channel | Who | How it gets there |
|---|---|---|
| **TestFlight beta** | Worldwide, external testers | Upload to App Store Connect, add the build to the external test group |
| **AltStore PAL** | EU and Japan | Same upload, notarized through App Store Connect, published in our AltStore source `docs/apps.json` |

The App Store is **not** a channel: Apple's review under guideline 3.1.5(iii) (May 2026) required per-country exchange licensing documentation, which a decentralized open-source project with no operating entity cannot provide. Our position is that Bisq Connect is client software, not an exchange (see [App reviews](#app-reviews)). Revisit around 2027-05. Ad-Hoc sideload IPAs are no longer produced.

A release takes one archive and one upload. The upload serves TestFlight directly, and after Apple notarizes the version, the same archive is exported again for AltStore PAL.

---

## Before you start (once per machine)

- Xcode signed in with the Bisq Connect Apple Developer team (`DEVELOPMENT_TEAM` in `iosClient/Configuration/Config.xcconfig`), with its Apple Distribution certificate available.
- `gh` logged in with write access to `bisq-network/bisq-mobile`.
- For the dSYM upload: `sentry-cli` (`brew install getsentry/tools/sentry-cli`), a Tor SOCKS proxy on `127.0.0.1:9050`, and an `apps/clientApp/sentry.properties` with your GlitchTip auth token (copy `apps/clientApp/sentry.properties.template`).

## Things that do NOT work (read before improvising)

These look right from Apple's generic docs but fail for this project:

- **`notarytool` does not notarize our IPA.** It fails with `Package … has no signed executables or bundles`, because the archive carries an App Store distribution profile, not an alternative-distribution one. Notarization goes through the App Store Connect web UI (step 6).
- **`stapler` does not work on `.ipa` files** and is not needed: AltStore PAL checks the notarization with Apple at install time.
- **App Store Connect has no "Alternative Distribution" tab and no "download notarized IPA" button.** The notarized IPA is exported again from the original Xcode archive (step 7).
- **Do not submit the version for App Store review.** That is the App Store review path, which we do not use. Only the notarization (alternative distribution) submission is used.
- **Archive the `iosClient Release` scheme.** The Debug scheme fails with `No such module 'ClientApp'` because the KMP framework is not linked for it.

---

## Release steps

### Step 1: Update version numbers

Edit `gradle.properties`:

```properties
client.ios.version=0.11.1
# Must increase with every upload to App Store Connect, also for a re-upload of the same version
client.ios.version.code=54
```

`client.ios.version` is the same version as the Android Bisq Connect release (`connect_<version>`).

### Step 2: Build the KMP framework

```bash
./gradlew :apps:clientApp:podInstall

# If you encounter issues, try:
./gradlew :apps:clientApp:generateDummyFramework
cd iosClient && pod install && cd ..
```

### Step 3: Archive in Xcode

1. Open `iosClient/iosClient.xcworkspace`.
2. Select the **`iosClient Release`** scheme and **Any iOS Device (arm64)** as destination.
3. Product → Archive, and wait for it to finish. The Organizer opens with the new archive.

Keep this archive: steps 4, 7 and 9 all use it.

### Step 4: Upload to App Store Connect

1. Organizer → select the archive → **Distribute App**.
2. **Custom** → **App Store Connect** → **Upload**.
3. Keep automatic signing and finish the upload.

After a few minutes of processing the build shows up in App Store Connect → Bisq Connect → TestFlight. The app declares `ITSAppUsesNonExemptEncryption = false`, so there is no export-compliance question.

### Step 5: Release on TestFlight (worldwide)

1. App Store Connect → Bisq Connect → **TestFlight** → select the new build.
2. Fill in **What to Test** with the user-facing release notes.
3. Add the build to the **external testing group** behind the public link `https://testflight.apple.com/join/abBmehCw`. A new version goes through Beta App Review first, usually within a day.

TestFlight builds expire after **90 days**. Releases normally come every few weeks, but if one is delayed, ship a new build before day 90 so the current beta stays installable. To check:

```bash
scripts/ios-altstore-publish.sh --check-expiry
```

It warns once the last `connect_*` release is 75 days old.

### Step 6: Notarize for AltStore PAL (App Store Connect web UI)

1. App Store Connect → My Apps → Bisq Connect → the **iOS App** section → **create a new version** (e.g. `0.11.1`).
2. Attach the build uploaded in step 4.
3. Submit it through the **alternative distribution / notarization** path, never as an App Store review submission.
4. Wait for Apple's approval email. Notarization runs on Apple's side and takes hours, not minutes.

### Step 7: Export the notarized IPA

1. Organizer → select the **same archive** as in step 4. Once Apple approved the version it shows as notarized.
2. **Distribute App** → **Custom** → **Direct Distribution** (or the alternative-distribution export Xcode offers for the notarized archive).
3. Export to a folder. The result is `Bisq Connect.ipa`.

### Step 8: Publish to the AltStore source

The GitHub release `connect_<version>` must already exist (it is created by the Android Bisq Connect release). Write the AltStore release notes to a markdown file, in the format of earlier entries in `docs/apps.json` (`# Bisq Connect v<version>`, `## What's New` with `•` bullets, `## Requirements`), then:

```bash
scripts/ios-altstore-publish.sh 0.11.1 "path/to/Bisq Connect.ipa" --notes notes.md \
    --news-title "Bisq Connect 0.11.1 — <headline>" --news-caption "<one or two sentences>"
```

Add `--dry-run` first to see the checks and the new `apps.json` entry without changing anything. The script:

- checks the IPA: bundle id, version and build match `gradle.properties`, a valid signature of our team with the Apple Distribution identity (it cannot check notarization, so make sure you use the step 7 export)
- attaches it to `connect_<version>` as `Bisq.Connect.ipa` (`--replace` to overwrite an existing one)
- downloads the asset again and confirms its SHA-256 matches the checked IPA
- adds the version (and the news entry, which notifies AltStore users) at the top of `docs/apps.json`, with the real size, today's date and the IPA's minimum iOS version
- warns when the previous release is more than 75 days old, i.e. TestFlight testers may have had an expired build

It does not commit. Review `git diff docs/apps.json`, commit and push to `main`. GitHub Pages serves the source at `https://bisq-network.github.io/bisq-mobile/apps.json` a few minutes later.

### Step 9: Upload the dSYMs to GlitchTip

So that iOS crash reports in GlitchTip are symbolicated, upload the dSYMs of the same archive to the `bisq-connect-ios` project, through Tor:

```bash
SENTRY_PROPERTIES=apps/clientApp/sentry.properties \
http_proxy=socks5h://127.0.0.1:9050 https_proxy=socks5h://127.0.0.1:9050 NO_PROXY= no_proxy= \
sentry-cli debug-files upload --org bisq --project bisq-connect-ios \
    ~/Library/Developer/Xcode/Archives/<date>/<archive>.xcarchive/dSYMs
```

The `dSYMs` folder includes the one `embed_libtor.sh` generates for LibTor. `NO_PROXY` is cleared so that a shell setting cannot route the upload around Tor.

### Step 10: Verify

- **TestFlight:** the build is available to the external group and installs from the public link.
- **AltStore PAL:** on a device in the EU or Japan, AltStore shows the new version and installs it.
- **GlitchTip:** `bisq-connect-ios` lists the uploaded debug files.

---

## Install counts

- **TestFlight:** App Store Connect → Bisq Connect → TestFlight → the build shows installs, sessions and crashes per build, and the external group shows its tester count. This is the iOS number for the monthly report (a manual input, see `scripts/monthly-report/`).
- **AltStore PAL:** not measurable from our side. AltStore mirrors the IPA, so the download count of `Bisq.Connect.ipa` on GitHub shows only the few fetches AltStore makes to ingest a release, not installs.

---

## Troubleshooting

### Notarization rejected

Check the reason in App Store Connect → Bisq Connect → Activity → the build → View Details. Common issues:

- **Invalid signature:** the archive must be signed with the Apple Distribution certificate.
- **Missing entitlements:** check `iosClient/Bisq ConnectRelease.entitlements`.
- **Bundle ID mismatch:** it must be `bisq.mobile.client.BisqConnect`.

### AltStore can't find the source

- Validate `docs/apps.json` (`python3 -m json.tool docs/apps.json > /dev/null`).
- Check that GitHub Pages deployed the latest `main`.
- Check that every URL in `apps.json` is reachable.

### App won't install from AltStore

- The Apple ID region and the device's location must be in the EU or Japan (the UK is not eligible).
- iOS 17.4 or later is required for AltStore PAL.
- The IPA must be the notarized export from step 7.

---

## Updating metadata & screenshots

AltStore PAL displays app metadata from `docs/apps.json`.

### App icon

The app icon is stored at `docs/assets/icon-512.png`: 512x512 (or 1024x1024) PNG without transparency. Replace the file, then commit and push.

### Screenshots

Screenshots live in `docs/assets/screenshots/` (PNG, recommended 1290x2796) and are listed in the `screenshots` array of the app in `docs/apps.json`:

```json
"screenshots": [
  "https://bisq-network.github.io/bisq-mobile/assets/screenshots/screenshot_1.png"
]
```

Screenshots of the store listing in `apps/clientApp/src/main/ios/listings/en-US/images/phoneScreenshots/` can be reused.

### App description

The app's `localizedDescription` in `apps.json` supports basic markdown: `\n` for line breaks, `•` for bullets. Each version entry has its own `localizedDescription` with the release notes, which `scripts/ios-altstore-publish.sh` takes from the `--notes` file.

---

## File locations

| File | Purpose |
|------|---------|
| `docs/apps.json` | AltStore PAL source (EU + Japan) |
| `docs/index.html` | Install page with TestFlight and AltStore PAL instructions |
| `docs/assets/icon-512.png` | App icon for AltStore |
| `docs/assets/screenshots/` | App screenshots for AltStore |
| `scripts/ios-altstore-publish.sh` | Publishes the notarized IPA and updates `apps.json`; `--check-expiry` for TestFlight |
| `docs/releases/ExportOptions-EU.plist` | Export options for a command-line `xcodebuild -exportArchive`; the Organizer flow above does not use it |
| `gradle.properties` | Version numbers |
| `apps/clientApp/src/main/ios/listings/en-US/` | iOS store listing assets (fastlane format) |

---

## App reviews

> **Status (2026-05-27):** The App Store path is **abandoned**. Apple's review under guideline 3.1.5(iii) required per-country exchange licensing documentation, which a decentralized open-source project with no operating entity cannot provide. Bisq Connect is client software for a user's own node, not an exchange (see the template below). The channels are the TestFlight beta and AltStore PAL (EU and Japan). Revisit annually (~2027-05).
>
> The template below got TestFlight Beta App Review approved but **no longer works** for a full App Store review as of May 2026. It is kept for reference and for a future App Store revisit.

For a project like ours, the main blocker is:

```
Guideline 3.1.5(iii) - which requires exchange services only be offered in countries or regions where the app has appropriate licensing and permissions to provide a cryptocurrency exchange.
```

Resulting in a demand for financial documents proving that we can operate in every country we are releasing to.

The following clarification template was used to get TestFlight beta testing approved:

```
Subject: Clarification Regarding Cryptocurrency Exchange Services – Bisq Connect

Bisq Connect does not provide cryptocurrency exchange services, operate an exchange, or act as a broker or financial intermediary.

Bisq is a fully decentralized, peer-to-peer open-source protocol. There is no operating company, no central matching engine, and no custodial entity. The Bisq network is composed of independently operated nodes run by individual users.

Bisq Connect is a non-custodial remote interface client that allows a user to connect to their own self-hosted Bisq “trusted node” via secure WebSocket connection.
Important clarifications:

 - The app does not custody user funds.
 - The app does not hold private keys.
 - The app does not execute trades.
 - The app does not operate or control any exchange infrastructure.
 - The app does not match counterparties.
 - The app does not intermediate transactions.
 - The app does not provide exchange services to users.

All trading logic, offer creation, order matching, transaction signing, and settlement occur entirely on the user-controlled node software outside of the app.

Bisq Connect functions similarly to a remote desktop client or node management interface. It allows users to remotely monitor and interact with software they independently operate.

The app is open source and available at:
[GitHub](https://github.com/bisq-network/bisq-mobile)

We hope this clarifies that Bisq Connect is client software for interacting with a decentralized protocol, not a cryptocurrency exchange service under Guideline 3.1.5(iii).
Please let us know if you require any additional technical documentation.

```

---

## Checklist

Before each release:

- [ ] `client.ios.version` and `client.ios.version.code` bumped in `gradle.properties`
- [ ] KMP framework rebuilt, archive created with the `iosClient Release` scheme
- [ ] Build uploaded to App Store Connect
- [ ] TestFlight: What to Test filled in, build added to the external group, Beta App Review passed
- [ ] Notarization submitted from the new version in App Store Connect and approved
- [ ] Notarized IPA exported from the same archive
- [ ] `scripts/ios-altstore-publish.sh` run (IPA attached to `connect_<version>`, `apps.json` updated)
- [ ] `apps.json` committed and pushed to `main`, GitHub Pages deployed
- [ ] dSYMs uploaded to GlitchTip `bisq-connect-ios`
- [ ] Installed from TestFlight, and from AltStore PAL on an EU or Japan device
