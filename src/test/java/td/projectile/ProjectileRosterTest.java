package td.projectile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectileRosterTest {

    private final ProjectileRoster roster = new ProjectileRoster();

    @Test
    void addedProjectilesAppearInGetProjectiles() {
        CannonballProjectile shell = new CannonballProjectile(0, 0, 100, 0, 10f, (x, y) -> {
        });

        roster.add(shell);

        assertThat(roster.getProjectiles()).containsExactly(shell);
    }

    @Test
    void doTickAdvancesEveryLiveProjectile() {
        CannonballProjectile shell = new CannonballProjectile(0, 0, 100, 0, 10f, (x, y) -> {
        });
        roster.add(shell);

        roster.doTick(1);

        assertThat(shell.getX()).isEqualTo(10.0);
    }

    @Test
    void aFinishedProjectileIsDroppedFromTheRosterAfterItsTick() {
        CannonballProjectile shell = new CannonballProjectile(0, 0, 10, 0, 10f, (x, y) -> {
        });
        roster.add(shell);

        roster.doTick(1); // arrives in exactly one tick

        assertThat(shell.isFinished()).isTrue();
        assertThat(roster.getProjectiles()).isEmpty();
    }

    @Test
    void clearingTheRosterRemovesEveryProjectileInFlight() {
        roster.add(new CannonballProjectile(0, 0, 100, 0, 10f, (x, y) -> {
        }));

        roster.clear();

        assertThat(roster.getProjectiles()).isEmpty();
    }
}
