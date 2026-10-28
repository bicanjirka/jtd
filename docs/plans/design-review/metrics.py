"""Static design metrics for jTD. Usage: python metrics.py <repo-root> <out-dir>"""
import collections, itertools, json, os, re, subprocess, sys

ROOT, OUT = sys.argv[1], sys.argv[2]
MAIN = os.path.join(ROOT, "src", "main", "java")
TEST = os.path.join(ROOT, "src", "test", "java")
os.makedirs(OUT, exist_ok=True)


def java_files(base):
    for d, _, fs in os.walk(base):
        for f in fs:
            if f.endswith(".java"):
                p = os.path.join(d, f)
                yield os.path.relpath(p, base).replace(os.sep, "/"), p


def strip(src):
    src = re.sub(r"/\*.*?\*/", lambda m: "\n" * m.group(0).count("\n"), src, flags=re.S)
    src = re.sub(r"//[^\n]*", "", src)
    src = re.sub(r'"""(.*?)"""', lambda m: '""' + "\n" * m.group(0).count("\n"), src, flags=re.S)
    src = re.sub(r'"(\\.|[^"\\\n])*"', '""', src)
    src = re.sub(r"'(\\.|[^'\\\n])'", "''", src)
    return src


def pkg_of(rel):
    parts = rel.split("/")[:-1]
    return ".".join(parts)


def top_pkg(pkg):
    p = pkg.split(".")
    return ".".join(p[:2]) if len(p) >= 2 else pkg


files = {}
for rel, p in java_files(MAIN):
    with open(p, encoding="utf-8") as fh:
        raw = fh.read()
    files[rel] = {"raw": raw, "src": strip(raw), "pkg": pkg_of(rel), "name": rel.split("/")[-1][:-5]}

# ---------- 1. package dependency graph (top-level td.x packages) ----------
simple_to_pkg = {}
for rel, f in files.items():
    simple_to_pkg.setdefault(f["name"], set()).add(f["pkg"])

edges = collections.Counter()  # (from_top, to_top) -> number of files with such an import
for rel, f in files.items():
    src_top = top_pkg(f["pkg"])
    targets = set()
    for m in re.finditer(r"^import\s+(static\s+)?td\.([\w.]+?)\.[A-Z]\w*", f["raw"], re.M):
        targets.add(top_pkg("td." + m.group(2)))
    # same-package references are invisible to imports; fine for top-level graph
    for t in targets:
        if t != src_top:
            edges[(src_top, t)] += 1

nodes = sorted({a for a, _ in edges} | {b for _, b in edges})
adj = collections.defaultdict(set)
for (a, b) in edges:
    adj[a].add(b)

# Tarjan SCC
index, low, stack, on, sccs, counter = {}, {}, [], set(), [], [0]


def strong(v):
    index[v] = low[v] = counter[0]
    counter[0] += 1
    stack.append(v)
    on.add(v)
    for w in adj[v]:
        if w not in index:
            strong(w)
            low[v] = min(low[v], low[w])
        elif w in on:
            low[v] = min(low[v], index[w])
    if low[v] == index[v]:
        comp = []
        while True:
            w = stack.pop()
            on.discard(w)
            comp.append(w)
            if w == v:
                break
        sccs.append(sorted(comp))


sys.setrecursionlimit(10000)
for v in nodes:
    if v not in index:
        strong(v)
cycles = [c for c in sccs if len(c) > 1]

fan_out_pkg = {n: len(adj[n]) for n in nodes}
fan_in_pkg = collections.Counter(b for (_, b) in edges)

# ---------- 2. per-class metrics ----------
METHOD_RE = re.compile(
    r"^[ \t]*((?:public|protected|private|static|final|abstract|synchronized|default|native|<[^>]+>)\s+)*"
    r"([\w<>\[\],.? ]+?)\s+(\w+)\s*\(([^)]*)\)\s*(?:throws[\w\s,.]+)?\{",
    re.M,
)
CTOR_KW = re.compile(r"^[ \t]*(public|protected|private)?\s*(\w+)\s*\(([^)]*)\)\s*(?:throws[\w\s,.]+)?\{", re.M)
KEYWORDS = {"if", "for", "while", "switch", "catch", "synchronized", "return", "new", "else", "try", "do"}


def param_count(params):
    params = params.strip()
    if not params:
        return 0
    depth, n = 0, 1
    for ch in params:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth -= 1
        elif ch == "," and depth == 0:
            n += 1
    return n


def block_end(src, open_idx):
    depth = 0
    for i in range(open_idx, len(src)):
        if src[i] == "{":
            depth += 1
        elif src[i] == "}":
            depth -= 1
            if depth == 0:
                return i
    return len(src) - 1


def max_nesting(body):
    depth, best = 0, 0
    for ch in body:
        if ch == "{":
            depth += 1
            best = max(best, depth)
        elif ch == "}":
            depth -= 1
    return best - 1 if best else 0


classes = []
methods = []
for rel, f in files.items():
    s = f["src"]
    name = f["name"]
    loc = sum(1 for line in s.splitlines() if line.strip())
    kind = "class"
    km = re.search(r"\b(class|interface|record|enum)\s+" + re.escape(name) + r"\b", s)
    if km:
        kind = km.group(1)
    fields = len(re.findall(r"^    (?:private|protected|public)?\s*(?:static\s+)?(?:final\s+)?(?:volatile\s+)?[\w<>\[\], ?]+\s+\w+\s*(?:=[^;]*)?;", s, re.M))
    instance_fields = len(re.findall(r"^    (?:private|protected|public)\s+(?!static)(?:final\s+)?(?:volatile\s+)?[\w<>\[\], ?]+\s+\w+\s*(?:=[^;]*)?;", s, re.M))
    mutable_fields = len(re.findall(r"^    (?:private|protected|public)\s+(?!static)(?!final)(?:volatile\s+)?[\w<>\[\], ?]+\s+\w+\s*(?:=[^;]*)?;", s, re.M))
    static_mutable = re.findall(r"^\s+(?:private|protected|public)?\s*static\s+(?!final)[\w<>\[\], ?]+\s+(\w+)\s*(?:=|;)", s, re.M)
    ctor_params = 0
    for m in CTOR_KW.finditer(s):
        if m.group(2) == name:
            ctor_params = max(ctor_params, param_count(m.group(3)))
    rec = re.search(r"\brecord\s+" + re.escape(name) + r"\s*(<[^>]*>)?\s*\(([^)]*)\)", s)
    record_components = param_count(rec.group(2)) if rec else 0
    public_methods = 0
    for m in METHOD_RE.finditer(s):
        mname = m.group(3)
        if mname in KEYWORDS or m.group(2).strip() in KEYWORDS:
            continue
        open_idx = s.index("{", m.end() - 1)
        end = block_end(s, open_idx)
        body = s[open_idx:end + 1]
        mloc = sum(1 for line in body.splitlines() if line.strip())
        mods = m.group(0)
        is_public = "public" in mods or kind == "interface"
        if is_public:
            public_methods += 1
        bools = len(re.findall(r"\bboolean\s+\w+", m.group(4)))
        methods.append({
            "file": rel, "method": mname, "loc": mloc, "params": param_count(m.group(4)),
            "bool_params": bools, "nesting": max_nesting(body), "public": is_public,
            "line": s[:m.start()].count("\n") + 1,
        })
    abstract_iface_methods = 0
    if kind == "interface":
        abstract_iface_methods = len(re.findall(r"^    (?!default|static|private)[\w<>\[\], ?.]+\s+\w+\s*\([^)]*\)\s*;", s, re.M))
        public_methods += abstract_iface_methods
    imports_td = len(set(re.findall(r"^import\s+td\.[\w.]+;", f["raw"], re.M)))
    classes.append({
        "file": rel, "pkg": f["pkg"], "kind": kind, "loc": loc, "fields": fields,
        "instance_fields": instance_fields, "mutable_fields": mutable_fields,
        "static_mutable": static_mutable, "ctor_params": ctor_params,
        "record_components": record_components, "public_methods": public_methods,
        "imports_td": imports_td,
    })

# ---------- 3. GameWorld usage ----------
gw = files.get("td/util/GameWorld.java")
gw_methods = sorted(set(m.group(3) for m in METHOD_RE.finditer(gw["src"]) if "public" in m.group(0))) if gw else []
gw_users = []
for rel, f in files.items():
    if rel.endswith("GameWorld.java"):
        continue
    if re.search(r"\bGameWorld\b", f["src"]):
        used = sorted(m for m in gw_methods if re.search(r"\.\s*" + m + r"\s*\(", f["src"]))
        gw_users.append({"file": rel, "accessors": used, "n": len(used)})

# ---------- 4. switch sites, sealed types, visitors ----------
enum_consts = {}
for rel, f in files.items():
    for m in re.finditer(r"\benum\s+(\w+)[^{]*\{", f["src"]):
        start = m.end()
        end = block_end(f["src"], m.end() - 1)
        body = f["src"][start:end]
        head = body.split(";")[0]
        head = re.sub(r"\([^()]*(\([^()]*\)[^()]*)*\)", "", head)
        head = re.sub(r"\{[^{}]*\}", "", head)
        for c in re.findall(r"\b([A-Z][A-Z0-9_]+)\b", head):
            enum_consts.setdefault(c, set()).add(m.group(1))
sealed = {}
for rel, f in files.items():
    for m in re.finditer(r"\bsealed\s+(?:interface|class)\s+(\w+)[^{]*?permits\s+([\w\s,.]+)\{", f["src"]):
        for sub in re.split(r"[\s,]+", m.group(2).strip()):
            if sub:
                sealed[sub.split(".")[-1]] = m.group(1)
    for m in re.finditer(r"\b(?:final\s+)?(?:record|class)\s+(\w+)[^{;]*?\bimplements\s+([\w\s,.<>]+)\{", f["src"]):
        pass

switch_sites = collections.defaultdict(list)
for rel, f in files.items():
    s = f["src"]
    for m in re.finditer(r"\bswitch\s*\(", s):
        brace = s.find("{", m.end())
        if brace < 0:
            continue
        end = block_end(s, brace)
        body = s[brace:end]
        labels = re.findall(r"\bcase\s+([\w.]+)", body)
        typ = None
        for lab in labels:
            lab = lab.split(".")[-1]
            if lab in enum_consts and len(enum_consts[lab]) == 1:
                typ = next(iter(enum_consts[lab]))
                break
            if lab in sealed:
                typ = sealed[lab] + " (sealed)"
                break
            if lab and lab[0].isupper() and not lab.isupper():
                typ = "pattern:" + lab
                break
        if typ:
            switch_sites[typ].append(f"{rel}:{s[:m.start()].count(chr(10)) + 1}")

visitors = {}
for rel, f in files.items():
    if re.search(r"\binterface\s+\w+Visitor\b", f["src"]):
        vname = f["name"]
        impls = [r for r, g in files.items() if re.search(r"\bimplements\b[^{]*\b" + vname + r"\b", g["src"]) or re.search(r"new\s+" + vname + r"\b", g["src"])]
        visit_methods = len(re.findall(r"^\s+\w[\w<>]*\s+visit\w*\s*\(", f["src"], re.M))
        visitors[vname] = {"methods": visit_methods, "implementors": impls}

# ---------- 5. listeners and callbacks (re-entrancy candidates) ----------
listeners = {}
for rel, f in files.items():
    if re.search(r"\binterface\s+\w+(Listener|Observer|Hook|Callback)\b", f["src"]):
        lname = f["name"]
        fire = [r for r, g in files.items() if re.search(r"\b" + lname + r"\b", g["src"])]
        listeners[lname] = fire
cow = [r for r, f in files.items() if "CopyOnWriteArrayList" in f["src"]]

# ---------- 6. parsers and text formats ----------
parsers = []
for rel, f in files.items():
    if re.search(r"\b(parse|tokens?|Grammar|readFrom|fromText|toText|write\w*Text)\b", f["src"]) and re.search(r"(?i)(parse|grammar|text|save|script)", rel):
        parsers.append({"file": rel, "loc": sum(1 for l in f["src"].splitlines() if l.strip())})

# ---------- 7. git co-change ----------
log = subprocess.run(["git", "-C", ROOT, "log", "--name-only", "--pretty=format:@@%h", "-n", "400", "--no-merges"],
                     capture_output=True, text=True, encoding="utf-8").stdout
commits, cur = [], None
for line in log.splitlines():
    if line.startswith("@@"):
        cur = []
        commits.append(cur)
    elif line.strip() and cur is not None:
        cur.append(line.strip())
churn = collections.Counter()
pairs = collections.Counter()
pkg_pairs = collections.Counter()
for c in commits:
    mains = sorted({p for p in c if p.startswith("src/main/java/") and p.endswith(".java")})
    for p in mains:
        churn[p] += 1
    if len(mains) > 25:
        continue  # sweeping commits (renames, format) say nothing about coupling
    for a, b in itertools.combinations(mains, 2):
        pa = top_pkg(pkg_of(a[len("src/main/java/"):]))
        pb = top_pkg(pkg_of(b[len("src/main/java/"):]))
        if pa != pb:
            pairs[(a, b)] += 1
    tops = sorted({top_pkg(pkg_of(p[len("src/main/java/"):])) for p in mains})
    for a, b in itertools.combinations(tops, 2):
        pkg_pairs[(a, b)] += 1
pkgs_per_commit = [len({top_pkg(pkg_of(p[len("src/main/java/"):])) for p in c if p.startswith("src/main/java/") and p.endswith(".java")}) for c in commits]
loc_by_file = {"src/main/java/" + c["file"]: c["loc"] for c in classes}
hotspots = sorted(((churn[p] * loc_by_file.get(p, 0), churn[p], loc_by_file.get(p, 0), p) for p in churn if p in loc_by_file), reverse=True)

# ---------- 8. tests ----------
fakes = collections.defaultdict(list)
test_files = list(java_files(TEST))
for rel, p in test_files:
    with open(p, encoding="utf-8") as fh:
        s = fh.read()
    for m in re.finditer(r"\b(?:class|record)\s+(Fake\w+|Recording\w+|Stub\w+|Dummy\w+)\b", s):
        fakes[m.group(1)].append(rel)
test_instanceof = 0
for rel, p in test_files:
    with open(p, encoding="utf-8") as fh:
        test_instanceof += len(re.findall(r"\binstanceof\b", strip(fh.read())))

result = {
    "package_edges": sorted([[a, b, n] for (a, b), n in edges.items()]),
    "package_cycles": cycles,
    "package_fan_out": fan_out_pkg,
    "package_fan_in": dict(fan_in_pkg),
    "classes": classes,
    "methods_top": sorted(methods, key=lambda m: -m["loc"])[:40],
    "methods_bool": [m for m in methods if m["bool_params"] > 0],
    "methods_params5": sorted([m for m in methods if m["params"] >= 5], key=lambda m: -m["params"]),
    "methods_nesting": sorted([m for m in methods if m["nesting"] >= 5], key=lambda m: -m["nesting"]),
    "gameworld_methods": gw_methods,
    "gameworld_users": sorted(gw_users, key=lambda u: -u["n"]),
    "switch_sites": {k: v for k, v in sorted(switch_sites.items(), key=lambda kv: -len(kv[1]))},
    "visitors": visitors,
    "listeners": listeners,
    "copy_on_write": cow,
    "parsers": parsers,
    "churn_top": churn.most_common(40),
    "hotspots": hotspots[:30],
    "cochange_pairs": [[a, b, n] for (a, b), n in pairs.most_common(40)],
    "cochange_pkg_pairs": [[a, b, n] for (a, b), n in pkg_pairs.most_common(30)],
    "pkgs_per_commit_hist": dict(collections.Counter(pkgs_per_commit)),
    "commits_scanned": len(commits),
    "fakes": {k: v for k, v in fakes.items()},
    "test_instanceof": test_instanceof,
}
with open(os.path.join(OUT, "metrics.json"), "w", encoding="utf-8") as fh:
    json.dump(result, fh, indent=1, default=list)
print("ok", len(classes), "classes", len(methods), "methods", len(commits), "commits")
