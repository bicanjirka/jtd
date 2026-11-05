# Feature Request: HUD identity

**Status: implemented.** The review and the options, as real screenshots of the game:
<https://claude.ai/artifact/29BfwEsGcojNEeP4UC7vp8>. The colours, values and rules: [`docs/UI.md`](../../UI.md).

## Summary

The interface should stop looking like a green console from an earlier version of the game and sit calmly beside the
board, so that the colours on screen are the towers', the enemies' and the levels'. It should also make each level feel
like a place from the moment it is chosen.

This is about identity, not a defect: the HUD's main and secondary text already reads at 8:1 or better on black, and it
keeps doing so.

## Current state (what exists today)

- **The neutrals are green.** `ui/CLAUDE.md` states it: "Neutrals are `Hud`'s green-tinted greys everywhere". About 50
  green-tinted literals outside the board painter sit across `Hud`, `HudTextField`, `InfoSheetDocument`,
  `UpgradeTreePainter`, `TooltipPanel`, `ConfirmPopup`, `SuggestionList`, `RowLook`, `PanelLevelSelect`, `PanelDev`,
  `PanelTowerInfo`, `TermIconPainter` and `BoardOverlays`.
- **Interface colours in the board painter's palette are green too.** `Java2DFrameRenderer` holds the credits icon, the
  boon and tower-buff term frames, the mechanic and status term frames, the wave, current-round, cleared-round and
  base-perk marks, the met upgrade gate, and the dev grid, blocked-cell and unmarked-glyph greys.
- **Black is scattered too:** `Hud.BACKGROUND` is one of several places; `TowerDefense`, `PanelHeader`, `SheetPane`,
  `PanelTowerInfo`, `PanelGameConsole`, `PanelUpgradeTree`, `PathWaveRow`, `PanelWaveInfo` and `GameBoard` set their own.
- **Green has other jobs.** It is the Sniper's colour, healing's and regeneration's, the "better" colour
  (`InfoSheetDocument.BETTER_COLOR`), the Info pane's bar fill, the XP bar's fill, the Base upgrade slot's colour on the
  tree and on the tower's pips, and the upgrade arrow in `FEATURE-tower-visual-revamp.md`.
- **Level cards ignore their level.** `PanelLevelSelect.ACCENTS` is one list of colours by position: mostly pure AWT
  colours. The Amber Hive's card is neon green around a honey lane, and The Storm Ridge's is pure red around blue and
  yellow lanes. Only The Labyrinth's matches. Every card's preview sits on the same dark dotted grid.
- **Each level already has colours of its own:** lane colours (`PathColor`) and a ground (`BoardGround`).

## What this feature adds

- **One neutral HUD, Graphite,** on every panel, control, tooltip and popup: background `#0D0E10`, the ramp in
  `docs/UI.md`. Each grey reads as well on the new background as today's does on black, and main text stays at 16:1.
- **Level cards from their own level:**
  - Each card takes one lane's colour, chosen so that no two look alike: honey, thunder blue, glass, rush orange, violet
    and stone.
  - Its preview sits on the level's real floor and motif instead of a shared grid.
  - The lane colour draws the border and the lanes. The card's name and numbers use a lighter tint where the lane colour
    falls below 7:1 (Storm Ridge, Iron Crossing, The Rift).
- **A colour rule:** a problem has a colour; good news needs none. A met gate or an improvement is plain text with its
  ✓ or →.
- **No green left in the chrome:**
  - The credits icon, the boon and tower-buff term frames, the XP bar, the Info pane's bars and the met gate become
    neutral.
  - The Base upgrade slot becomes near-white (`#E1E1E6`) on the tree and on the tower. Core, Head and Special keep their
    blue, amber and violet.
- **Green stays as an identity:** the Sniper, healing and regeneration, and the revamp's upgrade arrow.
- **The loaded level gives the HUD one colour, in four small places:** the game title, the panel titles, the selected
  speed button's border and the XP bar take the level's card colour, in its text tint. Every panel stays Graphite, and
  the card's colour carries on into the game.

### Out of scope

- Enemies, effects and the board's floors. A green enemy, poison mark or healing zone is that thing's own colour and
  stays.
- Layout: panels stay where they are.
- Tower and enemy identity colours, except the Base slot above. The pure primaries go to the revamp (see below).
- A name for the game. "Tower Defense" could change at any time, separately.
- Custom fonts, glow and animated backgrounds.

## Interconnections

- **`docs/UI.md`** is the palette later features choose from.
- **`FEATURE-tower-visual-revamp.md`** keeps the towers' own colours and adds a green upgrade arrow, which the colour
  rule leaves room for. It also removes the slot pips, after which the Base slot's colour appears only on the tree. With
  the chrome neutral, four towers' pure primaries (Sniper, Burst, Sonar, Pulse) become the loudest colours on screen.
  They belong in the revamp, which redraws those towers anyway, and softening them needs care: see `docs/UI.md` section 5.

## Constraints and open risks

- **One look on every OS** (`ui/CLAUDE.md`): every control paints itself; no platform look-and-feel.
- **Term and stat colours stay meaningful** on the new background: `TermLooks`, `StatLooks` and the damage colours were
  chosen against black, and they lose up to about a tenth of their contrast (physical 8.8:1 to 8.1:1, problem 7.6:1 to
  7.0:1).
- **Neutral term frames** have to stay legible as groups. On the enemy sheet they do, because its rows are labelled and
  its debuffs keep their colours. Boon, tower-buff, mechanic, trait and status frames now differ by little else.
- **Text this makes false changes in the same commit:** `ui/CLAUDE.md`'s "green-tinted greys" line, the `README.md` line
  saying a changed value is green or red, and the note at the top of `docs/UI.md`.
- A panel or control change is verified from one `run-jtd` screenshot.

## Decisions made

- **Graphite** over Ink, Ash and Level tint. It suits every level because it belongs to none of them. Blue would clash
  with the magic damage and chill colour, warm greys lean toward the warm levels, and Level tint lets the board edge
  almost disappear.
- **Text stays as readable as today.** The first mock lost contrast on dim text (6.6:1), because it averaged the colour
  channels. Each grey now keeps today's contrast.
- **Level card colours** as listed above: a choice per level, because three warm levels would otherwise look the same.
- **The remaining green goes from the chrome,** including the Base slot's identity colour.
- **The level gives the HUD a voice.** Graphite alone removes the console but says nothing of its own. These four colours
  follow the loaded level rather than being constants, unlike every other HUD colour. Chosen while implementing, as the
  recommended answer to the one open question: reverting its commit returns to a fully constant HUD.
- **A card's colour must be one of its lanes',** which the level record enforces. The text tint is computed, so its values
  differ a little from the first mock's.
- Split from the enemy and effect work, and done after it.
- The space theme is dropped: no indigo, no renames, no nebula. Levels already have identities of their own, and it
  would fight them.
