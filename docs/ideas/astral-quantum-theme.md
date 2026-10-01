# Theme Brainstorm: Astral & Quantum

A proposal to give jTD a character: a science theme at the two extreme scales, the **astral**
(stars, gravity, light-years) and the **quantum** (particles, probability, entanglement), leaning
quantum. It renames everything the player reads, recolours a little, reshapes a few symbols, and
changes no rules. Nothing here is decided.

**How to use it** (the same conventions as `towers-brainstorm.md`)

- `[x]` = keep it, `[ ]` = no, or not yet. Write anything after a 💬.
- ⭐ = my pick where options compete.
- Cost tags: 🟢 text or a constant · 🟡 one small new piece (a shape, a mark, a rule row) · 🔴 a
  new system.
- ☉ = astral and ψ = quantum, used throughout as the two scales' marks.
- In a hurry: sections 1, 2, 12, 13 and 17 (every decision in one list).

Contents: 1 The pitch · 2 Ground rules · 3 Setting and frame · 4 The two scales as a design grammar
· 5 Towers · 6 Upgrade trees · 7 Damage, stats and effects · 8 HUD, menus and levels · 9 Enemies
(draft) · 10 Visuals and UI · 11 Synergy mechanics (optional) · 12 Impact assessment · 13 For the
better, for the worse · 14 Iteration 2 and the towers brainstorm · 15 Adoption tiers · 16 Word bank
· 17 Decisions

---

## 1. The pitch

**Thesis: astral on the board, quantum in the rules.**

What the player *sees* (towers, shells, the boss) are cosmic objects with silhouettes everyone
knows: a comet, a pulsar, a nova. What *happens* (effects, stats, upgrades) is told in quantum
words, because quantum words describe *states* (superposed, observed, entangled, coherent,
unstable), and states are what rules are made of. The game leans quantum where the player reads
and astral where the player looks. The two scales meet where physics says they meet: at a black
hole, the one place where both matter at once.

**The fiction**, in four sentences. This is the whole lore budget; nothing in the game needs more.

> At the edge of a black hole, the very large and the very small stop being separate. Your lab
> keeps a quantum core there, and anomalies pour out of the horizon toward it: particles that
> shouldn't exist and the debris of dying stars. Every anomaly that reaches the core decoheres one
> of its qubits; lose them all and the computation is gone. You hold the line with instruments
> that harness both scales.

**Why this theme fits jTD unusually well.** Most themes would be paint. This one names mechanics
the game already has:

| Already in the game                                                                    | Already is physics                                                                                                      |
|----------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| Two damage types, and Elite armor that resists whichever has landed most this level    | A duality: mix the two scales or the rift adapts                                                                        |
| Invisible: towers can't target it, area damage still reaches it; Revealed undoes it    | Superposition and observation: no definite position until measured, yet a field across the whole region still touches it |
| Chill slows and adds up; freezing a chilled enemy lasts longer by the chill's level    | Time dilation deepening into stasis, like falling toward a horizon                                                      |
| Sonar's beam sweeps around once every 2 s                                               | A pulsar, exactly                                                                                                       |
| Burn and poison are separate damage-over-time pools that stack                          | Two kinds of slow damage: stellar heat and radiation                                                                    |
| Waves                                                                                   | Already a physics word; it stays                                                                                        |
| The Warden dies into an egg that hatches a weaker Warden, twice                         | A star's life: red giant, remnant, white dwarf, black dwarf                                                             |
| Reaver splits into two Simple mobs on death                                             | A meson is a pair of quarks                                                                                             |
| Black board, phosphor-green self-painted HUD                                            | Already reads as an instrument console                                                                                  |

It is also cheap. Board art is vector code keyed by colour roles in one class, every control
paints itself from one `Hud` palette, and effect and stat labels each live in one switch. The
theme is mostly words.

💬

---

## 2. Ground rules for the theme

- [ ] ⭐ **Rename what belongs to another genre; keep what already fits science.** Fantasy and
  military words go: Spirit, Hex, Curse, Aura, Awaken, Transcendent, Warden, Mender, Ghost, Grunt,
  Siege, Marksman, Warhead, Withering, Warding, Toxic Bloom, "mob". Science-ready words stay: Wave,
  Burning, Scorched, Shield, Plating, Resilience, Momentum, Range, Crit, Resonance. Fewer renames,
  same character.
  - 💬
- [ ] ⭐ **Clarity beats cleverness.** A themed effect keeps its plain meaning one click away: the
  inspector says "Dilated: slowed 40%", "Superposed: towers can't target it". Marker colours and
  glyphs don't change, so a returning player's eye still works.
  - 💬
- [ ] ⭐ **One word, one meaning.** The good quantum words are few (superposition, entanglement,
  coherence, observation, tunneling). Each is spent once, on the most visible thing it fits.
  Section 16 lists the words still unspent.
  - 💬
- [ ] ⭐ **Astral upgrades make a tower bigger; quantum upgrades make it stranger.** ☉ = more mass,
  reach, heat, area, raw power. ψ = chance, states, splitting in two, seeing the unseen, going
  through things. This is the lens for every node iteration 2 still has to write (section 4).
  - 💬
- [ ] ⭐ **One line of flavour at most.** No fake-science paragraphs. A name and its rule do the
  work, and poetic licence is fine.
  - 💬
- [ ] **Two marks, two hues.** ☉ warm (amber), ψ cool (violet). Used for meaning only (damage
  type, a chain's scale), never to colour every object.
  - 💬

---

## 3. Setting and frame

| Today                                | ⭐ Themed                            | Alternatives                      | Why                                                                                    |
|--------------------------------------|-------------------------------------|-----------------------------------|----------------------------------------------------------------------------------------|
| "Tower Defense" (window, title)      | **Quantum Horizon**                 | Planck & Parsec; Event Horizon; Rift | quantum first (the lean), horizon for the black hole. "Planck & Parsec" names the two extremes, but it's harder to say |
| enemies, "mob"                       | **anomalies**                       |                                   | neither scale owns the word                                                            |
| Lives                                | **Qubits**                          | Containment, Integrity            | a count that reads naturally ("Qubits: 5"); quantum computing in one word              |
| Cash / credits, `$35`                | **Energy**, written `35 eV`         | Quanta (`ħ 35`)                   | a destroyed anomaly releases its energy; `eV` reads as a unit like `$`                 |
| Score                                | **Data**                            | keep Score                        | the lab collects observations                                                          |
| Bounty                               | **Yield**                           |                                   | an anomaly's energy yield                                                              |
| Sell                                 | **Recycle**                         | Dismantle                         | energy comes back                                                                      |
| "Killed" (inspector)                 | **Annihilated**                     | Collapsed                         |                                                                                        |
| "Leaked"                             | **Breached**                        | Reached core                      |                                                                                        |
| "Game Over!"                         | **Decoherence**                     | Core lost                         | all qubits gone                                                                        |
| "Congratulations!"                   | **Horizon held**                    | Rift sealed                       | echoes the title                                                                       |
| Wave                                 | **Wave** (keep)                     |                                   | already physics                                                                        |
| tower (the generic word)             | **tower** (keep)                    | instrument                        | genre clarity: "Quasar tower" says what you're buying                                  |
| level                                | **sector** on the title screen      | keep level                        |                                                                                        |

- [ ] ⭐ Keep the word "tower". The shop and panels say "Quasar tower"; only the fiction calls them
  instruments.
  - 💬

---

## 4. The two scales as a design grammar

|                 | ☉ Astral, the very large                              | ψ Quantum, the very small                                         |
|-----------------|-------------------------------------------------------|-------------------------------------------------------------------|
| Feels like      | mass, gravity, heat, distance, time                   | chance, states, links, barriers that aren't there                 |
| Upgrades do     | more damage, area, range and heat; slow and stop      | crit, two-at-once, reveal, through armor and shields, links       |
| Damage type     | Kinetic                                               | Phase                                                             |
| Damage over time| Burning (stellar heat), leaving Scorched              | Irradiated (decay), leaving Decohering                            |
| Control         | Dilated, then Stasis                                  | Superposed and Observed; Unstable                                 |
| Name sources    | celestial objects and astronomy                       | particles, quantum effects, quantum computing                     |

**Every tower picks a scale.** Each tower's two exclusive head chains already split cleanly, one ☉
and one ψ (table in 6.2). Choosing a chain becomes choosing a scale, and the panel can say so with a
mark. Nothing in the rules changes.

- [ ] ⭐ 🟢 Head chain A / B = ☉ / ψ on every tower, marked in the upgrade panel.
  - 💬
- [ ] 🟡 The tower shows its chosen scale on the board too. Head pips are gold and special pips
  violet today, so either the slot colours change or the scale shows only in the panel. ⭐ panel
  only.
  - 💬
- [ ] ⭐ Iteration 2's **extra head node** (exclusive with nothing) is where a tower borrows from
  its *other* scale, for example a kinetic Quasar's Phase Rounds (every Nth shot deals phase). It
  carries both marks.
  - 💬
- [ ] ⭐ **Transcendent becomes Duality**: the tower holds two specials at once. Iteration 2 wants
  the two to affect each other; the theme calls each special pairing an **interference pattern**.
  - 💬

---

## 5. Towers

| Today  | ⭐ Themed        | Scale | Why it fits                                                                                                                                                       | Symbol (one closed shape)                       | Alternatives                                                                   |
|--------|-----------------|-------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------|--------------------------------------------------------------------------------|
| Sniper | **Quasar**      | ☉     | a quasar's jet is a narrow beam across the universe: long range, one target, huge hits. Its hotkey is already `q`                                                    | a dot with two opposite thin jets               | Photon ψ, Laser ψ, Railgun                                                     |
| Splash | **Nova**        | ☉     | a sudden burst of light at a point, cheap and frequent                                                                                                             | a four-point sparkle                            | Positron ψ (an annihilation burst; fits if Splash turns phase), Scatter ψ      |
| Sonar  | **Pulsar**      | ☉     | a spinning neutron star sweeping a beam: the tower's exact mechanic. Its "Rotation" stat becomes "Period"                                                          | a small disc; the sweeping head is the beam     | Interferometer, Lighthouse                                                     |
| Pulse  | **Singularity** | ☉ψ    | a tiny black hole that hurts everything near it, the unseen too. The brainstorm's Pulse ideas (undertow, pull, debuffs lasting longer inside, Event Horizon) are already black-hole physics | a thin bright ring around a dark centre         | Collider ψ, Cyclotron ψ                                                        |
| Aura   | **Entangler**   | ψ     | links nearby towers so they act as one; the faint lines it draws already look like entanglement                                                                    | two interlocked rings                           | Beacon ☉, Lattice                                                              |
| Mortar | **Comet**       | ☉     | a slow lob with a tail; comets are ice, so its chill explains itself                                                                                               | a disc with a tapered tail; the shell matches   | Meteor, Mass Driver                                                            |
| Seeker | **Tachyon**     | ψ     | a particle from the future: it can't miss, and where it lands time stops                                                                                           | keep the kite                                   | Probe, Positron                                                                |
| Cinder | **Flare**       | ☉     | a solar flare is a cone of burning plasma                                                                                                                          | keep the flame                                  | Corona                                                                         |

- [ ] ⭐ **The roster above.** On the board it leans astral (five ☉, two ψ, one both), by design:
  the rules carry the quantum lean.
  - 💬
- [ ] **A quantum-leaning roster**: Photon (Sniper), Positron (Splash), Pulsar, Collider (Pulse),
  Entangler, Comet, Tachyon, Flare. Four ψ. The trade: particles have no silhouette (a photon is a
  dot), so the board's shapes say less.
  - 💬

**Names against damage types.** Kinetic towers named for matter (Quasar's jet, Nova's shock,
Comet) and phase towers named for fields and exotic particles (Singularity, Tachyon) read right.
Three to watch:

- Pulsar deals kinetic damage while a real pulsar beams radiation. Fine as licence, or one more
  input to the brainstorm's damage-type review (1.3).
- Nova or Positron hangs on Splash's damage type: kinetic, Nova; phase (the brainstorm's "Arcane
  Splash", 3.2 B), Positron.
- Flare deals phase, as it should: quanta were discovered by studying the light of hot bodies.

**Pulsar and Quasar sound alike.** They sit on `e` and `q` and behave oppositely (one sweeps, one
aims), which helps, but it's a small recognition cost. 💬

**Tower sheet rows**: Physical damage → **Kinetic damage**; Magic damage → **Phase damage**;
Rotation → **Period**; Splash radius → **Blast radius**. Range, Fire rate, Crit chance, Targets,
Kills and Damage dealt stay.

---

## 6. Upgrade trees

32 names ship today: 2 base nodes, 16 head chains, 14 specials. Every one is renamed or
deliberately kept below.

### 6.1 The base slot

| Today                                  | ⭐ Themed          | Scale | Why                                                                                         |
|----------------------------------------|-------------------|-------|---------------------------------------------------------------------------------------------|
| Range (and iteration 2's II and III)   | **Aperture I-III** | ☉     | a bigger telescope sees further. The stat itself stays "Range"                              |
| Fortify (working name)                 | **Grounding**     | ψ     | grounds out interference: its brainstorm job is jam-proofing, and the Jammer becomes a Magnetar |
| Awaken                                 | **Excitation**    | ψ     | lifts the tower to a higher energy level; unlocks head III and the special                  |
| Transcendent                           | **Duality**       | ☉ψ    | two natures at once: the second special and head IV                                         |

Ground, excited, dual reads as one ladder. It also settles the brainstorm's complaint (9.5) that the
chain mixed an engineering word with two spiritual ones.

- [ ] ⭐ Aperture · Grounding → Excitation → Duality.
  - 💬
- [ ] Shielding → Coherence → Superposition. Clearer as quantum computing (shield the qubit, make
  it coherent, superpose it), but "Superposition" collides with Superposed (7.3) and "Coherence"
  with the stat (7.2).
  - 💬
- [ ] Slot names BASE / HEAD / SPECIAL: ⭐ keep (structural words that suit any theme), or SPECIAL
  → Anomaly.
  - 💬

### 6.2 Head chains: one ☉, one ψ per tower

| Tower       | ☉ chain (today → themed)                                      | ψ chain (today → themed)                                                      |
|-------------|---------------------------------------------------------------|-------------------------------------------------------------------------------|
| Quasar      | Focused Optics → **Lensing**                                  | Marksman's Eye → **Amplitude** (a crit is a probability)                      |
| Nova        | Blast Engineering → **Shockfront**                            | Rapid Battery → **Cascade** (a particle shower)                               |
| Pulsar      | Long Reach → **Parallax** (how astronomers measure distance)  | Twin Array → **Beam Splitter** (one beam, two paths)                          |
| Singularity | Overcharged Coils → **Accretion**                             | Resonant Field → **Observer Effect** (touches and reveals the superposed)     |
| Entangler   | Resonance Field → **Orbit**                                   | Amplifying Core → **Coupling**                                                |
| Comet       | Siege Rounds → **Impactor**                                   | Fragmentation Rounds → **Fission**                                            |
| Tachyon     | Twin Warhead → **Binary**                                     | Deep Freeze → **Zeno Lock** (a system watched closely can't change)           |
| Flare       | White Flame → **Fusion**                                      | Wide Nozzle → **Dispersion** (a wave packet spreads out)                      |

Every pair fits its lens with no content change: the ☉ chains are raw power, area, reach and heat;
the ψ chains are crit, reveal, stasis, splitting and spreading. It also ends the Resonant Field /
Resonance Field near-duplicate (brainstorm 9.5).

Still true after renaming: some level Is don't deliver their name (Beam Splitter I is +25% damage,
Binary I is fire rate, Zeno Lock I is damage). That is the content fix in brainstorm 9.4, not a
naming one; the themed names are vaguer, so they promise less.

- [ ] ⭐ The table above.
  - 💬

### 6.3 Specials

| Tower       | Today            | ⭐ Themed              | Why                                                                     |
|-------------|------------------|-----------------------|-------------------------------------------------------------------------|
| Quasar      | Marked Round     | **Destabilizer**      | applies Unstable; frees "mark" for the brainstorm's Marked effect        |
|             | Fifth Shot       | **Fifth Harmonic**    | keeps the "fifth" that explains it                                      |
|             | Momentum         | **Momentum** (keep)   | already physics                                                         |
| Nova        | Toxic Bloom      | **Fallout**           | applies Irradiated                                                      |
|             | Concussive Blast | **Annihilation**      | kills explode                                                           |
|             | Overpressure     | **Gamma Burst**       | after a crit, the next shot hits everything in range                    |
| Pulsar      | Wide Band        | **Sky Survey**        | each revolution observes every superposed anomaly it passes             |
|             | Mark on Sweep    | **Collapse**          | collapses chance into certainty: the next hit crits                     |
|             | Piercing Tone    | **Tunneling**         | its bonus goes through armor and shields                                |
| Singularity | Warding Field    | **Tidal Stress**      | applies Unstable, and no longer sounds protective                       |
| Entangler   | Withering Field  | **Instability Field** | enemies in range turn Unstable                                          |
| Comet       | Cursed Shrapnel  | **Volatile Core**     | comets are made of volatiles                                            |
| Tachyon     | Homing Curse     | **Paradox**           |                                                                         |
| Flare       | Hexflame         | **Magnetic Storm**    |                                                                         |

The six Vulnerable-applying specials lose their hex, curse and mark words (brainstorm 9.5). They
still all apply the same debuff: the theme renames the duplication in brainstorm 1.4, it doesn't
fix it. Its payload swaps get themed names in section 14.

- [ ] ⭐ The table above.
  - 💬

---

## 7. Damage, stats and effects

### 7.1 Damage types

| Today    | ⭐ Themed    | Scale | Reads as                                  |
|----------|-------------|-------|-------------------------------------------|
| Physical | **Kinetic** | ☉     | mass and momentum; plating stops it       |
| Magic    | **Phase**   | ψ     | phases through plating                    |

- [ ] ⭐ Kinetic / Phase.
  - 💬
- [ ] Kinetic / Quantum: more on the nose, but "quantum" would then mean a damage type *and* a
  chain's scale.
  - 💬
- [ ] Particle / Wave: the famous duality, but "wave damage" collides with enemy waves.
  - 💬
- [ ] Kinetic / Energy: plain sci-fi, but collides with Energy the currency.
  - 💬

Adaptive elite armor is the theme's synergy rule, already shipped: *Elite anomalies adapt to
whichever of kinetic and phase has landed most, so mix the scales.* Only its text changes. The
`armored` spawn shape keeps its wave-script keyword; its description says kinetic.

### 7.2 Enemy stats in the inspector

| Today                                         | ⭐ Themed                      | Note                                                                     |
|-----------------------------------------------|-------------------------------|--------------------------------------------------------------------------|
| Resist physical / magic, Weak to ...          | Resist kinetic / phase        | follows the damage words                                                 |
| Plating, physical / magic                     | Plating, kinetic / phase      |                                                                          |
| Spirit                                        | **Coherence**                 | scales heals and shields and how fast stacks wear off; at -100 an anomaly is fully decohered |
| Stealthed                                     | **Superposed**                |                                                                          |
| Resilience, Speed, Regenerates, Crit chance taken | keep                      | neutral words                                                            |

### 7.3 Effects

| Today     | Kind        | ⭐ Themed            | Scale | The inspector's plain line                                  |
|-----------|-------------|---------------------|-------|-------------------------------------------------------------|
| Chilled   | soft CC     | **Dilated**         | ☉     | slowed by its depth, never past 80%                          |
| Freeze    | hard CC     | **Stasis**          | ☉     | stopped. The ice-crystal art stays: cryostasis              |
| Burning   | DoT         | **Burning** (keep)  | ☉     | stars burn                                                  |
| Poisoned  | DoT         | **Irradiated**      | ψ     | its own pool; slows a little                                |
| Scorched  | debuff      | **Scorched** (keep) | ☉     | resilience down                                             |
| Sickened  | debuff      | **Decohering**      | ψ     | coherence down                                              |
| Vulnerable| debuff      | **Unstable**        | ψ     | takes more damage                                           |
| Shield    | restorative | **Shielded** (keep) |       |                                                             |
| Heal      | restorative | **Restoring**       |       | anomalies don't heal, they restore                          |
| Invisible | stealth     | **Superposed**      | ψ     | towers can't target it; area damage still reaches it        |
| Revealed  | stealth     | **Observed**        | ψ     | towers can target it again                                  |

The existing rules read as physics once renamed:

- Stasis on a Dilated anomaly uses the dilation up and lasts longer by it: time slowing until it
  stops.
- Dilation cuts burning damage: slower time, slower fire.
- Stasis puts the fire out but keeps Scorched.
- Burning (heat, ☉) and Irradiated (decay, ψ) are separate pools that stack: one fire for each
  scale.
- Superposed anomalies dodge every aimed shot but not a field over the whole region.

- [ ] ⭐ Time words: Dilated and Stasis.
  - 💬
- [ ] Cold words: keep Chilled and Frozen. The most legible, and Comet's chill fits them, but it
  loses the slow-then-stop story.
  - 💬

---

## 8. HUD, menus and levels

| Where                    | Today                                              | ⭐ Themed                                                         |
|--------------------------|----------------------------------------------------|------------------------------------------------------------------|
| Window title             | Tower Defense v1.4                                 | Quantum Horizon v1.4                                             |
| Title screen             | TOWER DEFENSE · SELECT YOUR LEVEL · CLICK TO DEPLOY | QUANTUM HORIZON · CHOOSE A SECTOR · CLICK TO DEPLOY (keep)       |
| Level card               | WAVES · CREDITS · LIVES                            | WAVES · ENERGY · QUBITS                                          |
| Status panel             | Status: Wave, Lives, Score, Cash                   | Telemetry: Wave, Qubits, Data, Energy                            |
| Speed panel              | Speed: ► ►► ►►►                                    | **Time**: ► ►► ►►► (the player literally dilates it)             |
| Info panel               | Info                                               | Readout                                                          |
| Shop / upgrades / waves  | Towers · Upgrades · Current & Next Wave            | keep                                                             |
| Sell button              | Sell ( $11 )                                       | Recycle (11 eV)                                                  |
| Every price              | $35                                                | 35 eV                                                            |
| Overlays                 | Game Over! · Congratulations! · Back to menu       | Decoherence · Horizon held · Back to menu                        |

**Levels**

| Today             | ⭐ Themed        | Scale | Why                                                                                            |
|-------------------|-----------------|-------|------------------------------------------------------------------------------------------------|
| Curly Path        | **Spiral Arm**  | ☉     | a lane that spirals through two tight loops: a galaxy's arm                                    |
| Zigzag Path       | **Double Slit** | ψ     | two lanes, one crossing the other twice: the experiment where one particle takes both paths    |
| Twisted Hourglass | **Wormhole**    | ☉ψ    | three lanes through a pinched waist: the textbook drawing of a wormhole *is* an hourglass      |

- [ ] ⭐ The three level names.
  - 💬
- [ ] Level descriptions call lanes **worldlines**.
  - 💬

---

## 9. Enemies (a draft, for the enemy iteration)

Enemies are yours to redo after the towers, so this is only the grammar and a first mapping, good
enough that a renamed game isn't left with "Simple mob" in it. **Grammar:** ψ particles for plain,
small or strange anomalies; ☉ bodies for heavy, big or supporting ones; the boss is a star's life.

| Today                                         | Draft                                            | Scale | Why                                                              |
|-----------------------------------------------|--------------------------------------------------|-------|------------------------------------------------------------------|
| Simple                                        | **Quark**                                        | ψ     | the basic building block                                         |
| Armored                                       | **Asteroid**                                     | ☉     | rock: armor, and no weak spot to crit                             |
| Frenzied                                      | **Meteoroid**                                    | ☉     | falls faster as it burns away                                    |
| Frenzy Spawnling                              | Shard                                            | ☉     |                                                                  |
| Ghost                                         | **Neutrino**                                     | ψ     | physicists' nickname for it is "the ghost particle"              |
| Mender                                        | **Nebula**                                       | ☉     | a stellar nursery restores what's near it                        |
| Jammer                                        | **Magnetar**                                     | ☉     | a magnetic field that wrecks instruments; Grounding answers it   |
| The Warden / Weakened / Exhausted, and its egg | **Red Giant / White Dwarf / Black Dwarf**, egg **Remnant** | ☉ | a dying star leaves a core that reignites smaller                |
| Reaver                                        | **Meson**                                        | ψ     | a quark pair: splitting into two Quarks is its exact ability     |
| Empty (spacer)                                | **Vacuum**                                       |       |                                                                  |

- [ ] ⭐ Rename enemies with these drafts in the same pass as everything else, so no "mob" survives,
  and redo them in the enemy iteration.
  - 💬
- [ ] Ranks Grunt / Soldier / Veteran / Elite / Boss → Trace / Stable / Heavy / Exotic / Prime. The
  Boss badge's skull → a horizon mark (a dark disc with a bright rim).
  - 💬

**The brainstorm's enemy ideas (section 5), themed** — they fall into the grammar easily: Blinker
(teleports forward when hit) → **Tunneler** ψ · Burrower (untargetable, hurt only by fields and
ground effects) → **Dark Matter** ☉ · Priest (cleanses allies' debuffs) → **Corrector** ψ (quantum
error correction removes errors) · Juggernaut (huge plating) → **Neutron Star** ☉ · Mites → **Quark
swarm** · Courier → **Photon** · Shieldbearer → **Magnetosphere** · Drummer (haste aura) →
**Accelerator** · Salamander (burn-immune) → **Sunspot** · Yeti (freeze-immune) → **Ice Giant** ·
Necromancer → **Recombiner** · Mirror (resists the last type it took) → **Antiparticle** · Elite
affixes → **exotic properties**.

**New enemy ideas the theme suggests**

- [ ] 🟡 **Entangled pair**: two anomalies sharing one health pool; damage to either drains both.
  - 💬
- [ ] 🟡 **Virtual pair**: two anomalies pop into existence mid-path and annihilate each other after
  a few seconds, unless one is killed first, which makes the other one real and permanent.
  - 💬
- [ ] 🟡 **Unobserved**: invisible everywhere except inside a Pulsar's range. Gives the Pulsar a job
  in every level.
  - 💬
- [ ] 🔴 **Schrödinger**: drawn at two points of the path at once; the first hit collapses it into
  that one.
  - 💬

---

## 10. Visuals and UI

### Is an overhaul necessary?

**No.** The look is already a black board with phosphor-green outlines and flat vector symbols: an
instrument console, which is what the fiction wants. Every control already paints itself from one
`Hud` palette, every board colour and shape lives in `Java2DFrameRenderer` behind a `Palette` role,
and the toolbar icons reuse the board shapes. The theme needs labels, a handful of colours and five
silhouettes, not a new UI.

What to do instead, highest value per cost first:

1. [ ] ⭐ 🟢 **Labels and the title** (sections 3 and 8).
   - 💬
2. [ ] ⭐ 🟢 **☉ / ψ marks** next to chain names in the upgrade panel. They double as iteration 2's
   exclusive-choice mark for the two chains: the choice reads "☉ or ψ".
   - 💬
3. [ ] ⭐ 🟢 **Recolour only what the new names contradict**: Comet brown → ice white-blue (body and
   shell); Singularity orange → amber ring around a dark centre; Entangler white → violet, its link
   lines too. Everything else keeps its colour, so the board still reads the same.
   - 💬
4. [ ] ⭐ 🟡 **New silhouettes** for Quasar, Nova, Singularity, Entangler and Comet (section 5),
   one closed shape each as the render rules require. The toolbar follows for free.
   - 💬
5. [ ] 🟡 **A backdrop.** [ ] ⭐ a faint lattice on buildable cells (a "spacetime grid": theme *and*
   it shows where you can build) / [ ] a dim starfield / [ ] none. A starfield of dots would
   camouflage the dotted path, so stars must stay off path cells and well below the markers'
   brightness. Either one is static, so it must stay out of the per-frame budget.
   - 💬
6. [ ] 🟡 **Path ends**: a swirl where anomalies enter (the horizon) and a core mark where they
   leave.
   - 💬
7. [ ] 🟡 **Projectile tails**: a comet tail on the shell, a streak behind the tachyon. Needs each
   shot's heading when it's drawn.
   - 💬
8. [ ] 🟢 **HUD tint**: [ ] ⭐ keep the phosphor green (an oscilloscope; no change) / [ ] a cool
   cyan-grey console with ☉ amber and ψ violet accents (about ten constants in `Hud`).
   - 💬

**Already in place, nothing to do:** physical damage already shows warm orange in the info sheet;
every effect marker already fits its themed name (Dilated blue, Stasis ice, Irradiated
yellow-green, Unstable pink, Observed violet), and Superposed is the existing body fade.

**Not recommended:** custom fonts (an asset to bundle and license), glow, bloom or CRT scanlines
(Java2D blur fights the 1 ms frame budget and adds no meaning), animated backgrounds.

### Vector or raster?

You don't insist on vector, so this is a real choice.

- [ ] ⭐ **Stay vector for every game piece.** Shapes scale with each level's cell size, recolour
  by role, stay in sync with the toolbar icons, and need no art pipeline and no licences. The place
  a raster image would add most is the backdrop, and a procedural lattice or starfield gets most of
  that for free.
  - 💬
- [ ] Allow exactly one raster asset: a nebula backdrop per level. The first image asset in the
  project, so the "no image assets" statements in `README.md` and `docs/ARCHITECTURE.md` change.
  - 💬

### Glyph risk

ψ is Greek and in every common font. ☉ is not guaranteed: the HUD uses Java's logical "Dialog"
font, whose fallbacks differ per OS, and the UI rule is one look on every OS. Either check a
screenshot on each OS, or paint ☉ as a tiny vector mark (a circle with a dot). The same goes for
`ħ` if the currency becomes Quanta.

---

## 11. Synergy of both scales (optional mechanics)

The brief asks for a synergy of the two. Two layers exist for free; the rest is optional.

1. [ ] ⭐ 🟢 **Already shipped**: adaptive elite armor resists whichever of kinetic and phase has
   landed most, so mixing scales is already rewarded. Say it in the text (7.1).
   - 💬
2. [ ] ⭐ 🟢 **Chain choice is scale choice** (section 4). Flavour, no rule.
   - 💬
3. [ ] 🟡 **Coupled scales**, the Entangler's own theme job: its buff is stronger while it links at
   least one ☉ tower and one ψ tower. It rewards mixed clusters and stays inside one tower, so the
   balance risk stays local. It needs a notion of which scale a tower is on (its owned chain).
   - 💬
4. [ ] 🟡 **Cross-scale reactions.** The theme gives reasons to effect interactions the towers
   brainstorm already lists (2.7) rather than inventing new ones: *burning reveals* (a burning
   anomaly glows, so it can't stay superposed); *revealed enemies are Exposed* (observed through a
   telescope); *freezing a burning enemy bursts the burn* (stasis meeting fusion).
   - 💬
5. Rejected: 🔴 **scale tides** (a level alternates rounds that favour ☉ or ψ; global rule load,
   little play value) and 🔴 **fog of observation** (towers target only what a Pulsar sees; a
   rewrite of targeting).
   - 💬

---

## 12. Impact assessment

| System                                                         | Impact                                   | What changes                                                                                                                       |
|----------------------------------------------------------------|------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| Engine, game loop, tick order, threading                       | none                                     |                                                                                                                                    |
| `board`, `cell`, `wave` (wave-script keywords too)             | none                                     | keywords are authoring syntax, not shown to the player                                                                             |
| `damage`                                                       | slight                                   | two display words                                                                                                                  |
| `economy`                                                      | slight                                   | labels, and every `$` in a price becomes `eV`                                                                                      |
| `stat`, `effect`                                               | slight                                   | labels in the inspector's switches; marker colours already fit                                                                     |
| `level`                                                        | slight                                   | three names and descriptions                                                                                                       |
| `enemy`                                                        | slight now, moderate in its own iteration | display names and descriptions, the boss chain's names; body shapes later                                                          |
| `projectile`                                                   | slight, optional                         | a comet tail and a tachyon streak, visual only                                                                                     |
| `tower`, `tower.upgrade`                                       | **moderate**                             | 8 tower names (today derived from the code name), 32 upgrade names, behaviour rows and shop descriptions, a ☉ / ψ mark per chain     |
| `ui` panels, menus, overlays                                   | **moderate**                             | a few dozen labels, title, banners, price formatting; an optional re-tint                                                          |
| `ui.render`, `Java2DFrameRenderer`                             | moderate, mostly optional                | a few colours, five body shapes, a backdrop                                                                                        |
| Tests                                                          | **moderate**                             | about 15 test files assert display strings (UI text tests, tower tests); rule tests and `GameEngineTest` are untouched              |
| Docs                                                           | **large in volume**                      | `README.md` tables, the iteration-2 request, the towers brainstorm, `td/tower/CLAUDE.md`'s naming rule; shipped feature docs are history and can keep old names |
| `BalanceHarness`, `PerformanceHarness`, `run-jtd` tooling      | none                                     | they use code names                                                                                                                |
| Balance and performance budget                                 | none                                     | unless an optional mechanic lands; a backdrop must stay out of the frame budget                                                    |

Read across: **a lot** = what the player reads about towers and upgrades, the panels, the tests
that check that text, and the docs. **Slightly** = every other domain package. **Not at all** = the
engine.

### The one structural decision: code names

Today a tower's panel title is built from its code name ("Splash" + " tower"), and
`td/tower/CLAUDE.md` says a tower's class name and UI name are the same word and name the
behaviour. Themed names describe fiction, not behaviour.

- [ ] ⭐ **Display names only.** Code keeps behaviour names (`SniperTower`, `CHILL`,
  `sniper.head.focused_optics.1`); the player sees themed ones. The CLAUDE.md line becomes "code
  names the behaviour; the UI names the fiction". A small diff; shipped feature docs stay true; the
  tooling is untouched. The cost is two vocabularies for good, so the README content tables carry
  both names and anyone (a Claude session included) can translate.
  - 💬
- [ ] **Rename the code too.** One vocabulary everywhere, at the price of a large mechanical diff
  (classes, enum constants, palette roles, node ids, tests, `run-jtd` scripts) and every feature
  doc's names going stale.
  - 💬

---

## 13. For the better, for the worse

**For the better**

1. **Identity.** "Tower Defense v1.4" becomes a game with a name, a place and a voice.
2. **Names that teach.** Pulsar sweeps. Superposed and Observed explain the stealth rule, including
   why area damage still lands. Neutrino is the ghost, Meson splits into Quarks, Magnetar jams,
   Comet chills.
3. **A grammar for the content flood.** Iteration 2 needs about 95 new node levels. "☉ bigger, ψ
   stranger" plus the word bank names them faster, and it doubles as a filter for the brainstorm's
   two hundred ideas: if an idea fits neither scale, that's a reason to cut it.
4. **Standing naming problems go away** (brainstorm 9.5): mixed name categories, Resonant Field vs
   Resonance Field, hex and curse words on plain Vulnerable, Marked Round vs Mark on Sweep, a
   protective-sounding Warding Field, Fortify / Awaken / Transcendent mixing registers.
5. **Existing systems gain meaning**: kinetic and phase plus adaptive armor become "mix the scales";
   chill into freeze becomes dilation into stasis; the two damage-over-time pools become heat and
   radiation.
6. **Nearly no engine work.** The architecture made a reskin cheap; this mostly spends that.

**For the worse**

1. **Legibility.** Genre words (slow, burn, poison, stun, invisible) are free knowledge; themed
   words must be learned. Mitigated by plain inspector lines and unchanged markers, but some names
   are obscure: Zeno Lock, Parallax, Fifth Harmonic, Meson.
2. **Technobabble.** Forced metaphors can feel cheap, and a physics-literate player notices the
   stretches (a comet that dilates time, a pulsar dealing kinetic damage).
3. **Two vocabularies** if only display names change: you and every session translate between
   `SniperTower` and Quasar. Or a large rename diff if the code changes too.
4. **Churn** in tests and docs. The towers brainstorm and the iteration-2 request are written in
   today's names.
5. **Constraint.** Every future idea must fit. Tar, Napalm, hexes and Soul Drain need translating
   (section 14), and some lose charm. A theme can also make content feel samey: not everything
   should be "a particle that does X".
6. **Half a theme is worse than none.** "Quasar tower deals Physical damage; buy Awaken" reads
   broken. The words should land in one pass.
7. **Portability**: the ☉ glyph (section 10).
8. **A starfield can hide the path** (section 10).
9. **It fixes no balance problem.** Pulse against plating, Seeker's throughput, the upgrade economy
   (brainstorm 9.1-9.4) are all exactly as they were.

💬

---

## 14. Iteration 2 and the towers brainstorm

**Timing.**

- [ ] ⭐ Settle the vocabulary (section 17's first block) **before** authoring iteration-2 content,
  so its new nodes are named once, in the new words, and designed through the two lenses.
  - 💬

**Iteration 2's open points the theme answers**

- Fortify's final name: **Grounding**; the base chain reads Grounding → Excitation → Duality.
- Range II and III: **Aperture II and III**.
- The exclusive-choice mark, for chains: **☉ or ψ**, plus the "1 of 2" the brainstorm proposed.
- Transcendent's own look: Duality could show as a second, counter-rotating ring.
- Pairing authorship: each pair of specials is an **interference pattern**, named per pair.
- "Does the extra head node branch?": it is the tower's bridge to its other scale (section 4).

**The brainstorm's vocabulary, themed.** The point is not to rename two hundred ideas but to show
the theme absorbs them; the rest follow the word bank.

New effects (brainstorm 2.2):

| Brainstorm        | Themed            | Scale |   | Brainstorm   | Themed           | Scale |
|-------------------|-------------------|-------|---|--------------|------------------|-------|
| Sundered (armor)  | **Fractured**     | ☉     |   | Brittle      | **Brittle** (keep) | ☉   |
| Unraveled (magic resist) | **Dephased** | ψ   |   | Anchored     | **Phase-locked**   | ψ   |
| Cracked (plating) | **Cracked** (keep)| ☉     |   | Bleeding (per cell travelled) | **Ablating** (meteors lose mass with speed) | ☉ |
| Exposed (crit taken) | **Exposed** (keep: a telescope exposure) | ☉ | | Resonating | **Resonating** (keep) | ψ |
| Marked (next hit crits) | **Measured** | ψ    |   | Susceptible  | **Susceptible** (keep: a physics word) | ψ |
| Silenced          | **Damped**        | ψ     |   | Soulfire (blue third pool) | **Cherenkov** (the blue glow of too-fast particles) | ψ |
| Dazed             | **Concussed**     | ☉     |   | Haste (enemy) | **Accelerated** |       |

Hexes (2.3) become **metastable states** ψ: a state that waits for a disturbance, then decays into
its payload. Echoes → **Echo**; Contagion → **Chain Decay**; Reversal (heals become damage) →
**Antimatter**; Inversion (speed-ups slow) → **Time Reversal**; Greed → **Harvest**; Doom (stored
damage released at the end) → **Half-life**; Binding / Soul Link → **Entangled** (consistent with
the Entangler: entangled things share, towers their strength, anomalies their pain; 💬 or too
close?); Kindling → **Ignition**; Grief → **Resonant Decay**.

Ground effects (2.4): burning ground → **plasma pool**; tar → **dark matter**; frost ground →
**cryo field**; fallout → **radiation zone**; mines → **antimatter mines**.

Extra head nodes, one per tower: Silver Ammunition → **Phase Rounds** (Quasar) · Arc Emitter →
**Quantum Leap** (Nova) · Frequency → **Spectrum** (Pulsar) · Field Shaping → **Curvature**
(Singularity: Undertow → Frame Dragging, Corrosion → Tidal Shear, Stasis → Deep Well, and Event
Horizon stays) · Tutelage → **Calibration** (Entangler) · Ballistics → **Orbital Mechanics**
(Comet) · Mixed Payloads → **Flavor Oscillation** (Tachyon: particles really do change "flavour"
in flight) · Fuel → **Fuel** (Flare; stars burn fuel).

Named nodes and specials: Railgun → **Relativistic Jet** · Executioner → **Gravitational Collapse**
· Deadeye → **Uncertainty** · Tactical Nuke → **Extinction Event** ·
Bunker Buster → **Neutron Core** · Cluster Shell → **Fragmenting Nucleus** · Carpet Bombing →
**Meteor Shower** · Cryo Shells → **Ice Core** (a comet's own core) · Napalm → **Solar Plasma** ·
Tar → **Dark Matter** · Gravity Shell (keep) · Seeker's nest → **Orbit** (stored missiles circle
the tower like moons) · Kill Zone → **Event Horizon** · Shockwave → **Gravitational Wave** ·
Harvester → **Hawking Radiation** (energy out of a black hole) · Null Field → **Damping Field** ·
Phase Lock (keep) · Anchor and Counter-Jamming → **Faraday Cage** · Keen Edge → **Focus** · Command
Ping → **Beacon**.

**Ideas the theme strains** (translate or cut): Soul Drain, Mentor and Chosen, Tithe, Headhunter,
Bipod. None is hard to rename, but each loses its picture; that is a small argument for cutting
them when the shortlist is made.

💬

---

## 15. Adoption tiers

What to take, as product scope. Each tier is usable on its own; none changes a rule.

| Tier | What                                                                                       | Size | Recommendation               |
|------|--------------------------------------------------------------------------------------------|------|------------------------------|
| 1    | **Words**: every name and label in sections 3 and 5-9, the README tables, a glossary        | M    | ⭐ yes, in one pass          |
| 2    | **Marks and tints**: ☉ / ψ on chains, three recolours, the title screen                    | S    | ⭐ yes, with tier 1          |
| 3    | **Silhouettes**: five body shapes, the comet tail, the singularity ring                     | S-M  | ⭐ yes                       |
| 4    | **Backdrop**: lattice or starfield, path-end marks                                          | S    | optional; check legibility   |
| 5    | **One theme mechanic**: Coupled scales or cross-scale reactions, as its own feature request | M + balance | optional; after iteration 2 |

Tier 1 is the bulk: about 25-30 source files of string literals, about 15 test files, and the docs.
One live `run-jtd` look at the end catches truncated labels (the side panel is narrow and some
themed names are longer).

- [ ] ⭐ Tiers 1-3 as one feature request, before iteration 2's content.
  - 💬

---

## 16. Word bank

Unspent words for content still to come. Strike one through when it's used.

**☉ Astral**: aphelion, apogee, aurora, barycentre, blueshift, celestial, chromosphere,
constellation, cosmic ray, dark energy, Doppler, eclipse, ejecta, escape velocity, galaxy,
heliosphere, hypernova, inertia, Kuiper, Lagrange point, light-year, magnitude, occultation,
parsec, perihelion, planetesimal, prominence, redshift, regolith, Roche limit, solar wind,
spaghettification, supernova, syzygy, tidal lock, transit, zenith.

**ψ Quantum**: annealing, Bell state, boson, bra-ket, Casimir, condensate, eigenstate, fermion,
fluctuation, gluon, Hadamard, Heisenberg, Higgs, isotope, lepton, muon, observable, Pauli
exclusion, Planck, positron, quantum foam, spin, teleportation, wavefunction, zero-point.

**Spent** (sections 3-14): Quantum Horizon, anomaly, qubit, eV, yield, annihilated, breached,
decoherence, Quasar, Nova, Pulsar, Singularity, Entangler, Comet, Tachyon, Flare, Aperture,
Grounding, Excitation, Duality, the 16 chain names, the 14 specials, Kinetic, Phase, Coherence,
Superposed, Observed, Dilated, Stasis, Irradiated, Decohering, Unstable, Restoring, Spiral Arm,
Double Slit, Wormhole, interference pattern, the enemy drafts, and the brainstorm translations in
section 14.

---

## 17. Decisions

**The vocabulary** (settle before iteration 2's content)

- [ ] Adopt the theme at all.
  - 💬
- [ ] Thesis: ⭐ astral on the board, quantum in the rules / quantum on the board too (section 5).
  - 💬
- [ ] Title: ⭐ Quantum Horizon / Planck & Parsec / Event Horizon / other.
  - 💬
- [ ] Code names: ⭐ display names only / rename the code (section 12).
  - 💬
- [ ] Damage words: ⭐ Kinetic / Phase (7.1).
  - 💬
- [ ] Economy words: ⭐ Qubits, Energy (eV), Data / alternatives (section 3).
  - 💬
- [ ] Control effects: ⭐ Dilated and Stasis / keep Chilled and Frozen (7.3).
  - 💬
- [ ] The generic word: ⭐ "tower" stays / "instrument".
  - 💬
- [ ] Every tower's chains split ☉ / ψ (section 4).
  - 💬
- [ ] Enemies: ⭐ rename now with drafts / wait for the enemy iteration (section 9).
  - 💬

**The look**

- [ ] UI: ⭐ no overhaul; labels, marks, three recolours (section 10).
  - 💬
- [ ] HUD tint: ⭐ keep the green / cyan-grey console.
  - 💬
- [ ] Silhouettes for five towers.
  - 💬
- [ ] Backdrop: ⭐ lattice / starfield / none.
  - 💬
- [ ] Raster art: ⭐ none / one backdrop image.
  - 💬

**Optional, later**

- [ ] A theme mechanic: none / Coupled scales / cross-scale reactions (section 11).
  - 💬
