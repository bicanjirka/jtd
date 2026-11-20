"""The next-gen Sniper, shot by shot: Steady Aim at its steady state, crits rolled per shot, kills one enemy after
another so overkill, executions and kill procs count."""
import random

import paper

DOC = "docs/features/shipped/FEATURE-next-gen-sniper-tower.md"
PRICE = 15
ROLE = "single"

P = dict(
    damage=32.0, interval=2.5, range=3.0, crit=0.0, crit_damage=1.5,
    stacks=5, stack_speed=0.04, stack_crit=0.02,
    rifling=0.05, long_barrel=1.0, heavy_rounds=12.0,
    fourth=4, deep_breath=3, keen_eye=0.02, deadeye=2.0,
    frenzy=0.35, frenzy_shots=3, ricochet_reach=1.5,
    sunder_stacks=2, sunder_armor=5.0, sunder_cap=10, sunder_seconds=5.0,
    silver_pen=50.0, hollow=0.33, long_barrel_2=1.0,
    heavy_rounds_2=12.0, execute_below=0.15, headhunter=14.0,
    shots=2500, seed=7,
)

NODES = {
    "Rifling": "crit chance",
    "Long Barrel": "range",
    "Heavy Rounds": "damage",
    "Fourth Shot": "every n-th shot a sure crit",
    "Deep Breath": "more Steady Aim stacks",
    "Keen Eye": "crit chance per stack",
    "Deadeye": "crit damage",
    "Frenzy": "attack speed for the shots after a crit; a crit during it doesn't restart it",
    "Ricochet": "a crit's hop to the enemy behind, in a crowd only; it can't crit",
    "Sundering Crits": "armor off the crit's enemy, on a shared clock",
    "Silver Round": "the n-th shot magic with magic penetration",
    "Hollow Point": "the n-th shot hits harder",
    "Long Barrel II": "range",
    "Heavy Rounds II": "damage",
    "Executioner": "a crit that leaves a normal enemy low kills it",
    "Headhunter": "damage against Elites and Bosses",
    "Unbroken Aim": "utility: reaches Steady Aim's cap sooner; at the steady state it adds nothing",
    "Momentum": "a kill grants Frenzy",
}

_CORE = ["Rifling", "Long Barrel", "Heavy Rounds", "Fourth Shot", "Deadeye", "Long Barrel II", "Heavy Rounds II",
         "Unbroken Aim", "Momentum"]
BUILDS = {
    "fast": _CORE + ["Deep Breath", "Frenzy", "Hollow Point", "Executioner"],
    "fast boss": _CORE + ["Deep Breath", "Frenzy", "Hollow Point", "Headhunter"],
    "crit size": _CORE + ["Keen Eye", "Frenzy", "Hollow Point", "Headhunter"],
    "crowd": _CORE + ["Deep Breath", "Ricochet", "Hollow Point", "Executioner"],
    "armor": _CORE + ["Deep Breath", "Sundering Crits", "Silver Round", "Executioner"],
}


def reach(b, p):
    return p["range"] + p["long_barrel"] * ("Long Barrel" in b) + p["long_barrel_2"] * ("Long Barrel II" in b), 0.0


def rate(b, scenario, p):
    foe = scenario.foe
    damage = p["damage"] + p["heavy_rounds"] * ("Heavy Rounds" in b) + p["heavy_rounds_2"] * ("Heavy Rounds II" in b)
    if foe.boss_or_elite() and "Headhunter" in b:
        damage += p["headhunter"]
    stacks = p["stacks"] + p["deep_breath"] * ("Deep Breath" in b)
    speed = stacks * p["stack_speed"]
    chance = p["crit"] + p["rifling"] * ("Rifling" in b) + stacks * (p["stack_crit"] + p["keen_eye"] * ("Keen Eye" in b))
    chance = paper.crit_chance(chance, foe)
    multiplier = p["deadeye"] if "Deadeye" in b else p["crit_damage"]
    fourth = p["fourth"] if "Fourth Shot" in b else 0
    silver, hollow = "Silver Round" in b, "Hollow Point" in b
    ricochet = "Ricochet" in b and scenario.crowd
    executes = "Executioner" in b and foe.rank == "normal"

    rng = random.Random(p["seed"])
    health = [foe.health, foe.health]
    sunder, sunder_until = 0, 0.0
    time = dealt = 0.0
    frenzy = 0
    for shot in range(1, p["shots"] + 1):
        time += p["interval"] / (1 + speed + (p["frenzy"] if frenzy else 0))
        if frenzy:
            frenzy -= 1
        roll = rng.random()
        primed = bool(fourth) and shot % fourth == 0
        crit = paper.can_crit(foe) and (primed or roll < chance)
        if time > sunder_until:
            sunder = 0
        amount = damage * (1 + p["hollow"]) if primed and hollow else damage
        magic = primed and silver
        landed = paper.hit(amount, foe, magic=magic, crit=crit, crit_multiplier=multiplier,
                           magic_pen=p["silver_pen"] if magic else 0.0, armor_shift=-sunder * p["sunder_armor"])
        left = health[0] - landed
        if crit and executes and 0 < left < p["execute_below"] * foe.health:
            landed = health[0]
        dealt += min(landed, health[0])
        health[0] -= landed
        if crit and "Sundering Crits" in b:
            sunder, sunder_until = min(p["sunder_cap"], sunder + p["sunder_stacks"]), time + p["sunder_seconds"]
        if crit and ricochet:
            hop = paper.hit(amount, foe, magic=magic, magic_pen=p["silver_pen"] if magic else 0.0)
            dealt += min(hop, health[1])
            health[1] -= hop
        if crit and "Frenzy" in b and not frenzy:
            frenzy = p["frenzy_shots"]
        if health[0] <= 0:
            health = [health[1] if scenario.crowd else foe.health, foe.health]
            sunder = 0
            if "Momentum" in b and not frenzy:
                frenzy = p["frenzy_shots"]
    return dealt / time
