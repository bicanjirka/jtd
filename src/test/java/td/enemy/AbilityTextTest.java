package td.enemy;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.effect.HealTemplate;
import td.effect.ShieldTemplate;
import td.zone.Zone;

import static org.assertj.core.api.Assertions.assertThat;

class AbilityTextTest {

    private static final float CELL = 40f;

    private final AbilityText.Words words = new AbilityText.Words(CELL, id -> "Broodling");

    private String plain(Ability ability) {
        return AbilityText.of(ability, this.words).plain();
    }

    @Test
    void aThresholdShieldSaysWhenWhatHowMuchAndAgainstWhat() {
        Ability brace = new Ability(AbilityTerm.BRACE, new HealthThresholdTrigger(0.5f),
                new ApplyEffectAction(ShieldTemplate.physicalOnly(300f, 100), new SelfTarget()));

        assertThat(this.plain(brace)).isEqualTo("once, below 50% health: gains Shield of 300 against physical for 5 s");
        assertThat(AbilityText.of(brace, this.words).mentions()).containsExactly(EffectKind.SHIELD);
    }

    @Test
    void aShieldThatLastsSaysItHoldsUntilBroken() {
        Ability shield = new Ability(AbilityTerm.BRACE, new HealthThresholdTrigger(0.5f),
                new ApplyEffectAction(ShieldTemplate.untilBroken(300f), new SelfTarget()));

        assertThat(this.plain(shield)).isEqualTo("once, below 50% health: gains Shield of 300 until broken");
    }

    @Test
    void aPeriodicAbilityWithAHealthFloorSaysAboveWhatItKeepsFiring() {
        Ability shield = new Ability(AbilityTerm.WARD, new PeriodicTrigger(40, 0.25f),
                new ApplyEffectAction(new ShieldTemplate(300f, 100), new SelfTarget()));

        assertThat(this.plain(shield)).startsWith("every 2 s, while above 25% health: ");
    }

    @Test
    void anAreaHealSaysHowFarItReachesInCells() {
        Ability mend = new Ability(AbilityTerm.MEND, new PeriodicTrigger(20),
                new ApplyEffectAction(new HealTemplate(1f, 40), new RadiusTarget(2 * CELL)));

        assertThat(this.plain(mend)).startsWith("every ").contains("every other enemy within 2 cells Heal of");
    }

    @Test
    void aSpawnNamesWhatItCalls() {
        Ability brood = new Ability(AbilityTerm.BROOD, new OnDeathTrigger(), new SpawnEnemiesAction("broodling", 3, false));

        assertThat(this.plain(brood)).isEqualTo("when it dies: calls 3 Broodling");
    }

    @Test
    void aBlindingBurstSaysHowFarItReachesHowMuchItBlindsAndHowLong() {
        Ability blast = new Ability(AbilityTerm.BLINDING_BURST, new OnDeathTrigger(),
                LeaveZoneAction.blindingField(2f, 0.75f));

        assertThat(this.plain(blast))
                .isEqualTo("when it dies: blinds the towers within 2 cells: 75% miss chance, until the wave is over");
    }

    @Test
    void aHealingPoolNamesTheHealItGivesAndHowLongItLasts() {
        Ability pool = new Ability(AbilityTerm.HEALING_POOL, new PeriodicTrigger(60),
                LeaveZoneAction.healingGround(1.5f, 100, 0.4f));

        assertThat(this.plain(pool))
                .startsWith("every 3 s: leaves ground 1.5 cells wide that gives enemies on it Heal of")
                .endsWith("a second, for 5 s");
        assertThat(AbilityText.of(pool, this.words).mentions()).containsExactly(EffectKind.HEAL);
    }

    @Test
    void aHasteFieldNamesTheHasteItGives() {
        Ability field = new Ability(AbilityTerm.HASTE_FIELD, new HealthThresholdTrigger(0.5f),
                LeaveZoneAction.hasteGround(1.5f, Zone.UNTIL_WAVE_END));

        assertThat(AbilityText.of(field, this.words).mentions()).containsExactly(EffectKind.HASTENED);
        assertThat(this.plain(field)).endsWith("until the wave is over");
    }
}
