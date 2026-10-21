package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyInspection.EffectState;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EnemyInspectionTest {

    private final GameWorld world = WorldFixtures.newWorld();

    private DefinedEnemyMob spawn(String id, Rank rank) {
        return (DefinedEnemyMob) EnemyCatalog.builtIn().spawn(id, this.world, 0, 100, 5, rank);
    }

    private static EffectState stateOf(DefinedEnemyMob mob, EffectKind kind) {
        return mob.inspect().effects().stream().filter(state -> state.kind() == kind).findFirst().orElseThrow();
    }

    @Test
    void effectsAreListedInTheOrderTheyWereGained() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.chill(0.4f, 100, DamageSink.none()));
        mob.doTick(1);
        mob.applyEffect(Effect.burn(Damage.magic(10), 100, DamageSink.none()));
        mob.doTick(2);

        List<EffectKind> kinds = mob.inspect().effects().stream().map(EffectState::kind).toList();

        assertThat(kinds).containsSubsequence(EffectKind.CHILL, EffectKind.BURN);
    }

    @Test
    void aFreezeThatEndedStaysFadedWhileItsDiminishingReturnsRun() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.freeze(3, DamageSink.none()));
        for (int tick = 1; tick <= 6; tick++) {
            mob.doTick(tick);
        }

        EffectState freeze = stateOf(mob, EffectKind.FREEZE);

        assertThat(freeze.faded()).isTrue();
        assertThat(freeze.nextShare().orElseThrow()).isEqualTo(0.5);
    }

    @Test
    void aBurnReportsHowFullItsPoolIs() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.burn(Damage.magic(10), 100, DamageSink.none()));

        assertThat(stateOf(mob, EffectKind.BURN).fullness().orElseThrow()).isBetween(0.0, 1.0);
    }

    @Test
    void aTimedEffectReportsTheTicksItsClockStartedFrom() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.vulnerable(1, 100, DamageSink.none()));
        mob.doTick(1);

        assertThat(stateOf(mob, EffectKind.VULNERABLE).clockTicks().orElseThrow()).isGreaterThanOrEqualTo(99);
    }

    @Test
    void anEffectKeptOutNamesWhatKeepsItOut() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.freeze(30, DamageSink.none()));

        assertThat(mob.inspect().immunities()).contains(
                new EnemyInspection.Immunity(EffectKind.BURN, java.util.Optional.of(EffectKind.FREEZE)));
    }

    @Test
    void theArmorBreakdownListsBaseEachSourceAndResult() {
        DefinedEnemyMob mob = this.spawn("s", Rank.ELITE);
        float base = mob.baseStat(EnemyStat.ARMOR);
        mob.applyEffect(Effect.sundered(2, 100, DamageSink.none()));

        StatBreakdown armor = mob.inspect().breakdowns().get(EnemyStat.ARMOR);

        assertThat(armor.base()).isEqualTo(base);
        assertThat(armor.parts()).extracting(StatPart::source).containsExactly(EffectKind.SUNDERED);
        assertThat(armor.current()).isCloseTo(Math.max(0f, base - 10f), within(0.001f));
    }

    @Test
    void aSpeedBreakdownRecordsChillAsAMultiplier() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.chill(0.4f, 100, DamageSink.none()));

        StatBreakdown speed = mob.inspect().breakdowns().get(EnemyStat.MOVE_SPEED);

        assertThat(speed.parts()).hasSize(1);
        assertThat(speed.parts().getFirst().source()).isEqualTo(EffectKind.CHILL);
        assertThat(speed.parts().getFirst().modifier().multiply()).isCloseTo(0.6f, within(0.001f));
    }

    @Test
    void abilitiesAndTraitsBecomeNamedEntries() {
        EnemyInspection armored = this.spawn("s", Rank.BOSS).inspect();
        EnemyInspection yeti = this.spawn("yeti", Rank.ELITE).inspect();

        assertThat(armored.abilityEntries()).extracting(AbilityEntry::term)
                .contains(AbilityTerm.BRACE, AbilityTerm.REINFORCE);
        assertThat(yeti.traitEntries()).anyMatch(entry -> entry.term() == EffectKind.FREEZE && entry.immune());
    }

    @Test
    void onlyStatsOffTheirDefaultShowAndSpeedAlways() {
        assertThat(StatRows.shows(EnemyStat.MOVE_SPEED, 1f, 1f)).isTrue();
        assertThat(StatRows.shows(EnemyStat.ARMOR, EnemyStat.ARMOR.defaultBase(), EnemyStat.ARMOR.defaultBase()))
                .isFalse();
        assertThat(StatRows.shows(EnemyStat.ARMOR, 10f, 10f)).isTrue();
        assertThat(StatRows.shows(EnemyStat.FREEZE_RESIST, 1f, 1f)).isFalse();
    }
}
