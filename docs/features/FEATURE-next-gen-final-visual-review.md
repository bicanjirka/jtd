# Feature Request: The final visual review, once the next-gen towers are in code

## Summary

The towers' looks were built ahead of the towers: every foundation, pose, cue, identity and icon is in the game and
on the art sheets, and each tower's rework only sends its state to them. Once all seven towers are reworked, one
review has to check that they did: that every tower in play looks as its sheet shows, that nothing was drawn a
second way, and that the board is alive without being busy.

## Current state (what exists today)

- `docs/features/next-gen-art/` holds the sheets, rendered from the game's own tables (`README.md` there says how).
  A look that changed since shows as a changed PNG.
- `FEATURE-next-gen-tower-looks.md`, "Building a tower's look", is the recipe each rework follows, with the
  mistakes the review of 2026-11-14 found. Each tower's doc has an "In code" list of the cues it must send.
- Sent from play today: reload and recoil (Sniper, Burst, Mortar), Steady Aim stacks, the Scorcher's stream, the
  Obelisk's knell, the Burst's fork. Every other cue and every identity look is drawn and waits for its tower.
- Known open at the time of writing:

  | Open | Whose |
  |---|---|
  | Soulfire, Ruby, Shade and the three runes have no tower-level query, so no tower wears them | Scorcher, Aerie, Obelisk |
  | Frenzy's jitter is a still offset | Sniper |
  | A flying shell is a plain ball, not its kind's drawing | Mortar |
  | The Beacon's rings, the Obelisk's ripples and the Aerie's orbit still run on an empty board | Beacon, Obelisk, Aerie |
  | The stream, the nest's missiles, shots in flight, enemy marks and field patches are lab drawings | each tower, shot flight |
  | Toolbar icons are 32 px images, soft on a HiDPI screen | the toolbar |

- Painting is outside `td.PerformanceHarness`: the harness measures the tick and the frame build. The sheets'
  `bench` times the painter (200 Tier III towers: 1.5 ms still, 2.9 ms with idle lights, 2026-11-14).

## What this feature adds

A review, and the fixes it finds. In this order:

1. **Render every sheet again** and read the difference to the committed PNGs. Every changed sheet must be one a
   tower's rework explains; an unexplained change is a regression in a shared builder.
2. **Render the game beside the board sheet**: `PreviewBoard` with all seven towers at Tiers 0 to III and each
   identity, at 0.5x, 1x and 2.5x, beside `board.png`. They must match.
3. **Walk each tower through its Look section** with a checklist:

   | Check | How |
   |---|---|
   | Every cue on the heads sheet shows in play | a `BoardRendererTest` per cue: the state in, the cue in the pose out |
   | Every identity look is worn by the tower that took it | the same, on the look |
   | No cue is painted outside the look's builder | no branch on a tower type or a cue in `TowerSprites` or `Java2DFrameRenderer` |
   | No new draw record, `RenderFrame` list or `TowerSpriteDraw` field arrived for one tower | read `td.ui.render`'s history |
   | `Row.aiming` is honest | `TowerFoundationsTest` holds it; check no look was moved to dodge the test |
   | A shot leaves from the posed muzzle | fire with a Long Barrel and at full recoil; no gap, no overlap |
   | One drawing per thing | a shell in the bore, in flight and in the pane; a missile on the ring and in flight |

4. **Clocks.** A moment (shot, reload, rise, placement, a cue gained) follows the tick clock: pause the game and it
   holds. Idle life follows the wall clock and is small. Nothing else moves on an empty board; list what does.
5. **Paint order.** A barrel turned south passes behind the wall in front of it; towers cover only what is north of
   them; enemy bars and marks show over a tower; what lies on the ground stays under everything.
6. **Cost.** `td.PerformanceHarness` on a board of Tier III towers, both loadouts. The sheets' `bench`. A live JFR
   of the painter in a long late-wave fight. Count the distinct poses a real fight makes: `TowerFoundations` and
   `TowerSprites` drop everything they keep when they pass their limits (`KEPT_POSES`, `PIXEL_BUDGET`), and a fight
   that passes them every few seconds redraws its towers again and again.
7. **Leftovers to delete**: lab drawings of what the game now paints (`nest`, `missile`, the
   shots and marks in `TowerArtSheets3` and `4`), a `TowerCue` or `LoadedShell` nothing sends, a `TowerLook` no tower
   wears, the old always-on loops, and any helper kept for the flat towers.
8. **The whole picture.** A late wave on the fullest level at 1x: can a player still find the enemies, read a
   tower's tier at a glance, and tell which towers are firing? The calm board was amended to let idle towers live;
   check that the amendment did not bring the noise back.

Out of scope: new looks, new cues, a change to the art direction.

## Interconnections

- `FEATURE-next-gen-tower-looks.md` owns the art direction and the recipe; a flaw in the recipe found here is fixed
  there.
- Each tower's doc owns its look: a tower that fails a check goes back to its own doc.
- `FEATURE-next-gen-shot-flight.md`: shots, hops and casts are reviewed with it, on the shots sheet.
- `FEATURE-balance-after-next-gen-towers.md` runs at the same point in the bundle and plays the same towers; a
  fight set up for one serves the other.
- `src/main/java/td/ui/CLAUDE.md` holds the rules as constraints; the review reads the code against it.

## Constraints and open risks

- The sheets prove a look can be drawn, not that play reaches it. A cue no tower sends is invisible on a sheet
  diff, which is why check 3 asks for a test per cue.
- A pose is snapped to a few steps so the kept pictures stay few. A tower that sends a fast-changing value in its
  pose (a count that moves every tick, a list in an unstable order) fills the cache without any visible fault.
- The painter's cost is seen only live or in the `bench`; the harness cannot fail on it.
- Several towers standing close with idle lights and scanning heads may read as noise at the smallest zoom, where
  a light is a pixel or two.

## Decisions made

- The review runs once, after the last tower's rework, with the same eyes as the review of 2026-11-14.
- The sheets stay rendered from the game; a look is never fixed on a sheet alone.
- Idle life stays: one small light a tower, a slow scan on a head with no target.

## Open questions

- Should idle lights dim or stop at the smallest zoom?
- Does the Aerie's nest count as idle life, or must it hold still on an empty board as its doc says?
- Should the toolbar icons be painted as vectors at the screen's scale instead of 32 px images?
- Is a tier unlock's flash too strong when several towers unlock in one tick?
