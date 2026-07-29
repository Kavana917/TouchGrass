# TouchGrass

An Android app that makes opening Instagram *expensive* instead of impossible.

You get a daily time budget for your watched social apps. When it runs out, you can always get back in — but the toll is writing an essay by hand, on a random word you don't get to choose, with paste disabled. Nothing is forbidden; it's just expensive enough that the reflex-open dies and only the deliberate open survives.

Blocking a habit leaves a hole, so the app also holds somewhere calmer to put your attention: a drawing book and a finite daily digest of what you're missing.

> **Guiding principle — friction over prohibition.**
> Never say "no." Say "yes, and here's what it costs."

## Status

**In development.** The Pass (usage monitoring, essay toll, and wall overlay) is implemented. Drawing book and FOMO digest are planned.

## The three features

| # | Feature | Purpose |
|---|---------|---------|
| 1 | **The Pass** | Time budget for social apps; renewed only by writing an essay |
| 2 | **Drawing Book** | Multi-page sketchbook for absorbing the hands-and-eyes impulse |
| 3 | **FOMO** | A finite daily digest of what's trending, so quitting doesn't feel like falling behind |

Feature 1 is the wall. Features 2–3 are the doors next to it.

## Planning documents

Everything lives in [`context/`](./context/). Read them in this order:

| Document | Answers |
|---|---|
| [`app_plan.md`](./context/app_plan.md) | **What** we're building — features, flows, edge cases, anti-goals |
| [`tech_stack.md`](./context/tech_stack.md) | **What with** — Kotlin, Compose, Room, and a one-JSON-file backend |
| [`design_theme.md`](./context/design_theme.md) | **What it looks like** — a late-90s desktop OS in pixel art |
| [`roadmap_plan.md`](./context/roadmap_plan.md) | **In what order** — nine phases, each with a "done when" test |

Reference imagery for the visual theme is in [`context/theme_images/`](./context/theme_images/).

## Stack

Kotlin · Jetpack Compose · Room · WorkManager — Android first, min SDK 26. Local-first: no accounts, no sync, no server-side database. Essays and drawings never leave the device.

## Privacy

There is no backend that knows who you are. The only network call fetches one static JSON file from a CDN (the FOMO digest). Your essays, drawings, and usage data stay on your phone.
