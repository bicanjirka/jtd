# 0020. Design principles, decisions and the overview live in separate documents

- Status: Accepted
- Date: 2026-10-28

## Context
Design guidance was split between root `CLAUDE.md` (a few style lines) and `ARCHITECTURE.md` (a mix
of rationale, history and description). Nothing said how to *choose* a design: how to split a
class, which coupling is acceptable, when to use data or code. A UI guide and a UX guide are
planned, so the documents need a shape that more guides can join.

## Decision
| Document | Holds |
|---|---|
| root and package `CLAUDE.md` | Rules for every edit, and per-package constraints and checklists |
| `docs/DESIGN.md` | How to choose a design: structured design, object design, the functional approach, SOLID as applied here, and a quick guide |
| `docs/adr/` | What was decided, why, and what was rejected: one record per decision |
| `docs/ARCHITECTURE.md` | The system as it is now: the main structures and flows, with diagrams, linking to records |
| future `docs/UI.md`, `docs/UX.md` | The visual language and the player's flows, in the same shape as `DESIGN.md` |

- Root `CLAUDE.md` points at `DESIGN.md` for structural choices and at `docs/adr/` before a
  structure is reversed. It keeps only operative rules.
- A rule goes to the cheapest place that enforces it (`DESIGN.md` §15).

## Consequences
- Each document has one reason to change. The overview changes with the code, a record never does,
  and the design guide changes only with a principle.
- Agents read `DESIGN.md` only when making a structural choice, so it costs nothing on ordinary
  edits.

## Rejected
- *Putting design principles in root `CLAUDE.md`*: loaded every session, and it would grow with
  each principle.
- *Keeping everything in `ARCHITECTURE.md`*: the drift that 0001 records.
