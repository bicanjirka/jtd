package td.ui;

import org.junit.jupiter.api.Test;
import td.damage.AttackOrigin;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.InvisibleTemplate;
import td.enemy.Ability;
import td.enemy.AbilityTerm;
import td.enemy.Affix;
import td.enemy.AffixContext;
import td.enemy.ApplyEffectAction;
import td.enemy.BodyArchetype;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.PathDirectionalMovement;
import td.enemy.PeriodicTrigger;
import td.enemy.PulseMovement;
import td.enemy.RadiusTarget;
import td.enemy.Rank;
import td.enemy.RegenerationTrait;
import td.enemy.StrideMovement;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionAura;
import td.ui.render.AffixPipsDraw;
import td.ui.render.CritSparkDraw;
import td.ui.render.EffectPulseDraw;
import td.ui.render.EnemyBarLook;
import td.ui.render.EnemyBarsDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyBurstDraw;
import td.ui.render.EnemyBurstLook;
import td.ui.render.EnemyDotLook;
import td.ui.render.EnemyDotsDraw;
import td.ui.render.EnemyDraw;
import td.ui.render.EnemyEggDraw;
import td.ui.render.EnemyEmissionDraw;
import td.ui.render.EnemyEmissionLook;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.EnemyIceDraw;
import td.ui.render.EnemyLook;
import td.ui.render.EnemyOverlayDraw;
import td.ui.render.EnemyRingDraw;
import td.ui.render.EnemyTintDraw;
import td.ui.render.EnemyTintLook;
import td.ui.render.HexGlyph;
import td.ui.render.HexRuneDraw;
import td.ui.render.MissMarkDraw;
import td.ui.render.Palette;
import td.ui.render.RankBadge;
import td.ui.render.RankBadgeDraw;
import td.util.GameWorld;
import td.wave.Path;
import td.wave.Vec2;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    private static RankBadgeDraw badgeOf(EnemyMob enemy) {
        return badgeOf(enemy, 0);
    }

    private static RankBadgeDraw badgeOf(EnemyMob enemy, int gameTime) {
        return overlaysOf(enemy, gameTime).stream()
                .filter(RankBadgeDraw.class::isInstance)
                .map(RankBadgeDraw.class::cast)
                .findFirst().orElseThrow();
    }

    private static EnemyDotsDraw dotsOf(EnemyMob enemy) {
        return overlaysOf(enemy, 0).stream()
                .filter(EnemyDotsDraw.class::isInstance)
                .map(EnemyDotsDraw.class::cast)
                .findFirst().orElseThrow();
    }

    private static CritSparkDraw critSparkOf(EnemyMob enemy, int gameTime) {
        EnemyFrameBuilder builder = new EnemyFrameBuilder(gameTime, 0.0);
        enemy.accept(builder);
        return builder.buildCritSparks().getFirst();
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
    void aGruntCarriesNoBadge() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50, 3);

        assertThat(overlaysOf(enemy, 0)).noneMatch(RankBadgeDraw.class::isInstance);
    }

    @Test
    void everyOtherRankCarriesItsOwnBadgeAtOneSizeJustAboveTheBody() {
        GameWorld context = contextWithStraightPath();

        List<RankBadgeDraw> badges = List.of(Rank.SOLDIER, Rank.VETERAN, Rank.ELITE, Rank.BOSS).stream()
                .map(rank -> badgeOf(EnemyFixtures.liveMob(context, EnemyFixtures.simpleDefinition("plain"), rank)))
                .toList();

        assertThat(badges).extracting(RankBadgeDraw::badge)
                .containsExactly(RankBadge.ONE_CHEVRON, RankBadge.TWO_CHEVRON, RankBadge.STAR, RankBadge.SKULL);
        assertThat(badges).extracting(RankBadgeDraw::size).containsOnly(badges.getFirst().size());
    }

    private static List<AffixPipsDraw> pipsOf(EnemyMob enemy) {
        return overlaysOf(enemy, 0).stream().filter(AffixPipsDraw.class::isInstance).map(AffixPipsDraw.class::cast)
                .toList();
    }

    private static EnemyDefinition withAffixes(Affix... affixes) {
        return EnemyFixtures.simpleDefinition("plain").withAffixes(List.of(affixes), new AffixContext(Rank.GRUNT, 1f));
    }

    @Test
    void anAffixedEnemyCarriesOnePipPerAffixInTheOrderWritten() {
        EnemyMob enemy = EnemyFixtures.liveMob(contextWithStraightPath(), withAffixes(Affix.WITHERED, Affix.HULKING));

        assertThat(pipsOf(enemy)).singleElement()
                .satisfies(pips -> assertThat(pips.affixes()).containsExactly(Affix.WITHERED, Affix.HULKING));
    }

    @Test
    void aPlainEnemyCarriesNoPips() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50, 3);

        assertThat(pipsOf(enemy)).isEmpty();
    }

    @Test
    void aHiddenEnemyShowsNoPips() {
        EnemyMob enemy = EnemyFixtures.liveMob(contextWithStraightPath(), withAffixes(Affix.ARMORED));
        enemy.applyEffect(Effect.invisible(100, DamageSink.none()));

        assertThat(pipsOf(enemy)).isEmpty();
    }

    @Test
    void theBadgeAndThePipsShareOneRowCentredOverTheBody() {
        EnemyMob enemy = EnemyFixtures.liveMob(contextWithStraightPath(), withAffixes(Affix.ARMORED), Rank.ELITE);

        RankBadgeDraw badge = badgeOf(enemy);
        AffixPipsDraw pips = pipsOf(enemy).getFirst();

        assertThat(pips.y()).isEqualTo(badge.y());
        assertThat((badge.x() + pips.x()) / 2).isCloseTo((float) enemy.getX(), within(1e-3f));
    }

    @Test
    void pipsShrinkOverASmallBodyWhileTheBadgeKeepsItsSize() {
        GameWorld bigBoard = WorldFixtures.newWorldOnBoard(64, 1001, 1001);
        bigBoard.setPath(new Path(List.of(new Vec2(5, 5), new Vec2(105, 5))));
        EnemyMob small = EnemyFixtures.liveMob(contextWithStraightPath(), withAffixes(Affix.ARMORED), Rank.ELITE);
        EnemyMob big = EnemyFixtures.liveMob(bigBoard, withAffixes(Affix.ARMORED), Rank.ELITE);

        assertThat(pipsOf(small).getFirst().size()).isLessThan(pipsOf(big).getFirst().size());
        assertThat(badgeOf(small).size()).isEqualTo(badgeOf(big).size());
    }

    @Test
    void aBadgeFadesWithACloakedBody() {
        EnemyMob enemy = EnemyFixtures.liveMob(contextWithStraightPath(), EnemyFixtures.simpleDefinition("plain"), Rank.ELITE);
        enemy.applyEffect(Effect.invisible(100, DamageSink.none()));
        enemy.doTick(0);

        assertThat(badgeOf(enemy, EnemyFrameBuilder.CLOAK_FADE_DURATION_TICKS).alpha()).isZero();
    }

    @Test
    void dotsComeOncePerKindInAFixedOrderWhateverTheEffects() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50, 3);
        enemy.applyEffect(Effect.exposed(50, DamageSink.none()));
        enemy.applyEffect(Effect.vulnerable(1, 50, DamageSink.none()));
        enemy.applyEffect(Effect.sundered(1, 50, DamageSink.none()));

        assertThat(dotsOf(enemy).dots()).containsExactly(EnemyDotLook.DEBUFF, EnemyDotLook.SPOTTED);
    }

    @Test
    void anEffectWithAPictureOfItsOwnEarnsNoDot() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50, 3);
        enemy.applyEffect(Effect.burn(Damage.magic(10), 50, DamageSink.none()));

        assertThat(overlaysOf(enemy, 0)).noneMatch(EnemyDotsDraw.class::isInstance);
    }

    @Test
    void dotsSitBetweenTheBodyAndTheBars() {
        DefinedEnemyMob enemy = EnemyFixtures.liveMob(contextWithStraightPath(), EnemyFixtures.simpleDefinition("plain"));
        enemy.doDamage(Damage.physical(1));
        enemy.applyEffect(Effect.exposed(50, DamageSink.none()));

        float dotY = dotsOf(enemy).y();

        assertThat(dotY).isGreaterThan((float) enemy.getY() + enemy.getBodyScale());
        assertThat(barsOf(enemy, 0).getFirst().y()).isGreaterThan(dotY);
    }

    @Test
    void aCritSparkKeepsItsRotationForItsWholeLife() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 500, 3);
        enemy.doDamage(Damage.physical(10), AttackProfile.critChance(1f));
        enemy.doTick(1);

        CritSparkDraw first = critSparkOf(enemy, 1);
        CritSparkDraw later = critSparkOf(enemy, 1 + EnemyFrameBuilder.CRIT_SPARK_DURATION_TICKS);

        assertThat(later.rotation()).isEqualTo(first.rotation());
    }

    @Test
    void laterCritsTurnTheirSparksDifferently() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50000, 3);
        Set<Float> rotations = new HashSet<>();

        for (int tick = 1; tick <= 12; tick++) {
            enemy.doDamage(Damage.physical(1), AttackProfile.critChance(1f));
            enemy.doTick(tick);
            rotations.add(critSparkOf(enemy, tick).rotation());
        }

        assertThat(rotations).hasSizeGreaterThan(1);
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

    private static List<EnemyBurstDraw> burstsOf(EnemyMob enemy, int gameTime) {
        return overlaysOf(enemy, gameTime).stream()
                .filter(EnemyBurstDraw.class::isInstance)
                .map(EnemyBurstDraw.class::cast)
                .toList();
    }

    @Test
    void aShieldedEnemyAtFullHealthShowsItsEggAndOnlyAShieldBarOnTheHealthBarsScale() {
        EnemyMob enemy = shielded(10, 100);

        assertThat(eggOf(enemy, 0).flash()).isZero();
        assertThat(barsOf(enemy, 0).getFirst().bars()).containsExactly(new EnemyBarsDraw.Bar(EnemyBarLook.SHIELD, 10f / 50f));
    }

    @Test
    void aHurtShieldedEnemyShowsItsShieldBarAboveItsHealthBar() {
        EnemyMob enemy = shielded(100, 100);
        enemy.doDamage(Damage.magic(DamageUnits.ofPoints(1)), AttackProfile.none().asPeriodic());

        assertThat(barsOf(enemy, 0).getFirst().bars()).extracting(EnemyBarsDraw.Bar::look)
                .containsExactly(EnemyBarLook.SHIELD, EnemyBarLook.HEALTH);
    }

    @Test
    void theShieldBarStopsAtFullWidthAndNeverGetsTooNarrowToSee() {
        float full = barsOf(shielded(500, 100), 0).getFirst().bars().getFirst().fill();
        float sliver = barsOf(shielded(1, 100), 0).getFirst().bars().getFirst().fill();

        assertThat(full).isEqualTo(1f);
        assertThat(sliver).isGreaterThan(1f / 50f);
    }

    @Test
    void theEggAndItsBarGoWhenTheShieldRunsOutWithNoPop() {
        EnemyMob enemy = shielded(10, 1);

        enemy.doTick(1);

        assertThat(overlaysOf(enemy, 1)).noneMatch(EnemyEggDraw.class::isInstance);
        assertThat(barsOf(enemy, 1)).isEmpty();
        assertThat(burstsOf(enemy, 1)).isEmpty();
    }

    @Test
    void aHitLandingOnAShieldFlashesTheEggUntilItsWindowEnds() {
        EnemyMob enemy = shielded(100, 500);

        enemy.doDamage(Damage.physical(DamageUnits.ofPoints(1)));
        enemy.doTick(1);

        assertThat(eggOf(enemy, 1).flash()).isEqualTo(1f);
        assertThat(eggOf(enemy, 1 + EnemyFrameBuilder.SHIELD_FLASH_DURATION_TICKS).flash()).isZero();
    }

    @Test
    void damageThatGoesPastTheShieldsDoesNotFlashTheEgg() {
        EnemyMob enemy = shielded(100, 500);

        enemy.doDamage(Damage.magic(DamageUnits.ofPoints(1)), AttackProfile.none().asPeriodic());
        enemy.doTick(1);

        assertThat(eggOf(enemy, 1).flash()).isZero();
    }

    @Test
    void aHitBreakingTheLastShieldPopsTheEggForAWhile() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 500, 3);
        enemy.applyEffect(Effect.shield(DamageUnits.ofPoints(1), 500, DamageSink.none()));

        enemy.doDamage(Damage.physical(DamageUnits.ofPoints(5)));
        enemy.doTick(1);

        assertThat(overlaysOf(enemy, 1)).noneMatch(EnemyEggDraw.class::isInstance);
        assertThat(burstsOf(enemy, 1)).singleElement()
                .satisfies(burst -> assertThat(burst.progress()).isZero());
        assertThat(burstsOf(enemy, 1 + EnemyFrameBuilder.SHIELD_POP_DURATION_TICKS + 1)).isEmpty();
    }

    @Test
    void aHitBreakingOneShieldWhileAnotherHoldsDoesNotPop() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 500, 3);
        enemy.applyEffect(Effect.shield(DamageUnits.ofPoints(1), 100, DamageSink.none()).from(AttackOrigin.fresh()));
        enemy.applyEffect(Effect.shield(DamageUnits.ofPoints(50), 500, DamageSink.none()).from(AttackOrigin.fresh()));

        enemy.doDamage(Damage.physical(DamageUnits.ofPoints(5)));
        enemy.doTick(1);

        assertThat(overlaysOf(enemy, 1)).anyMatch(EnemyEggDraw.class::isInstance);
        assertThat(burstsOf(enemy, 1)).isEmpty();
    }

    @Test
    void barsAndTheBadgeClearTheEgg() {
        DefinedEnemyMob enemy = EnemyFixtures.liveMob(contextWithStraightPath(), EnemyFixtures.simpleDefinition("plain"), Rank.ELITE);
        enemy.applyEffect(Effect.shield(DamageUnits.ofPoints(10), 100, DamageSink.none()));

        EnemyEggDraw egg = eggOf(enemy, 0);

        assertThat(barsOf(enemy, 0).getFirst().y()).isGreaterThan(egg.y() + egg.radiusY() * 0.92f);
        assertThat(badgeOf(enemy).y()).isLessThan(egg.y() - egg.radiusY());
    }

    @Test
    void theSelectionRingSnapsJustOutsideTheEgg() {
        EnemyMob enemy = shielded(10, 100);
        EnemyFrameBuilder selected = new EnemyFrameBuilder(0, 0.0, Optional.of(enemy), 0.0);

        enemy.accept(selected);

        EnemyEggDraw egg = eggOf(enemy, 0);
        assertThat(selected.buildOverlays()).filteredOn(EnemyRingDraw.class::isInstance).map(EnemyRingDraw.class::cast)
                .singleElement().satisfies(ring -> assertThat(ring.radius()).isGreaterThan(egg.radiusY()));
    }

    private static EnemyFrameBuilder builtAt(EnemyMob enemy, int gameTime, double seconds) {
        EnemyFrameBuilder builder = new EnemyFrameBuilder(gameTime, 0.0, Optional.empty(), seconds);
        enemy.accept(builder);
        return builder;
    }

    private static List<EnemyEmissionDraw> emissionsOf(EnemyMob enemy, EnemyEmissionLook look) {
        return overlaysOf(enemy, 0).stream()
                .filter(EnemyEmissionDraw.class::isInstance)
                .map(EnemyEmissionDraw.class::cast)
                .filter(emission -> emission.look() == look)
                .toList();
    }

    private static List<EnemyTintDraw> tintsOf(EnemyMob enemy) {
        return overlaysOf(enemy, 0).stream()
                .filter(EnemyTintDraw.class::isInstance)
                .map(EnemyTintDraw.class::cast)
                .toList();
    }

    private static EnemyIceDraw iceOf(EnemyMob enemy) {
        return overlaysOf(enemy, 0).stream()
                .filter(EnemyIceDraw.class::isInstance)
                .map(EnemyIceDraw.class::cast)
                .findFirst().orElseThrow();
    }

    private static DefinedEnemyMob regenerating(float perTick) {
        return EnemyFixtures.liveMob(contextWithStraightPath(), EnemyFixtures.simpleDefinition("regen")
                .withHealthAndPrice(50, 3).withTraits(List.of(new RegenerationTrait(perTick))));
    }

    @Test
    void aFullerPoolEmitsMoreThanAFadingOne() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50000, 3);
        enemy.applyEffect(Effect.burn(Damage.magic(1000), 40, DamageSink.none()));

        float fresh = emissionsOf(enemy, EnemyEmissionLook.BURN).getFirst().amount();
        for (int tick = 1; tick <= 20; tick++) {
            enemy.doTick(tick);
        }
        float fading = emissionsOf(enemy, EnemyEmissionLook.BURN).getFirst().amount();

        assertThat(fading).isPositive().isLessThan(fresh);
    }

    @Test
    void soulfireAndPoisonEmitTheirOwnWayAndPoisonTintsTheBody() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50000, 3);
        enemy.applyEffect(Effect.soulfire(Damage.magic(100), 40, DamageSink.none()));
        enemy.applyEffect(Effect.poison(Damage.magic(100), 40, DamageSink.none()));

        assertThat(emissionsOf(enemy, EnemyEmissionLook.SOULFIRE)).hasSize(1);
        assertThat(emissionsOf(enemy, EnemyEmissionLook.POISON)).hasSize(1);
        assertThat(tintsOf(enemy)).extracting(EnemyTintDraw::tint).containsExactly(EnemyTintLook.POISON);
    }

    @Test
    void emissionsRunOnTheWallClockAndKeepToOneSeedForTheirEnemy() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50000, 3);
        enemy.applyEffect(Effect.burn(Damage.magic(100), 40, DamageSink.none()));

        EnemyEmissionDraw early = (EnemyEmissionDraw) builtAt(enemy, 0, 1.0).buildOverlays().getFirst();
        EnemyEmissionDraw later = (EnemyEmissionDraw) builtAt(enemy, 0, 2.5).buildOverlays().getFirst();

        assertThat(later.seconds()).isEqualTo(2.5f);
        assertThat(later.seed()).isEqualTo(early.seed());
    }

    @Test
    void aBleedDripsBeneathTheBodyOnlyWhileTheEnemyMoves() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50000, 3);
        enemy.applyEffect(Effect.bleeding(1, 100, DamageSink.none()));

        List<EnemyDraw> standing = drawsAt(enemy, 0);
        enemy.doTick(1);
        List<EnemyDraw> walking = drawsAt(enemy, 1);
        enemy.applyEffect(Effect.freeze(50, DamageSink.none()));
        enemy.doTick(2);
        enemy.doTick(3);
        List<EnemyDraw> held = drawsAt(enemy, 3);

        assertThat(standing).hasSize(1);
        assertThat(walking.getFirst()).isInstanceOf(EnemyEmissionDraw.class);
        assertThat(walking.getLast()).isInstanceOf(EnemyBodyDraw.class);
        assertThat(held).hasSize(1);
    }

    @Test
    void plusSignsRiseOnlyWhileHealthComesBackAndFasterHealingEmitsMore() {
        DefinedEnemyMob slow = regenerating(1f);
        DefinedEnemyMob fast = regenerating(4f);

        assertThat(emissionsOf(slow, EnemyEmissionLook.HEAL)).isEmpty();

        slow.doDamage(Damage.physical(DamageUnits.ofPoints(10)));
        fast.doDamage(Damage.physical(DamageUnits.ofPoints(10)));

        float slowAmount = emissionsOf(slow, EnemyEmissionLook.HEAL).getFirst().amount();
        float fastAmount = emissionsOf(fast, EnemyEmissionLook.HEAL).getFirst().amount();
        assertThat(fastAmount).isGreaterThan(slowAmount);
    }

    @Test
    void slowRegenerationKeepsEmittingBetweenTheUnitsItLandsIn() {
        DefinedEnemyMob slow = regenerating(0.1f);
        slow.doDamage(Damage.physical(DamageUnits.ofPoints(10)));

        for (int tick = 1; tick <= 12; tick++) {
            slow.doTick(tick);
            assertThat(overlaysOf(slow, tick)).anyMatch(EnemyEmissionDraw.class::isInstance);
        }
    }

    @Test
    void noPlusSignsRiseUnderInversionThoughItRegenerates() {
        DefinedEnemyMob enemy = regenerating(1f);
        enemy.doDamage(Damage.physical(DamageUnits.ofPoints(10)));

        enemy.applyEffect(Effect.hex(EffectKind.INVERSION, 80, DamageSink.none()));

        assertThat(emissionsOf(enemy, EnemyEmissionLook.HEAL)).isEmpty();
    }

    @Test
    void noPlusSignsRiseWhileItCrumbles() {
        DefinedEnemyMob enemy = regenerating(-1f);
        enemy.doDamage(Damage.physical(DamageUnits.ofPoints(10)));

        assertThat(emissionsOf(enemy, EnemyEmissionLook.HEAL)).isEmpty();
    }

    @Test
    void aDeeperChillTintsAndEmitsMoreStrongly() {
        EnemyMob shallow = EnemyFixtures.plainMob(contextWithStraightPath(), 50, 3);
        EnemyMob deep = EnemyFixtures.plainMob(contextWithStraightPath(), 50, 3);
        shallow.applyEffect(Effect.chill(0.2f, 50, DamageSink.none()));
        deep.applyEffect(Effect.chill(0.7f, 50, DamageSink.none()));

        assertThat(tintsOf(deep).getFirst().strength()).isGreaterThan(tintsOf(shallow).getFirst().strength());
        assertThat(emissionsOf(deep, EnemyEmissionLook.CHILL).getFirst().amount())
                .isGreaterThan(emissionsOf(shallow, EnemyEmissionLook.CHILL).getFirst().amount());
    }

    @Test
    void aFrozenEnemyIsInsideOneBlockOfIceForItsWholeLifeAndEnemiesGetDifferentBlocks() {
        GameWorld context = contextWithStraightPath();
        Set<Integer> variants = new HashSet<>();
        for (int i = 0; i < 6; i++) {
            EnemyMob enemy = EnemyFixtures.plainMob(context, 50, 3);
            enemy.applyEffect(Effect.freeze(5, DamageSink.none()));
            assertThat(iceOf(enemy).variant()).isEqualTo(iceOf(enemy).variant());
            variants.add(iceOf(enemy).variant());
        }

        assertThat(variants).hasSizeGreaterThan(1);
    }

    @Test
    void barsAndTheBadgeClearTheIce() {
        DefinedEnemyMob enemy = EnemyFixtures.liveMob(contextWithStraightPath(), EnemyFixtures.simpleDefinition("plain"), Rank.ELITE);
        enemy.doDamage(Damage.physical(1));
        enemy.applyEffect(Effect.freeze(5, DamageSink.none()));
        EnemyIceDraw ice = iceOf(enemy);

        assertThat(barsOf(enemy, 0).getFirst().y()).isGreaterThan(ice.y() + EnemyEffects.iceBeneath(ice.variant(), ice.scale()));
        assertThat(badgeOf(enemy).y()).isLessThan(ice.y() - EnemyEffects.iceAbove(ice.variant(), ice.scale()));
    }

    @Test
    void iceThrowsShardsWhenAFreezeEndsAndThenTheyGo() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50, 3);
        enemy.applyEffect(Effect.freeze(2, DamageSink.none()));

        assertThat(burstsOf(enemy, 0)).isEmpty();
        enemy.doTick(1);
        enemy.doTick(2);

        assertThat(overlaysOf(enemy, 2)).noneMatch(EnemyIceDraw.class::isInstance);
        assertThat(burstsOf(enemy, 2)).singleElement()
                .satisfies(burst -> assertThat(burst.look()).isEqualTo(EnemyBurstLook.ICE_SHARDS));
        assertThat(burstsOf(enemy, 2 + EnemyFrameBuilder.ICE_SHARDS_DURATION_TICKS + 1)).isEmpty();
    }

    @Test
    void aDazedEnemySwaysWithStarsOverItsHead() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50, 3);
        enemy.applyEffect(Effect.dazed(50, DamageSink.none()));

        double first = ((EnemyBodyDraw) builtAt(enemy, 0, 1.0).build().getFirst()).facingRadians();
        double second = ((EnemyBodyDraw) builtAt(enemy, 0, 1.2).build().getFirst()).facingRadians();

        assertThat(first).isNotEqualTo(second);
        assertThat(emissionsOf(enemy, EnemyEmissionLook.DAZE)).hasSize(1);
    }

    @Test
    void aCloakedEnemysEmissionsAndEggGiveNothingAway() {
        EnemyMob enemy = EnemyFixtures.plainMob(contextWithStraightPath(), 50000, 3);
        enemy.applyEffect(Effect.invisible(100, DamageSink.none()));
        enemy.applyEffect(Effect.poison(Damage.magic(1), 100, DamageSink.none()));
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
        enemy.applyEffect(Effect.burn(Damage.magic(10000), 100, DamageSink.none()));
        enemy.doTick(0);

        List<EnemyOverlayDraw> overlays = overlaysOf(enemy, EnemyFrameBuilder.CLOAK_FADE_DURATION_TICKS);

        assertThat(overlays).filteredOn(EnemyEmissionDraw.class::isInstance).map(EnemyEmissionDraw.class::cast)
                .singleElement().satisfies(flames -> assertThat(flames.alpha()).isEqualTo(1f));
    }

    @Test
    void whatAnEnemyCarriesIsDrawnBackToFrontWithItsMarksOverEveryEffect() {
        DefinedEnemyMob enemy = EnemyFixtures.liveMob(contextWithStraightPath(),
                EnemyFixtures.simpleDefinition("plain").withHealthAndPrice(5000, 3), Rank.ELITE);
        enemy.doDamage(Damage.physical(DamageUnits.ofPoints(1)));
        enemy.applyEffect(Effect.poison(Damage.magic(1), 100, DamageSink.none()));
        enemy.applyEffect(Effect.chill(0.2f, 100, DamageSink.none()));
        enemy.applyEffect(Effect.burn(Damage.magic(1), 100, DamageSink.none()));
        enemy.applyEffect(Effect.shield(DamageUnits.ofPoints(1), 100, DamageSink.none()));
        enemy.applyEffect(Effect.exposed(100, DamageSink.none()));

        List<String> kinds = overlaysOf(enemy, 0).stream().map(overlay -> overlay.getClass().getSimpleName()).toList();

        assertThat(kinds.indexOf("EnemyEggDraw")).isGreaterThan(kinds.lastIndexOf("EnemyEmissionDraw"));
        assertThat(kinds.indexOf("RankBadgeDraw")).isGreaterThan(kinds.indexOf("EnemyEggDraw"));
        assertThat(kinds.indexOf("EnemyDotsDraw")).isGreaterThan(kinds.indexOf("RankBadgeDraw"));
        assertThat(kinds.indexOf("EnemyBarsDraw")).isGreaterThan(kinds.indexOf("EnemyDotsDraw"));
    }

    @Test
    void aSupportAuraRingShowsAtItsAuthoredRadiusOnlyWhileItsEnemyIsSelected() {
        GameWorld context = contextWithStraightPath();
        EnemyMob shrouder = EnemyFixtures.liveMob(context, EnemyFixtures.simpleDefinition("shrouder")
                .withAbilities(List.of(new Ability(AbilityTerm.SHROUD, new PeriodicTrigger(40),
                        new ApplyEffectAction(new InvisibleTemplate(60), new RadiusTarget(100f))))));
        EnemyFrameBuilder selected = new EnemyFrameBuilder(0, 0.0, Optional.of(shrouder), 0.0);

        shrouder.accept(selected);

        assertThat(overlaysOf(shrouder, 0)).isEmpty();
        assertThat(selected.buildOverlays()).filteredOn(EnemyRingDraw.class::isInstance).map(EnemyRingDraw.class::cast)
                .anySatisfy(ring -> {
                    assertThat(ring.radius()).isEqualTo(100f);
                    assertThat(ring.palette()).isEqualTo(Palette.STATUS_MARKER_INVISIBLE);
                });
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
