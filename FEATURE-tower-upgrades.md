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
— are documented here as a deliberately **deferred, later-phase** extension of this feature
(see Decisions made, below), not part of a first version.

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
  only stats are damage and range, and there is no notion of a modifier a player
  *purchases directly onto* one tower.
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
  text, disabled once the level has ended. The request asks for upgrade button(s) in "the
  same style and same position" as this sell button.
- **On the "sell button has red font" detail**: this is worth flagging directly rather than
  assumed — nothing in `td.ui.Hud` (the single place that owns the HUD's palette, per the
  root `CLAUDE.md`'s pinned UI-uniformity requirement) gives the sell button, or any button,
  a distinct text colour today. Every control currently renders identical foreground text
  (`Hud.FOREGROUND`, a pale green) regardless of role. Red sell text does not exist yet — it
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
- **Several independent enablement conditions**, each presumably tied to a different
  concrete upgrade path:
  1. Money only — enabled once the player can afford it.
  2. A minimum count of towers present on the field.
  3. A cluster of some number of towers built adjacent to one another.
  4. The specific tower having dealt at least some amount of damage.
  5. The specific tower having killed at least some number of enemies.
  6. A **global** upgrade: bought once, and retroactively applied to every tower of that
     type already on the field, and to every tower of that type built afterward — distinct
     in kind from the first five, which all upgrade one already-placed tower instance.
     **Deferred to a later phase** (see Decisions made, below) — documented here so the
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

- **Depends on the damage-types/projectiles feature** for the on-hit effect vocabulary
  (slow, burn) an upgrade is meant to grant — this feature can't apply "burn" to a target
  before that effect exists as a mechanism, however it ends up represented.
- **Depends on (or should share) the effects primitive** discussed in the enemy-traits
  document: the "damage-taken-increase aura" is mechanically a debuff *effect* applied to
  every enemy inside a radius, which is the same shape of thing an enemy's own ability
  (feature 1) or a tower's on-hit effect (feature 2) applies — building it as a third,
  independent implementation is avoidable if the shared primitive is settled first.
- **Naming collision with `td.tower.TowerUpgrade`/"Power tower" is resolved by renaming the
  existing tower**, not this feature's concept — see Current state, above. The rename
  (class, every reference, test, and doc) is a small but real, easy-to-forget prerequisite
  that should land before or alongside this feature's own implementation, so no code or
  documentation is ever written against a `TowerUpgrade` that means two different things.
- **Closes an existing tracked gap.** `TODO.md`'s "Tower upgrade doesn't gate on
  affordability" entry is written for exactly this feature; per the project's standing rule
  ("close a gap, delete its entry in the same commit"), landing this feature should delete
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
- **The "N towers exist on the field" enablement condition is the weakest of the five.** It
  gates a specific tower's upgrade on unrelated actions taken elsewhere on the board, which
  reads more like an idle-game checklist than a tactical decision about *this* tower.
  Recommendation: either drop it, or reframe it as something spatially tied to the tower
  itself (e.g. towers of the same type within its own range) so the condition is still
  legible from the tower's own info panel without the player having to reconstruct board
  state from memory. Not yet a final decision — flagged for the same conversation that
  settles the branching-vs-linear question.
- **`AbstractTower.getSellPrice()` doesn't currently know about upgrade spend.** It's a flat
  75% of the original build price. Once money can be sunk into an upgrade path, does
  selling refund any of that, or is upgrade spend a permanent sunk cost? This needs an
  explicit answer before implementation; it isn't addressed elsewhere in this document.

## Decisions made

- The naming collision is resolved by renaming the existing Power tower
  (`td.tower.TowerUpgrade`), not this feature's concept — "upgrade" stays the name of this
  feature's in-place mechanic, in code and in the UI.
- Upgrades are **branching specializations**: each tower offers several upgrade paths, and
  choosing one is a permanent choice for that tower instance, not a step on a shared linear
  tier ladder. See Open questions for how many concurrent paths a tower has and whether a
  path choice forecloses the others entirely.
- The global, buy-once-for-all-present-and-future upgrade (enablement condition 6) and the
  any-kill-in-aura bounty effect are both **deferred to a later phase**. Both stay
  documented above as future-feature requests rather than dropped, since they're real
  ideas worth building eventually — just not part of a first version, given the new
  persistent-state and cross-package coupling each would require. A first version targets
  the five per-instance enablement conditions and the per-tower-only bounty multiplier.
- Sequencing: this feature ships **first**, ahead of damage types/projectiles and enemy
  traits/effects (see Priority, above) — the highest-value, lowest-dependency of the three.

## Open questions

1. What should the existing Power tower (`td.tower.TowerUpgrade`) be renamed to?
2. How many upgrade paths does one tower offer, and does choosing one permanently foreclose
   the others, or can a tower eventually qualify for more than one specialization if the
   player meets every path's conditions?
3. Is the "N towers exist on the field" enablement condition kept, dropped, or reframed to
   tie back to the specific tower being upgraded (see Product review notes)?
4. Does selling an upgraded tower refund any of what was spent on its upgrade path, or is
   that spend permanently sunk?
5. For the red-sell-text detail: is a colour accent on text alone the intended scope, or is
   a further visual distinction (e.g. an icon) also wanted for destructive vs. constructive
   actions?
