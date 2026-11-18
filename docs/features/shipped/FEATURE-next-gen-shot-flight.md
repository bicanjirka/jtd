# Feature Request: Next-gen Shot Flight

**Status:** implemented 2026-11-15 (drafted 2026-11-10, reviewed with the whole bundle 2026-11-13; no open question
left). Part of `FEATURE-next-gen.md`; the shot timing of the visual request in its appendix. The Sniper's bullet (lands
with the stats it was fired with, when its tracer arrives) and the Mortar's shell (lands with its fired stats, drawn on
the shared arc over a shadow) came first, on 2026-11-14; the Burst's stone, its Arc's hops and its flying curses,
sparks and souls came with the Burst, and each shell drawn by its kind in flight with the Mortar. The shots sheet
still draws the lab's copies; replacing them is the final visual review's.

## Summary

Shots get a sense of flight. A shot may take time to reach its target where it lands instantly today, lobbed shots fly
on an arc, and chains travel hop by hop. Some of this changes when damage lands, not only how it looks.

## Current state (what exists today)

- **Sniper:** instant. The shot lands the tick it fires. Its trace is a line to where the shot ended, `3 × cooldown
  left` wide, so it thins over the whole reload. A crit's trace is white.
- **Burst:** instant. A beam to the target thinning over the cooldown, then a detonation that spreads as a ring for 8
  ticks. A Stormcaller's arcs are zigzags drawn all at once with the blast, and all their damage lands that tick.
- **Mortar shells** (`CannonballProjectile`) fly straight on screen, a dot with an optional streak. **Hive missiles**
  turn and leave smoke. The Radar's sweep leaves with the Radar; the Obelisk takes its geometry.
- `AbstractTower.beginShot()` spends next-shot charges and returns the stats the attack should use; most attackers
  read the live stats instead (`TODO.md`, "An attack can read the stats its shot already spent").

## What this feature adds

- A shot may take flight time where it lands instantly today, and its trace may fade faster.
- **Three kinds of shot**, each flying its own way:
  - **Bullet**: a tracer that flies along a straight line, fast but visible as travel, never a laser drawn all at
    once (the Sniper).
  - **Missile**: an aimed shot that flies slowly, may turn after its target and may leave a trail (the Aerie's
    missile, a hex in flight, a soul or a spark flying to its tower).
  - **Projectile**: a lobbed shot with a look of its own per tower (the Burst's stone, the Mortar's shell). It flies
    to a spot, not after an enemy.
- **A projectile's flight is a straight line on the ground.** Its ground point moves at one speed from the muzzle to
  the spot it was aimed at, never turning, speeding up or slowing down. The arc is only a picture: the height above
  the ground point the projectile is drawn at comes from one shared formula of the distance on the ground, from the
  height it leaves at (`FEATURE-mortar-strider.md`: the steep lob), so every lobbed shot arcs alike. A faint dot on the board marks its ground point, its shadow. The Aerie's missiles get a flight of their own
  in `FEATURE-next-gen-seeker-tower.md` (they leave an orbit, turn smoothly and speed up); the Scorcher's stream
  lands as it pours, and the Obelisk's Sundial sweeps.
- **Chains travel hop by hop**, with a visible pause between hops; each hop deals its damage when it arrives.
- **A cast that flies** (a curse, an ability's projectile) can't be dodged and takes effect when it lands.
- Each tower's own timing is in its doc and goes through `BalanceReport --compare` with the bundle's balance pass.

## Notes from the visual review (2026-11-14)

What was delivered then was read and holds; the Burst's stone, the chain hops and the flying casts were built after
it, on these notes:

- **Reuse the bullet's shape.** The Sniper keeps each shot as a value with the stats it was fired with and the ticks
  it flies (`SniperTower.Bullet`, `bulletsInFlight()`), lands it when its tick comes, and leaves a short trace
  (`lastShot()`). A hop of a chain is the same thing: fired stats, a start tick, a landing tick. The frame builder
  turns the ticks into 0..1; nothing stores an animation.
- **Reuse the arc.** Every lob takes its height from the one formula in `ProjectileFrameBuilder`; a shot's position
  is its point on the ground. The Burst's stone is a lobbed shot with a shadow, as the Mortar's shell is.
- **Reuse the tracer.** `TracerDraw` is a streak between two points with a head and a tail colour; a bolt or a soul
  that flies straight is the same record with other colours.
- **Leave from the muzzle.** A shot starts at `TowerSprites.muzzle(look, tier, pose, ...)` with the tower's real
  pose. The Sniper's tracer started at the idle muzzle and left a gap to a recoiled barrel.
- **A flying shell is drawn by its kind.** `CannonballDraw` carries its `LoadedShell`, and
  `TowerSprites.paintShell` draws it in flight as in the bore and the pane.
- **The shots sheet is still lab art.** Each shot that reaches the game is then drawn on the sheet by the game, and
  its lab drawing deleted (`next-gen-art/README.md`).

## Interconnections

- `FEATURE-next-gen-tower-looks.md`: what a shot looks like.
- `FEATURE-next-gen-sniper-tower.md` (the tracer), `FEATURE-next-gen-splash-tower.md` (the thrown stone, Arc hops,
  curses in flight).
- `FEATURE-next-gen-tower-abilities.md`: casts that fly.

## Constraints and open risks

- **A delayed hit carries the stats it was fired with.** A hit that lands when its tracer arrives, a lobbed stone or a
  chain hop in flight must not read the tower's live stats on landing, or a primed shot spent at firing is lost. The
  `TODO.md` entry above is done with the first timing change.
- **Timing touches balance and tests**: a shot that lands later can miss a kill, lose a target to a dodge or overkill.
  Tests that assert damage the tick a shot fires will need to tick on; keep that churn small and mechanical.
- **The Burst never missed**: today it is instant and cannot miss. Its lobbed stone is unguided (open question 2),
  a small cousin of the Mortar's shell, so it can land behind a fast enemy. The paper weighed a Burst that always
  hits, and the bare Burst is the bundle's floor: the balance pass reads the floor again from the built tower.
- Flight time and arcs are drawn on the tick clock, so they pause and speed with the game.

## Decisions made

- Arcs for every projectile but missiles.
- A projectile's arc is drawn, not simulated: its ground point flies straight at one speed, and one formula for
  every tower gives its height.
- Chain hops land when they arrive (the notes: "visually and in practice").
- A flying cast can't be dodged and counts when it lands.
- The Sniper's hit lands when its tracer arrives, 2-3 ticks after it fires (the user, 2026-11-13).
- The Burst's stone is unguided: it lands where its target stood when it was thrown (the user, 2026-11-13).

## Open questions

⭐ marks the preferred option.

1. ✅ **Sniper: when does damage land?** When the tracer reaches the enemy (2-3 ticks), so what you see is what
   happened, as the bundle already decided for chain hops and flying casts (the user, 2026-11-13). An enemy that
   dies or dodges meanwhile is the delayed hit's to resolve, with the stats the shot was fired with.
2. ✅ **Burst: how does the lobbed stone hit?** Unguided: it lands where the target stood, like a small Mortar
   shell (the user, 2026-11-13, against the earlier preference for a guided stone). How far it may miss is the
   Burst's question 14.
