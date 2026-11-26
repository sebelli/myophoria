---
version: 1
slug: "web-src-app-svelte"
primary_target: "web/src/App.svelte"
related_targets: ["web/src"]
---

# Surface: Myophoria app shell (all tabs, workout first)

Scope: the whole Svelte SPA in `web/src`, with the active Workout screen as the first surface.
Mode: Operate. A solo lifter, one-handed on a phone between sets, logs a set with one tap.
Constraints: black background with red accents (developer's brief), no new front-end
dependencies, 44px tap targets, numeric keyboards, the front end never recomputes numbers.
Planned behaviour in `UI_IMPROVEMENT_PLAN.md`: exercises open in place (one at a time) and a
workout bar with Resume and End shows on the other tabs.

## Direction contract

THESIS: The workout runs under the gym's wall clock. One big red elapsed clock owns the top of the workout; everything else is plain white on matte black. It refuses the category default of rounded dark cards, badges and red buttons everywhere.

OWN-WORLD: Matte black ground, white ink, one stepped grey ramp, and red only for time and live state: the clock, the open exercise's count, the active tab, the next set. Oswald condensed caps for headings, labels and numbers, Barlow for body text. Square corners, one-pixel hairlines, no cards and no shadows. Square checkboxes that turn white when done.

STORY: The lifter glances at the screen, sees how long they have trained and which exercise is open, finds the next set already filled, ticks it and moves on. On any other tab, a bar with the same red clock brings them back or ends the workout.

FIRST VIEWPORT: A top band holds the red elapsed clock at about 64px Oswald on the left, with the gym, date and start time on the right and a hairline beneath. Exercise rows follow, with caps names and a count on the right. The open row expands its set table (set, kg, reps, done) with + Add set beneath. Closed rows come next, then + Add exercise (outline) and Finish workout (solid white). The tab bar sits at the bottom with the active tab in red. The primary action is ticking the next set's box.

FORM: Wall Clock, position 1 of 7 on the re-rolled grounded list (taken as the pick card). seed:7542006f453092a6

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

Signature: the clock ticks once a second with no animation, and the same clock rides in the workout bar on the other tabs. Motion grammar: state changes are instant; nothing fades or slides.
