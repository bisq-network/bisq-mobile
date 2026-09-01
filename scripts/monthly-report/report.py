#!/usr/bin/env python3
"""Bisq Mobile monthly KPI report generator.

Phase 1 (this file): automates the two halves that need only access we already have —
  - Engagement & health from GlitchTip (opted-in floor; NEVER a user headcount, by privacy design)
  - Sideload base from GitHub download stats
plus a manual-inputs seam (inputs.json) for store/operator numbers until the Play/ASC APIs land.

Output is Markdown to stdout (or --out FILE) for you to review/tweak and paste into the GH wiki.

    python3 report.py --window 30 --inputs inputs.json --out report-2026-08.md

Design note on honesty: the report deliberately keeps three provenance tiers separate — real store
user counts (Play/ASC), sideload floors (GitHub), and engagement floors (GlitchTip). It never blends
them into a single "users" number, because the channels are not deduplicable (a person can be on
Play AND sideload). See README.md.
"""
from __future__ import annotations

import argparse
import json
import os
import re
from datetime import date

import glitchtip
import github_downloads

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
HISTORY_DIR = os.path.join(SCRIPT_DIR, "history")
MONTH_RE = re.compile(r"^\d{4}-\d{2}$")


def _load_env() -> None:
    """Populate os.environ from a gitignored `.env` next to this script (KEY=VALUE lines), without
    overriding anything already set in the shell. Lets `python3 report.py` just work — no exports."""
    try:
        with open(os.path.join(SCRIPT_DIR, ".env")) as f:
            for raw in f:
                line = raw.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    os.environ.setdefault(k.strip(), v.strip().strip('"').strip("'"))
    except FileNotFoundError:
        pass


def _fmt(v) -> str:
    return "—" if v is None else (f"{v:,}" if isinstance(v, int) else str(v))


def _funnel_count(funnel: list[dict], *prefixes: str) -> int:
    return sum(r["n"] for r in funnel if any(r["step"].startswith(p) for p in prefixes))


def _bar_chart(title: str, items: list[tuple[str, int]], width: int = 34) -> list[str]:
    """Horizontal bar chart as a monospace code block — renders in ANY markdown tool (MacDown,
    GitHub, wikis) with no image hosting, unlike Mermaid which needs a Mermaid-aware renderer."""
    total = sum(v for _, v in items) or 1
    mx = max((v for _, v in items), default=1) or 1
    lblw = max((len(lbl) for lbl, _ in items), default=0)
    out = ["```", title, ""]
    for label, v in items:
        bar = "█" * max(1, round(v / mx * width))
        out.append(f"{label.ljust(lblw)}  {bar}  {v:,} ({round(100 * v / total)}%)")
    out.append("```")
    return out


# --- Month-over-month history (self-contained JSON snapshots, error-safe) -----

def _load_snapshot(month: str) -> dict | None:
    """Saved snapshot for `month`, or None. Never raises."""
    try:
        with open(os.path.join(HISTORY_DIR, month + ".json")) as fh:
            return json.load(fh)
    except (OSError, ValueError):
        return None


def _load_prev_snapshot(month: str) -> tuple[str | None, dict | None]:
    """Most recent saved snapshot strictly older than `month`. Never raises — a missing/unreadable
    history dir just means 'no comparison yet' (e.g. the very first report)."""
    try:
        keys = sorted(f[:-5] for f in os.listdir(HISTORY_DIR)
                      if f.endswith(".json") and f[:-5] < month)
    except OSError:
        return None, None
    if not keys:
        return None, None
    snap = _load_snapshot(keys[-1])
    return (keys[-1], snap) if snap else (None, None)


def _save_snapshot(month: str, snap: dict) -> None:
    """Best-effort persist; a failure here must never break report generation."""
    try:
        os.makedirs(HISTORY_DIR, exist_ok=True)
        with open(os.path.join(HISTORY_DIR, month + ".json"), "w") as fh:
            json.dump(snap, fh, indent=2, sort_keys=True)
    except OSError:
        pass


def _num(v) -> str:
    if v is None:
        return "—"
    if isinstance(v, float):
        return f"{v:,.2f}".rstrip("0").rstrip(".")
    return f"{v:,}"


def _delta(cur, prev) -> str:
    if cur is None or prev is None:
        return "—"
    d = cur - prev
    if abs(d) < 1e-9:
        return "±0"
    mag = _num(round(abs(d), 2) if isinstance(d, float) else abs(d))
    return f"{'▲ +' if d > 0 else '▼ −'}{mag}"


def _sideload_mid(sideload, needle: str):
    for a in sideload:
        if needle.lower() in a.app.lower() and a.active_base_estimate:
            lo, hi = a.active_base_estimate
            return (lo + hi) // 2
    return None


def _mom_section(snap: dict, prev_month: str | None, prev: dict | None) -> list[str]:
    L = ["## Month over month", ""]
    if not prev:
        L.append("_First report — no prior month to compare yet. Next month's report will show "
                 "deltas here automatically._")
        L.append("")
        return L
    L.append(f"_Change vs **{prev_month}**._")
    L.append("")
    rows = [
        ("Bisq Easy — active devices", "node_active_devices"),
        ("Bisq Easy — MAU", "node_mau"),
        ("Bisq Easy — DAU", "node_dau"),
        ("Bisq Easy — rating", "node_rating"),
        ("Bisq Connect — audience (all platforms)", "connect_audience_total"),
        ("Bisq Connect — iOS testers", "connect_ios_testers"),
        ("Bisq Connect — new iOS testers (30d)", "connect_ios_new_testers"),
        ("Bisq Connect — MAU", "connect_mau"),
        ("Bisq Connect — rating", "connect_rating"),
        ("Sideload base — Easy (mid)", "sideload_easy_mid"),
        ("Sideload base — Connect (mid)", "sideload_connect_mid"),
        ("Analytics events (opted-in)", "analytics_events_total"),
        ("New opt-ins", "new_optins_total"),
        ("Trades started", "trades_started"),
        ("Trades completed", "trades_completed"),
    ]
    L.append("| Metric | This month | vs last month |")
    L.append("|---|---|---|")
    for lbl, key in rows:
        L.append(f"| {lbl} | {_num(snap.get(key))} | {_delta(snap.get(key), prev.get(key))} |")
    L.append("")
    return L


def _wikiify(md: str, month: str, heading: str) -> str:
    """Wiki-page variant: reports stack newest-first on one year page, so the H1 title becomes an
    H2 month section ('## July 2026') and every other heading demotes one level — no stacked H1s.
    Fenced code blocks (the bar charts) are left untouched."""
    try:
        y, m = (int(x) for x in month.split("-"))
        title = date(y, m, 1).strftime("%B %Y")
    except ValueError:
        title = heading
    out: list[str] = []
    fenced = replaced_title = False
    for ln in md.split("\n"):
        if ln.startswith("```"):
            fenced = not fenced
        elif not fenced and ln.startswith("#"):
            if not replaced_title:
                replaced_title = True
                out.append(f"## {title}")
                continue
            ln = "#" + ln
        out.append(ln)
    return "\n".join(out)


def render(window_days: int, inputs: dict, label: str | None = None, wiki: bool = False) -> str:
    # `month` keys the history snapshots and the Play bucket lookup, so it must stay YYYY-MM.
    # A YYYY-MM label sets it (the usual way to report on a past month); any other label
    # ('Aug 1–14') is display-only and falls back to inputs/today for the key.
    if label and MONTH_RE.match(label):
        month = label
    else:
        month = inputs.get("month", date.today().strftime("%Y-%m"))
    heading = label or month
    gt = glitchtip.collect(window_days)
    sideload = github_downloads.collect()
    stores = inputs.get("stores", {})

    # Overlay live Play Vitals (crash/ANR) when reachable; silently fall back to manual inputs
    # otherwise (missing key, no venv/google-auth, denied access, or app below Play's data floor).
    vitals_as_of = None
    try:
        import play
        for app_label, v in play.collect().items():
            s = stores.setdefault(app_label, {})
            if v.get("crash_rate_pct") is not None:
                s["play_crash_rate_pct"] = v["crash_rate_pct"]
            if v.get("anr_rate_pct") is not None:
                s["play_anr_rate_pct"] = v["anr_rate_pct"]
            vitals_as_of = v.get("as_of") or vitals_as_of
    except Exception:
        pass

    # Overlay live audience + new installs from the Play statistics bucket (via gcloud); falls back
    # to manual inputs when the bucket env / gcloud auth / month file is absent.
    installs_live = False
    try:
        import play_installs
        for app_label, v in play_installs.collect(month).items():
            s = stores.setdefault(app_label, {})
            for k, val in v.items():
                if val is not None:
                    s[k] = val
                    installs_live = True
    except Exception:
        pass

    connect = stores.get("Bisq Connect (Android)", {})
    node = stores.get("Bisq Easy Node (Android)", {})
    ios = stores.get("Bisq Connect (iOS)", {})
    L: list[str] = []

    L.append(f"# Bisq Mobile — KPI Report — {heading}")
    L.append("")
    L.append(f"_Generated {date.today().isoformat()} · window: last {window_days} days "
             "(Play metrics are a 28-day average). Sources: **Play / TestFlight** (real audience), "
             "**GitHub** (sideload), **self-hosted analytics** (engagement, opt-in only)._")
    L.append("")

    # ---- Audience overview (the honest combined picture) --------------------
    connect_android_audience = connect.get("play_active_devices_avg") or 0
    connect_ios_audience = ios.get("testflight_testers") or 0
    node_audience = node.get("play_active_devices_avg") or 0
    connect_total = connect_android_audience + connect_ios_audience

    # Trade funnel — computed once here, reused in the Trade activity section + the snapshot.
    # Rows come per (project, step); aggregate per step for the totals, keep the split for
    # per-app attribution in the reasons section.
    fn_rows = gt.trade_funnel
    _by_step: dict[str, int] = {}
    for r in fn_rows:
        _by_step[r["step"]] = _by_step.get(r["step"], 0) + r["n"]
    fn = sorted(({"step": s, "n": n} for s, n in _by_step.items()), key=lambda r: -r["n"])
    taken = _funnel_count(fn, "trade.taken")
    completed = _funnel_count(fn, "trade.completed")
    cancelled = _funnel_count(fn, "trade.cancelled")
    rejected = _funnel_count(fn, "trade.rejected")
    errored = _funnel_count(fn, "trade.errored")
    step_failures = sum(r["n"] for r in fn if r["step"].endswith("_failed"))
    in_flight = max(taken - completed - cancelled - rejected - errored, 0)

    # Month-over-month: load the PREVIOUS month before writing this one, then persist this snapshot.
    prev_month, prev_snap = _load_prev_snapshot(month)
    snap = {
        "month": month,
        "node_active_devices": node_audience or None,
        "node_mau": node.get("play_mau"),
        "node_dau": node.get("play_dau"),
        "node_rating": node.get("play_rating"),
        "connect_audience_total": connect_total or None,
        "connect_android_devices": connect_android_audience or None,
        "connect_ios_testers": connect_ios_audience or None,
        "connect_ios_new_testers": ios.get("testflight_new_testers_30d"),
        "connect_mau": connect.get("play_mau"),
        "connect_dau": connect.get("play_dau"),
        "connect_rating": connect.get("play_rating"),
        "sideload_easy_mid": _sideload_mid(sideload, "node"),
        "sideload_connect_mid": _sideload_mid(sideload, "connect"),
        "analytics_events_total": gt.total_events,
        "new_optins_total": sum(p.opt_in for p in gt.projects),
        "trades_started": taken,
        "trades_completed": completed,
    }
    # Re-running a month must never degrade its snapshot: keep previously saved values wherever
    # this run came back empty (e.g. Play overlay unreachable), overlay everything non-null.
    existing = _load_snapshot(month)
    if existing:
        snap = {**existing, **{k: v for k, v in snap.items() if v is not None}}
    _save_snapshot(month, snap)

    L.append("## Audience at a glance")
    L.append("")
    L.append(f"- **Bisq Easy (node app):** ~{node_audience:,} active devices (Android only), "
             f"the larger audience — plus an estimated sideload base on top (see below).")
    L.append(f"- **Bisq Connect:** ~{connect_total:,} across platforms "
             f"(~{connect_android_audience:,} Android active devices + ~{connect_ios_audience:,} iOS "
             "TestFlight testers), plus Android sideload.")
    L.append("- Channels are not deduplicable, so these are per-app pictures, not one global total. "
             "Store numbers are real device counts; sideload and analytics are floors.")
    if connect_total and connect_ios_audience and connect_ios_audience / connect_total >= 0.35:
        L.append(f"- **iOS is ~{round(100 * connect_ios_audience / connect_total)}% of the Connect "
                 "audience despite no App Store presence** (TestFlight + AltStore only) — validation "
                 "of the sideload-first iOS strategy.")
    L.append("")

    # Monospace bar charts render in ANY markdown tool (MacDown, GitHub, wikis) with no image
    # hosting — unlike Mermaid, which only renders in Mermaid-aware viewers (GitHub, not MacDown).
    if node_audience and connect_total:
        L += _bar_chart("Audience by app (active devices / TestFlight testers)",
                        [("Bisq Easy (node)", node_audience), ("Bisq Connect", connect_total)])
        L.append("")
    if connect_android_audience and connect_ios_audience:
        L += _bar_chart("Bisq Connect by platform",
                        [("Android (Play active devices)", connect_android_audience),
                         ("iOS (TestFlight testers)", connect_ios_audience)])
        L.append("")

    L += _mom_section(snap, prev_month, prev_snap)
    # Free-form caveats for this month's edition (e.g. one-off methodology notes) — from inputs.json.
    for note in inputs.get("notes", []):
        L.append(f"> ⚠️ {note}")
        L.append("")

    # ---- A. Reach & audience ------------------------------------------------
    L.append("## Reach & audience")
    L.append("")
    L.append("### App stores")
    L.append("")
    L.append("| App | Audience (active devices) | MAU | DAU | Total installs | New installs (28d) | Rating |")
    L.append("|---|---|---|---|---|---|---|")
    for app in ("Bisq Connect (Android)", "Bisq Easy Node (Android)"):
        s = stores.get(app, {})
        L.append(f"| {app} | {_fmt(s.get('play_active_devices_avg'))} | {_fmt(s.get('play_mau'))} | "
                 f"{_fmt(s.get('play_dau'))} | {_fmt(s.get('play_total_installs'))} | "
                 f"{_fmt(s.get('play_new_installs_30d'))} | {_fmt(s.get('play_rating'))} |")
    L.append("")
    L.append("_**Audience** = active devices (28-day average): devices with the app installed and "
             "used in the period — the truest 'how many people use it' number. "
             "**MAU** = monthly active users (unique users active in the last 28 days). "
             "**DAU** = daily active users (average per day over the period)._")
    L.append("")
    if installs_live:
        L.append(f"_Audience (active devices) & new installs are pulled live from the Play Console "
                 f"statistics export ({month} monthly average). MAU/DAU, total installs and rating "
                 "stay manual — Play exposes no API for those._")
        L.append("")
    ios_new = ios.get("testflight_new_testers_30d")
    new_testers_part = f" ({_fmt(ios_new)} new in the window)" if ios_new is not None else ""
    L.append(f"**Bisq Connect (iOS):** TestFlight testers {_fmt(ios.get('testflight_testers'))}"
             f"{new_testers_part}, "
             f"AltStore PAL installs {_fmt(ios.get('altstore_pal_installs'))} "
             "_(iOS active-user metrics aren't exposed like Play's; testers is the closest proxy)._")
    L.append("")

    # ---- Stability (Play-measured) -----------------------------------------
    if any(stores.get(app, {}).get(k) is not None
           for app in ("Bisq Connect (Android)", "Bisq Easy Node (Android)")
           for k in ("play_crash_rate_pct", "play_anr_rate_pct")):
        L.append("### Stability (Play-measured, all installs)")
        L.append("")
        L.append("| App | Crash-free | ANR rate | Rating |")
        L.append("|---|---|---|---|")
        for app in ("Bisq Connect (Android)", "Bisq Easy Node (Android)"):
            s = stores.get(app, {})
            crash = s.get("play_crash_rate_pct")
            cf = f"{100 - crash:.2f}%" if crash is not None else "—"
            anr = f"{s.get('play_anr_rate_pct')}%" if s.get("play_anr_rate_pct") is not None else "—"
            L.append(f"| {app} | {cf} | {anr} | {_fmt(s.get('play_rating'))} |")
        L.append("")
        if vitals_as_of:
            L.append(f"_Crash-free & ANR are pulled live from the Play Developer Reporting API "
                     f"(28-day user-weighted, as of {vitals_as_of}). Apps below Play's minimum-user "
                     "threshold show — (Vitals suppressed)._")
            L.append("")
        nr, cr, ncrash = node.get("play_rating"), connect.get("play_rating"), node.get("play_crash_rate_pct")
        if nr is not None and cr is not None and ncrash is not None and (cr - nr) >= 0.5:
            L.append(f"_The node app's lower rating ({nr}) despite {100 - ncrash:.2f}% crash-free "
                     "isn't a stability story — it points to UX/expectations rather than defects, "
                     "and is the clearest KPI to watch._")
            L.append("")

    L.append("### Sideload (GitHub download stats)")
    L.append("")
    L.append("_Active base ≈ the latest release's per-week APK pull, bot-discounted. Same base "
             "re-downloads each release; cumulative ≠ unique. iOS not measurable (AltStore mirrors "
             "the IPA)._")
    L.append("")
    L.append("| App | Latest | Latest /week | Active sideload base | All-time APK dl |")
    L.append("|---|---|---|---|---|")
    for a in sideload:
        est = a.active_base_estimate
        est_s = f"~{est[0]:,}–{est[1]:,}" if est else "—"
        latest = a.latest.tag if a.latest else "—"
        wk = f"{a.latest.per_week:,}" if a.latest else "—"
        L.append(f"| {a.app} | {latest} | {wk} | {est_s} | {a.total_all_time:,} |")
    L.append("")

    # ---- B. Engagement & health --------------------------------------------
    L.append("## Engagement & product health")
    L.append("")
    L.append("_From the app's own privacy-preserving analytics — **opt-in only** (off by default) "
             "and with no per-device identity, so these are engagement floors, not user counts._")
    L.append("")
    L.append(f"Total analytics events ({window_days}d): **{gt.total_events:,}** across opted-in "
             "installs.")
    L.append("")
    L.append("| App | Events | Errors | New opt-ins | Opt-outs | Dashboard opens |")
    L.append("|---|---|---|---|---|---|")
    for p in gt.projects:
        L.append(f"| {p.project} | {p.events:,} | {p.errors} | {p.opt_in} | {p.opt_out} | "
                 f"{p.dashboard_opens:,} |")
    L.append("")

    # ---- Trade funnel insights (funnel already computed near the top) -------
    L.append("### Trade activity")
    L.append("")
    if taken:
        def pct(n: int) -> str:
            return f"{round(100 * n / taken)}%"
        people = cancelled + rejected
        L.append(f"- **{taken:,} trades started** in the window; **{completed:,} completed** "
                 f"({pct(completed)}).")
        L.append(f"- Of the rest: {cancelled:,} cancelled ({pct(cancelled)}), "
                 f"{rejected:,} rejected by the counterparty ({pct(rejected)}), "
                 f"{in_flight:,} still in progress ({pct(in_flight)}), {errored:,} errored.")
        L.append(f"- Non-completion is **mostly people, not the app**: {people:,} of {taken:,} "
                 f"({pct(people)}) were user cancellations or counterparty rejections — read the "
                 "completion rate primarily as a matching/liquidity signal, though app-caused "
                 "friction can hide inside those cancels (see reasons below).")
        L.append(f"- **App mechanics are healthy: only {step_failures} step action(s) failed.** "
                 "(Completion % also understates the true rate: trades started late in the window "
                 "haven't finished yet.)")
    else:
        L.append("_No trades started in the window._")
    L.append("")

    # ---- Interrupt reasons (reason×stall variants from #1712) ---------------
    # New app versions emit trade.{cancelled,rejected}_<reason>[_<stall>] INSTEAD of the plain
    # event; plain trade.cancelled / trade.rejected therefore = older app versions (no reason data).
    REASON_SLUGS = ["peer_unresponsive", "price_moved", "payment_method_issue", "no_progress",
                    "changed_mind", "other", "unspecified"]
    # Must mirror AnalyticsEvent.Trade.StallBucket slugs.
    STALL_SLUGS = ["unknown", "lt_1h", "1h_24h", "1d_3d", "gt_3d"]

    def _split_reasons(prefix: str) -> tuple[int, dict[str, int], dict[str, int]]:
        """(plain_count, {reason: n}, {stall_bucket: n}) for trade.<prefix>* funnel rows."""
        plain, by_reason, by_stall = 0, {}, {}
        for r in fn:
            step = r["step"]
            if step == prefix:
                plain += r["n"]
                continue
            if not step.startswith(prefix + "_"):
                continue
            rest = step[len(prefix) + 1:]
            for reason in REASON_SLUGS:
                if rest == reason or rest.startswith(reason + "_"):
                    by_reason[reason] = by_reason.get(reason, 0) + r["n"]
                    stall = rest[len(reason) + 1:]
                    if stall in STALL_SLUGS:
                        by_stall[stall] = by_stall.get(stall, 0) + r["n"]
                    break
        return plain, by_reason, by_stall

    plain_c, reasons_c, stalls_c = _split_reasons("trade.cancelled")
    plain_r, reasons_r, _ = _split_reasons("trade.rejected")
    if reasons_c or reasons_r:
        L.append("#### Why trades get interrupted")
        L.append("")
        L.append("_From the optional single-tap reason chips on the cancel/reject dialog (new in "
                 "this cycle). Older app versions report no reason — shown as their own row._")
        L.append("")
        # Per-app attribution: the apps shipped the reason dialog at different times, so coverage
        # is uneven — say where the data actually comes from instead of implying all-apps.
        app_counts: dict[str, int] = {}
        for r in fn_rows:
            if r["step"].startswith("trade.cancelled_") or r["step"].startswith("trade.rejected_"):
                app_counts[r["project"]] = app_counts.get(r["project"], 0) + r["n"]
        if app_counts:
            parts = ", ".join(f"{app} {n:,}" for app, n
                              in sorted(app_counts.items(), key=lambda kv: -kv[1]))
            dominant = max(app_counts, key=lambda k: app_counts[k])
            share = round(100 * app_counts[dominant] / sum(app_counts.values()))
            L.append(f"_App coverage is uneven (the apps shipped the dialog at different times): "
                     f"{parts} reason-tagged interrupts — **{share}% comes from {dominant}**, so "
                     "read this table as that app's data this month._")
            L.append("")
        L.append("| Reason | Cancelled | Rejected | Total |")
        L.append("|---|---|---|---|")
        all_reasons = sorted(set(reasons_c) | set(reasons_r),
                             key=lambda k: -(reasons_c.get(k, 0) + reasons_r.get(k, 0)))
        for k in all_reasons:
            c, rj = reasons_c.get(k, 0), reasons_r.get(k, 0)
            label = "(chips skipped)" if k == "unspecified" else k.replace("_", " ")
            L.append(f"| {label} | {c:,} | {rj:,} | {c + rj:,} |")
        L.append(f"| _no reason data (older app versions)_ | {plain_c:,} | {plain_r:,} | "
                 f"{plain_c + plain_r:,} |")
        L.append("")
        with_reason = sum(reasons_c.values()) + sum(reasons_r.values())
        specified = with_reason - reasons_c.get("unspecified", 0) - reasons_r.get("unspecified", 0)
        if with_reason:
            L.append(f"- Of interrupts on reason-capable versions, **{specified:,} of "
                     f"{with_reason:,} ({round(100 * specified / with_reason)}%) tapped a reason "
                     "chip** (the chips are optional).")
        no_progress = reasons_c.get("no_progress", 0) + reasons_r.get("no_progress", 0)
        if no_progress == 0:
            L.append("- **No `no progress` cancel reasons this month — but that is NOT evidence of "
                     "zero stuck trades.** This dialog only samples users who cancel in-app; stuck "
                     "users more often wait, restart, or reach out to support instead of "
                     "cancelling, analytics is opt-in, and the chips are optional. Read it as a "
                     "floor on self-reported stuck-trade pain, nothing stronger.")
        else:
            L.append(f"- **{no_progress:,} `no progress` interrupt(s)** — the stuck-trade symptom; "
                     "worth triaging against trade-protocol errors above (and a floor: stuck users "
                     "who never cancel in-app aren't counted).")
        long_stalls = stalls_c.get("1h_24h", 0) + stalls_c.get("1d_3d", 0) + stalls_c.get("gt_3d", 0)
        L.append(f"- Cancel timing (time since last witnessed state change): "
                 f"{stalls_c.get('lt_1h', 0):,} under 1h (human decision), {long_stalls:,} after "
                 f"1h+ (desync fingerprint), {stalls_c.get('unknown', 0):,} of unknown age — the "
                 "app restarted since the last transition, which is also **where a long stall "
                 "would hide** (transition times aren't persisted yet, so a restart erases the "
                 "stall clock).")
        L.append("")

    L.append("<details><summary>Full step breakdown</summary>")
    L.append("")
    L.append("| Step | Count |")
    L.append("|---|---|")
    for r in fn:
        L.append(f"| {r['step']} | {r['n']:,} |")
    L.append("")
    L.append("</details>")
    L.append("")

    L.append("### Errors & crashes (from analytics)")
    L.append("")
    if gt.top_errors:
        fatals = sum(r["n"] for r in gt.top_errors if str(r["level"]).lower() == "fatal")
        ios_sigpipe = sum(r["n"] for r in gt.top_errors
                          if "iOS" in r["project"] and "SIGPIPE" in r["title"])
        summary = f"{fatals} fatal event(s) across opted-in installs this month"
        if ios_sigpipe:
            summary += (f"; iOS **SIGPIPE** crashes are the dominant class ({ios_sigpipe}) and the "
                        "top engineering-triage target")
        L.append(summary + ".")
        L.append("")
        L.append("<details><summary>Full error/crash breakdown</summary>")
        L.append("")
        L.append("| App | Level | Title | Count |")
        L.append("|---|---|---|---|")
        for r in gt.top_errors:
            L.append(f"| {r['project']} | {r['level']} | {r['title']} | {r['n']} |")
        L.append("")
        L.append("</details>")
    else:
        L.append("_No error/crash issues in window._")
    L.append("")

    L.append("---")
    L.append("_Caveats: GlitchTip is opt-in (default off) with no per-device identity — a floor, "
             "not a total. GitHub counts every asset GET (bots inflate absolutes). Store numbers are "
             "the authoritative user counts and land here once the Play/ASC APIs are wired._")
    md = "\n".join(L)
    return _wikiify(md, month, heading) if wiki else md


def main() -> None:
    _load_env()
    ap = argparse.ArgumentParser()
    ap.add_argument("--window", type=int, default=30,
                    help="reporting window in days (30 = monthly, 14 = fortnightly)")
    ap.add_argument("--label", help="period label for the header, e.g. '2026-08' or 'Aug 1–14'")
    ap.add_argument("--inputs", default="inputs.json", help="manual store/operator numbers (JSON)")
    ap.add_argument("--out", help="write markdown here instead of stdout")
    ap.add_argument("--wiki", action="store_true",
                    help="wiki-page variant: '## <Month> <Year>' section with demoted headings, "
                         "ready to paste at the top of the year page")
    args = ap.parse_args()

    try:
        with open(args.inputs) as f:
            inputs = json.load(f)
    except FileNotFoundError:
        inputs = {}

    md = render(args.window, inputs, args.label, args.wiki)
    if args.out:
        with open(args.out, "w") as f:
            f.write(md + "\n")
        print(f"wrote {args.out}")
    else:
        print(md)


if __name__ == "__main__":
    main()
