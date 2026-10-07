# Feature Request: Ground Zones, Mortar and Cinder

**Status: implemented**, except for the numbers, which
are placeholders until the balance pass. Feature 6 of 7 in the tower rework (order in `TODO.md`).
Needs features 1 to 5.

## Summary

Fire, ice and the ground. **Ground zones** are patches on the board that keep working after the
shot that made them: burning ground, tar, frost, fallout, mines, a cursed cloud. The **Mortar**
becomes the Artillery, the only tower that shapes the ground: slow, visible shells that crack
plating, range in on one chokepoint and leave fire, tar or frost behind. The **Cinder** becomes
the Pyre: magic damage over time that ignores armor, scorches resilience so crits land harder for
everyone, and burns what hides or heals. With them come the fire-and-ice rules that make mixing
those towers a combo instead of a conflict.

## Current state (what exists today)

- **No ground effects.** Everything a tower applies lands on an enemy. Projectiles are the only
  world objects with a lifetime besides enemies and towers.
- **Mortar today:** $30, 20 physical, range 4.0, a shell every 2.5 s, blast 2.0 with falloff,
  aims furthest along the path, shell speed 40 px a tick (it lands in about 0.16 s, so nothing can
  dodge it). Every shell **chills 50% for 2 s** on everything in the blast: the game's biggest mass
  slow, competing with its "big boom" identity. Head: Siege Rounds (+30% damage; +25% and +40%
  radius, which makes the blast 5.6 cells wide) and Fragmentation (a shrapnel ring at 25%; II
  chills, gated on 2 adjacent towers). Special: Cursed Shrapnel (Vulnerable on everything blasted,
  outdoing the Aura's Withering Field).
- **Cinder today:** $28, 1.5 magic burn a wave (the `README.md` line says 2), range 2.2, a cone of
  half-width 0.35 rad, a wave about once a second, burn 3 s, aims at the nearest enemy. Head: White
  Flame (+30% damage; +25% and +50% burn duration) and Wide Nozzle (+25% range and +30% cone; +20%
  range, +20% fire rate, +20% cone). Special: Hexflame (a *new* ignition grants Vulnerable, so each
  enemy gets one stack per visit).
- **Burn** is a decaying magic pool that earns Scorched (-1 resilience a stack); poison likewise
  earns Sickened. Freezing a burning enemy just puts the burn out. Burn can't touch the burn-immune
  (the Warden's second egg).
- **Plating** subtracts from every hit; nothing lowers it.
- **Heals and regeneration:** nothing reduces them.

## What this feature adds

### Ground zones

- **A zone** is a disc on the board with a lifetime. Each pulse it applies its effects to every
  enemy inside, credited to the tower that made it (damage and kills). Zones touch invisible
  enemies, like all area damage. A zone's pulses are periodic damage: they never crit.
- **Zones don't stack.** Two zones of the same kind on top of each other act as one: an enemy
  standing in both is treated as standing in one.
- **Each kind has its own look:** a translucent patch with a pattern of its own.
- **Zones combine through the enemies standing in them,** never zone against zone. Each zone puts
  its effect on the enemy, and the effect rules do the rest: an enemy in tar that steps into fire
  burns at double pool; an enemy that freezes in frost ground while burning has its burn burst; a
  tarred enemy that freezes stays frozen 1 s longer. The combos of earlier drafts (inferno, thermal
  shock, hardened tar) survive as these three rows.

| Zone | Made by | Effect on enemies inside |
|---|---|---|
| Burning ground | Mortar's Napalm, Cinder's Lingering Flames | burns (a burn pool, earning Scorched) |
| Tar | Mortar's Tar | 40% slow and poison; Tarred, so a burn starts at double pool |
| Frost ground | Mortar's Cryo Shells | chill builds while inside; 2 s inside without leaving freezes it (diminishing returns apply) |
| Fallout | Mortar's Tactical Nuke | drains spirit (Sickened stacks) and blocks healing |
| Mine | Splash, Range III (below) | the first enemy to enter sets it off as one blast; then it is gone |
| Cursed cloud | Hexer's Mastery (feature 4) | gives every enemy inside the debuffs the dead enemy had |

### Fire and ice, for every tower

- **Freezing a burning enemy bursts the burn:** instead of just going out, the remaining pool lands
  at once at 50%. (The Cinder's Thermal Shock raises it to 150%; the Hexer's Rime to 100%.)
- **Burning reveals:** a burning invisible enemy is visible while its burn pool is above a fuel
  level; a little burn doesn't reveal.
- **Tar and burn:** a tarred enemy's burn starts at double pool.
- **Tar and freeze:** a tarred enemy that freezes stays frozen 1 s longer.
- Dazed doesn't put out burn (feature 4).

### Mortar - the Artillery

**Why build it.** Slow shells with a huge area: nukes for swarms, busters for bosses, and fields of
fire, tar and frost that keep working after the shell lands. It holds the chokepoint where lanes
meet, and gets better the longer it shells the same spot (Bracketing). **Buy it when** waves come
packed, lanes converge, bosses bring escorts. **Weak against** fast single enemies (they dodge),
whatever is right next to it (the dead zone), spread-out lanes (Bracketing resets).

```
MORTAR   $30 | 32 physical | a shell every 3.5 s | range 4.5 | blast 1.75, falloff
         shell 8 px a tick (about 0.9 s flight) | furthest along the path | can't hit within 1.5 cells
         every shell Cracks plating
ATTUNE   Bracketing: a shell landing within 1.5 cells of the last gets +10% damage and radius,
         up to 3 steps; a shell elsewhere resets it
RANGE    +15%  ->  II +10%  ->  III Long Battery: +30% range; the dead zone grows to 2.5 cells
HEAD A   Siege Rounds (the big boom: bosses and armor)
         I    +30% damage; each Bracketing step gives +15%
         II   +25% damage, +25% radius, +10% crit
         III  Heavy Shell: +50% damage, -20% fire rate, a bigger, slower shell; enemies within
              0.5 cells of the impact are Dazed 0.5 s
         IV-A Tactical Nuke: every 4th shell deals x4 damage over x1.5 radius and leaves Fallout
              for 4 s, over 0.8 of the blast's radius
         IV-B Bunker Buster: the enemy at the centre takes x3 and is Sundered; the blast shrinks 30%
HEAD B   Fragmentation Rounds (swarms and lines)
         I    a shrapnel ring at 25%, reaching 0.25 cells further per Bracketing step
         II   shrapnel does 30% more of everything it does: damage, Cracked, and the special
              shell's effect it carries
         III  Cluster Shell: 4 bomblets scattered along the path around the impact, 40% each,
              1-cell radius
         IV-A Carpet Bombing: 8 bomblets in a line along the path ahead of the impact
         IV-B Shrapnel Storm: shrapnel makes enemies Bleed
EXTRA    Ballistics
         I    Rifled Barrel: shell speed +40%, drawn smaller with a streak
         II   Predictive Fire: aims where the enemy will be when the shell lands
         III  Airburst: +25% radius, no falloff in the inner half
         IV   Barrage: the reload takes twice as long, but each salvo is 3 shells 0.5 s apart at
              full damage
SPECIAL  one shared set of shell types: Napalm, Tar, Cryo Shells
```

- **Every shell Cracks plating:** everything in the blast is Cracked (plating -50%, 5 s,
  refreshed by each shell). Explosions crack plates; this is the Mortar's own debuff.
- **No base chill.** Chill moves to the Cryo special, so the base Mortar is a pure boom.
- **Slow, visible shells** (40 -> 8 px a tick) that fast enemies can dodge. Shell speed and size
  are tower stats (feature 5), shown in the info rows and drawn.
- **The dead zone** (1.5 cells, 2.5 with Long Battery) is a fixed distance: range bonuses don't
  change it.
- **Bracketing** shows a ranging marker on the spot, tightening with each step.
- **Tactical Nuke** has its own look: a nuke shell, and a white flash with an expanding ring.

**Specials: shell types.** Each special unlocks a shell type that leaves a zone where it lands.

- **Napalm** (the Mortar's magic opt-in): burning ground, 1-cell radius, 3 s.
- **Tar:** tar, 1.2 cells, 4 s.
- **Cryo Shells:** frost ground, 3 s.

How special shells are fired:

- **Every 3rd shell** is a special shell. With two specials owned, two shells in three are special:
  the first special, the second special, a plain shell, and over again.
- **A shell carries one type.** A special shell looks different in flight and explodes
  differently. A Tactical Nuke is a type of its own and never carries a special.
- **Every shell counts**, including each shell of a Barrage: with Cryo and Tar, eight shells in a
  row are Cryo, Tar, plain, Cryo, Tar, plain, Cryo, Tar.
- Shrapnel and bomblets don't make zones: shrapnel applies its shell's effect to the enemies it
  hits directly (Fragmentation II), and the zone appears once, at the impact.

**Look:** shell size grows with damage; a faster shell is smaller with a streak; napalm an orange
flickering patch, tar a dark glossy one, frost pale blue, fallout a sickly green.

**Purpose gate** (head III, feature 2): 15 bracketed shells (landing within 1.5 cells of the
last).

### Cinder - the Pyre

**Why build it.** Magic damage over time: it shrugs off armor and plating, scorches resilience so
every crit lands harder for everyone (and wears crit immunity down), and burns what hides or
heals. Its cone is short, so build it where the path bends around it: each enemy walks through
several waves, and every wave stokes the fire higher. **Buy it when** groups of mid-health
enemies come, against healers and regenerating bosses, against ghosts that pass close, alongside a
crit defence. **Weak against** far-away straights, burn-immune enemies (Soulfire answers), and
freeze-heavy teammates (unless Thermal Shock, or the Hexer's Ash).

```
CINDER   $28 | 1.5 magic burn a wave | range 2.2 | cone half-width 0.35 rad | about a wave a second
         burn 3 s | nearest enemy
         a burn pulse never crits; a crit ignition starts the pool at the crit multiplier
ATTUNE   Stoke: a wave on an enemy already burning from this Cinder raises its burn +10%, up to
         3 times; the stacks go when the burn ends
AWAKEN   Bellows: an Aura's fire-rate buff on this Cinder also widens its cone 10% per +10%
RANGE    +15%  ->  II +10%  ->  III Long Nozzle: +20% range; the wave travels twice as fast
HEAD A   White Flame (hot and short: the single enemy melts)
         I    +30% damage; Stoke steps +15%
         II   +50% damage, -25% burn duration, +10% crit (on ignitions)
         III  Soulfire: every other wave is Soulfire, a blue burn in its own pool that stacks
              with ordinary burn, earns Sickened as well as Scorched, and burns the burn-immune;
              Stoke applies to both
         IV-A Flashpoint: +20% crit; a crit ignition also adds 3 Scorched
         IV-B Combustion: the pool cap doubles; a pool that reaches its cap bursts for half of it
              onto enemies within 1 cell
HEAD B   Wide Nozzle (wide and long: the crowd smoulders)
         I    +25% range, +30% cone; an enemy that leaves the cone keeps its Stoke for 2 s
         II   +20% range, +20% fire rate, +20% cone
         III  Lingering Flames: each wave leaves a patch of burning ground where its target
              stands, for 2 s
         IV-A Inferno Ring: the cone becomes a full ring at -25% range
         IV-B Dragon's Breath: x3 fire rate, per-wave damage divided by 2.5, +30% range: a
              continuous stream that keeps every pool topped up and Stoke always full
EXTRA    Fuel (burn chemistry)
         I    Kindling: burning enemies earn Scorched twice as fast
         II   Cauterize: burning enemies receive 50% less healing and shielding
         III  Heat: burning enemies take +10% damage over time from every source
         IV   Everburn: a pool never decays below 25% while its enemy is in this Cinder's range
SPECIAL  one shared set: Searing Flame, Thermal Shock, Pyromancer's Mark
```

- **Awaken carries a perk on the Cinder:** Bellows. It is the one tower whose Awaken does more than
  unlock.
- **The Cinder owns regeneration and heals,** through Cauterize; burning alone doesn't cut healing.

**Specials** (all on the ignition and the pool):

- **Searing Flame** (today's Hexflame, renamed: hexes are the Hexer's): an ignition grants
  Vulnerable, and so does every wave that hits a burning enemy, at most one stack a second.
- **Thermal Shock:** when a burning enemy is frozen by anything, its remaining pool detonates as
  one hit at 150% (instead of the global 50%) and chills its neighbours. Fire and ice become a
  combo.
- **Pyromancer's Mark:** burning enemies take +15% magic damage from every source.

**Look:** flame colour by tier: orange, white (White Flame), blue (Soulfire), deep red under
Searing Flame; Stoke as a brighter core per step; lingering flames as flickering patches.

**Purpose gate** (head III, feature 2): 45 waves that Stoke an enemy already burning from it.

### Also in this feature

- **Bleeding** (new effect): physical damage per cell travelled; a stopped enemy doesn't bleed. The
  faster it runs, the more it bleeds. Applied by Shrapnel Storm.
- **Cracked** (new effect): plating -50%, 5 s, refreshes. Applied by every Mortar shell (Bunker
  Buster no longer needs to add it).
- **Soulfire** (new effect): a third fuel pool beside burn and poison; earns Sickened and Scorched.
- **Tarred** (new effect): put on by tar, while inside and briefly after; a burn it catches starts
  at double pool, and a freeze it suffers lasts 1 s longer.
- **Splash Range III gains Mines:** every 4th blast leaves a mine on the path where it landed; the
  next enemy to step on it sets it off as one blast. A mine lasts 10 s, and one Splash keeps at
  most 3 (a fourth replaces the oldest).
- **The Hexer's Mastery gains its cursed cloud** (feature 4).

### What happens to today's nodes

| Today | Becomes |
|---|---|
| Mortar 20 damage, every 2.5 s, range 4.0, blast 2.0, shell speed 40 | 32 damage, every 3.5 s, range 4.5, blast 1.75, shell speed 8 |
| Mortar base chill 50% 2 s | gone; chill is the Cryo special; every shell Cracks plating instead |
| Siege Rounds II (+40% radius) | +25% radius and +10% crit |
| Fragmentation II (chill, gated on 2 adjacent towers) | +30% to everything the shrapnel does |
| Cursed Shrapnel (Vulnerable) | retired; Cracking plates is the base passive |
| White Flame II (+50% burn duration) | -25% burn duration and +50% damage: "hot and short" |
| Hexflame (Vulnerable on a new ignition) | Searing Flame (on every wave that hits a burning enemy, once a second) |

## Interconnections

- **Feature 1:** Attune passives, level IV. The Cinder's Bellows is an Awaken perk, the only one.
- **Feature 2:** XP and the purpose gate kind (bracketed shells and Stokes are the deeds).
- **Feature 3:** periodic damage never crits (burns and zone pulses), Sundered (Bunker Buster),
  Scorched with Resonant Crack opening crit immunity.
- **Feature 4:** Dazed (Heavy Shell); the Hexer's Rime (freeze bursts) and Ash (bigger pools); the
  cursed cloud and the Splash's Mines are the zones it waited for.
- **Feature 5:** projectile speed and size as stats; the Seeker's freezes meet Thermal Shock and
  the global burst; the Pulse's Undertow holds a column inside the bracket.
- **Feature 7:** the Aura's fire-rate buff feeds Bellows; Conduit lengthens burns and zones'
  effects.

## Constraints and open risks

- **Zones are a new shared system:** a world list beside projectiles, a new draw command, and many
  enemies tested against many zones every pulse. A Barrage of Napalm shells, Lingering Flames on
  every wave and a cursed cloud per hexed death can put many zones on the board: the per-tick and
  per-frame budgets (`td.PerformanceHarness`) are the check. A cap per tower was rejected as a
  design rule; if the budget needs one, it is an implementation call.
- **"Zones don't stack":** when two zones of one kind overlap, the enemy takes one pulse a pulse,
  credited to the zone that was there first.
- **Special shells and the Nuke share one shell count.** Every 4th shell is a nuke; the special
  pattern runs on the remaining shells.
- **Slow shells miss.** Predictive Fire (Ballistics II) is the answer; until it is owned, a fast
  enemy dodges by design.
- **Bleeding reads distance travelled,** which nothing tracks per enemy today.
- **Fire and ice rules change existing balance:** freeze no longer simply wastes a burn, and
  burning reveals ghosts. Today's Seekers next to Cinders change value.
- Visual budget: patches, flames and the ranging marker are per-frame draws.

## Decisions made

- Zones ship with their two main users, the Mortar and the Cinder, after the control towers. They
  are the most expensive shared system after hexes, and the Splash's zone pieces (Mines, the
  cursed cloud) wait for them.
- Mortar: no base chill; **every shell Cracks plating** (your round-2 call; Plate Cracker the
  special is gone); dead zone 1.5 cells, 2.5 with Long Battery, never scaled by range.
- **Leading the target is Ballistics II** (Predictive Fire), not base. Proximity Fuse is dropped.
- Ballistics IV is **Barrage** (your call), replacing Twin Barrels.
- The Mortar's specials are only zone shells: Napalm, Tar and Cryo; every 3rd shell; one type per
  shell; nukes never carry one (your rules). Shell Shock and Fallout-as-a-special are not taken.
- Fragmentation II is your "30% more of what it already does".
- Tactical Nuke's Fallout covers 0.8 of the blast's radius (about half the area).
- Cinder: cone stays 0.35 rad; burn damage stays 1.5 (the `README.md` line is corrected); burning
  enemies get 50% less healing only with Cauterize.
- Soulfire alternates with ordinary burn (odd waves classic, even waves Soulfire), Stoke applies to
  both (your call).
- Kindling only speeds Scorched; it no longer lowers burn resistance (your call).
- **Heat (Fuel III) is "+10% damage over time taken"**, not "+10% magic damage": as written it
  duplicated the Pyromancer's Mark special you picked.
- Specials: Searing Flame, Thermal Shock, Pyromancer's Mark. Wildfire, Heat Haze and Phoenix Ash
  are not taken.
- Bellows (your pool pick) is the Cinder's Awaken perk.
- **Mines' home is the Splash's Range III** (every 4th blast leaves one). You wanted Mines once
  zones exist, but the Splash tree had no free slot.
- **Zones are kept in full** (your call). **Simplified for cost** around them:
  - **zone-to-zone interactions** (fire igniting tar, frost putting out fire, frost hardening tar)
    become enemy-effect rows: the same combos, with no zone ever reacting to another zone;
  - **Fallout** drains spirit and blocks healing, instead of turning regeneration into damage (that
    needs the same heal-to-damage hook as Hex of Inversion, for a 4 s patch);
  - **Lingering Flames** leaves one patch where its wave's target stands, instead of burning the
    stretch of path the cone swept (a cone-shaped zone, cut along a looping path).

## Open questions

- None blocking. Patch sizes and lengths, Mines' 10 s and 3 per Splash, and the special-shell
  pattern are starting points for the balance pass.
