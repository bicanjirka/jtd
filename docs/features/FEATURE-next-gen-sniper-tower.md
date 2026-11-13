# Feature Request: Next-gen Sniper Tower

**Status:** draft, 2026-11-11, reviewed with the whole bundle 2026-11-13. Tiers 0 to III and the look, from the visual
request's Sniper section (`FEATURE-next-gen.md`, appendix) and its handwritten notes, rewritten to the node rules in
`FEATURE-next-gen-upgrade-tiers.md`. Part of `FEATURE-next-gen.md`.

## Summary

The Sniper's mechanics, upgrades, prices and look move to the next gen. Steady Aim becomes built in. Its damage is
single-target, with more and better crits. The tree has no fork: every node pulls one lever or pays crits out one way,
so the player mixes them into a build.

## What this feature adds

The tables below are read by `td.ui.PreviewUpgrades` (run-jtd), which draws the tree beside today's panel and lists
what the nodes no row replaces leave unused. The column rules are in run-jtd's `SKILL.md`.

### How the tree builds

- **Three levers**, plain stats every build buys some of: damage per shot, shots per second (Steady Aim) and crit
  chance.
- **Crits are the currency, paid out two ways.** A per-crit payoff does something on every crit, so more crits a
  second pay more: fast builds. A crit-size payoff makes each crit land harder or finish the enemy, so more damage a
  shot pays more: heavy builds. A hybrid takes one of each.
- **The Fourth Shot bridges them**: every fourth shot is a sure crit, so a fast tower crits more often by firing more.
- **The Ammo pick is general or against armor.** Hollow Point at +50% beat the Silver Round against every enemy but a
  heavily plated one, since no armor in the game reaches the break-even of 50. At +33%, with the Silver Round's
  magic penetration at 50 points, the Silver Round wins from armor 33 up.
- **The On crit and Finisher picks were lopsided.** In a build with Rifling, Heavy Rounds, Deep Breath, the Fourth
  Shot and Killshot, Frenzy at +100% added 34% damage a second, 78% with the Fourth Shot: its sure crit kept Frenzy
  up for about 88% of shots. Ricochet added 20-29% in a crowd and Sundering Crits 0-43% by armor, slowly. At +35%
  Frenzy adds about 20% with the Fourth Shot, every shot counting toward it. Headhunter's +36 added 64-82% against
  Elites and Bosses, against Executioner's roughly 10% on the rest; +14 adds 25-32%.
- **Steady Aim is a pick in Tier I**: more stacks (Deep Breath: speed and crit) or richer ones (Keen Eye: crit).
  Deep Breath leads by about 8% alone and 2-5% in a crit build with Frenzy, where Keen Eye's higher crit chance
  grants Frenzy more often. As Tier 0 nodes beside Rifling, Deep Breath beat Rifling in every build after about seven
  shots, by 10-16%, since stacks add attack speed too: a pick with one right answer.
- **Its names**: Steady Aim, the Fourth Shot, Frenzy, Ricochet, Sunder, the Silver Round and the execution. Sunder
  is the only debuff it puts on enemies.

### Bare tower

| Stat | Value | Today | Replaces |
|---|---|---|---|
| Price | $15 | | |
| Range | 3 cells | 4 | |
| Damage | 32 | | |
| Crit damage | ×1.5 | | |
| Crit chance | 0% | 5% | |
| Targets | First | | |
| Passive | Steady Aim: Built in, no Attune. Each shot adds a stack worth +4% attack speed and +2% crit chance, up to 5 stacks. | | base.attune |

### Tiers

| Tier | Gate | Unlock | Colour |
|---|---|---|---|
| 0 | - | - | #8fd18a |
| I | XP 60 | $20 | #5cc8b0 |
| II | XP 120 | - | |
| III | XP 200 | - | |

### Upgrade tree

| Tier | Node | Kind | Cluster | After | Glyph | Colour | Cost | Replaces | Does | Notes |
|---|---|---|---|---|---|---|---|---|---|---|
| 0 | Rifling | stat | | | sniper.special.shatter_shot | | | | Crit chance +5% | |
| 0 | Long Barrel | stat | | | base.range | | | base.range | Range +1 cell | |
| 0 | Heavy Rounds | stat | | | sniper.head.focused_optics.1 | | | | Damage +12 | |
| I | Fourth Shot | ability | | | sniper.special.fifth_shot | | | sniper.special.fifth_shot | Every fourth shot is a sure crit. | Today's Deadeye Round, renamed. |
| I | Deep Breath | stat | Steady Aim | | Steady Aim | | | | Steady Aim holds 3 more stacks. | |
| I | Keen Eye | stat | Steady Aim | | sniper.head.marksmans_eye.1 | | | sniper.head.marksmans_eye.1 | Each Steady Aim stack also gives Crit chance +2%. | |
| I | Killshot | stat | | | sniper.special.shatter_shot | | | sniper.special.shatter_shot | Crit damage ×2.0, up from ×1.5. | Name is a proposal. |
| II | Frenzy | ability | On crit | | sniper.head.focused_optics.2 | | | sniper.head.focused_optics.2 | A crit grants Frenzy: attack speed +35% for the next 3 shots. | |
| II | Ricochet | ability | On crit | | sniper.special.ricochet | | | sniper.special.ricochet | A crit also hits the nearest other enemy within 1.5 cells. | |
| II | Sundering Crits | ability | On crit | | Sunder | | | sniper.head.marksmans_eye.4b | A crit adds 2 stacks of Sunder. | Name is a proposal. |
| II | Silver Round | ability | Ammo | Fourth Shot | sniper.extra.tradecraft.3 | | | sniper.extra.tradecraft.3 | The Fourth Shot is a Silver Round: magic damage, Magic penetration +50 points. | Against armor: it beats Hollow Point from armor 33 up, and every magic resist in the game today is below 50. Plating does not touch it; insulation does. |
| II | Hollow Point | ability | Ammo | Fourth Shot | sniper.special.hollow_point | | | sniper.special.hollow_point | The Fourth Shot is a Hollow Point: Damage +33%. | About +11% damage a second over all shots, close to Killshot. A hollow point is a bullet whose hollow tip opens on impact: it hits harder and goes through less, the opposite of the Silver Round. |
| II | Long Barrel II | stat | | | base.range | | | base.range.2 | Range +1 cell | |
| III | Heavy Rounds II | stat | | | sniper.head.focused_optics.1 | | | | Damage +12 | |
| III | Executioner | ability | Finisher | | sniper.head.focused_optics.4b | | | sniper.head.focused_optics.4b | A crit that leaves an enemy other than an Elite or a Boss under 15% health kills it. | |
| III | Headhunter | ability | Finisher | | sniper.special.headhunter | | | sniper.special.headhunter | Damage +14 against Elites and Bosses. | |
| III | Unbroken Aim | stat | | | sniper.head.marksmans_eye.4a | | | sniper.head.marksmans_eye.4a | Each shot adds 2 Steady Aim stacks. | |
| III | Momentum | ability | | | sniper.special.momentum | | | sniper.special.momentum | A kill grants Frenzy. | Frenzy reads the same with or without the Frenzy pick (Terms). |

### Terms

New or changed meanings of names the rows use. A name with no row here reads as the game says it today.

| Term | Glyph | Means |
|---|---|---|
| Frenzy | sniper.head.focused_optics.2 | A buff on the tower: attack speed +35% for its next 3 shots. A second Frenzy while one runs starts the 3 shots over; it never adds to them. A crit grants it with the Frenzy pick, a kill with Momentum. |

### Look

A watchtower with a long rifle. Keeps its triangle and its green, softened (`FEATURE-next-gen-tower-looks.md`).

| Tier | Foundation |
|---|---|
| 0 | a triangle, as today, more opaque |
| I | two triangles, one pointing up and one down |
| II | a hexagon |
| III | a hexagon made of triangles |

- **The foundation is a watchtower seen from above**, its tiers growing as the table says.
- **Head**: a long rifle barrel, recognisably a sniper's (a scope is welcome). It recoils when it fires and settles
  back as it reloads. Steady Aim shows on the head: the scope's glint brightens with each stack.
- **Shot**: the tracer flies, over about 3 frames from the muzzle to the enemy, fast but visible as travel. It is
  slimmer than today's line, whiter at its head and greener at its tail, and fades well inside the reload. A crit's
  bullet stays white. Its damage lands when the tracer arrives (`FEATURE-next-gen-shot-flight.md`, 1).
- **The Fourth Shot** is a primed shot and has a head cue; primed shots combine on the head, each readable beside the
  others.

Looks the request asked for that return in Tiers II and III:

- The Fourth Shot has its own shot; a Silver Round has its own line and a loaded head (a silver tip, a glint), and a
  Hollow Point gets its own the same way.
- Frenzy has a "firing faster" look (the barrel shakes, red sparks).
- An execution draws its own moment on the enemy it kills (a white shatter).
- Ricochet's bullet hops to the next enemy on an arc.
- The head changes while it aims at an Elite or a Boss (Headhunter).

Looks kept in case their node returns: Spotter's light red aiming laser to a target past 1x range; Charged Shot's head
cue; a very faint ring at 2/3 of the range (today's Tradecraft II).

## Interconnections

- `FEATURE-next-gen-upgrade-tiers.md` (its node rules), `FEATURE-next-gen-tower-looks.md`,
  `FEATURE-next-gen-shot-flight.md`.
- `TowerTerm.STEADY_AIM`'s stack cap and its Attune bonuses become the passive above.
- `TowerTerm.DEADEYE_ROUND` is renamed Fourth Shot. Crit Streak, Patience, Haste and Charged Shot leave the Sniper;
  the ledger says what else still uses them.
- Magic penetration in points is today's `magicPenetrationFlat`.

## Open questions

⭐ marks the preferred option.

1. ✅ **Tier gates.** XP 60, 120 and 200, as every tower (`FEATURE-next-gen.md`, "Answered by the user").
2. **Long Barrel and the foundation.** ✅ Moved to the bundle (`FEATURE-next-gen-tower-looks.md`, question 8). Its
   preference fits here: the tier alone changes the foundation, and Long Barrel lengthens the rifle a little.
3. ✅ **Two Frenzies.** A second one refreshes it to 3 shots: one buff, one number on the head, and a crit streak
   can't bank shots (the user, 2026-11-13; Terms).
4. ✅ **Executioner on crits only.** It keeps the crit, which makes it a crit-size payoff, the tree's second road
   (the user, 2026-11-13).
5. ✅ **Ricochet's crit.** One hop that can't crit: one trigger, one result, and it can't chain into itself (final
   review, by "one node, one change").
6. ✅ **Insulation and the Silver Round.** Left out: a third change in one node, and a name the Sniper doesn't have
   (final review, by the same rule).
7. ✅ **The Steady Aim pick's balance.** Deep Breath keeps +3 for the balance pass: the cluster is not marked
   `rebalance`, and Keen Eye wins beside Frenzy (final review, by "Balance waits").
8. ✅ **The On crit pick** reads `rebalance` on paper (Sundering Crits $1 against $15). It is kept as drafted and
   retuned later by its parameters (the user, 2026-11-14).
