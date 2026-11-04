# Feature Request: Enemy and effect visuals

> **Status: implemented.** Mock: <https://claude.ai/artifact/Ad29S1ti5eq1swojPPQ5tC> (animated, a
> browser drawing at the game's real sizes, to judge the idea, not final art).

## Summary

A player should see what is happening to an enemy by looking at the enemy, not by decoding coloured diamonds. A burning
enemy burns, a frozen one is inside ice, a shielded one is inside a bubble. Everything that exists only to label an enemy
goes unless it earns its place, and the board's floor steps back so that all of this reads.

The goal is visual clarity and less clutter. Where a new visual and clarity pull apart, clarity wins.

## Current state (what exists today)

- **Every effect is a diamond.** `EnemyFrameBuilder.markers` draws one filled diamond per effect above the body, up to
  `MAX_VISIBLE_MARKERS`, then one per category with a `+N` count. All 50 `EffectKind`s use the same shape and differ
  only by colour. `markerPaletteFor` also feeds `TermLooks`, so the same colours are the Info pane icons' colours.
- **Burn, poison, bleed, heal, chill and daze change nothing on the body.** Only the diamond shows them.
- **Shield** is a thin ring at 1.3 times the body's scale, so a square's corners poke through it. Its alpha follows
  `shieldFraction`. Shields stack per source.
- **Freeze** is one star-like crystal at 1.35 times the scale, over the body rather than around it.
- **Trait markers:** a row of hollow diamonds under the body, one per trait (`Trait.marker()`, `TraitMarker`,
  `TraitMarkerDraw`).
- **Rank badge** scales with the body: 0.6 of its scale, so the Warden's skull is a large white disc. A grunt has none.
- **Health is opacity:** `paintEnemyBody` fills the body at its health fraction and strokes it solid, so a hurt enemy
  loses its colour and shows the floor through. There is no bar on the board. `TODO.md` lists a board health bar as a
  feature idea, deferred from the restoration rework.
- **Other marks on an enemy:** a faint support aura ring and a stronger disruption ring, always on; an expanding ring on
  every effect gained, lost or cast (`EffectPulseDraw`); a body flicker while a tower's field holds an effect on it; the
  hex rune row; `miss` text; the crit spark.
- **Bodies:** 44 archetypes with distinct silhouettes (contact sheet on the mock page). Five bodies are under 3:1 against
  the lightest floor (`#1A1206`), the worst case rather than each body's own level: Warden and Warden egg 1.85, Minotaur
  2.12, Monolith 2.33, Thunder 2.36. The darker floor raises these by only about 6%. The Warden still reads through its
  size and gold core. The oldest bodies (Circle, Square, Triangle, Golem, Mite) are plain shapes, most in pure colours,
  beside newer bodies with detail and muted colours.
- **Sizes:** the board's scale is 32, and at the default window's zoom of about 1.22 a plain enemy's radius is about 6 px,
  a mite's about 4 and the Warden's about 26. A body's size is visual only: drawing, click reach and the preview read it,
  and no hit or rule does.
- **Floors:** each level has a floor and motif colour (`BoardGround`, `GroundPainter`). The floors are near-black, but
  motifs reach `#33240E` and read as a pattern over the whole board.
- All board art is vector, and every colour and shape for board content lives in `Java2DFrameRenderer`.

## What this feature adds

### The board

- Darker floors and quieter motifs on all six levels, each keeping its own hue. The mock uses floors at about 60% of
  today's brightness and motifs at half their distance from the floor; tune by eye.
- The lanes stay readable: nothing here dims a path.

### Removed

- Trait markers, entirely. What an enemy *is* lives in the Info pane.
- Anything that says only that an enemy *has* an ability, except the disruption ring (see Decisions made).
- The `+N` overflow count and the diamond row.
- The field-effect body flicker: the field already shows on the ground.

### Effects drawn on the enemy

Each has its own motion, so several can sit on one enemy and still be told apart.

| Effect | What it looks like |
|---|---|
| Shield | An egg-shaped outline around the whole body, whatever its shape, with a highlight and no tint: it adds no colour to what's inside and doesn't change as shields drain. The rim flashes when a hit lands on a shield. Plus a shield bar. When the last shield is broken, the egg pops. |
| Heal, Regeneration | Small plus signs rising, only while health is actually coming back, whatever the source. Faster healing emits more, measured against the enemy's maximum health and up to a limit. |
| Burn | Flames licking upward, embers drifting off. A fuller pool emits more, and the fire dies down as it fades. |
| Soulfire | The same fire in its own colour, a kin of burn, following its pool the same way. |
| Poison | Slow bubbles rising and popping, with a sick tint on the body, following its pool the same way. |
| Bleed | Drops falling and splatting just under the body, moving with it. A fixed amount, and only while the enemy moves, since a standing enemy takes no bleed damage. |
| Chill | A frost rim, a cold tint and falling flakes, all growing with the chill level. |
| Freeze | The enemy inside a block of ice, with variety per enemy: a block, a spiked cluster, a shard. |
| Daze | Stars circling the head, the body swaying. |
| Invisibility | As today: the body fades. |

Inspiration is the common language of action and strategy games: fire rises, drops fall, stars circle a stunned head.
A player should need no legend.

Drawn back to front: bleed, body, poison, chill, freeze, burn, soulfire, plus signs, stars, egg, rank badge, hex runes,
dots, bars.
An enemy's variety and its particles' timing stay the same for that enemy's whole life, and differ between enemies.

### The shield bar

A bar for all of an enemy's shields combined, above its health bar, in the shield's gold (`#FFDC78`). It uses the general
bar under the enemy from `FEATURE-enemy-health-bar.md`, and it shows at full health too.

It uses the health bar's scale: full width is the enemy's maximum health, so the two bars compare directly. It shows the
shields' raw points, the number the Info pane shows, stops at full width, and never gets too narrow to see. A shield
limited to one damage type counts in the same bar.

### Marks, only where they earn it

| Group | Effects | On the board |
|---|---|---|
| Drawn on the enemy | Burn, Soulfire, Poison, Bleed, Heal, Shield, Chill, Freeze, Daze, Invisibility | Its picture, no mark |
| Debuff dot | Vulnerable, Sunder, Fracture, Brittle, Crack, Tar, Toll, Condemn, Stasis, Resonance, Saturation, Unravel | One small dot, in the debuff colour |
| Spotted dot | Expose, Mark, Priority, Charge | One small dot, in the spotted colour |
| Buff dot | Sprint, Hasten, Spellproof, Unyielding | One small dot, in the buff colour |
| Nothing | Scorch, Sicken, Reveal, Unveil, Silence, Anchor, Corrode, Drag, Undertow, Dead Zone, Frail, Fallout | Info pane only |
| Left to the tower revamp | Doom, Blight, Contagion, Rime, Ash, Inversion, Sympathy, Reckoning | The rune row, until the revamp replaces it |

### Rank and bodies

- One small rank badge size for every enemy, boss included. Its size already says boss.
- Dark bodies brighten enough to read on every floor they appear on.
- The plain oldest bodies get detail, so they match the newer ones. Keep the shapes: the silhouettes are the strength.
- Every body gets larger, small ones most and bosses barely, so the order of sizes holds. Radii in board pixels at the
  board's scale, written relative to the scale in code; a spawn shape's size multiplier still applies on top. Tune by eye:

| Bodies | Today | New |
|---|---|---|
| Mite | 3.2 | 4.5 |
| Ember, Raindrop | 3.6 | 5 |
| Mirror Image, Sandling | 4 | 5 |
| Circle, Salamander | 5.3 | 6.5 |
| Larva | 5.8 | 6.5 |
| Yeti, Magma, Golem, Eclipse | 6.4 | 8 |
| Hive Guard, Raincloud, Glass Sentinel | 7.1 | 8.5 |
| Cocoon | 9.1 | 10 |
| Warden Egg | 10.7 | 11 |
| Molten Core | 12.3 | 13 |
| Colossus, Talos, Lightning | 14.5 | 15 |
| Monolith | 16 | 16.5 |
| Keeper | 16.8 | 17 |
| Thunder | 17.8 | 18 |
| Minotaur | 18.8 | 19 |
| Warden | 21.3 | 22 |

Bodies that grow with rank, Grunt to Boss:

| Bodies | Today | New |
|---|---|---|
| Square, Triangle, Ghost, Courier, Skirmisher, Jammer, Reaver, Mirage, Herald, Drone, Nurse, Mender, Shieldbearer | 4.6, 5.3, 6.4, 8, 10.7 | 6, 6.6, 7.4, 8.6, 11 |
| Hornet, Timekeeper | 4.9, 5.8, 7.1, 9.1, 12.8 | 6.2, 6.8, 7.6, 9.4, 13 |
| Juggernaut, Bronze Bull, Gale | 5.3, 6.4, 8, 10.7, 16 | 7, 7.6, 8.6, 11, 16 |

### Enemy looks as tables

Enemy looks move out of the painter's switches into tables per role (ADR 0018), including everything new here. It is the
first step of this work, before `FEATURE-enemy-health-bar.md`, and it changes nothing on screen.

### Out of scope

- The HUD, level cards and interface colours: `FEATURE-hud-identity.md`.
- Tower art, shots and tower marks: `FEATURE-tower-visual-revamp.md`.
- Any rule, number or balance change. Nothing here changes what an effect does.

## Interconnections

- **`FEATURE-tower-visual-revamp.md`** redraws a hexed enemy and wants a calmer board. The two agree; hexes stay out of
  this request so that they are drawn once.
- **ADR 0018** and `TODO.md` ("A board look is a switch in the painter"): enemy looks become tables with this request,
  ahead of the towers.
- **`FEATURE-enemy-health-bar.md`** ships after the tables and the new body sizes, and before everything else here: this
  request needs its solid bodies and its general bar.
- **`FEATURE-hud-identity.md`** comes later and reuses the darker board in its mocks.

## Constraints and open risks

- **Swarms are the worst case.** A wave can hold a dozen or more mites and drones, all burning. Emitting from each must
  not turn into noise, or undo the clarity this is for. Check real bunching live: the mock's enemies never overlap.
- **Tints can mislead.** An orange tint on a cyan body came out grey-green in the mock, like poison. Check the poison and
  chill tints against the real body colours.
- **Judge every look at real size.** The mock's bodies were first drawn up to twice too large, and many pictures look
  fine enlarged. Even at its new size a mite's radius is about 5.5 px on screen, and the view can zoom down to 0.5.
- **Larger bodies overlap more** in a wave, whose spacing doesn't change. Check formations and bunching live.
- **Big bodies:** the egg and the ice grow with the body, so a shielded or frozen boss could cover a lot of board. Check
  them on the largest bodies.
- **Slow regeneration lands in whole units, some ticks apart.** Its plus signs must not blink between them.
- **Performance budget:** frame build at most 1 ms p99 and 512 KB per frame. Frame building builds a draw record per
  drawn thing, so a particle per record has a real cost at scale. `td.PerformanceHarness` runs real towers on real waves,
  so effects land in it, but it doesn't report how many at once.
- **Painting is outside the harness.** It runs on the EDT, so translucent fills and clipping must be profiled live.
- **Draw records carry progress, never stored animation state** (`ui/CLAUDE.md`), and cosmetics follow one of two
  clocks. A moving enemy and its emission must not look detached, and fast-forward must not make them absurd.
- **A cloaked enemy must not be given away** by its flames or plus signs, except where a rule already reveals it
  (the burn reveal threshold).
- **Facts that break when markers go:** `EnemyFrameBuilderTest`, `AdaptiveResistTest` and `DefinedEnemyMobEffectTest`
  touch markers or badges. `Trait.marker()` is abstract on every trait, and `AdaptiveResist` changes its marker by damage
  type; the Info pane already says what it settled on. These describe today's behaviour: `ui/CLAUDE.md` (marker rows,
  overlay list, field flicker), `effect/CLAUDE.md` ("Adding a kind"), `enemy/CLAUDE.md` (`Trait.marker()`, adding a trait),
  `README.md` (markers on traits and effects) and `docs/ARCHITECTURE.md` (the frame builder table).
- **Status colours are shared with the term icons** (`TermLooks`), so removing a marker must not orphan its icon colour.
  Tower status marks paint with the same diamond, so it stays for them.
- **Standing rules:** vector art only, no image assets; `td.ui.render` stays AWT-free; verify looks with `PreviewBoard`,
  which shows one frame, so motion needs one live `run-jtd` pass.

## Decisions made

- This is split from the HUD work and done first.
- Trait markers go, and nothing says only that an enemy has an ability, with the disruption ring as the one exception.
- Whether an effect gets a mark depends on the effect: an effect with its own picture gets none.
- Shield is an egg bubble. Freeze really encases the enemy, with variety. Heal, burn, chill and every effect over time
  emit graphics.
- The boss mark shrinks. The floor gets darker.
- Health is a thin bar with solid bodies, in its own request (`FEATURE-enemy-health-bar.md`). This request adds the
  shield bar, all shields combined.
- The support aura ring shows only while its enemy is selected. The disruption ring stays always on, fainter, as the one
  exception: it shows which towers are being harmed, and only a few enemies have it.
- Gain and loss rings go. Cast and ability-spawn rings stay, softer. A freeze ending throws ice shards, since a freeze
  only ever runs out. Both this and the shield's pop are short bursts, not rings.
- The egg has no tint and doesn't change as shields drain. It pops only when a hit breaks the last shield: a shield that
  runs out of time just goes, and one breaking while another remains shows only in the bar. Damage that goes past shields
  doesn't flash the rim.
- Small bodies emit less, and there is no limit per effect: enemies with one effect look alike. If profiling a dense wave
  shows the game can't keep up, that comes back as a new decision.
- Emissions run on the wall clock: they keep moving while paused, keep their speed on fast-forward, and move with their
  enemy, so bleed drops land nowhere on the ground. Bursts follow game time, since they report an event.
- All 44 bodies get larger, at sizes chosen per body in this request. Other body work covers the five dark bodies and the
  five plain ones, then the owner reviews a before-and-after contact sheet of all 44. Dark bodies reach at least 3:1 against the floors they appear on. Circle, Square and Triangle lose their neon
  but keep their hue.
- Dots are one fixed colour per category, always in the order debuff, spotted, buff, centred under the body above the bars.
  A player cannot tell Sunder from Toll on the board; the Info pane says which. Info pane icons keep their own colours.
- Enemy looks move to tables per role now (ADR 0018), ahead of the towers, with no visible change of their own. Effect
  colours the Info pane icons use stay where they are.
- The board and the Info pane both show a shield's raw points, and the Info pane doesn't change. So lowered spirit doesn't
  shrink the bar, and at no worth a full-looking bar can pop on one hit. That is accepted.
- Burn, soulfire and poison emit by how full their pool is, between a visible minimum and a maximum, within what the
  body's size allows. So a strong and a weak tower's full pools look alike, and a stopped bleeding enemy shows no sign of
  it on the board. Both are accepted.
- The crit spark and `miss` text stay, since they report events. `miss` stays as it is, unless the live pass shows too many
  words on a dodging swarm. Every crit spark is the same star, size and colour, and the view never learns a crit's damage.
  Each crit has its own rotation, kept for that spark's life and derived from the enemy and the moment, so it can't touch
  game results. A second crit during a spark replaces it, as today.
- The selection ring stays at its size on a plain enemy and snaps to just outside the egg or ice while they show.
- The rank badge sits just above the body and moves above the egg or ice while they show, with flames and stars passing
  behind it. Hex runes stay where they are, drawn over effects. If a badge is hard to read on flames at real size, that
  comes back as a new decision rather than a backing shape added up front.
- Time left on an effect is not shown on the board: no timer bar, and the ice and stars don't change as it runs out. The
  Info pane has it.

## Open questions

None.

---

*After planning and implementation, update this document rather than deleting it: mark it implemented, prune resolved
open questions, and move it to `docs/features/shipped/`.*
