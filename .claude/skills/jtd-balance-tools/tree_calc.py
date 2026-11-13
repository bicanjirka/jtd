"""Weighs a next-gen tower's upgrade tree on paper against a stored reference, before any of it is code.

    python tree_calc.py check sniper                 # the model's nodes and builds against the doc's tree
    python tree_calc.py tiers sniper                 # each named build, tier by tier, in every scenario
    python tree_calc.py tiers mortar --set heavy=3   # the same with a model number changed for this run
    python tree_calc.py prices mortar                # each node's fair price, and whether each cluster is even
    python tree_calc.py combos burst                 # pairs of nodes worth more together, strategies side by side
    python tree_calc.py save-reference sniper        # store the tower as the yardstick every other is read against

The tree's structure (tiers, nodes, clusters, After, unlock prices) is read from the tower's doc; its numbers and
mechanics are the model in trees/<tower>.py. Value is damage while a stream crosses the range: damage a second (to
every enemy hit, overkill wasted) times cells of path in range. "/$" divides it by the tower's price, "/$ all" by
everything the build costs at fair node prices (tree_prices.py). Control counts in the value: each second an enemy
is held back is what paper.NEIGHBOURS bare reference towers deal it in a second.
"""
import argparse
import importlib
import json
import statistics
import sys
from dataclasses import dataclass
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
REFERENCE = HERE / "reference.json"
sys.path.insert(0, str(HERE))

import paper  # noqa: E402
import tree_prices  # noqa: E402

TIERS = ["0", "I", "II", "III"]


@dataclass(frozen=True)
class Node:
    tier: str
    name: str
    kind: str
    cluster: str
    after: str


@dataclass
class Tree:
    nodes: list
    unlocks: dict

    def node(self, name):
        return next(n for n in self.nodes if n.name == name)

    def names(self):
        return [n.name for n in self.nodes]

    def items(self, tier):
        """A tier's purchases: each orphan alone, each cluster as the list of its members."""
        seen, items = {}, []
        for n in self.nodes:
            if n.tier != tier:
                continue
            if not n.cluster:
                items.append([n.name])
            elif n.cluster not in seen:
                seen[n.cluster] = [m.name for m in self.nodes if m.cluster == n.cluster]
                items.append(seen[n.cluster])
        return items

    def valid(self, build):
        """Every node's After is owned (a node, or any member of a cluster) and every cluster has at most one active
        member."""
        clusters = {}
        owned = set(build) | {self.node(name).cluster for name in build}
        for name in build:
            n = self.node(name)
            if n.after and n.after not in owned:
                return False
            if n.cluster:
                if n.cluster in clusters:
                    return False
                clusters[n.cluster] = name
        return True

    def upto(self, build, tier):
        top = TIERS.index(tier)
        return frozenset(name for name in build if TIERS.index(self.node(name).tier) <= top)

    def unlock_cost(self, build):
        tiers = {self.node(name).tier for name in build}
        return sum(self.unlocks.get(t, 0) for t in tiers)


def table_rows(text, heading):
    """The rows of the markdown table right under `heading`, as dicts by column name."""
    lines = text.splitlines()
    start = next(i for i, line in enumerate(lines) if line.strip() == heading)
    rows = []
    for line in lines[start + 1:]:
        if not line.strip():
            if rows:
                break
            continue
        if not line.startswith("|"):
            break
        rows.append([c.strip() for c in line.strip().strip("|").split("|")])
    header, body = rows[0], rows[2:]
    return [dict(zip(header, r)) for r in body]


def read_tree(doc):
    text = (REPO / doc).read_text(encoding="utf-8")
    nodes = [Node(r["Tier"], r["Node"], r["Kind"], r["Cluster"], r["After"])
             for r in table_rows(text, "### Upgrade tree")]
    unlocks = {}
    for r in table_rows(text, "### Tiers"):
        price = r.get("Unlock", "").lstrip("$")
        if price.replace(".", "").isdigit():
            unlocks[r["Tier"]] = float(price)
    return Tree(nodes, unlocks)


class Calc:
    """One tower's model with its numbers for this run, and what it has worked out so far."""

    def __init__(self, tower, overrides):
        self.name = tower
        self.model = importlib.import_module(f"trees.{tower}")
        self.p = dict(self.model.P)
        for key, value in overrides.items():
            if key not in self.p:
                raise SystemExit(f"{tower} has no number {key!r}; it has {', '.join(sorted(self.p))}")
            self.p[key] = type(self.p[key])(value)
        self.tree = read_tree(self.model.DOC)
        reference = load_reference()
        self.held_back = reference.get("held_back", {}) if reference else {}
        # A model's own scenarios (copies that work together) join the plain ones in its mix
        self.plain = paper.PLAIN + list(getattr(self.model, "SCENARIOS", []))
        self.scenarios = self.plain + paper.DEFENCES
        self.memo = {}

    def rate(self, build, scenario):
        key = (build, scenario.name)
        if key not in self.memo:
            result = self.model.rate(build, scenario, self.p)
            damage, control = result if isinstance(result, tuple) else (result, 0.0)
            reach, dead = self.model.reach(build, self.p)
            cells = paper.path_cells(reach, dead)
            worth = paper.NEIGHBOURS * self.held_back.get(scenario.name, 0.0)
            self.memo[key] = ((damage + control * worth) * cells, control * cells)
        return self.memo[key]

    def value(self, build, scenario):
        return self.rate(build, scenario)[0]

    def mix(self, build, scenarios=None):
        scenarios = scenarios or [s for s in self.plain if s.weight]
        return sum(s.weight * self.value(build, s) for s in scenarios) / sum(s.weight for s in scenarios)

    def role(self, build):
        """The mix of the scenarios the tower is for: one enemy for a single-target tower, a crowd for an area one."""
        crowd = self.model.ROLE == "crowd"
        return self.mix(build, [s for s in self.plain if s.weight and s.crowd == crowd])

    def builds(self):
        return {name: frozenset(nodes) for name, nodes in self.model.BUILDS.items()}

    def tiers_of(self, build):
        top = max(TIERS.index(self.tree.node(n).tier) for n in build) if build else 0
        return TIERS[:top + 1]


def load_reference():
    return json.loads(REFERENCE.read_text(encoding="utf-8")) if REFERENCE.exists() else None


def print_table(headers, rows):
    widths = [max(len(str(h)), *(len(str(r[i])) for r in rows)) for i, h in enumerate(headers)]
    print("  ".join(str(h).ljust(w) if i == 0 else str(h).rjust(w) for i, (h, w) in enumerate(zip(headers, widths))))
    for r in rows:
        print("  ".join(str(c).ljust(w) if i == 0 else str(c).rjust(w) for i, (c, w) in enumerate(zip(r, widths))))


def percent(x):
    return f"{x:+.0%}"


def check(calc):
    doc, model = set(calc.tree.names()), set(calc.model.NODES)
    problems = [f"in the doc, not the model: {n}" for n in sorted(doc - model)]
    problems += [f"in the model, not the doc: {n}" for n in sorted(model - doc)]
    for name, build in calc.builds().items():
        unknown = build - doc
        if unknown:
            problems.append(f"build {name!r} names nodes the doc lacks: {', '.join(sorted(unknown))}")
        elif not calc.tree.valid(build):
            problems.append(f"build {name!r} breaks an After or holds two members of a cluster")
    return problems


def warnings(calc):
    used = set().union(*calc.builds().values()) if calc.builds() else set()
    doc, model = set(calc.tree.names()), set(calc.model.NODES)
    return [f"no named build owns {n}: no strategy reads it" for n in sorted(doc & model - used)]


SINGLE = ["lone 300", "elite", "boss"]
GROUP = ["crowd 75", "crowd 300", "crowd 1000"]
FLOOR_TOWER, FLOOR_BUILD = "burst", "base"


def sides(calc, build):
    """Damage per dollar of the tower against one enemy (lone, Elite, Boss) and against a crowd."""
    named = {s.name: s for s in paper.PLAIN}
    single = statistics.mean(calc.value(build, named[n]) for n in SINGLE) / calc.model.PRICE
    group = statistics.mean(calc.value(build, named[n]) for n in GROUP) / calc.model.PRICE
    return single, group


def bounds(calc, ref):
    """Every named build at every tier against the floor (the bare Burst with only its base tree: no tower is weaker
    on one enemy or on a crowd) and the ceiling (the Sniper's best build on one enemy: nothing is stronger there)."""
    floor_calc = calc if calc.name == FLOOR_TOWER else Calc(FLOOR_TOWER, {})
    base = floor_calc.builds()[FLOOR_BUILD]
    floor = {t: sides(floor_calc, floor_calc.tree.upto(base, t)) for t in TIERS}
    ceiling = (ref or {}).get("single_best", {})
    rows = []
    for name, build in calc.builds().items():
        if calc.name == FLOOR_TOWER and name == FLOOR_BUILD:
            continue
        for tier in calc.tiers_of(build):
            single, group = sides(calc, calc.tree.upto(build, tier))
            notes = []
            if single < floor[tier][0]:
                notes.append(f"below the floor on one enemy ({floor[tier][0]:.1f})")
            if group < floor[tier][1]:
                notes.append(f"below the floor on a crowd ({floor[tier][1]:.1f})")
            if calc.name != (ref or {}).get("tower") and tier in ceiling and single > ceiling[tier]:
                notes.append(f"above the Sniper on one enemy ({ceiling[tier]:.1f})")
            if notes:
                rows.append([name, tier, f"{single:.1f}", f"{group:.1f}", "; ".join(notes)])
    print(f"Floor ({FLOOR_TOWER} {FLOOR_BUILD}) and ceiling (the Sniper's best on one enemy), per dollar of the tower:")
    if rows:
        print_table(["build", "tier", "one enemy", "crowd", ""], rows)
    else:
        print("every build stands between them")


def tiers(calc, band):
    ref = load_reference()
    curve = ref["tiers"] if ref else {}
    plain = calc.plain
    headers = ["build", "tier", "/$ mix", "/$ role", "vs ref", "$ all", "ref $", "/$ all"] + \
              [s.name for s in plain] + [s.name for s in paper.DEFENCES] + ["control"]
    for name, build in calc.builds().items():
        rows = []
        for tier in calc.tiers_of(build):
            b = calc.tree.upto(build, tier)
            price = calc.model.PRICE
            role = calc.role(b) / price
            against = curve.get(tier, {}).get("role")
            verdict = ""
            if against:
                gap = role / against - 1
                verdict = percent(gap) + (" strong" if gap > band else " weak" if gap < -band else "")
            base = calc.value(b, next(s for s in plain if s.name == "crowd 300"))
            spent = tree_prices.cost(calc, b, ref)
            ref_cost = curve.get(tier, {}).get("cost")
            row = [name if tier == "0" else "", tier, f"{calc.mix(b) / price:.1f}", f"{role:.1f}", verdict,
                   f"{spent:.0f}", f"{ref_cost:.0f}" if ref_cost else "", f"{calc.mix(b) / spent:.1f}"]
            row += [f"{calc.value(b, s) / price:.1f}" for s in plain]
            row += [f"{calc.value(b, s) / base:.0%}" if base else "-" for s in paper.DEFENCES]
            control = calc.rate(b, plain[1])[1]
            row.append(f"{control / price:.1f}" if control else "")
            rows.append(row)
        print_table(headers, rows)
        print()
    bounds(calc, ref)
    print()
    print(f"/$: damage while a stream crosses the range, per dollar of the tower alone. $ all: the tower, fair node"
          f" prices and unlocks; ref $: the reference's median. Defences: % of crowd 300. vs ref: the role column"
          f" against {ref['tower'] if ref else 'no saved reference'}'s, band +-{band:.0%}.")


def prices_report(calc):
    ref = load_reference()
    table, fair, r = tree_prices.worths(calc), tree_prices.prices(calc, ref), tree_prices.rate(calc, ref)
    shown = ["crowd 300", "lone 300", "boss"]
    headers = ["node", "cluster", "+mix"] + [f"+{s}" for s in shown] + ["+best defence", "$", "note"]

    def row(name, cluster, price):
        w = table[name]
        best = max(paper.DEFENCES, key=lambda s: w.relative(s.name))
        return [name, cluster, percent(w.relative("mix"))] + [percent(w.relative(s)) for s in shown] + \
               [f"{percent(w.relative(best.name))} {best.name}", f"{price.dollars:.0f}", price.note]

    blank = [""] * 5
    for tier in TIERS:
        rows, total, whole = [], 0.0, []
        for item in calc.tree.items(tier):
            cluster = calc.tree.node(item[0]).cluster
            if not cluster:
                if item[0] in table:
                    rows.append(row(item[0], "", fair[item[0]]))
                    total += fair[item[0]].dollars
                    whole += [c["mix"] for c in table[item[0]].contexts]
                else:
                    rows.append([item[0], ""] + blank + ["", "no named build owns it"])
                continue
            for member in item:
                if member in table:
                    rows.append(row(member, cluster, tree_prices.price_of(calc, table[member], ref)))
                    whole += [c["mix"] for c in table[member].contexts]
                else:
                    rows.append([member, cluster] + blank + ["", "never weighed"])
            if cluster in fair:
                verdict = "; ".join(v for v in (tree_prices.verdict(calc, item), fair[cluster].note) if v)
                rows.append([f"= {cluster}", ""] + blank + [f"{fair[cluster].dollars:.0f}", verdict])
                total += fair[cluster].dollars
        if not rows:
            continue
        unlock = calc.tree.unlocks.get(tier, 0)
        sizing = 0.1 * statistics.median(whole) / r if whole else 0
        print(f"Tier {tier}: ${total:.0f} of nodes" + (f", ${unlock:.0f} to unlock" if unlock else "")
              + f"; +10% of a Tier {tier} build is worth ${sizing:.0f}")
        print_table(headers, rows)
        print()
    print(f"+: a node's Shapley share of its tier, as a share of the tier's whole build, averaged over the named builds"
          f" (and every member swapped into them). $: that share over the rate, ${1 / r:.3f} a unit of value, what a"
          f" copy buys once {paper.BREAK_EVEN_COPIES} copies stand. A cluster costs its members' mean.")


def combos_report(calc, band, threshold):
    ref = load_reference()
    pairs = sorted(tree_prices.synergy(calc).items(), key=lambda kv: -kv[1][0])
    rows = [[f"{i} + {j}", percent(share), percent(ratio), "OP?" if share > threshold else "", build]
            for (i, j), (share, ratio, build) in pairs[:15]]
    print("Pairs worth more together than apart, in the mix, with the rest of their build bought: the extra as a"
          " share of the whole build, and of the two apart")
    print_table(["pair", "of build", "of pair", "", "in build"], rows)
    print()
    print("Strategies at each tier, per dollar of everything bought (the role mix):")
    for tier in TIERS:
        at = {name: calc.tree.upto(b, tier) for name, b in calc.builds().items() if tier in calc.tiers_of(b)}
        if not at:
            continue
        values = {name: calc.role(b) / tree_prices.cost(calc, b, ref) for name, b in at.items()}
        middle = statistics.median(values.values())
        rows = []
        for name, value in sorted(values.items(), key=lambda kv: -kv[1]):
            gap = value / middle - 1
            note = "ahead" if gap > band else "behind" if gap < -band else ""
            rows.append([name, tier, f"{value:.1f}", f"${tree_prices.cost(calc, at[name], ref):.0f}", percent(gap),
                         note])
        print_table(["build", "tier", "/$ all", "costs", "vs median", ""], rows)
        print()


def save_reference(calc):
    builds = calc.builds()
    curve = {}
    for tier in TIERS:
        at = [calc.tree.upto(b, tier) for b in builds.values() if tier in calc.tiers_of(b)]
        if at:
            curve[tier] = {"mix": statistics.median(calc.mix(b) / calc.model.PRICE for b in at),
                           "role": statistics.median(calc.role(b) / calc.model.PRICE for b in at),
                           "cost": statistics.median(tree_prices.cost(calc, b, None) for b in at)}
    bare = frozenset()
    reach, dead = calc.model.reach(bare, calc.p)
    cells = paper.path_cells(reach, dead)
    held_back = {}
    for s in paper.SCENARIOS:
        damage = calc.model.rate(bare, s, calc.p)
        damage = damage[0] if isinstance(damage, tuple) else damage
        # A single-target tower shares its fire among the crowd in its range
        held_back[s.name] = damage / (paper.density(s.foe) * cells if s.crowd else 1.0)
    reference = {
        "tower": calc.name,
        "price": calc.model.PRICE,
        "bare_mix": calc.mix(bare),
        # What a dollar buys in a copy once BREAK_EVEN_COPIES stand: the rate every node is priced at
        "rate": calc.mix(bare) / (calc.model.PRICE * paper.COPY_PRICE_GROWTH ** paper.BREAK_EVEN_COPIES),
        "tiers": curve,
        # The best named build on one enemy (lone, Elite, Boss) per dollar of the tower: the single-target ceiling
        "single_best": {t: max(sides(calc, calc.tree.upto(b, t))[0] for b in builds.values() if t in calc.tiers_of(b))
                        for t in TIERS},
        # What the bare tower deals one enemy in a second it stays in range: the price of a second held back
        "held_back": held_back,
        "numbers": calc.p,
    }
    REFERENCE.write_text(json.dumps(reference, indent=2) + "\n", encoding="utf-8")
    print(f"saved {calc.name} as the reference: {json.dumps(curve)}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("command", choices=["check", "tiers", "prices", "combos", "save-reference"])
    parser.add_argument("tower")
    parser.add_argument("--set", action="append", default=[], metavar="NAME=VALUE",
                        help="change one of the model's numbers for this run")
    parser.add_argument("--band", type=float, default=0.15, help="how far from the reference reads as weak or strong")
    parser.add_argument("--synergy", type=float, default=0.10,
                        help="a pair whose extra together is this share of its build reads as a combo to check")
    args = parser.parse_args()
    calc = Calc(args.tower, dict(s.split("=", 1) for s in args.set))
    problems = check(calc)
    for line in warnings(calc):
        print(f"warning: {line}")
    if args.command == "check" or problems:
        print("\n".join(problems) or "the model matches the doc")
        if problems:
            raise SystemExit(1)
        return
    if args.command == "tiers":
        tiers(calc, args.band)
    elif args.command == "prices":
        prices_report(calc)
    elif args.command == "combos":
        combos_report(calc, args.band, args.synergy)
    else:
        save_reference(calc)


if __name__ == "__main__":
    main()
