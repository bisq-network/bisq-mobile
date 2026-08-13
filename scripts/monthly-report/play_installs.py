"""Play Console statistics bucket — live audience (active devices) + new installs per Android app.

Reads the monthly install "overview" CSVs Play exports to its Cloud Storage bucket:
    gs://<bucket>/stats/installs/installs_<package>_<YYYYMM>_overview.csv   (UTF-16, one row per day)

We take, over the report month:
    Active Device Installs -> audience / active devices (averaged across the month's days)
    Daily User Installs    -> new installs (summed)

NOT available from the bucket (stay manual in inputs.json): DAU/MAU (dashboard-only, never exported),
lifetime "Total installs", and rating (a separate ratings report).

AUTH: these pubsite_prod_* buckets are ACL-based and can't be shared with a service account, so this
reads via the `gcloud` CLI using your own credentials (the Play account Google authorized). Requires
`gcloud` installed and authed as the Play owner, plus BISQ_REPORT_PLAY_STATS_BUCKET set. If any of
that is missing, collect() returns {} and the report falls back to manual inputs — it never breaks.
"""
from __future__ import annotations

import gzip
import os
import subprocess
import sys

APPS = {
    "Bisq Connect (Android)": "network.bisq.mobile.client",
    "Bisq Easy Node (Android)": "network.bisq.mobile.node",
}


def _bucket() -> str:
    # Read at call time so report.py's .env loader applies regardless of import order.
    return os.environ.get("BISQ_REPORT_PLAY_STATS_BUCKET", "").replace("gs://", "").strip("/")


def _int(s: str):
    try:
        return int(s.strip())
    except (ValueError, AttributeError):
        return None


def _overview(pkg: str, yyyymm: str):
    """(column-index map, list-of-cell-rows) for one app-month, or raises if unreadable."""
    obj = f"gs://{_bucket()}/stats/installs/installs_{pkg}_{yyyymm}_overview.csv"
    r = subprocess.run(["gcloud", "storage", "cat", obj], capture_output=True, timeout=120)
    if r.returncode != 0:
        raise RuntimeError(r.stderr.decode("utf-8", "replace")[:200])
    raw = r.stdout
    if raw[:2] == b"\x1f\x8b":  # `gcloud storage cat` returns these objects gzip-compressed
        raw = gzip.decompress(raw)
    lines = [ln for ln in raw.decode("utf-16").splitlines() if ln.strip()]
    header = [h.strip() for h in lines[0].split(",")]
    idx = {name: i for i, name in enumerate(header)}
    rows = [ln.split(",") for ln in lines[1:] if len(ln.split(",")) >= len(header)]
    return idx, rows


def collect(month: str) -> dict:
    """{app_label: {play_active_devices_avg, play_new_installs_30d}} for readable apps; {} on failure.

    `month` is the report label 'YYYY-MM'; we read that calendar month's install overview.
    """
    if not _bucket():
        return {}
    yyyymm = month.replace("-", "")
    out: dict[str, dict] = {}
    for label, pkg in APPS.items():
        try:
            idx, rows = _overview(pkg, yyyymm)
            ad_i, ni_i = idx["Active Device Installs"], idx["Daily User Installs"]
        except Exception as e:
            # Diagnostics to stderr only — stdout is the report's Markdown.
            print(f"play_installs: skipping {label} ({yyyymm}): {e}", file=sys.stderr)
            continue
        active = [v for v in (_int(r[ad_i]) for r in rows) if v is not None]
        installs = [v for v in (_int(r[ni_i]) for r in rows) if v is not None]
        if not active and not installs:
            continue
        vals: dict = {}
        if active:
            vals["play_active_devices_avg"] = round(sum(active) / len(active))
        if installs:
            vals["play_new_installs_30d"] = sum(installs)
        out[label] = vals
    return out


if __name__ == "__main__":
    import sys
    m = sys.argv[1] if len(sys.argv) > 1 else "2026-07"
    data = collect(m)
    if not data:
        print(f"Play stats: no data for {m} (bucket unset, gcloud not authed, or month missing).")
    for label, v in data.items():
        print(f"{label} [{m}]: active devices avg {v.get('play_active_devices_avg')}, "
              f"new installs {v.get('play_new_installs_30d')}")
