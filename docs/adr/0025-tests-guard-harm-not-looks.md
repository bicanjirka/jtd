# 0025. Tests guard harm, not looks

- Status: Accepted
- Date: 2026-11-25

## Context
An audit of the suite (2,116 tests, 0.44 test lines per production line) found the rules on content worked: tuning
commits added no tests. The excess sat in `td.ui`, where no rule said what not to test. The verification ladder named
a test as the cheapest proof, and every proof was kept: frame-builder tests pinned draw order, sizes and particles;
`HudVoiceTest` restated `Hud`'s own colour formulas and was rewritten whenever the style changed on purpose; a hover
fix committed the probe that found it, debug output included. A frame-builder test was the second most edited test
file. Roughly a third of the tests the sampled commits added pinned a look, a sentence or an implementation.

## Decision
- A test guards harm: a defect that changes what the game decides (rules, money, lives, saves, balance numbers,
  thread hand-offs, a tick that throws) or what the player may know (hidden information, a figure the player decides
  on). Harm is judged first; visibility does not excuse it.
- What decides nothing (looks, wording, draw order, a paint or frame-build crash) is checked once by eye and gets no
  test. UI behaviour that decides nothing gets a test only when it is rare or fiddly enough that normal play would
  miss it.
- A test written to prove a change is scratch unless it meets the above. Tests print nothing (Checkstyle).
- A test is deleted when a legitimate change would rewrite its expected value, when a content rename breaks it, or
  when another test proves the same; it needs a replacement only when it guarded harm.
- The existing suite was pruned to this once, in bulk; named built-in content in older tests moves only when it
  breaks.

## Consequences
- A look regression is caught by eye, `PreviewBoard` or the live pass, not by `mvn verify`. A crash in a frame or a
  paint shows in play and in the log.
- Restyles, retunes and renames edit fewer tests.
- The kept frame-builder tests are the rules a look depends on: interpolation, what a hidden enemy shows, that
  drawing never takes from the game's random, a shield's state.

## Rejected
- *Testing by layer* (domain packages tested, `td.ui` only on plain values): simpler to apply, but it drops the
  hidden-information guards in the frame builders and keeps look tests in domain packages.
- *Sweeps over every face and state* (every colour valid, every frame builds): they catch crashes in rare looks, which
  decide nothing.
- *A deletion rule alone*: the cause was silence on what not to test, which a deletion rule leaves.
