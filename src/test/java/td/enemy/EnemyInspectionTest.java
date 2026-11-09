package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.ShieldTemplate;
import td.enemy.EnemyInspection.EffectState;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
import td.stat.StatBreakdown;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EnemyInspectionTest {

    private final GameWorld world = WorldFixtures.newWorld();

    private DefinedEnemyMob anEnemy() {
        return EnemyFixtures.plainMob(this.world, 100, 5);
    }

    private static EffectState stateOf(DefinedEnemyMob mob, EffectKind kind) {
        return mob.inspect().effects().stream().filter(state -> state.kind() == kind).findFirst().orElseThrow();
    }

    @Test
    void effectsAreListedInTheOrderTheyWereGained() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.chill(0.4f, 100, DamageSink.none()));
        mob.doTick(1);
        mob.applyEffect(Effect.burn(Damage.magic(10), 100, DamageSink.none()));
        mob.doTick(2);

        List<EffectKind> kinds = mob.inspect().effects().stream().map(EffectState::kind).toList();

        assertThat(kinds).containsSubsequence(EffectKind.CHILL, EffectKind.BURN);
    }

    @Test
    void aFreezeThatEndedStaysFadedWhileItsMeterRuns() {
        DefinedEnemyMob mob = this.anEnemy();
        int oneSecond = Math.round(TickRate.TICKS_PER_SECOND);
        mob.applyEffect(Effect.freeze(oneSecond, DamageSink.none()));
        for (int tick = 1; tick <= oneSecond; tick++) {
            mob.doTick(tick);
        }

        EffectState freeze = stateOf(mob, EffectKind.FREEZE);

        assertThat(freeze.faded()).isTrue();
        assertThat(freeze.nextShare().orElseThrow()).isCloseTo(0.5, within(0.001));
    }

    @Test
    void aFreezeLeavesTheListOnceItsMeterEmpties() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.freeze(1, DamageSink.none()));
        for (int tick = 1; tick <= 3; tick++) {
            mob.doTick(tick);
        }

        assertThat(mob.inspect().effects()).extracting(EffectState::kind).doesNotContain(EffectKind.FREEZE);
    }

    @Test
    void aBurnReportsHowFullItsPoolIs() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.burn(Damage.magic(10), 100, DamageSink.none()));

        assertThat(stateOf(mob, EffectKind.BURN).fullness().orElseThrow()).isBetween(0.0, 1.0);
    }

    @Test
    void aTimedEffectReportsTheTicksItsClockStartedFrom() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.vulnerable(1, 100, DamageSink.none()));
        mob.doTick(1);

        assertThat(stateOf(mob, EffectKind.VULNERABLE).clockTicks().orElseThrow()).isGreaterThanOrEqualTo(99);
    }

    @Test
    void anEffectKeptOutNamesWhatKeepsItOut() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.revealed(30, DamageSink.none()));

        assertThat(mob.inspect().immunities()).contains(
                new EnemyInspection.Immunity(EffectKind.INVISIBLE, Optional.of(EffectKind.REVEALED)));
    }

    @Test
    void theArmorBreakdownListsBaseEachSourceAndResult() {
        DefinedEnemyMob mob = EnemyFixtures.liveMob(this.world, EnemyFixtures.simpleDefinition("armored").withArmor(30f));
        float base = mob.baseStat(EnemyStat.ARMOR);
        mob.applyEffect(Effect.sundered(2, 100, DamageSink.none()));

        StatBreakdown armor = mob.inspect().breakdowns().get(EnemyStat.ARMOR);

        assertThat(armor.base()).isEqualTo(base);
        assertThat(armor.added()).extracting(part -> part.source().mentions())
                .containsExactly(List.of(EffectKind.SUNDERED));
        assertThat(armor.added().getFirst().stacks()).isEqualTo(2);
        assertThat(armor.current()).isCloseTo(Math.max(0f, base - 10f), within(0.001f));
    }

    @Test
    void aSpeedBreakdownListsOnlyTheSlowThatCounts() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.tarred(100, DamageSink.none()));
        float strongest = EffectKind.TARRED.rule().slowPerStack() + 0.1f;
        mob.applyEffect(Effect.chill(strongest, 100, DamageSink.none()));

        StatBreakdown speed = mob.inspect().breakdowns().get(EnemyStat.MOVE_SPEED);

        assertThat(speed.added()).isEmpty();
        assertThat(speed.scaled()).hasSize(1);
        assertThat(speed.scaled().getFirst().source().mentions()).containsExactly(EffectKind.CHILL);
        assertThat(speed.scaled().getFirst().modifier().multiply()).isCloseTo(1f - strongest, within(0.001f));
    }

    @Test
    void anInspectionCarriesTheAffixesInTheOrderWritten() {
        EnemyDefinition affixed = EnemyFixtures.simpleDefinition("grub")
                .withAffixes(List.of(Affix.WITHERED, Affix.HULKING), new AffixContext(Rank.GRUNT, 1f));

        EnemyInspection inspection = EnemyFixtures.liveMob(this.world, affixed).inspect();

        assertThat(inspection.affixes()).containsExactly(Affix.WITHERED, Affix.HULKING);
    }

    @Test
    void abilitiesAndTraitsBecomeNamedEntries() {
        EnemyDefinition definition = EnemyFixtures.simpleDefinition("bracer")
                .withAbilities(List.of(new Ability(AbilityTerm.BRACE, new HealthThresholdTrigger(0.5f),
                        new ApplyEffectAction(new ShieldTemplate(10f, 100), new SelfTarget()))))
                .withTraits(List.of(new ImmunityTrait(EffectKind.FREEZE)));

        EnemyInspection inspection = EnemyFixtures.liveMob(this.world, definition).inspect();

        assertThat(inspection.abilityEntries()).extracting(AbilityEntry::term).containsExactly(AbilityTerm.BRACE);
        assertThat(inspection.traitEntries()).anyMatch(entry -> entry.term() == EffectKind.FREEZE && entry.immune());
    }

    @Test
    void onlyStatsOffTheirDefaultShowSpeedAlwaysAndStealthNever() {
        assertThat(StatRows.shows(EnemyStat.MOVE_SPEED, 1f, 1f)).isTrue();
        assertThat(StatRows.shows(EnemyStat.ARMOR, EnemyStat.ARMOR.defaultBase(), EnemyStat.ARMOR.defaultBase()))
                .isFalse();
        assertThat(StatRows.shows(EnemyStat.ARMOR, 10f, 10f)).isTrue();
        assertThat(StatRows.shows(EnemyStat.TENACITY, 10f, 10f)).isTrue();
        assertThat(StatRows.shows(EnemyStat.STEALTH, 1f, 1f)).isFalse();
    }
}
