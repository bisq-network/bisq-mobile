# iOS EU Release Guide

This guide documents the manual release process for Bisq Connect iOS for the EU market via AltStore PAL.

## Overview

Since iOS 17.4, Apple allows alternative app distribution in the EU under the Digital Markets Act (DMA). This requires:
1. Building with an Apple Distribution certificate
2. Submitting the IPA for Apple Notarization
3. Hosting the notarized IPA and AltStore manifest

**Key Benefits:**
- No 7-day expiration (unlike ad-hoc sideloading)
- No device UUID registration required
- Automated security review (not content review)

**Limitations:**
- EU-only (device region must be set to EU country)
- Requires iOS 17.4+ for installation via AltStore PAL

---

## iOS Release Process

### Step 1: Update Version Numbers

Edit `gradle.properties`:
```properties
client.ios.version=0.2.0
client.ios.version.code=4  # Increment for each build
```

### Step 2: Build the KMP Framework

```bash
cd /path/to/bisq-mobile

# Clean and build the iOS framework
./gradlew :apps:clientApp:podInstall

# If you encounter issues, try:
./gradlew :apps:clientApp:generateDummyFramework
cd iosClient && pod install && cd ..
```

### Step 3: Archive in Xcode

1. Open `iosClient/iosClient.xcworkspace` in Xcode
2. Select **Bisq Connect** scheme (not Debug)
3. Select **Any iOS Device (arm64)** as destination
4. Product → Archive
5. Wait for archive to complete

> **Note:** You only need to archive once. From the same archive, you'll export twice (EU and Non-EU).

### Step 4a: Export for EU (Notarization)

1. In Xcode Organizer (Window → Organizer), select the archive
2. Click **Distribute App**
3. Select **Custom**
4. Select **App Store Connect** ← This enables notarization
5. Select **Export** (not Upload)
6. Choose **Automatically manage signing**
7. Export to a folder (e.g., `~/Desktop/BisqConnect-EU/`)

This creates the EU IPA: `Bisq Connect.ipa` → rename to `BisqConnect-0.2.0.ipa`

### Step 4b: Export for Non-EU (Sideloading)

1. In Xcode Organizer, select the **same archive**
2. Click **Distribute App**
3. Select **Custom**
4. Select **Ad Hoc** ← For sideloading
5. Choose **Automatically manage signing** (or select your Ad-Hoc profile manually)
6. Export to a different folder (e.g., `~/Desktop/BisqConnect-Sideload/`)

This creates the sideload IPA: `Bisq Connect.ipa` → rename to `BisqConnect-0.2.0-sideload.ipa`

> **Key Difference:**
> - **App Store Connect** = Uses Distribution cert, can be notarized, works with AltStore PAL (EU)
> - **Ad Hoc** = Uses Distribution cert + Ad-Hoc profile, requires registered device UUIDs, works with AltStore free (worldwide)

### Step 5: Notarize the IPA

```bash
# Navigate to export folder
cd ~/Desktop/BisqConnect-Release/

# Submit for notarization
xcrun notarytool submit "Bisq Connect.ipa" \
  --apple-id "your-apple-id@email.com" \
  --team-id "YRAZCRBR9J" \
  --password "your-app-specific-password-setup-in-apple-console" \
  --wait

# Check status (if needed)
xcrun notarytool history \
  --apple-id "your-apple-id@email.com" \
  --team-id "YRAZCRBR9J" \
  --password "your-app-specific-password-setup-in-apple-console"
```

**Expected output:**
```
Conducting pre-submission checks for Bisq Connect.ipa...
Submitting Bisq Connect.ipa...
Submission ID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
Successfully uploaded file.
Waiting for processing to complete...
Processing complete.
  Status: Accepted
```

### Step 6: Staple the Notarization (Optional for IPA)

Note: Stapling is primarily for macOS apps. For iOS IPAs distributed via web, the notarization ticket is checked online.

```bash
xcrun stapler staple "Bisq Connect.ipa"
```

If it fails with "not supported", that's okay - proceed without stapling.

### Step 7: Rename and Prepare IPA

```bash
# Rename with version
mv "Bisq Connect.ipa" "BisqConnect-0.2.0.ipa"
```

### Step 8: Create GitHub Release

1. Go to [GitHub Releases](https://github.com/bisq-network/bisq-mobile/releases)
2. Click **Draft a new release**
3. Tag: `iconnect_0.2.0`
4. Title: `Bisq Connect iOS v0.2.0`
5. Upload `BisqConnect-0.2.0.ipa` as release asset as well as non-EU ipa `BisqConnect-0.2.0-sideload.ipa` (signed with ad-hoc)
6. Add release notes
7. Publish sideloading release

### Step 9: Update apps.json

Edit `docs/apps.json`:

1. Update the version in the `versions` array
2. Update `date` to current date
3. Update `size` (check actual file size in bytes)
4. Update `downloadURL` to match the GitHub release asset URL
5. Update `localizedDescription` with changelog

```bash
# Get file size in bytes for apps.json update
ls -l BisqConnect-0.2.0.ipa | awk '{print $5}'
```

### Step 10: Deploy to GitHub Pages

```bash
git add docs/apps.json docs/index.html docs/assets/
git commit -m "Release iOS v0.2.0 for EU distribution"
git push origin main
```

GitHub Pages will automatically update (may take a few minutes).

### Step 11: Verify Installation

1. On an EU-region iPhone with **AltStore PAL** installed
2. Go to Sources → Add Source
3. Enter: `https://bisq-network.github.io/bisq-mobile/apps.json`
4. Install Bisq Connect
5. Verify app launches and functions correctly

---

## Troubleshooting

### Notarization Rejected

Check the rejection reason:
```bash
xcrun notarytool log <submission-id> \
  --apple-id "your-apple-id@email.com" \
  --team-id "YRAZCRBR9J" \
  --password "your-app-specific-password"
```

Common issues:
- **Invalid signature**: Ensure you're using Apple Distribution certificate
- **Missing entitlements**: Check entitlements file
- **Hardened runtime**: Usually not required for iOS

### AltStore Can't Find Source

- Verify `apps.json` is valid JSON (use a JSON validator)
- Check GitHub Pages is enabled and deployed
- Ensure URLs in `apps.json` are correct and accessible

### App Won't Install

- Verify device region is set to EU country
- Ensure iOS version is 17.4+
- Check bundle ID matches exactly

---

## Updating Metadata & Screenshots

AltStore PAL displays app metadata from `docs/apps.json`. Here's how to update it:

### App Icon

The app icon is stored at `docs/assets/icon-512.png`.

**Requirements:**
- Size: 512x512 pixels (or 1024x1024 for high-res)
- Format: PNG
- No transparency (use solid background)

**To update:**
1. Export icon from design tool at 512x512
2. Replace `docs/assets/icon-512.png`
3. Commit and push

### Screenshots

Screenshots are optional but recommended for AltStore listings.

**Location:** `docs/assets/screenshots/`

**Requirements:**
- iPhone screenshots (recommended: 1290x2796 for iPhone 15 Pro Max)
- PNG format
- Name them descriptively: `01-home.png`, `02-connect.png`, etc.

**To add screenshots:**

1. Create the screenshots directory:
   ```bash
   mkdir -p docs/assets/screenshots
   ```

2. Add your screenshot files

3. Update `docs/apps.json` to reference them:
   ```json
   "screenshots": [
     "https://bisq-network.github.io/bisq-mobile/assets/screenshots/01-home.png",
     "https://bisq-network.github.io/bisq-mobile/assets/screenshots/02-connect.png"
   ]
   ```

4. Commit and push

**Tip:** You can reuse screenshots from the iOS App Store listing at `docs/listings/connect/ios/screenshots/` if available.

### App Description

The `localizedDescription` field in `apps.json` supports basic markdown:
- Use `\n` for line breaks
- Use `•` for bullet points
- Keep it concise but informative

### Version Release Notes

Each version entry has its own `localizedDescription` for release notes:
```json
{
  "version": "0.2.0",
  "localizedDescription": "# What's New\n\n• Feature 1\n• Bug fix 2",
  ...
}
```

---

## File Locations

| File | Purpose |
|------|---------|
| `docs/apps.json` | AltStore PAL source manifest |
| `docs/index.html` | Landing page with instructions |
| `docs/assets/icon-512.png` | App icon for AltStore |
| `docs/assets/screenshots/` | App screenshots (optional) |
| `docs/releases/ExportOptions-EU.plist` | Xcode export configuration |
| `gradle.properties` | Version numbers |
| `docs/listings/connect/ios/` | iOS App Store listing assets (reference) |

---

## Checklist

Before each release:

- [ ] Version bumped in `gradle.properties`
- [ ] KMP framework rebuilt
- [ ] Archive created in Xcode
- [ ] IPA exported with App Store Connect method
- [ ] Notarization submitted and accepted
- [ ] IPA uploaded to GitHub Release
- [ ] `apps.json` updated with new version
- [ ] Changes pushed to main branch
- [ ] GitHub Pages deployment verified
- [ ] Installation tested on EU device

