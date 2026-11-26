---
name: Myophoria
description: A gym lift tracker that runs under the gym's wall clock.
colors:
  ground: "#000000"
  field: "#0f0f0f"
  line: "#242424"
  edge: "#3a3a3a"
  faint: "#5e5e5e"
  dim: "#8c8c8c"
  ink: "#f2f2f2"
  live: "#e8392f"
typography:
  display:
    fontFamily: "Oswald, Barlow, system-ui, sans-serif"
    fontSize: "3.75rem"
    fontWeight: 500
    lineHeight: 1
    fontFeature: "tnum"
  headline:
    fontFamily: "Oswald, Barlow, system-ui, sans-serif"
    fontSize: "1.875rem"
    fontWeight: 500
    lineHeight: 1
    letterSpacing: "0.04em"
  title:
    fontFamily: "Oswald, Barlow, system-ui, sans-serif"
    fontSize: "1.25rem"
    fontWeight: 400
    letterSpacing: "0.04em"
  number:
    fontFamily: "Oswald, Barlow, system-ui, sans-serif"
    fontSize: "1.25rem"
    fontWeight: 400
    fontFeature: "tnum"
  body:
    fontFamily: "Barlow, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.5
    fontFeature: "tnum"
  button:
    fontFamily: "Oswald, Barlow, system-ui, sans-serif"
    fontSize: "15px"
    fontWeight: 400
    letterSpacing: "0.08em"
  tab:
    fontFamily: "Oswald, Barlow, system-ui, sans-serif"
    fontSize: "13px"
    fontWeight: 400
    letterSpacing: "0.1em"
  label:
    fontFamily: "Oswald, Barlow, system-ui, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 400
    letterSpacing: "0.1em"
rounded:
  none: "0px"
spacing:
  gutter: "16px"
  tap: "44px"
  button: "48px"
  row: "56px"
components:
  button:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: "0 16px"
    height: "48px"
  button-primary:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.ground}"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: "0 16px"
    height: "48px"
  button-danger:
    backgroundColor: "transparent"
    textColor: "{colors.live}"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: "0 16px"
    height: "48px"
  button-disabled:
    backgroundColor: "transparent"
    textColor: "{colors.faint}"
  input:
    backgroundColor: "{colors.field}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.none}"
    padding: "0 8px"
    height: "44px"
  input-disabled:
    backgroundColor: "{colors.field}"
    textColor: "{colors.dim}"
  tick:
    backgroundColor: "transparent"
    rounded: "{rounded.none}"
    size: "32px"
  tick-checked:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.ground}"
  nav-tab:
    backgroundColor: "{colors.ground}"
    textColor: "{colors.dim}"
    typography: "{typography.tab}"
    height: "56px"
  nav-tab-active:
    backgroundColor: "{colors.ground}"
    textColor: "{colors.live}"
  badge:
    backgroundColor: "transparent"
    textColor: "{colors.dim}"
    typography: "{typography.label}"
    rounded: "{rounded.none}"
    size: "40px"
  clock:
    textColor: "{colors.live}"
    typography: "{typography.display}"
  workout-bar:
    backgroundColor: "{colors.field}"
    textColor: "{colors.dim}"
    padding: "8px 16px"
---

# Design System: Myophoria

## Overview

**Creative North Star: "The Wall Clock"**

The workout runs under the gym's wall clock. One big red elapsed clock owns the top of the Workout screen; everything else is plain white ink on matte black. The lifter glances, sees how long they have trained, which exercise is open and which set is next, ticks it and moves on. The system refuses the category default of rounded dark cards, coloured badges and red buttons everywhere.

Density is that of a scoreboard read from a bench: few elements, large tap targets, condensed capitals for anything that is a name, a label or a number. Structure comes from one-pixel hairlines and a stepped grey ramp, never from boxes, fills or shadows. Nothing moves except the clock's digits, which change once a second with no animation; every other state change is instant.

The world replaced an unstyled stock-Tailwind slate-and-white look. It is dark only, by design, and declares `color-scheme: dark` so native selects, date pickers, file pickers and dialogs render in the same world.

**Key Characteristics:**
- Matte black ground, near-white ink, one six-step grey ramp, one red.
- Red means time, live state or consequence; never decoration.
- Oswald condensed caps for headings, labels, numbers, tabs and buttons; Barlow for running text.
- Tabular numerals everywhere.
- Square corners, hairlines, no cards, no shadows, no motion.
- 44px minimum tap targets; 48px buttons; 56px rows.

## Colors

A monochrome scale from pure black to near-white, with a single signal red.

### Primary
- **Wall Clock Red** (live): the running elapsed clock (Workout header and the active workout bar), the open exercise's ticked count (for example 3/3), the outline of the next set's weight and reps cells, the active tab label, the RUNNING tag on an open session in History, the failure set label F, destructive buttons (outline and text), and error and warning frames. Also the text caret and the selection highlight.

### Neutral
- **Matte Black** (ground): the page and the tab bar; also the selection text colour and the ink on a primary button.
- **Panel Black** (field): input wells, the confirm dialog, the active workout bar and the status banner; the only raised tone, one step above ground.
- **Hairline** (line): every divider and default border (the base layer sets it as the border colour of every element); disabled button and input borders; a deleted exercise's badge outline.
- **Edge Grey** (edge): control outlines (buttons, inputs, unticked boxes, badges, dialog frame), the chart axis and the scrollbar thumb.
- **Faint Grey** (faint): disabled button text and a deleted exercise's initials.
- **Dim Grey** (dim): secondary text, labels, inactive tabs, closed exercise names, unticked set rows, placeholders, chart labels.
- **White Ink** (ink): primary text, the open exercise's name, ticked set values, the primary button fill, the ticked box fill, the chart line, the focus ring.

### Named Rules
**The Red Means Now Rule.** Red marks time, the thing to do next, live state, a failure set, or a consequence (destruction or error). If a red element answers none of those, it is wrong. Never use red as a fill, a background or an accent.

**The One Ramp Rule.** Hierarchy comes from stepping along the grey ramp (ground, field, line, edge, faint, dim, ink), not from new hues. Do not add a second accent colour.

## Typography

**Display Font:** Oswald (variable, 200-700; falls back to Barlow, then system-ui)
**Body Font:** Barlow (400 and 500-700; falls back to system-ui)

**Character:** Oswald is the gym's signage: condensed, tall, read from across the room. Barlow is the quiet grotesque for sentences. Both are self-hosted woff2 under the SIL OFL (`web/public/fonts/LICENSE.md`); nothing is fetched from a font CDN.

### Hierarchy
- **Display** (Oswald 500, 60px, line-height 1, red): the running clock in the Workout header only. The workout bar carries the same clock at 30px.
- **Headline** (Oswald 500, 30px, line-height 1, uppercase, 0.04em): screen titles (h1). The base layer makes every h1 and h2 Oswald caps.
- **Title** (Oswald 400, 20px, uppercase, 0.04em): exercise names in workout rows; ink when open, dim when closed.
- **Number** (Oswald 400-600, 18-20px): set labels (W, 1, 2, F) and the weight and reps values in the set table.
- **Body** (Barlow 400, 16px; 13-14px for secondary lines): sentences, dates, notes, list details. Gym name in the header is Barlow 600 at 15px.
- **Button** (Oswald 400, 15px, uppercase, 0.08em): every button label.
- **Tab** (Oswald 400, 13px, uppercase, 0.1em): bottom navigation.
- **Label** (Oswald 400, 12px, uppercase, 0.1em, dim): form labels, table headers (SET, KG, REPS, DONE), section headers such as Up next.

### Named Rules
**The Caps Are Oswald Rule.** Uppercase is only ever set in Oswald, always with positive tracking (0.04em for headings and names, 0.08em for buttons, 0.1em for labels and tabs). Barlow is never uppercased.

**The Tabular Rule.** The body sets `font-variant-numeric: tabular-nums`; weights, reps and the clock never jitter as digits change.

## Layout

A single centred column, max 36rem (576px) wide, with a 16px gutter at every width; desktop shows the same column centred on black. The tab bar and, while a workout runs on another tab, the active workout bar are pinned to the bottom; the bar sits directly above the tabs, and main content pads its bottom (96px, or 160px with the bar) to clear them. The tab bar respects the bottom safe-area inset.

Lists are full-width rows divided by hairlines, at least 56px tall. Every interactive target is at least 44px (inputs, set cells, unit select, ticks inside a 44px cell); buttons are 48px tall and full width when they are the screen's main action. The Workout header bleeds to the gutter edges with a hairline beneath: clock on the left, gym, date and start time right-aligned.

## Elevation & Depth

Flat. There are no shadows anywhere. Depth is one tonal step: panel black (field) under inputs, the dialog and the workout bar, against matte black ground. The modal dialog dims the page with an 80% black backdrop and has an edge-grey frame.

### Named Rules
**The No-Card Rule.** Groups are separated by hairlines above and below, never boxed. A "card" in this system is a top and bottom hairline with nothing else.

## Shapes

Square corners everywhere (0px radius). Borders are one pixel, except the done box, which uses a 2px edge. Icons are authored 24px line drawings with one 2px stroke and square caps (close, back, chevron, check, plus); the tick mark inside a checked box is the same square-capped check. Exercise badges are 40px squares holding up to four initials.

## Components

### Buttons
Outlined, square, confident; capitals that read like a sign.
- **Shape:** square (0px), 1px border, 48px tall, 16px side padding; 44px tall with 12px padding inside the workout bar.
- **Default:** transparent, edge-grey outline, white ink.
- **Primary:** solid white ink fill and border, black text. For the screen's main forward action (Finish workout, Start, Resume, Save) and every non-destructive answer in a confirm dialog.
- **Danger:** transparent with a red outline and red text. Destructive confirmations only.
- **Disabled:** hairline border, faint text.
- **Hover / Focus:** no hover change; keyboard focus draws a 2px white outline offset 2px. No transitions.

### Inputs / Fields
- **Style:** panel-black well, 1px edge-grey border, square, 44px tall, 8px side padding; placeholders in dim grey; caret red.
- **Focus:** the border turns white ink (plus the global 2px focus ring).
- **Disabled:** hairline border, dim text.
- **Required but empty:** a red outline (the bodyweight field on a calisthenics exercise).
- **File picker:** the native button is restyled as an Oswald caps segment, separated from the filename by an edge-grey rule.
- **Set table cells:** centred Oswald 20px. The next set to tick is outlined red; ticked sets lose their border and background and read as plain numbers, still editable.

### Done Box (signature)
A 32px square with a 2px edge-grey border. Checked, it fills white ink with a black square-capped check cut out. This is the screen's primary action: tick the next set and move on.

### Clock (signature)
Elapsed time since the workout started, Oswald 500, red, tabular, line-height 1. It ticks once a second with no animation, sits at 60px in the Workout header, and rides at 30px in the active workout bar on every other tab.

### Active Workout Bar
A panel-black strip above the tab bar with a top hairline, shown on every tab except Workout and Summary while a workout runs: the red clock, a two-line dim label (WORKOUT RUNNING), an outlined End button and a solid Resume button.

### Navigation
A bottom tab bar on matte black with a top hairline: four equal 56px tabs in Oswald 13px caps. Inactive tabs are dim grey; the active tab is red text only, with no underline, fill or icon.

### Lists and Rows
Exercise rows are full-width buttons, 56px minimum, separated by hairlines: name in Oswald caps on the left, a count or set total on the right, a chevron at the end. The open row shows its ticked count in red and expands the set table beneath it.

### Confirm Dialog
A native modal dialog on panel black with an edge-grey frame, width min(28rem, 100% minus 32px), 16px padding. With one answer, buttons sit right-aligned (Cancel, then the answer). With two or more answers, every button stacks full width in one column in the given order with Cancel last.

### Badge
A 40px square, 1px edge-grey outline, dim Oswald initials (14px, or 12px for three or four letters); deleted exercises step down to hairline and faint.

### Alerts
Inline errors are red text in a red 1px frame. The startup recovery banner is panel black with a red bottom border and a Dismiss link.

## Do's and Don'ts

### Do:
- **Do** keep every corner square (0px) and every border one pixel (2px only on the done box).
- **Do** separate groups with hairlines (#242424 line) above and below, not boxes.
- **Do** set every label, heading, tab, button and number in Oswald caps with positive tracking, and all running text in Barlow.
- **Do** keep tap targets at least 44px and buttons 48px tall.
- **Do** make state changes instant: the open row, ticked sets, tabs and dialogs switch without fades or slides.
- **Do** draw new icons as 24px line paths with a 2px stroke and square caps, using currentColor.
- **Do** reserve the solid white primary button for the screen's main forward action and dialog answers; everything else is outlined.

### Don't:
- **Don't** use red for anything but the clock, the next thing to do, live state, a failure set, destruction or an error; never as a fill or background.
- **Don't** add rounded corners, cards, shadows or gradients.
- **Don't** add a second accent hue; step the grey ramp instead.
- **Don't** animate anything; the clock ticks once a second with no transition.
- **Don't** uppercase Barlow or set body sentences in Oswald.
- **Don't** load fonts from a CDN; add weights as self-hosted woff2 with their licence.
