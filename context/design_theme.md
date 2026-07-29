# Design Theme

> [`app_plan.md`](./app_plan.md) — *what* we're building · [`tech_stack.md`](./tech_stack.md) — *what with* · **this file** — *what it looks like* · [`roadmap_plan.md`](./roadmap_plan.md) — *in what order*
> This document defines **what it looks like** — and, more importantly, the small set of forms we reuse everywhere so it stays consistent.
> **Reference images:** [`theme_images/`](./theme_images/) — 1: pixel icon set · 2: pixel desktop wallpaper + dialog · 3: retro icons on a phone home screen · 4: Win95 window chrome
> **Last updated:** 2026-07-28

---

## 1. The theme in one line

> **A late-90s desktop operating system, drawn in pixel art, running on your phone.**

Gray beveled windows with navy title bars, floating over a pixel sky-and-hills wallpaper. Chunky 3D buttons. Hard-outlined 32px icons. Dialog boxes that say things like *"Remember to drink water! [OK]"*.

### Why this theme fits this app

This isn't decoration picked at random — it does three jobs the product needs:

| The theme gives us | Why the app needs it |
|---|---|
| **Finite, bounded surfaces.** A window has edges, a title bar, and a status bar reading `0 objects`. | The core anti-goal is *"nothing scrolls forever; every surface has a bottom."* A window is literally a box with a bottom. Infinite scroll is architecturally foreign to this visual language. |
| **A non-judgemental machine voice.** An old OS states facts: `The pass has expired.` It doesn't have feelings about you. | The app must never scold. A 1997 dialog box is incapable of disappointment — it's the least guilt-inducing narrator available. |
| **Deliberate friction that reads as charm, not punishment.** Old software made you click OK. | The whole product is friction-over-prohibition. Here the friction is *in costume*, so paying the toll feels like using a quirky machine instead of being told off. |

**And the counterweight:** the retro layer is a *frame*, never a *filter*. It never lands on top of a drawing canvas or an essay you're writing. See §8.

---

## 2. What's actually in the reference images

So future work can match the source rather than a vague memory of "retro."

**`1.webp` — pixel icon set.** Isometric and front-facing objects on cream: joystick, gamepad, CRT monitor, Game Boy, folder stack, microphone, palm tree, cat. Every icon has a hard 1px black outline, flat fills, and 2–3 shading tones. No gradients, no anti-aliasing. Cheerful, saturated, slightly chunky.

**`2.webp` — pixel desktop.** A Windows XP "Bliss" wallpaper redrawn in pixels: blue sky, blocky white clouds, green hill, hard horizon. On top: a small cream dialog box with a message and a single `[OK]` button, plus a green taskbar with a `start` button. Shows the whole stack — wallpaper → window → button.

**`3.webp` — retro icons on a phone.** Win95 icons (gears, globe, folder, clock, camera, red pixel heart) laid over a periwinkle pixel sky, with an authentic right-click context menu: `Arrange Icons ▸ / Line up Icons / Paste / New ▸`, cascading submenu, navy highlight bar on the selected row, underlined keyboard mnemonics. This is the phone-sized proof the theme works on a small screen.

**`4.webp` — window chrome, up close.** The canonical Win95 frame: `#C0C0C0` face, double bevel, navy title bar with white bold text and `_ □ ✕` buttons, `File Edit View Help` menu, column headers, a sunken content well, a scrollbar with arrow buttons, and a status bar reading `0 object(s) | 0 bytes` with a resize grip. **This is our master reference for chrome.**

---

## 3. The four locked decisions

Confirmed with the project owner before this document was written. Everything downstream follows from these.

| # | Decision | Choice | What it means in practice |
|---|---|---|---|
| 1 | **Fidelity** | **Retro chrome, calm interiors** | Every screen is a Win95 window — title bar, bevels, status bar. What's *inside* gets modern spacing and touch targets. We keep the costume, not the 2px paddings. |
| 2 | **Typography** | **Pixel display + clean sans body** | Pixel font for chrome, labels, buttons, numbers. A normal sans for anything you read in sentences — essays, digest text, onboarding prose, settings explanations. |
| 3 | **Palette** | **XP sky + Win95 gray** | Pixel sky-and-hills wallpaper as the backdrop; gray beveled windows floating on it; navy title bars. |
| 4 | **Dark mode** | **Night = dusk wallpaper, same chrome** | Windows stay gray at all hours. Only the wallpaper shifts to a dusk/night sky. Authentic *and* cheap. |

**Decision 1 is the one to keep re-reading.** The failure mode for this theme is authenticity-poisoning: 11px text, 4px hit targets, five nested toolbars. Every one of those is period-correct and every one makes the app worse on a phone. When authenticity and legibility conflict, **legibility wins** — that isn't a compromise of the theme, it's the rule of the theme.

---

## 4. Colour

### 4.1 Chrome — the window system

Exact Win95 system colours. Don't invent new grays.

| Token | Hex | Used for |
|---|---|---|
| `surface` | `#C0C0C0` | Window face, buttons, menu bars, status bars, dialog bodies |
| `surfaceLight` | `#DFDFDF` | Inner top/left bevel |
| `surfaceWhite` | `#FFFFFF` | Outer top/left bevel; sunken field backgrounds |
| `surfaceShadow` | `#808080` | Inner bottom/right bevel; disabled glyphs |
| `surfaceBlack` | `#0A0A0A` | Outer bottom/right bevel; 1px icon outlines |
| `titleActive` | `#000080` | Active title bar, menu highlight, selection |
| `titleInactive` | `#808080` | Inactive/background window title bar |
| `titleText` | `#FFFFFF` | Text on navy |
| `bodyText` | `#0A0A0A` | Text on gray and on white fields |
| `disabledText` | `#808080` | Disabled labels — **never the only disabled signal**, see §9 |

### 4.2 Desktop — the wallpaper behind everything

| Token | Day | Night | Notes |
|---|---|---|---|
| `skyTop` | `#4A90D9` | `#141C3A` | Vertical band, not a smooth gradient — see §7 |
| `skyBottom` | `#7FB2E5` | `#2B3566` | |
| `cloud` | `#FFFFFF` | `#3D4675` | Blocky, hard-edged, 4px minimum feature size |
| `hillFar` | `#5C9E3C` | `#1E3A22` | |
| `hillNear` | `#3E7B28` | `#152B18` | |

Night flips the wallpaper only. **Windows, buttons, and text never recolour.** One set of chrome tokens, all day, all night — this is what makes decision 4 cheap.

### 4.3 Accents — used sparingly

| Token | Hex | Reserved for |
|---|---|---|
| `paperCream` | `#FDF3D3` | Balloon/notice dialogs (from `2.webp`), essay paper, drawing pages |
| `accentRed` | `#D32F2F` | The pixel heart; expiry states. **Never for scolding.** |
| `accentTeal` | `#008080` | Rare secondary highlight; classic desktop teal |
| `highlightYellow` | `#FFD54F` | Selected drawing tool, active map pin |

**Colour is not a status system here.** Chrome does the work — a raised button reads as pressable, a sunken well reads as content. Resist inventing a red/amber/green semantic layer; the app has no scores and no warnings to escalate.

---

## 5. Typography

| Role | Font | Size | Where |
|---|---|---|---|
| **Title bars** | Pixel, bold | 16sp | Window titles |
| **Chrome & controls** | Pixel | 16sp | Buttons, menus, tabs, labels, status bar |
| **Numerals** | Pixel | 20–32sp | Time remaining, word counter, page numbers |
| **Headings** | Pixel | 20–24sp | Section titles inside windows |
| **Body** | Sans (Inter / Roboto) | 16sp / 1.5 line | **Essays, FOMO digest text, onboarding prose, settings explanations** |
| **Body small** | Sans | 14sp / 1.5 | Captions, timestamps, secondary notes |

**The dividing line:** if you *scan* it, it's pixel. If you *read* it, it's sans. A 150-word essay in a bitmap font is a worse essay-writing experience, and the essay is the product.

**Font candidates:** `W95FA` (MS Sans Serif clone, free) or `Pixelify Sans` (OFL, on Google Fonts) for pixel; `Inter` or system `Roboto` for sans. Bundle the pixel face as a TTF asset — don't rely on a download at runtime, the app is local-first.

**Pixel font rendering rules:**
- Size in **exact multiples of the font's design size** (e.g. 8px design → 16sp, 24sp, 32sp). In-between sizes produce blurry half-pixels.
- Disable font smoothing/hinting where the platform allows it.
- No letter-spacing tweaks, no faux-bold, no italics. Bitmap faces have one weight; synthesizing more looks broken.
- **Never use pixel type below 16sp.** If it doesn't fit at 16sp, the layout is wrong, not the font.

**Mnemonic underlines** (`<u>F</u>ile`) are decorative here — there's no keyboard. Use them in menu bars for flavour; never imply a shortcut that doesn't exist.

---

## 6. The standard forms

**This is the section to build first.** Ten components. Everything in the app is assembled from these — if a screen needs an eleventh, that's a design conversation, not a quick addition.

### 6.1 Bevel — the one primitive

Every raised/sunken surface uses the same double-bevel recipe. Build it once.

```
RAISED (buttons, window frames, menu bars)
  outer top/left      #FFFFFF
  outer bottom/right  #0A0A0A
  inner top/left      #DFDFDF
  inner bottom/right  #808080
  face                #C0C0C0

PRESSED (button held)  — invert both bevel pairs, shift label 1px down-right
SUNKEN (text fields, list wells, canvas) — invert, face #FFFFFF
```

All borders are **1px hard lines**. No border radius anywhere. No shadows except the bevel itself. No blur, no glow, no translucency.

### 6.2 The component set

| # | Form | Spec | Used by |
|---|---|---|---|
| 1 | **Window** | Raised frame, 4dp inset content. Optional menu bar, optional status bar. The container for every screen. | Everything |
| 2 | **Title bar** | 32dp tall, `#000080`, 16dp pixel icon at left, bold white title, `_ □ ✕` at right. Inactive → `#808080`. | Every window |
| 3 | **Button** | Raised bevel, min **48dp touch target** (visual may be shorter — pad the hit area, not the pixels), pixel label, centred, ALL CAPS for primary actions. | Everywhere |
| 4 | **Sunken field** | White well, 1px sunken bevel, 12dp padding, sans body text. | Essay editor, search, settings inputs |
| 5 | **Menu bar** | 28dp strip on `#C0C0C0`, pixel labels with mnemonic underlines. Row highlight = `#000080` + white text. | Window headers, overflow menus |
| 6 | **Context menu** | Raised panel, 40dp rows, `▸` for submenus, 1px `#808080` groove separators — exactly as in `3.webp`. | Long-press actions |
| 7 | **List view** | Column headers as small raised buttons, alternating-free white rows, navy selection bar. As in `4.webp`. | FOMO digest, essay history |
| 8 | **Status bar** | 24dp, sunken well(s), pixel text, resize grip glyph at right. **Always states a fact, never an opinion.** | Bottom of most windows |
| 9 | **Dialog / balloon** | Small window, no menu bar, `paperCream` or gray face, message + 1–2 buttons. The `2.webp` form. | Confirmations, the Instagram overlay |
| 10 | **Pixel icon** | 32×32 base grid, 1px `#0A0A0A` outline, flat fills, 2–3 shading tones, no AA. Scale **only** at 2×/3× nearest-neighbour (32→64→96). | Navigation, tools, empty states |

### 6.3 Supporting details

- **Scrollbar** — visible, chunky, with arrow buttons and a raised thumb. Do not hide it; a visible scrollbar showing how little is left reinforces "this surface has a bottom."
- **Progress** — segmented blocks, not a smooth bar. Fills in discrete steps.
- **Checkbox / radio** — sunken square with a pixel `✓`; sunken circle with a filled dot.
- **Groove separator** — 1px `#808080` over 1px `#FFFFFF`. The only divider we use.
- **Taskbar** — persistent bottom strip: a start-style button opening the nav menu, and a sunken clock well showing **time left today**. Matches `2.webp` and `3.webp`, and it's exactly the persistent state Home already needs to show.

---

## 7. Motion, texture, and sound

**Motion is stepped, never eased.** Old software snapped. State changes are instant (0ms) or 2–3 discrete frames. No spring physics, no fades, no `easeInOutCubic`. A dialog appears; it does not gracefully arrive.

The one permitted flourish is the **wireframe zoom** — a 1px outline rectangle expanding from origin to target in 3 steps when a window opens. Use it for major screen transitions, not for every tap.

**Texture is quantized.** Any gradient becomes 3–5 hard bands. Any curve becomes a stair-step. Any circle is a pixel circle. Set `FilterQuality.None` on every pixel asset — bilinear filtering on pixel art is the single most common way this theme dies.

**Sound is off by default.** Retro system beeps are the most tempting and most wrong addition here: the app exists to be calmer than what it replaces. Optional, opt-in, and never on the essay screen.

---

## 8. Where the theme steps back

Two surfaces where chrome is a liability. These are load-bearing exceptions, not oversights.

| Surface | What happens |
|---|---|
| **Drawing canvas** | Pure white or `paperCream` sunken well. No pixel grid, no texture, no overlay. Tools live in a Paint-style palette *beside* the canvas, never on it. The canvas is the user's, not the theme's. |
| **Essay editor** | Retro window frame, but the writing area is a plain sunken white field with 16sp sans and generous line height. Someone is writing 150 words by hand under mild frustration — nothing in that rectangle may add friction. |

**The rule:** the theme owns the frame; the user owns the content.

---

## 9. Accessibility floor

Non-negotiable, and mostly cheap.

- **Touch targets ≥ 48dp**, always — visual chrome may render smaller, but pad the hit area.
- **Pixel type ≥ 16sp.** No exceptions.
- **Contrast:** `#0A0A0A` on `#C0C0C0` ✓ · `#FFFFFF` on `#000080` ✓ · `#808080` on `#C0C0C0` ✗ — so **disabled state must also change the bevel** (flatten it), never colour alone.
- **Respect system font scale** on all sans body text. Pixel chrome may cap its scaling to avoid layout breakage, but body copy scales fully.
- **Every icon-only control gets a content description.** Pixel icons are cute and frequently ambiguous.
- **Honour "reduce motion"** — trivial here, since we're already near-zero motion. Drop the wireframe zoom.

---

## 10. Screen ↔ metaphor map

Each surface gets a desktop-era counterpart. Worth following: it makes new screens obvious to design, and it keeps naming consistent across the codebase.

| App surface | Desktop metaphor | Notes |
|---|---|---|
| **Home** | The desktop | Wallpaper, pixel icon grid, taskbar clock showing time left |
| **Pass status & history** | Control Panel / Properties | Tabbed, factual, sunken info wells |
| **Essay editor** | Notepad — `Untitled — Essay` | Menu bar, sunken paper, `words: 84 / 150` in the status bar |
| **Instagram overlay** | Modal dialog box | Cream balloon, centred, 2 buttons. Must appear *instantly* — see plan §2.2 |
| **Drawing Book** | MS Paint | Tool palette, page tabs along the bottom |
| **FOMO digest** | A file list view | Columns, finite rows. Empty state reads `0 items — you're caught up.` — the `4.webp` status bar, doing real work |
| **Settings** | Control Panel | Grouped, tabbed, checkbox-heavy |
| **Onboarding** | Setup Wizard | `< Back` / `Next >` buttons bottom-right, one decision per pane |

The FOMO empty state is the best example of theme and product agreeing: a status bar reading `0 items` is authentically retro *and* is precisely the "you have reached the bottom, you are done" message the feature is built to deliver.

---

## 11. Voice

The theme carries a voice, and it has one hard boundary.

**Machine voice for states.** Flat, factual, unbothered:
> `The pass has expired.`
> `0 items. You're caught up.`
> `Essay saved to Documents.`

**Human voice for choices and prose.** Warm, plain, no jokes at the user's expense:
> "Write 150 words. Anything you like. Nobody reads this but you."

**Never:** guilt, streak-shaming, mock error sounds aimed at the user, `Are you sure? (Y/N)` played for laughs when they've done nothing wrong, or a Clippy-style character offering opinions about their habits. The plan's anti-goals apply to copy exactly as they apply to features — the retro costume is not a licence to be a hostile teacher in a funnier font.

---

## 12. Assets & legal

- **Draw our own icons.** Do not ship extracted Microsoft `.ico` files or Bliss photography — those are real assets under copyright and this app is going to the Play Store. Everything is original pixel art *in the style of* the reference images.
- **Check font licences.** `Pixelify Sans` and `Silkscreen` are OFL. Confirm redistribution terms before bundling any pixel face and record the chosen licence in the repo.
- **Avoid trademark surface area.** No Windows flag, no `start` wordmark styled as Microsoft's, no "Windows" in any user-facing string. The *language* of 90s UI isn't protected; specific marks and assets are.
- **Asset pipeline:** author at 1× (32×32 etc.), export PNG with no AA, scale only by integers at runtime with `FilterQuality.None`.

---

## Appendix — the theme at a glance

| | |
|---|---|
| **Fidelity** | Retro chrome, calm interiors — legibility wins ties |
| **Type** | Pixel for scanning, sans for reading; nothing pixel below 16sp |
| **Palette** | `#C0C0C0` windows · `#000080` title bars · pixel sky/hills wallpaper |
| **Dark mode** | Wallpaper goes to dusk; chrome never changes |
| **Primitive** | One double-bevel recipe, 1px hard borders, zero corner radius |
| **Forms** | 10 components (§6.2) — an 11th needs a conversation |
| **Motion** | Stepped, near-zero. One wireframe zoom, used sparingly |
| **Exceptions** | Canvas and essay text are chrome-free |
| **Floor** | 48dp targets · 16sp minimum · disabled ≠ colour alone |
| **Voice** | Machine for facts, human for choices, never scolding |
