# Scripts

## Bisq2 reconnect timing (optional)

These bash scripts measure timing between Bisq **desktop** (`Bisq2.app`) restarts and mobile client log markers (WebSocket connect). They are **macOS-only** (`open`, `~/Library/Application Support/Bisq2/`).

| Script | Role |
|--------|------|
| [`run_bisq_reconnect_rounds_android.sh`](run_bisq_reconnect_rounds_android.sh) | Android: tails desktop log + `adb logcat` while cycling Bisq2 |
| [`run_bisq_reconnect_rounds_ios.sh`](run_bisq_reconnect_rounds_ios.sh) | iOS Simulator: tails desktop log + `simctl log stream` |

**Requirements:** Bash, [ripgrep](https://github.com/BurntSushi/ripgrep) (`rg`), Bisq2 desktop installed. The Android script also needs `adb` (exactly one connected device/emulator, or set `ANDROID_SERIAL`). The iOS script needs Xcode Command Line Tools (`xcrun`); it uses **Simulator** log streaming, not USB device capture by default.

Artifacts default to `debug/<timestamped-folder>/` under the **repository root** when the script lives in a git clone (that tree is gitignored). Override with `OUT_DIR`. Environment variables are documented in each script header (`APP_PATH`, `BISQ_LOG`, timeouts, `IOS_*`, etc.).

### Round sequence (matches the scripts)

Each **round** repeats the steps below. Timestamps **t1** / **t2** and **duration** (`t2 − t1`, whole seconds) are only set when both waits succeed.

#### [`run_bisq_reconnect_rounds_android.sh`](run_bisq_reconnect_rounds_android.sh) (Android)

1. Snapshot `bisq_start_line` (line count of the desktop `BISQ_LOG` file before this round’s restart).
2. **Clear** logcat buffers (`adb logcat -b all -c`, twice), sleep 1s, log a post-clear dump line count.
3. **Start** background capture: `adb logcat -b all -v time` → `RoundN.adb.log` (runs for the rest of the round).
4. **Kill** Bisq2 desktop (`pkill -x Bisq2`).
5. **Sleep** `SLEEP_BEFORE_OPEN` seconds.
6. **Open** Bisq2 (`open "$APP_PATH"`).
7. **Wait** (poll desktop log from `bisq_start_line`) for **`ApplicationService initialized`** — record **t1**.
8. If that succeeded: **wait** (poll `RoundN.adb.log`) for **`WS connected successfully`** — record **t2** and duration (bounded by `ADB_CONNECT_TIMEOUT_SEC`).
9. **Stop** the logcat background process; **clear** logcat buffers again (same as step 2).
10. **Write** `RoundN.log`: metadata, matched lines, then the full **`RoundN.adb.log`** contents under `===== adb logcat slice =====`.
11. **Append** one row to the per-run summary TSV.

After all rounds, the script builds **`summary.csv`** from the TSV and prints paths.

#### [`run_bisq_reconnect_rounds_ios.sh`](run_bisq_reconnect_rounds_ios.sh) (iOS Simulator)

1. Snapshot `bisq_start_line` (line count of the desktop `BISQ_LOG` file before this round’s restart).
2. **Start** background capture: `xcrun simctl spawn "$IOS_SIM_DEVICE" log stream …` → `RoundN.ios.log` (predicate/process as configured).
3. **Kill** Bisq2 desktop (`pkill -x Bisq2`).
4. **Sleep** `SLEEP_BEFORE_OPEN` seconds.
5. **Open** Bisq2 (`open "$APP_PATH"`).
6. **Wait** (poll desktop log from `bisq_start_line`) for **`ApplicationService initialized`** — record **t1**, and set `ios_start_line_for_wait` to the current line count of `RoundN.ios.log` (so mobile-log matching starts after noise before t1).
7. If that succeeded: **wait** (poll new lines in `RoundN.ios.log` from that offset) for **`IOS_WS_MARKER_REGEX`** — record **t2** and duration (bounded by `IOS_CONNECT_TIMEOUT_SEC`).
8. **Stop** the `log stream` background process.
9. **Write** `RoundN.log`: metadata, matched lines, then the full **`RoundN.ios.log`** contents under `===== ios log stream slice =====`.
10. **Append** one row to the per-run summary TSV.

After all rounds, the script builds **`summary.csv`** from the TSV and prints paths.
