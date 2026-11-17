# Sound effects

All ball sounds are real recordings from [Freesound](https://freesound.org), every one released under
**CC0 1.0 (public domain)**: no attribution is required, but the sources are listed here so each clip can be traced.
Each clip was cut to a single impact that starts right on its transient (no leading silence), filtered against rumble
and hiss, faded out and matched in loudness. The game loads `<kind>_1.wav`, `<kind>_2.wav`, … and picks a take at
random, so more takes can be dropped in without code changes (16-bit PCM, 44.1 kHz, mono or stereo).

| Files | What | Freesound source (author) |
| :--- | :--- | :--- |
| `cue_1`–`cue_4` | Cue tip striking the cue ball | [675330](https://freesound.org/s/675330/) "Billiards cue stick hits ball" (craigsmith) |
| `ball_soft_1` | Light ball-on-ball tap | [655354](https://freesound.org/s/655354/) "Hitting Pool Ball" (jadevelloza) |
| `ball_soft_2`, `ball_soft_3` | Light ball-on-ball taps | [655629](https://freesound.org/s/655629/) "pool ball hits pool ball" (Hankof) |
| `ball_hard_1` | Ball-on-ball clack | [539854](https://freesound.org/s/539854/) "billiard ball clack" (Za-Games) |
| `ball_hard_2` | Ball-on-ball clack | [655346](https://freesound.org/s/655346/) "Ball being hit by white ball, then being sunk" (jadevelloza) |
| `ball_hard_3` | Ball-on-ball clack | [655354](https://freesound.org/s/655354/) "Hitting Pool Ball" (jadevelloza) |
| `ball_hard_4` | Ball-on-ball clack | [481911](https://freesound.org/s/481911/) "Pool Game" (craigsmith) |
| `cushion_1` | Ball bouncing off a rail | [42364](https://freesound.org/s/42364/) "pool-ball-bounce-off-rail" (mccarthy@bedmas.com) |
| `cushion_2` | Ball thudding into a rail | [481911](https://freesound.org/s/481911/) "Pool Game" (craigsmith) |
| `cushion_3` | Ball thudding into a cushion | [365525](https://freesound.org/s/365525/) "Potting snooker balls" (Caitlin_100) |
| `pot_1`, `pot_2` | Ball dropping into a pocket and rolling away | [763601](https://freesound.org/s/763601/) "Pool Ball Sinking" (ChloePieterse) |
| `pot_3` | Ball falling into a pocket | [441857](https://freesound.org/s/441857/) "B_1 pool ball falling" (Yarmonics / Stuart Bowditch) |
| `break_1` | Rack break | [766411](https://freesound.org/s/766411/) "Pool Break" (DariusOW) |
| `break_2` | Rack break | [204187](https://freesound.org/s/204187/) "pool_break.wav" (reg7783) |

`success_1` (the game-over fanfare) is not a ball sound and still comes from the `dist/sounds` folder of
https://github.com/tailuge/billiards (GNU General Public License v3.0). Check that licence before distributing the
game, or replace the file.
