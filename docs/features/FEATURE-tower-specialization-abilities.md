# Feature Request: Tower Specialization Abilities

*Formerly "Vulnerability Status Effect" — renamed because its scope grew from one status effect
into the full concrete content for every tower's `head`/`special` upgrade nodes, plus every new
combat primitive that content needs and the engine doesn't have yet. The Vulnerability effect is
still here — it's this document's largest and first primitive, not its only one anymore.*

## Summary

`docs/features/FEATURE-tower-upgrade-trees.md` proposes the three-slot (`base`/`head`/`special`)
upgrade *system* — nodes, slots, gating, prerequisites — but deliberately doesn't design any
tower's actual node content. This document is that content: a full `base`/`head`/`special`
catalogue for all 9 towers, worked out collaboratively and recorded here so it survives past the
conversation that drafted it. Building that content surfaced a repeating problem: most of the
interesting `special` nodes, and several `head` nodes, describe behavior the engine has no
primitive for — a damage-amplifying status effect, a partial armor bypass, a guaranteed-crit
trigger, an on-kill secondary effect, and others. This document's second job is to define and
implement those primitives. `FEATURE-tower-upgrade-trees.md` builds the node objects; this
document supplies both the concrete numbers those objects carry *and* the real behavior behind
whichever ones need something new.

## Current state (what exists today)

- `td.effect` ships exactly six kinds today (`EffectKind.java:6-13`): `SLOW`, `BURN`, `FREEZE`,
  `SHIELD`, `INVISIBLE`, `HEAL`. Nothing increases incoming damage; `SHIELD`
  (`ShieldTemplate.java`, `ActiveEffects.applyShield`, `ActiveEffects.java:190-198`) is the
  closest existing analogue to Vulnerability and is structurally its mirror image: it scales
  incoming damage *down* by a percentage while active, and can be restricted to one
  `td.damage.DamageType` via `Optional<DamageType> restrictedTo` (`ShieldTemplate.java:14`,
  `physicalOnly`/`magicOnly`).
- `ActiveEffects` holds at most one live `Effect` per `EffectKind`, and already has two
  established exceptions to its default "keep the stronger, extend the duration" reapply rule
  (`strongerOf`, `ActiveEffects.java:45-48`): `SLOW`'s bounded single-superseded-application
  stack (`applySlow`, `ActiveEffects.java:110-122`) and `BURN`'s additive, decaying fuel pool
  credited per contributing tower (`applyBurn`, `ActiveEffects.java:137-150`, via
  `BurnContribution`). "Stacks up to 3 times on the enemy, not per tower" — Vulnerability's own
  shape — is a *third* distinct reapply shape; none of the existing three fit it directly.
- Every hit already funnels through one place enemy-side: `AbstractEnemyMob.doDamage`
  (`AbstractEnemyMob.java:223-229`) computes
  `landed = activeEffects.applyShield(absorb(damage)).cappedAt(health)`. Whatever amplifies a hit
  for Vulnerability, or bypasses part of `absorb`/`applyShield` for the armor-piercing nodes
  below, is a sibling step in this same pipeline, not a new one.
- **There is direct precedent for a status effect universally affecting every tower's damage
  math, not just the tower that applied it.** `AbstractTower.rollCritical`
  (`AbstractTower.java:261-267`) checks `enemy.activeEffectKinds().contains(EffectKind.BURN)` and
  doubles *whichever tower is currently rolling*'s crit chance — regardless of which tower's
  Cinder actually applied the burn. `td/tower/CLAUDE.md`'s "A burning target doubles the
  effective crit chance for every tower" section documents this as deliberate, universal-by-design
  behavior. Vulnerability is the same shape of thing — a status the *enemy* carries, read by every
  tower's damage math — not a per-applicator tracked bonus.
- **`Damage.asCritical()` applies one fixed, project-wide crit multiplier** (`td/tower/CLAUDE.md`,
  "Aura buff stacking" section) — there is no per-shot or per-node override today. Several nodes
  below (Sniper's Fifth Shot and Momentum, Sonar's Mark on Sweep) describe their own multiplier
  (250%, 500%) or their own guaranteed-crit trigger; neither capability exists yet. See "New
  primitives," #3.
- `docs/features/FEATURE-tower-upgrade-trees.md` is itself still **proposed, not yet built**.
  Its "`head`/`special` are exclusive, pick-one-path-forever slots" decision, and its `base` slot
  allowing both of its own nodes to be bought rather than being exclusive, is what every gate and
  exclusivity assumption below is built against.

## Concrete specialization content

Every tower's `base` slot is the same shape, stated once rather than per tower (see
`FEATURE-tower-upgrade-trees.md` for the slot system this content plugs into): a **range** node
and an **Awaken** node that unlocks the `head`/`special` slots for purchase. Both are buyable —
`base` is the one slot that isn't exclusive — and both are money-gated (`always()`); Awaken is
deliberately gated on price alone rather than a performance condition, since a tower "grows into"
its slots by investment, and the slots' own nodes are where combat performance is asked for.

Each node below is tagged **[F]** (full logic, buildable directly by
`FEATURE-tower-upgrade-trees.md` using a primitive that exists today — a `TowerBuff` axis, the
existing per-tower `onUpgradePathChosen`-style field-bump hook, an existing `Effect` kind reused
as-is, or a straightforward composition of the existing `td.tower.targeting` pieces) or **[S]**
(stub — the node itself is real: an id, display name, price, gate and description exist, and it
is selectable and shows in the UI — but its behavioral hook is a documented no-op until this
document's own new primitive, listed under "New primitives" below, lands). A stub is not a
half-finished feature left melting in the tree; it is a deliberate two-document build order, and
each stub's no-op hook gets a `TODO.md` entry per root `CLAUDE.md`'s "no inline TODO" convention,
closed in the same commit that wires the real behavior in.

### Sniper

**head**
- *Focused Optics*: +20% damage (`always()`) → lv2 +20% damage, +25% fire rate (`always()`) — both **[F]**
- *Marksman's Eye*: +15% crit (`KillCountCondition(15)`) → lv2 +20% crit **[F]**, ignores 50% of
  armor/`PercentResistTrait`/`FlatResistTrait`/shield **[S]** (`DamageDealtCondition(20000)`)

**special** — choosing any node in this slot also switches the tower's targeting from "furthest
along the path" to **the highest-current-health enemy in range** — **[F]**, a new
`HighestHealthSelector` composed the same way `FurthestAlongPathSelector`/`NearestSelector`
already are, no new domain primitive needed. Deliberate, not incidental: a Sniper that's earned
this slot shouldn't spend its shots finishing off enemies that are already nearly dead — it
should apply pressure to whichever enemy still has the most health to lose, both to avoid
last-hitting (which wastes the shot the rest of the wave needed) and to put damage where it's
most needed.
- *Marked Round*: crits apply Vulnerable, +15% damage taken, stacks ×3 on the enemy **[S]**
  (`KillCountCondition(10)`)
- *Fifth Shot*: every 5th shot is a guaranteed crit; crits from this tower deal 250% **[S]**
  (`KillCountCondition(15)`)
- *Momentum*: on crit, the next shot deals 500% damage and fully ignores armor/
  `PercentResistTrait`/`FlatResistTrait`/shield — a complete bypass, not a percentage discount;
  on kill, +100% fire rate for 5s, does not stack **[S]** (`KillCountCondition(20)`)

### Splash

**head**
- *Blast Engineering*: +splash radius (`DamageDealtCondition(10000)`) **[F]** → lv2 +damage,
  flattens the falloff curve (`DamageDealtCondition(20000)`) **[F]** → lv3 fires 3 projectiles
  instead of 1 (`KillCountCondition(20)`) **[S]**
- *Rapid Battery*: +fire rate (`KillCountCondition(8)`) **[F]** → lv2 +damage, +15% crit
  (`KillCountCondition(18)`) **[F]** → lv3 crits splash 50% bigger (`DamageDealtCondition(25000)`)
  **[S]**

**special**
- *Toxic Bloom*: splash applies a toxic DoT — its own `EffectKind`, a different decay curve from
  burn **[S]** (`DamageDealtCondition(15000)`)
- *Concussive Blast*: -50% fire rate **[F]**, blast applies slow **[F]**, killed enemies explode
  **[S]** (`KillCountCondition(15)`)
- *Overpressure*: on crit, the next shot fires at every enemy in range **[S]**
  (`KillCountCondition(25)`)

### Sonar

**head**
- *Twin Array*: +damage (`DamageDealtCondition(10000)`) **[F]** → lv2 +damage, +crit
  (`DamageDealtCondition(20000)`) **[F]** → lv3 a second turret, facing the opposite direction
  (`KillCountCondition(25)`) **[S]**
- *Long Reach*: +15% crit (`KillCountCondition(10)`) **[F]** → lv2 damage scales up to +100% at
  max range (`DamageDealtCondition(20000)`) **[S]**

**special**
- *Wide Band*: each revolution briefly reveals invisible enemies to every tower **[S]**
  (`ClusterCondition(2)`)
- *Mark on Sweep*: a beam hit marks its target; the next hit on that enemy is a guaranteed crit
  **[S]** (`KillCountCondition(15)`)
- *Piercing Tone*: bonus magic damage against physically armored/shielded enemies, scaling with
  how much resistance they carry, up to a cap **[S]** (`DamageDealtCondition(20000)`)

### Pulse

**head**
- *Overcharged Coils*: +30% damage (`DamageDealtCondition(10000)`) **[F]** → lv2 +25% more
  damage, +10% flat crit chance (`KillCountCondition(20)`) **[F]**
- *Resonant Field*: +20% range, and the tower now damages invisible enemies every tick regardless
  of whether a non-ghost is also present to trigger it (`KillCountCondition(10)`) **[F]** — a
  straightforward removal of `PulseTower.doTick`'s existing "needs a cover target" condition, no
  new primitive → lv2 +15% more range, and any invisible enemy it hits is revealed to every tower
  for 2s (`DamageDealtCondition(20000)`) **[S]**

**special** — *Warding Field*: each tick, everything currently being hit has a 10% chance to gain
1 Vulnerable stack (cap 3) **[S]** (`KillCountCondition(20)`) — a chance-per-tick roll rather than
a guaranteed one, since Pulse ticks every frame with no cooldown; a guaranteed proc would hit max
stacks on anything that lingers almost instantly.

### Mortar

**head**
- *Siege Rounds*: +30% damage (`DamageDealtCondition(15000)`) **[F]** → lv2 +25% more damage,
  +40% splash radius (`DamageDealtCondition(30000)`) **[F]**
- *Fragmentation Rounds*: shrapnel deals 25% weapon damage in a wider ring past the main splash
  (`KillCountCondition(12)`) **[S]** → lv2 shrapnel also applies the tower's slow, at half
  duration (`ClusterCondition(2)`) **[F]** once lv1 exists

**special** — *Cursed Shrapnel*: every enemy caught in the blast gets a guaranteed Vulnerable
stack (cap 3), refreshed on every hit **[S]** (`KillCountCondition(20)`)

### Seeker

**head**
- *Twin Warhead*: +30% fire rate (`KillCountCondition(10)`) **[F]** → lv2 +25% more fire rate,
  fires two independently-retargeting missiles instead of one (`DamageDealtCondition(25000)`)
  **[F]** — `MissileProjectile` already retargets independently per instance, so firing two is
  reusing the existing projectile class twice, not a new primitive
- *Deep Freeze*: +30% damage (`KillCountCondition(12)`) **[F]** → lv2 +25% more damage, +75%
  freeze duration **[F]**, and killing a frozen enemy shatters it for 50% weapon damage splash to
  nearby enemies **[S]** (`KillCountCondition(25)`)

**special** — *Homing Curse*: impact applies 1 Vulnerable stack, or 2 if the target was already
frozen or slowed **[S]** (`KillCountCondition(20)`) — the frozen/slowed check itself is **[F]**,
reusing the same `activeEffectKinds()` query the burn/crit precedent already established.

### Cinder

**head**
- *White Flame*: +30% damage (`DamageDealtCondition(15000)`) **[F]** → lv2 +25% more damage,
  +50% burn duration (`DamageDealtCondition(30000)`) **[F]**
- *Wide Nozzle*: +25% range, +30% cone width (`KillCountCondition(10)`) **[F]** → lv2 +20% more
  range/width, -20% cooldown (`DamageDealtCondition(25000)`) **[F]** — the cooldown cut is the
  existing fire-rate `TowerBuff` axis, needing no new hook at all

**special** — *Hexflame*: each wave that newly ignites an enemy also grants 1 Vulnerable stack
(cap 3) **[S]** (`KillCountCondition(20)`) — stacks with the existing project-wide "burning
doubles crit chance for every tower" rule, so a fully-loaded Cinder target is both easier to crit
and takes bonus damage from everything shooting it.

### Aura

**head**
- *Amplifying Core*: buff strength +50% (`ClusterCondition(2)`) **[F]** → lv2 +50% more strength,
  and the aura now also grants a fire-rate bonus, not just damage/range (`ClusterCondition(3)`)
  **[F]** — `TowerBuff` already has a `fireRateBonus` axis; `AuraTower.buff()` just needs to set it
- *Resonance Field*: +30% range (`ClusterCondition(2)`) **[F]** → lv2 +25% more range, and the
  aura no longer refuses to buff other Aura towers (`ClusterCondition(3)`) **[F]** — a one-line
  change to `AuraTower.buffs()`'s existing predicate, not a new primitive

**special** — *Withering Field*: every few ticks, every enemy currently standing inside the
aura's range gains 1 Vulnerable stack (cap 3), decaying if they leave range **[S]**
(`ClusterCondition(2)`) — the mirror of Aura's own ally buff: allies inside get stronger, enemies
inside get weaker. The periodic-tick plumbing itself is **[F]**; `AuraTower.doTick` is empty
today and trivially gains a counter.

## New primitives this document must define

Eleven consumers above are tagged **[S]**. They share exactly these missing primitives:

1. **`EffectKind.VULNERABLE`** — this document's original, and largest, scope. Six consumers now,
   not one: Marked Round (Sniper), Warding Field (Pulse), Cursed Shrapnel (Mortar), Homing Curse
   (Seeker), Hexflame (Cinder), Withering Field (Aura). Six independent nodes needing the same
   answer raises the stakes on resolving this document's own open questions below (stack shape,
   magnitude, composition order) — they're no longer one tower's problem.
2. **Partial or full resistance-and-shield bypass** — Marksman's Eye lv2 (50% ignore) and
   Momentum (100% ignore). Needs a way to attenuate or skip part of
   `AbstractEnemyMob.doDamage`'s `absorb`/`applyShield` steps per hit; armor today is either
   fully applied or not applied at all.
3. **Guaranteed-crit trigger, with a per-node crit multiplier** — Fifth Shot (every-5th-shot
   counter, 250%), Momentum (crit-then-boosted-next-shot), Sonar's Mark on Sweep
   (mark-then-guaranteed-crit). Conflicts with `Damage.asCritical()`'s documented one fixed,
   project-wide multiplier — see Open questions, #7.
4. **A timed, non-stacking self-buff pulse triggered by a kill** — Momentum's "+100% fire rate
   for 5s, does not stack." Upgrade-node buffs today are permanent; this one has its own expiry,
   layered temporarily on top.
5. **An on-kill secondary trigger, aware of the kill's own status effects** — Deep Freeze lv2
   (shatter-on-kill splash, only if frozen) and Splash's Concussive Blast (kill explodes). No
   generic "this hit was a kill, and the target had status X" hook exists today.
6. **"Reveal an invisible enemy to every tower"** — Pulse's Resonant Field lv2 and Sonar's Wide
   Band. Distinct from Pulse's existing "already hit because something else triggered it"
   behavior — this is an active reveal other towers can then target off of.
7. **Crit-triggered behavior override, beyond bonus damage** — Splash's Overpressure (fire at
   everyone in range instead of the one target) and Rapid Battery lv3 (bigger splash on crit).
8. **Distance-scaling damage** — Sonar's Long Reach lv2.
9. **A second, independently-aimed turret head** — Sonar's Twin Array lv3. Primarily a
   render/aim concern (a second `TurretAim` instance, a second `TurretHeadDraw`), not damage math.
10. **A toxic-DoT `EffectKind` with its own decay curve, distinct from burn** — Splash's Toxic
    Bloom.
11. **Resistance-aware damage scaling** — Sonar's Piercing Tone (reads the target's own
    resistance and scales bonus damage against it).

## Interconnections

- **Two-way dependency with `FEATURE-tower-upgrade-trees.md`.** This document depends on that
  one for the slot/node/graph system that gives every node above an id, a slot, gating and a
  place in the UI — none of the content above can be built until that system exists. That
  document depends on this one for two things in return: (a) the concrete node content itself —
  every `base`/`head`/`special` node across all 9 towers, with names, numbers and gates, is
  authored here rather than invented at that feature's own planning time; and (b) the actual
  behavioral logic behind every **[S]** node above — that feature's own scope stops at building a
  real, selectable, priced, gated node object whose hook is a documented no-op; wiring the no-op
  into real behavior is this document's job, via the 11 primitives above.
- Extends `td/tower/CLAUDE.md`'s existing "a status effect universally affects every tower"
  precedent (the burn/crit-chance interaction) to a second axis — damage amplification — rather
  than introducing a new design principle to the codebase.
- Directly exercises `td/effect/CLAUDE.md`'s "Adding a new effect kind" checklist for both
  `VULNERABLE` and the toxic-DoT kind (`EffectKind`, `Effect`'s static factory, the non-`default`
  `ActiveEffects.magnitude` switch, a new `Palette` role) — that checklist exists specifically
  because additions like these are expected.

## Constraints and open risks

- **None of `ActiveEffects`'s three existing reapply shapes fit "stacks up to 3, shared across
  applicators" as-is.** The default (`strongerOf` — keep the stronger, extend the duration)
  collapses multiple applications into one; `SLOW`'s superseded-application mechanism is bounded
  at exactly one background application and is speed-specific; `BURN`'s fuel pool is a
  continuous decaying scalar, not discrete counted stacks. A fourth combine shape is a real
  design question for planning, not resolved here — and now six consumers depend on the answer.
- **`ActiveEffects.magnitude` has no `default` case** — any new `EffectKind` (`VULNERABLE`, and
  the toxic-DoT kind) is a compile error until this switch grows one, the deliberate,
  compiler-enforced half of `td/effect/CLAUDE.md`'s checklist. What "magnitude" means for a
  stacked effect (stack count? summed percentage? something else?) needs an answer before either
  case can be written.
- **Composition order with `SHIELD` and `Trait` resistance is unresolved**, and now interacts
  with the armor-bypass primitive too. `AbstractEnemyMob.doDamage` computes
  `applyShield(absorb(damage))` — trait resistance first, then the timed shield. Whether
  Vulnerability's amplification happens before `absorb` or after everything else changes the
  resulting numbers substantially; whether Marksman's Eye/Momentum's bypass skips `absorb`,
  `applyShield`, or both is the same open question from the other direction.
- **`Damage.asCritical()`'s one fixed, project-wide multiplier doesn't accommodate a per-node
  override.** See Open questions, #7 — a real design fork, not a detail.
- **`td.effect` is deliberately unaware of `td.tower`/`td.enemy`** (root `CLAUDE.md` §4,
  `td/effect/CLAUDE.md`'s own framing) — both new effect kinds must stay generic timed
  primitives with no knowledge of which tower or node applies them, exactly like every existing
  kind.
- Root `CLAUDE.md` §5's value-type rules (`no-static-random`, `wide-values-have-a-narrow-entry-point`,
  etc.) apply to whatever new type(s) this needs — `Effect.vulnerable(...)`/`Effect.toxic(...)`
  factories, and possibly template types alongside `ShieldTemplate`/`InvisibleTemplate`/
  `HealTemplate` mirroring the sealed `EffectTemplate` shape `td/effect/CLAUDE.md` documents.

## Decisions made

1. **This is a generic engine primitive in `td.effect`, not a tower-specific mechanic.** Any
   tower's upgrade node may apply Vulnerability once it exists.
2. **Vulnerability stacks belong to the enemy, capped at 3, shared across every tower that
   applies them** — matching `BURN`'s already-established "the pool belongs to the mob, not to
   one applicator" precedent, not a per-tower-tracked count.
3. **No new UI primitive for Vulnerability.** It rides the existing per-`EffectKind`
   status-marker convention rather than adding a new one.
4. **Sniper's `special` slot retargets to the highest-current-health enemy in range**, not the
   lowest-health nor the default furthest-along-the-path choice. Deliberate: it prevents the
   tower from spending its (now more expensive, gated) shots last-hitting enemies that are
   already nearly dead, and instead puts damage where an enemy still has the most health left to
   lose.
5. **Fifth Shot's guaranteed crit deals 250%, not 300%** — lowered from the original draft.
6. **Momentum's post-crit shot fully bypasses armor/`PercentResistTrait`/`FlatResistTrait`/
   shield**, not a percentage discount — a complete ignore, matching its escalated 500% damage.

## Open questions

1. What does a 4th (or later) Vulnerability application do once 3 stacks are already active —
   refresh the weakest stack's remaining duration, get dropped entirely, or something else?
2. Does each Vulnerable stack contribute a fixed amplification (flat +15% per stack, +45% at the
   cap) set once by this document, or does each granting node set its own per-stack potency —
   now a live question across six different consumers, some of which (Warding Field's
   chance-per-tick, Cursed Shrapnel's guaranteed-and-refreshed) already read as meaningfully
   different in aggressiveness even if their per-stack number matched?
3. Composition order with `SHIELD`/`Trait` resistance — before `absorb`, after `applyShield`, or
   interleaved with one of them? Also now the open question for where Marksman's Eye/Momentum's
   bypass cuts in.
4. Should Vulnerability be restrictable to one `DamageType`, the way `ShieldTemplate`/
   `PercentResistTrait` already are? None of the drafted nodes ask for a typed restriction, but
   the precedent exists on both the resistance side and the shield side.
5. Does each Vulnerable stack track its own independent remaining duration, or does any
   reapplication — even one that adds a new stack rather than refreshing an existing one —
   extend every current stack's duration to the longest?
6. Should the 11 new primitives be implemented incrementally, only as whichever tower's content
   is actually queued for a given implementation pass needs them, or built out fully as this
   document's own single pass once it's picked up? Not resolved here.
7. **`Damage.asCritical()`'s fixed, project-wide crit multiplier conflicts with Fifth Shot's
   250%, Momentum's 500%, and Sonar's guaranteed-crit-on-mark.** Does this feature revise that
   constant into a per-node override, or do these nodes instead reuse the existing fixed
   multiplier (making "250%"/"500%" descriptive flavor text to revise once the real number is
   known, rather than literal targets)? A real design fork, not resolved here.

---

*After planning and implementation, update this document rather than deleting it (see the
root `CLAUDE.md`'s documentation map): mark it implemented, prune resolved open questions, and
either promote deferred scope to a new request or note it's still wanted for a later version.*
