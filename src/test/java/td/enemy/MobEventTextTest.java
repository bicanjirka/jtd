package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.Delivery;
import td.effect.EffectKind;
import td.stat.EnemyStat;
import td.zone.ZoneKind;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

class MobEventTextTest {

    private static final MobLabel LABEL = new MobLabel(12, "Brute", Rank.ELITE, 50_000);
    private static final String PREFIX = "OBSERVE t=34 #12 Brute Elite ";

    @Test
    void aStartNamesHowItBeganThenHealthEveryStatTraitsAndEffects() {
        MobSnapshot snapshot = new MobSnapshot(40_000,
                List.of(new MobSnapshot.StatReading(EnemyStat.ARMOR, 30f, 40f),
                        new MobSnapshot.StatReading(EnemyStat.PLATING, 200f, 200f)),
                List.of("Tough"), List.of(new EffectState(EffectKind.CHILL, 0, OptionalInt.empty(), OptionalDouble.of(0.25))));

        String line = line(new MobEvent.Started("selected", snapshot));

        assertThat(line).isEqualTo(PREFIX + "START via=selected hp=400/500 armor=40(base 30) plating=2 traits=[Tough] effects=["
                + EffectKind.CHILL.title() + " full=0.25]");
    }

    @Test
    void aSnapshotShowsTheSameAsAStartWithoutHowItBegan() {
        MobSnapshot snapshot = new MobSnapshot(50_000, List.of(), List.of(), List.of());

        assertThat(line(new MobEvent.Snapshot(snapshot))).isEqualTo(PREFIX + "SNAPSHOT hp=500/500 traits=[] effects=[]");
    }

    @Test
    void aHitShowsEveryStepFromWhatArrivedToWhatLandedInPoints() {
        HitSteps steps = HitSteps.arriving(Damage.physical(1000), Delivery.HIT)
                .then(HitStep.crit(1.5f, 1500f))
                .then(HitStep.mitigation(40f, 30f, 1153.85f))
                .then(HitStep.flatReduction(200f, 100f, 1053.85f))
                .then(HitStep.damageTaken(1f, 1053.85f))
                .resolvedTo(new Damage(1054, DamageType.PHYSICAL, true));
        MobEvent.Hit hit = new MobEvent.Hit(new MobEvent.HitCause("Sniper@7,4", Optional.empty(), true), steps,
                new MobEvent.HitOutcome(250, Damage.physical(804), 40_000, 39_196));

        assertThat(line(hit)).isEqualTo(PREFIX + "HIT from=Sniper@7,4 physical in=10 mark=spent crit=x1.50->15"
                + " armor=40(pen->30)->11.54 plating=2(pen->1)->10.54 taken=x1.00->10.54 resolved=10.54 shield=-2.5"
                + " landed=8.04 hp=400->391.96/500");
    }

    @Test
    void periodicDamageNamesItselfAndItsEffectAndUsesTheMagicStatsForMagic() {
        HitSteps steps = HitSteps.arriving(Damage.magic(100), Delivery.PERIODIC)
                .then(HitStep.mitigation(0f, 0f, 100f))
                .then(HitStep.flatReduction(0f, 0f, 100f))
                .then(HitStep.damageTaken(1f, 100f))
                .then(HitStep.periodicTaken(1.5f, 150f))
                .resolvedTo(Damage.magic(150));
        MobEvent.Hit hit = new MobEvent.Hit(new MobEvent.HitCause("Cinder@3,4", Optional.of(EffectKind.BURN), false), steps,
                new MobEvent.HitOutcome(0, Damage.magic(150), 40_000, 39_850));

        assertThat(line(hit)).isEqualTo(PREFIX + "PERIODIC from=Cinder@3,4 effect=" + EffectKind.BURN.title()
                + " magic in=1 magic_resist=0->1 insulation=0->1 taken=x1.00->1 periodic_taken=x1.50->1.5 resolved=1.5"
                + " landed=1.5 hp=400->398.5/500");
    }

    @Test
    void aDodgeNamesTheAttackerAndWhatArrived() {
        assertThat(line(new MobEvent.Dodged("Sniper@7,4", Damage.physical(1000))))
                .isEqualTo(PREFIX + "DODGE from=Sniper@7,4 physical in=10");
    }

    @Test
    void aKillNamesWhoAndTheOverkill() {
        assertThat(line(new MobEvent.Killed("dev:kill", 500))).isEqualTo(PREFIX + "KILLED by=dev:kill overkill=5");
    }

    @Test
    void anEffectArrivingNamesWhereFromAndHowLong() {
        assertThat(line(new MobEvent.EffectRequested(EffectKind.FREEZE, "Pulse@1,1", 60)))
                .isEqualTo(PREFIX + "EFFECT " + EffectKind.FREEZE.title() + " requested from=Pulse@1,1 length=60t");
    }

    @Test
    void aShortenedEffectShowsBothFactorsAndWhatIsLeft() {
        assertThat(line(new MobEvent.EffectShortened(EffectKind.FREEZE, 0.5f, 0.25f, 8)))
                .isEqualTo(PREFIX + "EFFECT " + EffectKind.FREEZE.title() + " shortened resist=x0.50 dr=x0.25 length=8t");
    }

    @Test
    void aBlockedEffectSaysWhy() {
        assertThat(line(new MobEvent.EffectBlocked(EffectKind.FREEZE, "would last under a tick")))
                .isEqualTo(PREFIX + "EFFECT " + EffectKind.FREEZE.title() + " blocked: would last under a tick");
    }

    @Test
    void anInvertedEffectSaysWhatItBecame() {
        assertThat(line(new MobEvent.EffectInverted(EffectKind.SHIELD, "struck as 50 magic")))
                .isEqualTo(PREFIX + "EFFECT " + EffectKind.SHIELD.title() + " inverted: struck as 50 magic");
    }

    @Test
    void eachEffectChangeSaysWhatChangedAndUnderWhatCause() {
        EffectState vulnerableOne = new EffectState(EffectKind.VULNERABLE, 1, OptionalInt.of(5), OptionalDouble.empty());
        EffectState vulnerableTwo = new EffectState(EffectKind.VULNERABLE, 2, OptionalInt.of(10), OptionalDouble.empty());
        EffectState shieldFull = new EffectState(EffectKind.SHIELD, 0, OptionalInt.of(40), OptionalDouble.of(10));
        EffectState shieldHalf = new EffectState(EffectKind.SHIELD, 0, OptionalInt.of(40), OptionalDouble.of(5));
        String vulnerable = "EFFECT " + EffectKind.VULNERABLE.title() + " ";
        String shield = "EFFECT " + EffectKind.SHIELD.title() + " ";

        List<String> lines = List.of(
                line(new MobEvent.EffectChanged(MobEvent.Change.GAINED, vulnerableOne, vulnerableOne, "applied:x")),
                line(new MobEvent.EffectChanged(MobEvent.Change.REFRESHED, vulnerableOne, vulnerableTwo, "applied:x")),
                line(new MobEvent.EffectChanged(MobEvent.Change.STACKS, vulnerableOne, vulnerableTwo, "applied:x")),
                line(new MobEvent.EffectChanged(MobEvent.Change.TOPPED_UP, shieldHalf, shieldFull, "applied:x")),
                line(new MobEvent.EffectChanged(MobEvent.Change.WEAKENED, shieldFull, shieldHalf, "shield-broken")),
                line(new MobEvent.EffectChanged(MobEvent.Change.ENDED, shieldFull, shieldFull, "expired")),
                line(new MobEvent.EffectChanged(MobEvent.Change.UNCHANGED, shieldFull, shieldFull, "no-change")));

        assertThat(lines).containsExactly(
                PREFIX + vulnerable + "gained stacks=1 left=5t cause=applied:x",
                PREFIX + vulnerable + "refreshed left=5t->10t cause=applied:x",
                PREFIX + vulnerable + "stacks=1->2 left=10t cause=applied:x",
                PREFIX + shield + "topped-up pool=5->10 cause=applied:x",
                PREFIX + shield + "weakened pool=10->5 cause=shield-broken",
                PREFIX + shield + "ended cause=expired",
                PREFIX + shield + "unchanged left=40t pool=10 cause=no-change");
    }

    @Test
    void aHealthChangeShowsItsCauseAndHealthEitherSide() {
        assertThat(line(new MobEvent.HealthChanged("regen", 30_000, 30_050)))
                .isEqualTo(PREFIX + "HEAL cause=regen hp=300->300.5/500");
    }

    @Test
    void aCastOnOthersShowsItsRadiusAndReachAndOneOnItselfSaysSo() {
        assertThat(line(new MobEvent.Cast(EffectKind.HEAL, 2f, 3)))
                .isEqualTo(PREFIX + "CAST " + EffectKind.HEAL.title() + " radius=2 targets=3");
        assertThat(line(new MobEvent.Cast(EffectKind.HEAL, 0f, 1)))
                .isEqualTo(PREFIX + "CAST " + EffectKind.HEAL.title() + " on=self");
    }

    @Test
    void aZoneLeftBehindShowsItsKindAndRadius() {
        assertThat(line(new MobEvent.ZoneLeft(ZoneKind.HEALING_GROUND, 1.5f)))
                .isEqualTo(PREFIX + "CAST healing-ground zone radius=1.5");
    }

    @Test
    void aSpawnListsTheNewIdsAndAHatchNamesItsHatchling() {
        assertThat(line(new MobEvent.Spawned(List.of(31L, 32L), false))).isEqualTo(PREFIX + "SPAWN #31 #32");
        assertThat(line(new MobEvent.Spawned(List.of(31L), true))).isEqualTo(PREFIX + "HATCH into=#31");
    }

    @Test
    void anEndShowsHowLongItLivedWhatLandedBySourceAndWhatItEndedUnder() {
        Map<String, Integer> landed = new LinkedHashMap<>();
        landed.put("Sniper@7,4 physical", 380);
        landed.put("- magic", 120);
        MobTotals totals = new MobTotals(landed, 25, 2, 50);

        String line = line(new MobEvent.Ended("killed", 812, totals, List.of()));

        assertThat(line).isEqualTo(PREFIX + "END killed lived=812t damage=[Sniper@7,4 physical=3.8, - magic=1.2]"
                + " absorbed=0.25 dodges=2 overkill=0.5 effects=[]");
    }

    private static String line(MobEvent event) {
        return MobEventText.line(34, LABEL, event);
    }
}
