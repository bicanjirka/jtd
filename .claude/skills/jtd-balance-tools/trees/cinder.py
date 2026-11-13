"""The next-gen Scorcher, tick by tick: a flame stream that pours fuel into every enemy in its cone, and each enemy's
burn, which burns its fuel at a rate growing with the square root of what it holds, so a pool deals exactly its fuel
(before defences) and a bigger one burns longer as well as harder.

A crowd walks a straight path a cell from the tower; the cone aims at its target's bearing, so how many enemies it
reaches comes from that geometry. One enemy at a time stands in the Core of the cone until it dies, and the next
takes its place at once, as in the other models. Overkill is lost: fuel an enemy still holds when it dies goes,
unless Flashover passes it on.
"""
import math
from functools import lru_cache

import paper

DOC = "docs/features/FEATURE-next-gen-cinder-tower.md"
PRICE = 20
ROLE = "crowd"
# Two Scorchers on one crowd pour into the same pools, so together they reach a Flashover neither reaches alone
SCENARIOS = [paper.Scenario("pair 1000", paper.Foe("walker", 1000), True, 1, copies=2)]

P = dict(
    # Fuel a second the flame pours into each enemy in its cone
    flame=60.0, stream=1.0, reload=2.0, range=2.2, cone=60.0,
    # A pool of F fuel burns for burn_time * sqrt(F / 100) seconds
    burn_time=1.75,
    hot_flame=15.0, long_stream=0.5, wide_nozzle=0.5, long_nozzle=0.5,
    core_share=0.25, core=1.4,
    lance_range=0.65, lance_cone=0.6, fan_cone=1.4, fan_range=0.0, bellows=0.5,
    dragon=0.65, dragon_stream=2.0, dragon_reload=1.0, white_flame=30.0,
    flashover_reach=1.5, flashover_split=1, flashover_health=0.0, melt_feed=0.006, melt_floor=0.0, others=0.0, soulfire_flame=0.1, soulfire_pen=0.5, flash_share=0.5, flash_fuel=100.0, sunder_per_second=1.0, sunder_armor=5.0, sunder_cap=10, sunder_seconds=5.0,
    thermal=1.35, chilled_share=0.25,
    hot_flame_2=15.0, long_stream_2=1.0,
    stream_runs_out=1,
    seconds=90.0, warm_up=15.0,
)

NODES = {
    "Hot Flame": "Flame",
    "Long Stream": "stream length",
    "Wide Nozzle": "cone width",
    "Long Nozzle": "range",
    "Core": "the inner share of the cone pours more",
    "Lance": "range up, cone narrower",
    "Fan": "cone wider, range down",
    "Bellows": "reload",
    "Kindling": "utility: more Scorch, which lowers resilience for the crits of the towers around it",
    "Soulfire": "burns what is burn-immune and passes part of the magic resist (Sicken is utility)",
    "Dragon's Breath": "a longer stream and a shorter reload, a weaker flame",
    "White Flame": "Flame",
    "Flashover": "a hot burn pours part of the Flame into the enemies beside it",
    "Meltdown": "the burn grows with armor and plating, never by less than a floor",
    "Thermal Shock": "more Flame on an enemy that is chilled, frozen or dazed; a share of enemies are",
    "Hot Flame II": "Flame",
    "Long Stream II": "stream length",
}

_T0 = ["Hot Flame", "Long Stream", "Wide Nozzle", "Long Nozzle"]
_T1 = ["Core"]
_T2 = ["White Flame", "Bellows"]
_T3 = ["Hot Flame II", "Long Stream II"]
BUILDS = {
    "crowd": _T0 + _T1 + ["Fan"] + _T2 + ["Dragon's Breath"] + _T3 + ["Flashover"],
    "reach": _T0 + _T1 + ["Lance"] + _T2 + ["Soulfire"] + _T3 + ["Flashover"],
    "heavy": _T0 + _T1 + ["Lance"] + _T2 + ["Soulfire"] + _T3 + ["Thermal Shock"],
    "team": _T0 + _T1 + ["Fan", "Kindling"] + _T2 + ["Soulfire"] + _T3 + ["Meltdown"],
}


def _range(b, p):
    return (p["range"] + p["long_nozzle"] * ("Long Nozzle" in b) + p["lance_range"] * ("Lance" in b)
            + p["fan_range"] * ("Fan" in b))


def reach(b, p):
    return _range(b, p), 0.0


def _spec(b, p):
    flame = p["flame"] + p["hot_flame"] * ("Hot Flame" in b) + p["white_flame"] * ("White Flame" in b) \
        + p["hot_flame_2"] * ("Hot Flame II" in b)
    stream = p["stream"] + p["long_stream"] * ("Long Stream" in b) + p["long_stream_2"] * ("Long Stream II" in b)
    reload = p["reload"]
    if "Soulfire" in b:
        flame *= 1 + p["soulfire_flame"]
    if "Dragon's Breath" in b:
        flame, stream, reload = flame * p["dragon"], stream * p["dragon_stream"], p["dragon_reload"]
    if "Bellows" in b:
        reload = max(0.0, reload - p["bellows"])
    cone = p["cone"] * (1 + p["wide_nozzle"] * ("Wide Nozzle" in b))
    cone *= p["lance_cone"] if "Lance" in b else p["fan_cone"] if "Fan" in b else 1.0
    return flame, stream, reload, cone


@lru_cache(maxsize=None)
def _run(key, foe, crowd, copies, p_items):
    p = dict(p_items)
    b = frozenset(key)
    flame, stream, reload, cone = _spec(b, p)
    rng_ = _range(b, p)
    half = math.radians(cone) / 2
    core_half = half * p["core_share"] if "Core" in b else -1.0
    tps = paper.TICKS_PER_SECOND
    # The copies stand together and stream in step, so each pool takes all of their fuel
    per_tick = flame * copies / tps
    # Burn rate c * sqrt(F) a second empties F in 2 sqrt(F) / c: burn_time at 100 fuel
    c = 2 * math.sqrt(100) / p["burn_time"]
    speed = paper.WALKER_SPEED * foe.speed / tps
    spacing = paper.SPACING * foe.speed
    thermal = p["thermal"] if "Thermal Shock" in b else 1.0
    melt = "Meltdown" in b
    # The towers around it (paper.NEIGHBOURS bare reference towers sharing their fire in a crowd) kill enemies that
    # still burn; `others` overrides them
    others = p["others"] if p["others"] >= 0 else paper.NEIGHBOURS * _held(foe, crowd)

    enemies = []  # dicts: x, health, fuel, acc, clock, chilled, burning_s
    spawned = 0
    ticks, warm = int(p["seconds"] * tps), int(p["warm_up"] * tps)
    dealt = 0.0
    melt_value = 0.0
    state, left, target = "ready", 0, None

    def spawn():
        nonlocal spawned
        every = round(1 / p["chilled_share"]) if p["chilled_share"] > 0 else 0
        chilled = bool(every) and spawned % every == 0
        e = dict(x=-rng_ - 0.5 if crowd else 0.0, health=foe.health, fuel=0.0, acc=0.0, clock=0, chilled=chilled,
                 sunder=0, sunder_t=0, alive=True)
        spawned += 1
        enemies.append(e)
        return e

    if not crowd:
        spawn()

    def in_range(e):
        return e["alive"] and (not crowd or e["x"] * e["x"] + 1 <= rng_ * rng_)

    def bearing(e):
        return math.atan2(e["x"], 1.0) if crowd else 0.0

    next_spawn = 0.0
    for t in range(ticks):
        counting = t >= warm
        if crowd:
            if t >= next_spawn:
                spawn()
                next_spawn += spacing / speed
            for e in enemies:
                e["x"] += speed
        live = [e for e in enemies if in_range(e)]
        # The tower
        if state == "reload":
            left -= 1
            if left <= 0:
                state = "ready"
        if state == "ready" and live:
            state, left = "stream", stream * tps
        if state == "stream":
            if target is None or not in_range(target):
                target = min(live, key=lambda e: e["x"] * e["x"]) if live else None
            if target is None and not p["stream_runs_out"]:
                state, left = ("reload", reload * tps) if reload > 0 else ("ready", 0)
            elif target is None:
                # The stream runs its time into the air; the reload is the same
                left -= 1
                if left <= 0:
                    state, left = ("reload", reload * tps) if reload > 0 else ("ready", 0)
            else:
                aim = bearing(target)
                for e in live:
                    off = abs(bearing(e) - aim)
                    if off <= half + 1e-9:
                        amount = per_tick * (p["core"] if off <= core_half + 1e-9 else 1.0)
                        if crowd and e["chilled"]:
                            amount *= thermal
                        elif not crowd:
                            amount *= 1 + (thermal - 1) * p["chilled_share"]
                        e["fuel"] += amount
                        # While it streams, an enemy in the cone whose burn runs hot flashes over onto those beside it
                        if "Flashover" in b and crowd and p["flash_share"] and e["fuel"] >= p["flash_fuel"]:
                            for o in enemies:
                                if o is not e and o["alive"] and abs(o["x"] - e["x"]) <= p["flashover_reach"]:
                                    o["fuel"] += per_tick * p["flash_share"]
                left -= 1
                if left <= 0:
                    state, left = ("reload", reload * tps) if reload > 0 else ("ready", 0)
                    target = None
        # Other towers' damage, which the model doesn't credit to this one: in a full defence they kill burning enemies
        if crowd and others:
            for e in enemies:
                if e["alive"] and e["x"] > -rng_:
                    e["health"] -= others / tps
        # The pools
        for e in enemies:
            if not e["alive"] or e["fuel"] <= 0 and e["acc"] <= 0:
                continue
            burned = min(e["fuel"], c * math.sqrt(e["fuel"]) / tps)
            e["fuel"] -= burned
            e["acc"] += burned
            e["clock"] += 1
            if e["fuel"] < 0.01:
                e["fuel"] = 0.0
            if melt and not p["melt_feed"] and e["fuel"] > 0:
                if t - e["sunder_t"] >= tps:
                    e["sunder"], e["sunder_t"] = min(p["sunder_cap"], e["sunder"] + p["sunder_per_second"]), t
                if counting:
                    # What the towers around it gain on it this tick: their hits pass less armor
                    armor = foe.armor
                    after = max(0.0, armor - e["sunder"] * p["sunder_armor"])
                    gain = (100 + armor) / (100 + after) - 1
                    melt_value += gain * paper.NEIGHBOURS * _held(foe, crowd) / tps
            if e["clock"] % 5 == 0 or e["fuel"] == 0 or e["health"] <= 0:
                amount = e["acc"]
                if melt and p["melt_feed"]:
                    # Its armor and plating turn against it: each point adds a share to the burn
                    amount *= 1 + max(p["melt_floor"], p["melt_feed"] * (foe.armor + foe.plating))
                # Soulfire passes part of the magic resist
                pen = foe.magic_resist * p["soulfire_pen"] if "Soulfire" in b else 0.0
                hit = paper.hit(amount, foe, magic=True, magic_pen=pen)
                e["acc"] = 0.0
                landed = min(hit, e["health"])
                if counting:
                    dealt += landed
                e["health"] -= hit
                if e["health"] <= 0:
                    e["alive"] = False
                    if "Flashover" in b and crowd and not p["flash_share"]:
                        near = [o for o in enemies if o["alive"] and abs(o["x"] - e["x"]) <= p["flashover_reach"]]
                        for o in near:
                            o["fuel"] += e["fuel"] / (len(near) if p["flashover_split"] else 1)                                 + p["flashover_health"] * foe.health
                    e["fuel"] = 0.0
                    if target is e:
                        target = None
                    if not crowd:
                        enemies.remove(e)
                        spawn()
        if crowd:
            enemies[:] = [e for e in enemies if e["alive"] and (e["fuel"] > 0 or e["x"] < rng_ + 1)]
    seconds = (ticks - warm) / tps
    return dealt / copies / seconds, melt_value / seconds


def _held(foe, crowd):
    name = foe.name if foe.name in {s.name for s in paper.DEFENCES} else ("crowd 300" if crowd else "lone 300")
    return paper._held_back().get(name, 0.0)


def rate(b, scenario, p):
    key = tuple(sorted(b))
    damage, melt = _run(key, scenario.foe, scenario.crowd, scenario.copies, tuple(sorted(p.items())))
    return damage + melt
