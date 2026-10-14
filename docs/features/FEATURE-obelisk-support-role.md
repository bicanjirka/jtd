# Feature Request: The Obelisk as a Support Tower

**Status: draft.** Comes after `FEATURE-balance-verification.md` (first version implemented), whose
tools found the problem and will check the fix.

## Summary

Turn the Obelisk from the game's dominant damage dealer into a support tower, the mirror of the Beacon:
the Beacon makes towers stronger, the Obelisk makes enemies weaker. Its field keeps a small, fixed damage,
the Beacon can still raise it, and every upgrade path makes the field do something to the enemies inside
it (silence, slow, strip armor, make them take more damage, reveal them) instead of making it hit harder.
The Obelisk should be the tower that makes the others better, not the tower that makes the others
unnecessary.

## Current state (what exists today)

**What the Obelisk is.** `PulseTower`, shown as the Obelisk: 25 credits, a 1.5-cell field that deals
2 points of magic damage every tick (40 a second) to every enemy inside, invisible ones included, with no
aim, so nothing dodges it. Overlapping fields simply add up.

**What the balance report measured** (`BalanceReport --seeds 8 --variety`, 2026-10-13, competent
player; see `FEATURE-balance-verification.md`, Findings):

- It lands 46% to 59% of all damage on every built-in level. An army of Obelisks alone holds 221%,
  221% and 178% of the authored enemy health, above the 100% to 150% band the full mixed army is meant
  to sit in; no other tower alone wins Curly Path or Twisted Hourglass at any enemy health.
- Banning it costs x0.69 on Curly and x0.85 on Zigzag, and makes Twisted Hourglass unwinnable as authored
  (x0.06).
- It is not the most efficient tower: at its best cell it lands 99 to 105 damage per credit in a probe
  round, the Burst 113 to 121. What it has is **no bad matchup**: against every kind these levels send it
  brings down at least 0.43 of what it does against a plain mob, while every other tower meets a kind it
  does 0.00 to 0.09 against.
- **The Elite Ghost makes it mandatory.** From Elite, a Ghost makes every ally within about 3 cells
  invisible, refreshed every second, and Ghosts shroud each other, so a group with two is invisible for
  good. Every tower that needs a visible target goes idle; only the field keeps working. Removing the
  shroud (as an experiment) cut the Obelisk's share to 28% to 54% and the cost of banning it to x0.89 to
  x0.96.
- **It grows by damage.** Attune's Toll adds 10% field damage per stack; Overcharged Coils I and II add 60%
  and 50% (lab x1.47 and x1.21 to x1.28); Arc Discharge, Meltdown and Tesla Coil add crits and zaps; the
  Range nodes and Phase Field I and II widen the field, which for a field is damage (Range III x1.75 on
  Curly). Event Horizon and Soul Drain add damage on conditions.
- **Its other half is already support:** Null Field (silence), Dead Zone (no heals take hold), Corrosion
  (-30 armor), Undertow (a slow that counts as chill), Mirror Field (shields reflected), Rattle Field,
  Kill Zone (+25% damage taken from every source), True Sight, and Phase Field II's reveal on entry.
- **The Beacon helps it most:** +49% to +58% in the lab, against +11% to +38% for most towers, because
  +20% range on a 1.5-cell field covers a lot more path.
- **Experiments already run** (temporary edits, all reverted; results in `target/balance/ob-*.tsv` on the
  machine that ran them):

  | Change | Obelisk's share (Curly / Zigzag / Hourglass) | Ban cost | Obelisk alone |
  |---|---|---|---|
  | none | 55 / 59 / 46% | x0.69 / x0.85 / x0.06 | 221 / 221 / 178% |
  | Burst, Mortar, Scorcher and Radar may aim at hidden enemies | 34 / 54 / 32% | x0.91 / x0.81 / x1.01 | unchanged |
  | the same, and overlapping fields land at half | 33 / 54 / 32% | x1.03 / x0.84 / x1.01 | 184 / 198 / 138% |
  | -25% field damage | 41 / 51 / 52% | x0.81 / x0.96 / x0.06 | 172 / 168 / 168% |
  | Jammers in late rounds (with aiming at hidden) | 29 / 43 / 21% | x0.88 / x0.82 / x0.99 | 221 / 217 / 146% |

  A plain damage cut fixes neither the dependence nor the stacking, and hurts the naive player most
  (its Curly margin 88% to 61%).

**What the tools cannot see yet.** The competent player values a tower by the damage it lands, and the
lab measures a tower's nodes only by what that tower itself lands (the Beacon is the one exception: its
nodes are measured on partner towers beside it). A support Obelisk would read as worthless to both, and
the report would call it dead. `TODO.md`, "What the balance tools cannot see yet", records this gap.

## What this feature adds

1. **The tools learn to price support.** Before the Obelisk changes, the lab measures what a tower does
   for a partner tower beside it, and the competent player counts it, the way the Beacon's buff is already
   counted. Without this the redesign cannot be checked, only guessed. Check it on today's game first:
   Kill Zone, Corrosion and Undertow should start to show a worth they do not show now. Built on the
   balance-verification branch, before this feature starts.
2. **A fixed field.** The field's base damage drops to about 1 a tick at the same price, and no longer grows
   with the Obelisk's own upgrades. The Beacon may still raise it, as it raises every tower.
3. **Every upgrade path is an effect on the enemies inside.** The damage nodes (the Overcharged Coils line,
   Arc Discharge, Meltdown, Tesla Coil, Toll's damage bonus, Event Horizon, Soul Drain) become effects:
   debuffs that make other towers' hits land harder, crowd control, or denial of heals and shields. Toll
   stays the measure of time spent inside and becomes the strength of every effect the field applies: the
   longer an enemy stays inside, the harder each debuff bites. The existing support nodes stay, retuned as
   needed.
4. **The detector role.** The Obelisk reveals hidden enemies inside its field from its base, so the towers
   around it can shoot them. A hidden enemy must be revealed before any tower aims at it, as today; no
   tower gains the right to fire at hidden enemies. Elite Ghost rounds then need an Obelisk *and* damage,
   not an Obelisk *instead of* damage, with the Radar's Wide Band the other answer.
5. **A passive that answers stacking.** Overlapping fields keep adding up. Stacking is answered by an
   Obelisk passive designed with the tree, not by a stacking rule (open question 7).
6. **The levels are made harder to match.** Every built-in level is already far too easy (the report
   suggests enemy health x1.9 to x2.8, or an equivalent), and the detector role makes Elite Ghost rounds
   easier for every army that is not all Obelisks.
7. **Checked with the report.** Each step is compared with the game before it (`--save` / `--compare`,
   with `--variety`).

**Out of scope:** the other towers' trees (the Hive's Absolute Zero is its own finding), new enemies, and
new tower types.

## Interconnections

- **`FEATURE-balance-verification.md`**: its tools measured the problem; its "Later" item 3 (synergy) is
  item 1 here, and its findings are the baseline to compare against.
- **`TODO.md`**: "What the balance tools cannot see yet" (support worth) is closed by item 1; the
  "unbalanced placeholders" entries for upgrade nodes and effects cover the Obelisk's nodes.
- **`FEATURE-pulse-and-seeker.md`** (implemented): where the Obelisk's tree, Toll and field modes were
  designed.
- **`FEATURE-agility-and-dodge.md`** and the Ghost's invisibility: item 4 adds a revealer; which towers
  may aim at a hidden enemy does not change.
- **The Beacon** (`AuraTower`): stays the one tower that raises the field's damage, and buffs the Obelisk
  the way it buffs every tower. A combo between the two is an open idea (open question 8).

## Constraints and open risks

- **The tool must see support before it judges.** If item 1 is skipped, every later measurement will call
  the new Obelisk dead.
- **The Elite Ghost must keep an answer at every step.** Removing the Obelisk's damage before item 4 lands
  makes Twisted Hourglass unwinnable (x0.06 without it today).
- **Fixed damage still stacks.** At 1 a tick, eight Obelisks on a bend still land 160 a second early on;
  until the passive (item 5) answers it, the base number and the price have to be decided together.
- **Effects the enemies already resist.** Freeze has diminishing returns, Elite Armored and the Juggernaut
  have their own rules; a support effect that an enemy ignores is a dead node, and the counter matrix should
  show each new effect's reach.
- **Hidden enemies and targeting rules.** `td/tower/CLAUDE.md` says a tower aims through `visible` and an
  area effect touches `everyone`; that rule stays, and the reveal of item 4 goes through it.
- **Player-visible text.** The tower table, the Invisible and Ghost rows in `README.md`, and every node's
  words change.
- **Standing rules:** effects go through the effect system and node data, never node checks in tower code;
  the engine stays headless.

## Decisions made

- The Obelisk becomes a support tower: its upgrades add effects, not damage; its damage is a fixed base that
  only the Beacon raises.
- The tools learn to price support before the Obelisk changes.
- The Elite Ghost keeps an answer at every step: the Obelisk reveals; a hidden enemy is still revealed
  before any tower aims at it.
- Overlapping fields keep stacking; a passive answers it, not a stacking rule.
- A plain damage cut is not the fix (measured: it neither ends the dependence nor the stacking).
- Each step is checked with the balance report against the step before.

## Open questions

Each has two options and a preferred one (⭐).

1. ✅ **What is the base field damage?**
   - A ⭐: lower it (around 1 a tick) and keep the price; support should not also be the best early damage.
   - B: keep 2 a tick and raise the price.
   - ↳ A.
2. ✅ **How do overlapping fields combine?**
   - A: the first in full, every other at half.
   - B: only the strongest field applies, like two zones of one kind.
   - ↳ Neither: fields keep stacking in full, and an Obelisk passive answers it (question 7).
3. ✅ **Which towers may fire at hidden enemies?**
   - A: the Burst, the Mortar and the Scorcher (their damage already reaches hidden enemies).
   - B: those and the Radar's beam.
   - ↳ None: a hidden enemy is revealed first, as today. The Obelisk's reveal and the Radar's Wide Band are
     the answers.
4. ✅ **What happens to Toll?**
   - A: it stays as time inside and keeps slowing debuffs' clocks; its damage bonus goes.
   - B: it becomes the strength of every effect the field applies.
   - ↳ B.
5. ✅ **Does the Beacon's range buff stay for the Obelisk?**
   - A ⭐: yes, the Beacon raises every tower the same way.
   - B: the Beacon raises its damage only, since range is where the Obelisk gains most (+49% to +58%).
   - ↳ A, with a combo between the two kept as an open idea (question 8).
6. ✅ **In what order do the parts land?**
   - A ⭐: tools (item 1, on the balance-verification branch), then the detector role (4), then the field,
     the tree and the passive (2, 3, 5), then the levels' difficulty (6), each compared with the one before.
   - B: everything at once, compared once.
   - ↳ A.
7. ❔ **What passive answers stacking?**
   - A: a debuff an enemy already carries is refreshed, not strengthened, by a second field, so a cluster
     buys coverage rather than strength.
   - B: an Obelisk weakens its own field for every other Obelisk in reach, so spreading them out pays.
8. ❔ **Is there an Obelisk-Beacon combo?** The Beacon buffs towers and the Obelisk debuffs enemies; a pair
   could do something neither does alone.
   - A: a Beacon in reach of an Obelisk strengthens the field's debuffs instead of its damage.
   - B: none; each buffs or debuffs on its own, and the pair is only the sum.

---

*After planning and implementation, update this document rather than deleting it: mark it implemented, prune
resolved open questions, and either promote deferred scope to a new request or note it is still wanted for a
later version.*
