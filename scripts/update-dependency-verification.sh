#!/usr/bin/env bash
# Regenerates the SHA-256 checksums of the bisq2 jars in gradle/verification-metadata.xml.
#
# Gradle fails the build when a bisq2 jar's checksum differs from the one recorded there. That is
# the only check on them: JitPack builds them on request and nobody signs them. Every other
# dependency is trusted by the file's <configuration>, for now. Run this after changing
# bisq-core-commit or the bisq2 modules the node app uses. It fails when a file it had recorded
# now has other bytes although its version did not change: JitPack serves other content than
# before, which must be explained before the file is committed.
#
# With --bisq2-repo, it also builds the pinned commit from source and compares the jars with
# JitPack's. That checks that JitPack built them from the source, which the checksums alone cannot.
# It takes the path of an existing local bisq2 clone and clones nothing: the commit is checked out
# into a temporary worktree of that clone, which leaves its branch and working tree as they are,
# and fetched from GitHub only when the clone lacks it.
#
# Usage: scripts/update-dependency-verification.sh [--bisq2-repo <path of a local bisq2 clone>]
set -euo pipefail

usage() {
    echo "Usage: $0 [--bisq2-repo <path of a local bisq2 clone>]" >&2
    exit 2
}

bisq2_repo=
while [ $# -gt 0 ]; do
    case $1 in
        --bisq2-repo)
            [ $# -ge 2 ] || usage
            bisq2_repo=$(cd "$2" && pwd)
            git -C "$bisq2_repo" rev-parse --git-dir > /dev/null
            shift 2
            ;;
        *) usage ;;
    esac
done

cd "$(dirname "$0")/.."

readonly metadata=gradle/verification-metadata.xml

# java.util.Properties, which settings.gradle.kts reads it with, takes =, : or white space between
# key and value. The check of the recorded jars further down catches any form this misses.
if grep -Eqis '^[[:space:]]*BISQ2_FROM_MAVEN_LOCAL([[:space:]]*[=:][[:space:]]*|[[:space:]]+)true[[:space:]]*$' local.properties; then
    echo "BISQ2_FROM_MAVEN_LOCAL is set in local.properties. Unset it: the checksums must be the JitPack ones." >&2
    exit 1
fi

# JitPack builds a commit on the first request for it, and holds that request open for longer than
# Gradle waits. Start the build and wait for it here instead.
bisq2_version=$(sed -nE 's/^bisq-core-commit = "([^"]+)".*/\1/p' gradle/libs.versions.toml)
[[ $bisq2_version =~ ^[0-9a-f]{10}$ ]] || {
    echo "bisq-core-commit in gradle/libs.versions.toml must be the first 10 characters of a commit hash" >&2
    exit 1
}
readonly jitpack_build=https://jitpack.io/com/github/bisq-network/bisq2/$bisq2_version
echo "Waiting for JitPack to build bisq2 $bisq2_version"
curl --silent --output /dev/null --max-time 30 "$jitpack_build/bisq2-$bisq2_version.pom" || true
deadline=$((SECONDS + 3600))
failed_since=
last_status=
while :; do
    build=$(curl --silent --max-time 60 "https://jitpack.io/api/builds/com.github.bisq-network/bisq2/$bisq2_version" || true)
    status=$(sed -nE 's/.*"status" *: *"([^"]*)".*/\1/p' <<< "$build")
    [ "$status" = ok ] && break
    [ "$status" = "$last_status" ] || echo "JitPack: ${status:-no answer}"
    last_status=$status
    # Any other status is a failure, but not always a final one: JitPack has turned an "Error"
    # into "ok" minutes later by retrying the build itself
    case $status in
        "" | none | Queued | Building) failed_since= ;;
        *) failed_since=${failed_since:-$SECONDS} ;;
    esac
    if { [ -n "$failed_since" ] && [ $((SECONDS - failed_since)) -ge 900 ]; } || [ "$SECONDS" -ge "$deadline" ]; then
        echo "JitPack has not built bisq2 $bisq2_version. Build log: $jitpack_build/build.log" >&2
        exit 1
    fi
    sleep 30
done
# The full hash of the commit JitPack built
bisq2_commit=$(sed -nE 's/.*"commit" *: *"([0-9a-f]{40})".*/\1/p' <<< "$build")
[ "${bisq2_commit:0:10}" = "$bisq2_version" ] || {
    echo "JitPack reports commit '$bisq2_commit' for bisq2 $bisq2_version" >&2
    exit 1
}
grep -q "$bisq2_commit" gradle/libs.versions.toml || {
    echo "Write the full hash $bisq2_commit next to bisq-core-commit in gradle/libs.versions.toml" >&2
    exit 1
}

# The checksums only pin what JitPack built. That must be a commit of the for-mobile branch of the
# release in bisq-core: JitPack builds whatever commit the version names, and a 10 character prefix
# is short enough to grind a colliding commit for.
branch=for-mobile-based-on-$(sed -nE 's/^bisq-core = "([^"]+)".*/\1/p' gradle/libs.versions.toml)
# With GITHUB_TOKEN set, as in CI, the request counts against the token's rate limit instead of the
# 60 per hour GitHub allows an IP address without one
github_auth=()
[ -z "${GITHUB_TOKEN:-}" ] || github_auth=(-H "Authorization: Bearer $GITHUB_TOKEN")
compare=$(curl --silent --show-error --fail --max-time 60 -H "Accept: application/vnd.github+json" \
    ${github_auth[@]+"${github_auth[@]}"} \
    "https://api.github.com/repos/bisq-network/bisq2/compare/$branch...$bisq2_commit?per_page=1") || {
    echo "Could not ask GitHub whether $bisq2_commit is on bisq-network/bisq2 $branch" >&2
    exit 1
}
# The Gradle runs below execute the build scripts of the change under test, so keep the token from them
unset GITHUB_TOKEN
# The status at the top level of the answer, not those of the files further in
case $(sed -nE 's/^  "status": "([a-z]+)".*/\1/p' <<< "$compare") in
    behind | identical) ;;
    *)
        echo "$bisq2_commit is not on bisq-network/bisq2 $branch" >&2
        exit 1
        ;;
esac

work_dir=$(mktemp -d)
previous=$work_dir/previous.xml
cp "$metadata" "$previous"
checked=false
worktree=
worktree_admin=
clean_up() {
    # Unless every check passed, the old file stays. A run that fails midway would leave the file
    # without its checksums, and a check that fails would leave the new checksums, which the next
    # run would take as the previous ones and pass.
    if [ "$checked" != true ]; then
        cp "$previous" "$metadata"
        echo "Failed. Restored the previous $metadata." >&2
    fi
    if [ -n "$worktree" ] && ! git -C "$bisq2_repo" worktree remove --force "$worktree"; then
        # Remove its registration in the clone, which would dangle once the files are gone. Only
        # this one: git worktree prune would also drop registrations this script did not create.
        case $worktree_admin in */worktrees/*) rm -rf "$worktree_admin" ;; esac
    fi
    rm -rf "$work_dir"
}
trap clean_up EXIT

# One line per checksum: group:name:version/file sha256
checksums() {
    awk '
        /<component / {
            match($0, /group="[^"]*"/); group = substr($0, RSTART + 7, RLENGTH - 8)
            match($0, /name="[^"]*"/); name = substr($0, RSTART + 6, RLENGTH - 7)
            match($0, /version="[^"]*"/); version = substr($0, RSTART + 9, RLENGTH - 10)
        }
        /<artifact / { match($0, /name="[^"]*"/); file = substr($0, RSTART + 6, RLENGTH - 7) }
        /<sha256 / {
            match($0, /value="[^"]*"/)
            print group ":" name ":" version "/" file, substr($0, RSTART + 7, RLENGTH - 8)
        }
    ' "$1" | LC_ALL=C sort
}

# Gradle records only the files a build resolves, so resolve the bisq2 jars of the node app, which
# downloads them if they are not in Gradle's cache yet
init_script=$work_dir/resolve-bisq2-jars.gradle
cat > "$init_script" <<'GRADLE'
allprojects {
    if (path == ':apps:nodeApp') {
        tasks.register('resolveBisq2Jars') {
            def classpath = configurations.named('releaseRuntimeClasspath')
            doLast {
                classpath.get().incoming.artifactView {
                    componentFilter {
                        it instanceof org.gradle.api.artifacts.component.ModuleComponentIdentifier &&
                            it.group == 'com.github.bisq-network.bisq2'
                    }
                }.files.files
            }
        }
    }
}
GRADLE

# Rebuilt from scratch, so it lists only what the build uses. Gradle writes no checksums for the
# artifacts the <configuration> trusts, so only the bisq2 jars and their metadata end up in it.
# --refresh-dependencies, so the checksums are of what JitPack serves now: Gradle never downloads
# a version it has cached again, and would otherwise record the bytes JitPack served before.
awk '/<\/components>/ { skip = 0 } !skip { print } /<components>/ { skip = 1 }' "$previous" > "$metadata"
./gradlew --no-configuration-cache --init-script "$init_script" --write-verification-metadata sha256 \
    --refresh-dependencies --quiet :apps:nodeApp:resolveBisq2Jars

checksums "$previous" > "$work_dir/previous.txt"
checksums "$metadata" > "$work_dir/current.txt"
# A build that resolved none of JitPack's bisq2 jars, against a local publishAll for one, records no
# checksums at all, and every check below would pass on nothing
grep -q "^com\.github\.bisq-network\.bisq2:[^:]*:$bisq2_version/[^ ]*\.jar " "$work_dir/current.txt" || {
    echo "No bisq2 jar of JitPack's $bisq2_version was recorded. Is BISQ2_FROM_MAVEN_LOCAL set in local.properties?" >&2
    exit 1
}
cut -d ' ' -f 1 "$work_dir/previous.txt" | LC_ALL=C sort -u > "$work_dir/previous-files.txt"
cut -d ' ' -f 1 "$work_dir/current.txt" | LC_ALL=C sort -u > "$work_dir/current-files.txt"
echo "Updated $metadata:" \
    "$(LC_ALL=C comm -13 "$work_dir/previous-files.txt" "$work_dir/current-files.txt" | wc -l | tr -d ' ') files added," \
    "$(LC_ALL=C comm -23 "$work_dir/previous-files.txt" "$work_dir/current-files.txt" | wc -l | tr -d ' ') removed."
# The same file of the same version with other bytes than before: JitPack changed what it serves
changed=$(LC_ALL=C join "$work_dir/previous.txt" "$work_dir/current.txt" | awk '$2 != $3 { print "  " $1 }')
if [ -n "$changed" ]; then
    echo "These files changed although their versions did not. Find out why before committing:" >&2
    echo "$changed" >&2
    exit 1
fi

if [ -z "$bisq2_repo" ]; then
    checked=true
    exit 0
fi

sha256_of() {
    if command -v sha256sum > /dev/null; then sha256sum "$1"; else shasum -a 256 "$1"; fi | cut -d ' ' -f 1
}

# The content of a jar, whatever compressed it: the names of its entries, and the SHA-256 and name
# of each extracted file. Not the sizes and CRC-32 values the jar lists, which its maker chooses.
entries_of() {
    local dir
    dir=$(mktemp -d "$work_dir/entries.XXXXXX")
    unzip -Z1 "$1" | LC_ALL=C sort
    # -o and no stdin: a jar with a name twice would otherwise ask, reading the caller's input. A jar
    # that does not extract cleanly, a bad CRC for one, matches no other.
    unzip -q -o "$1" -d "$dir" < /dev/null || echo "not extracted cleanly: $1"
    (cd "$dir" && find . -type f -print0 | if command -v sha256sum > /dev/null; then
        xargs -0 sha256sum
    else
        xargs -0 shasum -a 256
    fi) | LC_ALL=C sort
    rm -rf "$dir"
}

git -C "$bisq2_repo" cat-file -e "$bisq2_commit^{commit}" 2> /dev/null ||
    git -C "$bisq2_repo" fetch --quiet --no-write-fetch-head https://github.com/bisq-network/bisq2.git "$bisq2_commit"
git -C "$bisq2_repo" worktree add --quiet --detach "$work_dir/bisq2" "$bisq2_commit"
worktree=$work_dir/bisq2
worktree_admin=$(git -C "$worktree" rev-parse --absolute-git-dir)

# The command of bisq2's jitpack.yml, into an empty repository instead of ~/.m2. Without the build
# cache, and without a daemon, which could be a running one that would not see the repository.
echo "Building bisq2 $bisq2_commit from source, which takes a few minutes"
local_repo=$work_dir/local-repo
(cd "$worktree" && JAVA_TOOL_OPTIONS="-Dmaven.repo.local=$local_repo" ./gradlew --no-daemon --no-build-cache \
    --console=plain "-Dmaven.repo.local=$local_repo" publishAll) > "$work_dir/bisq2-build.log" 2>&1 || {
    tail -n 40 "$work_dir/bisq2-build.log" >&2
    echo "The build of bisq2 from source failed." >&2
    exit 1
}
local_version=$(sed -nE 's/^version=(.*)/\1/p' "$worktree/gradle.properties")

# bisq2's tor-binary plugin packs a Tor binary into the tor jar as tor.zip on macOS and Windows,
# not on Linux, where JitPack builds. Off Linux, that one entry of the local jar is not compared.
local_only_entries='^$'
if [ "$(uname -s)" != Linux ]; then
    local_only_entries='^(tor\.zip|[0-9a-f]{64}  \./tor\.zip)$'
fi

identical=0 same_entries=0 platform=0 different=0
while read -r file sha; do
    case $file in *.jar) ;; *) continue ;; esac
    name=${file#*:}
    name=${name%%:*}
    jitpack_file=${file##*/}
    local_jar=$(find "$local_repo" -path "*/$name/$local_version/$name-$local_version${jitpack_file#"$name-$bisq2_version"}")
    if [ -z "$local_jar" ] || [ "$(wc -l <<< "$local_jar")" -ne 1 ]; then
        echo "MISSING     $jitpack_file has no single counterpart in the build from source"
        different=$((different + 1))
    elif [ "$(sha256_of "$local_jar")" = "$sha" ]; then
        identical=$((identical + 1))
    else
        curl --silent --show-error --fail --location --max-time 300 --output "$work_dir/$jitpack_file" \
            "https://jitpack.io/com/github/bisq-network/bisq2/$name/$bisq2_version/$jitpack_file" || {
            echo "Could not download $jitpack_file from JitPack to compare it with the build from source" >&2
            exit 1
        }
        if [ "$(sha256_of "$work_dir/$jitpack_file")" != "$sha" ]; then
            echo "DIFFERENT   JitPack now serves other bytes for $jitpack_file than were recorded"
            different=$((different + 1))
        elif [ "$(entries_of "$local_jar")" = "$(entries_of "$work_dir/$jitpack_file")" ]; then
            echo "COMPRESSION $jitpack_file has the same entries, compressed differently"
            same_entries=$((same_entries + 1))
        elif [ "$(entries_of "$local_jar" | grep -Ev "$local_only_entries")" = "$(entries_of "$work_dir/$jitpack_file")" ]; then
            echo "PLATFORM    $jitpack_file has the same entries, apart from the Tor binary only a build off Linux packs"
            platform=$((platform + 1))
        else
            echo "DIFFERENT   $jitpack_file"
            different=$((different + 1))
        fi
    fi
done < "$work_dir/current.txt"

echo "Built from source: $identical jars identical with JitPack's, $same_entries with the same entries," \
    "$platform with the same entries apart from the Tor binary, $different different."
if [ "$same_entries" -gt 0 ]; then
    # bisq2's jitpack.yml: JitPack's OpenJDK and Temurin bundle their own zlib, Zulu uses the system's
    echo "The JDK running Gradle compresses the jars. Build with Temurin 21 for byte-identical ones."
fi
if [ "$different" -gt 0 ]; then
    echo "The JitPack jars differ from a build of the source. Find out why before committing." >&2
    exit 1
fi
checked=true
