# Feature Request: In-Place Tower Upgrades

**Priority: Phase 1 — build first.** Of the three feature requests, this is the most
genre-standard, highest player-value mechanic and the one with the least new-system risk:
its core doesn't depend on feature 2 (damage types/projectiles) or feature 1 (enemy
traits/effects) existing, aside from the on-hit effects (slow/burn) an upgrade can grant,
which wait on feature 2. See the Product review notes section below for the reasoning.

## Summary

Add a way to upgrade a specific, already-placed tower in place, from a button in its info
panel — distinct from the game's current only "upgrade" mechanic, which is placing a
separate buffing tower (the "Power tower") nearby. Upgrades are **branching
specializations**: a tower offers several upgrade paths, and choosing one is a permanent
choice about what that specific tower becomes, not a step on a fixed linear tier ladder —
see Decisions made, below. A first version targets five independent enablement conditions:
money alone, a minimum number of towers on the field, a cluster of adjacent towers, a tower
having dealt a damage threshold, and a tower having reached a kill-count threshold. Upgraded
towers get a different visual and can apply new enemy-affecting effects (slow, burn, a
damage-taken-increase aura) or a bounty multiplier on their own kills. Two more far-reaching
cases — a one-time purchase that retroactively upgrades every tower of a type, present and
future, and an aura that boosts bounty for *any* kill in its area, not just this tower's own
— are documented here as a deliberately **deferred, later-phase** extension of this feature (see Decisions made, below),
not part of a first version.

## Current state (what exists today)

- **The name "upgrade" is already taken.** `td.tower.TowerUpgrade` (displayed as the
  "Power tower") is a distinct, placeable, passive tower that buffs every non-upgrade tower
  within its range via `TowerBuff` (`td.tower.buff.TowerBuff`), an additive
  damage/range-bonus algebra. It is built and sold exactly like any other tower through
  `TowerFactory`/`TowerRoster`. **This is a direct naming collision** with what this feature
  calls "tower upgrade" — this document's concept is upgrading one specific tower *in
  place*, which the codebase and its own `TODO.md` currently have no name or mechanism for
  at all. **Decided: the existing Power tower is renamed** (class, references, tests, and
  docs — `td/tower/CLAUDE.md`, `README.md`'s tower table), freeing the name "upgrade" for
  this feature's in-place mechanic, which keeps it in code and in the UI. The Power tower's
  new name is a small open item (see Open questions) but the direction — rename the
  existing tower, not the new feature — is settled.
- **This exact gap is already tracked**: `TODO.md`'s "Tower upgrade doesn't gate on
  affordability" entry says outright that "the only 'upgrade' mechanic is placing a separate
  `TowerUpgrade` tower nearby, not upgrading an existing tower in place" and that whether
  in-place upgrades are even wanted was an open decision. This feature request is, in
  effect, answering that question — implementing it should close that `TODO.md` entry in
  the same commit, per the project's standing rule.
- **The buff algebra already exists for two stats.** `AbstractTower.calcDamageRange()`
  folds every `TowerUpgrade` affecting a tower through `TowerBuff.combine` (identity
  `none()`, additive) to get `damageCurrent`/`rangeCurrent`. This is a real, working
  precedent for "a tower's effective stats are its base stats plus a reduce over some set of
  modifiers" — but today the only modifiers are *external* upgrade towers in radius, the
  only stats are damage and range, and there is no notion of a modifier a player *purchases directly onto* one tower.
- **`AbstractTower` already tracks the data most enablement conditions need**: `killCount`
  and `damageDealt` are accumulated on every hit via `dealDamage` (see `td/tower/CLAUDE.md`
  on why it must go through that one method). "Upgrade unlocks after N kills" or "after
  dealing X damage" needs no new tracking, just a read of fields that already exist.
- **`TowerRoster.all()`** gives the live, up-to-date tower list (a `CopyOnWriteArrayList`
  under the hood, safe to read from the EDT while ticks mutate it), which is what "N towers
  exist on the field" and "a cluster of M adjacent towers" would scan. Adjacency isn't
  currently computed anywhere — it would need cell coordinates via
  `BoardGeometry.cellX/cellY` (as `TowerRoster.sell`/`clear` already do) to test neighboring
  cells for occupancy.
- **The UI location this feature targets** is `td.ui.PanelTowerInfo`: today it shows the
  selected tower's live status text (`Tower.getStatusString()`) and exactly one button, a
  `HudButton` labelled `"Sell ( $N )"`, laid out in a small `jPanel_buttons` under the status
  text, disabled once the level has ended. The request asks for upgrade button (s) in "the
  same style and same position" as this sell button.
- **On the "sell button has red font" detail**: this is worth flagging directly rather than
  assumed — nothing in `td.ui.Hud` (the single place that owns the HUD's palette, per the
  root `CLAUDE.md`'s pinned UI-uniformity requirement) gives the sell button, or any button,
  a distinct text colour today. Every control currently renders identical foreground text (`Hud.FOREGROUND`, a pale
  green) regardless of role. Red sell text does not exist yet — it
  would itself be new scope introduced by this feature, not a color to merely "match."

## What this feature adds

- **A per-tower, in-place upgrade action**, reachable from a new button (or buttons) in
  `PanelTowerInfo`, styled identically to the existing sell button in every respect *except*
  the specific request that sell's text render in red while upgrade buttons' text does not.
- **Branching specialization, not a linear tier ladder.** A tower offers a small set of
  upgrade paths (e.g. "this Triangle tower can specialize toward raw single-target damage,
  toward applying slow, or toward extra range" — illustrative, not prescriptive); the player
  picks one, and that choice is permanent for that tower instance. This is a real design
  decision, distinct from and orthogonal to the enablement conditions below — a path can be
  gated by any one of them.
- **Four independent enablement conditions for v1** (see V1 Scope, below, for why a fifth —
  "a minimum count of towers on the field" — was cut):
    1. Money only — enabled once the player can afford it.
    2. A cluster of some number of towers built adjacent to one another.
    3. The specific tower having dealt at least some amount of damage.
    4. The specific tower having killed at least some number of enemies.
    5. A **global** upgrade: bought once, and retroactively applied to every tower of that
       type already on the field, and to every tower of that type built afterward — distinct
       in kind from the first four, which all upgrade one already-placed tower instance. **Deferred to a later phase**
       (see Decisions made, below) — documented here so the
       design isn't lost, not scoped for a first version.
- **New effects an upgrade can grant**: a different sprite/visual for the upgraded tower;
  enemy-affecting on-hit effects such as slow or burn (see the damage-types/projectiles
  feature); an aura that increases damage *enemies* take while standing in it (a debuff
  aura, the mirror image of the existing Power tower's buff aura, but targeting enemies
  instead of towers); a bounty multiplier on this tower's own kills; and an aura variant
  that boosts bounty for *any* kill within its area, not only kills this specific tower
  scores itself — this last one is, like the global upgrade above, **deferred to a later
  phase** (see Decisions made) rather than scoped for a first version.

## Interconnections with the other two feature requests

- **Depends on the damage-types/projectiles feature** for the on-hit effect vocabulary (slow, burn) an upgrade is meant
  to grant — this feature can't apply "burn" to a target
  before that effect exists as a mechanism, however it ends up represented.
- **Depends on (or should share) the effects primitive** discussed in the enemy-traits
  document: the "damage-taken-increase aura" is mechanically a debuff *effect* applied to
  every enemy inside a radius, which is the same shape of thing an enemy's own ability (feature 1) or a tower's on-hit
  effect (feature 2) applies — building it as a third,
  independent implementation is avoidable if the shared primitive is settled first.
- **Naming collision with `td.tower.TowerUpgrade`/"Power tower" is resolved by renaming the
  existing tower**, not this feature's concept — see Current state, above. The rename (class, every reference, test, and
  doc) is a small but real, easy-to-forget prerequisite
  that should land before or alongside this feature's own implementation, so no code or
  documentation is ever written against a `TowerUpgrade` that means two different things.
- **Closes an existing tracked gap.** `TODO.md`'s "Tower upgrade doesn't gate on
  affordability" entry is written for exactly this feature; per the project's standing rule ("close a gap, delete its
  entry in the same commit"), landing this feature should delete
  that entry.

## Architectural implications

- **The five existing enablement conditions fit the existing per-instance pattern** (a
  tower already tracks its own `killCount`/`damageDealt`; `TowerRoster` already exposes a
  live tower list to scan) — these are additive, moderate-sized changes.
- **The global, buy-once-applies-to-all-present-and-future upgrade does not fit that
  pattern at all**, and is architecturally the most significant piece of this request:
    - It must retroactively modify every already-built tower of the target type — something
      no existing mechanism does (today, a tower's stats are fixed at construction plus
      whatever `TowerBuff`s currently register against it; nothing reaches back into already-
      placed towers to change their base behavior).
    - It must also affect every *future* tower of that type, meaning `TowerFactory` (or
      whatever replaces the buy flow) needs to consult some new piece of persistent,
      per-level (or per-game?) state keyed by tower type when constructing a tower — state
      that doesn't exist anywhere in `GameWorld` today.
    - It must interact correctly with level teardown and reload. The root `CLAUDE.md`'s
      Levels section is explicit that `GameEngine.loadLevel` is idempotent and must not leak
      state between plays (`TowerRoster.clear()`, `EnemyRoster.clear()` were both carefully
      designed around exactly this). A global upgrade flag needs the same discipline, or a
      researched upgrade would incorrectly persist into the next level or the next run.
    - **Decided: deferred to a later phase.** The five per-instance conditions form a
      coherent, materially smaller feature on their own and are what a first version targets;
      this sub-case stays documented here as a future-feature request rather than being
      dropped or built prematurely.
- **The "bounty for any kill in this aura, not just my own" effect is a new cross-tower
  coupling that doesn't exist today.** `EconomyDelta.kill(bounty)` is currently computed
  once, inside `AbstractEnemyMob.doDamage`, purely from the dying enemy's own `price` — it
  has no notion of *which tower's* shot killed it, let alone *which auras happen to cover
  the kill location*. Supporting this would need either passing the killing tower (or its
  position) outward from `doDamage` so an aura-owning tower can be consulted, or a new
  aura-query step somewhere in the kill path — a real new coupling between `td.enemy` and
  `td.tower` that today's clean one-directional dependency (towers query enemies, never the
  reverse) doesn't have. This is worth scoping as its own decision, separately from the
  more self-contained per-tower bounty multiplier (which only needs the firing tower to
  know its own multiplier, no new coupling at all).
- **`PanelTowerInfo` needs to observe more than it does today.** Today it reacts only to
  `EconomyListener.economyChanged` (for sell-button affordability, refreshed on the render
  pulse via `refreshSelected()`). Tower-count and cluster-based enablement need it (or
  whatever computes button enabled-state) to also react to `TowerListener.towerBuild`/
  `towerRemoved` — a new dependency this panel doesn't currently have.
- **Multiple simultaneously-visible, independently-enabled buttons** is a real UI layout
  question beyond "add one more button next to sell": `jPanel_buttons`'s `GridBagLayout`
  currently lays out exactly one control, and needs a small redesign to hold several, each
  toggling enabled/disabled on its own condition, without crowding the panel.
- **Upgraded-tower visuals feed into the same fixed checklist `td/ui/CLAUDE.md` and
  `td/tower/CLAUDE.md` already document** for a new tower's art (a `Palette` role, a shape
  in `Java2DFrameRenderer`, wiring through `TowerVisitor`) — except now a single *tower
  type* can have more than one visual (base vs. upgraded), which the current one-shape-per-
  tower-type model doesn't yet express.

## Risks and costs

- **The uniform-HUD requirement and the red-sell-text request are in tension**, and need an
  explicit, narrow reconciliation: the root `CLAUDE.md` and this project's standing
  preference are that every clickable control shares one look — border, face, font, and
  states — with no control looking different for arbitrary reasons. A single-color text
  accent used consistently to mean "destructive/irreversible action" (sell) versus
  "constructive action" (upgrade) is a defensible, narrow exception, but it should be
  scoped precisely (foreground text colour only, everything else — border, fill, hover/
  press states, font — identical) rather than opening the door to per-button styling in
  general.
- **The global upgrade is disproportionately large relative to the rest of the feature** —
  see above. Building it without first designing where "per-tower-type persistent state"
  lives risks a bolt-on that doesn't survive level reload cleanly.
- **The any-kill-in-aura bounty effect introduces a new domain coupling** (enemy death path
  needs to know about nearby towers) that the codebase has deliberately avoided so far.
  It's deferred to a later phase for exactly this reason — the simpler, self-contained
  per-tower bounty multiplier (which needs no such coupling) is what a first version
  should ship instead.
- **Combinatorial stacking**: an upgraded tower can simultaneously carry this feature's
  modifiers *and* sit inside one or more existing Power-tower buff auras *and*, per
  feature 1/2, hit enemies carrying resistance/immunity traits — balance-testing the
  product of all three features together is a materially larger effort than any one alone.
- **Test surface**: enablement conditions need headless, clock-free proof the same way
  every other engine rule does (per the root `CLAUDE.md`'s Tests section) — tower-count and
  cluster conditions in particular need deterministic board setups to assert against.

## Product review notes

A senior-product-owner pass over this request, with a genre lens, surfaced a few points
worth recording alongside the decisions below:

- **This is the single most genre-expected mechanic of the three feature requests, and the
  one this game is currently missing.** In-place tower upgrades are closer to a TD-genre
  baseline expectation than resistances, projectile physics, or damage types are. That's
  why it's sequenced first — see Priority, above — ahead of the request's original
  ordering.
- **The "N towers exist on the field" enablement condition was the weakest of the five.** It
  gated a specific tower's upgrade on unrelated actions taken elsewhere on the board, which
  read more like an idle-game checklist than a tactical decision about *this* tower —
  dropped for v1 (see Decisions made and V1 Scope, below).
- **`AbstractTower.getSellPrice()` didn't know about upgrade spend.** Resolved: no refund —
  see Decisions made.

## Decisions made

- The naming collision is resolved by renaming the existing Power tower (`td.tower.TowerUpgrade`) to the **Aura tower**
  (`td.tower.TowerAura`) — "upgrade" stays
  the name of this feature's in-place mechanic, in code and in the UI.
- Upgrades are **branching specializations**: each tower offers exactly **two** upgrade
  paths, and choosing one is a **permanent, mutually exclusive** choice for that tower
  instance — picking one path forecloses the other for that tower forever.
- The "N towers exist on the field" enablement condition is **dropped for v1**. Its
  replacement four-condition set is: money, cluster-of-adjacent-towers, damage-dealt, and
  kill-count.
- Selling an upgraded tower **does not refund any upgrade spend** — `getSellPrice()` stays
  75% of the original build price only, unchanged from today. Upgrade cost is a permanent
  sunk cost, consistent with sell already being framed as "always a loss."
- The global, buy-once-for-all-present-and-future upgrade and the any-kill-in-aura bounty
  effect are both **deferred to a later phase**. Both stay documented above as
  future-feature requests rather than dropped, since they're real ideas worth building
  eventually — just not part of a first version, given the new persistent-state and
  cross-package coupling each would require. A first version targets the four per-instance
  enablement conditions and the per-tower-only bounty multiplier.
- Sequencing: this feature ships **first**, ahead of damage types/projectiles and enemy
  traits/effects (see Priority, above) — the highest-value, lowest-dependency of the three.

## V1 Scope

With the decisions above settled, this section pins down what a first version actually
contains: concrete content, the shape of the solution, and a phased implementation order.

### Boundary

- 4 attack towers (`TowerOne`/`TowerTwo`/`TowerThree`/`TowerFour`), each with exactly 2
  upgrade paths — 8 concrete upgrade paths total for v1.
- 4 enablement conditions in play across those 8 paths: money, cluster, damage-dealt,
  kill-count.
- No new targeting, delivery, or enemy-facing mechanic. **Because this feature ships before
  damage types/on-hit effects (phase 2), no v1 path can apply slow, burn, or any other
  enemy-affecting status effect** — those become natural additions to a tower's path set
  once phase 2 lands (see Priority, above), not something v1 blocks on. Every v1 path is
  built from primitives the engine already has: damage, range, fire-rate (cooldown), one
  tower-specific stat (`TowerTwo`'s splash radius, `TowerThree`'s sweep speed), a sprite/
  visual change, and a bounty multiplier on the tower's own kills (self-contained — no
  phase-2 dependency, since it only touches the firing tower's own kill accounting).
- The Aura tower (renamed from Power tower/`TowerUpgrade`) is **not** in scope for its own
  specialization paths in v1 — it stays passive and unupgradeable for now, to keep the
  content list to the four attack towers.

### Proposed content (illustrative — numbers are placeholders for a later balance pass)

| Tower                   | Path A                                                                                     | Gate         | Path B                                                                | Gate       |
|-------------------------|--------------------------------------------------------------------------------------------|--------------|-----------------------------------------------------------------------|------------|
| `TowerOne` (Triangle)   | **Veteran** — modest damage/range bump, plus a bounty multiplier on this tower's own kills | kill-count   | **Overclock** — shorter cooldown (faster fire), lower per-shot damage | money      |
| `TowerTwo` (Circle)     | **Siege** — bigger damage and splash radius                                                | damage-dealt | **Cluster Charge** — bigger damage and range                          | cluster    |
| `TowerThree` (Sunshine) | **Overcharged Array** — faster sweep (shorter `secondsPerRevolution`) and more range       | cluster      | **Marksman Beam** — bigger per-hit damage                             | kill-count |
| `TowerFour` (Stardust)  | **Overload Core** — bigger damage                                                          | damage-dealt | **Expanded Field** — bigger range                                     | money      |

Each condition is used exactly twice across the 8 paths, so the feature exercises all four
gates in actual content rather than leaving one theoretical. `TowerTwo`'s "Cluster Charge"
and `TowerThree`'s "Overcharged Array" deliberately lean into the cluster gate's flavor —
towers built as a group empowering each other reads naturally for a splash tower and a
sensor-sweep tower specifically.

### Shape of the solution

- **A path's stat bonus reuses the existing `TowerBuff` algebra rather than inventing a
  parallel one.** `TowerBuff` (`td.tower.buff.TowerBuff`) already models "damage/range bonus,
  identity `none()`, additive `combine`" — exactly what a chosen path's damage/range
  contribution is. `AbstractTower.calcDamageRange()`'s existing reduce over `upgTowers`
  should be widened to also fold in the tower's own chosen path (if any), so a
  specialization composes correctly with a nearby Aura tower's buff rather than needing
  separate code. Fire-rate and bounty-multiplier bonuses don't fit `TowerBuff`'s current two
  fields; widening the record with additional optional-bonus fields (each defaulting to 0,
  preserving `none()` as the identity) is more consistent with this codebase's existing
  "algebra" pattern than introducing a second, differently-shaped modifier type.
- **Tower-specific stats (`TowerTwo.spreadRadius`, `TowerThree`'s sweep-speed constant)
  stay outside `TowerBuff`.** Only one path per tower touches one, so this is the exception,
  not the rule — a per-leaf-class delta applied directly at path-selection time, the same
  way `spreadRadius` is already computed once at construction.
- **A tower's chosen path is permanent, one-time state**, unlike `TowerBuff`'s live,
  continuously-recomputed contribution from nearby Aura towers. It only needs to be applied
  once, when the path is chosen, and then folded into every future `calcDamageRange()` call
  the same way an `upgTowers` entry already is.
- **Enablement conditions need a small, closed set of evaluators** — one per condition kind (money, cluster,
  damage-dealt, kill-count) — each answering "is this specific tower's path
  currently available" given the tower itself, the current `EconomyState`, and (for cluster)
  the current `TowerRoster`. Cluster adjacency needs a new helper using
  `BoardGeometry.cellX/cellY` (precedent: `TowerRoster.sell`/`clear` already convert a
  tower's pixel position to a cell this way) to count same-type or any-type neighbors in the
  8 surrounding cells.
- **`PanelTowerInfo` needs real layout and observation changes**: two new buttons per
  selected tower (styled as `HudButton`s, one per path, each independently enabled/disabled
  by its own condition), replaced by a single "Specialized: `<path name>`" status line once
  a path is chosen (since the choice is permanent, the other button simply disappears
  rather than staying visible-but-disabled forever). This needs `PanelTowerInfo` to observe
  `TowerListener.towerBuild`/`towerRemoved` in addition to its existing
  `EconomyListener.economyChanged`, since the cluster condition can flip as neighbors are
  built or sold.
- **Rename mechanics for the Power tower → Aura tower**: class rename (`td.tower.TowerUpgrade` → `td.tower.TowerAura`),
  its display string ("Power tower" →
  "Aura tower"), every reference in `TowerFactory.type`, tests, `td/tower/CLAUDE.md`'s tower
  table, and `README.md`'s tower table — done as its own early phase (see below) so no code
  is ever written against the old name meaning two different things.
- **New tower art**: each of the 8 paths implies a distinct upgraded sprite, which is the
  same fixed checklist `td/tower/CLAUDE.md` already documents for a new tower (a `Palette`
  role, a shape in `Java2DFrameRenderer`, wiring through `TowerVisitor`) — except now one
  `TowerFactory.type` can render two different ways depending on its chosen path, which the
  current one-shape-per-type model doesn't yet express. This is likely the single largest
  piece of new-art work in the whole feature (8 new visuals, not 4).

### Phased implementation order

Per this project's standing "commit after each phase" convention:

1. **Rename the Power tower to the Aura tower** — mechanical, low-risk, unblocks everything
   else naming-wise. Closes no functional gap on its own but must land first.
2. **Core upgrade data model and permanent-selection state**: `TowerBuff` widened with the
   new optional bonus fields; a small value type describing one upgrade path (name, stat
   bonuses, gate); each attack tower's two path definitions; a chosen-path field on
   `AbstractTower` and its folding into `calcDamageRange()`. Provable headlessly — no UI yet.
3. **Enablement condition evaluators** (money, cluster, damage-dealt, kill-count), including
   the new cluster-adjacency helper. Also provable headlessly against constructed board
   states.
4. **`PanelTowerInfo` UI**: the two-button layout, `TowerListener` observation, the
   post-selection "Specialized: …" status line, and the sell-button red-text accent (see
   Open questions — still needs the styling-scope answer). Verified visually via the
   `run-jtd` skill, per this project's UI requirement.
5. **Per-path art**: the 8 upgraded visuals, following `td/tower/CLAUDE.md`'s checklist.
6. **Balance pass and `TODO.md` cleanup**: tune the placeholder numbers above via actual
   play, then delete the "Tower upgrade doesn't gate on affordability" `TODO.md` entry this
   feature closes.

## Open questions

1. For the red-sell-text detail: is a colour accent on text alone the intended scope, or is
   a further visual distinction (e.g. an icon) also wanted for destructive vs. constructive
   actions?
2. Does the proposed content table above (which tower gets which two specializations, and
   which gate each uses) match the intended feel, or should any pairing change before
   implementation starts?
