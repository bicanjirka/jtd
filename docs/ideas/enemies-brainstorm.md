# Enemy Design Brainstorm

Enemies that give each tower a job, to review once the towers are settled. Moved here from the
tower brainstorm (its section 5), which became the seven tower-rework feature requests
(`docs/features/`, order in `TODO.md`). Nothing here is reviewed yet.

## How to use it

- `[x]` = keep it, `[ ]` = no, or not yet. Write anything after a 💬.
- ⭐ = my pick where options compete.
- Cost tags: 🟢 existing primitives only · 🟡 one new small primitive · 🔴 a new shared system.
- Tower names and upgrades below are the ones in the feature requests. Where an idea leaned on a
  tower upgrade that was not taken (Wildfire, Permafrost, Carpet, Shell Shock, Counter-Jamming),
  it now names the decided answer instead.

## Already decided, waiting for enemies

These were decided in the tower rounds, but only an enemy makes them matter.

- **The Jammer in real waves.** Disruption exists and no level sends the Jammer. The Aura's half
  disruption at base and Broadcast II's quarter are pointless until it shows up.
- **Haste** (an enemy-side speed-up). Anchored (Pulse's Undertow) and Hex of Inversion counter it,
  and the enemy-stats request deferred agility until a haste exists.
- **High-spirit enemies** (a Priest, a Zealot) that shake debuffs off fast: spirit now paces every
  debuff, so they are the enemy that asks for poison, Soul Drain or Toll.
- **A typed shield's first user** (`TODO.md`): a physical-only shield that asks for magic or a
  Nullifier.

## Enemies that give each tower a job

Each exists to make one role matter, so a level can ask for a tower by what it sends. Stats are
existing `EnemyStat`s; abilities use today's triggers unless tagged.

- [ ] ⭐ 🟢 **Juggernaut**: huge physical plating, slow. Needs magic, Cracked or Sundered (the
  Pulse, the Seeker, every Mortar shell).
  - 💬
- [ ] ⭐ 🟡 **Priest**: high spirit, cleanses allies' debuffs every 5 s. Needs Silence, Soul Drain,
  or hexes that punish cleansing (the Pulse, the Hexer).
  - 💬
- [ ] ⭐ 🟡 **Blinker**: teleports 1.5 cells forward when hit (3 s cooldown). Needs lock-on, freeze
  or Null Field (the Seeker, the Pulse).
  - 💬
- [ ] ⭐ 🟢 **Mites**: a swarm of 12 tiny enemies that die to one hit each. Needs area (the Pulse,
  the Mortar's Cluster Shell, the Stormcaller's Chain Lightning).
  - 💬
- [ ] 🟢 **Courier**: very fast, fragile, high bounty; worth hunting (the Seeker, chill, the
  Sniper).
  - 💬
- [ ] ⭐ 🟢 **Shieldbearer**: periodically gives nearby allies a *physical-only* shield. The typed
  shield's first user; asks for magic or the Nullifier.
  - 💬
- [ ] 🟡 **Drummer**: a haste aura for allies. Needs Anchored or Hex of Inversion (the Pulse, the
  Hexer).
  - 💬
- [ ] 🟢 **Salamander**: burn-immune, fire-coloured. Punishes a Cinder-only defence; Soulfire
  answers.
  - 💬
- [ ] 🟢 **Yeti**: freeze-immune, 50% chill resist. Punishes a Seeker-only defence; Dazed (its own
  ladder) still stops it.
  - 💬
- [ ] 🟡 **Thornback**: each crit taken gives it +20 armor for 3 s. Punishes crit spam; Sundered
  and magic answer it.
  - 💬
- [ ] 🔴 **Burrower**: submerged (untargetable, immune to hits) on part of the path; ground zones
  and the Pulse's field still hurt it (the Mortar, the Cinder, the Pulse).
  - 💬
- [ ] 🟡 **Necromancer**: once per 8 s raises the last ally that died nearby. Needs Silence or
  killing it first (the Pulse, the Sniper).
  - 💬
- [ ] 🟡 **Mirror**: resists the damage type of the last hit it took. Needs mixed damage: the
  Sniper's Silver Rounds, the Sonar's Frequency.
  - 💬
- [ ] 🟡 **Elite affixes**: Elite and Boss ranks roll one random affix per spawn (hasted, shielded,
  regenerating, splitting, thorny). Cheap variety without new enemies.
  - 💬

---

## Appendix: parked game-wide ideas

The tower brainstorm's "wild ideas", never reviewed. Each would be a feature of its own; kept so
they aren't lost.

- [ ] 🔴 **Transcendent actives**: a Transcendent tower gains one click-to-cast ability with a
  cooldown (Sniper *Assassinate*, Mortar *Barrage* at a clicked spot, Pulse *Overload*, Seeker
  *Salvo*, Cinder *Firestorm*, Sonar *Ping* the whole map, Splash *Storm* or *Hex Nova*).
  - 💬
- [ ] 🔴 **Boss rewards**: after each boss wave, pick 1 of 3 global perks for the rest of the level.
  - 💬
- [ ] 🟡 **Interest**: unspent credits earn 5% at each wave start.
  - 💬
- [ ] 🟢 **Early call bonus**: starting the next wave while the current one is alive pays credits.
  - 💬
- [ ] 🔴 **Global upgrades** (the buy-once, per-type upgrade in `TODO.md`): an "Academy" menu.
  - 💬
- [ ] 🟡 **Overheat**: towers that fire nonstop for 20 s lose fire rate until they rest.
  - 💬
- [ ] 🟡 **Weather per level**: rain (burn -25%, chill +25%), heat (the reverse), fog (range -15%,
  the Sonar unaffected).
  - 💬
- [ ] 🔴 **Respec**: swap a bought special once per level, at a price.
  - 💬
- [ ] 🟡 **Combo feedback**: a small glyph burst when a combo fires (Thermal Shock, a Contagion
  jump, an inferno), so the player learns the combos exist.
  - 💬
- [ ] 🟡 **Tower bonds**: specific adjacent pairs get a small named bonus, shown when placed.
  (Combos otherwise emerge from the rules, with nothing hard-coded.)
  - 💬
