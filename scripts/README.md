# Scripts

## Bisq2 reconnect timing (optional)

These bash scripts measure timing between Bisq desktop (`Bisq2.app`) restarts and mobile client log markers (WebSocket connect). They are macOS-only (`open`, `~/Library/Application Support/Bisq2/`).

| Script | Role |
|--------|------|
| [`run_bisq_reconnect_rounds_android.sh`](run_bisq_reconnect_rounds_android.sh) | Android: tails desktop log + `adb logcat` while cycling Bisq2 |
| [`run_bisq_reconnect_rounds_ios.sh`](run_bisq_reconnect_rounds_ios.sh) | iOS Simulator: tails desktop log + `simctl log stream` |
| [`run_bisq_reconnect_rounds_android_ios.sh`](run_bisq_reconnect_rounds_android_ios.sh) | Android + iOS Simulator together: one Bisq2 restart per round, both mobile captures in parallel |

Requirements: Bash, [ripgrep](https://github.com/BurntSushi/ripgrep) (`rg`), Bisq2 desktop installed. Android scripts need `adb` (exactly one connected device/emulator, or set `ANDROID_SERIAL`). iOS scripts need Xcode Command Line Tools (`xcrun`); they use Simulator log streaming, not USB device capture by default. The combined script needs both `adb` and a booted iOS Simulator.

Artifacts default to `debug/<timestamped-folder>/` under the repository root when the script lives in a git clone (that tree is gitignored). Default folder names:

- Android: `reconnect-round-logs-<timestamp>`
- iOS: `reconnect-round-logs-ios-<timestamp>`
- Combined: `reconnect-round-logs-android-ios-<timestamp>`

Override with `OUT_DIR`. Environment variables are documented in each script header (`APP_PATH`, `BISQ_LOG`, timeouts, `IOS_*`, `ANDROID_SERIAL`, etc.).

If the primary desktop log (`BISQ_LOG`, default `~/Library/Application Support/Bisq2/bisq.log`) is missing, all scripts fall back to `bisq_1.log` in the same directory.

### Round sequence (matches the scripts)

Each round repeats the steps below.

**Timestamps:** t1 is recorded when `ApplicationService initialized` appears in the desktop log. t2 is when the platform’s WS success marker appears in the mobile log. Duration is `t2 − t1` in whole seconds.

**When t1/t2/duration are set:**

- **Android / iOS (single-platform):** t1, t2, and duration are only set when **both** the Bisq init wait and the mobile WS wait succeed. On timeout, `RoundN.log` and `summary.csv` still record status (e.g. `bisq-init-timeout`, `adb-connect-timeout`, `ios-connect-timeout`) with `<missing>` timestamps where applicable.
- **Combined:** t1 is set if Bisq init succeeds. Android and iOS each get their own t2 and duration from t1. Round status is `success` only when **both** platforms connect within `CONNECT_TIMEOUT_SEC`. Partial success is recorded (one platform’s t2/duration may be present) with statuses such as `adb-connect-timeout`, `ios-connect-timeout`, or `adb-ios-connect-timeout`.

**Inter-round delay:** If the round succeeded and more rounds remain, the script sleeps `SLEEP_AFTER_CONNECT_SEC` (default 30; set `0` to skip) **before** stopping mobile log capture. For the combined script, “success” means both Android and iOS connected.

#### [`run_bisq_reconnect_rounds_android.sh`](run_bisq_reconnect_rounds_android.sh) (Android)

1. Snapshot `bisq_start_line` (line count of the desktop `BISQ_LOG` file before this round’s restart).
2. Clear logcat buffers (`adb logcat -b all -c`, twice), sleep 1s, log a post-clear dump line count.
3. Start background capture: `adb logcat -b all -v time` → `RoundN.adb.log` (runs for the rest of the round).
4. Kill Bisq2 desktop (`pkill -x Bisq2`).
5. Sleep `SLEEP_BEFORE_OPEN` seconds (default 45).
6. Open Bisq2 (`open "$APP_PATH"`).
7. Wait (poll desktop log from `bisq_start_line`, with baseline reset if the log rotates) for `ApplicationService initialized` — record t1, and set `adb_start_line_for_wait` to the current line count of `RoundN.adb.log` (mobile matching starts after noise before t1).
8. If that succeeded: wait (poll new lines in `RoundN.adb.log` from that offset) for `WS connected successfully` — record t2 and duration (bounded by `ADB_CONNECT_TIMEOUT_SEC`, default 300). If another round follows and this round succeeded, sleep `SLEEP_AFTER_CONNECT_SEC` while logcat keeps running.
9. Stop the logcat background process; clear logcat buffers again (same as step 2).
10. Write `RoundN.log`: metadata and matched lines, then the full `RoundN.adb.log` contents under `===== adb logcat slice =====`.
11. Append one row to `summary.csv` (`round,node_start_timestamp,app_connect_timestamp,duration_seconds,status`).

After all rounds, print `summary.csv` path and the logs directory.

#### [`run_bisq_reconnect_rounds_ios.sh`](run_bisq_reconnect_rounds_ios.sh) (iOS Simulator)

1. Snapshot `bisq_start_line` (line count of the desktop `BISQ_LOG` file before this round’s restart).
2. Start background capture: `xcrun simctl spawn "$IOS_SIM_DEVICE" log stream …` → `RoundN.ios.log` (predicate/process as configured; default filters on process `Bisq Connect`).
3. Kill Bisq2 desktop (`pkill -x Bisq2`).
4. Sleep `SLEEP_BEFORE_OPEN` seconds (default 45).
5. Open Bisq2 (`open "$APP_PATH"`).
6. Wait (poll desktop log from `bisq_start_line`, with baseline reset if the log rotates) for `ApplicationService initialized` — record t1, and set `ios_start_line_for_wait` to the current line count of `RoundN.ios.log` (mobile matching starts after noise before t1).
7. If that succeeded: wait (poll new lines in `RoundN.ios.log` from that offset) for `IOS_WS_MARKER_REGEX` (default includes `WS connected successfully` and CFNetwork hints) — record t2 and duration (bounded by `IOS_CONNECT_TIMEOUT_SEC`, default 300). If another round follows and this round succeeded, sleep `SLEEP_AFTER_CONNECT_SEC` while the log stream keeps running.
8. Stop the `log stream` background process.
9. Write `RoundN.log`: metadata and matched lines, then the full `RoundN.ios.log` contents under `===== ios log stream slice =====`.
10. Append one row to `summary.csv` (`round,node_start_timestamp,app_connect_timestamp,duration_seconds,status`).

After all rounds, print `summary.csv` path and the logs directory.

#### [`run_bisq_reconnect_rounds_android_ios.sh`](run_bisq_reconnect_rounds_android_ios.sh) (Android + iOS Simulator)

One Bisq2 restart per round while **both** mobile captures run. Same clear/start/stop patterns as the single-platform scripts for each side.

1. Snapshot `bisq_start_line`.
2. Clear logcat buffers (same as Android script).
3. Start background capture: `adb logcat -b all -v time` → `RoundN.adb.log`.
4. Start background capture: `simctl log stream` → `RoundN.ios.log` (same options as the iOS script).
5. Kill Bisq2 desktop (`pkill -x Bisq2`).
6. Sleep `SLEEP_BEFORE_OPEN` seconds (default 45).
7. Open Bisq2 (`open "$APP_PATH"`).
8. Wait for `ApplicationService initialized` — record t1; set `adb_start_line_for_wait` and `ios_start_line_for_wait` from the current line counts of `RoundN.adb.log` and `RoundN.ios.log`.
9. If that succeeded: poll **both** logs in parallel until each platform’s WS marker appears or `CONNECT_TIMEOUT_SEC` (default 300) elapses for that side — record `t2_android`, `t2_ios`, and separate durations from t1. If another round follows and **both** connected, sleep `SLEEP_AFTER_CONNECT_SEC` while captures keep running.
10. Stop adb logcat and the iOS log stream; clear logcat buffers again.
11. Write `RoundN.log`: **metadata and match lines only** (full mobile logs are **not** inlined). Paths to `RoundN.adb.log` and `RoundN.ios.log` are listed in the metadata.
12. Append one row to `summary.csv` (`round,t1_bisq_initialized,t2_android_ws,t2_ios_ws,android_duration_seconds,ios_duration_seconds,status`).

After all rounds, print `summary.csv` path and the logs directory.

**WS markers (combined script):** Android uses `WS connected successfully` in logcat; iOS uses `IOS_WS_MARKER_REGEX` (same defaults as the iOS-only script).
