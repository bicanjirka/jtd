# `td.tower.mortar`

Constraints for `MortarTower` (in `td.tower`) and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- The Mortar's shell is slow and unguided: it flies to where its target stood, so a fast enemy dodges it. Its minimum
  range is `TowerStat.MINIMUM_RANGE`, put on the `Reach` as its dead zone. Predictive Fire picks the biggest group
  weighed by how far along it is (its node's `TargetPriority`), asks its enemy where it will stand
  (`EnemyTarget.positionAfter`) and re-aims a few times, since flight time depends on where the shell is going.
- What it fires comes from its `Magazine`, a fixed number of shells, front first. A firing takes the front
  shells (one, or a volley's), and the back is drawn full again from `MagazineSpec`, the weights the owned nodes
  set. The magazine is build state, not fight state (ADR 0024): no wave clear resets it and the save keeps it shell by
  shell. It changes only on the game loop: at a firing, and each tick through `keptTo`, which drops the kinds no
  longer owned and draws behind. Never draw at purchase, on the EDT.
- A shell's `ShellKind` is fixed when it is drawn, so the pane shows it in the magazine and the board draws it in its
  look in flight. Everything a kind does differently (blast, damage type, flight speed, ground) is on the kind;
  the tower never compares kinds but for a Heavy's pick, a Nuke's flash and the Flares it keeps. A pick a node makes
  between kinds is a value in the spec (`HeavyRider`), not a branch per mechanic.
- The reload after a firing is how long loading the shells it loads next takes: each kind's load time is a stat
  (`ShellKind.loadStat`), a light shell's (`isLight`) times `CHAIN_LOAD` when the shell before it is light too, a
  volley's the mean of its shells' times its factor. The Fire rate row averages the kinds by their chances and the
  chained ones by `MagazineSpec.chainedShare`, and the reload art reads the reload under way (`getCoolDownFraction`,
  `reloadTicks`), never one period.
- What the pane says a kind does (its hit, speed, blast, effects) is a `ShellProfile` the tower works out from what it
  would fire now, never the kind's constants read in the UI.
- The chances the panel shows come from `MagazineSpec.chances()` (`MagazineOdds`, which counts Fusion and Rich Mix),
  never from the weights, which no player sees.
- A shell hits for the Mortar's damage plus its kind's own points (`ShellKind.damage`): a Normal's and a Flare's
  are the `NORMAL_DAMAGE` stat, a Heavy's the `HEAVY_DAMAGE` stat, a Nuke two Heavies. Its ground's pulse and a Bleed are flat
  points, never a share of the damage.
- How much wider than the Mortar's blast a kind lands is on the kind (`ShellKind.wideningCells`): a Heavy's is the
  `HEAVY_BLAST` stat, a payload's its Payload spec's; a Nuke's blast is its own.
- A shell lands in two zones: its core at full damage and its edge at its kind's share. With Cracking Shells every
  shell gives what it hits the Mortar's share of Crack (Deep Crack makes the share worth more stacks). A shell's ground is one zone, owned by the Mortar (`ZoneOwner`); a
  Mortar keeps only its newest flares and puts out the oldest (`Zone.putOut`).
- Carpet Bombing lays a volley along the enemy path centred on the target (`PathLine`), never by geometry around the
  impact, with a Normal sneaked between each two loaded shells (`VolleySpec.sequence`), never taken from the magazine;
  a Barrage's later shells re-pick their target when they fire. The pane shows the next firing as it is sent, sneaked
  Normals in place (`LinedUpShell`), then the rest of the magazine.
- With Thick Mix a Normal right behind a payload carries it (`HeldShell`): in the magazine, where the front shell is
  behind the one fired last (`Magazine.lastFired`, kept by the save), and in a volley, where a sneaked Normal is behind
  the loaded shell before it. Its blast puts a share of a pulse of that payload's ground on what it reaches, through
  the ground's own `ZoneEffects.touch`, never a zone of its own.
- A Nuke's flash gives every enemy on the board, hidden ones too, a dose of Radiation (`ShellKind.irradiatesTheBoard`);
  its fallout poisons nothing.
- Every blast leaves a `BlastMark`, kept as long as the longest detonation is drawn; the board turns it into that
  shell's detonation (and, for a nuke, a moment's whiteout of the whole board), so the tower stores nothing more.
