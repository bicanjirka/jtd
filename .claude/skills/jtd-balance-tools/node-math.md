# Node math on paper

How to weigh next-gen upgrade nodes from a tower doc's rows, before any code exists: the harnesses in `SKILL.md`
need the nodes built. The answer goes in the node's Notes and in the doc's "How the tree builds" (or Decisions made),
and the commit body names the numbers that moved and why.

## Assumptions

1. **Everything in earlier tiers is bought.** In an earlier cluster, the member best for the build being weighed.
   A node of the same tier outside the node's own cluster is weighed both ways and given as a range
   ("+25-32%": with and without Heavy Rounds II).
2. **Passives at their steady state.** The Sniper's Steady Aim is at its cap (higher with Deep
   Breath), so its attack speed and crit chance are always on.
3. **Measure damage a second against the target the node is for**, relative to the same build without the node:
   "+20%". A bonus against Elites and Bosses is weighed against an Elite or a Boss, a crowd node in a crowd.
4. **Enemy numbers come from the code**, never from memory: `src/main/java/td/enemy/*Enemy.java` (`withArmor`,
   `withPlating`, `withMagicResist`, `withInsulation`, per rank) and the affixes in `docs/WAVE-SCRIPTS.md`'s affix table
   (Armored and Plated grow with the level's physical damage).

## What a cluster must be

A cluster's members are **even** (damage a second within a few points of each other in the build they fit) or
**situational**: each wins in its own situation, such as one enemy against a crowd, an Elite or a Boss against the
rest, plain against armored. A situational pair states its break-even ("the Silver Round wins from armor 33 up",
"Shockwave matches it from 10 enemies up"), and the break-even must fall inside what the game's enemies span.
A member that wins everywhere is a pick with one right answer: retune it.

A node outside a cluster is weighed against the other nodes of its tier, so no one of them is a must-buy or a
never-buy.

## Formulas

- **A hit**, in the game's order: crit (× crit damage) → shield → mitigation, keeping `100 / (100 + armor)`
  (magic resist for magic) → flat reduction, losing plating points (insulation for magic), never below 0 → damage
  taken.
- **Crit share** of shots: `c` per shot rolled; a sure crit every n-th shot adds `1/n` and leaves `c` on the rest:
  `crit share = 1/n + (1 - 1/n) · c`.
- **Damage a second** = damage per shot (`1 + crit share · (crit damage - 1)` times the hit) × shots a second.
  Attack speed bonuses add: shots a second = base rate × `(1 + Σ attack speed)`.
- **"% of base damage"** reads the tower's damage stat after its flat damage nodes (Heavy Stones counts), so an
  ability and a flat node multiply. Weigh it with and without the flat node.
- **Flat vs percent bonus**: with every flat node bought they are the same number; a flat one reads like the tower's
  other damage rows. Size it on the final damage stat: `bonus / damage` is its percent.
- **Mass vs single**: an ability on every enemy in range for `p%` matches a single-target one for `q%` at `q / p`
  enemies.
- **Bonuses that turn a defence around** (a hit growing by `(100 + armor)%`, or by plating points) scale with the
  enemy's defence, not with the tower: check them on the highest values in the game, the affixes included, where a
  small hit can grow several times over.

## Shot sequences

Anything that chains on shots (Frenzy after a crit, a sure crit every n-th shot, a status for the next shots) is a
sequence, not a formula. Simulate it with `node_sim.py` beside this file:

```bash
python .claude/skills/jtd-balance-tools/node_sim.py --crit 0.21 --attack-speed 0.32 --fourth 4 --frenzy 0.35 --frenzy-shots 3
```

It prints damage a second against a build without the chained bonus. Give the range with and without the passive's
attack speed in the base (`--attack-speed 0`), as the passive's stacks may not be up on every target.

## Area towers

- **A path is one cell wide**, so a blast reaches enemies in proportion to its radius, never its area: a blast 1.5
  times wider hits 1.5 times as many. `area_sim.py cover` gives the cells of path a set of zones covers.
- **The default crowd** is Walkers at a wave's default spacing (`SpawnParameters.DEFAULT_DELAY_TICKS_PER_SLOT`):
  `paper.py` derives how far apart they walk (`SPACING`) and how many stand on a cell (`DENSITY`). Denser waves, slowed enemies and
  corners scale area damage up linearly.
- **Grounds** (burn, poison, tar, frost, fallout) are sequences: pulses twice a second, a pool topped up by
  `L0 · (1 - level / (2 · L0))` and decaying over its authored duration, a freeze after 4 pulses in a row inside.
  Simulate them with `area_sim.py ground`; never take "the pool holds its level" as the steady state.
- **A weighted magazine** (or any queue drawn by weight with an order rule): `area_sim.py magazine`. A "never the
  same kind twice" rule inflates small weights.
- **Enemy health** for a stream: the Walker's ladder (`WalkerEnemy`; `paper.py` holds the factors it uses); every other
  enemy's is written rank by rank to its weight against it. Weigh anything that earns from deaths at 75, 300 and
  1000 health.

## Comparing towers per dollar

- **Damage while a stream crosses the range, per dollar of the tower**: damage a second × cells of path within range
  (a straight path a cell from the tower, minus a dead zone) / the tower's price. One enemy for a single-target tower,
  the default crowd for an area tower. This counts range; raw damage a second per dollar does not.
- **Per tier, with every node of the tier bought** in the build being weighed, from Tier 0 up. The Sniper is the
  single-target yardstick; the figures are `reference.json`'s. No group yardstick is chosen: the Mortar was one, and
  is in doubt.
- **The floor and the ceiling.** The bare Burst with only its base tree (no Stormcaller or Hexer, nor their nodes)
  is the floor: no tower's build may be weaker at the same tier, on one enemy or on a crowd. The Sniper is the
  ceiling on one enemy: nothing may be stronger there. A build past either is reported, to be tuned, not let be.
- **Node prices follow each node's worth** (`tree_calc.py prices`), the same however many copies stand. "/$"
  divides by the tower's price alone, "/$ all" by the tower, its nodes and its unlocks; say which. A tree that
  multiplies damage about as much as another's keeps the bare towers' ratio.
- **Defences**: run plain, a Knight (armor and a little plating) and a Juggernaut (heavy plating) at least. Small hits
  lose most to plating; a tower's own Crack lowers it for every hit after the first. A Knight's resilience stops
  every crit, a sure one too.

## The tree calculator

`tree_calc.py` does the comparison above for every named build of a tower at once. A tower that has a model in
`trees/` is weighed there, not by hand.

- **The tree comes from the doc**: tiers, nodes, clusters, After and the tier unlocks are read from the doc's "Tiers"
  and "Upgrade tree" tables; an After names a node or a cluster, met by any member. `check` fails when the model's node names and the doc's differ, or a named build
  breaks an After or holds two members of a cluster.
- **A model** (`trees/<tower>.py`) holds `DOC`, `PRICE`, `ROLE` (`single` or `crowd`), `P` (every number, each one
  `--set` can change), `NODES` (each node and what the model makes of it; `utility:` for a node it can't count),
  `BUILDS` (the strategies, each a list of nodes from Tier 0 to III), `reach(build, p)` (range and dead zone) and
  `rate(build, scenario, p)` (damage a second, or a pair with enemy-seconds of control a second). A build is a
  frozenset of node names; a cluster's active member stands for the cluster. Shared physics is in `paper.py`:
  `hit` in the game's order, crit chance and factor after resilience, `zone_hits` for a blast on a crowd.
- **Scenarios** (`paper.py`): Walkers in a crowd at 75, 300 and 1000 health, one at a time at 300, an Elite and a
  Boss, all with no defences; they make the mix, crowds weighted double. `endless` (one enemy that never dies) is the
  old yardstick. The defences (a Knight, a Juggernaut, a Relay, a Golem, a Templar and a Runner, each at its
  base rank's defences on a crowd of 300: plating and insulation, armor and magic resist, both at once, pace; a burn-immune enemy is left out, since a counter to it adds the
  whole tower) read as a share of `crowd 300`: they show what a build counters, and set half of a counter's price
  (below). A model may add its own plain scenarios (`SCENARIOS`) for copies that work together, each reading what
  one copy deals: the Scorcher's `pair 1000`, two copies pouring into the same pools. It joins that model's mix and
  role only, so `vs ref` reads it against a Sniper that has none.
- **A stream dies**: a model kills its enemies one after another, so overkill is lost, executions and kill procs
  count, and a crowd's next enemy is the one a hop or a blast already hurt. An area model caps each hit at an
  enemy's health but not damage spread over several shells; a tower that fills a bar earns from its own kills only
  (deaths are its damage over an enemy's health, at most the stream), so a full defence pays it more in play.
- **Damage over time takes a hit's steps**, plating included, so a small tick against plating comes to nothing.
- **Control is priced as time.** A model returns enemy-seconds held back a second: a slow's share of each second
  it holds (Speed -30% for 1 s is 0.3 s), a Daze or a freeze all of it. Each is worth what `paper.NEIGHBOURS` (four) bare
  reference towers deal that enemy in a second, and in a crowd they share their fire among the enemies in their
  range (`held_back` in `reference.json`). It adds to the value, so it prices like damage; the `control` column shows
  the seconds alone. `utility:` nodes (Reveal, Silence, anti-heal, aiming) count only for the damage they change.
- **A faster enemy** (the Runner) walks further apart at the same spawn interval, so a blast reaches fewer of it;
  negative agility raises crit chance. A positive agility's dodge is left out.
- **Bounds** (`tiers`, at the end): every named build at every tier, per dollar of its tower, against the floor
  (the Burst model's `base` build) and the ceiling (`single_best` in `reference.json`): one enemy is the mean of
  `lone 300`, `elite` and `boss`, a crowd the mean of the three crowds.
- **Role** is the mix of the scenarios a tower is for (one enemy, or a crowd); `vs ref` compares it, tier by tier,
  with the reference's median named build. Outside `--band` it reads weak or strong.
- **The reference** is `reference.json`, saved from the Sniper with `save-reference`: its median builds per tier and
  `rate`, the value a dollar buys in a copy once `BREAK_EVEN_COPIES` copies stand. A change to the Sniper shows
  against it until it is saved again on purpose.

- **A fair price** (`prices`) is a node's Shapley share of its tier: in each named build, with the lower tiers
  bought, what it adds averaged over every order that tier's nodes can be bought in, so a tier's prices add up to
  what the tier adds, whatever comes first. It is averaged over the named builds, each with every combination of its clusters' members swapped in (so a price never hangs on which member a named build owns), and divided by `rate`: with two
  copies standing, a node is as good a buy as a third copy; with fewer, a copy is better, with more, the node. Every
  node is priced halfway between what it adds in the Walker mix and that plus its edge on the defence it does best
  on (what it adds there over what it adds on the plain `crowd 300`, never below nothing): the mix alone underprices
  a counter, its best enemy alone overprices it.
  A node worth under 1% in the mix and at least three times that against one defence is marked a counter.
  "+10% of a Tier N build is worth $X" sizes a node before it has numbers.
- **A cluster** is weighed by swapping each member into the same builds: `even` (within 10% in the mix),
  `situational` (each member wins some scenario, or controls), or `one right answer` (a member never wins). It is
  bought once, so it costs its members' mean. Members whose fair prices lie
  more than half their mean (and $5) apart read `rebalance`: one price can't fit them, so the cluster is retuned before
  it is built.
- **Combos** (`combos`): for each pair of nodes a named build owns, with the rest bought, the extra the two add
  together beyond each apart, as a share of the build; over `--synergy` it reads `OP?`. Damage and range multiply,
  so two big levers always pair a little; a node that only changes another's (an After, a crit payoff) pairs high
  of the pair and low of the build. Then each tier's named builds per dollar of everything bought, against their
  median: a strategy outside `--band` is ahead or behind.
- **A new tower**: copy the closest model in `trees/`, keep `P`'s names in the doc's words, list `NODES` from the
  doc's tree, name two to five `BUILDS` (one per strategy the doc means), and `check` it. Reproduce one number the
  doc already states before trusting the rest.

## Bar-fed abilities

- **Income** a second = base-shot damage dealt (to every enemy it hits) / bar size + tagged deaths a second × what a
  death fills / bar size. Only the base shot fills the bar; counting ability damage into it makes a runaway loop.
- **A refund per cast** (a crit adding to the bar) lowers an ability's cost: casts = income / (cost - refund), and each
  unit charges every owned ability, so a refund raises them all.
- **Weigh both cases**: the tower alone (its deaths are capped by its own damage / enemy health) and a full defence
  (every enemy it tags dies: deaths = the stream, about 0.9 a second). A passive paid by deaths earns from other
  towers' kills, so the second case can far exceed the first; report the cast interval with the damage.
- **Against a boss alone** only the base shot fills the bar: bar / base damage a second is the wait for one unit.
- **Abilities sized as a share of base damage** grow with every flat damage node, so a damage node in an ability tower
  counts twice: weigh it with its abilities.
