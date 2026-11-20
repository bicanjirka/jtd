"""The next-gen Mortar: the shares its magazine fires, each shell's blast in zones on the crowd it lands on, and the
grounds, bleeds and dazes it leaves. Hits are capped at an enemy's health; damage spread over several shells is
not, so a crowd of small enemies reads a little high, except a Barrage's three shells on the same spot, whose blasts
are capped together and whose bleeds, grounds and Dazes only renew one another."""
from functools import lru_cache

import area_sim
import paper

DOC = "docs/features/shipped/FEATURE-next-gen-mortar-tower.md"
PRICE = 20
ROLE = "crowd"

P = dict(
    damage=32.0, interval=3.5, range=4.5, dead_zone=1.5,
    blast=1.0, blast_edge=1.75, edge_share=0.5,
    normal_weight=9.0, heavy_weight=1.0, big_bore=2.0, big_bore_2=4.0, payload_weight=3.0, flare_weight=1.0,
    heavy=2.0, overcharge=3.0, heavy_charge=10.0, elevation=1.0, elevation_2=1.0, quick_reload=3.0,
    nuke_blast=2.0, nuke_edge=2.75, nuke_edge_share=0.75, fallout_radius=2.2, fallout_life=4.0, fallout_share=0.02,
    payload_damage=0.5, payload_life=3.0, lasting_mix=1.0, thick_mix=0.5,
    napalm_radius=1.0, napalm_share=0.04, tar_radius=1.2, tar_share=0.015, cryo_radius=1.2,
    flare_blast=0.75, bleed_share=0.05, bleed_seconds=4.0, daze=1.0, daze_edge=0.5,
    crack=5.0, deep_crack=10.0, crack_insulation=5.0,
    # Placeholder in the doc: how much slower a Volley's three shells reload than three single ones
    volley_reload=2.5,
    draws=20000,
)

NODES = {
    "Elevation": "range",
    "Heavy Charge": "damage",
    "Big Bore": "Heavy weight",
    "Predictive Fire": "utility: keeps fast enemies from walking out of a shell; Walkers don't",
    "Concussion": "control: Heavy dazes its blast",
    "Shrapnel": "Heavy bleeds its blast, by the cells each enemy walks",
    "Napalm": "payload shells: half damage, magic blast, burning ground",
    "Tar": "payload shells: half damage, a poisoned slowing ground (control)",
    "Cryo": "payload shells: half damage, frost ground that freezes (control)",
    "Flare": "utility: Reveal; in the model a small shell that dilutes the rest",
    "Barrage": "three shells on one target, reloading slower",
    "Carpet Bombing": "three shells along the path behind the target, reloading slower; a lone enemy takes one",
    "Overcharge": "Heavy multiplier",
    "Big Bore II": "Heavy weight",
    "Thick Mix": "payload blast and ground radius",
    "Fusion": "two Heavies in a row fuse into a Nuke with Fallout",
    "Rich Mix": "never the same kind twice in a row",
    "Lasting Mix": "payload ground time",
    "Deep Crack": "Crack takes more plating",
    "Quick Reload": "reload",
    "Elevation II": "range",
}

_T0 = ["Elevation", "Heavy Charge", "Big Bore", "Predictive Fire"]
_LATE = ["Overcharge", "Quick Reload", "Elevation II", "Deep Crack"]
BUILDS = {
    "heavy steady": _T0 + ["Shrapnel", "Carpet Bombing", "Big Bore II", "Rich Mix"] + _LATE,
    "heavy bursts": _T0 + ["Shrapnel", "Carpet Bombing", "Big Bore II", "Fusion"] + _LATE,
    "heavy daze": _T0 + ["Concussion", "Carpet Bombing", "Big Bore II", "Rich Mix"] + _LATE,
    "heavy boss": _T0 + ["Shrapnel", "Barrage", "Big Bore II", "Fusion"] + _LATE,
    "napalm": _T0 + ["Shrapnel", "Napalm", "Flare", "Carpet Bombing", "Thick Mix", "Rich Mix", "Lasting Mix"] + _LATE,
    "tar": _T0 + ["Shrapnel", "Tar", "Flare", "Carpet Bombing", "Thick Mix", "Rich Mix", "Lasting Mix"] + _LATE,
    "cryo": _T0 + ["Shrapnel", "Cryo", "Flare", "Carpet Bombing", "Thick Mix", "Rich Mix", "Lasting Mix"] + _LATE,
    "hybrid": _T0 + ["Shrapnel", "Napalm", "Carpet Bombing", "Big Bore II", "Thick Mix", "Rich Mix",
                     "Lasting Mix"] + _LATE,
}

_PAYLOADS = ("Napalm", "Tar", "Cryo")


def reach(b, p):
    return p["range"] + p["elevation"] * ("Elevation" in b) + p["elevation_2"] * ("Elevation II" in b), p["dead_zone"]


@lru_cache(maxsize=None)
def _magazine(weights, rich, fusion, draws):
    return area_sim.magazine(dict(weights), rich, fusion, draws=draws)


@lru_cache(maxsize=None)
def _ground(kind, radius, life, crowd, speed):
    """Damage (a share of 1 damage a tick), freezes, enemy-seconds held back: averaged over the pulse's phase."""
    spacing, behind = (area_sim.DEFAULT_SPACING * speed, 12.0) if crowd else (10 ** 6, 0.0)
    runs = [area_sim.ground(kind, radius, life, 1.0, 1.0, area_sim.WALKER_SPEED * speed, spacing, phase, behind)
            for phase in range(0, area_sim.PULSE_TICKS, 2)]
    return tuple(sum(r[i] for r in runs) / len(runs) for i in (0, 1, 4))


def _hit_at(profile, x):
    """A shell's hit on an enemy `x` cells from where it lands: its first zone that reaches that far."""
    return next((hit for radius, hit in profile if x < radius), 0.0)


def _blast_total(profile, foe, crowd):
    if not crowd:
        return min(profile[0][1], foe.health)
    total, inner = 0.0, 0.0
    for radius, hit in profile:
        total += 2 * (radius - inner) * paper.density(foe) * min(hit, foe.health)
        inner = radius
    return total


def _barrage(profiles, shares, foe, crowd):
    """Three shells from the magazine on one spot, each enemy's sum of the three capped at its health."""
    def three(x):
        return min(3 * sum(share * _hit_at(profiles[k], x) for k, share in shares.items()), foe.health)
    if not crowd:
        return three(0.0)
    radii = sorted({r for k in shares for r, _ in profiles[k]})
    total, inner = 0.0, 0.0
    for radius in radii:
        total += 2 * (radius - inner) * paper.density(foe) * three((inner + radius) / 2)
        inner = radius
    return total


def rate(b, scenario, p):
    foe, crowd = scenario.foe, scenario.crowd
    damage = p["damage"] + p["heavy_charge"] * ("Heavy Charge" in b)
    payload = next((k for k in _PAYLOADS if k in b), None)
    heavy_weight = p["big_bore_2"] if "Big Bore II" in b else p["big_bore"] if "Big Bore" in b else p["heavy_weight"]
    weights = {"N": p["normal_weight"], "H": heavy_weight}
    if payload:
        weights["P"] = p["payload_weight"]
    if "Flare" in b:
        weights["F"] = p["flare_weight"]
    shares = _magazine(tuple(sorted(weights.items())), "Rich Mix" in b, "Fusion" in b, p["draws"])

    crack = p["deep_crack"] if "Deep Crack" in b else p["crack"]

    profiles = {}

    def blast(kind, amount, zones, magic=False):
        """A shell's blast, its hit on one enemy in each zone kept for a Barrage, which caps its three together."""
        shift = -(p["crack_insulation"] if magic else crack)
        profiles[kind] = profiles.get(kind, []) + [(r, paper.hit(amount * share, foe, magic=magic, flat_shift=shift))
                                                   for r, share in zones]
        return paper.zone_hits(zones, foe, amount, crowd, magic=magic, flat_shift=shift)

    def reached(zones):
        return 2 * zones[-1][0] * paper.density(foe) if crowd else 1.0

    def ground(kind, radius, life, share, magic):
        dealt, freezes, slowed = _ground(kind, radius, life, crowd, foe.speed)
        tick = share * damage
        factor = paper.hit(tick, foe, magic=magic, flat_shift=-crack) / tick if tick else 0.0
        return dealt * tick * factor, freezes, slowed

    zones = [(p["blast"], 1.0), (p["blast_edge"], p["edge_share"])]
    heavy = damage * (p["overcharge"] if "Overcharge" in b else p["heavy"])
    dealt, control = {}, {}
    dealt["N"] = blast("N", damage, zones)
    dealt["H"] = blast("H", heavy, zones)
    control["H"] = 0.0
    if "Shrapnel" in b:
        bleed = p["bleed_share"] * heavy * area_sim.WALKER_SPEED * p["bleed_seconds"]
        dealt["H"] += reached(zones) * min(bleed, foe.health)
    if "Concussion" in b:
        inner = 2 * p["blast"] * paper.density(foe) if crowd else 1.0
        edge = 2 * (p["blast_edge"] - p["blast"]) * paper.density(foe) if crowd else 0.0
        control["H"] = inner * p["daze"] + edge * p["daze_edge"]
    nuke_zones = [(p["nuke_blast"], 1.0), (p["nuke_edge"], p["nuke_edge_share"])]
    fallout, _, _ = ground("burn", p["fallout_radius"], p["fallout_life"], p["fallout_share"], False)
    dealt["Nuke"] = blast("Nuke", 2 * heavy, nuke_zones) + fallout
    if payload:
        grow = p["thick_mix"] * ("Thick Mix" in b)
        payload_zones = [(r + grow, s) for r, s in zones]
        life = p["payload_life"] + p["lasting_mix"] * ("Lasting Mix" in b)
        if payload == "Napalm":
            burn, _, _ = ground("burn", p["napalm_radius"] + grow, life, p["napalm_share"], True)
            dealt["P"] = blast("P", damage * p["payload_damage"], payload_zones, magic=True) + burn
            control["P"] = 0.0
        elif payload == "Tar":
            poison, _, slowed = ground("tar", p["tar_radius"] + grow, life, p["tar_share"], False)
            dealt["P"] = blast("P", damage * p["payload_damage"], payload_zones) + poison
            control["P"] = slowed
        else:
            _, _, slowed = ground("frost", p["cryo_radius"] + grow, life, 0.0, False)
            dealt["P"] = blast("P", damage * p["payload_damage"], payload_zones)
            control["P"] = slowed
    dealt["F"] = blast("F", damage, [(p["flare_blast"], 1.0)])

    shots = 1 / (p["quick_reload"] if "Quick Reload" in b else p["interval"])
    per_shell = 1.0
    if "Barrage" in b or "Carpet Bombing" in b:
        shots *= 3 / p["volley_reload"]
        if "Carpet Bombing" in b and not crowd:
            per_shell = 1 / 3
    damage_rate = shots * per_shell * sum(share * dealt[k] for k, share in shares.items())
    if "Barrage" in b:
        # The three land on the same enemies, so what they deal one enemy together is capped at its health
        apart = sum(share * _blast_total(profiles[k], foe, crowd) for k, share in shares.items())
        together = _barrage(profiles, shares, foe, crowd)
        damage_rate -= shots * (apart - together / 3)
    control_rate = shots * per_shell * sum(share * control.get(k, 0.0) for k, share in shares.items())
    if "Barrage" in b:
        # A bleed, a ground or a Daze on the same spot only renews: a volley leaves each kind's once, if a shell of it
        # is in
        lasting = {k: dealt[k] - _blast_total(profiles[k], foe, crowd) for k in shares}
        held = {k: control.get(k, 0.0) for k in shares}
        damage_rate -= shots * sum(share * lasting[k] for k, share in shares.items())
        damage_rate += shots / 3 * sum((1 - (1 - share) ** 3) * lasting[k] for k, share in shares.items())
        control_rate -= shots * sum(shares[k] * v for k, v in held.items())
        control_rate += shots / 3 * sum((1 - (1 - shares[k]) ** 3) * v for k, v in held.items())
    return damage_rate, control_rate
