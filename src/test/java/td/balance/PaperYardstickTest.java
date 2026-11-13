package td.balance;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaperYardstickTest {

    private static final Offset<Double> EXACT = Offset.offset(1e-9);

    @Test
    void theBareTowerLandsItsDamageEveryIntervalForItsPrice(@TempDir Path folder) throws IOException {
        PaperYardstick yardstick = PaperYardstick.read(stored(folder));

        assertThat(yardstick.reachCells()).isEqualTo(2.5, EXACT);
        assertThat(yardstick.steps().getFirst().cost()).isEqualTo(10, EXACT);
        assertThat(yardstick.steps().getFirst().damagePerSecond()).isEqualTo(20.0 / 4, EXACT);
    }

    @Test
    void aTierLandsTheBareDamageTimesItsMixOverTheBareMix(@TempDir Path folder) throws IOException {
        PaperYardstick yardstick = PaperYardstick.read(stored(folder));

        assertThat(yardstick.steps()).hasSize(3);
        assertThat(yardstick.steps().get(1).cost()).isEqualTo(30, EXACT);
        assertThat(yardstick.steps().get(1).damagePerSecond()).isEqualTo(5.0 * 8 * 10 / 40, EXACT);
        assertThat(yardstick.steps().get(2).damagePerSecond()).isEqualTo(5.0 * 12 * 10 / 40, EXACT);
    }

    @Test
    void aMissingReferenceSaysHowToMakeOne(@TempDir Path folder) {
        assertThatThrownBy(() -> PaperYardstick.read(folder.resolve("none.json")))
                .isInstanceOf(UncheckedIOException.class).hasMessageContaining("tree calculator");
    }

    private static Path stored(Path folder) throws IOException {
        return Files.writeString(folder.resolve("reference.json"), """
                {
                  "price": 10,
                  "bare_mix": 40.0,
                  "tiers": {
                    "0": { "mix": 8.0, "role": 1.0, "cost": 30.0 },
                    "I": { "mix": 12.0, "role": 1.0, "cost": 60.0 }
                  },
                  "numbers": { "damage": 20.0, "interval": 4.0, "range": 2.5 }
                }
                """);
    }
}
