"""The next-gen Aerie on paper: a tick simulation of its nest, its missiles, freezes under the freeze meter, the Release
count, Feeding, and the Tier III picks, on a straight path a cell from the tower. Control is enemy-seconds held: a
freeze all of its time, a Chill (Ice Wake) its share. Ruby counts its x2 damage only (no freeze-immune scenario);
Shade's Shield and Silence are utility (no shielded scenario); shade_mr and shade_insulation weigh Shadow damage.
shatter_mode weighs other Shatters: 1 hits the thawed enemy again, 2 lets the tower's freezes fill no freeze meter."""
import math
from functools import lru_cache

import paper

DOC = "docs/features/shipped/FEATURE-next-gen-seeker-tower.md"
PRICE = 30
ROLE = "crowd"

P = dict(
    damage=15.0, load=2.0, freeze=1.0, range=4.0, nest=3.0, missile_speed=4.0, launch_gap=0.2,
    quick_brood=1.6, quick_brood_2=1.3, deep_freeze=0.3, deep_freeze_2=1.0, wide_comb=2.0, wide_comb_2=2.0,
    long_flight=1.0, long_flight_2=1.0, swift_wings=0.5, sharp_sting=10.0,
    release_every=14.0, release_missiles=7.0, hatch_gap=0.4, feeding=0.25,
    second_share=1 / 3, rich_brood=0.5, ruby=2.0,
    wake_radius=0.5, wake_slow=0.3, wake_tail=0.5, shatter_radius=1.0, shatter_share=0.3,
    retarget=2.0, shatter_mode=0.0, shade_mr=0.0, shade_insulation=0.0, # A wave from its start, the nest full and the Release count at none: the first Release takes longest, so a
    # steady stream (warm_up past it) hides what a bigger nest is worth
    seconds=25.0, warm_up=0.0,
)

NODES = {
    "Quick Brood": "load time",
    "Deep Freeze": "freeze time",
    "Wide Comb": "nest size",
    "Long Flight": "range",
    "Swarm": "every Nth freeze, missiles at once at different enemies",
    "Hatching": "every Nth freeze, missiles hatch into the nest one after another",
    "Feeding": "each freeze takes seconds off the next load",
    "Swift Wings": "missile speed",
    "Ruby": "a share of missiles deal x2 (freeze-immune is utility)",
    "Shade": "utility: past Shield, Silence after the thaw",
    "Sharp Sting": "damage",
    "Wide Comb II": "nest size",
    "Long Flight II": "range",
    "Ice Wake": "a frozen enemy chills the ones walking past (control)",
    "Shatter": "a thaw bursts on the enemies near it",
    "Rich Brood": "more of the second kind",
    "Deep Freeze II": "freeze time",
    "Quick Brood II": "load time",
}

_T0 = ["Quick Brood", "Deep Freeze", "Wide Comb", "Long Flight"]
_T1 = ["Feeding", "Swift Wings", "Wide Comb II"]
_T2 = ["Sharp Sting", "Long Flight II"]
_T3 = ["Rich Brood", "Deep Freeze II", "Quick Brood II"]
BUILDS = {
    "swarm wake": _T0 + _T1 + ["Swarm"] + _T2 + ["Ruby"] + _T3 + ["Ice Wake"],
    "swarm shatter": _T0 + _T1 + ["Swarm"] + _T2 + ["Ruby"] + _T3 + ["Shatter"],
    "hatch wake": _T0 + _T1 + ["Hatching"] + _T2 + ["Ruby"] + _T3 + ["Ice Wake"],
    "hatch shatter": _T0 + _T1 + ["Hatching"] + _T2 + ["Ruby"] + _T3 + ["Shatter"],
    "shade": _T0 + _T1 + ["Swarm"] + _T2 + ["Shade"] + _T3 + ["Ice Wake"],
}


def _range(b, p):
    return p["range"] + p["long_flight"] * ("Long Flight" in b) + p["long_flight_2"] * ("Long Flight II" in b)


def reach(b, p):
    return _range(b, p), 0.0


def _drain(health):
    """Rank's freeze drain (Rank.freezeDrain) by the Walker's ladder its health sits on."""
    ladder = [(75, 1.5), (138, 1.0), (458, 0.67), (1175, 0.5), (2400, 0.33)]
    if health <= ladder[0][0]:
        return ladder[0][1]
    for (h0, d0), (h1, d1) in zip(ladder, ladder[1:]):
        if health <= h1:
            t = (math.log(health) - math.log(h0)) / (math.log(h1) - math.log(h0))
            return d0 + t * (d1 - d0)
    return ladder[-1][1]


def rate(b, scenario, p):
    return _run(tuple(sorted(b)), scenario.foe, scenario.crowd, tuple(sorted(p.items())))


@lru_cache(maxsize=None)
def _run(key, foe, crowd, p_items):
    p = dict(p_items)
    b = frozenset(key)
    tps = paper.TICKS_PER_SECOND
    dt = 1 / tps
    rng = _range(b, p)
    half = math.sqrt(max(rng * rng - 1, 0.0))
    load = p["quick_brood_2"] if "Quick Brood II" in b else p["quick_brood"] if "Quick Brood" in b else p["load"]
    freeze = p["freeze"] + p["deep_freeze"] * ("Deep Freeze" in b) + p["deep_freeze_2"] * ("Deep Freeze II" in b)
    cap = int(p["nest"] + p["wide_comb"] * ("Wide Comb" in b) + p["wide_comb_2"] * ("Wide Comb II" in b))
    speed = p["missile_speed"] * (1 + p["swift_wings"] * ("Swift Wings" in b))
    damage = p["damage"] + p["sharp_sting"] * ("Sharp Sting" in b)
    second = "Ruby" if "Ruby" in b else "Shade" if "Shade" in b else None
    share = (p["rich_brood"] if "Rich Brood" in b else p["second_share"]) if second else 0.0
    release = "Swarm" if "Swarm" in b else "Hatching" if "Hatching" in b else None
    drain = _drain(foe.health)
    walk = paper.WALKER_SPEED * foe.speed
    gap = paper.SPACING * foe.speed / walk if crowd else None

    enemies = []  # dicts: x, hp, frozen, meter, slow
    missiles = []  # [target, time left, ruby]
    stored, clock, launch_wait = cap, 0.0, 0.0
    counter, hatch_left, hatch_wait = 0, 0, 0.0
    lot = 0.0
    spawn_wait = 0.0
    dealt = held = 0.0
    ticks, warm = int(p["seconds"] * tps), int(p["warm_up"] * tps)
    start_x = -half - 0.5

    def hit_amount(kind):
        if kind == "Shade":
            return paper.hit(damage, foe, magic=True, magic_pen=p["shade_mr"] * foe.magic_resist,
                             flat_shift=-p["shade_insulation"] * foe.insulation)
        return paper.hit(damage * (p["ruby"] if kind == "Ruby" else 1.0), foe, magic=True)

    def in_range(e):
        return -half <= e["x"] <= half and e["hp"] > 0

    def targets(exclude=()):
        live = [e for e in enemies if in_range(e) and e["frozen"] <= 0 and e not in exclude]
        # Soonest out: equal paths ahead, so the furthest along, a slowed one counted by its speed
        return sorted(live, key=lambda e: (half + 10 - e["x"]) / (walk * (1 - e["slow_share"]) or 1e-9))

    def draw():
        nonlocal lot
        lot += share
        if lot >= 1 - 1e-9:
            lot -= 1
            return second
        return None

    def launch(target, measuring):
        d = math.hypot(target["x"], 1.0)
        missiles.append([target, d / speed, draw()])

    for t in range(ticks):
        measuring = t >= warm
        # Spawning
        if crowd:
            spawn_wait -= dt
            if spawn_wait <= 0:
                enemies.append(dict(x=start_x, hp=foe.health, frozen=0.0, meter=0.0, slow=0.0, slow_share=0.0))
                spawn_wait += gap
        elif not any(e["hp"] > 0 and e["x"] <= half for e in enemies):
            enemies.append(dict(x=start_x, hp=foe.health, frozen=0.0, meter=0.0, slow=0.0, slow_share=0.0))
        # Enemies walk, freeze and thaw
        for e in enemies:
            if e["hp"] <= 0:
                continue
            if e["frozen"] > 0:
                e["frozen"] -= dt
                if not ("Shatter" in b and p["shatter_mode"] == 2):
                    e["meter"] += dt
                if measuring and in_range(e):
                    held += dt
                if e["frozen"] <= 0 and "Shatter" in b and p["shatter_mode"] == 1:
                    amount = min(paper.hit(damage * p["shatter_share"], foe, magic=True), e["hp"])
                    e["hp"] -= amount
                    if measuring:
                        dealt += amount
                if e["frozen"] <= 0 and "Shatter" in b and p["shatter_mode"] == 0:
                    for o in enemies:
                        if o is not e and o["hp"] > 0 and abs(o["x"] - e["x"]) <= p["shatter_radius"]:
                            amount = min(paper.hit(damage * p["shatter_share"], foe, magic=True), o["hp"])
                            o["hp"] -= amount
                            if measuring:
                                dealt += amount
                continue
            e["meter"] = max(0.0, e["meter"] - drain * dt)
            slowed = e["slow"] > 0
            e["slow_share"] = p["wake_slow"] if slowed else 0.0
            if slowed:
                e["slow"] -= dt
                if measuring and in_range(e):
                    held += p["wake_slow"] * dt
            e["x"] += walk * (1 - e["slow_share"]) * dt
        if "Ice Wake" in b:
            for e in enemies:
                if e["hp"] > 0 and e["frozen"] > 0:
                    for o in enemies:
                        if o is not e and o["hp"] > 0 and o["frozen"] <= 0 and abs(o["x"] - e["x"]) <= p["wake_radius"]:
                            o["slow"] = p["wake_tail"]
        enemies = [e for e in enemies if e["hp"] > 0 and e["x"] <= half + 1]
        # The nest loads; Hatching hatches past its size
        if stored < cap:
            clock += dt
            if clock >= load:
                clock -= load
                stored += 1
        else:
            clock = min(clock + dt, load)
        if hatch_left > 0:
            hatch_wait -= dt
            if hatch_wait <= 0:
                stored += 1
                hatch_left -= 1
                hatch_wait += p["hatch_gap"]
        # Launches
        launch_wait -= dt
        if stored > 0 and launch_wait <= 0:
            pick = targets()
            if pick:
                launch(pick[0], measuring)
                stored -= 1
                launch_wait = p["launch_gap"]
        # Missiles land
        landed = []
        for m in missiles:
            m[1] -= dt
            if m[1] > 0:
                continue
            target = m[0]
            if not in_range(target) or target["frozen"] > 0:
                near = [e for e in targets() if abs(e["x"] - target["x"]) <= p["retarget"]]
                if near:
                    m[0], m[1] = near[0], abs(near[0]["x"] - target["x"]) / speed
                    continue
                landed.append(m)
                if stored < cap:
                    stored += 1
                continue
            landed.append(m)
            amount = min(hit_amount(m[2]), target["hp"])
            target["hp"] -= amount
            if measuring:
                dealt += amount
            if amount > 0 and target["hp"] > 0:
                target["frozen"] = freeze / (1 + target["meter"])
                if "Feeding" in b:
                    clock = min(clock + p["feeding"], load)
                if release:
                    counter += 1
                    if counter >= p["release_every"]:
                        counter -= int(p["release_every"])
                        n = int(p["release_missiles"])
                        if release == "Swarm":
                            for e in targets()[:n]:
                                launch(e, measuring)
                        else:
                            hatch_left += n
                            hatch_wait = 0.0
        missiles = [m for m in missiles if m not in landed]
    seconds = p["seconds"] - p["warm_up"]
    return dealt / seconds, held / seconds
