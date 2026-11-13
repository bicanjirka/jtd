"""Shared physics and scenarios for tree_calc's tower models: a hit in the game's order, the crowd on a path one cell
wide, the enemies every tower is weighed against, and the price of a copy.

Numbers here come from the code; the comment beside each says where, so a change there is found here.
"""
import json
import math
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

TICKS_PER_SECOND = 20
WALKER_SPEED = 1.28
# SpawnParameters.DEFAULT_DELAY_TICKS_PER_SLOT: Walkers about 1.43 cells apart
SPACING = 22.4 / TICKS_PER_SECOND * WALKER_SPEED
DENSITY = 1 / SPACING
STREAM_RATE = WALKER_SPEED / SPACING
# TowerFactory.COPY_PRICE_GROWTH: each copy of a type costs this much more than the one before it
COPY_PRICE_GROWTH = 1.25
# A node pays off as well as another copy once this many copies of its tower stand
BREAK_EVEN_COPIES = 2
# The towers around one, as bare reference towers: a second an enemy is held back, or a payout from the damage it
# takes (a Doom), is worth what they deal it
NEIGHBOURS = 4
# The Walker's ladder (WalkerEnemy: 75, 138, 458, 1175, 2400 health) over its Grunt
ELITE_FACTOR = 1175 / 75
BOSS_FACTOR = 2400 / 75


@dataclass(frozen=True)
class Foe:
    name: str
    health: float
    rank: str = "normal"
    armor: float = 0.0
    magic_resist: float = 0.0
    plating: float = 0.0
    insulation: float = 0.0
    resilience: float = 0.0
    agility: float = 0.0
    # Its pace against a Walker's
    speed: float = 1.0

    def boss_or_elite(self):
        return self.rank in ("elite", "boss")


@dataclass(frozen=True)
class Scenario:
    """Enemies of one kind streaming past the tower, one after another: alone (no neighbour for a blast or a hop)
    or as a crowd at the default spacing. A weight above zero puts it in the mix that prices nodes. `copies` towers of
    the kind stand side by side on the same enemies, and the scenario reads what one of them deals: a model only adds
    one where its copies work together."""
    name: str
    foe: Foe
    crowd: bool
    weight: float = 0.0
    defence: bool = False
    copies: int = 1


PLAIN = [
    Scenario("crowd 75", Foe("walker", 75), True, 2),
    Scenario("crowd 300", Foe("walker", 300), True, 2),
    Scenario("crowd 1000", Foe("walker", 1000), True, 2),
    Scenario("lone 300", Foe("walker", 300), False, 1),
    Scenario("elite", Foe("elite", 75 * ELITE_FACTOR, "elite"), False, 1),
    Scenario("boss", Foe("boss", 75 * BOSS_FACTOR, "boss"), False, 1),
    # The old yardstick: one enemy that never dies, so nothing that earns from deaths or wastes overkill counts
    Scenario("endless", Foe("endless", math.inf), False, 0),
]

# Each enemy's base-rank defences (src/main/java/td/enemy/*Enemy.java) on a crowd of 300 health, so a column reads
# as what the defence alone takes against "crowd 300", the plain scenario each defence is a variant of. They pair up:
# a flat loss on every hit of one type (plating, insulation) and a share of it (armor, magic resist), with the Knight
# for both types at once and the Runner for pace. Tenacity, shields and spawns are left out. A burn-immune enemy is
# left out: what a counter to it adds is the whole tower, which no price can follow
COUNTER_BASE = "crowd 300"
DEFENCES = [
    Scenario("knight", Foe("knight", 300, armor=25, magic_resist=25, plating=2, resilience=100), True, defence=True),
    Scenario("juggernaut", Foe("juggernaut", 300, plating=25), True, defence=True),
    Scenario("relay", Foe("relay", 300, insulation=15), True, defence=True),
    Scenario("golem", Foe("golem", 300, armor=35, resilience=40), True, defence=True),
    Scenario("templar", Foe("templar", 300, magic_resist=70), True, defence=True),
    # HurtSpeedTrait: faster the more it is hurt, so about halfway to its top pace across a range
    Scenario("runner", Foe("runner", 300, agility=-20, speed=1.2), True, defence=True),
]

SCENARIOS = PLAIN + DEFENCES


def crit_chance(chance, foe):
    """HitResolution.rollsCrit: resilience shrinks the chance, and at full resilience nothing crits, not even a
    sure crit; agility below zero raises it (EnemyStat.AGILITY). A dodge on positive agility is left out."""
    if foe.resilience >= 100:
        return 0.0
    return chance * (1 - max(foe.resilience, 0) / 100) * (1 - min(foe.agility, 0) / 100)


def can_crit(foe):
    return foe.resilience < 100


def crit_factor(multiplier, foe):
    return 1 + (multiplier - 1) * (1 - foe.resilience / 100)


def hit(amount, foe, magic=False, crit=False, crit_multiplier=1.0, armor_pen=0.0, magic_pen=0.0, armor_shift=0.0,
        flat_shift=0.0, flat_pen_share=0.0):
    """One hit after the enemy's defences, in HitResolution.steps' order: crit, mitigation (100 / (100 + armor),
    after penetration in points), flat reduction (plating, or insulation for magic; `flat_shift` moves whichever it
    is), never below zero. Damage over time takes the same steps, so plating eats small ticks."""
    if crit:
        amount *= crit_factor(crit_multiplier, foe)
    if magic:
        mitigation, flat = foe.magic_resist, foe.insulation + flat_shift
        pen = magic_pen
    else:
        mitigation, flat = foe.armor + armor_shift, foe.plating + flat_shift
        pen = armor_pen
    mitigation = max(0.0, mitigation)
    if mitigation > 0:
        mitigation = max(0.0, mitigation - pen)
    amount *= 100 / (100 + mitigation)
    return max(0.0, amount - max(0.0, flat) * (1 - flat_pen_share))


@lru_cache(maxsize=None)
def _held_back():
    path = Path(__file__).resolve().parent / "reference.json"
    return json.loads(path.read_text(encoding="utf-8")).get("held_back", {}) if path.exists() else {}


def neighbours(scenario):
    """What the towers around one deal one of the scenario's enemies in a second: NEIGHBOURS bare reference towers,
    sharing their fire in a crowd. A payout from others' damage (a Doom) and a second held back are priced on it."""
    return NEIGHBOURS * _held_back().get(scenario.name, 0.0)


def density(foe):
    """Enemies a cell of path holds in a crowd: spawned at the same interval, a faster kind walks further apart."""
    return DENSITY / foe.speed


def chord(radius, offset=1.0):
    """Cells of a straight path `offset` cells from the tower that lie within `radius`."""
    return 2 * math.sqrt(radius * radius - offset * offset) if radius > offset else 0.0


def path_cells(range_cells, dead_zone=0.0):
    """Cells of path a tower can shoot: a straight path a cell from it, minus its dead zone."""
    return chord(range_cells) - chord(dead_zone)


def copy_price(price, copies_on_board):
    """TowerFactory.Type.priceFor: the surcharge compounds per copy, rounded half up."""
    return math.floor(price * COPY_PRICE_GROWTH ** copies_on_board + 0.5)


def cover(zones):
    """Cells of path a blast covers, each weighted by its zone's share of the damage: zones as (radius, share),
    outward. The path is one cell wide, so a blast reaches enemies in proportion to its radius."""
    total, inner = 0.0, 0.0
    for radius, share in zones:
        total += 2 * (radius - inner) * share
        inner = radius
    return total


def zone_hits(zones, foe, amount, crowd, **hit_args):
    """Damage one blast deals, landing on its target: to the target alone, or to the crowd its zones reach, each
    enemy's share capped at its health (a big hit on a small enemy wastes the rest)."""
    if not crowd:
        return min(hit(amount * zones[0][1], foe, **hit_args), foe.health)
    total, inner = 0.0, 0.0
    for radius, share in zones:
        enemies = 2 * (radius - inner) * density(foe)
        total += enemies * min(hit(amount * share, foe, **hit_args), foe.health)
        inner = radius
    return total
