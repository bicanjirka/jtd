package td.fixtures;

import td.stat.StatModifiers;
import td.term.TermText;
import td.tower.AbstractTower;
import td.tower.TowerStat;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.List;

public final class TowerFixtures {

    private TowerFixtures() {
    }

    /**
     * {@code tower} with {@code buff} from a neighbour, as an aura would give it: a stat changed without
     * the node that changes it in the game.
     */
    public static <T extends AbstractTower> T buffed(T tower, GameWorld world, StatModifiers<TowerStat> buff) {
        world.towers().add(FakeTower.offering(world, 0, 0, UpgradeTree.none()).giving(buff));
        tower.recalculateStats();
        return tower;
    }

    /**
     * Ticks until no projectile is left, capped at 50 ticks so a test that never fires fails fast.
     */
    public static void flyProjectilesToCompletion(GameWorld world) {
        for (int t = 1; t <= 50 && !world.projectiles().getProjectiles().isEmpty(); t++) {
            world.projectiles().doTick(t);
        }
    }

    /** Each text as its plain words, mentions as their titles. */
    public static List<String> plain(List<TermText> texts) {
        return texts.stream().map(TermText::plain).toList();
    }
}
