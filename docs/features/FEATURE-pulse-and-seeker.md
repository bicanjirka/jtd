# Feature Request: Pulse and Seeker, the Control Towers

**Status: not started.** Feature 4 of 6 in the tower rework (order in `TODO.md`). Needs features
1 to 3.

## Summary

The two towers that decide where and how enemies move and act. The **Pulse** becomes the Field: a
circle where the rules change, the place enemies must not linger. It deals magic, so plating no
longer erases it, and the longer an enemy stays inside, the worse it gets (Toll). The **Seeker**
becomes the Hunter: it never misses and never lets anything get away. It hunts the fastest enemy,
keeps its lock through invisibility, banks missiles in a nest for a salvo, and freezes.

This feature adds **Silenced**, **Anchored**, **Unraveled** and **Brittle**, dispelling shields,
and projectile speed and size as visible tower stats.

## Current state (what exists today)

- **Pulse today:** $25, 2 **physical** a tick to every enemy in range 1.5, no cooldown, fires only
  while a visible enemy is in range. Plating subtracts per hit, so Armored enemies at Elite (8) and
  Boss (10) rank and the Warden (10) take nothing from it at all. Head: Overcharged Coils (+30%
  damage; +25% and +10% crit, the crit being a hidden +5% rolled 20 times a second) and Resonant
  Field (+20% range and hits invisible enemies; +15% range and reveals what it hits for 2 s).
  Special: Warding Field (a 10% chance a tick to add Vulnerable, so anything inside is fully
  Vulnerable within 1.5 s).
- **Seeker today:** $35, 26 magic, range 4.5, a missile about every 2.3 s, freezes 1.5 s, missile
  speed 35 px a tick (about 22 cells a second, effectively instant), aims furthest along the path.
  A frozen target drops out of that lead, so the next missile goes elsewhere and the tower never
  finishes what it froze. Head: Twin Warhead (+30% fire rate; +25% fire rate and a second missile
  at the same target, which overkills and freezes the already frozen) and Deep Freeze (+30%
  damage; +25% damage, +75% freeze, a frozen kill shatters; most of the freeze bonus is eaten by
  diminishing returns). Special: Homing Curse (Vulnerable, 2 on a frozen or chilled target). Four
  of its five nodes gate on kills, on the tower that kills least.
- **A frozen enemy can't cast** (the Ghost's first-hit vanish is suppressed): freeze is already a
  silence. Nothing else stops an ability.
- Shields exist (`EffectKind.SHIELD`), including typed ones that block only one damage type; heals
  over time exist. Nothing removes either.
- The Jammer's disruption is an aura on towers, not a cast.

## What this feature adds

### Pulse - the Field

**Why build it.** It touches every enemy inside on every tick, so it carries "while inside"
effects and builds stacks faster than anything. Its damage is incidental; its job is to make its
circle the place enemies must not linger: slowed, anchored, silenced, softened, drained,
amplified. Build it where the path loops or bends around it, and pair it with chill. **Buy it
when** casters show up (Mender, an Elite Ghost's shroud, the Warden), for clumps in loops, against
rushes. **Weak against** long straights and spread-out lanes.

```
PULSE    $25 | 2 a tick to every enemy inside, magic | range 1.5 | fires without a visible target
ATTUNE   Toll: each second inside adds a stack (up to 5), fading 1 s after the enemy leaves;
         each stack: +10% field damage, and every debuff on it wears off 10% slower
RANGE    +15%  ->  II +10%  ->  III Wide Field: +20% range; Toll lasts 1 s longer after leaving
HEAD A   Overcharged Coils (the charge: the field bites)
         I    +30% damage; Toll builds twice as fast
         II   +25% damage; an enemy at full Toll takes +25% from the field
         III  Arc Discharge: once a second a zap hits the healthiest enemy inside for 15x the
              tick damage, magic, with the Pulse's crit (+10%)
         IV-A Meltdown: Toll stacks to 10; the zap gets +10% more crit, and Toll raises the
              zap's damage too
         IV-B Tesla Coil: the zap chains to 3 more enemies within 1.5 cells, even outside the
              field; every zapped enemy is Dazed 0.25 s
HEAD B   Phase Field (the rules)
         I    +20% range; Toll stays 2 s after an enemy leaves
         II   +15% range; reveals what it hits for 2 s, once per enemy
         III  Null Field: enemies inside are Silenced
         IV-A True Sight: an enemy the field reveals is Dazed 2 s; reveals last 4 s; an enemy can
              be revealed again after it leaves the field and comes back
         IV-B Dead Zone: nothing inside can be healed or shielded
EXTRA    Field Shaping
         I    Corrosion: -30 armor while inside (armor stops at 0)
         II   Mirror Field: damage a shield absorbs inside is dealt back to its enemy as magic
         III  Undertow: inside, enemies are chilled 25% and the chill doesn't fade while they
              stay (it counts for freezes), and they are Anchored
         IV   Event Horizon: each death inside adds +5% field damage until the wave ends, up to
              +100%
SPECIAL  one shared set: Rattle Field, Soul Drain, Kill Zone
```

- **Magic damage**, so its ticks land on Armored Elites and the Warden. Range stays 1.5.
- **Fires without a visible target** from the start: it is the ghost hitter, and Phase Field I no
  longer has to buy that.
- **Arc Discharge's zap** is a visible moment instead of a hidden crit on 20 ticks a second: a
  bright bolt from the tower to its target, magic damage.
- **Tesla Coil's Dazed** follows every Dazed rule, its diminishing returns included.

**Specials** (all about what happens inside):

- **Rattle Field** (today's Warding Field, renamed: it weakens, it doesn't protect): each tick, a
  5% chance to add a stack of Sundered or Exposed. The node text says what that means in practice
  (about how fast an enemy inside is fully Sundered or Exposed).
- **Soul Drain:** each second inside costs 5 spirit (as Sickened stacks), and the field deals +1%
  damage per point of spirit below zero. At -100: double field damage, no heals or shields, and
  stacked debuffs never wear off. High-spirit enemies (bosses) are drained longest.
- **Kill Zone:** enemies inside take +25% damage from every *other* tower, as its own multiplier,
  outside Vulnerable's cap. The place every other tower should point at.

They combine by themselves: Soul Drain's lost spirit and Toll both slow every debuff, so Rattle
Field's stacks never fall off; Kill Zone turns that into the whole defence's damage.

**Look:** rings rippling outward, faster as Toll builds; the field's colour says its mode (violet
under Null Field, green under Corrosion, blue under Undertow); enemies inside flicker.

**XP job events:** a Toll stack; a silence; a reveal. **Rank perk (Expert):** Toll starts at 1.

### Seeker - the Hunter

**Why build it.** The only single-target magic tower: its missile ignores physical armor and
plating, homes onto fast enemies, keeps its lock through invisibility, strips shields and freezes.
It answers whatever breaks the defence: the rush, the Ghost, the shielded Warden. Its nest stores
missiles in quiet moments and releases them as a salvo when the dangerous one arrives. **Buy it
when** enemies are fast, evasive, shielded or plated, or a boss arrives after a quiet stretch.
**Weak against** swarms and long waves of weak enemies.

```
SEEKER   $30 | 40 magic | a missile about every 2.3 s | missile 8 px a tick | range 4.5 | freezes 1.5 s
         aims at the fastest enemy | lock-on through invisibility
ATTUNE   Nest: the cooldown loads a missile into the nest (up to 3) instead of firing; with a
         target in range, stored missiles launch 4 ticks apart; it fills between waves too
RANGE    +15%  ->  II +10%  ->  III Over the Horizon: +10%; may fire at Revealed or Marked
         enemies within 1.5x its range
HEAD A   Twin Warhead (more missiles: salvos and swarms)
         I    +30% fire rate; the nest holds one more
         II   two missiles a shot, the second at the next target
         III  Brood: a nest of 6; a salvo spreads across different targets
         IV-A Shatterburst: a missile hitting a frozen enemy bursts for 35% magic damage around
              it; every enemy the burst hits is Silenced and gains Unraveled
         IV-B Rearm: a missile that freezes its target launches a new missile from the tower at
              once; a rearmed missile's freeze rearms nothing
HEAD B   Deep Freeze (control: the boss stands still)
         I    +50% freeze
         II   +30% damage, +10% crit; a frozen kill shatters
         III  Brittle: frozen enemies take +30% physical damage
         IV-A Absolute Zero: the missile freezes everything within 1 cell of the impact; shatters
              deal x2; an enemy this tower froze, or hit while frozen, shatters when its freeze
              ends (the same burst, without dying)
         IV-B Frostbite: this tower's hits on a frozen enemy are guaranteed crits, and so are its
              hits on an enemy whose freezes are diminished by repeated freezing
EXTRA    Mixed Payloads (some missiles carry something else)
         I    Arcane: every 3rd missile applies Unraveled instead of freezing
         II   EMP: strips shields and Silences for 2 s
         III  Tracer: reveals and Marks
         IV   every missile carries a payload, cycling Cryo, Arcane, EMP, Tracer, each 25%
              stronger; the orbiting missiles show the order
SPECIAL  one shared set: Arcane Warhead, Nullifier, Hunter's Mark
```

- **Slower missiles** (35 -> 8 px a tick): you can see them hunt, and the nest has time to matter.
  **Projectile speed and size become tower stats**, shown in the info rows and drawn (a heavier
  missile looks heavier). The Mortar's shells use the same stats in feature 5.
- **Aims at the fastest enemy.** Changing targets often is wanted: a frozen enemy has stopped, so
  the next missile hunts the next runner.
- **Lock-on:** a missile keeps its target through invisibility; the Ghost's first-hit vanish no
  longer shakes it off.
- **The nest fills between waves too**, so a wave opens with a free salvo.
- **Mixed Payloads before IV:** every 3rd missile carries the next owned payload in turn (with I
  and II owned: Arcane, then EMP, then Arcane...); a payload missile doesn't freeze.

**Specials** (all on the missile's impact):

- **Arcane Warhead** (today's Homing Curse, renamed: "hex" and "curse" now belong to the Hexer):
  the impact applies Unraveled, 1 stack, 2 on a frozen or chilled target. The magic enabler for
  the Cinder, the Pulse, the Sonar's Pure Tone and the Stormcaller.
- **Nullifier:** the impact strips shields and heals over time; +50% damage against shielded
  enemies. The Warden answer.
- **Hunter's Mark:** each consecutive hit on the same target +20%, up to +100%; aims at the
  highest rank instead of the fastest. A salvo is a string of consecutive hits, so the bonus climbs
  inside one salvo.

**Look:** slower missiles with smoke trails and a visible curve; stored missiles orbit the tower as
the nest fills; payload colours (ice blue Cryo, violet Arcane, yellow EMP, red Tracer).

**XP job events:** a freeze; a shield stripped; a salvo of 3 or more. **Rank perk (Expert):** the
nest holds one more.

### New effects and rules

| Effect | What it does | Applied by |
|---|---|---|
| Silenced | its abilities don't fire: no heal or shield pulses, summons, shrouds, vanishing or reshields. Death abilities still fire, and the Jammer's aura keeps running | Null Field (while inside), EMP (2 s), Shatterburst |
| Anchored | can't be sped up (hurt-speed traits and haste ignored), speed capped at 75% of base | Undertow (while inside) |
| Unraveled | -10 magic resist a stack, 5 stacks, 5 s, stops at 0 | Mixed Payloads' Arcane, Arcane Warhead, Shatterburst |
| Brittle | a frozen enemy takes +30% physical damage | Deep Freeze III |

- **Dispel:** a hit can remove shields and heals over time (EMP, Nullifier). Dead Zone instead
  prevents new ones while inside, and Mirror Field turns what a shield absorbs into damage.
- Frozen enemies take extra physical damage **only** under Brittle, never globally.

### What happens to today's nodes

| Today | Becomes |
|---|---|
| Pulse physical, needs a visible enemy | magic, fires regardless |
| Overcharged Coils II's hidden crit | the zap's visible crit (III) |
| Resonant Field I (hits invisible) | Phase Field I: range and lingering Toll |
| Resonant Field II (reveals 2 s) | Phase Field II, once per enemy |
| Warding Field (10% Vulnerable a tick) | Rattle Field (5% Sundered or Exposed a tick) |
| Seeker $35, 26 damage, missile 35 | $30, 40 damage, missile 8 |
| Seeker aims furthest along the path | aims at the fastest enemy |
| Twin Warhead II (second missile, same target) | the second missile goes to the next target |
| Deep Freeze I (+30% damage) | +50% freeze; damage moves to II |
| Homing Curse (Vulnerable) | Arcane Warhead (Unraveled) |

## Interconnections

- **Feature 1:** Attune passives, level IV, XP and job events.
- **Feature 2:** Sundered and Exposed (Rattle Field), Marked (Tracer, Over the Horizon),
  guaranteed crits that land below 100 resilience (Frostbite), armor stopping at 0 (Corrosion),
  spirit pacing every debuff (Toll and Soul Drain build on it).
- **Feature 3:** Dazed and its ladder (Tesla Coil, True Sight). Hexes last longer under Toll and
  Soul Drain, and Rime makes the Seeker's freezes longer and burstier.
- **Feature 5:** freezing a burning enemy bursts its burn (the global row and the Cinder's
  Thermal Shock), and the Mortar's shells reuse projectile speed and size.

## Constraints and open risks

- **Silence is new.** Abilities fire from triggers today and only freeze suppresses them. Silence
  must stop casts without stopping death abilities or auras, and "while inside" silences must end
  the moment an enemy leaves.
- **"Fires without a visible target"** means the Pulse hits invisible enemies; area damage
  already does, so it stays within the stealth rules (`Revealed` and invisibility in the effect
  rules).
- **Rearm and Absolute Zero chain on freezes.** Freeze's diminishing returns bound how often one
  enemy freezes, but a fresh crowd can keep Rearm firing; the guard (a rearmed missile rearms
  nothing) keeps it to one extra missile per shot.
- **Frostbite** turns diminished freezes into guaranteed crits, which against a boss means every
  hit once its freezes run out. Crit immunity still holds at 100 resilience.
- **The fastest selector** reorders constantly; with sticky targeting rejected, a salvo spreads
  over runners by design. `TODO.md`'s finding (the Seeker never finishes what it froze) becomes
  intended behaviour.
- **Slower projectiles** can miss a target that dies or vanishes mid-flight: lock-on covers
  invisibility; what a missile does when its target dies is today's retargeting.
- Visual budget: the zap, rippling rings, smoke trails and orbiting missiles are per-frame draws;
  check `td.PerformanceHarness`.

## Decisions made

- The Pulse and the Seeker ship together: both are control (silence, freeze, anchor, Daze), and
  both lean on what features 2 and 3 built.
- Pulse: magic, range 1.5 (1.75 rejected), fires without a visible target.
- Field Shaping order is **Corrosion, Mirror Field, Undertow, Event Horizon** (your reorder:
  Undertow is strong, so it moved to III). Stasis is dropped; Mirror Field takes its place.
- True Sight: no doubled reveal radius; it Dazes what it reveals for 2 s and can reveal an enemy
  again on a later visit. Phase Field II's "once per enemy" means once per visit when True Sight is
  owned.
- Dead Zone only blocks heals and shields inside; it doesn't strip shields on entry.
- Rattle Field rolls at 5% a tick (not 10%).
- Seeker: aims at the fastest enemy (sticky targeting rejected); the nest fills between waves.
- Twin Warhead IV-A and IV-B are your replacements (Shatterburst, Rearm); names are new.
  Rearm's guard (a rearmed missile's freeze rearms nothing) fills a gap: every streak needs one.
- Deep Freeze I is +50% freeze only; Absolute Zero keeps its area freeze and adds shatter on thaw;
  Permafrost is replaced by Frostbite.
- Renames: Twin Warhead III Hive -> **Brood** (feature 6 renames the tower itself "Hive"); Homing
  Curse -> Arcane Warhead; Warding Field -> Rattle Field; Resonant Field -> Phase Field (it
  clashed with the Aura's Resonance Field).

## Open questions

- **Shatter on thaw (Absolute Zero):** how big is the burst (the shatter kill's share), and does
  an enemy that shatters on thaw count toward Shatterburst?
- **Mirror Field:** is the reflected damage the full absorbed amount, or a share of it?
- **Soul Drain** spends spirit as Sickened stacks: does that stack with poison's Sickened toward
  the -100 floor, as written?
