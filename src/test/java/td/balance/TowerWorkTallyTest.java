package td.balance;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyMob;
import td.enemy.HitResult;
import td.fixtures.EnemyFixtures;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TowerWorkTallyTest {

    private final GameWorld world = WorldFixtures.newWorld();
    private final TowerWorkTally tally = new TowerWorkTally();

    private Tower towerAtCellOf(EnemyMob mob, int cellsAway) {
        int scale = this.world.getBoard().scale();
        return FakeTower.offering(this.world, (int) mob.getX() / scale + cellsAway, (int) mob.getY() / scale,
                UpgradeTree.none());
    }

    private PlayRecord.TowerWork workOf(Tower tower) {
        return this.tally.totals().get(tower.getType());
    }

    @Test
    void itAddsUpWhatHitsTookWhatShieldsHeldAndWhatWentPastTheHealthLeft() {
        DefinedEnemyMob mob = EnemyFixtures.plainMob(this.world, 100, 3);
        Tower tower = this.towerAtCellOf(mob, 0);

        this.tally.landed(tower, mob, new HitResult(Damage.physical(300), 100));
        this.tally.landed(tower, mob, new HitResult(Damage.physical(100), 0).withOverkill(500));

        assertThat(this.workOf(tower).hits()).isEqualTo(new PlayRecord.HitTotals(500, 100, 500));
        assertThat(this.workOf(tower).hits().shieldShare()).isEqualTo(0.1);
        assertThat(this.workOf(tower).hits().overkillShare()).isEqualTo(0.5);
    }

    @Test
    void itSplitsWhatATowerIsCreditedWithByEnemyKind() {
        DefinedEnemyMob plain = EnemyFixtures.plainMob(this.world, 100, 3);
        DefinedEnemyMob other = EnemyFixtures.liveMob(this.world, EnemyFixtures.simpleDefinition("other"));
        Tower tower = this.towerAtCellOf(plain, 0);

        this.tally.landed(tower, plain, new HitResult(Damage.physical(300), 0));
        this.tally.landed(tower, other, new HitResult(Damage.physical(200), 50));

        assertThat(this.workOf(tower).damageByKind())
                .containsEntry(plain.definition().id(), 300L).containsEntry(other.definition().id(), 250L);
    }

    @Test
    void aHitThatDidNothingNamesNoKind() {
        DefinedEnemyMob mob = EnemyFixtures.plainMob(this.world, 100, 3);
        Tower tower = this.towerAtCellOf(mob, 0);

        this.tally.landed(tower, mob, HitResult.none());

        assertThat(this.workOf(tower).damageByKind()).isEmpty();
    }

    @Test
    void reachCountsEachLivingEnemyWithinATowersRangeEachTick() {
        DefinedEnemyMob mob = EnemyFixtures.plainMob(this.world, 100, 3);
        Tower near = this.towerAtCellOf(mob, 0);
        int beyondRange = (int) (near.getRangeReal() / this.world.getBoard().scale()) + 2;
        Tower far = this.towerAtCellOf(mob, beyondRange);
        EnemyMob[] enemies = {mob};

        this.tally.countReach(enemies, List.of(near, far));
        this.tally.countReach(enemies, List.of(near, far));

        assertThat(this.workOf(near).ticksInReach()).isEqualTo(2);
    }

    @Test
    void aDeadEnemyIsInNobodysReach() {
        DefinedEnemyMob mob = EnemyFixtures.plainMob(this.world, 1, 3);
        Tower tower = this.towerAtCellOf(mob, 0);
        mob.doDamage(Damage.physical(100_000));

        this.tally.countReach(new EnemyMob[] {mob}, List.of(tower));

        assertThat(this.tally.totals()).doesNotContainKey(tower.getType());
    }
}
