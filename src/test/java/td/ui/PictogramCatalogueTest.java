package td.ui;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.enemy.AbilityTerm;
import td.enemy.TraitTerm;
import td.term.Term;
import td.tower.AbstractTower;
import td.tower.TowerFactory;
import td.tower.TowerTerm;
import td.tower.upgrade.UpgradeTier;
import td.util.GameHost;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PictogramCatalogueTest {

    private static final Set<UpgradeTier> ART_TIERS = Set.of(UpgradeTier.RANGE_1, UpgradeTier.ATTUNE,
            UpgradeTier.HEAD_1, UpgradeTier.EXTRA_1, UpgradeTier.HEAD_4, UpgradeTier.SPECIAL);

    @Test
    void everyTermHasAPictogram() {
        List<String> missing = Stream.<Term[]>of(EffectKind.values(), TowerTerm.values(), TraitTerm.values(),
                        AbilityTerm.values())
                .flatMap(Arrays::stream)
                .map(Term::key)
                .filter(key -> PictogramCatalogue.of(key).isEmpty())
                .toList();

        assertThat(missing).isEmpty();
    }

    @Test
    void everyArtNodeHasAPictogram() {
        List<String> missing = new ArrayList<>();
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            AbstractTower tower = (AbstractTower) TowerFactory.createTower(type, new GameWorld(GameHost.noOp()), 0, 0);
            tower.upgradeTree().nodes().stream()
                    .filter(node -> ART_TIERS.contains(node.tier()))
                    .filter(node -> PictogramCatalogue.of(node.id()).isEmpty())
                    .forEach(node -> missing.add(type + " " + node.id()));
        }

        assertThat(missing).isEmpty();
    }

    @Test
    void everySpecParses() {
        assertThat(PictogramCatalogue.keys()).hasSizeGreaterThanOrEqualTo(197);
        assertThat(PictogramCatalogue.keys()).allSatisfy(key -> assertThat(PictogramCatalogue.of(key)
                .orElseThrow().layers()).isNotEmpty());
    }
}
