"""Damage a second of a shot sequence with a chained attack-speed bonus, against the same build without it.

Every shot counts toward the sure crit. A crit grants the bonus for the next shots; a crit during it doesn't
restart it.
"""
import argparse
import random


def damage_a_second(args, bonus, seed=1):
    rng = random.Random(seed)
    time = damage = 0.0
    left = 0
    for shot in range(1, args.shots + 1):
        time += 1 / (1 + args.attack_speed + (bonus if left else 0))
        if left:
            left -= 1
        crit = (args.fourth and shot % args.fourth == 0) or rng.random() < args.crit
        damage += args.crit_damage if crit else 1
        if crit and not left and bonus:
            left = args.frenzy_shots
    return damage / time


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--crit", type=float, default=0.21, help="crit chance per shot")
    parser.add_argument("--crit-damage", type=float, default=2.0)
    parser.add_argument("--attack-speed", type=float, default=0.32, help="always-on attack speed bonus")
    parser.add_argument("--fourth", type=int, default=0, help="every n-th shot is a sure crit; 0 for none")
    parser.add_argument("--frenzy", type=float, default=0.35, help="attack speed bonus a crit grants")
    parser.add_argument("--frenzy-shots", type=int, default=3)
    parser.add_argument("--shots", type=int, default=400000)
    args = parser.parse_args()
    gain = damage_a_second(args, args.frenzy) / damage_a_second(args, 0) - 1
    print(f"{gain:+.1%} damage a second")


if __name__ == "__main__":
    main()
