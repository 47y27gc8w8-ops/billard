R# Code review 2026-11-18: Billard against the vault coding regulations

Reviewed: all of `src/main/java` (model, ui, audio, app) at commit `46d8446`.
Standards: Clean Code Overview, Clean Code Quick Reference, Clean Code - Functions, Clean Code - Code Smells,
SOLID Overview, SOLID Code Review Checklist, Performance (Second Brain, `02 - Coding Regulations`).
Priority order (Coding Regulations Research Overview): correctness first, then clear behaviour, simplicity,
testability, deduplication.

Baseline before any change: 106 tests, all passing.

Each thread is a finding. It goes from **Open** to **Resolved** once fixed and checked.

**Result:** all 7 resolved. 110 tests pass (106 + 4 new). Before and after were also compared with four test agents:
- **Pixel diff** of about 146 renders per build: identical apart from the intended T1 change.
- **Scripted interactions** and a real-app smoke run: no regressions.
- **Frame-time and allocation profiling:** no regression.
- **Independent review:** its 4 low-severity findings are fixed. They were import order, a dangling Javadoc, the error path in the new test and NaN handling in `bounceAlong`.

---

## T1: Player card can stay highlighted after the turn passes (correctness)

**Status:** Resolved. `PlayerCard` toggles the `:shooting` pseudo-class (`style.css` updated). `PlayerCardTest` covers the turn passing after changes, and checks that the stylesheet uses the same state. Verified by the behaviour and visual tests: on the baseline, both cards stayed lit after a miss and at game over. Now only the shooter's card is lit.
**Where:** `ui/PlayerCard.java:58-62`
**Rule:** Correctness comes first. Clean Code - Functions: "No side effects (or make them explicit)".

`update` calls `getStyleClass().add("active")` every time the view changes while the player is active.
`ObservableList` keeps duplicates. The view changes while the player is still shooting: a ball drops, or the
group is assigned. `remove("active")` only takes out one copy, so the card stays styled `.active`. After a pot on
the break, both cards can show the blue turn marker.

**Fix:** Toggle a CSS pseudo-class. That operation is idempotent by design. Add a regression test.

---

## T2: GameScreen has several reasons to change (SRP)

**Status:** Resolved. `GameOverOverlay` and `SettingsOverlay` were extracted with the same node tree. GameScreen went from 292 to 230 lines. The behaviour test confirmed open/close, Escape, backdrop click, focus handling and the single fanfare.
**Where:** `ui/GameScreen.java` (292 lines)
**Rule:** SOLID - SRP: "Could you describe its purpose in a single sentence without using 'and'?"
Clean Code: classes small.

GameScreen does four jobs:
- it lays out the bars,
- it runs the frame loop,
- it refreshes the HUD,
- it builds and drives two overlays (game over, settings) with their own labels, focus and key handling.

**Fix:** Move each overlay into its own class (`GameOverOverlay`, `SettingsOverlay`) with the same node tree.
GameScreen keeps layout, loop and HUD.

---

## T3: Ball look is defined twice (DRY)

**Status:** Resolved. `BallPainter` holds the ball-geometry constants and `clipToCircle`; `RollingBallPainter` and `PocketDrops` use them. Pixel-identical.
**Where:** `ui/BallPainter.java:15-17`, `ui/RollingBallPainter.java:24-28`; circle clip in
`BallPainter.java:70-73`, `RollingBallPainter.java:142-145`, `PocketDrops.java:50-53`
**Rule:** Clean Code - DRY; Code Smells: duplicate code.

`STRIPE_HALF_HEIGHT`, `NUMBER_DISC` and `NUMBER_SIZE` are copied between the flat painter (menu, potted strip)
and the rolling painter (table). If someone tunes one copy, a ball on the table stops matching the same ball in
the menu. The "clip to a circle" path sequence appears three times.

**Fix:** Keep the constants once, in `BallPainter`, and have `RollingBallPainter` use them. Add
`BallPainter.clipToCircle` and use it at all three sites.

---

## T4: Long parameter lists in TableRenderer (function arguments)

**Status:** Resolved. Uses `drawRail(gc, wood, edge)`, the `Cushion`/`Jaw`/`Polygon` records and the `point(edge, along, depth)` helper. 0 differing pixels across about 146 renders per build.
**Where:** `ui/TableRenderer.java:111` `drawRail` (8 parameters), `:198` `drawCushion` (7),
`Edge.toTable` (4, with `width, height` passed at every call)
**Rule:** Clean Code - Functions: "Few arguments (0-2 ideal, avoid >3)". "Replace clusters that represent one
concept with a value object."

Each rail is described by eight loose numbers that repeat the rail geometry. `Edge` already knows how to place a
point along an edge.

**Fix:** Derive each rail's outline and profile from its `Edge`, so the call is `drawRail(gc, wood, edge)`. Group the
cushion span into a small `Cushion` record. Add a `point(edge, along, depth)` helper so `width, height` are no longer
passed at every call.

---

## T5: GameCanvas repeats the layer-painting ritual and uses a flag argument

**Status:** Resolved. `paintLayer(layer, viewport, painter)` and `nudgeHorizontally(direction, aimStep)` are in. The ball layer's canvas command stream is byte-identical over a full break.
**Where:** `ui/GameCanvas.java:62-100` (draw and redrawSurfaceIfResized), `:171` `nudge(int, boolean fine)`
**Rule:** Clean Code - DRY; Functions: "No flag arguments".

Both layers repeat the same sequence: reset the transform, clear, save, apply the viewport, paint, restore.
`nudge(direction, fine)` takes a boolean that picks between two behaviours.

**Fix:** Add one `paintLayer(layer, viewport, painter)` helper. Pass the aim step instead of a flag.

---

## T6: PhysicsEngine.bounceOffCushions duplicates the per-axis logic (long function)

**Status:** Resolved. `bounceAlong` and the `Bounce` record replace the mirrored halves. A replay of 1,383 seeded shots gives the same fingerprint as before (`c04e28dd2061d49e`). Balls well inside the cushions now return early, so physics allocates 16% less than the baseline (1.29 vs 1.54 MB per simulated second).
**Where:** `model/PhysicsEngine.java:103-139` (37 lines; the x and y halves are mirror copies)
**Rule:** Clean Code - Functions: small, do one thing; Code Smells: duplicate code.

**Fix:** Move the one-wall logic into `bounceAlong(...)`, which returns a small record, and call it once per axis.
The results must stay bit-for-bit the same: check this with a deterministic replay of many shots before and after.

---

## T7: Formatting drifts from the agreed standard

**Status:** Resolved. No line over 120 characters; imports sorted. `SourceLineLengthTest` enforces the limit.
**Where:** 18 lines over 120 characters in 11 files. Imports out of order in `ui/PlayerCard.java`. A redundant
lambda in `ui/SpinPicker.java:44`.
**Rule:** Clean Code Quick Reference - Formatting: "Line length: aim for 80-120 characters", "Agree on team
standards and enforce them".

**Fix:** Wrap the lines and sort the imports. Add `SourceLineLengthTest` so the 120-character limit is enforced the
same way `SourceEncodingTest` enforces ASCII.

---

## Considered and deliberately not opened

- **Per-frame HUD and gradient allocations** (`GameScreen.refreshHud`, `Lighting.*`): a few dozen small objects per
  frame. Performance says "measure first", and nothing shows them as a cost, so they stay as they are.
- **`SettingsPanel` and `GameScreen` call `SoundBank.standard()` directly** (DIP): this is a load-once resource
  inside the UI package. Injecting it would make `SoundBank` public for no current benefit (YAGNI).
- **`PocketDrops` looks for the nearest pocket again**: carrying the pocket through `PhysicsListener` would change a
  shared interface for one consumer.
- **`smoothStep` in two classes**: only two copies, below the rule of three.
