# `td.damage`

- Health, damage, plating and regeneration are stored as `int` units of `DamageUnits`
  (`PER_POINT` per point). The UI shows points.
- Content is authored in points and converted once, at the boundary that takes it
  (`TowerBaseStats`, `FlatResistTrait`, `HealTemplate`, `DamageDealtCondition`). A parameter in
  points is a `float` named for it; a raw `int` of health, damage, plating or regeneration is in
  units.
- Convert for display with `DamageUnits.inPoints`, never a literal `/ 100`. Nothing outside
  `DamageUnits` knows the factor.
