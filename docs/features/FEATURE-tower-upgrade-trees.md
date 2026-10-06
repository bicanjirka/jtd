# Feature Request: Three-Slot Tower Upgrade Trees

**Status: implemented.** Every tower has a `base`/`head`/`special` `UpgradeTree`
(`td.tower.upgrade`), the sidebar swaps `PanelWaveInfo` for `PanelUpgradeTree` while a tower is
selected, number keys buy the correspondingly-numbered offered node
(`GameEngine.buyUpgradeForSelected`), and the board shows per-slot pips, ready chevrons and a
`SPECIAL`-slot halo instead of the old two-role accent ring. The concrete node content itself
(names, prices, gates, buffs) is `docs/features/FEATURE-tower-specialization-abilities.md`'s
job, and that document's own `[S]`-tagged stub nodes still wait on their primitives - see
`TODO.md`'s "Tower specialization primitives". `FEATURE-tower-progression.md` since reshaped
the slots: `base` is the Range line and Attune -> Awaken -> Transcendent, `head` gains an extra
node, and exclusivity became an `ExclusiveChoice` the panel marks before the player pays.

## Summary

Replace each tower's single, permanent, two-choice upgrade with three independently-progressing
upgrade slots — `base` (chassis), `head` (weapon) and `special` (a magical/supernatural augment)
— each of which can branch into a small tree or DAG of its own nodes rather than a flat
two-option pick. A tower keeps mutating in place, the same way it does today; this feature
widens *how many* things about it can specialize at once and *how deep* each one can go, and
adds the UI, keyboard shortcuts and indicators needed to navigate that. `AuraTower`, which
shipped with zero upgrade paths, becomes a scoped-in future consumer of the new system.

## Current state (what exists today)

- **Exactly one upgrade path, ever, per tower.** `AbstractTower.chosenPath` is a single
  `protected volatile Optional<UpgradePath>`
  (`src/main/java/td/tower/AbstractTower.java:62`). `chooseUpgradePath` (`AbstractTower.java:293-308`)
  refuses a second call once `chosenPath` is present. `availablePaths()`
  (`AbstractTower.java:285-287`) returns a fixed `List<UpgradePath>` of exactly two for seven of
  the eight attack towers; `AuraTower` (`src/main/java/td/tower/AuraTower.java`) does not
  override it at all and offers none.
- **`UpgradePath`** (`src/main/java/td/tower/upgrade/UpgradePath.java`) is a record: display
  name, price, a `TowerBuff` stat bonus, an `UpgradeCondition`, and an optional `extraEffect`
  string. It has no id field — a path is identified only by being one of the two object
  references `availablePaths()` returns.
- **`TowerBuff`** (`src/main/java/td/tower/buff/TowerBuff.java:19-20`) is a five-axis additive
  record (`damageBonus`, `rangeBonus`, `fireRateBonus`, `bountyBonus`, `critChanceBonus`),
  combined purely by addition (`combine`, `TowerBuff.java:104-111`). Everything a path changes
  that isn't one of those five axes — `SplashTower`'s splash radius, `SonarTower`'s sweep speed,
  `MortarTower`'s slow duration, `SeekerTower`'s freeze duration, `CinderTower`'s wedge width —
  goes through a per-tower `onUpgradePathChosen(UpgradePath path)` hook
  (`AbstractTower.java:310-316`), matched by **Java reference equality** against each leaf's own
  private static `UpgradePath` constants, e.g. `SplashTower`'s `if (path == SIEGE) { … }`. This
  match works only because `chooseUpgradePath` always stores back one of the exact instances
  `availablePaths()` returned — there is no id, string, or other stable identity a node carries.
- **`UpgradeCondition`** (`src/main/java/td/tower/upgrade/UpgradeCondition.java:16-29`) is a
  two-method interface (`isSatisfied(Tower, GameWorld)`, `describe()`). Four implementations
  exist: `AlwaysCondition`, `ClusterCondition` (`ClusterCondition.java:1-39`, adjacency via
  `BoardGeometry.cellX/cellY`), `DamageDealtCondition`, `KillCountCondition` — all in
  `src/main/java/td/tower/upgrade/`. There are no `AndCondition`/`OrCondition` combinators, and no
  condition can ask "has this tower already chosen node X" — prerequisites between upgrades don't
  exist as a concept today.
- **The only visual signal for a chosen path is one accent ring**, coloured by
  `Palette.TOWER_UPGRADE_PATH_A` or `_B`. `TowerSpriteFrameBuilder.accentPaletteFor`
  (`src/main/java/td/ui/TowerSpriteFrameBuilder.java:73-87`) picks between them via
  `tower.availablePaths().indexOf(tower.getChosenPath())` — literally index 0 vs. index 1 of a
  two-element list, the same two roles reused across every tower type. `Java2DFrameRenderer
  .paintUpgradeAccent` (`src/main/java/td/ui/Java2DFrameRenderer.java:844`) draws it as a fixed
  ring regardless of body shape. This hard-codes "exactly two, unordered slots" at the render
  layer.
- **`TowerSpriteFrameBuilder` already builds two separate draw lists per tower** — a static body
  (`TowerSpriteDraw`, via `build()`) and an animated turret head (`TurretHeadDraw`, via
  `buildHeads()`), dispatched through `TowerVisitor` (see `td/ui/CLAUDE.md`, "The render
  pipeline"). Both currently key off the *same* `Palette` role per tower type
  (`bodyPaletteFor`, `TowerSpriteFrameBuilder.java:60-71`) — "head" today is a second shape, not
  an independently stylable or upgradable render component. There is no third,
  "enchantment/special" render layer at all. The closest existing analogue is `AuraTower`'s own
  pulsing-aura visual (`td/ui/CLAUDE.md`'s render pipeline and `animationSeconds` sections), which
  belongs to a wholly separate tower type today, not a slot on another tower.
- **`PanelTowerInfo` and `PanelWaveInfo` share the same fixed 200px-wide sidebar column**
  (`PanelGameConsole`, `src/main/java/td/ui/PanelGameConsole.java`). Both are given
  `setMaximumSize(new Dimension(200, …))`/`setPreferredSize` at construction
  (`PanelTowerInfo.java:212-214`; `PanelGameConsole.java:142-144`). `PanelTowerInfo` is added with
  `weighty = 0.1` (`PanelGameConsole.java:326-335`, so it expands to fill remaining vertical
  space); `PanelWaveInfo` is added below it with no `weighty` (`PanelGameConsole.java:337-346`,
  bottom-docked, sized to its own content). The shipped v1 upgrade doc already flagged this
  column as tight even for two stacked path buttons
  (`FEATURE-tower-upgrades.md`, "Multiple simultaneously-visible, independently-enabled buttons").
  `PanelTowerInfo.updatePathButtons`/`updatePathButton` (`PanelTowerInfo.java:111-131`) currently
  lay out at most two `HudButton`s (`jButton_path1`/`jButton_path2`, `PanelTowerInfo.java:54-55`)
  in that space.
- **The shipped v1 doc explicitly locked in "exactly two, mutually exclusive, permanent" as a
  deliberate v1 decision** (`FEATURE-tower-upgrades.md`, "Decisions made": "each tower offers
  exactly two upgrade paths, and choosing one is a permanent, mutually exclusive choice … picking
  one path forecloses the other for that tower forever"), and its own V1 Scope explicitly kept
  `AuraTower` out ("not in scope for its own specialization paths in v1 — it stays passive and
  unupgradeable for now"). This document is the sequel that revisits both calls.
- **No new tower object is ever spawned by an upgrade today**, and this stays true: a chosen path
  mutates the same `AbstractTower` instance's stats/fields in place
  (`chooseUpgradePath`/`onUpgradePathChosen`), it does not replace it. `td/tower/CLAUDE.md`'s
  "In-place upgrade paths" section documents this as the mechanic's whole point, distinct from
  `AuraTower`'s continuous external buff.
- **Existing per-tower keyboard shortcuts are a single-char dispatch, not per-selection.**
  `TowerDefense.keyTyped` (`src/main/java/td/TowerDefense.java:622-662`) loops over
  `TowerFactory.Type.values()` matching each type's own `placementKey` char
  (`TowerFactory.java:45-49`) to start *placing* a new tower of that type. There is no existing
  precedent for a number key acting on the *currently selected, already-built* tower — this
  feature's keyboard-shortcut ask (see below) is new dispatch, not a widening of `keyTyped`'s
  existing loop.

## What this feature adds

- **Three named upgrade slots per tower — `base`, `head`, `special`** — in place of today's one
  `chosenPath`. `base` covers chassis-level stats (range, rotation/turret speed); `head` covers
  weapon-level stats (projectile/damage, damage type, crit chance, fire rate — everything about
  "the weapon installed"); `special` covers a magical/supernatural augment (auras, status-effect
  application, curses/blessings), visually the slot carrying a pulsing/aura-style animation,
  analogous to what `AuraTower` already has today as a whole separate tower type.
  - **Naming note:** `base` is used instead of the more obvious `body` specifically to avoid
    colliding with `td.enemy`'s existing `BodyArchetype`/`bodyScaleFor` vocabulary
    (`src/main/java/td/enemy/BodyArchetype.java`), which already means something unrelated —
    an enemy's silhouette family — for enemies. Reusing "body" for a tower's chassis slot would
    make the same word mean two different things in two sibling packages.
- **Each slot holds at most one active node at a time, but each slot's own progression can
  branch, fork and reconverge independently of the other two slots** — three independent
  per-slot progression graphs (each can be a simple tree, or a DAG where forks reconverge), not
  one shared global tree and not three fully unrelated pick-lists. Cross-slot compatibility or
  synergy constraints (e.g. "this special requires that head") are explicitly **out of scope for
  this feature** — noted as a possible future extension, not designed here.
- **Upgrades still modify the tower's own instance — same instance, different internals. No new
  tower object is ever spawned**, matching today's mechanic exactly. Each slot's active node can
  still contribute a `TowerBuff` (combined additively across all three active slots' nodes plus
  any external `AuraTower` buff, exactly the way today's single path's buff already combines with
  an aura buff), alongside slot-specific bespoke mutations for stats `TowerBuff` can't express —
  the same `onUpgradePathChosen`-style hook concept, but now needing to identify nodes by a
  stable id rather than only by Java reference equality (see Constraints, below, for why
  reference-equality matching doesn't generalize once prerequisites and reconvergence exist).
- **Node identity and prerequisites.** Upgrade nodes get a stable id (not just uniqueness as
  static constants), and `UpgradeCondition` is extended with an "already chose node X in slot Y"
  check, plus `AndCondition`/`OrCondition` combinators wrapping the existing interface. This is
  what lets a slot's graph express "base → option1 OR option2" and "special → optionA AND
  optionB, both independently reachable."
- **UI: a new tower-upgrade-tree panel replaces `PanelWaveInfo` in the sidebar while a tower is
  selected**, swapping back to wave info once nothing is selected — a toggle in the same layout
  slot, not a permanent removal of wave-info visibility. Losing wave-info visibility while a
  tower is selected is confirmed acceptable.
- **Three coupled UX changes, delivered together with the panel/model since they all depend on
  it, not as separate features:**
  1. **Keyboard shortcuts.** With a tower selected, pressing a number key (1, 2, 3…) chooses the
     correspondingly-numbered available upgrade, mirroring the on-screen ordering.
  2. **An "upgrade available" indicator on a built tower**, shown per slot whose next node's
     condition is currently satisfied — something like an index-plus-up-arrow glyph (e.g. `"1⇧"`)
     — visible on the board, not only in the side panel.
  3. **Upgrade-path descriptive text moves off the base tower's build-button description.** That
     description should instead show upgrade *conditions and their current progress*, marked with
     coloured ✔/✘ symbols. The path/node descriptions themselves move onto the built tower's own
     per-slot upgrade buttons, revealed on hover.
- **`AuraTower` becomes a scoped-in future consumer of this system** — it gets its own upgrade
  paths under the new three-slot model once this feature is implemented. This document scopes
  that in as a target; it does not design `AuraTower`'s specific upgrade content.
- **This feature builds a real `UpgradeNode` for every `base`/`head`/`special` node across all 9
  towers** — the concrete content (names, prices, gates, `TowerBuff` bonuses) is authored in
  `docs/features/FEATURE-tower-specialization-abilities.md`, not invented at this feature's own
  planning time. Every node's hook is wired to real behavior where the engine already supports
  the mechanic (a `TowerBuff` axis, the existing per-tower field-bump hook pattern, an existing
  `Effect` kind reused as-is, a straightforward `td.tower.targeting` composition); where a node
  depends on a combat primitive that doesn't exist yet (that sibling document tags these **[S]**
  — a stacking damage-amplifying status effect, a guaranteed-crit trigger, an on-kill secondary
  effect, and others), this feature still builds the node itself — a real, selectable, priced,
  gated `UpgradeNode` with its own id and description — and gives its hook a documented no-op
  body, carrying a `TODO.md` entry per root `CLAUDE.md`'s "no inline TODO" convention. Wiring
  that no-op into real behavior is `FEATURE-tower-specialization-abilities.md`'s job, not a third
  feature.

## Interconnections

- **Directly supersedes/extends `docs/features/FEATURE-tower-upgrades.md`.** That document's
  "exactly two, mutually exclusive, permanent" decision and its "not in scope: `AuraTower`"
  decision are the two calls this feature revisits. The eight existing shipped `UpgradePath`
  constants across `SniperTower`/`SplashTower`/`SonarTower`/`PulseTower`/`MortarTower`/
  `SeekerTower`/`CinderTower` (two each, per `td/tower/CLAUDE.md`'s tower table) need to land
  somewhere in the new three-slot model — see Open questions.
- **Shares render-pipeline vocabulary with `td/ui/CLAUDE.md`'s existing per-domain-type frame
  builders** (`TowerSpriteFrameBuilder`, `TowerEffectFrameBuilder`) — a third render layer for
  `special` is new content in that pipeline, not a new pipeline.
- **The `special` slot's visual language is the same shape of thing `AuraTower`'s existing pulse/
  ring already is** (per `td/ui/CLAUDE.md`'s `animationSeconds` guidance for cosmetic, non-domain
  animation) — this feature doesn't invent a new rendering primitive so much as make an existing
  one (a pulsing aura) available as a slot on other towers instead of only as `AuraTower`'s whole
  identity.
- **Touches the same `PanelTowerInfo`/`PanelGameConsole`/`PanelWaveInfo` sidebar surface** the v1
  upgrade doc already flagged as tight for two buttons — this feature's panel-swap and
  three-times-the-content ask make that tension concrete rather than theoretical.
- **Two-way dependency with `docs/features/FEATURE-tower-specialization-abilities.md`** (formerly
  "Vulnerability Status Effect" — renamed once its scope grew to cover every tower's concrete
  node content, not just one status effect). This feature is that document's prerequisite for the
  slot/node/gating shape — in particular, `head`/`special` being exclusive pick-one-path-forever
  slots, and `base` being the one slot where both of its own nodes are buyable rather than
  exclusive — which that document's own node content and gating stay compatible with, not the
  other way around. In return, this feature depends on that document for the concrete node
  content itself (every node's name, price, gate and `TowerBuff` bonus) and for the real
  behavioral logic behind every node this feature can only stub out — see "What this feature
  adds," above.

## Constraints and open risks

- **Reference-equality node matching doesn't generalize.** Today's `onUpgradePathChosen`
  (`AbstractTower.java:310-316`) is matched per-leaf by `if (path == SIEGE)` against a private
  static constant. That only works because a tower has exactly one linear choice ever. Once a
  slot's graph branches, forks and reconverges, a leaf's hook needs to identify *which node in
  which slot* was just chosen without relying on object identity alone — some stable id is
  required, and every existing per-leaf `onUpgradePathChosen` override
  (`SplashTower`, `SonarTower`, `MortarTower`, `SeekerTower`, `CinderTower`) needs to move onto
  whatever that identity mechanism ends up being.
- **`chosenPath` is one `protected volatile Optional<UpgradePath>` field today; this feature
  needs three.** Per the root `CLAUDE.md`'s threading rules (§3), each slot's current node is an
  independent piece of tower state read by both the EDT (panel/render) and, via
  `recalculateStats()`, folded into the correlated `TowerStats` snapshot
  (`td/tower/CLAUDE.md`, "Aura buff stacking" section, `AbstractTower.recalculateStats()`).
  Whether three independent `volatile` fields are correct here, or whether "which node is active
  in each of my three slots" is itself a correlated set that needs one snapshot type, is a real
  design question for planning — not resolved here.
- **The accent-ring render model is hard-coded to two roles.**
  `TowerSpriteFrameBuilder.accentPaletteFor` (`TowerSpriteFrameBuilder.java:73-87`) and
  `Java2DFrameRenderer.paintUpgradeAccent` (`Java2DFrameRenderer.java:844`) assume "at most one
  active path, one of two colours." Three independently-active slots (`base`/`head`/`special`)
  need either three simultaneous visual signals or a redesigned single accent — this feature adds
  a slot the current render model has no vocabulary for at all (`special`'s aura-style
  animation), not just a third colour.
- **The sidebar column is a fixed 200px width today**
  (`PanelTowerInfo.java:212-214`, `PanelGameConsole.java:142-144`), already flagged as tight for
  two buttons by the shipped v1 doc. A three-slot branching tree view will likely need either a
  wider column or a compact/scrollable rendering. **This is an open engineering question, flagged
  here and not resolved** — see Open questions.
- **`no-wide-value-literals-in-tests` and `wide-values-have-a-narrow-entry-point`** (root
  `CLAUDE.md` §5/§7) apply to whatever new node/graph value type this feature introduces — a
  node with a handful of fields (id, display name, price, buff, condition, extra effect,
  prerequisites) is already close to the "5 or more components" threshold that requires a narrow
  factory rather than a widened constructor.
- **Existing tests reference `UpgradePath`/`chooseUpgradePath`/`getChosenPath` directly**
  (`GameEngineTest` per root `CLAUDE.md` §7's "integration surface" convention, plus any
  `td.tower` package tests) — widening the single-path model to three slots is a real-surface
  API change, not additive-only, and per the project's standing preference
  (`minimize-test-churn-from-feature-work`) should route through shared fixtures rather than a
  mass edit of positional test literals.
- **UI uniformity and headless-engine constraints still apply unchanged**: any new indicator
  glyph, hover text, or panel-swap must go through `HudButton`/`Hud`'s existing painting (root
  `CLAUDE.md` §8, `td/ui/CLAUDE.md`'s "HUD look is ours" section) rather than introducing new
  ad hoc Swing styling, and none of the new per-slot graph/condition logic may live in
  `TowerDefense` or any `Panel*` class (root `CLAUDE.md` §2.2) — it belongs in `AbstractTower`/
  `td.tower.upgrade`, observed by the UI the same way `availablePaths()`/`getChosenPath()` are
  today.

## Decisions made

These are settled by the project owner; planning designs the mechanism, not whether these hold.

1. **Three slots, named `base`, `head`, `special`.** `base` = chassis stats (range, rotation
   speed); `head` = weapon stats (projectile/damage/damage type/crit chance); `special` =
   magical/supernatural augment (auras, status effects, curses/blessings). `base` is chosen over
   `body` to avoid colliding with `td.enemy`'s `BodyArchetype` vocabulary.
2. **Each slot holds at most one active node**, but each slot's own progression is an
   independent branching/reconverging graph (tree or DAG). Cross-slot compatibility/synergy
   constraints are explicitly **out of v1 scope** — a possible future extension, not designed
   now.
3. **Upgrades modify the tower's own instance. No new tower object is ever spawned.** Each
   slot's active node contributes a `TowerBuff` (combined additively across all three slots plus
   any external aura buff) alongside bespoke, non-`TowerBuff` mutations via a hook analogous to
   today's `onUpgradePathChosen`, now keyed by a stable node id rather than only reference
   equality.
4. **Nodes get a stable id.** `UpgradeCondition` is extended with an "already chose node X in
   this slot" check, plus `AndCondition`/`OrCondition` combinators wrapping the existing
   single-method interface — cheap, since the interface is already a tidy one-method shape.
5. **UI: the upgrade-tree panel replaces `PanelWaveInfo` in the sidebar when a tower is
   selected**, and `PanelWaveInfo` is shown again when nothing is selected — a swap in that
   layout slot, not a permanent loss of wave-info. The existing fixed 200px sidebar column is
   flagged as an open engineering question (see Open questions), not resolved here.
6. **Three coupled UX changes ship as sub-scope of this same feature**, not as separate feature
   requests: number-key shortcuts for the currently-selected tower's available upgrades; a
   per-slot "upgrade available" indicator (e.g. `"1⇧"`) on a built tower; and moving upgrade
   descriptive text off the build-button description (which instead shows gate conditions and
   ✔/✘ progress) onto the built tower's own per-slot buttons, revealed on hover.
7. **`AuraTower` is an explicit target/follow-up consumer** of the new system once it's built —
   it currently has zero upgrade paths (`FEATURE-tower-upgrades.md`'s v1 explicitly excluded
   it). Its specific upgrade content is not designed by this document.
8. **Every node this feature builds is a real `UpgradeNode`, never a placeholder entry in a
   list.** A node whose behavior depends on a combat primitive that doesn't exist yet still gets
   a real id, price, gate and description, and is selectable in the UI — only its hook body is a
   documented no-op, closed out via `TODO.md` once
   `FEATURE-tower-specialization-abilities.md` supplies the real behavior. This feature never
   invents the node content itself; it's authored in that sibling document.

## Proposed shape (planning input, not commitments)

Sketches only — planning owns the actual data model. Included because the requester asked for
concrete-enough proposals that a planning pass has somewhere to start.

- **`UpgradeSlot`** — an enum or sealed set of three: `BASE`, `HEAD`, `SPECIAL`.
- **`UpgradeNode`** — the successor to `UpgradePath`: a stable `id` (String or a small value
  type), display name, price, `TowerBuff` bonus, `UpgradeCondition`, optional `extraEffect`
  text, and the set of node ids it requires as prerequisites (empty for a tree's root nodes).
  Given the component count, this likely needs the narrow-factory treatment root `CLAUDE.md` §5
  already requires for 5+ component records (`UpgradeNode.of(...)`/`withX` copies), the same
  shape `PathDefinition`/`EnemyDefinition`/`LevelDefinition` already use.
- **`UpgradeGraph`** (per slot, per tower type) — the node set plus edges (which node unlocks
  which), exposed as "nodes currently reachable from what's chosen so far" rather than a raw
  edge list, mirroring this codebase's existing "expose the queries callers make, not the
  internal structure" convention (root `CLAUDE.md` §6).
- **On `AbstractTower`**: three `Optional<UpgradeNode>`-shaped fields (one per slot) replacing
  today's single `chosenPath` — or, if the threading analysis above concludes they're
  correlated, one small value type holding all three, published the way `TowerStats` already is.
  `availablePaths()` becomes slot-scoped (`availableNodes(UpgradeSlot)` or similar), and
  `chooseUpgradePath`/`onUpgradePathChosen` become slot- and id-aware.
- **`PrerequisiteCondition`** — a new `UpgradeCondition` implementation alongside
  `ClusterCondition`/`DamageDealtCondition`/`KillCountCondition`, checking "this tower's `slot`
  currently holds a node whose id is in `requiredIds`." `AndCondition`/`OrCondition` wrap two
  `UpgradeCondition`s each, mirroring `TargetQuery.and`'s existing combinator shape
  (`td/tower/CLAUDE.md`, "Targeting" section) rather than inventing a new composition style.
- **Keyboard shortcuts**: a new dispatch in `TowerDefense`, distinct from `keyTyped`'s existing
  placement-key loop (`TowerDefense.java:636-643`), active only while a tower is selected —
  number key *n* maps to the *n*-th currently-available node across the panel's on-screen
  ordering (which itself needs to decide slot order: `base`, `head`, `special` is the natural
  reading order matching this document's own listing).
- **Indicator glyph**: a new `RenderFrame`/`TowerSpriteFrameBuilder` component (or an addition to
  the existing accent-ring mechanism) drawn per slot with a satisfied-and-affordable next node,
  distinct from the chosen-node accent ring itself — these are two different signals ("what did I
  pick" vs. "what could I pick right now").

## Open questions

Resolved during implementation: the sidebar kept its fixed 200px column, rendering each slot as
a header line plus up to three numbered buttons rather than widening the panel or drawing a
graph; migration of the eight shipped v1 `UpgradePath`s was superseded outright by
`FEATURE-tower-specialization-abilities.md`'s fresh catalogue rather than migrated verbatim; the
`special` slot's render layer is a pulsing halo reusing the Aura tower's own pulse clock; the
board-marker legibility question was resolved by dropping the doc's original `"1⇧"` digit
proposal in favour of a plain chevron with no digit (the number already appears on the sidebar's
own buttons), avoiding text rendering on the board entirely.

**Still open, deliberately deferred:** cross-slot compatibility/synergy (e.g. "this `special`
requires that `head`") stays out of scope, as a plausible future extension with no mechanism
proposed here.

---

*After planning and implementation, update this document rather than deleting it: mark it
implemented, prune resolved open questions, and either promote deferred scope (cross-slot
compatibility) to a new request or note it's still wanted for a later version.*
