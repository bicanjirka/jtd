# Feature Request: Waves panel

**Status: implemented.** `WavesReadout` holds the panel's words and when each part shows; `PanelEnemy` is the shared
strip. As built: a mob stands on a baseline over its count and its body is capped at a third of the mob line, so its
rank badge always fits inside the strip.

## Summary

Restyle the "Current & Next Wave" sidebar panel to match the HUD's newer look: the round skulls of the header
strip, the tower bar's price-under-icon counts and key chips. It should also show what the player can do between
rounds. The dev panel's wave-script preview is the same preview and changes with it.

1:1 mock of the chosen design (option B) beside today's panel, and the dev Spawn row:
https://claude.ai/artifact/H9QfzpaA5XpsNzRA85Xx6a

## Current state (what exists today)

- `PanelWaveInfo` is titled "Current & Next Wave". It holds two outlined sections, each with a bare round number
  over one `PathWaveRow` per path. A row is a path chevron (only when the level has more than one path) and a
  `PanelEnemy` strip.
- `PanelEnemy` stretches its mobs to `min(width / types, height)`, so one type draws at 40 px and six at about
  30 px. It draws each count in pink plain text at 30% of the mob's size, under the mob's corner. Preview mobs
  carry their rank badge and a unique's halo, and each has a hover tooltip (`spotAt`).
- `TowerDefense.setWavePreview` fills the panel from `GameEngine.waveProgress()` at level start, when a round
  starts, and on dev skips and jumps. `current` is the last round started, so it stays filled after its enemies
  are dead, and is empty only before round 1. `next` is empty during the last round.
- A round can be sent only once the previous one is cleared: `waveReady` turns true when no enemy is alive.
  `TowerDefense.doTick` already notices that flip and posts `syncTransportButtons`.
- The header's wave status draws a skull per round, with art and tone by its strongest enemy and by state
  (`RoundSkulls`, `RoundStrip`). A finished round's skull is cracked, in a cleared or leaked tone. The current one
  pulses.
- The console is 300 px wide (`PanelGameConsole.WIDTH`). The panel is docked at the bottom under the Info pane, so
  a change in its height moves the Info pane's bottom edge.
- The dev panel's Spawn row shows the script's wave in a `PathWaveRow` inside a 34 px card. The card switches to
  red problem text for a bad script, and gets about 230 px of room. Its help text says the preview can be hovered
  to inspect.
- Space sends a ready round (`GameKey`). The speed strip shows it as a key chip.

## What this feature adds

### The panel

- One box titled **Waves**, with no nested outlines. NOW sits above NEXT, split by a faint rule.
- Each round's heading is its skull (the header's art and tone for that round, static), then **Round 7** in bold
  and a dim **/20**. A small dim caption, **NOW** or **NEXT**, sits at the right.
- Each enemy type is a bare mob, 24 px, with no frame, in a 32 px slot. Its count, **×14**, sits under it in the
  tower bar's price font and the HUD's text colour. When a path's types don't fit on one line, every slot shrinks
  evenly. Nothing wraps or scrolls, and the panel's height never depends on how many types a wave has.
- An enemy's rank shows only through the mob's own badge or halo.
- With more than one path, each path's row starts with its chevron in the path colour, as today.
- Hovering an enemy opens its tooltip, as today.

### Between rounds and at the ends

- Once a round is cleared, NOW is one line: the round's cracked skull and **Round 3 cleared** or **Round 3
  leaked**, in the header's tone for that state. Under it, **Send round 4** with the Space key chip.
- Before round 1, NOW reads **Ready**, with no skull, over **Send round 1** and the chip.
- The Send line shows only while a next round exists and the level isn't over. It is a prompt, not a control.
  Sending stays with Space and the speed strip.
- During the last round NEXT is hidden, since the heading's **/20** already says it is the last.
- When a round ends the panel switches to its between-rounds line at once, not when the next round starts.

### The dev panel's preview

- The Spawn row's preview is the same preview as the panel's path row, with the same slots, counts, shrinking,
  chevron and tooltips. It keeps its 34 px card and its problem text.

### Out of scope

- How many of each enemy are left in the current round, as a count (5/12) or a progress line.
- A pulsing current skull, or a round tooltip on the heading's skull.
- A clickable Send control.

## Interconnections

- `FEATURE-wave-preview-cleanup.md` (implemented) gave the panel its two outlined sections and the chevron-only
  path marker. This request replaces the sections and keeps the marker.
- `FEATURE-key-hints-and-hud-unification.md` set the key chip look and the Space binding the Send line shows.

## Constraints and open risks

- The Space chip must read the binding Space runs, never a copy of it (`td/ui/CLAUDE.md`).
- The skull must stay the header's own art and tone for the same round, so the two never disagree.
- The panel refreshes on the EDT. Tick code never touches Swing.
- A 24 px mob plus its count line is exactly the dev card's 34 px height. Anything taller grows the dev panel.
- The board's enemy art is painted through the frame pipeline. The preview must keep reusing it, not draw its own.
- The run-jtd Driver addresses components by index. A change in the panel's component tree may shift what it
  sees.
- Standing requirements: one look on every OS, borders from `Hud`, the engine headless.

## Decisions made

- Option B from the mock, without the rounded tile borders.
- One shared preview for the Waves panel and the dev Spawn row: one component and one overflow behaviour
  (shrink), with no copy and no per-owner mode.
- Shrink rather than wrap, so heights stay fixed.
- Rank cue: the mob's badge only.
- Wording: "Waves", "NOW"/"NEXT", "Round 7 /20", "×14", "Round 3 cleared"/"leaked", "Ready", "Send round 4".
- Remaining counts are dropped, not recorded as a gap.

## Open questions

None.
