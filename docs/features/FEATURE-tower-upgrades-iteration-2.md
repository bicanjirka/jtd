# Feature Request: Tower Upgrades, Second Iteration

## Summary

Deepen the tower upgrade system past its first shipped form. The headline change is a second
level of Awaken, **Transcendent**, which lets a tower carry two specials at once and unlocks a
fourth level of its head upgrades, chosen from two options. Around it, this iteration is the home
for further upgrade growth: more head levels, more specials, and the new effects and gates they
call for. The aim is that a fully invested tower feels like a different, more capable machine than one that stopped at
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

## Upgrade trees (today, plus this iteration's slots to fill)

Every tower's tree as it stands today, with a placeholder row wherever this iteration adds a node.
A damage gate is stored in hundredths, so the numbers below are the ones the panel shows. Entries
marked `[NEW]` do not exist yet: Transcendent, Range II and III, the missing level 3 of each head
chain, the two level 4 options (IV-A and IV-B) under every chain's level 3, the extra head node and
the second special. To design an upgrade, replace the `TBD` and `$? [?]` of its row with a name,
price, gate and effect; nothing else in the tree needs to change.

```
LEGEND   $n = price            [gate] = performance condition to clear once offered (price is always due)
         kills  = tower's own kills          dmg   = damage the tower has dealt (as shown in the panel)
         near-N = N of the 8 surrounding cells hold towers
         (no gate) = price only              $? [?] = price and gate not decided yet
         [NEW] = added by this iteration     TBD = to be filled in, differs per tower
         (needs X) = also requires X to be owned, on top of the levels before it
         stat effects = either a bonus ("+25% range") or an absolute value ("range 2.0"), which
                        replaces the tower's current base stat with that value (Aura's Range
                        "base range 1.5" raises its 1.1 to 1.5). Use either wherever it fits
         Fortify = working name of the new base root that opens HEAD; the final name is undecided
         II / III / IV = the next level of the entry above it; offered only after that one is owned
         IV-A / IV-B = the two level 4 options under a level 3; only one of them can ever be owned
         TOWER LINE = base price and stats before buffs and upgrades, one per tower, meant for
                      balancing edits. dmg is per hit as the panel shows it, range and radii are
                      in cells, cd = cooldown in ticks (20 ticks = 1s; shots/s = 20 / (cd + 1)),
                      t = ticks

EVERY TOWER
├── BASE            (Range is bought on its own, price only. Fortify -> Awaken -> Transcendent is one chain)
│   ├── Range       +15% range
│   │   ├── II  [NEW]   needs Range + Awaken
│   │   └── III [NEW]   needs Range II + Transcendent
│   └── Fortify [NEW]   new base root (working name), price only. The base is strengthened to carry
│       │               heavier turrets. Unlocks HEAD lv1 and lv2
│       └── Awaken      needs Fortify, so it is level 2 of the chain. Unlocks HEAD lv3 and SPECIAL
│           └── Transcendent  [NEW] Awaken II; needs an owned special and a head lv3.
│                             Unlocks HEAD lv4 (IV-A or IV-B) and SPECIAL slot 2
├── HEAD            (lv1 and lv2 need Fortify, lv3 needs Awaken, lv4 needs Transcendent)
│   ├── chain A     lv1 -> lv2 -> lv3 -> lv4-A | lv4-B   ┐ pick ONE of the two chains; the other
│   ├── chain B     lv1 -> lv2 -> lv3 -> lv4-A | lv4-B   ┘ locks out for good
│   │               (lv3 and both lv4 options are [NEW] wherever a chain stops short today;
│   │                a chain's lv4-A and lv4-B exclude each other)
│   └── [NEW] one further node per tower, levels I to IV, exclusive with nothing
│                   (same unlock levels as the chains)
└── SPECIAL         (needs Awaken. Pick ONE, the rest lock out for good)
    ├── slot 1      one of the tower's specials
    └── slot 2 [NEW]  a second special, unlocked by Transcendent; pairs with slot 1

EXCEPTION: AURA caps at Awaken. It has no Transcendent, so no Range III, no lv4 head nodes
and no second special. Its chains stop at lv3 and its extra head node at III.

HEAD and SPECIAL are independent slots: a tower progresses in both at once.
Stat bonuses from every owned node add up (fire-rate bonuses multiply).
Only one special is ever owned per tower today, so no two specials ever coexist yet.
Each base step opens more of the other slots: Fortify opens HEAD, Awaken opens HEAD lv3 and
SPECIAL, Transcendent opens HEAD lv4 and SPECIAL slot 2.


AURA        $20 | dmg 0 (passive) | range 1.5 | buff +20% range and damage
├── BASE
│   ├── Range               $40  base range 2.05
│   │   └── II  [NEW]       $80  base range 3.17  (needs Range + Awaken)
│   └── Fortify [NEW]       $20 [near-2]  TBD  (unlocks HEAD lv1 and lv2)
│       └── Awaken          $100 [near-6] (needs Fortify)  unlocks HEAD lv3 and SPECIAL
│                           (aura caps here: no Transcendent, no Range III)
├── HEAD    (lv1 and lv2 need Fortify, lv3 needs Awaken, no lv4)
│   ├── Amplifying Core     $20 [near-2]  buff +25% range and damage
│   │   ├── II              $30 [near-3]  buff +30% range and damage, buff +10% fire rate
│   │   └── III [NEW]       $? [?]  TBD  (needs Awaken)
│   ├── Resonance Field     $20 [near-2]  +30% range
│   │   ├── II              $30 [near-3]  +25% range; aura may buff other Auras
│   │   └── III [NEW]       $? [?]  TBD  (needs Awaken)
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       └── III             $? [?]  TBD  (needs Awaken)
└── SPECIAL (needs Awaken; one slot only)
    └── Withering Field     $40 [near-2]  enemies in range gain Vulnerable

CINDER      $28 | dmg 2 | range 2.2 | cd 20t (0.95/s) | cone half-width 0.35 rad | burn 60t (3s)
├── BASE
│   ├── Range               $17  +15% range
│   │   ├── II  [NEW]       $? [?]  TBD  (needs Range + Awaken)
│   │   └── III [NEW]       $? [?]  TBD  (needs Range II + Transcendent)
│   └── Fortify [NEW]       $? [?]  TBD  (unlocks HEAD lv1 and lv2)
│       └── Awaken          $28  (needs Fortify)  unlocks HEAD lv3 and SPECIAL
│           └── Transcendent  $? [special + head III]  [NEW]  2nd special, head IV-A or IV-B
├── HEAD    (lv1 and lv2 need Fortify, lv3 needs Awaken, lv4 needs Transcendent)
│   ├── White Flame         $30 [150 dmg]   +30% damage
│   │   ├── II              $45 [300 dmg]   +25% damage, +50% burn duration
│   │   └── III [NEW]       $? [?]  TBD  (needs Awaken)
│   │       ├── IV-A [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   │       └── IV-B [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   ├── Wide Nozzle         $25 [10 kills]  +25% range, +30% cone
│   │   ├── II              $38 [250 dmg]   +20% range, +20% fire rate, +20% cone
│   │   └── III [NEW]       $? [?]  TBD  (needs Awaken)
│   │       ├── IV-A [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   │       └── IV-B [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       ├── III             $? [?]  TBD  (needs Awaken)
│       └── IV              $? [?]  TBD  (needs Transcendent)
└── SPECIAL (needs Awaken; the 2nd slot needs Transcendent)
    ├── Hexflame            $56 [20 kills]  a new ignite also grants Vulnerable
    └── [NEW] 2nd special   $? [?]  TBD  (slot 2, needs Transcendent; pairing with slot 1: TBD)

MORTAR      $30 | dmg 20 | range 4.0 | cd 50t (0.39/s) | splash 2.0 | chill 50% for 40t (2s)
├── BASE
│   ├── Range               $18  +15% range
│   │   ├── II  [NEW]       $? [?]  TBD  (needs Range + Awaken)
│   │   └── III [NEW]       $? [?]  TBD  (needs Range II + Transcendent)
│   └── Fortify [NEW]       $? [?]  TBD  (unlocks HEAD lv1 and lv2)
│       └── Awaken          $30  (needs Fortify)  unlocks HEAD lv3 and SPECIAL
│           └── Transcendent  $? [special + head III]  [NEW]  2nd special, head IV-A or IV-B
├── HEAD    (lv1 and lv2 need Fortify, lv3 needs Awaken, lv4 needs Transcendent)
│   ├── Siege Rounds        $35 [150 dmg]   +30% damage
│   │   ├── II              $53 [300 dmg]   +25% damage, +40% splash radius
│   │   └── III [NEW]       $? [?]  TBD  (needs Awaken)
│   │       ├── IV-A [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   │       └── IV-B [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   ├── Fragmentation       $30 [12 kills]  shrapnel ring at 25% damage
│   │   ├── II              $45 [near-2]    shrapnel also chills (half duration)
│   │   └── III [NEW]       $? [?]  TBD  (needs Awaken)
│   │       ├── IV-A [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   │       └── IV-B [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       ├── III             $? [?]  TBD  (needs Awaken)
│       └── IV              $? [?]  TBD  (needs Transcendent)
└── SPECIAL (needs Awaken; the 2nd slot needs Transcendent)
    ├── Cursed Shrapnel     $60 [20 kills]  every blasted enemy gets Vulnerable
    └── [NEW] 2nd special   $? [?]  TBD  (slot 2, needs Transcendent; pairing with slot 1: TBD)

PULSE       $25 | dmg 2 per tick to all in range (40/s) | range 1.5 | no cooldown
├── BASE
│   ├── Range               $15  +15% range
│   │   ├── II  [NEW]       $? [?]  TBD  (needs Range + Awaken)
│   │   └── III [NEW]       $? [?]  TBD  (needs Range II + Transcendent)
│   └── Fortify [NEW]       $? [?]  TBD  (unlocks HEAD lv1 and lv2)
│       └── Awaken          $25  (needs Fortify)  unlocks HEAD lv3 and SPECIAL
│           └── Transcendent  $? [special + head III]  [NEW]  2nd special, head IV-A or IV-B
├── HEAD    (lv1 and lv2 need Fortify, lv3 needs Awaken, lv4 needs Transcendent)
│   ├── Overcharged Coils   $30 [100 dmg]   +30% damage
│   │   ├── II              $45 [20 kills]  +25% damage, +10% crit
│   │   └── III [NEW]       $? [?]  TBD  (needs Awaken)
│   │       ├── IV-A [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   │       └── IV-B [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   ├── Resonant Field      $25 [10 kills]  +20% range, hits invisible enemies
│   │   ├── II              $38 [200 dmg]   +15% range, hit invisible enemies revealed 2s
│   │   └── III [NEW]       $? [?]  TBD  (needs Awaken)
│   │       ├── IV-A [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   │       └── IV-B [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       ├── III             $? [?]  TBD  (needs Awaken)
│       └── IV              $? [?]  TBD  (needs Transcendent)
└── SPECIAL (needs Awaken; the 2nd slot needs Transcendent)
    ├── Warding Field       $50 [20 kills]  10% chance per tick to add Vulnerable
    └── [NEW] 2nd special   $? [?]  TBD  (slot 2, needs Transcendent; pairing with slot 1: TBD)

SEEKER      $35 | dmg 26 | range 4.5 | cd 45t (0.43/s) | freeze 30t (1.5s) | missile speed 35
├── BASE
│   ├── Range               $21  +15% range
│   │   ├── II  [NEW]       $? [?]  TBD  (needs Range + Awaken)
│   │   └── III [NEW]       $? [?]  TBD  (needs Range II + Transcendent)
│   └── Fortify [NEW]       $? [?]  TBD  (unlocks HEAD lv1 and lv2)
│       └── Awaken          $35  (needs Fortify)  unlocks HEAD lv3 and SPECIAL
│           └── Transcendent  $? [special + head III]  [NEW]  2nd special, head IV-A or IV-B
├── HEAD    (lv1 and lv2 need Fortify, lv3 needs Awaken, lv4 needs Transcendent)
│   ├── Twin Warhead        $30 [10 kills]  +30% fire rate
│   │   ├── II              $45 [250 dmg]   +25% fire rate, two missiles
│   │   └── III [NEW]       $? [?]  TBD  (needs Awaken)
│   │       ├── IV-A [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   │       └── IV-B [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   ├── Deep Freeze         $35 [12 kills]  +30% damage
│   │   ├── II              $53 [25 kills]  +25% damage, +75% freeze, shatter on kill
│   │   └── III [NEW]       $? [?]  TBD  (needs Awaken)
│   │       ├── IV-A [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   │       └── IV-B [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       ├── III             $? [?]  TBD  (needs Awaken)
│       └── IV              $? [?]  TBD  (needs Transcendent)
└── SPECIAL (needs Awaken; the 2nd slot needs Transcendent)
    ├── Homing Curse        $70 [20 kills]  impact adds Vulnerable (2 if frozen/chilled)
    └── [NEW] 2nd special   $? [?]  TBD  (slot 2, needs Transcendent; pairing with slot 1: TBD)

SNIPER      $10 | dmg 30 | range 3.8 | cd 39t (0.50/s) | crit 15%
├── BASE
│   ├── Range               $6  +15% range
│   │   ├── II  [NEW]       $? [?]  TBD  (needs Range + Awaken)
│   │   └── III [NEW]       $? [?]  TBD  (needs Range II + Transcendent)
│   └── Fortify [NEW]       $? [?]  TBD  (unlocks HEAD lv1 and lv2)
│       └── Awaken          $10  (needs Fortify)  unlocks HEAD lv3 and SPECIAL
│           └── Transcendent  $? [special + head III]  [NEW]  2nd special, head IV-A or IV-B
├── HEAD    (lv1 and lv2 need Fortify, lv3 needs Awaken, lv4 needs Transcendent)
│   ├── Focused Optics      $25 (no gate)   +20% damage
│   │   ├── II              $38 (no gate)   +20% damage, +25% fire rate
│   │   └── III [NEW]       $? [?]  TBD  (needs Awaken)
│   │       ├── IV-A [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   │       └── IV-B [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   ├── Marksman's Eye      $30 [15 kills]  +15% crit chance
│   │   ├── II              $45 [200 dmg]   +20% crit, +50% armor penetration
│   │   └── III [NEW]       $? [?]  TBD  (needs Awaken)
│   │       ├── IV-A [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   │       └── IV-B [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       ├── III             $? [?]  TBD  (needs Awaken)
│       └── IV              $? [?]  TBD  (needs Transcendent)
└── SPECIAL (needs Awaken; the 2nd slot needs Transcendent)  (any of these also retargets the tower to the highest-health enemy)
    ├── Marked Round        $20 [10 kills]  crits apply Vulnerable
    ├── Fifth Shot          $20 [15 kills]  every 5th shot crits, crits deal 250%
    ├── Momentum            $20 [20 kills]  post-crit shot 500%, ignores armor; kill = +100% fire rate 5s
    └── [NEW] 2nd special   $? [?]  TBD  (slot 2, needs Transcendent: another special above or a new one; pairings TBD)

SONAR       $20 | dmg 16 | range 4.2 | sweep 4s per revolution (no cooldown) counter-clockwise
├── BASE
│   ├── Range               $12  +15% range
│   │   ├── II  [NEW]       $? [?]  TBD  (needs Range + Awaken)
│   │   └── III [NEW]       $? [?]  TBD  (needs Range II + Transcendent)
│   └── Fortify [NEW]       $? [?]  TBD  (unlocks HEAD lv1 and lv2)
│       └── Awaken          $20  (needs Fortify)  unlocks HEAD lv3 and SPECIAL
│           └── Transcendent  $? [special + head III]  [NEW]  2nd special, head IV-A or IV-B
├── HEAD    (lv1 and lv2 need Fortify, lv3 needs Awaken, lv4 needs Transcendent)
│   ├── Twin Array          $35 [100 dmg]   +25% damage
│   │   ├── II              $53 [200 dmg]   +25% damage, +10% crit
│   │   └── III             $70 [25 kills]  second turret, opposite direction  (needs Awaken)
│   │       ├── IV-A [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   │       └── IV-B [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   ├── Long Reach          $30 [10 kills]  +15% crit chance
│   │   ├── II              $45 [200 dmg]   damage up to +100% at max range
│   │   └── III [NEW]       $? [?]  TBD  (needs Awaken)
│   │       ├── IV-A [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   │       └── IV-B [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       ├── III             $? [?]  TBD  (needs Awaken)
│       └── IV              $? [?]  TBD  (needs Transcendent)
└── SPECIAL (needs Awaken; the 2nd slot needs Transcendent)
    ├── Wide Band           $40 [near-2]    each revolution reveals invisible enemies
    ├── Mark on Sweep       $40 [15 kills]  marked target's next hit is a guaranteed crit
    ├── Piercing Tone       $40 [200 dmg]   up to +50% magic damage vs armored/shielded
    └── [NEW] 2nd special   $? [?]  TBD  (slot 2, needs Transcendent: another special above or a new one; pairings TBD)

SPLASH      $15 | dmg 16 | range 3.2 | cd 19t (1.00/s) | spread 1.75 | chill 50% for 40t (2s)
├── BASE
│   ├── Range               $9  +15% range
│   │   ├── II  [NEW]       $? [?]  TBD  (needs Range + Awaken)
│   │   └── III [NEW]       $? [?]  TBD  (needs Range II + Transcendent)
│   └── Fortify [NEW]       $? [?]  TBD  (unlocks HEAD lv1 and lv2)
│       └── Awaken          $15  (needs Fortify)  unlocks HEAD lv3 and SPECIAL
│           └── Transcendent  $? [special + head III]  [NEW]  2nd special, head IV-A or IV-B
├── HEAD    (lv1 and lv2 need Fortify, lv3 needs Awaken, lv4 needs Transcendent)
│   ├── Blast Engineering   $35 [100 dmg]   +30% splash radius
│   │   ├── II              $53 [200 dmg]   +25% damage, flatter falloff
│   │   └── III             $70 [20 kills]  3 projectiles instead of 1  (needs Awaken)
│   │       ├── IV-A [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   │       └── IV-B [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   ├── Rapid Battery       $30 [8 kills]   +25% fire rate
│   │   ├── II              $45 [18 kills]  +25% damage, +15% crit
│   │   └── III             $60 [250 dmg]   crits splash 50% bigger  (needs Awaken)
│   │       ├── IV-A [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   │       └── IV-B [NEW]  $? [?]  TBD  (needs Transcendent; IV-A or IV-B, not both)
│   └── [NEW] name TBD      $? [?]  stats TBD (coexists with either chain above)
│       ├── II              $? [?]  TBD
│       ├── III             $? [?]  TBD  (needs Awaken)
│       └── IV              $? [?]  TBD  (needs Transcendent)
└── SPECIAL (needs Awaken; the 2nd slot needs Transcendent)
    ├── Toxic Bloom         $30 [150 dmg]   splash applies poison
    ├── Concussive Blast    $30 [15 kills]  -50% fire rate, blast chills, kills explode
    ├── Overpressure        $30 [25 kills]  on crit, next shot hits every enemy in range
    └── [NEW] 2nd special   $? [?]  TBD  (slot 2, needs Transcendent: another special above or a new one; pairings TBD)
```

**Coexistence today.** Range and Awaken coexist; Range II and III are new and follow Range. One head chain and one special coexist, and the
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
- Buying it also unlocks the **two fourth-level options of each head chain** (IV-A and IV-B),
  which continue the chain the tower is already on; the tower picks one.
- Because a special is a prerequisite, Transcendent is always bought *after* the first special;
  the second special is picked afterwards.

**More Range levels**

- Range gets two more levels in the `base` slot, bought for money like Range itself.
- **Range II** needs Range and Awaken. **Range III** needs Range II and Transcendent.
- Each level is a further range increase, still to be decided per tower along with its price. It
  may be written as a percentage bonus or as an absolute base range (Aura's "base range 2.05"),
  as may any other stat effect in the trees.

**Head chains reach level 4, and level 4 is a choice**

- Both original head chains of every tower grow to level 3 and then branch: level 3 leads to two
  alternative level 4 options, **IV-A** and **IV-B**. Where a chain stops at 2 today it gains lv3
  and both level 4 options, and where it has 3 it gains both level 4 options.
- Levels 1 to 3 need only the level before them. **Both level 4 options also need Transcendent.**
- The tower buys **one** of the two, and the other locks out for good, the same permanent choice
  as picking a chain or a special. The two options are meant to feel different (a different way to
  use the chain), not a stronger and a weaker version of one idea.
- The tower still picks one chain and the other locks out, as today, so a tower ends with exactly
  one level 4 head node from its chosen chain.
- Each option is offered as soon as level 3 and Transcendent are owned, and disappears from the
  panel once the other is bought. The rules need a way to say "not if that one is owned".

**A further head node on every tower**

- Every tower gets one additional node in its `head` slot, shown as `[NEW]` in the trees above.
- It is **not exclusive** with the tower's two existing head chains: picking either chain does not
  lock it out, and having it does not lock either chain out. It coexists with whichever chain the
  tower is on.
- It needs Awaken, like the other head nodes.
- It is a chain of four levels, I to IV, each requiring the one before it.
- Each tower's node is its own, with its own stats, not one shared upgrade. Names, prices, gates
  and stats at every level are still to be decided.

**Marking exclusive choices in the upgrade panel**

- The panel must show, at a glance, whether the nodes on offer are an **exclusive choice** (buying
  one locks the others out for good) or **ordinary** (buying it forecloses nothing). Today it does
  not: a player finds out only after paying.
- A group of nodes that exclude each other is drawn as one visibly linked set, with a mark on the
  set saying that only one can be taken. The same mark serves every kind of exclusive choice:
  the two head chains, a tower's specials (including the second slot's pick), and IV-A / IV-B.
- A node that is not part of such a set carries no mark, so the absence of the mark means "safe to
  buy". The extra head node is never in an exclusive set.
- The mark also names what a pick costs the player: when a set is on offer, the panel says which
  alternatives will be locked out (for example, the other chain's name), not just that something
  will be.
- Once a choice is made, the panel shows what was picked and that the alternatives are gone,
  instead of silently dropping them.
- The mark is drawn by the panel itself in the shared HUD look, not by a platform control, and
  reads without colour alone (a glyph or label as well).

**Further upgrade growth (open-ended, tracked here)**

This document also collects the additional upgrade content this iteration may cover. Each is a
candidate, to be scoped when picked up, not a commitment:

- **More head levels.** Level 3 for every chain that stops at 2 (Transcendent's prerequisite, so
  without it Transcendent cannot be reached on that tower), and two level 4 options for every chain
  (what Transcendent unlocks). These are the `III`, `IV-A` and `IV-B` `[NEW]` rows in the trees.
- **More Range levels.** Range II and III on every tower, the `[NEW]` rows under Range.
- **More specials.** At least a second special for each of the five towers that offer only one, so
  every tower can fill both special slots. This is the `[NEW] 2nd special` row in each tree.
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
  (about 45: a level 3 for the 13 chains that lack one and two level 4 options for all 16 chains), the new head node (32 levels, four on
  each of eight towers), the Range levels (16, two on each of eight towers), new specials (at least five) and special pairings (about 14 across the
  eight towers) is where the work and the balance risk lie.
- **Balance.** Two coexisting specials multiply, not add. A pairing that combines two damage
  amplifiers can run away, and `td.BalanceHarness` numbers and the last-level performance budget
  are the checks on that.
- **Some pairings change what a tower is for.** Sniper's special already switches its targeting.
  Two specials that each change targeting or behaviour may conflict, and the request has to say
  which wins.
- **Two exclusive siblings are a new kind of choice.** Today exclusivity is between roots of a
  slot, and a chain's levels are strictly sequential. IV-A and IV-B share one prerequisite and each
  must disappear from the panel once the other is bought, so the rules need a way to say "not if
  that one is owned".
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
- Range gets two more levels: II needs Awaken, III needs Transcendent.
- Both original head chains of every tower grow to level 3 and then branch into two alternative
  level 4 options (IV-A, IV-B). Both need Transcendent, and only one can be bought.
- Transcendent is what opens the second special slot, not Awaken.
- A chain's two level 4 options exclude each other, like chains and specials do.
- The panel marks exclusive choices distinctly from ordinary purchases.
- Every tower gets one extra head node that is exclusive with nothing and has stats of its own.
  It needs Awaken and grows levels I to IV.
- The mechanism ships first and is proven on one tower. Content is then added tower by tower.

## Open questions

- **Does the extra head node branch too?** Its level IV needs a decision anyway. Is it a single
  level like the others in its chain, or two exclusive options like the chains' level 4?
- **The extra head node's level IV.** Transcendent unlocks the fourth level of each head chain.
  Does the extra node's level IV need Transcendent too, or is it open once level III is owned?
- **Does it count toward Transcendent?** Transcendent needs a head level 3. Does the extra node's
  level III count for that, or only a chain's third level does?
- **The extra head node's content.** What are its name, price, gate and stats at each level on
  each tower?
- **Free choice or restricted?** May the second special be any unowned special, or must it differ
  in some way from the first, such as being on a different branch?
- **Price.** What does Transcendent cost per tower relative to its Awaken?
- **Range II and III.** What do they add (a further +15% each, or a shrinking step) and what do
  they cost per tower? Does Range III's need for Transcendent stand, given that Transcendent
  itself needs a special and a head level 3?
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
- **Exclusive-choice mark.** What does the mark look like (a bracket around the set, a shared
  header, a glyph on each node), and does it also appear on the tower's pips or only in the panel?
- **Visual.** Does Transcendent get a mark of its own on the tower, or do the extra pips and the
  second special's glow suffice?
- **First proving tower.** Which tower carries the first Transcendent, given that Splash already
  has three-level chains and three specials?

---

*After planning and implementation, update this document rather than deleting it: mark it
implemented, prune resolved open questions, and either promote deferred scope to a new request or
note it's still wanted for a later version.*
