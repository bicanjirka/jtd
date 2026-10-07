package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.tower.AuraTower;
import td.tower.SniperTower;
import td.tower.Tower;
import td.ui.render.TowerSpriteDraw;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TowerSpriteFacingTest {

    private final GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

    private TowerSpriteDraw spriteOf(Tower tower) {
        TowerSpriteFrameBuilder builder = new TowerSpriteFrameBuilder(this.context, 0, 1.0, 0.0);
        tower.accept(builder);
        return builder.build().getFirst();
    }

    @Test
    void anAimingTowersBodyIsGivenTheHeadingItsTurretTurnsTo() {
        SniperTower sniper = new SniperTower(this.context, 3, 3);
        this.context.towers().add(sniper);
        FakeEnemyMob target = FakeEnemyMob.at(sniper.getX(), sniper.getY() - 100);
        this.context.enemies().setEnemies(new td.enemy.EnemyMob[]{target});
        for (int t = 0; t < 40; t++) {
            sniper.doTick(t);
        }

        TowerSpriteDraw sprite = this.spriteOf(sniper);

        assertThat(sprite.aims()).isTrue();
        assertThat((double) sprite.facingRadians()).isCloseTo(sniper.getTurretAim().radiansAt(1.0), within(1e-5));
    }

    @Test
    void aTowerThatDoesNotAimGivesNoFacing() {
        AuraTower aura = new AuraTower(this.context, 3, 3);

        assertThat(this.spriteOf(aura).aims()).isFalse();
    }
}
