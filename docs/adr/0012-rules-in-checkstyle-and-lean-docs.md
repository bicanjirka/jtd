# 0012. Checkable rules live in Checkstyle; `CLAUDE.md` files hold constraints only

- Status: Accepted
- Date: 2026-09-24

## Context
A hand-rolled checker of several hundred lines backed every `CLAUDE.md` rule. Some of its checks
policed the docs themselves: every backticked type had to exist, and every check had to be cited.
The package docs grew until a small task loaded tens of thousands of tokens, and a rename meant
editing prose. That cost was too high for a hobby project.

## Decision
- Rules a tool can check are standard Checkstyle modules (`checkstyle.xml`,
  `checkstyle-imports.xml`), run by `mvn verify`.
- Checks that policed documentation were dropped. So was a check that matched only the easy case
  of its rule, because it gave false confidence (`32cc0cb`), and one that needed cross-file
  analysis.
- `CLAUDE.md` files hold operative constraints only: no history, no feature narrative, no numbers
  the code holds. The long-form package docs remain in git at `7a69e5c`.
- No second document per package. It would be one more thing to keep in step.

## Consequences
- A check is either honest and standard or doesn't exist. A rule that only prose carries may
  drift, and the review rounds catch that.
- New package docs stay short: a constraint goes in, a description doesn't.

## Rejected
- *Keeping the hand-rolled checker.* It cost more to maintain than it saved.
- *Long-form per-package architecture files.*
