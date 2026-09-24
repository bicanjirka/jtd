package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.enemy.EnemyMobVisitor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HighestHealthSelectorTest {

    @Test
    void picksTheCandidateWithTheMostHealthRemaining() {
        EnemyMob weak = new FakeHealthEnemyMob(10);
        EnemyMob strong = new FakeHealthEnemyMob(9000);

        Optional<EnemyMob> selected = new HighestHealthSelector().selectFrom(List.of(weak, strong));

        assertThat(selected).contains(strong);
    }

    @Test
    void selectingFromNoCandidatesReturnsEmpty() {
        Optional<EnemyMob> selected = new HighestHealthSelector().selectFrom(List.of());

        assertThat(selected).isEmpty();
    }

    private static final class FakeHealthEnemyMob implements EnemyMob {
        private final int health;

        FakeHealthEnemyMob(int health) {
            this.health = health;
        }

        @Override
        public int getHealth() {
            return this.health;
        }

        @Override
        public td.damage.Damage doDamage(td.damage.Damage damage) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void applyEffect(td.effect.Effect effect) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Set<td.effect.EffectKind> activeEffectKinds() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double getX() {
            return 0;
        }

        @Override
        public double getY() {
            return 0;
        }

        @Override
        public boolean validTarget() {
            return true;
        }

        @Override
        public boolean validTarget(Type type) {
            return true;
        }

        @Override
        public boolean validTarget(Type type0, Type type1) {
            return true;
        }

        @Override
        public boolean isDead() {
            return false;
        }

        @Override
        public int getBounty() {
            return 0;
        }

        @Override
        public int getProgression() {
            return 0;
        }

        @Override
        public float getSpeed() {
            return 0;
        }

        @Override
        public void doTick(int gameTime) {
        }

        @Override
        public <R> R accept(EnemyMobVisitor<R> visitor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getInfoString() {
            return "fake";
        }
    }
}
