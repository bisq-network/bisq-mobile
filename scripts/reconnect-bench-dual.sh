#!/usr/bin/env bash
set -euo pipefail

# Bisq2 desktop ↔ Android + iOS (Simulator) reconnect timing in one run (macOS).
#
# Artifacts per round: RoundN.log (metadata + match lines only), RoundN.adb.log, RoundN.ios.log
# (full captures; not inlined into RoundN.log).
# One Bisq2 restart per round while both captures run (same clear/start/stop patterns as the single-platform scripts).
#
# After desktop "ApplicationService initialized" (t1), polls BOTH mobile logs until each platform's
# WS success marker appears (or that platform's timeout). Durations are seconds from t1 to each t2.
#
# Prerequisites: same as running the Android and iOS scripts separately (adb + booted simulator,
# Bisq2.app, bash, rg). Do not modify the single-platform scripts; this file is standalone.
#
# Environment (optional):
#   Same as reconnect-bench-android.sh: APP_PATH, BISQ_LOG, OUT_DIR, SLEEP_BEFORE_OPEN,
#   SLEEP_AFTER_CONNECT_SEC, BISQ_INIT_TIMEOUT_SEC, ANDROID_SERIAL
#   CONNECT_TIMEOUT_SEC   Per-platform WS wait after t1, seconds (default 300; Android + iOS)
#   Same as reconnect-bench-ios.sh: IOS_WS_MARKER, IOS_WS_MARKER_REGEX,
#   IOS_SIM_DEVICE, IOS_APP_PROCESS, IOS_LOG_PREDICATE, IOS_LOG_LEVEL, IOS_WS_FAILURE_MARKER
#
# Usage: ./scripts/reconnect-bench-dual.sh [ROUNDS]

_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
_REPO_ROOT="$(git -C "$_SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || true)"
if [[ -n "$_REPO_ROOT" ]]; then
  cd "$_REPO_ROOT"
fi
_BENCH_GIT_HASH="$(git rev-parse --short=8 HEAD 2>/dev/null || true)"
_BENCH_GIT_HASH="${_BENCH_GIT_HASH:-unknown}"
unset _SCRIPT_DIR _REPO_ROOT

ROUNDS="${1:-1}"
APP_PATH="${APP_PATH:-/Applications/Bisq2.app}"
BISQ_LOG="${BISQ_LOG:-$HOME/Library/Application Support/Bisq2/bisq.log}"
SLEEP_BEFORE_OPEN="${SLEEP_BEFORE_OPEN:-45}"
SLEEP_AFTER_CONNECT_SEC="${SLEEP_AFTER_CONNECT_SEC:-30}"
BISQ_INIT_TIMEOUT_SEC="${BISQ_INIT_TIMEOUT_SEC:-180}"
CONNECT_TIMEOUT_SEC="${CONNECT_TIMEOUT_SEC:-300}"
IOS_WS_MARKER="${IOS_WS_MARKER:-WS connected successfully}"
IOS_WS_MARKER_REGEX="${IOS_WS_MARKER_REGEX:-${IOS_WS_MARKER}|Connection [0-9]+: connected successfully|received response, status 101}"
IOS_SIM_DEVICE="${IOS_SIM_DEVICE:-booted}"
IOS_APP_PROCESS="${IOS_APP_PROCESS:-Bisq Connect}"
IOS_LOG_PREDICATE="${IOS_LOG_PREDICATE:-}"
IOS_LOG_LEVEL="${IOS_LOG_LEVEL:-debug}"
IOS_WS_FAILURE_MARKER="${IOS_WS_FAILURE_MARKER:-WS connection failed to connect|Exception occurred whilst listening for WS messages}"
OUT_DIR="${OUT_DIR:-$PWD/debug/reconnect-bench-dual-$(date +%Y%m%d-%H%M%S)-${_BENCH_GIT_HASH}}"

adb_cmd() {
  if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    command adb -s "$ANDROID_SERIAL" "$@"
  else
    command adb "$@"
  fi
}

require_cmd() {
  local cmd="$1"
  local hint="$2"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing required command: ${cmd}. ${hint}" >&2
    exit 1
  fi
}

ensure_adb_device() {
  local st count
  if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    st="$(adb_cmd get-state 2>/dev/null || true)"
    if [[ "$st" != "device" ]]; then
      echo "adb: device ANDROID_SERIAL=${ANDROID_SERIAL} is not ready (state=${st:-missing})." >&2
      exit 1
    fi
    return 0
  fi
  count="$(adb_cmd devices 2>/dev/null | awk '$2 == "device" { c++ } END { print c + 0 }')"
  if [[ "$count" -eq 0 ]]; then
    echo "adb: no device in 'device' state. Connect hardware or start an emulator." >&2
    exit 1
  fi
  if [[ "$count" -gt 1 ]]; then
    echo "adb: multiple devices connected (${count}). Set ANDROID_SERIAL (see: adb devices)." >&2
    exit 1
  fi
}

if ! [[ "$ROUNDS" =~ ^[0-9]+$ ]] || [[ "$ROUNDS" -lt 1 ]]; then
  echo "ROUNDS must be a positive integer, got: $ROUNDS" >&2
  exit 1
fi

require_cmd rg "Install ripgrep (e.g. brew install ripgrep)."
require_cmd adb "Install Android platform-tools and ensure adb is on PATH."

if ! command -v xcrun >/dev/null 2>&1; then
  echo "xcrun not found; install Xcode command line tools first." >&2
  exit 1
fi

ensure_adb_device

if [[ ! -d "$APP_PATH" ]]; then
  echo "APP_PATH is not a directory (expecting a macOS .app bundle): $APP_PATH" >&2
  exit 1
fi

if [[ ! -f "$BISQ_LOG" ]]; then
  fallback_log="${HOME}/Library/Application Support/Bisq2/bisq_1.log"
  if [[ -f "$fallback_log" ]]; then
    BISQ_LOG="$fallback_log"
  else
    echo "Bisq log file not found: $BISQ_LOG (fallback missing: $fallback_log)" >&2
    exit 1
  fi
fi

mkdir -p "$OUT_DIR"
SUMMARY_CSV="$OUT_DIR/summary.csv"
echo "round,t1_bisq_initialized,t2_android_ws,t2_ios_ws,android_duration_seconds,ios_duration_seconds,status" > "$SUMMARY_CSV"

log_step() {
  echo "[$(date '+%H:%M:%S')] $*" >&2
}

cleanup_pid() {
  local pid="${1:-}"
  if [[ -n "$pid" ]]; then
    kill "$pid" >/dev/null 2>&1 || true
    wait "$pid" 2>/dev/null || true
  fi
}

ADB_LOGCAT_PID=""
IOS_STREAM_PID=""
on_exit() {
  cleanup_pid "$ADB_LOGCAT_PID"
  ADB_LOGCAT_PID=""
  cleanup_pid "$IOS_STREAM_PID"
  IOS_STREAM_PID=""
}
trap on_exit EXIT INT TERM

clear_adb_logcat() {
  log_step "Round ${CURRENT_ROUND}: clearing adb logcat buffers with 'adb logcat -b all -c'"
  adb_cmd logcat -b all -c
  adb_cmd logcat -b all -c
  sleep 1
  local post_clear_lines
  post_clear_lines="$(adb_cmd logcat -b all -d | wc -l | tr -d ' ')"
  log_step "Round ${CURRENT_ROUND}: post-clear adb dump line count=${post_clear_lines}"
}

start_ios_log_stream() {
  local out_file="$1"
  local stream_pid=""
  local effective_predicate="$IOS_LOG_PREDICATE"

  if [[ -z "$effective_predicate" && -n "$IOS_APP_PROCESS" ]]; then
    effective_predicate="process == \"$IOS_APP_PROCESS\""
  fi

  if [[ -n "$effective_predicate" ]]; then
    xcrun simctl spawn "$IOS_SIM_DEVICE" log stream --style compact --level "$IOS_LOG_LEVEL" --predicate "$effective_predicate" > "$out_file" 2>&1 &
  else
    xcrun simctl spawn "$IOS_SIM_DEVICE" log stream --style compact --level "$IOS_LOG_LEVEL" > "$out_file" 2>&1 &
  fi
  stream_pid="$!"
  sleep 1
  if ! kill -0 "$stream_pid" >/dev/null 2>&1; then
    echo "Failed to start iOS log stream. Is an iOS simulator booted?" >&2
    return 1
  fi
  printf '%s\n' "$stream_pid"
}

wait_for_bisq_initialized() {
  local start_line="$1"
  local deadline=$(( $(date +%s) + BISQ_INIT_TIMEOUT_SEC ))
  local wait_start_epoch last_report_epoch baseline_reset_logged
  wait_start_epoch="$(date +%s)"
  last_report_epoch="$wait_start_epoch"
  baseline_reset_logged="false"

  while (( $(date +%s) <= deadline )); do
    local now_epoch current_line_count line
    now_epoch="$(date +%s)"
    current_line_count="$(wc -l < "$BISQ_LOG" || echo "$start_line")"

    if (( current_line_count < start_line )); then
      if [[ "$baseline_reset_logged" == "false" ]]; then
        log_step "Round ${CURRENT_ROUND}: Bisq log truncated/rotated, resetting baseline to 0"
        baseline_reset_logged="true"
      fi
      start_line=0
    fi

    line="$(sed -n "$((start_line + 1)),\$p" "$BISQ_LOG" | rg -m1 "ApplicationService initialized" || true)"
    if [[ -n "$line" ]]; then
      printf '%s\n' "$line"
      return 0
    fi
    if (( now_epoch - last_report_epoch >= 5 )); then
      log_step "Round ${CURRENT_ROUND}: waiting Bisq init... elapsed=$((now_epoch - wait_start_epoch))s bisq_lines=${current_line_count}"
      last_report_epoch="$now_epoch"
    fi
    sleep 1
  done
  return 1
}

# After t1, poll both log files until each marker is found or that side's deadline.
# Sets: t2_adb_epoch, t2_ios_epoch, adb_line, ios_line, mobile_wait_status
wait_for_both_mobile_ws() {
  local adb_log="$1" adb_sl="$2" ios_log="$3" ios_sl="$4"
  local wait_start now_epoch last_report_epoch
  wait_start="$(date +%s)"
  last_report_epoch="$wait_start"
  local adb_dead ios_dead
  adb_dead=$((wait_start + CONNECT_TIMEOUT_SEC))
  ios_dead=$((wait_start + CONNECT_TIMEOUT_SEC))

  t2_adb_epoch=""
  t2_ios_epoch=""
  adb_line=""
  ios_line=""

  while true; do
    now_epoch="$(date +%s)"

    if [[ -z "$t2_adb_epoch" ]]; then
      local hit
      hit="$(sed -n "$((adb_sl + 1)),\$p" "$adb_log" | rg -m1 "WS connected successfully" || true)"
      if [[ -n "$hit" ]]; then
        t2_adb_epoch="$now_epoch"
        adb_line="$hit"
        log_step "Round ${CURRENT_ROUND}: Android WS marker captured"
      fi
    fi

    if [[ -z "$t2_ios_epoch" ]]; then
      local hit_ios
      hit_ios="$(sed -n "$((ios_sl + 1)),\$p" "$ios_log" | rg -m1 "$IOS_WS_MARKER_REGEX" || true)"
      if [[ -n "$hit_ios" ]]; then
        t2_ios_epoch="$now_epoch"
        ios_line="$hit_ios"
        log_step "Round ${CURRENT_ROUND}: iOS WS marker captured"
      fi
    fi

    if [[ -n "$t2_adb_epoch" && -n "$t2_ios_epoch" ]]; then
      mobile_wait_status="success"
      return 0
    fi

    local adb_fin ios_fin
    adb_fin=false
    ios_fin=false
    [[ -n "$t2_adb_epoch" || "$now_epoch" -gt "$adb_dead" ]] && adb_fin=true
    [[ -n "$t2_ios_epoch" || "$now_epoch" -gt "$ios_dead" ]] && ios_fin=true

    if [[ "$adb_fin" == true && "$ios_fin" == true ]]; then
      if [[ -n "$t2_adb_epoch" && -z "$t2_ios_epoch" ]]; then
        mobile_wait_status="ios-connect-timeout"
      elif [[ -z "$t2_adb_epoch" && -n "$t2_ios_epoch" ]]; then
        mobile_wait_status="adb-connect-timeout"
      else
        mobile_wait_status="adb-ios-connect-timeout"
      fi
      return 1
    fi

    if (( now_epoch - last_report_epoch >= 5 )); then
      local adb_line_count total_ios app_new marker_hits failure_hits
      adb_line_count="$(wc -l < "$adb_log" | tr -d ' ')"
      total_ios="$(wc -l < "$ios_log" | tr -d ' ')"
      app_new="$(sed -n "$((ios_sl + 1)),\$p" "$ios_log" | rg -c "$IOS_APP_PROCESS\\[[0-9]+:" || true)"
      marker_hits="$(sed -n "$((ios_sl + 1)),\$p" "$ios_log" | rg -c "$IOS_WS_MARKER_REGEX" || true)"
      failure_hits="$(rg -c "$IOS_WS_FAILURE_MARKER" "$ios_log" || true)"
      log_step "Round ${CURRENT_ROUND}: waiting mobile WS... adb_lines=${adb_line_count} ios_lines=${total_ios} ios_app_new_lines=${app_new:-0} ios_marker_hits=${marker_hits:-0} ios_failure_hits=${failure_hits:-0} have_android=$([[ -n "$t2_adb_epoch" ]] && echo yes || echo no) have_ios=$([[ -n "$t2_ios_epoch" ]] && echo yes || echo no)"
      last_report_epoch="$now_epoch"
    fi
    sleep 1
  done
}

CURRENT_ROUND=0

for round in $(seq 1 "$ROUNDS"); do
  echo "=== Round $round / $ROUNDS (Android + iOS) ==="
  CURRENT_ROUND="$round"
  local_round_file="$OUT_DIR/Round${round}.log"
  local_adb_file="$OUT_DIR/Round${round}.adb.log"
  local_ios_file="$OUT_DIR/Round${round}.ios.log"

  round_start_iso="$(date '+%Y-%m-%d %H:%M:%S')"
  bisq_start_line="$(wc -l < "$BISQ_LOG" || echo 0)"
  t2_adb_epoch=""
  t2_ios_epoch=""
  adb_line=""
  ios_line=""
  adb_pid=""
  ios_pid=""
  bisq_line=""
  t1_epoch=""
  t1_iso=""
  t2_adb_iso=""
  t2_ios_iso=""
  dur_adb=""
  dur_ios=""
  round_status="success"
  adb_start_line_for_wait="0"
  ios_start_line_for_wait="0"

  clear_adb_logcat

  adb_cmd logcat -b all -v time > "$local_adb_file" 2>&1 &
  adb_pid="$!"
  ADB_LOGCAT_PID="$adb_pid"

  log_step "Round $round: starting iOS log stream (device=${IOS_SIM_DEVICE})"
  ios_pid="$(start_ios_log_stream "$local_ios_file")"
  IOS_STREAM_PID="$ios_pid"
  log_step "Round $round: iOS log stream pid=$ios_pid, adb logcat pid=$adb_pid"

  log_step "Round $round: killing Bisq2"
  pkill -x Bisq2 >/dev/null 2>&1 || true
  log_step "Round $round: sleeping ${SLEEP_BEFORE_OPEN}s before relaunch"
  sleep "$SLEEP_BEFORE_OPEN"
  log_step "Round $round: opening Bisq2 app"
  open "$APP_PATH"

  log_step "Round $round: waiting for Bisq \"ApplicationService initialized\""
  if bisq_line="$(wait_for_bisq_initialized "$bisq_start_line")"; then
    t1_epoch="$(date +%s)"
    t1_iso="$(date '+%Y-%m-%d %H:%M:%S')"
    adb_start_line_for_wait="$(wc -l < "$local_adb_file" | tr -d ' ')"
    ios_start_line_for_wait="$(wc -l < "$local_ios_file" | tr -d ' ')"
    log_step "Round $round: t1=$t1_iso adb_search_line=${adb_start_line_for_wait} ios_search_line=${ios_start_line_for_wait}"
  else
    round_status="bisq-init-timeout"
    log_step "Round $round: timeout waiting for Bisq initialization marker"
  fi

  mobile_wait_status="skipped"
  if [[ "$round_status" == "success" ]]; then
    log_step "Round $round: waiting Android and iOS WS markers from t1 (≤${CONNECT_TIMEOUT_SEC}s each)"
    if wait_for_both_mobile_ws "$local_adb_file" "$adb_start_line_for_wait" "$local_ios_file" "$ios_start_line_for_wait"; then
      round_status="success"
    else
      round_status="$mobile_wait_status"
    fi
  fi

  if [[ "$round_status" == "success" ]]; then
    t2_adb_iso="$(date -r "$t2_adb_epoch" '+%Y-%m-%d %H:%M:%S' 2>/dev/null || date '+%Y-%m-%d %H:%M:%S')"
    t2_ios_iso="$(date -r "$t2_ios_epoch" '+%Y-%m-%d %H:%M:%S' 2>/dev/null || date '+%Y-%m-%d %H:%M:%S')"
    dur_adb="$((t2_adb_epoch - t1_epoch))"
    dur_ios="$((t2_ios_epoch - t1_epoch))"
    log_step "Round $round: t2_android=$t2_adb_iso duration=${dur_adb}s | t2_ios=$t2_ios_iso duration=${dur_ios}s"
    if [[ "$round" -lt "$ROUNDS" ]] && [[ "${SLEEP_AFTER_CONNECT_SEC:-0}" =~ ^[0-9]+$ ]] && [[ "${SLEEP_AFTER_CONNECT_SEC:-0}" -gt 0 ]]; then
      log_step "Round $round: sleeping ${SLEEP_AFTER_CONNECT_SEC}s before next round"
      sleep "$SLEEP_AFTER_CONNECT_SEC"
    fi
  else
    if [[ -n "${t2_adb_epoch:-}" ]]; then
      t2_adb_iso="$(date -r "$t2_adb_epoch" '+%Y-%m-%d %H:%M:%S' 2>/dev/null || true)"
      dur_adb="$((t2_adb_epoch - t1_epoch))"
    fi
    if [[ -n "${t2_ios_epoch:-}" ]]; then
      t2_ios_iso="$(date -r "$t2_ios_epoch" '+%Y-%m-%d %H:%M:%S' 2>/dev/null || true)"
      dur_ios="$((t2_ios_epoch - t1_epoch))"
    fi
    log_step "Round $round: ended status=$round_status"
  fi

  cleanup_pid "$adb_pid"
  ADB_LOGCAT_PID=""
  cleanup_pid "$ios_pid"
  IOS_STREAM_PID=""
  clear_adb_logcat

  {
    echo "round: $round"
    echo "status: $round_status"
    echo "round_start: $round_start_iso"
    echo "t1_bisq_initialized: ${t1_iso:-<missing>}"
    echo "t2_android_ws: ${t2_adb_iso:-<missing>}"
    echo "t2_ios_ws: ${t2_ios_iso:-<missing>}"
    echo "duration_android_seconds: ${dur_adb:-<missing>}"
    echo "duration_ios_seconds: ${dur_ios:-<missing>}"
    echo "connect_timeout_seconds: $CONNECT_TIMEOUT_SEC"
    echo "ios_ws_marker_regex: $IOS_WS_MARKER_REGEX"
    if [[ -n "${bisq_line:-}" ]]; then echo "bisq_match: $bisq_line"; else echo "bisq_match: <missing>"; fi
    if [[ -n "${adb_line:-}" ]]; then echo "adb_match: $adb_line"; else echo "adb_match: <missing>"; fi
    if [[ -n "${ios_line:-}" ]]; then echo "ios_match: $ios_line"; else echo "ios_match: <missing>"; fi
    echo
    echo "android_log_file: $(basename "$local_adb_file")"
    echo "ios_log_file: $(basename "$local_ios_file")"
    echo "android_log_path: $local_adb_file"
    echo "ios_log_path: $local_ios_file"
  } > "$local_round_file"

  printf "%s,%s,%s,%s,%s,%s,%s\n" \
    "$round" \
    "${t1_iso:-<missing>}" \
    "${t2_adb_iso:-<missing>}" \
    "${t2_ios_iso:-<missing>}" \
    "${dur_adb:-<missing>}" \
    "${dur_ios:-<missing>}" \
    "$round_status" >> "$SUMMARY_CSV"

  echo "Saved: $local_round_file + $(basename "$local_adb_file") + $(basename "$local_ios_file")"
  echo "Updated: $SUMMARY_CSV"
  echo
done

echo "Summary CSV: $SUMMARY_CSV"
echo "Done. Logs directory: $OUT_DIR"
