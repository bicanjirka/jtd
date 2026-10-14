# Feature Request: The Obelisk as a Support Tower

**Status: in progress.** Items 1 (the tools price support), 4 (the bought reveal) and 2, 3 and 5 (the field, the tree and
Interference) are implemented; findings below. The levels (item 6) wait on them.

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
4. **The detector role, bought.** No reveal is free. The Elite Ghost's shroud is cast every 3 s and lasts
   3 s, so a reveal holds until the next cast; the Radar's Wide Band and the Obelisk's Phase Field line (a
   short reveal and Daze from Phase Field I) are the answers, each bought. A hidden enemy must be revealed
   before any tower aims at it, as today. Elite Ghost rounds then need a revealer *and* damage.
5. **A passive that answers stacking.** Overlapping fields keep adding up. Stacking is answered by an
   Obelisk passive designed with the tree, not by a stacking rule (open question 7).
6. **Out of this feature** (open question 22): the levels are retuned by wave composition in a feature of their
   own. ~~The levels are made harder to match.~~ Every built-in level is already far too easy (the report
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
- The Elite Ghost keeps an answer at every step; a hidden enemy is still revealed before any tower aims at
  it. No reveal is free: every reveal is bought (how, open questions 17 to 19).
- Overlapping fields keep stacking; a passive answers it, not a stacking rule.
- A plain damage cut is not the fix (measured: it neither ends the dependence nor the stacking).
- Each step is checked with the balance report against the step before.

## Design

The words and numbers each node carries. Numbers are starting points the report tunes.

**Base (25 credits, unchanged price).** 1 magic damage a tick to everyone inside (20 a second, half of today).
It reveals nothing: a reveal is always bought (open questions 17 to 19). Its field still touches hidden
enemies, as every area effect does. Attune, Awaken and the Range line stay.

**Interference (passive, from the base).** An Obelisk's field damage drops 25% for every other Obelisk whose field
overlaps its own, down to 25%. Its effects are not weakened.

**Toll (Attune).** An enemy still earns a stack a second inside, up to five. Toll is the strength of every
numeric effect the field applies (armor taken, slow, damage taken): half strength on entry, full at five
stacks, and past full when Toll stacks higher (Meltdown: 1.5x at ten). Rules that are on or off (silence, no
heals, reveal) hold from the first tick. Its +10% field damage per stack goes, and so does its slowing of every
debuff's clock.

**Chain A, the amplifier line** (today Overcharged Coils, damage):

| Node | Today | Proposed |
|---|---|---|
| I Resonant Coils (was Overcharged Coils) | +60% damage, Toll twice as fast | Toll builds twice as fast (full in 2.5 s) |
| II Resonant Coils II | +50% damage, +25% at full Toll | enemies at full Toll are Vulnerable twice (+30% damage taken) while inside and 2 s after |
| III Arc Discharge | zap for 15 ticks' damage, crits | once a second the zap marks the healthiest enemy inside: Priority (every tower turns on it) and Exposed for 3 s, no damage |
| IV-A Meltdown | Toll cap +5, zap crits and damage per Toll | Toll cap 10: effects keep growing past full, up to 1.5x |
| IV-B Tesla Coil | zap chains to 3 more, Dazes 0.25 s | unchanged, the chain carries the mark's Exposed (not the Priority) |

**Chain B, Phase Field** (already control, now the detector): I and II keep their range. Phase Field I adds
the reveal: a visit's first Toll stack reveals the enemy for 0.5 s and Dazes it for 0.5 s; Phase Field II
lengthens the reveal to 2 s; Null Field (silence) stays; True Sight
lengthens it to 4 s with a 2 s Daze; Dead Zone stays.

**Extra line:** Corrosion (-50 armor at full Toll, was -30), Mirror Field and Undertow (chill 35% at full Toll,
was 25%, Anchored) stay, armor and chill scaled by Toll. Event Horizon: each death inside widens the field by 10% of its range until the wave ends, up
to twice its range, instead of adding damage.

**Specials:** Rattle Field stays; Kill Zone's +40% at full Toll (was +25%) scales with Toll; Soul Drain drains 15 spirit a second, three
times as fast (so every debuff on the enemy lasts longer, and at -100 nothing wears off), keeps no heals or
shields at the floor, and loses its damage per lost spirit.

**Linger.** Every rule the field holds on an enemy (silence, no heals or shields, armor, chill and Anchored,
damage taken, Vulnerable) lasts 2 s after the enemy leaves, so the towers beyond the field's edge gain from it too.

**Facts that shape the numbers.** A 1.5-cell field crossed straight holds an enemy about 2 to 3 seconds, so
without Coils I or a bend few enemies reach full Toll. Plating and insulation take a flat amount off every
hit, periodic ones included: the Reaver's 1 point of insulation blanks a 1-point field tick entirely.
Effects of one kind from two Obelisks already refresh rather than add (one stack each), so of today's
stacking only the damage is left.

## Findings after the field, the tree and Interference

From `BalanceReport --seeds 12 --strong --variety --compare final`, 2026-10-14, Soul Drain still at 5 spirit a
second. Saved as `target/balance/ob-phase2`. The bought reveal alone (saved as `ob-phase1`) moved nothing: every
margin within noise.

| Level | competent holds (before) | strong holds | Obelisk's share | banning it | Obelisks alone |
|---|---|---|---|---|---|
| Curly Path | 58% (159%) | 58% | 16% | x0.02 | never win |
| Zigzag Path | 50% (122%) | 58% | 10% | x0.84 | hold 19% |
| Twisted Hourglass | 34% (142%) | 56% | 18% | x0.03 | never win |

- **Every level is lost as authored**: the competent player wins none; Curly Path would need enemy health x0.46.
- **The Obelisk no longer carries by damage** (10% to 18% of it) and an all-Obelisk army wins nothing.
- **It is still mandatory on Curly Path and Twisted Hourglass** (x0.02, x0.03 without it), for the Elite Ghost:
  a shrouded group is reached only by the field, since no player buys a reveal for its own sake. The players'
  rules know nothing of hidden enemies; they buy Phase Field I (58% to 100%) for its range. Enemies spend 24% to 35%
  of their time invisible; leaks are mostly Elite Ghosts and a Boss's spawn.
- **No player buys the support nodes**: Corrosion, Undertow, Kill Zone, Vulnerable and Priority are never put on an
  enemy. The lab prices them low beside a Sniper (none above +10%; Arc Discharge's mark +13% to +25%), since a
  field effect holds only while the enemy is inside a 1.5-cell field, a small share of a partner's shots.

## Findings after the players learned to reveal and the field's effects grew

From `BalanceReport --seeds 12 --strong --variety --compare ob-phase2`, 2026-10-14: the lab measures what each
reveal shows and what each tower lands on a hiding kind revealed, the competent player counts both; Kill Zone,
Corrosion, Undertow and Resonant Coils II are stronger, Soul Drain drains 15 spirit a second. Saved as
`target/balance/ob-phase3`.

| Level | competent holds | strong holds | Obelisk's share | banning it | banning the Radar |
|---|---|---|---|---|---|
| Curly Path | 100% (was 58%), in band | 109% | 5% | x0.67 | x0.65 |
| Zigzag Path | 50% | 58% | 11% | x0.87 | x1.02 |
| Twisted Hourglass | 40% (was 34%) | 92% (was 56%) | 16% | x0.03 | x1.15 |

- **Curly Path is in band, and the Obelisk is no longer mandatory there**: every play buys Wide Band, and the
  Radar matters as much as the Obelisk.
- **What each reveal shows a Sniper beside it** of an Elite Ghost group: Wide Band 0.73 to 0.90; Phase Field I
  0.20 to 0.65, the deeper Phase Field nodes up to 0.83. Phase Field I shows nothing of a lone fading Ghost.
- **Twisted Hourglass still needs the Obelisk**, for its level: every player loses 3 of its 4 lives to the
  Reaver in round 1, and it sends shrouded groups down three lanes, which one Obelisk per lane reveals far more
  cheaply than a Wide Band Radar per lane. That is the wave-composition feature's to answer.
- **The field's own effects are still never bought**: Corrosion, Undertow, Kill Zone and Soul Drain are put on no
  enemy in any play, even stronger. The lab prices Arc Discharge's mark at +10% to +32% for a Sniper beside it and
  Resonant Coils II at +9%; nothing else of the tree reaches +9%.
- **With the 2-second linger** (saved as `ob-phase4`) no play changes: no player buys those nodes yet, so every
  margin is the same. The lab now prices Kill Zone at +13% for a Sniper beside it on Zigzag Path (under +9% before).

## Open questions

Asked after the findings above:

20. ✅ **Should the headless players learn that hidden enemies need a reveal?** ↳ Yes: the lab measures what each
    reveal shows and what each tower lands on a hidden kind once revealed, and the competent player counts both.
21. ✅ **How do the field's effects reach the other towers' shots?** Options were a linger after leaving, a wider
    field, or stronger effects. ↳ Stronger effects at the same reach.
22. ✅ **When are the levels retuned?** ↳ Not in this feature. 💬 "do not tune by % hp, tune by wave composition, but it
    will be another feature I guess". Question 16's answer (by enemy health) is withdrawn.
23. ✅ **What happens to the field's effects that no player buys, even stronger?** Options were a linger, larger
    numbers, or leaving them to the general repricing. ↳ They linger 2 s after the enemy leaves.

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
7. ✅ **What passive answers stacking?** Effects of one kind already refresh rather than add, so only the
   field damage stacks.
   - A: a debuff an enemy already carries is refreshed, not strengthened, by a second field (true already).
   - B ⭐: Interference: an Obelisk's field damage drops 25% for every other Obelisk whose field overlaps it,
     down to 25%; its effects don't. Spreading them out pays, which suits a tower that covers the path.
   - C: no passive yet; add B only if the report still shows Obelisk-only armies holding 100% or more.
   - ↳ B.
8. ✅ **Is there an Obelisk-Beacon combo?**
   - A: a Beacon in reach of an Obelisk also strengthens the field's effects by its damage share.
   - B ⭐: none for now; the Beacon's range buff already widens what the field covers, and the report shows
     whether the pair needs more.
   - ↳ B.
9. ✅ **How do effects grow with Toll?**
   - A ⭐: half strength on entry, +10% a stack, full at five: a fast enemy still feels the field.
   - B: nothing on entry, +20% a stack: the field rewards slowing and bends.
   - ↳ A, and past full when Toll stacks beyond five (Meltdown's cap of 10: 1.5x).
10. ❌ **How long does the base reveal last after an enemy leaves?** Superseded: there is no base reveal.
    💬 "I do not agree with revealing any unit instantly. Invisibility should be a late game mechanic so it is
    fine that the game forces player to buy sonar tower or some other invisibility revealing option. I might
    agree with adding a "reveal for half a second and daze for half a second" obelisk field option to head
    upgrade after attune. But having invisibility revealing mechanics for "free" is not good."
    - A ⭐: a moment (about 0.5 s), so a tower beside the field can finish its shot; longer is Phase Field
      II's and True Sight's.
    - B: only while inside.
11. ✅ **Does Toll keep making every debuff wear off 10% slower a stack?**
    - A ⭐: yes; it helps every other tower's debuffs, which is support.
    - B: no; Toll does one thing, the field's own strength.
    - ↳ B.
12. ✅ **Does the zap (Arc Discharge, Tesla Coil) keep any damage?**
    - A ⭐: none; it marks (Priority and Exposed) and Tesla Coil Dazes.
    - B: a small fixed hit (5 field ticks), which the Beacon raises like the field.
    - ↳ A.
13. ✅ **What does Event Horizon become?**
    - A ⭐: each death inside strengthens the field's effects, +5% up to +100%, until the wave ends.
    - B: each death inside Dazes everyone still inside for 0.5 s.
    - ↳ Neither. 💬 "each death increases the range for this wave by 10% stacking to 200%": each death
      inside widens the field by 10% of its range until the wave ends, up to twice its range.
14. ✅ **Node names.** The node ids stay either way.
    - A ⭐: rename only names that now lie: Overcharged Coils I and II become Resonant Coils I and II.
    - B: keep every name.
    - ↳ A.
15. ✅ **What counts as balanced?** Measured with `--seeds 12 --strong --variety` against `final`.
    - A ⭐: an all-Obelisk army holds under 100% on every level; banning it costs x0.75 to x0.95 (it matters,
      it is not mandatory); the lab shows support from its nodes; Elite Ghost rounds need an Obelisk or Wide
      Band *and* damage; the competent player still builds it.
    - B: only that it no longer carries a level (ban cost above x0.75), whatever its support worth.
    - ↳ A.
16. ✅ **Does this feature retune the levels (item 6)?** Weakening the Obelisk will drop every margin.
    - A ⭐: yes, last, back to the middle of the band (about 125% for the competent player), by enemy health,
      which leaves the economy's pacing alone.
    - B: yes, by bounty, which the sweep found moves the margin most.

    - ↳ A.
17. ✅ **What is the full answer to a shrouded group?** Fact: an Elite Ghost re-shrouds every ally within
    about 3 cells every second, and shrouding ends a reveal, so every timed reveal in the game (Wide Band's 3 s,
    Phase Field II's 2 s) is undone within a second: Wide Band shows a shrouded group about a sixth of each
    3-second revolution. Only the field's damage, which needs no sight, answers it today.
    - A ⭐: the Radar's Wide Band (Awaken, a special: about 136 credits with the Radar, 150 XP, and it gives up
      Mark on Sweep) is the full answer, and a reveal holds the shroud off while it lasts, so its 3 s per
      3-second revolution keeps its whole reach visible. A revealed Ghost can't fade either, as an Inverted one
      can't today.
    - B: the shroud rule stays; Phase Field II reveals continuously while inside (paid, mid-tree), the Obelisk
      the full answer and Wide Band the partial one.
    - ↳ Neither. 💬 "make ghost cast once per 3s and make the invisibility last 3s as well": the shroud is
      cast every 3 s and lasts 3 s, so a reveal holds until the next cast instead of under a second. The
      reveal rule stays; Wide Band and the bought Obelisk reveals become real answers.
18. ✅ **Where does the Obelisk's small reveal go?** Proposed as 💬 suggested: the first Toll stack of a visit
    reveals for 0.5 s and Dazes for 0.5 s; Phase Field II lengthens the reveal to 2 s, True Sight to 4 s with a
    2 s Daze.
    - A ⭐: Phase Field I (head I, after Attune, 25 credits, no XP): the detector is a commitment, exclusive
      with the amplifier chain.
    - B: the extra line's first node (20 credits, 25 XP), where any Obelisk can take it beside either chain.
    - ↳ A.
19. ✅ **Is invisibility late enough?** Below Elite, a Ghost fades for 10 s (15 s from Veteran) when first hit;
    they arrive in round 4 of Twisted Hourglass, 6 of Zigzag Path and 9 of Curly Path. A faded one alone is
    still reached by area damage near visible allies and by the field. The shroud arrives with Elite: round 11
    of Curly, 8 of Zigzag, 10 of Hourglass.
    - A ⭐: keep the rounds; only the shroud demands a bought revealer.
    - B: move Ghost rounds later, so no invisibility shows before the second half of a level.
    - ↳ A.

---

*After planning and implementation, update this document rather than deleting it: mark it implemented, prune
resolved open questions, and either promote deferred scope to a new request or note it is still wanted for a
later version.*
