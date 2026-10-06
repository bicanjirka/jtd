# Feature Request: Sniper and Sonar, the Crit Team

**Status: not started.** Feature 2 of 6 in the tower rework (order in `TODO.md`). Needs feature
1 (`FEATURE-tower-progression.md`).

## Summary

The first two towers rebuilt on the new tree, and the shared crit and debuff vocabulary every
later tower uses. The **Sniper** becomes the Assassin: it deletes the one enemy that matters and
gets better the longer it stays on one target. The **Sonar** becomes the Spotter: it sees
everything, Exposes the biggest threat to crits from every tower, marks targets for a guaranteed
crit, wears crit immunity down and bridges to magic. They are built together because each is the
other's best partner: Sonar Exposes and marks, the Sniper fires.

Alongside them, this feature adds the effect rules that hold for every tower: crit per tower,
armor and magic resist floored at 0, spirit pacing every debuff, and markers grouped by category.

## Current state (what exists today)

- **Crit** (`FEATURE-critical-damage.md`, implemented): one multiplier for everybody,
  `AttackProfile.DEFAULT_CRIT_MULTIPLIER` (x1.5), carried with every hit by `AttackProfile`.
  `TowerBuff` has a crit-chance axis but no crit-damage axis. A forced crit (Fifth Shot, Mark on
  Sweep) still rolls against resilience, so at 70 resilience it lands only 30% of the time.
  Burn and poison pulses go through the applying tower's `dealDamage`, so a DoT can crit today.
- **Crit-immune enemies:** resilience 100 means no crit; Scorched lowers resilience by one per
  stack, floored at -100, and below zero it raises the crit bonus taken (double at -100).
- **Effects** that exist: chill, burn, freeze, shield, invisible, heal, vulnerable, revealed,
  poison, scorched, sickened. Nothing lowers armor, magic resist, plating or crit chance taken.
  `CRIT_CHANCE_TAKEN` is in `EnemyStat` and unused.
- **Spirit** paces how fast Scorched and Sickened stacks wear off (none at -100) and scales heals
  and shields. No other effect reads it.
- **Markers:** one marker per effect kind, overflow shown as "+N".
- **Sniper today:** $10, 30 physical, range 3.8, a shot every 2 s, 15% crit, aims furthest along
  the path. Head: Focused Optics (+20% damage; +20% damage and +25% fire rate, no gate) and
  Marksman's Eye (+15% crit at 15 kills; +20% crit and 50% armor penetration). Specials: Marked
  Round (crits apply Vulnerable), Fifth Shot (every 5th shot a forced crit, crits deal 250%),
  Momentum (the shot after a crit deals 500% and ignores armor; a kill by it +100% fire rate
  5 s). Any special switches its aim to most health.
- **Sonar today:** $20, 16 physical, range 5.2, a revolution every 2 s counterclockwise (the
  `README.md` line says 4.2 and 4 s). Head: Twin Array (+25% damage; +25% and +10% crit; a second
  beam opposite) and Long Reach (+15% crit; damage up to +100% at max range). Specials: Wide Band
  (reveals invisible enemies it passes), Mark on Sweep (the marked enemy's next hit **from this
  Sonar** crits: the marks live in the tower's own set), Piercing Tone (up to +50% magic damage
  against armored or shielded enemies).
- `InWedgeTargetQuery` (a narrow wedge is a line) and `HitReceiver.reductionAgainst` exist.
  Selectors that exist: furthest along path, highest health, nearest, random.

## What this feature adds

### Rules for every tower

- **Crit per tower.** Each tower has its own crit multiplier: the Sniper x2.0, everyone else
  x1.5. A crit-damage bonus adds to the multiplier (x2.0 + 0.5 = x2.5), so several sources stack
  instead of overwriting each other.
- **Only the Sniper is born with crit** (5%). Other towers earn 5-15% from one head level, given
  in their own features. Crit chance from today's nodes stays until their tower is reworked.
- **A guaranteed crit lands whenever the enemy's resilience is below 100.** Its size still
  shrinks with resilience, as every crit's does. **At 100 nothing crits**: not a Mark, not a
  guaranteed crit, not penetration. Lowering resilience is the only way in.
- **A DoT pulse never crits; the hit that applies it can**, and a crit application starts the pool
  at the crit multiplier. One crit moment per ignition, visible, instead of a hidden crit chance
  rolled four times a second.
- **Armor and magic resist stop at 0.** Extra damage comes from stacking armor or resist loss with
  Vulnerable, never from negative armor.
- **Spirit paces every debuff**, not only Scorched and Sickened: a debuff's timer runs at
  `max(0.25, 1 + spirit / 100)` of normal speed. Freeze keeps its own diminishing returns, and
  Scorched and Sickened keep today's spirit-scaled stack decay. The enemy inspector says it in
  words ("debuffs wear off 1.5x slower").
- **Revealed enemies are Exposed** while revealed: the Sonar's two jobs feed each other.
- **Re-shrouding ends a reveal.** A revealed enemy is targetable like any other, but if it is
  shrouded or turns invisible again, it loses Revealed and hides again.
- **Markers grouped by category** once there are this many kinds: one marker per category with a
  count on the enemy, every effect listed in the inspector.

### New effects

| Effect | What it does | Applied by (this feature) |
|---|---|---|
| Sundered | -5 armor a stack, 10 stacks, one 5 s clock | Sniper's Sunder Rounds |
| Exposed | crit chance taken x2 | Sonar's Ping; every revealed enemy |
| Marked | the next hit from **any** tower is a guaranteed crit and spends the mark; one per enemy; a hit that can't crit (resilience 100) doesn't spend it | Sonar's Mark on Sweep |
| Resonating | +8% magic damage taken a stack, 3 stacks, 4 s | Sonar's Harmonics |
| Fractured | -10 resilience a hit, down to -50, recovering 10 a second | Sonar's Resonant Crack |

Later features apply some of these too (Pulse's Rattle Field rolls Sundered or Exposed, the
Seeker's Tracer marks, Mortar's Bunker Buster sunders).

### Sniper - the Assassin

**Why build it.** A duellist that gets better the longer it stays on one target. It deletes the
enemy that matters: the Elite inside a Soldier wave, the Mender behind the wall, the Warden.
Chain A turns patience into tempo, chain B into certainty. **Buy it when** waves carry Elite or
Boss ranks, or support enemies must die first. **Weak against** swarms (Ricochet is its answer),
crit-immune armor (accepted: other towers open it), the invisible (the Sonar's job).

```
SNIPER   $15 | 40 physical | a shot every 2.5 s | range 4.0 | crit 5%, x2.0 | furthest along the path
ATTUNE   Steady Aim: +10% crit chance on every shot after the first at one target (15%)
RANGE    +15%  ->  II +10%  ->  III Overwatch: +35% range, can't shoot within 2 cells
HEAD A   Focused Optics (tempo: patience becomes speed)
         I    Steady Aim also gives +25% fire rate; Quick Scope: the first shot at a new target
              has +50% crit chance
         II   +40% damage; a crit starts Frenzy: the next 3 shots come twice as fast; a crit
              during Frenzy doesn't restart it
         III  Weak Spot: +30% damage; non-crit shots ignore plating and 50 armor
         IV-A Railgun: the shot pierces every enemy on the line from the tower through the
              target, reaching 2 cells past max range; each one after the first takes 25% less
         IV-B Executioner: a non-boss enemy left under 15% health by a shot dies; Boss rank
              takes +50% while under 25%; executions count as crits for triggers
HEAD B   Marksman's Eye (certainty: patience becomes crits)
         I    Steady Aim stacks to 3 (+30%: 35% crit from the 4th shot)
         II   ignores 30 armor; +25% fire rate
         III  Clean Shot: crits go through shields straight to health
         IV-A Unbroken Aim: Steady Aim stacks to 5 (55% crit) and survives a kill; only
              retargeting a living enemy resets it
         IV-B Sunder Rounds: every crit adds 1 Sundered
EXTRA    Tradecraft (the assassin's technique, for either chain)
         I    a crit makes the next crit deal +25% crit damage, two in a row +50%, no further
         II   +25% damage against targets past two thirds of its range
         III  Silver Rounds: every 3rd shot is magic with +25% magic penetration; it keeps the
              crit streak and profits from it
         IV   Spotter Uplink: may shoot Marked or Revealed enemies within twice its range
SPECIAL  a set per chain
         A    Momentum, Ricochet, Headhunter
         B    Hollow Point, Fifth Shot, Shatter Shot
```

**Specials.** Chain A's set is speed, crowds and bosses; chain B's is the team, the rhythm and the
combo with freezes.

- **Momentum** (exists): a crit charges the next shot to x5 damage that ignores armor and plating;
  a kill by it gives +100% fire rate for 5 s. Aims at the highest rank.
- **Ricochet** (new): a crit bounces to the nearest enemy within 1.5 cells for 60%, up to 3
  bounces; each bounce can crit. The Sniper's swarm answer. Aims at the enemy with most neighbours
  within 1.5 cells, so bounces find targets.
- **Headhunter** (new): +40% damage against Elite and Boss rank. Aims at the highest rank.
- **Hollow Point** (today's Marked Round, renamed: "Mark" now means a guaranteed crit): crits
  apply Vulnerable. Aims at most health.
- **Fifth Shot** (exists, reworded): every 5th shot is a guaranteed crit with +50% crit damage
  (instead of "crits deal 250%"), so it stacks with the x2.0 base. Aims at most health.
- **Shatter Shot** (new): crits on a frozen or Dazed enemy deal +50% crit damage. Aims at a frozen
  or Dazed enemy first. Dazed arrives with feature 3; until then it reads frozen only.

**Look by tier:** a thin barrel; a faint laser line to its target that brightens with each Steady
Aim stack (Attune); a gold barrel and a white tracer (Transcendent); silver shots as a pale blue
beam.

**XP job events:** a crit; reaching full Steady Aim; an execution. **Rank perk (Expert):** Steady
Aim starts at 1 stack.

### Sonar - the Spotter

**Why build it.** Long-range support that also deals steady damage over a huge area. The sweep
hits all it passes; on top of that it Exposes the biggest threat, reveals the invisible, marks for
a guaranteed crit from any tower, wears crit immunity down and bridges to magic. Where the Sniper
is patient with one target, the Sonar is patient with the whole map: its rhythm is the
revolution. **Buy it when** ghosts appear, the defence leans on crits, against crit-immune armor,
on spread-out lanes. **Weak against** packed fast groups (one hit a revolution) and plating
(small hits).

```
SONAR    $20 | 16 physical | a revolution every 3 s, counterclockwise | range 4.5 | hits all it passes
ATTUNE   Ping: each revolution Exposes the healthiest enemy the beam passes, until the next pass
RANGE    +15%  ->  II +10%  ->  III Deep Scan: +10%; each revolution reveals invisible enemies
         in the outer quarter of its range for 1 s
HEAD A   Rapid Array (more passes: more hits, more pings)
         I    +25% damage; Ping Exposes the two healthiest enemies it passes
         II   +25% damage, +10% crit; a beam crit refreshes Exposed on its target
         III  Spin-Up: a revolution every 2 s instead of 3
         IV-A Twin Beam: a second beam, opposite, at 70% damage
         IV-B Phased Array: stops spinning, locks onto the enemy with most health in range and
              sweeps a 60 degree arc back and forth over it (about 3x the hits on the focus)
HEAD B   Long Reach (far: the spotter on the hill)
         I    +20% range; Ping picks the healthiest enemy past half range, Exposed for two passes
         II   damage up to +100% at max range, +10% crit
         III  Resonant Crack: each beam hit takes 10 resilience, down to -50, back 10 a second
         IV-A Horizon: +40% range, far bonus up to +150%, but no damage within 1.5 cells
         IV-B Fault Line: Resonant Crack's loss doesn't recover while the enemy is Exposed, and
              falls to -100, where every crit's bonus is doubled
EXTRA    Frequency (the magic opt-in)
         I    Ultrasound: 20% of each hit is added as magic, up to 50% against armored or
              shielded enemies
         II   Harmonics: hits apply Resonating
         III  Pure Tone: the beam deals magic instead of physical, +15% magic penetration
         IV   Shatter Tone: a hit on a shielded enemy breaks a quarter of the shield
SPECIAL  one shared set: Mark on Sweep, Wide Band, Command Ping
```

**Specials** (each works on either chain, through the pass):

- **Mark on Sweep** (reworked): the mark goes on the enemy (Marked), so the next hit from any
  tower is a guaranteed crit. One mark per enemy, renewed each pass. Against a crit-immune enemy
  it waits, and fires once Scorched or Resonant Crack opens it.
- **Wide Band** (exists, extended): each revolution reveals invisible enemies it passes, and a
  revealed enemy is Pinged (Exposed) for 1 s.
- **Command Ping** (new): each revolution names the pinged enemy the Priority: every tower that
  can reach it switches to it and deals +15% to it. A built-in focus fire, not a player choice.

**Look:** a faint ping ring each revolution, so the rhythm reads; a crosshair over the pinged
enemy; the second beam (Twin Beam); a blue beam once it deals magic.

**XP job events:** a reveal; an enemy Exposed by Ping; a Mark another tower spends. **Rank perk
(Expert):** Ping's Exposed lasts one pass longer.

### What happens to today's nodes

| Today | Becomes |
|---|---|
| Sniper base 15% crit, $10, 30 damage, range 3.8 | 5% crit (Steady Aim adds the rest), $15, 40, range 4.0, a shot every 2.5 s |
| Focused Optics I-II (+20% damage; +20% and fire rate) | Focused Optics I-II as above |
| Marksman's Eye I-II (+15% crit; +20% crit, 50% armor penetration) | Steady Aim stacks; flat 30 armor penetration and fire rate |
| Marked Round | Hollow Point |
| Fifth Shot (crits deal 250%) | Fifth Shot (+50% crit damage) |
| Momentum | Momentum, in chain A's set |
| "any special aims at most health" | each special aims for its own job |
| Sonar range 5.2, 2 s a revolution | range 4.5, 3 s a revolution |
| Twin Array I-III (damage; damage and crit; second beam) | Rapid Array I-II; Spin-Up at III; the second beam at IV-A, 70% |
| Long Reach I (+15% crit) | +20% range (the chain finally delivers reach); crit moves to II |
| Piercing Tone | folded into Ultrasound |
| Mark on Sweep (this Sonar's next hit) | Marked, for any tower |

## Interconnections

- **Feature 1** provides Attune, level IV, the extra node, per-chain specials, XP, job events and
  rank perks. The Sniper is the first tower with full content, so it is where Transcendent and
  the level IV choice are first seen in play.
- **Feature 3** adds Dazed (Shatter Shot then reads it) and the Splash's Sympathy, which copies
  Sundered and Exposed across hexed groups.
- **Feature 4** adds Seeker's Tracer (another Marked source) and Pulse's Rattle Field (Sundered
  or Exposed).
- **Feature 5**'s Cinder lowers resilience with Scorched: with Resonant Crack it opens crit
  immunity fast.
- **Feature 6**'s Aura grants crit damage (Keen Edge) on top of the per-tower multiplier.
- `TODO.md`'s "Critical-damage numbers are unbalanced placeholders" names
  `DEFAULT_CRIT_MULTIPLIER` and the Sniper's 15%; both change here.

## Constraints and open risks

- **Command Ping overrides other towers' aim.** Today each tower composes its own targeting and
  nothing reaches across towers. The Priority must not become a player-chosen target, and towers
  whose job is not single-target (a Pulse field, a Sonar sweep) have nothing to switch.
- **Marked moves from the tower to the enemy.** Any tower's next hit spends it, including small
  and periodic hits. Field ticks and DoT pulses should not spend a mark meant for a real shot;
  this is a rule to state, not yet decided in code.
- **Spirit pacing every debuff** touches every existing timed effect (Vulnerable, Revealed,
  chill) and so every existing balance number; the high-spirit enemies that would show it off
  are in the enemies brainstorm, not built.
- **The crit ceiling.** The Sniper's crit damage can reach about x3.0 (x2.0, Tradecraft's streak
  +0.5, Aura's Keen Edge +0.5 in feature 6), and Momentum multiplies a shot by 5: about x15 in
  one hit. Momentum's x5 may need to become x3 (balance pass).
- **Fire rate on chain A:** +25% under Steady Aim, doubled in Frenzy (fire-rate bonuses multiply)
  is x2.5 for three shots. A timed self-buff never stacks today: Momentum's kill burst replaces a
  running Frenzy instead of multiplying with it. Keep that.
- **Executions count as crits** for triggers: they charge Momentum but start no Ricochet bounce
  (the target is gone). Executioner and chain B's set never meet (different chains).
- **Railgun** reaches past the targeting range; the piercing line must be drawn so the player
  sees why an enemy beyond range was hit.
- Visual budget: the laser line, ping ring and crosshair are per-frame draws; check
  `td.PerformanceHarness` after adding them.
- Standing requirements: headless engine, board content through the render pipeline, the
  uniform HUD look.

## Decisions made

- The Sniper and the Sonar ship together, before the Splash. Your rollout order put the Sonar
  among "the rest"; it moves up because Exposed and Marked are shared vocabulary every later tower
  reads, and Sniper content (Spotter Uplink, Shatter Shot's targets) is half-useless without them.
- Crit immunity holds: a Mark, a guaranteed crit or penetration never gets through 100
  resilience. Being countered by crit-immune enemies is fine for the Sniper.
- Marksman's Eye III is **Clean Shot** (crits go through shields). It answers what really
  counters the certainty chain: the shields that soak a big crit, including the Warden's own
  shield raised when it survives a crit. Find the Seam was rejected; the other candidate was Armor
  Piercer (every 3rd shot at one target ignores all armor and plating).
- Sonar chain A is renamed **Rapid Array**: its level III makes the sweep faster (2 s) and the
  twin beam moved to IV-A (at 70%), so "Twin" no longer named what level I does.
- Dead zones (Overwatch's 2 cells) are fixed distances: range bonuses don't shrink or grow them,
  the same rule the Mortar's dead zone follows.
- Renames: Marked Round -> Hollow Point; Tradecraft III is "Silver Rounds". Resonant Crack's
  effect on the enemy is called **Fractured**, so it doesn't collide with the Mortar's Cracked
  (plating).
- The Sonar's Command Ping is the only automatic focus fire in the game; there is no
  player-chosen target priority.

## Open questions

- **Which hits spend a Mark?** Proposed: any tower's direct hit, never a field tick, a DoT pulse
  or a zone pulse.
- **Command Ping and fields:** does "+15% to it" apply to the Pulse field's ticks and burn
  pulses, or only to direct hits?
