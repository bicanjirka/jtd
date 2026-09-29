# Feature Request: Tower Upgrades, Second Iteration

## Summary

Deepen the tower upgrade system past its first shipped form. The headline change is a second
level of Awaken, **Transcendent**, which lets a tower carry two specials at once and unlocks a
fourth level of its head upgrades. Around it, this iteration is the home for further upgrade
growth: more head levels, more specials, and the new effects and gates they call for. The aim is
that a fully invested tower feels like a different, more capable machine than one that stopped at
Awaken, and that the two specials it carries interact rather than merely add.

## Current state (what exists today)

- **Three slots.** Every tower has a `base`, `head` and `special` slot (see
  `FEATURE-tower-upgrade-trees.md` and `FEATURE-tower-specialization-abilities.md`, both
  implemented). `base` holds a Range node and an Awaken node, both bought for money alone. Awaken
  is what makes `head` and `special` purchasable.
- **A tower gets one special, ever.** A special's root is offered only while nothing is owned in
  the `special` slot, so the roots are mutually exclusive and the choice is permanent.
- **Head chains are two levels deep, with three exceptions.** Sonar's Twin Array and both of
  Splash's chains have three levels. No chain has a fourth. A head root is likewise exclusive with
  the other chain, and each level requires the one before it.
- **Uneven special content.** Sniper, Sonar and Splash offer three specials each. Aura, Cinder,
  Mortar, Pulse and Seeker offer one.
- **Specials are independent of each other today.** Each is switched on by owning its node, so a
  tower with two would run both, but no such tower can exist. Sniper's three specials already
  react to one another through crits (a forced crit feeds the crit-triggered ones), which is the
  kind of interplay this iteration wants deliberately.
- **Gates.** A node is offered by its prerequisites (`requires`) and bought once its gate clears.
  Existing gates are money only, a kill count, damage dealt, and adjacent towers, and gates can be
  combined with and/or.
- **Effects a tower can apply** are the ones in `td.effect`: chill, burn, freeze, vulnerable,
  revealed and poison, with scorched and sickened earned from burn and poison. Effects a special
  needs that don't exist yet are added per special.
- **UI limits.** The upgrade panel shows up to three offered nodes per slot, numbered across all
  slots for the number-key shortcuts. A tower on the board shows one row of pips per slot and a
  glow while it owns a special.

## Current upgrade trees (today, with this iteration's head node marked NEW)

Every tower's tree as it stands today, for reference while planning what to extend. A damage gate
is stored in hundredths, so the numbers below are the ones the panel shows. Entries marked
`[NEW]` do not exist yet: they are Transcendent and the extra head node this iteration adds to
every tower, and their names, prices, gates and stats are still to be decided.

```
LEGEND   $n = price            [gate] = performance condition to clear once offered (price is always due)
         kills  = tower's own kills          dmg   = damage the tower has dealt (as shown in the panel)
         near-N = N of the 8 surrounding cells hold towers
         (no gate) = price only              $? [?] = price and gate not decided yet
         [NEW] = added by this iteration     TBD = to be filled in, differs per tower
         II / III / IV = the next level of the entry above it; offered only after that one is owned

EVERY TOWER
├── BASE            (Range and Awaken are bought independently, price only, any order)
│   ├── Range       +15% range
│   └── Awaken      unlocks HEAD and SPECIAL
│       └── Transcendent  [NEW] Awaken II; needs an owned special and a head lv3; unlocks a 2nd special and head lv4
├── HEAD            (needs Awaken)
│   ├── chain A     lv1 -> lv2 (-> lv3)   ┐ pick ONE of the two chains; the other
│   ├── chain B     lv1 -> lv2 (-> lv3)   ┘ locks out for good
│   └── [NEW] one further node per tower, levels I to IV, exclusive with nothing
└── SPECIAL         (needs Awaken. Pick ONE, the rest lock out for good)

HEAD and SPECIAL are independent slots: a tower progresses in both at once.
Stat bonuses from every owned node add up (fire-rate bonuses multiply).
Only one special is ever owned per tower today, so no two specials ever coexist yet.


AURA
├── BASE
│   ├── Range               $12  +15% range
│   └── Awaken              $20  unlocks HEAD and SPECIAL
│       └── Transcendent    $? [special + head III]  [NEW]  2nd special, head IV
├── HEAD    (needs Awaken)
│   ├── Amplifying Core     $20 [near-2]  +50% buff strength
│   │   └── II              $30 [near-3]  +50% more; aura also grants fire rate
│   ├── Resonance Field     $20 [near-2]  +30% range
│   │   └── II              $30 [near-3]  +25% range; aura may buff other Auras
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       ├── III             $? [?]  TBD
│       └── IV              $? [?]  TBD
└── SPECIAL (needs Awaken)
    └── Withering Field     $40 [near-2]  enemies in range gain Vulnerable

CINDER
├── BASE
│   ├── Range               $17  +15% range
│   └── Awaken              $28  unlocks HEAD and SPECIAL
│       └── Transcendent    $? [special + head III]  [NEW]  2nd special, head IV
├── HEAD    (needs Awaken)
│   ├── White Flame         $30 [150 dmg]   +30% damage
│   │   └── II              $45 [300 dmg]   +25% damage, +50% burn duration
│   ├── Wide Nozzle         $25 [10 kills]  +25% range, +30% cone
│   │   └── II              $38 [250 dmg]   +20% range, +20% fire rate, +20% cone
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       ├── III             $? [?]  TBD
│       └── IV              $? [?]  TBD
└── SPECIAL (needs Awaken)
    └── Hexflame            $56 [20 kills]  a new ignite also grants Vulnerable

MORTAR
├── BASE
│   ├── Range               $18  +15% range
│   └── Awaken              $30  unlocks HEAD and SPECIAL
│       └── Transcendent    $? [special + head III]  [NEW]  2nd special, head IV
├── HEAD    (needs Awaken)
│   ├── Siege Rounds        $35 [150 dmg]   +30% damage
│   │   └── II              $53 [300 dmg]   +25% damage, +40% splash radius
│   ├── Fragmentation       $30 [12 kills]  shrapnel ring at 25% damage
│   │   └── II              $45 [near-2]    shrapnel also chills (half duration)
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       ├── III             $? [?]  TBD
│       └── IV              $? [?]  TBD
└── SPECIAL (needs Awaken)
    └── Cursed Shrapnel     $60 [20 kills]  every blasted enemy gets Vulnerable

PULSE
├── BASE
│   ├── Range               $15  +15% range
│   └── Awaken              $25  unlocks HEAD and SPECIAL
│       └── Transcendent    $? [special + head III]  [NEW]  2nd special, head IV
├── HEAD    (needs Awaken)
│   ├── Overcharged Coils   $30 [100 dmg]   +30% damage
│   │   └── II              $45 [20 kills]  +25% damage, +10% crit
│   ├── Resonant Field      $25 [10 kills]  +20% range, hits invisible enemies
│   │   └── II              $38 [200 dmg]   +15% range, hit invisible enemies revealed 2s
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       ├── III             $? [?]  TBD
│       └── IV              $? [?]  TBD
└── SPECIAL (needs Awaken)
    └── Warding Field       $50 [20 kills]  10% chance per tick to add Vulnerable

SEEKER
├── BASE
│   ├── Range               $21  +15% range
│   └── Awaken              $35  unlocks HEAD and SPECIAL
│       └── Transcendent    $? [special + head III]  [NEW]  2nd special, head IV
├── HEAD    (needs Awaken)
│   ├── Twin Warhead        $30 [10 kills]  +30% fire rate
│   │   └── II              $45 [250 dmg]   +25% fire rate, two missiles
│   ├── Deep Freeze         $35 [12 kills]  +30% damage
│   │   └── II              $53 [25 kills]  +25% damage, +75% freeze, shatter on kill
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       ├── III             $? [?]  TBD
│       └── IV              $? [?]  TBD
└── SPECIAL (needs Awaken)
    └── Homing Curse        $70 [20 kills]  impact adds Vulnerable (2 if frozen/chilled)

SNIPER
├── BASE
│   ├── Range               $6  +15% range
│   └── Awaken              $10  unlocks HEAD and SPECIAL
│       └── Transcendent    $? [special + head III]  [NEW]  2nd special, head IV
├── HEAD    (needs Awaken)
│   ├── Focused Optics      $25 (no gate)   +20% damage
│   │   └── II              $38 (no gate)   +20% damage, +25% fire rate
│   ├── Marksman's Eye      $30 [15 kills]  +15% crit chance
│   │   └── II              $45 [200 dmg]   +20% crit, +50% armor penetration
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       ├── III             $? [?]  TBD
│       └── IV              $? [?]  TBD
└── SPECIAL (needs Awaken)  (any of these also retargets the tower to the highest-health enemy)
    ├── Marked Round        $20 [10 kills]  crits apply Vulnerable
    ├── Fifth Shot          $20 [15 kills]  every 5th shot crits, crits deal 250%
    └── Momentum            $20 [20 kills]  post-crit shot 500%, ignores armor; kill = +100% fire rate 5s

SONAR
├── BASE
│   ├── Range               $12  +15% range
│   └── Awaken              $20  unlocks HEAD and SPECIAL
│       └── Transcendent    $? [special + head III]  [NEW]  2nd special, head IV
├── HEAD    (needs Awaken)
│   ├── Twin Array          $35 [100 dmg]   +25% damage
│   │   ├── II              $53 [200 dmg]   +25% damage, +10% crit
│   │   └── III             $70 [25 kills]  second turret, opposite direction
│   ├── Long Reach          $30 [10 kills]  +15% crit chance
│   │   └── II              $45 [200 dmg]   damage up to +100% at max range
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       ├── III             $? [?]  TBD
│       └── IV              $? [?]  TBD
└── SPECIAL (needs Awaken)
    ├── Wide Band           $40 [near-2]    each revolution reveals invisible enemies
    ├── Mark on Sweep       $40 [15 kills]  marked target's next hit is a guaranteed crit
    └── Piercing Tone       $40 [200 dmg]   up to +50% magic damage vs armored/shielded

SPLASH
├── BASE
│   ├── Range               $9  +15% range
│   └── Awaken              $15  unlocks HEAD and SPECIAL
│       └── Transcendent    $? [special + head III]  [NEW]  2nd special, head IV
├── HEAD    (needs Awaken)
│   ├── Blast Engineering   $35 [100 dmg]   +30% splash radius
│   │   ├── II              $53 [200 dmg]   +25% damage, flatter falloff
│   │   └── III             $70 [20 kills]  3 projectiles instead of 1
│   ├── Rapid Battery       $30 [8 kills]   +25% fire rate
│   │   ├── II              $45 [18 kills]  +25% damage, +15% crit
│   │   └── III             $60 [250 dmg]   crits splash 50% bigger
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       ├── III             $? [?]  TBD
│       └── IV              $? [?]  TBD
└── SPECIAL (needs Awaken)
    ├── Toxic Bloom         $30 [150 dmg]   splash applies poison
    ├── Concussive Blast    $30 [15 kills]  -50% fire rate, blast chills, kills explode
    └── Overpressure        $30 [25 kills]  on crit, next shot hits every enemy in range
```

**Coexistence today.** Range and Awaken coexist. One head chain and one special coexist, and the
stat bonus of every owned node stacks. The two head chains are mutually exclusive, and so are a
tower's specials: whichever is taken first locks out the rest permanently. Head levels are
sequential. The `[NEW]` head node is the exception to exclusivity: it coexists with either chain.

## What this feature adds

**Transcendent (the anchor of this iteration)**

- Awaken becomes a two-level upgrade. Level 2 is called **Transcendent**. It appears in place of
  Awaken once Awaken is bought, like the second level of any head chain, not as an extra button.
- Transcendent is **gated on investment**: a tower must already own a special (level 1 of the
  special slot) and the third level of a head chain. Until both are owned it is not available. It
  also costs money, and has no stat bonus of its own.
- Buying it unlocks a **second special**: a tower may then own two specials instead of one. The
  two coexist, both are always active, and they are designed to **affect each other**, so a pair
  does something neither does alone.
- Buying it also unlocks the **fourth level of each head chain**, which continues the chain the
  tower is already on.
- Because a special is a prerequisite, Transcendent is always bought *after* the first special;
  the second special is picked afterwards.

**A further head node on every tower**

- Every tower gets one additional node in its `head` slot, shown as `[NEW]` in the trees above.
- It is **not exclusive** with the tower's two existing head chains: picking either chain does not
  lock it out, and having it does not lock either chain out. It coexists with whichever chain the
  tower is on.
- It needs Awaken, like the other head nodes.
- It is a chain of four levels, I to IV, each requiring the one before it.
- Each tower's node is its own, with its own stats, not one shared upgrade. Names, prices, gates
  and stats at every level are still to be decided.

**Further upgrade growth (open-ended, tracked here)**

This document also collects the additional upgrade content this iteration may cover. Each is a
candidate, to be scoped when picked up, not a commitment:

- **More head levels.** Level 3 for every chain that stops at 2 (Transcendent's prerequisite, so
  without it Transcendent cannot be reached on that tower), and level 4 for every chain (what
  Transcendent unlocks).
- **More specials.** At least a second special for each of the five towers that offer only one, so
  every tower can fill both special slots.
- **New effects** that a head level or special needs, added when a concrete upgrade asks for one.
- **New gates**, meaning new performance conditions for a node to clear, added the same way.
- **Special pairings.** How each pair of specials on a tower interacts, tower by tower.

**Out of scope for this iteration**

- Any third special slot or a level beyond Transcendent.
- Refunding, respeccing or swapping a bought special.
- Enemy-side changes, apart from whatever a new effect strictly needs.

## Interconnections

- **`FEATURE-tower-upgrade-trees.md` and `FEATURE-tower-specialization-abilities.md`** are the
  systems and content this builds on. Their "one special" and "Awaken unlocks head and special"
  statements become false when this ships, and each should be updated rather than left stale.
- **`FEATURE-effect-interactions.md`** governs how effects act on each other. A special that adds
  or reshapes an effect must respect it.
- **`FEATURE-critical-damage.md`** matters for special pairings, since several specials trigger
  off crits.
- **`README.md`'s tower table** and each tower's own upgrade text will change with the content.

## Constraints and open risks

- **Level 3 is now mandatory content.** A head chain that stops at level 2 makes Transcendent
  unreachable on that tower. Thirteen of the sixteen chains stop there today, so a tower does not
  get Transcendent until its chains are extended.
- **Content volume dwarfs the mechanism.** The rule change is small. The number of new head levels
  (about 29 to give every chain a level 3 and a level 4), the new head node (32 levels, four on
  each of eight towers), new specials (at least five) and special pairings (about 14 across the
  eight towers) is where the work and the balance risk lie.
- **Balance.** Two coexisting specials multiply, not add. A pairing that combines two damage
  amplifiers can run away, and `td.BalanceHarness` numbers and the last-level performance budget
  are the checks on that.
- **Some pairings change what a tower is for.** Sniper's special already switches its targeting.
  Two specials that each change targeting or behaviour may conflict, and the request has to say
  which wins.
- **UI room.** Four head levels, the extra head node, two specials and a two-level Awaken must
  still read at a glance in the panel and on the tower, with no new platform-styled controls. The
  panel shows at most three offered nodes per slot: a fresh head slot would offer the two chain
  roots plus the new node's first level, which is exactly three, so it fits with no room to spare.
  Once a chain is picked, the slot offers at most that chain's next level and the new node's next
  level.
- **A head node that coexists with everything is a new kind of node.** Today a head root is
  offered only while the slot is empty, so all head roots exclude each other. The head slot's
  pips count owned nodes, so the extra node also changes what "head level" means on the tower.
- **Standing requirements:** the engine stays headless, board content goes through the render
  pipeline, and the UI look stays uniform on every OS.

## Decisions made

- Transcendent is Awaken's second level, not a separate button.
- It is gated: the tower must own a special and a head level 3. Money alone is not enough.
- It unlocks the second special and the fourth head level, and nothing else.
- Both specials on a tower coexist and are meant to affect each other.
- Every tower gets one extra head node that is exclusive with nothing and has stats of its own.
  It needs Awaken and grows levels I to IV.
- The mechanism ships first and is proven on one tower. Content is then added tower by tower.

## Open questions

- **The extra head node's level IV.** Transcendent unlocks the fourth level of each head chain.
  Does the extra node's level IV need Transcendent too, or is it open once level III is owned?
- **Does it count toward Transcendent?** Transcendent needs a head level 3. Does the extra node's
  level III count for that, or only a chain's third level does?
- **The extra head node's content.** What are its name, price, gate and stats at each level on
  each tower?
- **Free choice or restricted?** May the second special be any unowned special, or must it differ
  in some way from the first, such as being on a different branch?
- **Price.** What does Transcendent cost per tower relative to its Awaken?
- **Pairing authorship.** Is each pair's interaction hand-designed, or is a generic bonus for
  owning both acceptable where a bespoke interaction would be too costly?
- **Towers with a single special.** Do they get a second special in this iteration, or does
  Transcendent do nothing for them until they do?
- **Visibility until unlocked.** Until the tower owns a special and a head level 3, is Transcendent
  hidden (as unreachable nodes are today) or shown as locked with what is missing?
- **Anything on top?** Besides those two prerequisites and its price, does Transcendent also carry
  a performance gate (kills, damage dealt), or are the prerequisites the whole gate?
- **Rollout order.** Which towers get their level-3 head chains first, given that Transcendent
  cannot exist on a tower until it has one?
- **Visual.** Does Transcendent get a mark of its own on the tower, or do the extra pips and the
  second special's glow suffice?
- **First proving tower.** Which tower carries the first Transcendent, given that Splash already
  has three-level chains and three specials?

---

*After planning and implementation, update this document rather than deleting it: mark it
implemented, prune resolved open questions, and either promote deferred scope to a new request or
note it's still wanted for a later version.*
