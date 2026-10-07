package td.tower.seeker;

import org.junit.jupiter.api.Test;
import td.projectile.MissileLook;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadPlanTest {

    private static List<MissileLook> looks(PayloadPlan plan, int missiles) {
        return IntStream.rangeClosed(1, missiles)
                .mapToObj(n -> plan.loadFor(n).map(load -> load.payload().look()).orElse(MissileLook.STANDARD))
                .toList();
    }

    @Test
    void withNoPayloadsEveryMissileIsPlain() {
        assertThat(looks(PayloadPlan.none(), 6)).containsOnly(MissileLook.STANDARD);
    }

    @Test
    void everyThirdMissileCarriesTheOnlyOwnedPayload() {
        PayloadPlan plan = PayloadPlan.none().with(new ArcanePayload());

        assertThat(looks(plan, 7)).containsExactly(MissileLook.STANDARD, MissileLook.STANDARD, MissileLook.ARCANE,
                MissileLook.STANDARD, MissileLook.STANDARD, MissileLook.ARCANE, MissileLook.STANDARD);
    }

    @Test
    void theOwnedPayloadsTakeTurnsInTheOrderTheyWereBought() {
        PayloadPlan plan = PayloadPlan.none().with(new ArcanePayload()).with(new EmpPayload());

        assertThat(looks(plan, 9)).containsExactly(MissileLook.STANDARD, MissileLook.STANDARD, MissileLook.ARCANE,
                MissileLook.STANDARD, MissileLook.STANDARD, MissileLook.EMP, MissileLook.STANDARD,
                MissileLook.STANDARD, MissileLook.ARCANE);
    }

    @Test
    void aFullRackSendsEveryMissileWithAPayloadCyclingThroughAllFourAtAQuarterMoreStrength() {
        PayloadPlan plan = PayloadPlan.none().with(new ArcanePayload()).forEveryMissile();

        assertThat(looks(plan, 5)).containsExactly(MissileLook.CRYO, MissileLook.ARCANE, MissileLook.EMP,
                MissileLook.TRACER, MissileLook.CRYO);
        assertThat(plan.loadFor(1).orElseThrow().strength()).isEqualTo(1.25f);
    }

    @Test
    void aPayloadFromBeforeTheFullRackIsStillAtBaseStrength() {
        PayloadPlan plan = PayloadPlan.none().with(new ArcanePayload());

        assertThat(plan.loadFor(3).orElseThrow().strength()).isEqualTo(1f);
    }
}
