# `td.tower.cinder`

Constraints for `CinderTower` (in `td.tower`) and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- The Cinder never deals direct damage: each wave sets an enemy alight once, and `ignite` is the one place
  that decides how strong the pool starts (Stoke steps, then a crit ignition's factor, which it rolls with
  `HitReceiver.rollsCrit` since an ignition is no hit). The `Burning` ledger is the only memory of whom it
  has set alight: Stoke steps live there and go when the burn has been out longer than the grace. Soulfire is a
  second pool the same ignition applies beside the ordinary burn, with the same level; Combustion reads an
  enemy's burning pools together. What its pools do to an enemy (hold more, mark faster, cauterize, heat,
  mark for magic, burst at a freeze) is a `PoolTuning` the pool carries, never state the Cinder keeps on the
  enemy; a Thermal Shock's chill and an
  Everburn top-up are the Cinder's own work in `tendBurning`. Bellows reads the attack speed in what the tower
  receives (`TowerStats.received()`), so it needs no Aura-specific code.
