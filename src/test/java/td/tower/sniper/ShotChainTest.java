package td.tower.sniper;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;
import td.term.TermText;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShotChainTest {

    private static final float BASE_CRIT = 0.1f;
    private static final float HUNTED_CRIT = 0.4f;

    private final EnemyMob hunted = FakeEnemyMob.at(0, 0);
    private final EnemyMob plain = FakeEnemyMob.at(10, 0);

    /** Sharpens a shot at the hunted enemy only. */
    private final SniperMechanic hunter = new SniperMechanic() {
        @Override
        public SniperShot shape(SniperShot shot, EnemyMob target) {
            return target == ShotChainTest.this.hunted
                    ? shot.withAttack(attack -> attack.withCritChance(attack.critChance() + HUNTED_CRIT))
                    : shot;
        }

        @Override
        public TermText describe() {
            return TermText.of("hunts");
        }
    };

    private final ShotChain chain = ShotChain.of(SniperShot.of(AttackProfile.critChance(BASE_CRIT)));

    @Test
    void eachLegIsShapedForItsOwnEnemyFromTheShotAsFiredNotFromTheLegBefore() {
        SniperShot first = this.chain.shapedFor(this.hunted, List.of(this.hunter));

        SniperShot hop = this.chain.landedOn(this.hunted).shapedFor(this.plain, List.of(this.hunter));

        assertThat(first.attack().critChance()).isEqualTo(BASE_CRIT + HUNTED_CRIT);
        assertThat(hop.attack().critChance()).isEqualTo(BASE_CRIT);
    }

    @Test
    void aHopNeverFliesBackToAnEnemyTheShotAlreadyStruck() {
        EnemyMob third = FakeEnemyMob.at(20, 0);
        ShotChain struck = this.chain.landedOn(this.hunted).landedOn(this.plain);

        List<EnemyMob> targets = struck.hopTargets(List.of(this.hunted, this.plain, third), 3);

        assertThat(targets).containsExactly(third);
    }

    @Test
    void theChainHopsNoMoreOnceItHasHoppedItsCapOfTimes() {
        List<EnemyMob> near = List.of(this.hunted, this.plain, FakeEnemyMob.at(20, 0));
        ShotChain fired = this.chain.landedOn(this.hunted);
        ShotChain hopped = fired.landedOn(this.plain);

        assertThat(fired.hopTargets(near, 1)).isNotEmpty();
        assertThat(hopped.hopTargets(near, 1)).isEmpty();
        assertThat(hopped.hopTargets(near, 2)).isNotEmpty();
    }
}
