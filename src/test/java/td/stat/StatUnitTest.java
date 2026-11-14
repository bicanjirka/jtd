package td.stat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatUnitTest {

    @Test
    void aCountOfExactlyOneReadsInTheSingularEitherSign() {
        assertThat(StatUnit.STACKS.word(1f)).isEqualTo("stack");
        assertThat(StatUnit.CELLS.word(-1f)).isEqualTo("cell");
        assertThat(StatUnit.STACKS.word(2f)).isEqualTo("stacks");
        assertThat(StatUnit.CELLS.word(0.5f)).isEqualTo("cells");
    }
}
