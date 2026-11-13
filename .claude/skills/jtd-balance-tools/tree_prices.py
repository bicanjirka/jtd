"""Fair node prices, cluster balance and synergy for tree_calc.

A node's worth is its Shapley share of its tier: in a named build, with the build's lower tiers bought, the average
of what it adds over every order the build's nodes of that tier can be bought in. Its fair price is the mean of its worth in
the Walker mix and that worth plus what it adds on its best defence over the plain crowd of 300, over the reference
rate, what a dollar buys in a copy once paper.BREAK_EVEN_COPIES stand; a node costs the same however
many copies stand. A cluster is weighed by swapping each member into the same builds, and priced at its members'
mean, since it is bought once.
"""
import statistics
from dataclasses import dataclass, field
from itertools import combinations
from math import factorial

import paper

COUNTER_SHARE = 0.01
COUNTER_FACTOR = 3
EVEN_SPREAD = 0.10
# A cluster whose members' fair prices lie this far apart (a share of their mean, and at least these dollars) is no
# real choice at one price: it needs a rebalance before it is built
REBALANCE_SPREAD = 0.5
REBALANCE_DOLLARS = 5


@dataclass
class Worth:
    """A node's Shapley share in each scenario, averaged over the builds it was weighed in."""
    name: str
    shares: list = field(default_factory=list)
    contexts: list = field(default_factory=list)

    def mean(self, scenario):
        return statistics.mean(s[scenario] for s in self.shares)

    def relative(self, scenario):
        return statistics.mean(s[scenario] / c[scenario] if c[scenario] else 0.0
                               for s, c in zip(self.shares, self.contexts))


def closure(tree, build, name):
    """`name` and every node of `build` that needs it, directly or not."""
    out, grew = {name}, True
    while grew:
        grew = False
        for other in build:
            if other not in out and tree.node(other).after in out:
                out.add(other)
                grew = True
    return out


def scores(calc, build):
    values = {s.name: calc.value(build, s) for s in calc.scenarios}
    values["mix"] = calc.mix(build)
    values["control"] = calc.rate(build, paper.PLAIN[1])[1]
    return values


def shapley(calc, lower, items):
    """Each item's Shapley share of what `items` add to `lower`, scenario by scenario; an order that would break an
    After is left out and the rest weighed up to make up for it."""
    n, tree, out = len(items), calc.tree, {}
    for item in items:
        others = [x for x in items if x != item]
        total, weight = {}, 0.0
        for size in range(len(others) + 1):
            w = factorial(size) * factorial(n - size - 1) / factorial(n)
            for rest in combinations(others, size):
                without = lower | frozenset(rest)
                with_item = without | {item}
                if not (tree.valid(without) and tree.valid(with_item)):
                    continue
                a, b = scores(calc, with_item), scores(calc, without)
                for k in a:
                    total[k] = total.get(k, 0.0) + w * (a[k] - b[k])
                weight += w
        out[item] = {k: v / weight for k, v in total.items()} if weight else None
    return out


def contexts(calc):
    """The named builds with every combination of their clusters' members swapped in, so a node's price never hangs
    on which member of another cluster a named build happens to own."""
    tree, seen, out = calc.tree, set(), []
    for build in calc.builds().values():
        variants = [build]
        for cluster in sorted({tree.node(n).cluster for n in build if tree.node(n).cluster}):
            members = [m.name for m in tree.nodes if m.cluster == cluster]
            grown = []
            for v in variants:
                owned = next(n for n in v if tree.node(n).cluster == cluster)
                for member in members:
                    swapped = v if member == owned else (v - closure(tree, v, owned)) | {member}
                    if tree.valid(swapped):
                        grown.append(frozenset(swapped))
            variants = grown
        for v in variants:
            if v not in seen:
                seen.add(v)
                out.append(v)
    return out


def worths(calc):
    if hasattr(calc, "_worths"):
        return calc._worths
    tree, table = calc.tree, {}
    for build in contexts(calc):
        for tier in calc.tiers_of(build):
            top = calc.tree.upto(build, tier)
            items = [n for n in top if tree.node(n).tier == tier]
            lower = top - frozenset(items)
            whole = scores(calc, top)
            for name, share in shapley(calc, lower, items).items():
                if share is None:
                    continue
                worth = table.setdefault(name, Worth(name))
                worth.shares.append(share)
                worth.contexts.append(whole)
    calc._worths = table
    return table


def rate(calc, reference):
    if reference:
        return reference["rate"]
    bare = calc.mix(frozenset())
    return bare / (calc.model.PRICE * paper.COPY_PRICE_GROWTH ** paper.BREAK_EVEN_COPIES)


@dataclass
class Price:
    dollars: float
    note: str = ""


def price_of(calc, worth, reference):
    """A node's fair price, and why when it isn't simply its worth in the mix."""
    r = rate(calc, reference)
    mix = worth.mean("mix")
    desc = calc.model.NODES.get(worth.name, "")
    notes = []
    best = max(paper.DEFENCES, key=lambda s: worth.mean(s.name) - worth.mean(paper.COUNTER_BASE))
    # Halfway between the Walkers and the enemy it counters: the mix alone underprices a counter, which pays where it
    # is needed, and its best enemy alone overprices it, since that enemy is a share of the game
    # A defence is a crowd of 300, so what it adds there is read against what it adds on the plain crowd of 300
    edge = max(0.0, worth.mean(best.name) - worth.mean(paper.COUNTER_BASE))
    dollars = (mix + (mix + edge)) / 2 / r
    if worth.relative("mix") < COUNTER_SHARE and worth.relative(best.name) > COUNTER_FACTOR * max(worth.relative("mix"), 0.005):
        notes.append(f"counter ({worth.relative(best.name):+.0%} on {best.name})")
    if dollars <= 0:
        notes.append("costs damage" if mix < 0 else "adds no damage")
        dollars = 0.0
    if desc.startswith("utility:"):
        notes.append("utility")
    if worth.mean("control") / calc.model.PRICE >= 0.05:
        notes.append(f"control +{worth.mean('control') / calc.model.PRICE:.1f}/$")
    return Price(dollars, "; ".join(notes))


def prices(calc, reference):
    """Each purchase's price: an orphan by its name, a cluster by its cluster's name."""
    if hasattr(calc, "_prices"):
        return calc._prices
    table, out = worths(calc), {}
    for node in calc.tree.nodes:
        if node.cluster:
            continue
        if node.name in table:
            out[node.name] = price_of(calc, table[node.name], reference)
    for cluster in {n.cluster for n in calc.tree.nodes if n.cluster}:
        members = [price_of(calc, table[m.name], reference).dollars
                   for m in calc.tree.nodes if m.cluster == cluster and m.name in table]
        if members:
            low, high = min(members), max(members)
            note = f"rebalance: members ${low:.0f}-${high:.0f}" if high - low > REBALANCE_SPREAD * statistics.mean(members)                 and high - low >= REBALANCE_DOLLARS else ""
            out[cluster] = Price(statistics.mean(members), note)
    calc._prices = out
    return out


def cost(calc, build, reference):
    """What the build costs: the tower, each purchase once at its fair price, and each tier's unlock."""
    table = prices(calc, reference)
    keys = {calc.tree.node(n).cluster or n for n in build}
    return calc.model.PRICE + sum(table[k].dollars for k in keys if k in table) + calc.tree.unlock_cost(build)


def verdict(calc, members):
    """Whether a cluster's members are even, each wins somewhere, or one wins everywhere."""
    table = worths(calc)
    weighed = [table[m] for m in members if m in table]
    if len(weighed) < 2:
        return "one member weighed"
    mixes = [w.mean("mix") for w in weighed]
    spread = (max(mixes) - min(mixes)) / (abs(statistics.mean(mixes)) or 1)
    if spread <= EVEN_SPREAD:
        return f"even (within {spread:.0%})"
    wins = {}
    for s in [s for s in calc.plain if s.weight] + paper.DEFENCES:
        winner = max(weighed, key=lambda w: w.mean(s.name))
        wins.setdefault(winner.name, []).append(s.name)
    controls = [w for w in weighed if w.mean("control") > 0 and w.name not in wins]
    if len(wins) + len(controls) == len(weighed):
        ranked = sorted(wins.items(), key=lambda kv: len(kv[1]))
        parts = [f"{n} wins {', '.join(v)}" for n, v in ranked[:-1]] + [f"{ranked[-1][0]} the rest"]
        return "situational: " + "; ".join(parts + [f"{w.name} controls" for w in controls])
    losers = [w.name for w in weighed if w.name not in wins and w not in controls]
    return f"one right answer: {', '.join(losers)} never wins (spread {spread:.0%})"


def synergy(calc):
    """For each pair of nodes a named build owns, with the rest of the build bought, what they add together beyond
    what each adds apart: as a share of the whole build (how much the combo matters) and of the two apart (how
    much more they are together). A node that only changes another's (an After, a crit payoff) pairs high on the
    second and low on the first."""
    tree, out = calc.tree, {}
    for bname, build in calc.builds().items():
        names = sorted(build)
        for i, j in combinations(names, 2):
            ci, cj = closure(tree, build, i), closure(tree, build, j)
            if j in ci or i in cj:
                continue
            base = build - ci - cj
            if not (tree.valid(base | {i}) and tree.valid(base | {j}) and tree.valid(base | {i, j})):
                continue
            v = {s: calc.mix(s) for s in (base, base | {i}, base | {j}, base | {i, j})}
            di, dj = v[base | {i}] - v[base], v[base | {j}] - v[base]
            both = v[base | {i, j}] - v[base]
            if di + dj <= 0.01 * v[base]:
                continue
            extra = both - di - dj
            key = (i, j)
            share = extra / v[base | {i, j}]
            if key not in out or share > out[key][0]:
                out[key] = (share, extra / (di + dj), bname)
    return out
