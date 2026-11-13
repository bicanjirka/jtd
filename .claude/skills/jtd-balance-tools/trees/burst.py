"""The next-gen Burst alone: a stone's blast in zones on a random enemy, the bar its base shot and its passive's
deaths fill, and the abilities every unit charges. What it kills, it kills itself (deaths are its own damage over an
enemy's health, at most the stream), so a full defence, where other towers' kills pay its bar, reads higher in play.
"""
import paper

DOC = "docs/features/FEATURE-next-gen-splash-tower.md"
PRICE = 15
ROLE = "crowd"

P = dict(
    damage=14.0, interval=1.0, range=2.5, blast=0.75, blast_edge=1.25, edge_share=0.5,
    long_throw=0.7, wide_blast=1.0, wide_edge=1.75, flint=10.0, heavy_stones=7.0, long_throw_2=1.0,
    quick_hands=0.85, piercing=40.0,
    bar=200.0, soul_harvest=2.0,
    static=15.0, static_below=0.1, soulbrand=10.0, soulbrand_reach=1.0,
    arc_cost=2.0, arc_jumps=3, arc_share=0.4, chain=3, high_voltage=0.6, conductor=0.15, crit_damage=1.5,
    feedback=0.5, thunder_cost=3.0, thunderclap=0.4, thunderstrike=2.0,
    hex_cost=2.0, hex_seconds=4.0, doom=0.3, deep_doom=0.45, vulnerable=0.15,
    blight_share=0.03, blight_slow=0.3, cloud_reach=1.0, twin=2,
)

NODES = {
    "Long Throw": "range",
    "Wide Charge": "blast zones",
    "Flint Stones": "armor penetration",
    "Stormcaller": "the fork; Static: a tagged enemy low on health discharges, and its death fills the Spark bar",
    "Hexer": "the fork; Soulbrand: a tagged enemy bursts on death, and its death fills the Soul bar",
    "Arc": "jumps from the landing, magic; a lone enemy takes none without Wild Arc",
    "Hex of Doom": "returns a share of what its enemy took while hexed, from this tower and the towers around it; nothing if it dies first",
    "Hex of Blight": "poison while it lasts, and its slow, growing to its top as the hex ends (control)",
    "Deep Doom": "Doom's share",
    "Choking Blight": "utility: Silence on a Blighted enemy",
    "Agony": "hexed enemies Vulnerable, by the share of the range the hexes cover",
    "Contagion": "a hex outlives its enemy: without it a hex ends with the enemy",
    "Soul Harvest": "the base shot fills the Soul bar faster",
    "Hex of Inversion": "utility: turns healing, shields and vanishing against its enemy",
    "Blight Cloud": "Blight curses the enemies within reach of its target too",
    "Twin Hex": "each hex curses more enemies, in a crowd",
    "Chain Lightning": "more Arc jumps, in a crowd",
    "Wild Arc": "the Arc may hit an enemy again: a lone enemy takes every jump",
    "Conductor": "Arc jumps crit",
    "Short Circuit": "armor raises the Arc's damage",
    "Thunderclap": "magic to every enemy in range",
    "Thunderstrike": "magic to the healthiest enemy",
    "High Voltage": "Arc's share",
    "Feedback": "an Arc crit gives back part of a Spark",
    "Quick Hands": "reload",
    "Heavy Stones": "damage: every ability is a share of it",
    "Long Throw II": "range",
    "Piercing Stones": "armor penetration",
}

_T0 = ["Long Throw", "Wide Charge", "Flint Stones"]
_SHARED = ["Heavy Stones", "Long Throw II", "Quick Hands", "Piercing Stones"]
_STORM = ["Stormcaller", "Arc", "Conductor", "Short Circuit", "High Voltage", "Feedback"]
_HEX = ["Hexer", "Agony", "Contagion", "Soul Harvest", "Twin Hex"]
BUILDS = {
    # Neither fork, only the base tree: the floor no tower may fall below (tree_calc's bounds)
    "base": _T0 + _SHARED,
    "storm crowd": _T0 + _STORM + ["Chain Lightning", "Thunderclap"] + _SHARED,
    "storm boss": _T0 + _STORM + ["Wild Arc", "Thunderstrike"] + _SHARED,
    "doom": _T0 + _HEX + ["Hex of Doom", "Deep Doom", "Hex of Inversion"] + _SHARED,
    "blight": _T0 + _HEX + ["Hex of Blight", "Blight Cloud", "Choking Blight"] + _SHARED,
}


def reach(b, p):
    return p["range"] + p["long_throw"] * ("Long Throw" in b) + p["long_throw_2"] * ("Long Throw II" in b), 0.0


def _death_units(health):
    """What a death fills, in bars: steps of the dead enemy's max health."""
    return 0.25 if health < 150 else 0.5 if health < 500 else 1.0 if health < 1500 else 2.0


def rate(b, scenario, p):
    foe, crowd = scenario.foe, scenario.crowd
    health = foe.health
    damage = p["damage"] + p["heavy_stones"] * ("Heavy Stones" in b)
    interval = p["quick_hands"] if "Quick Hands" in b else p["interval"]
    pen = p["flint"] * ("Flint Stones" in b) + p["piercing"] * ("Piercing Stones" in b)
    zones = ([(p["wide_blast"], 1.0), (p["wide_edge"], p["edge_share"])] if "Wide Charge" in b
             else [(p["blast"], 1.0), (p["blast_edge"], p["edge_share"])])
    in_range = paper.path_cells(reach(b, p)[0]) * paper.density(foe) if crowd else 1.0
    stream = paper.STREAM_RATE if crowd else float("inf")

    def magic(amount):
        return min(paper.hit(amount, foe, magic=True), health)

    base = paper.zone_hits(zones, foe, damage, crowd, armor_pen=pen) / interval
    storm, hexer = "Stormcaller" in b, "Hexer" in b
    fill = base / p["bar"] * (p["soul_harvest"] if "Soul Harvest" in b else 1.0)

    jumps = 0
    if "Arc" in b:
        jumps = p["arc_jumps"] + p["chain"] * ("Chain Lightning" in b)
        if not crowd and "Wild Arc" not in b:
            jumps = 0
    crit = p["conductor"] if "Conductor" in b and paper.can_crit(foe) else 0.0
    crit = paper.crit_chance(crit, foe)
    arc_hit = damage * (p["high_voltage"] if "High Voltage" in b else p["arc_share"])
    if "Short Circuit" in b:
        arc_hit *= 1 + foe.armor / (100 + foe.armor)
    arc_hit = magic(arc_hit) * (1 + crit * (paper.crit_factor(p["crit_damage"], foe) - 1))
    # Each crit of a jump gives part of a Spark back, and every unit charges every ability, so it raises them all
    refund = p["feedback"] * jumps * crit / p["arc_cost"] if "Feedback" in b else 0.0

    total, control = base, 0.0
    for _ in range(30):
        deaths = min(stream, total / health) if health != float("inf") else 0.0
        units = fill + (deaths * _death_units(health) if storm or hexer else 0.0)
        units /= max(0.05, 1 - refund)
        extra, control = 0.0, 0.0
        vulnerable = 0.0
        if storm:
            extra += deaths * magic(min(p["static"], p["static_below"] * health))
            if "Arc" in b:
                extra += units / p["arc_cost"] * jumps * arc_hit
            if "Thunderclap" in b:
                extra += units / p["thunder_cost"] * in_range * magic(damage * p["thunderclap"])
            if "Thunderstrike" in b:
                extra += units / p["thunder_cost"] * magic(damage * p["thunderstrike"])
        if hexer:
            neighbours = 2 * p["soulbrand_reach"] * paper.density(foe) if crowd else 0.0
            extra += deaths * neighbours * magic(p["soulbrand"])
            life = in_range / deaths if deaths else float("inf")
            seconds = p["hex_seconds"] if "Contagion" in b else min(p["hex_seconds"], life)
            targets = p["twin"] if "Twin Hex" in b and crowd else 1
            casts = units / p["hex_cost"]
            hexed = casts * targets * seconds
            if "Hex of Blight" in b and "Blight Cloud" in b and crowd:
                hexed += casts * 2 * p["cloud_reach"] * paper.density(foe) * seconds
            # A hex on an enemy that has one only renews it: no more are hexed at once than the range holds
            hexed = min(hexed, in_range)
            if "Hex of Doom" in b:
                # The healthiest enemy without a Doom: it pays only if it outlives the Doom, and one at a time
                per_enemy = total / in_range + paper.neighbours(scenario)
                lives = health / per_enemy if per_enemy else float("inf")
                if lives >= p["hex_seconds"]:
                    dooms = min(casts * targets, in_range / p["hex_seconds"])
                    taken = min(per_enemy * p["hex_seconds"], health)
                    extra += dooms * magic(taken * (p["deep_doom"] if "Deep Doom" in b else p["doom"]))
            if "Hex of Blight" in b:
                tick = paper.hit(p["blight_share"] * damage, foe)
                extra += hexed / seconds * min(tick * paper.TICKS_PER_SECOND * seconds, health)
                control += hexed * p["blight_slow"] * seconds / p["hex_seconds"] / 2
            if "Agony" in b:
                vulnerable = p["vulnerable"] * min(1.0, hexed / in_range)
        new_total = (base + extra) * (1 + vulnerable)
        if abs(new_total - total) < 1e-6:
            break
        total = new_total
    return total, control
