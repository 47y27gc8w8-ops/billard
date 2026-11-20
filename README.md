<img src="logo/billard.svg" alt="Billard logo: an 8-ball on green cloth" width="128">

# Billard

Eight-ball against a computer opponent, a second player at the same screen, or a friend online, written in
JavaFX 21 (Java 21+).

**Version 1.3.0.** See [CHANGELOG.md](CHANGELOG.md) for what each release added.

## Features

- Play against a bot at one of four levels, from Easy to a Boss that never misses, or two players taking turns
  on one screen
- Online matches against a player on another computer: host and join by address, or meet through a relay
  server by room code; live aim, quick chat, rematches and automatic reconnecting
- Eight-ball rules with groups, fouls and ball in hand
- Ball physics with rolling, sliding and cushion bounces, plus cue-ball spin (draw, follow and side)
- Aim lock, a guide line and a power bar for precise shots
- A realistic table: cloth texture, wood rails, balls that visibly roll and drop into the pockets
- Recorded ball, cushion and pocket sounds, and lounge jazz background music, each with its own volume
- Full-screen mode and a window that scales from small laptops up to large monitors

## Opponents

Pick an opponent on the title screen:

- **Play vs bot**: pick a level, then play as Player 1 and break; the bot plays as Player 2, named after its
  level (for example "Hard bot"). On its turn it thinks for a moment, places the cue ball if it has ball in hand,
  swings the cue onto its line and shoots. While it plays, the table, the power bar and the spin button ignore
  input; Settings, Menu and New match keep working.
- **Two players**: two people take turns at the same mouse and keyboard, as Player 1 and Player 2.
- **Play online**: play against someone on another computer; see [Online play](#online-play).

**New match** and **Play again** keep the opponent and level you chose; go back to the menu to switch.

### Bot levels

| Level | Plays like | Measured in bot-against-itself matches |
| :--- | :--- | :--- |
| Easy | Still learning: misses most pots and never plays safe. You would have to try to lose. | Keeps the table on about 1 shot in 6, around 25 fouls a match |
| Medium | A casual player: pots the easy ones, misses plenty. Tough for beginners, beatable for regulars. | About 1 shot in 3, around 3 fouls a match |
| Hard | A strong player: pots most shots and plays for position, but misses now and then. | About 3 shots in 5, rarely fouls |
| Boss | Never misses: every shot is played exactly as planned. Give it one turn and the match is usually over. | Cleared the table on its first visit in 10 of 12 test racks, never fouled |

The levels differ in three ways, set in `BotLevel`: how far the bot's aim and power stray from the planned shot,
how much it cares about the position it leaves (Easy not at all), and how many shots it tries. Easy, Medium and
Hard try a few hundred; the Boss tries a few thousand, including banks, kicks and spin for position. When the Boss
finds no pot at all it plays a safety, so your only chances against it are the break and those rare safeties.

### How the bot plays

The bot (`ShotPlanner` in `com.example.billard.model`) tries its shots out before playing one, at every level. It lists pots of
every ball it may legally hit into every pocket, at several powers and with centre, draw or follow spin, plus a
ring of plain shots all round the cue ball for when no pot is on. With ball in hand it also tries spots straight
behind each of its balls. Each candidate is played forward on a copy of the table with the game's own physics and
judged by the game's own rules, so it sees scratches, fouls and an early 8-ball exactly as the match would. It
prefers shots that win, then shots that keep the table with an easy next shot, then safeties that leave you
nothing easy. The best few are checked again with the aim slightly off, so it favours shots with room for error.
Finally its aim and power get a random error that depends on its level. The Boss gets none, and since the
simulation uses the same fixed-step physics as the game, its shot ends exactly as it planned.

Planning runs on background threads from a snapshot of the table, so the game never stutters while the bot
thinks: typically well under a tenth of a second, and under a second for the Boss.

## Online play

Choose **Play online**, type the name your opponent will see, and pick how the two computers meet:

- **Directly, by address.** One player presses **Host a match**; the game shows this computer's addresses
  (port 47474). The other types one of them under **Or join a match** and presses **Join**. This works at once on
  the same network. Over the internet the host has to forward TCP port 47474 on their router, or both players
  join the same VPN (for example Tailscale) and use its address.
- **Through a relay server, by room code.** Both players type the relay server's address. The host gets a
  four-character room code such as `K7QM` and passes it on; the other player joins with it. Nobody needs to touch
  their router, but someone has to run the relay (see below).

The host breaks in the first match, and the break alternates with every rematch. While your opponent plays you see
their cue move live and the table ignores your input. **Chat** sends one of a few preset messages. After the match,
**Play again** asks for a rematch, which starts once both players have asked. If the connection drops, both games
keep trying to reconnect for up to a minute and then carry on where they were; **Menu** leaves the match and tells
your opponent.

### How the two games stay in step

Only the shooter's decisions travel: where they placed the cue ball, and the shot itself (direction, power and
where the tip meets the ball). Both computers then roll the same shot with the same physics. Since floating-point
maths can differ in the last digits between machines, the shooter's computer sends its final table once its balls
stop, and the other side takes it over; until then the other player cannot act. Messages that must arrive are
numbered and sent again after a reconnect; live aim is simply replaced by the next update.

Everything received is checked before use: lines are capped at 4 KiB, every field is parsed against an expected
type and range (no NaN or infinite numbers, shot power 0 to 1, known names only), and player names are limited to
16 letters, digits and simple punctuation. Chat uses preset phrases only, so no typed text reaches the other screen.
A table state is accepted only right after the other player's own shot.

### Running a relay server

The relay is part of the game's code but needs no JavaFX. Build the game, then on any machine both players can
reach:

```
./mvnw compile
java -cp target/classes com.example.billard.net.RelayServer 47475
```

The port defaults to 47475. The relay only pairs players by room code and passes their lines on; it never reads
the game. Each player gets a secret for their seat, so after a dropped connection only they can take it back.
Empty rooms are removed after two minutes, and at most 500 rooms exist at once. Traffic is not encrypted, so run
it for friends rather than as a public service.

## Run

```
./mvnw javafx:run
```

## Test

```
./mvnw test
```

The rules, physics, turn cycle and the bot's shot planner live in `com.example.billard.model`, which has no
JavaFX dependency and is covered by unit tests. Further tests guard the UI state, the audio mixer, and the source files
themselves (ASCII only, at most 120 columns per line).

## Package

```
./mvnw javafx:jlink
```

builds a self-contained runtime image in `target/billard`, started with `target/billard/bin/billard`.

## Controls

| Action | Input |
| :--- | :--- |
| Aim | Mouse, or `←` `→` (hold `Shift` for fine steps) |
| Lock / unlock aim | Right click |
| Power | Mouse wheel, `↑` `↓`, or click the power bar |
| Spin | `Spin` button, then click or drag on the cue ball, or use the arrow keys; `Centre` resets it |
| Shoot | Left click, `Space` or `Enter` |
| Place the cue ball (after a foul) | Mouse or arrow keys, then click or `Enter` |
| Full screen on / off | `F` |
| Close the spin picker or settings | `Esc` |

Against the bot these controls work on your turns only.

## Rules

Simplified standard eight-ball. The first ball potted on the open table decides each player's group
(solids or stripes). Clear your group, then pot the 8-ball to win; pot it early, or foul while potting
it, and you lose. A foul gives your opponent ball in hand: scratching, hitting the wrong ball first,
hitting nothing, or a shot where nothing is potted and no ball reaches a cushion. If the 8-ball drops on
the break it is re-spotted.

Not modelled: jump shots, called pockets, kitchen restriction on the break.

## Settings

Sound effect and music volumes are set on the Settings page (from the menu or during a match) and are
remembered between sessions through Java Preferences.

## Logo

The 8-ball logo is drawn in `logo/`: `billard.svg` for 48 px and up, `billard-small.svg` for 24 and 32 px,
and `billard-tiny.svg`, snapped to whole pixels, for 16 px. `logo/billard.ico` is the Windows icon for
installers and shortcuts, and `.idea/icon.svg` is the project icon in IntelliJ. After changing an SVG, run

```
python logo/render_icons.py
```

which uses headless Chrome or Edge to redraw the PNG window icons in
`src/main/resources/com/example/billard/icon/` and the `.ico` (needs Pillow).

## Credits

- Music: five lounge jazz tracks by Kevin MacLeod (incompetech.com), licensed under Creative Commons
  Attribution 4.0. Details and the changes made are in
  [`music/README.md`](src/main/resources/com/example/billard/music/README.md).
- Sound effects: CC0 recordings from Freesound, listed per file in
  [`sounds/README.md`](src/main/resources/com/example/billard/sounds/README.md). No game-over fanfare
  ships yet; drop a `success_1.wav` into that folder to add one.

## Licence

The code is released under the [MIT License](LICENSE). The bundled music and sounds keep their own
licences, listed above and in `LICENSE`.
