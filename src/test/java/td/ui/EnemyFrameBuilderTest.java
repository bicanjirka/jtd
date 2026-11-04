package td.ui;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.InvisibleTemplate;
import td.enemy.Ability;
import td.enemy.AbilityTerm;
import td.enemy.ApplyEffectAction;
import td.enemy.BodyArchetype;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyMob;
import td.enemy.PathDirectionalMovement;
import td.enemy.PeriodicTrigger;
import td.enemy.PulseMovement;
import td.enemy.RadiusTarget;
import td.enemy.Rank;
import td.enemy.StrideMovement;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionAura;
import td.ui.render.CritSparkDraw;
import td.ui.render.EffectPulseDraw;
import td.ui.render.EnemyBarLook;
import td.ui.render.EnemyBarsDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.EnemyLook;
import td.ui.render.EnemyOverlayDraw;
import td.ui.render.EnemyRingDraw;
import td.ui.render.HexGlyph;
import td.ui.render.HexRuneDraw;
import td.ui.render.IceCrystalDraw;
import td.ui.render.MissMarkDraw;
import td.ui.render.Palette;
import td.ui.render.RankBadge;
import td.util.GameWorld;
import td.wave.Path;
import td.wave.Vec2;

import java.util.List;

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

    private static List<EnemyBarsDraw> barsOf(EnemyMob enemy, int gameTime) {
        return overlaysOf(enemy, gameTime).stream()
                .filter(EnemyBarsDraw.class::isInstance)
                .map(EnemyBarsDraw.class::cast)
                .toList();
    }

    @Test
    void anEnemyAtFullHealthCarriesNoBar() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50, 3);

        assertThat(barsOf(enemy, 0)).isEmpty();
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
    void aBarSitsBelowTheFurthestTheBodyReaches() {
        DefinedEnemyMob mob = EnemyFixtures.liveMob(contextWithStraightPath(),
                EnemyFixtures.simpleDefinition("tailed").withArchetype(BodyArchetype.SALAMANDER));
        mob.doDamage(Damage.physical(1));

        EnemyBarsDraw bars = barsOf(mob, 0).getFirst();

        float reach = EnemyLooks.of(EnemyLook.SALAMANDER).reach();
        assertThat(reach).isGreaterThan(1f);
        assertThat(bars.y()).isGreaterThan((float) mob.getY() + mob.getBodyScale() * reach);
    }

    @Test
    void aBarKeepsItsSizeAndPlaceWhileTheBodyPulses() {
        DefinedEnemyMob mob = EnemyFixtures.liveMob(contextWithStraightPath(),
                EnemyFixtures.simpleDefinition("pulsing").withMovement(new PulseMovement()));
        mob.doDamage(Damage.physical(1));
        EnemyBarsDraw still = barsOf(mob, 0).getFirst();
        float stillBody = bodyDrawAt(mob, 0, 0.0).scale();

        for (int tick = 0; tick < 5; tick++) {
            mob.doTick(tick);
        }

        assertThat(bodyDrawAt(mob, 5, 0.0).scale()).isNotEqualTo(stillBody);
        EnemyBarsDraw pulsed = barsOf(mob, 5).getFirst();
        assertThat(pulsed.width()).isEqualTo(still.width());
        assertThat(pulsed.y()).isEqualTo(still.y());
    }

    @Test
    void aBarFadesWithACloakedBody() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50, 3);
        enemy.doDamage(Damage.physical(1));
        enemy.applyEffect(Effect.invisible(100, DamageSink.none()));
        enemy.doTick(0);

        EnemyBarsDraw bars = barsOf(enemy, EnemyFrameBuilder.CLOAK_FADE_DURATION_TICKS).getFirst();

        assertThat(bars.alpha()).isZero();
    }

    @Test
    void aDyingEnemyCarriesNoBar() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50, 3);

        enemy.doDamage(Damage.physical(5000));

        assertThat(barsOf(enemy, 0)).isEmpty();
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

    @Test
    void aMobThatFacesItsPathIsDrawnPointingTheWayItWalksAndAFixedOneIsNot() {
        GameWorld context = WorldFixtures.newWorldOnBoard(1, 1001, 1001);
        context.setPath(new Path(List.of(new Vec2(5, 5), new Vec2(5, 105))));
        EnemyMob facing = EnemyFixtures.liveMob(context,
                EnemyFixtures.simpleDefinition("facing").withMovement(new PathDirectionalMovement()));
        EnemyMob fixed = EnemyFixtures.plainMob(context, 50, 3);
        facing.doTick(1);
        fixed.doTick(1);

        assertThat(bodyDrawAt(facing, 1, 1.0).facingRadians()).isCloseTo(Math.PI / 2, within(1e-6));
        assertThat(bodyDrawAt(fixed, 1, 1.0).facingRadians()).isZero();
    }

    @Test
    void aStridingMobFacesItsPathTurnedByItsSway() {
        GameWorld context = WorldFixtures.newWorldOnBoard(1, 1001, 1001);
        context.setPath(new Path(List.of(new Vec2(5, 5), new Vec2(5, 105))));
        StrideMovement stride = new StrideMovement(0.5f, 4);
        EnemyMob striding = EnemyFixtures.liveMob(context, EnemyFixtures.simpleDefinition("striding").withMovement(stride));
        striding.doTick(1);
        striding.doTick(2);

        double sway = bodyDrawAt(striding, 2, 1.0).facingRadians() - Math.PI / 2;

        assertThat(sway).isNotZero().isCloseTo(stride.angleAt(2), within(1e-6));
    }

    @Test
    void aGruntSpawnsWithNoBadgeAtAll() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);

        assertThat(bodyDrawAt(enemy, 0, 0.0).badge()).isEqualTo(RankBadge.NONE);
    }

    @Test
    void everyNonGruntRankMapsToItsOwnDistinctBadge() {
        GameWorld context = contextWithStraightPath();

        assertThat(bodyDrawAt(EnemyFixtures.liveMob(context, EnemyFixtures.simpleDefinition("plain").withHealthAndPrice(50, 3), Rank.SOLDIER), 0, 0.0).badge())
                .isEqualTo(RankBadge.ONE_CHEVRON);
        assertThat(bodyDrawAt(EnemyFixtures.liveMob(context, EnemyFixtures.simpleDefinition("plain").withHealthAndPrice(50, 3), Rank.VETERAN), 0, 0.0).badge())
                .isEqualTo(RankBadge.TWO_CHEVRON);
        assertThat(bodyDrawAt(EnemyFixtures.liveMob(context, EnemyFixtures.simpleDefinition("plain").withHealthAndPrice(50, 3), Rank.ELITE), 0, 0.0).badge())
                .isEqualTo(RankBadge.STAR);
        assertThat(bodyDrawAt(EnemyFixtures.liveMob(context, EnemyFixtures.simpleDefinition("plain").withHealthAndPrice(50, 3), Rank.BOSS), 0, 0.0).badge())
                .isEqualTo(RankBadge.SKULL);
    }

    @Test
    void deathFadeIsDrawnUntilFadeDurationTicksAfterTheTickThatNoticedDeath() {
        GameWorld context = contextWithStraightPath();
        Rank rank = Rank.SOLDIER;
        EnemyMob enemy = EnemyFixtures.liveMob(context, EnemyFixtures.simpleDefinition("plain").withHealthAndPrice(50, 3), rank);
        int fadeDuration = EnemyFrameBuilder.fadeDurationTicks(rank);

        enemy.doDamage(Damage.physical(5000));
        enemy.doTick(1);

        assertThat(drawsAt(enemy, 1 + fadeDuration)).hasSize(1).first().isInstanceOf(EnemyFadeDraw.class);
        assertThat(drawsAt(enemy, 1 + fadeDuration + 1)).isEmpty();
    }

    @Test
    void anEnemyKilledBeforeItsNextTickFadesFromTheStart() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.liveMob(context, EnemyFixtures.simpleDefinition("plain").withHealthAndPrice(50, 3), Rank.GRUNT);

        enemy.doDamage(Damage.physical(5000));

        assertThat(drawsAt(enemy, 0)).singleElement().isInstanceOfSatisfying(EnemyFadeDraw.class,
                fade -> assertThat(fade.fadeProgress()).isZero());
    }

    @Test
    void anEnemyThatReachesThePathsEndFadesInPlaceInsteadOfWrappingToTheStart() {
        GameWorld context = WorldFixtures.newWorldOnBoard(1, 1001, 1001);
        context.setPath(new Path(List.of(new Vec2(5, 5), new Vec2(15, 5))));

        EnemyMob enemy = EnemyFixtures.plainMob(context, 10, 3);
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
    void aHexedEnemyWearsItsHexsRuneAndAPlainOneNone() {
        GameWorld context = contextWithStraightPath();
        EnemyMob doomed = EnemyFixtures.plainMob(context, 50, 3);
        doomed.applyEffect(Effect.hex(EffectKind.DOOM, 80, DamageSink.none()));
        EnemyMob plain = EnemyFixtures.plainMob(context, 50, 3);

        EnemyFrameBuilder builder = new EnemyFrameBuilder(0, 0.0);
        doomed.accept(builder);
        plain.accept(builder);

        assertThat(builder.buildOverlays()).filteredOn(HexRuneDraw.class::isInstance)
                .singleElement().extracting(draw -> ((HexRuneDraw) draw).glyph()).isEqualTo(HexGlyph.DOOM);
    }

    @Test
    void anEnemyThatJustDodgedYieldsAMissMarkBesideItAndNoCritSpark() {
        GameWorld context = WorldFixtures.newWorldOnBoard(() -> 0.0, 1, 1001, 1001);
        context.setPath(new Path(List.of(new Vec2(5, 5), new Vec2(105, 5))));
        EnemyMob nimble = EnemyFixtures.liveMob(context, EnemyFixtures.simpleDefinition("nimble").withAgility(50f));
        nimble.takeHit(Damage.physical(10), AttackProfile.critChance(1f).asAimed());
        nimble.doTick(1); // captures the dodge

        EnemyFrameBuilder builder = new EnemyFrameBuilder(1, 0.0);
        nimble.accept(builder);
        EnemyBodyDraw body = (EnemyBodyDraw) builder.build().getFirst();

        assertThat(builder.buildCritSparks()).isEmpty();
        assertThat(builder.buildOverlays()).filteredOn(MissMarkDraw.class::isInstance).singleElement()
                .satisfies(draw -> {
                    MissMarkDraw miss = (MissMarkDraw) draw;
                    assertThat(miss.fadeProgress()).isZero();
                    assertThat(miss.x()).isGreaterThan(body.x());
                    assertThat(miss.y()).isEqualTo(body.y());
                });
    }

    @Test
    void anEnemyThatNeverTookACriticalHitYieldsNoCritSpark() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        enemy.doTick(1);

        EnemyFrameBuilder builder = new EnemyFrameBuilder(1, 0.0);
        enemy.accept(builder);

        assertThat(builder.buildCritSparks()).isEmpty();
    }

    @Test
    void anEnemyThatJustSurvivedACriticalHitYieldsOneCritSpark() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
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
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        enemy.doDamage(Damage.physical(10), AttackProfile.critChance(1f));
        enemy.doTick(1); // captures the critical hit at tick 1

        int afterDuration = 1 + EnemyFrameBuilder.CRIT_SPARK_DURATION_TICKS + 1;
        EnemyFrameBuilder builder = new EnemyFrameBuilder(afterDuration, 0.0);
        enemy.accept(builder);

        assertThat(builder.buildCritSparks()).isEmpty();
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
    void anEnemyWithNoShieldAndNoSupportAuraYieldsNoOverlays() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);

        assertThat(overlaysOf(enemy, 0)).isEmpty();
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

    @Test
    void anActiveShieldYieldsARingInTheShieldMarkersColour() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        enemy.applyEffect(Effect.shield(0.3f, 5, DamageSink.none()));

        List<EnemyOverlayDraw> overlays = overlaysOf(enemy, 0);

        assertThat(overlays).hasSize(1);
        EnemyRingDraw ring = (EnemyRingDraw) overlays.getFirst();
        assertThat(ring.palette()).isEqualTo(Palette.STATUS_MARKER_SHIELD);
    }

    @Test
    void theShieldRingDisappearsOnceTheShieldExpires() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        enemy.applyEffect(Effect.shield(0.3f, 1, DamageSink.none()));
        enemy.doTick(1); // the shield expires this tick

        assertThat(overlaysOf(enemy, 1)).isEmpty();
    }

    @Test
    void anActiveFreezeYieldsAnIceCrystalOverlayInTheFreezeCrystalColour() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        enemy.applyEffect(Effect.freeze(5, DamageSink.none()));

        List<EnemyOverlayDraw> overlays = overlaysOf(enemy, 0);

        assertThat(overlays).hasSize(1);
        IceCrystalDraw crystal = (IceCrystalDraw) overlays.getFirst();
        assertThat(crystal.palette()).isEqualTo(Palette.FREEZE_CRYSTAL);
    }

    @Test
    void theIceCrystalOverlayDisappearsOnceTheFreezeExpires() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        enemy.applyEffect(Effect.freeze(1, DamageSink.none()));
        enemy.doTick(1); // the freeze expires this tick

        assertThat(overlaysOf(enemy, 1)).isEmpty();
    }

    @Test
    void aDefinitionWithARadiusAbilityYieldsASupportAuraRingAtItsAuthoredRadius() {
        GameWorld context = contextWithStraightPath();
        EnemyMob shrouder = EnemyFixtures.liveMob(context, EnemyFixtures.simpleDefinition("shrouder")
                .withAbilities(List.of(new Ability(AbilityTerm.SHROUD, new PeriodicTrigger(40),
                        new ApplyEffectAction(new InvisibleTemplate(60), new RadiusTarget(100f))))));

        List<EnemyOverlayDraw> overlays = overlaysOf(shrouder, 0);

        assertThat(overlays).hasSize(1);
        EnemyRingDraw ring = (EnemyRingDraw) overlays.getFirst();
        assertThat(ring.radius()).isEqualTo(100f);
        assertThat(ring.palette()).isEqualTo(Palette.STATUS_MARKER_INVISIBLE);
    }

    @Test
    void anEnemyTakesNoRingForGainingOrLosingAnEffect() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        enemy.applyEffect(Effect.chill(0.5f, 2, DamageSink.none()));

        enemy.doTick(1);
        List<EnemyOverlayDraw> gained = overlaysOf(enemy, 1);
        enemy.doTick(2);
        enemy.doTick(3);
        List<EnemyOverlayDraw> lost = overlaysOf(enemy, 3);

        assertThat(gained).noneMatch(EffectPulseDraw.class::isInstance);
        assertThat(lost).noneMatch(EffectPulseDraw.class::isInstance);
    }

    @Test
    void anAbilityDrivenSpawnYieldsASpawnBurstPulseAtItsArrival() {
        GameWorld context = contextWithStraightPath();
        EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
        ((DefinedEnemyMob) enemy).recordAbilitySpawn(0);

        List<EnemyOverlayDraw> overlays = overlaysOf(enemy, 0);

        assertThat(overlays).hasSize(1);
        EffectPulseDraw pulse = (EffectPulseDraw) overlays.getFirst();
        assertThat(pulse.palette()).isEqualTo(Palette.SPAWN_BURST);
        assertThat(pulse.progress()).isZero();
    }

}
