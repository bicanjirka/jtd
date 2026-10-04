# Tower Design Brainstorm

A big idea pool for towers, effects, debuffs and upgrades, grounded in what the engine can do today,
cut down to a design in review rounds.

## Where we are

- **Round 1** (done) reviewed sections 0 to 3.1. Its outcome is marked in place (✅ settled, ❌
  omitted, ❔ needs your input) and summed up in **Decisions so far**. The Sniper's raw round-1
  notes moved to Appendix A, so 3.1 reads as a spec.
- **Prepared for round 2** (2026-10-04): the Sniper is settled except a few gaps, each with two
  options (3.1). Splash and the six other towers are proposed on the Sniper template, each with a
  full setup (3.2 to 3.8; all eight side by side in 3.0). XP has its own chapter (10).
- **Next session, in this order:**
  1. The Sniper gaps (3.1), the only ❔ items left.
  2. Splash and the six other towers (3.2 to 3.8), starting from 3.0's overview.
  3. XP (10).
  4. Combos (4) and enemies (5), once the towers hold.
  5. Sections 6 to 9 last: they predate round 1 and will shrink once the towers are settled.

## How to use it

- **Status marks**, set by review rounds: ✅ settled · ❌ omitted (kept for the record) · ❔ needs
  your input. A struck-through part of a ✅ line was cut.
- **Still to review:** `[x]` = I like it, keep it. `[ ]` = no, or not yet. Write anything after a
  💬.
- ⭐ = my pick where options compete; "alt" marks the other option. ↳ = my reply to a 💬. Where a
  💬 rewrites a line, the 💬 wins.
- Cost tags, so you can see what an idea drags in:
  - 🟢 existing primitives only: a number, a `TowerBuff` axis, a hook or query that exists.
  - 🟡 one new small primitive: an effect kind, a gate, a selector, a projectile variant.
  - 🔴 a new shared system (ground zones, hexes, magazine). Expensive once, then cheap for every
    idea that reuses it.
- Numbers are placeholders that make an idea concrete, not balance proposals. Seconds assume 20
  ticks per second.
- Trees follow `FEATURE-tower-upgrades-iteration-2.md`: BASE (Range I-III, Fortify -> Awaken ->
  Transcendent), HEAD (two exclusive chains I -> II -> III -> IV-A | IV-B, plus one extra
  non-exclusive node I-IV), SPECIAL (slot 1, slot 2 after Transcendent). Aura caps at Awaken.
- Section 8 is my old shortlist and section 9 challenges what already exists (base stats with
  damage per credit, every shipped node, the names); both predate round 1.

Contents: Decisions so far · 0 What the code says · 1 The big picture · 2 Shared mechanics ·
3 Towers (3.0 the roster at a glance, then Sniper, Splash, Sonar, Pulse, Aura, Mortar, Seeker,
Cinder) · 4 Cross-tower combos · 5 Enemies that give towers a job · 6 Wild ideas · 7 Answers to
the iteration-2 open questions · 8 Shortlist · 9 Challenging what exists · 10 XP · Appendix A
Round-1 record

---

## Decisions so far

### Resolved after round 1 (no input needed)

- ✅ **No written special pairings.** 2.9 ticked them, 3.1 said "no custom pairings"; 3.1 is the
  later word.
- ✅ **Silence** stops everything cast (heal and shroud pulses, vanishing, summons, reshields); the
  Jammer's aura keeps running. That keeps both 2.2's tick and 2.7's "no".
- ✅ **Headhunter** loses its double bounty (no bounty modifiers).
- ✅ **Splash** keeps random aim at base; smart aim comes with Fortify (your own note).
- ✅ **Pulse's role** loses knockback (1.1 against 2.5).
- ✅ **Sunder Rounds** follows 2.2's numbers: -5 armor a stack, 10 stacks.
- ✅ **The extra head node may carry damage** when it is a mechanic (your template correction), so
  2.9's "never raw damage" now reads "never a flat bonus".
- ✅ **A ticked line rewritten by a 💬**: the tick keeps the slot, the 💬 is the content.

### Agreed after round 1 (your follow-up)

- ✅ **DoT crits:** a DoT pulse never crits; the hit that applies it can, and a crit application
  starts the pool at the crit multiplier.
- ✅ **Brittle only:** frozen enemies take extra physical damage only under Seeker's Brittle (+30%),
  not globally.
- ✅ **Click-to-focus** is a bought Aura special, Command (3.5).
- ✅ **XP** replaces the gate zoo; the details are chapter 10, to review.
- ✅ **Marksman's Eye II ignores 30 armor** (flat; the table is in Appendix A).
- ✅ **The hex framework** is decided: Splash's Hex chain stands on it.
- ✅ **Specials per chain** where one set can't serve both chains (your idea from the Splash rework):
  Splash uses it, the Sniper is proposed to (3.1), the other towers share one set.
- ✅ **Crit immunity wears down through resilience only.** A Mark, a guaranteed crit or penetration
  never gets through 100 resilience; Scorched and Resonant Crack lower it.
- ✅ **A guaranteed crit** ("crits 100%": a Mark, Fifth Shot, Seeker's Tracer) lands
  whenever the enemy's crit chance isn't 0, that is, whenever its resilience is below 100. Its
  size still shrinks with resilience, as every crit's does today. Code: today a forced crit
  still rolls against resilience (at 70 it lands 30% of the time), so `HitResolution` must let
  a guaranteed crit through below 100. 🟢
- ✅ **Weak Spot's non-crit clause stays on Focused Optics III**, as you wrote it (3.1).
- ✅ **Cinder owns regeneration and incoming heals**, through Fuel II Cauterize (3.8).

### Ground rules ✅

They cut ideas across every tower.

- **No bounty modifiers**, anywhere. Gone: Tithe, Harvester, Hex of Greed, Bounty Hunter,
  Headhunter's double bounty.
- **No knockback and no pull.** Freeze and Dazed are the hard crowd control. Gone: Shockwave,
  Sonic Boom, Gravity Shell, Implosion, Pull.
- **No player-chosen target priority.** Each tower aims by a built-in strategy that fits its job,
  and an upgrade may change it where that makes sense. New selectors join as towers need them
  (densest group, highest rank, fastest, most neighbours). Click-to-focus exists only as Aura's
  Command.
- **Armor and magic resist stop at 0.** Extra damage comes from stacking Sundered or Unraveled
  with Vulnerable, never from negative armor. No enemy has negative armor today, so the code
  change is only the stat's floor.
- **Crit immunity holds.** At 100 resilience nothing crits: not a Mark, not a guaranteed crit,
  not penetration. Lowering resilience (Scorched, Resonant Crack) is the one way in, and below
  100 a guaranteed crit always lands.
- **Hexes are rare**: at most two runes on an enemy, and only the Hex Splash casts them.
- **All specials cost the same**, the second one too; Transcendent (about 4x the tower's price)
  carries the weight. Final prices come from `td.BalanceHarness` simulation that counts effects
  and support as well as damage; the 2.9 formula is only the starting point.
- **Eight towers**, with the roles of 1.1.

### Damage and stats ✅

- Physical: Sniper, Splash, Sonar, Mortar. Magic: Pulse, Seeker, Cinder. Aura: none. Each physical
  tower gets one magic opt-in in its tree (Sniper's silver rounds, Splash's arcs and runes,
  Sonar's Frequency, Mortar's Napalm). No true damage.
- Stat owners as in 1.2 (and 3.0). Nobody owns effect resistances (Susceptible is cut).
  Regeneration and incoming heals: Cinder (Cauterize).
- Vulnerable stays on Withering Field, Marked Round and Hexflame (renamed Searing Flame). Cursed
  Shrapnel (Plate Cracker) applies Cracked, Homing Curse (Arcane Warhead) Unraveled, Warding Field
  Exposed or Sundered. The 3-stack cap stays.

### Crit ✅

- Crit multiplier per tower (Sniper x2.0, everyone else x1.5), plus a `critDamageBonus` axis that
  adds to it. Only Sniper is born with crit (5%); the others earn 5-15% from one head level.
  Aura's +50% crit damage comes from a head level (Keen Edge).
- A DoT pulse never crits; the hit that applies it can.

### Effects and interactions ✅ (numbers are yours)

- Sundered: -5 armor a stack, 10 stacks, one 5 s clock. Unraveled: -10 magic resist a stack, 5
  stacks, 5 s. Both floor at 0.
- Also in: Cracked, Exposed, Marked, Silenced, Dazed, Brittle, Anchored, Bleeding, Resonating,
  Soulfire, Haste, hexes, ground zones. Markers group by category, with a count.
- Interactions in: freezing a burning enemy bursts the burn (50%, Thermal Shock 150%); burning
  reveals, but only above a fuel level; tar doubles a burn's pool; Dazed doesn't block burn;
  revealed enemies are Exposed; frozen enemies take extra physical only under Brittle. Out:
  silence switching off auras, toxic smoke.
- Spirit paces every debuff; high-spirit enemies shake debuffs off; the inspector says it in
  words.

### Gates ✅

- Assists count (damage in the last 3 s, or a DoT on it). Purpose, neighbour-type and diversity
  gates are in. Out: buyout, waves served, rank gate, softening.
- XP is to replace kill, damage and purpose gates (chapter 10, to review); the layout gates stay.

### Upgrade structure ✅

- Range steps +15%, +10%, +10%; Range III carries a per-tower perk.
- Transcendent: a slow halo ring and a gold pip; about 4x the tower's price.
- Head power budget: I and II about +25% or one small verb; III the chain's defining verb. A
  chain's level I decides how the tower is used, and IV-A and IV-B are two ways to make that use
  overpowered. Specials are rule-breakers.

### The tower template ✅ (from your Sniper; rule 7 as you corrected it)

1. **A plain base with one token of identity.** One damage type and simple numbers. The Sniper is
   born with 5% crit and nothing else.
2. **Fortify grants the signature passive**: a conditional bonus that rewards using the tower for
   its job. Steady Aim pays the Sniper for staying on one target.
3. **Each chain's level I re-reads that passive in its own direction**, so the fork is felt at
   once. Focused Optics I: Steady Aim also gives fire rate. Marksman's Eye I: Steady Aim stacks
   to 3.
4. **Level II is a stat step plus one small trigger**, with a guard. Focused Optics II: +40%
   damage, and a crit starts Frenzy, which can't restart itself.
5. **Level III answers what counters the tower** (or the chain). Weak Spot lets non-crits through
   the plating and armor of crit-immune Armored enemies.
6. **IV-A and IV-B are two overpowered versions of the chain's strategy**, each changing where you
   build or what you aim at: Railgun (build along a straight), Executioner, Sunder Rounds (for the
   team).
7. **The extra head node is small mechanics that work with both chains and push the tower's
   general direction**, one per level. Magic is not part of the rule: the Sniper's opt-in sits
   there, other towers put theirs wherever it fits.
8. **Specials share the tower's core trigger** (the Sniper's are all crits), so any two combo
   without a written pairing.
9. **Readable numbers and guards**: flat values where players compare (armor penetration 30, not
   50%), and a cap or no-retrigger rule on every streak.
10. **One shared set of three specials** when they serve both chains; **a set of three per chain**
    when they can't (Splash, and the Sniper if you take 3.1's pick). Awaken then offers a chain's
    set once its level I is owned. Either way the panel shows three.

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

- ✅ ⭐ **Sniper - the Assassin.** Deletes the one enemy that matters: the Elite, the Mender, the
  boss. Physical, crits, armor answers, magic ammo.
  - 💬yes, but ammo should be physical, with magical opt-in as an upgrade
- ✅ ⭐ **Splash - the Spreader.** Cheap, instant, never misses: the delivery vehicle for anything
  that should land on a whole group (poison, hexes, arcs, debuffs). It weakens crowds for
  everyone rather than killing them itself.
  - 💬
- ✅ ⭐ **Sonar - the Spotter.** Sees and exposes: reveals the invisible, marks targets for
  guaranteed crits from any tower, cracks crit immunity, bridges to magic. Long-range support that
  also deals steady damage.
  - 💬
- ✅ ⭐ **Pulse - the Field.** A circle where the rules change: silence, slower recovery,
  corrosion, amplification, knockback. Its damage is incidental; its job is to be the place
  enemies must not linger. Built at loops and bends.
  - 💬
- ✅ ⭐ **Aura - the Mentor.** Buffs a cluster, shields it from jamming, and helps young towers
  reach their upgrade gates.
  - 💬
- ✅ ⭐ **Mortar - the Artillery.** The only tower that shapes the ground: nukes for bosses,
  carpets for swarms, fields of fire, tar and frost that keep working after the shell lands.
  Holds chokepoints where lanes meet.
  - 💬
- ✅ ⭐ **Seeker - the Hunter.** Never misses and never lets anything get away: magic single
  target that homes onto fast enemies, keeps its lock through invisibility, strips shields, freezes,
  and stores missiles for a salvo when the dangerous one arrives.
  - 💬
- ✅ ⭐ **Cinder - the Pyre.** Magic damage over time that ignores armor, scorches resilience so
  crits land harder for everyone, and burns what hides or heals.
  - 💬

### 1.2 Who owns which enemy stat

Give each tower "its" debuff so specials stop converging on Vulnerable. A player learns: *Sonar
means crits, Seeker means magic, Mortar cracks plates.*

- ✅ ⭐ **Armor** -> Sniper (Sundered, single-target stacking) and Pulse (Corrosion, while inside).
  - 💬
- ✅ ⭐ **Magic resist** -> Seeker (Unraveled).
  - 💬
- ✅ ⭐ **Plating** -> Mortar (Cracked: explosions crack plates).
  - 💬
- ✅ ⭐ **Crit chance taken** -> Sonar (Exposed); Cinder's Heat Haze as an alternative carrier.
  - 💬
- ✅ ⭐ **Resilience** -> Cinder (Scorched, exists); Sonar's Resonant Crack as the burst version.
  - 💬
- ✅ ⭐ **Spirit** -> Pulse (Soul Drain) and poison (Sickened, exists).
  - 💬
- ✅ **Regeneration and incoming heals** -> Cinder (Cauterize). (Agreed after round 1.)
  - 💬
- ❌ **Effect resistances** -> Splash (Susceptible: every effect lasts longer). The one stat no
  other tower touches.
  - 💬
- ✅ ⭐ **Damage taken (Vulnerable)** -> Aura (Withering Field) as the owner, plus Sniper's Marked
  Round and Cinder's Hexflame. The other three sources switch payload (see 1.4).
  - 💬
- ✅ **Speed** -> everyone chills; Seeker freezes; Mortar tars; Pulse anchors.
  - 💬
- ✅ **Stealth** -> Sonar (reveal), Pulse (hits and reveals the hidden), Cinder (burning reveals).
  - 💬
- ✅ **Abilities (silence)** -> Pulse (Null Field), with Mortar and Seeker as short-silence
  carriers.
  - 💬
- ❌ **Bounty** -> Aura (Tithe), Splash (Hex of Greed), Sniper (Headhunter).
  - 💬no bounty modifiers

### 1.3 Damage types

- ✅ ⭐ 🟢 **Pulse deals magic.** An energy field; magic plating barely exists, so its tiny ticks
  finally land on Armored Elites and the Warden.
  - 💬
- ❌ 🟢 **Splash deals magic** ("arcane bolt"). Cheap early magic AoE; adaptive armor then leans
  magic early, which rewards physical towers later.
  - 💬
- ✅ ⭐ 🟡 **Every physical tower gets one magic option** somewhere in its tree: Sniper's Silver
  Ammunition, Sonar's Frequency, Mortar's Napalm, Splash's arcs.
  - 💬
- ✅ ⭐ **Final split:** physical Sniper, Splash, Sonar, Mortar; magic Pulse, Seeker, Cinder; Aura
  none. Magic enablers: Seeker (-magic resist), Sonar (Resonating). Physical enablers: Sniper
  (-armor), Mortar (-plating), Seeker's Brittle (frozen take extra physical).
  - 💬
- ❌ 🔴 A third damage type (pure/true, ignores mitigation) only for executes and hex payloads.
  Risky: it makes armor matter less everywhere it appears.
  - 💬

### 1.4 Un-duplicate Vulnerable

- ✅ ⭐ Keep Vulnerable on Withering Field (Aura), Marked Round (Sniper), Hexflame (Cinder).
  - 💬
- ✅ ⭐ Cursed Shrapnel (Mortar) applies **Cracked** (plating -50%) instead.
  - 💬 
- ✅ ⭐ Homing Curse (Seeker) applies **Unraveled** (magic resist down) instead: 1 stack, 2 on a
  frozen or chilled target, as today.
  - 💬 
- ✅ ⭐ Warding Field (Pulse) rolls **Exposed** or **Sundered** instead.
  - 💬
- ❌ Or keep all six and raise the cap: 3 stacks from one source type, 5 from several.
  - 💬

### 1.5 The roster

- ✅ ⭐ **Keep eight towers.** Every one has a job once Splash, Pulse and Mortar are re-aimed.
  Adding a ninth adds balance load right when you want to wrap up.
  - 💬
- ❌ **Splash becomes Arc** (chain lightning): the beam jumps enemy to enemy instead of bursting.
  Clearly not Mortar (lines versus clumps), but the splash ideas go.
  - 💬 this only if splash become magic damage or electricity "arc" may be a tower head upgrade (the one that is not pathA or pathB as a first step)
  - ↳ Your 3.2 call superseded this: the arc became chain A rather than the extra node. Arcs deal
    magic while the blast stays physical.
- ❌ **Add Arc as a ninth tower** (`o` key) and keep Splash as the Spreader.
  - 💬
- ❌ **Merge Splash into Mortar** (Mortar's cheap early form) and add Arc in its slot.
  - 💬
- ❌ **Add an economy tower** (Bank / Harvester: interest on credits, bonus bounty in its
  radius). Classic TD decision layer; Aura's Tithe and Pulse's Harvester cover it without a tower.
  - 💬

---

## 2. Shared mechanics

Primitives several towers reuse. Building one of these usually unlocks five to ten ideas below.

### 2.1 Crit

- ✅ ⭐ 🟢 **Base crit multiplier per tower**: Sniper x2.0, everyone else x1.5.
  `TowerBaseStats` gains it; `AttackProfile.critMultiplier` already travels with every hit.
  - 💬
- ✅ ⭐ 🟡 **`critDamageBonus` axis on `TowerBuff`**, added to the multiplier (x2.0 + 0.5 =
  x2.5), so Aura, Weak Spot and Fifth Shot stack instead of overwriting each other.
  - 💬
- ✅ ⭐ **Only Sniper is born with crit.** Others earn it from one head level, 5-15%.
  - 💬
- ✅ ⭐ 🟢 **Aura's crit damage (+50%) comes from a head level** (Amplifying Core III), never the
  base aura: it multiplies with Sniper's big hits. Sniper 250%, others 200% under it.
  - 💬
- ✅ 🟢 **DoT crits** (agreed after round 1): a DoT pulse never crits; the hit that applies it
  can, and a crit application starts the pool at the crit multiplier.
  - 💬 no opinion od DoT crits. And I am interested on your opinion. DoT crit ticking sounds too strong to me.
  - ↳ Not stronger on average (a 15% crit at x1.5 is +7.5% either way), but worse in three ways:
    nobody sees it, it sparks four times a second, and Scorched feeds Cinder's own burn crits
    (less resilience, harder crits). My pick: **a pulse never crits; the hit that applies a DoT
    can, and a crit application starts the pool at the crit multiplier.** One crit moment per
    ignition, and Flashpoint reads "crit ignitions add 3 Scorched". 🟢
- ✅ 🟢 No crit spark for DoT crits: moot now, since a pulse never crits.
  - 💬
- ❌ 🟡 **Resilience penetration** on `AttackProfile` (Sniper's Deadeye): the only way through crit
  immunity besides a Mark.
  - 💬
- ❌ 🟡 **A Mark beats crit immunity**: a marked hit crits even at 100 resilience, for half the
  bonus. Makes Sonar the tower that cracks armored enemies open for crit teams.
  - 💬 no, crit immunity means crit immunity, even if marked

### 2.2 New effects and debuffs

Each is one `EffectKind` and a stat modifier, following the existing pattern. Who applies it is in
brackets.

- ✅ ⭐ 🟡 **Sundered**: armor -5 per stack, 10 stacks, one 5 s clock. ~~Can push armor below zero,
  where the enemy takes extra physical damage.~~ (Sniper Sunder Rounds, Pulse Corrosion, Mortar
  Bunker Buster)
  - 💬 armor below 0 is forbidden, armor 0 is minimum
- ✅ ⭐ 🟡 **Unraveled**: magic resist -10 per stack, 5 stacks, 5 s. (Seeker)
  - 💬 magic resist can not go below 0 as well, unravel and sunder stacks with vulnerable etc to deal extra damage
- ✅ ⭐ 🟡 **Cracked**: plating -50%, 5 s, refreshes. The Warden's and Armored Elite's plates
  become beatable. (Mortar, Sniper Heavy Caliber)
  - 💬
- ✅ ⭐ 🟡 **Exposed**: crit chance taken x2, 3 s. Revives the unused stat. (Sonar, Cinder Heat
  Haze)
  - 💬
- ✅ ⭐ 🟡 **Marked**: the next hit from any tower is a guaranteed crit; consumed by that hit; one
  per enemy. (Sonar Mark on Sweep, Seeker Tracer)
  - 💬
- ✅ ⭐ 🟡 **Silenced**: the enemy's abilities don't fire and its support auras (heal, shroud,
  jam) switch off. Death abilities still fire. (Pulse Null Field, Mortar Shell Shock, Seeker EMP)
  - 💬
  - ↳ This clashes with your "no" on 2.7's "silence switches off enemy auras". In the code the
    Mender's heal and the Ghost's shroud are cast abilities; only the Jammer's disruption is an
    aura. My pick: silence stops everything cast (heal pulses, shrouds, vanishing, summons,
    reshields) and leaves the Jammer's aura running. That keeps both of your marks.
- ✅ 🟡 **Dazed** (the stun the interactions doc left out): stops like freeze and silences, but
  doesn't put out burn or consume chill, and shares freeze's diminishing-returns ladder. (Mortar
  Heavy Shell, Splash Concussive Blast)
  - 💬
- ✅ 🟡 **Brittle**: a frozen enemy takes +30% physical damage. The freeze-then-smash combo.
  (Seeker Deep Freeze III)
  - 💬
- ✅ 🟡 **Anchored**: can't be sped up (hurt speed and haste ignored), speed capped at 75% of base.
  (Pulse Phase Lock)
  - 💬
- ✅ 🟡 **Bleeding**: physical damage per cell travelled; a stopped enemy doesn't bleed. The
  faster it runs, the more it bleeds: the anti-Frenzied DoT. (Mortar Shrapnel Storm)
  - 💬
- ✅ 🟡 **Resonating**: +8% magic damage taken per stack, 3 stacks, 4 s. (Sonar Harmonics)
  - 💬
- ❌ 🟡 **Susceptible**: every effect resistance -30%, down to -50%. Below zero, effects last
  longer than authored. (Splash)
  - 💬 no
- ✅ 🟡 **Soulfire**: a third fuel pool (blue flame) that earns Sickened stacks, so it stacks with
  burn and poison and drains spirit. (Cinder White Flame III)
  - 💬
- ✅ 🔴 **Hexes**: timed debuffs with a trigger. See 2.3.
  - 💬
- ✅ 🔴 **Ground effects** (burning ground, tar, frost, fallout). See 2.4.
  - 💬
- ✅ 🟡 **Haste** (enemy side, for future enemies). Anchored and Hex of Inversion counter it. The
  enemy-stats doc deferred agility until a haste exists.
  - 💬
- ✅ 🟡 **Markers grouped by category** once there are this many kinds: one marker per category
  with a count, the inspector lists them all.
  - 💬 yes I like this UI improvement

### 2.3 Hexes (curses)

A hex is a timed debuff that waits for something to happen to the enemy and then fires a payload,
credited to the tower that cast it (through its `DamageSink`, so it counts for gates too). The
triggers mirror the enemy ability triggers. Drawn as a small rune on the enemy and a flash when it
fires.

- ✅ 🔴 **The framework**: a hex = trigger + payload + duration.
  - 💬 I wonder why didnt you suggest this as a gold start emoji, I understand the framework as a way to implement hexes, right?
  - ↳ Yes: it is the plumbing every hex shares (trigger, payload, duration, the rune, the
    two-per-enemy cap, crediting the caster). ⭐ marked my pick where ideas competed for one
    slot; the framework had no rival, it is a prerequisite. Splash's Hex chain now stands on
    it, so it is effectively decided, and it is that chain's most expensive piece.
- ✅ ⭐ An enemy carries up to two different hexes; recasting one refreshes it.
  - 💬 Hexes should be quite rare in the game, 2 is enough
- ✅ **Hex of Echoes** (your idea): each critical hit the enemy takes deals +20 extra magic
  damage.
  - 💬
  - ↳ Its home: a Hex Splash special, offered against Reckoning (3.2).
- ✅ ⭐ **Hex of Contagion**: on death, its debuffs (Vulnerable, Sundered, poison, Scorched, other
  hexes) jump to up to 3 enemies within 1.5 cells with their remaining time.
  - 💬
- ✅ ⭐ **Hex of Reversal**: heals and shields it receives are dealt to it as damage instead.
  Menders become bombs; the Warden's reshield hurts it.
  - 💬
- ✅ ⭐ **Hex of Inversion**: anything that would speed it up slows it instead (Frenzied and
  Reaver hurt speed, a future haste).
  - 💬
- ❌ **Hex of Brittleness**: when frozen, it loses 10% of its current health.
  - 💬 do not this one, I have another proposal: how hard is it to make a hex that would do "when frozen, remaining chill prolongs effect of freeze and remaining burn is taken instantly"
  - ↳ Cheap once two things exist. Half of it already does: a freeze that consumes a chill lasts
    longer by the chill's level (up to +80%). The burn half is the 2.7 burst row you ticked
    (50%). The hex only raises both: twice the chill bonus, a 100% burst. That is two numbers
    the freeze path reads, a 🟡 on top of the framework. It is now **Rime Coven**, Splash
    Hex IV-A (3.2).
- ❌ **Hex of Greed**: dies hexed -> +50% bounty, whoever kills it.
  - 💬
- ✅ **Hex of Doom**: when the hex ends, it takes 30% of all damage it took while hexed, as one
  magic hit.
  - 💬
- ❌ **Hex of Binding** (Soul Link): enemies hexed together share 25% of the damage any of them
  takes.
  - 💬
- ❌ **Hex of Kindling**: the next burn it catches starts at double pool.
  - 💬 another idea: hexed enemy has DoT pools at double the capacity, which makes it super vulnerable, but it makes it immune to freeze and very resistant to chill (it is burn-supporting hex, conterpart to the "Hex of Brittleness" idea above)
  - ↳ A good counterpart. Freeze and chill immunity are existing resist stats; double capacity
    needs one new pool-capacity number. 🟡 It is now **Ash Coven**, Splash Hex IV-B, the
    exclusive twin of Rime (3.2).
- ❌ **Hex of Exposure**: if it would turn invisible, it is revealed instead and takes a hit.
  - 💬 other idea: when it turns invisible, it applies full stacks of sunder armor, unraveled, vulnerable or some other one/many buffs that makes sense. Maybe slowdown or freeze.
  - ↳ Folded into **Hex of Inversion** (3.2): what helps it hurts it, so a vanish reveals it and
    lands full Vulnerable. Full stacks of several debuffs at once looked like too much for one
    Ghost trick.
- ❌ **Hex of Grief**: takes 5% of its max health whenever an ally within 1.5 cells dies. Swarms
  unravel in cascades.
  - 💬 or vulnerable stacking - takes vulnerable whenever an ally within distance receives vulnerable. Or spreads vulnerable (or another debuff) when it receives one. (should contain prevention against one-vulnerable-curses-everyone kind of unvanted behavior)
  - ↳ Became **Hex of Sympathy** (3.2): a debuff landing on one hexed enemy is copied to the
    hexed enemies near it. Your guard is two rules: a copy never copies again, and one enemy
    shares each debuff kind at most once a second.

### 2.4 Ground effects

- ✅ 🔴 **A world list of zones**, like projectiles: a disc with a lifetime that applies an effect
  to every enemy inside on each pulse, crediting its tower. One new draw command, a translucent
  patch with a per-kind pattern. Zones hit invisible enemies, like all area damage.
  - 💬
- ✅ **Kinds**: burning ground, tar, frost ground, fallout, mines, embers, lingering flames.
  - 💬
- ✅ ⭐ **Zone interactions** in the same table as effect interactions: fire on tar ignites
  (inferno), frost on fire bursts it (thermal shock), frost on tar hardens it (1 s root).
  - 💬
- ❌ 🟢 A cap per tower (say 4 live zones) to stay inside the 1 ms tick budget.
  - 💬
  - ↳ Omitted as a design rule. If the 1 ms tick budget needs a cap, that is an implementation
    call.

### 2.5 Other primitives

- ❌ 🔴 **Knockback**: move an enemy back along its path. Shares freeze's diminishing returns;
  bosses take half. (Pulse Shockwave, Sonar Sonic Boom; a pull variant for Mortar's Gravity Shell)
  - 💬 not needed, freeze and stun is enough to CC
- ✅ 🟡 **Chain targeting**: the nearest enemy not yet hit, within a jump radius. (Splash Arc,
  Pulse Tesla Coil)
  - 💬
- ✅ 🟢 **Line pierce**: a narrow `InWedgeTargetQuery` from the tower through the target.
  (Sniper Railgun)
  - 💬
- ✅ 🟡 **Magazine**: the cooldown loads shots into a store instead of firing. (Seeker nest)
  - 💬
- ✅ 🟡 **Dispel**: remove a shield or a heal-over-time from an enemy. (Seeker Nullifier, Pulse
  Dead Zone)
  - 💬
- ✅ 🟡 **Damage type per shot**: every Nth shot magic. (Sniper Silver Ammunition)
  - 💬
- ✅ 🟡 **Execute**: kill outright below a health share. (Sniper Executioner)
  - 💬 
- ✅ 🟡 **Predictive aim**: an unguided shell aims where the enemy will be. (Mortar)
  - 💬
- ❌ ⭐ 🟡 **Target priority chosen by the player** per tower: first, last, strongest, weakest,
  nearest, fastest, highest rank. Four selectors exist already. Unlocked by Fortify, or on every
  tower from the start.
  - 💬 no, each tower has built-in strategy based on it's designed purpose and usage strategy, add strategies like most armored enemy, wakest enemy, fastest enemy, ... and use them in correct towers and their upgrades, I welcome if the tower changes targeting strategy based on a upgrade chosen sometimes, only where it makes sense. Possible addition, either to high-single-damage towers or aura tower that towers targets highlighted enemy, clicked by mouse, if in range. Consider it.
  - ↳ Click-to-focus: yes, as something you buy rather than a default. My pick is an Aura
    special, **Command** (3.5): towers this Aura buffs switch to the enemy you selected while
    it is in their range. One purchase steers a cluster, Aura gains an active role, and a
    player who never clicks loses nothing. The selection is already world state the game loop
    resolves. 🟡 Selectors the trees already ask for: densest group (Splash), highest rank
    (Headhunter), fastest (Seeker), most health (Sniper specials, exists).
- ✅ 🟡 **Projectile speed and size as tower stats**, shown in the info rows and drawn: a
  heavier shell looks heavier.
  - 💬

### 2.6 Spirit becomes tenacity

- ✅ ⭐ 🟡 **Spirit paces every debuff**, not only Scorched and Sickened: timers tick at
  `max(0.25, 1 + spirit / 100)` per tick. Freeze keeps its own diminishing returns. Poison and
  Pulse's Soul Drain then make *everything* last longer: your "slower recovery from any effect" as
  one stat rule.
  - 💬
- ✅ 🟡 **High-spirit enemies** (a Priest, a Zealot) shake debuffs off fast: the enemy that asks
  for poison or a Pulse.
  - 💬
- ✅ 🟢 The inspector says it in words: "debuffs wear off 1.5x slower".
  - 💬

### 2.7 New effect interactions (reward, not only punish)

The interactions doc worried that fire-versus-frost only punishes mixing towers. Rows that pay
off instead:

- ✅ ⭐ 🟡 **Freezing a burning enemy bursts the burn**: instead of just going out, the remaining
  pool lands at once at 50%. Cinder's Thermal Shock raises it to 150%.
  - 💬
- ✅ 🟡 **Frozen enemies take extra physical** only under Seeker's Brittle (+30%), ~~or +20%
  globally~~ (agreed after round 1).
  - 💬
  - ↳ Ticked without picking one. My pick: only with Brittle. A global +20% on every freeze moves
    balance everywhere and leaves Seeker's Deep Freeze III nothing to add.
- ✅ ⭐ 🟡 **Burning reveals**: stealth 0 while burning (your idea).
  - 💬 burning reveals but only when above ertain fuel level. Burning a little does not reveal anymore.
- ✅ 🟡 **Tar + burn**: a tarred enemy's burn starts at double pool.
  - 💬
- ✅ 🟡 **Dazed doesn't block burn**; Dazed and freeze share one diminishing-returns ladder.
  - 💬
- ✅ 🟡 **Revealed enemies are Exposed** while revealed: Sonar's two jobs feed each other.
  - 💬
- ❌ 🟡 **Silence switches off enemy auras** (Mender heal, Ghost shroud, Jammer disruption).
  - 💬 no
- ❌ 🟡 **Poison + burn = toxic smoke**: a burning poisoned enemy leaks a little poison to
  neighbours.
  - 💬 no

### 2.8 Kill gates and the farming problem

- ✅ ⭐ 🟡 **Assists count**: a kill counts for every tower that damaged the enemy in the last
  3 s, or has a DoT on it. Kill gates read "takedowns".
  - 💬
- ❌ ⭐ 🟢 **Gate buyout**: a gated node can be bought now at double price, or at +$N per missing
  kill.
  - 💬
- ❌ 🟡 **Waves-served gate**: "on the board for 4 waves". Seniority, not kills.
  - 💬 no, I like assists better, it is almost like introducing an XP count
- ✅ ⭐ 🟡 **Purpose gates**: count what the tower is for. "Applied 60 freezes", "revealed 10
  hidden enemies", "landed 40 crits", "marked 30 targets", "burned for 5000". Support towers stop
  starving.
  - 💬 and what about previously mentioned XP count - low XP for fired shot, more XP for applied whatever the target is for (slow, freeze, crit...), lot of XP for a kill. XP is nevel spent, it climbs only up. 
  - ↳ I like it, and I'd let it **replace** kill, damage and purpose gates rather than sit beside
    them: one number per tower, a bar on the panel, gates that read "XP 300". Points per landed
    hit (not per shot, so misses earn nothing), more per purpose event (a freeze, a crit, a
    reveal, a hex), more per assist, most per kill. The purpose list is per tower, so support
    towers climb by doing their job, and the per-hit value is per tower too (Pulse lands 20
    hits a second). Layout gates (neighbour, diversity) stay separate: they are about the board,
    not progress. Weights come from the harness: a tower doing its job should reach head III
    around the same wave, whatever the job. Veterancy stars (6) come free. 🟡
- ❌ 🟡 **Rank gate**: "killed an Elite or Boss", or "dealt 2000 to Boss-rank enemies".
  - 💬
- ✅ ⭐ 🟡 **Neighbour-type gate**: "next to an Aura" / "next to 2 Auras". Your Aura-beside-Aura
  condition, reusable elsewhere ("next to a Sonar" for a Sniper node).
  - 💬
- ✅ 🟡 **Diversity gate**: "3 different tower types adjacent". Rewards mixed clusters.
  - 💬
- ❌ 🟡 **Softening gate**: a kill requirement drops 10% for every wave the node has been on offer.
  - 💬
- ❌ 🟢 **Prefer damage gates for late nodes**: damage is shared fairly; kills are not.
  - 💬
- ❌ Aura's Tutelage line (section 3.5) as the in-world fix.
  - 💬

### 2.9 Upgrade structure

**Fortify** (the new base root; working name)

- ❌ ⭐ 🟢 **Jam-proof**: halves disruption on this tower. "A reinforced base" in plain terms,
  and a reason to put the Jammer into waves.
  - 💬
- ✅ ⭐ **Plus a small per-tower perk**, listed with each tower.
  - 💬
- ❌ 🟡 Unlocks the player-chosen target priority (2.5).
  - 💬
- ❌ 🟢 Only a price gate; the perk is a sweetener, not a power spike.
  - 💬
- ❌ Look: a thicker base ring with rivets.
  - 💬

**Range II and III**

- ✅ ⭐ 🟢 Shrinking steps: +15%, +10%, +10%.
  - 💬 
- ✅ 🟡 Range III carries a per-tower perk (listed per tower), so it is a small event, not +10%.
  - 💬

**Transcendent**

- ✅ ⭐ Look: a slow halo ring on the base and a gold pip; the turret's shot takes a lighter,
  whiter colour.
  - 💬
- ✅ 🟢 Price about 4x the tower's price.
  - 💬

**A power budget, so levels read the same on every tower**

- ✅ ⭐ Head I and II: about +25% effective output each, or one small verb.
  - 💬
- ✅ ⭐ Head III: the chain's defining verb (it is what Transcendent needs, so it must feel like
  an arrival).
  - 💬
- ✅ ⭐ Head IV-A / IV-B: a new mode that changes *where and how* you use the tower, never "+50%".
  - 💬 the first nodeA or nodeB of the head should fork the usage strategy, the final iv-a or iv-b for each nodeA or nodeB should correspond to the branch strategy and make it OP
- ✅ ⭐ Extra head node: utility or a second axis (damage type, targeting, projectile, economy),
  never raw damage, so owning it doesn't dwarf a chain.
  - 💬
- ✅ ⭐ Specials: rule-breakers. Pairings: combos you'd plan a level around.
  - 💬

**Prices** (P = the tower's own price)

- ✅ 🟢 A formula, as the starting point (your 💬: the harness sets the prices): Range I 0.6P, Fortify
  0.8P, Awaken 1P, Range II 1.2P, Range III 2P, Transcendent 4P; head I 1P, II 1.5P, III 2.5P, IV
  4P; extra node 0.8P / 1.2P / 2P / 3P; special 2P~~, second special 3P~~.
  - 💬 all specials costs the same, even the second one, but transcendent is expensive to even that out. I like the formula idea but towers will not be balanced, different prices should balance them. I would like to see using headless testing, harness and many simulations to asses the price. It is not only damage, value of some towers is in the effects it has, and in support it gives to the others
  - ↳ Agreed. A concrete harness method: a fixed budget per level, then for each node, how much it
    cuts leaks (or raises health left) per credit against the best alternative buy; reprice
    until those are close. Support and effects are then measured the same way as damage.
- ❌ Or absolute tiers per level whatever the tower costs, so a $10 Sniper isn't the cheapest to
  max out.
  - 💬

**Specials**

- ❌ 🟢 Tag each special Offense, Control or Support; the second special must have a different
  tag. Forces a pairing to be a combo, not two damage amplifiers (the balance risk the doc names).
  - 💬
- ✅ ⭐ Free choice~~, with hand-written pairings for the good pairs and a fallback of "+20%
  strength to both" for the rest~~ (no written pairings, 3.1).
  - 💬
  - ↳ Clashes with 3.1's "no custom pairings". The doc follows 3.1, the later word: free choice,
    no written pairings, no fallback; specials combo through shared triggers.

---

## 3. Towers

Each tower: its status, why build it, a proposed setup (the whole tree at a glance, my ⭐ picks),
then the base line, BASE, both head chains, the extra head node, the specials, what it plays well
with, and a pool of loose ideas. 3.0 puts all eight side by side; 3.1 is the settled Sniper the
others follow.

### 3.0 The roster at a glance

Every tower on the Sniper template (Decisions so far). The Sniper row is settled except its gaps;
the others are my proposals for round 2, each detailed in its own section.

| Tower | Job | Damage | Fortify passive | Chain A | Chain B | Extra node | Specials |
|---|---|---|---|---|---|---|---|
| Sniper | the Assassin: the one enemy that matters | physical, crit x2.0 | Steady Aim | Focused Optics (tempo) | Marksman's Eye (certainty) | Tradecraft ❔ (magic at III) | per chain ❔ |
| Splash | the Spreader: whatever lands on a group | physical blast; magic arcs and runes | Saturation + Fire Control | Arc (the storm) | Hex (the witch) | Blast Engineering | per chain |
| Sonar | the Spotter: crits for everyone, sight | physical; magic through Frequency | Ping | Twin Array (more passes) | Long Reach (far, cracks immunity) | Frequency | shared |
| Pulse | the Field: the place not to linger | magic | Toll | Overcharged Coils (the bite) | Phase Field (the rules) | Field Shaping | shared |
| Aura | the Mentor: a cluster, and its XP | none | Kinship | Amplifying Core (strength) | Broadcast (reach) | Tutelage (XP) | one slot |
| Mortar | the Artillery: the ground at a chokepoint | physical; magic through Napalm | Bracketing | Siege Rounds (bosses) | Fragmentation (swarms) | Ballistics | shared |
| Seeker | the Hunter: nothing gets away | magic | Nest | Twin Warhead (salvos) | Deep Freeze (control) | Mixed Payloads | shared |
| Cinder | the Pyre: burns, Scorched, no hiding | magic, over time | Stoke | White Flame (hot, short) | Wide Nozzle (wide, long) | Fuel | shared |

**What each tower owns** (1.2, decided), so a player learns "this tower means that debuff":
Sniper armor (Sundered) · Splash runes (hexes) · Sonar crit chance taken (Exposed), resilience
(Resonant Crack), sight · Pulse silence, spirit, speed (Anchored), armor inside (Corrosion) · Aura
damage taken (Vulnerable) · Mortar plating (Cracked), the ground, speed (tar) · Seeker magic resist
(Unraveled), freeze · Cinder resilience (Scorched), regeneration and heals (Cauterize), burning
reveals.

**How the passives meet.** Each passive pays for using its tower where it belongs, and several
feed each other: Ping's Exposed doubles Steady Aim's crit chance on the boss; Toll and Undertow keep
enemies in Bracketing's spot and in Stoke's cone; Saturation and Toll make runes last; Kinship pays
for putting four of them next to each other.

### 3.1 Sniper - the Assassin

**Status:** ✅ settled in round 1, except the ❔ gaps below. The round-1 items, your 💬 notes and my
↳ replies are kept in Appendix A.

**Why build it.** The cheapest tower and the only one born with crit. A duellist that gets better
the longer it stays on one target: it deletes the enemy that matters (the Elite inside a Soldier
wave, the Mender behind the wall, the Warden). Chain A turns patience into tempo, chain B into
certainty. **Buy it when** waves carry Elite or Boss ranks, or support enemies must die first.
**Weak against** swarms (Ricochet is its only answer), crit-immune armor (Weak Spot and Find the
Seam), the invisible (Sonar's job). **Look by tier:** a thin barrel; a faint laser line to its
target that brightens with each Steady Aim stack (Fortify); a gold barrel and a white tracer
(Transcendent); silver shots as a pale blue beam.

**The settled tree** (❔ = a gap below)

```
SNIPER   $10 | dmg 30 | crit 5%, x2.0 | physical
BASE     Fortify   Steady Aim: +10% crit on every shot after the first at one target (15%)
         Range     +15%  ->  II +10%  ->  III Overwatch: +35% range, can't shoot within 2 cells
HEAD A   Focused Optics (tempo: patience becomes speed)
         I    Steady Aim also gives +25% fire rate                       ❔ + Quick Scope?
         II   +40% damage; a crit starts Frenzy: the next 3 shots twice as fast, no restart
         III  Weak Spot: +30% damage, non-crit shots ignore plating and 50 armor
         IV-A Railgun: pierces every enemy on the line, reaching 2 cells past max range
         IV-B Executioner: a non-boss left under 15% dies; bosses take +50% under 25%
HEAD B   Marksman's Eye (certainty: patience becomes crits)
         I    Steady Aim stacks to 3 (+30%, so 35% crit from the 4th shot)
         II   ignores 30 armor; +25% fire rate
         III  ❔
         IV-A ❔
         IV-B Sunder Rounds: every crit adds 1 Sundered (-5 armor, 10 stacks)
EXTRA    ❔ name (the assassin's kit: small mechanics for both chains)
         I    a crit makes the next crit +25% crit damage, two in a row +50%, no further
         II   +25% damage against targets past two thirds of its range
         III  every 3rd shot is magic, +25% magic penetration; it keeps the crit streak and
              profits from it
         IV   Spotter Uplink: may shoot Marked or Revealed enemies within twice its range
SPECIAL  ❔ per chain (my pick) or one shared set of three, see the gap
```

**Gaps to fill** (two options each, ⭐ = my pick)

- ❔ **The specials.** All five of today's feed on crits, but the panel shows three per slot. Your
  idea from the Splash rework fits here: let the chain decide.
  - [ ] ⭐ **A set per chain** (Awaken offers the chain's three once its level I is owned; the
    second special after Transcendent comes from the same three). Every special you ticked keeps a
    home, and each set sharpens its chain's strategy:
    - **Focused Optics** (tempo): **Momentum** (a crit charges a x5 shot that ignores armor and
      plating; a kill by it gives +100% fire rate for 5 s), **Ricochet** (a crit bounces to the
      nearest enemy within 1.5 cells for 60%, up to 3 bounces, each can crit), **Headhunter**
      (highest rank first, +40% damage against Elite and Boss). Speed, crowds, bosses.
    - **Marksman's Eye** (certainty): **Marked Round** (crits apply Vulnerable; rename it Hollow
      Point, 9.5), **Fifth Shot** (every 5th shot is a guaranteed crit, +50% crit damage; it still
      can't crit an immune enemy), **Shatter Shot** (crits on a frozen or Dazed enemy deal +50%
      crit damage). The team, the rhythm, the combo with Seeker and Splash.
    - 💬
  - [ ] alt **One shared set of three: Momentum, Ricochet, Marked Round**: the boss killer, the
    crowd answer, the team player, on either chain. Simpler to build (all three exist but
    Ricochet), but Headhunter and Fifth Shot are cut, and Shatter Shot needs B III as its home.
    - 💬
- ❔ **How each special aims** (today any special switches to "most health").
  - [ ] ⭐ Each aims for its own job: Momentum and Headhunter at the highest rank (the burst wants
    the elite), Ricochet at the enemy with most neighbours within 1.5 cells (so the bounces find
    targets), Marked Round and Fifth Shot at most health (the enemy the team should focus),
    Shatter Shot at a frozen or Dazed enemy first.
    - 💬
  - [ ] alt All keep "most health", as today: one rule to learn.
    - 💬
- ✅ **Weak Spot's non-crit clause lives on A III**, as you wrote it (agreed after round 1).
  B III takes its own verb (next gap).
- ❔ **B III** (level III answers what counters the chain: here, crit immunity).
  - [ ] ⭐ **Find the Seam**: against an enemy that can't be crit, each Steady Aim stack gives +15%
    damage and ignores 10 armor instead. Crit immunity still holds, but patience pays anyway.
    - 💬
  - [ ] alt **Armor Piercer**: every 3rd shot at one target ignores all armor and plating, crit or
    not. A rhythm instead of a conversion; it also helps against plated enemies that can be crit.
    - 💬
  - (Shatter Shot, your other "add it where it fits", is a chain-B special in my pick above; with
    the shared set it moves here instead.)
- ❔ **B IV-A** (Deadeye is out; IV-B Sunder Rounds is the team version, so IV-A is the selfish one).
  - [ ] ⭐ **Unbroken Aim**: Steady Aim stacks to 5 (55% crit) and survives a kill; only retargeting
    a living enemy resets it. The crit engine that walks through a line of elites.
    - 💬
  - [ ] alt **Follow-Through**: every crit is followed at once by a free shot at the same target for
    50%; that shot can crit, but its crit starts no further follow-up.
    - 💬
- ❔ **Quick Scope's home** (you liked it: the first shot at a new target has +50% crit chance).
  - [ ] ⭐ On A I, beside Steady Aim: Steady Aim covers every shot but the first, Quick Scope covers
    the first. In the tempo chain it feeds Frenzy: a kill, a new target, a likely crit, Frenzy
    again.
    - 💬
  - [ ] alt On Ricochet: every bounce is a "first shot", so bounces crit far more often and the
    crowd answer gets stronger.
    - 💬
- ❔ **The extra node's name.**
  - [ ] ⭐ **Tradecraft**: the assassin's technique (streak), patience at range, special rounds and
    intel.
    - 💬
  - [ ] alt **Long Watch**.
    - 💬
- ❔ **Frenzy's length.**
  - [ ] ⭐ Counted: "the next 3 shots come twice as fast". It reads exactly, at any fire rate.
    - 💬
  - [ ] alt Timed: +100% fire rate for 3 s (about 4 shots at A II's rate).
    - 💬

**Second review** (things the harness should watch, no decision needed yet)

- Crit damage tops out around x3.0 (x2.0, the extra node's streak +0.5, Aura's Keen Edge +0.5),
  and a Momentum shot multiplies that by 5: about x15 in one hit. If Keen Edge ships, Momentum's
  x5 should become x3 (9.4 already suspects it).
- Fire rate on chain A: +25% under Steady Aim, doubled in Frenzy (fire-rate bonuses multiply), so
  x2.5 for three shots. Momentum's kill burst replaces a running Frenzy instead of stacking.
- Executions count as crits for triggers. They charge Momentum but start no Ricochet bounce
  (the target is gone). Executioner and the Marksman's Eye set never meet, since they sit on
  different chains.

**Plays well with:** Sonar (Ping doubles its crit chance on the boss; Spotter Uplink reaches what
Sonar marks or reveals), Aura (Keen Edge, Chosen, Command), Seeker (Brittle, and Shatter Shot if
chosen), Splash (Sympathy spreads its Sunder to the hexed group; Static Charge turns its hits into
lightning), Cinder (Scorched opens crit immunity for it).

### 3.2 Splash - the Spreader

**Status:** to review (round 2). Rebuilt from your call below, on the Sniper template, then
reviewed once more.

💬 I call for complete rework of this tower. Base function is good, it splashes. Give it cool passive like the sniper tower has "steady aim". Extra head slot should have good synergy with the rest. And the main head fork, nodeA and nodeB, shopuld desice the purpose of this tower. One, nodeA, is the electric arc, blasting with electricity, blue color, chains, stuns, crit synergy etc. Second buff path nodeB is the witch tower with hexes, spreading curses, controlling the enemy behavior etc. When specials are unlocked, they are based on this head path taken, either hex-based or arc-based, 3 each. Good synergies, strong. If the nodeA or nodeB was not decided, special slot is not unlocked by Awaken.

**The shape in one line: the blast is the delivery, the chain is the payload.** Fortify and the
extra head node improve the blast: where it lands, how wide, how often. The fork decides what the
blast carries: lightning (chain A, Arc) or curses (chain B, Hex). The specials follow the fork.
Splash stays physical, cheap and instant; arcs and hexes are its magic opt-in (1.3).

**Why build it.** Cheap, instant, fires every second, never misses: the delivery vehicle for
whatever should land on a group. Unforked, it is early area damage. **Arc** makes it a storm that
runs along columns and lines, stuns with crits, and turns other towers' hits into lightning.
**Hex** makes it a witch: curses that store damage and poison, jump from the dead to the living,
and turn enemies' own abilities against them. **Buy it when** early (cheap area); Arc for
strung-out lanes, columns and crit teams; Hex for elites in escorts, healers, rushers, and mixed
defences whose debuffs it multiplies. **Weak against** lone bosses and heavy plating on its
physical blast; both chains answer with magic (arcs, Doom's single big hit), and each IV has a boss
mode. **Look:** a white burst at base. Arc: a blue-white burst, jagged arcs, a crackle ring on
Dazed enemies. Hex: a violet burst and a rune over each hexed enemy that fills as Doom stores
damage. The tower's name can follow the fork (Stormcaller, Hexer; pool below).

**Proposed setup**

```
SPLASH   $15 | dmg 16 | 1 shot/s | blast 1.75 | random target | physical
BASE     Fortify   Fire Control: aims for its purpose; Saturation (+5% blast damage a stack, 3)
         Range     +15%  ->  II +10%  ->  III +10% and blast radius +10%
HEAD A   Arc (the storm: lines, stuns, crits; magic)
         I    arcs carry the blast past its edge: 2 jumps at 50%; Saturation adds reach
         II   Conductor: +10% crit, arcs can crit, +1 jump
         III  Overload: an arc crit Dazes 0.5 s                              (alt Ground Strike)
         IV-A Chain Lightning: 6 jumps, no loss, the 3rd forks
         IV-B Lightning Rod: arcs with nowhere to go return to the primary, up to 3 times
HEAD B   Hex (the witch: curses, spread, control; magic)
         I    Hex of Doom on the primary: 30% of damage taken while hexed, released at the end
         II   Witch's Brew: the rune poisons; hexes the primary and the two most Saturated
         III  Spreading Curse: a hexed death passes rune and debuffs on, two generations at most
         IV-A Rime Coven (your freeze hex)       IV-B Ash Coven (your fire hex)
EXTRA    Blast Engineering: Wide Charge, Shaped Charge, Aftershock, Carpet
SPECIAL  Arc: Thunderclap, Static Charge, Thunderstrike
         Hex: Inversion, Sympathy, Reckoning                                  (alt Echoes)
```

**Base line**

- [ ] ⭐ 🟢 Keep it as it is: physical blast, random target in range, instant, one shot a second,
  falloff from the centre ("base function is good").
  - 💬
- [ ] ⭐ 🟢 Drop the chill from the tower line: the code has none at base, and every slow now
  belongs to a chain (Arc's Dazed, Rime Coven's chill).
  - 💬

**BASE**

- [ ] ⭐ 🟡 Fortify **Fire Control**: your smart aim, plus the passive you asked for.
  - **Aims for its purpose.** Unforked: the enemy with most neighbours inside the blast. Arc: where
    blast and arcs together reach most enemies (the head of a column). Hex: the highest-rank enemy
    in the densest group that doesn't carry this tower's rune yet.
  - **Saturation**, Splash's Steady Aim: an enemy caught by blasts on consecutive shots gains a
    stack, up to 3; a shot that misses it clears them. +5% blast damage a stack. It is an effect on
    the enemy, so two Splashes build it together. Each chain's level I reads it, and the extra node
    grows it. Smart aim keeps pounding one crowd, so stacks build; random aim rarely builds them.
  - 💬
- [ ] alt passive **Crowd Tempo**: every enemy beyond the 3rd caught in one blast shortens the next
  cooldown by 10%, up to 40%. It pays for hitting crowds with fire rate, which both payloads
  multiply; but the chains then have nothing per-enemy to read.
  - 💬
- [ ] 🟢 Range II +15%; Range III +10% and blast radius +10%.
  - 💬

**The fork.** The two chain roots exclude each other, as today. Level I decides the tower's purpose
and how Fire Control aims; IV-A and IV-B are two ways to make that purpose overpowered. **Awaken
opens the special slot only once a chain root is owned**, and offers that chain's three specials:
exactly the panel's three-per-slot limit. Each special simply `requires` Awaken and its chain's
level I, so no new rule is needed. 🟢 (The same rule now serves the Sniper, 3.1.)

**HEAD chain A - Arc** (the storm: lines, stuns, crits)

- [ ] ⭐ 🟡 I **Arc**: the blast carries past its edge. From the outermost enemy it caught, an arc
  jumps to the nearest enemy the shot hasn't hit, within 1.5 cells, then on from there: 2 jumps,
  50% of the blast each, as magic. Arcs jump 0.5 cells further from a Saturated enemy per stack.
  The blast owns clumps, the arcs own lines.
  - 💬
- [ ] ⭐ 🟢 II **Conductor**: +10% crit chance (Splash's one crit-granting level), arcs can crit,
  +1 jump.
  - 💬
- [ ] ⭐ 🟡 III **Overload** (the stun): an arc crit Dazes its target for 0.5 s, on freeze's
  diminishing-returns ladder. Arcs into a fully Saturated enemy get +10% crit chance.
  - 💬
- [ ] alt III **Ground Strike** (the answer to a tight clump, where arcs find nobody outside the
  blast): an arc with nowhere to jump strikes the primary again at 50%, once per shot.
  - 💬
- [ ] ⭐ 🟡 IV-A **Chain Lightning** (the swarm storm): up to 6 jumps with no loss per jump, and the
  3rd jump forks into two arcs. A column dies in one shot.
  - 💬
- [ ] ⭐ 🟡 IV-B **Lightning Rod** (the focused storm): an arc that finds no new enemy returns to the
  primary at full strength, up to 3 times; arcs on a Dazed enemy deal +50% crit damage. A boss
  with two escorts takes four strikes a shot.
  - 💬

**HEAD chain B - Hex** (the witch: curses, spread, control)

One rule keeps hexes rare and readable: **a tower casts one rune.** Its hex is a single rune built
of clauses: Doom from level I, poison from II, a coven from IV, plus its specials. An enemy carries
at most two runes (from two hex towers), and recasting refreshes. Only a Hex Splash casts runes,
and the rune's glyphs say what it does.

- [ ] ⭐ 🔴 I **Hex of Doom**: the blast hexes its primary target for 4 s, +1 s per Saturation
  stack. When the hex ends, the enemy takes 30% of all the damage it took while hexed, as one magic
  hit credited to this tower. One big hit is also the witch's answer to plating. The first use of
  the hex framework (2.3).
  - 💬
- [ ] ⭐ 🟡 II **Witch's Brew**: the rune also poisons (Toxic Bloom's poison, tamed to 4% of weapon
  damage a tick), and the blast hexes up to 3 enemies: the primary and the two most Saturated.
  Poison's Sickened lowers spirit, and spirit paces every debuff (2.6), so the hex and everything
  else on that enemy lasts longer.
  - 💬
- [ ] ⭐ 🟡 III **Spreading Curse** (the spread): when a hexed enemy dies, its rune and its debuffs
  (Vulnerable, Sundered, Unraveled, Cracked, Exposed, poison, Scorched) jump to the 2 nearest
  unhexed enemies within 1.5 cells, with their remaining time. A jumped rune can jump once more,
  never a third time: your guard against one curse taking the whole wave. (Contagion, folded into
  the chain.)
  - 💬
- [ ] ⭐ 🟡 IV-A **Rime Coven** (your Brittleness replacement): the rune chills 30% when cast, and
  when the enemy freezes, its remaining chill buys twice today's extra freeze time and its burn
  lands at once at 100% (the global row lands 50%). Build it where Seekers freeze.
  - 💬
- [ ] ⭐ 🟡 IV-B **Ash Coven** (your Kindling replacement): burn and poison pools on the enemy hold
  twice as much and earn Scorched and Sickened twice as fast, but it can't be frozen and shrugs off
  75% of chill, so no teammate's freeze puts the fire out. Build it where Cinders burn. A Rime rune
  and an Ash rune can't share an enemy: the newer replaces the other.
  - 💬
- [ ] IV alternatives, if the covens lean too hard on teammates: **Plague** (every cast also hexes
  2 neighbours: the crowd witch) / **Grand Hex** (Doom stores 60%, and the rune lasts 8 s and can't
  be cleansed: the boss witch).
  - 💬

**Extra head node - Blast Engineering** (the delivery line: today's chain A, moved here so its
shipped code stays)

Small mechanics that serve both chains and push the tower's direction (template rule 7): every
level makes the blast reach more enemies, or reach them more often, which is exactly what both
payloads multiply: more arc starts, more rune hosts, more Saturation.

- [ ] ⭐ 🟢 I **Wide Charge**: +25% blast radius, and the blast's edge deals 25% instead of nothing.
  - 💬
- [ ] ⭐ 🟡 II **Shaped Charge**: enemies in the inner half of the blast gain 2 Saturation stacks
  instead of 1, and Saturation caps at 4.
  - 💬
- [ ] ⭐ 🟡 III **Aftershock** (your tick): each blast goes off again 1 s later on the same spot at
  50%, catching what walks in. On Arc it arcs; on Hex it refreshes the runes it catches but never
  casts a new one.
  - 💬
- [ ] ⭐ 🟢 IV **Carpet** (your tick): 3 blasts a shot at 60% each, on the three densest groups,
  each carrying the payload. Today's Blast Engineering III, now behind Transcendent.
  - 💬

**Specials** (all the same price; the second, after Transcendent, comes from the same three)

Arc (shared trigger: the arc crit):

- [ ] ⭐ 🟢 **Thunderclap** (Overpressure, reborn and tamed): after a crit, the next shot discharges
  into every enemy in range as arcs at 50%, and each one it crits is Dazed. Its own crits don't
  re-arm it. Today's Overpressure code, re-aimed.
  - 💬
- [ ] ⭐ 🟡 **Static Charge** (the team's conductor): arcs leave enemies Charged for 3 s. The next
  hit from another tower discharges it for +30% of that hit as magic, credited to this Splash; a
  crit discharges at double. Field ticks and DoT pulses don't discharge it, so a Pulse can't eat
  the charge. Every Sniper, Mortar and Sonar hit on a Charged crowd becomes a lightning strike.
  - 💬
- [ ] ⭐ 🟡 **Thunderstrike** (the boss answer): every 6th shot calls lightning onto the enemy with
  most health in range: 4x the blast as magic, Dazed 0.5 s, and that shot's arcs start there at
  full damage. Counts as a crit for triggers.
  - 💬
- [ ] alt 🟡 **Ball Lightning**: every 4 s a ball rolls back up the path from the target, zapping
  each enemy it touches once. Swap it in for Thunderstrike if lines matter more than bosses.
  - 💬

How they combine by themselves: a Thunderstrike arms Thunderclap; Thunderclap's discharge Charges
everything in range for Static Charge; Chain Lightning and Lightning Rod multiply all three.

Hex (shared trigger: the rune):

- [ ] ⭐ 🟡 **Hex of Inversion** (control: what helps it hurts it): heals and shields it receives
  are dealt to it as damage; anything that would speed it up slows it instead; if it would turn
  invisible, it is revealed and takes full Vulnerable (your Exposure idea). Menders become bombs,
  the Warden's reshield hurts it, Frenzied and Reaver crawl, a Ghost's vanish backfires. (2.3's
  Reversal and Inversion, merged into one rule.)
  - 💬
- [ ] ⭐ 🟡 **Hex of Sympathy** (support: what one suffers, all suffer; your Grief idea): a debuff
  that lands on a hexed enemy (Vulnerable, Sundered, Unraveled, Cracked, Exposed, chill) is copied
  to every other enemy carrying this tower's rune within 2 cells. A copy never copies again, and
  one enemy shares each debuff kind at most once a second, so one Sniper's Sunder becomes the
  group's without a loop.
  - 💬
- [ ] ⭐ 🟡 **Hex of Reckoning** (offense): a hexed enemy's death releases the Doom of every enemy
  carrying this tower's rune within 2 cells at once, and their runes restart. Kills chain into
  detonations. (Concussive Blast's "kills explode", rebuilt on hexes.)
  - 💬
- [ ] alt 🟡 **Hex of Echoes** (you ticked it in 2.3): each crit the hexed enemy takes deals +20
  magic, credited to this Splash. The witch beside a crit team (Sniper, Sonar's Ping). Swap it in
  for Reckoning if the witch should back crits rather than kill on its own.
  - 💬

How they combine by themselves: Sympathy's copied debuffs make every hit bigger, so each Doom
stores more for Reckoning; Inversion's reversed heals count as damage taken, so they fill Doom; an
inverted Ghost's Vulnerable is shared by Sympathy; Spreading Curse carries all of it on to the
next enemies.

**What happens to today's Splash** (node ids stay where a node survives in a new role, to spare
tests)

| Today | Becomes |
|---|---|
| Blast Engineering I-III (radius; damage and flatter falloff; 3 blasts) | the extra head node: Wide Charge, then Carpet at IV |
| Rapid Battery I-III (fire rate; damage and crit; bigger crit splash) | retired; its crit moves to Arc II |
| Toxic Bloom (the blast poisons; the game's biggest outlier, 9.3) | Hex II Witch's Brew, at 4% a tick |
| Concussive Blast (-50% fire rate, chill, kills explode) | retired: its stun to Arc III, its chill to Rime Coven, its explosions to Reckoning |
| Overpressure (after a crit, the next shot hits everything in range) | Thunderclap, at 50% |
| Random target | stays at base; Fire Control aims |

**What it costs.** Arc: chain targeting and Dazed (🟡 each), magic arcs (🟢). Hex: the hex
framework (🔴, once), then a 🟡 per clause. Most clauses are stat modifiers, the way traits and
effects already change a mob (Ash is freeze and chill resist plus a pool-capacity stat; Rime is two
numbers the freeze path reads). Doom, Spreading Curse, Sympathy and Reckoning are triggers. Arc is
the cheaper chain, so it ships first.

**Second review** (my own pass, round 2)

- Saturation at +5% a stack is tame on its own; its weight is in what the chains read from it
  (arc reach, rune length, Brew's targets). If it feels flat in play, raise it to +8%, not the
  chains.
- Arc's weak spot is a tight clump: nothing sits outside the blast for an arc to reach until
  IV-B. Ground Strike (the III alt) fixes it earlier; Overload is more fun. Watch it in the
  harness on swarm waves.
- Hex I lands on one enemy a shot by design (hexes stay rare); II's three targets are where the
  witch scales. If the Hex chain feels slow before Awaken, move Brew's extra targets to I.
- The panel should say why no special is offered on a fresh Awaken: "choose Arc or Hex first".

**Plays well with:** Sniper (Sympathy spreads its Sunder and Hollow Point's Vulnerable; Static
Charge turns its crits into double discharges), Seeker (Rime Coven; Overload's Dazed feeds Shatter
Shot), Cinder (Ash Coven; Spreading Curse carries Scorched), Pulse (Toll and Soul Drain make every
rune last longer), Sonar (Ping's Exposed means more arc crits; Echoes if chosen), Aura (Conduit
lengthens runes; Kinship).

**Pool**

- [ ] 🟢 **The name follows the fork**: Splash unforked, **Stormcaller** on Arc, **Hexer** on Hex.
  Only the panel title and tooltip change; the internal id stays.
  - 💬
- [ ] 🔴 **Mines**: a blast leaves a charge on the path that detonates under the next enemy (a
  ground zone).
  - 💬
- [ ] If the astral and quantum theme is adopted, the witch clashes with its "fantasy words go"
  rule. The mechanics survive renaming: rune -> metastable state, Doom -> Half-life, Sympathy ->
  Entanglement, Spreading Curse -> Chain Decay.
  - 💬

### 3.3 Sonar - the Spotter

**Status:** to review (round 2). Rebuilt on the Sniper template; every earlier idea that no
decision rules out is still here, in the setup or the pool.

**Why build it.** It sees everything and makes everything easier to hit. The sweep reaches far
and hits all it passes; on top of that it Exposes the biggest threat to crits from every tower,
reveals the invisible, marks targets for a guaranteed crit from any tower, wears crit immunity
down, and bridges to magic. Support that also deals steady damage over a huge area. Where the
Sniper is patient with one target, the Sonar is patient with the whole map: its rhythm is the
revolution. **Buy it when** ghosts appear, when the defence leans on crits, against crit-immune
armor, and on spread-out lanes (on Twisted Hourglass one Sonar can cover all three). **Weak
against** packed fast groups (one hit per revolution) and plating (its hits are small). **Look:**
a faint ping ring each revolution; a crosshair over the pinged enemy; a second beam (Twin Array);
a blue beam once it deals magic.

**Proposed setup**

```
SONAR    $20 | dmg 16 | sweep 3 s, range 4.5 (settle, 9.2) | physical
BASE     Fortify   Ping: each pass Exposes the healthiest enemy it sweeps (crit taken x2)
         Range     +15%  ->  II +10%  ->  III Deep Scan: the outer quarter reveals for 1 s
HEAD A   Twin Array (more passes: more hits, more pings)
         I    +25% damage; Ping Exposes the two healthiest
         II   +25% damage, +10% crit; a beam crit refreshes Exposed
         III  Twin Beam: a second beam opposite, at 75%
         IV-A Quad Array: 4 beams at 70%        IV-B Phased Array: locks and sweeps the boss
HEAD B   Long Reach (far: the spotter on the hill)
         I    +20% range; Ping picks the healthiest past half range, Exposed for two passes
         II   damage up to +100% at max range, +10% crit
         III  Resonant Crack: each hit takes 10 resilience (to -50, back 10 a second)
         IV-A Horizon: +40% range, far bonus +150%, blind within 1.5 cells
         IV-B Fault Line: resilience stays down while Exposed, and falls to -100
EXTRA    Frequency (the magic opt-in): Ultrasound, Harmonics, Pure Tone, Shatter Tone
SPECIAL  shared: Mark on Sweep, Wide Band, Command Ping
```

**Base line**

- [ ] 🟢 Settle the base sweep: the tower line says 4 s per turn and range 4.2, the code 2 s and
  5.2. Fire rate *is* the rotation speed, so this is its DPS dial. ⭐ 3 s and 4.5 (9.2).
  - 💬
- [ ] 🟢 A faint ping ring each revolution (visual only), so the rhythm reads.
  - 💬

**BASE**

- [ ] ⭐ 🟡 Fortify **Ping** (the signature passive): each revolution Exposes the enemy with most
  health the beam passes (crit chance taken x2) until the next pass. Crits for everyone, from the
  first base upgrade, and a visible "this one" for the player.
  - 💬
- [ ] alt passive **Tracking**: an enemy the beam hits on consecutive passes gains a Tracked stack,
  up to 3; each stack gives every tower +4% crit chance against it. Slower to build, spread over
  everything the beam keeps touching.
  - 💬
- [ ] 🟢 Range II +15%, Range III +10%.
  - 💬
- [ ] ⭐ 🟡 Range III **Deep Scan**: each revolution reveals invisible enemies in the outer quarter
  of its range for 1 s.
  - 💬
- [ ] alt Range III **Counter-Jamming**: every tower within the Sonar's range takes half
  disruption. The Sonar tracks the Jammer (once the Jammer is in waves, 5).
  - 💬

**HEAD chain A - Twin Array** (more passes: more hits, more pings, more reveals)

- [ ] ⭐ 🟢 I: +25% damage, and Ping Exposes the two healthiest enemies it passes.
  - 💬
- [ ] ⭐ 🟢 II: +25% damage, +10% crit (Sonar's crit level); a beam crit refreshes Exposed on its
  target.
  - 💬
- [ ] ⭐ 🟢 III **Twin Beam**: a second beam, opposite, at 75% damage (9.3 smooths the cliff).
  Answers its counter: a packed fast group now takes two hits a revolution.
  - 💬
- [ ] ⭐ 🟢 IV-A **Quad Array**: 4 beams at 70% damage each. Twice the hits, pings, marks and
  reveals.
  - 💬
- [ ] ⭐ 🟡 IV-B **Phased Array**: stops spinning, locks onto the enemy with most health in range and
  sweeps a 60 degree arc back and forth over it: about 3x the hits on the focus. Boss mode.
  - 💬

**HEAD chain B - Long Reach** (far: the spotter on the hill; level I now delivers reach, 9.4)

- [ ] ⭐ 🟢 I: +20% range, and Ping picks the healthiest enemy past half range, Exposed for two
  passes.
  - 💬
- [ ] ⭐ 🟢 II: damage up to +100% at max range (exists), +10% crit.
  - 💬
- [ ] ⭐ 🟡 III **Resonant Crack** (moved up from IV-B: it is the chain's answer to crit-immune
  armor): each beam hit lowers resilience by 10, down to -50, recovering 10 a second. Crit-immune
  enemies open after a few passes, through the one door Decisions leaves.
  - 💬
- [ ] alt III **Far Echo**: hits beyond half range apply Exposed for 3 s. (Overlaps Ping now.)
  - 💬
- [ ] ⭐ 🟢 IV-A **Horizon**: +40% range, far bonus up to +150%, but no damage within 1.5 cells. The
  selfish version: a second Sniper made of sound.
  - 💬
- [ ] ⭐ 🟡 IV-B **Fault Line**: Resonant Crack's loss doesn't recover while the enemy is Exposed,
  and goes down to -100, where every crit's bonus is doubled. The team version: the boss under
  Ping becomes the whole defence's crit target.
  - 💬

**Extra head node - Frequency** (the magic opt-in; small mechanics both chains feed, since more
hits mean more of each)

- [ ] ⭐ 🟡 I **Ultrasound**: 20% of each hit is added as magic damage, up to 50% against armored or
  shielded enemies (Piercing Tone, folded in: it was the special that made Sonar matter against
  armor).
  - 💬
- [ ] ⭐ 🟡 II **Harmonics**: hits apply Resonating (+8% magic damage taken per stack, 3 stacks,
  4 s). Support for Seeker, Cinder, Pulse and the Arc Splash.
  - 💬
- [ ] ⭐ 🟡 III **Pure Tone**: the beam deals magic instead of physical, +15% magic penetration.
  - 💬
- [ ] ⭐ 🟡 IV **Shatter Tone**: a hit on a shielded enemy breaks a quarter of the shield; at zero the
  shield is gone.
  - 💬
- [ ] alt extra node **Datalink** (detection): I towers within Sonar's range can target invisible
  enemies within 1 cell of themselves; II within 2 cells; III reveals last +1 s; IV Marked enemies
  are visible to every tower on the map.
  - 💬

**Specials** (one shared set: each works on either chain, through the pass)

- [ ] ⭐ 🟡 **Mark on Sweep**, reworked: the mark goes on the enemy (the Marked effect), so the next
  hit from **any** tower is a guaranteed crit. One mark per enemy, renewed each pass. The Spotter
  team's heart: Sonar marks, the Sniper fires.
  - 💬
- [ ] Against crit-immune enemies the mark can't crit (Decisions). It: [ ] ⭐ waits: an immune hit
  doesn't spend it, so it fires once Scorched or Resonant Crack opens the enemy / [ ] is wasted /
  [ ] turns into 1 Vulnerable.
  - 💬
- [ ] ⭐ 🟢 **Wide Band** (exists): each revolution reveals invisible enemies it passes. The ghost
  answer.
  - 💬
- [ ] ⭐ 🟡 **Command Ping**: each revolution names the pinged enemy the Priority; every tower that
  can reach it switches to it and deals +15% to it. A built-in focus fire, not a player choice.
  - 💬
- [ ] alt 🟡 **Echo**: every beam hit repeats 0.5 s later at 40%. Hits fast groups twice.
  - 💬
- [ ] alt 🟡 **Exposure** (the old proposal): beam hits apply Exposed for 3 s. Only if Ping isn't
  the Fortify passive.
  - 💬

How they combine by themselves: Wide Band reveals a Ghost, Ping or Command Ping picks it, Mark on
Sweep marks it, and the next Sniper shot is a guaranteed crit on a target that was invisible a
moment ago.

**Plays well with:** Sniper (Ping doubles its crit chance on the boss; Spotter Uplink shoots what
Sonar marks or reveals), Cinder (Scorched and Resonant Crack together open crit immunity fast),
Seeker, Pulse and Cinder (Harmonics' Resonating), Splash Arc (Exposed means more arc crits, and
Static Charge discharges on beam hits), Mortar (Command Ping points the shells; Spotter Call if
chosen).

**Pool**

- [ ] 🟡 Shrouded allies of an Elite Ghost are revealed while inside Sonar's range.
  - 💬
- [ ] 🟡 **Scan**: an enemy the Sonar has hit shows its resistances and weaknesses in the inspector
  ("Scanned").
  - 💬
- [ ] 🟢 Sweep direction toggle (cosmetic).
  - 💬
- [ ] 🟡 **Doppler** (new): the beam deals +30% to enemies moving toward the Sonar and -15% to those
  moving away. Build it facing the incoming lane.
  - 💬
- [ ] 🟡 **Boss Sweep** (new): once a wave, when a Boss enters range, the beam reverses and sweeps
  it twice in a row.
  - 💬

### 3.4 Pulse - the Field

**Status:** to review (round 2). Rebuilt on the Sniper template. Knockback is gone by rule
(Shockwave, Pull), so is the bounty (Harvester); everything else earlier is still here.

**Why build it.** A circle where the rules change. It touches every enemy inside on every tick, so
it carries "while inside" effects and builds stacks faster than anything. Its damage is
incidental; its job is to make its circle the place enemies must not linger: slowed, anchored,
silenced, softened, drained, amplified. The longer an enemy stays, the worse it gets (Toll), so
build it where the path loops or bends around it and pair it with chill. **Buy it when** casters
show up (Mender, Elite Ghost's shroud, the Warden), for clumps in loops, against rushes. **Weak
against** long straights and spread-out lanes. **Look:** rings rippling outward, faster as Toll
builds; the field's colour says its mode (violet silence, green corrosion, blue stasis); enemies
inside flicker.

**Proposed setup**

```
PULSE    $25 | 2 a tick, magic ✅ | range 1.75 | fires without a visible target
BASE     Fortify   Toll: each second inside adds a stack (5): +10% field damage, debuffs 10% slower
         Range     +15%  ->  II +10%  ->  III Wide Field: +20% range, Toll lingers 1 s longer
HEAD A   Overcharged Coils (the charge: the field bites)
         I    +30% damage; Toll builds twice as fast
         II   +25% damage; an enemy at full Toll takes +25% from the field
         III  Arc Discharge: a zap a second on the healthiest inside, 15x a tick, can crit (+10%)
         IV-A Meltdown: Toll stacks to 10       IV-B Tesla Coil: the zap chains to 3 more
HEAD B   Phase Field (the rules; renamed from Resonant Field, 9.5)
         I    +20% range; Toll stays 2 s after an enemy leaves
         II   +15% range; reveals what it hits for 2 s
         III  Null Field: enemies inside are Silenced
         IV-A True Sight: reveals within twice the field, 4 s
         IV-B Dead Zone: no shields, no heals inside
EXTRA    Field Shaping: Undertow (chill and Anchored), Corrosion, Stasis, Event Horizon
SPECIAL  shared: Warding Field, Soul Drain, Kill Zone
```

**Base line**

- ✅ 🟢 **Magic damage** (decided in 1.3). Physical plating erased its 2-per-tick hits entirely.
- [ ] 🟢 Range 1.5 -> 1.75, so it covers both sides of a bend.
  - 💬
- [ ] ⭐ 🟢 Fires without a visible target from the start (it is the ghost hitter); Phase Field I
  then pays with Toll instead.
  - 💬

**BASE**

- [ ] ⭐ 🟡 Fortify **Toll** (the signature passive; the old resonance ramp plus your "slower
  recovery"): each second an enemy spends inside adds a Toll stack, up to 5, fading 1 s after it
  leaves. Each stack: +10% field damage, and every debuff on it wears off 10% slower. The longer it
  lingers, the more it pays: loops, bends and chill are rewarded at once.
  - 💬
- [ ] alt passive **Grounding**: +25% damage for the first second after it starts firing (a
  capacitor kick). Simpler, but it pays for arrivals, not lingering.
  - 💬
- [ ] 🟢 Range II +15%, Range III +10%.
  - 💬
- [ ] ⭐ 🟡 Range III **Wide Field**: +20% range, and Toll stacks last 1 s longer after leaving.
  - 💬

**HEAD chain A - Overcharged Coils** (the charge: the field bites)

- [ ] ⭐ 🟢 I: +30% damage, and Toll builds twice as fast (a stack every half second).
  - 💬
- [ ] ⭐ 🟡 II: +25% damage, and an enemy at full Toll takes +25% from the field.
  - 💬
- [ ] ⭐ 🟡 III **Arc Discharge**: once a second a zap hits the enemy inside with most health for 15x
  the tick damage. The zaps carry Pulse's crit (+10%), so crits are visible moments instead of a
  hidden +5% on 20 ticks a second (9.4). A burst that answers its counter: big hits through what
  ticks barely scratch.
  - 💬
- [ ] ⭐ 🟡 IV-A **Meltdown**: Toll stacks to 10 instead of 5. Built at a loop, nothing leaves alive.
  - 💬
- [ ] ⭐ 🟡 IV-B **Tesla Coil**: the zap chains to 3 more enemies within 1.5 cells, even outside the
  field. The field reaches out.
  - 💬

**HEAD chain B - Phase Field** (the rules; renamed from Resonant Field so it no longer echoes
Aura's Resonance Field, 9.5)

- [ ] ⭐ 🟢 I: +20% range, and Toll stays 2 s after an enemy leaves, so loops and second passes carry
  it back in. (If the base keeps needing a visible target, I also lets it hit invisible enemies,
  as today.)
  - 💬
- [ ] ⭐ 🟢 II (exists): +15% range, and it reveals what it hits for 2 s.
  - 💬
- [ ] ⭐ 🟡 III **Null Field**: enemies inside are Silenced: no heal or shield pulses, summons,
  shrouds or vanishing while inside; the Jammer's aura keeps running (Decisions).
  - 💬
- [ ] ⭐ 🟡 IV-A **True Sight**: reveals everything within twice the field's radius; reveals last 4 s.
  - 💬
- [ ] ⭐ 🟡 IV-B **Dead Zone**: entering strips shields, and nothing inside can be healed or shielded.
  Build it where the Warden walks.
  - 💬

**Extra head node - Field Shaping** (your crazy ideas, as rules every Pulse can add on either chain)

- [ ] ⭐ 🟡 I **Undertow**: enemies inside are chilled 25%, the chill doesn't fade while they stay (it
  counts as chill for freezes), and they are Anchored: no speed-ups, speed capped at 75% of base.
  Pulse owns "anchors" (1.2), so Phase Lock lives here.
  - 💬
- [ ] ⭐ 🟡 II **Corrosion**: -30 armor while inside; armor stops at 0.
  - 💬
- [ ] ⭐ 🟡 III **Stasis**: debuffs on enemies inside wear off at a quarter of the speed: Vulnerable,
  marks, Scorched, Sickened, chill, runes.
  - 💬
- [ ] ⭐ 🟡 IV **Event Horizon**: each death inside adds +5% field damage until the wave ends (up to
  +100%), and a dying enemy's debuffs pass to the nearest enemy inside.
  - 💬

**Specials** (one shared set, all about what happens inside)

- [ ] ⭐ 🟡 **Warding Field** (exists; payload decided in 1.4): each tick, a 10% chance to add a stack
  of Sundered or Exposed. Inside the field that means "fully Sundered or Exposed within 2 s", so
  the node text should say exactly that (9.4). Rename it Rattle Field (9.5).
  - 💬
- [ ] ⭐ 🟡 **Soul Drain** (your spirit idea; Pulse owns spirit, 1.2): each second inside costs 5
  spirit (as Sickened stacks), and the field deals +1% damage per point of spirit below zero. At
  -100: double damage, no heals or shields, and stack debuffs never wear off. High-spirit enemies
  (bosses, a future Priest) are drained longest.
  - 💬
- [ ] ⭐ 🟡 **Kill Zone**: enemies inside take +25% damage from every *other* tower (its own
  multiplier, outside Vulnerable's cap). The place every other tower should point at.
  - 💬
- [ ] alt 🔴 **Magnetic Field**: Mortar shells and Seeker missiles landing inside home onto the
  nearest enemy; nothing misses inside the field.
  - 💬
- [ ] alt sets per chain, if one shared set feels flat: Overcharged Coils -> Kill Zone, Warding
  Field, Magnetic Field (the field that hurts); Phase Field -> Soul Drain, Mirror Field, Dead Air
  (pool) (the field that rules).
  - 💬

How they combine by themselves: Soul Drain's lost spirit and Toll both slow every debuff, so
Warding Field's stacks never fall off; Kill Zone turns that into the whole defence's damage.

**Plays well with:** Mortar (Undertow holds a column inside the bracket; Kill Zone under the
shells), Splash Hex (Toll, Stasis and Soul Drain stretch every rune; Doom stores more inside Kill
Zone), Seeker (Undertow's chill makes freezes longer; Null Field plus EMP shuts casters down),
Sniper (Corrosion plus Sunder strips an Elite bare), Cinder (an Inferno Ring beside it: two fields,
one loop).

**Pool**

- [ ] 🟡 **Overcharge Grid**: adjacent towers get +15% fire rate while the Pulse is firing.
  - 💬
- [ ] 🟡 **Capacitor**: charges while idle (up to 5 s) and releases one nova when the first enemy
  enters.
  - 💬
- [ ] 🟡 **Mirror Field**: damage absorbed by shields inside is reflected back as magic.
  - 💬
- [ ] 🟡 **Tuning Fork** (new): two Pulses whose fields overlap share Toll, and an enemy in the
  overlap gains it twice as fast. Build pairs at a loop.
  - 💬
- [ ] 🟡 **Dead Air** (new): an enemy that would cast inside the field (if it isn't silenced) takes
  10% of its max health instead, and the cast still fails half the time.
  - 💬

### 3.5 Aura - the Mentor

**Status:** to review (round 2). Rebuilt on the Sniper template, cut to Aura's shape: it caps at
Awaken, so its chains and extra node stop at III and it has one special slot. The bounty special
(Tithe) is gone by rule; Command (your click-to-focus) is new.

**Why build it.** The support tower: it makes a cluster stronger, shields it from jamming, and
helps young towers climb (XP, chapter 10). It never attacks, so its worth is what it lets the
others do, and it is worth most in the middle of a *mixed* cluster (Kinship). **Buy it when**
you've committed to a cluster, when one tower takes all the kills, against Jammers. **Weak
against** spread-out defences. **Look:** glow lines to buffed towers (exist), coloured by what the
aura grants; a small glyph per tower type it counts for Kinship; a book glyph on towers it is
mentoring.

**Proposed setup**

```
AURA     $20 | passive | range 1.5 | buff +20% range and damage | towers in range: half disruption
BASE     Fortify   Kinship: +5% buff strength per other tower type in range, up to +20%
         Range     base 2.05  ->  II base 3.17            (no Range III, no Transcendent)
HEAD A   Amplifying Core (strength)
         I    +25% range and damage; Kinship counts double for damage
         II   +30% range and damage, +10% fire rate
         III  Keen Edge [next to another Aura]: buffed towers +50% crit damage ✅
HEAD B   Broadcast (reach; renamed from Resonance Field, 9.5)
         I    +30% range; Kinship also counts tower types up to 1 cell beyond the aura
         II   buffed towers +15% range (replaces aura-on-aura)
         III  Conduit: buffed towers' effects last 25% longer and bite 15% harder
EXTRA    Tutelage (XP): +25% XP, shared lessons, a head start            (I to III)
SPECIAL  one slot, shared: Withering Field, Command, Chosen
```

**Rules**

- [ ] ⭐ 🟢 **Auras never buff Auras.** Drop Resonance Field II's aura-on-aura.
  - 💬
- ✅ 🟡 **"Next to an Aura" as a gate** for Aura nodes (decided in 2.8): two Auras side by side
  unlock each other's top levels without feeding each other's numbers.
- [ ] 🟢 **Diminishing stacking**: the 2nd aura on a tower gives 75%, the 3rd 50%. Stops aura
  carpets, keeps two worthwhile.
  - 💬
- [ ] 🟡 **New axes Aura can grant**: crit damage, effect duration, effect potency, penetration,
  projectile speed. A support tower for support towers, not only for damage dealers.
  - 💬
- [ ] ⭐ Keep the cap at Awaken (no Transcendent, one special).
  - 💬
- [ ] Or give Aura a Transcendent gated on "next to 2 Auras", unlocking a second special.
  - 💬

**BASE**

- [ ] ⭐ 🟢 Base: towers in range take half disruption (the old Fortify Anchor, moved to the base):
  jamming is no longer handled by every Fortify, so the Aura is the answer to it, from the start.
  - 💬
- [ ] ⭐ 🟡 Fortify **Kinship** (the signature passive): the aura's buff grows +5% for each other
  tower type in range, up to +20%. It pays Aura for crowning a mixed cluster, and makes combining
  towers a goal of its own.
  - 💬
- [ ] alt passive **Mentorship**: buffed towers earn +25% XP. On purpose (the gate helper), but
  invisible in a fight.
  - 💬
- Range (base range 2.05) and Range II (3.17), as in the feature doc.

**HEAD chain A - Amplifying Core** (strength: the cluster hits harder)

- [ ] ⭐ 🟢 I (feature doc numbers): +25% range and damage, and Kinship counts double for damage.
  - 💬
- [ ] ⭐ 🟢 II (feature doc numbers, 9.3): +30% range and damage, +10% fire rate.
  - 💬
- ✅ 🟡 III **Keen Edge** [next to another Aura]: buffed towers +50% crit damage (decided in 2.1:
  Sniper 250%, others 200%).
- [ ] alt III **Overdrive**: +20% more fire rate, if Keen Edge's gate is too strict.
  - 💬

**HEAD chain B - Broadcast** (reach: the cluster grows; renamed from Resonance Field, 9.5)

- [ ] ⭐ 🟢 I: +30% range, and Kinship also counts tower types up to 1 cell beyond the aura.
  - 💬
- [ ] ⭐ 🟢 II: buffed towers also gain +15% range (replaces aura-on-aura).
  - 💬
- [ ] ⭐ 🟡 III **Conduit**: buffed towers' effects last 25% longer and bite 15% harder (chill level,
  burn and poison pool, freeze time, Vulnerable clock, runes). Makes Aura matter to Sonar, Seeker,
  Cinder and the Hex Splash, not only to damage dealers.
  - 💬

**Extra head node - Tutelage** (the gate helper, rebuilt on XP; I to III)

- [ ] ⭐ 🟡 I: buffed towers earn +25% XP.
  - 💬
- [ ] ⭐ 🟡 II **Shared Lessons**: a kill by a buffed tower gives 25% of its XP to every other buffed
  tower.
  - 💬
- [ ] ⭐ 🟡 III **Head Start**: a tower built in range starts with a third of the XP of the most
  experienced buffed tower.
  - 💬
- [ ] alt extra node **Veterancy**: I a tower built in range starts with 50 XP; II with 150; III it
  also starts with Fortify owned.
  - 💬

**Special** (one slot; three on offer, shared by both chains)

- [ ] ⭐ 🟡 **Withering Field** (exists): enemies within the aura's range +1 cell gain Vulnerable.
  Aura owns Vulnerable (1.4); the extra cell is the 9.4 fix (an Aura stands among towers, not on
  the path).
  - 💬
- [ ] ⭐ 🟡 **Command** (your click-to-focus, 2.5): towers this Aura buffs switch to the enemy you
  have selected while it is in their range. One purchase steers a cluster; nothing changes for a
  player who never clicks. The selection is already world state, resolved on the game loop.
  - 💬
- [ ] ⭐ 🟢 **Chosen**: buffs only the one tower in range with the most damage dealt, at triple
  strength. For the Transcendent hero; the opposite choice to spreading thin.
  - 💬
- [ ] alt 🟡 **Rally**: when an Elite or Boss enters a buffed tower's range, every buffed tower gets
  +30% fire rate for 5 s.
  - 💬
- [ ] alt 🟡 **Beacon**: buffed towers can target invisible enemies within their own range
  (detection as a buff).
  - 💬
- [ ] alt 🟡 **Bulwark**: towers in range are immune to disruption.
  - 💬

**Plays well with:** everyone, by design, and Kinship pays for variety: one Sniper, one Sonar, one
Splash and one Seeker around one Aura is +20% before any chain. The hero (Chosen + Keen Edge on a
Transcendent Sniper); Conduit with the Hex Splash, Seeker and Cinder; Command with Momentum
Snipers; Withering Field with Sympathy (the Vulnerable spreads through the hexed group).

**Pool**

- [ ] 🟡 **Mentor**: each kill by the strongest buffed tower also counts as XP for the buffed tower
  with the least.
  - 💬
- [ ] 🟡 **Harmony** (new): an enemy hit by three different buffed tower types within 2 s takes
  +15% from all buffed towers for 3 s. Combining towers, paid on the enemy.
  - 💬

### 3.6 Mortar - the Artillery

**Status:** to review (round 2). Rebuilt on the Sniper template. Gravity Shell is gone by rule (no
pull); everything else earlier is still here.

**Why build it.** The only tower that shapes the ground. Slow shells with a huge area: nukes for
swarms, busters for bosses, and fields of fire, tar and frost that keep working after the shell
lands. It holds the chokepoint where lanes meet, and it gets better the longer it shells the same
spot (Bracketing): artillery ranging in. **Buy it when** waves come packed (swarm, column), lanes
converge (Twisted Hourglass's waist), bosses bring escorts. **Weak against** fast single enemies
(misses), whatever is right next to it, and spread-out lanes (Bracketing resets). **Look:** shell
size grows with damage; a ranging marker on the bracketed spot that tightens per step; a nuke is a
white flash and an expanding ring; napalm an orange flickering patch, tar a dark glossy one, frost
pale blue; a faster shell is smaller and leaves a streak.

**Proposed setup**

```
MORTAR   $30 | dmg 32 | a shell every 3.5 s | range 4.5 | blast 1.75 | shell 8 px a tick (9.2)
         physical | leads its target | can't hit within 1 cell
BASE     Fortify   Bracketing: a shell within 1.5 cells of the last: +10% damage and radius (3)
         Range     +15%  ->  II +10%  ->  III Long Battery: +30% range, dead zone 1.5 cells
HEAD A   Siege Rounds (the big boom: bosses and armor)
         I    +30% damage; Bracketing steps +15%
         II   +25% damage, +25% radius, +10% crit
         III  Heavy Shell: +50% damage, -20% fire rate; the centre is Dazed 0.5 s
         IV-A Tactical Nuke: every 4th shell x4 over x2 radius, leaves Fallout
         IV-B Bunker Buster: the centre takes x3, Cracked and Sundered; splash -30%
HEAD B   Fragmentation Rounds (swarms and lines)
         I    shrapnel ring at 25%, reaching 0.25 cells further per Bracketing step
         II   shrapnel chills 30%, +25% shrapnel damage
         III  Cluster Shell: 4 bomblets along the path around the impact, 40% each
         IV-A Carpet Bombing: 8 bomblets in a line ahead      IV-B Shrapnel Storm: Bleeding
EXTRA    Ballistics: Rifled Barrel, Proximity Fuse, Airburst, Twin Barrels
SPECIAL  shared: Plate Cracker (Cracked), Napalm (magic), Tar
```

**Base line**

- [ ] ⭐ 🟡 **Leads its target**: aims where the enemy will be when the shell lands (predictive aim,
  decided as a primitive in 2.5). At base, so Ballistics II is free for something else.
  - 💬
- [ ] 🟢 **Dead zone of 1 cell**: artillery can't hit what's under it. A trade-off for its long
  range, and a reason to build it back from the path.
  - 💬
- [ ] ⭐ Base chill moves to a special (Cryo Shells, pool), so base Mortar is a pure boom and not a
  slower Splash.
  - 💬
- [ ] ⭐ 🟢 Slow, visible shells (9.1, 9.2): projectile speed and size are tower stats (decided in
  2.5), and fast enemies can dodge.
  - 💬

**BASE**

- [ ] ⭐ 🟡 Fortify **Bracketing** (the signature passive): artillery ranges in. A shell landing
  within 1.5 cells of the last one gets +10% damage and radius, up to 3 steps; a shell elsewhere
  resets it. It pays Mortar for holding one chokepoint, and the ranging marker shows the player
  where.
  - 💬
- [ ] alt passive **Creeping Barrage**: each shell lands 0.5 cells further back along the path than
  the last, up to 3 steps, then starts over: a moving wall that walks into a column.
  - 💬
- [ ] alt Fortify **Reinforced Barrel**: shell speed +25%. Plain, if neither passive convinces.
  - 💬
- [ ] 🟢 Range II +15%, Range III +10%.
  - 💬
- [ ] ⭐ 🟡 Range III **Long Battery**: +30% range, dead zone 1.5 cells.
  - 💬
- [ ] alt Range III **Spotter Call**: +50% range against Marked or Revealed enemies (Sonar, Seeker's
  Tracer).
  - 💬

**HEAD chain A - Siege Rounds** (the big boom: bosses and armor)

- [ ] ⭐ 🟢 I: +30% damage, and each Bracketing step gives +15% instead of +10%.
  - 💬
- [ ] ⭐ 🟢 II: +25% damage, +25% radius (down from +40%, 9.4), +10% crit (Mortar's crit level).
  - 💬
- [ ] ⭐ 🟡 III **Heavy Shell**: +50% damage, -20% fire rate, a bigger, slower shell; enemies within
  0.5 cells of the impact are Dazed for 0.5 s (Dazed silences, so Mortar is a short-silence carrier
  as 1.2 asks). One big hit is the answer to plating.
  - 💬
- [ ] ⭐ 🔴 IV-A **Tactical Nuke** (your nuke): every 4th shell deals x4 damage over x2 radius and
  leaves Fallout for 4 s (spirit drained, regeneration turned into damage).
  - 💬
- [ ] ⭐ 🟡 IV-B **Bunker Buster**: the enemy at the centre takes x3 and is Cracked (plating -50%,
  5 s) and Sundered; the splash shrinks 30%. The anti-boss, anti-armor shell.
  - 💬

**HEAD chain B - Fragmentation Rounds** (swarms and lines)

- [ ] ⭐ 🟢 I (exists): a shrapnel ring at 25%, reaching 0.25 cells further per Bracketing step.
  - 💬
- [ ] ⭐ 🟢 II: shrapnel chills 30% (exists) and deals +25%. Gate it on XP, not on two adjacent
  towers (9.4).
  - 💬
- [ ] ⭐ 🟡 III **Cluster Shell**: splits into 4 bomblets scattered along the path around the impact
  (40% each, 1-cell radius). The answer to its counter: runners that dodge the shell meet a
  bomblet.
  - 💬
- [ ] ⭐ 🟡 IV-A **Carpet Bombing**: 8 bomblets laid in a line along the path ahead of the impact.
  - 💬
- [ ] ⭐ 🟡 IV-B **Shrapnel Storm**: shrapnel makes enemies Bleed (damage per cell travelled). The
  faster they run, the more they bleed.
  - 💬

**Extra head node - Ballistics** (your projectile speed, size and look: small mechanics every shell
uses, on either chain)

- [ ] ⭐ 🟢 I **Rifled Barrel**: shell speed +40%, drawn smaller with a streak.
  - 💬
- [ ] ⭐ 🟡 II **Proximity Fuse** (new, since leading the target moved to the base): a shell that
  passes over 3 or more enemies bursts early, over them. Columns stop dodging.
  - 💬
- [ ] alt II **Predictive Fire**: leads the target, if it isn't base.
  - 💬
- [ ] ⭐ 🟡 III **Airburst**: detonates above the target: +25% radius, no falloff in the inner half.
  - 💬
- [ ] ⭐ 🟡 IV **Twin Barrels**: two shells per shot at the two leading enemies, -15% damage each.
  - 💬

**Specials** (one shared set: everything the impact leaves behind)

- [ ] ⭐ 🟡 **Plate Cracker** (today's Cursed Shrapnel, renamed, 9.5; payload decided in 1.4): the
  blast applies Cracked (plating -50%). Explosions crack plates.
  - 💬
- [ ] ⭐ 🔴 **Napalm** (your burning ground; Mortar's magic opt-in, 1.3): the impact leaves burning
  ground (1-cell radius, 3 s); anything inside burns (a burn pool, earning Scorched like Cinder's).
  - 💬
- [ ] ⭐ 🔴 **Tar** (your sticky matter; Mortar tars, 1.2): the impact leaves tar (1.2 cells, 4 s):
  40% slow and poison; a tarred enemy that catches fire burns at double pool (decided in 2.7).
  - 💬
- [ ] alt 🔴 **Cryo Shells**: frost ground (3 s); chill builds while inside, and 2 s inside without
  leaving freezes (diminishing returns apply). Frost on fire bursts it, frost on tar hardens it
  (2.4's zone table).
  - 💬
- [ ] alt 🟡 **Shell Shock**: blasted enemies are Silenced for 2 s.
  - 💬
- [ ] alt 🔴 **Fallout**: irradiated ground (5 s): -10 spirit a second, heals inside become damage.
  - 💬

How they combine by themselves: through 2.4's zone table, decided. Napalm landing on Tar is an
inferno; Plate Cracker on a burning, tarred boss leaves it slow, plateless and burning double.
Bracketing keeps all of them stacking on one spot.

**Plays well with:** Pulse (Undertow holds a column inside the bracket; Kill Zone under the
shells), Seeker (Brittle: frozen enemies take +30% physical from the shell), Cinder (Tar and Napalm
double its pools), Sonar (Command Ping aims the shells; Spotter Call if chosen), Sniper (Bunker
Buster's Cracked plus Sunder Rounds), Splash Arc (bomblets and arcs: lines from both ends).

**Pool**

- [ ] 🟡 **Delayed Fuse**: the shell lies 1 s before detonating (a mine).
  - 💬
- [ ] 🟡 **Barrage**: 3 shells in quick succession, then a long reload.
  - 💬
- [ ] 🟢 Shell drawn larger as its damage grows, whichever node raised it.
  - 💬
- [ ] 🟡 **Skip Shell** (new): a shell that hits nobody bounces once, 1.5 cells along the path, and
  detonates there.
  - 💬
- [ ] 🔴 **Craters** (new): every impact leaves a crater that slows 15% for 3 s; Bracketing keeps
  one crater deep instead of many shallow ones.
  - 💬

### 3.7 Seeker - the Hunter

**Status:** to review (round 2). Rebuilt on the Sniper template; every earlier idea is still here,
in the setup or the pool.

**Why build it.** It never misses and never lets anything get away. The only single-target magic
tower: its missile ignores physical armor and plating, homes onto fast enemies, keeps its lock
through invisibility, strips shields and freezes. It is the answer to whatever breaks your
defence: the Frenzied rush, the Ghost, the shielded Warden. Its nest stores missiles in quiet
moments and releases them as a salvo when the dangerous one arrives: patience, like the Sniper,
but banked. **Buy it when** enemies are fast, evasive, shielded or plated, or a boss arrives after
a quiet stretch. **Weak against** swarms and long waves of weak enemies (Twin Warhead answers).
**Look:** slower missiles with smoke trails and a visible curve; stored missiles orbit the tower as
the nest fills; payload colours (ice blue freeze, violet arcane, yellow EMP, red tracer).

**Proposed setup**

```
SEEKER   $30 | dmg 40 magic | missile 8 px a tick (9.2) | range 4.5 | freezes
         sticky targeting | lock-on through invisibility
BASE     Fortify   Nest: the cooldown loads missiles (up to 3); a target in range gets a salvo
         Range     +15%  ->  II +10%  ->  III Over the Horizon: fires at revealed or marked at 1.5x
HEAD A   Twin Warhead (more missiles: salvos and swarms)
         I    +30% fire rate; nest +1
         II   two missiles a shot, the second at the next target
         III  Hive: nest of 6; a salvo spreads over different targets
         IV-A Swarm: each missile splits into 3      IV-B Relay: a killing missile flies on
HEAD B   Deep Freeze (control: the boss stands still)
         I    +50% freeze; the first missile of a salvo freezes for double
         II   +30% damage, +10% crit; a frozen kill shatters (exists)
         III  Brittle: frozen enemies take +30% physical ✅
         IV-A Absolute Zero: freezes everything within 1 cell     IV-B Permafrost
EXTRA    Mixed Payloads: Arcane, EMP, Tracer, the cycling nest
SPECIAL  shared: Arcane Warhead (Unraveled), Nullifier, Hunter's Mark
```

**Base line**

- [ ] ⭐ 🟢 **Slower missiles** (35 -> 8 px a tick, 9.2): you can see them hunt, and the nest has
  time to matter.
  - 💬
- [ ] ⭐ 🟡 **Sticky targeting**: keeps firing at its current target until it dies or leaves range.
  Fixes the freeze reordering (0.8). A fix, so it is base, not a perk.
  - 💬
- [ ] ⭐ 🟡 **Lock-on**: a missile keeps its target through invisibility. The Ghost's first-hit
  vanish no longer shakes it off.
  - 💬

**BASE**

- [ ] ⭐ 🟡 Fortify **Nest** (the signature passive; your nest, moved here from the base line): the
  cooldown loads a missile into the nest (up to 3) instead of firing; with a target in range,
  stored missiles launch 4 ticks apart. Idle time becomes a burst, and the orbiting missiles show
  how much is banked.
  - 💬
- [ ] The nest fills between waves too: [ ] ⭐ yes, a free opening salvo / [ ] no, only during
  waves.
  - 💬
- [ ] alt passive **Pursuit**: a missile chasing an enemy faster than base Simple speed hits +50%.
  The faster it runs, the harder it is hit: the anti-rush identity, without a nest.
  - 💬
- [ ] 🟢 Range II +15%, Range III +10%.
  - 💬
- [ ] ⭐ 🟡 Range III **Over the Horizon**: may fire at revealed or marked targets within 1.5x range.
  - 💬
- [ ] alt Range III **Last Line** (new): when an enemy enters the last 15% of its path inside the
  Seeker's range, the whole nest fires at it at once, whatever else is in range. Never lets
  anything get away, literally.
  - 💬

**HEAD chain A - Twin Warhead** (more missiles: salvos and swarms)

- [ ] ⭐ 🟢 I: +30% fire rate, and the nest holds one more.
  - 💬
- [ ] ⭐ 🟡 II: two missiles a shot, the second at the next target, not the same one (9.4: today it
  overkills and freezes the already frozen).
  - 💬
- [ ] ⭐ 🟡 III **Hive**: nest of 6; a salvo spreads across different targets. The answer to its
  counter: swarms.
  - 💬
- [ ] ⭐ 🟡 IV-A **Swarm**: each missile splits into 3 micro-missiles at launch (35% damage each,
  chill instead of freeze). The anti-swarm Seeker.
  - 💬
- [ ] ⭐ 🟡 IV-B **Relay**: a missile that kills flies on to a new target at 60% damage, up to 3
  kills.
  - 💬

**HEAD chain B - Deep Freeze** (control: the boss stands still; I and II swapped so level I freezes,
9.4)

- [ ] ⭐ 🟢 I: +50% freeze, and the first missile of a salvo freezes for double.
  - 💬
- [ ] ⭐ 🟢 II: +30% damage, +10% crit (Seeker's crit level); a frozen kill shatters (exists).
  - 💬
- ✅ 🟡 III **Brittle** (decided in 2.2 and 2.7): frozen enemies take +30% physical damage. Seeker
  freezes, Sniper and Mortar smash.
- [ ] ⭐ 🟡 IV-A **Absolute Zero**: the missile freezes everything within 1 cell of the impact;
  shatters deal x2.
  - 💬
- [ ] ⭐ 🟡 IV-B **Permafrost**: freeze +100%; a target the freeze can't hold (immune or diminished)
  is chilled 60% instead and takes +25% magic. The boss version: diminishing returns stop being
  the end of the story.
  - 💬

**Extra head node - Mixed Payloads** (your "some freeze, some do something else": small mechanics
on every 3rd missile, on either chain)

- [ ] ⭐ 🟡 I: every 3rd missile is **Arcane**: Unraveled (magic resist down) instead of a freeze.
  - 💬
- [ ] ⭐ 🟡 II: every 3rd missile is **EMP**: strips shields and Silences for 2 s (Seeker is a
  short-silence carrier, 1.2).
  - 💬
- [ ] ⭐ 🟡 III: every 3rd missile is a **Tracer**: reveals and Marks (the next hit crits).
  - 💬
- [ ] ⭐ 🟡 IV: the nest cycles Cryo, Arcane, EMP, Tracer, each payload +25% stronger. The orbiting
  missiles show the order.
  - 💬
- [ ] alt extra node **Guidance**: I lock-on through invisibility (if not base); II missile speed
  and turn rate +50%; III retargets without slowing when its target dies; IV **Seek and Destroy**:
  fires at Marked enemies anywhere on the map.
  - 💬

**Specials** (one shared set, all on the missile's impact)

- [ ] ⭐ 🟡 **Arcane Warhead** (today's Homing Curse, renamed, 9.5; payload decided in 1.4): the
  impact applies Unraveled, 1 stack, 2 on a frozen or chilled target. Seeker becomes the magic
  enabler for Cinder, Pulse, Sonar's Pure Tone and the Arc Splash.
  - 💬
- [ ] ⭐ 🟡 **Nullifier**: the impact strips shields and heals over time (dispel, decided in 2.5);
  +50% damage against shielded enemies. The Warden answer.
  - 💬
- [ ] ⭐ 🟡 **Hunter's Mark**: each consecutive hit on the same target +20% (up to +100%); prefers the
  highest rank. The boss hunter, and the Seeker's Steady Aim.
  - 💬
- [ ] alt 🟡 **Heat Seeker**: prefers burning targets; on a burning target the freeze is replaced by
  a Thermal Shock (the burn detonates at 150%). Fire and ice on one tower: swap it in for
  Hunter's Mark beside a Cinder.
  - 💬
- [ ] alt 🟡 **Swarm Nest**: nest x2, and a full salvo launches by itself when an Elite or Boss
  enters range.
  - 💬
- [ ] alt 🟡 **Arcane Missiles**: Unraveled on every impact, if Homing Curse keeps Vulnerable.
  (Moot once 1.4's payload change ships.)
  - 💬

How they combine by themselves: a nest salvo is a string of consecutive hits, so Hunter's Mark
climbs inside one salvo; Nullifier strips the shield on the first missile and the rest land
Unraveled.

**Plays well with:** Cinder (Frostfire: freezing a burning enemy bursts the burn, 2.7), Sniper and
Mortar (Brittle), Splash (Rime Coven makes every freeze longer and burstier; Overload's Dazed and
freeze share a ladder), Pulse (Undertow's chill lengthens freezes; Null Field plus EMP), Sonar
(Tracer plus Mark on Sweep: two kinds of guaranteed crit).

**Pool**

- [ ] 🟡 **Proximity Fuse**: a missile passing within 0.5 cells of 3 or more enemies bursts for a
  small blast.
  - 💬
- [ ] 🟡 **Decoy Flare**: a missile whose target vanished flies to its last known spot and reveals
  everything within 1 cell.
  - 💬
- [ ] 🟡 **Afterburner**: missiles speed up the longer they fly, and hit harder for it.
  - 💬
- [ ] 🟡 Fortify alternative: aims at the fastest enemy, built in (the runner hunter).
  - 💬
- [ ] 🟡 **Ice Trail** (new): a missile leaves a short chill trail along its flight; a salvo paints
  the path blue.
  - 💬

### 3.8 Cinder - the Pyre

**Status:** to review (round 2). Rebuilt on the Sniper template and on the decided DoT rule (a
pulse never crits; the ignition can). Every earlier idea is still here, in the setup or the pool.

**Why build it.** The magic damage-over-time tower: it shrugs off armor and plating, scorches
resilience so every crit lands harder for everyone (and wears crit immunity down), and burns what
hides or heals. Its cone is short, so build it where the path bends around it: each enemy walks
through several waves, and every wave stokes the fire higher (Stoke). The crit team's best friend
and the healer's worst enemy. **Buy it when** groups of mid-health enemies come, against healers
(Menders) and regenerating bosses, against ghosts that pass close, alongside a crit defence.
**Weak against** far-away straights, burn-immune enemies (the Warden's second egg; Soulfire
answers), and freeze-heavy teammates (unless Thermal Shock, or Splash's Ash Coven). **Look:**
flame colour by tier: orange, white (White Flame), blue (Soulfire), deep red under Searing Flame;
Stoke as a brighter core per step; lingering flames as flickering patches.

**Proposed setup**

```
CINDER   $28 | burn 2 a wave, magic | range 2.2 | cone 0.45 rad (9.2) | burn 3 s
         a pulse never crits; a crit ignition starts the pool at the crit multiplier ✅
BASE     Fortify   Stoke: a wave on an enemy already burning from it: +10% burn (3)
         Range     +15%  ->  II +10%  ->  III Long Nozzle: +20% range, a wave twice as fast
HEAD A   White Flame (hot and short: the single enemy melts)
         I    +30% damage; Stoke steps +15%
         II   +50% damage, -25% burn duration, +10% crit (ignitions)
         III  Soulfire: a blue third pool; earns Sickened too; burns the burn-immune
         IV-A Flashpoint: +20% crit; crit ignitions add 3 Scorched      IV-B Combustion
HEAD B   Wide Nozzle (wide and long: the crowd smoulders)
         I    +25% range, +30% cone; an enemy leaving the cone keeps its Stoke 2 s
         II   +20% range, +20% fire rate, +20% cone
         III  Lingering Flames: each wave leaves burning ground for 2 s
         IV-A Inferno Ring: a full ring at -25% range      IV-B Dragon's Breath: a stream
EXTRA    Fuel: Kindling, Cauterize, Heat, Everburn
SPECIAL  shared: Searing Flame (Vulnerable), Wildfire, Thermal Shock
```

**Base line**

- ✅ 🟡 **Burning reveals** (decided in 2.7, as an interaction row, so any burn does it): a burning
  invisible enemy is visible while its pool is above a fuel level; a little burn doesn't reveal.
- ✅ 🟢 **DoT crits** (decided after round 1): a burn pulse never crits, so no crit sparks four
  times a second; the wave that ignites can crit, and a crit ignition starts the pool at the crit
  multiplier.
- [ ] 🟡 **Burning enemies receive 50% less healing** at base. ⭐ Or only with Fuel II (Cauterize).
  Either way Cinder owns regeneration and heals (agreed after round 1); this is only where.
  - 💬
- [ ] 🟢 Cone half-width 0.35 -> 0.45 rad (9.2): today the cone covers 1.7 square cells, so an enemy
  crossing it sideways barely burns.
  - 💬

**BASE**

- [ ] ⭐ 🟡 Fortify **Stoke** (the signature passive): each wave that hits an enemy already burning
  from this Cinder raises its burn +10%, up to 3 times; the stacks go when the burn ends. It pays
  Cinder for being built where the path bends around it.
  - 💬
- [ ] alt Fortify **Pilot Light**: the first wave at a fresh target burns 50% harder. It pays for
  arrivals instead of staying, which suits a straight.
  - 💬
- [ ] 🟢 Range II +15%, Range III +10%.
  - 💬
- [ ] ⭐ 🟡 Range III **Long Nozzle**: +20% range, and the wave travels twice as fast.
  - 💬

**HEAD chain A - White Flame** (hot and short: the single enemy melts)

- [ ] ⭐ 🟢 I: +30% damage, and Stoke steps are +15%.
  - 💬
- [ ] ⭐ 🟢 II: +50% damage, -25% burn duration (instead of today's +50% duration), so this chain
  means *hot and short* and chain B means *wide and long*; +10% crit, which counts on ignitions.
  - 💬
- [ ] ⭐ 🟡 III **Soulfire** (your higher-level burn; decided as an effect in 2.2): a second burn
  type, blue, in its own pool, so it stacks with ordinary burn; it earns Sickened as well as
  Scorched. It answers the chain's counter: a burn-immune enemy still takes Soulfire.
  - 💬
- [ ] ⭐ 🟡 IV-A **Flashpoint**: +20% crit chance; a crit ignition also adds 3 Scorched. The crit
  team's furnace: every crit it lands makes everyone else's crits hit harder.
  - 💬
- [ ] ⭐ 🟡 IV-B **Combustion**: the pool cap doubles; a pool that reaches its cap bursts for half of
  it onto enemies within 1 cell. The boss melter that also hurts the escort.
  - 💬

**HEAD chain B - Wide Nozzle** (wide and long: the crowd smoulders)

- [ ] ⭐ 🟢 I: +25% range, +30% cone (exists), and an enemy that leaves the cone keeps its Stoke for
  2 s.
  - 💬
- [ ] ⭐ 🟢 II (exists): +20% range, +20% fire rate, +20% cone.
  - 💬
- [ ] ⭐ 🔴 III **Lingering Flames**: each wave leaves burning ground on the path it swept for 2 s
  (ground zones, decided in 2.4). The answer to its counter: what walks out of the cone keeps
  walking through fire.
  - 💬
- [ ] ⭐ 🟢 IV-A **Inferno Ring**: the cone becomes a full ring at -25% range. A burning Pulse.
  - 💬
- [ ] ⭐ 🟢 IV-B **Dragon's Breath**: x3 fire rate, per-wave damage divided by 2.5, +30% range: a
  continuous stream that keeps every pool topped up and Stoke always at full.
  - 💬

**Extra head node - Fuel** (burn chemistry: small mechanics every burn uses, on either chain)

- [ ] ⭐ 🟡 I **Kindling**: burning enemies lose 30% burn resistance and earn Scorched twice as fast.
  - 💬
- [ ] ⭐ 🟡 II **Cauterize**: burning enemies receive 50% less healing and shielding. Cinder owns
  regeneration and heals (agreed after round 1), so this node is how.
  - 💬
- [ ] ⭐ 🟡 III **Heat**: burning enemies take +10% magic damage (burning reveals is now a global row,
  so III no longer needs Illumination).
  - 💬
- [ ] ⭐ 🟡 IV **Everburn**: a pool never decays below 25% while its enemy is inside Cinder's range.
  - 💬

**Specials** (one shared set, all on the ignition and the pool)

- [ ] ⭐ 🟡 **Searing Flame** (today's Hexflame, renamed: hexes are Splash's now, 9.5): an ignition
  grants Vulnerable, and so does every wave that hits a burning enemy, at most one stack a second
  (9.4: today each visit gives one stack). Cinder keeps its share of Vulnerable (1.4).
  - 💬
- [ ] ⭐ 🔴 **Wildfire**: once a second a burning enemy ignites neighbours within 0.75 cells at 40% of
  its pool; on death the whole pool spreads. The swarm killer.
  - 💬
- [ ] ⭐ 🟡 **Thermal Shock**: when a burning enemy is frozen by anything, its remaining pool
  detonates as one hit at 150% (the decided global row lands 50%) and chills its neighbours. Fire
  and ice become a combo instead of a conflict.
  - 💬
- [ ] alt 🟡 **Heat Haze**: burning enemies are Exposed (crit chance taken x1.5); the alternative
  carrier of Exposed (1.2).
  - 💬
- [ ] alt 🟡 **Pyromancer's Mark**: burning enemies take +15% magic damage from every source.
  - 💬
- [ ] alt 🔴 **Phoenix Ash**: an enemy that dies burning leaves embers (1 cell, 2 s) that burn
  whatever it spawns: Reaver's split, the Frenzied brood, the Warden's egg.
  - 💬

How they combine by themselves: Wildfire spreads ignitions, and every new ignition is a Searing
Flame stack; Thermal Shock turns a teammate's freeze into a burst instead of a lost pool, so
Wildfire's spreading is never wasted. With Stoke at full, each starts from a hotter pool.

**Plays well with:** Seeker (Frostfire, with Thermal Shock at 150%), Mortar (Tar doubles its pools,
Napalm adds a second source), Splash (Ash Coven: double pools no freeze puts out; Spreading Curse
carries Scorched; Brew's poison stacks with burn), Sonar and Sniper (Scorched plus Resonant Crack
opens crit immunity; Flashpoint feeds the crit team), Pulse (Inferno Ring beside a Pulse: two
fields in one loop).

**Pool**

- [ ] 🔴 **Fire Wall**: a line of fire across the path in front of the tower, 3 s, every 10 s.
  - 💬
- [ ] 🟡 **Backdraft**: the first wave after 3 s of idling is twice as wide.
  - 💬
- [ ] 🔴 **Ember Trail**: a burning enemy drops small embers on the path behind it, so the enemies
  following it catch fire. Strongest against columns and drips.
  - 💬
- [ ] 🟡 **Bellows** (new): an adjacent Aura's fire-rate buff also widens the cone 10% per +10%.
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
  Resonant Crack does it in a burst; once resilience drops, every tower's crits land.
  - 💬
- [ ] ⭐ **Frostfire**: Seeker freezes a burning enemy -> the burn bursts (the new interaction row,
  150% with Thermal Shock).
  - 💬
- [ ] ⭐ **Freeze and smash**: Seeker's Brittle -> frozen enemies take +30% physical from Sniper and
  Mortar.
  - 💬
- [ ] ⭐ **Inferno pit**: Mortar's Tar + any burn (Cinder, Napalm) -> double pools.
  - 💬
- [ ] ⭐ **Kill box**: Pulse's Kill Zone + Undertow in a loop, Mortar and Sonar aimed into it.
  - 💬
- [ ] **Magnetic artillery**: Pulse's Magnetic Field -> Mortar shells never miss inside it.
  - 💬
- [ ] **Sympathetic sniping**: Splash's Hex of Sympathy -> one Sniper's Sunder and Marked Round's
  Vulnerable land on the whole hexed group.
  - 💬
- [ ] ⭐ **Magic chain**: Seeker's Unraveled and Sonar's Resonating -> Cinder, Pulse and Sonar's
  Pure Tone hit far harder.
  - 💬
- [ ] ⭐ **Physical chain**: Sniper's Sunder, Mortar's Cracked, Pulse's Corrosion -> every physical
  tower hits harder.
  - 💬
- [ ] **Plague**: Splash's Spreading Curse carries Scorched, the Brew's poison and every other
  debuff from each death on to the next enemies.
  - 💬
- [ ] ⭐ **Conductor**: Splash's Static Charge -> every Sniper, Mortar and Sonar hit on a Charged
  crowd becomes a lightning strike, crits at double.
  - 💬
- [ ] ⭐ **Frost coven**: Splash's Rime Coven + Seeker's freeze -> long freezes that detonate burns,
  then Sniper and Mortar smash what is frozen (Brittle).
  - 💬
- [ ] ⭐ **Ash coven**: Splash's Ash Coven + Cinder, Napalm or Tar -> double pools that no freeze
  puts out.
  - 💬
- [ ] ⭐ **Ping and patience**: Sonar's Ping Exposes the boss -> the Sniper's Steady Aim crit chance
  doubles on it (35% to 70% on Marksman's Eye).
  - 💬
- [ ] ⭐ **The bracket**: Pulse's Undertow holds a column inside the Mortar's bracket, under
  Kill Zone. Three towers, one spot.
  - 💬
- [ ] ⭐ **Long curses**: Pulse's Toll, Stasis and Soul Drain -> Splash's runes and every other
  debuff last far longer (spirit paces every debuff).
  - 💬
- [ ] **Charged volley**: Splash's Static Charge + the Sniper's Ricochet -> every bouncing crit
  discharges at double.
  - 💬
- [ ] **Kinship cluster**: one each of four tower types around an Aura -> +20% before any chain.
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
  - ↳ For every tower it clashes with "no player-chosen priority"; the bought version is Aura's
    Command (3.5).
- [ ] 🔴 **Transcendent actives**: a Transcendent tower gains one click-to-cast ability with a
  cooldown. Sniper *Assassinate* (a guaranteed crit on the selected enemy), Mortar *Barrage* (5
  shells at a clicked spot), Pulse *Overload* (x5 damage for 2 s), Seeker *Salvo* (empty the nest),
  Cinder *Firestorm* (ring of fire), Sonar *Ping* (reveal the whole map for 3 s), Splash *Storm*
  (Arc) or *Hex Nova* (Hex).
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
  - ↳ With XP (2.8) the stars can read XP instead of kills.
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
- [ ] ⭐ **Its content**: per tower in section 3 (Sniper's silver line, Splash's Blast Engineering,
  Frequency, Field Shaping, Tutelage, Ballistics, Mixed Payloads, Fuel).
  - 💬
- [ ] ⭐ **Second special: free or restricted?** Free among the tower's own specials, with no
  written pairings (Decisions). Splash picks from its chain's three.
  - 💬
- [ ] ⭐ **Transcendent price**: about 4x the tower's price.
  - 💬
- [ ] ⭐ **Range II and III**: +15% then +10%; Range III keeps needing Transcendent, as part of the
  spike.
  - 💬
- [ ] ⭐ **Pairing authorship**: none; specials combo through shared triggers (Decisions).
  - 💬
- [ ] ⭐ **Single-special towers**: get their second (and third) special in this iteration.
  - 💬
- [ ] ⭐ **Visibility until unlocked**: show Transcendent locked, with what's missing ("needs a
  special and a level 3 head"). It is the goal; hiding it hides the game's ceiling.
  - 💬
- [ ] ⭐ **Anything on top?** No performance gate. The two prerequisites and the price are enough.
  - 💬
- [ ] ⭐ **Rollout order and first proving tower**: Sniper first: its rework is designed and mostly
  🟢/🟡. Splash's rework is now the most new content (arcs, the hex framework), so it comes next,
  Arc chain before Hex. Then Pulse (most in need), then the rest.
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
7. [ ] **Splash's fork**: Fire Control and Saturation, then the Arc chain (chain targeting,
   Dazed), then the Hex chain on the hex framework.
   - 💬
8. [ ] **Cinder**: burning reveals, Thermal Shock (fire and ice as a combo), Soulfire.
   - 💬
9. [ ] **Aura**: no aura-on-aura, half disruption at base, Kinship, Tutelage on XP, Keen Edge,
   Command.
   - 💬
10. [ ] **Kill gates count assists** (decided), or XP as the one progress number.
    - 💬
11. [ ] **Fortify = the tower's signature passive** (the Sniper template), and the Jammer in real
    waves.
    - 💬
12. [ ] **Sniper as the first Transcendent tower**, then Splash.
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
  - ↳ Written before round 1: base crit is now 5%, with Steady Aim from Fortify.
- [ ] ⭐ **Splash** damage 16 -> 14, blast radius 1.75 -> 1.5, once it aims on purpose (Fire
  Control) and has a falloff floor (Wide Charge): it can afford less.
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
  - ↳ If XP replaces the gate types (2.8), this becomes XP tuning.
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
  - ↳ Resolved by your rework: II is flat penetration and fire rate; A III's Weak Spot handles
    plating.
- [ ] 50% armor penetration barely matters: +11% against an Armored grunt's 25 armor, about +25%
  against an Elite's 67, and the plating (the real wall) is untouched. Plating penetration instead.
  - 💬
  - ↳ Resolved: flat 30 (the table in Appendix A), plating through Weak Spot.
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
  - ↳ Resolved by the rework: it becomes Hex II's poison, at 4%.
- [ ] ⭐ Rapid Battery III (crits splash 50% bigger) triggers on 15% of shots: roughly +5-15% for
  $60, the weakest node in the game. Replace it: crits blast twice, or crits Daze.
  - 💬
  - ↳ Resolved: Rapid Battery retires; crits Daze on Arc III.
- [ ] ⭐ Overpressure does nothing without Rapid Battery II (Splash has no base crit). Arm it every
  8th shot instead, or give Splash 5% base crit.
  - 💬
  - ↳ Resolved: it becomes Thunderclap on the Arc chain, which grants crit at II.
- [ ] Concussive Blast costs a third of the tower's damage for a chill and 50% death explosions.
  -25% fire rate, or no penalty and 30% explosions.
  - 💬
  - ↳ Resolved: retired; its parts went to Arc III, Rime Coven and Reckoning.
- [ ] Blast Engineering I (radius only) is worth little while the target is random; with
  densest-group targeting it's fine.
  - 💬
  - ↳ Resolved: Wide Charge on the extra node, with Fire Control aiming.

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
  - ↳ With the fork the name can follow the chain (3.2: Stormcaller, Hexer).
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
  - ↳ Now near-certain: hexes are a Splash mechanic only.
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

---

## 10. XP: one progress number

**Status:** to review (round 2). Your idea from 2.8: a little XP for a shot, more for doing the
tower's job (a slow, a freeze, a crit), a lot for a kill; XP is never spent and only climbs. Agreed
after round 1; this chapter turns it into choices to tick, the same way as the rest.

**Why.** Today a node can gate on kills, damage dealt or adjacent towers, and 9.1 shows the first
two are badly matched: damage gates clear in one pass, kill gates take waves and starve support
towers. Assists (decided) and purpose gates (decided) fix that piecemeal; XP fixes it once, with
one number a player can read on every tower.

### 10.1 What other games do

As I remember them; the lessons are what matter.

- **Command & Conquer** (Red Alert 2, Tiberian Sun): units earn veterancy from kills, weighted by
  the value of what they destroyed. Ranks show as chevrons and bring stat bonuses; at the top, some
  units self-repair or change weapons. *Lesson: weight by the target's value; show the rank on the
  unit.*
- **Company of Heroes 2**: units earn experience from damage and kills and reach veterancy levels
  that grant per-unit perks rather than a flat percentage. *Lesson: a rank can change how a unit
  plays, not just its numbers.*
- **Dota 2 and Warcraft III**: when an enemy dies, every hero within a radius shares the
  experience, whoever landed the blow. *Lesson: XP by presence, so supports level too.*
- **Bloons TD 6**: the Paragon's degree is one power number fed by several kinds of contribution
  (pops, money spent, upgrades bought), and every tower panel counts its pops. *Lesson: blend
  contributions into one number, and show the count.*
- **Kingdom Rush**: heroes level from combat; towers don't, they are bought. *Lesson: XP where you
  care about one individual; jTD's towers are individuals once they carry trees.*
- **Infinitode 2**: towers gain experience while they fight and level up for small bonuses. *Lesson:
  tower XP works in a tower defense if it is visible and steady.*
- **XCOM**: a promotion is a choice between abilities. *Lesson: the rank-up is a moment; here the
  moment is a node becoming buyable.*

### 10.2 The core rule

- [ ] ⭐ 🟡 **XP replaces kill, damage and purpose gates.** A gated node reads "XP 300". The
  layout gates (neighbour, diversity) stay: they are about the board, not progress. Every node
  still costs money.
  - 💬
- ✅ **Never spent, never lost, only climbs** (your rule).
- [ ] ⭐ 🟢 **Shown as a bar** in the tower panel, with a tick at the next gate ("XP 120 / 300 to
  Hive"), and the total in the tower's info rows.
  - 💬
- [ ] ⭐ 🟢 **Selling loses it**, unless Sacrifice (6) passes it on.
  - 💬

### 10.3 Where XP comes from

- [ ] ⭐ 🟡 **Four sources**, as you sketched: a landed hit 1, a job event 3, an assist 5 (the
  decided assist: damage in the last 3 s, or a DoT on it), a kill 10. Misses earn nothing, so fire
  rate alone doesn't farm it.
  - 💬
- [ ] ⭐ 🟡 **Value weighting** (the C&C lesson): kill and assist XP scale with rank: Grunt x1,
  Soldier x1.5, Veteran x2, Elite x4, Boss x8. Killing the Warden is an event; killing mites is
  not.
  - 💬
- [ ] alt 🟡 Weight by the enemy's price instead of its rank: one number the levels already author.
  - 💬
- [ ] 🟡 **Presence XP** (the Dota lesson): every tower whose range covers a death gets 2, whoever
  killed it. Support towers climb by being where the fight is.
  - 💬
- [ ] ⭐ 🟡 **Per-tower hit scaling**, so tick-heavy towers don't flood: Pulse earns hit XP per second
  an enemy spends inside, not per tick; Cinder per wave, not per pulse; Splash per enemy caught,
  capped at 5 a blast.
  - 💬

**Job events** (3 XP each; each tower's list follows its purpose, 1.1)

| Tower | Job events |
|---|---|
| Sniper | a crit; reaching full Steady Aim; an execution |
| Splash | an arc jump; a rune cast; a curse passed on by Spreading Curse |
| Sonar | a reveal; an enemy Exposed by Ping; a Mark another tower spends |
| Pulse | a Toll stack; a silence (Null Field); a debuff slowed by Stasis |
| Aura | no own events: 20% of the XP its buffed towers earn (Tutelage raises it) |
| Mortar | each enemy beyond the first in one blast; a Cracked; a zone tick that lands |
| Seeker | a freeze; a shield stripped; a salvo of 3 or more |
| Cinder | an ignition; 5 Scorched stacks earned; a burning enemy revealed |

- [ ] ⭐ 🟡 This table, as the starting point for the harness to tune.
  - 💬

### 10.4 Gates on the XP scale

- [ ] ⭐ 🟢 **Thresholds per level, the same on every tower**, with the job weights tuned per tower so
  a tower doing its job reaches them around the same wave: head I 0 (price only, so Fortify -> I is
  never blocked), II 100, III 300, IV 800; extra node I 50, II 150, III 400, IV 1000; specials 300.
  - 💬
- [ ] alt 🟢 **XP levels** (1 to 10) instead of raw numbers: "needs level 4" reads simpler; the
  thresholds hide inside the levels.
  - 💬
- [ ] 🟢 **Transcendent needs XP too** (for example 1500), on top of a special and a head III. 7
  answered "no performance gate"; XP is the first gate that wouldn't feel like grinding.
  - 💬
- [ ] ⭐ 🟢 **The harness owns the weights**: for each built-in level, the wave at which each tower
  type, built early and doing its job, reaches head III. The target is a narrow band, not equal
  numbers.
  - 💬

### 10.5 Ranks you can see

- [ ] ⭐ 🟢 **Rank pips** on the tower at XP milestones (Recruit, Veteran, Elite, Hero), purely
  visual: the board shows which towers carried the level.
  - 💬
- [ ] 🟡 **Ranks with a small bonus** (6's veterancy stars): +3% damage per rank.
  - 💬
- [ ] 🟡 **A rank perk at Elite** (the Company of Heroes lesson), one per tower that changes play a
  little: Sniper's Steady Aim starts at 1 stack; Splash's Saturation caps one higher; Sonar's Ping
  lasts a pass longer; Pulse's Toll starts at 1; Mortar's Bracketing starts at 1 step; Seeker's nest
  +1; Cinder's Stoke starts at 1; Aura's Kinship counts itself.
  - 💬
- [ ] 🟢 **A rank-up moment**: a brief glow ring on the tower. No text on the board.
  - 💬

### 10.6 Catch-up and helpers

- [ ] ⭐ 🟡 **Aura's Tutelage** (3.5): +25% XP for buffed towers, shared lessons, a head start.
  - 💬
- [ ] 🟡 **Late-build boost**: a tower built after wave 5 starts with a quarter of the average XP of
  its type on the board.
  - 💬
- [ ] 🟡 **Sacrifice** (6): selling a tower passes half its XP to its neighbours.
  - 💬
- [ ] XP carried between levels: no. That is meta-progression, out of scope.
  - 💬

### 10.7 Cost and risks

- One counter per tower, raised from hooks that exist (`dealDamage`'s landed result, `onKill`,
  effect application) plus the assist record. One gate kind (`XP`) replaces three. 🟡
- Risks: towers that touch many enemies a second flooding (10.3 caps them); support towers
  starving (job events, presence XP, Aura's share); the panel growing a bar (one row, under the
  pips). All are harness and panel work, no new system.

---

## Appendix A. Round-1 record

Kept so the reasoning behind the settled spec stays traceable. Read the spec, not this.

### A.1 Sniper

The Sniper section as round 1 left it: your ticks and 💬 notes and my ↳ replies, each item
now marked by what the spec in 3.1 took from it.

**Why build it.** The cheapest tower and the only one born with crit. It deletes one important
enemy at a time: the Elite inside a Soldier wave, the Mender behind the wall, the Warden. Every
upgrade answers "how do I kill *that* one": crits, armor answers, magic ammo, executes.
**Buy it when** waves carry Elite or Boss ranks, or support enemies (Mender, Jammer, Elite Ghost)
must die first. **Weak against** swarms, crit-immune armor until upgraded, the invisible.
**Look by tier:** thin barrel; scope glint (Fortify); a faint laser line to its target (Awaken);
gold barrel and a white tracer (Transcendent); silver shots as a pale blue beam.

**Base line**

- ✅ ⭐ 🟢 Crit multiplier x2.0 (the only tower above x1.5).
  - 💬
- ✅ 🟢 Keep 15% base crit chance.
  - 💬 5%
- ✅ 🟡 **Steady Aim** (moved to Fortify): +10% crit chance while it keeps shooting the same target;
  resets on retarget. Rewards boss duels.
  - 💬 move to fortify base buff

**BASE**

- ❌ ⭐ 🟢 Fortify **Bipod** (replaced by Steady Aim): jam-proof, and the turret turns 50% faster.
  - 💬 I like steady aim as a fortify perk. Give 5% initial base crit and fortify +10% making it +15% on every but the first shot, as intended.
- ❌ 🟡 Fortify **Scope** (alternative): choose target priority (first, strongest, highest rank).
  - 💬 no priority choosing
- ✅ 🟢 Range II +15%, Range III +10%.
  - 💬
- ✅ 🟡 Range III **Overwatch**: +35% range, but it can't shoot within 2 cells (a dead zone).
  - 💬

**HEAD chain A - Focused Optics** (raw power). Exists: I +20% damage; II +20% damage, +25% fire
rate.
💬 rework I - steady aim grants also +25% fire rate; rework II - +40% damage, critical shot gives frenzy that increases firerate by 100% (for the time duration to shoo 3-4 times), dealing crit while in frenzy does not grant another frenzy
↳ Frenzy and Momentum's kill burst are both +100% fire rate. A timed self-buff never stacks
today (a new one replaces the running one), so they can't multiply to x4; I'd keep that.

- ✅ ⭐ 🟢 III **Heavy Caliber** (rewritten as Weak Spot): +30% damage, ignores 50% of plating. Answers
  plated elites without touching crit.
  - 💬 +30% damage, non-crit attacks ignores enemy plating and a lot of armor (rename it to Weak Spot)
  - ↳ Read with your B III note this is ambiguous: does the non-crit clause live here, on B III,
    or both? The doc keeps it here as you wrote it. "A lot of armor": 50, since it only applies
    to non-crits (B II's 30 applies to every shot).
- ✅ ⭐ 🟢 IV-A **Railgun**: the shot pierces every enemy on the line from the tower through the
  target to max range; each one after the first takes 25% less. Build it along a straight.
  - 💬 I would like the railgun to travel more than the max range. Max range is for targeting, railgun reaches 1 or 2 cells further
- ✅ ⭐ 🟡 IV-B **Executioner**: a non-boss enemy left below 15% health by a shot dies; Boss rank
  takes +50% while below 25%. Executions count as crits for triggers.
  - 💬
- ❌ 🟡 IV-B alternative **Overkill**: damage beyond a kill carries into the nearest enemy, once.
  - 💬 

**HEAD chain B - Marksman's Eye** (crit). Exists: I +15% crit; II +20% crit, 50% armor
penetration.
💬 rework - lvI Steady Aim stacks up to 3 times, lvII armor penetration and +25% fire rate
💬 I do not like % armor penetration, I prefer a number, like 20 (find out which number would fit here based on how armor scales)
↳ Armor keeps `100 / (100 + armor)` of a hit. The Armored ladder is 25, 33, 43, 67 and 100
armor (Grunt to Boss). Damage gained:

| Penetration | Grunt 25 | Soldier 33 | Veteran 43 | Elite 67 | Boss 100 |
|---|---|---|---|---|---|
| flat 20 | +19% | +18% | +16% | +14% | +11% |
| **flat 30** | +25% (to 0) | +29% | +27% | +22% | +18% |
| today's 50% | +11% | +14% | +18% | +25% | +33% |

Flat helps most against light armor, percent against heavy. **30** nearly matches today's 50%
where the Sniper works (the Elite), and with a full Sundered (-50) an Elite has no armor left.

- ✅ ⭐ 🟡 III **Weak Spot** (moved to the extra node as the crit streak): +50% crit damage (x2.0 ->
  x2.5).
  - 💬 this effect will be in extra head slot, add here "non crit attacks"
  - ↳ If you meant "move the non-crit clause here", A III needs a new verb; if not, B III is
    empty. My pick for B III: **Shatter Shot**, widened: crits on a frozen or Dazed enemy deal
    +50% crit damage. The crit chain gets a combo verb with Seeker's freeze and Splash's arcs.
- ❌ ⭐ 🟡 IV-A **Deadeye**: ignores 50% of resilience, so crit-immune enemies can be crit for
  half the bonus. Crit-immune Armored mobs stop being a Sniper's hard counter.
  - 💬
  - ↳ Unticked, and it would break "crit immunity holds". A replacement in your Steady Aim
    language: **Unbroken Aim**: Steady Aim stacks to 5 and survives a kill (only retargeting a
    living enemy resets it). The selfish crit engine, against IV-B's Sunder for the team.
- ✅ ⭐ 🟡 IV-B **Sunder Rounds** (your shattered armor): every crit applies 1 Sundered stack
  (-5 armor each, 10 max, per 2.2). The team option: every physical tower hits that enemy harder.
  - 💬

**Extra head node - Silver Ammunition** (your idea; the damage-type line)
↳ Only level III is silver now. A name for the whole line: **Tradecraft** (the assassin's
technique, range, ammunition and intel). I and II are damage, which bends 2.9's "extra node:
never raw damage"; fine if their prices carry it.

- ✅ ⭐ 🟡 I: new effect
  - 💬 critical strike makes next shot crit deal 25% more crit damage (x2.0 -> x2.25), if 2 crit in a row it goes to +50% crit damage (x2.0 -> x2.5), does not stack any further
- ✅ ⭐ 🟡 II: new effect
  - 💬 +25% damage against targets past two thirds of its range
- ✅ 🟡 III (rewritten by your 💬): silver shots reveal their target for 2 s and ignore shields.
  - 💬 every 3rd shot is magic, with +25% magic penetration, magic silver bullet does not consume previous crit stack but can profit from it
- ✅ 🟡 IV (rewritten by your 💬): every 2nd shot is magic; silver shots apply 1 Unraveled.
  - 💬 may shoot any enemy another tower has Marked or Revealed within twice its range.

Alternative extra node - **Rangefinder**:

- ❌ 🟡 I +10% range; II target-priority choice; III +20% damage against targets past two thirds
  of its range; IV **Spotter Uplink**: may shoot any enemy another tower has Marked or Revealed
  within twice its range.
  - 💬 no, see above

**Specials** (any of them switches targeting to most health, as today)
↳ Five specials, but the panel offers at most three nodes per slot. Options: widen the panel;
cut to three; or Splash's rule, specials follow the chain (A: Momentum, Ricochet, Headhunter;
B: Marked Round, Fifth Shot and a third crit special). My pick is the last: no UI work, and the
fork matters more.

- Marked Round (exists): crits apply Vulnerable.
- Fifth Shot (exists): every 5th shot a guaranteed crit, crits deal 250%.
  💬 but should not crit if the enemy has crit immunity and this tower does not lower resilience
  ↳ That is already the code: a forced crit still rolls against resilience, so it never lands on
  a crit-immune enemy.
- ✅ ⭐ 🟢 Fifth Shot tweak: "+50% crit damage" instead of "crits deal 250%", so it stacks with the
  x2.0 base, Weak Spot and Aura rather than overwriting them.
  - 💬
- Momentum (exists): the shot after a crit deals 500% and ignores armor and plating; a kill by this shot grants
  +100% fire rate for 5 s.
- ✅ ⭐ 🟡 **Ricochet**: a crit bounces to the nearest other enemy within 1.5 cells for 60%; the
  bounce can crit again, up to 3 bounces. Sniper's only swarm answer, still crit-driven.
  - 💬
- ✅ ⭐ 🟡 **Headhunter**: highest rank first; +40% damage against Elite and Boss; Elite and Boss
  kills pay double bounty.
  - 💬
  - ↳ The bounty clause is dropped (no bounty modifiers).
- ❌ 🟡 **Cold Bore**: +40% damage per second without firing, up to +200%. The first shot at a
  boss walking into range is enormous; build it far down the path.
  - 💬
- ❌ 🟡 **Spotter's Link**: may fire at any enemy another tower has Marked or Revealed, within
  twice its range; those come first.
  - 💬

**Pairings** (both specials owned, after Transcendent)
💬 no custom pairings, having both specials already pairs well
↳ Followed: none of the pairings below is planned. The specials all feed on crits, so they
combo anyway.

- ❌ ⭐ Fifth Shot + Momentum -> **Rhythm**: the 5th shot's guaranteed crit charges Momentum, and
  the charged shot always crits too. Every 6th shot is x5 and critical. The boss killer.
  - 💬
- ❌ Fifth Shot + Marked Round: the guaranteed crit applies all 3 Vulnerable stacks at once.
  - 💬
- ❌ Marked Round + Momentum: the x5 shot spends the target's Vulnerable stacks for +30% each,
  then applies 1 again.
  - 💬
- ❌ ⭐ Momentum + Ricochet: a Momentum shot ricochets at full x5; ricochet kills also grant the
  fire-rate burst.
  - 💬
- ❌ ⭐ Ricochet + Marked Round: every bounce applies Vulnerable. The Sniper spreads Vulnerable to
  a group.
  - 💬
- ❌ Headhunter + Fifth Shot: against Elite and Boss, every 3rd shot is the guaranteed crit.
  - 💬
- ❌ Headhunter + Cold Bore: the charge is spent only on Elite and Boss; lesser shots leave it
  building.
  - 💬
- ❌ Cold Bore + Momentum: a kill refunds half the charge.
  - 💬
- ❌ Spotter's Link + Marked Round: crits on a spotted target apply 2 stacks.
  - 💬

**More ideas**

- ❌ 🟡 **Double Tap**: 15% chance to fire a second shot at once.
  - 💬
- ❌ 🟡 **Tracer**: every hit reveals its target for 1 s. A Ghost vanishes on the first hit; the
  tracer lets the second shot land.
  - 💬
- ✅ 🟡 **Shatter Shot**: crits on a frozen target deal +50% crit damage (the Seeker combo).
  - 💬 I like this one, add it where it fits
  - ↳ My home for it: Marksman's Eye III, widened to Dazed targets too.
- ❌ 🟢 **Bounty Hunter**: a crit kill pays +1 credit.
  - 💬
- ✅ 🟡 **Quick Scope**: the first shot at a new target has +50% crit chance.
  - 💬 I like this one, add it where it fits
  - ↳ My home for it: Focused Optics I, beside Steady Aim. Steady Aim covers every shot but the
    first, Quick Scope covers the first. In the tempo chain it feeds Frenzy: a kill, a new
    target, a likely crit, Frenzy again.
- ❌ 🟡 **Two in the Chamber**: holds one extra shot during idle time, fired instantly on the next
  target (a one-round magazine).
  - 💬
