---
name: grill-me
description: Interview the user relentlessly about a plan or design until reaching shared understanding, resolving each branch of the decision tree one at a time. Use when the user wants to stress-test a plan, feature request or design, asks to be grilled, or says "grill me".
---

Interview me relentlessly about every aspect of this plan until we reach a shared understanding.
Walk down each branch of the design tree, resolving dependencies between decisions one by one:
settle a decision before asking about the ones that depend on it.

- Ask one question at a time, and give your recommended answer with each.
- If the codebase or the docs (`README.md`, `docs/`) can answer a question, read them instead of
  asking.
- Push back on vague answers, hidden assumptions and contradictions with earlier answers or with
  `CLAUDE.md`; say which edge case or failure an answer leaves open.
- Keep a running list of settled decisions and open branches. Stop when no branch is left open,
  then summarise the decisions and ask where to write them down.
