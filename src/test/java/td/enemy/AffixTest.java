package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageMix;
import td.effect.EffectKind;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AffixTest {

    private static final AffixContext WHOLE = new AffixContext(Rank.GRUNT, 1f);

    private final GameWorld world = WorldFixtures.newWorld();
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

    private static float regeneration(EnemyDefinition definition) {
        return definition.traitsFor(DamageMix.none()).stream()
                .filter(RegenerationTrait.class::isInstance)
                .map(trait -> ((RegenerationTrait) trait).pointsPerSecond())
                .findFirst().orElseThrow();
    }
}
