# Feature Request: Effect Interactions and Effect Categories

**Status: draft, brainstorming.** Nothing here is decided except where "Decisions made" says so.
Written from a design discussion; the user will keep iterating on it.

## Summary

Status effects today are independent of each other: each kind does its own thing and nothing
reacts to another effect being present. This request explores giving effects a shared vocabulary
(categories) and a small set of interactions between them (fire vs frost, hard crowd control),
adds the missing effect kinds (vulnerable, stun, toxic), and replaces burn's crit-chance doubling,
which is too strong. The aim is a game whose combinations are learnable by the player and cheap
to extend.

## Current state (what exists today)

- Six kinds: slow, burn, freeze, shield, invisible, heal (`td.effect.EffectKind`). See
  `td/effect/CLAUDE.md`.
- One live effect per kind; reapplying refreshes. Exceptions: slow keeps one superseded
  application ticking in the background, burn is a decaying fuel pool with one contribution per
  application.
- Effects act on an enemy only through stat modifiers. No effect reads or reacts to another
  effect.
- Burning doubles the crit chance the enemy takes, from any tower (`FEATURE-critical-damage.md`
  Addendum). It is a tuned-by-guess placeholder and is judged too strong: it multiplies every
  tower's crit chance, so it scales with everything.
- Freeze is the only kind with diminishing returns. Resistances shorten durations only.
- Heal already exists as a linear per-tick regeneration for its duration.
- Vocabulary in the code today: a *trait* is innate and permanent (part of an enemy definition),
  an *ability* is a trigger plus an authored effect, an *effect* is a timed state applied at
  runtime. Buff/debuff (polarity) is not modelled.
- Related gaps already in `TODO.md`: `EffectKind.VULNERABLE`, a toxic DoT distinct from burn,
  acid, a typed shield with no user, the effect-marker overflow with no count, and several
  placeholder-number entries.

## What this feature adds

Candidate scope, to be cut down by the requester:

- **Effect categories** that players can learn: hard crowd control (freeze, stun), soft crowd
  control (slow), damage over time (burn, toxic), stat debuff (vulnerable), restorative (heal,
  shield). Marker colour and inspector text follow the category.
- **New kinds:** vulnerable (stacking damage amplifier, cap 3; already wanted by six upgrade
  nodes), stun, toxic/curse DoT that lowers spirit.
- **Fire vs frost interactions:** a frozen enemy is immune to burn, and freezing an enemy removes
  all its burn. Burn and slow interact in one direction (see open questions).
- **A new role for burn** in place of the crit doubling.
- **Visible interactions:** when an interaction blocks or removes something, the inspector says
  so.

Out of scope for a first version: acid, cleansing/dispel, buff-vs-debuff polarity rules.

## Interconnections

- `FEATURE-tower-specialization-abilities.md`: vulnerable and toxic are primitives it names;
  Deep Freeze II (shatter on a frozen kill) would gain meaning if freeze and stun stay distinct.
- `FEATURE-effect-diminishing-returns.md`: stun and freeze plausibly share one hard-CC group.
- `FEATURE-critical-damage.md`: owns the burn crit doubling this request replaces.
- `FEATURE-enemy-traits-and-effects.md` and the Warden chain: the natural test bed for anti-heal
  and armor-melt, and for stun interrupting abilities.

## Constraints and open risks

- **Balance load grows with combinations.** Every number in the tower, enemy and crit tables is
  still a placeholder. More interactions multiply what needs tuning; keep the rule set small.
- **Opposition rules pull against mixed defences.** Fire-vs-frost rules punish combining towers.
  Without reward-style synergies alongside, mixing is only ever penalised.
- **Silent interactions read as bugs.** Any block or removal needs to be visible to the player.
- **Marker capacity.** Three visible markers plus one overflow glyph is already tight at six
  kinds; nine kinds makes the overflow gap in `TODO.md` matter much more.
- **`td.effect` cannot depend on `td.ui` or `td.enemy`.** Palette roles and mob knowledge stay
  outside it.
- **`CLAUDE.md` and `TODO.md` go stale on this change.** The effect package doc names burn's crit
  contribution; the `TODO.md` crit-numbers entry names it too. Both are fixed in the same commit.
- Standing requirements: headless engine, UI uniformity, threading rules, `RandomSource`.

## Decisions made

- Vulnerable is wanted.
- Burn doubling crit chance is too strong and is to be replaced.
- Heal stays as it is.

## Open questions

**Burn's new role** (pick one, or a combination):
- Anti-heal: burning enemies receive about half the healing. A niche against the Warden's regen
  and reshield that nothing else fills.
- Armor melt: burn reduces flat armor. The Warden's flat resist is 200, so it has a clear target.
- A small additive crit bonus (+10 to 25%) instead of a multiplier.
- How should burn's anti-restoration differ from toxic's spirit reduction so they do not overlap?
  What else does spirit affect beyond scaling shields and heals?

**Fire vs frost:**
- Slow and burn: does burning weaken slow, or does slow reduce burn damage, or neither?
- Freeze clears burn and blocks new burn: confirmed as wanted? Should the lost burn contributions
  credit anything?
- Should there be a matching synergy so mixing towers is rewarded (for example, frozen enemies
  take extra damage to one damage type)?

**Stun:**
- What makes it more than a renamed freeze? Candidates: interrupts enemy abilities; extra crit
  chance taken (same risk as burn's doubling, so probably small); different visuals only.
- Does it share the freeze diminishing-returns group?
- Do enemy abilities have a can-act gate today, or would interrupting need one?

**Vulnerable:**
- Stack shape: does each stack have its own duration, or does one clock refresh all stacks?
- Per-stack magnitude, and is it one amplifier for all damage or per damage type?
- Which damage types does it amplify?

**Toxic/curse:**
- Its own decay curve, or the same fuel-pool shape as burn?
- Does it stack per tower like burn (shared credit) or refresh?

**Scope:**
- Are categories player-visible (marker colour, inspector line), or only a design tool?
- Is cleansing or dispel ever wanted?

## Ideas for the planning phase

Not decisions and not a design; enablers the discussion found that would make this work cheaper.
Planning should weigh them fresh.

- **Stacking policy as a value.** Today `ActiveEffects.apply` has bespoke branches for slow and
  burn. Vulnerable and toxic would add two more. A per-kind stacking policy (refresh, stack to a
  cap, pool, two-deep) would let a new kind choose one instead of adding code to a class already
  near 300 lines.
- **One home for cross-effect rules.** Freeze blocking burn, burn weakening slow and stun
  behaviour are three rules already. Keeping them as data in one place, read by `ActiveEffects`,
  would be easier to review than conditions scattered through `apply` and `contributeTo`.
- **Kind metadata beside the kind.** Category, resisting stat and diminishing-returns group could
  live on `EffectKind`, shrinking the per-kind switches. Palette roles stay in `td.ui`.
- **Prove each rule in an `ActiveEffects` unit test** rather than in `GameEngineTest`; it is
  headless and cheap.
- **Write the rulebook (categories, interaction table, numbers) in `README.md` first.** Changing a
  table is cheaper than changing code and keeps `CLAUDE.md` unchanged.
- **Fix the marker overflow count alongside**, since more kinds make it worse.
- Suggested order from the discussion: burn's new role, then freeze/burn immunity, then the
  stacking policy, then vulnerable and stun on top.

## Other ideas from the discussion

- Name the layers consistently in code and UI: trait (innate) → ability (trigger) → effect
  (timed state). "Active effect" is a misnomer, since in LoL an active is a click-to-cast.
- Diminishing returns generalised from freeze to every hard-CC kind (the WoW model); tenacity-like
  resist stats already exist as the resist family.
- Interactions defined per category, not per pair, to avoid N² special cases.
- Reward-style synergies as the counterweight to fire-vs-frost opposition: vulnerable
  amplification, stun plus crit, shatter on a frozen kill.
- A short in-game legend or inspector line per category so rules are learnable without a manual.

---

*After planning and implementation, update this document rather than deleting it: mark it
implemented, prune resolved open questions, and either promote deferred scope to a new request or
note it's still wanted for a later version.*
