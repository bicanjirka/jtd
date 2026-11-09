# Feature Request: Wave checkpoints

## Where we are

Refined over two rounds. Every question is settled, and it is ready for planning. The decision behind it is
recorded in [ADR 0021](../adr/0021-a-wave-starts-from-its-save.md). ADR 0011 becomes "Superseded by
0021" when this ships.

## Summary

Every wave ends clean and starts from a known state, so the game between two waves is fully described by
its save. The game keeps that save in memory as a checkpoint. A player can restart the last wave from the
header, retry the wave that lost them the level, or replay the wave that won it, without the dev panel.
The balance tools and headless players can load a checkpoint and play on exactly as if they had played
the level up to it. Anything bought and not yet used in a live wave sells back at full price, without a
question.

## Current state (what exists today)

**Does a save and load give the same game today? No, in four ways:**

1. **Fight state crosses the wave boundary and the save drops it.** Per tower, it lasts past the last
   enemy's death:
   - the fire clock's banked charge;
   - the shot count that every-Nth-shot procs read (`shotsBegun`);
   - the Sniper's Aim stacks and target, and its crit streak;
   - the Seeker's missile stock, launch gap, salvo and target streak;
   - Thunderclap's armed flag;
   - the Mortar's bracket marker;
   - the sonar beam's angle and ping memory;
   - turret aim;
   - timed statuses enemies put on towers.

   Zones that last until the wave ends are removed at the clear, but timed zones (Cinder, Mortar, Splash
   ground) live on into the next wave. ADR 0011 recorded what dropping this state costs: rehearsing from
   a save mispredicted rounds, and on eight Curly Path seeds the margin fell from 99% to 90%.
2. **Ticks run between waves, so idle time changes the state.** A Seeker restocks, Aim stacks fade,
   timed zones age and beams turn while the player is building.
3. **The randomness isn't saved.** The base game draws from the unseeded shared `RandomSource`. Only the
   balance tools seed it.
4. **Things the player chose or saw aren't saved:**
   - an Obelisk's Ley Line pin (set by a click);
   - the round history the waves panel shows (rounds leaked, clear bonuses paid);
   - the per-wave damage tally.

   The save does keep each tower's type, cell, upgrades in purchase order and tally (XP, deeds, damage,
   kills), plus the economy (credits, score, lives) and the next wave.

Also relevant:

- **Saves are dev-panel only.** There is one file per level in `saves/`. Saving is refused while a round
  runs. Loading rebuilds the towers through placement in build order, so the rising price of each extra
  copy replays.
- **Waves never overlap.** The next wave can start only once the board is clear. Enemies the dev panel
  spawns between waves hold the next wave back until they're gone.
- **Refunds are flat.** Selling and downgrading refund 75% (`SellRefund.SHARE`) of what was paid, at the
  price paid. While free build is on, refunds pay nothing. A downgrade asks first through a
  `ConfirmPopup`; selling asks nothing.
- **There is no player-facing restart.**
  - The header reads: the title label, the Menu button (`M`, which asks to abandon the level), the status
    icons, then the speed buttons.
  - The win and lose banners offer only "Back to menu".
  - The dev panel has Restart (the whole level), skip wave, jump to wave, save and load, all on Alt keys.
- **Plain keys in use:** Space, D, F, G, Del, M, Enter, Esc, Q to I for towers, and 1 to 0 for upgrades.
- **The balance tools already start from saves.** `WaveCheckReport --save-before` / `--from` do. The
  strong player's rehearsals replay from the first tick because saves mispredict.

## What this feature adds

1. **A clean wave end.** When the last enemy of a wave dies, everything the wave left behind ends:
   - zones on the ground;
   - projectiles in flight;
   - each tower's fight state;
   - statuses enemies put on towers.

   It ends visibly but briefly: shells land, zones fade, and turrets and the sonar beam turn to their rest
   pose. The settle is only how it looks, and nothing in it changes the game. The game is between waves
   from the last death: building, selling and sending the next wave work at once, and sending cuts the
   settle short.
2. **Every wave starts with every tower ready.**
   - No stacks, streaks or armed procs.
   - Shot counts start at zero, every Seeker's stock is full, and the fire clock is ready to shoot.
   - Turrets and the sonar beam start in the rest pose: facing the nearest path point in reach, of any
     path. That pose is fixed per cell, so it needs no saving.

   "Ready" means the state a tower reaches by waiting with nothing in reach. It never depends on how long
   the player waited.
3. **A checkpoint in memory, taken the moment a wave is sent.** It holds the towers and upgrades bought
   in the break before it, recorded as they were just before sending, still unused. It holds everything
   the player chose and sees between waves:
   - towers, upgrades, tallies and XP;
   - Ley Line pins;
   - credits, score and lives;
   - the waves panel's round history;
   - the game's luck.

   Each game draws its own seed when the level starts, and the checkpoint carries it.
4. **Restart wave.**
   - **Header:** it loses its title label, and its left side becomes `[Menu M][↺ Z]`. ↺ is a glyph
     `HudButton` with a key chip for `Z`.
   - **Tooltip:** "Restart wave N - back to how it was sent".
   - **What it does:** ↺ or `Z` pauses the game and asks through a `ConfirmPopup` (Enter restarts, Esc
     cancels). Cancel restores the speed. Restarting puts the checkpoint back.
   - **When:** it restarts the last wave sent, from the moment it was sent until the next wave is sent.
     That covers the break after a clear and the end of the level, won or lost, but not before wave 1,
     when no checkpoint exists.
   - **What a restart undoes and keeps:** a tower or upgrade bought after the checkpoint goes, and its
     money comes back. A used tower sold during the break before the checkpoint stays sold.
5. **Retry and replay on the banners.** The lose banner offers "Retry wave N" and the win banner "Replay
   wave N", each beside "Back to menu". Both do what ↺ does.
6. **Full refund for unused purchases.**
   - **When it applies:** a tower or upgrade node is used once it was owned while a wave ran. One bought
     mid-wave is used at once. Until then, selling or downgrading it refunds 100% of its price; after,
     the 75% rule applies. It is judged per tower and per upgrade node, so a used tower can carry unused
     nodes.
   - **No question:** a full refund asks nothing. A downgrade that removes only unused nodes skips the
     confirmation. A mixed one still asks: removing a used node takes the unused nodes above it with
     it.
   - **The Sell button** reads "Sell ( $120 · full )" when the whole tower refunds in full, and otherwise
     the real amount. Its tooltip says why.
   - **Downgrade wording** (a right-click on an owned node in the upgrade tree):
     - the footer ends in "· full" when every node going is unused;
     - the hover lists a "Refund 100%" row for unused nodes, beside "Refund 75%" for used ones, each with
       its amount.
   - **The mark is a bare `$`:**
     - **Colour:** the cash grey `#CDCDD1` with a thin dark outline. It is the credits icon's colour, and
       good news takes no hue.
     - **Shape:** drawn from the credits icon's own `$` stroke (`status.cash`), never a font character, so
       it looks the same on every OS.
     - **On the board:** on any tower holding something unused, in its top-right corner, until the next
       wave is sent. It shares that spot with the disruption diamond, which only shows during a wave.
     - **In the upgrade tree:** on every unused owned node, in the sealed badge's spot (owned nodes never
       show that badge), with no disc behind it.
     - **In the tooltips:** the same `$` marks the "Refund 100%" row.
     - **Considered and rejected:** a speech bubble (it doesn't say money), a `$` chip (shaped like the
       tree's key chips), the credits coin (a dot at 1:1) and gold (it means score, and merges with Head
       nodes, the Obelisk's star and honey lanes).
7. **The dev panel uses the same checkpoint.**
   - Save never refuses. Between waves it writes the board as it is now ("Saved the board before wave
     8"). During a wave it writes the checkpoint ("Saved wave 7 as it was sent").
   - The playtest rules (free build, infinite lives, gates ignored) are session settings and are not
     saved.
   - Load, skip wave and jump to wave all leave a clean, ready board, and the next send takes a fresh
     checkpoint.
8. **The checkpoint is complete.** Loading it and playing the wave with the same inputs gives the same
   play as having played on to it, tick for tick. A restart replays the same luck: the same build and the
   same clicks give the same result. The balance tools and headless players can start a test at any wave,
   for example the last boss, without replaying the level.

Out of scope for v1:

- ❌ a save during a wave;
- ❌ the base game's checkpoint on disk (quitting loses it);
- ❌ rewinding more than one wave;
- ❌ a cost or count for restarts (restarting is free and unlimited);
- ❌ switching the strong player's rehearsals to start from checkpoints. That is a follow-up this
  feature makes possible.

## Interconnections

- **Supersedes ADR 0011's consequences:** "a save is for playing on, not for reproducing a play tick for
  tick" and "no fight state, no randomness". 0011's rejection of full-state snapshots still holds.
- **Tower visual revamp** (`FEATURE-tower-visual-revamp.md`): it animates turret heads. Their rest pose
  facing the path, and the turn to it at the wave's end, are poses that revamp must draw.

## Constraints and open risks

- **Balance moves.** Whatever value carried from one wave into the next is gone: a mine laid late in a
  wave, Aim stacks built on its last enemy. A Seeker that started a wave half-empty now starts it full,
  and a turret facing the path loses less time on its first shot. The numbers need a
  `BalanceReport --compare`, run once the user asks, since routine runs wait for the revamp.
- **Anything a click sets on a tower must survive a load**, today and in future towers. The Ley Line pin
  is the one now.
- **A future tower mechanic that accumulates must end at the wave's end, or be a player choice the save
  keeps** (ADR 0021).
- **A restart leaves nothing behind.** The wave's XP, kills, damage, bounty, clear bonus and score are
  rolled back with the rest, so nothing counts twice. A replay after a win undoes the win.
- **Restart and replay tear the level down.** They go through the loop's stop-then path and are EDT
  requests the next tick applies (ADR 0019).
- **New controls follow the UI rules.** They are `HudButton`s, questions go through `ConfirmPopup`, chips
  show the real binding, and they look the same on every OS. Removing the header's title label touches
  the level's voice colour, which the title takes today.
- **Cost:** wave-end cleanup is a one-off at the clear, and the board mark sits inside the frame budget.
- **A wave can be scouted:** start it, watch, restart and build for it. The waves panel already shows
  what's coming, so this changes little in a single-player game.
- **Left to planning:** whether a wave's luck depends on how the wave before it went, and how long the
  settle runs and on which clock. Either way, a restart replays the same luck and the settle changes
  nothing.

## Decisions made

- ✅ No save during a wave. All fight state, zones and timed effects end with the wave. Towers start every
  wave fully ready.
- ✅ The base game checkpoints automatically, in memory, the moment a wave is sent (break builds
  included, recorded unused). The checkpoint is fully replayable, and the balance tools and headless
  players rely on it.
- ✅ "Used" means owned while a wave ran, per tower and per node. Unused purchases refund 100% with no
  question, except a mixed downgrade, which still asks. The mark is a bare `$` in cash grey: on the board
  in the tower's top-right corner, on unused upgrade nodes in the badge spot, and in the refund rows. The
  Sell button says "· full".
- ✅ A restart replays the same luck. Each game has its own seed.
- ✅ Restarting is free and unlimited.
- ✅ Turrets and the sonar beam rest facing the nearest path point in reach.
- ✅ The header becomes `[Menu M][↺ Z]` with no title label. ↺ asks first and works from the first send
  onwards, including breaks and a finished level. The banners offer "Retry wave N" (lose) and "Replay
  wave N" (win).
- ✅ The dev Save writes the board as it is between waves, and the checkpoint during a wave. Playtest rules
  are not saved.
- ✅ The player acts as soon as the last enemy dies. The settle is cosmetic.
- ❌ P as a pause key: not in this feature (a feature idea in `TODO.md`).

## Open questions

None.

---

*After planning and implementation, update this document rather than deleting it: mark it implemented,
prune resolved open questions, and either promote deferred scope to a new request or note it's still
wanted for a later version.*
