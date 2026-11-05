# Feature Request: Effect Simplification

**Status:** draft, 2026-11-05. To be finalized in another session. Most of it is still proposal: only
"Decisions made" is settled. ⭐ marks the option preferred so far, and every number under an option is a
starting point that nothing has measured.

## Summary

Make enemy stats and effects simpler to read and simpler to build on. Remove edge cases, exceptions and
conditions, and places that work out the same mechanic twice. A player should be able to read an enemy
and plan a defence without knowing hidden rules.

Four changes carry most of it:

- Cut most of the rules by which one effect changes another.
- Replace the per-effect resists with one stat, Tenacity.
- Replace diminishing returns, while keeping a Seeker-only defence from winning by holding everything still.
- Give the main defensive stats one shape, each running down to -100.

## Current state (what exists today)

### Enemy stats

16 stats, all in `td.stat.EnemyStat`. Stealth is the only one with no row in the enemy pane; Move speed
always has one; the rest show when their base or current value is off the default (`StatRows.shows`).

| Stat | Default | Range | What it does |
|---|---|---|---|
| Armor | 0 | 0 and up | A physical hit × `100/(100+armor)` |
| Magic resist | 0 | 0 and up | The same for a magic hit |
| Plating | 0 | 0 and up | Flat loss per physical hit, after armor |
| Insulation | 0 | 0 and up | Flat loss per magic hit, after magic resist |
| Physical damage taken | 100% | 10% and up | Multiplies what gets past plating |
| Magic damage taken | 100% | 10% and up | Multiplies what gets past insulation |
| Damage over time taken | 100% | 10% and up | An extra multiplier on periodic damage |
| Resilience | 0 | -100 to 100 | Crit chance and crit bonus × `1 - res/100`; 100 is immune |
| Agility | 0 | 0 and up | Dodge chance `a/(a+100)`, after accuracy; aimed hits only |
| Spirit | 0 | -100 and up | Regeneration and shield worth × `max(0, 1 + spirit/100)` |
| Regeneration | 0 | any | Health a second; below zero a crumble, which ignores spirit |
| Move speed | definition × spawn multiplier | 0 and up | Walking speed |
| Chill resist | 0 | 0 to 100% | Shortens the effect; at 100% it never takes hold |
| Burn resist | 0 | 0 to 100% | The same |
| Freeze resist | 0 | 0 to 100% | The same |
| Stealth | 0 | 0 to 100% | At 100% towers can't aim at it |

No hidden stat remains: healing received went in `391c995f`, and Resilience is the only enemy stat crits
read.

### Crit today

- -100 Resilience doubles crit chance and doubles the crit bonus. It does not make everything crit: a 5%
  tower gets 10%, and a 50% tower does always crit.
- Lowered by Scorch (-1 a stack, up to 100 stacks), Fracture (-10 a stack, up to 5, or 10 with Fault Line)
  and Expose (-50). Reveal implies Expose. Raised by Unyielding (+100).
- A long burn reaches the floor in about 50 s (`TODO.md`, "Effect and specialization numbers"), which
  already asks whether that is too strong.
- A Mark is a guaranteed crit on the next hit and waits on a crit-immune enemy. A Charge discharges twice
  as hard on a crit.
- Fast has -20 Resilience up to Elite ("reckless"); its Boss rank has 100.
- "A stopped enemy gets crit" exists three separate ways: the Sniper's `HitCondition.TargetStopped` node
  (+100% crit chance, +100% crit damage), the Splash's `ArcSpec.stoppedCritDamage` and the Seeker's
  Frostbite (a guaranteed crit on a frozen enemy, or one whose next freeze is diminished).

### Agility and aimed hits

- Only aimed hits can be dodged, and only the Sniper's shots and bounces and the Sonar's beam are aimed. A
  Sniper's line (Railgun) is not.
- Freeze and Daze set Agility to 0. A set value replaces the whole total, and the lowest set value wins.
- Authored on Mite, Courier, Skirmisher and small amounts elsewhere.

### Effect resists

Effect resists are traits (`EffectResistTrait`), not base stats. The full list, level enemies included:

| Enemy | Chill | Burn | Freeze |
|---|---|---|---|
| Yeti | 50%, 75% from Veteran | | immune |
| Salamander | | immune | |
| Magma | | 75%, immune from Veteran | |
| Juggernaut | 30% | | 30% |
| Armored | 25% from Veteran | | 20% |
| Colossus (level boss) | | immune on some stages | 50% or immune by stage |
| Storm Twins (level boss) | 50% on some forms | | 50% on some forms |
| Warden (level boss) | immune on one form | immune on some forms | immune on some forms |
| Enemies authored in `LabyrinthLevel` | one 50% | one immune | one 50% |
| Enemies authored in `RiftLevel` | one immune | | one immune |

So bosses already rely on freeze immunity. Whichever way this goes, immunity is load-bearing, not only lore.

`applyResisted` only ever shortens: `duration = authored × (1 - resist) × diminishing step`, and an
application under one tick is blocked. `Effect.withDurationScaledBy` also makes a pool decay faster, so
shortening a burn or poison cuts its **total damage**, and it leaves a lasting effect alone. Nothing resists
Poison, Soulfire or Daze today.

Earlier decisions this reverses: `FEATURE-enemy-stats.md` dropped Tenacity because per-kind resistance
covered it, and `FEATURE-effect-interactions.md` repeats that.

### Diminishing returns

- `DiminishingReturns`, one ladder per mob per kind. Freeze 100/50/25/0%. Daze 100/80/60/40/20/0%.
- The window is 10 s from the last fresh application. A stop that is kept out doesn't record, so the window
  still runs out.
- Freeze and Daze climb separate ladders, so alternating them gets around each.
- A stop can't be renewed while it holds (`ActiveEffects.apply`): "a hold renewed before it ends would never
  end".
- The pane shows the next share on an active effect (`EnemyStatText`). The Seeker's Frostbite reads
  `freezeDiminished`.
- Two open `TODO.md` entries are about it: "A hard-control kind without a ladder fails in play" and "A hard
  crowd-control kind's diminishing-returns ladder is a switch that throws in play".

### Stops

| Source | Length |
|---|---|
| Seeker missile | Freeze 1.5 s; 2.25 s with Deep Freeze |
| Seeker Absolute Zero | The same freeze, on everything within its area |
| Mortar Cryo shell | Frost ground: chill, and a freeze on an enemy that stays |
| Mortar Heavy Shell | Daze 0.5 s, small radius |
| Obelisk (`PulseTower`) Snare | Daze 0.5 s |
| Obelisk Stasis | Daze at full Toll, tied to Toll and the Stasis effect |
| Burst Thunderstrike, Thunderclap, Arc crits | Daze, 1 s when long |

The Seeker's cooldown is 2.25 s, so with Deep Freeze its freeze lasts as long as its cooldown. It aims at
the fastest visible enemy, so a frozen one is skipped and the next missile spreads to the next runner. Its
head III gates count landed freezes (`FREEZES_NEEDED`), and a freeze that doesn't land doesn't count.

### Slows

Slows **multiply** today, and each kind has its own shape:

| Kind | How it works | Numbers |
|---|---|---|
| Chill | A level made of one contribution per application, each fading linearly on its own slope | Cap 80%; 20% on frost ground, 30% from Rime, 40% from Thermal Shock |
| Tar | × a fixed share while it lasts | × 0.6 |
| Drag | Stacks of 1% of speed, held by a field | 15 base, up to 50 |
| Anchor | Holds speed to a share of base; the lowest cap wins and it also holds against speed gains | 75% |
| Poison | Scales with how full the pool is | Up to 30% |

Speed gains: Sprint +60%, Hasten × 2. While Hastened, Chill, Tar, Drag and Anchor can't take hold, and Poison
gives no slow.

Chill's level is read by other rules: how long a freeze lasts, how much a burn deals, Sympathy's copying and
Ash. Its fading is the only thing that softens speed coming back today, and only for Chill.

### Effect interactions

`EffectInteractions` holds 13 tables. `ActiveEffects` also holds its own rules: the chill cutting a burn, a
burn revealing above a fuel threshold, poison's slow. The full list is under "Interactions" below.

### What a stop-locked enemy does today

From a small model of one enemy, not the game. A 2 s stop is tried at a fixed interval, and a held enemy is
not renewed. It ignores damage, targeting and the walk.

| Stop tried every | Share stopped under today's ladder |
|---|---|
| 6 s | 15% |
| 4 s | 17% |
| 2.25 s | 22% |
| 1.5 s | 23% |
| 1 s or less | 27% |

This holds on every rank, Grunts included. So anything looser than about 27% makes a stop-lock stronger than
it is today.

### Not measured

**Whether a Seeker-only defence wins today is not known.** `BalanceReport --variety` plays each tower alone
under the name `only-seeker`, which is the measure this feature needs. A run with `--seeds 3 --levels 0,2`
was stopped after several minutes with no result, and the report prints nothing until it finishes.

## What this feature adds

### Stats: one shape

| Stat | Above 0 | Below 0 | At -100 |
|---|---|---|---|
| Resilience | Lower crit chance and bonus; 100 is immune | Higher crit bonus only | Bonus × 2 |
| Agility | Dodges aimed hits | See below | See below |
| Tenacity | Debuffs are shorter | Debuffs are longer | Duration × 2 |
| Spirit | More recovery | Less recovery | Recovery worth nothing |

Armor and magic resist keep their floor of 0.

### Negative Resilience

Resilience keeps doing crit potency, and positive Resilience keeps lowering crit chance taken. Below zero it
stops raising crit chance.

```text
crit bonus  = tower bonus × (1 - res/100)
crit chance = tower chance × (1 - max(res, 0)/100) × whatever negative agility adds
```

A consequence to weigh: a long burn no longer doubles crit chance, so crit builds get weaker unless
something else takes that part over.

### Negative Agility

The stat needs a meaning that replaces something another mechanic does.

1. ⭐ **Easier to crit, by multiplying.** Crit chance × `(1 - agility/100)` below zero, so -100 is × 2. The stat
   then runs from miss through normal to crit. It takes over the crit chance negative Resilience gives up,
   keeps today's strength at the floor, and needs a tower that can already crit.
2. **Easier to crit, by adding.** A flat share of crit chance, say +50% at -100. Every tower starts critting,
   including those with no crit today, at the default × 1.5.
3. **Slower.** Below zero, speed × `(1 + agility/100)`, so -100 means no movement and Freeze and Daze become
   "set Agility to -100". All slows then share one stat. A nimble enemy absorbs slows but loses its dodge. The
   cost is that a Skirmisher's matchup changes a lot, slows stop reading as slows, and speed has two inputs.
4. **A second hit.** A chance an aimed hit lands twice. It overlaps damage taken and adds another roll and more
   triggers.
5. **More aimed damage.** Only the Sniper and the Sonar would benefit, and it overlaps damage taken.

With option 1, these become possible:

- Freeze and Daze set Agility to -100, so every tower that can crit does so twice as often on a stopped enemy.
  The floor matters: a set value replaces the total, so setting a higher value could *raise* an enemy's
  Agility from, say, -70.
- The three "stopped means crit" bonuses are retired or become plain numbers. See open questions.
- Expose moves at least partly to Agility, because its words are "crits come more often and hit harder".
- Fast's "reckless" -20 reads better as Agility.

The dodge side stays on aimed hits. The crit side would apply to every hit that can crit.

### Tenacity replaces Chill, Burn and Freeze resist

- One stat: how long debuffs last on this enemy.
- Lore that must stay absolute stays as a separate immunity: the Yeti can't be frozen, the Salamander can't
  burn, and bosses keep their freeze immunity. Partial resists become Tenacity, which suits heavy enemies.

Curve options:

1. ⭐ **Diminishing above 0, linear below.** `100/(100+t)` above 0 and `1 - t/100` below. It matches armor and
   agility and never reaches immunity by accident, and immunity stays its own rule.
2. **Linear both ways.** `1 - t/100`, the same as Resilience, so 100 is immune. One formula, but a cliff at the
   top, and each late point is worth far more than the last.

Scope options:

1. ⭐ **Everything that doesn't help the carrier.** One rule, no list. Buffs, heals and shields are untouched.
2. **Crowd control only.** It reads as "tenacity" in other games, but leaves the other debuffs with nothing.
3. **Every effect.** It would shorten an enemy's own buffs, which rewards nobody.

Facts to weigh:

- It cuts total burn and poison damage, as Burn resist does today, and now Poison and Soulfire too, which
  nothing resists today. Soulfire exists to burn what is burn-immune; immunity would still leave that true.
- A field or ground effect refreshed every tick barely notices it, since only the time after leaving shrinks.
- Earned stacks and lasting effects have no duration to change.
- Doom pays out when it ends, so shortening it changes what it pays.
- Negative Tenacity makes things last longer, which `applyResisted` never does today.
- Should enemies get base Tenacity by rank? Unknown; ⭐ only by identity at first.

Rejected: keeping the three resists and deriving them from Resilience, Agility or Tenacity. It keeps everything
that exists today and adds a layer on top, and `td.stat` holds that no stat scales another on the sheet.

### Perma-CC

The goal: **a Seeker-only defence must not win a level by holding everything still.** Upcoming stops get
weaker; they aren't blocked.

1. ⭐ **A stop meter that shortens stops only.** Each enemy has a meter that fills by 1 a second while stopped
   and drains at a rate set by rank while it walks. Each new stop lasts × `100 / (100 + 100 × meter)`.
   Freeze and Daze share it.
2. **The same meter feeding Tenacity.** One stat does it all and it shows in that stat's breakdown, but
   every other debuff shortens too.
3. **A trait that triggers after a share of time stopped and grants Tenacity by rank** (the original idea). It
   needs a threshold and a timer, and it shortens every other debuff.
4. **Keep today's ladder.** Nothing changes, and the `TODO.md` entries stay.

Rejected: **Steadied**, a buff after a stop that blocks all stops for the stop's length × a rank number. A 0.3 s
Daze would block stops for 0.3 to 0.6 s, and a freeze landing then is wasted. It prevents rather than lowers.

What option 1 gives:

- **Never blocks.** A 3 s freeze straight after a 0.3 s Daze on a Veteran lasts 2.31 s.
- **The cap doesn't depend on how many Seekers fire.** Long-run stopped time settles at `drain / (1 + drain)`.
- **It is still diminishing returns,** based on time stopped rather than a count, with no ladder, window, hard
  zero or per-kind table.
- It can show through the existing next-share row.
- A kept-out stop no longer exists, so every missile counts toward the Seeker's freeze gate unless a stop would
  last under a tick. That makes the gate easier to reach.

Modelled with a 2 s stop, with share of time stopped and how long each stop lasts:

| Stop tried every | Today | Drain 1 | Drain 0.67 | Drain 0.5 |
|---|---|---|---|---|
| 6 s | 15% | 33%, 2.00 s | 33%, 2.00 s | 33%, 2.00 s |
| 4 s | 17% | 50%, 2.00 s | 40%, 1.59 s | 33%, 1.33 s |
| 2.25 s | 22% | 50%, 1.12 s | 40%, 0.90 s | 34%, 0.75 s |
| 1.5 s | 23% | 50%, 0.75 s | 40%, 0.60 s | 34%, 0.50 s |
| 1 s | 27% | 50%, 0.50 s | 40%, 0.40 s | 34%, 0.33 s |
| 0.5 s | 27% | 51%, 0.25 s | 41%, 0.20 s | 34%, 0.17 s |
| 0.25 s | 27% | 51%, 0.13 s | 41%, 0.10 s | 36%, 0.08 s |

Why not feed Tenacity (options 2 and 3):

- After a single 2 s freeze, Tenacity is +200, so the next burn or Expose lasts a third as long, and that burn
  deals about a third of its damage.
- Under a lock, Tenacity climbs to about 340 to 550, leaving debuffs at 15 to 23% of their duration.
- A slower fill helps after a single stop, but under a lock Tenacity must still climb that high. In the model
  the fill rate changes how soon a lock loosens and not the long-run share.
- That punishes Seeker plus Cinder, the pairing removing the freeze and burn rules opens up.

Caps to start from, chosen to sit around today's 27%:

| Rank | Cap | Drain |
|---|---|---|
| Grunt | 60%, or exempt | 1.5 |
| Soldier | 50%, or exempt | 1 |
| Veteran | 40% | 0.67 |
| Elite | 33% | 0.5 |
| Boss | 25% | 0.33 |

The first idea was that Grunts and Soldiers could be held forever. That works against the goal:

- With Deep Freeze, one Seeker can hold one enemy forever.
- Absolute Zero freezes an area.
- The Seeker moves on from frozen enemies by itself.
- Every built-in level sends Soldiers.

A held enemy also stays alive, and the round waits for it.

### Chill and slows

- A slow takes its full % of speed for its duration. Slows don't stack: the highest applies.
- The chill stops being a level that adds up, so its cap and per-application fading go.

Across which slows?

1. ⭐ **All of them:** Chill, Tar, Drag, Anchor and any other. "The strongest slow applies" is one sentence, and
   Anchor's lowest-cap rule already works this way.
2. **Chill only.** A smaller change, but slows of different kinds keep multiplying.

Consequences:

- A second chill tower adds uptime, not strength, so values need retuning against today's 80% cap.
- Holding speed to a share of base also holds it against speed gains: Sprint, Hasten and `HurtSpeedTrait` do
  nothing while slowed. Then Hasten's "no slow takes hold" can go, and a slow always beats a speed gain.
  Open: is that wanted?
- Poison's slow by fullness goes under this. ⭐ Poison becomes damage and Sicken only. Otherwise a fixed slow.

A weak long slow against a strong short one. Today's "stronger and longer" refresh would turn 60% for 1 s plus 10%
for 8 s into 60% for 8 s.

1. ⭐ **Compare current values and ignore the weaker** while the stronger runs.
2. **Keep one share per source** and apply the highest. It is exact, but heavier to read and run.

### Speed coming back

**Speed must not jump from 20% to 100% in one tick.**

1. ⭐ **The slow fades over the last second of its own duration,** from its full value down to nothing. 20% speed
   back to 100% takes 1 s, about 4 points of speed a tick. It depends only on the ticks left. The icon stays until
   the slow is gone, so nothing is hidden. A field or ground slow stays full while refreshed and fades once the
   enemy leaves. A slow shorter than a second fades over its whole length.
2. **Today's linear fade over the whole duration.** No hold at full value, which is what the change moves away from.
3. **Speed ramps up in movement, for everything.** One rule covering every slow and speed gain, but it applies after
   stops too. With a stop every 0.5 s lasting 0.25 s, an enemy on a 1 s ramp walks at about 6% of normal speed,
   which brings perma-CC back past the cap. It also hides a slow with no icon. An exception for stops would solve
   that with one more rule.
4. **Fade speed set by Agility or Tenacity.** Nimble enemies recover faster, which fits lore, but it is one more
   thing a stat does, and a fade length you can't read off the enemy.
5. **No recovery.** The simplest, and rejected: it is the jump this section exists to prevent.

A thaw still jumps from 0 to full speed under option 1. Open: is that fine, or does it want visual smoothing only?

### Interactions

| Rule today | Advice |
|---|---|
| A frozen enemy can't burn, and a freeze puts out burn and Soulfire, landing part at once | **Decided: remove** |
| Thermal Shock, and `PoolTuning.freezeBurstShare` | Follow from that; see open questions |
| Rime: full freeze burst, twice the chill's extra freeze time; Ash: pools hold × 2 and mark × 2 as fast, chill × 0.25, keeps freeze out; each replaces the other | **Decided: one hex giving negative Tenacity** |
| A chill cuts burn damage, up to half at the cap | ⭐ Remove: a hidden penalty |
| A chill or Undertow lengthens a freeze | ⭐ Remove; Undertow's special rule goes with it |
| A freeze removes a chill | ⭐ Remove: the chill resumes after |
| Tar: a freeze lasts 1 s longer | ⭐ Remove |
| Tar: a new fire starts × 2 | ⭐ Remove. Otherwise the one combination kept, if it is worth one sentence |
| Reveal implies Expose | ⭐ Remove: a tower wanting both applies both, as Wide Band already does |
| Brittle counts only during a Freeze | ⭐ Remove: the Seeker applies both together with the same length |
| Hasten keeps slows out; Poison's slow stops under Hasten | ⭐ Remove, with the slow rule above |
| Poison's slow by fullness | ⭐ Remove |
| A burning pool above a fuel threshold reveals | ⭐ Any burn reveals, with no hidden threshold. Otherwise no reveal at all |
| Dead Zone keeps heals and shields out; Fallout keeps heals out | ⭐ Both set Spirit to -100, so recovery is driven by Spirit only. Stronger: it also cancels regeneration and shields already there |
| Unveil keeps Invisibility out | ⭐ Merge into Reveal |
| Inversion keeps Invisibility out | ⭐ Remove: Inversion only turns heals and shields into damage |
| Burn and poison earn Scorch and Sicken | Keep: visible, and core to those towers |
| Reveal beats Invisibility | Keep |
| A Mark waits on a crit-immune enemy | Keep |
| A stop can't be renewed while it holds | Keep, or the stop meter never gets a say |

This takes `EffectInteractions` from 13 tables to about 3.

Also worth a look: Saturation, "the Splash's own rhythm, not a stat", is damage taken from one tower only. It is
left alone unless the tower revamp changes it.

### Out of scope

- Rethinking the Hexer's hexes as a whole: that belongs to the tower revamp.
- Tuning every number: only what these changes need.
- New towers or enemies.

## Interconnections

- `FEATURE-tower-visual-revamp.md` carries a note: hexes are rethought there, and Rime and Ash are one
  negative-Tenacity hex until then. That doc still says "visuals only" and rules tower abilities out of scope.
- `FEATURE-enemy-stats.md` and `FEATURE-effect-interactions.md` (shipped) dropped Tenacity. This reverses that.
- `FEATURE-effect-diminishing-returns.md` (shipped) is what the stop meter replaces.
- `391c995f` and `d6316448` moved recovery to Spirit alone; Dead Zone and Fallout finish that.
- ADR 0015 says every rule between kinds is a row of `EffectInteractions`. That stands, with far fewer rows.
- `TODO.md`'s two diminishing-returns entries close with this work. "Effect and specialization numbers" changes.

## Constraints and open risks

- **Balance moves everywhere at once.** Nothing above has been played. Each part needs `BalanceReport --compare`,
  and the Seeker-only margin needs `--variety`, which is slow and silent until done.
- **The model is not the game.** It has one enemy, fixed intervals and no damage. A 25 to 40% stopped enemy still
  takes far longer to walk, giving a Seeker much more time to shoot, so Seeker-only might still win by damage.
  That wouldn't be winning by holding everything still, and the request should say what counts.
- **Crit builds lose their strongest multiplier** if negative Resilience stops raising chance. The Sonar's identity
  rests on Expose.
- **Stopped enemies taking more crits raises the worth of every stop.** That works against the perma-CC goal.
- **Towers lose identity with the rules they read:** Thermal Shock and `freezeBurstShare` (Cinder), Frostbite (Seeker),
  Tar and Cryo shells (Mortar), Undertow and Stasis (Obelisk), Rime and Ash (Hexer).
- **Bosses rely on freeze immunity,** so immunity has to survive the removal of the resist stats.
- **Duration × 2:** effects have never been lengthened through resistance. A Sonar mark timed to the next pass, a
  shared stack clock and Doom's payout all read a duration.
- **A new base stat is expensive to author:** `withStats` sets all seven and is called in 24 files. Tenacity needs a
  way in that doesn't touch them all, or it touches them all.
- **Standing rules:** a new stat has a case in `td.ui.StatLooks`; a shipped node id never changes
  (ADR 0016), so Rime's and Ash's nodes keep theirs; effect sentences are built from rules and constants, never
  typed numbers; `StatSheet` allocates nothing; the tick budget in root `CLAUDE.md` holds; simulation
  iteration order stays fixed; all chance goes through the injected random source.
- **Tests:** removing rules removes the tests that prove them (`EffectInteractionsTest`, `ActiveEffectsTest`, and
  parts of `SeekerTowerTest`, `CinderTowerTest`, `MortarTowerTest`, `SplashTowerTest`). Check what each exercised
  in passing before deleting it.

### Texts and docs to review

Decided: **all texts and docs get reviewed.** Shipped features, shipped plans and release notes stay as history.

A search for these rules found 48 live files. Some are false hits (`Reach`'s dead zone is a different thing), and
the search should be repeated when this is planned.

- **Docs:** `README.md` (74 lines on these topics), `TODO.md`, `.claude/skills/run-jtd/SKILL.md`, and `CLAUDE.md` in
  `td/effect`, `td/tower`, `td/tower/cinder`, `td/tower/mortar`, `td/zone`.
- **Rules and their words:** `DevEffects`, `balance/WaveCheckReport`, `effect/ActiveEffects`, `effect/Effect`,
  `effect/EffectInteractions`, `effect/EffectKind`, `effect/EffectSentences`, `effect/EffectTag`,
  `enemy/AbilityText`, `enemy/MobAbilityContext`, `stat/EnemyStat`.
- **Towers:** `CinderTower`, `MortarTower`, `PulseTower`, `SeekerTower`, `SplashTower`, `TowerStat`, `TowerTerm`,
  `cinder/CinderSpec`, `cinder/ThermalShockMechanic`, `mortar/NukeSpec`, `mortar/ShellType`,
  `mortar/TacticalNukeMechanic`, `pulse/FieldMode`, `pulse/ModeMechanic`, `pulse/UndertowMechanic`,
  `seeker/BrittleMechanic`, `seeker/FreezeSpec`, `splash/CovenHex`, `targeting/BeyondRadiusTargetQuery`,
  `targeting/Reach`.
- **UI and zones:** `ui/EffectPictograms`, `ui/EnemyFrameBuilder`, `ui/Java2DFrameRenderer`, `ui/PerkPictograms`,
  `ui/ZoneFrameBuilder`, `ui/render/HexGlyph`, `ui/render/Palette`, `zone/ZoneEffects`, `zone/ZoneKind`.

That search looked for fire, ice and heal-denial rules. Resists and diminishing returns reach further, at least:
`td/enemy/CLAUDE.md`, `enemy/DefinedEnemyMob`, `enemy/EffectResistTrait`, `enemy/EnemyInspection`,
`enemy/MobEvent`, `enemy/StatRows`, `effect/DiminishingReturns`, `effect/EffectRule`, `ui/EnemyStatText`,
`ui/StatLooks`, `seeker/FrostbiteMechanic`.

Already stale, and fixed by the same review: `TODO.md` says Scorched and Sickened "never wear off" at -100
spirit, which reads as false since spirit stopped pacing timers in `391c995f`.

## Decisions made

- The goal is simpler reading and simpler code: fewer edge cases, exceptions and conditions, and one place per
  mechanic.
- Freeze and burn stop interacting completely. All texts and docs get reviewed for it.
- Rime and Ash become one hex that gives negative Tenacity. The hexes get rethought in the tower revamp, and that
  doc carries the note.
- Speed must not jump from a heavy slow back to full in one tick.
- Perma-CC must not let a Seeker-only defence win. Upcoming stops are lowered, not blocked, so Steadied is out.
- Resilience keeps crit potency, and positive Resilience keeps lowering crit chance. Negative Resilience gets reworked.
- This doc is finalized in another session.

## Open questions

### Must answer before planning

1. **Grunts and Soldiers under the stop meter.**
   - ⭐ The meter with looser caps, around 60% and 50%. Closes the hole, and they can still be held for long stretches.
   - Exempt, as first thought. Keeps the idea and may leave Seeker-only strong early.
2. **Does the meter shorten stops only?**
   - ⭐ Stops only. Tenacity no longer covers every change to a duration on its own.
   - It feeds Tenacity. One stat, every other debuff shortens with it.
3. **What is a Seeker-only win?** A margin from `--variety`, on which levels, and against what? Should only-Seeker fall
   behind other single-tower defences, or simply lose? Get today's number first.
4. **Meaning of negative Agility.** ⭐ Crit chance by multiplying; options above.
5. **Which slows lose stacking?** ⭐ All of them.
6. **Do slows beat speed gains?** ⭐ Yes, and Hasten's blocking rule goes.

### Tower consequences

7. **Thermal Shock and `freezeBurstShare`.** With freeze and burn apart, what triggers Thermal Shock?
   - ⭐ Replace it with a mechanic that doesn't read a freeze.
   - Remove the node's effect and leave a stat.
8. **The three "stopped means crit" bonuses.**
   - ⭐ Stops set Agility to -100, and the bonuses become plain numbers or new mechanics.
   - Keep them on top of it, which may be too strong.
   - Keep them and leave Agility at 0 on a stop, so a stop gives no crits from the stat.
9. **Frostbite** reads diminished freezes. What is its condition without a ladder? A frozen enemy only?
10. **Expose:** all Agility, or split between Agility and Resilience?
11. **Fast's "reckless":** -20 Agility instead of -20 Resilience?
12. **Dead Zone and Fallout** at Spirit -100 also cancel regeneration and shields already on the enemy. Is the stronger version
    wanted, or a value above the floor? Do they stay two effects?
13. **Undertow** without its chill equivalence: what does the node give?
14. **Tar** without its fire and freeze rules: is a plain slow enough for that shell?
15. **The Seeker's freeze gate** gets easier when no stop is kept out. Raise it?
16. **Doom** pays when it ends: should Tenacity change its length?

### Numbers and names

17. **Is Tenacity the right name?** And what does the player see the meter as: a word, a row, the next-share line only?
18. **Base Tenacity:** by identity only (⭐), or a little by rank?
19. **Which sources give negative Tenacity,** other than the hex? How low should it go in play?
20. **Caps by rank:** the table above, or tighter at the top?
21. **The fade's length:** ⭐ 1 s, or shorter?
22. **A thaw's jump** from 0 to full speed: leave it (⭐), or smooth it visually?
23. **Burn reveal:** any burn (⭐), or no reveal?
24. **Tar's fire × 2:** the one kept combination, or gone (⭐)?
25. **Should Agility's dodge reach more than aimed hits?** Removing the "aimed" condition would be one less rule and a large
    balance change. ⭐ Not now.

### Sequencing

26. One feature, or several? Each part can be measured alone in this order: cut the interactions, rework Resilience and
    Agility, change chill and the slows, add Tenacity, replace diminishing returns. Planning should challenge this.

---

## Appendix: the request as given

Typos fixed. The options above came out of a design conversation on this request.

> list me all enemy stats, list all invisible stats (there used to be "healing received", maybe there is some related to crit,
> because tower abilities need them). My goal is to review it and do similar I did with spirit few commits earlier - I removed
> shield and heal related hidden stats, all is driven by spirit only.

> Help me simplify this. I am considering following changes. Goal is to simplify how the game reads, works internally, removing
> edge cases, exceptions, conditions everywhere and places that implement/evaluate the same mechanics at multiple places in the
> code.
>
> Make agility go negative, and think up what it can mean, to replace something that other mechanic in the game does.
>
> Make negative resilience weaker, it increases crit chance taken so -100 resilience means everything always crits, right? That
> is too powerful. Resilience should keep doing crit potency, and positive resilience lowers crit chance taken. But negative
> resilience needs rework.
>
> Remove slow and other effects resists, introduce tenacity, or something like that, that does it all. But it may break some lore
> enemies, such as frost themed ones, or fire themed ones. Is it a good change? Or perhaps introduce a mechanics - crit potency,
> chill/burn/freeze resists, which are affected by resilience/agility/tenacity and traits.
>
> I would like to remove some effect interactions, they make the game hard to read and hard to plan. Advise me please.
>
> I want to remove diminishing returns completely. Perma daze and perma freeze should be prevented somehow. My idea is that perma
> hard CC of grunt/soldier enemies will be possible, but each and every veteran+ enemy gets native trait that triggers when it is
> hard CCd for significant portion of time and it buffs itself with tenacity, stronger per higher ranks. Tenacity makes all effects
> last longer/shorter, longer for negative (if negative value is allowed).
>
> I am considering removing chill slowing linearly, it instead slows full % of move speed for the duration of the effect, effects
> do not stack, always the highest slow is applied only, and when it stops, the enemy is getting faster up to the base speed, based
> either on agility, tenacity, or based on nothing. Opinion?
>
> Having agility and tenacity go negative to -100 would make all main stats behave the same. But what it does when it is negative
> needs to be thought of and balanced.

> Recovery: I'd have none. - rethink this again, I would like to prevent speed jumps from 20% speed to 100% speed in one tick
>
> remove diminishing returns - does it mean ending 0.3s daze prevent another 3s freeze from landing? The tenacity solution proposed
> solves this by lowering upcoming perma cc, not by preventing it. Think again. What I want is to prevent seeker-only levels from
> winning the game by perma cc everything.
>
> questions:
> 1 yes, review all texts and docs
> 2 explain more
> 3 yes, I will rethink hexes in the upcoming tower revamp feature. Make a note there.

> when done, create a feature request doc, with many details, all suggestions, options and open questions, put many details there
> as I will finalize it in another session, commit it, push it
