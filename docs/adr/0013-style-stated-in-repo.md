# 0013. The code style is stated in this repository, not by reference

- Status: Accepted
- Date: 2026-09-17 (recorded)

## Context
The style was first described by reference to a sibling repository on the local disk, read as a
normative dependency. A fresh clone, a build server or another machine can't follow an
instruction that points outside the repository. Such an instruction is skipped silently while the
work still claims to comply.

## Decision
The rules jTD applies are stated in its own `CLAUDE.md` files and `docs/DESIGN.md`, in their
jTD-specific form, with the exceptions this codebase takes written next to them.

## Consequences
- Every instruction can be followed from a clone.

## Rejected
- *Keeping the reference*, or vendoring the sibling repository's docs: rules nobody here applies
  would come along.
