"""The next-gen Obelisk on paper: a tick simulation of its field, Toll, drag, Frailty, the Sundial's shadow and the
Tier II and III picks, on a straight path a cell from the stone.

What it is worth is mostly what the towers around it gain, so the model counts that as control, which tree_calc
prices at what paper.NEIGHBOURS bare reference towers deal the enemy in a second (`held_back`):
- drag: its share of each second inside (a 30% slow for 1 s is 0.3 s), a Daze all of it;
- Frailty: its share of damage taken for each second the enemy carries it within the neighbours' reach (+20% for
  1 s is 0.2 s), since it adds that share of their fire.
The neighbours' fire also kills: an enemy within their reach loses what they deal it (with Frailty), and its kills
end the enemies' walks as they would in play. Their damage is not counted as the tower's. Ward's Silence is weighed on a
crowd with Menders in it (`menders`): what a Silenced Mender would have healed counts as damage.

tree_calc multiplies every value by the cells of path in reach; reach is the bare field's, fixed, since the field's
own damage and control already grow with its chord in the simulation, and counting range twice would square it."""
import math
from functools import lru_cache

import paper

DOC = "docs/features/shipped/FEATURE-next-gen-pulse-tower.md"
PRICE = 20
ROLE = "crowd"

P = dict(
    damage=0.5, range=1.5, drag=0.10, drag_per_stack=0.04, slow_cap=0.5,
    toll_every=1.0, toll_cap=5.0, fade_every=1.0,
    wide_field=0.5, wide_field_2=0.5, deep_pull=0.02, deep_pull_2=0.02, quick_toll=0.75, hum=0.25, hum_2=0.25,
    frailty=0.04, knell=20.0,
    sundial_every=3.0, sundial_reach=4.0, sundial_hit=20.0, deep_shadow=16.0, swift_shadow=2.0,
    daze=0.75, daze_after=1.0, umbra=2.0, meltdown_cap=10.0, burden_per_stack=0.03, burden_cap=0.75,
    # How far past the field the towers around it reach the enemy, in cells
    neighbour_reach=1.5,
    # The menders crowd: every Nth enemy a Mender (MenderEnemy, about Veteran for 300 health: 0.35 a tick to every
    # other ally within 2 cells), whose heal Ward's Silence stops while it is inside
    mender_every=3.0, mender_heal=7.0, mender_reach=2.0,
    seconds=40.0, warm_up=10.0,
)

# Casters for Ward: a crowd of 300 with Menders in it. Its held time is priced at crowd 300's worth inside the model,
# since reference.json holds no worth for it
SCENARIOS = [paper.Scenario("menders", paper.Foe("walker", 300), True, 1)]

NODES = {
    "Wide Field": "field range",
    "Deep Pull": "drag per Toll stack",
    "Quick Toll": "Toll pace",
    "Hum": "field damage",
    "Sundial": "a shadow every few seconds: an aimed hit and a Toll stack on every enemy it passes",
    "Wide Field II": "field range",
    "Gravity": "a second inside Dazes, once a visit (control)",
    "Umbra": "the shadow's hit doubled on an enemy in the field",
    "Burden": "drag per Toll stack, and a deeper slow cap (control)",
    "Ward": "Silence inside: a Mender inside heals nobody (menders)",
    "Hum II": "field damage",
    "Deep Shadow": "the shadow's hit",
    "Knell": "while anyone inside carries full Toll, the stone rings: every enemy in the field takes a hit",
    "Meltdown": "Toll cap",
    "Deep Pull II": "drag per Toll stack",
    "Swift Shadow": "the shadow's pace",
}

_T0 = ["Wide Field", "Hum"]
_T1 = ["Ward", "Sundial", "Quick Toll", "Deep Pull"]
_T2 = ["Hum II", "Deep Shadow", "Wide Field II"]
_T3 = ["Deep Pull II", "Swift Shadow"]
BUILDS = {f"{stone.lower()} {payoff.lower()}": _T0 + _T1 + [stone] + _T2 + [payoff] + _T3
          for stone in ["Burden", "Gravity", "Umbra"] for payoff in ["Meltdown", "Knell"]}


def _range(b, p):
    return p["range"] + p["wide_field"] * ("Wide Field" in b) + p["wide_field_2"] * ("Wide Field II" in b)


def reach(b, p):
    return p["range"], 0.0


def _half(radius):
    return math.sqrt(max(radius * radius - 1, 0.0))


def rate(b, scenario, p):
    return _run(tuple(sorted(b)), scenario.name, scenario.foe, scenario.crowd, tuple(sorted(p.items())))


@lru_cache(maxsize=None)
def _run(key, name, foe, crowd, p_items):
    p = dict(p_items)
    b = frozenset(key)
    tps = paper.TICKS_PER_SECOND
    dt = 1 / tps
    base_range = _range(b, p)
    every = p["quick_toll"] if "Quick Toll" in b else p["toll_every"]
    cap = p["meltdown_cap"] if "Meltdown" in b else p["toll_cap"]
    slow_cap = p["burden_cap"] if "Burden" in b else p["slow_cap"]
    per_stack = (p["drag_per_stack"] + p["deep_pull"] * ("Deep Pull" in b) + p["deep_pull_2"] * ("Deep Pull II" in b)
                 + p["burden_per_stack"] * ("Burden" in b))
    tick_damage = p["damage"] + p["hum"] * ("Hum" in b) + p["hum_2"] * ("Hum II" in b)
    frail = p["frailty"]
    sundial = "Sundial" in b
    shadow_every = p["swift_shadow"] if "Swift Shadow" in b else p["sundial_every"]
    shadow_hit = p["sundial_hit"] + p["deep_shadow"] * ("Deep Shadow" in b)
    shadow_half = _half(p["sundial_reach"])
    umbra = p["umbra"] if "Umbra" in b else 1.0
    menders = name == "menders"
    worth_name = "crowd 300" if menders else name
    neighbour_dps = paper.NEIGHBOURS * paper._held_back().get(worth_name, 0.0)
    spawned = 0
    walk = paper.WALKER_SPEED * foe.speed
    gap = paper.SPACING / walk if crowd else None

    enemies = []
    spawn_wait = shadow_clock = knell_clock = 0.0
    dealt = held = 0.0
    ticks, warm = int(p["seconds"] * tps), int(p["warm_up"] * tps)

    def field_range():
        return base_range

    outer = max(shadow_half if sundial else 0.0, _half(base_range) + p["neighbour_reach"])
    start_x = -outer - 0.5

    def spawn():
        nonlocal spawned
        spawned += 1
        enemies.append(dict(mender=menders and spawned % int(p["mender_every"]) == 0, x=start_x, hp=foe.health, stacks=0, clock=0.0, fade=0.0,
                            dazed=0.0, visit_dazed=False, inside=False, in_time=0.0))

    def damage(e, amount):
        landed = min(paper.hit(amount, foe, magic=True) * (1 + frail * e["stacks"]), e["hp"])
        e["hp"] -= landed
        return landed

    def gain(e):
        e["stacks"] = min(cap, e["stacks"] + 1)
        e["fade"] = 0.0

    for t in range(ticks):
        measuring = t >= warm
        if crowd:
            spawn_wait -= dt
            if spawn_wait <= 0:
                spawn()
                spawn_wait += gap
        elif not any(e["hp"] > 0 and e["x"] <= outer for e in enemies):
            spawn()
        half = _half(field_range())
        near_half = half + p["neighbour_reach"]
        # The shadow turns: every enemy within its reach takes the hit and a stack
        if sundial:
            shadow_clock += dt
            if shadow_clock >= shadow_every:
                shadow_clock -= shadow_every
                for e in enemies:
                    if e["hp"] > 0 and abs(e["x"]) <= shadow_half:
                        landed = damage(e, shadow_hit * (umbra if abs(e["x"]) <= half else 1.0))
                        if measuring:
                            dealt += landed
                        gain(e)
        for e in enemies:
            if e["hp"] <= 0:
                continue
            inside = abs(e["x"]) <= half
            if inside and not e["inside"]:
                e["visit_dazed"] = False
                e["clock"] = 0.0
                e["in_time"] = 0.0
            e["inside"] = inside
            if inside:
                e["clock"] += dt
                e["in_time"] += dt
                if e["clock"] >= every:
                    e["clock"] -= every
                    gain(e)
                # A beat inside Dazes, once a visit, whatever the Toll: never at the edge
                if e["in_time"] >= p["daze_after"] and not e["visit_dazed"] and "Gravity" in b:
                    e["visit_dazed"] = True
                    e["dazed"] = p["daze"]
                landed = damage(e, tick_damage)
                if measuring:
                    dealt += landed
                if e["hp"] <= 0:
                    continue
            else:
                if e["stacks"] > 0:
                    e["fade"] += dt
                    if e["fade"] >= p["fade_every"]:
                        e["fade"] -= p["fade_every"]
                        e["stacks"] -= 1
            # The towers around it: their fire, and Frailty's share of it as held time
            if abs(e["x"]) <= near_half and e["hp"] > 0:
                e["hp"] -= neighbour_dps * (1 + frail * e["stacks"]) * dt
                if measuring:
                    held += frail * e["stacks"] * dt
            if e["hp"] <= 0:
                continue
            # Walking, held back by the drag or a Daze
            if e["dazed"] > 0:
                e["dazed"] -= dt
                if measuring:
                    held += dt
                continue
            slow = min(slow_cap, p["drag"] + per_stack * e["stacks"]) if inside else 0.0
            if measuring:
                held += slow * dt
            e["x"] += walk * (1 - slow) * dt
        # Knell: the stone rings on its own beat while anyone inside carries full Toll
        if "Knell" in b:
            knell_clock += dt
            if knell_clock >= 1.0:
                knell_clock -= 1.0
                if any(o["hp"] > 0 and abs(o["x"]) <= half and o["stacks"] >= p["toll_cap"] for o in enemies):
                    for o in enemies:
                        if o["hp"] > 0 and abs(o["x"]) <= half:
                            landed = damage(o, p["knell"])
                            if measuring:
                                dealt += landed
        # Menders heal the others near them; a Silenced one heals nobody, and what it would have healed counts
        for m in enemies:
            if not m["mender"] or m["hp"] <= 0:
                continue
            silenced = "Ward" in b and abs(m["x"]) <= half
            for o in enemies:
                if o is m or o["hp"] <= 0 or abs(o["x"] - m["x"]) > p["mender_reach"]:
                    continue
                amount = min(p["mender_heal"] * dt, foe.health - o["hp"])
                if silenced:
                    if measuring:
                        dealt += amount
                else:
                    o["hp"] += amount
        enemies = [e for e in enemies if e["hp"] > 0 and e["x"] <= outer + 1]
    seconds = p["seconds"] - p["warm_up"]
    if menders:
        worth = paper.NEIGHBOURS * paper._held_back().get(worth_name, 0.0)
        return (dealt + held * worth) / seconds, held / seconds
    return dealt / seconds, held / seconds


def stacks_on_leaving(b, p, foe_speed=1.0):
    """The Toll an enemy carries out of the field on one straight crossing: the pace check behind Toll's numbers."""
    every = p["quick_toll"] if "Quick Toll" in b else p["toll_every"]
    cap = p["meltdown_cap"] if "Meltdown" in b else p["toll_cap"]
    per_stack = (p["drag_per_stack"] + p["deep_pull"] * ("Deep Pull" in b) + p["deep_pull_2"] * ("Deep Pull II" in b)
                 + p["burden_per_stack"] * ("Burden" in b))
    slow_cap = p["burden_cap"] if "Burden" in b else p["slow_cap"]
    half = _half(_range(b, p))
    x, stacks, clock, dt = -half, 0, 0.0, 1 / paper.TICKS_PER_SECOND
    while x <= half:
        clock += dt
        if clock >= every:
            clock -= every
            stacks = min(cap, stacks + 1)
        x += paper.WALKER_SPEED * foe_speed * (1 - min(slow_cap, p["drag"] + per_stack * stacks)) * dt
    return stacks
