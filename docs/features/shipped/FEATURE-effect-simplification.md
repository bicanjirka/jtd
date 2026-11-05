# Feature Request: Effect Simplification

**Status: implemented.** Decided while building it: Reveal on an already invisible enemy strips nothing, and the cloak
resumes after; Daze immunity also goes on the reborn twins, the two weaker Warden stages and the two hatching eggs;
Brittle only comes with a Freeze that landed; fallout ground keeps its Sickened stacks; Thermal Shock fires on a kill, not
a leak, for Burn only, once per enemy, from every Cinder tracking it; of one kind a stronger shorter slow loses the
weaker tail; only Fast moved to negative Agility. Chill's own numbers are left for the designer. The Freeze cap holds in
the long run: a try every tick can hold an enemy for a long first stretch, bounded by the freeze's authored length in
seconds of meter.

## Summary

Make enemy stats and effects simpler to read and simpler to build on. Remove edge cases, exceptions and conditions,
and places that work out the same mechanic twice. A player should be able to read an enemy and plan a defence without
knowing hidden rules.

Five changes carry it:

- Cut most of the rules by which one effect changes another.
- Rework crit: Agility goes negative and takes over crit chance from negative Resilience.
- Give all slows one rule: the strongest applies, and it fades out at the end.
- Replace the per-effect resists with one stat, Tenacity, and a separate immunity.
- Replace diminishing returns with a meter that makes perma-freeze impossible.

## Current state (what exists today)

### Enemy stats

16 stats, all in `td.stat.EnemyStat`. Stealth is the only one with no row in the enemy pane; Move speed always has one;
the rest show when their base or current value is off the default (`StatRows.shows`).

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

No hidden stat remains: healing received went in `391c995f`, and Resilience is the only enemy stat crits read.

### Crit

- -100 Resilience doubles crit chance and doubles the crit bonus. A 5% tower gets 10%, and a 50% tower always crits.
- Lowered by Scorch (-1 a stack, up to 100 stacks), Fracture (-10 a stack, up to 5, or 10 with Fault Line) and Expose
  (-50). Reveal implies Expose. Raised by Unyielding (+100).
- A Mark is a guaranteed crit on the next hit and waits on a crit-immune enemy. A Charge discharges twice as hard on a
  crit.
- Fast has -20 Resilience up to Elite ("reckless"); its Boss rank has 100.
- "A stopped enemy gets crit" exists three separate ways:
  - The Sniper's node (`SniperTower`, `HitCondition.TargetStopped`, used by nothing else): +100% crit chance and +100%
    crit damage.
  - Burst's Lightning Rod (`ArcSpec.stoppedCritDamage`): +50% crit damage on arcs.
  - The Seeker's Frostbite: a guaranteed crit on a frozen enemy, or one whose next freeze is diminished. The Seeker has
    no crit chance of its own.

### Agility and aimed hits

- Only aimed hits can be dodged, and only the Sniper's shots and bounces and the Sonar's beam are aimed. A Sniper's line
  (Railgun) is not.
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

- Immunity has no check of its own: it is a resist of 100%, which scales the duration to nothing, and an application
  under one tick is blocked (`DefinedEnemyMob.applyResisted`).
- `applyResisted` only ever shortens. `Effect.withDurationScaledBy` also makes a pool decay faster, so shortening a burn
  or poison cuts its total damage, and it leaves a lasting effect alone.
- Nothing resists Poison, Soulfire or Daze. The Daze ladder is the only protection enemies have against Daze, bosses
  immune to freeze included.

### Diminishing returns

- `DiminishingReturns`, one ladder per mob per kind. Freeze 100/50/25/0%. Daze 100/80/60/40/20/0%.
- The window is 10 s from the last fresh application. A stop that is kept out doesn't record, so the window still runs
  out.
- Freeze and Daze climb separate ladders, so alternating them gets around each.
- A stop can't be renewed while it holds, per kind (`ActiveEffects.apply`), so a Daze can land during a Freeze.
- A kind stays listed in the pane while its ladder runs, even after the effect has ended, with the next share under it
  (`DefinedEnemyMob.effectKindsByGain`, `EnemyStatText`). The Seeker's Frostbite reads `freezeDiminished`.
- Two open `TODO.md` entries are about it: "A hard-control kind without a ladder fails in play" and "A hard crowd-control
  kind's diminishing-returns ladder is a switch that throws in play".

### Stops

| Source | Length |
|---|---|
| Seeker missile | Freeze 1.5 s; 2.25 s with Deep Freeze |
| Seeker Absolute Zero | The same freeze, on everything within its area |
| Mortar Cryo shell | Frost ground: chill, and a freeze on an enemy that stays |
| Mortar Heavy Shell | Daze 0.5 s, small radius |
| Obelisk (`PulseTower`) Snare | Daze 0.5 s, once per visit |
| Obelisk Stasis | Daze 1 s at full Toll, tied to Toll and the Stasis effect |
| Burst Thunderstrike, Thunderclap, Arc crits | Daze, 1 s when long |

- The Seeker's cooldown is 2.25 s, so with Deep Freeze its freeze lasts as long as its cooldown. It aims at the fastest
  visible enemy, so a frozen one is skipped. Its head III gates count landed freezes (`FREEZES_NEEDED`).
- Burst fires every 19 ticks and a long Daze lasts 20, so one arc crit holds an enemy until the next shot. Mastery adds
  10% arc crit chance, and Overload adds more on a saturated enemy.
- A stopped enemy can't cast. The README names Daze and Freeze as ways to stop the Keeper of Hours from casting.

### What a stop-locked enemy does today

From a small model of one enemy, not the game. A 2 s stop is tried at a fixed interval, and a held enemy is not renewed.
It ignores damage, targeting and the walk.

| Stop tried every | Share stopped under today's ladder |
|---|---|
| 6 s | 15% |
| 4 s | 17% |
| 2.25 s | 22% |
| 1.5 s | 23% |
| 1 s or less | 27% |

### Slows

Slows **multiply** today, and each kind has its own shape:

| Kind | How it works | Numbers |
|---|---|---|
| Chill | A level made of one contribution per application, each fading linearly on its own slope | Cap 80%; 20% on frost ground, 30% from Rime, 40% from Thermal Shock |
| Tar | × a fixed share while it lasts | × 0.6 |
| Drag | Stacks of 1% of speed, held by a field | 15 base, up to 50 |
| Anchor | Holds speed to a share of base; the lowest cap wins and it also holds against speed gains | 75% |
| Poison | Scales with how full the pool is | Up to 30% |

Speed gains: Sprint +60%, Hasten × 2. While Hastened, Chill, Tar, Drag and Anchor can't take hold, and Poison gives no
slow. `StatModifier.capAtShareOfBase` is what Anchor uses.

### Effect interactions

`EffectInteractions` holds 13 tables. `ActiveEffects` also holds its own rules: the chill cutting a burn, a burn
revealing above a fuel threshold, poison's slow.

- Turning invisible removes a Reveal, and Unveil exists to stop that.
- A Rewind counts as a heal only through a check that heals are kept out (`DefinedEnemyMob`), and Spirit plays no part in
  it.
- Thermal Shock reads a freeze landing on a burning enemy, and `PoolTuning.freezeBurstShare` holds how much lands.
- Undertow is a flag beside the field's drag that counts as 25% chill for a freeze.
- Rime Coven and Ash Coven are the two sides of one exclusive choice at the Hexer's head IV (`SplashTower`), and Rime also
  applies 30% Chill when cast (`CovenHex`). Both target the healthiest enemy without the hex.

## What this feature adds

### Stats: one shape

| Stat | Above 0 | Below 0 | At -100 |
|---|---|---|---|
| Resilience | Lower crit chance and bonus; 100 is immune | Higher crit bonus only | Bonus × 2 |
| Agility | Dodges aimed hits | Crit chance rises | Chance × 2 |
| Tenacity | Debuffs are shorter | Debuffs are longer | Duration × 2 |
| Spirit | More recovery | Less recovery | Recovery worth nothing |

Armor and magic resist keep their floor of 0.

### Crit

```text
crit bonus  = tower bonus × (1 - res/100)
crit chance = tower chance × (1 - max(res, 0)/100) × (1 - min(agility, 0)/100)
```

- A tower with no crit chance stays without one. Dodge stays on aimed hits; the crit side applies to every hit that can
  crit.
- **Freeze and Daze set Agility to -100,** so a stopped enemy can't dodge and every tower that can crit does so twice as
  often on it.
- **Expose** is -50 Agility and -50 Resilience, which on an enemy with neither gives today's × 1.5 chance and × 1.5 bonus.
  It now also cuts dodge. Its Agility half does nothing on a stopped enemy.
- **Fast's "reckless"** is -20 Agility below Boss rank. Its Boss rank keeps 100 Resilience and has no negative Agility.
- **Scorch and Fracture** stay on Resilience, so they raise only the crit bonus. A long burn no longer raises crit chance.
- **The three stopped-crit bonuses become plain crit stats** with no condition, and stopped-means-crit lives in Agility
  alone. The Sniper node gets crit chance and crit damage, Lightning Rod crit damage, and Frostbite crit chance. Frostbite
  starts at +50%, which reaches 100% on a frozen enemy. Node ids stay.

### Tenacity and immunity

- Tenacity replaces Chill, Burn and Freeze resist. It changes how long everything that doesn't help the carrier lasts on
  this enemy: stops, slows, pools, debuffs, marks, hexes and Doom, with no list and no exception. Buffs, heals and shields
  are untouched, and effects with no duration have nothing to change.
- Duration × `100/(100+t)` above 0 and `1 - t/100` below, down to -100.
- Tenacity changes pool damage: high Tenacity cuts a burn or poison's total, and -100 about doubles it.
- A Doom on a high-Tenacity enemy collects less and pays sooner, and a longer one collects more.
- **Only immunity keeps an effect out.** Scaling never reduces an effect below one tick. The Freeze meter is the one
  exception, below.
- **"Immune to X" is its own authored rule,** naming any kind, Daze included, used only where a fight needs it. Soulfire
  still burns what is immune to Burn, because it is its own kind.
- Base Tenacity comes from an enemy's identity only, never from rank.
- Partial resists convert by one test: is the enemy themed on that element, or just tough?

| Enemy | Becomes |
|---|---|
| Yeti | Immune to Freeze and Chill |
| Salamander | Immune to Burn |
| Magma | Immune to Burn at every rank |
| Juggernaut | +43 Tenacity |
| Armored | +33 Tenacity from Veteran; nothing below that |
| Colossus | Immunities stay; the 50% stages become +100 Tenacity |
| Storm Twins | +100 Tenacity on the forms that had 50% |
| Warden | Immunities stay |
| `LabyrinthLevel`, `RiftLevel` enemies | Immune stays; 50% becomes +100 Tenacity |

### Malaise

Rime and Ash become one hex, Malaise, that gives -50 Tenacity. It is the only source of negative Tenacity, so the floor
isn't reached in play yet. The hexes get rethought in the tower revamp.

Rime Coven and Ash Coven stay as the two sides of their exclusive choice, with their names and node ids. Both give Malaise,
and each adds one thing on cast, so the choice stays real: Rime Coven its existing 30% Chill, and Ash Coven a small Burn.
Neither reads another effect's rules.

### The Freeze meter

**Perma-freeze is impossible, by construction.** Nothing measures this: if the competent player starts winning with
Seeker-heavy builds, that is a balance signal for the designer to handle.

- Each enemy has a meter that fills by 1 a second while frozen and drains at a rate set by rank while it walks.
- A freeze lasts `authored × Tenacity factor × 1/(1 + meter)`. The meter shortens Freeze and nothing else.
- Over time an enemy can't stay frozen longer than `drain / (1 + drain)` of the time, however many towers fire. A test can
  prove this by firing freezes at an enemy every tick.
- **A freeze has no minimum length.** One that would last under a tick doesn't happen. A floor would break the guarantee: at
  20 ticks a second, one-tick stops with one-tick gaps hold any rank half the time.
- Every rank has the meter; none is exempt. A Freeze can't be renewed while it holds.

| Rank | Cap | Drain |
|---|---|---|
| Grunt | 60% | 1.5 |
| Soldier | 50% | 1 |
| Veteran | 40% | 0.67 |
| Elite | 33% | 0.5 |
| Boss | 25% | 0.33 |

**What the player sees:** only the existing line. The Freeze row stays listed while the meter is above zero and reads "the
next one lasts N%", and it goes when the meter empties. The meter has no name, the recovery rate shows no number, and "kept
out" goes. The Freeze sentence says time frozen shortens the next one, and walking wins that back, more slowly at higher
rank.

### Daze

Daze has no meter and no ladder, and it refreshes like any other effect, keeping the stronger and the longer. Perma-Daze is
possible and left to game design, which keeps it rare. Freeze and Daze both stay: Freeze is the limited stop, and Daze is a
short interrupt with no limit on how often.

**Immune to Daze:** The Warden, the Iron Colossus and Molten Core, The Thunder, The Lightning, The Monolith and Talos, whose
casts are what their fights are about. The Keeper of Hours and The Minotaur stay open to it, because interrupting them is a
fair answer. No regular enemy is immune at any rank, Boss rank included.

### Slows

- **A slow holds speed to a share of base, for its duration.** A 40% slow means speed is at most 60% of base. This covers
  Chill, Tar, Drag and Anchor, and any later slow.
- **The strongest applies.** Slows don't stack, and a second slow tower adds uptime, not strength. Chill stops being a level
  that adds up, so its cap and per-application fading go.
- **A slow beats any speed gain,** so Sprint, Hasten and `HurtSpeedTrait` do nothing while slowed, however weak the slow.
  Hasten's "no slow takes hold" goes. Anchor becomes a plain 25% slow.
- **Of the same kind, a weaker slow is ignored while the stronger runs,** so 60% for 1 s plus 10% for 8 s doesn't become 60%
  for 8 s.
- **Speed comes back over the last 0.5 s of a slow's own duration,** from its full value to nothing, so 20% speed returns to
  100% in 10 ticks. A slow shorter than that fades over its whole length. The icon stays until the slow is gone. A field or
  ground slow stays full while refreshed and fades once the enemy leaves.
- A thaw still jumps from 0 to full speed, with no visual smoothing.

### Interactions

`EffectInteractions` goes from 13 tables to 2: pools earning marks, and Reveal against Invisibility.

| Rule today | Becomes |
|---|---|
| A frozen enemy can't burn; a freeze puts out burn and Soulfire, landing part at once | Removed |
| Rime and Ash, and each replacing the other | Removed; see Malaise |
| A chill cuts burn damage | Removed |
| A chill or Undertow lengthens a freeze | Removed |
| A freeze removes a chill | Removed: the chill resumes after |
| Tar: a freeze lasts 1 s longer; a new fire starts × 2 | Removed: Tar is a plain 40% slow |
| Poison's slow by fullness | Removed: Poison is damage and Sicken |
| Reveal implies Expose | Removed: a tower wanting both applies both |
| Brittle counts only during a Freeze | Removed: it can outlast a Freeze the meter shortened |
| Hasten keeps slows out; Poison's slow stops under Hasten | Removed |
| Inversion keeps Invisibility out | Removed: Inversion only turns heals and shields into damage |
| A burning pool above a fuel threshold reveals | Any burn reveals |
| Unveil keeps Invisibility out; Invisibility removes a Reveal | Unveil merges into Reveal, and a Reveal blocks Invisibility |
| Dead Zone keeps heals and shields out; Fallout keeps heals out | Dead Zone absorbs Fallout as one effect that sets Spirit to -100; the ground zone stays and applies it |
| A Rewind is kept out like a heal | A Rewind's gain is scaled by what Spirit makes it worth |
| Burn and poison earn Scorch and Sicken | Kept |
| A Mark waits on a crit-immune enemy | Kept |
| A Freeze can't be renewed while it holds | Kept, for Freeze only |

Dead Zone is stronger than either rule today: it also cancels regeneration and shields already on the enemy, and Fallout's
ground now denies shields. Crumble still ignores Spirit. Sicken now weakens a Rewind too.

### Towers

- **Thermal Shock (Scorcher):** when a burning enemy dies, what is left of its Burn lands on nearby enemies at 150% of its
  value, and they gain Chill. It reads no other effect. The node id stays.
- **Undertow (Obelisk):** the Undertow effect and its chill rule go. The node keeps its deeper drag and Anchor. Anchor only
  matters until the drag passes 25%, and that stays until the tower revamp.
- **The Seeker's freeze gate** stays at 25 landed freezes, which come sooner now.
- **Frostbite, Lightning Rod and the Sniper node:** see Crit.
- **Mortar's Tar shell** is a plain slow; see Interactions.

### Out of scope

- Rethinking the Hexer's hexes as a whole: that belongs to the tower revamp.
- Tuning every number: only what these changes force.
- Rebalancing Burst: the designer's next task.
- Measuring a Seeker-only defence, or any gate on balance numbers.
- New towers or enemies.
- Dodge reaching more than aimed hits.

## Interconnections

- `FEATURE-tower-visual-revamp.md` carries a note: hexes are rethought there, and Rime and Ash are one negative-Tenacity hex
  until then. That doc still says "visuals only" and rules tower abilities out of scope.
- `FEATURE-enemy-stats.md` and `FEATURE-effect-interactions.md` (shipped) dropped Tenacity. This reverses that.
- `FEATURE-effect-diminishing-returns.md` (shipped) is what the Freeze meter replaces.
- `391c995f` and `d6316448` moved recovery to Spirit alone; Dead Zone, Fallout and the Rewind finish that.
- ADR 0015 says every rule between kinds is a row of `EffectInteractions`. That stands, with far fewer rows.
- `TODO.md`'s two diminishing-returns entries close with this work. "Effect and specialization numbers" changes.

## Constraints and open risks

- **Balance moves everywhere at once, and nothing above has been played.** These are accepted as signals for the designer:
  - Seeker builds may get stronger: four of the five caps are looser than today's 27%, and a sparse stop goes from 15% to
    33%.
  - Daze makes crits likelier, and Burst's arc crits cause Daze, with no meter to break that loop until Burst is rebalanced.
  - Slows get much weaker overall until Chill's values are reset, and a weak slow fully cancels Sprint and Hasten.
  - Crit builds lose the chance multiplier from burn and Fracture.
  - The plain crit nodes work on enemies that aren't stopped, so they are stronger overall.
  - The Yeti and Magma get stronger against frost and fire towers, and heavy enemies now shorten every debuff.
  - Snare, Stasis and Burst's Daze do nothing against the Daze-immune bosses, which weakens the Obelisk's Gravity line in
    boss rounds. Perma-Daze stays possible on The Keeper of Hours and The Minotaur.
- **Towers lose identity with the rules they read:** Thermal Shock (Scorcher), Frostbite (Seeker), Tar and Cryo shells
  (Mortar), Undertow (Obelisk), Rime and Ash (Hexer).
- **Duration × 1.5:** effects have never been lengthened through resistance. A Sonar mark timed to the next pass, a shared
  stack clock and Doom's payout all read a duration, and pools deal more.
- **Tenacity is a new base stat.** `withStats` sets the current seven and is called in 24 files; a stat authored by its own
  named method needs none of them to change, but `td/enemy/CLAUDE.md` describes the seven.
- **`td/enemy/CLAUDE.md` says there is no separate immunity check,** and that every enemy diminishes hard crowd control.
- **Standing rules:** a new stat has a case in `td.ui.StatLooks`; a shipped node id never changes (ADR 0016); effect sentences
  are built from rules and constants, never typed numbers; `StatSheet` allocates nothing; the tick budget in root `CLAUDE.md`
  holds; simulation iteration order stays fixed; all chance goes through the injected random source.
- **Tests:** removing rules removes the tests that prove them (`EffectInteractionsTest`, `ActiveEffectsTest`, and parts of
  `SeekerTowerTest`, `CinderTowerTest`, `MortarTowerTest`, `SplashTowerTest`). Check what each exercised in passing before
  deleting it.

### Texts and docs to review

**All texts and docs get reviewed.** Shipped features, shipped plans and release notes stay as history.

A search for these rules found 48 live files. Some are false hits (`Reach`'s dead zone is a different thing), so repeat the
search per part.

- **Docs:** `README.md` (74 lines on these topics), `TODO.md`, `.claude/skills/run-jtd/SKILL.md`, and `CLAUDE.md` in
  `td/effect`, `td/enemy`, `td/tower`, `td/tower/cinder`, `td/tower/mortar`, `td/zone`.
- **Rules and their words:** `DevEffects`, `balance/WaveCheckReport`, `effect/ActiveEffects`, `effect/Effect`,
  `effect/EffectInteractions`, `effect/EffectKind`, `effect/EffectSentences`, `effect/EffectTag`, `enemy/AbilityText`,
  `enemy/MobAbilityContext`, `stat/EnemyStat`.
- **Towers:** `CinderTower`, `MortarTower`, `PulseTower`, `SeekerTower`, `SplashTower`, `TowerStat`, `TowerTerm`,
  `cinder/CinderSpec`, `cinder/ThermalShockMechanic`, `mortar/NukeSpec`, `mortar/ShellType`, `mortar/TacticalNukeMechanic`,
  `pulse/FieldMode`, `pulse/ModeMechanic`, `pulse/UndertowMechanic`, `seeker/BrittleMechanic`, `seeker/FreezeSpec`,
  `splash/CovenHex`, `targeting/BeyondRadiusTargetQuery`, `targeting/Reach`.
- **UI and zones:** `ui/EffectPictograms`, `ui/EnemyFrameBuilder`, `ui/Java2DFrameRenderer`, `ui/PerkPictograms`,
  `ui/ZoneFrameBuilder`, `ui/render/HexGlyph`, `ui/render/Palette`, `zone/ZoneEffects`, `zone/ZoneKind`.
- **Resists and diminishing returns reach further:** `enemy/DefinedEnemyMob`, `enemy/EffectResistTrait`,
  `enemy/EnemyInspection`, `enemy/MobEvent`, `enemy/StatRows`, `effect/DiminishingReturns`, `effect/EffectRule`,
  `ui/EnemyStatText`, `ui/StatLooks`, `seeker/FrostbiteMechanic`.

Already stale, and fixed by the same review: `TODO.md` says Scorched and Sickened "never wear off" at -100 spirit, which reads
as false since spirit stopped pacing timers in `391c995f`.

## Decisions made

- The goal is simpler reading and simpler code: fewer edge cases, exceptions and conditions, and one place per mechanic.
- Perma-freeze is impossible by construction, and nothing measures a Seeker defence. Winning with Seeker-heavy builds is a
  balance signal.
- Everything under "What this feature adds" above is decided; only its numbers are starting points.
- **One feature, shipped in parts on `main`,** each measurable alone. The order is the implementor's to choose; see "Suggested
  order" below.
- `BalanceReport --save` before the first part, and `--compare` after each, to record what moved. Nothing gates on it.
- Each part reviews the texts it makes false. One live `run-jtd` pass after the last part, for the "next one lasts N%" line, the
  new Agility and Tenacity rows, and Malaise's text.
- No plan doc: this request is what gets implemented from.

### Suggested order

A suggestion only: **the implementor re-evaluates it against the code and changes it if a better order shows up.**

1. Interactions. Rime and Ash lose only their freeze and burn rules here, because what replaces them needs Tenacity.
2. Crit.
3. Slows, so Chill's level is gone before Tenacity has to scale it.
4. Tenacity, with immunity and the hex that replaces Rime and Ash.
5. Freeze meter, with plain Daze. Last, so the ladder holds until the meter replaces it.

## Open questions

1. **Numbers the changes force,** to set during the work: Chill's values, tuned for stacking; what the Sniper node and Lightning
   Rod give; Thermal Shock's radius; the size of Ash Coven's Burn; whether Brittle's number holds on a walking enemy.

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
