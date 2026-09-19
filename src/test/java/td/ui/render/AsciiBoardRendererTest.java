package td.ui.render;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AsciiBoardRenderer's whole point is deterministic, assertable output - unlike
 * Java2DFrameRenderer, its result is a plain String that can be compared exactly.
 */
class AsciiBoardRendererTest {

    private static final int SCALE = 10;

    private static RenderFrame frameOf(List<EnemyDraw> enemies, List<TowerSpriteDraw> towers) {
        return new RenderFrame(SCALE, 3 * SCALE - 1, 2 * SCALE - 1, List.of(), enemies, List.of(), List.of(), towers,
                List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void anEmptyBoardIsAllDots() {
        String ascii = new AsciiBoardRenderer().render(frameOf(List.of(), List.of()));

        assertThat(ascii).isEqualTo("...\n...\n");
    }

    @Test
    void placesTowersAndEnemiesAtTheirCellWithFadingEnemiesUppercased() {
        TowerSpriteDraw tower1 = new TowerSpriteDraw(Palette.TOWER_SNIPER_BODY, SCALE, 0, false, 15f, 5f, 50f, Optional.empty());
        EnemyBodyDraw circle = new EnemyBodyDraw(Palette.ENEMY_CIRCLE, 25f, 15f, 0.0, 5f, 1f, RankBadge.NONE);
        EnemyFadeDraw fadingSquare = new EnemyFadeDraw(Palette.ENEMY_SQUARE, 5f, 5f, 0.0, 5f, 2f, 0.5f);

        String ascii = new AsciiBoardRenderer().render(frameOf(List.of(circle, fadingSquare), List.of(tower1)));

        assertThat(ascii).isEqualTo("S1.\n..c\n");
    }

    @Test
    void anEnemyOutsideTheBoardIsSafelyIgnored() {
        EnemyBodyDraw offBoard = new EnemyBodyDraw(Palette.ENEMY_TRIANGLE, 999f, 999f, 0.0, 5f, 1f, RankBadge.NONE);

        String ascii = new AsciiBoardRenderer().render(frameOf(List.of(offBoard), List.of()));

        assertThat(ascii).isEqualTo("...\n...\n");
    }
}
