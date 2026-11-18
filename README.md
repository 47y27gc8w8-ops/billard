<img src="logo/billard.svg" alt="Billard logo: an 8-ball on green cloth" width="128">

# Billard

Eight-ball for two players at one table, written in JavaFX 21 (Java 21+).

## Run

```
./mvnw javafx:run
```

## Test

```
./mvnw test
```

The rules, physics and turn cycle live in `com.example.billard.model`, which has no JavaFX dependency
and is covered by unit tests.

## Package

```
./mvnw javafx:jlink
```

builds a self-contained runtime image in `target/billard`, started with `target/billard/bin/billard`.

## Logo

The 8-ball logo is drawn in `logo/`: `billard.svg` for 48 px and up, `billard-small.svg` for 24 and 32 px,
and `billard-tiny.svg`, snapped to whole pixels, for 16 px. `logo/billard.ico` is the Windows icon for
installers and shortcuts, and `.idea/icon.svg` is the project icon in IntelliJ. After changing an SVG, run

```
python logo/render_icons.py
```

which uses headless Chrome or Edge to redraw the PNG window icons in
`src/main/resources/com/example/billard/icon/` and the `.ico` (needs Pillow).

## Controls

| Action | Input |
| :--- | :--- |
| Aim | Mouse, or `←` `→` (hold `Shift` for fine steps) |
| Lock / unlock aim | Right click |
| Power | Mouse wheel, `↑` `↓`, or click the power bar |
| Shoot | Left click or `Space` |
| Place the cue ball (after a foul) | Mouse or arrow keys, then click or `Enter` |

## Rules

Simplified standard eight-ball. The first ball potted on the open table decides each player's group
(solids or stripes). Clear your group, then pot the 8-ball to win; pot it early, or foul while potting
it, and you lose. A foul gives your opponent ball in hand: scratching, hitting the wrong ball first,
hitting nothing, or a shot where nothing is potted and no ball reaches a cushion. If the 8-ball drops on
the break it is re-spotted.

Not modelled: spin (English), jump shots, called pockets, kitchen restriction on the break.
