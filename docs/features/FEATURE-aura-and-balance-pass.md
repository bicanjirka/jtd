# Feature Request: Aura and the Balance Pass

**Status: implemented, except the repricing.** Feature 7 of 7 in the tower rework (order in `TODO.md`).
The Aura (shown as the Beacon) is built as described below, the display names are in, and `README.md` is
rewritten. The balance pass has its tool, `td.PurchaseHarness`, and its first measurements, but no node was
repriced: how a copy of a tower is limited has to be decided first (the placeholder entries in `TODO.md`
keep what it did not settle).

As built, where it differs from this request:

- The Range line keeps the shared Range steps (+15%, +10%); the "base range 2.05 / II 3.17" figures of the
  request were not applied.
- The Apprentice is marked with a ring on its tower, not a book glyph.
- Buffs a tower earns XP through, and Withering Field's interval, are axes of `TowerBuff` the Aura sets, so
  they reach a tower through its published stats like any buff; the XP bonus is asked of every tower when a
  walk ends (`Tower.xpBonusFor`, `Tower.xpSharedWith`).
- Display names live on `TowerFactory.Type`; the class-name rule in `td/tower/CLAUDE.md` now says so.

## Summary

The last tower and the finish of the rework. The **Aura** becomes the Mentor: it makes a mixed
cluster stronger, shields it from jamming and helps young towers climb (XP). It comes last because
its worth is what it lets every other tower do. Then the **balance pass** prices everything with
`td.BalanceHarness` simulations that count effects and support as well as damage, checks the XP
and purpose tables (feature 2) against play, and closes the placeholder-number entries in
`TODO.md`. Finally the towers get their new
**display names** and `README.md` is rewritten for the new trees.

## Current state (what exists today)

- **Aura today:** $20, passive, range 1.5, buffs towers in range +20% damage and range; it buffs no
  Aura until Resonance Field II. Base: Range (base range 2.05) and Awaken (gated on 6 of the 8
  surrounding cells holding towers). Head: Amplifying Core (I and II each multiply the buff by 1.5:
  +20% -> +30% -> +45%, and II adds a fire-rate bonus equal to the buff, +45%) and Resonance Field
  (+30% range; +25% range and it may buff other Auras). Gates are neighbour counts (near-2,
  near-3). Special: Withering Field (every second, every enemy in the aura's own 1.5 cells gains
  Vulnerable; an Aura usually stands among towers, not on the path, so it often touches nothing).
- **Amplifying Core II is the one upgrade that beats building another tower:** about x2.6 output
  for a whole cluster for $70 of upgrades.
- **Disruption** (the Jammer) is an aura on towers: it cuts their fire rate and range while they
  stand in it. The Jammer is in no level yet.
- `TowerBuff` axes: damage, range, fire rate, bounty, crit chance, armor and magic penetration.
  Feature 3 adds crit damage.
- **`td.BalanceHarness`** runs one built-in loadout on a level, headless and reproducible (seeded
  `RandomSource`), and reports what happened. It doesn't compare purchases.
- `TODO.md` carries five "unbalanced placeholders" entries: upgrade-tree node numbers, new tower
  numbers, effect and specialization numbers, enemy traits and abilities, critical damage.
- `README.md`'s tower section still describes the first upgrade system ("two permanent,
  mutually-exclusive upgrade paths... marked on the board by a coloured ring", "the Aura tower is
  passive and offers none").
- **Tower names:** each tower's class name and its UI name are the same word (`td/tower/CLAUDE.md`).

## What this feature adds

### Aura - the Mentor

**Why build it.** The support tower: it makes a cluster stronger, shields it from jamming, and
helps young towers climb. It never attacks, so its worth is what it lets the others do, and it is
worth most in the middle of a *mixed* cluster (Kinship). **Buy it when** you've committed to a
cluster, when one tower takes all the kills, against Jammers. **Weak against** spread-out
defences.

```
AURA     $20 | passive | range 1.5 | towers in range: +20% damage and range, half disruption
         never buffs an Aura
ATTUNE   Kinship: buffed towers earn +5% XP for each other tower type in range, up to +20%
RANGE    base range 2.05  ->  II base range 3.17         (no Range III, no Transcendent)
HEAD A   Amplifying Core (strength: the cluster hits harder)
         I    +10 to the buff (+30% damage and range); Kinship also adds +5 buff strength per
              other tower type
         II   +10 to the buff (+40%), and +10% fire rate
         III  Keen Edge [next to another Aura]: buffed towers +50% crit damage
HEAD B   Broadcast (reach: the cluster grows)
         I    +30% range; Kinship also counts tower types up to 1 cell beyond the aura, and loses
              its cap
         II   a timed buff on a buffed tower lasts twice as long, a timed debuff half as long;
              disruption is halved again
         III  Conduit: effects that buffed towers apply last 30% longer
EXTRA    Tutelage (the gate helper, I to III)
         I    buffed towers earn +10% XP, on top of Kinship
         II   Shared Lessons: buffed towers share what they see: each earns the XP of every
              enemy any of them reached (once per enemy)
         III  Apprentice: the buffed tower with the least XP is marked and earns double XP from
              every source; it stacks with Kinship
SPECIAL  one slot: Withering Field, Chosen, Rally
```

- **Base:** towers in range take half disruption, from the start: jamming is the Aura's problem to
  solve, not every tower's. **Auras never buff Auras**; Resonance Field II's aura-on-aura is gone.
- **Kinship** (Attune) pays the Aura for crowning a mixed cluster, in XP. On Amplifying Core it
  also pays in buff strength; on Broadcast it counts further and without a cap.
- **Amplifying Core's numbers** (your "review this and propose a solution"): the buff grows by
  **adding**, not multiplying, and its fire rate is a flat +10% instead of the whole buff. Today:
  +20% -> +30% -> +45% with +45% fire rate. Proposed: +20% -> +30% -> +40% with +10% fire rate,
  plus up to +20 from Kinship on this chain. The fire rate was what made it outclass building
  another tower; damage and range stay strong, as the chain's purpose. The balance pass confirms.
- **Broadcast II** works on what is timed: Frenzy's counted shots count double, Momentum's and
  Rally's bursts last twice as long. No tower debuff is timed today (disruption lasts while the
  Jammer is near), so "half as long" also means disruption is halved again: a quarter under a
  Broadcast Aura.
- **Conduit** lengthens every effect a buffed tower puts on an enemy: chill, burn and poison,
  freeze, Vulnerable, Sundered and the rest, hexes, and the zones it leaves. It makes the Aura
  matter to the Sonar, Seeker, Cinder and Hexer, not only to damage dealers.
- **Keen Edge** needs another Aura next to this one: two Auras side by side unlock each other's
  top level without feeding each other's numbers.
- **Gates** (feature 2): price and XP like every tower; the Aura earns an enemy's XP when any
  tower it buffs earns it. Its purpose gate on head III is a layout condition, **buffing 4 or more
  towers**, and Keen Edge also needs another Aura next to it. Today's neighbour gates (near-2,
  near-3, near-6 on Awaken) go.

**Specials** (one slot; three on offer, for either chain):

- **Withering Field** (reworked): every buffed tower applies Vulnerable with its next hit once
  every 10 s. That is your "10% a second, whatever the fire rate" without dice: a tower firing 20
  times a second and one firing every 2 s both add one stack every 10 s. The Aura still owns
  Vulnerable; it now reaches the path through its towers.
- **Chosen:** buffs only the most experienced tower in range (most XP), at triple strength (every
  buff this Aura gives, tripled). For the Transcendent hero; the opposite of spreading thin.
- **Rally:** the Aura marks the path spot closest to it (shown only while the Aura is selected).
  When an enemy steps on it, every buffed tower gets +30% fire rate for 5 s; then 10 s cooldown.

**Look:** glow lines to buffed towers (they exist), coloured by what the aura grants; a book glyph
on the tower Apprentice is mentoring; Rally's spot when selected. Kinship's count shows in the
Aura's info rows.

**Purpose gate** (head III, feature 2): buffing 4 or more towers.

### The balance pass

- **A harness method that compares purchases.** For each built-in level and a fixed budget, for
  each node or tower on offer: how much buying it cuts leaks (or raises the health left) per credit,
  against the best alternative buy. Reprice until those are close. Support and effects are
  measured the same way as damage, through the result on the level, so the Aura, the Sonar and the
  Pulse are priced by what they let others do. Many seeded runs, not one.
- **Targets written down,** so every later number has a reference:
  - damage per credit by role: single-target about 1.2; area towers 0.4-0.6 per enemy reached;
    control and support priced by their effect;
  - a stat node gains between 0.7x and 1x of the damage per credit that building another copy of
    the tower would; prefer bigger bonuses to tiny prices;
  - specials cost more than head III; all of a tower's specials cost the same;
  - a tower being weak against some enemies is intended (plating against small hits, crit
    immunity against the Sniper): other towers and combos answer it, so the pass never tunes a
    tower out of its counter;
  - **XP and purpose:** XP needs no weights (feature 2 derives it from bounty); the pass checks
    that the gate table opens where feature 2's table says on every built-in level, and that each
    tower placed for its job meets its purpose gate no later than its XP reaches head III. A deed
    rate far from feature 2's estimate changes that tower's threshold, never the rule.
- **Watch list** carried from the design rounds:
  - the crit ceiling: x2.0 base, Tradecraft +0.5, Keen Edge +0.5, then Momentum's x5 (x15 in one
    hit; Momentum may need x3);
  - Saturation at +5% a stack may feel flat; raise Saturation, not the chains;
  - Arc on tight clumps before Lightning Rod (Ground Strike was the rejected fix);
  - whether the Hexer feels slow before Awaken (Brew's extra targets could move to I);
  - Rally under Broadcast II lasts 10 s on a 10 s cooldown: always on;
  - Withering Field's 10 s, Rattle Field's 5%, Soul Drain's -100 state;
  - the per-copy price rule (+15%) against the value rule above.
- **Closes** the `TODO.md` placeholder entries it settles, in the same commits (upgrade-tree node
  numbers, new tower numbers, effect and specialization numbers, critical-damage numbers) and keeps
  what it doesn't settle.

### Display names and the README

- **Towers get names that say what they are** (display only; ids, hotkeys and order stay):

  | Today | Shown as |
  |---|---|
  | Sniper | Sniper |
  | Splash | Burst (Stormcaller and Hexer once forked, feature 4) |
  | Sonar | Radar (a rotating beam is radar; sonar is pings) |
  | Pulse | Obelisk (a stone that changes the rules around it) |
  | Aura | Beacon |
  | Mortar | Mortar |
  | Seeker | Hive (it nests missiles) |
  | Cinder | Scorcher (it applies Scorched) |

- **`README.md`'s tower section is rewritten** for the new trees, prices and names; the first
  upgrade system's description goes. Your call was to do it once, after every feature has landed.

### Out of scope

- The Command special (click-to-focus): not chosen (see Decisions).
- New Aura axes with no node using them yet (penetration, projectile speed): they come with the
  node that needs them.
- Enemy balance beyond what the tower numbers need: the enemies brainstorm comes next.

## Interconnections

- **Feature 1:** price rules; the Aura caps at Awaken.
- **Feature 2:** XP (Kinship, Tutelage, Apprentice, Shared Lessons and Chosen read it), the
  purpose and layout gates.
- **Feature 3:** the crit-damage axis (Keen Edge), Exposed and Marked in the harness's crit
  measurements.
- **Features 4 to 6:** every effect Conduit stretches, every timed tower buff Broadcast II
  lengthens (Frenzy, Momentum's burst), the Cinder's Bellows reading the Aura's fire rate.
- **`docs/ideas/enemies-brainstorm.md`** is the next round of design once this lands: the Jammer
  in real waves is what makes the Aura's half disruption matter.

## Constraints and open risks

- **Display names break a written rule.** `td/tower/CLAUDE.md` says a tower's class name and UI
  name are the same word, and the earlier decision was to rename display names only so node ids and
  tests don't churn. Either the rule changes in the same commit or the classes are renamed too;
  the request leaves that to planning, and the rule must not be left false.
- **Brood vs Hive:** the Seeker's head III was renamed Brood (feature 5) so it doesn't share the
  tower's new name.
- **Withering Field and Conduit reach into every buffed tower's attacks.** Withering Field needs
  "the next hit" (feature 3's hit rule: a Pulse's field tick is not a hit); Conduit needs every
  effect a tower applies to know how long its maker's buffs stretch it. Effects are made in many
  places today, one per tower.
- **Rally's spot** is a path point chosen per Aura; on a level with several paths, "closest" may
  sit on a path the Aura's towers can't reach.
- **The harness method costs time:** many seeded runs per node per level. It runs offline, not in
  `mvn verify`; the 5 s per-test budget doesn't apply to it, but it must stay reproducible.
- Standing requirements: the engine stays headless, the HUD look stays uniform.

## Decisions made

- The Aura is last because it amplifies everything else, and the balance pass must see complete
  content.
- Kinship pays in XP, not buff strength (your round-2 call); Amplifying Core adds buff strength per
  type, Broadcast lifts the cap.
- Broadcast II is your timed buff/debuff rule; halving disruption again is the reading for today,
  when no tower debuff is timed.
- Tutelage III is your least-XP mentoring rule (named **Apprentice**); Head Start and Veterancy are
  not taken.
- Specials: Withering Field (reworked as a per-shot chance), Chosen, Rally (reworked as a path
  spot). **Command is dropped**: you ticked three specials and Command was not among them, and the
  panel offers three. With it goes the last click-to-focus in the design; the Sonar's Command Ping
  is the only focus fire.
- No diminishing stacking of several Auras; no Transcendent for the Aura; the price stays $20 and
  the buff +20%.
- Amplifying Core is additive with a flat +10% fire rate (proposal above, your request).
- Tower display names as in the table; ids and hotkeys unchanged; toolbar order unchanged.
- Chosen picks by XP: one number every tower shows, and the most experienced tower is the hero.
- **Simplified for cost:** Conduit lengthens effects (+30%) but no longer makes them "bite 15%
  harder" (strength means something different for every effect: chill level, pool size, freeze
  time, stacks); Withering Field is a 10 s timer per tower instead of a chance scaled by each
  tower's shot interval (towers without a shot interval, a field or a sweep, had no answer); the
  Kinship glyph per tower type becomes a count in the info rows; Shared Lessons shares what the
  cluster sees, since XP no longer comes from kills.
- New Aura axes are built only with a node that uses them: crit damage (Keen Edge) and effect
  duration (Conduit). Penetration and projectile speed, ticked in the brainstorm, wait for one.

## Open questions

- None blocking.
