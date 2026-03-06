# iOS Release Guide

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

> **Note:** We are not doing this for now

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

### Step 5: Rename and Prepare IPA

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

## App reviews

For a project like ours, generally the main blocker gets to:


```
Guideline 3.1.5(iii) - which requires exchange services only be offered in countries or regions where the app has appropriate licensing and permissions to provide a cryptocurrency exchange.
```

Resulting in a demand for financial documents proving that we can operate in every country we are releasing
to.

From all the backs and forth with Apple, the one and only explanation that has proven to work is this one

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

