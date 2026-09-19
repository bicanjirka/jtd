package td.ui;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.Effect;
import td.enemy.AbstractEnemyMob;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.fixtures.WorldFixtures;
import td.ui.render.CritSparkDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.Palette;
import td.ui.render.StatusMarkerDraw;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Verifies EnemyFrameBuilder actually lerps an alive body between the mob's
 * previous and current tick position, rather than just plumbing gameTime
 * through untouched.
 */
class EnemyFrameBuilderTest {

    private static GameWorld contextWithStraightPath() {
        GameWorld context = WorldFixtures.newWorldOnBoard(1, 1001, 1001);
        context.setPath(new PathNormal(List.of(new Vec2(5, 5), new Vec2(105, 5))));
        return context;
    }

    private static EnemyBodyDraw bodyDrawAt(EnemyMob enemy, int gameTime, double alpha) {
        EnemyFrameBuilder builder = new EnemyFrameBuilder(gameTime, alpha);
        enemy.accept(builder);
        return (EnemyBodyDraw) builder.build().getFirst();
    }

    @Test
    void alphaZeroReproducesThePreviousTickPosition() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        AbstractEnemyMob mob = (AbstractEnemyMob) enemy;
        enemy.doTick(1);

        EnemyBodyDraw draw = bodyDrawAt(enemy, 1, 0.0);

        assertThat(draw.x()).isEqualTo((float) mob.getPrevX());
        assertThat(draw.y()).isEqualTo((float) mob.getPrevY());
    }

    @Test
    void alphaOneReproducesTheCurrentTickPosition() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        AbstractEnemyMob mob = (AbstractEnemyMob) enemy;
        enemy.doTick(1);

        EnemyBodyDraw draw = bodyDrawAt(enemy, 1, 1.0);

        assertThat(draw.x()).isEqualTo((float) mob.getX());
        assertThat(draw.y()).isEqualTo((float) mob.getY());
    }

    @Test
    void alphaOneHalfIsTheMidpointBetweenPreviousAndCurrentPosition() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        AbstractEnemyMob mob = (AbstractEnemyMob) enemy;
        enemy.doTick(1);
        float expectedX = (float) ((mob.getPrevX() + mob.getX()) / 2.0);
        float expectedY = (float) ((mob.getPrevY() + mob.getY()) / 2.0);

        EnemyBodyDraw draw = bodyDrawAt(enemy, 1, 0.5);

        assertThat(draw.x()).isCloseTo(expectedX, within(0.01f));
        assertThat(draw.y()).isCloseTo(expectedY, within(0.01f));
    }

    @Test
    void anEnemyThatReachesThePathsEndFadesInPlaceInsteadOfWrappingToTheStart() {
        GameWorld context = WorldFixtures.newWorldOnBoard(1, 1001, 1001);
        context.setPath(new PathNormal(List.of(new Vec2(5, 5), new Vec2(15, 5))));

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, 1);
        int initialLives = context.economy().getLives();
        int tick = 0;
        while (context.economy().getLives() == initialLives) {
            tick++;
            enemy.doTick(tick);
        }

        assertThat(enemy.isDead()).isTrue();
        EnemyFrameBuilder builder = new EnemyFrameBuilder(tick, 0.0);
        enemy.accept(builder);
        assertThat(builder.build().getFirst()).isInstanceOf(EnemyFadeDraw.class);
    }

    @Test
    void anEnemyWithNoActiveEffectsYieldsNoStatusMarkers() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);

        EnemyFrameBuilder builder = new EnemyFrameBuilder(0, 0.0);
        enemy.accept(builder);

        assertThat(builder.buildMarkers()).isEmpty();
    }

    @Test
    void anActiveSlowYieldsExactlyOneStatusMarkerWithTheSlowRole() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        enemy.applyEffect(Effect.slow(0.5f, 5, d -> {
        }));

        EnemyFrameBuilder builder = new EnemyFrameBuilder(0, 0.0);
        enemy.accept(builder);
        List<StatusMarkerDraw> markers = builder.buildMarkers();

        assertThat(markers).hasSize(1);
        assertThat(markers.getFirst().palette()).isEqualTo(Palette.STATUS_MARKER_SLOW);
    }

    @Test
    void twoActiveEffectsYieldTwoDistinctlyPositionedMarkers() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        enemy.applyEffect(Effect.slow(0.5f, 5, d -> {
        }));
        enemy.applyEffect(Effect.burn(td.damage.Damage.magic(10), 5, d -> {
        }));

        EnemyFrameBuilder builder = new EnemyFrameBuilder(0, 0.0);
        enemy.accept(builder);
        List<StatusMarkerDraw> markers = builder.buildMarkers();

        assertThat(markers).hasSize(2);
        assertThat(markers.get(0).x()).isNotEqualTo(markers.get(1).x());
    }

    @Test
    void aFourthSimultaneousEffectCollapsesIntoOneOverflowMarkerInsteadOfGrowingTheRow() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        enemy.applyEffect(Effect.slow(0.5f, 5, d -> {
        }));
        enemy.applyEffect(Effect.burn(td.damage.Damage.magic(10), 5, d -> {
        }));
        enemy.applyEffect(Effect.freeze(5, d -> {
        }));
        enemy.applyEffect(Effect.shield(0.3f, 5, d -> {
        }));

        EnemyFrameBuilder builder = new EnemyFrameBuilder(0, 0.0);
        enemy.accept(builder);
        List<StatusMarkerDraw> markers = builder.buildMarkers();

        assertThat(markers).hasSize(EnemyFrameBuilder.MAX_VISIBLE_MARKERS + 1);
        assertThat(markers.getLast().palette()).isEqualTo(Palette.STATUS_MARKER_OVERFLOW);
    }

    @Test
    void anEnemyThatNeverTookACriticalHitYieldsNoCritSpark() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        enemy.doTick(1);

        EnemyFrameBuilder builder = new EnemyFrameBuilder(1, 0.0);
        enemy.accept(builder);

        assertThat(builder.buildCritSparks()).isEmpty();
    }

    @Test
    void anEnemyThatJustSurvivedACriticalHitYieldsOneCritSpark() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        enemy.doDamage(Damage.physical(10).asCritical());
        enemy.doTick(1); // captures the critical hit

        EnemyFrameBuilder builder = new EnemyFrameBuilder(1, 0.0);
        enemy.accept(builder);
        List<CritSparkDraw> sparks = builder.buildCritSparks();

        assertThat(sparks).hasSize(1);
        assertThat(sparks.getFirst().palette()).isEqualTo(Palette.CRIT_SPARK);
        assertThat(sparks.getFirst().fadeProgress()).isZero();
    }

    @Test
    void theCritSparkStopsShowingAfterItsDurationElapses() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        enemy.doDamage(Damage.physical(10).asCritical());
        enemy.doTick(1); // captures the critical hit at tick 1

        int afterDuration = 1 + EnemyFrameBuilder.CRIT_SPARK_DURATION_TICKS + 1;
        EnemyFrameBuilder builder = new EnemyFrameBuilder(afterDuration, 0.0);
        enemy.accept(builder);

        assertThat(builder.buildCritSparks()).isEmpty();
    }

    @Test
    void aFifthSimultaneousEffectDoesNotGrowTheRowFurther() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        enemy.applyEffect(Effect.slow(0.5f, 5, d -> {
        }));
        enemy.applyEffect(Effect.burn(td.damage.Damage.magic(10), 5, d -> {
        }));
        enemy.applyEffect(Effect.freeze(5, d -> {
        }));
        enemy.applyEffect(Effect.shield(0.3f, 5, d -> {
        }));
        enemy.applyEffect(Effect.invisible(5, d -> {
        }));

        EnemyFrameBuilder builder = new EnemyFrameBuilder(0, 0.0);
        enemy.accept(builder);
        List<StatusMarkerDraw> markers = builder.buildMarkers();

        assertThat(markers).hasSize(EnemyFrameBuilder.MAX_VISIBLE_MARKERS + 1);
    }
}
