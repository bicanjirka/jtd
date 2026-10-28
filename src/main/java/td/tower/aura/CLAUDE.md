# `td.tower.aura`

Constraints for `AuraTower` (in `td.tower`) and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- The Aura gives `buffFor`: damage and range at `AURA_POWER` (plus Kinship), half the disruption, and
  every owned node's emission; Chosen scales it all ×3. Its mechanics (Kinship, the XP mentoring,
  Chosen, Rally) are `AuraMechanic`s over an `AuraSpec`.
- The Aura never reaches into another tower's state except through `Tower.receiveStatus` (Rally).
  Withering Field is an interval in the buff (`WITHER_INTERVAL`) that each tower spends on its own first
  hit after it, never on periodic damage. Chosen's hero is derived from XP on every call, so
  `AuraTower.doTick` republishes every tower's stats when the hero changes. Rally's spot is the path point
  nearest the Aura, found once.
