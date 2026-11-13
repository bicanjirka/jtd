"""The next-gen Beacon on paper: it deals no damage, so it is worth what the towers it lights gain.

The towers in its light are paper.NEIGHBOURS reference towers (the Sniper's median build) at the Beacon's own tier:
the towers around a Beacon climb the tiers as it does. Their damage on a scenario's enemies, D, is held_back's bare
figure scaled by the reference's tier curve. Each mechanic is a share of D, so a Beacon reads against a reference copy
as 4 x its share: it beats another copy of the reference where it adds more than a quarter of what four towers deal.
The towers' fire is single-target and crit-built as the reference's, mixed by `turn_share`, `crit_share` and
`physical_share`, and `lit` names the kinds of tower in the light (Kinship's count, Prism's marks).

- Priority (the Spotlight): +15% damage taken on the held enemy while it is in the light; on a crowd, the share of
  the towers' fire that turns to it.
- Lessons: XP is time to the next tier. A Lit tower reaching its next tier sooner deals the next tier's damage for
  that share of the climb (`xp_bound`: how much of the climb XP, not credits, holds it back). No next tier past III.
- Glare (Expose), Prism's Sunder, Crack and Scorch: the towers' hits recomputed on the enemy with the debuff.
- Prism's Chill: a slow's share of each second in the light, priced as held time, the paper's rule; Toll: Frailty.
  Static and Soulbrand: what one more stack discharges or bursts on each enemy's death.
- The Beacon's own scenarios, crowds of 300 read like the defences: `jammed` (a jammer near the cluster part of the
  time), `blinded` (a blinding field), `ghosts` (an enemy that vanishes on its first hit: the towers that aim lose it
  outside the Reveal), `commanded` (Resolve on most of the crowd) and `elusive` (dodges aimed hits).

tree_calc multiplies every value by the cells of path in reach: the lit towers' reach, the reference's range."""
import math

import paper

DOC = "docs/features/FEATURE-next-gen-aura-tower.md"
PRICE = 15
ROLE = "crowd"

P = dict(
    light=2.5, ref_range=3.0, linger=1.0, afterglow=3.0,
    jam_taken=0.66, steady=0.33,
    # Jamming at about Veteran: attack speed and range taken; a jammer near the cluster this share of a jamming wave
    jam_speed=0.4, jam_range=0.8, jam_uptime=0.5,
    # A Juggernaut's blinding field over the cluster, this share of the wave
    blind=0.75, blind_uptime=0.5,
    priority=0.15, turn_share=0.6,
    # Lessons: XP a gate needs from Tier I to II and II to III (placeholders), and how much of a climb XP holds back
    gap_ii=120.0, gap_iii=200.0, xp_bound=0.5, kinship=0.05, kinship_cap=5, breakthrough=10.0,
    # The lit towers' hits: their size, the share that is physical, the share from crit builds and their crits by tier
    hit_size=40.0, physical_share=0.5, crit_share=0.5,
    crit_0=0.15, crit_i=0.25, crit_ii=0.35, crit_iii=0.40, crit_damage_0=1.5, crit_damage=2.0,
    expose=25.0,
    lit="sniper,mortar,cinder,seeker",
    chill=0.15, scorch=5.0, frailty=0.04, sunder=5.0, crack=5.0, static=15.0, soulbrand=10.0,
    # Ghosts: the share of the towers' fire that needs to aim, lost on a Ghost that vanished
    aimed_share=0.6,
    # Commanding at about Veteran on this share of the crowd; Elusive's agility
    resolve=0.2, commanded_share=0.75, elusive_agility=25.0,
)

_BEACON = [
    ("jammed", "crowd 300"), ("blinded", "crowd 300"), ("ghosts", "crowd 300"), ("commanded", "crowd 300"),
    ("elusive", "crowd 300"),
]
BEACON_DEFENCES = [paper.Scenario(name, paper.Foe("walker", 300), True, defence=True) for name, _ in _BEACON]
# The Beacon's counters weigh as the defences do, for this run only
paper.DEFENCES = paper.DEFENCES + [s for s in BEACON_DEFENCES if s.name not in {d.name for d in paper.DEFENCES}]

NODES = {
    "Steady Light": "jam and blinding taken, two thirds to a third",
    "Afterglow": "utility: Reveal 3 s after the light; inside one cluster the bare light already covers the towers' reach",
    "Searchlight": "Priority on the held enemy in the light",
    "Last Light": "Priority on the held enemy in the light",
    "Ringleader": "Priority on the held enemy in the light",
    "Kinship": "Lit towers' XP per other type in the light",
    "Mentor": "the Lit tower with the least XP earns twice as much",
    "Breakthrough": "XP to the others when a Lit tower opens a tier",
    "Daylight": "no Resolve in the light (commanded)",
    "Glare": "Expose in the light",
    "Sanctuary": "no jam and no blinding",
    "Prism": "one stack of each Lit type's mark in the light",
}

TIER_OF = {"Steady Light": "0", "Afterglow": "0", "Searchlight": "I", "Last Light": "I", "Ringleader": "I",
           "Kinship": "I", "Mentor": "I", "Breakthrough": "I", "Daylight": "II", "Glare": "II", "Sanctuary": "III",
           "Prism": "III"}
TIERS = ["0", "I", "II", "III"]

_T0 = ["Steady Light", "Afterglow"]
_T3 = ["Sanctuary", "Prism"]
BUILDS = {
    "boss": _T0 + ["Searchlight", "Mentor", "Glare"] + _T3,
    "mixed": _T0 + ["Searchlight", "Kinship", "Daylight"] + _T3,
    "leakers": _T0 + ["Last Light", "Breakthrough", "Daylight"] + _T3,
    "affixes": _T0 + ["Ringleader", "Kinship", "Daylight"] + _T3,
}


def reach(b, p):
    return p["ref_range"], 0.0


def _tier(b):
    return max((TIER_OF[n] for n in b), key=TIERS.index, default="0")


def _curve():
    import json
    from pathlib import Path
    ref = json.loads((Path(__file__).resolve().parents[1] / "reference.json").read_text(encoding="utf-8"))
    bare = ref["bare_mix"] / ref["price"]
    return {t: ref["tiers"][t]["mix"] / bare for t in TIERS}


def _towers(scenario, tier):
    """What the lit towers deal the scenario's enemies in a second, D: on a crowd, every enemy in their reach."""
    worth = paper._held_back().get(dict(_BEACON).get(scenario.name, scenario.name), 0.0)
    each = paper.NEIGHBOURS * worth * _curve()[tier]
    return each * paper.density(scenario.foe) * paper.chord(3.0) if scenario.crowd else each


def _light_share(p):
    return paper.chord(p["light"]) / paper.chord(p["ref_range"])


def _hits(foe, p, tier, crit_factor_scale=1.0, **shift):
    """The lit towers' average hit on `foe`, crits included at the tier's crit share, physical and magic mixed."""
    crit = {"0": p["crit_0"], "I": p["crit_i"], "II": p["crit_ii"], "III": p["crit_iii"]}[tier]
    mult = p["crit_damage_0"] if tier == "0" else p["crit_damage"]
    chance = paper.crit_chance(crit, foe)
    factor = paper.crit_factor(mult, foe)
    expected = 1 + p["crit_share"] * chance * (factor - 1)
    phys = paper.hit(p["hit_size"], foe, armor_shift=shift.get("armor", 0.0),
                     flat_shift=shift.get("plating", 0.0))
    magic = paper.hit(p["hit_size"], foe, magic=True, flat_shift=shift.get("insulation", 0.0))
    return expected * (p["physical_share"] * phys + (1 - p["physical_share"]) * magic)


def _with(foe, **changes):
    return paper.Foe(**{**foe.__dict__, **changes})


def _jam_out(taken, p):
    speed = 1 - p["jam_speed"] * taken
    reach_left = paper.chord(p["ref_range"] - p["jam_range"] * taken) / paper.chord(p["ref_range"])
    return speed * reach_left


def _lessons(b, p, tier):
    """The share of the lit towers' damage that XP to their next tier is worth."""
    nxt = {"I": "II", "II": "III"}.get(tier)
    if not nxt:
        return 0.0
    curve = _curve()
    jump = curve[nxt] / curve[tier] - 1
    gap = p["gap_ii"] if nxt == "II" else p["gap_iii"]
    n = paper.NEIGHBOURS
    if "Kinship" in b:
        types = len(p["lit"].split(","))
        x = p["kinship"] * min(p["kinship_cap"], types - 1)
        saved = x / (1 + x)
    elif "Mentor" in b:
        saved = 0.5 / n
    elif "Breakthrough" in b:
        saved = min(1.0, (n - 1) / 2 * p["breakthrough"] / gap)
    else:
        return 0.0
    return p["xp_bound"] * jump * saved


def rate(b, scenario, p):
    tier = _tier(b)
    d = _towers(scenario, tier)
    f = _light_share(p)
    foe = scenario.foe
    name = scenario.name
    taken = 0.0 if "Sanctuary" in b else p["steady"] if "Steady Light" in b else p["jam_taken"]

    # What disrupts the towers: the share of their output left, without the Beacon and with it
    out_without = out_with = 1.0
    if name == "jammed":
        u = p["jam_uptime"]
        out_without = 1 - u + u * _jam_out(1.0, p)
        out_with = 1 - u + u * _jam_out(taken, p)
    elif name == "blinded":
        u = p["blind_uptime"]
        out_without = 1 - u + u * (1 - p["blind"])
        out_with = 1 - u + u * (1 - p["blind"] * taken)
    elif name == "ghosts":
        window = min(1.0, (paper.chord(p["light"]) + paper.WALKER_SPEED * p["linger"]) / paper.chord(p["ref_range"]))
        out_without = 1 - p["aimed_share"]
        out_with = 1 - p["aimed_share"] * (1 - window)

    gains = 0.0
    if any(n in b for n in ("Searchlight", "Last Light", "Ringleader")):
        gains += p["priority"] * f * (p["turn_share"] if scenario.crowd else 1.0)
    gains += _lessons(b, p, tier)
    if name == "commanded" and "Daylight" in b:
        gains += p["commanded_share"] * f * p["resolve"] / (1 - p["resolve"])
    target = foe
    if name == "elusive":
        target = _with(foe, agility=p["elusive_agility"])
    base_hit = _hits(target, p, tier)
    if name == "elusive":
        base_hit *= 1 - p["aimed_share"] * p["elusive_agility"] / (p["elusive_agility"] + 100)
    shifted = dict(armor=0.0, plating=0.0, insulation=0.0)
    lit_foe = target
    lit = set(p["lit"].split(","))
    if "Glare" in b:
        lit_foe = _with(lit_foe, agility=lit_foe.agility - p["expose"], resilience=lit_foe.resilience - p["expose"])
    if "Prism" in b:
        if "sniper" in lit:
            shifted["armor"] -= p["sunder"]
        if "mortar" in lit:
            shifted["plating"] -= p["crack"]
            shifted["insulation"] -= p["crack"]
        if "cinder" in lit:
            lit_foe = _with(lit_foe, resilience=lit_foe.resilience - p["scorch"])
        if "seeker" in lit:
            gains += p["chill"] * f
        if "pulse" in lit:
            gains += p["frailty"] * f
        per_death = 0.0
        if "stormcaller" in lit:
            per_death += p["static"]
        if "hexer" in lit:
            per_death += p["soulbrand"] * (1 + (2 * paper.density(foe) if scenario.crowd else 0.0))
        if per_death and math.isfinite(foe.health):
            gains += per_death / foe.health
    lit_hit = _hits(lit_foe, p, tier, **shifted)
    if name == "elusive":
        dodge = 0.0 if "Glare" in b else lit_foe.agility / (lit_foe.agility + 100)
        lit_hit *= 1 - p["aimed_share"] * max(dodge, 0.0)
    if base_hit > 0:
        gains += f * (lit_hit / base_hit - 1)
    return d * (out_with * (1 + gains) - out_without), 0.0
