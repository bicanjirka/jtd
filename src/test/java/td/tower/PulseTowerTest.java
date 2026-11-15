package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.Delivery;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.tower.pulse.PulseSpec;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PulseTowerTest {

    private static final int SECOND = Math.round(TickRate.TICKS_PER_SECOND);

    private final GameWorld board = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

    private PulseTower pulseWith(String... nodes) {
        PulseTower tower = new PulseTower(this.board, 5, 5);
        UpgradePaths.buy(tower, this.board, nodes);
        return tower;
    }

    private void spawn(EnemyMob... enemies) {
        this.board.enemies().setEnemies(enemies);
    }

    /** Ticks every tower from {@code from} to {@code to}, then the effects of every enemy given, as the engine orders them. */
    private static void run(int from, int to, List<PulseTower> towers, FakeEnemyMob... enemies) {
        for (int t = from; t <= to; t++) {
            for (PulseTower tower : towers) {
                tower.doTick(t);
            }
            for (FakeEnemyMob enemy : enemies) {
                enemy.tickEffects();
            }
        }
    }

    private static List<Damage> hitsOf(FakeEnemyMob enemy, Delivery delivery) {
        List<Damage> hits = new ArrayList<>();
        List<AttackProfile> attackers = enemy.attackers();
        for (int i = 0; i < attackers.size(); i++) {
            if (attackers.get(i).delivery() == delivery) {
                hits.add(enemy.hits().get(i));
            }
        }
        return hits;
    }

    private static float dragOn(FakeEnemyMob enemy) {
        return enemy.activeEffects().stream().filter(effect -> effect.kind() == EffectKind.DRAGGED).findFirst()
                .map(Effect::slowStrength).orElse(0f);
    }

    private static long applied(FakeEnemyMob enemy, EffectKind kind) {
        return enemy.appliedEffects().stream().filter(effect -> effect.kind() == kind).count();
    }

    /** A cell's worth of board pixels times {@code cells}. */
    private static double cells(float cells) {
        return cells * BoardFixtures.SCALE;
    }

    @Test
    void theFieldHitsEveryEnemyInsideForMagicEveryTickHiddenOnesIncludedAndNoneOutside() {
        PulseTower tower = this.pulseWith();
        FakeEnemyMob visible = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob hidden = FakeEnemyMob.ghostAt(tower.getX(), tower.getY());
        FakeEnemyMob outside = FakeEnemyMob.at(tower.getX() + cells(3f), tower.getY());
        this.spawn(visible, hidden, outside);

        tower.doTick(1);
        tower.doTick(2);

        Damage tick = Damage.magic(Math.round(tower.damageCurrent() / TickRate.TICKS_PER_SECOND));
        assertThat(visible.hits()).containsExactly(tick, tick);
        assertThat(hidden.hits()).containsExactly(tick, tick);
        assertThat(outside.hits()).isEmpty();
    }

    @Test
    void anEnemyInsideBuildsAStackOfTollEachPaceUpToTheFieldsCap() {
        PulseTower tower = this.pulseWith();
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.spawn(enemy);
        int pace = Math.round(tower.stats().value(TowerStat.TOLL_PACE));

        run(1, pace, List.of(tower), enemy);
        int afterOnePace = enemy.effectStacks(EffectKind.TOLL);
        run(pace + 1, pace * (PulseSpec.TOLL_CAP + 3), List.of(tower), enemy);

        assertThat(afterOnePace).isEqualTo(1);
        assertThat(enemy.effectStacks(EffectKind.TOLL)).isEqualTo(PulseSpec.TOLL_CAP);
    }

    @Test
    void aQuickerTollPaceBuildsTheFirstStackSooner() {
        PulseTower plain = this.pulseWith();
        int pace = Math.round(plain.stats().value(TowerStat.TOLL_PACE));
        PulseTower quick = TowerFixtures.buffed(new PulseTower(this.board, 12, 12), this.board,
                TowerModifiers.tollPace(-pace / 2));
        FakeEnemyMob enemy = FakeEnemyMob.at(quick.getX(), quick.getY());
        this.spawn(enemy);

        run(1, pace - pace / 2, List.of(quick), enemy);

        assertThat(enemy.effectStacks(EffectKind.TOLL)).isEqualTo(1);
    }

    @Test
    void twoFieldsOverAnEnemyBuildOneTollNotTwo() {
        PulseTower first = this.pulseWith();
        PulseTower second = new PulseTower(this.board, 5, 5);
        FakeEnemyMob enemy = FakeEnemyMob.at(first.getX(), first.getY());
        this.spawn(enemy);
        int pace = Math.round(first.stats().value(TowerStat.TOLL_PACE));

        run(1, pace, List.of(first, second), enemy);

        assertThat(enemy.effectStacks(EffectKind.TOLL)).isEqualTo(1);
    }

    @Test
    void deepTollBuildsTollPastAPlainFieldsCap() {
        PulseTower tower = this.pulseWith("Deep Toll");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.spawn(enemy);
        int pace = Math.round(tower.stats().value(TowerStat.TOLL_PACE));

        run(1, pace * (PulseSpec.deepestToll() + 2), List.of(tower), enemy);

        assertThat(enemy.effectStacks(EffectKind.TOLL)).isEqualTo(PulseSpec.deepestToll());
    }

    @Test
    void theDragDeepensByItsShareForEveryStackOfToll() {
        PulseTower tower = this.pulseWith();
        FakeEnemyMob fresh = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob tolled = FakeEnemyMob.at(tower.getX(), tower.getY());
        tolled.applyEffect(Effect.toll(3, PulseSpec.TOLL_CAP));
        this.spawn(fresh, tolled);

        tower.doTick(1);

        assertThat(dragOn(tolled) - dragOn(fresh))
                .isCloseTo(3 * tower.stats().value(TowerStat.DRAG_PER_TOLL), within(0.0001f));
        assertThat(dragOn(fresh)).isPositive();
    }

    @Test
    void theDragStopsAtTheFieldsCapAndBurdenLetsItGoDeeper() {
        PulseTower plain = TowerFixtures.buffed(this.pulseWith(), this.board, TowerModifiers.dragPerToll(1f));
        PulseTower burden = TowerFixtures.buffed(new PulseTower(this.board, 12, 12), this.board,
                TowerModifiers.dragPerToll(1f));
        UpgradePaths.buy(burden, this.board, "Burden");
        FakeEnemyMob atPlain = FakeEnemyMob.at(plain.getX(), plain.getY());
        FakeEnemyMob atBurden = FakeEnemyMob.at(burden.getX(), burden.getY());
        atPlain.applyEffect(Effect.toll(PulseSpec.TOLL_CAP, PulseSpec.TOLL_CAP));
        atBurden.applyEffect(Effect.toll(PulseSpec.TOLL_CAP, PulseSpec.TOLL_CAP));
        this.spawn(atPlain, atBurden);

        plain.doTick(1);
        burden.doTick(1);

        assertThat(dragOn(atPlain)).isEqualTo(PulseSpec.DRAG_CAP);
        assertThat(dragOn(atBurden)).isGreaterThan(PulseSpec.DRAG_CAP).isLessThan(1f);
    }

    @Test
    void wardSilencesEveryEnemyInsideAndNoOther() {
        PulseTower tower = this.pulseWith("Ward");
        FakeEnemyMob inside = FakeEnemyMob.at(tower.getX(), tower.getY());
        FakeEnemyMob outside = FakeEnemyMob.at(tower.getX() + cells(3f), tower.getY());
        this.spawn(inside, outside);

        tower.doTick(1);

        assertThat(inside.hasEffect(EffectKind.SILENCED)).isTrue();
        assertThat(outside.hasEffect(EffectKind.SILENCED)).isFalse();
    }

    @Test
    void gravityDazesAnEnemyOnceAVisitABeatAfterItWalksIn() {
        PulseTower tower = this.pulseWith("Gravity");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.spawn(enemy);

        run(1, 10, List.of(tower), enemy);
        long early = applied(enemy, EffectKind.DAZED);
        run(11, 100, List.of(tower), enemy);
        long oneVisit = applied(enemy, EffectKind.DAZED);
        enemy.moveTo(tower.getX() + cells(3f), tower.getY());
        tower.doTick(101);
        enemy.moveTo(tower.getX(), tower.getY());
        run(102, 200, List.of(tower), enemy);

        assertThat(early).isZero();
        assertThat(oneVisit).isEqualTo(1);
        assertThat(applied(enemy, EffectKind.DAZED)).isEqualTo(2);
    }

    @Test
    void theSundialTurnsOnceHittingEveryVisibleEnemyItPassesOnceWithAStackOfToll() {
        PulseTower tower = this.pulseWith("Sundial");
        List<FakeEnemyMob> around = IntStream.range(0, 4)
                .mapToObj(i -> FakeEnemyMob.at(tower.getX() + cells(2.5f) * Math.cos(i * Math.PI / 2),
                        tower.getY() + cells(2.5f) * Math.sin(i * Math.PI / 2)))
                .toList();
        FakeEnemyMob hidden = FakeEnemyMob.ghostAt(tower.getX() + cells(2f), tower.getY() + cells(1f));
        FakeEnemyMob far = FakeEnemyMob.at(tower.getX() + cells(6f), tower.getY());
        List<EnemyMob> all = new ArrayList<>(around);
        all.add(hidden);
        all.add(far);
        this.spawn(all.toArray(EnemyMob[]::new));

        run(1, 2 * SECOND, List.of(tower));

        for (FakeEnemyMob enemy : around) {
            assertThat(hitsOf(enemy, Delivery.HIT)).hasSize(1);
            assertThat(enemy.effectStacks(EffectKind.TOLL)).isEqualTo(1);
        }
        assertThat(hidden.hits()).isEmpty();
        assertThat(far.hits()).isEmpty();
    }

    @Test
    void theShadowTurnsAtOnceWhenAnEnemyComesInReachThenWaitsItsPeriod() {
        PulseTower tower = this.pulseWith("Sundial");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX() + cells(2.5f), tower.getY());
        this.spawn();
        run(1, 100, List.of(tower));
        this.spawn(enemy);

        run(101, 101 + 2 * SECOND, List.of(tower));
        boolean waiting = !tower.isCastingShadow();
        int firstTurn = hitsOf(enemy, Delivery.HIT).size();
        run(102 + 2 * SECOND, 101 + 4 * SECOND, List.of(tower));

        assertThat(firstTurn).isEqualTo(1);
        assertThat(waiting).isTrue();
        assertThat(hitsOf(enemy, Delivery.HIT)).hasSize(2);
    }

    @Test
    void umbraDoublesTheShadowsHitOnAnEnemyInTheField() {
        PulseTower tower = this.pulseWith("Sundial", "Umbra");
        FakeEnemyMob inside = FakeEnemyMob.at(tower.getX() + cells(0.5f), tower.getY());
        FakeEnemyMob outside = FakeEnemyMob.at(tower.getX() - cells(2.5f), tower.getY());
        this.spawn(inside, outside);

        run(1, SECOND, List.of(tower));

        assertThat(hitsOf(inside, Delivery.HIT).getFirst().amount())
                .isEqualTo(2 * hitsOf(outside, Delivery.HIT).getFirst().amount());
    }

    @Test
    void deepShadowsHitGoesPastShields() {
        PulseTower tower = this.pulseWith("Sundial", "Deep Shadow");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX() + cells(2.5f), tower.getY());
        this.spawn(enemy);

        run(1, SECOND, List.of(tower));

        assertThat(enemy.attackers()).filteredOn(attacker -> attacker.delivery() == Delivery.HIT)
                .singleElement().satisfies(attacker -> assertThat(attacker.piercesShields(false)).isTrue());
    }

    @Test
    void knellRingsOnceASecondWhileAnEnemyAtFullTollIsInsideHittingEveryoneInside() {
        PulseTower tower = this.pulseWith("Knell");
        FakeEnemyMob full = FakeEnemyMob.at(tower.getX(), tower.getY());
        full.applyEffect(Effect.toll(PulseSpec.TOLL_CAP, PulseSpec.TOLL_CAP));
        FakeEnemyMob fresh = FakeEnemyMob.at(tower.getX(), tower.getY());
        this.spawn(full, fresh);

        run(1, 2 * SECOND, List.of(tower));

        assertThat(hitsOf(fresh, Delivery.HIT)).hasSize(2);
        assertThat(hitsOf(full, Delivery.HIT)).hasSize(2);
    }

    @Test
    void knellStaysSilentWhileNobodyInsideHasFullToll() {
        PulseTower tower = this.pulseWith("Knell");
        FakeEnemyMob enemy = FakeEnemyMob.at(tower.getX(), tower.getY());
        enemy.applyEffect(Effect.toll(PulseSpec.TOLL_CAP - 1, PulseSpec.TOLL_CAP));
        this.spawn(enemy);

        run(1, 2 * SECOND, List.of(tower));

        assertThat(hitsOf(enemy, Delivery.HIT)).isEmpty();
    }
}
