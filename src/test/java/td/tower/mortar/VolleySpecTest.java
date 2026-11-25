package td.tower.mortar;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VolleySpecTest {

    @Test
    void aSneakedNormalCarriesThePayloadItFliesRightBehindAndTheLoadedShellsKeepTheirOwn() {
        VolleySpec volley = VolleySpec.alongThePath(3, 1, 1f, 1f).withNormalsBetween();
        HeldShell payload = new HeldShell(ShellKind.TAR, Optional.empty());
        HeldShell carrying = new HeldShell(ShellKind.NORMAL, Optional.of(ShellKind.TAR));
        HeldShell heavy = new HeldShell(ShellKind.HEAVY, Optional.empty());

        List<LinedUpShell> sent = volley.sequence(List.of(payload, carrying, heavy));

        assertThat(sent).containsExactly(LinedUpShell.held(payload), LinedUpShell.sneaked(carrying),
                LinedUpShell.held(carrying), LinedUpShell.sneaked(new HeldShell(ShellKind.NORMAL, Optional.empty())),
                LinedUpShell.held(heavy));
    }
}
