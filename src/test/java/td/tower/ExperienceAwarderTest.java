package td.tower;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyWalk;
import td.fixtures.BoardFixtures;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class ExperienceAwarderTest {

    private final GameWorld world = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
    private final ExperienceAwarder awarder = new ExperienceAwarder(this.world.towers());

    /** A finished walk that came within reach of exactly the towers standing at {@code reached}. */
    private static EnemyWalk walkNear(int bounty, Tower... reached) {
        return new EnemyWalk() {
            @Override
            public int getBounty() {
                return bounty;
            }

            @Override
            public long entryOrdinal() {
                return Long.MAX_VALUE;
            }

            @Override
            public boolean walkedWithin(double x, double y, double radius) {
                for (Tower tower : reached) {
                    if (tower.getX() == x && tower.getY() == y) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private <T extends AbstractTower> T place(T tower) {
        this.world.towers().add(tower);
        return tower;
    }

    private AuraTower auraWith(int x, int y, String... nodes) {
        AuraTower aura = this.place(new AuraTower(this.world, x, y));
        UpgradePaths.buy(aura, this.world, nodes);
        return aura;
    }

    @Test
    void aTowerThatReachedTheWalkEarnsItsBountyAndOneThatDidNotEarnsNothing() {
        SniperTower near = this.place(new SniperTower(this.world, 1, 1));
        SniperTower far = this.place(new SniperTower(this.world, 10, 10));

        this.awarder.walkEnded(walkNear(100, near));

        assertThat(near.experience().xp()).isEqualTo(100);
        assertThat(far.experience().xp()).isZero();
    }

    @Test
    void anAuraEarnsWhatTheTowersItBuffsEarnWithoutAnyBonusOfItsOwn() {
        AuraTower aura = this.place(new AuraTower(this.world, 3, 3));
        SniperTower near = this.place(new SniperTower(this.world, 3, 2));

        this.awarder.walkEnded(walkNear(100, near));

        assertThat(aura.experience().xp()).isEqualTo(100);
    }

    @Test
    void kinshipPaysFivePercentForEachOtherTowerTypeInRange() {
        AuraTower aura = this.auraWith(3, 3);
        SniperTower sniper = this.place(new SniperTower(this.world, 3, 2));
        this.place(new SplashTower(this.world, 3, 4));
        this.place(new MortarTower(this.world, 2, 3));

        this.awarder.walkEnded(walkNear(100, sniper));

        assertThat(aura).isNotNull();
        assertThat(sniper.experience().xp()).isEqualTo(110);
    }

    @Test
    void kinshipCountsNoTypeTheBuffedTowerIsItself() {
        this.auraWith(3, 3);
        SniperTower sniper = this.place(new SniperTower(this.world, 3, 2));
        this.place(new SniperTower(this.world, 3, 4));

        this.awarder.walkEnded(walkNear(100, sniper));

        assertThat(sniper.experience().xp()).isEqualTo(100);
    }

    @Test
    void kinshipStopsAtTwentyPercentWithoutBroadcast() {
        this.auraWith(3, 3);
        SniperTower sniper = this.place(new SniperTower(this.world, 3, 2));
        this.place(new SplashTower(this.world, 3, 4));
        this.place(new MortarTower(this.world, 2, 3));
        this.place(new PulseTower(this.world, 4, 3));
        this.place(new SeekerTower(this.world, 2, 2));
        this.place(new SonarTower(this.world, 4, 4));

        this.awarder.walkEnded(walkNear(100, sniper));

        assertThat(sniper.experience().xp()).isEqualTo(120);
    }

    @Test
    void broadcastLiftsKinshipsCapAndCountsOneCellFurther() {
        this.auraWith(3, 3, "Broadcast");
        SniperTower sniper = this.place(new SniperTower(this.world, 3, 2));
        this.place(new SplashTower(this.world, 3, 4));
        this.place(new MortarTower(this.world, 2, 3));
        this.place(new PulseTower(this.world, 4, 3));
        this.place(new SeekerTower(this.world, 2, 2));
        this.place(new SonarTower(this.world, 4, 4));

        this.awarder.walkEnded(walkNear(100, sniper));

        assertThat(sniper.experience().xp()).isEqualTo(125);
    }

    @Test
    void tutelageAddsTenPercentOnTopOfKinship() {
        this.auraWith(3, 3, "Tutelage");
        SniperTower sniper = this.place(new SniperTower(this.world, 3, 2));
        this.place(new SplashTower(this.world, 3, 4));

        this.awarder.walkEnded(walkNear(100, sniper));

        assertThat(sniper.experience().xp()).isEqualTo(115);
    }

    @Test
    void sharedLessonsPaysEveryBuffedTowerForWhatAnyOfThemReached() {
        this.auraWith(3, 3, "Tutelage", "Shared Lessons");
        SniperTower near = this.place(new SniperTower(this.world, 3, 2));
        SplashTower beside = this.place(new SplashTower(this.world, 3, 4));
        SonarTower outside = this.place(new SonarTower(this.world, 15, 15));

        this.awarder.walkEnded(walkNear(100, near));

        assertThat(beside.experience().xp()).isGreaterThan(100);
        assertThat(outside.experience().xp()).isZero();
    }

    @Test
    void withoutSharedLessonsABuffedTowerThatDidNotReachEarnsNothing() {
        this.auraWith(3, 3, "Tutelage");
        SniperTower near = this.place(new SniperTower(this.world, 3, 2));
        SplashTower beside = this.place(new SplashTower(this.world, 3, 4));

        this.awarder.walkEnded(walkNear(100, near));

        assertThat(beside.experience().xp()).isZero();
    }

    @Test
    void theApprenticeTheBuffedTowerWithTheLeastXpEarnsDouble() {
        this.auraWith(3, 3, "Tutelage", "Shared Lessons", "Apprentice");
        SniperTower veteran = this.place(new SniperTower(this.world, 3, 2));
        SplashTower novice = this.place(new SplashTower(this.world, 3, 4));
        veteran.earnXp(500);

        this.awarder.walkEnded(walkNear(100, veteran, novice));

        assertThat(veteran.experience().xp()).isEqualTo(500 + 110 + 5);
        assertThat(novice.experience().xp()).isEqualTo(100 + 100 + 10 + 5);
    }

    @Test
    void theFractionOfXpABonusCannotHoldCarriesToTheNextWalk() {
        this.auraWith(3, 3);
        SniperTower sniper = this.place(new SniperTower(this.world, 3, 2));
        this.place(new SplashTower(this.world, 3, 4));
        EnemyWalk walk = walkNear(5, sniper);

        for (int i = 0; i < 4; i++) {
            this.awarder.walkEnded(walk);
        }

        assertThat(sniper.experience().xp()).isEqualTo(21);
    }
}
