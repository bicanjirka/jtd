package td;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.tower.TowerTerm;

import static org.assertj.core.api.Assertions.assertThat;

class DevEffectsTest {

    @Test
    void everyOfferedEnemyEffectBuildsAnEffectOfItsOwnKind() {
        for (EffectKind kind : DevEffects.enemyKinds()) {
            assertThat(DevEffects.enemyEffect(kind, 5, 3).kind()).isEqualTo(kind);
        }
    }

    @Test
    void everyOfferedTowerBuffBuildsAStatusOfItsOwnTerm() {
        for (TowerTerm term : DevEffects.towerBuffs()) {
            assertThat(DevEffects.towerBuff(term, 5, 50).term()).isEqualTo(term);
        }
    }
}
