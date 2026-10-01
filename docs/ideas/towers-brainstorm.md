# Tower Design Brainstorm

A big idea pool for towers, effects, debuffs and upgrades, grounded in what the engine can do
today. Nothing here is decided. It is meant to be cut down: tick what you like, leave the rest,
and write on the 💬 lines.

**How to use it**

- `[x]` = I like it, keep it. `[ ]` = no, or not yet. Write anything after a 💬.
- ⭐ = my recommendation where several ideas compete for the same slot.
- Cost tags, so you can see what an idea drags in:
  - 🟢 existing primitives only: a number, a `TowerBuff` axis, a hook or query that exists.
  - 🟡 one new small primitive: an effect kind, a gate, a selector, a projectile variant.
  - 🔴 a new shared system (ground zones, hexes, knockback, magazine). Expensive once, then
    cheap for every idea that reuses it.
- Numbers are placeholders that make an idea concrete, not balance proposals. Seconds assume 20
  ticks per second.
- Trees follow `FEATURE-tower-upgrades-iteration-2.md`: BASE (Range I-III, Fortify -> Awaken ->
  Transcendent), HEAD (two exclusive chains I -> II -> III -> IV-A | IV-B, plus one extra
  non-exclusive node I-IV), SPECIAL (slot 1, slot 2 after Transcendent). Aura caps at Awaken.
- In a hurry? Section 8 is my shortlist: the ideas I'd build if I could pick only those.
- Section 9 challenges what already exists: base stats (with computed damage per credit),
  every shipped upgrade node, and the names.

Contents: 0 What the code says · 1 The big picture · 2 Shared mechanics · 3 Towers (Sniper,
Splash, Sonar, Pulse, Aura, Mortar, Seeker, Cinder) · 4 Cross-tower combos · 5 Enemies that give
towers a job · 6 Wild ideas · 7 Answers to the iteration-2 open questions · 8 Shortlist · 9
Challenging what exists (stats, shipped upgrades, names)

---

## 0. What the code says today

Facts found while reading the towers, effects and enemy stats. They are the reasons behind many
proposals below. No boxes here, only a place to disagree.

1. **Pulse is erased by plating.** It hits for 2 per tick, physical. Plating subtracts per hit,
   so Armored at Elite (8) and Boss (10) rank and the Warden (10) take nothing from it at all.
   Pulse's weakness is structural, not a number to tune.
2. **Magic is rare.** Seeker's hit, Cinder's burn, Toxic Bloom's poison and Piercing Tone's bonus
   are the only magic damage. Adaptive elite armor punishes a one-type defence, so more magic
   pays off, and it is what gets through physical plating.
3. **Six specials apply the same Vulnerable**: Marked Round, Withering Field, Warding Field,
   Cursed Shrapnel, Homing Curse and Hexflame. It caps at 3 stacks on one clock, so once one
   source saturates the others add nothing, and six towers' specials feel alike.
4. **Half the enemy stat sheet has nothing that lowers it.** No tower lowers armor, magic resist,
   plating, crit chance taken, regeneration or any effect resistance. The stats exist in
   `EnemyStat`: they are ready-made targets for new debuffs.
5. **`CRIT_CHANCE_TAKEN` is unused** since burn's crit doubling was removed.
6. **A DoT can already crit.** Burn and poison pulses go through the applying tower's
   `dealDamage` with its attack profile, so a burn crits as soon as Cinder has any crit chance.
7. **Mark on Sweep is private.** The marks live in the Sonar's own set, so only that Sonar's next
   hit crits. "The next hit from any tower" needs the mark on the enemy, as an effect.
8. **Seeker never finishes what it freezes.** A frozen target drops out of the "furthest along
   the path" lead, so the next missile goes elsewhere (`TODO.md` has the evidence). Part of
   Seeker's identity problem is a targeting problem.
9. **Splash and Mortar overlap.** Both are physical, radial, falloff AoE. What separates them
   (instant, cheap, random target versus slow, far, first target, can miss) is tuning, not
   purpose.
10. **Only the killing blow counts.** `dealDamage` credits a kill to the tower that landed it, so
    kill gates favour whichever tower already hits hardest. That is the farming problem.
11. **A frozen enemy cannot cast** (the Ghost's first-hit vanish is suppressed). Freeze is already
    a silence; a Silence effect would be its "keeps walking" sibling.
12. **Spirit is one step from being tenacity.** It already paces how fast Scorched and Sickened
    wear off and scales heals and shields.
13. **Hexes would reuse what exists.** The enemy trigger vocabulary (`OnCriticalHitTaken`,
    `OnDeath`, `OnFirstDamageTaken`, `HealthThreshold`, `TimeSinceLastHit`, `Periodic`) is what a
    curse reacts to, and `DamageSink` already credits effect damage to its tower.
14. **Doc and code disagree on Splash's chill.** The tower line gives Splash "chill 50% for 40t";
    the code chills only with Concussive Blast.
15. **The Jammer is in no level.** Disruption exists and nothing uses it yet, which makes a
    jam-resistant Fortify pointless until it shows up in waves.
16. **Cheap lines already exist:** `InWedgeTargetQuery` (a narrow wedge is a line, which a
    piercing shot needs), `grantTimedBuff`, `onKill` with the dead enemy's effects,
    `reductionAgainst` (how much an enemy resists a damage type), `EffectInteractions` (a table of
    effect rules) and `AttackProfile` (per-shot crit multiplier and penetration).

💬

---

## 1. The big picture

### 1.1 One job per tower

The question a player should be able to answer at a glance: *why this tower, for this wave?*

- [ ] ⭐ **Sniper - the Assassin.** Deletes the one enemy that matters: the Elite, the Mender, the
  boss. Physical, crits, armor answers, magic ammo.
  - 💬
- [ ] ⭐ **Splash - the Spreader.** Cheap, instant, never misses: the delivery vehicle for anything
  that should land on a whole group (poison, hexes, arcs, debuffs). It weakens crowds for
  everyone rather than killing them itself.
  - 💬
- [ ] ⭐ **Sonar - the Spotter.** Sees and exposes: reveals the invisible, marks targets for
  guaranteed crits from any tower, cracks crit immunity, bridges to magic. Long-range support that
  also deals steady damage.
  - 💬
- [ ] ⭐ **Pulse - the Field.** A circle where the rules change: silence, slower recovery,
  corrosion, amplification, knockback. Its damage is incidental; its job is to be the place
  enemies must not linger. Built at loops and bends.
  - 💬
- [ ] ⭐ **Aura - the Mentor.** Buffs a cluster, shields it from jamming, and helps young towers
  reach their upgrade gates.
  - 💬
- [ ] ⭐ **Mortar - the Artillery.** The only tower that shapes the ground: nukes for bosses,
  carpets for swarms, fields of fire, tar and frost that keep working after the shell lands.
  Holds chokepoints where lanes meet.
  - 💬
- [ ] ⭐ **Seeker - the Hunter.** Never misses and never lets anything get away: magic single
  target that homes onto fast enemies, keeps its lock through invisibility, strips shields, freezes,
  and stores missiles for a salvo when the dangerous one arrives.
  - 💬
- [ ] ⭐ **Cinder - the Pyre.** Magic damage over time that ignores armor, scorches resilience so
  crits land harder for everyone, and burns what hides or heals.
  - 💬

### 1.2 Who owns which enemy stat

Give each tower "its" debuff so specials stop converging on Vulnerable. A player learns: *Sonar
means crits, Seeker means magic, Mortar cracks plates.*

- [ ] ⭐ **Armor** -> Sniper (Sundered, single-target stacking) and Pulse (Corrosion, while inside).
  - 💬
- [ ] ⭐ **Magic resist** -> Seeker (Unraveled).
  - 💬
- [ ] ⭐ **Plating** -> Mortar (Cracked: explosions crack plates).
  - 💬
- [ ] ⭐ **Crit chance taken** -> Sonar (Exposed); Cinder's Heat Haze as an alternative carrier.
  - 💬
- [ ] ⭐ **Resilience** -> Cinder (Scorched, exists); Sonar's Resonant Crack as the burst version.
  - 💬
- [ ] ⭐ **Spirit** -> Pulse (Soul Drain) and poison (Sickened, exists).
  - 💬
- [ ] **Regeneration and incoming heals** -> Cinder (Cauterize).
  - 💬
- [ ] **Effect resistances** -> Splash (Susceptible: every effect lasts longer). The one stat no
  other tower touches.
  - 💬
- [ ] ⭐ **Damage taken (Vulnerable)** -> Aura (Withering Field) as the owner, plus Sniper's Marked
  Round and Cinder's Hexflame. The other three sources switch payload (see 1.4).
  - 💬
- [ ] **Speed** -> everyone chills; Seeker freezes; Mortar tars; Pulse anchors.
  - 💬
- [ ] **Stealth** -> Sonar (reveal), Pulse (hits and reveals the hidden), Cinder (burning reveals).
  - 💬
- [ ] **Abilities (silence)** -> Pulse (Null Field), with Mortar and Seeker as short-silence
  carriers.
  - 💬
- [ ] **Bounty** -> Aura (Tithe), Splash (Hex of Greed), Sniper (Headhunter).
  - 💬

### 1.3 Damage types

- [ ] ⭐ 🟢 **Pulse deals magic.** An energy field; magic plating barely exists, so its tiny ticks
  finally land on Armored Elites and the Warden.
  - 💬
- [ ] 🟢 **Splash deals magic** ("arcane bolt"). Cheap early magic AoE; adaptive armor then leans
  magic early, which rewards physical towers later.
  - 💬
- [ ] ⭐ 🟡 **Every physical tower gets one magic option** somewhere in its tree: Sniper's Silver
  Ammunition, Sonar's Frequency, Mortar's Napalm, Splash's arcs.
  - 💬
- [ ] ⭐ **Final split:** physical Sniper, Splash, Sonar, Mortar; magic Pulse, Seeker, Cinder; Aura
  none. Magic enablers: Seeker (-magic resist), Sonar (Resonating). Physical enablers: Sniper
  (-armor), Mortar (-plating), Seeker's Brittle (frozen take extra physical).
  - 💬
- [ ] 🔴 A third damage type (pure/true, ignores mitigation) only for executes and hex payloads.
  Risky: it makes armor matter less everywhere it appears.
  - 💬

### 1.4 Un-duplicate Vulnerable

- [ ] ⭐ Keep Vulnerable on Withering Field (Aura), Marked Round (Sniper), Hexflame (Cinder).
  - 💬
- [ ] ⭐ Cursed Shrapnel (Mortar) applies **Cracked** (plating -50%) instead.
  - 💬
- [ ] ⭐ Homing Curse (Seeker) applies **Unraveled** (magic resist down) instead: 1 stack, 2 on a
  frozen or chilled target, as today.
  - 💬
- [ ] ⭐ Warding Field (Pulse) rolls **Exposed** or **Sundered** instead.
  - 💬
- [ ] Or keep all six and raise the cap: 3 stacks from one source type, 5 from several.
  - 💬

### 1.5 The roster

- [ ] ⭐ **Keep eight towers.** Every one has a job once Splash, Pulse and Mortar are re-aimed.
  Adding a ninth adds balance load right when you want to wrap up.
  - 💬
- [ ] **Splash becomes Arc** (chain lightning): the beam jumps enemy to enemy instead of bursting.
  Clearly not Mortar (lines versus clumps), but the splash ideas go.
  - 💬
- [ ] **Add Arc as a ninth tower** (`o` key) and keep Splash as the Spreader.
  - 💬
- [ ] **Merge Splash into Mortar** (Mortar's cheap early form) and add Arc in its slot.
  - 💬
- [ ] **Add an economy tower** (Bank / Harvester: interest on credits, bonus bounty in its
  radius). Classic TD decision layer; Aura's Tithe and Pulse's Harvester cover it without a tower.
  - 💬

---

## 2. Shared mechanics

Primitives several towers reuse. Building one of these usually unlocks five to ten ideas below.

### 2.1 Crit

- [ ] ⭐ 🟢 **Base crit multiplier per tower**: Sniper x2.0, everyone else x1.5.
  `TowerBaseStats` gains it; `AttackProfile.critMultiplier` already travels with every hit.
  - 💬
- [ ] ⭐ 🟡 **`critDamageBonus` axis on `TowerBuff`**, added to the multiplier (x2.0 + 0.5 =
  x2.5), so Aura, Weak Spot and Fifth Shot stack instead of overwriting each other.
  - 💬
- [ ] ⭐ **Only Sniper is born with crit.** Others earn it from one head level, 5-15%.
  - 💬
- [ ] ⭐ 🟢 **Aura's crit damage (+50%) comes from a head level** (Amplifying Core III), never the
  base aura: it multiplies with Sniper's big hits. Sniper 250%, others 200% under it.
  - 💬
- [ ] 🟢 **DoT crits**: keep today's behaviour (a DoT pulse crits with its tower's chance) and
  make it a feature through Cinder's Flashpoint. Alternatively forbid DoT crits.
  - 💬
- [ ] 🟢 No crit spark for DoT crits: 4 pulses a second of sparks is noise.
  - 💬
- [ ] 🟡 **Resilience penetration** on `AttackProfile` (Sniper's Deadeye): the only way through crit
  immunity besides a Mark.
  - 💬
- [ ] 🟡 **A Mark beats crit immunity**: a marked hit crits even at 100 resilience, for half the
  bonus. Makes Sonar the tower that cracks armored enemies open for crit teams.
  - 💬

### 2.2 New effects and debuffs

Each is one `EffectKind` and a stat modifier, following the existing pattern. Who applies it is in
brackets.

- [ ] ⭐ 🟡 **Sundered**: armor -10 per stack, 5 stacks, one 4 s clock. Can push armor below zero,
  where the enemy takes extra physical damage. (Sniper Sunder Rounds, Pulse Corrosion, Mortar
  Bunker Buster)
  - 💬
- [ ] ⭐ 🟡 **Unraveled**: magic resist -15 per stack, 3 stacks, 5 s. (Seeker)
  - 💬
- [ ] ⭐ 🟡 **Cracked**: plating -50%, 5 s, refreshes. The Warden's and Armored Elite's plates
  become beatable. (Mortar, Sniper Heavy Caliber)
  - 💬
- [ ] ⭐ 🟡 **Exposed**: crit chance taken x2, 3 s. Revives the unused stat. (Sonar, Cinder Heat
  Haze)
  - 💬
- [ ] ⭐ 🟡 **Marked**: the next hit from any tower is a guaranteed crit; consumed by that hit; one
  per enemy. (Sonar Mark on Sweep, Seeker Tracer)
  - 💬
- [ ] ⭐ 🟡 **Silenced**: the enemy's abilities don't fire and its support auras (heal, shroud,
  jam) switch off. Death abilities still fire. (Pulse Null Field, Mortar Shell Shock, Seeker EMP)
  - 💬
- [ ] 🟡 **Dazed** (the stun the interactions doc left out): stops like freeze and silences, but
  doesn't put out burn or consume chill, and shares freeze's diminishing-returns ladder. (Mortar
  Heavy Shell, Splash Concussive Blast)
  - 💬
- [ ] 🟡 **Brittle**: a frozen enemy takes +30% physical damage. The freeze-then-smash combo.
  (Seeker Deep Freeze III)
  - 💬
- [ ] 🟡 **Anchored**: can't be sped up (hurt speed and haste ignored), speed capped at 75% of base.
  (Pulse Phase Lock)
  - 💬
- [ ] 🟡 **Bleeding**: physical damage per cell travelled; a stopped enemy doesn't bleed. The
  faster it runs, the more it bleeds: the anti-Frenzied DoT. (Mortar Shrapnel Storm)
  - 💬
- [ ] 🟡 **Resonating**: +8% magic damage taken per stack, 3 stacks, 4 s. (Sonar Harmonics)
  - 💬
- [ ] 🟡 **Susceptible**: every effect resistance -30%, down to -50%. Below zero, effects last
  longer than authored. (Splash)
  - 💬
- [ ] 🟡 **Soulfire**: a third fuel pool (blue flame) that earns Sickened stacks, so it stacks with
  burn and poison and drains spirit. (Cinder White Flame III)
  - 💬
- [ ] 🔴 **Hexes**: timed debuffs with a trigger. See 2.3.
  - 💬
- [ ] 🔴 **Ground effects** (burning ground, tar, frost, fallout). See 2.4.
  - 💬
- [ ] 🟡 **Haste** (enemy side, for future enemies). Anchored and Hex of Inversion counter it. The
  enemy-stats doc deferred agility until a haste exists.
  - 💬
- [ ] 🟡 **Markers grouped by category** once there are this many kinds: one marker per category
  with a count, the inspector lists them all.
  - 💬

### 2.3 Hexes (curses)

A hex is a timed debuff that waits for something to happen to the enemy and then fires a payload,
credited to the tower that cast it (through its `DamageSink`, so it counts for gates too). The
triggers mirror the enemy ability triggers. Drawn as a small rune on the enemy and a flash when it
fires.

- [ ] 🔴 **The framework**: a hex = trigger + payload + duration.
  - 💬
- [ ] ⭐ An enemy carries up to two different hexes; recasting one refreshes it.
  - 💬
- [ ] **Hex of Echoes** (your idea): each critical hit the enemy takes deals +20 extra magic
  damage.
  - 💬
- [ ] ⭐ **Hex of Contagion**: on death, its debuffs (Vulnerable, Sundered, poison, Scorched, other
  hexes) jump to up to 3 enemies within 1.5 cells with their remaining time.
  - 💬
- [ ] ⭐ **Hex of Reversal**: heals and shields it receives are dealt to it as damage instead.
  Menders become bombs; the Warden's reshield hurts it.
  - 💬
- [ ] ⭐ **Hex of Inversion**: anything that would speed it up slows it instead (Frenzied and
  Reaver hurt speed, a future haste).
  - 💬
- [ ] **Hex of Brittleness**: when frozen, it loses 10% of its current health.
  - 💬
- [ ] **Hex of Greed**: dies hexed -> +50% bounty, whoever kills it.
  - 💬
- [ ] **Hex of Doom**: when the hex ends, it takes 30% of all damage it took while hexed, as one
  magic hit.
  - 💬
- [ ] **Hex of Binding** (Soul Link): enemies hexed together share 25% of the damage any of them
  takes.
  - 💬
- [ ] **Hex of Kindling**: the next burn it catches starts at double pool.
  - 💬
- [ ] **Hex of Exposure**: if it would turn invisible, it is revealed instead and takes a hit.
  - 💬
- [ ] **Hex of Grief**: takes 5% of its max health whenever an ally within 1.5 cells dies. Swarms
  unravel in cascades.
  - 💬

### 2.4 Ground effects

- [ ] 🔴 **A world list of zones**, like projectiles: a disc with a lifetime that applies an effect
  to every enemy inside on each pulse, crediting its tower. One new draw command, a translucent
  patch with a per-kind pattern. Zones hit invisible enemies, like all area damage.
  - 💬
- [ ] **Kinds**: burning ground, tar, frost ground, fallout, mines, embers, lingering flames.
  - 💬
- [ ] ⭐ **Zone interactions** in the same table as effect interactions: fire on tar ignites
  (inferno), frost on fire bursts it (thermal shock), frost on tar hardens it (1 s root).
  - 💬
- [ ] 🟢 A cap per tower (say 4 live zones) to stay inside the 1 ms tick budget.
  - 💬

### 2.5 Other primitives

- [ ] 🔴 **Knockback**: move an enemy back along its path. Shares freeze's diminishing returns;
  bosses take half. (Pulse Shockwave, Sonar Sonic Boom; a pull variant for Mortar's Gravity Shell)
  - 💬
- [ ] 🟡 **Chain targeting**: the nearest enemy not yet hit, within a jump radius. (Splash Arc,
  Pulse Tesla Coil)
  - 💬
- [ ] 🟢 **Line pierce**: a narrow `InWedgeTargetQuery` from the tower through the target.
  (Sniper Railgun)
  - 💬
- [ ] 🟡 **Magazine**: the cooldown loads shots into a store instead of firing. (Seeker nest)
  - 💬
- [ ] 🟡 **Dispel**: remove a shield or a heal-over-time from an enemy. (Seeker Nullifier, Pulse
  Dead Zone)
  - 💬
- [ ] 🟡 **Damage type per shot**: every Nth shot magic. (Sniper Silver Ammunition)
  - 💬
- [ ] 🟡 **Execute**: kill outright below a health share. (Sniper Executioner)
  - 💬
- [ ] 🟡 **Predictive aim**: an unguided shell aims where the enemy will be. (Mortar)
  - 💬
- [ ] ⭐ 🟡 **Target priority chosen by the player** per tower: first, last, strongest, weakest,
  nearest, fastest, highest rank. Four selectors exist already. Unlocked by Fortify, or on every
  tower from the start.
  - 💬
- [ ] 🟡 **Projectile speed and size as tower stats**, shown in the info rows and drawn: a
  heavier shell looks heavier.
  - 💬

### 2.6 Spirit becomes tenacity

- [ ] ⭐ 🟡 **Spirit paces every debuff**, not only Scorched and Sickened: timers tick at
  `max(0.25, 1 + spirit / 100)` per tick. Freeze keeps its own diminishing returns. Poison and
  Pulse's Soul Drain then make *everything* last longer: your "slower recovery from any effect" as
  one stat rule.
  - 💬
- [ ] 🟡 **High-spirit enemies** (a Priest, a Zealot) shake debuffs off fast: the enemy that asks
  for poison or a Pulse.
  - 💬
- [ ] 🟢 The inspector says it in words: "debuffs wear off 1.5x slower".
  - 💬

### 2.7 New effect interactions (reward, not only punish)

The interactions doc worried that fire-versus-frost only punishes mixing towers. Rows that pay
off instead:

- [ ] ⭐ 🟡 **Freezing a burning enemy bursts the burn**: instead of just going out, the remaining
  pool lands at once at 50%. Cinder's Thermal Shock raises it to 150%.
  - 💬
- [ ] 🟡 **Frozen enemies take +20% physical** (globally), or only with Seeker's Brittle.
  - 💬
- [ ] ⭐ 🟡 **Burning reveals**: stealth 0 while burning (your idea).
  - 💬
- [ ] 🟡 **Tar + burn**: a tarred enemy's burn starts at double pool.
  - 💬
- [ ] 🟡 **Dazed doesn't block burn**; Dazed and freeze share one diminishing-returns ladder.
  - 💬
- [ ] 🟡 **Revealed enemies are Exposed** while revealed: Sonar's two jobs feed each other.
  - 💬
- [ ] 🟡 **Silence switches off enemy auras** (Mender heal, Ghost shroud, Jammer disruption).
  - 💬
- [ ] 🟡 **Poison + burn = toxic smoke**: a burning poisoned enemy leaks a little poison to
  neighbours.
  - 💬

### 2.8 Kill gates and the farming problem

- [ ] ⭐ 🟡 **Assists count**: a kill counts for every tower that damaged the enemy in the last
  3 s, or has a DoT on it. Kill gates read "takedowns".
  - 💬
- [ ] ⭐ 🟢 **Gate buyout**: a gated node can be bought now at double price, or at +$N per missing
  kill.
  - 💬
- [ ] 🟡 **Waves-served gate**: "on the board for 4 waves". Seniority, not kills.
  - 💬
- [ ] ⭐ 🟡 **Purpose gates**: count what the tower is for. "Applied 60 freezes", "revealed 10
  hidden enemies", "landed 40 crits", "marked 30 targets", "burned for 5000". Support towers stop
  starving.
  - 💬
- [ ] 🟡 **Rank gate**: "killed an Elite or Boss", or "dealt 2000 to Boss-rank enemies".
  - 💬
- [ ] ⭐ 🟡 **Neighbour-type gate**: "next to an Aura" / "next to 2 Auras". Your Aura-beside-Aura
  condition, reusable elsewhere ("next to a Sonar" for a Sniper node).
  - 💬
- [ ] 🟡 **Diversity gate**: "3 different tower types adjacent". Rewards mixed clusters.
  - 💬
- [ ] 🟡 **Softening gate**: a kill requirement drops 10% for every wave the node has been on offer.
  - 💬
- [ ] 🟢 **Prefer damage gates for late nodes**: damage is shared fairly; kills are not.
  - 💬
- [ ] Aura's Tutelage line (section 3.5) as the in-world fix.
  - 💬

### 2.9 Upgrade structure

**Fortify** (the new base root; working name)

- [ ] ⭐ 🟢 **Jam-proof**: halves disruption on this tower. "A reinforced base" in plain terms,
  and a reason to put the Jammer into waves.
  - 💬
- [ ] ⭐ **Plus a small per-tower perk**, listed with each tower.
  - 💬
- [ ] 🟡 Unlocks the player-chosen target priority (2.5).
  - 💬
- [ ] 🟢 Only a price gate; the perk is a sweetener, not a power spike.
  - 💬
- [ ] Look: a thicker base ring with rivets.
  - 💬

**Range II and III**

- [ ] ⭐ 🟢 Shrinking steps: +15%, +10%, +10%.
  - 💬
- [ ] 🟡 Range III carries a per-tower perk (listed per tower), so it is a small event, not +10%.
  - 💬

**Transcendent**

- [ ] ⭐ Look: a slow halo ring on the base and a gold pip; the turret's shot takes a lighter,
  whiter colour.
  - 💬
- [ ] 🟢 Price about 4x the tower's price.
  - 💬

**A power budget, so levels read the same on every tower**

- [ ] ⭐ Head I and II: about +25% effective output each, or one small verb.
  - 💬
- [ ] ⭐ Head III: the chain's defining verb (it is what Transcendent needs, so it must feel like
  an arrival).
  - 💬
- [ ] ⭐ Head IV-A / IV-B: a new mode that changes *where and how* you use the tower, never "+50%".
  - 💬
- [ ] ⭐ Extra head node: utility or a second axis (damage type, targeting, projectile, economy),
  never raw damage, so owning it doesn't dwarf a chain.
  - 💬
- [ ] ⭐ Specials: rule-breakers. Pairings: combos you'd plan a level around.
  - 💬

**Prices** (P = the tower's own price)

- [ ] 🟢 A formula: Range I 0.6P, Fortify 0.8P, Awaken 1P, Range II 1.2P, Range III 2P,
  Transcendent 4P; head I 1P, II 1.5P, III 2.5P, IV 4P; extra node 0.8P / 1.2P / 2P / 3P;
  special 2P, second special 3P.
  - 💬
- [ ] Or absolute tiers per level whatever the tower costs, so a $10 Sniper isn't the cheapest to
  max out.
  - 💬

**Specials**

- [ ] 🟢 Tag each special Offense, Control or Support; the second special must have a different
  tag. Forces a pairing to be a combo, not two damage amplifiers (the balance risk the doc names).
  - 💬
- [ ] ⭐ Or free choice, with hand-written pairings for the good pairs and a fallback of "+20%
  strength to both" for the rest.
  - 💬

---

## 3. Towers

Each tower: why build it, then base line, BASE, both head chains, the extra head node,
specials, pairings for two specials, and a pool of loose ideas. Existing nodes are listed plainly
for context; only proposals carry a box.

### 3.1 Sniper - the Assassin

**Why build it.** The cheapest tower and the only one born with crit. It deletes one important
enemy at a time: the Elite inside a Soldier wave, the Mender behind the wall, the Warden. Every
upgrade answers "how do I kill *that* one": crits, armor answers, magic ammo, executes.
**Buy it when** waves carry Elite or Boss ranks, or support enemies (Mender, Jammer, Elite Ghost)
must die first. **Weak against** swarms, crit-immune armor until upgraded, the invisible.
**Look by tier:** thin barrel; scope glint (Fortify); a faint laser line to its target (Awaken);
gold barrel and a white tracer (Transcendent); silver shots as a pale blue beam.

**Base line**

- [ ] ⭐ 🟢 Crit multiplier x2.0 (the only tower above x1.5).
  - 💬
- [ ] 🟢 Keep 15% base crit chance.
  - 💬
- [ ] 🟡 **Steady Aim**: +10% crit chance while it keeps shooting the same target; resets on
  retarget. Rewards boss duels.
  - 💬

**BASE**

- [ ] ⭐ 🟢 Fortify **Bipod**: jam-proof, and the turret turns 50% faster.
  - 💬
- [ ] 🟡 Fortify **Scope** (alternative): choose target priority (first, strongest, highest rank).
  - 💬
- [ ] 🟢 Range II +15%, Range III +10%.
  - 💬
- [ ] 🟡 Range III **Overwatch**: +35% range, but it can't shoot within 1 cell (a dead zone).
  - 💬

**HEAD chain A - Focused Optics** (raw power). Exists: I +20% damage; II +20% damage, +25% fire
rate.

- [ ] ⭐ 🟢 III **Heavy Caliber**: +30% damage, ignores 50% of plating. Answers plated elites
  without touching crit.
  - 💬
- [ ] ⭐ 🟢 IV-A **Railgun**: the shot pierces every enemy on the line from the tower through the
  target to max range; each one after the first takes 25% less. Build it along a straight.
  - 💬
- [ ] ⭐ 🟡 IV-B **Executioner**: a non-boss enemy left below 15% health by a shot dies; Boss rank
  takes +50% while below 25%. Executions count as crits for triggers.
  - 💬
- [ ] 🟡 IV-B alternative **Overkill**: damage beyond a kill carries into the nearest enemy, once.
  - 💬

**HEAD chain B - Marksman's Eye** (crit). Exists: I +15% crit; II +20% crit, 50% armor
penetration.

- [ ] ⭐ 🟡 III **Weak Spot**: +50% crit damage (x2.0 -> x2.5).
  - 💬
- [ ] ⭐ 🟡 IV-A **Deadeye**: ignores 50% of resilience, so crit-immune enemies can be crit for
  half the bonus. Crit-immune Armored mobs stop being a Sniper's hard counter.
  - 💬
- [ ] ⭐ 🟡 IV-B **Sunder Rounds** (your shattered armor): every crit applies 2 Sundered stacks
  (-10 armor each, 5 max). The team option: every physical tower hits that enemy harder.
  - 💬

**Extra head node - Silver Ammunition** (your idea; the damage-type line)

- [ ] ⭐ 🟡 I: every 4th shot is magic.
  - 💬
- [ ] ⭐ 🟡 II: every 3rd shot is magic, with +25% magic penetration.
  - 💬
- [ ] 🟡 III: silver shots reveal their target for 2 s and ignore shields.
  - 💬
- [ ] 🟡 IV: every 2nd shot is magic; silver shots apply 1 Unraveled.
  - 💬

Alternative extra node - **Rangefinder**:

- [ ] 🟡 I +10% range; II target-priority choice; III +20% damage against targets past two thirds
  of its range; IV **Spotter Uplink**: may shoot any enemy another tower has Marked or Revealed
  within twice its range.
  - 💬

**Specials** (any of them switches targeting to most health, as today)

- Marked Round (exists): crits apply Vulnerable.
- Fifth Shot (exists): every 5th shot a guaranteed crit, crits deal 250%.
- [ ] ⭐ 🟢 Fifth Shot tweak: "+50% crit damage" instead of "crits deal 250%", so it stacks with the
  x2.0 base, Weak Spot and Aura rather than overwriting them.
  - 💬
- Momentum (exists): the shot after a crit deals 500% and ignores armor and plating; a kill grants
  +100% fire rate for 5 s.
- [ ] ⭐ 🟡 **Ricochet**: a crit bounces to the nearest other enemy within 1.5 cells for 60%; the
  bounce can crit again, up to 3 bounces. Sniper's only swarm answer, still crit-driven.
  - 💬
- [ ] ⭐ 🟡 **Headhunter**: highest rank first; +40% damage against Elite and Boss; Elite and Boss
  kills pay double bounty.
  - 💬
- [ ] 🟡 **Cold Bore**: +40% damage per second without firing, up to +200%. The first shot at a
  boss walking into range is enormous; build it far down the path.
  - 💬
- [ ] 🟡 **Spotter's Link**: may fire at any enemy another tower has Marked or Revealed, within
  twice its range; those come first.
  - 💬

**Pairings** (both specials owned, after Transcendent)

- [ ] ⭐ Fifth Shot + Momentum -> **Rhythm**: the 5th shot's guaranteed crit charges Momentum, and
  the charged shot always crits too. Every 6th shot is x5 and critical. The boss killer.
  - 💬
- [ ] Fifth Shot + Marked Round: the guaranteed crit applies all 3 Vulnerable stacks at once.
  - 💬
- [ ] Marked Round + Momentum: the x5 shot spends the target's Vulnerable stacks for +30% each,
  then applies 1 again.
  - 💬
- [ ] ⭐ Momentum + Ricochet: a Momentum shot ricochets at full x5; ricochet kills also grant the
  fire-rate burst.
  - 💬
- [ ] ⭐ Ricochet + Marked Round: every bounce applies Vulnerable. The Sniper spreads Vulnerable to
  a group.
  - 💬
- [ ] Headhunter + Fifth Shot: against Elite and Boss, every 3rd shot is the guaranteed crit.
  - 💬
- [ ] Headhunter + Cold Bore: the charge is spent only on Elite and Boss; lesser shots leave it
  building.
  - 💬
- [ ] Cold Bore + Momentum: a kill refunds half the charge.
  - 💬
- [ ] Spotter's Link + Marked Round: crits on a spotted target apply 2 stacks.
  - 💬

**More ideas**

- [ ] 🟡 **Double Tap**: 15% chance to fire a second shot at once.
  - 💬
- [ ] 🟡 **Tracer**: every hit reveals its target for 1 s. A Ghost vanishes on the first hit; the
  tracer lets the second shot land.
  - 💬
- [ ] 🟡 **Shatter Shot**: crits on a frozen target deal +50% crit damage (the Seeker combo).
  - 💬
- [ ] 🟢 **Bounty Hunter**: a crit kill pays +1 credit.
  - 💬
- [ ] 🟡 **Quick Scope**: the first shot at a new target has +50% crit chance.
  - 💬
- [ ] 🟡 **Two in the Chamber**: holds one extra shot during idle time, fired instantly on the next
  target (a one-round magazine).
  - 💬

### 3.2 Splash - the Spreader

**First, its identity.** Splash and Mortar are both physical radial AoE today. Pick what Splash is
for:

- [ ] ⭐ **A. The Spreader**: keep the instant bolt and burst. Its job is to put things on a whole
  group: poison, hexes, arcs, debuffs. Modest damage, big utility. Magic comes through arcs and
  hexes.
  - 💬
- [ ] **B. Arcane Splash**: A, plus base damage becomes magic.
  - 💬
- [ ] **C. Arc**: rework into chain lightning (beam jumps instead of bursting).
  - 💬
- [ ] **D. Remove** Splash; Mortar owns AoE; an Arc tower takes the slot.
  - 💬

My take is A, with the arc as its extra head node so any Splash can become a chain tower: the cheap
early AoE stays, and it gets a purpose Mortar can't copy (instant, every second, the carrier of
debuffs).

**Why build it.** Cheap, instant, never misses, fires every second: the best vehicle for anything
that should land on many enemies. It doesn't kill big things; it makes a whole group worse for
everyone else. The tower you re-spec per level: poison for healers and swarms, hexes for elites,
arcs for strung-out lines. **Buy it when** early (cheap AoE), for groups with healers or shields,
for mixed waves where every debuff multiplies. **Weak against** lone bosses and heavy plating (its
hits are small). **Look:** white burst; blue arcs (Arc Emitter); violet runes over hexed enemies.

**Base line**

- [ ] ⭐ 🟡 **Targets the densest group** (the enemy with the most neighbours within blast radius)
  instead of a random one. An AoE tower on purpose, and unlike Mortar's "first".
  - 💬
- [ ] 🟢 Settle the chill mismatch: give base Splash the 50% chill (Concussive Blast then needs a new
  payoff), or drop it from the tower line.
  - 💬
- [ ] 🟢 Falloff floor: the edge of a blast deals 25% instead of nothing.
  - 💬

**BASE**

- [ ] ⭐ 🟢 Fortify **Stabilized Coils**: jam-proof, falloff floor 25%.
  - 💬
- [ ] 🟢 Range II +15%; Range III +10% and blast radius +10%.
  - 💬

**HEAD chain A - Blast Engineering** (area). Exists: I +30% radius; II +25% damage, flatter
falloff; III 3 blasts per shot.

- [ ] ⭐ 🟢 IV-A **Carpet**: 5 blasts per shot at 60% damage each. The swarm shredder.
  - 💬
- [ ] ⭐ 🟡 IV-B **Aftershock**: every blast detonates again 1 s later on the same spot for 60%,
  catching the enemies walking into it. Deadly on columns and drips.
  - 💬
- [ ] 🔴 IV-B alternative **Implosion**: one focused blast; the primary takes x2.5 and everything
  caught is pulled 0.3 cells toward the centre (clumps for Mortar).
  - 💬

**HEAD chain B - Rapid Battery** (tempo, crit). Exists: I +25% fire rate; II +25% damage, +15%
crit; III crits splash 50% bigger.

- [ ] ⭐ 🟡 IV-A **Chain Reaction**: a critical blast sets off a second blast on the enemy it hit
  hardest, once per shot.
  - 💬
- [ ] ⭐ 🟡 IV-B **Suppressive Fire**: +60% fire rate, -25% damage; every blast refreshes all debuffs
  on everything it catches (Vulnerable clock, chill level, hex timers). The upkeep tower.
  - 💬
- [ ] 🟢 alternative **Scatter**: 2 targets per shot, radius -30%.
  - 💬

**Extra head node - Arc Emitter** (the multi-strike turret you wanted)

- [ ] ⭐ 🟡 I: the primary arcs to 1 more enemy within 1.5 cells for 50%, as magic.
  - 💬
- [ ] ⭐ 🟡 II: arcs to 2 enemies; jump range 2 cells.
  - 💬
- [ ] 🟡 III: arcs carry the blast's effects (poison, chill, hexes).
  - 💬
- [ ] 🟡 IV: arcs to 4, each can crit, and arcs prefer enemies not yet hit this shot.
  - 💬

Alternative extra node - **Hex Focus**:

- [ ] 🟡 I hexes last +1 s; II +25% hex potency; III an enemy can carry two of this tower's hexes;
  IV a hex spreads to one neighbour when cast.
  - 💬

**Specials**

- Toxic Bloom (exists): the blast poisons.
- Concussive Blast (exists): -50% fire rate, the blast chills, kills explode.
- [ ] ⭐ 🟡 Concussive Blast addition: enemies at the blast centre are Dazed for 0.3 s (your stun).
  - 💬
- Overpressure (exists): after a crit, the next shot blasts every enemy in range.
- [ ] ⭐ 🟢 Overpressure tamed (you called it too strong): the armed shot blasts every enemy in range
  at 50% damage.
  - 💬
- [ ] 🟢 or: the armed shot blasts up to 6 enemies.
  - 💬
- [ ] ⭐ 🔴 **Soul Link**: enemies caught in one blast are linked for 4 s; 25% of any damage one of
  them takes is dealt to each other linked enemy (magic, credited to this Splash). Every Sniper hit
  becomes group damage.
  - 💬
- [ ] ⭐ 🔴 **Contagion**: when a blasted enemy dies, its debuffs (Vulnerable, Sundered, poison,
  Scorched, hexes) jump to up to 3 enemies within 1.5 cells, with their remaining time.
  - 💬
- [ ] 🔴 **Hex of Inversion**: for 4 s, whatever would speed a blasted enemy up slows it instead. The
  anti-rush hex for Frenzied and Reaver.
  - 💬
- [ ] 🔴 **Hex of Reversal**: for 4 s, heals and shields on a blasted enemy become damage. The
  anti-Mender, anti-Warden hex.
  - 💬
- [ ] 🔴 **Hex of Echoes** (your idea): for 6 s, each crit the enemy takes deals +20 magic. Pure
  support for a crit team.
  - 💬
- [ ] 🟡 **Susceptibility**: blasted enemies lose 30% of every effect resistance for 4 s; below zero
  effects last longer. The stat no other tower touches.
  - 💬
- [ ] 🔴 **Hex of Greed**: an enemy that dies hexed pays +50% bounty, whoever kills it.
  - 💬

**Pairings**

- [ ] ⭐ Toxic Bloom + Contagion -> **Plague**: poison jumps on death at full pool. One sick enemy
  can infect a wave.
  - 💬
- [ ] ⭐ Concussive Blast + Soul Link: kill explosions travel through the links. Chain reactions.
  - 💬
- [ ] Overpressure + Soul Link: the armed shot links everything in range into one pool.
  - 💬
- [ ] Overpressure + Toxic Bloom: the armed shot poisons at double pool.
  - 💬
- [ ] Concussive Blast + Contagion: explosions also spread the dead enemy's debuffs.
  - 💬
- [ ] Soul Link + Hex of Echoes: echo damage also travels the links.
  - 💬
- [ ] Hex of Reversal + Toxic Bloom: reversed heals hit twice as hard on a Sickened enemy.
  - 💬
- [ ] Hex of Inversion + Concussive Blast: an inverted enemy that gets chilled freezes for 0.5 s.
  - 💬
- [ ] Susceptibility + Toxic Bloom: a susceptible enemy's poison decays half as fast.
  - 💬

**More ideas**

- [ ] 🔴 **Mines**: a blast leaves a charge on the path that detonates under the next enemy (a
  ground effect).
  - 💬
- [ ] 🟡 **Static**: blasted enemies become Charged; arcs jump twice as far between Charged
  enemies.
  - 💬
- [ ] 🟢 An Overpressure shot drawn as a shockwave ring across the whole range.
  - 💬
- [ ] Rename to Hex tower or Arcanist if it goes the hex way.
  - 💬

### 3.3 Sonar - the Spotter

**Why build it.** It sees everything and makes everything easier to hit. The sweep reaches far and
hits all it passes; on top of that it reveals the invisible, marks targets for a guaranteed crit
from *any* tower, exposes enemies to crits (even crit-immune ones), and bridges to magic. Support
that also deals steady damage over a huge area. **Buy it when** ghosts appear, when the defence
leans on crits, against crit-immune armor, and on spread-out lanes (on Twisted Hourglass one Sonar
can cover all three). **Weak against** packed fast groups (one hit per revolution) and plating (its
hits are small). **Look:** a second beam; a faint ping ring each revolution; a crosshair over marked
enemies; a blue beam once it deals magic.

**Base line**

- [ ] 🟢 Settle the base sweep: the tower line says 4 s per turn and range 4.2, the code 2 s and
  5.2. Fire rate *is* the rotation speed, so this is its DPS dial.
  - 💬
- [ ] 🟢 A faint ping ring each revolution (visual only), so the rhythm reads.
  - 💬

**BASE**

- [ ] ⭐ 🟡 Fortify **Counter-Jamming**: jam-proof, and every tower within the Sonar's range takes
  half disruption. The Sonar tracks the Jammer.
  - 💬
- [ ] 🟢 Range II +15%, Range III +10%.
  - 💬
- [ ] 🟡 Range III **Deep Scan**: each revolution reveals invisible enemies in the outer quarter of
  its range for 1 s.
  - 💬

**HEAD chain A - Twin Array** (hits). Exists: I +25% damage; II +25% damage, +10% crit; III a
second beam, opposite.

- [ ] ⭐ 🟢 IV-A **Quad Array**: 4 beams at -30% damage each. Twice the hits, marks and reveals.
  - 💬
- [ ] ⭐ 🟡 IV-B **Phased Array**: stops spinning, locks onto the enemy with most health in range and
  sweeps a 60 degree arc back and forth over it: about 3x the hits on the focus. Boss mode.
  - 💬

**HEAD chain B - Long Reach** (distance, crit). Exists: I +15% crit; II damage up to +100% at max
range.

- [ ] ⭐ 🟡 III **Far Echo**: hits beyond half range apply Exposed (crit chance taken x2, 3 s).
  - 💬
- [ ] 🟢 IV-A **Horizon**: +40% range, far bonus up to +150%, but no damage within 1.5 cells.
  - 💬
- [ ] ⭐ 🟡 IV-B **Resonant Crack**: each hit lowers resilience by 10 (down to -50, recovering 10 a
  second). Crit-immune armor becomes crittable after a few passes; below zero every crit hurts more.
  - 💬

**Extra head node - Frequency** (your magic synergy)

- [ ] ⭐ 🟡 I **Ultrasound**: 20% of each hit is added as magic damage.
  - 💬
- [ ] ⭐ 🟡 II **Harmonics**: hits apply Resonating (+8% magic damage taken per stack, 3 stacks,
  4 s). Support for Seeker, Cinder and a magic Pulse.
  - 💬
- [ ] 🟡 III **Pure Tone**: the beam deals magic instead of physical, +15% magic penetration.
  - 💬
- [ ] 🟡 IV **Shatter Tone**: a hit on a shielded enemy breaks a quarter of the shield; at zero the
  shield is gone.
  - 💬

Alternative extra node - **Datalink** (detection):

- [ ] 🟡 I towers within Sonar's range can target invisible enemies within 1 cell of themselves;
  II within 2 cells; III reveals last +1 s; IV Marked enemies are visible to every tower on the map.
  - 💬

**Specials**

- Wide Band (exists): each revolution reveals invisible enemies it passes.
- Mark on Sweep (exists): a beam hit marks; the next hit on it is a guaranteed crit.
- [ ] ⭐ 🟡 Mark on Sweep rework (your wish): the mark goes on the enemy (the Marked effect), so the
  next hit from **any** tower is a guaranteed crit. One mark per enemy, renewed each pass.
  - 💬
- [ ] Against crit-immune enemies the mark: [ ] is wasted / [ ] ⭐ crits anyway for half the bonus
  / [ ] turns into 1 Vulnerable.
  - 💬
- Piercing Tone (exists): up to +50% magic damage against armored or shielded enemies.
- [ ] ⭐ 🟡 **Exposure**: beam hits apply Exposed (crit chance taken x2, 3 s). Burn's old crit
  doubling returns as a Sonar choice, where it belongs.
  - 💬
- [ ] ⭐ 🟡 **Command Ping**: each revolution names the enemy with most health in range the
  Priority; every tower that can reach it switches to it and deals +15% to it. The Sonar as a
  commander of focus fire.
  - 💬
- [ ] 🔴 **Sonic Boom**: each revolution knocks every enemy in range back 0.3 cells along the path
  (knockback diminishing returns).
  - 💬
- [ ] 🟡 **Echo**: every beam hit repeats 0.5 s later at 40%. Hits fast groups twice.
  - 💬

**Pairings**

- [ ] ⭐ Wide Band + Mark on Sweep: every revealed enemy is also marked. A Sniper one-shots the Ghost
  the moment it's found.
  - 💬
- [ ] ⭐ Mark on Sweep + Exposure: a marked crit on an Exposed target deals +50% crit damage;
  crit-immune enemies lose the immunity while Exposed.
  - 💬
- [ ] Piercing Tone + Exposure: Piercing Tone's magic bonus can crit and grows by half on Exposed
  targets.
  - 💬
- [ ] Wide Band + Piercing Tone: revealed enemies take Piercing Tone's bonus from every tower's
  physical hit, not only the Sonar's.
  - 💬
- [ ] Command Ping + Mark on Sweep: the Priority is re-marked every revolution.
  - 💬
- [ ] Command Ping + Wide Band: an invisible enemy is always chosen as Priority first.
  - 💬
- [ ] Sonic Boom + Exposure: knocked-back enemies stay Exposed 1 s longer.
  - 💬
- [ ] Echo + Mark on Sweep: the echo lays a fresh mark, so a marked enemy is crit twice per pass.
  - 💬

**More ideas**

- [ ] 🟡 Shrouded allies of an Elite Ghost are revealed while inside Sonar's range.
  - 💬
- [ ] 🟡 **Scan**: an enemy the Sonar has hit shows its resistances and weaknesses in the
  inspector ("Scanned").
  - 💬
- [ ] 🟢 Sweep direction toggle (cosmetic).
  - 💬

### 3.4 Pulse - the Field

**Why build it.** A circle where the rules change. It touches every enemy inside on every tick, so
it is the natural carrier of "while inside" effects and the fastest stack builder in the game. Its
damage is incidental; its job is to make its circle the place enemies must not linger: slowed,
silenced, softened, drained, amplified, pushed back. Build it where the path loops or bends around
it so enemies stay inside, and pair it with chill so they stay longer. **Buy it when** casters show
up (Mender, Elite Ghost's shroud, the Warden), for clumps in loops, against rushes. **Weak against**
long straights and spread-out lanes. **Look:** rings rippling outward; the field's colour says its
mode (violet silence, green corrosion, blue stasis); enemies inside flicker.

**Base line**

- [ ] ⭐ 🟢 **Magic damage.** Physical plating erases its 2-per-tick hits entirely today (Armored
  Elite and Boss, the Warden).
  - 💬
- [ ] ⭐ 🟡 **Resonance ramp**: +10% damage for each second an enemy stays inside (up to +100%),
  reset when it leaves. Rewards loops and chill, and eventually beats flat plating.
  - 💬
- [ ] 🟢 Range 1.5 -> 1.75, so it covers both sides of a bend.
  - 💬
- [ ] 🟢 Fire without a visible target from the start (it's the ghost hitter); Resonant Field I then
  needs a new payoff.
  - 💬

**BASE**

- [ ] ⭐ 🟢 Fortify **Grounding**: jam-proof, and +25% damage for the first second after it starts
  firing (a capacitor kick).
  - 💬
- [ ] 🟢 Range II +15%, Range III +10%.
  - 💬
- [ ] 🟡 Range III **Wide Field**: +20% range, and enemies at the field's edge take full damage too
  (no distance weakening if one is ever added).
  - 💬

**HEAD chain A - Overcharged Coils** (damage). Exists: I +30% damage; II +25% damage, +10% crit.

- [ ] ⭐ 🟡 III **Arc Discharge**: once a second a zap hits the enemy inside with most health for 15x
  the tick damage. A burst that clears plating.
  - 💬
- [ ] 🟡 IV-A **Meltdown**: the resonance ramp caps at +250% instead of +100% and climbs twice as
  fast.
  - 💬
- [ ] ⭐ 🟡 IV-B **Tesla Coil**: the zap chains to 3 more enemies within 1.5 cells, even outside the
  field.
  - 💬

**HEAD chain B - Resonant Field** (stealth, rules). Exists: I +20% range, hits invisible enemies
without cover; II +15% range, reveals what it hits for 2 s.

- [ ] ⭐ 🟡 III **Null Field**: enemies inside are Silenced. No heals, shields, summons, shrouds,
  jamming or vanishing while inside.
  - 💬
- [ ] 🟡 IV-A **True Sight**: reveals everything within twice the field's radius; reveals last 4 s.
  - 💬
- [ ] ⭐ 🟡 IV-B **Dead Zone**: entering strips shields, and nothing inside can be healed or shielded.
  Build it where the Warden walks.
  - 💬

**Extra head node - Field Shaping** (your crazy ideas, as the line every Pulse can add)

- [ ] ⭐ 🟡 I **Undertow**: enemies inside are chilled 25% and it doesn't fade while they stay (it
  counts as chill for freezes).
  - 💬
- [ ] ⭐ 🟡 II **Corrosion**: -30 armor while inside (your "reduce armor while in the effect").
  - 💬
- [ ] ⭐ 🟡 III **Stasis**: debuffs on enemies inside wear off at a quarter of the speed: Vulnerable,
  marks, Scorched, Sickened, chill, hexes (your "slower recovery from any effect").
  - 💬
- [ ] 🟡 IV **Event Horizon**: each death inside adds +5% field damage until the wave ends (up to
  +100%); a dying enemy's debuffs pass to the nearest enemy inside.
  - 💬

**Specials**

- Warding Field (exists): each tick, 10% chance to add a Vulnerable stack.
- [ ] ⭐ 🟡 Warding Field payload: Exposed or Sundered instead of Vulnerable (see 1.4).
  - 💬
- [ ] ⭐ 🟡 **Soul Drain** (your spirit idea): each second inside costs 5 spirit (as Sickened stacks),
  and the field deals +1% damage per point of spirit below zero. At -100: double damage, no heals
  or shields, and stack debuffs never wear off. Enemies with high spirit (bosses, a future Priest)
  are drained longest.
  - 💬
- [ ] ⭐ 🔴 **Shockwave**: every 3 s, knocks everything inside back 0.5 cells (diminishing like
  freeze: 100, 50, 25%, then nothing for 10 s; bosses half). At the exit, a last line that buys
  seconds; at a loop, it keeps them in your kill zone.
  - 💬
- [ ] ⭐ 🟡 **Kill Zone**: enemies inside take +25% damage from every *other* tower (its own
  multiplier, outside Vulnerable's cap). The place every other tower should point at.
  - 💬
- [ ] 🟡 **Phase Lock**: enemies inside are Anchored (no speed-ups, speed capped at 75% of base).
  Frenzied and Reaver stop sprinting.
  - 💬
- [ ] 🟡 **Harvester**: each enemy that dies inside pays +2 credits.
  - 💬
- [ ] 🔴 **Magnetic Field**: Mortar shells and Seeker missiles landing inside home onto the nearest
  enemy; nothing misses inside the field. The Mortar's accuracy fix, from another tower.
  - 💬

**Pairings**

- [ ] ⭐ Shockwave + Kill Zone: knocked-back enemies are pushed back into the zone and stay amplified
  for 2 s after leaving.
  - 💬
- [ ] ⭐ Soul Drain + Warding Field: at -50 spirit or lower, Vulnerable stacks never expire and the
  cap rises to 5.
  - 💬
- [ ] Soul Drain + Shockwave: the shockwave deals 1% of max health per 10 missing spirit.
  - 💬
- [ ] Soul Drain + Phase Lock: drained enemies (-50 spirit or lower) are also Silenced.
  - 💬
- [ ] Kill Zone + Harvester: kills inside by any tower pay +25% bounty.
  - 💬
- [ ] Phase Lock + Shockwave: anchored enemies are knocked back twice as far.
  - 💬
- [ ] Warding Field + Kill Zone: Vulnerable caps at 5 inside the field.
  - 💬
- [ ] Magnetic Field + Kill Zone: homed shells and missiles deal +25% more.
  - 💬

**More ideas**

- [ ] 🟡 **Overcharge Grid**: adjacent towers get +15% fire rate while the Pulse is firing.
  - 💬
- [ ] 🟡 **Capacitor**: charges while idle (up to 5 s) and releases one nova when the first enemy
  enters.
  - 💬
- [ ] 🟡 **Mirror Field**: damage absorbed by shields inside is reflected back as magic.
  - 💬
- [ ] 🟡 **Pull**: enemies inside drift toward the path centre (swarm and line spawns bunch up).
  - 💬

### 3.5 Aura - the Mentor

**Why build it.** The support tower: it makes a cluster stronger, keeps it working under jamming,
and, as its new job, helps young towers reach their upgrade gates while a big tower hogs the kills.
It never attacks, so its worth is what it lets the others do. **Buy it when** you've committed to a
cluster, when one tower takes all the kills, against Jammers. **Weak against** spread-out defences.
**Look:** glow lines to buffed towers (exist), coloured by what the aura grants; a small book glyph
on towers it is mentoring.

**Rules**

- [ ] ⭐ 🟢 **Auras never buff Auras.** Drop Resonance Field II's aura-on-aura.
  - 💬
- [ ] ⭐ 🟡 **"Next to an Aura" as a gate** for Aura nodes (your condition). Two Auras side by side
  unlock each other's top levels without feeding each other's numbers.
  - 💬
- [ ] 🟢 **Diminishing stacking**: the 2nd aura on a tower gives 75%, the 3rd 50%. Stops aura
  carpets, keeps two worthwhile.
  - 💬
- [ ] 🟡 **New axes Aura can grant**: crit damage, effect duration, effect potency, penetration,
  projectile speed. A support tower for support towers, not only for damage dealers.
  - 💬
- [ ] ⭐ Keep the cap at Awaken (no Transcendent, one special).
  - 💬
- [ ] 🟡 Or give Aura a Transcendent gated on "next to 2 Auras", unlocking a second special.
  - 💬

**BASE**

- [ ] ⭐ 🟢 Fortify **Anchor** ($20, near-2): towers in range take half disruption. Your "reduce the
  debuffs towers receive".
  - 💬
- Range (base range 2.05) and Range II (3.17) as in the feature doc.

**HEAD chain A - Amplifying Core** (strength). In the doc: I +25% range and damage; II +30% range
and damage, +10% fire rate.

- [ ] ⭐ 🟡 III **Keen Edge** [next to another Aura]: buffed towers +50% crit damage (your aura crit
  idea: Sniper 250%, others 200%).
  - 💬
- [ ] 🟢 III alternative **Overdrive**: +20% more fire rate.
  - 💬

**HEAD chain B - Resonance Field** (reach). In the doc: I +30% range; II +25% range and aura-on-aura.

- [ ] ⭐ 🟢 II replacement for aura-on-aura: buffed towers also gain +15% range.
  - 💬
- [ ] ⭐ 🟡 III **Conduit**: buffed towers' effects last 25% longer and bite 15% harder (chill level,
  burn and poison pool, freeze time, Vulnerable clock, hexes). Makes Aura matter to Sonar, Seeker
  and Cinder, not only to damage dealers.
  - 💬

**Extra head node - Tutelage** (the gate helper; levels I to III)

- [ ] ⭐ 🟡 I: buffed towers count assists (damage in the last 3 s) toward kill gates.
  - 💬
- [ ] ⭐ 🟡 II (your idea): every 10 kills by buffed towers lower the next kill gate of every buffed
  tower by 1.
  - 💬
- [ ] 🟡 III: buffed towers' damage gates count 25% faster.
  - 💬

Alternative extra node - **Veterancy**:

- [ ] 🟡 I a tower built in range starts with 5 kills credited; II 10 kills and 1000 damage; III it
  also starts with Fortify owned.
  - 💬

**Special** (one slot)

- Withering Field (exists): enemies in range gain Vulnerable. With 1.4, Aura becomes Vulnerable's
  owner.
- [ ] ⭐ 🟡 **Tithe**: any kill inside the aura's range pays +25% bounty, whoever made it (the
  `TODO.md` entry "bounty for any kill inside an aura").
  - 💬
- [ ] ⭐ 🟢 **Chosen**: buffs only the one tower in range with the most damage dealt, at triple
  strength. For the Transcendent hero; the opposite choice to spreading thin.
  - 💬
- [ ] 🟡 **Bulwark**: towers in range are immune to disruption; enemies inside the aura can't jam.
  - 💬
- [ ] 🟡 **Rally**: when an Elite or Boss enters a buffed tower's range, every buffed tower gets
  +30% fire rate for 5 s.
  - 💬
- [ ] 🟡 **Mentor**: each kill by the strongest buffed tower also counts as a kill for the buffed
  tower with the fewest.
  - 💬
- [ ] 🟡 **Beacon**: buffed towers can target invisible enemies within their own range (detection
  as a buff).
  - 💬

### 3.6 Mortar - the Artillery

**Why build it.** The only tower that shapes the ground. Slow shells with a huge area: nukes for
bosses, carpets for swarms, and fields of fire, tar and frost that keep working after the shell
lands. It holds the chokepoint where lanes meet. **Buy it when** waves come packed (swarm, column),
lanes converge (Twisted Hourglass's waist), bosses bring escorts. **Weak against** fast single
enemies (misses) and whatever is right next to it. **Look:** shell size grows with Siege levels; a
nuke is a white flash and an expanding ring; napalm an orange flickering patch; tar a dark glossy
patch; frost pale blue; a faster shell is smaller and leaves a streak.

**Base line**

- [ ] ⭐ 🟡 **Leads its target**: aims where the enemy will be when the shell lands. Base, or
  Ballistics II.
  - 💬
- [ ] 🟢 **Dead zone of 1 cell**: artillery can't hit what's under it. A trade-off for its long
  range, and a reason to build it back from the path.
  - 💬
- [ ] Base chill: [ ] keep / [ ] ⭐ move it to a payload (Cryo shells), so base Mortar is a pure
  boom and not a slower Splash.
  - 💬

**BASE**

- [ ] ⭐ 🟢 Fortify **Reinforced Barrel**: jam-proof, shell speed +25%.
  - 💬
- [ ] 🟢 Range II +15%, Range III +10%.
  - 💬
- [ ] 🟡 Range III **Long Battery**: +30% range, dead zone 1.5 cells.
  - 💬

**HEAD chain A - Siege Rounds** (the big boom). Exists: I +30% damage; II +25% damage, +40% radius.

- [ ] ⭐ 🟡 III **Heavy Shell**: +50% damage, -20% fire rate, a bigger, slower shell; enemies within
  0.5 cells of the impact are Dazed for 0.5 s.
  - 💬
- [ ] ⭐ 🔴 IV-A **Tactical Nuke** (your nuke): every 4th shell deals x4 damage over x2 radius and
  leaves Fallout for 4 s (spirit drained, regeneration turned into damage).
  - 💬
- [ ] ⭐ 🟡 IV-B **Bunker Buster**: the enemy at the centre takes x3 and is Cracked (plating -50%,
  5 s) and Sundered; the splash shrinks 30%. The anti-boss, anti-armor shell.
  - 💬

**HEAD chain B - Fragmentation Rounds** (swarms). Exists: I shrapnel ring at 25%; II shrapnel
chills.

- [ ] ⭐ 🟡 III **Cluster Shell**: splits into 4 bomblets scattered along the path around the impact
  (40% each, 1-cell radius).
  - 💬
- [ ] 🟡 IV-A **Carpet Bombing**: 8 bomblets laid in a line along the path ahead of the impact.
  - 💬
- [ ] ⭐ 🟡 IV-B **Shrapnel Storm**: shrapnel makes enemies Bleed (damage per cell travelled). The
  faster they run, the more they bleed.
  - 💬

**Extra head node - Ballistics** (your projectile speed, size and look)

- [ ] ⭐ 🟢 I **Rifled Barrel**: shell speed +40%, drawn smaller with a streak.
  - 💬
- [ ] ⭐ 🟡 II **Predictive Fire**: leads the target (if not base).
  - 💬
- [ ] 🟡 III **Airburst**: detonates above the target: +25% radius, no falloff in the inner half.
  - 💬
- [ ] 🟡 IV **Twin Barrels**: two shells per shot at the two leading enemies, -15% damage each.
  - 💬

**Specials**

- Cursed Shrapnel (exists): the blast applies Vulnerable.
- [ ] ⭐ 🟡 Cursed Shrapnel payload: Cracked (plating -50%) instead of Vulnerable. Explosions crack
  plates (see 1.4).
  - 💬
- [ ] ⭐ 🔴 **Napalm** (your burning ground): the impact leaves burning ground (1-cell radius, 3 s);
  anything inside burns (a burn pool, earning Scorched like Cinder's). Magic damage from a physical
  tower.
  - 💬
- [ ] ⭐ 🔴 **Tar** (your sticky matter): the impact leaves tar (1.2 cells, 4 s): 40% slow and
  poison; a tarred enemy that catches fire burns at double pool.
  - 💬
- [ ] 🔴 **Cryo Shells**: frost ground (3 s); chill builds while inside, and 2 s inside without
  leaving freezes (diminishing returns apply).
  - 💬
- [ ] 🔴 **Gravity Shell**: before detonating, pulls enemies within twice the radius 0.75 cells
  toward the impact along the path. Clumps for everyone.
  - 💬
- [ ] 🟡 **Shell Shock**: blasted enemies are Silenced for 2 s.
  - 💬
- [ ] 🔴 **Fallout**: irradiated ground (5 s): -10 spirit a second, heals inside become damage.
  - 💬

**Pairings**

- [ ] ⭐ Napalm + Tar -> **Inferno Pit**: napalm landing on tar sets it ablaze: double burn, and the
  patch lasts twice as long.
  - 💬
- [ ] ⭐ Napalm + Cryo -> **Thermal Shock**: freezing inside napalm detonates the burn at once
  instead of putting it out.
  - 💬
- [ ] ⭐ Gravity + Tar: pulled into tar and held there for the next shell.
  - 💬
- [ ] Gravity + Cursed Shrapnel: everyone pulled is Cracked too.
  - 💬
- [ ] Cryo + Gravity: pulled together and frozen together; a shatter spreads to the whole clump.
  - 💬
- [ ] Fallout + Tar: tar's poison doubles in fallout.
  - 💬
- [ ] Shell Shock + Gravity: casters are dragged out of formation and silenced.
  - 💬
- [ ] Napalm + Cursed Shrapnel: burning in napalm keeps an enemy Cracked.
  - 💬

**More ideas**

- [ ] 🟡 **Delayed Fuse**: the shell lies 1 s before detonating (a mine); pairs with Gravity.
  - 💬
- [ ] 🟡 **Barrage**: 3 shells in quick succession, then a long reload.
  - 💬
- [ ] 🟡 **Spotter Call**: +50% range against targets a Sonar has marked.
  - 💬
- [ ] 🟢 Shell drawn larger as its damage grows, whichever node raised it.
  - 💬

### 3.7 Seeker - the Hunter

**Why build it.** It never misses and never lets anything get away. The only single-target magic
tower: its missile ignores physical armor and plating, homes onto fast enemies, keeps its lock
through invisibility, strips shields and freezes. It is the answer to whatever breaks your defence:
the Frenzied rush, the Ghost, the shielded Warden. Its nest stores missiles in quiet moments and
releases them as a salvo when the dangerous enemy arrives. That is why it is single-target and
magic: it is a precision instrument for the one enemy your physical towers can't handle.
**Buy it when** enemies are fast, evasive, shielded or plated, or a boss arrives after a quiet
stretch. **Weak against** swarms and long waves of weak enemies. **Look:** slower missiles with
smoke trails and a visible curve; stored missiles orbit the tower as the nest fills; payload
colours (ice blue freeze, violet arcane, yellow EMP, red tracer).

**Base line**

- [ ] ⭐ 🟡 **Nest** (your idea): the cooldown loads a missile into the nest (up to 3) instead of
  firing; with a target in range, stored missiles launch 4 ticks apart. Idle time becomes a burst.
  - 💬
- [ ] 🟢 The nest fills between waves too: [ ] yes, a free opening salvo / [ ] no, only during waves.
  - 💬
- [ ] ⭐ 🟢 **Slower missiles** (35 -> 22): you can see them hunt, and the nest has time to matter.
  - 💬
- [ ] ⭐ 🟡 **Sticky targeting**: keeps firing at its current target until it dies or leaves range.
  Fixes the freeze reordering (0.8).
  - 💬
- [ ] ⭐ 🟡 **Lock-on**: a missile keeps its target through invisibility. The Ghost's first-hit
  vanish no longer shakes it off.
  - 💬

**BASE**

- [ ] ⭐ 🟢 Fortify **Launch Rails**: jam-proof, nest +1.
  - 💬
- [ ] 🟡 Fortify alternative: target priority "fastest enemy" (the runner hunter).
  - 💬
- [ ] 🟢 Range II +15%, Range III +10%.
  - 💬
- [ ] 🟡 Range III **Over the Horizon**: may fire at revealed or marked targets within 1.5x range.
  - 💬

**HEAD chain A - Twin Warhead** (more missiles). Exists: I +30% fire rate; II two missiles per shot.

- [ ] ⭐ 🟡 III **Hive**: nest of 6; a salvo spreads across different targets.
  - 💬
- [ ] ⭐ 🟡 IV-A **Swarm**: each missile splits into 3 micro-missiles at launch (35% damage each,
  chill instead of freeze). The anti-swarm Seeker.
  - 💬
- [ ] ⭐ 🟡 IV-B **Relay**: a missile that kills flies on to a new target at 60% damage, up to 3
  kills.
  - 💬

**HEAD chain B - Deep Freeze** (control). Exists: I +30% damage; II +25% damage, +75% freeze,
shatter on a frozen kill.

- [ ] ⭐ 🟡 III **Brittle**: frozen enemies take +30% physical damage. Seeker freezes, Sniper and
  Mortar smash.
  - 💬
- [ ] ⭐ 🟡 IV-A **Absolute Zero**: the missile freezes everything within 1 cell of the impact;
  shatters deal x2.
  - 💬
- [ ] 🟡 IV-B **Permafrost**: freeze +100%; a target the freeze can't hold (immune or diminished) is
  chilled 60% instead and takes +25% magic.
  - 💬

**Extra head node - Mixed Payloads** (your "some freeze, some do something else")

- [ ] ⭐ 🟡 I: every 3rd missile is **Arcane**: Unraveled (magic resist down) instead of a freeze.
  - 💬
- [ ] ⭐ 🟡 II: every 3rd missile is **EMP**: strips shields and Silences for 2 s.
  - 💬
- [ ] 🟡 III: every 3rd missile is a **Tracer**: reveals and Marks (the next hit crits).
  - 💬
- [ ] 🟡 IV: the nest cycles Cryo, Arcane, EMP, Tracer, each payload +25% stronger. The orbiting
  missiles show the order.
  - 💬

Alternative extra node - **Guidance**:

- [ ] 🟡 I lock-on through invisibility (if not base); II missile speed and turn rate +50%; III
  retargets without slowing when its target dies; IV **Seek and Destroy**: fires at Marked enemies
  anywhere on the map.
  - 💬

**Specials**

- Homing Curse (exists): impact applies 1 Vulnerable, 2 if the target was frozen or chilled.
- [ ] ⭐ 🟡 Homing Curse payload: Unraveled instead of Vulnerable. Seeker becomes the magic enabler
  for Cinder, Pulse and Sonar (see 1.4).
  - 💬
- [ ] ⭐ 🟡 **Nullifier**: impact strips shields and heals over time; +50% damage against shielded
  enemies. The Warden answer.
  - 💬
- [ ] ⭐ 🟡 **Hunter's Mark**: each consecutive hit on the same target +20% (up to +100%); prefers the
  highest rank. The boss hunter.
  - 💬
- [ ] 🟡 **Heat Seeker**: prefers burning targets; on a burning target the freeze is replaced by a
  Thermal Shock (the burn detonates at 150%). Fire and ice on one tower.
  - 💬
- [ ] 🟡 **Swarm Nest**: nest x2, and a full salvo launches by itself when an Elite or Boss enters
  range.
  - 💬
- [ ] 🟡 **Arcane Missiles**: Unraveled on every impact, if Homing Curse keeps Vulnerable.
  - 💬

**Pairings**

- [ ] ⭐ Hunter's Mark + Nullifier: the hunted target can't be shielded or healed while marked.
  - 💬
- [ ] ⭐ Swarm Nest + Hunter's Mark: every missile in a salvo counts as a consecutive hit (the last
  ones land at +100%).
  - 💬
- [ ] Homing Curse + Hunter's Mark: the hunted target's stack cap rises by 2.
  - 💬
- [ ] Nullifier + Arcane Missiles: a stripped shield leaves 3 Unraveled stacks behind.
  - 💬
- [ ] Swarm Nest + Arcane Missiles: each missile in a salvo stacks Unraveled.
  - 💬
- [ ] Heat Seeker + Homing Curse: each Thermal Shock applies the full payload stack.
  - 💬
- [ ] Heat Seeker + Swarm Nest: a salvo launches by itself at any burning Elite or Boss.
  - 💬

**More ideas**

- [ ] 🟡 **Proximity Fuse**: a missile passing within 0.5 cells of 3 or more enemies bursts for a
  small blast.
  - 💬
- [ ] 🟡 **Decoy Flare**: a missile whose target vanished flies to its last known spot and reveals
  everything within 1 cell.
  - 💬
- [ ] 🟡 **Afterburner**: missiles speed up the longer they fly, and hit harder for it.
  - 💬

### 3.8 Cinder - the Pyre

**Why build it.** The magic damage-over-time tower: it shrugs off armor and plating, scorches
resilience so every crit lands harder, and burns what hides or heals. Its cone is short, so build it
where the path bends around it: each enemy walks through several waves. The crit team's best
friend and the healer's worst enemy. **Buy it when** groups of mid-health enemies come, against
healers (Menders) and regenerating bosses, against ghosts that pass close, alongside a crit
defence. **Weak against** far-away straights, burn-immune enemies (the Warden's second egg) and
freeze-heavy teammates (freeze puts fire out, unless Thermal Shock). **Look:** flame colour by tier:
orange, white (White Flame), blue (Soulfire), violet (Hexflame); lingering flames as flickering
patches.

**Base line**

- [ ] ⭐ 🟡 **Burning reveals** (your idea): a burning invisible enemy is visible. Written as an
  interaction row, so any burn does it (Napalm too).
  - 💬
- [ ] 🟡 **Burning enemies receive 50% less healing** at base (or with Fuel II).
  - 💬
- [ ] ⭐ 🟢 **Burns that crit** (your idea): already possible, since a burn pulse rolls its tower's
  crit chance. Make it Flashpoint's feature.
  - 💬

**BASE**

- [ ] ⭐ 🟢 Fortify **Pilot Light**: jam-proof; the first wave at a fresh target burns 50% harder.
  - 💬
- [ ] 🟢 Range II +15%, Range III +10%.
  - 💬
- [ ] 🟡 Range III **Long Nozzle**: +20% range, and the wave travels twice as fast.
  - 💬

**HEAD chain A - White Flame** (intensity: short, extremely hot). Exists: I +30% damage; II +25%
damage, +50% burn duration.

- [ ] ⭐ 🟢 II tweak: "+50% damage, -25% burn duration" instead of "+50% duration", so this chain
  means *hot and short* and chain B means *wide and long*.
  - 💬
- [ ] ⭐ 🟡 III **Soulfire** (your higher-level burn): a second burn type, blue, in its own pool, so
  it stacks with ordinary burn; it earns Sickened as well as Scorched, draining spirit twice as fast
  (your idea).
  - 💬
- [ ] ⭐ 🟡 IV-A **Flashpoint**: +20% crit chance; burn pulses crit for x1.5, and each crit adds 3
  Scorched.
  - 💬
- [ ] ⭐ 🟡 IV-B **Combustion**: the pool cap doubles; a pool that reaches its cap bursts for half
  of it onto enemies within 1 cell.
  - 💬

**HEAD chain B - Wide Nozzle** (area, long burns). Exists: I +25% range, +30% cone; II +20% range,
+20% fire rate, +20% cone.

- [ ] ⭐ 🔴 III **Lingering Flames**: each wave leaves burning ground on the path it swept for 2 s.
  - 💬
- [ ] ⭐ 🟢 IV-A **Inferno Ring**: the cone becomes a full ring at -25% range. A burning Pulse.
  - 💬
- [ ] ⭐ 🟢 IV-B **Dragon's Breath**: x3 fire rate, per-wave damage divided by 2.5, +30% range: a
  continuous stream that keeps every pool topped up.
  - 💬

**Extra head node - Fuel** (burn chemistry and the debuffs it causes)

- [ ] ⭐ 🟡 I **Kindling**: burning enemies lose 30% burn resistance and earn Scorched twice as fast.
  - 💬
- [ ] ⭐ 🟡 II **Cauterize**: burning enemies receive 50% less healing and shielding.
  - 💬
- [ ] 🟡 III **Illumination** (if burning doesn't reveal at base), else **Heat**: burning enemies
  take +10% magic damage.
  - 💬
- [ ] 🟡 IV **Everburn**: a pool never decays below 25% while its enemy is inside Cinder's range.
  - 💬

**Specials**

- Hexflame (exists): a new ignition also grants Vulnerable.
- [ ] ⭐ 🔴 **Wildfire**: once a second a burning enemy ignites neighbours within 0.75 cells at 40%
  of its pool; on death the whole pool spreads. The swarm killer.
  - 💬
- [ ] ⭐ 🟡 **Thermal Shock**: when a burning enemy is frozen by anything, its remaining pool
  detonates as one hit at 150% and chills its neighbours. Fire and ice become a combo instead of a
  conflict.
  - 💬
- [ ] 🟡 **Heat Haze**: burning enemies are Exposed (crit chance taken x1.5).
  - 💬
- [ ] 🟡 **Pyromancer's Mark**: burning enemies take +15% magic damage from every source.
  - 💬
- [ ] 🔴 **Phoenix Ash**: an enemy that dies burning leaves embers (1 cell, 2 s) that burn whatever
  it spawns: Reaver's split, the Frenzied brood, the Warden's egg.
  - 💬

**Pairings**

- [ ] ⭐ Hexflame + Wildfire: spread ignitions grant Vulnerable too. A whole wave vulnerable.
  - 💬
- [ ] ⭐ Wildfire + Thermal Shock: detonations spread the burn instead of ending it.
  - 💬
- [ ] Hexflame + Heat Haze: burning means vulnerable and exposed: the crit team's ideal target.
  - 💬
- [ ] Heat Haze + Thermal Shock: detonations always crit.
  - 💬
- [ ] Thermal Shock + Pyromancer's Mark: detonations leave 2 Unraveled stacks.
  - 💬
- [ ] Phoenix Ash + Wildfire: embers spread like the wildfire does.
  - 💬
- [ ] Hexflame + Pyromancer's Mark: newly ignited enemies take the magic bonus at double for 2 s.
  - 💬

**More ideas**

- [ ] 🔴 **Fire Wall**: a line of fire across the path in front of the tower, 3 s, every 10 s.
  - 💬
- [ ] 🟡 **Backdraft**: the first wave after 3 s of idling is twice as wide.
  - 💬
- [ ] 🔴 **Ember Trail**: a burning enemy drops small embers on the path behind it, so the enemies
  following it catch fire. Strongest against columns and drips.
  - 💬

---

## 4. Cross-tower combos

Where the roles above meet. These are not extra content: each falls out of ideas already listed,
and together they are the reason to build a mixed defence. Tick the ones worth teaching the player
(tower descriptions, a hint on the level screen).

- [ ] ⭐ **Spotter team**: Sonar marks -> the next Sniper shot is a guaranteed crit -> Momentum
  charges. Wide Band + Mark on Sweep makes a revealed Ghost die to one Sniper shot.
  - 💬
- [ ] ⭐ **Burn the armor off crit immunity**: Cinder's Scorched pushes resilience down; Sonar's
  Resonant Crack does it in a burst; Sniper's Deadeye goes through the rest.
  - 💬
- [ ] ⭐ **Frostfire**: Seeker freezes a burning enemy -> the burn bursts (the new interaction row,
  150% with Thermal Shock).
  - 💬
- [ ] ⭐ **Freeze and smash**: Seeker's Brittle -> frozen enemies take +30% physical from Sniper and
  Mortar.
  - 💬
- [ ] ⭐ **Inferno pit**: Mortar's Tar + any burn (Cinder, Napalm) -> double pools.
  - 💬
- [ ] ⭐ **Kill box**: Pulse's Kill Zone + Shockwave in a loop, Mortar and Sonar aimed into it.
  - 💬
- [ ] **Magnetic artillery**: Pulse's Magnetic Field -> Mortar shells never miss inside it.
  - 💬
- [ ] **Soul-linked sniping**: Splash's Soul Link -> every Sniper hit spreads to the linked group.
  - 💬
- [ ] ⭐ **Magic chain**: Seeker's Unraveled and Sonar's Resonating -> Cinder, Pulse and Sonar's
  Pure Tone hit far harder.
  - 💬
- [ ] ⭐ **Physical chain**: Sniper's Sunder, Mortar's Cracked, Pulse's Corrosion -> every physical
  tower hits harder.
  - 💬
- [ ] **Plague**: Splash's Contagion spreads Cinder's burn and Toxic Bloom's poison on every death.
  - 💬
- [ ] **Stasis field**: Pulse's Stasis keeps every other tower's debuffs alive while enemies stay
  inside.
  - 💬
- [ ] **The hero**: Aura's Chosen + Keen Edge on one Transcendent Sniper.
  - 💬
- [ ] **Focus fire**: Sonar's Command Ping points every tower at the healthiest enemy.
  - 💬
- [ ] **Anti-caster wall**: Pulse's Null Field + Seeker's EMP + Mortar's Shell Shock: the Warden
  never gets to shield or summon.
  - 💬
- [ ] 🟡 **Tower bonds** (a rule, not content): specific adjacent pairs get a small named bonus,
  shown when placed. Sniper beside Sonar (+10% crit), Cinder beside Seeker (Thermal Shock at 100%
  without the special), Mortar beside Pulse (+15% radius), Splash beside Cinder (poison and burn
  pools +20%). Makes layout a puzzle.
  - 💬

---

## 5. Enemies that give each tower a job

You want to focus on enemies next. Each of these exists to make one role matter, so a level can
ask for a tower by what it sends. Stats are existing `EnemyStat`s; abilities use today's triggers
unless tagged.

- [ ] ⭐ **Jammer into the waves** (exists, unused): Fortify and Sonar's Counter-Jamming only matter
  once it shows up.
  - 💬
- [ ] ⭐ 🟢 **Juggernaut**: huge physical plating, slow. Needs magic, Cracked or Sunder. (Pulse,
  Seeker, Mortar's Bunker Buster)
  - 💬
- [ ] ⭐ 🟡 **Priest**: high spirit, cleanses allies' debuffs every 5 s. Needs Silence, Soul Drain,
  or hexes that punish cleansing. (Pulse, Splash)
  - 💬
- [ ] ⭐ 🟡 **Blinker**: teleports 1.5 cells forward when hit (3 s cooldown). Needs lock-on,
  freeze or Null Field. (Seeker, Pulse)
  - 💬
- [ ] ⭐ 🟢 **Mites**: a swarm of 12 tiny enemies that die to one hit each. Needs area. (Pulse,
  Cinder's Wildfire, Mortar's Cluster, Splash's Carpet)
  - 💬
- [ ] 🟢 **Courier**: very fast, fragile, high bounty; worth hunting. (Seeker, chill, Sniper)
  - 💬
- [ ] ⭐ 🟢 **Shieldbearer**: periodically gives nearby allies a *physical-only* shield. Gives the
  typed shield in `TODO.md` its first user, and asks for magic or Nullifier.
  - 💬
- [ ] 🟡 **Drummer**: a haste aura for allies. Needs Anchored or Hex of Inversion. (Pulse, Splash)
  - 💬
- [ ] 🟢 **Salamander**: burn-immune, fire-coloured. Punishes a Cinder-only defence.
  - 💬
- [ ] 🟢 **Yeti**: freeze-immune, 50% chill resist. Punishes a Seeker-only defence; Permafrost's
  fallback matters.
  - 💬
- [ ] 🟡 **Thornback**: each crit taken gives it +20 armor for 3 s. Punishes crit spam; Sunder and
  magic answer it.
  - 💬
- [ ] 🔴 **Burrower**: submerged (untargetable, immune to hits) on part of the path; ground effects
  and the Pulse field still hurt it. (Mortar, Cinder, Pulse)
  - 💬
- [ ] 🟡 **Necromancer**: once per 8 s raises the last ally that died nearby. Needs Silence, Phoenix
  Ash, or killing it first. (Pulse, Cinder, Sniper)
  - 💬
- [ ] 🟡 **Mirror**: resists the damage type of the last hit it took. Needs mixed damage: Silver
  Ammunition, Sonar's Frequency.
  - 💬
- [ ] 🟡 **Elite affixes**: Elite and Boss ranks roll one random affix per spawn (hasted, shielded,
  regenerating, splitting, thorny). Cheap variety without new enemies.
  - 💬

---

## 6. Wild ideas

Bigger swings, mostly outside the tower trees. Each is a feature of its own; tick only what you'd
want a request for.

- [ ] 🟡 **Player focus**: the enemy you select in the inspector becomes the priority target of every
  tower that can reach it. Enemy selection already exists; this makes it a tactical input.
  - 💬
- [ ] 🔴 **Transcendent actives**: a Transcendent tower gains one click-to-cast ability with a
  cooldown. Sniper *Assassinate* (a guaranteed crit on the selected enemy), Mortar *Barrage* (5
  shells at a clicked spot), Pulse *Overload* (x5 damage for 2 s), Seeker *Salvo* (empty the nest),
  Cinder *Firestorm* (ring of fire), Sonar *Ping* (reveal the whole map for 3 s), Splash *Hex Nova*.
  - 💬
- [ ] 🔴 **Boss rewards**: after each boss wave, pick 1 of 3 global perks for the rest of the level
  (roguelite). Huge replay value, easy to tune per level.
  - 💬
- [ ] 🟡 **Interest**: unspent credits earn 5% at each wave start. Spend now or save for Transcendent.
  - 💬
- [ ] 🟢 **Early call bonus**: starting the next wave while the current one is still alive pays
  credits.
  - 💬
- [ ] 🔴 **Global upgrades** (the buy-once, per-type upgrade in `TODO.md`): an "Academy" menu.
  - 💬
- [ ] 🟡 **Overheat**: towers that fire nonstop for 20 s lose fire rate until they rest. Pulse and
  Cinder would care most.
  - 💬
- [ ] 🟡 **Weather per level**: rain (burn -25%, chill +25%), heat (the reverse), fog (range -15%,
  Sonar unaffected).
  - 💬
- [ ] 🟡 **Tower veterancy stars**: auto-earned by kills, +3% damage each, purely a feel-good layer.
  - 💬
- [ ] 🟡 **Sacrifice**: selling a Transcendent tower passes its kills and damage to its neighbours.
  - 💬
- [ ] 🔴 **Respec**: swap a bought special once per level at a price (out of scope for iteration 2).
  - 💬
- [ ] 🟡 **Combo feedback**: a small glyph burst when a combo fires (Thermal Shock, Plague, Inferno
  Pit), so the player learns the combos exist. No text rendering needed.
  - 💬

---

## 7. Suggested answers to the iteration-2 open questions

From `FEATURE-tower-upgrades-iteration-2.md`, with the answer I'd pick.

- [ ] ⭐ **Does the extra head node branch?** No. It is the safe, non-exclusive line; branching
  would make it a third chain.
  - 💬
- [ ] ⭐ **Does its level IV need Transcendent?** Yes, like every level 4.
  - 💬
- [ ] ⭐ **Does its level III count toward Transcendent?** No. Transcendent should need a commitment
  to one chain, and the extra node commits to nothing.
  - 💬
- [ ] ⭐ **Its content**: per tower in section 3 (Silver Ammunition, Arc Emitter, Frequency, Field
  Shaping, Tutelage, Ballistics, Mixed Payloads, Fuel).
  - 💬
- [ ] ⭐ **Second special: free or restricted?** Free, with hand-written pairings (section 3) and a
  "+20% to both" fallback. Or restrict by tag (2.9).
  - 💬
- [ ] ⭐ **Transcendent price**: about 4x the tower's price.
  - 💬
- [ ] ⭐ **Range II and III**: +15% then +10%; Range III keeps needing Transcendent, as part of the
  spike.
  - 💬
- [ ] ⭐ **Pairing authorship**: hand-designed for the pairs listed here, fallback for the rest.
  - 💬
- [ ] ⭐ **Single-special towers**: get their second (and third) special in this iteration.
  - 💬
- [ ] ⭐ **Visibility until unlocked**: show Transcendent locked, with what's missing ("needs a
  special and a level 3 head"). It is the goal; hiding it hides the game's ceiling.
  - 💬
- [ ] ⭐ **Anything on top?** No performance gate. The two prerequisites and the price are enough.
  - 💬
- [ ] ⭐ **Rollout order and first proving tower**: Splash first, since both its chains already
  reach level 3 and it has three specials, so only IV-A/IV-B and pairings are new. Then Sniper
  (clearest identity), then Pulse (most in need), then the rest.
  - 💬
- [ ] ⭐ **Exclusive-choice mark**: a bracket joining the set, a "1 of 2" / "1 of 3" label on it,
  and a lock glyph on the alternatives once one is bought. Panel only; the tower's pips stay as
  they are.
  - 💬
- [ ] ⭐ **Transcendent's own look**: a slow halo ring on the base and a gold pip.
  - 💬

---

## 8. Shortlist

If I could build only these, in roughly this order. Each unlocks or fixes the most for
its cost.

1. [ ] **Roles and stat ownership** (1.1, 1.2): decide these first; everything else follows.
   - 💬
2. [ ] **Pulse deals magic, with the Field Shaping line** (Undertow, Corrosion, Stasis) and Null
   Field: fixes the weakest tower and gives it a job no other tower has.
   - 💬
3. [ ] **Five debuffs**: Sundered, Unraveled, Cracked, Exposed, Marked (plus Silenced). They feed a
   third of the ideas in this document and un-duplicate Vulnerable.
   - 💬
4. [ ] **Crit per tower**: Sniper x2.0, a crit-damage buff axis, Aura's Keen Edge, Fifth Shot as
   "+50%".
   - 💬
5. [ ] **Seeker's nest, sticky targeting and lock-on**, plus Nullifier: the Hunter gets a reason
   to exist.
   - 💬
6. [ ] **Ground effects** with Mortar's Napalm and Tar, and the Nuke / Bunker Buster choice: the
   Artillery becomes its own thing.
   - 💬
7. [ ] **Splash as the Spreader**: densest-group targeting, Arc Emitter, Soul Link or Contagion.
   - 💬
8. [ ] **Cinder**: burning reveals, Thermal Shock (fire and ice as a combo), Soulfire.
   - 💬
9. [ ] **Aura**: no aura-on-aura, Anchor Fortify, Tutelage (gate help), Keen Edge.
   - 💬
10. [ ] **Kill gates count assists**, plus a gate buyout.
    - 💬
11. [ ] **Fortify = jam-proof plus a small perk**, and the Jammer in real waves.
    - 💬
12. [ ] **Splash as the first Transcendent tower**, then Sniper.
    - 💬
13. [ ] **Slow the projectiles** (Mortar and Seeker to about 8 px a tick) so their identities
    show at all (9.1).
    - 💬
14. [ ] **Fix the upgrade economy**: a rising price per copy of a tower type, damage gates x10,
    specials priced above head III (9.3).
    - 💬
15. [ ] **Rein in the outliers**: Toxic Bloom, Momentum, the code's Amplifying Core (9.4).
    - 💬

---

## 9. Challenging what exists

Everything above adds. This section questions what is already there: base stats, the shipped
upgrade nodes and the names. Numbers are computed from the code (not the feature doc's tower
lines, where they differ) for a Simple mob walking at its base speed, 0.8 cells a second, straight
through the tower's centre. Expected crit is included. Cinder's figure is its steady burn while an
enemy stays in the cone.

### 9.1 The numbers today

| Tower | Price | Hit | Hits/s | DPS | DPS per credit | Range | Secs in range | Dmg per pass | vs Armored Elite | vs Warden |
|---|---|---|---|---|---|---|---|---|---|---|
| Sniper | 10 | 30 phys | 0.50 | 16.1 | **1.61** | 3.8 | 9.5 | 153 | 5.0 | 10.0 |
| Splash | 15 | 16 phys + blast | 1.00 | 16.0 | 1.07 | 3.2 | 8.0 | 128 | 1.6 | 6.0 |
| Sonar (code) | 20 | 16 phys | 0.50 | 8.0 each | 0.40 each | 5.2 | 13.0 | 104 each | 0.8 | 3.0 |
| Sonar (doc line) | 20 | 16 phys | 0.25 | 4.0 each | 0.20 each | 4.2 | 10.5 | 42 each | 0.4 | 1.5 |
| Pulse | 25 | 2 phys a tick | 20 | 40 each | **1.60 each** | 1.5 | 3.8 | 150 each | **0** | **0** |
| Mortar | 30 | 20 phys + blast | 0.39 | 7.8 | 0.26 | 4.0 | 10.0 | 78 | 1.6 | 3.9 |
| Seeker | 35 | 26 magic | 0.43 | 11.3 | 0.32 | 4.5 | 11.2 | 127 | 6.8 | 11.3 |
| Cinder | 28 | burn, magic | ~1 wave | 23 each | 0.82 each | 2.2 cone | varies | ~130, +29 after | **13.8** | **23.1** |

"each" means every enemy it reaches gets this. Armored Elite: armor 67 against both damage types
(keeps 60%), 8 physical plating, crit-immune. The Warden: 10 physical plating, 8000 health.

What the table says:

1. **Pulse is not weak on paper.** It ties the Sniper for the most damage per credit, and deals it
   to every enemy in range: 150 to each one that walks through. It collapses only against plating,
   where it does exactly nothing. That is the late game, which is where it *feels* weakest.
   - 💬
2. **The cheapest tower is the best one.** Sniper leads single-target damage per credit by 50% over
   Splash and 5x over Seeker, and a x2.0 crit would widen the gap.
   - 💬
3. **Mortar and Seeker are the worst per credit** (0.26 and 0.32), as `TODO.md`'s tests found.
   - 💬
4. **Projectiles are effectively instant.** A Mortar shell flies 40 px a tick (25 cells a second,
   0.16 s to max range); a Seeker missile 35 (22 cells a second). Nothing can dodge a shell and
   nobody sees a missile hunt: both towers' stated identities never show on screen.
   - 💬
5. **Plating erases every small-hit tower.** Against an Armored Elite, Splash, Sonar and Mortar
   deal under 2 DPS and Pulse none; only the magic towers and the big-hit Sniper work. 8 plating on
   a 16-point hit is -50% before armor even applies.
   - 💬
6. **Upgrading is 3 to 12 times worse per credit than building another copy** (9.3). Only board
   space and special effects make upgrades worth buying.
   - 💬
7. **Damage gates are almost free, kill gates grind.** A 100-300 damage gate clears in one or two
   enemy passes; 8-25 kills can take waves. Four of the Seeker's five nodes gate on kills, on the
   tower that kills least.
   - 💬
8. **The two Sonar lines differ 2.5x.** The code's 5.2 range and 2 s turn deal 104 per enemy per
   pass; the doc line's 4.2 and 4 s deal 42. `TODO.md`'s formation test had Sonar out-damaging
   everything, so a nerf is plausible, but -60% is a lot.
   - 💬
9. **Level III is a cliff.** Chains give +25-30% at I and II, then Blast Engineering III triples
   the blasts and Twin Array III doubles the hits.
   - 💬

### 9.2 Base stats, challenged

One coherent proposal:

- [ ] ⭐ **Sniper** $10 -> $15; damage 30 -> 40, cooldown 39 -> 49 (a shot every 2.5 s), range 4.0.
  18 DPS with crit, 1.2 per credit. Bigger, slower hits *are* the identity, and a 40-point hit
  keeps 16 through an Armored Elite instead of 10.
  - 💬
- [ ] ⭐ **Splash** damage 16 -> 14, blast radius 1.75 -> 1.5, densest-group targeting, 25%
  falloff floor. It aims its blast on purpose now, so it can afford less.
  - 💬
- [ ] ⭐ **Sonar** range 4.5, 3 s per turn: 60 per enemy per pass, between the code and the doc.
  - 💬
- [ ] ⭐ **Pulse** magic, range 1.75 (3.4): 175 per pass, and 24 DPS against an Armored Elite
  instead of 0.
  - 💬
- [ ] **Aura** $20 -> $25, +20% -> +15%: weaker alone, strong once upgraded, since its upgrades
  multiply.
  - 💬
- [ ] ⭐ **Mortar** damage 20 -> 32, cooldown 50 -> 69 (a shell every 3.5 s), range 4.5, blast
  2.0 -> 1.75, shell speed 40 -> 8 px a tick (0.9 s flight). A big slow boom that fast enemies can
  dodge, and 32 survives plating.
  - 💬
- [ ] ⭐ **Seeker** $35 -> $30; damage 26 -> 40 magic; missile speed 35 -> 8 px a tick; nest of 3.
  17 DPS, 0.58 per credit, and you can watch it hunt.
  - 💬
- [ ] **Cinder** cone half-width 0.35 -> 0.45 rad. Today the cone covers 1.7 square cells, so an
  enemy crossing it sideways barely burns.
  - 💬

Rules behind the numbers:

- [ ] ⭐ **Damage-per-credit bands by role**: single-target about 1.2; area towers 0.4-0.6 per enemy
  reached; control and support priced by their effect. Written down, so every later number has a
  reference.
  - 💬
- [ ] ⭐ 🟢 **Plating is the anti-small-hit stat on purpose.** Keep it, but then every small-hit tower
  needs one answer in its tree (magic, Cracked, a burst); section 3 lists one per tower.
  - 💬
- [ ] 🟢 Settle the three doc/code mismatches: Sonar's range and turn, Splash's chill, Cinder's
  damage (the line says 2, the code 1.5).
  - 💬

### 9.3 The upgrade economy

First stat node of a chain against building one more copy of the same tower:

| Node | Price | Gain | DPS per credit | Building another | Ratio |
|---|---|---|---|---|---|
| Sniper Focused Optics I | 25 | +20% damage | 0.13 | 1.61 | 8% |
| Splash Rapid Battery I | 30 | +25% fire rate | 0.18 | 1.07 | 17% |
| Sonar Twin Array I | 35 | +25% damage | 0.06 | 0.40 | 14% |
| Pulse Overcharged Coils I | 30 | +30% damage | 0.40 | 1.60 | 25% |
| Mortar Siege Rounds I | 35 | +30% damage | 0.07 | 0.26 | 26% |
| Seeker Twin Warhead I | 30 | +30% fire rate | 0.15 | 0.32 | 46% |
| Cinder White Flame I | 30 | +30% damage | 0.23 | 0.82 | 28% |

Each of these also needs Awaken first (and Fortify soon). Meanwhile the specials, cheaper than head
nodes, are the strongest upgrades: Momentum ($20) is about +60% Sniper damage plus armor-ignoring
shots, Fifth Shot about +38%, and Toxic Bloom ($30) adds about 30 magic DPS to every enemy a blast
reaches, nearly triple the blast itself. In the code, Amplifying Core II is the one upgrade that
beats building: x2.6 output for a whole cluster, for $70 of upgrades.

- [ ] ⭐ 🟡 **Each extra copy of a tower type costs more** (+15% per copy already on the board). The
  classic anti-spam rule: upgrading becomes the efficient path for your fourth Sniper, and it pushes
  mixed defences.
  - 💬
- [ ] ⭐ **A value rule for stat nodes**: damage gained per credit between 0.7x and 1x of building
  another copy. Focused Optics I would be +20% for $2-3, or +60% for $8-10; prefer bigger bonuses
  to tiny prices.
  - 💬
- [ ] ⭐ **Specials cost more than head III** (say 4-6x the tower's price): the most powerful slot
  should not be the cheapest.
  - 💬
- [ ] ⭐ **Damage gates x10-15** (1000-5000 damage), so they wait about as long as the kill gates
  beside them.
  - 💬
- [ ] ⭐ **Each chain gates on its purpose** (2.8): crit chains on crits landed, freeze chains on
  freezes, reveal chains on reveals.
  - 💬
- [ ] 🟢 **Smooth the level III cliff**: Blast Engineering III gives 2 blasts (IV-A takes 5); Twin
  Array III's second beam at -25% damage.
  - 💬
- [ ] 🟢 **Adopt the feature doc's Amplifying Core numbers** (+25%, then +30% and +10% fire rate).
  The code's x1.5 twice, with a +45% fire rate, is the strongest upgrade in the game.
  - 💬

### 9.4 Existing upgrades, node by node

**Sniper**

- [ ] Focused Optics has no gates while Marksman's Eye needs 15 kills, so the free chain is always
  picked first. Gate both roots alike.
  - 💬
- [ ] ⭐ Marksman's Eye II mixes crit with armor penetration, but the enemies that have armor
  (Armored) are crit-immune: half the node is dead exactly where the other half matters. Make the
  chain pure crit and move penetration to Focused Optics (Heavy Caliber).
  - 💬
- [ ] 50% armor penetration barely matters: +11% against an Armored grunt's 25 armor, about +25%
  against an Elite's 67, and the plating (the real wall) is untouched. Plating penetration instead.
  - 💬
- [ ] ⭐ Momentum outclasses its siblings: about +60%, armor ignore and a fire-rate burst, against
  Fifth Shot's +38% and Marked Round's Vulnerable that mostly helps other towers. Momentum's shot
  x5 -> x3, or Marked Round applies 2 stacks.
  - 💬
- [ ] Every special switches targeting to most health, but Momentum's burst wants kills. Let
  Momentum keep "first".
  - 💬

**Splash**

- [ ] ⭐ Toxic Bloom is the biggest outlier in the game (9.3). Poison at 4% of weapon damage per
  tick instead of 10%, or for 2 s instead of 4 s.
  - 💬
- [ ] ⭐ Rapid Battery III (crits splash 50% bigger) triggers on 15% of shots: roughly +5-15% for
  $60, the weakest node in the game. Replace it: crits blast twice, or crits Daze.
  - 💬
- [ ] ⭐ Overpressure does nothing without Rapid Battery II (Splash has no base crit). Arm it every
  8th shot instead, or give Splash 5% base crit.
  - 💬
- [ ] Concussive Blast costs a third of the tower's damage for a chill and 50% death explosions.
  -25% fire rate, or no penalty and 30% explosions.
  - 💬
- [ ] Blast Engineering I (radius only) is worth little while the target is random; with
  densest-group targeting it's fine.
  - 💬

**Sonar**

- [ ] ⭐ Long Reach I gives +15% crit and no reach, on a tower without base crit: +7.5% for $30.
  Make I +20% range and move crit to II.
  - 💬
- [ ] Twin Array I and II gate on 100 and 200 damage: effectively no gate.
  - 💬
- [ ] Piercing Tone is the only thing that makes Sonar relevant against armor. Consider it for the
  base tower.
  - 💬

**Pulse**

- [ ] ⭐ "Needs a visible enemy to fire" makes Resonant Field I's headline a removed handicap. Drop
  the restraint at base and give Resonant Field I something real.
  - 💬
- [ ] Overcharged Coils II's +10% crit, rolled 20 times a second, is a hidden +5% damage. Say it as
  damage, or make Pulse crits do something (Daze?).
  - 💬
- [ ] Warding Field at 10% a tick reaches 3 stacks within 1.5 s on anything inside, so it just means
  "enemies inside are fully Vulnerable". Say that in the node text.
  - 💬

**Mortar**

- [ ] ⭐ Base Mortar chills 50% for 2 s on everything in a 2-cell blast (2.8 after Siege II): the
  game's biggest mass slow, which competes with its "big boom" identity. Choose one (3.6).
  - 💬
- [ ] Siege Rounds II's +40% radius makes the blast 5.6 cells wide, covering most of a path loop. A
  smaller base radius (1.75) so the step means something.
  - 💬
- [ ] Fragmentation Rounds II gates on 2 adjacent towers, which has nothing to do with shrapnel. A
  kill or purpose gate.
  - 💬
- [ ] Cursed Shrapnel puts Vulnerable on everything in that huge blast, outdoing Aura's Withering
  Field, the specialist. Cracked instead (1.4).
  - 💬

**Seeker**

- [ ] ⭐ Twin Warhead II's second missile flies at the same target: it overkills, and its freeze
  lands on an already frozen enemy. Send the second missile to the next target.
  - 💬
- [ ] ⭐ Deep Freeze I is +30% damage (no freeze), and II's +75% freeze is mostly eaten by
  diminishing returns (a second freeze within 10 s is halved). Swap: I +50% freeze, II damage and
  shatter.
  - 💬
- [ ] ⭐ Kill gates on the tower that kills least. Damage or freeze gates.
  - 💬

**Cinder**

- [ ] Hexflame grants a stack only on a *new* ignition, and Cinder refreshes its burn every second,
  so each enemy gets one stack (+15%) per visit. Let every wave that hits a burning enemy add a
  stack, or make a new ignition grant 2.
  - 💬
- [ ] White Flame II's +50% duration raises each burn's total by half (a burn's total scales with
  its duration): with I it's about +75% output. Strong; fine if chain B keeps up.
  - 💬

**Aura**

- [ ] Withering Field reaches only the aura's own 1.5 cells, and an Aura usually stands among
  towers rather than on the path, so it often touches nothing. Aura range + 1 cell for enemies.
  - 💬
- [ ] Awaken at 6 of 8 neighbours is steep next to every other tower's price-only Awaken. Fine if
  meant as "the Aura crowns a cluster".
  - 💬

### 9.5 Names

**Tower names.** The set mixes a person (Sniper), devices (Sonar, Mortar), a projectile (Seeker), a
material (Cinder) and plain effects (Splash, Pulse, Aura). An effect name describes what several
towers do; a name should say what this one *is*.

- [ ] Sniper: ⭐ keep. Alternatives: Marksman, Longshot.
  - 💬
- [ ] Splash: ⭐ rename. Burst (same meaning, but a thing rather than an effect); Hexer or Arcanist
  if it goes the hex way; Arc or Tesla if chain lightning; Blight if poison.
  - 💬
- [ ] Sonar: ⭐ Radar (a rotating beam is radar; sonar is pings). Or Lighthouse (a sweeping light
  that reveals the hidden), Scanner, Oracle.
  - 💬
- [ ] Pulse: ⭐ Obelisk (a magic stone that changes the rules around it). Or Coil if it zaps,
  Nexus, Resonator.
  - 💬
- [ ] Aura: ⭐ Beacon. Or Banner, Shrine, Totem, Relay.
  - 💬
- [ ] Mortar: ⭐ keep. Alternative: Bombard.
  - 💬
- [ ] Seeker: ⭐ Hive (it nests missiles). Or Launcher, Hunter, Falcon.
  - 💬
- [ ] Cinder: ⭐ Scorcher (it applies Scorched; a cinder is what's left after a fire). Or Brazier,
  Pyre, Flamer.
  - 💬
- [ ] ⭐ Rename display names only; keep internal ids (`sniper.head...`, `TowerFactory.Type`), so
  node ids and tests don't churn.
  - 💬

**Upgrade names**

- [ ] ⭐ Resonant Field (Pulse) and Resonance Field (Aura) are nearly the same name on two towers.
  Pulse: Phase Field or Ghostsight. Aura: Broadcast or Wide Signal.
  - 💬
- [ ] ⭐ Keep "hex" and "curse" for the hex mechanic. Homing Curse, Cursed Shrapnel and Hexflame all
  apply Vulnerable today: Weakening Warhead (or Arcane Warhead once it applies Unraveled), Rending
  Shrapnel (or Plate Cracker), Searing Flame.
  - 💬
- [ ] ⭐ Marked Round applies Vulnerable, while Mark on Sweep and the new Marked effect mean a
  guaranteed crit. Rename it Crippling Round or Hollow Point.
  - 💬
- [ ] Warding Field (Pulse) sounds protective but weakens enemies. Dread Field, Rattle Field,
  Unsettling Field.
  - 💬
- [ ] Chain names that level I doesn't deliver: Long Reach I (crit, no reach), Deep Freeze I
  (damage, no freeze), Twin Warhead I (fire rate, one missile), Twin Array I and II (damage, one
  beam). Rename the chain for its theme, or make level I do the named thing (9.4 does the latter).
  - 💬
- [ ] The base chain Fortify -> Awaken -> Transcendent mixes an engineering word with two spiritual
  ones. Reinforce -> Awaken -> Transcend; Attune -> Awaken -> Ascend; Forge -> Temper ->
  Masterwork; or Mk I, Mk II, Mk III.
  - 💬

### 9.6 Loose ends

- [ ] The toolbar and hotkey order (q to i) is Sniper, Splash, Sonar, Pulse, Aura, Mortar, Seeker,
  Cinder, at prices 10, 15, 20, 25, 20, 30, 35, 28. Order attackers by price with Aura last, or
  group by role.
  - 💬
- [ ] `README.md`'s tower section still describes the first upgrade system ("two permanent,
  mutually-exclusive upgrade paths... marked on the board by a coloured ring", "The Aura tower is
  passive and offers none"). It went stale when upgrade trees shipped.
  - 💬
