package td.ui;

import org.junit.jupiter.api.Test;
import td.economy.StatusTerm;
import td.effect.EffectKind;
import td.enemy.AbilityTerm;
import td.enemy.Affix;
import td.enemy.TraitTerm;
import td.term.Term;
import td.tower.AbstractTower;
import td.tower.TowerFactory;
import td.tower.TowerTerm;
import td.util.GameHost;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PictogramCatalogueTest {

    @Test
    void everyTermHasAPictogram() {
        List<String> missing = Stream.<Term[]>of(EffectKind.values(), TowerTerm.values(), TraitTerm.values(),
                        AbilityTerm.values(), StatusTerm.values(), Affix.values())
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
                    .filter(node -> tower.upgradeTree().artNodeOf(node).equals(node))
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
