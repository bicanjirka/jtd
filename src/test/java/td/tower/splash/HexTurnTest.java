package td.tower.splash;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class HexTurnTest {

    private static final HexScene SCENE = new HexScene(List.of(FakeEnemyMob.at(0, 0)), 48f, 48f);

    /** A hex that picks the first candidate, or nobody. */
    private record StubHex(EffectKind kind, boolean finds) implements Hex {

        @Override
        public Optional<EnemyMob> target(HexScene scene) {
            return this.finds ? Optional.of(scene.candidates().getFirst()) : Optional.empty();
        }

        @Override
        public void cast(EnemyMob target, HexSpec spec, HexActions actions) {
        }
    }

    private static List<EffectKind> takeTimes(HexTurn turn, List<Hex> pool, int times) {
        return IntStream.range(0, times)
                .mapToObj(i -> turn.take(pool, SCENE).map(pick -> pick.hex().kind()).orElse(null))
                .toList();
    }

    @Test
    void castsTakeTheHexesInTurn() {
        List<Hex> pool = List.of(new StubHex(EffectKind.DOOM, true), new StubHex(EffectKind.EXPOSED, true));

        assertThat(takeTimes(new HexTurn(), pool, 3)).containsExactly(EffectKind.DOOM, EffectKind.EXPOSED,
                EffectKind.DOOM);
    }

    @Test
    void aHexWithNoTargetIsSkippedForThatCast() {
        List<Hex> pool = List.of(new StubHex(EffectKind.DOOM, false), new StubHex(EffectKind.EXPOSED, true));

        assertThat(takeTimes(new HexTurn(), pool, 2)).containsExactly(EffectKind.EXPOSED, EffectKind.EXPOSED);
    }

    @Test
    void whenNoHexHasATargetThereIsNoCast() {
        List<Hex> pool = List.of(new StubHex(EffectKind.DOOM, false));

        assertThat(new HexTurn().take(pool, SCENE)).isEmpty();
    }

    @Test
    void aRestartGoesBackToTheFirstHex() {
        List<Hex> pool = List.of(new StubHex(EffectKind.DOOM, true), new StubHex(EffectKind.EXPOSED, true));
        HexTurn turn = new HexTurn();
        turn.take(pool, SCENE);

        turn.restart();

        assertThat(turn.take(pool, SCENE)).map(pick -> pick.hex().kind()).contains(EffectKind.DOOM);
    }
}
