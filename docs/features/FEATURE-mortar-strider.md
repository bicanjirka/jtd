# Feature Request: The Strider Mortar and the steep lob

Status: **designed, ready to implement** (the user approved the art, 2026-11-18). Aesthetic only: no mechanic,
number or balance change.

## Summary

The Mortar becomes a fat, ribbed mortar standing on four jointed legs, and it acts out every shot. After a shell drops
into its mouth it pumps once, deeply, and holds there, cocked. On the shot it spits the shell straight up in a burst
of smoke and a smoke ring, slams down, bounces and settles. Every lobbed shot (the Mortar's shell and the Burst's
stone) flies a new, steep arc that starts at its muzzle: a short shot climbs high and drops almost straight down.
The fuller body also fills the Tier 0 cell, which looked empty.

Inspiration: a squat segmented mortar on four splayed spider legs (a reference picture the user showed: fat ribbed
barrel, wide mouth, knees bent up and out, pointed feet).

## Art (approved)

All in `docs/features/mortar-art/`:

| File | Shows |
|---|---|
| `mortar-looks.png` | today vs the Strider, Tiers 0-3 at 220 px a cell, then both rows at board size (36 px) |
| `mortar-closeup.png` | the Strider at Tiers 0, 1 and 3, at 420 px a cell |
| `mortar-frames.png` | the firing cycle frame by frame, labelled with ticks from the load and from the shot |
| `mortar-fire.gif` | the whole cycle at Tier I, one frame per tick (50 ms) with a 69-tick reload; not committed, regenerate it |
| `mortar-arcs.png` | three arc options from one tower to seven targets; **B is the one chosen** |
| `mortar-arcs.gif` | the same arcs with shells flying at their real speed; not committed |

The lab that drew them is in `docs/features/mortar-art/lab/`:

- `strider-mortar-builder.java.txt`: the new body, written as extra members of `td.ui.TowerFoundations`, in the game's
  own part vocabulary (`Build.solid/flat/glow/line`, `LIFT`, `plinth`, `shell`, `rackedShell`, `arcOver`,
  `mortarPiece`). **It is the source of truth for every shape, colour and number below.** Its `Lab` class holds the
  animated values as statics, which stand in for the pose (see "Where it goes"). Ignore `Lab.bulge` and
  `Lab.pump = false`: they are the rejected "gulp" variant.
- `MortarLab.java.txt`: the renderer. `state(t, cd)` with `variant == 0` is the approved timeline, tick by tick.
  `lift(1, d, s, launch)` is the approved arc.
- `run.sh` / `assemble.py`: render everything again (`bash docs/features/mortar-art/lab/run.sh` from the repo root,
  after `mvn -q compile`). The lab's `TowerFoundations` is the one at commit `8b3a32b6` with the builder appended. It
  shadows the game's class on the classpath, so it only compiles against classes from before the builder moves in.

## Current state (what exists today)

- `TowerFoundations.siegeMortar` (a `Row.posed` look, no head): a bedplate, four thin splayed legs with braces, a
  6-slice barrel at lean `MORTAR_LEAN` 0.30, a rack of every unlocked kind in front, a plank deck from Tier I, an iron
  edge from Tier II, a back rail at Tier III. `mortarMouth`, `mortarPiece`, `mortarPlace`, `mortarLoadedCount` and
  `mortarShowing` are shared with `TowerMomentArt.shellCarry`, which lifts a shell from the rack into the muzzle.
- Pose: `TowerSpriteFrameBuilder.poseOf(MortarTower)` = `poseOfReload(coolDownFraction)` plus `load`, `stock`,
  `VOLLEY`, `LONG_BARREL`. Recoil eases out over the whole reload, and the flash shows for the first 12% of it.
  `TowerSprites` snaps poses to `POSE_STEPS` (4).
- Volley: the shells of a Barrage or Carpet Bombing leave one by one, `VolleySpec.gapTicks()` apart
  (`MortarTower.volleyLeft`).
- The lob: `ProjectileFrameBuilder.lobPeak(ticks, scale)` = `min(LOB_TOP_CELLS 2, LOB_PULL_CELLS 0.03 × ticks² / 8)`
  cells, with `lobLift(peak, flown) = peak × 4s(1-s)`. A Mortar shell (8 px a tick, 32 px cells) fired 1.5 cells out
  peaks at about 0.14 cells, and it starts on the ground at the tower's centre, not at the muzzle. The Burst's stone
  (`TowerEffectFrameBuilder.stone`) uses the same `lobPeak` and `lobLift`, and is already released from the
  thrower's arm (`released`).
- Rules this touches: `src/main/java/td/ui/CLAUDE.md` ("a lobbed shot's position is its point on the ground; how high
  it is drawn comes from the one arc in `ProjectileFrameBuilder`, never from the shot or its tower") and
  `FEATURE-next-gen-shot-flight.md` ("one shared formula of its speed and the distance, so every lobbed shot arcs
  alike"). **Both stay true in spirit**: there is still one arc for every lob. Only its formula changes, and it now
  starts at a launch height.

## What this feature adds

### 1. The body (all tiers)

Centre `FAT_X 0, FAT_Y -0.05`. The barrel leans north by `FAT_LEAN 0.22` (y moves `-0.22` per cell of height), which
is steeper than today and still shows its ribbed side from the board's camera.

- **Hip drum:** an octagon of radius 0.155 (dark iron `0x363b47`), from floor + 0.075 to floor + 0.145, with a bronze
  ring on top. It stands on the legs, with a gap under it.
- **Barrel:** 5 rings (6 with `LONG_BARREL`), each a dark groove (0.012 high, radius −0.018) and then a ring 0.088
  high: 78% body, then a 22% chamfer in highlight colour at radius −0.012. Radii 0.150, 0.160, 0.166, 0.168, 0.168
  (0.168). Ring 1 and the top ring are bronze (`mix(MORTAR colour, white, 0.12)`), the rest iron `0x5d6372`.
- **Muzzle:** a bronze collar (0.03 high, radius 0.182), then a bronze lip ring (0.035 high, outer 0.182, bore 0.118),
  split at the bore's centre line into a far half (under the loaded shell) and a near half (over it). Inside: a dark
  bore with a dark inner rim, and bronze rivets on the near half. The loaded shell shows its top in the bore at
  `MORTAR_SHELL × 1.15`.
- **Legs**, at plan angles −130, −50, 30, 150 (back legs drawn before the drum, front legs after the barrel): from the
  drum's side (radius 0.15, 0.05 above the drum's top) up and out to a knee (radius 0.27, 0.10 higher), then down to a
  pointed triangular foot at radius `FAT_FOOT 0.335`. Leg width 0.062 + 0.004 × tier, shins 80% of that, each with a
  highlight line. Knees are discs with a bronze glint. **A foot stands on whatever is under it** (`floorAt`): the deck,
  or on higher tiers the plinth step it reaches, so the legs grip down over the plinth's edge rather than shrinking
  inward.
- **Pump (east side):** a bronze cylinder (radius 0.032) on a bracket, from ring 1 to ring 3, with a rod and a T-handle
  on top. The handle stands 0.10 above the cylinder when up and sits on it when down (`piston` 0..1).
- **Gauge (front face of ring 1):** a bronze-rimmed cream dial (radius 0.034). Its needle sweeps from −210° to +30°
  with the pressure (`gauge` 0..1) and turns red above 0.85.
- **Rack:** as today (one shell of every unlocked kind, the stock, never empties), moved to `y 0.2`, standing on
  `floorAt(tier, 0.2)`.
- **Tier 0 only:** packed dirt under it (two dark ellipses, 0.43 × 0.40 and 0.34 × 0.31, and four darker scuffs at
  0.30 out) and a powder keg on the west side at (−0.36, −0.03): wood, two iron hoops, a lid with a bung. From Tier I
  the plank deck, the Tier II iron edge and the Tier III back rail are as today.
- Heights of the muzzle top at rest: floor + 0.71, so 0.71, 0.81, 0.875 and 0.945 cells for Tiers 0-3 (+0.10 with
  `LONG_BARREL`). It reaches above its cell, like today's.
- **Removed:** today's bedplate, splayed legs, braces, `MORTAR_LEAN`, `MORTAR_SLICE(S)`, `MORTAR_BORE`,
  `MORTAR_MUZZLE` and `MORTAR_RECOIL`, along with the recoil "thrown down its axis".

### 2. The firing cycle

Two values deform the body: `stretch` (the barrel's ring height × stretch, its radius × 1/√stretch, except the bottom
ring) and `crouch` (the drum drops 0.03 × crouch, knees spread 0.035 × crouch out and drop 0.07 × crouch, hips drop
0.02 × crouch). At rest both are neutral: stretch 1, crouch 0.

| When (Mortar reload of 69 ticks in the art) | stretch | crouch | Other |
|---|---|---|---|
| Shot + 0: **spit** | 1.24 | −0.7 | muzzle flash (warm glow 0.30 and white 0.14 above the mouth); smoke ring born; 5 smoke puffs burst sideways round the mouth; the shell leaves going up |
| Shot + 1: **slam** | 0.80 | 1.0 | dust kicks up at every foot |
| Shot + 2 / + 3 / + 4: **bounce** | 1.08 / 0.96 / 1.0 | 0.25 / 0.5 / 0.15 | dust spreads and fades over shot + 1..4 |
| Shot + 0..8: smoke ring | | | rises 0.55 × √age above the mouth, radius 0.13 → 0.26, fading; the side puffs fade within its first 45% |
| Shot + 5 until the load | 1 | 0 | rest; today's bore smoke and today's carry from the rack to the mouth (`shellCarry`), retargeted to the new rack and mouth |
| Load (the shell drops in, today at 3/4 of the reload) + 0..8: **one deep pump** | 1 → 0.88 | 0 → 0.5 | eased (smoothstep over 8 ticks): the handle sinks all the way, the gauge climbs to full; a steam puff from the pump's foot over load + 7..10 |
| After the pump until the shot: **cocked** | 0.88 | 0.5 | handle down, needle in the red; while it waits for a target, a short hiss (a steam puff fading over 4 ticks) about every 1.5 s |

- **One pump, not several** (the user, 2026-11-18): three strokes were rejected, and so was a "gulp" (a bulge running
  down the barrel).
- **Volley:** each shell that leaves gets its own spit, slam and bounce (they leave `gapTicks` apart). The pump
  happens once, after the volley's last shell goes in.
- A reload too short for the pump to finish (very fast fire) just reaches the shot part-way through the pump. That is
  fine: the spit starts from whatever crouch it had.

### 3. The steep lob, for every lobbed shot

Flight time, ground path and landing are unchanged; only the height it is drawn at changes.

```
lift(s) = launch × (1 − s) + P × 4s(1 − s)        s = share of the flight flown, 0..1
P       = min(2.6, 1.1 + 0.32 × d) cells           d = the flight's length on the ground, in cells
```

- **Launch:** where the shot leaves, as a height above its ground point on screen. The Burst's stone already starts
  at its arm (`released` in `TowerEffectFrameBuilder.stone`) and keeps doing so. The Mortar's shell starts at its
  muzzle: **0.55 cells** above the tower's centre on screen. The real muzzle is 0.50 to 0.60 by tier, and the flash
  and smoke hide the difference, so the shell keeps no link to its tower.
- The shadow still shrinks with height (`SHADOW_SHRINK`), measured against `P`.
- A shot fired due north still reads as a line going up and coming down onto the target: that is the camera, and it
  is accepted.

## Where it goes

| What | Lab | Game target |
|---|---|---|
| Body | `fatMortar`, `strideLeg`, `keg`, `floorAt`, `fatY`, `FAT_*` | replaces `siegeMortar`, `leg` and the `MORTAR_*` body constants in `TowerFoundations`; still `Row.posed`, no head |
| Rack and mouth | rack at `y 0.2` on `floorAt`; `Lab.mouth` | `mortarPiece` and `mortarMouth` follow the new geometry, so `TowerMomentArt.shellCarry` carries into the new mouth |
| stretch, crouch, handle, gauge | `Lab.stretch/crouch/piston/gauge` | read from the pose: the body's kept pictures (few states: rest, 4 pump steps, cocked, 5 shot frames) |
| Flash, smoke ring and puffs, dust, steam/hiss | `Lab.flash/ring/dust/steam` | live tower effects on the tick clock, like `SteamDraw` and `CasingDraw`, so they don't multiply the kept pictures |
| Shot frames | `state(t)` for `t` 0..8 | ticks since the last shell left, from the tower, like `SniperTower.ticksSinceFired(gameTime, window)` |
| Pump and cocked | `state(t)` from the load | ticks since the shell dropped in, or the reload's progress past the load point turned into ticks with the cooldown |
| Steep lob | `MortarLab.lift(1, …)` | `ProjectileFrameBuilder.lobPeak` / `lobLift` / `lobShadowScale` with the formula above; the shell's launch constant there; the stone's call site passes its distance instead of its flight ticks |

## Constraints and open risks

- Pose snapping: the shot frames run on ticks, not on the reload's quarters, and today's recoil-and-flash from
  `poseOfReload` no longer fits the Mortar. Keep the kept pictures few, and run `td.PerformanceHarness` (both
  loadouts) after.
- Threading and the render pipeline are unchanged: the new values are read on the game loop into the frame, and
  `td.ui.render` stays AWT-free.
- The Burst's stone flies higher than now; that is wanted ("default for every lob", the user, 2026-11-18).
- Tests: the arc and the pose reads are mechanics (test them on inputs and formulas, never the art's numbers as
  content); a `BoardRendererTest` or `TowerPoseTest` per new pose read, as the visual polish did.

## Decisions made (the user, 2026-11-18)

- The Strider body as drawn, including the extras: side pump, gauge, Tier 0 keg and dirt.
- The muzzle lip is bronze, so the Mortar keeps its colour at board size.
- **Pump A: one deep pump** after the load, then cocked until the shot. Three pumps and the gulp were both rejected.
- Arc **B** (steep, from the muzzle) is the arc of **every** lobbed shot. C (towering) was rejected because it flies
  off the board's top edge.
- A volley spits once per shell.
- Accepted: a northward shot reads as a vertical line.

## Open questions

None.

## Instructions for the implementing session

Implement in three phases, one commit each, `mvn verify` before each, on `main`, staging only your own files by name
(other sessions edit this checkout).

1. **Body:** port `fatMortar` and its helpers into `TowerFoundations` in place of `siegeMortar` (rename to the game's
   naming: e.g. `striderMortar`, `STRIDER_*`). Keep the rack and mouth helpers working for `shellCarry`. Turn the
   `Lab` statics into pose reads (stretch, crouch, handle, gauge), with the shot frames and the pump driven as in
   "Where it goes". Render the game's art sheets (`next-gen-art/README.md`) and compare them with `mortar-looks.png` /
   `mortar-closeup.png`.
2. **Moments:** flash, smoke ring and puffs, foot dust, pump steam and the waiting hiss as tick-clock tower effects;
   a volley spits per shell. Compare with `mortar-frames.png` and `mortar-fire.gif`.
3. **Lob:** the steep arc for every lob, with the Mortar's launch of 0.55 and the stone's launch from its arm. Compare
   with `mortar-arcs.png`.

Then one live `run-jtd` pass:

- A Mortar fires: the pump follows the load, the mortar holds cocked while no enemy is in reach, and spit, slam and
  bounce follow every shot.
- A Barrage spits three times.
- Shells fired at 1.5 cells climb high and drop steeply.
- A Burst stone arcs from its arm.
- Pause holds every moment.
- `td.PerformanceHarness` stays in budget.

Update the docs this makes false, in the same commits:

- `src/main/java/td/ui/CLAUDE.md` (the arc's launch height).
- `FEATURE-next-gen-shot-flight.md`'s "formula of its speed and the distance".
- `FEATURE-tower-visual-polish.md`'s "The Mortar" section (point it here).
- `next-gen-art/README.md`'s Mortar row.
- `README.md` has no Mortar look text to change.

When done, mark this doc implemented.
