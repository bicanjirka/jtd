package td.tower.proc;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.EffectKind;
import td.fixtures.FakeEnemyMob;
import td.tower.TowerModifiers;
import td.tower.TowerStatus;
import td.tower.TowerTerm;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ProcRunnerTest {

    private static final ProcPayload VULNERABLE = new ProcPayload.ApplyStacks(EffectKind.VULNERABLE, 1);

    private final RecordingProcActions actions = new RecordingProcActions();
    private final FakeEnemyMob target = FakeEnemyMob.at(0, 0);

    private AttackOutcome outcome(HitKind kind, boolean critical, boolean killed) {
        return new AttackOutcome(this.target, kind, critical, killed);
    }

    @Test
    void anOnHitProcRunsOncePerOutcome() {
        List<Proc> procs = List.of(Proc.onHit(VULNERABLE));

        ProcRunner.onOutcome(procs, this.outcome(HitKind.DIRECT, false, false), this.actions);
        ProcRunner.onOutcome(procs, this.outcome(HitKind.DIRECT, true, false), this.actions);

        assertThat(this.actions.calls()).containsExactly("stacks VULNERABLE 1", "stacks VULNERABLE 1");
    }

    @Test
    void anOnCritProcRunsOnlyOnACrit() {
        List<Proc> procs = List.of(Proc.onCrit(VULNERABLE));

        ProcRunner.onOutcome(procs, this.outcome(HitKind.DIRECT, false, false), this.actions);
        ProcRunner.onOutcome(procs, this.outcome(HitKind.DIRECT, true, false), this.actions);

        assertThat(this.actions.calls()).containsExactly("stacks VULNERABLE 1");
    }

    @Test
    void anOnKillProcRunsOnlyOnTheKill() {
        List<Proc> procs = List.of(Proc.onKill(new ProcPayload.Charge(60)));

        ProcRunner.onOutcome(procs, this.outcome(HitKind.DIRECT, true, false), this.actions);
        ProcRunner.onOutcome(procs, this.outcome(HitKind.DIRECT, false, true), this.actions);

        assertThat(this.actions.calls()).containsExactly("charge 60");
    }

    @Test
    void onlyAnOnDodgeProcRunsOnADodge() {
        List<Proc> procs = List.of(Proc.onHit(VULNERABLE), Proc.onCrit(VULNERABLE), Proc.onDodge(new ProcPayload.Charge(60)));

        ProcRunner.onDodge(procs, this.outcome(HitKind.DIRECT, false, false), this.actions);
        ProcRunner.onOutcome(procs, this.outcome(HitKind.DIRECT, true, false), this.actions);

        assertThat(this.actions.calls()).containsExactly("charge 60", "stacks VULNERABLE 1", "stacks VULNERABLE 1");
    }

    @Test
    void aConditionThatFailsStopsTheProc() {
        List<Proc> procs = List.of(Proc.onHit(VULNERABLE).when(new ProcCondition.HitKindIn(Set.of(HitKind.ARC))));

        ProcRunner.onOutcome(procs, this.outcome(HitKind.BLAST, false, false), this.actions);
        ProcRunner.onOutcome(procs, this.outcome(HitKind.ARC, false, false), this.actions);

        assertThat(this.actions.calls()).containsExactly("stacks VULNERABLE 1");
    }

    @Test
    void everyNthShotFiresOnlyOnMultiples() {
        TowerStatus silver = TowerStatus.nextShots(TowerTerm.SILVER_ROUND, 1, TowerModifiers.magicPenetration(0.25f));
        List<Proc> procs = List.of(Proc.everyNthShot(3, new ProcPayload.GrantStatus(silver)));

        IntStream.rangeClosed(1, 7).forEach(shot -> ProcRunner.onShot(procs, shot, this.actions));

        assertThat(this.actions.calls()).containsExactly("status Silver Round", "status Silver Round");
    }

    @Test
    void aPayloadSkipsADeadTarget() {
        List<Proc> procs = List.of(Proc.onHit(VULNERABLE), Proc.onHit(new ProcPayload.Dispel()));
        this.target.dieOnAnyHit();
        this.target.takeHit(Damage.physical(1), AttackProfile.none());

        ProcRunner.onOutcome(procs, this.outcome(HitKind.DIRECT, false, true), this.actions);

        assertThat(this.actions.calls()).isEmpty();
        assertThat(this.target.dispels()).isZero();
    }
}
