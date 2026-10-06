package td.ui;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.Effect;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyFactory;
import td.enemy.Rank;
import td.fixtures.WorldFixtures;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.RankBadge;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnemyStatTextTest {

    private final GameWorld world = WorldFixtures.newWorld();

    private InfoSheet hoverSheet(String id, Rank rank) {
        EnemyDefinition definition = EnemyCatalog.builtIn().ranked(id).definitionFor(rank);
        return PanelEnemy.previewMob(definition, rank, this.world).accept(new EnemyInfoText());
    }

    private DefinedEnemyMob spawn(String id, Rank rank) {
        return (DefinedEnemyMob) EnemyFactory.getEnemy(id, this.world, 0, 100, 5, rank);
    }

    private static List<String> labels(InfoSheet sheet) {
        return sheet.lines().stream().filter(Row.class::isInstance).map(Row.class::cast).map(Row::label).toList();
    }

    @Test
    void aPlainEnemyShowsOnlyItsSpeed() {
        InfoSheet sheet = this.hoverSheet("c", Rank.GRUNT);

        assertThat(labels(sheet)).containsExactly("Speed");
    }

    @Test
    void theHeaderShowsTheBodyGlyphNameAndRankBadge() {
        InfoSheet sheet = this.hoverSheet("s", Rank.BOSS);

        assertThat(sheet.lines().getFirst())
                .isEqualTo(new SheetLine.EnemyHeader(Palette.ENEMY_SQUARE, RankBadge.SKULL, "Armored mob", "Boss"));
    }

    @Test
    void aTraitAndTheStatsItSetsShowAsOneRowInTheTraitsBoardColour() {
        InfoSheet sheet = this.hoverSheet("s", Rank.GRUNT);

        assertThat(sheet.lines()).contains(Row.trait(Palette.TRAIT_MARKER_PERCENT_RESIST, "Resist all", "-20%"),
                Row.trait(Palette.TRAIT_MARKER_CRITICAL_IMMUNE, "Crit immune", ""));
        assertThat(labels(sheet)).containsExactly("Speed", "Resist all", "Crit immune");
    }

    @Test
    void platingOnOneDamageTypeNamesItAndShowsThePointsAHitLoses() {
        InfoSheet sheet = this.hoverSheet("s", Rank.ELITE);

        assertThat(sheet.lines()).contains(Row.trait(Palette.TRAIT_MARKER_FLAT_RESIST, "Plating, physical", "-8/hit"),
                Row.trait(Palette.TRAIT_MARKER_PERCENT_RESIST, "Resist all", "-40%"));
    }

    @Test
    void aFrozenEnemyShowsThatItIsImmuneToBurnAndWhatItsNextFreezeWillLast() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.freeze(30, d -> {
        }));

        InfoSheet sheet = EnemyStatText.live(mob.inspect());

        assertThat(sheet.lines()).contains(Row.effect(Palette.STATUS_MARKER_BURN, "Immune to burn", ""),
                Row.plain(Glyph.DOT, "Freeze DR", "next 50%"));
    }

    @Test
    void anAdaptiveTraitIsShownAsTheMostItCanReach() {
        InfoSheet sheet = this.hoverSheet("c", Rank.ELITE);

        assertThat(sheet.lines()).contains(Row.trait(Palette.TRAIT_MARKER_PERCENT_RESIST, "Adaptive resist", "up to -40%"));
    }

    @Test
    void thePreviewEndsWithTheDescription() {
        InfoSheet sheet = this.hoverSheet("s", Rank.GRUNT);

        assertThat(sheet.lines().getLast()).isEqualTo(new SheetLine.Prose("Takes less damage. Immune to critical hits."));
    }

    @Test
    void theLiveViewShowsCurrentHealthEffectsAndFate() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.freeze(30, d -> {
        }));
        mob.doDamage(Damage.physical(4000));

        InfoSheet alive = EnemyStatText.live(mob.inspect());
        mob.doDamage(Damage.physical(1_000_000));
        InfoSheet killed = EnemyStatText.live(mob.inspect());

        assertThat(alive.lines()).contains(new SheetLine.HealthBar(60, 100, "$5", false),
                Row.effect(Palette.STATUS_MARKER_FREEZE, "Frozen", "hard CC 1.5 s"), Row.plain(Glyph.CHEVRON, "Speed", "0 px/s"));
        assertThat(killed.lines()).contains(new SheetLine.HealthBar(0, 100, "Killed", true));
    }

    @Test
    void aBurningEnemyShowsItsScorchedStacksAndTheResilienceTheyCost() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.burn(Damage.magic(100), 100, d -> {
        }));

        InfoSheet sheet = EnemyStatText.live(mob.inspect());

        assertThat(sheet.lines()).contains(
                Row.effect(Palette.STATUS_MARKER_BURN, "Burning", "damage over time"),
                Row.effect(Palette.STATUS_MARKER_SCORCHED, "Scorched x1", "debuff"),
                Row.plain(Glyph.DOT, "Resilience", "+1% crit damage"));
    }

    @Test
    void aChilledEnemyShowsItsLevelAndAPoisonedOneItsSickenedStacksAndSpiritLoss() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.chill(0.4f, 100, d -> {
        }));
        mob.applyEffect(Effect.poison(Damage.magic(100), 100, d -> {
        }));

        InfoSheet sheet = EnemyStatText.live(mob.inspect());

        assertThat(sheet.lines()).contains(
                Row.effect(Palette.STATUS_MARKER_CHILL, "Chilled", "soft CC 40%"),
                Row.effect(Palette.STATUS_MARKER_POISON, "Poisoned", "damage over time"),
                Row.effect(Palette.STATUS_MARKER_SICKENED, "Sickened x1", "debuff"),
                Row.plain(Glyph.DOT, "Spirit", "-1% heals"));
    }

    @Test
    void theSpotterEffectsShowTheirStacksAndASpiritedEnemyShowsHowFastDebuffsWearOff() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.sundered(3, 100, d -> {
        }));
        mob.applyEffect(Effect.exposed(100, d -> {
        }));
        mob.applyEffect(Effect.priority(100, d -> {
        }));

        InfoSheet sheet = EnemyStatText.live(mob.inspect());

        assertThat(sheet.lines()).contains(
                Row.effect(Palette.STATUS_MARKER_SUNDERED, "Sundered x3", "debuff 5.0 s"),
                Row.effect(Palette.STATUS_MARKER_EXPOSED, "Exposed", "spotted 5.0 s"),
                Row.effect(Palette.STATUS_MARKER_PRIORITY, "Priority", "spotted 5.0 s"));
    }

    @Test
    void spiritThatChangesTheDebuffPaceSaysSoInWords() {
        assertThat(EnemyStatText.statText(td.stat.EnemyStat.SPIRIT, 50f).value())
                .isEqualTo("+50% heals, debuffs 1.5x faster");
        assertThat(EnemyStatText.statText(td.stat.EnemyStat.SPIRIT, -50f).value())
                .isEqualTo("-50% heals, debuffs 2x slower");
        assertThat(EnemyStatText.statText(td.stat.EnemyStat.SPIRIT, -1f).value()).isEqualTo("-1% heals");
    }
}
