# 0002. The engine is headless; Swing stays in `TowerDefense` and `td.ui`

- Status: Accepted
- Date: 2026-09-17 (recorded; the refactor came earlier)

## Context
`TowerDefense` began as one `JFrame` that owned the window, the game state, input handling and
painting. Every gameplay rule lived in a Swing class, so no rule could be tested without a display.

## Decision
- `GameEngine`, `GameWorld` and the domain packages own all game state and input semantics. They
  never construct a window or touch `Graphics2D`, and can be built, driven and asserted on from a
  test.
- `TowerDefense` and `td.ui` own presentation: layout, painting, mouse and key handling, and
  translating screen coordinates into board-relative pixels for the engine.
- `GameHost` is the engine's only channel back to the UI. `GameHost.noOp()` serves worlds with no
  display (previews, tests, the balance harness), so no caller passes `null`.
- The engine decides the end of a wave and of a level at the end of each tick, and every host polls
  it, so the window, tests and the balance harness all play by the same rules.

## Consequences
- `GameEngineTest` is the integration surface for gameplay rules, not a UI-driven harness.
- `TowerDefense` is the one class deliberately on both sides. A gameplay rule placed there can't be
  tested, which is the reason the boundary exists.
- Checkstyle bans AWT and Swing outside `td.ui`, `Main` and `TowerDefense`.

## Rejected
- *Testing gameplay through the window.* That's slow and needs a display, and it couples every rule
  to layout.
