"""GitHub Releases download stats -> active sideload-base estimate.

Automates the methodology from docs/analysis (github-download-stats-2026-08.md):
the per-release APK download_count *while that release is the latest one* is the best proxy for
the updating (i.e. active) sideload user base — mostly the same people re-downloading each release.
We take the current-latest release's per-week rate and discount for bots/scanners/re-fetches.

CAVEATS baked into the output so the report stays honest:
  - GitHub counts every asset GET (bots, scanners, CDN) -> absolutes are an upper bound.
  - Cumulative != unique users; it's the same base re-downloading.
  - iOS .ipa is meaningless here: AltStore PAL mirrors the binary (only a handful of ingestion
    fetches show on GH). iOS adoption is not measurable from GitHub.
"""
from __future__ import annotations

import json
import subprocess
from dataclasses import dataclass
from datetime import date, datetime

REPO = "bisq-network/bisq-mobile"
# Bot/re-fetch discount applied to raw download_count for the active-base estimate.
BOT_DISCOUNT = 0.20

# Match a release tag to an app + asset kind. Tags look like `connect_0.8.0`, `anode_0.10.0`, etc.
APPS = {
    "Bisq Easy Node (Android)": {"tag_prefix": ("anode_", "node_", "bisq_easy"), "asset": ".apk"},
    "Bisq Connect (Android)": {"tag_prefix": ("connect_",), "asset": ".apk"},
}


@dataclass
class ReleaseDl:
    tag: str
    published: date
    apk_downloads: int
    days_as_latest: int

    @property
    def per_week(self) -> int:
        # Floor at a full week: a release only days old would extrapolate its launch spike
        # (everyone updates in the first days) into a wildly inflated weekly rate.
        d = max(self.days_as_latest, 7)
        return round(self.apk_downloads / d * 7)


@dataclass
class AppSideload:
    app: str
    latest: ReleaseDl | None
    recent: list[ReleaseDl]
    total_all_time: int

    @property
    def active_base_estimate(self) -> tuple[int, int] | None:
        """Low-high active sideload base from the latest release's weekly pull, bot-discounted."""
        if not self.latest:
            return None
        base = self.latest.per_week * (1 - BOT_DISCOUNT)
        return (round(base * 0.75), round(base))  # widen to a range


def _gh_releases() -> list[dict]:
    cmd = ["gh", "api", f"repos/{REPO}/releases", "--paginate", "--jq",
           ".[] | {tag: .tag_name, published: .published_at, "
           "assets: [.assets[] | {name: .name, dl: .download_count}]}"]
    out = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    if out.returncode != 0:
        raise RuntimeError(f"gh api failed: {out.stderr.strip()}")
    return [json.loads(line) for line in out.stdout.splitlines() if line.strip()]


def _app_for_tag(tag: str) -> str | None:
    for app, cfg in APPS.items():
        if tag.startswith(cfg["tag_prefix"]):
            return app
    return None


def collect() -> list[AppSideload]:
    raw = _gh_releases()
    # Bucket releases per app, newest first, computing each one's "days as latest" window.
    by_app: dict[str, list[dict]] = {app: [] for app in APPS}
    for rel in raw:
        app = _app_for_tag(rel["tag"])
        if app:
            by_app[app].append(rel)

    result: list[AppSideload] = []
    today = date.today()
    for app, rels in by_app.items():
        rels.sort(key=lambda r: r["published"], reverse=True)
        parsed: list[ReleaseDl] = []
        total = 0
        for idx, rel in enumerate(rels):
            pub = datetime.fromisoformat(rel["published"].replace("Z", "+00:00")).date()
            apk_dl = sum(a["dl"] for a in rel["assets"] if a["name"].lower().endswith(".apk"))
            total += apk_dl
            # Window = until the next (newer) release, or today for the latest.
            next_date = (
                datetime.fromisoformat(rels[idx - 1]["published"].replace("Z", "+00:00")).date()
                if idx > 0 else today
            )
            days = max((next_date - pub).days, 1)
            parsed.append(ReleaseDl(rel["tag"], pub, apk_dl, days))
        result.append(AppSideload(
            app=app,
            latest=parsed[0] if parsed else None,
            recent=parsed[:6],
            total_all_time=total,
        ))
    return result


if __name__ == "__main__":
    for a in collect():
        est = a.active_base_estimate
        est_s = f"~{est[0]}-{est[1]}" if est else "n/a"
        print(f"{a.app}: latest {a.latest.tag if a.latest else '-'} "
              f"({a.latest.per_week if a.latest else 0}/wk), active sideload base {est_s}, "
              f"all-time APK dl {a.total_all_time}")
