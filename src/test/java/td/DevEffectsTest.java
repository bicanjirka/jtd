package td;

import org.junit.jupiter.api.Test;
import td.damage.AttackOrigin;
import td.effect.DamageSink;
import td.effect.EffectKind;
import td.tower.TowerTerm;

import static org.assertj.core.api.Assertions.assertThat;

class DevEffectsTest {

    @Test
    void everyUsableEnemyEffectLeadsWithAnEffectOfItsOwnKind() {
        for (EffectKind kind : DevEffects.enemyKinds()) {
            if (DevEffects.whyUnusable(kind).isEmpty()) {
                assertThat(DevEffects.enemyEffects(kind, 5, 3, DamageSink.none(), AttackOrigin.none()))
                        .first().satisfies(effect -> assertThat(effect.kind()).isEqualTo(kind));
            }
        }
    }

    @Test
    void anUnusableEffectBuildsNothing() {
        EffectKind unusable = DevEffects.enemyKinds().stream().filter(kind -> DevEffects.whyUnusable(kind).isPresent())
                .findFirst().orElseThrow();

        assertThat(DevEffects.enemyEffects(unusable, 5, 3, DamageSink.none(), AttackOrigin.none())).isEmpty();
    }

    @Test
    void everyOfferedTowerBuffBuildsAStatusOfItsOwnTerm() {
        for (TowerTerm term : DevEffects.towerBuffs()) {
            assertThat(DevEffects.towerBuff(term, 5, 50).term()).isEqualTo(term);
        }
    }
}
