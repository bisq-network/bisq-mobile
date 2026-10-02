# Monthly KPI report generator

Produces a Markdown monthly report for the 3 Bisq mobile apps (Connect Android, Connect iOS,
Easy Node Android) to review and paste into the GH wiki. Replaces the ad-hoc Matrix status updates
(issue #1359).

## The honesty model (read this first)

Three provenance tiers are kept **separate on purpose** — they are not the same kind of number and
the channels are not deduplicable (a person can be on Play *and* sideload):

| Tier | Source | What it is |
|---|---|---|
| Real user counts | **Play Console / App Store Connect** | DAU/MAU/installs — the only true headcounts |
| Sideload floor | **GitHub download stats** | active updating base, estimated; bots inflate absolutes |
| Engagement floor | **GlitchTip** | event volume + behavior for the **opted-in** subset — NO user identity, by privacy design |

There is deliberately **no single "total users"** number. GlitchTip cannot count users (the opt-in
contract nulls `event.user` and disables sessions) — that is a feature of the privacy posture, not a
gap to paper over.

## Run

```bash
cd scripts/monthly-report
cp .env.example .env                         # set BISQ_REPORT_SSH_HOST (+ optional bucket); gitignored
cp inputs.example.json inputs.json           # fill store/operator numbers you have; leave unknowns null
python3 report.py --month 2026-09 --inputs inputs.json --out report-2026-09.md
```

`--month YYYY-MM` reports on exactly that calendar month (UTC), so the result does not depend on the
day you run it; run it in the first days of the following month. `--window N` (rolling N days back
from now) remains for ad-hoc readouts. Add `--wiki` for the variant that pastes into the year page.

`report.py` auto-loads `.env` (shell exports still win), so no per-run exports are needed. The host is
never hardcoded — it lives only in your gitignored `.env`.

Requirements: SSH access to the GlitchTip host (read-only Postgres) and the `gh` CLI (GitHub).

**Optional — live Play data.** Two independent halves, each degrades to `inputs.json` if unavailable:

- **Vitals (crash-free % + ANR)** via `play.py` → Play Developer Reporting API. Needs a service-account
  key granted read-only in Play Console (`BISQ_REPORT_PLAY_KEY`, default `../../credentials/…`,
  gitignored) and the `google-auth`/`requests` packages (hence the venv). Apps below Play's
  minimum-user threshold have Vitals suppressed and show `—`.
- **Audience (active devices) + new installs** via `play_installs.py` → Play Console statistics
  bucket. These `pubsite_prod_*` buckets are ACL-based and can't be shared with a service account, so
  it reads through the `gcloud` CLI with your own creds: install `gcloud`, `gcloud auth login` as the
  Play owner, and set `BISQ_REPORT_PLAY_STATS_BUCKET` (from Play Console → Download reports →
  Statistics → "Copy Cloud Storage URI"). MAU/DAU, total installs and rating stay manual — Play
  exposes no API for those.

```bash
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
export BISQ_REPORT_PLAY_STATS_BUCKET=pubsite_prod_XXXXXXXXXXXX
.venv/bin/python3 report.py --month 2026-09 --inputs inputs.json --out report-2026-09.md
```

A number typed into `inputs.json` always wins over the export, and the export only fills what is left
`null`. The export lags by days to weeks, so right after month end it is partial: installs and
uninstalls are therefore daily averages × 28 (the report says how many days they cover), and the
dashboard's 28-day "Active devices" is the better audience figure to type in.

## Modules

- `glitchtip.py` — engagement & health over SSH+SQL (event volume, opt-in, trade funnel `trade.*`,
  every event name per app, week-by-week and per-app-version splits, errors). Runnable standalone
  for a quick readout.
- `github_downloads.py` — sideload active-base estimate from release `download_count`
  (methodology from the manual `github-download-stats` report). Runnable standalone.
- `play.py` — live Play Vitals (crash-free % + ANR) via the Play Developer Reporting API; optional,
  degrades to `inputs.json` when the key/venv/access is absent. Runnable standalone.
- `play_installs.py` — audience (active devices), new installs and uninstalls from the Play
  statistics bucket via `gcloud`, with how much of the month the export covers; optional, degrades to
  `inputs.json`. Runnable standalone (`python3 play_installs.py 2026-07`).
- `report.py` — orchestrates all of the above + `inputs.json`, renders the md, and saves a snapshot
  per month in `history/` (gitignored) that next month's "vs last month" column reads.
- `inputs.example.json` — remaining manual store numbers (MAU/DAU, total installs, rating, iOS).


## Tuning

`github_downloads.BOT_DISCOUNT` (default 0.20) and the active-base low-bound factor (0.75) can be
tuned against the manual `github-download-stats` estimates. `UNSETTLED_SHARE` (default 0.25) is the
share of its predecessor's downloads below which the latest release is not used for the estimate.

## Reading the screen counts

Screen events are views: a screen is counted again each time it is re-entered (Back, edit, returning
from a sub-screen). Only the splash screen shows exactly once per app launch, so every "per 100" rate
in the report uses app launches as its base. Do not divide one funnel step by the step before it.
