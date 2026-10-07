package td.ui;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.BodyArchetype;
import td.enemy.CriticalImmunityTrait;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.FlatResistTrait;
import td.enemy.HurtSpeedTrait;
import td.enemy.PercentResistTrait;
import td.enemy.Rank;
import td.fixtures.WorldFixtures;
import td.ui.render.CritSparkDraw;
import td.ui.render.EffectPulseDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.EnemyOverlayDraw;
import td.ui.render.EnemyRingDraw;
import td.ui.render.HexGlyph;
import td.ui.render.HexRuneDraw;
import td.ui.render.IceCrystalDraw;
import td.ui.render.Palette;
import td.ui.render.PulseDirection;
import td.ui.render.RankBadge;
import td.ui.render.StatusMarkerDraw;
import td.ui.render.TraitMarkerDraw;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EnemyFrameBuilderTest {

    private static GameWorld contextWithStraightPath() {
        GameWorld context = WorldFixtures.newWorldOnBoard(1, 1001, 1001);
        context.setPath(new PathNormal(List.of(new Vec2(5, 5), new Vec2(105, 5))));
        return context;
    }

    private static EnemyBodyDraw bodyDrawAt(EnemyMob enemy, int gameTime, double alpha) {
        EnemyFrameBuilder builder = new EnemyFrameBuilder(gameTime, alpha);
        enemy.accept(builder);
        return (EnemyBodyDraw) builder.build().getFirst();
    }

    private static List<EnemyDraw> drawsAt(EnemyMob enemy, int gameTime) {
        EnemyFrameBuilder builder = new EnemyFrameBuilder(gameTime, 0.0);
        enemy.accept(builder);
        return builder.build();
    }

    private static List<EnemyOverlayDraw> overlaysOf(EnemyMob enemy, int gameTime) {
        EnemyFrameBuilder builder = new EnemyFrameBuilder(gameTime, 0.0);
        enemy.accept(builder);
        return builder.buildOverlays();
    }

    @Test
    void anEnemyHeldByAFieldEffectFlickersBetweenSteadyAndDimmed() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.corroded(100, d -> {
        }));
        int half = EnemyFrameBuilder.FLICKER_HALF_PERIOD_TICKS;

        float first = bodyDrawAt(enemy, 0, 0.0).flicker();
        float second = bodyDrawAt(enemy, half, 0.0).flicker();
        float third = bodyDrawAt(enemy, 2 * half, 0.0).flicker();

        assertThat(first).isZero();
        assertThat(second).isEqualTo(1f);
        assertThat(third).isZero();
    }

    @Test
    void anEnemyWithNoFieldEffectNeverFlickers() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.burn(Damage.magic(10), 100, d -> {
        }));

        float dimmedHalf = bodyDrawAt(enemy, EnemyFrameBuilder.FLICKER_HALF_PERIOD_TICKS, 0.0).flicker();

        assertThat(dimmedHalf).isZero();
    }

    @Test
    void alphaZeroReproducesThePreviousTickPosition() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        DefinedEnemyMob mob = (DefinedEnemyMob) enemy;
        enemy.doTick(1);

        EnemyBodyDraw draw = bodyDrawAt(enemy, 1, 0.0);

        assertThat(draw.x()).isEqualTo((float) mob.getPrevX());
        assertThat(draw.y()).isEqualTo((float) mob.getPrevY());
    }

    @Test
    void alphaOneReproducesTheCurrentTickPosition() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        DefinedEnemyMob mob = (DefinedEnemyMob) enemy;
        enemy.doTick(1);

        EnemyBodyDraw draw = bodyDrawAt(enemy, 1, 1.0);

        assertThat(draw.x()).isEqualTo((float) mob.getX());
        assertThat(draw.y()).isEqualTo((float) mob.getY());
    }

    @Test
    void alphaOneHalfIsTheMidpointBetweenPreviousAndCurrentPosition() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        DefinedEnemyMob mob = (DefinedEnemyMob) enemy;
        enemy.doTick(1);
        float expectedX = (float) ((mob.getPrevX() + mob.getX()) / 2.0);
        float expectedY = (float) ((mob.getPrevY() + mob.getY()) / 2.0);

        EnemyBodyDraw draw = bodyDrawAt(enemy, 1, 0.5);

        assertThat(draw.x()).isCloseTo(expectedX, within(0.01f));
        assertThat(draw.y()).isCloseTo(expectedY, within(0.01f));
    }

    @Test
    void aGruntSpawnsWithNoBadgeAtAll() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);

        assertThat(bodyDrawAt(enemy, 0, 0.0).badge()).isEqualTo(RankBadge.NONE);
    }

    @Test
    void everyNonGruntRankMapsToItsOwnDistinctBadge() {
        GameWorld context = contextWithStraightPath();

        assertThat(bodyDrawAt(EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.SOLDIER), 0, 0.0).badge())
                .isEqualTo(RankBadge.ONE_CHEVRON);
        assertThat(bodyDrawAt(EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.VETERAN), 0, 0.0).badge())
                .isEqualTo(RankBadge.TWO_CHEVRON);
        assertThat(bodyDrawAt(EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.ELITE), 0, 0.0).badge())
                .isEqualTo(RankBadge.STAR);
        assertThat(bodyDrawAt(EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.BOSS), 0, 0.0).badge())
                .isEqualTo(RankBadge.SKULL);
    }

    @Test
    void deathFadeIsDrawnUntilFadeDurationTicksAfterTheTickThatNoticedDeath() {
        GameWorld context = contextWithStraightPath();
        Rank rank = Rank.SOLDIER;
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, rank);
        int fadeDuration = EnemyFrameBuilder.fadeDurationTicks(rank);

        enemy.doDamage(Damage.physical(5000));
        enemy.doTick(1);

        assertThat(drawsAt(enemy, 1 + fadeDuration)).hasSize(1).first().isInstanceOf(EnemyFadeDraw.class);
        assertThat(drawsAt(enemy, 1 + fadeDuration + 1)).isEmpty();
    }

    @Test
    void fadeProgressStaysWithinRangeBeforeTheDeathTickIsCaptured() {
        float progress = EnemyFrameBuilder.fadeProgress(Rank.GRUNT, -1);

        assertThat(progress).isBetween(0f, 1f);
    }

    @Test
    void anEnemyThatReachesThePathsEndFadesInPlaceInsteadOfWrappingToTheStart() {
        GameWorld context = WorldFixtures.newWorldOnBoard(1, 1001, 1001);
        context.setPath(new PathNormal(List.of(new Vec2(5, 5), new Vec2(15, 5))));

        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, Rank.GRUNT);
        int initialLives = context.economy().getLives();
        int tick = 0;
        while (context.economy().getLives() == initialLives) {
            tick++;
            enemy.doTick(tick);
        }

        assertThat(enemy.isDead()).isTrue();
        EnemyFrameBuilder builder = new EnemyFrameBuilder(tick, 0.0);
        enemy.accept(builder);
        assertThat(builder.build().getFirst()).isInstanceOf(EnemyFadeDraw.class);
    }

    @Test
    void anEnemyWithNoActiveEffectsYieldsNoStatusMarkers() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);

        EnemyFrameBuilder builder = new EnemyFrameBuilder(0, 0.0);
        enemy.accept(builder);

        assertThat(builder.buildMarkers()).isEmpty();
    }

    @Test
    void anActiveSlowYieldsExactlyOneStatusMarkerWithTheSlowRole() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.chill(0.5f, 5, d -> {
        }));

        EnemyFrameBuilder builder = new EnemyFrameBuilder(0, 0.0);
        enemy.accept(builder);
        List<StatusMarkerDraw> markers = builder.buildMarkers();

        assertThat(markers).hasSize(1);
        assertThat(markers.getFirst().palette()).isEqualTo(Palette.STATUS_MARKER_CHILL);
    }

    @Test
    void twoActiveEffectsYieldTwoDistinctlyPositionedMarkers() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.chill(0.5f, 5, d -> {
        }));
        enemy.applyEffect(Effect.heal(1, 5, d -> {
        }));

        EnemyFrameBuilder builder = new EnemyFrameBuilder(0, 0.0);
        enemy.accept(builder);
        List<StatusMarkerDraw> markers = builder.buildMarkers();

        assertThat(markers).hasSize(2);
        assertThat(markers.get(0).x()).isNotEqualTo(markers.get(1).x());
    }

    @Test
    void aFourthSimultaneousEffectCollapsesIntoOneOverflowMarkerInsteadOfGrowingTheRow() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.chill(0.5f, 5, d -> {
        }));
        enemy.applyEffect(Effect.heal(1, 5, d -> {
        }));
        enemy.applyEffect(Effect.vulnerable(1, 5, d -> {
        }));
        enemy.applyEffect(Effect.shield(0.3f, 5, d -> {
        }));
        enemy.applyEffect(Effect.revealed(5, d -> {
        }));

        EnemyFrameBuilder builder = new EnemyFrameBuilder(0, 0.0);
        enemy.accept(builder);
        List<StatusMarkerDraw> markers = builder.buildMarkers();

        assertThat(markers).hasSize(EnemyFrameBuilder.MAX_VISIBLE_MARKERS + 1);
        assertThat(markers.getLast().palette()).isEqualTo(Palette.STATUS_MARKER_OVERFLOW);
        assertThat(markers.getLast().hiddenCount()).isEqualTo(1);
    }

    @Test
    void effectsPastTheCapGroupByCategoryAndEachMarkerCountsTheOtherKindsOfItsCategory() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.burn(Damage.magic(10), 50, d -> {
        }));
        enemy.applyEffect(Effect.poison(Damage.magic(10), 50, d -> {
        }));
        enemy.applyEffect(Effect.exposed(5, d -> {
        }));
        enemy.applyEffect(Effect.marked(5, d -> {
        }));

        EnemyFrameBuilder builder = new EnemyFrameBuilder(0, 0.0);
        enemy.accept(builder);
        List<StatusMarkerDraw> markers = builder.buildMarkers();

        assertThat(markers).extracting(StatusMarkerDraw::palette)
                .containsExactly(Palette.STATUS_MARKER_BURN, Palette.STATUS_MARKER_SCORCHED,
                        Palette.STATUS_MARKER_EXPOSED);
        assertThat(markers).extracting(StatusMarkerDraw::hiddenCount).containsExactly(1, 1, 1);
        assertThat(markers.get(1).x() - markers.get(0).x()).isGreaterThanOrEqualTo(
                EnemyFrameBuilder.COUNTED_MARKER_SPACING);
        assertThat(markers.get(2).x() - markers.get(1).x()).isGreaterThanOrEqualTo(
                EnemyFrameBuilder.COUNTED_MARKER_SPACING);
    }

    @Test
    void aHexedEnemyWearsItsHexsRuneAndAPlainOneNone() {
        GameWorld context = contextWithStraightPath();
        EnemyMob doomed = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        doomed.applyEffect(Effect.hex(EffectKind.DOOM, 80, d -> {
        }));
        EnemyMob plain = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);

        EnemyFrameBuilder builder = new EnemyFrameBuilder(0, 0.0);
        doomed.accept(builder);
        plain.accept(builder);

        assertThat(builder.buildOverlays()).filteredOn(HexRuneDraw.class::isInstance)
                .singleElement().extracting(draw -> ((HexRuneDraw) draw).glyph()).isEqualTo(HexGlyph.DOOM);
    }

    @Test
    void anEnemyThatNeverTookACriticalHitYieldsNoCritSpark() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.doTick(1);

        EnemyFrameBuilder builder = new EnemyFrameBuilder(1, 0.0);
        enemy.accept(builder);

        assertThat(builder.buildCritSparks()).isEmpty();
    }

    @Test
    void anEnemyThatJustSurvivedACriticalHitYieldsOneCritSpark() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.doDamage(Damage.physical(10), AttackProfile.critChance(1f));
        enemy.doTick(1); // captures the critical hit

        EnemyFrameBuilder builder = new EnemyFrameBuilder(1, 0.0);
        enemy.accept(builder);
        List<CritSparkDraw> sparks = builder.buildCritSparks();

        assertThat(sparks).hasSize(1);
        assertThat(sparks.getFirst().palette()).isEqualTo(Palette.CRIT_SPARK);
        assertThat(sparks.getFirst().fadeProgress()).isZero();
    }

    @Test
    void theCritSparkStopsShowingAfterItsDurationElapses() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.doDamage(Damage.physical(10), AttackProfile.critChance(1f));
        enemy.doTick(1); // captures the critical hit at tick 1

        int afterDuration = 1 + EnemyFrameBuilder.CRIT_SPARK_DURATION_TICKS + 1;
        EnemyFrameBuilder builder = new EnemyFrameBuilder(afterDuration, 0.0);
        enemy.accept(builder);

        assertThat(builder.buildCritSparks()).isEmpty();
    }

    @Test
    void aFifthSimultaneousEffectDoesNotGrowTheRowFurther() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.chill(0.5f, 5, d -> {
        }));
        enemy.applyEffect(Effect.vulnerable(1, 5, d -> {
        }));
        enemy.applyEffect(Effect.exposed(5, d -> {
        }));
        enemy.applyEffect(Effect.shield(0.3f, 5, d -> {
        }));
        enemy.applyEffect(Effect.invisible(5, d -> {
        }));

        EnemyFrameBuilder builder = new EnemyFrameBuilder(0, 0.0);
        enemy.accept(builder);
        List<StatusMarkerDraw> markers = builder.buildMarkers();

        assertThat(markers).hasSize(EnemyFrameBuilder.MAX_VISIBLE_MARKERS + 1);
    }

    @Test
    void anEnemyThatWasNeverInvisibleHasNoCloakProgress() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);

        assertThat(bodyDrawAt(enemy, 0, 0.0).cloakProgress()).isZero();
    }

    @Test
    void cloakProgressRampsUpOverTheFadeDurationAfterGainingInvisibility() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.invisible(200, d -> {
        }));
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
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.invisible(3, d -> {
        }));
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
        world.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(1000, 0))));
        EnemyMob ghost = world.getEnemyCatalog().spawn("g", world, 0, 100, 4, Rank.GRUNT);
        world.enemies().add(ghost);

        ghost.doDamage(Damage.physical(10));
        ghost.doTick(1); // captures the hit and fires the vanish ability in the same call

        EnemyBodyDraw draw = bodyDrawAt(ghost, 1, 0.0);

        assertThat(draw.cloakProgress()).isBetween(0f, 1f);
    }

    @Test
    void anEnemyWithNoShieldAndNoSupportAuraYieldsNoOverlays() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);

        assertThat(overlaysOf(enemy, 0)).isEmpty();
    }

    @Test
    void aJammerDrawsADisruptionRingAsWideAsItsReach() {
        GameWorld context = contextWithStraightPath();
        DefinedEnemyMob jammer = (DefinedEnemyMob) EnemyFactory.getEnemy("j", context, 0, 50, 3, Rank.GRUNT);

        List<EnemyOverlayDraw> overlays = overlaysOf(jammer, 0);

        assertThat(overlays).filteredOn(EnemyRingDraw.class::isInstance).map(EnemyRingDraw.class::cast)
                .singleElement()
                .satisfies(ring -> {
                    assertThat(ring.palette()).isEqualTo(Palette.DISRUPTION);
                    assertThat(ring.radius()).isEqualTo(jammer.definition().disruption().orElseThrow().radius());
                });
    }

    @Test
    void anActiveShieldYieldsARingInTheShieldMarkersColour() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.shield(0.3f, 5, d -> {
        }));

        List<EnemyOverlayDraw> overlays = overlaysOf(enemy, 0);

        assertThat(overlays).hasSize(1);
        EnemyRingDraw ring = (EnemyRingDraw) overlays.getFirst();
        assertThat(ring.palette()).isEqualTo(Palette.STATUS_MARKER_SHIELD);
    }

    @Test
    void theShieldRingDisappearsOnceTheShieldExpires() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.shield(0.3f, 1, d -> {
        }));
        enemy.doTick(1); // the shield expires this tick

        assertThat(overlaysOf(enemy, 1)).isEmpty();
    }

    @Test
    void anActiveFreezeYieldsAnIceCrystalOverlayInTheFreezeCrystalColour() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.freeze(5, d -> {
        }));

        List<EnemyOverlayDraw> overlays = overlaysOf(enemy, 0);

        assertThat(overlays).hasSize(1);
        IceCrystalDraw crystal = (IceCrystalDraw) overlays.getFirst();
        assertThat(crystal.palette()).isEqualTo(Palette.FREEZE_CRYSTAL);
    }

    @Test
    void theIceCrystalOverlayDisappearsOnceTheFreezeExpires() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.freeze(1, d -> {
        }));
        enemy.doTick(1); // the freeze expires this tick

        assertThat(overlaysOf(enemy, 1)).isEmpty();
    }

    @Test
    void aDefinitionWithARadiusAbilityYieldsASupportAuraRingAtItsAuthoredRadius() {
        GameWorld context = contextWithStraightPath();
        EnemyMob eliteGhost = EnemyFactory.getEnemy("g", context, 0, 800, 16, Rank.ELITE); // has the shroud ability

        List<EnemyOverlayDraw> overlays = overlaysOf(eliteGhost, 0);

        assertThat(overlays).hasSize(1);
        EnemyRingDraw ring = (EnemyRingDraw) overlays.getFirst();
        assertThat(ring.radius()).isEqualTo(100f);
        assertThat(ring.palette()).isEqualTo(Palette.STATUS_MARKER_INVISIBLE);
    }

    @Test
    void aGainedEffectYieldsAnOutwardPulseAtTheMomentItsGained() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.chill(0.5f, 200, d -> {
        }));
        enemy.doTick(1); // captures the gain at tick 1

        List<EnemyOverlayDraw> overlays = overlaysOf(enemy, 1);

        assertThat(overlays).hasSize(1);
        EffectPulseDraw pulse = (EffectPulseDraw) overlays.getFirst();
        assertThat(pulse.palette()).isEqualTo(Palette.STATUS_MARKER_CHILL);
        assertThat(pulse.direction()).isEqualTo(PulseDirection.OUTWARD);
        assertThat(pulse.progress()).isZero();
    }

    @Test
    void theGainPulseDisappearsAfterItsDuration() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.chill(0.5f, 200, d -> {
        }));
        enemy.doTick(1);

        assertThat(overlaysOf(enemy, 1 + EnemyFrameBuilder.EFFECT_PULSE_DURATION_TICKS)).hasSize(1);
        assertThat(overlaysOf(enemy, 1 + EnemyFrameBuilder.EFFECT_PULSE_DURATION_TICKS + 1)).isEmpty();
    }

    @Test
    void aLostEffectYieldsAnInwardPulseAtTheMomentItExpires() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        int durationTicks = EnemyFrameBuilder.EFFECT_PULSE_DURATION_TICKS + 2;
        enemy.applyEffect(Effect.chill(0.5f, durationTicks, d -> {
        }));
        for (int t = 1; t <= durationTicks; t++) { // the slow expires on the last of these
            enemy.doTick(t);
        }

        // Query past the gain pulse's own window, so only the loss pulse this expiry just
        // produced remains - the two are far enough apart in every real effect's duration.
        List<EnemyOverlayDraw> overlays = overlaysOf(enemy, durationTicks);

        assertThat(overlays).hasSize(1);
        EffectPulseDraw pulse = (EffectPulseDraw) overlays.getFirst();
        assertThat(pulse.direction()).isEqualTo(PulseDirection.INWARD);
    }

    @Test
    void anAbilityDrivenSpawnYieldsASpawnBurstPulseAtItsArrival() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        ((DefinedEnemyMob) enemy).recordAbilitySpawn(0);

        List<EnemyOverlayDraw> overlays = overlaysOf(enemy, 0);

        assertThat(overlays).hasSize(1);
        EffectPulseDraw pulse = (EffectPulseDraw) overlays.getFirst();
        assertThat(pulse.palette()).isEqualTo(Palette.SPAWN_BURST);
        assertThat(pulse.direction()).isEqualTo(PulseDirection.OUTWARD);
        assertThat(pulse.progress()).isZero();
    }

    @Test
    void anEnemyWithNoTraitsYieldsNoTraitMarkers() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT); // no traits

        assertThat(overlaysOf(enemy, 0)).isEmpty();
    }

    @Test
    void anArmoredEnemysTraitsEachYieldTheirOwnMarker() {
        GameWorld context = contextWithStraightPath();
        EnemyMob armored = EnemyFactory.getEnemy("s", context, 0, 80, 3, Rank.GRUNT); // resist + crit-immune

        List<EnemyOverlayDraw> overlays = overlaysOf(armored, 0);

        assertThat(overlays).hasSize(2);
        assertThat(overlays).allMatch(TraitMarkerDraw.class::isInstance);
        List<Palette> palettes = overlays.stream().map(o -> ((TraitMarkerDraw) o).palette()).toList();
        assertThat(palettes).containsExactlyInAnyOrder(Palette.TRAIT_MARKER_PERCENT_RESIST, Palette.TRAIT_MARKER_CRITICAL_IMMUNE);
    }

    @Test
    void traitMarkersSitBelowTheBodyWhileEffectMarkersSitAbove() {
        GameWorld context = contextWithStraightPath();
        EnemyMob armored = EnemyFactory.getEnemy("s", context, 0, 80, 3, Rank.GRUNT);
        EnemyBodyDraw body = bodyDrawAt(armored, 0, 0.0);

        List<EnemyOverlayDraw> overlays = overlaysOf(armored, 0);

        assertThat(overlays).allSatisfy(o -> assertThat(((TraitMarkerDraw) o).y()).isGreaterThan(body.y()));
    }

    @Test
    void aFourthSimultaneousTraitCollapsesIntoOneOverflowMarkerInsteadOfGrowingTheRow() {
        GameWorld context = contextWithStraightPath();
        EnemyDefinition heavilyTraited = EnemyDefinition.of("heavy", "Heavy", 100, 5, 1.28f, BodyArchetype.CIRCLE)
                .withTraits(List.of(new PercentResistTrait(0.8f), new FlatResistTrait(5),
                        new CriticalImmunityTrait(), new HurtSpeedTrait(1.2f)));
        context.getEnemyCatalog().register(heavilyTraited);
        EnemyMob enemy = context.getEnemyCatalog().spawn("heavy", context, 0, 100, 5, Rank.GRUNT);

        List<EnemyOverlayDraw> overlays = overlaysOf(enemy, 0);

        assertThat(overlays).hasSize(EnemyFrameBuilder.MAX_VISIBLE_MARKERS + 1);
        assertThat(((TraitMarkerDraw) overlays.getLast()).palette()).isEqualTo(Palette.TRAIT_MARKER_OVERFLOW);
    }
}
