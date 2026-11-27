package td.tower;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyInspection;
import td.enemy.EnemyWalk;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.tower.aura.KinshipMechanic;
import td.tower.aura.MentorMechanic;
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
            public Rank getRank() {
                return Rank.GRUNT;
            }

            @Override
            public EnemyInspection.Fate fate() {
                return EnemyInspection.Fate.KILLED;
            }

            @Override
            public int livesOnLeak() {
                return 1;
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
        UpgradePaths.buy(aura, this.world, "Lantern");
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
    void aBeaconEarnsWhatTheTowersItLightsEarnWithoutAnyBonusOfItsOwn() {
        AuraTower aura = this.place(new AuraTower(this.world, 3, 3));
        SniperTower near = this.place(new SniperTower(this.world, 3, 2));

        this.awarder.walkEnded(walkNear(100, near));

        assertThat(aura.experience().xp()).isEqualTo(100);
    }

    @Test
    void kinshipPaysItsShareForEachOtherTowerTypeInTheLight() {
        this.auraWith(3, 3, "Kinship");
        SniperTower sniper = this.place(new SniperTower(this.world, 3, 2));
        this.place(new SplashTower(this.world, 3, 4));
        this.place(new MortarTower(this.world, 2, 3));

        this.awarder.walkEnded(walkNear(100, sniper));

        assertThat(sniper.experience().xp()).isEqualTo(Math.round(100 * (1f + KinshipMechanic.xpFor(2))));
    }

    @Test
    void mentorDoublesWhatTheLitTowerWithTheLeastXpEarns() {
        this.auraWith(3, 3, "Mentor");
        SniperTower veteran = this.place(new SniperTower(this.world, 3, 2));
        SplashTower novice = this.place(new SplashTower(this.world, 3, 4));
        veteran.earnXp(500);

        this.awarder.walkEnded(walkNear(100, veteran, novice));

        assertThat(veteran.experience().xp()).isEqualTo(500 + 100);
        assertThat(novice.experience().xp()).isEqualTo(Math.round(100 * (1f + MentorMechanic.XP)));
    }

    @Test
    void theFractionOfXpABonusCannotHoldCarriesToTheNextWalk() {
        this.auraWith(3, 3, "Kinship");
        SniperTower sniper = this.place(new SniperTower(this.world, 3, 2));
        this.place(new SplashTower(this.world, 3, 4));
        EnemyWalk walk = walkNear(5, sniper);

        for (int i = 0; i < 4; i++) {
            this.awarder.walkEnded(walk);
        }

        assertThat(sniper.experience().xp()).isEqualTo(21);
    }
}
