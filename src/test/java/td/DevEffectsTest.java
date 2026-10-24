package td;

import org.junit.jupiter.api.Test;
import td.damage.AttackOrigin;
import td.effect.DamageSink;
import td.effect.EffectKind;
import td.tower.TowerTerm;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DevEffectsTest {

    @Test
    void everyUsableEnemyEffectBuiltFromItsStartingValuesLeadsWithAnEffectOfItsOwnKind() {
        for (EffectKind kind : DevEffects.enemyKinds()) {
            if (DevEffects.whyUnusable(kind).isEmpty()) {
                assertThat(DevEffects.enemyEffects(kind, initials(DevEffects.enemyFields(kind)), DamageSink.none(),
                        AttackOrigin.none())).first().satisfies(effect -> assertThat(effect.kind()).isEqualTo(kind));
            }
        }
    }

    @Test
    void everyFieldStartsInsideItsOwnRangeAndThereAreNeverMoreThanTheSlots() {
        for (EffectKind kind : DevEffects.enemyKinds()) {
            assertThat(DevEffects.enemyFields(kind)).hasSizeLessThanOrEqualTo(DevField.MOST_PER_ENTRY)
                    .allSatisfy(field -> assertThat(field.problemWith(field.initial())).isEmpty());
        }
        for (TowerTerm term : DevEffects.towerTerms()) {
            assertThat(DevEffects.towerFields(term)).hasSizeLessThanOrEqualTo(DevField.MOST_PER_ENTRY)
                    .allSatisfy(field -> assertThat(field.problemWith(field.initial())).isEmpty());
        }
    }

    @Test
    void anUnusableEffectAsksForNothingAndBuildsNothing() {
        EffectKind unusable = DevEffects.enemyKinds().stream().filter(kind -> DevEffects.whyUnusable(kind).isPresent())
                .findFirst().orElseThrow();

        assertThat(DevEffects.enemyFields(unusable)).isEmpty();
        assertThat(DevEffects.enemyEffects(unusable, List.of(), DamageSink.none(), AttackOrigin.none())).isEmpty();
    }

    @Test
    void everyGrantableTowerTermBuiltFromItsStartingValuesGrantsSomethingAndAStatusCarriesItsOwnTerm() {
        for (TowerTerm term : DevEffects.towerTerms()) {
            if (DevEffects.whyUngrantable(term).isEmpty()) {
                assertThat(DevEffects.towerGrant(term, initials(DevEffects.towerFields(term)), 40)).get()
                        .satisfies(grant -> {
                            switch (grant) {
                                case DevEffects.TowerGrant.Status status -> assertThat(status.status().term()).isEqualTo(term);
                                case DevEffects.TowerGrant.Jam jam -> assertThat(jam.ticks()).isPositive();
                            }
                        });
            }
        }
    }

    @Test
    void anUngrantableTowerTermAsksForNothingAndGrantsNothing() {
        TowerTerm ungrantable = DevEffects.towerTerms().stream().filter(term -> DevEffects.whyUngrantable(term).isPresent())
                .findFirst().orElseThrow();

        assertThat(DevEffects.towerFields(ungrantable)).isEmpty();
        assertThat(DevEffects.towerGrant(ungrantable, List.of(), 40)).isEmpty();
    }

    private static List<Float> initials(List<DevField> fields) {
        return fields.stream().map(DevField::initial).toList();
    }
}
