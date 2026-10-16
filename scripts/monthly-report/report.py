#!/usr/bin/env python3
"""Bisq Mobile monthly KPI report generator.

Phase 1 (this file): automates the two halves that need only access we already have —
  - Engagement & health from GlitchTip (opted-in floor; NEVER a user headcount, by privacy design)
  - Sideload base from GitHub download stats
plus a manual-inputs seam (inputs.json) for store/operator numbers until the Play/ASC APIs land.

Output is Markdown to stdout (or --out FILE) for you to review/tweak and paste into the GH wiki.

    python3 report.py --month 2026-08 --inputs inputs.json --out report-2026-08.md

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
import sys
from datetime import date

import glitchtip
import github_downloads

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
HISTORY_DIR = os.path.join(SCRIPT_DIR, "history")
MONTH_RE = re.compile(r"^\d{4}-\d{2}$")
# Below this many started trades an app's per-version split is one device's behaviour, not a trend.
MIN_TRADES_FOR_VERSION_SPLIT = 20


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


def _per_100(n: int, base: int) -> str:
    return f"{100 * n / base:.1f}" if base else "—"


def _short_version(version: str) -> str:
    """'bisq-easy-node@0.13.0' -> '0.13.0'."""
    return version.split("@", 1)[-1]


def _sideload_app(sideload, needle: str):
    for a in sideload:
        if needle.lower() in a.app.lower():
            return a
    return None


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
        ("Bisq Easy — new installs (28d)", "node_new_installs"),
        ("Bisq Easy — uninstalls (28d)", "node_uninstalls"),
        ("Bisq Connect — audience (all platforms)", "connect_audience_total"),
        ("Bisq Connect — Android active devices", "connect_android_devices"),
        ("Bisq Connect — iOS testers", "connect_ios_testers"),
        ("Bisq Connect — new iOS testers (30d)", "connect_ios_new_testers"),
        ("Bisq Connect — MAU", "connect_mau"),
        ("Bisq Connect — rating", "connect_rating"),
        ("Sideload base — Easy (mid)", "sideload_easy_mid"),
        ("Sideload base — Connect (mid)", "sideload_connect_mid"),
        ("Analytics events (opted-in)", "analytics_events_total"),
        ("New opt-ins", "new_optins_total"),
        ("App launches (opted-in)", "app_launches"),
        ("Offerbook opens", "offerbook_opens"),
        ("Take-offer reviews", "take_reviews"),
        ("Trades started", "trades_started"),
        ("Trades completed", "trades_completed"),
        ("Trade interrupts (cancelled + rejected)", "trade_interrupts"),
        ("Out-of-sync detections", "out_of_sync"),
        ("Community hub opens", "community_hub_opens"),
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


def render(window_days: int, inputs: dict, label: str | None = None, wiki: bool = False,
           calendar_month: str | None = None) -> str:
    # `month` keys the history snapshots and the Play bucket lookup, so it must stay YYYY-MM.
    # --month sets it AND pins the analytics window to that exact calendar month. Without it, a
    # YYYY-MM label sets the key only (rolling window); any other label ('Aug 1–14') is
    # display-only and falls back to inputs/today for the key.
    if calendar_month:
        month = calendar_month
    elif label and MONTH_RE.match(label):
        month = label
    else:
        month = inputs.get("month", date.today().strftime("%Y-%m"))
    heading = label or month
    if inputs.get("month") and inputs["month"] != month:
        print(f"report: inputs.json is for {inputs['month']} but the report is for {month} — "
              "manual store numbers may be stale", file=sys.stderr)
    gt = glitchtip.collect(window_days, calendar_month)
    sideload = github_downloads.collect()
    stores = inputs.get("stores", {})
    period = (f"calendar month {month}, UTC" if calendar_month else f"last {window_days} days")
    span = "month" if calendar_month else f"{window_days}d"

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

    # Fill audience / installs / uninstalls from the Play statistics bucket (via gcloud) wherever
    # inputs.json left them empty. A number typed into inputs.json always wins: the export lags,
    # so right after month end the dashboard figure is the more complete one.
    installs_live = False
    export_coverage = None  # (days exported, days in month, last exported day)
    try:
        import play_installs
        for app_label, v in play_installs.collect(month).items():
            s = stores.setdefault(app_label, {})
            export_coverage = (v.get("play_export_days"), v.get("play_month_days"),
                               v.get("play_export_last_day"))
            for k, val in v.items():
                if val is not None and s.get(k) is None:
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
    L.append(f"_Generated {date.today().isoformat()} · window: {period} "
             "— Play metrics are a 28-day average. Sources: **Play / TestFlight** (real audience), "
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
    step_stalls = sum(r["n"] for r in fn if r["step"].endswith("_stalled"))
    out_of_sync = _funnel_count(fn, "trade.out_of_sync_detected")
    address_confirmed = _funnel_count(fn, "trade.btc_address_confirmed")

    # Per-app trade outcomes. `trade.taken` only fires for the taker, so an app whose users are
    # mostly makers shows more completions than starts — "still in progress" is therefore summed
    # per app (floored at zero) instead of netted across apps.
    trade_apps: dict[str, dict[str, int]] = {}
    for r in fn_rows:
        row = trade_apps.setdefault(r["project"], {})
        for key in ("taken", "completed", "cancelled", "rejected", "errored",
                    "out_of_sync_detected"):
            if r["step"].startswith("trade." + key):
                row[key] = row.get(key, 0) + r["n"]
    in_flight = sum(max(a.get("taken", 0) - a.get("completed", 0) - a.get("cancelled", 0)
                        - a.get("rejected", 0) - a.get("errored", 0), 0)
                    for a in trade_apps.values())

    # Every non-error event, total and per app — screens, community, contacts, settings.
    ev: dict[str, int] = {}
    ev_app: dict[str, dict[str, int]] = {}
    for r in gt.event_counts:
        ev[r["title"]] = ev.get(r["title"], 0) + r["n"]
        ev_app.setdefault(r["project"], {})[r["title"]] = r["n"]
    # The splash screen shows once per app launch, so it is the one screen count that navigation
    # (Back, tab switches) cannot inflate — the denominator for every "per 100" rate below.
    app_launches = ev.get("screen.splash_opened", 0)
    easy_sideload = _sideload_app(sideload, "node")
    connect_sideload = _sideload_app(sideload, "connect")

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
        "trade_interrupts": cancelled + rejected,
        "out_of_sync": out_of_sync or None,
        "address_confirmed": address_confirmed or None,
        "app_launches": app_launches or None,
        "offerbook_opens": ev.get("screen.offerbook_market_opened"),
        "take_reviews": ev.get("screen.take_offer_review_opened"),
        "community_hub_opens": ev.get("screen.community_hub_opened"),
        "node_new_installs": node.get("play_new_installs_30d"),
        "node_uninstalls": node.get("play_uninstalls_28d"),
        "connect_new_installs": connect.get("play_new_installs_30d"),
        "connect_uninstalls": connect.get("play_uninstalls_28d"),
        "sideload_easy_all_time": easy_sideload.total_all_time if easy_sideload else None,
        "sideload_connect_all_time": connect_sideload.total_all_time if connect_sideload else None,
        "generated": date.today().isoformat(),
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
    L.append("| App | Audience (active devices) | MAU | DAU | Total installs | New installs (28d) "
             "| Uninstalls (28d) | Rating |")
    L.append("|---|---|---|---|---|---|---|---|")
    for app in ("Bisq Connect (Android)", "Bisq Easy Node (Android)"):
        s = stores.get(app, {})
        L.append(f"| {app} | {_fmt(s.get('play_active_devices_avg'))} | {_fmt(s.get('play_mau'))} | "
                 f"{_fmt(s.get('play_dau'))} | {_fmt(s.get('play_total_installs'))} | "
                 f"{_fmt(s.get('play_new_installs_30d'))} | {_fmt(s.get('play_uninstalls_28d'))} | "
                 f"{_fmt(s.get('play_rating'))} |")
    L.append("")
    churn = []
    for name, s in (("Bisq Easy", node), ("Bisq Connect (Android)", connect)):
        ins, unins = s.get("play_new_installs_30d"), s.get("play_uninstalls_28d")
        if ins and unins is not None:
            churn.append(f"{name} {round(100 * unins / ins)}")
    if churn:
        L.append(f"Churn — uninstalls per 100 new installs: {', '.join(churn)}. Growth is net of "
                 "this turnover, so retention matters as much as acquisition.")
        L.append("")
    L.append("_**Audience** = active devices (28-day average): devices with the app installed and "
             "used in the period — the truest 'how many people use it' number. "
             "**MAU** = monthly active users (unique users active in the last 28 days). "
             "**DAU** = daily active users (average per day over the period)._")
    L.append("")
    if installs_live:
        coverage = ""
        if export_coverage and export_coverage[0] and export_coverage[0] < export_coverage[1]:
            coverage = (f" The export lags: it currently covers {export_coverage[0]} of "
                        f"{export_coverage[1]} days (to {export_coverage[2]}), so these are "
                        "estimates from that part of the month.")
        L.append("_New installs and uninstalls are daily averages × 28 from the Play Console "
                 f"statistics export for {month}; audience comes from the same export unless it "
                 f"was entered by hand from the dashboard.{coverage} MAU/DAU, total installs and "
                 "rating stay manual — Play exposes no API for those._")
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
    L.append("_Active base ≈ a release's per-week APK pull while it is the latest, bot-discounted. "
             "Same base re-downloads each release; cumulative ≠ unique. iOS not measurable "
             "(AltStore mirrors the IPA)._")
    L.append("")
    prev_day = None
    try:
        prev_day = date.fromisoformat((prev_snap or {}).get("generated", ""))
    except ValueError:
        pass
    since_days = (date.today() - prev_day).days if prev_day else None
    since_hdr = f"Downloads since last report ({since_days}d)" if since_days else "Downloads since last report"
    L.append(f"| App | Latest | Latest /week | Active sideload base | {since_hdr} | All-time APK dl |")
    L.append("|---|---|---|---|---|---|")
    fallbacks = []
    for a, key in ((easy_sideload, "sideload_easy_all_time"),
                   (connect_sideload, "sideload_connect_all_time")):
        if not a:
            continue
        est = a.active_base_estimate
        est_s = f"~{est[0]:,}–{est[1]:,}" if est else "—"
        latest = a.latest.tag if a.latest else "—"
        wk = f"{a.latest.per_week:,}" if a.latest else "—"
        prev_total = (prev_snap or {}).get(key)
        since = f"{a.total_all_time - prev_total:,}" if prev_total is not None else "—"
        L.append(f"| {a.app} | {latest} | {wk} | {est_s} | {since} | {a.total_all_time:,} |")
        if a.latest and a.estimate_release and a.estimate_release is not a.latest:
            fallbacks.append(f"Only {a.latest.apk_downloads:,} APK downloads so far for "
                             f"{a.latest.tag}, so the {a.app} base is estimated from "
                             f"{a.estimate_release.tag} instead")
    L.append("")
    for note in fallbacks:
        L.append(f"_{note}. A release that never becomes the repository's "
                 "\"latest release\" (another app's release was published right after it) may be "
                 "missed by automated update followers — which would also mean a large share of "
                 "the usual download count is automation, not people._")
        L.append("")

    # ---- B. Engagement & health --------------------------------------------
    L.append("## Engagement & product health")
    L.append("")
    L.append("_From the app's own privacy-preserving analytics — **opt-in only** (off by default) "
             "and with no per-device identity, so these are engagement floors, not user counts._")
    L.append("")
    L.append(f"Total analytics events ({span}): **{gt.total_events:,}** across opted-in "
             "installs.")
    L.append("")
    L.append("| App | Events | Errors | New opt-ins | Opt-outs | Dashboard opens |")
    L.append("|---|---|---|---|---|---|")
    for p in gt.projects:
        L.append(f"| {p.project} | {p.events:,} | {p.errors} | {p.opt_in} | {p.opt_out} | "
                 f"{p.dashboard_opens:,} |")
    L.append("")

    # ---- Path to a trade: the screens before `trade.taken` --------------------
    L.append("### Path to a trade")
    L.append("")
    L.append("_Screen counts are views, not visits: a screen is counted again every time it is "
             "re-entered (Back, edit), so compare each step against app launches or month over "
             "month — not against the step before it._")
    L.append("")
    path_rows = [
        ("Offerbook opened", "screen.offerbook_market_opened"),
        ("Take offer — amount", "screen.take_offer_amount_opened"),
        ("Take offer — payment method", "screen.take_offer_payment_method_opened"),
        ("Take offer — payout address (first-time buyers only)",
         "screen.take_offer_btc_address_opened"),
        ("Take offer — review", "screen.take_offer_review_opened"),
        ("**Trades started**", "trade.taken"),
        ("Create offer — started", "screen.create_offer_direction_opened"),
        ("Create offer — review", "screen.create_offer_review_opened"),
    ]
    L.append("| Step | Count | Per 100 app launches |")
    L.append("|---|---|---|")
    for lbl, title in path_rows:
        n = ev.get(title, 0)
        L.append(f"| {lbl} | {n:,} | {_per_100(n, app_launches)} |")
    L.append("")
    L.append("_The create-offer path ends at its review screen: there is no event yet for an offer "
             "actually being published._")
    L.append("")

    if gt.weekly:
        weeks: dict[str, dict[str, int]] = {}
        for r in gt.weekly:
            weeks.setdefault(r["week"], {})[r["title"]] = r["n"]
        L.append("#### Week by week")
        L.append("")
        L.append("| Week of | App launches | Take-offer reviews | Trades started "
                 "| Started per 100 app launches | Completed |")
        L.append("|---|---|---|---|---|---|")
        for wk in sorted(weeks):
            w = weeks[wk]
            launches, started = w.get("screen.splash_opened", 0), w.get("trade.taken", 0)
            L.append(f"| {wk} | {launches:,} | {w.get('screen.take_offer_review_opened', 0):,} | "
                     f"{started:,} | {_per_100(started, launches)} | {w.get('trade.completed', 0):,} |")
        L.append("")
        L.append("_Weeks run Monday–Sunday; the first and last can be partial, which is why the "
                 "per-100 rate is the column to read._")
        L.append("")

    # Versions carrying at least 5% of their app's events — adoption plus the trade signals that
    # tell a release-specific change apart from a market-wide one. Apps with too few trades to
    # split are skipped: one device would decide every row.
    app_events: dict[str, int] = {}
    for r in gt.versions:
        app_events[r["project"]] = app_events.get(r["project"], 0) + r["events"]
    version_rows = [r for r in gt.versions
                    if trade_apps.get(r["project"], {}).get("taken", 0) >= MIN_TRADES_FOR_VERSION_SPLIT
                    and r["events"] / app_events[r["project"]] >= 0.05]
    if version_rows:
        L.append("#### By app version")
        L.append("")
        L.append("| App | Version | Share of app's events | Trades started "
                 "| Started per 100 app launches | Payout address confirmed | Out-of-sync |")
        L.append("|---|---|---|---|---|---|---|")
        for r in version_rows:
            share = round(100 * r["events"] / app_events[r["project"]])
            # Makers confirm an address without a recorded start, so the share only makes sense
            # while confirmations don't exceed starts.
            addr = (f"{r['address_confirmed']:,} ({round(100 * r['address_confirmed'] / r['taken'])}%)"
                    if r["taken"] >= r["address_confirmed"] > 0 else f"{r['address_confirmed']:,}")
            L.append(f"| {r['project']} | {_short_version(r['version'])} | {share}% | "
                     f"{r['taken']:,} | {_per_100(r['taken'], r['app_launches'])} | {addr} | "
                     f"{r['out_of_sync']:,} |")
        L.append("")
        L.append("_Only apps with enough trades to split, and versions with at least 5% of that "
                 "app's events. Payout address confirmed is shown against trades started on that "
                 "version. Versions overlap in time with everything else that changed during the "
                 "month, and small counts swing widely — treat differences as hints, not results._")
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
        L.append(f"- **Step actions: {step_failures} failed, {step_stalls} stalled** (stalled = "
                 "the tap was accepted but the trade didn't advance within 45s). Completion % also "
                 "understates the true rate: trades started late in the window haven't finished "
                 "yet.")
        if address_confirmed:
            prev_addr, prev_taken = (prev_snap or {}).get("address_confirmed"), (prev_snap or {}).get("trades_started")
            vs = (f" (last month: {round(100 * prev_addr / prev_taken)}%)"
                  if prev_addr and prev_taken else "")
            L.append(f"- **Payout address confirmed after starting: {address_confirmed:,} of "
                     f"{taken:,} ({pct(address_confirmed)})**{vs} — the first big drop-off after a "
                     "trade starts.")
        if out_of_sync:
            L.append(f"- **{out_of_sync:,} out-of-sync detections** — a trade still in its initial "
                     "state more than 10 minutes after being taken. Counted once per trade per app "
                     "session, so one stuck trade is counted again on every restart: read it as how "
                     "often users face a stuck trade, not as a number of trades.")
    else:
        L.append("_No trades started in the window._")
    L.append("")

    if trade_apps:
        L.append("| App | Started | Completed | Cancelled | Rejected | Errored | Out-of-sync |")
        L.append("|---|---|---|---|---|---|---|")
        for app, a in sorted(trade_apps.items(), key=lambda kv: -sum(kv[1].values())):
            L.append(f"| {app} | {a.get('taken', 0):,} | {a.get('completed', 0):,} | "
                     f"{a.get('cancelled', 0):,} | {a.get('rejected', 0):,} | "
                     f"{a.get('errored', 0):,} | {a.get('out_of_sync_detected', 0):,} |")
        L.append("")
        if any(a.get("completed", 0) > a.get("taken", 0) for a in trade_apps.values()):
            L.append("_\"Started\" is only recorded for the user who takes an offer. Trades where "
                     "the user was the maker complete (or error) without a matching start, so an "
                     "app used mostly by makers shows more completions than starts and the overall "
                     "completion % is slightly flattered._")
            L.append("")

    # ---- Interrupt reasons (reason×stall variants from #1712) ---------------
    # New app versions emit trade.{cancelled,rejected}_<reason>[_<stall>] INSTEAD of the plain
    # event; plain trade.cancelled / trade.rejected therefore = older app versions (no reason data).
    REASON_SLUGS = ["peer_unresponsive", "price_moved", "payment_method_issue", "no_progress",
                    "too_complex", "changed_mind", "other", "unspecified", "banned_account_data"]
    # Not chips: skipped chips, and the automatic cancel on banned seller account data.
    REASON_LABELS = {"unspecified": "(chips skipped)",
                     "banned_account_data": "banned account data (automatic)"}
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
                    # The automatic cancel always reports "unknown"; it says nothing about stalls.
                    if stall in STALL_SLUGS and reason != "banned_account_data":
                        by_stall[stall] = by_stall.get(stall, 0) + r["n"]
                    break
        return plain, by_reason, by_stall

    plain_c, reasons_c, stalls_c = _split_reasons("trade.cancelled")
    plain_r, reasons_r, _ = _split_reasons("trade.rejected")
    if reasons_c or reasons_r:
        L.append("#### Why trades get interrupted")
        L.append("")
        L.append("_From the optional single-tap reason chips on the cancel/reject dialog. Older "
                 "app versions report no reason — shown as their own row._")
        L.append("")
        # Per-app attribution: the apps shipped the reason dialog at different times, so coverage
        # is uneven — say where the data actually comes from instead of implying all-apps.
        app_counts: dict[str, int] = {}
        for r in fn_rows:
            step = r["step"]
            # The automatic cancel is not a chip answer, so it does not count toward the share.
            if step.startswith(("trade.cancelled_banned_account_data", "trade.rejected_banned_account_data")):
                continue
            if step.startswith("trade.cancelled_") or step.startswith("trade.rejected_"):
                app_counts[r["project"]] = app_counts.get(r["project"], 0) + r["n"]
        if app_counts:
            parts = ", ".join(f"{app} {n:,}" for app, n
                              in sorted(app_counts.items(), key=lambda kv: -kv[1]))
            dominant = max(app_counts, key=lambda k: app_counts[k])
            share = round(100 * app_counts[dominant] / sum(app_counts.values()))
            L.append(f"_By app: {parts} reason-tagged interrupts — **{share}% comes from "
                     f"{dominant}**, so read this table as that app's data this month._")
            L.append("")
        L.append("| Reason | Cancelled | Rejected | Total |")
        L.append("|---|---|---|---|")
        all_reasons = sorted(set(reasons_c) | set(reasons_r),
                             key=lambda k: -(reasons_c.get(k, 0) + reasons_r.get(k, 0)))
        for k in all_reasons:
            c, rj = reasons_c.get(k, 0), reasons_r.get(k, 0)
            label = REASON_LABELS.get(k, k.replace("_", " "))
            L.append(f"| {label} | {c:,} | {rj:,} | {c + rj:,} |")
        L.append(f"| _no reason data (older app versions)_ | {plain_c:,} | {plain_r:,} | "
                 f"{plain_c + plain_r:,} |")
        L.append("")
        automatic = reasons_c.get("banned_account_data", 0) + reasons_r.get("banned_account_data", 0)
        with_reason = sum(reasons_c.values()) + sum(reasons_r.values()) - automatic
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
                 f"1h+ (desync fingerprint), {stalls_c.get('unknown', 0):,} of unknown age (no "
                 "state change of that trade was witnessed by the app, e.g. it all happened while "
                 "the app was closed or on a version that didn't keep the clock yet).")
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

    # ---- Community & contacts --------------------------------------------------
    community = [
        ("Hub", "screen.community_hub_opened"),
        ("Discussions", "screen.community_discussions_opened"),
        ("Private messages", "screen.community_messages_opened"),
        ("Contacts", "screen.community_contacts_opened"),
        ("Support", "screen.community_support_opened"),
    ]
    if any(ev.get(title) for _, title in community):
        L.append("### Community & contacts")
        L.append("")
        L.append("| App | " + " | ".join(lbl for lbl, _ in community) + " |")
        L.append("|---|" + "---|" * len(community))
        for p in gt.projects:
            counts = ev_app.get(p.project, {})
            L.append(f"| {p.project} | "
                     + " | ".join(f"{counts.get(title, 0):,}" for _, title in community) + " |")
        L.append("")
        hub = ev.get("screen.community_hub_opened", 0)
        L.append(f"- The Community hub was opened {hub:,} times — "
                 f"{_per_100(hub, app_launches)} per 100 app launches (screen views, not "
                 "people).")
        edits = sum(n for title, n in ev.items() if title.startswith("contact.details_edited"))
        failed = sum(n for title, n in ev.items() if title.startswith("contact.action_failed"))
        L.append(f"- Contacts: {ev.get('contact.added', 0):,} added and "
                 f"{ev.get('contact.removed', 0):,} removed by hand, {edits:,} detail edits "
                 f"(tag / notes / trust score), {failed:,} failed actions. Contacts the app adds "
                 "automatically after a trade or chat are deliberately not counted, so removals "
                 "can include those.")
        L.append("")

    # ---- Language & notifications (settings baseline) ---------------------------
    lang_prefix = "settings.language_changed_"
    langs = sorted(((title[len(lang_prefix):], n) for title, n in ev.items()
                    if title.startswith(lang_prefix)), key=lambda kv: -kv[1])
    # Only Connect has a push service; the node app always reports "disabled".
    push_apps = [p.project for p in gt.projects if "connect" in p.project.lower()]
    push_rows = [(app, ev_app.get(app, {}).get("settings.push_notifications_enabled", 0),
                  ev_app.get(app, {}).get("settings.push_notifications_disabled", 0))
                 for app in push_apps]
    push_rows = [r for r in push_rows if r[1] or r[2]]
    if langs or push_rows:
        L.append("### Language & notifications")
        L.append("")
    if langs:
        lang_total = sum(n for _, n in langs)
        L.append("_App language, reported when a user opts in, on app launch and on change — so "
                 "it is weighted by how often people open the app, not a user count._")
        L.append("")
        L.append("| Language | Signals | Share |")
        L.append("|---|---|---|")
        shown = langs[:8]
        for code, n in shown:
            L.append(f"| {code.replace('_', '-')} | {n:,} | {round(100 * n / lang_total)}% |")
        rest = sum(n for _, n in langs[8:])
        if rest:
            L.append(f"| other ({len(langs) - 8}) | {rest:,} | {round(100 * rest / lang_total)}% |")
        L.append("")
    if push_rows:
        L.append("| App | Push notifications on | Push notifications off |")
        L.append("|---|---|---|")
        for app, on, off in push_rows:
            L.append(f"| {app} | {on:,} | {off:,} |")
        L.append("")
        L.append("_Push state is reported once when a user opts in to analytics and again on "
                 "every toggle, so it approximates the split among newly opted-in installs._")
        L.append("")

    L.append("### Errors & crashes (from analytics)")
    L.append("")
    if gt.top_errors:
        fatals = [r for r in gt.top_errors if str(r["level"]).lower() == "fatal"]
        total_errors = sum(p.errors for p in gt.projects)
        summary = (f"{total_errors:,} error event(s) across opted-in installs this {span}, "
                   f"{sum(r['n'] for r in fatals):,} of them fatal")
        # The same failure surfaces under a different exception class per app, so group by the
        # message after the class name to find what actually recurs.
        by_message: dict[str, int] = {}
        for r in gt.top_errors:
            msg = r["title"].split(": ", 1)[-1]
            by_message[msg] = by_message.get(msg, 0) + r["n"]
        top_msg = max(by_message, key=lambda k: by_message[k])
        if by_message[top_msg] >= 3:
            summary += f"; most frequent: **{top_msg.rstrip('.')}** ({by_message[top_msg]})"
        L.append(summary + ".")
        clean = [p.project for p in gt.projects if not p.errors]
        if clean:
            L.append("")
            L.append(f"No error or crash events from: {', '.join(clean)}.")
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
    L.append("_Caveats: the analytics are opt-in (default off) with no per-device identity — a "
             "floor, not a total. GitHub counts every asset GET (bots inflate absolutes). Store "
             "numbers are the authoritative user counts._")
    md = "\n".join(L)
    return _wikiify(md, month, heading) if wiki else md


def main() -> None:
    _load_env()
    ap = argparse.ArgumentParser()
    ap.add_argument("--month", help="report on exactly this calendar month (YYYY-MM, UTC) — the "
                                    "usual monthly run; overrides --window")
    ap.add_argument("--window", type=int, default=30,
                    help="rolling window in days back from now, for ad-hoc readouts "
                         "(30 = monthly, 14 = fortnightly)")
    ap.add_argument("--label", help="period label for the header, e.g. '2026-08' or 'Aug 1–14'")
    ap.add_argument("--inputs", default="inputs.json", help="manual store/operator numbers (JSON)")
    ap.add_argument("--out", help="write markdown here instead of stdout")
    ap.add_argument("--wiki", action="store_true",
                    help="wiki-page variant: '## <Month> <Year>' section with demoted headings, "
                         "ready to paste at the top of the year page")
    args = ap.parse_args()
    if args.month and not MONTH_RE.match(args.month):
        ap.error("--month must be YYYY-MM")

    try:
        with open(args.inputs) as f:
            inputs = json.load(f)
    except FileNotFoundError:
        inputs = {}

    md = render(args.window, inputs, args.label or args.month, args.wiki, args.month)
    if args.out:
        with open(args.out, "w") as f:
            f.write(md + "\n")
        print(f"wrote {args.out}")
    else:
        print(md)


if __name__ == "__main__":
    main()
