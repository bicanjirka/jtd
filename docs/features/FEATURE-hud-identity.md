# Feature Request: HUD identity

> **Status: draft, for later.** Options, as real screenshots of the game:
> <https://claude.ai/artifact/29BfwEsGcojNEeP4UC7vp8>. Pick one before planning.

## Summary

The interface should stop looking like a green console from an earlier version of the game and sit calmly beside the
board, so that the colours on screen are the towers', the enemies' and the levels'.

This is about identity, not a defect: HUD text already reads at 8:1 or better on black.

## Current state (what exists today)

- **The neutrals are green.** `ui/CLAUDE.md` states it: "Neutrals are `Hud`'s green-tinted greys everywhere". About 50
  green-tinted literals outside the board painter sit across `Hud`, `HudTextField`, `InfoSheetDocument`,
  `UpgradeTreePainter`, `TooltipPanel`, `ConfirmPopup`, `SuggestionList`, `RowLook`, `PanelLevelSelect`, `PanelDev`,
  `PanelTowerInfo`, `TermIconPainter` and `BoardOverlays`, plus a few cell colours in `Java2DFrameRenderer`.
- **Black is scattered too:** `Hud.BACKGROUND` is one of several places; `TowerDefense`, `PanelHeader`, `SheetPane`,
  `PanelTowerInfo`, `PanelGameConsole`, `PanelUpgradeTree`, `PathWaveRow`, `PanelWaveInfo` and `GameBoard` set their own.
- **Green has other jobs.** It is the Sniper's colour, the "better" colour (`InfoSheetDocument.BETTER_COLOR`), the Info
  pane's bar fill, and the upgrade arrow in `FEATURE-tower-visual-revamp.md`.
- **Level cards ignore their level.** `PanelLevelSelect.ACCENTS` is one list of colours by position: mostly pure AWT
  colours. The Amber Hive's card is neon green around a honey lane, and The Storm Ridge's is pure red around blue and
  yellow lanes. Only The Labyrinth's matches.
- **Each level already has colours of its own:** lane colours (`PathColor`) and a ground (`BoardGround`).

## What this feature adds

- One neutral HUD on every panel, control, tooltip and popup, in the option chosen below.
- Level cards coloured from their own level.
- **A colour rule:** a problem has a colour; good news needs none. A met gate or an improvement is plain text with its
  ✓ or →, which frees green to mean the Sniper and the revamp's upgrade arrow.

### The options

| Option | Background | Neutral ramp | Good at | Costs |
|---|---|---|---|---|
| ⭐ Graphite | `#0D0E10` | no tint | Neutral beside all six floors; towers and enemies become the only colour on screen | Plainest, little character of its own |
| Ink | `#0A0E14` | cool, blue-grey | Clean, some identity | Blue is the magic damage and chill colour, and is close to The Storm Ridge's floor |
| Ash | `#100E0B` | warm grey | Soft and calm | Leans toward the warm levels, away from the cold ones |
| Level tint | The level's floor hue | tinted per level | HUD and board read as one, each level feels different | Interface colours change at runtime; the board edge almost disappears |

The mocks map today's greys onto each ramp by brightness. Treat those as a starting point, not final values.

### Out of scope

- Enemies, effects and the board's floors: `FEATURE-enemy-and-effect-visuals.md`.
- Layout: panels stay where they are.
- Tower and enemy identity colours.
- A name for the game. "Tower Defense" could change at any time, separately.
- Custom fonts, glow and animated backgrounds.

## Interconnections

- **`FEATURE-enemy-and-effect-visuals.md`** ships first. The mocks here already show its darker board and small badge.
- **`FEATURE-tower-visual-revamp.md`** keeps the towers' own colours and adds a green upgrade arrow, which the colour
  rule leaves room for.

## Constraints and open risks

- **One look on every OS** (`ui/CLAUDE.md`): every control paints itself; no platform look-and-feel.
- **Term and stat colours stay meaningful** on the new background: `TermLooks`, `StatLooks` and the damage colours were
  chosen against black, and a small rise in the background lowers their contrast slightly.
- **The Level tint option is the odd one structurally:** today's HUD colours are constants, and it would make them
  follow the loaded level. The requirement "one component, no mode flags" applies.
- **`ui/CLAUDE.md`'s "green-tinted greys" line** becomes false and changes in the same commit.
- A panel or control change is verified from one `run-jtd` screenshot.

## Decisions made

- Split from the enemy and effect work, and done after it.
- The space theme is dropped: no indigo, no renames, no nebula. Levels already have identities of their own, and it
  would fight them.

## Open questions

1. **Which option?** ⭐ Graphite. Level tint is the most interesting alternative if the runtime colour cost is acceptable.
2. **Which lane colours a card when a level has several?** ⭐ One chosen per level so that no two cards look alike. The
   mock uses honey, thunder blue, glass, rush orange, violet and stone. Three warm levels would otherwise look the same.

---

*After planning and implementation, update this document rather than deleting it: mark it implemented, prune resolved
open questions, and move it to `docs/features/shipped/`.*
