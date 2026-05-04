#!/usr/bin/env bash
set -euo pipefail

# Bisq2 desktop ↔ Android client reconnect timing (macOS).
#
# Prerequisites:
#   - macOS (uses `open`, ~/Library/Application Support/Bisq2/, default APP_PATH)
#   - bash, ripgrep (`rg`), Android platform-tools (`adb`) on PATH
#   - Bisq2 desktop installed; Android device or emulator attached (see adb note below)
#
# Working directory: if this script lives inside a git clone, cwd is set to the repo root
# so default OUT_DIR lands in <repo>/debug/ (that folder is gitignored).
#
# Environment (all optional unless noted):
#   APP_PATH              Bisq2.app bundle (default /Applications/Bisq2.app)
#   BISQ_LOG              Desktop bisq log file to tail for "ApplicationService initialized"
#   OUT_DIR               Output directory (default ./debug/reconnect-round-logs-<timestamp>)
#   SLEEP_BEFORE_OPEN     Seconds between kill and reopen (default 5)
#   BISQ_INIT_TIMEOUT_SEC Seconds to wait for desktop init marker (default 180)
#   ADB_CONNECT_TIMEOUT_SEC  Seconds to wait for "WS connected successfully" in logcat (default 300)
#   ANDROID_SERIAL        When multiple adb devices exist, set to the target UDID (adb devices).
#
# Round sequence (numbered steps): see scripts/README.md section "Round sequence".
#
# Usage: ./scripts/run_bisq_reconnect_rounds_android.sh [ROUNDS]

_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
_REPO_ROOT="$(git -C "$_SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || true)"
if [[ -n "$_REPO_ROOT" ]]; then
  cd "$_REPO_ROOT"
fi
unset _SCRIPT_DIR _REPO_ROOT

ROUNDS="${1:-1}"
APP_PATH="${APP_PATH:-/Applications/Bisq2.app}"
BISQ_LOG="${BISQ_LOG:-$HOME/Library/Application Support/Bisq2/bisq.log}"
SLEEP_BEFORE_OPEN="${SLEEP_BEFORE_OPEN:-5}"
BISQ_INIT_TIMEOUT_SEC="${BISQ_INIT_TIMEOUT_SEC:-180}"
ADB_CONNECT_TIMEOUT_SEC="${ADB_CONNECT_TIMEOUT_SEC:-300}"
OUT_DIR="${OUT_DIR:-$PWD/debug/reconnect-round-logs-$(date +%Y%m%d-%H%M%S)}"

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
    echo "adb: no device in 'device' state. Connect hardware or start an emulator; accept USB debugging if prompted." >&2
    exit 1
  fi
  if [[ "$count" -gt 1 ]]; then
    echo "adb: multiple devices/emulators connected (${count}). Set ANDROID_SERIAL to one UDID (see: adb devices)." >&2
    exit 1
  fi
}

if ! [[ "$ROUNDS" =~ ^[0-9]+$ ]] || [[ "$ROUNDS" -lt 1 ]]; then
  echo "ROUNDS must be a positive integer, got: $ROUNDS" >&2
  exit 1
fi

require_cmd rg "Install ripgrep (e.g. brew install ripgrep)."
require_cmd adb "Install Android platform-tools and ensure adb is on PATH."

ensure_adb_device

if [[ ! -d "$APP_PATH" ]]; then
  echo "APP_PATH is not a directory (expecting a macOS .app bundle): $APP_PATH" >&2
  echo "Set APP_PATH to your Bisq2.app location." >&2
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
SUMMARY_TMP="$OUT_DIR/.summary_rows.tsv"
: > "$SUMMARY_TMP"

log_step() {
  echo "[$(date '+%H:%M:%S')] $*"
}

cleanup_adb_tail() {
  local pid="${1:-}"
  if [[ -n "$pid" ]]; then
    kill "$pid" >/dev/null 2>&1 || true
    wait "$pid" 2>/dev/null || true
  fi
}

clear_adb_logcat() {
  log_step "Round ${CURRENT_ROUND}: clearing adb logcat buffers with 'adb logcat -b all -c'"
  adb_cmd logcat -b all -c
  # Repeat once to reduce chance of buffer race with in-flight lines.
  adb_cmd logcat -b all -c
  sleep 1
  local post_clear_lines
  post_clear_lines="$(adb_cmd logcat -b all -d | wc -l | tr -d ' ')"
  log_step "Round ${CURRENT_ROUND}: post-clear adb dump line count=${post_clear_lines}"
}

wait_for_bisq_initialized() {
  local start_line="$1"
  local deadline=$(( $(date +%s) + BISQ_INIT_TIMEOUT_SEC ))
  local wait_start_epoch
  wait_start_epoch="$(date +%s)"
  local last_report_epoch
  last_report_epoch="$wait_start_epoch"
  local baseline_reset_logged="false"

  while (( $(date +%s) <= deadline )); do
    local now_epoch
    now_epoch="$(date +%s)"
    local current_line_count
    current_line_count="$(wc -l < "$BISQ_LOG" || echo "$start_line")"

    # Bisq may rotate/truncate log files during restart. If the file shrank,
    # reset baseline so we continue scanning the new file content.
    if (( current_line_count < start_line )); then
      if [[ "$baseline_reset_logged" == "false" ]]; then
        log_step "Round ${CURRENT_ROUND}: detected Bisq log truncation/rotation (start_line=${start_line}, current_line_count=${current_line_count}), resetting baseline to 0"
        baseline_reset_logged="true"
      fi
      start_line=0
    fi

    local line
    line="$(sed -n "$((start_line + 1)),\$p" "$BISQ_LOG" | rg -m1 "ApplicationService initialized" || true)"
    if [[ -n "$line" ]]; then
      printf '%s\n' "$line"
      return 0
    fi
    if (( now_epoch - last_report_epoch >= 5 )); then
      local new_lines="$((current_line_count - start_line))"
      local elapsed="$((now_epoch - wait_start_epoch))"
      log_step "Round ${CURRENT_ROUND}: waiting Bisq init... elapsed=${elapsed}s bisq_lines_total=${current_line_count} bisq_new_lines=${new_lines} baseline_start_line=${start_line}"
      last_report_epoch="$now_epoch"
    fi
    sleep 1
  done

  return 1
}

wait_for_adb_connected() {
  local adb_log_file="$1"
  local start_line="${2:-0}"
  local deadline=$(( $(date +%s) + ADB_CONNECT_TIMEOUT_SEC ))
  local wait_start_epoch
  wait_start_epoch="$(date +%s)"
  local last_report_epoch
  last_report_epoch="$wait_start_epoch"

  while (( $(date +%s) <= deadline )); do
    local now_epoch
    now_epoch="$(date +%s)"
    local line
    # Only lines after t1 (adb snapshot at Bisq init): full-file rg could match earlier noise.
    line="$(sed -n "$((start_line + 1)),\$p" "$adb_log_file" | rg -m1 "WS connected successfully" || true)"
    if [[ -n "$line" ]]; then
      printf '%s\n' "$line"
      return 0
    fi
    if (( now_epoch - last_report_epoch >= 5 )); then
      local elapsed="$((now_epoch - wait_start_epoch))"
      log_step "Round ${CURRENT_ROUND}: waiting adb WS connect... elapsed=${elapsed}s search_start_line=${start_line}"
      last_report_epoch="$now_epoch"
    fi
    sleep 1
  done

  return 1
}

for round in $(seq 1 "$ROUNDS"); do
  echo "=== Round $round / $ROUNDS ==="
  CURRENT_ROUND="$round"
  local_round_file="$OUT_DIR/Round${round}.log"
  local_adb_file="$OUT_DIR/Round${round}.adb.log"

  round_start_iso="$(date '+%Y-%m-%d %H:%M:%S')"
  bisq_start_line="$(wc -l < "$BISQ_LOG" || echo 0)"
  adb_pid=""
  bisq_line=""
  adb_line=""
  adb_start_line_for_wait="0"
  t1_epoch=""
  t2_epoch=""
  t1_iso=""
  t2_iso=""
  duration_sec=""
  round_status="success"

  clear_adb_logcat
  adb_cmd logcat -b all -v time > "$local_adb_file" 2>&1 &
  adb_pid="$!"

  # 1) Kill Bisq2 app.
  log_step "Round $round: killing Bisq2"
  pkill -x Bisq2 >/dev/null 2>&1 || true
  # 2) Wait.
  log_step "Round $round: sleeping ${SLEEP_BEFORE_OPEN}s before relaunch"
  sleep "$SLEEP_BEFORE_OPEN"
  # 3) Open Bisq2 again.
  log_step "Round $round: opening Bisq2 app"
  open "$APP_PATH"

  # 4) Wait for "ApplicationService initialized" and capture t1.
  log_step "Round $round: monitoring Bisq log file: $BISQ_LOG"
  log_step "Round $round: waiting for Bisq log marker \"ApplicationService initialized\""
  if bisq_line="$(wait_for_bisq_initialized "$bisq_start_line")"; then
    t1_epoch="$(date +%s)"
    t1_iso="$(date '+%Y-%m-%d %H:%M:%S')"
    adb_start_line_for_wait="$(wc -l < "$local_adb_file" | tr -d ' ')"
    log_step "Round $round: captured t1=$t1_iso"
  else
    round_status="bisq-init-timeout"
    log_step "Round $round: timeout waiting for Bisq initialization marker"
  fi

  # 5) Wait for adb WS success and capture t2.
  if [[ "$round_status" == "success" ]]; then
    log_step "Round $round: waiting up to ${ADB_CONNECT_TIMEOUT_SEC}s for \"WS connected successfully\" (search starts after adb_line>${adb_start_line_for_wait})"
    if adb_line="$(wait_for_adb_connected "$local_adb_file" "$adb_start_line_for_wait")"; then
      t2_epoch="$(date +%s)"
      t2_iso="$(date '+%Y-%m-%d %H:%M:%S')"
      duration_sec="$((t2_epoch - t1_epoch))"
      log_step "Round $round: captured t2=$t2_iso duration=${duration_sec}s"
    else
      round_status="adb-connect-timeout"
      log_step "Round $round: timeout waiting for adb WS connect marker"
    fi
  fi

  cleanup_adb_tail "$adb_pid"
  clear_adb_logcat

  {
    echo "round: $round"
    echo "status: $round_status"
    echo "round_start: $round_start_iso"
    if [[ -n "$t1_iso" ]]; then
      echo "t1_bisq_initialized: $t1_iso"
    else
      echo "t1_bisq_initialized: <missing>"
    fi
    if [[ -n "$t2_iso" ]]; then
      echo "t2_ws_connected: $t2_iso"
    else
      echo "t2_ws_connected: <missing>"
    fi
    if [[ -n "$duration_sec" ]]; then
      echo "duration_seconds: $duration_sec"
    else
      echo "duration_seconds: <missing>"
    fi
    echo "adb_connect_timeout_seconds: $ADB_CONNECT_TIMEOUT_SEC"
    if [[ -n "$bisq_line" ]]; then
      echo "bisq_match: $bisq_line"
    else
      echo "bisq_match: <missing>"
    fi
    if [[ -n "$adb_line" ]]; then
      echo "adb_match: $adb_line"
    else
      echo "adb_match: <missing>"
    fi
    echo
    echo "===== adb logcat slice ====="
    cat "$local_adb_file"
  } > "$local_round_file"

  if [[ "$round_status" == "success" ]]; then
    echo "Round $round completed in ${duration_sec}s."
  else
    echo "Round $round ended with status: $round_status"
  fi
  node_start_ts="${t1_iso:-<missing>}"
  app_connect_ts="${t2_iso:-<missing>}"
  duration_value="${duration_sec:-<missing>}"
  printf "%s\t%s\t%s\t%s\t%s\n" \
    "$round" \
    "$node_start_ts" \
    "$app_connect_ts" \
    "$duration_value" \
    "$round_status" >> "$SUMMARY_TMP"
  echo "Saved: $local_round_file"
  echo
done

{
  echo "round,node_start_timestamp,app_connect_timestamp,duration_seconds,status"
  while IFS=$'\t' read -r round_num node_start app_connect duration status; do
    echo "${round_num},${node_start},${app_connect},${duration},${status}"
  done < "$SUMMARY_TMP"
} > "$SUMMARY_CSV"
rm -f "$SUMMARY_TMP"

echo "Summary CSV: $SUMMARY_CSV"
echo "Done. Logs directory: $OUT_DIR"
