# Feature Request: Next-gen Tower Abilities

**Status:** draft, 2026-11-10, reviewed with the whole bundle 2026-11-13; no open question left. Part of `FEATURE-next-gen.md`; from the
request's Burst notes, written so any tower can use it. The Burst's Stormcaller and Hexer are its only users: the
other towers' "ability" nodes are counts (the Fourth Shot, the Aerie's Release) or rhythms (the Sundial), with no bar.

## Summary

A tower can own abilities that charge up and cast on their own, beside its normal attack. The tower earns a
currency by fighting, and whatever it earns, a whole unit or a fraction, fills every ability it owns at once, each
in its own bar. The Burst's Stormcaller and Hexer are the first users.

## Current state (what exists today)

- A Hexer casts instead of attacking: every 4th shot is a Hex in place of a blast, and the hexes it owns form a pool
  it picks from (`HexCaster`, `HexPick`, `HexLedger`; `shipped/FEATURE-splash-stormcaller-and-hexer.md`).
- A primed shot is a `TowerStatus` granted with `grantStatus` and spent by the next attack (`beginShot()`); different
  ones stack on one shot.
- The tower pane lists the tower's buffs with their icons (`TowerBuffState`); each has a hover sheet.
- Every wave starts with every tower ready (ADR 0021): what a fight built up resets at the clear and at the send.
- A save holds a game only between rounds (ADR 0011).

## What this feature adds

- **Buying an ability** in the upgrade tree adds it to the tower's abilities, much as a hex joins the Hexer's pool
  today.
- **Earning.** The tower's normal attack earns currency by the damage it deals, at a rate the tower sets (so many
  damage per unit), in fractions as it goes; its passive's event pays a large share at once. Nothing fills with time
  alone, so a tower that isn't fighting earns nothing. The tower itself holds no bar.
- **Ability bars.** Each ability has its own cost in the tower's currency, a small whole number (1, 2 or 3), and its
  own bar. Everything the tower earns goes into every active ability's bar at once: half a unit earned is half a unit
  in each. It is not a shared mana pool: all abilities charge together.
- **A bar holds 1 unit more than its cost**, so a full ability waiting for a target, or a large payout landing at
  once, loses less. Past a full bar the rest is lost.
- **Casting.** An ability casts as soon as its cost is in and it has a fitting target; with none, it waits. A cast
  pays its cost from the bar and the rest stays. A cast never replaces the tower's normal attack, which keeps firing.
- **Units only.** The game speaks of the currency in units and their fractions; any finer count the code keeps is
  never shown. Every tuning number (damage per unit, a passive's payout, a cost) is written in units.
- **Kinds of cast.** A cast may fly to a target as its own projectile (a curse), or prime the tower's next normal
  shot (a charged shot). Primed shots from repeated casts stack, and each normal shot spends one.
- **The tower pane** lists the tower's abilities under each other, each with its bar (two at most today);
  hovering one shows its numbers, its fill as units and fractions. A bar is as long as the ability's cost plus 1, so a costlier ability's
  is longer; a faint glyph of the currency sits behind each unit, and a mark shows where one unit ends and the next
  begins. The board never shows the bars; at most it shows which abilities are full and waiting for a target.
- **Every cast is visible**: its own projectile or animation.

## Interconnections

- `FEATURE-next-gen-upgrade-tiers.md`: abilities are bought as ability upgrades.
- `FEATURE-next-gen-shot-flight.md`: casts that fly.
- `FEATURE-next-gen-splash-tower.md`: Stormcaller (Sparks) and Hexer (Souls), the first abilities and currencies.

## Constraints and open risks

- **Names**: "Charge" is taken by the Charged effect; the Burst's draft calls its currencies Sparks and Souls.
- What a wave resets (ADR 0021) has to cover the ability bars and queued primed shots.
- The tower pane has room for a few rows; two abilities fit, and a 3-unit ability's bar is 4 units long.
- **A full ability waiting for a target** shows on the board at most as a cue on the tower; the Burst has no turning
  head, so where that cue sits is its doc's question 5.

## Decisions made

- Abilities charge together from one currency per tower; casts don't replace the normal attack.
- The currency comes from the normal attack's damage and the passive, not from the passive alone. A passive that
  only paid on a kill or a threshold earned too little, too late, and left the normal attack out of the economy.
- Each ability has its own bar and the tower has none. A tower bar that turned into units that then charged the
  abilities was two steps for one thing, and its capacity was a number the player never needed.
- A wave starts with 1 unit in every ability bar, what a full tower bar used to give, so the first cast comes early.
  That fixes the wave-start state ADR 0021 needs.
- Ability costs are whole numbers and differ per ability, but stay small.
- The ability bars live only in the tower pane; the board stays calm.
- Every ability a tower owns is active; there is no picker in the pane. No tree gives a tower more than two (the
  Hexer's hexes are one cluster, and so is the Stormcaller's Storm), so the cap of 3 once drawn here never binds. A
  tree that grows past three brings the cap and the picker back, since every bought ability would otherwise add
  casts for free.

## Open questions

1. ✅ **Charges between waves.** 1 unit in each ability bar.
2. ✅ **Too much currency.** A bar keeps 1 unit past its cost; the rest is lost.
3. ✅ **Cast order.** When two abilities fill on the same tick, they cast in the pane's order, top first, which is
   the order they were bought; both cast that tick if both have a target (the user, 2026-11-13).
4. ✅ **Changing the active abilities.** There is nothing to change: every owned ability is active (Decisions made).
   A cluster pick that swaps one ability for another changes only between waves, as every pick does.
