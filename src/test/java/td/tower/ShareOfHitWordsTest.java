package td.tower;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.TowerFixtures;
import td.fixtures.WorldFixtures;
import td.tower.mortar.ShellKind;
import td.tower.mortar.ShrapnelMechanic;
import td.tower.sniper.HemorrhageMechanic;
import td.tower.upgrade.Mechanic;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** A mechanic whose words quote a share of the tower's hit says the points that share comes to on the tower now. */
class ShareOfHitWordsTest {

    private static final Pattern FIGURE = Pattern.compile("about ([\\d.]+)|([\\d.]+) at its edge");

    private final GameWorld world = WorldFixtures.newWorld();
    private final UpgradeNode node = UpgradeNode.of("words", "Words", 10);

    @Test
    void aCritsBleedFigureFollowsTheTowersDamageAndItsCritMultiplier() {
        FakeTower tower = this.towerWith(HemorrhageMechanic::new);
        float before = this.figures(tower).getFirst();
        float multiplier = tower.stats().attack().critMultiplier();

        TowerFixtures.buffed(tower, this.world, TowerModifiers.damageTimes(2f).plus(TowerModifiers.critMultiplier(1f)));

        assertThat(this.figures(tower).getFirst()).isCloseTo(before * 2f * (multiplier + 1f) / multiplier, within(0.05f));
    }

    @Test
    void aHeavysBleedFigureFollowsTheTowersDamageAtFullDamageAndAtTheEdge() {
        FakeTower tower = this.towerWith(ShrapnelMechanic::new);
        float before = this.figures(tower).getFirst();

        TowerFixtures.buffed(tower, this.world, TowerModifiers.damageTimes(2f));

        List<Float> after = this.figures(tower);
        assertThat(after.getFirst()).isCloseTo(before * 2f, within(0.05f));
        assertThat(after.get(1)).isCloseTo(after.getFirst() * ShellKind.HEAVY.blast().edgeShare(), within(0.05f));
    }

    private FakeTower towerWith(Supplier<? extends Mechanic> mechanic) {
        FakeTower tower = FakeTower.offering(this.world, 0, 0, UpgradeTree.of(this.node),
                MechanicCatalogue.of(MechanicCatalogue.entry(this.node.id(), mechanic)));
        this.world.towers().add(tower);
        return tower;
    }

    /** The points the node's words quote, in the order they say them. */
    private List<Float> figures(FakeTower tower) {
        String words = String.join(" ", TowerFixtures.plain(tower.wordsOf(this.node).sentences()));
        Matcher matcher = FIGURE.matcher(words);
        List<Float> found = new ArrayList<>();
        while (matcher.find()) {
            found.add(Float.parseFloat(matcher.group(1) != null ? matcher.group(1) : matcher.group(2)));
        }
        assertThat(found).as(words).isNotEmpty();
        return found;
    }
}
