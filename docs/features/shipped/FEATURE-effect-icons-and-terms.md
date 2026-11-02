# Feature Request: Effect Icons and Terms

## Summary

The enemy and tower panes have grown one text row per stat, trait, effect, immunity and
diminishing-returns line, and the text describes effects in loose verbs ("unravels its target",
"is Sundered", "gains Unraveled"). Give the game one vocabulary in the style of League of Legends
and World of Warcraft: every buff, debuff, trait and ability is a square icon with a hover
tooltip, every mention of one in text is a hoverable term (`applies [icon]Unravel to the enemy`),
and the panes drop to stats plus icon rows. Towers get the same vocabulary: every upgrade is a perk,
and what a tower carries during a fight (Steady Aim, a primed shot, an Aura's buff, a jam) is a
tower buff or debuff with its own icon.

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
- **Upgrade nodes** are data (`UpgradeNode` with `NodeEffects`: stats, procs, hit bonuses, targeting,
  capabilities); a node may also bring code `Perk`s through a `PerkCatalogue` keyed by node id, and
  `Tower.bonusesOf(node)` merges both into one description. In code, "perk" means a mechanic's
  implementation, which only some nodes have. `after(previous)` is only a purchase requirement, not
  a structural link; a node's line is known from its `UpgradeTier` step and `UpgradeTree.keyOf`.
- **Tower-side states** come in three shapes, none shown today except a "Jammed" row and an aura
  count:
  - `TowerStatus`: a buff for a time or for the next shots — Frenzy, Momentum's charge and haste,
    Patience, Silver Rounds' and Fourth Shot's primed shot, the Aura's Rally. Different ids stack,
    so two primed shots land on the same shot.
  - Counters and rhythms kept in their own classes: Steady Aim (`AimFocus`), Tradecraft's crit
    streak, the Seeker's focus, the Mortar's shell cycle and Bracketing, the Seeker's payload cycle,
    Event Horizon's widening.
  - Received from outside: Aura emissions, Ley Line hosting and casting, enemy disruption.
  - Stoke is the Cinder's private memory per enemy (its `Burning` ledger); the enemy doesn't carry
    it.
- **Steady Aim** is kept across targets and kills; a stack fades 3 s after the last shot, one at a
  time, counting time since the last shot (so a slowed or jammed Sniper bleeds stacks mid-fight).
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
- Perk art: one pictogram per line, shared by its nodes and told apart by the numeral; each head
  IV-A/IV-B choice and each special has its own. About ten per tower, plus the Range and Attune
  lines shared by every tower.

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

The tower pane mirrors the enemy pane.

```
▲ Sniper tower
 ○ Range               4.5
 ● Damage          40 → 56
 › Fire rate         0.4/s
 Perks    [▲II][◎ Attune][⌖I Marksman's Eye][✦ Fourth Shot 3/4][✧III Silver Rounds 2/3]
 Buffs    [◎ Steady Aim x3 2.1s][✧ Silvered][❀ Aura]
 Debuffs  [⚡ Jammed]
 Applies  [◆ Vulnerable]
 ◆ XP           340 · Hero
 ☠ Kills    41 · 5120 dmg
```

- Stat rows stay as today (base → current). Behaviour text rows leave the pane.
- **Perks**: every owned upgrade node is a perk, named after its node, its tooltip the node's
  text. The row shows one icon per **line** (the Range line, Attune → Awaken → Transcendent, a head
  chain, the extra line), as its furthest owned node with a level numeral; the tooltip lists the
  line's owned nodes. Each special is its own icon.
- A perk that counts toward something shows its progress on its icon ("3/4").
- **Buffs** and **Debuffs**: everything a tower carries that changes its next attack or its stats
  and can change mid-fight, with the same face as an enemy effect (stacks, time sweep):
  - a running state: Steady Aim, Tradecraft's crit streak, the Seeker's focus, Frenzy, Momentum's
    haste, Event Horizon's widening, Bracketing;
  - a **primed shot**: when a perk fires, it grants a separately named buff that the shot carrying
    it spends (Fourth Shot → Deadeye Round, Silver Rounds → Silvered, Momentum → Charged Shot). The
    Nth shot is the one that carries it: "every 4th shot is a [Deadeye Round]". Two primed buffs on
    one shot are both spent;
  - received: the Aura's buff, Rally, a Ley Line, and Jammed as a debuff, each named after its
    source.
- **Applies**: one icon per effect the tower can put on an enemy, counting its owned perks. Its
  tooltip is the effect's own tooltip, a divider, then one line per source on this tower saying
  how ("Arcane Warhead: 1 stack per impact, 2 if it has [Freeze] or [Chill]"). A tower that buffs
  others (Aura, Obelisk) lists the tower buffs it gives here too.
- **Perk text says when, the buff says what**: "Every 4th shot is a [Deadeye Round]." The buff's
  tooltip holds the numbers ("Guaranteed crit, +150% crit damage. Spent by the shot."). Stat changes
  the node itself makes stay as value rows. Each number lives in one place.
- Memory a tower keeps about one enemy shows **on that enemy**: Stoke is a debuff "Stoked" on the
  enemy. When several towers hold such a mark, the enemy shows one icon with the highest value, and
  its tooltip lists each source tower.
- The shop sheet (toolbar hover) has the same layout, with the price in the title and no XP or
  kills.

### Upgrades panel

- Shows only what can be bought: one button per offered node, with its perk icon, name and price.
  Hovering one shows the perk tooltip it would grant, its stat changes as value rows, and for a
  line's next level what changes from the current one.
- Slot headers shrink to the slot name and why it is locked ("Special: needs Awaken"). Owned perks
  are the Perks row; "chosen over X" moves into the owned perk's tooltip.

### Text pass

Every perk, proc, payload, behaviour, bonus, trait and description text is rewritten under the
wording rule, with every mention of a term as a term.

### Out of scope

- The board: enemies keep their diamonds; only the colours are shared with the pane icons. Towers
  show no buff icons on the board; their slot and rank pips stay.
- Tooltips for damage types, stats other than the breakdown, and tower names.

## Interconnections

- `FEATURE-enemy-info-rows.md` and `FEATURE-tower-info-rows.md` set today's one-row-per-thing
  pane rules; this request replaces their effect, trait and behaviour rows.
- `FEATURE-effect-visuals.md` set the board's marker colours, which the icons now share.
- `docs/ideas/sniper-and-scorcher-identity.md` decided the Sniper's Steady Aim rule and the Fourth
  Shot rename this request displays.
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
- "Perk" means two things: in code a mechanic's implementation (only some nodes have one), for the
  player every owned node. The two will drift apart unless one is renamed.
- Lines are not explicit data; grouping a tower's perks by line has to come from what the tree
  already knows (tier steps, key numbers), not from per-node rules.
- Tower buffs, counters and per-enemy marks live in several classes today (`ActiveTowerStatuses`,
  `AimFocus`, trackers, ledgers); the pane needs one snapshot of them per tower, built where the
  frame is built.
- Stoke's per-tower memory has to reach the enemy's inspection, which today knows only effects the
  enemy carries.
- A primed buff and its perk are two terms that must stay in step: the perk's progress and the
  buff's appearance come from the same counter.

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

- Board markers are out of scope, for enemies and towers.
- Every upgrade node is a perk (not only nodes that add a mechanic): it maps 1:1 onto how nodes are
  modelled and needs no rule for what counts as a mechanic.
- A primed bonus is its own named buff; the perk shows progress toward it.
- Proposed names for the new tower terms, to confirm during planning: Steady Aim, Crit Streak,
  Focus (Seeker), Frenzy, Haste, Charged Shot (Momentum), Patience, Deadeye Round (Fourth Shot),
  Silvered (Silver Rounds), Bracketed, Nuke Shell (Mortar's cycle), Event Horizon, Rally, Aura,
  Ley Line, Jammed, Stoked.

## Open questions

- Category border colours: one colour per `EffectCategory`, or fewer groups (control, damage,
  weakness, hex, mark) so the eye learns fewer colours?
- Names for every enemy ability, every named tower mechanic, and every primed buff not named
  above (the Seeker's payloads, the Mortar's other shell types).
- Which effects get the first pictogram tranche.
