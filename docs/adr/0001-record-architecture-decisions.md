# 0001. Record architecture decisions

- Status: Accepted
- Date: 2026-10-28

## Context
`docs/ARCHITECTURE.md` held every decision in one file, mixed with history and a description of
the current system. Its sections were edited in place as the code moved, and a design review
found them drifting: a section named a class that no longer existed, another claimed a check that
had been dropped, others quoted sizes that had tripled. Most of the work here is done by agents,
and the main risk is not a missing rule but an agent "improving" something decided on purpose.

## Decision
Each decision worth keeping is an Architecture Decision Record in `docs/adr/`, in the format of
Michael Nygard's original: status, context, decision, consequences, and the rejected alternatives.
A record is never rewritten once accepted; a new record supersedes it. `docs/ARCHITECTURE.md`
becomes an overview of the system as it is now, linking to the records.

## Consequences
- A record can't go stale silently. It is dated history, and the record that supersedes it says
  what holds now.
- Before reversing a structure, an agent checks `docs/adr/` for the decision behind it.
- Records cost nothing per session: nothing loads them automatically.
- Only decisions with real alternatives get a record, so the folder stays short.

## Rejected
- *Keeping one `ARCHITECTURE.md` and giving each section a status and a date.* That means fewer
  files, but in-place edits keep inviting the same drift.
- *adr-tools or another generator.* A template and an index are enough at this size.
