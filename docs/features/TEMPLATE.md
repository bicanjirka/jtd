# Feature Request: <Name>

> Delete this line and everything above it when writing a real request. This template exists
> to separate what a request should decide from what the planning phase should design: this
> file states intent, constraints and product-level decisions; it does not propose a solution,
> a data-model shape, or an implementation order — that thinking belongs to planning, and
> writing it here pre-empts it.

## Summary

What the feature is and why it's wanted, in product terms. A sentence or two, not a pitch.

## Current state (what exists today)

Facts about the codebase this request depends on: what already exists, what's adjacent, what
name or mechanism would collide, what a `TODO.md` entry already says. This section grounds the
planning phase in reality — it answers "what is true today", never "what should we build."

## What this feature adds

The scope, described as player/product-visible behavior — what's new, what changes, what's
explicitly out of scope for a first version. Still no data model, no class names, no "this
should use X pattern."

## Interconnections

Does this depend on, conflict with, or share a concept with other in-flight or recent feature
requests? Note it here so planning doesn't discover the collision mid-implementation. Omit this
section if there's nothing to say.

## Constraints and open risks

Real tensions the codebase or the product imposes, stated as facts or questions — not as
answers. E.g. "nothing today retroactively modifies an already-built X" is a constraint worth
flagging; "so we should add a Y registry" is a solution, and belongs to planning instead.
Also flag standing project requirements this feature will need to satisfy (UI uniformity,
headless engine, threading rules) without prescribing how.

## Decisions made

Product-level calls only the requester can make: what's cut from v1, naming, sequencing,
priority relative to other requests, whether a tension gets resolved narrowly or broadly.
Not implementation decisions — those are planning's output, not this document's.

## Open questions

Anything still undecided that planning needs an answer to before or during design.

---

*After planning and implementation, update this document rather than deleting it (see the
root `CLAUDE.md`'s documentation map): mark it implemented, prune resolved open questions, and
either promote deferred scope to a new request or note it's still wanted for a later version.*
