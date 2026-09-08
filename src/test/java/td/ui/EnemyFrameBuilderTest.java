package td.ui;

import org.junit.jupiter.api.Test;
import td.enemy.AbstractEnemyMob;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.ui.render.EnemyBodyDraw;
import td.util.Context;
import td.util.RecordingGameHost;
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

    private static Context contextWithStraightPath() {
        Context context = new Context(new RecordingGameHost());
        context.maxX = 1000;
        context.maxY = 1000;
        context.setPath(new PathNormal(List.of(new Vec2(5, 5), new Vec2(105, 5))));
        return context;
    }

    private static EnemyBodyDraw bodyDrawAt(EnemyMob enemy, int gameTime, double alpha) {
        EnemyFrameBuilder builder = new EnemyFrameBuilder(gameTime, alpha);
        enemy.accept(builder);
        return (EnemyBodyDraw) builder.build().get(0);
    }

    @Test
    void alphaZeroReproducesThePreviousTickPosition() {
        Context context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        AbstractEnemyMob mob = (AbstractEnemyMob) enemy;
        enemy.doTick(1);

        EnemyBodyDraw draw = bodyDrawAt(enemy, 1, 0.0);

        assertThat(draw.x()).isEqualTo((float) mob.getPrevX());
        assertThat(draw.y()).isEqualTo((float) mob.getPrevY());
    }

    @Test
    void alphaOneReproducesTheCurrentTickPosition() {
        Context context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        AbstractEnemyMob mob = (AbstractEnemyMob) enemy;
        enemy.doTick(1);

        EnemyBodyDraw draw = bodyDrawAt(enemy, 1, 1.0);

        assertThat(draw.x()).isEqualTo((float) mob.getX());
        assertThat(draw.y()).isEqualTo((float) mob.getY());
    }

    @Test
    void alphaOneHalfIsTheMidpointBetweenPreviousAndCurrentPosition() {
        Context context = contextWithStraightPath();
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
    void anEnemyThatJustWrappedBackToThePathStartIsNotInterpolatedAcrossTheBoard() {
        Context context = new Context(new RecordingGameHost());
        context.maxX = 1000;
        context.maxY = 1000;
        context.setPath(new PathNormal(List.of(new Vec2(5, 5), new Vec2(15, 5))));

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, 1);
        int initialLives = context.getLives();
        int tick = 0;
        while (context.getLives() == initialLives) {
            tick++;
            enemy.doTick(tick);
        }

        EnemyBodyDraw drawAtZero = bodyDrawAt(enemy, tick, 0.0);
        EnemyBodyDraw drawAtOne = bodyDrawAt(enemy, tick, 1.0);

        assertThat(drawAtZero.x()).isEqualTo(drawAtOne.x());
        assertThat(drawAtZero.y()).isEqualTo(drawAtOne.y());
    }
}
