"""GlitchTip (self-hosted, Tor-only) data source for the monthly report.

Reads the analytics Postgres over SSH — the same read-only path used for manual readouts:
    ssh $BISQ_REPORT_SSH_HOST -> docker exec $BISQ_REPORT_PG_CONTAINER psql

HARD PRIVACY LIMIT (by design, not a bug): the opt-in analytics contract nulls event.user and
disables session tracking, so GlitchTip has NO per-device identity. Everything here is EVENT
VOLUME and a version FLOOR for the opted-in subset — never a user headcount. User counts come
from the stores (Play/ASC), never from here. See docs and the project memory for context.

Connection is configured via environment (this is a PUBLIC repo — no infra names are hardcoded):
    BISQ_REPORT_SSH_HOST      SSH host/alias for the GlitchTip host (required)
    BISQ_REPORT_PG_CONTAINER  Postgres container name (default: glitchtip's stock compose name)

Projects: 2=Bisq Easy Node (Android), 3=Bisq Connect (Android), 4=Bisq Connect (iOS).
"""
from __future__ import annotations

import json
import os
import shlex
import subprocess
from dataclasses import dataclass, field


def _psql(sql: str) -> list[dict]:
    """Run a query over SSH and return rows as dicts (JSON via psql's row_to_json).

    Host/container are read from env at call time (so report.py's .env loader applies regardless of
    import order); no deployment-specific host lives in this public repo."""
    ssh_host = os.environ.get("BISQ_REPORT_SSH_HOST", "")
    pg_container = os.environ.get("BISQ_REPORT_PG_CONTAINER", "glitchtip-postgres-1")
    if not ssh_host:
        raise RuntimeError(
            "BISQ_REPORT_SSH_HOST is not set — export it (or add it to .env) as your GlitchTip "
            "host/alias, e.g.\n    export BISQ_REPORT_SSH_HOST=your-host-alias")
    wrapped = f"SELECT coalesce(json_agg(t), '[]') FROM ({sql}) t;"
    # shlex.quote so the remote shell treats container name and SQL as literals.
    cmd = [
        "ssh", "-o", "ConnectTimeout=15", "-o", "BatchMode=yes", ssh_host,
        f"docker exec -i {shlex.quote(pg_container)} psql -U postgres -d postgres -t -A "
        f"-c {shlex.quote(wrapped)}",
    ]
    out = subprocess.run(cmd, capture_output=True, text=True, timeout=90)
    if out.returncode != 0:
        raise RuntimeError(f"psql failed: {out.stderr.strip() or out.stdout.strip()}")
    return json.loads(out.stdout.strip() or "[]")


@dataclass
class ProjectEngagement:
    project: str
    events: int
    errors: int
    opt_in: int
    opt_out: int
    os_versions: int
    trade_taken: int
    trade_completed: int
    dashboard_opens: int

    @property
    def trade_success_pct(self) -> float | None:
        return round(100 * self.trade_completed / self.trade_taken, 1) if self.trade_taken else None


@dataclass
class GlitchTipSnapshot:
    window_days: int
    projects: list[ProjectEngagement] = field(default_factory=list)
    top_errors: list[dict] = field(default_factory=list)
    trade_funnel: list[dict] = field(default_factory=list)
    # Every non-error event name per app: {project, title, n}. Feeds the screen funnel, community
    # and settings sections without one bespoke query per section.
    event_counts: list[dict] = field(default_factory=list)
    # Key funnel events per ISO week: {week, title, n} — shows the trend inside the window.
    weekly: list[dict] = field(default_factory=list)
    # Key events per app version: {project, version, events, app_launches, taken, ...}.
    versions: list[dict] = field(default_factory=list)

    @property
    def total_events(self) -> int:
        return sum(p.events for p in self.projects)


# Events whose week-by-week trend the report shows.
WEEKLY_TITLES = ("screen.splash_opened", "screen.take_offer_review_opened", "trade.taken",
                 "trade.completed")


def _window_sql(window_days: int, month: str | None) -> str:
    """Rolling `window_days` back from now, or — when `month` ('YYYY-MM') is given — exactly that
    calendar month in UTC, so the result no longer depends on the day the report is generated."""
    if not month:
        return f"e.timestamp >= now() - interval '{window_days} days'"
    y, m = (int(x) for x in month.split("-"))
    ny, nm = (y + 1, 1) if m == 12 else (y, m + 1)
    return (f"e.timestamp >= '{y:04d}-{m:02d}-01 00:00+00' "
            f"AND e.timestamp < '{ny:04d}-{nm:02d}-01 00:00+00'")


def collect(window_days: int = 30, month: str | None = None) -> GlitchTipSnapshot:
    win = _window_sql(window_days, month)

    engagement = _psql(f"""
        SELECT p.name AS project,
               count(*) AS events,
               count(*) FILTER (WHERE e.type = 1) AS errors,
               count(*) FILTER (WHERE e.title = 'settings.analytics_enabled') AS opt_in,
               count(*) FILTER (WHERE e.title = 'settings.analytics_disabled') AS opt_out,
               count(DISTINCT e.data #>> '{{contexts,os,version}}') AS os_versions,
               count(*) FILTER (WHERE e.title LIKE 'trade.taken%') AS trade_taken,
               count(*) FILTER (WHERE e.title LIKE 'trade.completed%') AS trade_completed,
               count(*) FILTER (WHERE e.title LIKE 'screen.dashboard_opened%') AS dashboard_opens
        FROM issue_events_issueevent e
        JOIN issue_events_issue i ON i.id = e.issue_id
        JOIN projects_project p ON p.id = i.project_id
        WHERE {win}
        GROUP BY p.name
        ORDER BY events DESC
    """)

    # Real errors/crashes only (type=1), most frequent first.
    top_errors = _psql(f"""
        SELECT p.name AS project,
               CASE e.level WHEN 5 THEN 'fatal' WHEN 4 THEN 'error' ELSE e.level::text END AS level,
               left(e.title, 80) AS title,
               count(*) AS n
        FROM issue_events_issueevent e
        JOIN issue_events_issue i ON i.id = e.issue_id
        JOIN projects_project p ON p.id = i.project_id
        WHERE {win} AND e.type = 1
        GROUP BY p.name, e.level, e.title
        ORDER BY n DESC
        LIMIT 15
    """)

    # Trade-funnel step outcomes (the trade.* sealed events added for #1622), split per app so the
    # report can attribute reason-chip coverage (apps shipped the instrumentation at different times).
    trade_funnel = _psql(f"""
        SELECT p.name AS project, left(e.title, 60) AS step, count(*) AS n
        FROM issue_events_issueevent e
        JOIN issue_events_issue i ON i.id = e.issue_id
        JOIN projects_project p ON p.id = i.project_id
        WHERE {win} AND e.title LIKE 'trade.%'
        GROUP BY p.name, step
        ORDER BY n DESC
    """)

    event_counts = _psql(f"""
        SELECT p.name AS project, left(e.title, 60) AS title, count(*) AS n
        FROM issue_events_issueevent e
        JOIN issue_events_issue i ON i.id = e.issue_id
        JOIN projects_project p ON p.id = i.project_id
        WHERE {win} AND e.type <> 1
        GROUP BY p.name, left(e.title, 60)
        ORDER BY n DESC
    """)

    weekly_titles = ", ".join(f"'{t}'" for t in WEEKLY_TITLES)
    weekly = _psql(f"""
        SELECT to_char(date_trunc('week', e.timestamp AT TIME ZONE 'UTC'), 'YYYY-MM-DD') AS week,
               e.title AS title, count(*) AS n
        FROM issue_events_issueevent e
        WHERE {win} AND e.title IN ({weekly_titles})
        GROUP BY 1, 2
        ORDER BY 1
    """)

    versions = _psql(f"""
        SELECT p.name AS project,
               coalesce(r.version, '(unknown)') AS version,
               count(*) AS events,
               count(*) FILTER (WHERE e.title = 'screen.splash_opened') AS app_launches,
               count(*) FILTER (WHERE e.title = 'trade.taken') AS taken,
               count(*) FILTER (WHERE e.title = 'trade.btc_address_confirmed') AS address_confirmed,
               count(*) FILTER (WHERE e.title = 'trade.completed') AS completed,
               count(*) FILTER (WHERE e.title = 'trade.out_of_sync_detected') AS out_of_sync
        FROM issue_events_issueevent e
        JOIN issue_events_issue i ON i.id = e.issue_id
        JOIN projects_project p ON p.id = i.project_id
        LEFT JOIN releases_release r ON r.id = e.release_id
        WHERE {win}
        GROUP BY p.name, r.version
        ORDER BY p.name, events DESC
    """)

    projects = [
        ProjectEngagement(
            project=r["project"], events=r["events"], errors=r["errors"],
            opt_in=r["opt_in"], opt_out=r["opt_out"], os_versions=r["os_versions"],
            trade_taken=r["trade_taken"], trade_completed=r["trade_completed"],
            dashboard_opens=r["dashboard_opens"],
        )
        for r in engagement
    ]
    return GlitchTipSnapshot(window_days, projects, top_errors, trade_funnel, event_counts, weekly,
                             versions)


if __name__ == "__main__":
    snap = collect()
    print(f"GlitchTip {snap.window_days}d — total events (opted-in floor): {snap.total_events}")
    for p in snap.projects:
        print(f"  {p.project}: {p.events} events, {p.errors} errors, "
              f"opt-in {p.opt_in}/opt-out {p.opt_out}, trade {p.trade_completed}/{p.trade_taken} "
              f"({p.trade_success_pct}% success)")
