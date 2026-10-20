# Sniper and Scorcher: identity and meaningful paths

A strong identity for each tower, and a job for every node in its tree. Started 2026-10-20 from
two `TODO.md` entries (the Sniper's specials, the Cinder's fuel line and weakest last nodes). Not
reviewed yet.

## How to use it

- `[x]` = keep it, `[ ]` = no, or not yet. Write anything after a 💬.
- ⭐ = my pick where options compete. Status: ✅ settled · ❌ dropped · ❔ needs input.
- Cost tags: 🟢 numbers or data on a node · 🟡 one perk or rule changed · 🔴 a new mechanic.
- 🧪 = measured on the experiment branch (`experiment/sniper-scorcher-identity`, uncommitted).

## Where we are

- Baselines and two rounds of experiments are measured (below). The round-2 Sniper numbers and the
  round-1 Scorcher numbers are adopted on `main` (see Decisions so far); the ❔ items stay open.
- Next session: review the ❔ items in this order: Sniper identity (S1), then S2 to S5, then
  the Scorcher (C1 to C4). Then turn the ticked items into a feature request and tune the numbers
  with `td.BuildHarness`.

## Decisions so far

- ✅ Adopted as tested, 2026-10-20: Steady Aim never resets (kept across targets and kills),
  Marksman's Eye +20% damage a stack, Headhunter +75% vs Elite and up, Ricochet also on a kill,
  Hollow Point on every hit, Shatter Shot always crits a stopped enemy for +1.0 crit damage, the
  three Marksman's Eye specials keep the default targeting; Flashpoint +50% crit and 8 Scorched,
  Inferno Ring without its range cut, Pyromancer's Mark +40%.
- ✅ Fifth Shot fires every 4th shot and is renamed **Fourth Shot** (+1.5 crit damage).
- ✅ Unbroken Aim's survive-a-kill rule is gone, since nothing resets Steady Aim on a retarget or a
  kill; the node is +2 stacks.
- ✅ Steady Aim fades instead of S1-A's idle reset: a stack lasts 3 s after the last shot, then one
  goes, and the next 3 s another. It counts time since the last shot, so a slowed or jammed Sniper
  bleeds stacks mid-fight. Added after round 2, so its numbers are slightly off for this rule.

## How it was measured

`td.BuildHarness <level> 40 <tower> 4 2`: four copies, all bought the same build, among two of
every other tower, 40 seeds. **Z** = Zigzag Path, **H** = Twisted Hourglass, in lives saved
against four plain copies (Z loses 38.9, H about 60). About one life is noise. Curly Path was
tried and dropped: the Warden's leak costs about 1,000 lives in every build and hides the rest.
Neither measured level sends healers, shields or much armour, so a node that answers them reads
as zero here. That is a gap in the levels, not proof the node is useless.

---

## Sniper

### What the numbers say today

| Node or build                          | Z     | H     | What it shows |
|----------------------------------------|-------|-------|---------------|
| Attune (Steady Aim)                    | 0.0   | -0.3  | the "patience" identity pays nothing |
| Marksman's Eye I / II / III            | 0.3 / 3.4 / 3.4 | 0.0 / 3.1 / 3.1 | a head that adds no damage |
| Unbroken Aim / Sunder Rounds           | 3.6 / 3.4 | 5.0 / 3.1 | last nodes worth a third of the other head's |
| Focused Optics II / III                | 8.1 / 12.3 | 7.9 / 10.8 | +28 flat damage on a base of 40 |
| Railgun / Executioner                  | 20.3 / 16.3 | 17.5 / 12.9 | |
| Railgun + Momentum                     | 30.4  | 30.2  | the best build by far |
| Hollow Point / Fifth Shot / Shatter Shot | -2.0 / -1.8 / -1.2 | -3.1 / -3.8 / -4.0 | lose lives |
| Ricochet / Headhunter                  | 3.6 / 2.8 | 2.4 / 1.9 | |

**Why the patience identity fails:** a shot every 2.5 s rarely fires twice in a row at the same
enemy, so Steady Aim stacks reset before they build. Only Unbroken Aim, which keeps them through a
kill, raised damage (+40%). **Why the Marksman's Eye specials lose lives:** they switch
targeting to "most health" or "stopped first", which costs more leaks at the front than their
crit-only payoff earns back.

### S1. The Sniper's identity ❔

- [ ] ⭐ 🟡 **A: The rhythm marksman.** It hits the one enemy that matters, and the longer it
  keeps firing, the harder each shot lands. Steady Aim belongs to the tower's rhythm, not to one
  target: it is kept on retarget and lost only when the Sniper goes idle (say 2 s without a shot)
  or between waves. The two heads are two ways to spend the rhythm: Focused Optics fires *faster*
  (Quickdraw), Marksman's Eye hits *harder* per stack (Deadeye).
  - 🧪 Steady Aim never reset on retarget, Marksman's Eye +20% damage per stack, and the three
    specials kept the default targeting (round 2). Marksman's Eye I: 0.3 → **5.6** Z, 0.0 →
    **5.0** H. II: 3.4 → **9.5** Z, 3.1 → **8.6** H. Unbroken Aim: 3.6 → **13.4** Z, 5.0 →
    **12.3** H. Focused Optics also gained (FO II 8.1 → 10.1 Z), so the gap closes but stays.
  - 💬
- [ ] 🟡 **B: The patient hunter, kept per target.** Keep stacks per enemy, but make them build
  from time aimed rather than shots fired (a stack each second the turret holds the target), so
  one 2.5 s cooldown still builds two. Keeps the original wording. Costs a timer in `AimFocus` and
  no longer suits a fast Focused Optics Sniper.
  - 💬

### S2. Marksman's Eye: a head worth taking ❔

- [ ] ⭐ 🟢 **A: Damage per Steady Aim stack.** Marksman's Eye I adds +20% damage per stack
  (🧪 above). Next to try: +30%, so the head's best build reaches about Focused Optics' (now 17
  against 32 lives on Z).
  - 💬
- [ ] 🟢 **B: Flat damage like Focused Optics, crit on top.** +12 damage on II and III, keep the
  rest. Simple, but both heads then grow the same way and lose their difference.
  - 💬

### S3. Unbroken Aim under the rhythm rule ❔

With S1-A, "Steady Aim survives a kill" says nothing new. Its +2 stack cap is what saved lives.

- [ ] ⭐ 🟡 **A: The rhythm never breaks.** Steady Aim is never lost to idling or a new wave, and
  the cap goes up by 2. The Deadeye arrives at a wave already at full stacks.
  - 💬
- [ ] 🟡 **B: A kill feeds the rhythm.** Each kill adds a stack at once, past the cap, until the
  next miss or dodge. Rewards a Sniper that keeps finishing things.
  - 💬

### S4. The specials: one job each ❔

The pattern I propose: Focused Optics' three specials are about *what* it kills (bosses, crowds,
chains of kills); Marksman's Eye's three are about *how hard* a held shot lands, and none of them
changes targeting.

| Special      | Job | 🧪 Round 2 (Z / H) | Change tested |
|--------------|-----|--------------------|---------------|
| Momentum     | chain kills: a crit charges a huge next shot | 17.6 / 11.0 (with FO I) | none |
| Headhunter   | bosses and Elites | 9.3 / 6.0 | +40% → +75% damage against Elite and up |
| Ricochet     | crowds | 6.7 / 4.4 | also bounces on a kill, not only on a crit |
| Hollow Point | marks one enemy for the whole defence | 8.5 / 8.0 | Vulnerable on every hit, not only a crit; no targeting change |
| Fifth Shot   | a sure big shot | 8.5 / 6.6 | every 4th shot instead of 5th, +1.5 crit damage instead of +0.5; no targeting change |
| Shatter Shot | partner of freezes and dazes | 7.1 / 5.1 | always crits a stopped enemy, +1.0 crit damage; no targeting change |

Round 2 changed all of these together with S1-A, so how much is the special and how much is the
rhythm is not separated yet. On top of Unbroken Aim, Hollow Point adds 3.8 Z / 3.9 H, Fifth Shot
4.4 / 1.5 and Shatter Shot 1.8 / -0.1.

- [ ] ⭐ 🟢 **A: Keep the six, tune toward one band.** Trim Momentum's charged shot (x3 → x2.5),
  raise Ricochet's bounce share (60% → 80%), keep the rest as tested. Check each against Momentum
  on the same head.
  - 💬
- [ ] 🟡 **B: Move Shatter Shot to Focused Optics, Headhunter to Marksman's Eye.** A held shot
  on a boss suits the Deadeye. A quick crit on whatever stopped suits Quickdraw. Same numbers.
  - 💬

### S5. The rest of the tree ❔

Still worth nothing measurable after round 2: Attune on its own (0.5 / 0.3), the Tradecraft line
(Tradecraft 0.5 / 0.6, II 1.3 / 1.4, Silver Rounds 2.5 / 0.8, Spotter Uplink 2.5 / 0.8), Marksman's
Eye III (no shields on these levels) and Sunder Rounds (no armoured waves).

- [ ] ⭐ 🟡 **A: Tradecraft becomes the long-range line.** It already rewards far shots (II).
  Make it the rhythm's reach: Tradecraft adds a Steady Aim stack for a shot past two-thirds of the
  range, Silver Rounds keeps its magic shot, Spotter Uplink stays. Judge Marksman's Eye III and
  Sunder Rounds only once a level sends shields and armour.
  - 💬
- [ ] 🟢 **B: Leave the line, cut its prices.** It is a cheap side line that pays a little. A
  price that matches what it saves is enough meaning for an extra line.
  - 💬

---

## Scorcher (`CinderTower`)

### What the numbers say today

| Node or build                    | Z     | H     |
|----------------------------------|-------|-------|
| White Flame II / Soulfire        | 7.7 / 16.6 | 6.5 / 12.7 |
| Wide Nozzle II / Lingering Flames | 9.0 / 14.4 | 9.0 / 13.6 |
| Combustion / Flashpoint          | 28.4 / 23.4 | 17.5 / 15.3 |
| Dragon's Breath / Inferno Ring   | 25.7 / 21.3 | 24.3 / 17.2 |
| Searing Flame / Pyromancer's Mark / Thermal Shock | 8.7 / 3.2 / 2.4 | 6.4 / 4.4 / 7.6 |
| Kindling / Cauterize / Heat / Everburn | 0.3 / 0.3 / 1.4 / 3.2 | 1.0 / 1.0 / 3.8 / 5.1 |

Unlike the Sniper, both heads and all four last nodes pay. The identity problems are smaller:
the fuel line has no job you can see, and Flashpoint and Inferno Ring trail their partners.
Round 1 tried number changes only and moved little:

- 🧪 Inferno Ring without its range cut: 21.3 → 22.1 Z, 17.2 → 19.1 H.
- 🧪 Flashpoint at +50% crit chance and 8 Scorched instead of 3: 23.4 → 25.1 Z, 15.3 → 15.9 H.
- 🧪 Pyromancer's Mark at +40% magic taken instead of +25%: 3.2 → 5.0 Z, 4.4 → 5.2 H.

### C1. The Scorcher's identity ❔

- [ ] ⭐ 🟢 **A: The tower that sets the board up.** Everything it touches burns, and a burning
  enemy is worse off against everyone. White Flame makes one fire hotter (depth), Wide Nozzle sets
  more of the lane alight (breadth), the fuel line is what a burning enemy loses, and each
  special names a partner: Searing Flame every tower, Thermal Shock the freezers, Pyromancer's
  Mark the magic towers.
  - 💬
- [ ] 🟢 **B: A pure damage-over-time carry.** Drop the support reading: the fuel line becomes
  more burn for the Scorcher itself (longer pools, a higher cap). Easier to tune, but it overlaps
  the White Flame head.
  - 💬

### C2. The fuel line: a job you can see ❔

Kindling, Cauterize, Heat and Everburn save 0 to 5 lives, including on Curly Path's Menders.

- [ ] ⭐ 🔴 **A: The lane-wide answer to sustain.** This is the `TODO.md` gap "no tower denies
  heals or shields along a whole lane". Cauterize stops healing outright while a pool burns, not
  -50%. Everburn holds a pool at its floor anywhere on the path, not only in range, so a burning
  Mender stays Cauterized all the way down the lane. Kindling and Heat stay as the line's damage.
  Measure with `td.balance.WaveCheckReport --checks 12:sustain` on Curly Path.
  - 💬
- [ ] 🟡 **B: The team-amplifier line.** Fold Pyromancer's Mark into it (Heat → magic taken), so
  the line is "burning enemies take more from everyone", and give the special slot a new third
  option. Shows up in any mixed defence, but leaves the sustain gap open.
  - 💬

### C3. Flashpoint ❔

The Scorcher rarely crits, so a crit-ignition capstone has little to work with. Round 1's bigger
numbers added only 0.6 to 1.7.

- [ ] ⭐ 🔴 **A: Flashpoint, the detonation.** When Stoke is full, the next ignition sets off
  what is left of the enemy's pools at once, on that enemy alone. White Flame's depth becomes a
  single-target finisher, and Combustion stays the spreading one.
  - 💬
- [ ] 🟡 **B: Flashpoint feeds itself.** A critical ignition adds Scorched, and Scorched lowers
  resilience, so the enemy's next ignitions crit more and harder. It is the same snowball, made
  steep enough to matter: about 10% crit chance per 10 Scorched from this tower.
  - 💬

### C4. Inferno Ring ❔

- [ ] ⭐ 🟢 **A: No range cut.** A full ring is already the cost: it cannot reach far down a lane.
  🧪 +0.8 Z / +1.9 H.
  - 💬
- [ ] 🟡 **B: The ring pulses.** It fires at twice the rate for 60% of the damage, so a tower
  inside a loop of path keeps every side burning.
  - 💬

---

## Decisions so far

None yet.

## Appendix: the experiment branch

`experiment/sniper-scorcher-identity` holds round 1 and round 2 as uncommitted changes to
`SniperTower`, `AimFocus` (a constant switch that stops the reset on retarget), `RicochetPerk`,
`CinderTower`, `FlashpointPerk` and `PyromancersMarkPerk`. They are measurement hacks, not the
shape a real change would take: the experiment never adds the idle reset S1-A needs, and it
leaves Unbroken Aim's perk saying something that no longer matters.
