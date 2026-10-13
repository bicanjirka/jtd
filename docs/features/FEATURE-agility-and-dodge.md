# Feature Request: Agility and Dodge

**Status:** requested 2026-10-12. Implement after `docs/plans/stat-driven-buffs.md` has shipped.

## Summary

Give enemies an **agility** stat that lets them dodge an aimed attack completely, and give towers
ways to answer it. Today almost every upgrade trigger and synergy runs through critical hits; dodge
adds a second axis, so building against a wave becomes a choice between hitting harder, hitting
surely and hitting everything.

## Current state (what exists today)

- **There is no agility stat.** `FEATURE-enemy-stats.md` deferred "agility / movement inertia" (how
  fast an enemy regains speed after a slow) until a haste effect exists, and lists evasion as out of
  scope. `docs/ideas/enemies-brainstorm.md` repeats that deferral. `td.stat.EnemyStat` has no
  agility, dodge or accuracy constant.
- **Spirit already paces debuff recovery.** Every spirit-paced timer, chill included, runs at the
  enemy's spirit pace (never below a quarter, and not at all at spirit −100). So the old "agility
  recovers from slows" idea has no job left.
- **Crits are the trigger currency.**
  - Sniper: Focused Optics II's Frenzy, Momentum, Ricochet, Hollow Point, Sunder Rounds, Tradecraft's
    crit streak, Fifth Shot, Shatter Shot, Clean Shot; Executioner counts an execution as a crit.
  - Sonar: Rapid Array II refreshes Exposed on a crit; Ping and Wide Band expose; Mark on Sweep makes
    the next hit a guaranteed crit.
  - Splash: Conductor's arcs crit, Overload dazes on an arc crit, Thunderclap follows a crit,
    Thunderstrike counts as one, a Static Charge discharges double on a crit.
  - Pulse: the Arc Discharge zap crits (Meltdown adds crit chance). Seeker: Frostbite's guaranteed
    crits. Cinder: a critical ignition (Flashpoint).
  - Enemies: the Warden shields itself whenever it survives a critical hit.
  - After the stat-driven plan, resilience owns crit chance taken both ways, and Exposed is −50
    resilience.
- **One hit formula.** Every hit lands through `td.enemy.HitResolution`. Its crit roll draws from the
  world's random source only when the attacker has crit chance, so balance runs stay reproducible.
- **Some attacks already miss physically.** The Mortar's shell flies to where its target stood and a
  fast enemy outruns it (Predictive Fire answers that). The Seeker's missile homes and never misses.
- **Attacks by shape:**
  - aimed at one enemy: the Sniper's shot (and its Ricochet bounces and Railgun line), the Seeker's
    missile, the Sonar beam's hit on each enemy it crosses, the Pulse's zap and its Tesla chains, a
    Stormcaller arc;
  - area: the Splash blast, the Mortar's blast, shrapnel and bomblets, the Pulse field, the Cinder's
    wave, mines, zones, bursts and shatters;
  - over time: burn, poison, Soulfire, bleed, a field's tick.
  - After the stat-driven plan, every landed attack a tower reports carries a hit kind (`DIRECT`,
    `PIERCE`, `BOUNCE`, `BLAST`, `ARC`, `BEAM`, `MISSILE`, `ZAP`), and procs fire on hit, on crit,
    on kill and every Nth shot.
- **No enemy has a trigger for avoiding something.** The ability triggers are periodic, once, health
  threshold, on death, time since last hit, on critical hit taken and on first damage taken.

## What this feature adds

**Agility and dodge**

- Enemies gain an **agility** stat, authored in absolute points like armor. It is 0 for every
  existing enemy unless content gives it.
- An enemy with positive agility has a **chance to dodge** an aimed attack completely. The chance
  grows with agility but never reaches 100%: each point matters less than the one before, the same
  feel as armor.
- Agility below zero means nothing extra. It never raises crit chance; that is resilience's job.

**What can be dodged**

| Attack | Dodgeable |
|---|---|
| Sniper's shot | yes |
| Sonar beam hit | yes |
| Seeker's missile | no: the Seeker is the tower that never misses |
| Splash blast, Mortar blast, shrapnel and bomblets, Pulse field, Cinder wave, mines, zones, bursts | no: area attacks can't be dodged |
| Burn, poison, Soulfire, bleed, any damage over time | no |
| Ricochet bounces, Railgun line, Pulse zap, Stormcaller arcs | open question |

- A dodged attack deals no damage, cannot crit, and triggers none of the tower's on-hit, on-crit or
  on-kill effects.
- A dodge is visible on the board (a "miss" mark where the hit would have landed, in the shared look)
  and countable in the enemy inspector.

**Towers answer agility**

- Towers gain an **accuracy** stat that cancels agility point for point, but never pushes it below
  zero. It comes from upgrades, statuses and auras.

**Content candidates.** The first version picks from these; the open questions decide which ship.

- Statuses that lower agility:
  - a frozen or Dazed enemy can't dodge;
  - a chill lowers agility with its strength;
  - a Pulse field lowers agility inside it;
  - Marked (Mark on Sweep, Tracer Payload) makes the next hit undodgeable, from any tower;
  - Exposed lowers agility as well as resilience.
- Tower upgrade effects that react to a miss:
  - Sniper "Patience": after a dodged shot, the next shot is a guaranteed crit;
  - Sniper Frenzy (Focused Optics II) also starts on a dodged shot, its crit trigger kept;
  - Sniper Steady Aim stacks also add accuracy;
  - Sonar "Echo": a dodged beam hit marks the target;
  - Aura: accuracy in what it gives nearby towers.
- Enemy traits:
  - "Nimble": flat agility;
  - "Evasive when hurt": agility grows as health drops, a sibling of hurt speed;
  - "Swarm Instinct": an aura giving nearby allies agility.
- Enemy abilities with a new **on dodge** trigger:
  - a short speed burst;
  - a brief vanish after three dodges in a few seconds;
  - "Learned": each critical hit taken grants a stack of agility, so an enemy adapts to crit towers.

**Out of scope for a first version**

- Haste and movement inertia, the other half of the deferred agility idea.
- Dodging area damage, damage over time or projectiles in flight.
- A miss chance for towers other than through enemy agility (blindness, fog).

## Interconnections

- **`docs/plans/stat-driven-buffs.md`** must ship first. This feature relies on its tower stat sheet
  (accuracy is a `TowerStat`, auras can give it), its procs and hit kinds (what is aimed; an "on
  dodge" tower trigger), its statuses, and its resilience rule (resilience, not agility, makes crits
  likelier).
- **`FEATURE-enemy-stats.md`** deferred agility as movement inertia and listed evasion as out of scope.
  This request takes the name for dodge. That document's out-of-scope lines should point here once
  this ships.
- **`FEATURE-critical-damage.md`** and every crit-triggered upgrade: a dodge happens before the crit
  roll, so a dodged attack never triggers anything crit-based.
- **`FEATURE-ground-zones-mortar-and-cinder.md`**: the Mortar's shell can already be outrun. A dodge is
  a different thing (a roll on an aimed hit), and the two must read differently.

## Constraints and open risks

- **Reproducible balance runs.** A dodge roll must draw from the world's random source only when the
  chance is above zero, or every existing balance run changes.
- **XP and purpose gates slow down.** Purpose deeds count attacks that landed; agile waves delay the
  Sniper's and Sonar's gates. That is the point, but it must not stall a tower's progression on a
  level full of agile enemies.
- **Crit-specials lose value against agile waves.** Most Sniper specials and several Sonar and Splash
  upgrades depend on landing crits. The content picked must leave each of those towers a counter.
- **Readability.** A dodge must read at a glance on the board and in the inspector, without a new
  platform-styled control, and must not be confused with a crit spark.
- **Standing requirements:** the engine stays headless; board content goes through the render
  pipeline; the hit formula stays in one place; the tick and frame budgets hold.

## Decisions made

1. Agility drives dodge only. Slow recovery stays with spirit, which already paces it.
2. Agility is authored in absolute points. The dodge chance is `agility / (agility + 100)`, the
   armor curve (25 -> 20%, 50 -> 33%, 100 -> 50%), with no cap of its own: it never reaches 100%.
3. Only aimed single-target attacks can be dodged: the Sniper's shot, its Ricochet bounces and the
   Sonar beam's hit. The Railgun line, the Pulse's zap and chains, Stormcaller arcs and the Seeker's
   missile are not.
4. A dodged attack deals nothing, cannot crit and triggers no on-hit, on-crit or on-kill effects.
5. Negative agility does nothing extra; resilience owns crit chance taken.
6. Accuracy on towers cancels agility and never makes it negative.
7. Implemented after the stat-driven buffs plan.
8. A dodge is not a hit for "time since last hit": an enemy that keeps dodging still heals when
   left alone.
9. A dodged Sonar hit still counts as seen: the enemy can still be pinged and revealed. Only the
   damage and what reacts to a landed hit miss.
10. First version content:
    - towers: a frozen or Dazed enemy can't dodge; Marked makes the next hit undodgeable; Steady Aim
      stacks add accuracy; Patience (after a dodged shot, the next shot is a guaranteed crit);
      Frenzy also starts on a dodged shot;
    - enemies: the Nimble trait (flat agility), Evasive when hurt (agility grows as health drops),
      an "on dodge" ability trigger used for a short speed burst; a new agile enemy, and agility on
      the Courier.
11. A dodge shows as a small grey "miss" rising where the hit would have landed. The inspector
    shows agility as a stat row with its dodge chance, and counts dodges.

## Open questions

- None for the first version. Still wanted later: chill, Exposed and the Pulse field lowering
  agility; Sonar "Echo"; accuracy from the Aura; Swarm Instinct; vanish after three dodges;
  "Learned".

---

*After planning and implementation, update this document rather than deleting it: mark it
implemented, prune resolved open questions, and either promote deferred scope to a new request or
note it's still wanted for a later version.*
