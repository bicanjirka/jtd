# Feature Request: Splash, Stormcaller and Hexer

**Status: implemented**, except Mines and the cursed cloud, which wait for ground zones (`TODO.md`).
Feature 4 of 7 in the tower rework (order in `TODO.md`). Needs features 1 to 3.

## Summary

A complete rework of the Splash. The base stays what it is (cheap, instant, never misses, it
splashes); the fork decides what the blast carries. **Chain A, Arc** makes it the
**Stormcaller**: lightning that runs past the blast along columns and lines, crits, stuns and
turns other towers' hits into lightning. **Chain B, Hex** makes it the **Hexer**: a witch that
curses enemies with hexes that store damage, poison, jump from the dead to the living and turn
enemies' own abilities against them. The specials follow the fork. In one line: **the blast is
the delivery, the chain is the payload.**

This feature also adds **Dazed**, the stun, with its own diminishing returns, and the **hex**
system.

## Current state (what exists today)

- **Splash today:** $15, 16 physical, range 3.2, one shot a second, blast radius 1.75 with falloff
  `1 - (d / radius)²`, aims at a random enemy in range. The `README.md` line says it chills 50%
  for 2 s; the code doesn't (only Concussive Blast chills).
- **Head:** Blast Engineering (+30% radius; +25% damage and flatter falloff; 3 projectiles) and
  Rapid Battery (+25% fire rate; +25% damage and +15% crit; crits splash 50% bigger).
- **Specials:** Toxic Bloom (the blast poisons, 10% of weapon damage a tick for 4 s: about 30 magic
  DPS on every enemy it reaches, the game's biggest outlier), Concussive Blast (-50% fire rate,
  chill, kills explode), Overpressure (after a crit the next shot hits every enemy in range; dead
  without Rapid Battery II, since the Splash has no base crit).
- The board draws lines connecting the enemies a blast hit.
- **Stun doesn't exist.** Freeze is the only hard crowd control; it puts out burn and consumes
  chill, and repeated freezes diminish (a second within the window lasts half, then a quarter,
  then immune). A frozen enemy can't cast.
- Enemy abilities fire from triggers (`OnCriticalHitTaken`, `OnDeath`, `OnFirstDamageTaken`,
  `HealthThreshold`, `TimeSinceLastHit`, `Periodic`), and effect damage is credited to its tower
  through `DamageSink`: the vocabulary a curse reacts to and pays out through.

## What this feature adds

### The tower

**Why build it.** Cheap, instant, never misses: the delivery vehicle for whatever should land on a
group. Unforked, it is early area damage. **Buy it when** early (cheap area); Stormcaller for
strung-out lanes, columns and crit teams; Hexer for elites in escorts, healers, rushers, and mixed
defences whose debuffs it multiplies. **Weak against** lone bosses and heavy plating on its
physical blast; both chains answer with magic, and each level IV has a boss mode.

```
SPLASH   $15 | 14 physical | 1 shot/s | range 3.2 | blast 1.5, falloff | random target in range
ATTUNE   Fire Control: aims for its purpose; Saturation (+5% blast damage a stack, 3 stacks)
RANGE    +15%  ->  II +10%  ->  III +10% and blast radius +10%
HEAD A   Arc (the Stormcaller: lines, stuns, crits; magic)
         I    Arc: the blast carries past its edge: 2 jumps at 50%, further from Saturated enemies
         II   Conductor: +10% crit, arcs can crit, +1 jump
         III  Overload: an arc crit Dazes its target 0.5 s
         IV-A Chain Lightning: up to 6 jumps, no loss per jump, the 3rd jump forks
         IV-B Lightning Rod: an arc with nowhere to go returns to the primary, up to 3 times
HEAD B   Hex (the Hexer: curses, spread, control; magic)
         I    casts Hex of Doom
         II   Witch's Brew: adds Hex of Blight; a cast curses up to 3 enemies
         III  adds Hex of Contagion
         IV-A adds Rime Coven          IV-B adds Ash Coven
EXTRA    Blast Engineering
         I    Wide Charge: +25% blast radius; the blast's edge deals 25% instead of nothing
         II   Shaped Charge: enemies in the inner half gain 2 Saturation instead of 1; cap 4
         III  Potency: strengthens the chain's level I
         IV   Mastery: strengthens the chain's levels II and III
SPECIAL  a set per chain
         Arc  Thunderclap, Static Charge, Thunderstrike
         Hex  Hex of Inversion, Hex of Sympathy, Hex of Reckoning
```

**Base line.** Physical blast, random target, instant, one shot a second, falloff from the
centre. Damage 16 -> 14 and blast radius 1.75 -> 1.5, because Fire Control aims it and Wide
Charge gives it a falloff floor. No chill (the `README.md` line is corrected). **The look
changes:** no more lines connecting hit enemies; a blast looks like a detonation, a bright circle
that expands as a ring and darkens from light to dark as it grows.

**Attune: Fire Control.**

- **Aims for its purpose:** the blast lands on the enemy with most neighbours inside the blast,
  forked or not.
- **Saturation**, the Splash's Steady Aim: every blast that catches an enemy adds a stack, up to 3;
  the stacks fade 1.5 s after the last blast that caught it, so only an enemy pounded shot after
  shot keeps them. +5% blast damage a stack. It is an effect on the enemy, so two Splashes build it
  together. Each chain's level I reads it, and the extra node grows it.

**Range III and the blast radius.** Every distance the chains use (an arc's jump reach, how far a
hex spreads or shares) grows with the tower's blast radius bonus, so Range III and Wide Charge
help the payload as well as the blast.

**The name follows the fork.** Unforked it is the Splash; on Arc the **Stormcaller**, on Hex the
**Hexer**. Only the panel title and tooltip change; the id stays. (The unforked display name
becomes "Burst" in feature 7.)

**Specials follow the fork.** Awaken offers no special until a chain root is owned; then it offers
that chain's three (feature 1's per-chain rule).

### Chain A - Arc (the Stormcaller)

- **I Arc:** from the outermost enemy the blast caught, an arc jumps to the nearest enemy the shot
  hasn't hit, within 1.5 cells, then on from there: 2 jumps, 50% of the blast each, as **magic**.
  Arcs jump 0.5 cells further from a Saturated enemy per stack. The blast owns clumps, the arcs own
  lines. **Look:** arcs are jagged electric zigzags, and the tower recolours blue.
- **II Conductor:** +10% crit chance (the Splash's one crit-granting level), arcs can crit, +1
  jump.
- **III Overload** (the stun): an arc crit Dazes its target for 0.5 s. Arcs into a fully Saturated
  enemy get +10% crit chance.
- **IV-A Chain Lightning** (the swarm storm): up to 6 jumps with no loss per jump, and the 3rd jump
  forks into two arcs. A column dies in one shot.
- **IV-B Lightning Rod** (the focused storm): an arc that finds no new enemy returns to the primary
  at full strength, up to 3 times; arcs on a Dazed enemy deal +50% crit damage. A boss with two
  escorts takes four strikes a shot.

**Arc specials** (shared trigger: the arc crit):

- **Thunderclap** (today's Overpressure, re-aimed and tamed): after a crit, the next shot
  discharges into every enemy in range as arcs at 50%, and each one it crits is Dazed. Its own
  crits don't re-arm it.
- **Static Charge** (the team's conductor): arcs leave enemies **Charged** for 3 s. The next hit
  from another tower discharges it for +30% of that hit as magic, credited to this Splash; a crit
  discharges at double. Periodic damage never discharges it (feature 3's hit rule), so a Pulse's
  field or a burn can't eat the charge.
- **Thunderstrike** (the boss answer): every 6th shot calls lightning onto the enemy with most
  health in range: 4x the blast as magic, Dazed 0.5 s, and that shot's arcs start there at full
  damage. It counts as a crit for triggers.

They combine by themselves: a Thunderstrike arms Thunderclap; Thunderclap's discharge Charges
everything in range; Chain Lightning and Lightning Rod multiply all three.

### Chain B - Hex (the Hexer)

**How a Hexer curses.** Casting is the Hexer's ability, much slower than its blast: **every 4th
shot is a cast** that curses one enemy instead of blasting, so fire-rate bonuses speed casting
too. Every hex the tower owns joins its **pool**: buying a level adds a hex, it never replaces
one.

**Which hex, on whom.** The Hexer **takes its hexes in turn**, and each hex picks its own target
with one simple rule (below). A hex with no fitting target in range is skipped for that cast, and
the turn restarts with Doom whenever the tower's range has been empty. So a fully upgraded Hexer
still opens on a lone tough enemy with Doom, but meets a swarm with Blight on the densest group
and Contagion on the enemy about to die. When no hex in its pool has a target, the cast becomes a
plain blast.

| Hex | Its target |
|---|---|
| Doom | the healthiest enemy in range without Doom |
| Blight | the enemy with most neighbours, without Blight |
| Contagion | the lowest-health enemy with a neighbour within 1.5 cells, without Contagion |
| Rime, Ash | the healthiest enemy without it |
| Inversion | an enemy that heals, shields or vanishes (a Mender, the Warden, a Ghost), without it |
| Sympathy | the enemy with most neighbours, without Sympathy |
| Reckoning | the enemy with most Doomed neighbours within 2 cells, if any |

**Rare by design.** Only the Hexer casts hexes, one every four shots, and an enemy carries each
hex at most once. Each hex is a rune drawn over the enemy, and its glyph says what it does. A
hex lasts 6 s unless it says otherwise (spirit and the Pulse's Toll stretch it like any debuff).
A Rime rune and an Ash rune can't share an enemy: the newer replaces the other.

Every hex is credited to the Hexer that cast it, for damage and kills.

- **I Hex of Doom:** lasts 4 s, +1 s per Saturation stack. When it ends, the enemy takes 30% of
  all the damage it took while hexed, as one magic hit. One big hit is also the witch's answer to
  plating.
- **II Witch's Brew:** adds **Hex of Blight**: the hexed enemy is poisoned (today's Toxic Bloom
  poison, tamed to 4% of weapon damage a tick) for the hex's length. From II on, a cast curses up
  to 3 enemies: the target and the two most Saturated within the blast. Poison's Sickened lowers
  spirit, and spirit paces every debuff, so every hex on that enemy lasts longer.
- **III Spreading Curse:** adds **Hex of Contagion**: when the hexed enemy dies, its hexes and
  debuffs (Vulnerable, Sundered, Exposed, poison, Scorched, and every debuff later features add)
  jump to the 2 nearest unhexed enemies within 1.5 cells with their remaining time. A jumped hex
  can jump once more, never a third time.
- **IV-A Rime Coven:** adds **Hex of Rime**: chills 30% when cast; when the enemy freezes, its
  remaining chill buys twice the usual extra freeze time, and its burn lands at once at 100%
  instead of going out. Build it where Seekers freeze.
- **IV-B Ash Coven:** adds **Hex of Ash**: burn and poison pools on the enemy hold twice as much
  and earn Scorched and Sickened twice as fast, but it can't be frozen and shrugs off 75% of
  chill, so no teammate's freeze puts the fire out. Build it where Cinders burn.

**Hex specials** (each adds its hex to the pool):

- **Hex of Inversion** (control: what helps it hurts it): heals and shields it receives are dealt
  to it as damage instead, and it can't turn invisible (it stays revealed). Menders become bombs,
  the Warden's reshield hurts it, a Ghost can't vanish.
- **Hex of Sympathy** (support: what one suffers, all suffer): once a second, the debuffs on this
  enemy (Vulnerable, Sundered, Exposed, chill, and later ones such as Unraveled and Cracked) are
  copied to every other enemy within 2 cells that carries any hex from this tower. Only the
  Sympathy carrier shares, so copies never spread further: one Sniper's Sunder becomes the
  group's without a loop.
- **Hex of Reckoning** (offense): when this enemy dies, every Doom within 2 cells cast by this
  tower releases at once and restarts. Kills chain into detonations.

They combine by themselves: Sympathy's copied debuffs make every hit bigger, so each Doom stores
more for Reckoning; Inversion's reversed heals count as damage taken, so they fill Doom;
Contagion carries all of it on to the next enemies.

### Extra node - Blast Engineering

Small mechanics for either chain: every level makes the blast reach more enemies, or makes the
chain's payload stronger. Levels III and IV read the chain, so they need a chain's level I.

- **I Wide Charge:** +25% blast radius, and the blast's edge deals 25% instead of nothing.
- **II Shaped Charge:** enemies in the inner half of the blast gain 2 Saturation stacks instead of
  1, and Saturation caps at 4.
- **III Potency** (strengthens level I): Stormcaller: +1 jump, and arcs deal 65% of the blast
  instead of 50%. Hexer: Doom stores 45% instead of 30%.
- **IV Mastery** (strengthens levels II and III): Stormcaller: Conductor gives +10% more crit
  (+20% in all) and Overload's Daze lasts 1 s. Hexer: Blight poisons 50% harder, and Contagion's
  death leaves a **cursed cloud** for 4 s that holds every debuff the dead enemy had; any enemy
  entering it gets them. The cloud is a ground zone, so it arrives with feature 6; until then
  Mastery's Hexer half is Blight's boost alone.

### Dazed (the stun)

- A Dazed enemy stops and can't cast, like a frozen one, but Dazed **doesn't put out burn and
  doesn't consume chill**.
- **Its own diminishing returns, slower than freeze's.** Freeze goes 100% -> 50% -> 25% ->
  immune; Dazed goes 100% -> 80% -> 60% -> 40% -> 20% -> immune, within the same kind of window.
  Freeze and Dazed don't share a ladder.
- Applied in this feature by Overload, Thunderclap, Thunderstrike and Mastery; later by the
  Pulse (Tesla Coil, True Sight) and the Mortar (Heavy Shell).

### Purpose gate

On head III (feature 2): 45 blasts whose primary target is Saturated.

### What happens to today's Splash

| Today | Becomes |
|---|---|
| Blast Engineering I-III (radius; damage and flatter falloff; 3 blasts) | the extra node: Wide Charge, Shaped Charge, Potency, Mastery; the 3-blast carpet is retired |
| Rapid Battery I-III (fire rate; damage and crit; bigger crit splash) | retired; its crit moves to Arc II |
| Toxic Bloom | Hex of Blight, at 4% a tick |
| Concussive Blast | retired: its stun to Overload, its chill to Rime, its explosions to Reckoning |
| Overpressure | Thunderclap, at 50% |
| Random target | stays at base; Fire Control aims |
| Lines connecting hit enemies | a detonation ring |

## Interconnections

- **Feature 1:** per-chain specials (Awaken offers them once a chain root is owned), the extra
  node's chain requirement.
- **Feature 2:** XP and the purpose gate kind.
- **Feature 3:** crit per tower and guaranteed crits (Overload and Thunderstrike are crit-driven);
  the hit / periodic rule (Static Charge); Sundered and Exposed (Sympathy and Contagion carry
  them); Shatter Shot starts reading Dazed.
- **Feature 5:** Seeker's freezes feed Rime; Pulse's Toll and Soul Drain stretch every hex; the
  Pulse and Seeker add Unraveled and Silenced, which Sympathy and Contagion then carry; Tesla
  Coil and True Sight Daze.
- **Feature 6:** ground zones (the Hexer's cursed cloud, and the Splash's Mines); Cinder's burns
  feed Ash; the global "freezing a burning enemy bursts the burn (50%)" row arrives there, and
  Rime's 100% burst is the hex's stronger version of it.
- **Feature 7** renames the unforked tower "Burst" and its Aura's Conduit lengthens hexes.

## Constraints and open risks

- **Hexes are a new system**: a timed debuff that waits for something to happen to the enemy
  (its end, its death, a heal or shield landing) and then pays out, credited to its caster. It is
  the most expensive piece of this feature. The enemy trigger vocabulary and `DamageSink` are the
  closest existing pieces. Doom also has to add up the damage its enemy takes while hexed.
- **Choosing a hex** runs once per cast (every 4th shot): one target rule for one hex, over the
  enemies in range, with skips. Never a per-tick scan.
- **Chain targeting** ("the nearest enemy not yet hit, within a jump radius") is a new kind of
  query; arcs, Chain Lightning's fork and Lightning Rod's returns run many of them per shot on a
  swarm. Check the per-tick budget.
- **Copying effects with their remaining time** (Contagion, Sympathy) must keep each copy's own
  clock; the guards are that only a Sympathy carrier shares, and a jumped hex jumps once more at
  most.
- **Static Charge** credits damage to a different tower than the one whose hit discharged it.
- Visual budget: arcs, runes and the detonation ring are per-frame draws; check
  `td.PerformanceHarness`.

## Decisions made

- Base function unchanged (physical, random target, instant, falloff); damage 14, blast 1.5; no
  chill.
- Fire Control and Saturation are the Attune passive. Crowd Tempo (fire rate per crowd) was
  rejected.
- The arc chain and the hex chain are exclusive. Each has its own three specials, and no special
  is offered before the fork.
- **The Hexer casts from a pool** (your round-2 call): every level and special adds a hex; every
  4th shot is a cast instead of a blast; when nothing in range can take a hex, the cast is a blast.
- **"The most valuable hex" is taken in turns, each hex with its own target rule.** A scoring
  system that weighs every hex against every enemy is what your "smart, fast and cheap" asked to
  avoid; turns plus target rules keep the behaviour you described (Doom first on a lone tough
  enemy, a different mix on a swarm) at the cost of one simple pick per cast.
- **No cap on hexes per enemy** beyond "each hex at most once": one cast every four shots already
  keeps hexes rare. You left the cap to planning; this is the default, and planning may add one if
  play shows hexes everywhere.
- **Simplified for cost:** Saturation fades with time instead of being cleared by "a shot that
  misses it" (which needed a rule for whose shot); the Stormcaller aims like the unforked tower
  (simulating every arc path per candidate target was dropped); Inversion keeps heals, shields and
  vanishing and drops "speed-ups become slows" (that one rewrote speed traits); Sympathy shares
  once a second from its carrier instead of reacting to every debuff landing anywhere.
- Hex III Spreading Curse becomes Hex of Contagion in the pool; IV are Rime and Ash, exclusive.
  Plague and Grand Hex were rejected. Hex of Echoes was the alternative to Reckoning and is not
  taken.
- Blast Engineering III and IV amplify the chain instead of Aftershock and Carpet (your call);
  the numbers in Potency and Mastery fill the gap you left and are starting points.
- Blast radius scales the chains' distances (your call); the rule is one: every distance the
  payload uses grows with the blast radius bonus.
- **Dazed has its own ladder**, 100/80/60/40/20/immune, not freeze's.
- Overload over Ground Strike at Arc III; Thunderstrike over Ball Lightning.
- Arc ships before Hex inside this feature: it is the cheaper chain.
- Mines (a blast leaves a charge on the path) and the cursed cloud are wanted, and land with
  ground zones in feature 6.

## Open questions

- None blocking. The cast rate (every 4th shot), hex length (6 s) and the uncapped pool are
  starting points for the balance pass (feature 7).
