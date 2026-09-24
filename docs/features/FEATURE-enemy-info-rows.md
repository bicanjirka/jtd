# Feature Request: Enemy info as rows

**Status: implemented** in `a6729a0` (row content) and `00e146c` (glyphs, colours, health bar),
as decided below. It closed the `TODO.md` entry "A long enemy stat block overflows the Info
panel". `FEATURE-tower-info-rows.md` asks for the same treatment for towers.

## Summary

The enemy text in the Info panel is too long for the pane, repeats itself, and looks nothing
like the board it describes. Rework it so every stat, trait and effect is one line, marked with
the same glyph and colour the board draws next to the enemy, so the panel fits and doubles as a
legend for the board's markers.

## Current state (what exists today)

- **Two enemy views share the Info pane.** The wave-preview hover (`EnemyInfoText` →
  `EnemyStatText.preview`) reaches the pane through `GameWorld.setInfoText` →
  `GameHost.setInfoText`. The live inspector (`EnemyStatText.live`) is built with each frame and
  travels in `RenderFrame.enemyInspectionText`. Both are plain `String`s shown in a `JTextPane`.
- **The pane** is about 190 × 210 px at the default window size, 13 lines of 12 px text.
- **Measured in the live game:** an Armored boss needs 16 visual lines. The pane opens scrolled
  to the bottom, so the name is out of view. `TODO.md` records this as "A long enemy stat block
  overflows the Info panel".
- **Every trait is said twice.** A trait changes a mob only through stat modifiers, so "Resists
  50% of all damage" also appears as the Armor line *and* the Magic resist line. "Immune to
  critical hits" repeats "Resilience 100", and the plating trait repeats the plating stat. On the
  Armored boss, 3 concepts take 6 lines.
- **Headers and spacers** ("Traits:", "Effects:", blank lines) take 4 to 5 lines. Lines that give
  both the internal number and its meaning ("Resilience 100 (immune to crits)", "Plating:
  shrugs off 10 of physical hit") wrap onto a second line.
- **The pane keeps its old scroll position** when its text is replaced, and its scrollbar is the
  only control on the HUD still drawn by the platform look-and-feel.
- **The board already has a glyph for everything the panel describes:**
  - a hollow diamond per trait below the enemy, one colour per `TraitMarker`;
  - a filled diamond per active effect above it, one colour per effect kind;
  - a rank badge (chevrons, star, skull);
  - the body shape itself.

  All of these are painted by `Java2DFrameRenderer`, keyed by `Palette`.
- **Traits describe themselves** as full sentences (`TraitTemplate.describe()`). An adaptive
  resist describes the most it can reach, because the preview can't know the level's damage mix.

## What this feature adds

- **One row per concept.** Each row has a glyph, a label and a right-aligned value, and fits on
  one line.
- **A header row:** the enemy's body shape, its name in bold, and its rank badge with the rank
  name on the right.
- **A health bar** with the numbers written across it and the bounty on the right. In the live
  view, a killed or leaked enemy's bar dims and says "Killed" or "Leaked" in red.
- **Rows use the board's glyphs:**
  - A **trait** row starts with a hollow diamond in the trait's board colour, and its label is in
    that colour.
  - A **timed effect** row starts with a filled diamond in the effect's colour and shows the time
    left.
  - A **stat that no board marker explains** gets a small neutral dot. Speed gets a chevron.
- **No duplicates.** A trait and the stats it sets are one row ("Resist all −50%"). A trait whose
  stats show nothing, such as hurt speed or an adaptive resist not yet resolved, keeps its own
  row.
- **Meaning, not internal numbers:** "−50%", not "Armor 100 (−50% physical)". Armor and magic
  resist merge into "Resist all" when they're equal. Plating merges the same way.
- **Order:** identity, health, speed, other stats and traits, then timed effects. In the preview
  the description comes last, dimmed. The live view leaves it out.
- **Every Info-pane text opens from the top**, not just enemy text.

**Out of scope:**
- Countdown bars on effects.
- A restyled scrollbar.
- Tower text (now `FEATURE-tower-info-rows.md`).
- The effect-marker overflow count (its own `TODO.md` entry).

## Interconnections

- **`FEATURE-enemy-stats.md`** introduced the stat sheet and the inspection snapshot this feature
  displays.
- **`FEATURE-enemy-traits-and-effects.md`** introduced the trait and effect markers whose glyphs
  and colours the rows reuse.
- **`FEATURE-tower-info-rows.md`** asks for the same treatment for towers.

## Constraints and open risks

- **Headless engine and `GameHost`.** The engine never touches Swing, and `GameHost` is its only
  channel to the UI, never widened speculatively. The wave-preview hover is a UI-to-UI path that
  happens to go through `GameHost` today.
- **Threading.** The live inspector's content is built on the `game-loop` thread with the frame
  and must stay AWT-free. The EDT only shows it.
- **UI uniformity** (standing requirement): glyphs must look the same on every OS, so a font's
  fallback glyphs (◇/◆, ☠) are a risk. There is still no font glyph for a skull badge or a square
  body.
- **Legibility.** Some board colours are dim as text on black, such as plating violet and slow
  blue.
- **Space.** The worst built-in case, a live Armored boss with effects, must fit 13 lines.

## Decisions made

- **Drawing:** option B, glyphs painted by the board's renderer inline in the text pane, combined
  with option A's coloured labels. The rejected alternatives were A alone (font glyphs only) and
  C (a custom-painted card).
- **Health** is a bar in both views.
- **No countdown bars** on timed effects.
- **Rank-given stats** that no trait explains (for example an elite's or boss's freeze diminishing
  returns) show a neutral dot, because the board draws no marker for them.
- **Every Info-pane text scrolls to the top** when it's replaced, tower status included.

## Open questions

1. Plating violet is the hardest label to read. Should it change, even though that changes the
   board marker too?
2. Stats an effect causes, such as "Crit chance taken ×2" while burning, show a plain dot.
   Should they take the causing effect's colour instead?
3. A long name at a long rank name, such as "Frenzy Spawnling" at Veteran, may wrap the header
   row. Should the rank name be dropped when space runs out, leaving the badge alone?
