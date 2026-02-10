# iOS Release Guide

<!--
================================================================================
TODO: UPDATE AFTER DISTRIBUTION METHODS ARE CONFIRMED
================================================================================
This guide documents distribution methods that are PENDING confirmation. See:
https://github.com/bisq-network/bisq-mobile/issues/936#issuecomment-3858661062

Current status (as of Feb 2026):
- TestFlight: Pending Apple review (may or may not pass external testing)
- EU AltStore PAL: Requires notarization via App Store Connect (untested)
- Non-EU Sideloading: Available but limited (100 devices, 7-day refresh)

Once we confirm which distribution methods work, update this guide accordingly.
================================================================================
-->

This guide documents the manual release process for Bisq Connect iOS.

## Distribution Methods

### EU Market (AltStore PAL) - PENDING

Since iOS 17.4, Apple allows alternative app distribution in the EU under the Digital Markets Act (DMA).

**Requirements:**
1. App must be **notarized by Apple** via App Store Connect
2. Must agree to **Alternative Terms Addendum for Apps in the EU**
3. Notarized IPA hosted on GitHub, referenced in `apps.json`

**Key Benefits:**
- No 7-day expiration (unlike ad-hoc sideloading)
- No device UUID registration required
- Notarization review is less strict than App Store review (security-focused, not content-focused)

**Limitations:**
- EU-only (device region must be set to EU country)
- Requires iOS 17.4+ for installation via AltStore PAL
- Must go through App Store Connect (no command-line notarization for iOS)

### Non-EU Market (Sideloading)

For users outside the EU, we provide Ad-Hoc signed IPAs.

**Requirements:**
- Device UUIDs must be registered in Apple Developer Portal
- Users install via AltStore (free version) or similar tools

**Limitations:**
- 100 device limit per year
- Apps require weekly refresh (7-day certificate)

---

## ⚠️ Important: iOS Notarization Reality

**iOS notarization is completely different from macOS notarization!**

| Platform | Notarization Method |
|----------|---------------------|
| **macOS** | `xcrun notarytool` command line ✅ |
| **iOS** | App Store Connect web interface only ❌ no CLI |

The `xcrun notarytool` command does **NOT** work for iOS IPAs. All iOS apps for EU alternative distribution must be uploaded to App Store Connect.

### Notarization vs App Store Review

| Aspect | App Store Review | Notarization Only |
|--------|------------------|-------------------|
| Content policies | ✅ Enforced | ❌ Not checked |
| Business model | ✅ Reviewed | ❌ Not checked |
| Malware/security | ✅ Checked | ✅ Checked |
| Privacy basics | ✅ Checked | ✅ Checked |
| Fraud prevention | ✅ Checked | ✅ Checked |

**If your app was rejected from the App Store for content/policy reasons, it may still pass notarization.**

### Web Distribution Eligibility (Direct from Website)

To distribute directly from your own website (not via AltStore PAL), Apple requires:
- ❌ **Organization account** (Individual accounts don't qualify)
- ✅ **2+ years continuous membership**
- ❌ **1M+ first annual installs in EU** in prior calendar year

Most small developers won't qualify for Web Distribution. The alternative is to distribute via a marketplace like AltStore PAL.

---

## iOS Release Process

### Step 1: Update Version Numbers

Edit `gradle.properties`:
```properties
client.ios.version=0.2.0
# Increment for each build
client.ios.version.code=4
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

### Step 5: Notarize the IPA via App Store Connect

> ⚠️ **Important:** iOS notarization must be done through App Store Connect, NOT via `xcrun notarytool`.
> The `xcrun notarytool` command only works for macOS apps.

**Option A: Using Transporter App (Recommended)**

1. Download [Transporter](https://apps.apple.com/app/transporter/id1450874784) from the Mac App Store
2. Sign in with your Apple Developer account
3. Drag and drop the IPA file into Transporter
4. Click "Deliver" to upload to App Store Connect
5. Wait for processing (usually 5-15 minutes)

**Option B: Using App Store Connect Web Interface**

1. Go to [App Store Connect](https://appstoreconnect.apple.com)
2. Navigate to your app → "TestFlight" or "Distribution" section
3. Upload the IPA using the web interface
4. Wait for processing

**After Upload:**

1. Go to App Store Connect → Your App → "Distribution" tab
2. Select "Notarization Only" (not App Store submission)
3. Wait for notarization to complete (usually 5-30 minutes)
4. Download the notarized IPA from App Store Connect

> **Note:** The exact UI flow may vary. Apple's documentation for EU alternative distribution
> is at: https://developer.apple.com/help/app-store-connect/distribute-in-the-european-union/

### Step 6: Rename and Prepare IPA

```bash
# Rename with version
mv "Bisq Connect.ipa" "BisqConnect-0.2.0.ipa"
```

### Step 7: Create GitHub Release

1. Go to [GitHub Releases](https://github.com/bisq-network/bisq-mobile/releases)
2. Click **Draft a new release**
3. Tag: `iconnect_0.2.0`
4. Title: `Bisq Connect iOS v0.2.0`
5. Upload `BisqConnect-0.2.0.ipa` as release asset as well as non-EU ipa `BisqConnect-0.2.0-sideload.ipa` (signed with ad-hoc)
6. Add release notes
7. Publish sideloading release

### Step 8: Update apps.json

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

### Step 9: Deploy to GitHub Pages

```bash
git add docs/apps.json docs/index.html docs/assets/
git commit -m "Release iOS v0.2.0 for EU distribution"
git push origin main
```

GitHub Pages will automatically update (may take a few minutes).

### Step 10: Verify Installation

1. On an EU-region iPhone with **AltStore PAL** installed
2. Go to Sources → Add Source
3. Enter: `https://bisq-network.github.io/bisq-mobile/apps.json`
4. Install Bisq Connect
5. Verify app launches and functions correctly

---

## Troubleshooting

### Notarization Rejected

Check the rejection reason in App Store Connect:
1. Go to App Store Connect → Your App → Activity
2. Find the failed build and click "View Details"
3. Review the notarization issues listed

Common issues:
- **Invalid signature**: Ensure you're using Apple Distribution certificate
- **Missing entitlements**: Check entitlements file
- **Bundle ID mismatch**: Ensure bundle ID matches your App Store Connect app

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

