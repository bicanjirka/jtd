package td.balance;

import td.GameEngine;
import td.damage.DamageUnits;
import td.enemy.EnemyMob;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.*;
import td.util.GameHost;
import td.util.RandomSource;
import td.wave.Point;
import java.util.*;

/**
 * Highest-DPS builds of a tower: every cluster pick of the full tree against one slow plain Walker, damage taken while
 * it stands in range over the time it does. Single target only; a burn tail after range and area damage are not counted.
 * Package-private access to TowerBench, so compile into td.balance:
 * {@code javac -cp "target/classes;$(cat target/runtime-classpath.txt)" -d out DpsProbe.java} then
 * {@code java -cp "out;target/classes;$(cat target/runtime-classpath.txt)" td.balance.DpsProbe SNIPER SEEKER CINDER}.
 */
public class DpsProbe {
    static final Point SUBJECT = new Point(6, 0);
    public static void main(String[] a) throws Exception {
        for (String t : a) {
            TowerFactory.Type type = TowerFactory.Type.valueOf(t);
            UpgradeTree tree = TowerBench.treeOf(type);
            List<UpgradeCluster> groups = new ArrayList<>(tree.clusters());
            tree.fork().ifPresent(groups::add);
            List<Map<UpgradeCluster, String>> combos = new ArrayList<>();
            combos.add(new LinkedHashMap<>());
            for (UpgradeCluster c : groups) {
                List<Map<UpgradeCluster, String>> next = new ArrayList<>();
                for (Map<UpgradeCluster, String> m : combos) for (String id : c.memberIds()) { Map<UpgradeCluster, String> n = new LinkedHashMap<>(m); n.put(c, id); next.add(n); }
                combos.addAll(List.of()); combos = next;
            }
            List<String[]> results = new ArrayList<>();
            for (Map<UpgradeCluster, String> picks : combos) {
                Set<String> ids = new LinkedHashSet<>();
                for (int tier = 1; tier <= 3; tier++) tree.unlockOf(tier).map(UpgradeNode::id).ifPresent(ids::add);
                for (UpgradeNode n : tree.nodes()) {
                    boolean picked = tree.clusterOf(n).map(c -> picks.get(c).equals(n.id())).orElse(true);
                    if (picked && !TowerBench.STRATEGY_ONLY.contains(n.id())) ids.add(n.id());
                }
                double dps = dps(type, ids);
                results.add(new String[]{String.format(Locale.ROOT, "%.2f", dps), String.join(",", picks.values())});
            }
            results.sort((x, y) -> Double.compare(Double.parseDouble(y[0]), Double.parseDouble(x[0])));
            System.out.println("== " + t + " (" + results.size() + " builds)");
            for (int i = 0; i < results.size(); i++) System.out.println(results.get(i)[0] + "  " + results.get(i)[1]);
        }
    }
    static double dps(TowerFactory.Type type, Set<String> wanted) {
        double sum = 0; int n = 0;
        for (long seed = 1; seed <= 3; seed++) {
            GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(seed));
            TowerBench.Scenario sc = TowerBench.Scenario.lone("single", "walker").slowedTo(0.08);
            engine.loadLevel(sc.lane(300000));
            engine.getGameWorld().playtestRules().setUpgradeGatesIgnored(true);
            engine.debugGrantCredits(10_000_000);
            Tower tower = Turn.place(engine, type, SUBJECT).orElseThrow();
            boolean bought = true;
            while (bought) { bought = false;
                for (UpgradeNode node : tower.offeredUpgrades(engine.getGameWorld())) if (wanted.contains(node.id()) && tower.buyUpgrade(node)) { bought = true; break; } }
            tower.earnXp(1_000_000);
            engine.requestNextWave();
            int tick = 1; engine.doTick(tick);
            double range = 0; long start = -1, end = -1; int hp0 = -1, hpFirst = -1, hpLast = -1;
            while (tick < 12000 && !engine.outcome().isOver() && engine.getGameWorld().enemies().leaks() == 0 && !engine.isWaveReady()) {
                tick++; engine.doTick(tick);
                EnemyMob[] mobs = engine.getGameWorld().enemies().getEnemies();
                if (mobs.length == 0 || mobs[0] == null) continue;
                EnemyMob m = mobs[0];
                double d = Math.hypot(m.getX() - tower.getX(), m.getY() - tower.getY());
                if (d <= tower.getRangeReal()) { if (start < 0) { start = tick; hpFirst = m.getHealth(); } end = tick; hpLast = m.getHealth(); }
            }
            if (start >= 0 && end > start) { sum += DamageUnits.inPoints(hpFirst - hpLast) / ((end - start) / 20.0); n++; }
        }
        return n == 0 ? 0 : sum / n;
    }
}
