# Feature Request: Curly Path Retune

**Status: planned** (2026-10-19).

## Summary

Curly Path becomes the hard level: eighteen waves that build up to the Warden, with five signature waves that each ask
the player to build a different counter, and money tight enough that a decent army only just kills the Warden. The best
headless player falls just short of it, so a human who plays better wins narrowly.

## Current state (what exists today)

- Curly Path: one long lane, 18 waves, 50 starting credits, 5 lives. Its waves send only Simple, Armored, Frenzied,
  Ghost and Mender mobs, plus the Warden chain in the last wave. It has been too easy since the first balance report.
- Built, in no level: Juggernaut, Mite, Courier, Shieldbearer, Salamander, Yeti, Jammer, Skirmisher.
- Income is kill bounty only: no wave reward, no interest. Selling refunds three quarters of everything paid.
- The Warden (16000 health, then 10000 and 6000 after each hatching) leaves a stationary egg that hatches after 8 s; the
  first egg is nearly immune. It summons a Simple every 15 s, shields itself against magic, shields allies below half
  health, heals when left alone and shields itself when it survives a crit. Only Curly Path sends it.
- `td.balance`: naive, competent and strong headless players, none of whom sells. `--variety` bans one tower for the
  whole level and reports the level's result, not which wave broke. The pressure-by-round table (closest, walls,
  breathers) reads the competent player only. A margin is the highest enemy health, as a percentage of the authored
  one, at which a player still wins.
- The other two levels' retune (`TODO.md`, "The built-in levels are not tuned to the support Obelisk") is separate.

## What this feature adds

**Target.** The strong player's margin on Curly Path averages 90% to 100% (12 seeds while tuning, 24 for the final
check), and it wins the authored level on at least one seed. Curly Path is the recorded exception to the 100% to 150%
band of `FEATURE-balance-verification.md`. Starting credits (50) and lives (5) stay as they are.

**Money.** Tight, with no spare. The Warden is where the strong player loses on at least two thirds of its losing
seeds; with 10% less bounty its margin drops by at least 10 points, with 10% more it rises clearly.

**Add, don't sell.** Each check is answered by adding a counter that keeps paying later. The strong player learns to
sell, only as a last resort (it rehearses a selling round only when every other way leaks), and the level is tuned to
its margin without selling. If selling is what wins a wave, that is reported, not tuned around.

**Roles.** Single-target (Sniper, Hive); area (Burst, Mortar, Scorcher, Radar, the Obelisk's field); anti-physical
(magic damage and armor breaking, the Mortar's Cracked included); control (slows, holds and stops: the Obelisk's
Gravity head, freeze, Daze, chill, Tar and Cryo shells); anti-sustain (no heals or shields, silence, spirit drain,
poison and Sickened, Inverted, the Scorcher's fuel line). A role is banned by upgrade node where a tower serves two.

**The checks.**

- *Role ban* (waves 6, 11, 12, 15): with the role banned from the start, the competent player's first heavy leak (a
  life or more) is at that wave; the strong player doesn't leak heavily there; banning any other role doesn't make that
  wave the first heavy leak.
- *Strip* (wave 17): the strong player's army at wave 17 with one role's towers removed leaks heavily there, for every
  role; the full army holds it.
- Each condition holds on at least two thirds of the seeds.

**The waves.**

| Wave | What it sends | What it is for |
|---|---|---|
| 1 | A column of Grunt Simples | The path |
| 2 | A drip of Grunt Couriers | Money: fast, fragile, high bounty |
| 3 | A line of Soldier Armored | First plating |
| 4 | A Soldier Ghost, then Simples | Invisibility (flavour, not a check) |
| 5 | Soldier Yetis | Preview of wave 6 |
| **6** | A short Grunt lead-in, then Elite Yetis dripped | **Single-target**: the fur blunts every small hit |
| 7 | Couriers on the flanks | Breather and money |
| 8 | A Veteran Jammer with an escort | The Jammer's debut |
| 9 | Soldier Skirmishers | Dodging |
| 10 | Soldier Mite swarms and a Frenzied | Preview of wave 11 |
| **11** | Veteran Mite columns, a swarm or two, a line of Soldier Simples | **Area** |
| **12** | A Boss Frenzied with Elite Menders and Elite Salamanders | **Control** (the boss) and **anti-sustain** (the Elites) |
| 13 | A drip of Veteran Couriers | Breather and money |
| 14 | Veteran Juggernauts and an Elite Ghost | Preview of wave 15; the Ghost shrouds |
| **15** | A column of two Elite Juggernauts, Veteran Shieldbearers on the flanks, an `armored` tail | **Anti-physical** |
| 16 | Elite Skirmishers and a Jammer | The hardest wave that isn't a check |
| **17** | One group per check, a step harder than its wave, fast to slow | **Every check** |
| **18** | The Warden, then Mites, Skirmishers and a Mender as it nears half health | **The ultimate test** |

Each enemy in a signature wave tests one thing. The `armored` tail of wave 15 goes if it makes the check too noisy.

**Enemy changes.** On Curly Path only, the Elite Mender also gives nearby allies a small periodic shield against every
damage type. The Warden's health, shields and hatch time are its tuning knobs; its first egg stays nearly immune, so
every fight reaches the second Warden.

**Build-up.** Read from the strong player's pressure row at authored health: walls only at signature waves, each
signature wave a wall, breathers at 7 and 13, waves 1 to 3 gentle, and pressure rising across each stretch.

**Out of scope:** Zigzag Path and Twisted Hourglass, new enemies, tower changes beyond bug fixes, detection as a check.

## Interconnections

- **`FEATURE-balance-verification.md`**: its band and its players; Curly Path is an exception to the band, and its
  tools gain a selling strong player, the role-ban and strip checks and the strong player's pressure row.
- **`FEATURE-obelisk-kill-box.md`**: the Obelisk's heads split across control (Gravity) and anti-sustain (Ward).
- **`docs/ideas/enemies-brainstorm.md`**: puts its built enemies into waves.

## Constraints and open risks

- **Agility and area damage.** Mites only test area if a blast can't be dodged; to check before tuning.
- **The strong player is slow** (a dozen rehearsals a round); the tuning loop stays on one level and 12 seeds.
- **The `armored` shape adapts to how the player has dealt damage**, so it moves the target while tuning.
- **Slow enemies favour control.** If banning control breaks wave 15 first, the Juggernauts get faster.
- **Player-visible text.** The README's level and enemy rows ("not in any level's waves yet"), and the Mender's
  description on this level.

## Decisions made

- Everything in *What this feature adds*, settled in a grill round on 2026-10-19.
- Phases, each committed: the tools and a baseline; the game changes (the Mender's shield, the dodge check); a draft of
  all eighteen waves; the checks in wave order; the Warden and the money; the build-up; the 24-seed check, the README
  and one live `run-jtd` pass.
- Each bug found gets its own commit with a `GameEngineTest`; a design problem goes to `TODO.md` and is reported. A
  phase that won't converge stops and reports its numbers rather than loosening a criterion.

## Open questions

None.

---

*After planning and implementation, update this document rather than deleting it: mark it implemented, prune
resolved open questions, and either promote deferred scope to a new request or note it is still wanted for a
later version.*
