#!/usr/bin/env bash
# Publishes a notarized Bisq Connect IPA to the AltStore PAL source (EU + Japan).
#
# Runs the mechanical steps after Xcode and App Store Connect (see docs/releases/iOS-Release-Guide.md):
#   1. checks the IPA: bundle id, version and build match gradle.properties, Apple Distribution signature
#   2. attaches it as Bisq.Connect.ipa to the existing GitHub release connect_<version>
#   3. adds the version (and optionally a news entry) to docs/apps.json
# It does not commit: review `git diff docs/apps.json`, then commit and push.
#
# Usage:
#   scripts/ios-altstore-publish.sh <version> <notarized.ipa> --notes <notes.md> \
#       [--news-title <title> --news-caption <caption>] [--replace] [--dry-run]
#   scripts/ios-altstore-publish.sh --check-expiry
#
# --notes         markdown shown by AltStore for this version (the "What's New" text)
# --news-title    with --news-caption: also add an AltStore news entry that notifies users
# --replace       replace an existing Bisq.Connect.ipa asset and apps.json entry of the same version
# --dry-run       run the checks and print the apps.json entry, change nothing
# --check-expiry  warn when the last Bisq Connect release is old enough for its TestFlight build to expire soon
set -euo pipefail

repo=bisq-network/bisq-mobile
bundle_id=bisq.mobile.client.BisqConnect
asset_name=Bisq.Connect.ipa
apps_json=docs/apps.json
# TestFlight builds expire after 90 days. Warn early enough to ship a build before that.
expiry_warn_days=75

cd "$(git rev-parse --show-toplevel)"

fail() {
    echo "Error: $*" >&2
    exit 1
}

days_since() {
    python3 -I -c 'import sys, datetime
d = datetime.datetime.fromisoformat(sys.argv[1].replace("Z", "+00:00"))
print((datetime.datetime.now(datetime.timezone.utc) - d).days)' "$1"
}

# Prints the newest connect_* release other than $1 as "<tag> <publishedAt>"
latest_connect_release() {
    gh release list --repo "$repo" --limit 50 --json tagName,publishedAt \
        --jq "[.[] | select(.tagName | startswith(\"connect_\")) | select(.tagName != \"${1:-}\")][0] | \"\(.tagName) \(.publishedAt)\""
}

check_expiry() {
    local tag published days
    read -r tag published <<< "$(latest_connect_release "${1:-}")"
    [ -n "$tag" ] && [ "$tag" != null ] || fail "no connect_* release found"
    days=$(days_since "$published")
    if [ "$days" -ge "$expiry_warn_days" ]; then
        echo "WARNING: the last Bisq Connect release ($tag) is $days days old. TestFlight builds expire after 90 days:" \
            "upload a new build to TestFlight before day 90 so testers never see an expired beta." >&2
    else
        echo "Last Bisq Connect release: $tag, $days days ago (TestFlight build expires at 90)."
    fi
}

if [ "${1:-}" = --check-expiry ]; then
    check_expiry
    exit 0
fi

[ $# -ge 2 ] || fail "usage: $0 <version> <notarized.ipa> --notes <notes.md> [--news-title T --news-caption C] [--replace] [--dry-run]"
version=$1
ipa=$2
shift 2
notes='' news_title='' news_caption='' replace=false dry_run=false
while [ $# -gt 0 ]; do
    case $1 in
        --notes) notes=${2:?}; shift 2 ;;
        --news-title) news_title=${2:?}; shift 2 ;;
        --news-caption) news_caption=${2:?}; shift 2 ;;
        --replace) replace=true; shift ;;
        --dry-run) dry_run=true; shift ;;
        *) fail "unknown argument $1" ;;
    esac
done
[ -n "$notes" ] || fail "--notes <notes.md> is required"
[ -f "$notes" ] || fail "$notes not found"
[ -f "$ipa" ] || fail "$ipa not found"
if [[ (-n $news_title && -z $news_caption) || (-z $news_title && -n $news_caption) ]]; then
    fail "--news-title and --news-caption go together"
fi
for tool in gh plutil codesign unzip shasum curl python3; do
    command -v "$tool" > /dev/null || fail "$tool not found (this script runs on macOS)"
done

tag=connect_$version
expected_version=$(sed -nE 's/^client\.ios\.version=(.*)/\1/p' gradle.properties)
expected_build=$(sed -nE 's/^client\.ios\.version\.code=(.*)/\1/p' gradle.properties)
[ "$version" = "$expected_version" ] ||
    fail "version $version is not client.ios.version in gradle.properties ($expected_version)"

team_id=$(sed -nE 's/^DEVELOPMENT_TEAM=([A-Z0-9]+)$/\1/p' iosClient/Configuration/Config.xcconfig | head -1)
[ -n "$team_id" ] || fail "no DEVELOPMENT_TEAM in iosClient/Configuration/Config.xcconfig"

work_dir=$(mktemp -d)
trap 'rm -rf "$work_dir"' EXIT

# 1. The IPA. Checked and uploaded from one private copy, so the file cannot change in between.
ipa_copy=$work_dir/$asset_name
cp "$ipa" "$ipa_copy"
unzip -q "$ipa_copy" -d "$work_dir/ipa"
app=$(find "$work_dir/ipa/Payload" -maxdepth 1 -name '*.app' -type d | head -1)
[ -n "$app" ] || fail "$ipa has no Payload/*.app"
plist_value() { plutil -extract "$1" raw -o - "$app/Info.plist"; }
ipa_bundle_id=$(plist_value CFBundleIdentifier)
ipa_version=$(plist_value CFBundleShortVersionString)
ipa_build=$(plist_value CFBundleVersion)
min_os=$(plist_value MinimumOSVersion)
[ "$ipa_bundle_id" = "$bundle_id" ] || fail "bundle id is $ipa_bundle_id, expected $bundle_id"
[ "$ipa_version" = "$version" ] || fail "IPA version is $ipa_version, expected $version"
[ "$ipa_build" = "$expected_build" ] ||
    fail "IPA build is $ipa_build, expected client.ios.version.code $expected_build"
# A valid signature of our team, with the distribution identity AltStore PAL needs (debug and ad-hoc
# exports carry another one)
codesign --verify --deep --strict "$app" 2> /dev/null || fail "$ipa has no valid code signature"
signature=$(codesign -dvv "$app" 2>&1 || true)
signing_team=$(sed -nE 's/^TeamIdentifier=(.*)/\1/p' <<< "$signature")
[ "$signing_team" = "$team_id" ] || fail "$ipa is signed by team '$signing_team', expected $team_id"
authority=$(sed -nE 's/^Authority=(Apple Distribution: .*)/\1/p' <<< "$signature" | head -1)
[ -n "$authority" ] || fail "$ipa is not signed with an Apple Distribution certificate"
size=$(stat -f%z "$ipa_copy" 2> /dev/null || stat -c%s "$ipa_copy")
sha256=$(shasum -a 256 "$ipa_copy" | cut -d ' ' -f 1)
echo "IPA ok: $bundle_id $ipa_version ($ipa_build), min iOS $min_os, $size bytes, sha256 $sha256, team $team_id"
echo "Note: the script cannot check notarization. Use the IPA re-exported from the archive AFTER Apple approved the version."

# A gap of more than 90 days means TestFlight testers had an expired build in between
check_expiry "$tag"

# 2. The GitHub release
gh release view "$tag" --repo "$repo" > /dev/null 2>&1 ||
    fail "release $tag does not exist yet. Publish the Bisq Connect release (Android) first, then attach the IPA to it."
asset_exists=$(gh release view "$tag" --repo "$repo" --json assets --jq "[.assets[].name] | index(\"$asset_name\") != null")
if [ "$asset_exists" = true ] && [ "$replace" != true ]; then
    fail "$tag already has $asset_name. Pass --replace to overwrite it."
fi
download_url=https://github.com/$repo/releases/download/$tag/$asset_name

# 3. apps.json, written to a temporary file first and moved into place only after the upload worked
python3 -I - "$apps_json" "$work_dir/apps.json" "$version" "$notes" "$download_url" "$size" "$min_os" \
    "$replace" "$news_title" "$news_caption" "$bundle_id" << 'PY'
import datetime, json, sys

src, dst, version, notes, url, size, min_os, replace, news_title, news_caption, bundle_id = sys.argv[1:]
with open(src, encoding="utf-8") as f:
    source = json.load(f)
app = next((a for a in source["apps"] if a["bundleIdentifier"] == bundle_id), None)
if app is None:
    sys.exit(f"Error: {src} has no app {bundle_id}")
today = datetime.date.today().isoformat()
with open(notes, encoding="utf-8") as f:
    description = f.read().strip()
entry = {
    "version": version,
    "date": today,
    "localizedDescription": description,
    "downloadURL": url,
    "size": int(size),
    "minOSVersion": min_os,
}
versions = [v for v in app["versions"] if v["version"] != version]
if len(versions) != len(app["versions"]) and replace != "true":
    sys.exit(f"Error: {src} already lists version {version}. Pass --replace to overwrite it.")
app["versions"] = [entry] + versions
if news_title:
    identifier = f"bisq-connect-{version}"
    news = [n for n in source.get("news", []) if n.get("identifier") != identifier]
    source["news"] = [{
        "title": news_title,
        "identifier": identifier,
        "caption": news_caption,
        "date": today,
        "tintColor": app.get("tintColor", "#25B135"),
        "notify": True,
        "appID": bundle_id,
    }] + news
with open(dst, "w", encoding="utf-8") as f:
    f.write(json.dumps(source, indent=2, ensure_ascii=False) + "\n")
print(json.dumps(entry, indent=2, ensure_ascii=False))
PY

if [ "$dry_run" = true ]; then
    echo "Dry run: nothing uploaded, $apps_json unchanged."
    exit 0
fi

upload_args=(release upload "$tag" "$ipa_copy" --repo "$repo")
[ "$replace" = true ] && upload_args+=(--clobber)
gh "${upload_args[@]}"

# The URL AltStore will download must serve exactly the checked bytes
curl --silent --show-error --fail --location --proto '=https' --proto-redir '=https' --max-time 600 \
    --output "$work_dir/served.ipa" "$download_url" ||
    fail "could not download $download_url. $apps_json was not changed."
served_sha256=$(shasum -a 256 "$work_dir/served.ipa" | cut -d ' ' -f 1)
[ "$served_sha256" = "$sha256" ] ||
    fail "$download_url serves sha256 $served_sha256, expected $sha256. $apps_json was not changed."

mv "$work_dir/apps.json" "$apps_json"
echo "Done: $asset_name attached to $tag, $apps_json updated."
echo "Next: review 'git diff $apps_json', commit and push to main (GitHub Pages serves the source), then check it in AltStore."
