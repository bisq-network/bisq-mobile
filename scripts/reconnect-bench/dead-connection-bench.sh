#!/usr/bin/env bash
# Measures how long Bisq Connect takes to notice a dead connection to its trusted node, and to
# recover once the node answers again.
#
# A dead connection is simulated by freezing the node process (SIGSTOP): the socket stays open
# and the OS keeps acknowledging TCP, but nothing answers. Unlike killing the node, no FIN is
# sent, so the app only finds out through its own checks. SIGCONT resumes the node. The script
# always resumes the node on exit, also on Ctrl-C.
#
# Times are host-side: from the moment the script acts (or you press Enter) to the moment the
# app's log line reaches the Mac. Log relay adds well under a second. Use a debug build: release
# builds only log errors.
#
# Usage:
#   scripts/reconnect-bench/dead-connection-bench.sh <scenario> --source <device|sim|android> [options]
#
# Scenarios:
#   freeze      app in the foreground and connected; the script freezes the node, waits for the
#               app to report RECONNECTING, holds, resumes the node and waits for it to reconnect.
#   resume      you background the app; the script waits --away seconds, optionally with the node
#               frozen (--frozen-while-away), then asks you to foreground the app and measures until
#               the app reports a verified connection or RECONNECTING.
#   netswitch   manual: press Enter at the moment you switch the phone's network (e.g. Wi-Fi off);
#               measures until the app reports RECONNECTING, then until it is connected again.
#
# Options:
#   --source device|sim|android  where to read the app log from (USB iPhone via pymobiledevice3,
#                                booted iOS Simulator, or the adb device)
#   --rounds N                   rounds to run (default 3)
#   --label TEXT                 free text stored with the results, e.g. "tor-baseline" (default none)
#   --away SECONDS               resume: time in the background (default 60)
#   --frozen-while-away          resume: freeze the node while the app is away, keep it frozen on return
#   --hold SECONDS               freeze: keep the node frozen this long after detection (default 10)
#   --settle SECONDS             wait between rounds so the connection is steady again (default 20)
#   --timeout SECONDS            max wait for any app log marker (default 180)
#
# Environment:
#   NODE_PID           pid of the node process (default: pgrep -x "$NODE_PROCESS")
#   NODE_PROCESS       node process name (default Bisq2, the desktop app; for a headless node pass NODE_PID)
#   IOS_APP_PROCESS    iOS process name (default "Bisq Connect")
#   ANDROID_PACKAGE    Android package (default network.bisq.mobile.client.debug)
#   PYMOBILEDEVICE3    pymobiledevice3 binary (default: on PATH, else ~/.local/bin/pymobiledevice3)
#   OUT_DIR            output folder (default debug/dead-connection-<scenario>-<timestamp>-<git-short>)
#
# Results: summary.csv plus the full app log of the run in OUT_DIR.
set -euo pipefail

cd "$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"

# Log markers. They match ClientConnectivityService log lines, so update both together.
MARK_RECONNECTING='Connectivity transition from [A-Z_]+ to RECONNECTING'
MARK_CONNECTED='Connectivity transition from [A-Z_]+ to (CONNECTED_AND_DATA_RECEIVED|REQUESTING_INVENTORY|CONNECTED_WITH_LIMITATIONS)'
MARK_HEALTH_OK='Health check result: alive=true|Connection trust restored'
MARK_BACKGROUND='App is in foreground -> false'
MARK_FOREGROUND='App is in foreground -> true'

fail() {
    echo "Error: $*" >&2
    exit 1
}

step() {
    echo "[$(date '+%H:%M:%S')] $*" >&2
}

[ $# -ge 1 ] || fail "usage: $0 <freeze|resume|netswitch> --source <device|sim|android> [options]"
scenario=$1
shift
case $scenario in
    freeze | resume | netswitch) ;;
    *) fail "unknown scenario $scenario" ;;
esac

source='' rounds=3 label='' away=60 frozen_while_away=false hold=10 settle=20 timeout=180
while [ $# -gt 0 ]; do
    case $1 in
        --source) source=${2:?}; shift 2 ;;
        --rounds) rounds=${2:?}; shift 2 ;;
        --label) label=${2:?}; shift 2 ;;
        --away) away=${2:?}; shift 2 ;;
        --frozen-while-away) frozen_while_away=true; shift ;;
        --hold) hold=${2:?}; shift 2 ;;
        --settle) settle=${2:?}; shift 2 ;;
        --timeout) timeout=${2:?}; shift 2 ;;
        *) fail "unknown argument $1" ;;
    esac
done
for n in "$rounds" "$away" "$hold" "$settle" "$timeout"; do
    [[ $n =~ ^[0-9]+$ ]] || fail "expected a number, got $n"
done

NODE_PROCESS=${NODE_PROCESS:-Bisq2}
IOS_APP_PROCESS=${IOS_APP_PROCESS:-Bisq Connect}
ANDROID_PACKAGE=${ANDROID_PACKAGE:-network.bisq.mobile.client.debug}
git_short=$(git rev-parse --short=8 HEAD 2> /dev/null || echo unknown)
OUT_DIR=${OUT_DIR:-$PWD/debug/dead-connection-$scenario-$(date +%Y%m%d-%H%M%S)-$git_short}

# --- node freeze / resume ------------------------------------------------------------------

node_pid=${NODE_PID:-}
node_frozen=false

needs_node() {
    [ "$scenario" = freeze ] || { [ "$scenario" = resume ] && [ "$frozen_while_away" = true ]; }
}

if needs_node; then
    if [ -z "$node_pid" ]; then
        node_pid=$(pgrep -x "$NODE_PROCESS" | head -1 || true)
    fi
    [ -n "$node_pid" ] || fail "node process '$NODE_PROCESS' is not running (or set NODE_PID)"
    kill -0 "$node_pid" 2> /dev/null || fail "no process with pid $node_pid"
    # A launcher (e.g. run_api.sh) may run the node as a child JVM: freeze the JVM, which is what answers
    node_jvm=$node_pid
    while true; do
        comm=$(ps -o comm= -p "$node_jvm" || true)
        case $comm in
            java | */java) break ;;
        esac
        node_jvm=$(pgrep -P "$node_jvm" | head -1 || true)
        [ -n "$node_jvm" ] || { node_jvm=$node_pid; break; }
    done
    node_pid=$node_jvm
fi

freeze_node() {
    kill -STOP "$node_pid"
    node_frozen=true
    sleep 0.2
    case $(ps -o stat= -p "$node_pid") in
        T*) ;;
        *) fail "pid $node_pid did not stop after SIGSTOP" ;;
    esac
}

resume_node() {
    if [ "$node_frozen" = true ]; then
        kill -CONT "$node_pid" 2> /dev/null || true
        node_frozen=false
    fi
}

# --- app log stream ------------------------------------------------------------------------

mkdir -p "$OUT_DIR"
app_log=$OUT_DIR/app.log
: > "$app_log"
stream_pid=''

start_stream() {
    case $source in
        device)
            local pmd=${PYMOBILEDEVICE3:-}
            if [ -z "$pmd" ]; then
                pmd=$(command -v pymobiledevice3 || echo "$HOME/.local/bin/pymobiledevice3")
            fi
            [ -x "$pmd" ] || fail "pymobiledevice3 not found (pipx install pymobiledevice3)"
            "$pmd" syslog live --process-name "$IOS_APP_PROCESS" > "$app_log" 2>&1 &
            ;;
        sim)
            command -v xcrun > /dev/null || fail "xcrun not found"
            xcrun simctl spawn booted log stream --style compact --level debug \
                --predicate "process == \"$IOS_APP_PROCESS\"" > "$app_log" 2>&1 &
            ;;
        android)
            command -v adb > /dev/null || fail "adb not found"
            local app_pid
            app_pid=$(adb shell pidof "$ANDROID_PACKAGE" 2> /dev/null | tr -d '\r' || true)
            [ -n "$app_pid" ] || fail "$ANDROID_PACKAGE is not running on the adb device"
            adb logcat -v time -T 1 --pid="$app_pid" > "$app_log" 2>&1 &
            ;;
        *) fail "--source must be device, sim or android" ;;
    esac
    stream_pid=$!
    sleep 2
    kill -0 "$stream_pid" 2> /dev/null || fail "log stream did not start: $(tail -3 "$app_log")"
}

cleanup() {
    resume_node
    if [ -n "$stream_pid" ]; then
        kill "$stream_pid" 2> /dev/null || true
        wait "$stream_pid" 2> /dev/null || true
    fi
}
trap cleanup EXIT
trap 'exit 130' INT TERM

line_count() {
    wc -l < "$app_log" | tr -d ' '
}

now_ms() {
    python3 -c 'import time; print(int(time.time() * 1000))'
}

# Waits until a line after line $1 matches regex $2. Prints the matching line number.
wait_for() {
    local from=$1 regex=$2 deadline match
    deadline=$(($(date +%s) + timeout))
    while [ "$(date +%s)" -le "$deadline" ]; do
        match=$(tail -n +"$((from + 1))" "$app_log" | grep -nE -m1 "$regex" || true)
        if [ -n "$match" ]; then
            echo $((from + ${match%%:*}))
            return 0
        fi
        sleep 0.2
    done
    return 1
}

seconds_since() {
    python3 -c 'import sys; print(f"{(int(sys.argv[2]) - int(sys.argv[1])) / 1000:.1f}")' "$1" "$(now_ms)"
}

# --- rounds --------------------------------------------------------------------------------

summary=$OUT_DIR/summary.csv
echo "round,scenario,source,label,detect_s,recover_s,result,detail" > "$summary"

record() {
    # '-' rather than an empty field, so `column` keeps the columns aligned
    echo "$1,$scenario,$source,${label:--},${2:--},${3:--},$4,${5:--}" >> "$summary"
    step "round $1: detect=${2:--}s recover=${3:--}s result=$4 $5"
}

start_stream
step "reading app log from $source into $app_log"
# The monitor logs a health check every 5s. If none is readable, the markers will never match.
sleep 6
if ! grep -qE 'Health check|Connectivity' "$app_log"; then
    if grep -q '<private>' "$app_log"; then
        fail "the app log text is hidden as <private>: install a debug build that includes useReadableDeviceLogsInDebugBuilds()"
    fi
    fail "no connectivity log lines from the app in 6s: is a debug build running and connected?"
fi
if needs_node; then
    step "node: pid $node_pid ($(ps -o args= -p "$node_pid" | cut -c1-80))"
fi

prompt() {
    printf '\a' >&2
    read -r -p "$* " _ < /dev/tty
}

run_freeze() {
    local round=$1 t0 from detect recover
    from=$(line_count)
    freeze_node
    t0=$(now_ms)
    step "round $round: node frozen, waiting for RECONNECTING"
    if from=$(wait_for "$from" "$MARK_RECONNECTING"); then
        detect=$(seconds_since "$t0")
    else
        resume_node
        record "$round" '' '' timeout "no RECONNECTING within ${timeout}s"
        return
    fi
    step "round $round: detected after ${detect}s, holding ${hold}s"
    sleep "$hold"
    resume_node
    t0=$(now_ms)
    if wait_for "$from" "$MARK_CONNECTED" > /dev/null; then
        recover=$(seconds_since "$t0")
        record "$round" "$detect" "$recover" ok ''
    else
        record "$round" "$detect" '' timeout "not connected within ${timeout}s of resuming the node"
    fi
}

run_resume() {
    local round=$1 t0 from match
    from=$(line_count)
    prompt "round $round: put the app in the background (home gesture), then press Enter"
    wait_for "$from" "$MARK_BACKGROUND" > /dev/null || {
        record "$round" '' '' timeout "app did not log going to the background"
        return
    }
    [ "$frozen_while_away" = true ] && freeze_node
    step "round $round: app in background (node frozen: $node_frozen), waiting ${away}s"
    sleep "$away"
    from=$(line_count)
    prompt "round $round: open the app now, then press Enter"
    if ! from=$(wait_for "$from" "$MARK_FOREGROUND"); then
        resume_node
        record "$round" '' '' timeout "app did not log coming to the foreground"
        return
    fi
    t0=$(now_ms)
    if [ "$frozen_while_away" = true ]; then
        # The connection is dead: measure how fast the app notices
        if wait_for "$from" "$MARK_RECONNECTING" > /dev/null; then
            record "$round" "$(seconds_since "$t0")" '' ok "node frozen while away"
        else
            record "$round" '' '' timeout "no RECONNECTING within ${timeout}s"
        fi
        resume_node
    else
        # The node is fine: measure how fast the app confirms the connection, and whether it had to reconnect
        if match=$(wait_for "$from" "$MARK_HEALTH_OK|$MARK_RECONNECTING"); then
            if sed -n "${match}p" "$app_log" | grep -qE "$MARK_RECONNECTING"; then
                if wait_for "$match" "$MARK_CONNECTED" > /dev/null; then
                    record "$round" '' "$(seconds_since "$t0")" ok "socket was dead after resume and reconnected"
                else
                    record "$round" '' '' timeout "socket was dead after resume and no reconnect within ${timeout}s"
                fi
            else
                record "$round" '' "$(seconds_since "$t0")" ok "existing socket verified alive"
            fi
        else
            record "$round" '' '' timeout "no health check result within ${timeout}s"
        fi
    fi
}

run_netswitch() {
    local round=$1 t0 from
    from=$(line_count)
    prompt "round $round: press Enter at the moment you switch the phone's network"
    t0=$(now_ms)
    if from=$(wait_for "$from" "$MARK_RECONNECTING"); then
        local detect
        detect=$(seconds_since "$t0")
        t0=$(now_ms)
        if wait_for "$from" "$MARK_CONNECTED" > /dev/null; then
            record "$round" "$detect" "$(seconds_since "$t0")" ok ''
        else
            record "$round" "$detect" '' timeout "not connected again within ${timeout}s"
        fi
    else
        record "$round" '' '' no-reconnect "no RECONNECTING within ${timeout}s - the socket may have survived the switch"
    fi
    prompt "round $round: switch the network back, then press Enter"
}

for round in $(seq 1 "$rounds"); do
    "run_$scenario" "$round"
    if [ "$round" -lt "$rounds" ]; then
        step "settling ${settle}s"
        sleep "$settle"
    fi
done

echo
column -s, -t < "$summary"
echo
echo "Results: $OUT_DIR"
