# Architecture decision records

One file per decision that someone could plausibly undo or debate again. Read the record before
reversing a structure it covers. To change a decision, write a new record that supersedes it;
never rewrite an accepted one.

- **When to write one:** a structural choice with real alternatives, whose reason isn't visible in
  the code. Not for every change, and not for rules (those go in `CLAUDE.md`) or general design
  principles (`docs/DESIGN.md`).
- **Name:** `NNNN-what-was-decided.md`, numbered in order and never reused.
- **Status:** `Accepted`, `Accepted, not yet implemented`, `Superseded by NNNN`, or `Rejected` (an
  option worth recording as declined).
- **Once accepted,** only the status line and a "Superseded by" link may change. Facts in an old
  record describe the code at its date.

## Template

```markdown
# NNNN. What was decided

- Status: Accepted
- Date: YYYY-MM-DD

## Context
The forces at play: what problem, what constraint, what was measured.

## Decision
What we do, in the present tense.

## Consequences
What becomes easier, what becomes harder, what to watch.

## Rejected
Each alternative, with why it lost.
```

## Index

| # | Decision | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-headless-engine.md) | The engine is headless; Swing stays in `TowerDefense` and `td.ui` | Accepted |
| [0003](0003-frames-are-described-then-painted.md) | Frames are described as AWT-free records and painted by one backend; all art is vector | Accepted |
| [0004](0004-gameworld-hands-out-collaborators.md) | `GameWorld` hands out collaborators; narrowing stops where it would be ritual | Accepted |
| [0005](0005-level-load-tears-down-first.md) | Loading a level tears the old one down first, in a fixed order | Accepted |
| [0006](0006-paths-are-pixel-geometry.md) | Paths are pixel geometry with one arc-length answer; buildability follows the final shape | Accepted |
| [0007](0007-owner-publishes-snapshots.md) | The owning thread publishes; correlated state crosses as one snapshot | Accepted |
| [0008](0008-values-that-combine-have-an-algebra.md) | Values that combine have an algebra | Accepted |
| [0009](0009-logging-and-one-fatal-boundary.md) | Logging setup, and one fatal start-up boundary | Accepted |
| [0010](0010-balance-is-measured-by-players.md) | Balance is measured by headless players on the real level | Accepted |
| [0011](0011-saves-hold-only-between-rounds.md) | A saved game holds only what exists between rounds | Accepted |
| [0012](0012-rules-in-checkstyle-and-lean-docs.md) | Checkable rules live in Checkstyle; `CLAUDE.md` files hold constraints only | Accepted |
| [0013](0013-style-stated-in-repo.md) | The code style is stated in this repository, not by reference | Accepted |
| [0014](0014-narrow-factories-and-shared-fixtures.md) | Wide records grow through narrow factories; tests share fixtures | Accepted |
| [0015](0015-stats-drive-buffs-towers-stay-code.md) | Stats drive buffs on both sides; towers stay code | Accepted |
| [0016](0016-upgrade-node-ids-are-permanent.md) | Upgrade node ids are permanent | Accepted |
| [0017](0017-package-cycle-accepted.md) | The domain packages form one cycle through `GameWorld`; only boundaries that hold are enforced | Accepted |
| [0018](0018-board-looks-are-role-tables.md) | Board looks are tables per role, not switches in the painter | Accepted, not yet implemented |
| [0019](0019-edt-commands-are-requests.md) | An EDT command to the engine is a request the next tick applies | Accepted, not yet implemented |
| [0020](0020-design-docs-family.md) | Design principles, decisions and the overview live in separate documents | Accepted |
