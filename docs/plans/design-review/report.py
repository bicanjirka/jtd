import json, sys, collections

m = json.load(open(sys.argv[1], encoding="utf-8"))
section = sys.argv[2] if len(sys.argv) > 2 else "all"


def show(title, rows, n=20):
    print(f"\n## {title}")
    for r in rows[:n]:
        print("  ", r)


if section in ("all", "pkg"):
    show("package cycles (SCCs > 1)", m["package_cycles"])
    print("\n## package fan-out / fan-in")
    for p in sorted(m["package_fan_out"], key=lambda p: -m["package_fan_out"][p]):
        print(f"   {p:16} out={m['package_fan_out'][p]:2} in={m['package_fan_in'].get(p, 0):2}")
    show("package edges (from, to, files importing)", sorted(m["package_edges"], key=lambda e: (e[0], -e[2])), 400)

if section in ("all", "cls"):
    cl = m["classes"]
    by_pkg = collections.defaultdict(lambda: [0, 0])
    for c in cl:
        top = ".".join(c["pkg"].split(".")[:2])
        by_pkg[top][0] += 1
        by_pkg[top][1] += c["loc"]
    show("package size (files, loc)", sorted(([k, *v] for k, v in by_pkg.items()), key=lambda r: -r[2]), 40)
    show("largest classes (loc, file, public methods, fields, ctor params)",
         [(c["loc"], c["file"], c["public_methods"], c["fields"], c["ctor_params"]) for c in sorted(cl, key=lambda c: -c["loc"])], 25)
    show("most constructor params", [(c["ctor_params"], c["file"]) for c in sorted(cl, key=lambda c: -c["ctor_params"])], 20)
    show("widest public surface", [(c["public_methods"], c["kind"], c["file"]) for c in sorted(cl, key=lambda c: -c["public_methods"])], 20)
    show("interfaces with > 5 abstract-or-public methods", [(c["public_methods"], c["file"]) for c in sorted(cl, key=lambda c: -c["public_methods"]) if c["kind"] == "interface" and c["public_methods"] > 5], 30)
    show("most mutable instance fields", [(c["mutable_fields"], c["file"]) for c in sorted(cl, key=lambda c: -c["mutable_fields"])], 20)
    show("most td imports (efferent coupling per file)", [(c["imports_td"], c["file"]) for c in sorted(cl, key=lambda c: -c["imports_td"])], 20)
    show("static mutable fields", [(c["file"], c["static_mutable"]) for c in cl if c["static_mutable"]], 40)
    show("records with >= 5 components", [(c["record_components"], c["file"]) for c in sorted(cl, key=lambda c: -c["record_components"]) if c["record_components"] >= 5], 40)
    locs = sorted(c["loc"] for c in cl)
    print("\n   class loc p50/p90/p99/max:", locs[len(locs)//2], locs[int(len(locs)*.9)], locs[int(len(locs)*.99)], locs[-1])

if section in ("all", "meth"):
    show("longest methods (loc, nesting, file:line method)", [(x["loc"], x["nesting"], f"{x['file']}:{x['line']} {x['method']}") for x in m["methods_top"]], 25)
    show("deepest nesting >= 5", [(x["nesting"], x["loc"], f"{x['file']}:{x['line']} {x['method']}") for x in m["methods_nesting"]], 20)
    show(">= 5 params", [(x["params"], f"{x['file']}:{x['line']} {x['method']}") for x in m["methods_params5"]], 25)
    bp = m["methods_bool"]
    print(f"\n## methods with boolean params: {len(bp)} (public: {sum(1 for x in bp if x['public'])})")
    for x in sorted(bp, key=lambda x: -x["bool_params"])[:30]:
        print("  ", x["bool_params"], f"{x['file']}:{x['line']} {x['method']}", "public" if x["public"] else "")

if section in ("all", "gw"):
    print("\n## GameWorld public methods:", m["gameworld_methods"])
    print(f"## GameWorld users: {len(m['gameworld_users'])}")
    hist = collections.Counter(u["n"] for u in m["gameworld_users"])
    print("   accessors-used histogram:", dict(sorted(hist.items())))
    for u in m["gameworld_users"][:25]:
        print("  ", u["n"], u["file"], u["accessors"])

if section in ("all", "sw"):
    print("\n## switch sites per type (types switched in >= 3 places)")
    for t, sites in m["switch_sites"].items():
        if len(sites) >= 3:
            print(f"   {len(sites):3} {t}")
    show("visitors (visit methods, implementors)", [(k, v["methods"], len(v["implementors"])) for k, v in m["visitors"].items()], 20)
    print("\n## test instanceof count:", m["test_instanceof"])

if section in ("all", "lis"):
    print("\n## listener/observer/hook/callback interfaces and files referencing them")
    for k, v in sorted(m["listeners"].items(), key=lambda kv: -len(kv[1])):
        print(f"   {len(v):3} {k}")
    print("\n## CopyOnWriteArrayList users:", len(m["copy_on_write"]))
    show("parsers / text formats (file, loc)", [(p["file"], p["loc"]) for p in m["parsers"]], 40)

if section in ("all", "git"):
    print("\n## commits scanned:", m["commits_scanned"])
    print("   top-level packages touched per commit:", dict(sorted((int(k), v) for k, v in m["pkgs_per_commit_hist"].items())))
    show("churn (commits, file)", [(n, p.replace('src/main/java/', '')) for p, n in m["churn_top"]], 30)
    show("hotspots churn*loc (score, churn, loc, file)", [(a, b, c, d.replace('src/main/java/', '')) for a, b, c, d in m["hotspots"]], 25)
    show("cross-package co-change file pairs", [(n, a.replace('src/main/java/', ''), b.replace('src/main/java/', '')) for a, b, n in m["cochange_pairs"]], 30)
    show("co-changing package pairs", m["cochange_pkg_pairs"], 30)

if section in ("all", "test"):
    print("\n## fakes by name (defined in how many files)")
    for k, v in sorted(m["fakes"].items(), key=lambda kv: -len(kv[1])):
        print(f"   {len(v)} {k} {v if len(v) > 1 else ''}")
