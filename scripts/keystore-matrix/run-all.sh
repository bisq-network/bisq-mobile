#!/usr/bin/env bash
# Sequential release-signing / CI matrix. One Gradle invocation at a time.
# Run from the repo: ./scripts/keystore-matrix/run-all.sh
# Rewrites local.properties per case and restores the original on exit.
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
KS_DIR="$SCRIPT_DIR/ks"
BASE_PROPS="$SCRIPT_DIR/base.properties"
PROPS_BACKUP="$SCRIPT_DIR/local.properties.bak"
PROPS_SNAPSHOT="$SCRIPT_DIR/local.properties.snapshot"
LOGDIR="$SCRIPT_DIR/logs"
RESULT="$SCRIPT_DIR/results/matrix.tsv"
GSERVICES="$ROOT/apps/clientApp/google-services.json"
GSERVICES_STUB="$SCRIPT_DIR/google-services.stub.json"
GSERVICES_MARKER="$SCRIPT_DIR/google-services.created"
RESULT_HEADER=$'section\tid\tks\tcompanions\toptin\ttask\textra\texit\texpected\tverdict\tnotes'

if [[ ! -f "$ROOT/local.properties" ]]; then
  echo "local.properties is required at $ROOT" >&2
  exit 1
fi
if [[ ! -x "$ROOT/gradlew" ]]; then
  echo "gradlew not found at $ROOT" >&2
  exit 1
fi

mkdir -p "$KS_DIR" "$LOGDIR" "$SCRIPT_DIR/results"

# A run killed before its EXIT trap fired (SIGKILL, crash) leaves local.properties
# holding matrix values. Recover from the snapshot before anything below copies
# that file over a restore point. Assumes no other run is in flight.
if [[ -f "$PROPS_SNAPSHOT" ]]; then
  echo "recovering local.properties from $PROPS_SNAPSHOT (previous run did not clean up)" >&2
  cp "$PROPS_SNAPSHOT" "$ROOT/local.properties"
  rm -f "$PROPS_SNAPSHOT"
fi

# --fresh wipes prior results. --resume (or an existing matrix) skips PASS rows and retries the rest.
RESUME=0
case "${1:-}" in
  --fresh) RESUME=0 ;;
  --resume) RESUME=1 ;;
  *)
    if [[ -f "$RESULT" ]] && awk -F'\t' 'NR>1 {found=1} END {exit !found}' "$RESULT"; then
      RESUME=1
    fi
    ;;
esac

if [[ $RESUME -eq 0 ]]; then
  rm -f "$RESULT" "$SCRIPT_DIR/results/progress.log" "$SCRIPT_DIR/results/summary.txt"
  rm -rf "$LOGDIR"
  mkdir -p "$LOGDIR"
  printf '%s\n' "$RESULT_HEADER" > "$RESULT"
  cp "$ROOT/local.properties" "$PROPS_BACKUP"
else
  mkdir -p "$LOGDIR"
  # Every reader filters with NR>1, so an absent header would hide the first row
  # from already_done, the summary, and the FAIL/UNKNOWN gate.
  if [[ ! -s "$RESULT" ]]; then
    printf '%s\n' "$RESULT_HEADER" > "$RESULT"
  fi
  if [[ ! -f "$PROPS_BACKUP" ]]; then
    cp "$ROOT/local.properties" "$PROPS_BACKUP"
  fi
fi

cleanup() {
  chmod 600 "$KS_DIR/release.jks" 2>/dev/null || true
  if [[ -f "$PROPS_SNAPSHOT" ]]; then
    cp "$PROPS_SNAPSHOT" "$ROOT/local.properties"
    rm -f "$PROPS_SNAPSHOT"
  elif [[ -f "$PROPS_BACKUP" ]]; then
    cp "$PROPS_BACKUP" "$ROOT/local.properties"
  fi
  if [[ -f "$GSERVICES_MARKER" ]]; then
    rm -f "$GSERVICES" "$GSERVICES_MARKER"
  fi
}
# Installed before the snapshot exists so a preflight failure below cannot leave
# a stale one behind.
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

# Per-invocation restore point, separate from the persistent baseline: cleanup
# restores the file as it was when this run started, so edits made between
# runs survive --resume.
cp "$ROOT/local.properties" "$PROPS_SNAPSHOT"

# Keep machine keys (sdk.dir, cocoapods, feature flags). Drop signing keys.
grep -Ev '^(KEYSTORE_PATH|KEYSTORE_PASSWORD|KEY_ALIAS|KEY_PASSWORD|CLI_KEY_ALIAS|CLI_KEY_PASSWORD)=' \
  "$PROPS_SNAPSHOT" > "$BASE_PROPS"

SDK_DIR="$(grep -E '^sdk\.dir=' "$PROPS_SNAPSHOT" | head -1 | cut -d= -f2-)"
if [[ -z "${APKSIGNER:-}" ]]; then
  APKSIGNER="$(ls -1 "$SDK_DIR"/build-tools/*/apksigner 2>/dev/null | tail -1 || true)"
fi
if [[ -z "${APKSIGNER:-}" || ! -x "${APKSIGNER:-}" ]]; then
  echo "apksigner not found under $SDK_DIR/build-tools" >&2
  exit 1
fi

# Throwaway JKS (CN=PR1747 Review). PKCS12 cannot use a distinct keypass (K8).
# On resume, keep the existing JKS so later cases match earlier signing checks.
if [[ $RESUME -eq 0 || ! -f "$KS_DIR/release.jks" ]]; then
  rm -f "$KS_DIR/release.jks" "$KS_DIR/unreadable.jks" "$KS_DIR/not-a-keystore"
  keytool -genkeypair -storetype JKS -keystore "$KS_DIR/release.jks" -alias releasekey \
    -storepass storepass -keypass keypass \
    -dname "CN=PR1747 Review" -keyalg RSA -keysize 2048 -validity 2 -noprompt
fi
chmod 600 "$KS_DIR/release.jks" 2>/dev/null || true
printf 'this is not a keystore\n' > "$KS_DIR/not-a-keystore"
# A prior run left this at mode 000; cp onto it would fail with EACCES.
rm -f "$KS_DIR/unreadable.jks"
cp "$KS_DIR/release.jks" "$KS_DIR/unreadable.jks"
chmod 000 "$KS_DIR/unreadable.jks"

if [[ ! -f "$GSERVICES" ]]; then
  cp "$GSERVICES_STUB" "$GSERVICES"
  : > "$GSERVICES_MARKER"
fi

log_note() { echo "[$(date '+%H:%M:%S')] $*" | tee -a "$SCRIPT_DIR/results/progress.log"; }

gradle_stop() {
  log_note "gradle --stop"
  (cd "$ROOT" && ./gradlew --stop) >>"$LOGDIR/_stop.log" 2>&1 || true
}

write_props() {
  local ks="$1"
  local companions="${2:-both}"
  /bin/cat "$BASE_PROPS" > "$ROOT/local.properties"

  if [[ "$ks" == "CI" ]]; then
    {
      echo "KEYSTORE_PASSWORD=yourKeystorePassword"
      echo "KEY_ALIAS=yourKeyAlias"
      echo "CLI_KEY_ALIAS=yourCliKeyAlias"
      echo "KEY_PASSWORD=yourKeyPassword"
      echo "CLI_KEY_PASSWORD=yourCliKeyPassword"
    } >> "$ROOT/local.properties"
    return 0
  fi

  case "$ks" in
    K0) : ;;
    K1) echo "KEYSTORE_PATH=" >> "$ROOT/local.properties" ;;
    K2) echo "KEYSTORE_PATH=$KS_DIR/does-not-exist.jks" >> "$ROOT/local.properties" ;;
    K3) echo "KEYSTORE_PATH=$KS_DIR/not-a-keystore" >> "$ROOT/local.properties" ;;
    K4) echo "KEYSTORE_PATH=$KS_DIR/unreadable.jks" >> "$ROOT/local.properties" ;;
    K5|K6|K7|K8|K9) echo "KEYSTORE_PATH=$KS_DIR/release.jks" >> "$ROOT/local.properties" ;;
    *) echo "unknown ks=$ks" >&2; return 2 ;;
  esac

  local storepass=storepass alias=releasekey keypass=keypass
  case "$ks" in
    K6) storepass=wrongstore ;;
    K7) alias=does-not-exist-alias ;;
    K8) keypass=wrongkey ;;
  esac

  include_field() {
    local name="$1" value="$2"
    if [[ "$companions" == missing:* ]]; then
      local miss="${companions#missing:}"
      if [[ "$name" == "$miss" ]]; then
        return 0
      fi
    fi
    if [[ "$companions" == "client" && ( "$name" == "KEY_ALIAS" || "$name" == "KEY_PASSWORD" ) ]]; then
      return 0
    fi
    if [[ "$companions" == "node" && ( "$name" == "CLI_KEY_ALIAS" || "$name" == "CLI_KEY_PASSWORD" ) ]]; then
      return 0
    fi
    echo "$name=$value" >> "$ROOT/local.properties"
  }

  include_field KEYSTORE_PASSWORD "$storepass"
  include_field CLI_KEY_ALIAS "$alias"
  include_field CLI_KEY_PASSWORD "$keypass"
  include_field KEY_ALIAS "$alias"
  include_field KEY_PASSWORD "$keypass"
}

# Newest by mtime. Reject cwd fallback from BSD xargs on empty find.
find_newest() {
  local dir="$1" pattern="$2"
  local f
  [[ -d "$dir" ]] || return 0
  f="$(find "$dir" -name "$pattern" -type f -print0 2>/dev/null | xargs -0 ls -t 2>/dev/null | head -1 || true)"
  if [[ -n "$f" && -f "$f" && "$f" == "$dir"* ]]; then
    printf '%s\n' "$f"
  fi
}

find_apk() {
  local app="$1" variant="$2"
  find_newest "$ROOT/apps/${app}/build/outputs/apk/${variant}" "*.apk"
}

find_aab() {
  local app="$1"
  find_newest "$ROOT/apps/${app}/build/outputs/bundle/release" "*.aab"
}

wipe_packaging_outputs() {
  case "$1" in
    *":apps:clientApp:assembleRelease"*) rm -rf "$ROOT/apps/clientApp/build/outputs/apk/release" ;;
    *":apps:nodeApp:assembleRelease"*) rm -rf "$ROOT/apps/nodeApp/build/outputs/apk/release" ;;
    *":apps:clientApp:bundleRelease"*) rm -rf "$ROOT/apps/clientApp/build/outputs/bundle/release" ;;
    *":apps:nodeApp:bundleRelease"*) rm -rf "$ROOT/apps/nodeApp/build/outputs/bundle/release" ;;
    *":apps:nodeApp:assembleProfile"*) rm -rf "$ROOT/apps/nodeApp/build/outputs/apk/profile" ;;
  esac
}

cert_cn() {
  local apk="$1"
  "$APKSIGNER" verify --print-certs "$apk" 2>/dev/null | grep -m1 "DN:" || true
}

SIGNING_FAIL_RE='Failed to read key|Keystore was tampered|Cannot recover key|No key with alias|password was incorrect|given final block not properly padded'

judge() {
  local expected="$1" exitc="$2" log="$3"
  local verdict=UNKNOWN
  case "$expected" in
    fail-no-keystore)
      if [[ $exitc -ne 0 ]] && grep -q "Release packaging needs a readable KEYSTORE_PATH" "$log"; then
        verdict=PASS
      else
        verdict=FAIL
      fi
      ;;
    fail-bad-path)
      if [[ $exitc -ne 0 ]] && grep -q "that path is not a readable keystore file" "$log"; then
        verdict=PASS
      else
        verdict=FAIL
      fi
      ;;
    fail-missing-prop:*)
      local prop="${expected#fail-missing-prop:}"
      if [[ $exitc -ne 0 ]] && grep -q "$prop must be set in local.properties when KEYSTORE_PATH is set" "$log"; then
        verdict=PASS
      else
        verdict=FAIL
      fi
      ;;
    fail-signing)
      if [[ $exitc -ne 0 ]] && ! grep -q "BUILD SUCCESSFUL" "$log" &&
        grep -qE "$SIGNING_FAIL_RE" "$log"; then
        verdict=PASS
      else
        verdict=FAIL
      fi
      ;;
    fail-package-profile)
      if [[ $exitc -ne 0 ]] && ! grep -q "BUILD SUCCESSFUL" "$log" &&
        grep -qE "$SIGNING_FAIL_RE" "$log"; then
        verdict=PASS
      else
        verdict=FAIL
      fi
      ;;
    success)
      if [[ $exitc -eq 0 ]]; then verdict=PASS; else verdict=FAIL; fi
      ;;
    success-unsigned)
      if [[ $exitc -ne 0 ]]; then
        verdict=FAIL
      elif grep -q "Packaging an unsigned release because -PallowUnsignedRelease=true" "$log"; then
        verdict=PASS
      elif grep -qE "Reusing configuration cache|Configuration cache entry reused" "$log"; then
        # whenReady does not re-run on configuration-cache reuse.
        verdict=KNOWN
      else
        verdict=FAIL
      fi
      ;;
    success-no-unsigned)
      if [[ $exitc -eq 0 ]] && ! grep -q "Packaging an unsigned release" "$log"; then
        verdict=PASS
      else
        verdict=FAIL
      fi
      ;;
    success-debug-signed)
      if [[ $exitc -ne 0 ]]; then
        verdict=FAIL
      else
        local apk cn=""
        apk="$(find_apk nodeApp profile)"
        [[ -n "$apk" ]] && cn="$(cert_cn "$apk")"
        if [[ -z "$apk" || -z "$cn" ]]; then
          verdict=FAIL
        elif echo "$cn" | grep -q "CN=PR1747 Review"; then
          verdict=FAIL
        else
          verdict=PASS
        fi
      fi
      ;;
    success-release-signed)
      if [[ $exitc -ne 0 ]]; then
        verdict=FAIL
      else
        local apk
        apk="$(find_apk nodeApp profile)"
        if [[ -z "$apk" ]]; then
          verdict=FAIL
        elif echo "$(cert_cn "$apk")" | grep -q "CN=PR1747 Review"; then
          verdict=PASS
        else
          verdict=FAIL
        fi
      fi
      ;;
    record-cc-chmod)
      if [[ $exitc -ne 0 ]] && grep -q "that path is not a readable keystore file" "$log"; then
        verdict=PASS
      else
        verdict=FAIL
      fi
      ;;
    unique_name-zero)
      local uc
      uc=$(grep -c unique_name "$log" || true)
      if [[ $exitc -eq 0 && "$uc" -eq 0 ]]; then
        verdict=PASS
      else
        verdict=FAIL
      fi
      ;;
    *)
      verdict=UNKNOWN
      ;;
  esac
  printf '%s\n' "$verdict"
}

already_done() {
  local id="$1"
  [[ -f "$RESULT" ]] || return 1
  awk -F'\t' -v id="$id" 'NR>1 && $2==id && $10=="PASS" {found=1} END {exit !found}' "$RESULT"
}

drop_result_id() {
  local id="$1"
  [[ -f "$RESULT" ]] || return 0
  awk -F'\t' -v id="$id" 'NR==1 || $2!=id' "$RESULT" > "${RESULT}.tmp" && mv "${RESULT}.tmp" "$RESULT"
}

run_case() {
  local section="$1" id="$2" ks="$3" companions="$4" optin="$5" task="$6" extra="$7" expected="$8"
  local log="$LOGDIR/${id}.log"
  if already_done "$id"; then
    log_note "SKIP $id (PASS in matrix.tsv)"
    return 0
  fi
  drop_result_id "$id"
  write_props "$ks" "$companions"
  wipe_packaging_outputs "$task"

  local cmd=(./gradlew --console=plain)
  case "$optin" in
    default|"") : ;;
    *) cmd+=(-PallowUnsignedRelease="$optin") ;;
  esac
  # shellcheck disable=SC2206
  local tasks=($task)
  cmd+=("${tasks[@]}")
  if [[ -n "$extra" && "$extra" != "-" ]]; then
    # shellcheck disable=SC2206
    local extras=($extra)
    cmd+=("${extras[@]}")
  fi

  log_note "START $id  ${cmd[*]}  ks=$ks companions=$companions expected=$expected"
  local start end exitc elapsed
  start=$(date +%s)
  set +e
  (
    cd "$ROOT" || exit 99
    "${cmd[@]}"
  ) >"$log" 2>&1
  exitc=$?
  set +e
  end=$(date +%s)
  elapsed=$((end - start))

  local notes="exit=${exitc};s=${elapsed}"
  if grep -q "Packaging an unsigned release because -PallowUnsignedRelease=true" "$log"; then
    notes="${notes};unsigned_lifecycle=$(grep -c "Packaging an unsigned release because -PallowUnsignedRelease=true" "$log" || true)"
  fi
  if grep -qE "Reusing configuration cache|Configuration cache entry reused" "$log"; then
    notes="${notes};cc_reuse"
  fi
  if grep -q "Configuration cache entry stored" "$log"; then
    notes="${notes};cc_stored"
  fi
  if grep -q "unique_name" "$log"; then
    notes="${notes};unique_name=$(grep -c unique_name "$log" || true)"
  fi
  if [[ "$expected" == fail-no-keystore && $exitc -ne 0 ]] && ! grep -q "Release packaging needs a readable KEYSTORE_PATH" "$log"; then
    notes="${notes};no_keystore_phrase"
  fi
  local err
  err=$(grep -E "Release packaging needs|KEYSTORE_PATH is set to|must be set in local.properties|What went wrong|BUILD FAILED|BUILD SUCCESSFUL|packageProfile|packageRelease" "$log" | tail -8 | tr '\n' '|' | tr '\t' ' ')
  notes="${notes};${err:0:350}"

  if [[ "$expected" == success-debug-signed || "$expected" == success-release-signed ]]; then
    local apk
    apk="$(find_apk nodeApp profile)"
    notes="${notes};apk=${apk:-none};cn=$(cert_cn "${apk:-/dev/null}" | tr '\n' ' ')"
  fi

  local verdict
  verdict="$(judge "$expected" "$exitc" "$log")"

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$section" "$id" "$ks" "$companions" "$optin" "$task" "$extra" "$exitc" "$expected" "$verdict" "$notes" \
    | tee -a "$RESULT"
  log_note "DONE  $id  verdict=$verdict  ${elapsed}s"
}

run_swift() {
  local section="$1" id="$2"
  if already_done "$id"; then
    log_note "SKIP $id (PASS in matrix.tsv)"
    return 0
  fi
  drop_result_id "$id"
  local log="$LOGDIR/${id}.log"
  log_note "START $id  swift support/test_nse_decryption.swift"
  local start end exitc elapsed
  start=$(date +%s)
  set +e
  (
    cd "$ROOT" || exit 99
    swift support/test_nse_decryption.swift
  ) >"$log" 2>&1
  exitc=$?
  set +e
  end=$(date +%s)
  elapsed=$((end - start))
  local verdict=FAIL
  if [[ $exitc -eq 0 ]]; then verdict=PASS; fi
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$section" "$id" "CI" "ci" "default" "swift" "test_nse_decryption" "$exitc" "success" "$verdict" "exit=${exitc};s=${elapsed}" \
    | tee -a "$RESULT"
  log_note "DONE  $id  verdict=$verdict  ${elapsed}s"
}

verify_signed() {
  local section="$1" id="$2" kind="$3" app="$4"
  if already_done "$id"; then
    log_note "SKIP $id (PASS in matrix.tsv)"
    return 0
  fi
  drop_result_id "$id"
  local log="$LOGDIR/${id}.log"
  local artifact="" cn="" verdict=FAIL notes="" verc=""
  if [[ "$kind" == apk ]]; then
    artifact="$(find_apk "$app" release)"
    if [[ -n "$artifact" ]]; then
      "$APKSIGNER" verify --print-certs "$artifact" >"$log" 2>&1
      verc=$?
      cn=$(grep -m1 "DN:" "$log" || true)
      if [[ $verc -eq 0 ]] && echo "$cn" | grep -q "CN=PR1747 Review"; then
        verdict=PASS
      fi
    else
      echo "no apk" >"$log"
    fi
  else
    artifact="$(find_aab "$app")"
    if [[ -n "$artifact" ]]; then
      # AABs are v1 JAR-signed by design; apksigner rejects the AAB format.
      jarsigner -verify -verbose -certs "$artifact" >"$log" 2>&1
      verc=$?
      cn=$(grep -m1 "CN=PR1747 Review" "$log" || true)
      if [[ $verc -eq 0 ]] && grep -q "CN=PR1747 Review" "$log"; then
        verdict=PASS
      fi
    else
      echo "no aab" >"$log"
    fi
  fi
  notes="artifact=${artifact:-none};verify_exit=${verc:-na};${cn:0:200}"
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$section" "$id" "K9" "both" "default" "verify-$kind" "$app" "0" "CN=PR1747 Review" "$verdict" "$notes" \
    | tee -a "$RESULT"
  log_note "VERIFY $id  $verdict  $notes"
}

if [[ $RESUME -eq 1 ]]; then
  log_note "ROOT=$ROOT  resume (skip PASS; retry other verdicts)"
else
  log_note "ROOT=$ROOT  fresh run"
fi

# ---------------------------------------------------------------------------
# Section 1
# ---------------------------------------------------------------------------
log_note "==== SECTION 1 ===="
run_case 1 S1-c-rel-K0 K0 both default ":apps:clientApp:assembleRelease" - fail-no-keystore
run_case 1 S1-n-rel-K0 K0 both default ":apps:nodeApp:assembleRelease" - fail-no-keystore
run_case 1 S1-c-rel-K1 K1 both default ":apps:clientApp:assembleRelease" - fail-no-keystore
run_case 1 S1-n-rel-K1 K1 both default ":apps:nodeApp:assembleRelease" - fail-no-keystore
run_case 1 S1-c-bun-K0 K0 both default ":apps:clientApp:bundleRelease" - fail-no-keystore
run_case 1 S1-n-bun-K0 K0 both default ":apps:nodeApp:bundleRelease" - fail-no-keystore
run_case 1 S1-c-rel-K0-false K0 both false ":apps:clientApp:assembleRelease" - fail-no-keystore
run_case 1 S1-c-rel-K0-yes K0 both yes ":apps:clientApp:assembleRelease" - fail-no-keystore
run_case 1 S1-n-rel-K0-false K0 both false ":apps:nodeApp:assembleRelease" - fail-no-keystore
run_case 1 S1-n-rel-K0-yes K0 both yes ":apps:nodeApp:assembleRelease" - fail-no-keystore
gradle_stop
run_case 1 S1-c-build-K0 K0 both default ":apps:clientApp:build" - fail-no-keystore
gradle_stop
run_case 1 S1-n-build-K0 K0 both default ":apps:nodeApp:build" - fail-no-keystore

# ---------------------------------------------------------------------------
# Section 2
# ---------------------------------------------------------------------------
log_note "==== SECTION 2 ===="
run_case 2 S2-c-rel-True-dry K0 both True ":apps:clientApp:assembleRelease" "--dry-run" success-unsigned
run_case 2 S2-c-rel-TRUE-dry K0 both TRUE ":apps:clientApp:assembleRelease" "--dry-run" success-unsigned
run_case 2 S2-c-rel-true K0 both true ":apps:clientApp:assembleRelease" - success-unsigned
run_case 2 S2-c-bun-true K0 both true ":apps:clientApp:bundleRelease" - success-unsigned
run_case 2 S2-n-rel-true K0 both true ":apps:nodeApp:assembleRelease" - success-unsigned
run_case 2 S2-c-rel-true-2 K0 both true ":apps:clientApp:assembleRelease" - success-unsigned
gradle_stop
run_case 2 S2-c-build-true K0 both true ":apps:clientApp:build" "-x test -x lint" success-unsigned

# ---------------------------------------------------------------------------
# Section 3
# ---------------------------------------------------------------------------
log_note "==== SECTION 3 ===="
for ks in K2 K3 K4; do
  run_case 3 "S3-c-rel-${ks}" "$ks" both default ":apps:clientApp:assembleRelease" - fail-bad-path
  run_case 3 "S3-c-rel-${ks}-optin" "$ks" both true ":apps:clientApp:assembleRelease" - fail-bad-path
  run_case 3 "S3-n-rel-${ks}" "$ks" both default ":apps:nodeApp:assembleRelease" - fail-bad-path
  run_case 3 "S3-n-rel-${ks}-optin" "$ks" both true ":apps:nodeApp:assembleRelease" - fail-bad-path
done
run_case 3 S3-c-bun-K2-optin K2 both true ":apps:clientApp:bundleRelease" - fail-bad-path
run_case 3 S3-n-bun-K3 K3 both default ":apps:nodeApp:bundleRelease" - fail-bad-path

# ---------------------------------------------------------------------------
# Section 4
# ---------------------------------------------------------------------------
log_note "==== SECTION 4 ===="
for field in KEYSTORE_PASSWORD CLI_KEY_ALIAS CLI_KEY_PASSWORD; do
  run_case 4 "S4-c-rel-omit-${field}" K5 "missing:${field}" default ":apps:clientApp:assembleRelease" - "fail-missing-prop:${field}"
  run_case 4 "S4-c-rel-omit-${field}-optin" K5 "missing:${field}" true ":apps:clientApp:assembleRelease" - "fail-missing-prop:${field}"
done
for field in KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD; do
  run_case 4 "S4-n-rel-omit-${field}" K5 "missing:${field}" default ":apps:nodeApp:assembleRelease" - "fail-missing-prop:${field}"
  run_case 4 "S4-n-rel-omit-${field}-optin" K5 "missing:${field}" true ":apps:nodeApp:assembleRelease" - "fail-missing-prop:${field}"
done

# ---------------------------------------------------------------------------
# Section 5
# ---------------------------------------------------------------------------
log_note "==== SECTION 5 ===="
run_case 5 S5-c-dry-clientonly K9 client default ":apps:clientApp:assembleRelease" "--dry-run" success-no-unsigned
run_case 5 S5-n-dry-clientonly K9 client default ":apps:nodeApp:assembleRelease" "--dry-run" fail-missing-prop:KEY_ALIAS
run_case 5 S5-n-dry-nodeonly K9 node default ":apps:nodeApp:assembleRelease" "--dry-run" success-no-unsigned
run_case 5 S5-c-dry-nodeonly K9 node default ":apps:clientApp:assembleRelease" "--dry-run" fail-missing-prop:CLI_KEY_ALIAS

# ---------------------------------------------------------------------------
# Section 6
# ---------------------------------------------------------------------------
log_note "==== SECTION 6 ===="
run_case 6 S6-c-rel-K9 K9 both default ":apps:clientApp:assembleRelease" - success-no-unsigned
verify_signed 6 S6-c-apk-K9 apk clientApp
run_case 6 S6-c-bun-K9 K9 both default ":apps:clientApp:bundleRelease" - success-no-unsigned
verify_signed 6 S6-c-aab-K9 aab clientApp
run_case 6 S6-n-rel-K9 K9 both default ":apps:nodeApp:assembleRelease" - success-no-unsigned
verify_signed 6 S6-n-apk-K9 apk nodeApp
run_case 6 S6-n-bun-K9 K9 both default ":apps:nodeApp:bundleRelease" - success-no-unsigned
verify_signed 6 S6-n-aab-K9 aab nodeApp
run_case 6 S6-c-rel-K9-2 K9 both default ":apps:clientApp:assembleRelease" - success-no-unsigned
for ks in K6 K7 K8; do
  run_case 6 "S6-c-rel-${ks}" "$ks" both default ":apps:clientApp:assembleRelease" - fail-signing
  run_case 6 "S6-c-rel-${ks}-optin" "$ks" both true ":apps:clientApp:assembleRelease" - fail-signing
done
run_case 6 S6-n-rel-K6 K6 both default ":apps:nodeApp:assembleRelease" - fail-signing
run_case 6 S6-n-rel-K8 K8 both default ":apps:nodeApp:assembleRelease" - fail-signing
run_case 6 S6-n-rel-K7-optin K7 both true ":apps:nodeApp:assembleRelease" - fail-signing

# ---------------------------------------------------------------------------
# Section 7
# ---------------------------------------------------------------------------
log_note "==== SECTION 7 ===="
for ks in K0 K1 K2 K3 K4; do
  run_case 7 "S7-n-prof-${ks}" "$ks" both default ":apps:nodeApp:assembleProfile" "-x test -x lint" success-debug-signed
done
run_case 7 S7-n-prof-K9 K9 both default ":apps:nodeApp:assembleProfile" "-x test -x lint" success-release-signed
run_case 7 S7-n-prof-K5 K5 missing:KEY_ALIAS default ":apps:nodeApp:assembleProfile" - fail-missing-prop:KEY_ALIAS
run_case 7 S7-n-prof-K6 K6 both default ":apps:nodeApp:assembleProfile" "-x test -x lint" fail-package-profile

# ---------------------------------------------------------------------------
# Section 8
# ---------------------------------------------------------------------------
log_note "==== SECTION 8 ===="
for ks in K0 K1 K2 K3 K4 K6 K7 K8 K9; do
  run_case 8 "S8-c-dbg-${ks}" "$ks" both default ":apps:clientApp:assembleDebug" "-x test -x lint" success-no-unsigned
  run_case 8 "S8-n-dbg-${ks}" "$ks" both default ":apps:nodeApp:assembleDebug" "-x test -x lint" success-no-unsigned
done
for ks in K0 K2 K9; do
  run_case 8 "S8-help-${ks}" "$ks" both default "help" - success-no-unsigned
  run_case 8 "S8-ktlint-${ks}" "$ks" both default "ktlintCheck" "-Pkotlin.native.ignoreDisabledTargets=true" success-no-unsigned
done
gradle_stop
run_case 8 S8-iosmeta-K0 K0 both default ":shared:domain:compileIosMainKotlinMetadata" - success-no-unsigned
gradle_stop
run_case 8 S8-iosmeta-K9 K9 both default ":shared:domain:compileIosMainKotlinMetadata" - success-no-unsigned

# ---------------------------------------------------------------------------
# Section 9  (no --stop between pairs)
# ---------------------------------------------------------------------------
log_note "==== SECTION 9 ===="
run_case 9 S9-c-dbg-K2-a K2 both default ":apps:clientApp:assembleDebug" "-x test -x lint" success-no-unsigned
run_case 9 S9-c-dbg-K2-b K2 both default ":apps:clientApp:assembleDebug" "-x test -x lint" success-no-unsigned
run_case 9 S9-c-rel-K0-a K0 both default ":apps:clientApp:assembleRelease" - fail-no-keystore
run_case 9 S9-c-rel-K0-b K0 both default ":apps:clientApp:assembleRelease" - fail-no-keystore
run_case 9 S9-c-rel-K9-dry K9 both default ":apps:clientApp:assembleRelease" "--dry-run" success-no-unsigned
chmod 000 "$KS_DIR/release.jks"
run_case 9 S9-c-rel-K9-dry-chmod K9 both default ":apps:clientApp:assembleRelease" "--dry-run" record-cc-chmod
chmod 600 "$KS_DIR/release.jks"

# ---------------------------------------------------------------------------
# Section 10
# ---------------------------------------------------------------------------
log_note "==== SECTION 10 ===="
run_case 10 S10-ktlint CI ci default "ktlintCheck" "-Pkotlin.native.ignoreDisabledTargets=true" success-no-unsigned
run_case 10 S10-podInstall CI ci default ":apps:clientApp:podInstall" - success
gradle_stop
run_case 10 S10-c-dbg CI ci default ":apps:clientApp:assembleDebug" "-x test -x lint" success-no-unsigned
gradle_stop
run_case 10 S10-pres-ios CI ci default "shared:presentation:iosSimulatorArm64Test" - success
gradle_stop
if already_done S10-domain-ios; then
  log_note "SKIP S10-domain-ios (PASS in matrix.tsv)"
else
drop_result_id S10-domain-ios
write_props CI ci
log_note "START S10-domain-ios  CI=true :shared:domain:iosSimulatorArm64Test --info"
start=$(date +%s)
set +e
(
  cd "$ROOT" || exit 99
  CI=true ./gradlew --console=plain :shared:domain:iosSimulatorArm64Test --info
) >"$LOGDIR/S10-domain-ios.log" 2>&1
exitc=$?
set +e
end=$(date +%s)
elapsed=$((end - start))
verdict=FAIL
if [[ $exitc -eq 0 ]]; then verdict=PASS; fi
notes="exit=${exitc};s=${elapsed}"
printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
  10 S10-domain-ios CI ci default ":shared:domain:iosSimulatorArm64Test" "--info CI=true" "$exitc" success "$verdict" "$notes" \
  | tee -a "$RESULT"
log_note "DONE  S10-domain-ios  verdict=$verdict  ${elapsed}s"
fi
run_swift 10 S10-swift
gradle_stop
run_case 10 S10-node-tests CI ci default ":apps:nodeApp:testDebugUnitTest :apps:nodeApp:testReleaseUnitTest" "--info" success

# ---------------------------------------------------------------------------
# Section 11
# ---------------------------------------------------------------------------
log_note "==== SECTION 11 ===="
gradle_stop
run_case 11 S11-c-clean-build K0 both true "apps:clientApp:clean apps:clientApp:build" "--continue" unique_name-zero

log_note "ALL SECTIONS COMPLETE"
awk -F'\t' 'NR>1 {v[$2]=$10} END {for (id in v) c[v[id]]++; for (k in c) print k, c[k]}' "$RESULT" | tee "$SCRIPT_DIR/results/summary.txt"

# KNOWN is accepted (configuration-cache reuse leaves whenReady unverifiable);
# FAIL/UNKNOWN fail the run.
bad=$(awk -F'\t' 'NR>1 && ($10=="FAIL" || $10=="UNKNOWN") {n++} END {print n+0}' "$RESULT")
if [[ "$bad" -gt 0 ]]; then
  log_note "RESULT: $bad row(s) with FAIL/UNKNOWN verdicts"
  exit 1
fi
