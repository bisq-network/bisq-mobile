#!/usr/bin/env bash
set -euo pipefail

# Bisq2 desktop ↔ iOS client reconnect timing (macOS, iOS Simulator).
#
# Prerequisites:
#   - macOS (uses `open`, ~/Library/Application Support/Bisq2/, default APP_PATH)
#   - bash, ripgrep (`rg`), Xcode Command Line Tools (`xcrun`)
#   - Bisq2 desktop installed; a booted iOS Simulator (or device UDID for simctl — see IOS_SIM_DEVICE)
#
# This script uses `xcrun simctl spawn … log stream`; it does not attach log capture for a USB
# physical iPhone the way macOS Console / `log stream` for a plugged-in device would. For physical
# devices, capture logs through Xcode or Console and compare timestamps manually, or extend this script.
#
# Working directory: if this script lives inside a git clone, cwd is set to the repo root
# so default OUT_DIR lands in <repo>/debug/ (that folder is gitignored).
#
# Environment (all optional unless noted):
#   APP_PATH              Bisq2.app bundle (default /Applications/Bisq2.app)
#   BISQ_LOG              Desktop bisq log file for "ApplicationService initialized"
#   OUT_DIR               Output directory (default ./debug/reconnect-round-logs-ios-<timestamp>)
#   SLEEP_BEFORE_OPEN     Seconds between kill and reopen (default 5)
#   BISQ_INIT_TIMEOUT_SEC Seconds to wait for desktop init marker (default 180)
#   IOS_CONNECT_TIMEOUT_SEC  Seconds to wait for WS success markers in sim logs (default 300)
#   IOS_WS_MARKER         Primary substring marker for success (default "WS connected successfully")
#   IOS_WS_MARKER_REGEX   Regex passed to rg for success (default combines marker + CFNetwork hints)
#   IOS_SIM_DEVICE        simctl target: "booted" or a simulator UDID (default booted)
#   IOS_APP_PROCESS       Process name as shown in Simulator logs (default "Bisq Connect")
#   IOS_LOG_PREDICATE     If set, used as `log stream --predicate` instead of process-based default
#   IOS_LOG_LEVEL         Argument for log stream --level (default debug; info omits debug messages)
#   IOS_WS_FAILURE_MARKER Regex for failure hints in heartbeat logs (optional diagnostics)
#
# Round sequence (numbered steps): see scripts/README.md section "Round sequence".
#
# Usage: ./scripts/run_bisq_reconnect_rounds_ios.sh [ROUNDS]

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
IOS_CONNECT_TIMEOUT_SEC="${IOS_CONNECT_TIMEOUT_SEC:-300}"
IOS_WS_MARKER="${IOS_WS_MARKER:-WS connected successfully}"
IOS_WS_MARKER_REGEX="${IOS_WS_MARKER_REGEX:-${IOS_WS_MARKER}|Connection [0-9]+: connected successfully|received response, status 101}"
IOS_SIM_DEVICE="${IOS_SIM_DEVICE:-booted}" # e.g. "booted" or specific UDID
IOS_APP_PROCESS="${IOS_APP_PROCESS:-Bisq Connect}" # process label shown in iOS logs
IOS_LOG_PREDICATE="${IOS_LOG_PREDICATE:-}" # optional override; if empty we default to app-process predicate
IOS_LOG_LEVEL="${IOS_LOG_LEVEL:-debug}"
IOS_WS_FAILURE_MARKER="${IOS_WS_FAILURE_MARKER:-WS connection failed to connect|Exception occurred whilst listening for WS messages}"
OUT_DIR="${OUT_DIR:-$PWD/debug/reconnect-round-logs-ios-$(date +%Y%m%d-%H%M%S)}"

require_cmd() {
  local cmd="$1"
  local hint="$2"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing required command: ${cmd}. ${hint}" >&2
    exit 1
  fi
}

if ! [[ "$ROUNDS" =~ ^[0-9]+$ ]] || [[ "$ROUNDS" -lt 1 ]]; then
  echo "ROUNDS must be a positive integer, got: $ROUNDS" >&2
  exit 1
fi

require_cmd rg "Install ripgrep (e.g. brew install ripgrep)."

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

if ! command -v xcrun >/dev/null 2>&1; then
  echo "xcrun not found; install Xcode command line tools first." >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
SUMMARY_CSV="$OUT_DIR/summary.csv"
SUMMARY_TMP="$OUT_DIR/.summary_rows.tsv"
: > "$SUMMARY_TMP"

log_step() {
  echo "[$(date '+%H:%M:%S')] $*"
}

cleanup_ios_tail() {
  local pid="${1:-}"
  if [[ -n "$pid" ]]; then
    kill "$pid" >/dev/null 2>&1 || true
    wait "$pid" 2>/dev/null || true
  fi
}

IOS_STREAM_PID=""
on_exit() {
  cleanup_ios_tail "$IOS_STREAM_PID"
  IOS_STREAM_PID=""
}
trap on_exit EXIT INT TERM

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
    echo "Failed to start iOS log stream. Is an iOS simulator/device booted?" >&2
    return 1
  fi
  printf '%s\n' "$stream_pid"
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

wait_for_ios_connected() {
  local ios_log_file="$1"
  local start_line="${2:-0}"
  local deadline=$(( $(date +%s) + IOS_CONNECT_TIMEOUT_SEC ))
  local wait_start_epoch
  wait_start_epoch="$(date +%s)"
  local last_report_epoch
  last_report_epoch="$wait_start_epoch"

  while (( $(date +%s) <= deadline )); do
    local now_epoch
    now_epoch="$(date +%s)"
    local line
    line="$(sed -n "$((start_line + 1)),\$p" "$ios_log_file" | rg -m1 "$IOS_WS_MARKER_REGEX" || true)"
    if [[ -n "$line" ]]; then
      printf '%s\n' "$line"
      return 0
    fi
    if (( now_epoch - last_report_epoch >= 5 )); then
      local elapsed="$((now_epoch - wait_start_epoch))"
      local total_lines
      local app_lines
      local app_new_lines
      local marker_hits
      local failure_hits
      total_lines="$(wc -l < "$ios_log_file" || echo 0)"
      app_lines="$(rg -c "$IOS_APP_PROCESS\\[[0-9]+:" "$ios_log_file" || true)"
      app_new_lines="$(sed -n "$((start_line + 1)),\$p" "$ios_log_file" | rg -c "$IOS_APP_PROCESS\\[[0-9]+:" || true)"
      marker_hits="$(sed -n "$((start_line + 1)),\$p" "$ios_log_file" | rg -c "$IOS_WS_MARKER_REGEX" || true)"
      failure_hits="$(rg -c "$IOS_WS_FAILURE_MARKER" "$ios_log_file" || true)"
      log_step "Round ${CURRENT_ROUND}: waiting iOS WS connect... elapsed=${elapsed}s marker_regex='${IOS_WS_MARKER_REGEX}' ios_lines=${total_lines} app_lines=${app_lines:-0} app_new_lines=${app_new_lines:-0} marker_hits=${marker_hits:-0} failure_hits=${failure_hits:-0} search_start_line=${start_line}"
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
  local_ios_file="$OUT_DIR/Round${round}.ios.log"

  round_start_iso="$(date '+%Y-%m-%d %H:%M:%S')"
  bisq_start_line="$(wc -l < "$BISQ_LOG" || echo 0)"
  ios_pid=""
  bisq_line=""
  ios_line=""
  t1_epoch=""
  t2_epoch=""
  t1_iso=""
  t2_iso=""
  duration_sec=""
  round_status="success"
  ios_start_line_for_wait="0"

  log_step "Round $round: starting iOS log stream (device=${IOS_SIM_DEVICE})"
  ios_pid="$(start_ios_log_stream "$local_ios_file")"
  IOS_STREAM_PID="$ios_pid"
  log_step "Round $round: iOS log stream pid=$ios_pid"

  log_step "Round $round: killing Bisq2"
  pkill -x Bisq2 >/dev/null 2>&1 || true

  log_step "Round $round: sleeping ${SLEEP_BEFORE_OPEN}s before relaunch"
  sleep "$SLEEP_BEFORE_OPEN"

  log_step "Round $round: opening Bisq2 app"
  open "$APP_PATH"

  log_step "Round $round: monitoring Bisq log file: $BISQ_LOG"
  log_step "Round $round: waiting for Bisq log marker \"ApplicationService initialized\""
  if bisq_line="$(wait_for_bisq_initialized "$bisq_start_line")"; then
    t1_epoch="$(date +%s)"
    t1_iso="$(date '+%Y-%m-%d %H:%M:%S')"
    ios_start_line_for_wait="$(wc -l < "$local_ios_file" || echo 0)"
    log_step "Round $round: captured t1=$t1_iso"
  else
    round_status="bisq-init-timeout"
    log_step "Round $round: timeout waiting for Bisq initialization marker"
  fi

  if [[ "$round_status" == "success" ]]; then
    log_step "Round $round: waiting up to ${IOS_CONNECT_TIMEOUT_SEC}s for iOS marker regex \"${IOS_WS_MARKER_REGEX}\" (search starts at ios_line>${ios_start_line_for_wait})"
    if ios_line="$(wait_for_ios_connected "$local_ios_file" "$ios_start_line_for_wait")"; then
      t2_epoch="$(date +%s)"
      t2_iso="$(date '+%Y-%m-%d %H:%M:%S')"
      duration_sec="$((t2_epoch - t1_epoch))"
      log_step "Round $round: captured t2=$t2_iso duration=${duration_sec}s"
    else
      round_status="ios-connect-timeout"
      log_step "Round $round: timeout waiting for iOS WS connect marker"
    fi
  fi

  cleanup_ios_tail "$ios_pid"

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
    echo "ios_connect_timeout_seconds: $IOS_CONNECT_TIMEOUT_SEC"
    echo "ios_ws_marker: $IOS_WS_MARKER"
    if [[ -n "$bisq_line" ]]; then
      echo "bisq_match: $bisq_line"
    else
      echo "bisq_match: <missing>"
    fi
    if [[ -n "$ios_line" ]]; then
      echo "ios_match: $ios_line"
    else
      echo "ios_match: <missing>"
    fi
    echo
    echo "===== ios log stream slice ====="
    cat "$local_ios_file"
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
