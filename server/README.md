# The Live Feed registry

There is no server. This directory is a scheduled script that writes a JSON
file, and that file is the entire backend.

```
.github/workflows/refresh-streams.yml   every 6 hours
        │
        ├── reads   server/channels.json     ← the curated part, edit this
        ├── asks    YouTube Data API v3      ← what's live right now
        └── writes  server/streams.json      ← committed, served from the repo
                          │
                    every phone fetches ~20KB, caches it for 6 hours
                          │
                    video streams YouTube → device, never touching us
```

## Why it works this way

`search.list` is capped at **100 calls per day for the whole project**, shared
across every install. An app that called YouTube for itself would exhaust the
global budget at around five users, and would need the API key inside the
APK where anyone can read it.

Running it here makes cost **O(channels), not O(users)**: the same handful of
calls a day whether ten people use the app or ten million. That is the
property that keeps the app free at any scale, and it is the reason the key
lives in a GitHub secret rather than in the build.

## One-time setup

1. **Get a key.** Google Cloud Console → new project → enable **YouTube Data
   API v3** → Credentials → Create API key. Free, no billing account, no card.
2. **Restrict it** to the YouTube Data API (Credentials → the key → API
   restrictions). Not strictly required, but it limits the blast radius if it
   ever leaks.
3. **Add the secret.** Repo → Settings → Secrets and variables → Actions →
   New repository secret, named exactly `YOUTUBE_API_KEY`.
4. **Run it.** Actions → *Refresh Live Feed* → Run workflow. The log names
   every channel it resolved and every stream it rejected, with the reason.

## Adding a place

Edit `channels.json` and push. That is the whole workflow — pushing a change
to it triggers a run.

Curate **channels**, never videos. A 24/7 stream is restarted regularly and
gets a new video ID each time, so a pinned video ID rots within days; that is
exactly how the first registry died (commit `edb57f6`). Channel handles are
permanent.

## Quota, per run

| Call | Cost | When |
|---|---|---|
| `videos.list` | 1 unit per 50 streams | every run — verifies what we already have |
| `channels.list` | 1 unit per handle | once per channel, then cached in `channel_ids.json` |
| `search.list` | 1 call per channel | only for channels whose stream went dark |

A quiet run — nothing died — costs **1 unit**. The daily allowance is 10,000
units plus 100 search calls, so we use a fraction of a percent of it.

`MAX_SEARCH_CALLS_PER_RUN` in `refresh_streams.py` is set to 20, which bounds
the worst case at 80 search calls a day across 4 runs. **If you change the
schedule, change that constant to match** — otherwise a bad day exhausts the
search quota until midnight Pacific and the registry stops updating.

## Things it refuses to do

- **Publish an empty registry.** If nothing is live anywhere, the previous
  `streams.json` is left in place. The app treats a successful-but-empty
  fetch as authoritative, so writing one would blank the feed for everybody.
- **Ship a stream that can't be embedded.** Every candidate is checked for
  `status.embeddable`, public privacy status, and region blocks. A stream can
  be perfectly live and still refuse to play because the uploader disabled
  embedding — that is the likely cause of the error 152 failures noted in
  `StreamPlayer.kt`, and filtering here means users never meet it.

## Running it locally

```bash
YOUTUBE_API_KEY=... python server/refresh_streams.py
YOUTUBE_API_KEY=... python server/refresh_streams.py --full   # ignore the cache
```

Standard library only — nothing to install.
