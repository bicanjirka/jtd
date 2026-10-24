package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.AttackOrigin;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EnemyObservationTest {

    private final List<String> lines = new ArrayList<>();
    private final AttackOrigin tower = AttackOrigin.fresh();
    private final EnemyObservation observation = new EnemyObservation(this.lines::add,
            origin -> origin == this.tower ? Optional.of("Tower@1,2") : Optional.empty());

    @Test
    void anAttackFromATowerIsNamedAfterTheTower() {
        assertThat(this.observation.nameOf(this.tower)).isEqualTo("Tower@1,2");
    }

    @Test
    void anOriginGivenANameIsCalledByIt() {
        AttackOrigin panel = AttackOrigin.fresh();

        this.observation.name(panel, "dev:effect");

        assertThat(this.observation.nameOf(panel)).isEqualTo("dev:effect");
    }

    @Test
    void anAttackWithNoTowerIsNamedAfterWhatIsRunningAndOtherwiseNoOne() {
        List<String> names = new ArrayList<>();

        this.observation.during("dev:kill", () -> names.add(this.observation.nameOf(AttackOrigin.none())));
        names.add(this.observation.nameOf(AttackOrigin.none()));

        assertThat(names).containsExactly("dev:kill", "-");
    }

    @Test
    void aSnapshotWithNothingObservedSaysSoInOneLine() {
        this.observation.beginTick(7);

        this.observation.snapshotAll();

        assertThat(this.lines).containsExactly("OBSERVE t=7 SNAPSHOT nothing observed");
    }

    @Test
    void everyLineIsStampedWithTheTickBeingSimulated() {
        GameWorld world = WorldFixtures.newWorldOnStraightPath();
        DefinedEnemyMob mob = EnemyFixtures.plainMob(world, 100, 1);
        this.observation.observe(mob, "test");

        this.observation.beginTick(42);
        this.observation.end(mob, "stopped");

        assertThat(this.lines.getFirst()).startsWith("OBSERVE t=0 #" + mob.id() + " ");
        assertThat(this.lines.getLast()).startsWith("OBSERVE t=42 #" + mob.id() + " ");
    }

    @Test
    void anObservedEnemyThatLeftTheRosterWithoutAnEndEndsAsRemoved() {
        GameWorld world = WorldFixtures.newWorldOnStraightPath();
        DefinedEnemyMob mob = EnemyFixtures.plainMob(world, 100, 1);
        RecordingMobEvents events = new RecordingMobEvents();
        this.observation.observe(mob, events, "test");
        world.enemies().remove(mob);

        this.observation.sweep(world.enemies());

        assertThat(events.of(MobEvent.Ended.class)).singleElement()
                .satisfies(ended -> assertThat(ended.how()).isEqualTo("removed"));
        assertThat(this.observation.anyObserved()).isFalse();
    }

    @Test
    void endingEveryTraceEndsEachWithTheReasonGiven() {
        GameWorld world = WorldFixtures.newWorldOnStraightPath();
        RecordingMobEvents events = new RecordingMobEvents();
        this.observation.observe(EnemyFixtures.plainMob(world, 100, 1), events, "test");
        this.observation.observe(EnemyFixtures.plainMob(world, 100, 1), events, "test");

        this.observation.endAll("level-ended");

        assertThat(events.of(MobEvent.Ended.class)).extracting(MobEvent.Ended::how)
                .containsExactly("level-ended", "level-ended");
    }

    @Test
    void anEnemyAlreadyObservedIsNotObservedTwice() {
        GameWorld world = WorldFixtures.newWorldOnStraightPath();
        DefinedEnemyMob mob = EnemyFixtures.plainMob(world, 100, 1);
        RecordingMobEvents events = new RecordingMobEvents();
        this.observation.observe(mob, events, "test");

        this.observation.observe(mob, events, "again");

        assertThat(events.of(MobEvent.Started.class)).hasSize(1);
    }
}
