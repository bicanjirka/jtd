package td.ui;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.DamageSink;
import td.effect.Effect;
import td.enemy.Affix;
import td.enemy.AffixContext;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionAura;
import td.ui.render.AffixPipsDraw;
import td.ui.render.EnemyBarLook;
import td.ui.render.EnemyBarsDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyEggDraw;
import td.ui.render.EnemyEmissionDraw;
import td.ui.render.EnemyOverlayDraw;
import td.ui.render.EnemyRingDraw;
import td.ui.render.Palette;
import td.ui.render.RankBadgeDraw;
import td.util.GameWorld;
import td.wave.Path;
import td.wave.Vec2;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EnemyFrameBuilderTest {

    private static GameWorld contextWithStraightPath() {
        GameWorld context = WorldFixtures.newWorldOnBoard(1, 1001, 1001);
        context.setPath(new Path(List.of(new Vec2(5, 5), new Vec2(105, 5))));
        return context;
    }

    private static EnemyBodyDraw bodyDrawAt(EnemyMob enemy, int gameTime, double alpha) {
        EnemyFrameBuilder builder = new EnemyFrameBuilder(gameTime, alpha);
        enemy.accept(builder);
        return (EnemyBodyDraw) builder.build().getFirst();
    }

    private static List<EnemyOverlayDraw> overlaysOf(EnemyMob enemy, int gameTime) {
        EnemyFrameBuilder builder = new EnemyFrameBuilder(gameTime, 0.0);
        enemy.accept(builder);
        return builder.buildOverlays();
    }

    private static RankBadgeDraw badgeOf(EnemyMob enemy) {
        return badgeOf(enemy, 0);
    }

    private static RankBadgeDraw badgeOf(EnemyMob enemy, int gameTime) {
        return overlaysOf(enemy, gameTime).stream()
                .filter(RankBadgeDraw.class::isInstance)
                .map(RankBadgeDraw.class::cast)
                .findFirst().orElseThrow();
    }

    private static List<EnemyBarsDraw> barsOf(EnemyMob enemy, int gameTime) {
        return overlaysOf(enemy, gameTime).stream()
                .filter(EnemyBarsDraw.class::isInstance)
                .map(EnemyBarsDraw.class::cast)
                .toList();
    }

    @Test
    void aJammerStandingInDaylightShowsNoJammingRing() {
        GameWorld context = contextWithStraightPath();
        DefinedEnemyMob jammer = EnemyFixtures.liveMob(context,
                EnemyFixtures.simpleDefinition("jammer").withDisruption(new DisruptionAura(50f, 0.3f, 0.2f)));
        boolean ringedInTheDark = overlaysOf(jammer, 0).stream().anyMatch(EnemyFrameBuilderTest::isJammingRing);

        context.daylight().add(jammer.getX(), jammer.getY(), 100f);

        assertThat(ringedInTheDark).isTrue();
        assertThat(overlaysOf(jammer, 0)).noneMatch(EnemyFrameBuilderTest::isJammingRing);
    }

    private static boolean isJammingRing(EnemyOverlayDraw draw) {
        return draw instanceof EnemyRingDraw ring && ring.palette() == Palette.DISRUPTION;
    }

    @Test
    void aHurtEnemyCarriesAHealthBarFilledToItsHealth() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50, 3);

        enemy.doDamage(Damage.physical(10));

        assertThat(barsOf(enemy, 0)).singleElement().satisfies(bars -> {
            assertThat(bars.bars()).containsExactly(new EnemyBarsDraw.Bar(EnemyBarLook.HEALTH, enemy.getHealthFraction()));
            assertThat(bars.alpha()).isEqualTo(1f);
        });
    }

    @Test
    void alphaZeroReproducesThePreviousTickPosition() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        DefinedEnemyMob mob = (DefinedEnemyMob) enemy;
        enemy.doTick(1);

        EnemyBodyDraw draw = bodyDrawAt(enemy, 1, 0.0);

        assertThat(draw.x()).isEqualTo((float) mob.getPrevX());
        assertThat(draw.y()).isEqualTo((float) mob.getPrevY());
    }

    @Test
    void alphaOneReproducesTheCurrentTickPosition() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        DefinedEnemyMob mob = (DefinedEnemyMob) enemy;
        enemy.doTick(1);

        EnemyBodyDraw draw = bodyDrawAt(enemy, 1, 1.0);

        assertThat(draw.x()).isEqualTo((float) mob.getX());
        assertThat(draw.y()).isEqualTo((float) mob.getY());
    }

    @Test
    void alphaOneHalfIsTheMidpointBetweenPreviousAndCurrentPosition() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        DefinedEnemyMob mob = (DefinedEnemyMob) enemy;
        enemy.doTick(1);
        float expectedX = (float) ((mob.getPrevX() + mob.getX()) / 2.0);
        float expectedY = (float) ((mob.getPrevY() + mob.getY()) / 2.0);

        EnemyBodyDraw draw = bodyDrawAt(enemy, 1, 0.5);

        assertThat(draw.x()).isCloseTo(expectedX, within(0.01f));
        assertThat(draw.y()).isCloseTo(expectedY, within(0.01f));
    }

    private static List<AffixPipsDraw> pipsOf(EnemyMob enemy) {
        return overlaysOf(enemy, 0).stream().filter(AffixPipsDraw.class::isInstance).map(AffixPipsDraw.class::cast)
                .toList();
    }

    private static EnemyDefinition withAffixes(Affix... affixes) {
        return EnemyFixtures.simpleDefinition("plain").withAffixes(List.of(affixes), new AffixContext(Rank.GRUNT, 1f));
    }

    @Test
    void aHiddenEnemyShowsNoPips() {
        EnemyMob enemy = EnemyFixtures.liveMob(contextWithStraightPath(), withAffixes(Affix.ARMORED));
        enemy.applyEffect(Effect.invisible(100, DamageSink.none()));

        assertThat(pipsOf(enemy)).isEmpty();
    }

    @Test
    void anEnemyThatWasNeverInvisibleHasNoCloakProgress() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);

        assertThat(bodyDrawAt(enemy, 0, 0.0).cloakProgress()).isZero();
    }

    @Test
    void cloakProgressRampsUpOverTheFadeDurationAfterGainingInvisibility() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        enemy.applyEffect(Effect.invisible(200, DamageSink.none()));
        enemy.doTick(1); // captures the gain at tick 1

        assertThat(bodyDrawAt(enemy, 1, 0.0).cloakProgress()).isZero();
        assertThat(bodyDrawAt(enemy, 1 + EnemyFrameBuilder.CLOAK_FADE_DURATION_TICKS / 2, 0.0).cloakProgress())
                .isCloseTo(0.5f, within(0.01f));
        assertThat(bodyDrawAt(enemy, 1 + EnemyFrameBuilder.CLOAK_FADE_DURATION_TICKS, 0.0).cloakProgress())
                .isEqualTo(1f);
    }

    @Test
    void cloakProgressRampsBackDownOverTheFadeDurationAfterInvisibilityExpires() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        enemy.applyEffect(Effect.invisible(3, DamageSink.none()));
        enemy.doTick(1);
        enemy.doTick(2);
        enemy.doTick(3); // the effect expires on this tick

        assertThat(bodyDrawAt(enemy, 3, 0.0).cloakProgress()).isEqualTo(1f);
        assertThat(bodyDrawAt(enemy, 3 + EnemyFrameBuilder.CLOAK_FADE_DURATION_TICKS / 2, 0.0).cloakProgress())
                .isCloseTo(0.5f, within(0.01f));
        assertThat(bodyDrawAt(enemy, 3 + EnemyFrameBuilder.CLOAK_FADE_DURATION_TICKS, 0.0).cloakProgress())
                .isZero();
    }

    /**
     * An ability can apply invisibility after this tick's transitions were observed; a frame built
     * on that tick must not see a negative progress.
     */
    @Test
    void cloakProgressIsValidOnTheExactTickAnAbilityFirstAppliesInvisibility() {
        GameWorld world = WorldFixtures.newWorldOnBoard(32, 1000, 1000);
        world.setPath(new Path(List.of(new Vec2(0, 0), new Vec2(1000, 0))));
        EnemyMob ghost = world.getEnemyCatalog().spawn("ghost", world, 0, 100, 4, Rank.GRUNT);
        world.enemies().add(ghost);

        ghost.doDamage(Damage.physical(10));
        ghost.doTick(1); // captures the hit and fires the vanish ability in the same call

        EnemyBodyDraw draw = bodyDrawAt(ghost, 1, 0.0);

        assertThat(draw.cloakProgress()).isBetween(0f, 1f);
    }

    @Test
    void aJammerDrawsADisruptionRingAsWideAsItsReach() {
        GameWorld context = contextWithStraightPath();
        DefinedEnemyMob jammer = EnemyFixtures.liveMob(context,
                EnemyFixtures.simpleDefinition("jammer").withDisruption(new DisruptionAura(100f, 0.3f, 0.6f)));

        List<EnemyOverlayDraw> overlays = overlaysOf(jammer, 0);

        assertThat(overlays).filteredOn(EnemyRingDraw.class::isInstance).map(EnemyRingDraw.class::cast)
                .singleElement()
                .satisfies(ring -> {
                    assertThat(ring.palette()).isEqualTo(Palette.DISRUPTION);
                    assertThat(ring.radius()).isEqualTo(jammer.definition().disruption().orElseThrow().radius());
                });
    }

    private static EnemyMob shielded(int shieldPoints, int ticks) {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50, 3);
        enemy.applyEffect(Effect.shield(DamageUnits.ofPoints(shieldPoints), ticks, DamageSink.none()));
        return enemy;
    }

    private static EnemyEggDraw eggOf(EnemyMob enemy, int gameTime) {
        return overlaysOf(enemy, gameTime).stream()
                .filter(EnemyEggDraw.class::isInstance)
                .map(EnemyEggDraw.class::cast)
                .findFirst().orElseThrow();
    }

    @Test
    void aHurtShieldedEnemyShowsItsShieldBarAboveItsHealthBar() {
        EnemyMob enemy = shielded(100, 100);
        enemy.doDamage(Damage.magic(DamageUnits.ofPoints(1)), AttackProfile.none().asPeriodic());

        assertThat(barsOf(enemy, 0).getFirst().bars()).extracting(EnemyBarsDraw.Bar::look)
                .containsExactly(EnemyBarLook.SHIELD, EnemyBarLook.HEALTH);
    }

    private static EnemyFrameBuilder builtAt(EnemyMob enemy, int gameTime, double seconds) {
        EnemyFrameBuilder builder = new EnemyFrameBuilder(gameTime, 0.0, Optional.empty(), seconds);
        enemy.accept(builder);
        return builder;
    }

    @Test
    void emissionsRunOnTheWallClockAndKeepToOneSeedForTheirEnemy() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50000, 3);
        enemy.applyEffect(Effect.burn(Damage.magic(100), DamageSink.none()));

        EnemyEmissionDraw early = (EnemyEmissionDraw) builtAt(enemy, 0, 1.0).buildOverlays().getFirst();
        EnemyEmissionDraw later = (EnemyEmissionDraw) builtAt(enemy, 0, 2.5).buildOverlays().getFirst();

        assertThat(later.seconds()).isEqualTo(2.5f);
        assertThat(later.seed()).isEqualTo(early.seed());
    }

    @Test
    void aCloakedEnemysEmissionsAndEggGiveNothingAway() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50000, 3);
        enemy.applyEffect(Effect.invisible(100, DamageSink.none()));
        enemy.applyEffect(Effect.poison(Damage.magic(1), DamageSink.none()));
        enemy.applyEffect(Effect.shield(DamageUnits.ofPoints(1), 100, DamageSink.none()));
        enemy.doTick(0);

        int cloaked = EnemyFrameBuilder.CLOAK_FADE_DURATION_TICKS;

        assertThat(eggOf(enemy, cloaked).alpha()).isZero();
        assertThat(overlaysOf(enemy, cloaked)).filteredOn(EnemyEmissionDraw.class::isInstance)
                .map(EnemyEmissionDraw.class::cast).allSatisfy(emission -> assertThat(emission.alpha()).isZero());
    }

    @Test
    void aBurnStrongEnoughToRevealACloakedEnemyStillShows() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50000, 3);
        enemy.applyEffect(Effect.invisible(100, DamageSink.none()));
        enemy.applyEffect(Effect.burn(Damage.magic(10000), DamageSink.none()));
        enemy.doTick(0);

        List<EnemyOverlayDraw> overlays = overlaysOf(enemy, EnemyFrameBuilder.CLOAK_FADE_DURATION_TICKS);

        assertThat(overlays).filteredOn(EnemyEmissionDraw.class::isInstance).map(EnemyEmissionDraw.class::cast)
                .singleElement().satisfies(flames -> assertThat(flames.alpha()).isEqualTo(1f));
    }

}
