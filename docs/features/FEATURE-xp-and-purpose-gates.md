# Feature Request: XP and Purpose Gates

**Status: implemented.** Feature 2 of 7 in the tower rework (order in `TODO.md`). Needs feature
1 (`FEATURE-tower-progression.md`). XP is paid by `ExperienceAwarder` when `EnemyRoster` reports a
walk's end; reach is `Tower.reached` (an entry ordinal and `ArcLengthPath.passesWithin` over the
stretch walked); XP and deeds live in `TowerExperience`; the gate table is `UpgradeTier`'s XP;
`PurposeCondition` is the purpose gate kind. No tower has a deed yet: each arrives with its own
feature (3 to 7), with the info row that shows its count.

## Summary

A new way to gate upgrades, balanced by its design rather than by tuning. Two conditions replace
today's kill and damage-dealt gates:

- **XP**, one progress number per tower that says how much of the fight it has seen. Every tower
  earns it the same way, whatever its job, so every tower opens the same levels at about the same
  wave.
- **A purpose gate**, one per tower, on its head level III, that says the tower has been doing *its*
  job: the Sniper staying on a target, the Mortar shelling one spot, the Seeker freezing.

Together they read: *this tower has been in the fight* (XP) and *it has been used for what it is
for* (purpose). Neither can be farmed, and neither needs per-tower weights tuned before it works.

## Current state (what exists today)

- **Gates:** a node is offered by its prerequisites (`requires`) and bought once its gate clears.
  Gate kinds: `KillCountCondition`, `DamageDealtCondition`, `ClusterCondition` (N of the 8
  surrounding cells hold towers) and `always()`, combined with and/or. A gate's `progress()` feeds
  the panel, and every button already says why it can't be bought.
- **What's wrong with them,** measured against play: damage gates (100-300) clear in one or two
  enemy passes; kill gates (8-25) take waves, favour whichever tower already hits hardest (only
  the killing blow counts), and starve support towers. Four of the Seeker's five nodes gate on
  kills, on the tower that kills least.
- **Bounty:** every enemy has a price, paid when it dies. Price grows with rank (a Simple mob pays
  2 as a Grunt, 13 as a Veteran, 83 as a Boss), and a swarm splits one slot's bounty across its
  members, so a swarm is worth no more than one enemy. Rank also carries a score multiplier, used
  only for the score.
- The tower panel shows kills and damage dealt; nothing shows progress toward anything but the next
  gate.
- Feature 1 adds Attune (the signature passive), Transcendent and the extra head node; until this
  feature lands, its new nodes are price only.

## What this feature adds

### XP: how much of the fight a tower has seen

- **The rule:** when an enemy's walk ends (killed or leaked), **every tower it had reached earns
  XP equal to its bounty.** "Reached" means the enemy entered the tower's range after the tower
  was built: only enemies that spawned after the tower was built count, and reach is measured with
  the tower's range without disruption.
- **The Aura** has no reach on the path: it earns an enemy's XP when any tower it buffs earns it
  (once per enemy).
- XP is never spent, never lost while the tower stands, and only climbs. Selling loses it. It is
  not carried between levels.
- Shown as a **bar** in the tower panel, with a tick at the next gate ("XP 120 / 150 to Weak
  Spot"), and the total in the tower's info rows.
- **Ranks** you can see, at the XP that opens each tier, so there is nothing new to learn: Recruit,
  Seasoned (50), Expert (150), Hero (300). Shown as pips on the tower, with a brief glow ring at
  each rank-up. Purely visual.

### Purpose gates: the tower has done its job

- Each tower counts one **deed**: the moment it does its job. For most towers that is its
  signature passive (its Attune, feature 1) paying off; the Seeker's is a freeze landing, since
  its passive (the nest) pays off only after quiet stretches; the Aura's is a layout condition. The
  count starts when Attune is bought.
- **Each deed counts at most once per attack or per second**, never per enemy, so a crowd doesn't
  multiply it and a tower's own cadence sets its pace.
- **The purpose gate sits on head level III** (both chains): a tower gets its chain's defining
  verb once it has proven it is used for its job. Transcendent needs a head III, so it inherits the
  gate.
- The threshold is **about 75 seconds of doing the job** at the tower's base cadence (with Attune
  owned). Owning head I and II only makes deeds come faster.

| Tower | Deed | Base rate (estimate) | Purpose gate |
|---|---|---|---|
| Sniper | a shot at a target it has already shot (a shot under Steady Aim) | 0.4 shots/s, ~70% on a held target | 20 shots |
| Splash | a blast whose primary target is Saturated | 1 blast/s, ~60% | 45 blasts |
| Sonar | a Ping: a revolution that Exposes an enemy | 1 per 3 s | 25 pings |
| Pulse | a second during which an enemy at full Toll is inside | ~0.4/s at a bend | 30 seconds |
| Mortar | a bracketed shell (landing within 1.5 cells of the last) | 0.29 shells/s, ~70% | 15 shells |
| Seeker | a freeze that lands (not resisted or diminished to nothing) | ~0.35/s | 25 freezes |
| Cinder | a wave that Stokes an enemy already burning from it | ~1 wave/s, ~60% | 45 waves |
| Aura | buffs 4 or more towers (a layout condition, not a count) | - | 4 towers |

- Shown on the gated node like any gate ("Steady Aim shots 12 / 20"), and the count in the info
  rows.
- Each tower's deed and gate land with its own feature (3 to 7), because most deeds read that
  tower's new passive. This feature builds the gate kind.

### The gate table

The same on every tower. Nodes behind Transcendent need nothing more: Transcendent is their gate.

| Node | Gate |
|---|---|
| Range I, Attune, Awaken, Range II | price only |
| head I | price only |
| head II | XP 50 |
| head III | XP 150 and the purpose gate |
| extra I / II / III | XP 25 / 75 / 200 |
| special (slot 1) | XP 150 |
| Transcendent | XP 300 (plus an owned special and a head III, feature 1) |
| head IV, extra IV, Range III, special (slot 2) | Transcendent only |

**Layout gates stay** where content asks for them: the Aura's purpose gate (4 towers buffed) and
Keen Edge's "next to another Aura" (feature 7). Kill and damage-dealt gates are removed, and every
existing node moves to this table by its level.

### Why this balances by design

- **One currency the game already balances.** XP is bounty, the value every level already pays its
  player in. A level's XP curve is its money curve, so every present and future level produces XP
  on the same scale as credits, with no XP numbers per enemy or per tower.
- **Per enemy, never per hit.** Fire rate, tick rate, damage and area don't change XP: a Pulse
  touching every enemy 20 times a second earns the same as a Sniper beside it. No flooding, nothing
  to scale per tower.
- **Swarms are worth one enemy** (bounty is split across members), so area towers don't earn more
  on swarm waves.
- **No kill credit.** Nobody can steal XP and support towers don't starve: every tower an enemy
  reached earns it, assists included by construction.
- **Nothing to farm.** XP doesn't grow with time, so stalling or freezing an enemy earns nothing
  extra; leaked and killed enemies pay the same, so there is no reason to leak; XP can't be moved
  between towers.
- **Late builds catch up by themselves.** Bounty grows wave on wave, so a tower built late meets the
  richest waves: on Twisted Hourglass, a tower built after wave 6 reaches head III in wave 8.
- **Purpose is measured in time, not crowds.** Deeds count at most once per attack or second, so
  each threshold is a known number of seconds of doing the job, the same for every tower.
- **XP says "in the fight", purpose says "doing its job".** A tower built where nothing passes
  earns neither; a tower in the fight but misused (a Pulse on a straight, a Mortar spread over
  three lanes) earns XP but its purpose lags, so it gets head I and II but not its defining verb.

**Where the gates open** (a front-line tower built before wave 1, every enemy of its lane reaching
it), computed from the built-in levels' bounty:

| Level (lanes) | Waves | head II (50) | head III, special (150) | extra III (200) | Transcendent (300) |
|---|---|---|---|---|---|
| Curly Path (1) | 18 | wave 4 | wave 7 | wave 8 | wave 9 |
| Zig Zag (2), each lane | 8 | wave 4 | wave 6 | wave 7 | wave 7 |
| Twisted Hourglass (3), by lane | 10 | wave 3-4 | wave 6-7 | wave 6-7 | wave 7-8 |

A tower covering two lanes sees both lanes' bounty and gets there a wave or two sooner: placement
pays. Back-line towers see only what survives the front, and get there later.

### Out of scope

- Per-hit, per-job-event, assist, kill and presence XP from earlier drafts (see Decisions).
- XP bonuses per rank, rank perks, late-build boosts, passing XP on when selling.
- The diversity gate ("3 different tower types adjacent"): no node uses it. It comes with the first
  node that does.
- XP carried between levels.

## Interconnections

- **Feature 1** provides the tree shape, Attune (whose passive defines each deed) and
  Transcendent (whose XP gate this feature sets).
- **Features 3 to 7** each add their towers' deeds and put the purpose gate on their head III.
  Until then, today's level-III nodes (the Sonar's and the Splash's) gate on XP only.
- **Feature 7:** the Aura's XP mechanics (Kinship's +5% per tower type, Tutelage, Apprentice,
  Shared Lessons) build on this; Chosen picks the most experienced tower. The balance pass checks
  the deed rates and the bounty curves against these tables.
- `FEATURE-tower-upgrade-trees.md` and `FEATURE-tower-specialization-abilities.md` list kill and
  damage gates that this feature removes.

## Constraints and open risks

- **Cost.** Deciding which towers an enemy had reached is cheap if done once when its walk ends;
  tracking every tower's enemies every tick is not. The performance budget (every buildable cell a
  tower, last level) is the check.
- **"Reached" on a looping or multi-lane path:** a tower covering a lane twice still counts an
  enemy once. An enemy that appears mid-path (a summon, a hatched egg) reaches only the towers
  downstream of where it appeared.
- **Disrupted range** is ignored for reach, so a Jammer doesn't cost XP.
- **The thresholds assume today's bounty scale.** If bounties are rescaled across the board, the
  XP table moves with them; the table above is the check.
- **Deed rates are estimates.** If the harness finds one far from its table row, change that
  tower's threshold to keep "about 75 s of doing the job", never the rule.
- **Tests churn:** existing tests build nodes with kill and damage gates. Prefer the shared
  fixtures over hand-editing each (standing preference).
- Standing requirements: XP and gates are engine state (headless); the bar, the rank pips and the
  glow follow the HUD look and the render pipeline.

## Decisions made

- XP and purpose replace kill and damage gates; layout gates stay where content uses them.
- **XP is bounty seen, not events scored.** Earlier drafts (and your round-2 approval) built XP from
  hits, job events, assists, kills and presence, each with a weight per tower. That is balanced by
  tuning: every weight needs the harness, and the known risks (fast towers flooding, support towers
  starving) stay open until tuned. The bounty rule has no weights, and the job half moves to the
  purpose gate, which you asked to keep.
- Kills and leaks pay the same XP: no death spiral when a defence starts leaking, no reason to
  leak.
- One deed per tower (not per chain), on head III only.
- Ranks are the XP tiers themselves (50 / 150 / 300) and purely visual. The +3% damage per rank,
  the per-tower rank perks, the late-build boost and passing XP on when selling are dropped: each
  was a small rule with its own exploit or tuning (selling cheap towers to pump a neighbour's XP).
- Tower ranks are Recruit, Seasoned, Expert, Hero: the enemy ranks already use Veteran and Elite.
- Specials need XP 150; the second special, head IV, extra IV and Range III need only
  Transcendent.

- **As built:**
  - Reach is measured once, at walk end, with the tower's range at that moment (before
    disruption): a range upgrade bought mid-walk counts for the whole walk.
  - "Spawned after the tower was built" means went live after it: wave mobs wait out a spawn
    delay, so a tower built mid-wave counts the mobs still waiting.
  - The existing layout gates (`ClusterCondition` on the Aura's nodes, Wide Band and
    Fragmentation Rounds II) stay, on top of the node's XP.
  - XP is node data (`UpgradeNode.xp`), not a condition, so the panel's bar can tick at the next
    node XP opens. The hover shows XP and any other gate as separate ✔/✘ rows.
  - Rank pips are silver diamonds stacked beside the tower (slot pips stay below it), and the
    rank-up glow is a silver ring that widens and fades over 1.5 s. The info row reads
    "XP 160 · Expert".

## Open questions

- **The Aura's XP** when it buffs towers on two lanes: it earns from either, which is intended
  (it crowns a cluster), but the balance pass should confirm it doesn't outpace its towers.
