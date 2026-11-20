# Changelog

All notable changes to Billard. Versions follow [Semantic Versioning](https://semver.org).

## [Unreleased]

## [1.3.0] - 2026-11-20

### Added

- Online play. **Play online** on the title screen hosts or joins a match against another computer, either
  directly by address (port 47474) or through a relay server by four-character room code. Both games replay each
  shot, and the shooter's final table settles any difference.
- Live opponent aim, preset quick-chat messages, rematches that start once both players ask, and automatic
  reconnecting for up to a minute after a dropped connection.
- A small relay server (`com.example.billard.net.RelayServer`) that runs without JavaFX.
- How to play and the README explain online play, how the two games stay in step, and how to run a relay.

### Changed

- The table, power bar and spin button also ignore input while the online opponent is playing.
- The title screen gives **Play online** a row of its own between the match buttons and the rest.

### Security

- Everything received from the other game is checked before use: lines are capped at 4 KiB, every field must
  have the expected type and range (no NaN or infinite numbers, shot power from 0 to 1), player names are limited
  to 16 letters, digits and simple punctuation, and a table state is accepted only right after the other
  player's own shot. Relay seats are protected by secrets compared in constant time.

## [1.2.0] - 2026-11-20

### Added

- Play against a bot. The title screen now offers **Play vs bot** and **Two players**; the bot takes Player 2's
  turns, plans each shot by simulating candidates with the game's own physics and rules, and places the cue ball
  when it has ball in hand.
- Four bot levels, picked on a new page after **Play vs bot**: Easy, Medium, Hard and Boss. They differ in aim and
  power error, in how much they play for position, and in how many shots they try; the Boss never misses and
  searches thousands of shots, including banks and kicks.
- How to play and the README explain the opponents, the bot levels and how the bot plays.

### Changed

- The table, power bar and spin button ignore input while the bot is playing.
- **New match** and **Play again** keep the chosen opponent and level.
- The bot is named after its level, for example "Hard bot", on the player card and in the status line.
- The bot sets its final aim exactly, and plans on several threads at once.
- The cue stroke is shared by players and the bot (`Shot`), and the rules expose which balls a player may hit
  first, so the bot and the referee use the same logic.

## [1.0.0] - 2026-11-18

The first full release.

### Added

- Cue-ball spin: pick where the cue strikes the ball for draw, follow and side spin, with matching physics.
- Right-click aim lock, so the power can be set without moving the aim.
- Recorded sound effects for the cue, ball contacts, cushions, pockets and the break, mixed in stereo by
  table position.
- Lounge jazz background music that shuffles five tracks, with separate effect and music volume sliders
  that are remembered between sessions.
- Realistic table: textured cloth, wood rails, ball shadows and highlights, and balls that visibly roll.
- Potted balls roll into the pocket and sink instead of disappearing.
- Full-screen mode, toggled with `F`.
- 8-ball logo as the window icon, on the title screen and as a Windows `.ico`.
- Tests that keep source files ASCII-only and within 120 columns.
- MIT `LICENSE` for the code, with the licences of the bundled music and sounds.

### Changed

- Smoother rendering: the table is drawn once and cached, and ball numbers sit centred on a white disc.
- Code cleaned up after a full review (`docs/code-review-2026-11-18.md`): smaller renderers, overlays split
  out of the game screen, and a simpler cushion-bounce calculation with unchanged results.
- `Esc` now closes the settings and spin overlays instead of leaving full screen.
- How to play now covers spin, ball in hand, `Enter` and full screen, and scrolls on short windows.

### Removed

- The GPL-licensed game-over fanfare (`success_1.wav`). Game over is silent until a new fanfare is added.

### Fixed

- The active player's card stays lit only while it is that player's turn.
- Garbled characters caused by source files saved in the wrong encoding.

## [0.1.0] - 2026-11-10

First playable beta: two-player eight-ball with physics, rules, fouls and ball in hand.

[Unreleased]: https://github.com/47y27gc8w8-ops/billard/compare/v1.3.0...HEAD
[1.3.0]: https://github.com/47y27gc8w8-ops/billard/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/47y27gc8w8-ops/billard/compare/v1.0.0...v1.2.0
[1.0.0]: https://github.com/47y27gc8w8-ops/billard/compare/75184b8...v1.0.0
[0.1.0]: https://github.com/47y27gc8w8-ops/billard/commit/75184b8
