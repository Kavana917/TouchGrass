# TouchGrass

An Android app that makes opening Instagram *expensive* instead of impossible.

You get a daily time budget for your watched social apps. When it runs out, you can always get back in — but the toll is writing an essay by hand, on a random word you don't get to choose, with paste disabled. Nothing is forbidden; it's just expensive enough that the reflex-open dies and only the deliberate open survives.

Blocking a habit leaves a hole, so the app also holds somewhere calmer to put your attention: live streams of real places, a drawing book, and a finite daily digest of what you're missing.

> **Guiding principle — friction over prohibition.**
> Never say "no." Say "yes, and here's what it costs."

## Status

**Pre-build.** Planning is complete; no application code yet. Work starts at Phase 1 of the roadmap.

## The four features

| # | Feature | Purpose |
|---|---------|---------|
| 1 | **The Pass** | Time budget for social apps; renewed only by writing an essay |
| 2 | **Live Feed** | Full-screen live streams of real places, chosen from a world map |
| 3 | **Drawing Book** | Multi-page sketchbook, standalone or pulled up over a live stream |
| 4 | **FOMO** | A finite daily digest of what's trending, so quitting doesn't feel like falling behind |

Feature 1 is the wall. Features 2–4 are the doors next to it.

## Planning documents

Everything lives in [`context/`](./context/). Read them in this order:

| Document | Answers |
|---|---|
| [`app_plan.md`](./context/app_plan.md) | **What** we're building — features, flows, edge cases, anti-goals |
| [`tech_stack.md`](./context/tech_stack.md) | **What with** — Kotlin, Compose, Room, and a two-JSON-files backend |
| [`design_theme.md`](./context/design_theme.md) | **What it looks like** — a late-90s desktop OS in pixel art |
| [`roadmap_plan.md`](./context/roadmap_plan.md) | **In what order** — twelve phases, each with a "done when" test |

Reference imagery for the visual theme is in [`context/theme_images/`](./context/theme_images/).

## Stack

Kotlin · Jetpack Compose · Room · WorkManager · Media3/ExoPlayer · MapLibre — Android first, min SDK 26. Local-first: no accounts, no sync, no server-side database. Essays and drawings never leave the device.

## Privacy

There is no backend that knows who you are. The only network calls fetch two static JSON files from a CDN. Your essays, drawings, and usage data stay on your phone.
