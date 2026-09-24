# Pending `run-jtd` checks (enemy stats)

Deferred visual and balance checks from implementing `docs/plans/enemy-stats.md`, to run in one
batch. Each entry says what to do and what should be true.

## 1. Phase 1 - balance sanity after the stat core (`td.BalanceHarness`)

- Run `td.BalanceHarness` on one built-in level (e.g. Curly Path) with a mixed loadout.
- Expect: numbers in a sensible range, not equal to before. Armored Elite/Boss now apply armor
  before plating, and a shield multiplies with armor, so they take slightly different damage.

## 2. Phase 2 - crit rolled by the target (`td.BalanceHarness` + board)

- Run `td.BalanceHarness` on the same level and loadout as entry 1.
- Expect: Sniper still crits at about its crit chance; Cinder-burning targets crit about twice as
  often; Armored and Frenzied Boss never show a crit spark.
- On the board: a Sniper crit still draws its crit beam at the moment it fires.

## 3. Phase 3 - effect resistance and freeze diminishing returns

- Run `td.BalanceHarness` on a level with Seeker towers against Elite/Boss waves (e.g. Curly
  Path's later waves). Expect: Elite/Boss freezes shorten 100/50/25% then stop within 10 s.
- On the board: debug-spawn an Elite `c`, freeze it with Seekers repeatedly; check the freeze
  marker disappears sooner each time. The Warden egg (burn/freeze immune) still shows its immune
  glyphs; any `EffectResistTrait` below 1 shows the new resist glyph.

## 4. Phase 4 - disruption (screenshot)

- Start any level, place a few towers near the path's start, and debug-spawn the Jammer (`j`,
  cycle the debug spawn to it). Screenshot while it walks past the towers.
- Expect: a pink ring (100 px radius) around the Jammer; every tower whose centre is inside it
  shows a small pink diamond in its top-right corner, and its range circle (when selected) is
  20% smaller; both clear once the Jammer passes or dies.

## 5. Phase 5 - wave-preview stat block (screenshot)

- Start Curly Path (or any level with Armored and Elite/Boss waves), hover each mob in the wave
  preview strip, and screenshot the info panel.
- Expect: the panel shows name, description, the Rank/Health/Bounty line, then `Speed N px/s`,
  and for Armored `Armor 25 (-20% physical)`, `Magic resist 25 (-20% magic)`,
  `Resilience 100 (immune to crits)` and a `Traits:` list; an Elite Simple shows
  `- Adaptive: up to 67 armor or magic resist, depending on your damage mix` and
  `Freeze diminishing returns`. Check the text fits the panel (no clipping) in the Hud style.

## 6. Phase 6 - live enemy inspector (screenshots)

- Start a level, start a wave, and click a moving enemy (not while placing a tower).
  Expect: a white selection ring around it; the side panel shows `Name (Rank)`,
  `Health: cur / max`, the stat lines, `Effects:` with time left (e.g. `- Frozen 1.2s` after a
  Seeker hit) and `Traits:`.
- Pause (`p`) and screenshot twice a second apart: the text must still be shown (live while
  paused).
- Let a tower kill it: the ring disappears and the panel keeps its last state with `Killed`.
  Let one leak: `Leaked`.
- Click a tower: the enemy ring and text go away and the tower's status shows. Click the enemy
  again: the tower deselects. Hover an upgrade node while inspecting a selected tower: the hover
  text still takes over the panel.
- Try to click a Ghost while invisible: nothing gets selected. Select a Warden egg, let it hatch:
  the selection clears.
