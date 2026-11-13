"""Paper tools for area towers: how much path a blast covers, what a ground deals as enemies walk through it, the
shares a weighted magazine fires, and how often a bar-fed ability casts.

    python area_sim.py cover 1.0:1 1.75:0.5                       # zones as radius:share, outward
    python area_sim.py ground --radius 1 --life 3 --share 0.04    # burn or poison ground (share of damage a tick)
    python area_sim.py ground --radius 1.2 --life 3 --kind frost  # chill, and the freeze after 4 pulses inside
    python area_sim.py ground --radius 1.2 --life 3 --kind tar --share 0.015
    python area_sim.py magazine N=9 H=4 --fusion                  # or --rich; kinds and weights
    python area_sim.py bar --fill 47.5 --stream-health 300 --cost 2   # seconds between casts

Enemies walk a path one cell wide, so a blast reaches them in proportion to its radius, not its area. The default
crowd is Walkers (1.28 cells/s) at a wave's default spacing (22.4 ticks, about 1.43 cells apart).
"""
import argparse
import math
import random
from collections import Counter

TICKS_PER_SECOND = 20
PULSE_TICKS = 10
POOL_DECAY_EXPONENT = -3.0  # ActiveEffects: a pool keeps about 5% of itself after its authored duration
WALKER_SPEED = 1.28
DEFAULT_SPACING = 22.4 / TICKS_PER_SECOND * WALKER_SPEED


def cover(zones):
    """Cells of path a blast covers, each cell weighted by its zone's share of the shell's damage."""
    total, inner = 0.0, 0.0
    for radius, share in zones:
        total += 2 * (radius - inner) * share
        inner = radius
    return total


def ground(kind, radius, life_s, share, damage, speed, spacing, phase, behind=12.0):
    """Damage, freezes, enemy-seconds slowed, enemy-seconds inside and enemy-seconds held back (a slow's share of each
    second it holds, a freeze all of it), for enemies from `behind` cells back walking
    through ground [-radius, radius] that lands under the target at tick 0. Pulses start at `phase`."""
    life = int(life_s * TICKS_PER_SECOND)
    per_tick = damage * share
    alpha = math.exp(POOL_DECAY_EXPONENT / 60)
    totals = [0.0, 0, 0.0, 0.0, 0.0]
    starts = [-k * spacing for k in range(int(behind / spacing) + 1)]
    for x in starts:
        pool = tar = chill = frozen = streak = 0
        for tick in range(life + 200):
            alive = tick < life
            inside = alive and -radius <= x <= radius
            totals[3] += inside / TICKS_PER_SECOND
            if alive and tick >= phase and (tick - phase) % PULSE_TICKS == 0:
                if inside:
                    if kind in ("burn", "tar"):
                        # ActiveEffects.applyPool: a top-up adds less the fuller the pool is, up to twice its level
                        pool += max(0.0, per_tick * (1 - pool / (2 * per_tick)))
                    if kind == "tar":
                        tar = 2 * PULSE_TICKS
                    if kind == "frost":
                        chill = 40
                        streak += 1
                        if streak >= 4 and frozen <= 0:
                            frozen, streak = 30, 0
                            totals[1] += 1
                else:
                    streak = 0
            totals[0] += pool
            pool *= alpha
            pace = speed
            if tar > 0:
                pace, tar = speed * 0.6, tar - 1
            if chill > 0:
                pace, chill = min(pace, speed * 0.8), chill - 1
            if frozen > 0:
                pace, frozen = 0, frozen - 1
            totals[2] += (pace < speed) / TICKS_PER_SECOND
            totals[4] += (1 - pace / speed) / TICKS_PER_SECOND
            x += pace / TICKS_PER_SECOND
    return totals


def magazine(weights, rich, fusion, draws=200000, seed=1):
    """Shares of the shells fired from a magazine of 10, each new slot drawn by weight."""
    rng = random.Random(seed)
    kinds, ws = list(weights), list(weights.values())
    mag, fired = [], Counter()

    def refill():
        while len(mag) < 10:
            kind = rng.choices(kinds, ws)[0]
            if rich and mag and kind == mag[-1]:
                continue
            if fusion and kind == "H" and mag and mag[-1] == "H":
                mag[-1] = "Nuke"
            else:
                mag.append(kind)

    refill()
    for _ in range(draws):
        fired[mag.pop(0)] += 1
        refill()
    return {kind: count / draws for kind, count in sorted(fired.items())}


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="tool", required=True)
    c = sub.add_parser("cover")
    c.add_argument("zones", nargs="+", help="radius:share, outward")
    g = sub.add_parser("ground")
    g.add_argument("--kind", choices=["burn", "tar", "frost"], default="burn")
    g.add_argument("--radius", type=float, required=True)
    g.add_argument("--life", type=float, required=True, help="seconds")
    g.add_argument("--share", type=float, default=0.0, help="pool damage a tick, as a share of the tower's damage")
    g.add_argument("--damage", type=float, default=42.0, help="the tower's damage stat")
    g.add_argument("--speed", type=float, default=WALKER_SPEED)
    g.add_argument("--spacing", type=float, default=DEFAULT_SPACING, help="cells between enemies")
    m = sub.add_parser("magazine")
    m.add_argument("weights", nargs="+", help="KIND=weight")
    m.add_argument("--rich", action="store_true", help="never the same kind twice in a row")
    m.add_argument("--fusion", action="store_true", help="a Heavy right behind a Heavy fuses into a Nuke")
    b = sub.add_parser("bar")
    b.add_argument("--fill", type=float, required=True, help="damage a second the base shot deals (to all it hits)")
    b.add_argument("--bar", type=float, default=200.0)
    b.add_argument("--stream-health", type=float, default=0.0, help="max health of the tagged enemies that die")
    b.add_argument("--deaths", type=float, default=1 / DEFAULT_SPACING * WALKER_SPEED, help="tagged deaths a second")
    b.add_argument("--death-share", type=float, default=0.2)
    b.add_argument("--death-floor", type=float, default=20.0)
    b.add_argument("--cost", type=float, default=2.0)
    b.add_argument("--refund", type=float, default=0.0, help="units an ability gives back each cast (Feedback)")
    args = parser.parse_args()

    if args.tool == "cover":
        zones = [tuple(float(v) for v in z.split(":")) for z in args.zones]
        print(f"{cover(zones):.3f} cells of path at full damage, {cover(zones) / DEFAULT_SPACING:.2f} Walkers")
    elif args.tool == "ground":
        runs = [ground(args.kind, args.radius, args.life, args.share, args.damage, args.speed, args.spacing, p)
                for p in range(0, PULSE_TICKS, 2)]
        avg = [sum(r[i] for r in runs) / len(runs) for i in range(5)]
        one = [sum(r[i] for r in (ground(args.kind, args.radius, args.life, args.share, args.damage, args.speed,
                                         10 ** 6, p, behind=0) for p in range(0, PULSE_TICKS, 2))) / 5
               for i in range(5)]
        print(f"a crowd: {avg[0]:.0f} damage, {avg[1]:.1f} freezes, {avg[2]:.1f} enemy-seconds slowed "
              f"({avg[4]:.1f} held back), "
              f"{avg[3]:.1f} inside; the target alone: {one[0]:.0f} damage")
    elif args.tool == "magazine":
        weights = {k: float(v) for k, v in (w.split("=") for w in args.weights)}
        print({k: f"{v:.1%}" for k, v in magazine(weights, args.rich, args.fusion).items()})
    else:
        units = args.fill / args.bar
        if args.stream_health:
            units += args.deaths * max(args.death_floor, args.death_share * args.stream_health) / args.bar
        casts = units / (args.cost - args.refund)
        print(f"{units:.2f} units a second, a cast every {1 / casts:.1f} s")


if __name__ == "__main__":
    main()
