# Feature Request: Effect Icons and Terms

## Summary

The enemy and tower panes have grown one text row per stat, trait, effect, immunity and
diminishing-returns line, and the text describes effects in loose verbs ("unravels its target",
"is Sundered", "gains Unraveled"). Give the game one vocabulary in the style of League of Legends
and World of Warcraft: every buff, debuff, trait and ability is a square icon with a hover
tooltip, every mention of one in text is a hoverable term (`applies [icon]Unravel to the enemy`),
and the panes drop to stats plus icon rows.

## Current state (what exists today)

- Every info pane is a `JTextPane` showing an AWT-free `InfoSheet` (lines: `EnemyHeader`, `Title`,
  `HealthBar`, `Row`, `Prose`, `Gap`), which `InfoSheetDocument` alone lays out; glyphs are
  `PaintedIcon`s painted by `Java2DFrameRenderer`. Nothing in the UI hovers inside a text pane.
- **Enemy pane** (`EnemyStatText`): speed, then every stat off its default, where a stat a trait
  explains takes that trait's colour and stands in for it; then traits no stat covers; on the live
  pane also one row per active effect ("Unraveled x3", "Burning"), per blocked kind ("Immune to
  freeze"), per diminishing-returns line ("Freeze DR: next 50%"), and a dodge count. The wave
  preview ends with a prose description.
- **Enemy abilities** (sprint when hurt, spawn on death, heal nearby…) appear only in that prose
  description; they have no rows and no player-facing names.
- **Tower pane** (`TowerSheetText`): stat rows (base → current), one text row per behaviour,
  jammed, aura, XP, kills. The placed-tower pane already leaves out what the Upgrades panel shows
  (owned nodes, as slot headers). The upgrade-node hover lists bonuses as rows.
- 47 `EffectKind`s, each with its own palette colour and an `EffectCategory`. `EffectCategory`
  doesn't say which side an effect helps: STEALTH holds both Invisible (good for the enemy) and
  Revealed (bad for it).
- The board marks effects above an enemy and traits below it as coloured diamonds; no per-kind
  symbol exists anywhere. No image assets exist; all art is vector.
- Effect descriptions exist only as Javadoc on `EffectKind`. Display names are adjectives built
  per row ("Unraveled", "Burning", "Frozen").
- All player-facing text is plain `String`: 123 `describe()` implementations (perks, procs,
  payloads), `BehaviourLine`, `UpgradeBonus`, `TraitLine`, enemy and tower descriptions. 8 test
  files assert on `describe()` output.
- Stats resolve by summing every contributor's modifiers anonymously (`StatSheet`); nothing
  records which source contributed what.
- An active effect's snapshot (`EnemyInspection.EffectState`) has its remaining ticks and stacks,
  not the duration it started with. Burn, Poison and Soulfire are decaying pools with no fixed
  maximum.
- The live enemy sheet is built on the game-loop thread with every frame, inside the frame-build
  budget (≤ 1 ms, ≤ 512 KB).

## What this feature adds

### Terms

- Every effect, enemy trait, enemy ability and named tower mechanic (Kinship, Stoke, Bracketing,
  Curse, …) is a **term**: one canonical name, one icon, one tooltip.
- Wherever text mentions a term it shows as a small icon followed by the name in the term's
  colour, no underline; it brightens on hover and opens the term's tooltip.
- **Wording rule:** text always uses the term's name, never a verb or adjective made from it —
  "applies Unravel", "an enemy with Burn", "immune to Freeze", "removes Chill".
- Damage types (physical, magic) and tower names stay plain coloured words, not terms.

### Icons

- A square, filled with a dark shade of the term's colour (the same colour the board's diamond
  uses), a border in the effect's category colour, and a symbol in the middle.
- Live state on the face: stack count bottom-right; time left as a clockwise dim sweep, plus a
  number under the icon below 10 s; a decaying pool (Burn, Poison, Soulfire) shows a bar along the
  bottom instead of a clock; an immunity is the effect's icon slashed.
- Symbols are vector pictograms. A term without its pictogram yet shows a two-letter monogram, so
  the feature can ship before all art exists; pictograms land in tranches, most-seen first.

### Tooltips

- Short, WoW style: the icon and name as a title, one sentence that includes the numbers, and on a
  live enemy the time left and stacks. The sentence's numbers come from the effect's own rules,
  so a balance change can't leave them stale.
- Diminishing returns are a line in the Freeze/Daze tooltip ("Next lasts 50%", "Next is
  blocked"). While diminishing returns still run after the effect ended, a faded icon stays in
  the Debuffs row so there is something to hover.
- An immunity's tooltip says "Immune to [Freeze]" and why (the trait, or the effect that blocks
  it).
- Opens ~150 ms after the pointer rests on a term, beside the term without covering it, leaning
  toward the board and kept inside the window.
- Sticky: the pointer can move into a tooltip and hover the terms inside it, chaining another
  tooltip beside it. Leaving them all closes them all. A chain never opens the same term twice.
- On a live pane, a tooltip belongs to its effect, not to a screen position: it keeps updating,
  stays put if the icon shifts, and shows a dimmed "Expired" if the effect ends while open.

### Enemy pane

```
◆ Golem                    ✦ Elite
[██████████ 420 / 420 ]        $12
 › Speed          22 px/s   (red)
 · Armor               40
 · Magic resist       -30   (green)
 · Regenerates        4/s
 Traits   [◇][↯][⊘❄]
 Buffs    [⛨]
 Debuffs  [❄][◆3][♨]
```

- **Stat rows** show the current value after every trait, buff, debuff and rank. A row shows
  when the stat's base or current value is off its default. The current value is green when it is
  better for the player than the enemy's base, red when worse. Regeneration is a stat row.
- Hovering a stat row opens a breakdown: the base, one line per trait or effect that changes it
  (as terms), and the current value.
- Traits no longer stand in for stat rows; they are icons.
- **Three icon rows**, each hidden while empty:
  - **Traits**: the enemy's permanent traits, its abilities, and its immunities (slashed).
  - **Buffs**: effects that help the enemy (Shield, Sprint, Heal, Invisibility…).
  - **Debuffs**: everything else towers put on it.
  - Within a row, icons keep the order they were gained; a new one appends.
- **Abilities** are icons on the Traits row. Their tooltip says the trigger and what happens, with
  terms ("Once, below 50% health: gains [Sprint] for 2 s").
- The description shrinks to one role sentence, or goes where the icons say it all.
- The wave-preview hover uses the same layout with base stats, the Traits row and the role
  sentence; it has no Buffs or Debuffs.

### Tower pane

```
▲ Seeker tower
 ○ Range             4.5
 ● Magic damage  30 → 39
 › Fire rate       0.8/s
 Abilities [⌖][☄]
 Applies   [◆][❄]
 ◆ XP         340 · Hero
 ☠ Kills  41 · 5120 dmg
```

- Stat rows stay as today (base → current).
- **Abilities**: one icon per piece of the tower's base kit (targeting, its built-in payload or
  proc, its aura); the tooltip holds the full text with terms. Behaviour text rows leave the pane.
- **Applies**: one icon per effect the tower can put on an enemy, counting its owned perks. Its
  tooltip is the effect's own tooltip, a divider, then one line per source on this tower saying
  how ("Arcane Warhead: 1 stack per impact, 2 if it has [Freeze] or [Chill]").
- Owned perks stay in the Upgrades panel; its slot headers become hoverable, showing the perk's
  text with terms. A perk's icon is its slot's pip in the tower's colour — no per-perk pictograms.
- The shop sheet (toolbar hover) has the same layout, with the price in the title and no XP or
  kills. The upgrade-node hover writes its bonuses as text with terms.

### Text pass

Every perk, proc, payload, behaviour, bonus, trait and description text is rewritten under the
wording rule, with every mention of a term as a term.

### Out of scope

- The board: enemies keep their diamonds; only the colours are shared with the pane icons.
- Tooltips for damage types, stats other than the breakdown, and tower names.

## Interconnections

- `FEATURE-enemy-info-rows.md` and `FEATURE-tower-info-rows.md` set today's one-row-per-thing
  pane rules; this request replaces their effect, trait and behaviour rows.
- `FEATURE-effect-visuals.md` set the board's marker colours, which the icons now share.
- The `experiment/sniper-scorcher-identity` branch (`docs/ideas/sniper-and-scorcher-identity.md`)
  is rewriting Sniper and Cinder perks; their texts join the text pass.

## Constraints and open risks

- The stat breakdown needs each source's contribution to a stat, which stat resolution doesn't
  keep today. It is only needed while a row is hovered, never per tick.
- Splitting Buffs from Debuffs needs a side for every effect kind; category alone can't decide it.
- The time sweep needs each effect's starting duration, which its snapshot lacks.
- A pool's bar needs a reference for "full"; Burn has no fixed maximum (its peak since applied is
  one candidate).
- Abilities and named mechanics have no player-facing names yet; naming them is content work.
- The live enemy sheet is built every frame: icon rows and term text must stay within the
  frame-build budget (`td.PerformanceHarness`).
- Tooltip numbers must come from the same rules the engine uses, never be typed twice.
- The engine stays headless: names and words belong to the domain, icons, colours and tooltips to
  the UI. UI uniformity: tooltips are the game's own painted panels with `Hud` borders, never the
  platform's tooltip or popup look.
- The text pass touches ~120 texts and the 8 test files that assert on them; keep that churn
  mechanical.
- Something should catch an effect name left as a bare word in text, or the wording rule erodes.

## Decisions made

- Everything above ships as one feature; the plan phases it.
- **Naming rule**: the one-word base form that reads after "applies"; a status word stays where
  no base form reads well. Proposed names, to confirm during planning:

  | Kind | Name | Kind | Name | Kind | Name |
  |---|---|---|---|---|---|
  | CHILL | Chill | RESONATING | Resonance | UNRAVELED | Unravel |
  | BURN | Burn | FRACTURED | Fracture | BRITTLE | Brittle |
  | FREEZE | Freeze | DAZED | Daze | TOLL | Toll |
  | SHIELD | Shield | SATURATED | Saturation | CORRODED | Corrode |
  | INVISIBLE | Invisibility | CHARGED | Charge | DRAGGED | Drag |
  | HEAL | Heal | DOOM | Doom | UNDERTOW | Undertow |
  | VULNERABLE | Vulnerable | BLIGHT | Blight | CONDEMNED | Condemn |
  | REVEALED | Reveal | CONTAGION | Contagion | STILLED | Still |
  | POISON | Poison | RIME | Rime | UNVEILED | Unveil |
  | SCORCHED | Scorch | ASH | Ash | DEAD_ZONE | Dead Zone |
  | SICKENED | Sicken | INVERSION | Inversion | FALLOUT | Fallout |
  | SUNDERED | Sunder | SYMPATHY | Sympathy | FRAIL | Frail |
  | EXPOSED | Expose | RECKONING | Reckoning | CRACKED | Crack |
  | MARKED | Mark | SILENCED | Silence | TARRED | Tar |
  | PRIORITY | Priority | ANCHORED | Anchor | BLEEDING | Bleed |
  | SOULFIRE | Soulfire | SPRINTING | Sprint | | |

- Board markers are out of scope.
- Per-perk pictograms are out of scope; perks use their slot pip.

## Open questions

- Category border colours: one colour per `EffectCategory`, or fewer groups (control, damage,
  weakness, hex, mark) so the eye learns fewer colours?
- Do tower-side buffs (Aura, Ley Line, Jammed) also become icons on the tower pane, or stay rows?
- Names for every enemy ability and every named tower mechanic.
- Which effects get the first pictogram tranche.
