package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageMix;
import td.effect.EffectKind;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AffixTest {

    private static final AffixContext WHOLE = new AffixContext(Rank.GRUNT, 1f);
    private static final Damage LETHAL = Damage.physical(1_000_000_000);
    private static final int SECONDS_3 = Math.round(3 * TickRate.TICKS_PER_SECOND);

    private final GameWorld world = WorldFixtures.newWorldOnStraightPath();
    private final EnemyDefinition grub = EnemyFixtures.simpleDefinition("grub").withDisplayName("Grub");

    @Test
    void anAffixsTraitStacksWithANamedTraitTheEnemyAlreadyHas() {
        EnemyDefinition named = this.grub
                .withIdentifiedTraits(List.of(IdentifiedTrait.named("armor", new RegenerationTrait(1f))));

        EnemyDefinition affixed = named.withAffixes(List.of(Affix.ARMORED), WHOLE);

        assertThat(affixed.traitsFor(DamageMix.none())).hasSize(2);
    }

    @Test
    void aDefinitionRecordsItsAffixesAndItsTitleNamesThemBeforeTheNoun() {
        EnemyDefinition affixed = this.grub.withAffixes(List.of(Affix.ARMORED), WHOLE);

        assertThat(affixed.affixes()).containsExactly(Affix.ARMORED);
        assertThat(affixed.title()).isEqualTo(Affix.ARMORED.title() + " Grub");
        assertThat(affixed.displayName()).isEqualTo("Grub");
        assertThat(this.grub.title()).isEqualTo("Grub");
    }

    @Test
    void theTitleKeepsTheWrittenOrderWhateverOrderTheAffixesApplyIn() {
        EnemyDefinition affixed = this.grub.withAffixes(List.of(Affix.WITHERED, Affix.HULKING), WHOLE);

        assertThat(affixed.title()).isEqualTo(Affix.WITHERED.title() + " " + Affix.HULKING.title() + " Grub");
    }

    @Test
    void aTraceNamesTheEnemyWithItsAffixes() {
        EnemyDefinition affixed = this.grub.withAffixes(List.of(Affix.ARMORED), WHOLE);

        DefinedEnemyMob mob = EnemyFixtures.liveMob(this.world, affixed);

        assertThat(mob.label().name()).isEqualTo(affixed.title());
    }

    @Test
    void onlyATauntingAffixMakesAnEnemyTaunt() {
        EnemyDefinition taunting = this.grub.withAffixes(List.of(Affix.ARMORED, Affix.TAUNTING), WHOLE);
        EnemyDefinition armored = this.grub.withAffixes(List.of(Affix.ARMORED), WHOLE);

        assertThat(EnemyFixtures.liveMob(this.world, taunting).taunts()).isTrue();
        assertThat(EnemyFixtures.liveMob(this.world, armored).taunts()).isFalse();
    }

    @Test
    void hulkingRaisesHealthLowersSpeedAndGrowsTheBodyButNotTheBounty() {
        EnemyDefinition hulking = this.grub.withAffixes(List.of(Affix.HULKING), WHOLE);

        assertThat(hulking.baseHealth()).isGreaterThan(this.grub.baseHealth());
        assertThat(hulking.baseSpeed()).isLessThan(this.grub.baseSpeed());
        assertThat(hulking.sizeMultiplier()).isGreaterThan(1f);
        assertThat(hulking.price()).isEqualTo(this.grub.price());
    }

    @Test
    void aShareOfHealthAffixMeasuresTheHealthOneMemberIsSentWith() {
        float whole = regeneration(this.grub.withAffixes(List.of(Affix.REGENERATING), WHOLE));
        float quarter = regeneration(this.grub.withAffixes(List.of(Affix.REGENERATING),
                new AffixContext(Rank.GRUNT, 0.25f)));

        assertThat(quarter).isCloseTo(whole / 4f, within(0.01f));
    }

    @Test
    void hulkingAppliesFirstSoAShareOfHealthMeasuresTheHulkingHealth() {
        EnemyDefinition hulking = this.grub.withAffixes(List.of(Affix.HULKING), WHOLE);
        float plain = regeneration(this.grub.withAffixes(List.of(Affix.REGENERATING), WHOLE));

        float both = regeneration(this.grub.withAffixes(List.of(Affix.REGENERATING, Affix.HULKING), WHOLE));

        assertThat(both).isCloseTo(plain * hulking.baseHealth() / this.grub.baseHealth(), within(0.01f));
    }

    @Test
    void exaltedDoublesHowFarEachStatIsFromItsDefaultAndLeavesSpeed() {
        EnemyDefinition sheet = this.grub.withArmor(10f).withAgility(-20f);

        EnemyDefinition exalted = sheet.withAffixes(List.of(Affix.EXALTED), WHOLE);

        assertThat(exalted.baseStats().value(EnemyStat.ARMOR)).isEqualTo(20f);
        assertThat(exalted.baseStats().value(EnemyStat.AGILITY)).isEqualTo(-40f);
        assertThat(exalted.baseStats().value(EnemyStat.MAGIC_RESIST)).isEqualTo(EnemyStat.MAGIC_RESIST.defaultBase());
        assertThat(exalted.baseSpeed()).isEqualTo(sheet.baseSpeed());
    }

    @Test
    void exaltedHasNothingToDoOnAnEnemyWithEveryStatAtItsDefault() {
        assertThat(Affix.EXALTED.problemWith(this.grub)).isPresent();
        assertThat(Affix.EXALTED.problemWith(this.grub.withArmor(10f))).isEmpty();
    }

    @Test
    void shieldedRaisesAShieldOnceBelowHalfItsHealth() {
        DefinedEnemyMob mob = EnemyFixtures.liveMob(this.world, this.grub.withAffixes(List.of(Affix.SHIELDED), WHOLE));

        mob.doDamage(Damage.physical(mob.getHealth() * 6 / 10));
        mob.doTick(1);

        assertThat(mob.hasEffect(EffectKind.SHIELD)).isTrue();
    }

    @Test
    void witheredMovesFasterAndComesApartOnItsOwn() {
        EnemyDefinition withered = this.grub.withAffixes(List.of(Affix.WITHERED), WHOLE);
        DefinedEnemyMob mob = EnemyFixtures.liveMob(this.world, withered);
        int before = mob.getHealth();

        EnemyFixtures.tickEnemies(this.world, 1, 100);

        assertThat(withered.baseSpeed()).isGreaterThan(this.grub.baseSpeed());
        assertThat(mob.getHealth()).isLessThan(before);
    }

    @Test
    void anAllyBesideACommanderIsGivenResolveAndLosesItAfterTheCommanderFalls() {
        DefinedEnemyMob commander = EnemyFixtures.liveMob(this.world, this.grub.withAffixes(List.of(Affix.COMMANDING), WHOLE));
        DefinedEnemyMob ally = EnemyFixtures.liveMob(this.world, this.grub);

        EnemyFixtures.tickEnemies(this.world, 1, SECONDS_3);
        boolean heldWhileItLived = ally.hasEffect(EffectKind.RESOLVE);
        commander.doDamage(LETHAL);
        EnemyFixtures.tickEnemies(this.world, SECONDS_3 + 1, 3 * SECONDS_3);

        assertThat(heldWhileItLived).isTrue();
        assertThat(commander.hasEffect(EffectKind.RESOLVE)).isFalse();
        assertThat(ally.hasEffect(EffectKind.RESOLVE)).isFalse();
    }

    @Test
    void anAllyBesideAVengefulEnemySprintsAsItDiesAndOneOutOfReachDoesNot() {
        DefinedEnemyMob vengeful = EnemyFixtures.liveMob(this.world, this.grub.withAffixes(List.of(Affix.VENGEFUL), WHOLE));
        DefinedEnemyMob near = EnemyFixtures.liveMob(this.world, this.grub);
        DefinedEnemyMob far = EnemyFixtures.liveMob(this.world, this.grub.withBaseSpeed(20f));
        EnemyFixtures.tickEnemies(this.world, 1, 20);

        vengeful.doDamage(LETHAL);
        EnemyFixtures.tickEnemies(this.world, 21, 21);

        assertThat(near.hasEffect(EffectKind.SPRINTING)).isTrue();
        assertThat(far.hasEffect(EffectKind.SPRINTING)).isFalse();
    }

    @Test
    void aCommandersAndAVengefulEnemysReachIsTheRingTheBoardDraws() {
        assertThat(this.grub.withAffixes(List.of(Affix.COMMANDING), WHOLE).supportAura())
                .hasValueSatisfying(aura -> assertThat(aura.kind()).isEqualTo(EffectKind.RESOLVE));
        assertThat(this.grub.withAffixes(List.of(Affix.VENGEFUL), WHOLE).supportAura())
                .hasValueSatisfying(aura -> assertThat(aura.kind()).isEqualTo(EffectKind.SPRINTING));
    }

    private static float regeneration(EnemyDefinition definition) {
        return definition.traitsFor(DamageMix.none()).stream()
                .filter(RegenerationTrait.class::isInstance)
                .map(trait -> ((RegenerationTrait) trait).pointsPerSecond())
                .findFirst().orElseThrow();
    }
}
