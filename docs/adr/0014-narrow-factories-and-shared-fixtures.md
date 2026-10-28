# 0014. Wide records grow through narrow factories; tests share fixtures

- Status: Accepted
- Date: 2026-09-19

## Context
A review of recent feature commits found that a large share of the work was editing tests the
feature had nothing to do with. In the ten heaviest commits, about two fifths of the test diff was
mechanical (the worst cases: `a675939` and `61dd95e`, whose test diffs were several times their
production diffs): a call site rewritten because a constructor grew an argument, or because something was
renamed. Two causes:
- **Wide positional records with no factory.** One enemy definition was built from a long literal
  in every test. Records that already had a narrow factory plus `withX` copies absorbed the same
  feature change with no call-site edits.
- **No shared fixture layer.** Many test files built a world, a board scale and the same helpers by
  hand, so each copy was a separate edit.

## Decision
- A record with five or more components has a narrow factory and grows by `withX` copies, never a
  wider constructor. Checkstyle requires the factory.
- Tests build wide records through the factory or `td.fixtures` (`WorldFixtures`, `BoardFixtures`,
  `LevelFixtures`, `TowerFixtures`, `EnemyFixtures`), never a full positional literal, except in
  that record's own test. A helper a second package needs moves to `td.fixtures`.
- All tests moved onto the fixtures at once, not opportunistically.
- `td.ui.render`'s draw records are exempt. Each is built once per frame by its own frame builder,
  so there are no scattered call sites for a factory to protect, and a factory on a single-caller
  record is abstraction with no payoff. The exemption follows the package boundary the AWT ban
  already draws.

## Consequences
- A new record component is a change in one place.
- The check against wide literals in tests was later dropped (0012), so that half is held by
  review, not by a tool.

## Rejected
- *Migrating fixtures only when a feature touches a test.* A half-migrated suite leaves a new test
  unable to tell which copy to extend.
- *Fixing only the widest records.* A wide factory standing in for a narrow one was the same
  problem, one level removed.
- *An advisory report instead of a failing check.* Later superseded in part by 0012, which accepts
  prose for what a standard tool can't check.
