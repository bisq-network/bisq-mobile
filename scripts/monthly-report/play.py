"""Google Play Developer Reporting API — live app Vitals (crash + ANR) for the monthly report.

Pulls the rolling 28-day user-weighted crash rate and ANR rate for each Android app, i.e. the same
core-vitals figures shown in the Play Console. This is the ONLY store metric a Google API exposes:
audience / installs / DAU / MAU are NOT available here (they live in the Play Console statistics
GCS bucket) and stay manual in inputs.json.

Auth: a service-account key granted read-only access in Play Console (Users & permissions). Path is
read from BISQ_REPORT_PLAY_KEY, else ../../credentials/bisq-mobile-reporting.json (gitignored). If the
key is missing or access is denied, collect() returns {} so the report cleanly falls back to manual
inputs — this module augments the report, it must never break it.

Requires the google-auth + requests packages (see requirements.txt); run report.py under a venv that
has them to activate live Vitals. Plain stdlib python3 still runs the report, just without this half.
"""
from __future__ import annotations

import os
import time
from datetime import date, timedelta

from google.oauth2 import service_account
from google.auth.transport.requests import AuthorizedSession

# Google occasionally returns transient 5xx/429 on these endpoints — retry with backoff.
_TRANSIENT = {429, 500, 502, 503, 504}

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
KEY_PATH = os.environ.get(
    "BISQ_REPORT_PLAY_KEY",
    os.path.join(SCRIPT_DIR, "..", "..", "credentials", "bisq-mobile-reporting.json"),
)
SCOPE = "https://www.googleapis.com/auth/playdeveloperreporting"
BASE = "https://playdeveloperreporting.googleapis.com/v1beta1"

# Play applicationId per app (must match the store listing).
APPS = {
    "Bisq Connect (Android)": "network.bisq.mobile.client",
    "Bisq Easy Node (Android)": "network.bisq.mobile.node",
}

# metric_set -> the rolling-28d user-weighted rate metric (value is a [0,1] fraction).
METRICS = {
    "crash_rate_pct": ("crashRateMetricSet", "crashRate28dUserWeighted"),
    "anr_rate_pct": ("anrRateMetricSet", "anrRate28dUserWeighted"),
}


def _session() -> AuthorizedSession:
    creds = service_account.Credentials.from_service_account_file(KEY_PATH, scopes=[SCOPE])
    return AuthorizedSession(creds)


def _request(sess: AuthorizedSession, method: str, url: str, *, attempts: int = 4, **kw):
    """GET/POST with backoff on transient 5xx/429; raises on the last failure or a non-transient one."""
    for i in range(attempts):
        r = sess.request(method, url, timeout=45, **kw)
        if r.status_code in _TRANSIENT and i < attempts - 1:
            time.sleep(2 ** i)  # 1s, 2s, 4s
            continue
        r.raise_for_status()
        return r
    raise RuntimeError("unreachable")


def _latest_daily_end(sess: AuthorizedSession, pkg: str, metric_set: str) -> dict | None:
    """The most recent DAILY end date the API has data for, as a google.type.DateTime dict."""
    r = _request(sess, "GET", f"{BASE}/apps/{pkg}/{metric_set}")
    for f in r.json().get("freshnessInfo", {}).get("freshnesses", []):
        if f.get("aggregationPeriod") == "DAILY":
            return f.get("latestEndTime")
    return None


def _latest_rate(sess: AuthorizedSession, pkg: str, metric_set: str, metric: str):
    """Return (value_fraction, day) for the most recent available row, or (None, None)."""
    end = _latest_daily_end(sess, pkg, metric_set)
    if not end:
        return None, None
    tz = end.get("timeZone", {"id": "America/Los_Angeles"})
    end_d = date(end["year"], end.get("month", 1), end.get("day", 1))
    start_d = end_d - timedelta(days=5)
    body = {
        "timelineSpec": {
            "aggregationPeriod": "DAILY",
            "startTime": {"year": start_d.year, "month": start_d.month, "day": start_d.day, "timeZone": tz},
            "endTime": {"year": end_d.year, "month": end_d.month, "day": end_d.day, "timeZone": tz},
        },
        "metrics": [metric],
        "pageSize": 100,
    }
    r = _request(sess, "POST", f"{BASE}/apps/{pkg}/{metric_set}:query", json=body)
    best_val, best_day = None, None
    for row in r.json().get("rows", []):
        st = row.get("startTime", {})
        d = date(st.get("year", 1), st.get("month", 1), st.get("day", 1))
        for m in row.get("metrics", []):
            if m.get("metric") == metric and "decimalValue" in m:
                if best_day is None or d > best_day:
                    best_val, best_day = float(m["decimalValue"]["value"]), d
    return best_val, best_day


def _safe(sess, pkg, metric_set, metric):
    try:
        return _latest_rate(sess, pkg, metric_set, metric)
    except Exception:
        return None, None


def collect() -> dict:
    """{app_label: {"crash_rate_pct", "anr_rate_pct", "as_of"}} for readable apps; {} on any failure."""
    if not os.path.exists(KEY_PATH):
        return {}
    try:
        sess = _session()
    except Exception:
        return {}

    out: dict[str, dict] = {}
    for label, pkg in APPS.items():
        vals, days = {}, []
        for field, (metric_set, metric) in METRICS.items():
            v, d = _safe(sess, pkg, metric_set, metric)
            vals[field] = round(v * 100, 3) if v is not None else None
            if d:
                days.append(d)
        if any(v is not None for v in vals.values()):
            vals["as_of"] = max(days).isoformat() if days else None
            out[label] = vals
    return out


if __name__ == "__main__":
    data = collect()
    if not data:
        print("Play Vitals: no data (key missing, access denied, or API unreachable).")
    for label, v in data.items():
        print(f"{label}: crash {v.get('crash_rate_pct')}% · ANR {v.get('anr_rate_pct')}% "
              f"(28d user-weighted, as of {v.get('as_of')})")
