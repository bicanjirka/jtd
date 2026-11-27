package td.tower;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.tower.splash.ArcSpec;
import td.tower.splash.Curse;
import td.tower.splash.HexSpec;
import td.tower.splash.SoulbrandMechanic;
import td.tower.splash.StaticMechanic;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A visible enemy is one the tower may aim at; a ghost is one only what lands reaches. A tower that has to cast is
 * buffed to deal so much that every stone it lands fills its bars.
 */
class SplashTowerTest {

    private static final int S = BoardFixtures.SCALE;
    /** Ticks to wait for something that must happen, so a test that never gets there fails fast. */
    private static final int PATIENCE = 200;

    private final GameWorld context = WorldFixtures.newWorldOnBoard(S, 20, 20);
    private int now;

    private SplashTower tower(String... nodes) {
        SplashTower tower = new SplashTower(this.context, 3, 3);
        if (nodes.length > 0) {
            UpgradePaths.buy(tower, this.context, nodes);
        }
        return tower;
    }

    /** A tower whose every landed stone earns more than any ability costs. */
    private SplashTower casting(String... nodes) {
        return TowerFixtures.buffed(this.tower(nodes), this.context, TowerModifiers.damageTimes(1000f));
    }

    /** {@code tower} with the XP that opens its tier II, which it holds free. */
    private static SplashTower atTierTwo(SplashTower tower) {
        tower.earnXp(tower.upgradeTree().tiers().get(2).xp());
        return tower;
    }

    private void enemies(FakeEnemyMob... enemies) {
        this.context.enemies().setEnemies(enemies);
    }

    private void run(SplashTower tower, int ticks) {
        for (int i = 0; i < ticks; i++) {
            tower.doTick(this.now++);
        }
    }

    private void runUntil(SplashTower tower, BooleanSupplier done) {
        for (int i = 0; i < PATIENCE && !done.getAsBoolean(); i++) {
            tower.doTick(this.now++);
        }
    }

    /** Ticks until its first stone has landed, and no further. */
    private void landFirstStone(SplashTower tower) {
        this.run(tower, 1);
        this.runUntil(tower, () -> tower.stonesInFlight().isEmpty());
    }

    private static FakeEnemyMob near(SplashTower tower, float dxCells, float dyCells) {
        return FakeEnemyMob.at(tower.getX() + dxCells * S, tower.getY() + dyCells * S);
    }

    private static FakeEnemyMob ghostBeside(FakeEnemyMob enemy, float cells) {
        return FakeEnemyMob.ghostAt(enemy.getX() + cells * S, enemy.getY());
    }

    /**
     * A heavy enemy beside a tower of its own world that owns {@code spell}, its damage {@code damageTimes} over, once
     * the spell's first magic hit or poison reached it.
     */
    private static FakeEnemyMob struckBySpell(float damageTimes, String spell) {
        GameWorld world = WorldFixtures.newWorldOnBoard(S, 20, 20);
        SplashTower tower = new SplashTower(world, 3, 3);
        UpgradePaths.buy(tower, world, spell);
        TowerFixtures.buffed(tower, world, TowerModifiers.damageTimes(damageTimes));
        FakeEnemyMob heavy = near(tower, 1, 0).withMaxHealthPoints(1_000_000);
        world.enemies().setEnemies(new FakeEnemyMob[] {heavy});
        for (int t = 0; t < PATIENCE && magicHits(heavy).isEmpty() && !heavy.hasEffect(EffectKind.POISON); t++) {
            tower.doTick(t);
        }
        return heavy;
    }

    private static float poisonFuel(FakeEnemyMob enemy) {
        return (float) enemy.appliedEffects().stream()
                .filter(effect -> effect.kind() == EffectKind.POISON)
                .mapToDouble(Effect::fuelLevel)
                .sum();
    }

    private static List<Integer> remainingTicks(FakeEnemyMob enemy, EffectKind kind) {
        return enemy.activeEffects().stream().filter(effect -> effect.kind() == kind).map(Effect::remainingTicks)
                .toList();
    }

    private static List<Damage> magicHits(FakeEnemyMob enemy) {
        return enemy.hits().stream().filter(hit -> hit.type() == DamageType.MAGIC).toList();
    }

    @Test
    void aStoneLandsWhereItsTargetStoodWhenItWasThrown() {
        SplashTower tower = this.tower();
        FakeEnemyMob target = near(tower, 1, 0);
        FakeEnemyMob left = FakeEnemyMob.ghostAt(target.getX(), target.getY());
        this.enemies(target, left);

        this.run(tower, 1);
        target.moveTo(target.getX(), target.getY() + 3 * SplashTower.EDGE_CELLS * S);
        this.runUntil(tower, () -> tower.stonesInFlight().isEmpty());

        assertThat(target.hits()).isEmpty();
        assertThat(left.hits()).hasSize(1);
    }

    @Test
    void nothingLandsWhileTheStoneIsInTheAir() {
        SplashTower tower = this.tower();
        FakeEnemyMob target = near(tower, 2, 0);
        this.enemies(target);

        this.run(tower, 1);

        assertThat(tower.stonesInFlight()).hasSize(1);
        assertThat(target.hits()).isEmpty();
    }

    @Test
    void theCoreTakesFullDamageTheEdgeItsShareAndBeyondTheEdgeNothing() {
        SplashTower tower = this.tower();
        int full = tower.damageCurrent();
        FakeEnemyMob target = near(tower, 1, 0);
        FakeEnemyMob inEdge = ghostBeside(target, (SplashTower.CORE_CELLS + SplashTower.EDGE_CELLS) / 2);
        FakeEnemyMob beyond = ghostBeside(target, SplashTower.EDGE_CELLS * 1.2f);
        this.enemies(target, inEdge, beyond);

        this.landFirstStone(tower);

        assertThat(target.onlyHitAmount()).isEqualTo(full);
        assertThat(inEdge.onlyHitAmount()).isEqualTo(Math.round(full * SplashTower.EDGE_SHARE));
        assertThat(beyond.hits()).isEmpty();
    }

    @Test
    void aStoneDealsWhatTheTowerHadWhenItWasThrown() {
        SplashTower tower = this.tower();
        int full = tower.damageCurrent();
        FakeEnemyMob target = near(tower, 2, 0);
        this.enemies(target);

        this.run(tower, 1);
        TowerFixtures.buffed(tower, this.context, TowerModifiers.damageTimes(2f));
        this.runUntil(tower, () -> tower.stonesInFlight().isEmpty());

        assertThat(target.onlyHitAmount()).isEqualTo(full);
    }

    @Test
    void staticDischargesIntoAnEnemyItDamagedOnceThatEnemyIsBadlyHurt() {
        SplashTower tower = this.tower("Stormcaller");
        FakeEnemyMob hurt = near(tower, 1, 0).atHealthFraction(StaticMechanic.DISCHARGE_AT / 2);
        FakeEnemyMob whole = ghostBeside(hurt, 0f);
        this.enemies(hurt, whole);

        this.landFirstStone(tower);
        this.run(tower, 1);

        assertThat(magicHits(hurt)).containsExactly(Damage.magic(DamageUnits.ofPoints(StaticMechanic.DISCHARGE_POINTS)));
        assertThat(magicHits(whole)).isEmpty();
    }

    @Test
    void theSparkADischargeSendsHomeChargesTheNextStoneWithAnArc() {
        SplashTower tower = this.tower("Arc");
        FakeEnemyMob heavy = near(tower, 1, 0).atHealthFraction(StaticMechanic.DISCHARGE_AT / 2)
                .withMaxHealthPoints(1_000_000);
        this.enemies(heavy);

        this.landFirstStone(tower);
        this.runUntil(tower, () -> !tower.stonesInFlight().isEmpty());

        assertThat(tower.stonesInFlight().getFirst().charged()).isTrue();
    }

    @Test
    void anArcJumpsFromWhereItsStoneLandsToTheNearestEnemiesAsMagic() {
        SplashTower tower = this.casting("Arc");
        FakeEnemyMob target = near(tower, 1, 0);
        FakeEnemyMob next = ghostBeside(target, (SplashTower.EDGE_CELLS + ArcSpec.REACH_CELLS) / 2);
        FakeEnemyMob tooFar = ghostBeside(next, ArcSpec.REACH_CELLS * 1.2f);
        this.enemies(target, next, tooFar);

        this.runUntil(tower, () -> !magicHits(next).isEmpty());
        this.run(tower, 10);

        assertThat(magicHits(target)).hasSize(1).isEqualTo(magicHits(next));
        assertThat(tooFar.hits()).isEmpty();
    }

    @Test
    void anArcJumpTagsNothingSoItsHitEarnsNoSpark() {
        SplashTower tower = this.casting("Arc");
        FakeEnemyMob target = near(tower, 1, 0);
        FakeEnemyMob next = ghostBeside(target, (SplashTower.EDGE_CELLS + ArcSpec.REACH_CELLS) / 2)
                .atHealthFraction(StaticMechanic.DISCHARGE_AT / 2).withMaxHealthPoints(1_000_000);
        this.enemies(target, next);

        this.runUntil(tower, () -> !magicHits(next).isEmpty());
        this.run(tower, 10);

        assertThat(magicHits(next)).hasSize(1);
        assertThat(tower.wispsInFlight()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Arc", "Shockwave", "Thunderstrike"})
    void aSparkSpellDealsTheSameWhateverTheTowersDamage(String spell) {
        FakeEnemyMob weaker = struckBySpell(1000f, spell);
        FakeEnemyMob stronger = struckBySpell(2000f, spell);

        assertThat(magicHits(stronger)).isNotEmpty().first().isEqualTo(magicHits(weaker).getFirst());
    }

    @Test
    void blightsPoisonCarriesTheSameFuelWhateverTheTowersDamage() {
        FakeEnemyMob weaker = struckBySpell(1000f, "Hex of Blight");
        FakeEnemyMob stronger = struckBySpell(2000f, "Hex of Blight");

        assertThat(poisonFuel(stronger)).isPositive().isEqualTo(poisonFuel(weaker));
    }

    @Test
    void tierTwoMakesEachJumpOfASpellsArcLeaveItsEnemyVulnerable() {
        SplashTower tower = atTierTwo(this.casting("Arc"));
        FakeEnemyMob target = near(tower, 1, 0);
        FakeEnemyMob next = ghostBeside(target, (SplashTower.EDGE_CELLS + ArcSpec.REACH_CELLS) / 2);
        this.enemies(target, next);

        this.runUntil(tower, () -> !magicHits(next).isEmpty());
        this.run(tower, 10);

        assertThat(next.hasEffect(EffectKind.VULNERABLE)).isTrue();
    }

    @Test
    void wildArcStrikesALoneEnemyWithEveryJump() {
        SplashTower tower = this.casting("Wild Arc");
        FakeEnemyMob lone = near(tower, 1, 0);
        this.enemies(lone);

        this.runUntil(tower, () -> !magicHits(lone).isEmpty());
        this.run(tower, 10);

        assertThat(magicHits(lone)).hasSize(ArcSpec.JUMPS);
    }

    @Test
    void shockwaveStrikesEveryEnemyInRangeAlikeAndNothingBeyond() {
        SplashTower tower = this.casting("Shockwave");
        FakeEnemyMob first = near(tower, 1, 0);
        FakeEnemyMob second = near(tower, -1, 0);
        FakeEnemyMob outOfRange = near(tower, 0, SplashTower.RANGE * 2);
        this.enemies(first, second, outOfRange);

        this.runUntil(tower, () -> !magicHits(first).isEmpty());

        assertThat(magicHits(first)).hasSize(1).isEqualTo(magicHits(second));
        assertThat(outOfRange.hits()).isEmpty();
    }

    @Test
    void thunderstrikeStrikesOnlyTheHealthiestEnemyInRange() {
        SplashTower tower = this.casting("Thunderstrike");
        FakeEnemyMob weak = near(tower, 1, 0).withHealth(100);
        FakeEnemyMob strong = near(tower, -1, 0).withHealth(900);
        this.enemies(weak, strong);

        this.runUntil(tower, () -> !magicHits(strong).isEmpty());

        assertThat(magicHits(strong)).hasSize(1);
        assertThat(magicHits(weak)).isEmpty();
    }

    @Test
    void thunderstrikeDazesTheEnemyItStrikes() {
        SplashTower tower = this.casting("Thunderstrike");
        FakeEnemyMob strong = near(tower, 1, 0).withHealth(900);
        this.enemies(strong);

        this.runUntil(tower, () -> !magicHits(strong).isEmpty());

        assertThat(strong.hasEffect(EffectKind.DAZED)).isTrue();
    }

    @Test
    void aKilledEnemyTheHexerDamagedBurstsOnTheEnemiesAroundIt() {
        SplashTower tower = this.tower("Hexer");
        FakeEnemyMob victim = near(tower, 1, 0);
        victim.dieOnAnyHit();
        FakeEnemyMob close = ghostBeside(victim, SoulbrandMechanic.BURST_CELLS / 2);
        FakeEnemyMob apart = ghostBeside(victim, (SoulbrandMechanic.BURST_CELLS + SplashTower.EDGE_CELLS) / 2);
        this.enemies(victim, close, apart);

        this.landFirstStone(tower);
        this.run(tower, 1);

        assertThat(magicHits(close)).containsExactly(Damage.magic(DamageUnits.ofPoints(SoulbrandMechanic.BURST_POINTS)));
        assertThat(magicHits(apart)).isEmpty();
    }

    @Test
    void hexOfDoomCursesTheEnemyClosestToLeakingOnlyOnceItsCurseArrives() {
        SplashTower tower = this.casting("Hex of Doom");
        FakeEnemyMob behind = near(tower, 1, 0).withProgression(100);
        FakeEnemyMob ahead = near(tower, -1, 0).withProgression(900);
        this.enemies(behind, ahead);

        this.runUntil(tower, () -> !tower.cursesInFlight().isEmpty());
        boolean cursedAtCast = ahead.hasEffect(EffectKind.DOOM);
        this.runUntil(tower, () -> ahead.hasEffect(EffectKind.DOOM));

        assertThat(cursedAtCast).isFalse();
        assertThat(ahead.hasEffect(EffectKind.DOOM)).isTrue();
        assertThat(behind.hasEffect(EffectKind.DOOM)).isFalse();
    }

    @Test
    void hexOfBlightCursesTheEnemyThatWouldLeaveSoonest() {
        SplashTower tower = this.casting("Hex of Blight");
        FakeEnemyMob far = near(tower, 1, 0).withDistanceToExit(900);
        FakeEnemyMob close = near(tower, -1, 0).withDistanceToExit(100);
        this.enemies(far, close);

        this.runUntil(tower, () -> close.hasEffect(EffectKind.BLIGHT));

        assertThat(close.hasEffect(EffectKind.BLIGHT)).isTrue();
        assertThat(far.hasEffect(EffectKind.BLIGHT)).isFalse();
    }

    @Test
    void whenDoomEndsItsEnemyTakesItsShareOfWhatItTookMeanwhileAsAShadowHit() {
        SplashTower tower = this.casting("Hex of Doom");
        FakeEnemyMob enemy = near(tower, 1, 0);
        this.enemies(enemy);
        this.runUntil(tower, () -> enemy.hasEffect(EffectKind.DOOM));
        enemy.doDamage(Damage.physical(10_000), AttackProfile.none());
        int hitsBeforeEnd = enemy.hits().size();

        enemy.expire(EffectKind.DOOM);
        this.run(tower, 1);

        assertThat(enemy.hits().get(hitsBeforeEnd)).isEqualTo(Damage.magic(Math.round(10_000 * HexSpec.DOOM_SHARE)));
        assertThat(enemy.attackers().get(hitsBeforeEnd).piercesShields(false)).isTrue();
        assertThat(enemy.attackers().get(hitsBeforeEnd).delivery().name()).isEqualTo("HIT");
    }

    @Test
    void twinHexCursesTwoEnemiesWithOneCast() {
        SplashTower tower = this.casting("Hex of Doom", "Twin Hex");
        FakeEnemyMob first = near(tower, 1, 0).withHealth(100);
        FakeEnemyMob second = near(tower, -1, 0).withHealth(900);
        this.enemies(first, second);

        this.runUntil(tower, () -> !tower.cursesInFlight().isEmpty());

        assertThat(tower.cursesInFlight()).extracting(Curse::target).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void tierTwoMakesAHexLeaveItsEnemyVulnerableForAsLongAsTheHexLasts() {
        SplashTower tower = atTierTwo(this.casting("Hex of Doom"));
        FakeEnemyMob enemy = near(tower, 1, 0);
        this.enemies(enemy);

        this.runUntil(tower, () -> enemy.hasEffect(EffectKind.DOOM));

        assertThat(remainingTicks(enemy, EffectKind.VULNERABLE)).isNotEmpty()
                .isEqualTo(remainingTicks(enemy, EffectKind.DOOM));
    }

    @ParameterizedTest
    @CsvSource({"Shockwave", "Thunderstrike"})
    void tierTwoMakesASpellsLightningLeaveTheEnemyItStrikesVulnerable(String spell) {
        SplashTower tower = atTierTwo(this.casting(spell));
        FakeEnemyMob enemy = near(tower, 1, 0).withHealth(900);
        this.enemies(enemy);

        this.runUntil(tower, () -> !magicHits(enemy).isEmpty());

        assertThat(enemy.hasEffect(EffectKind.VULNERABLE)).isTrue();
    }

    @Test
    void aStoneIsNoSpellAndLeavesNothingVulnerable() {
        SplashTower tower = atTierTwo(this.tower());
        FakeEnemyMob enemy = near(tower, 1, 0);
        this.enemies(enemy);

        this.landFirstStone(tower);

        assertThat(enemy.hits()).isNotEmpty();
        assertThat(enemy.hasEffect(EffectKind.VULNERABLE)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"Hex of Doom, DOOM", "Hex of Blight, BLIGHT", "Hex of Backlash, BACKLASH"})
    void chokingHexesSilenceAHexedEnemyForAsLongAsItsHexLasts(String hexNode, EffectKind hex) {
        SplashTower tower = this.casting(hexNode, "Choking Hexes");
        FakeEnemyMob enemy = near(tower, 1, 0).thatRegenerates();
        this.enemies(enemy);

        this.runUntil(tower, () -> enemy.hasEffect(hex));

        assertThat(remainingTicks(enemy, EffectKind.SILENCED)).isNotEmpty().isEqualTo(remainingTicks(enemy, hex));
    }

    @Test
    void blightCloudAlsoCursesTheEnemiesAroundItsTarget() {
        SplashTower tower = this.casting("Hex of Blight", "Blight Cloud");
        FakeEnemyMob target = near(tower, 1, 0);
        FakeEnemyMob close = ghostBeside(target, HexSpec.CLOUD_CELLS / 2);
        FakeEnemyMob apart = ghostBeside(target, HexSpec.CLOUD_CELLS * 1.5f);
        this.enemies(target, close, apart);

        this.runUntil(tower, () -> target.hasEffect(EffectKind.BLIGHT));
        this.runUntil(tower, () -> tower.cursesInFlight().isEmpty());

        assertThat(close.hasEffect(EffectKind.BLIGHT)).isTrue();
        assertThat(apart.hasEffect(EffectKind.BLIGHT)).isFalse();
    }

    @Test
    void blightCloudForksItsCurseInFlightBeforeItReachesItsTarget() {
        SplashTower tower = this.casting("Hex of Blight", "Blight Cloud");
        FakeEnemyMob target = near(tower, 2, 0);
        FakeEnemyMob close = ghostBeside(target, HexSpec.CLOUD_CELLS / 2);
        this.enemies(target, close);

        this.runUntil(tower, () -> tower.cursesInFlight().stream().anyMatch(curse -> curse.target() == close));

        assertThat(tower.cursesInFlight()).anyMatch(curse -> curse.target() == close);
        assertThat(target.hasEffect(EffectKind.BLIGHT)).isFalse();
    }

    @Test
    void blightCloudCursesOnlyTheFewestEnemiesAroundItsTargetThatWouldLeaveSoonest() {
        SplashTower tower = this.casting("Hex of Blight", "Blight Cloud");
        FakeEnemyMob target = near(tower, 1, 0);
        List<FakeEnemyMob> crowd = new ArrayList<>();
        for (int i = 0; i < HexSpec.CLOUD_TARGETS + 2; i++) {
            crowd.add(ghostBeside(target, HexSpec.CLOUD_CELLS / 2).withDistanceToExit(100 + 10 * i));
        }
        this.enemies(Stream.concat(Stream.of(target), crowd.stream()).toArray(FakeEnemyMob[]::new));

        this.runUntil(tower, () -> target.hasEffect(EffectKind.BLIGHT));
        this.runUntil(tower, () -> tower.cursesInFlight().isEmpty());

        assertThat(crowd.stream().filter(enemy -> enemy.hasEffect(EffectKind.BLIGHT)).toList())
                .isEqualTo(crowd.subList(0, HexSpec.CLOUD_TARGETS));
    }

    @Test
    void contagionSplitsTheBlightOfAnEnemyThatDiedBetweenTheTwoNearestEnemiesWithoutOne() {
        SplashTower tower = this.casting("Hex of Blight", "Contagion");
        FakeEnemyMob carrier = near(tower, 1, 0);
        FakeEnemyMob nearest = ghostBeside(carrier, HexSpec.SPREAD_CELLS / 3);
        FakeEnemyMob further = ghostBeside(carrier, HexSpec.SPREAD_CELLS * 2 / 3);
        FakeEnemyMob farthest = ghostBeside(carrier, HexSpec.SPREAD_CELLS * 0.9f);
        this.enemies(carrier, nearest, further, farthest);
        this.runUntil(tower, () -> carrier.hasEffect(EffectKind.BLIGHT));

        carrier.dieOnAnyHit();
        carrier.doDamage(Damage.physical(1), AttackProfile.none());
        this.runUntil(tower, () -> nearest.hasEffect(EffectKind.BLIGHT) && further.hasEffect(EffectKind.BLIGHT));

        assertThat(nearest.hasEffect(EffectKind.BLIGHT)).isTrue();
        assertThat(further.hasEffect(EffectKind.BLIGHT)).isTrue();
        assertThat(farthest.hasEffect(EffectKind.BLIGHT)).isFalse();
    }

    @Test
    void aDoomWhoseEnemyDiedJumpsToTheNearestEnemyWithoutOneCarryingWhatItRecorded() {
        SplashTower tower = this.casting("Hex of Doom");
        FakeEnemyMob carrier = near(tower, 1, 0);
        FakeEnemyMob nearest = ghostBeside(carrier, HexSpec.SPREAD_CELLS / 3);
        FakeEnemyMob further = ghostBeside(carrier, HexSpec.SPREAD_CELLS * 2 / 3);
        this.enemies(carrier, nearest, further);
        this.runUntil(tower, () -> carrier.hasEffect(EffectKind.DOOM));
        carrier.doDamage(Damage.physical(10_000), AttackProfile.none());

        carrier.dieOnAnyHit();
        carrier.doDamage(Damage.physical(1), AttackProfile.none());
        this.runUntil(tower, () -> nearest.hasEffect(EffectKind.DOOM));
        int hitsBeforeEnd = nearest.hits().size();
        nearest.expire(EffectKind.DOOM);
        this.run(tower, 1);

        assertThat(further.hasEffect(EffectKind.DOOM)).isFalse();
        assertThat(nearest.hits().get(hitsBeforeEnd))
                .isEqualTo(Damage.magic(Math.round(10_000 * HexSpec.DOOM_SHARE)));
    }

    @Test
    void contagionSplitsADoomsRecordBetweenItsTwoHalves() {
        SplashTower tower = this.casting("Hex of Doom", "Contagion");
        FakeEnemyMob carrier = near(tower, 1, 0);
        FakeEnemyMob first = ghostBeside(carrier, HexSpec.SPREAD_CELLS / 3);
        FakeEnemyMob second = ghostBeside(carrier, HexSpec.SPREAD_CELLS * 2 / 3);
        this.enemies(carrier, first, second);
        this.runUntil(tower, () -> carrier.hasEffect(EffectKind.DOOM));
        carrier.doDamage(Damage.physical(10_000), AttackProfile.none());

        carrier.dieOnAnyHit();
        carrier.doDamage(Damage.physical(1), AttackProfile.none());
        this.runUntil(tower, () -> first.hasEffect(EffectKind.DOOM) && second.hasEffect(EffectKind.DOOM));
        int firstHits = first.hits().size();
        int secondHits = second.hits().size();
        first.expire(EffectKind.DOOM);
        second.expire(EffectKind.DOOM);
        this.run(tower, 1);

        assertThat(first.hits().get(firstHits)).isEqualTo(Damage.magic(Math.round(5_000 * HexSpec.DOOM_SHARE)));
        assertThat(second.hits().get(secondHits)).isEqualTo(Damage.magic(Math.round(5_000 * HexSpec.DOOM_SHARE)));
    }

    @Test
    void theForkNamesTheTower() {
        SplashTower plain = this.tower();
        SplashTower stormcaller = this.tower("Stormcaller");
        SplashTower hexer = this.tower("Hexer");

        assertThat(List.of(plain.inspect().name(), stormcaller.inspect().name(), hexer.inspect().name()))
                .doesNotHaveDuplicates();
    }

    @Test
    void everyNodeBelongsToTheForkSideItsPathCrosses() {
        SplashTower tower = this.tower();

        assertThat(SplashTower.forkOf(UpgradePaths.named(tower, "Arc"))).isEqualTo(SplashTower.Fork.STORMCALLER);
        assertThat(SplashTower.forkOf(UpgradePaths.named(tower, "Contagion"))).isEqualTo(SplashTower.Fork.HEXER);
        assertThat(SplashTower.forkOf(UpgradePaths.named(tower, "Sharp Stones"))).isEqualTo(SplashTower.Fork.NONE);
    }
}
