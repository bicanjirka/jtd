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
import td.ui.render.EnemySheet;
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

    private EnemySheet hoverSheet(String id, Rank rank) {
        EnemyDefinition definition = EnemyCatalog.builtIn().ranked(id).definitionFor(rank);
        return PanelEnemy.previewMob(definition, rank, this.world).accept(new EnemyInfoText());
    }

    private DefinedEnemyMob spawn(String id, Rank rank) {
        return (DefinedEnemyMob) EnemyFactory.getEnemy(id, this.world, 0, 100, 5, rank);
    }

    private static List<String> labels(EnemySheet sheet) {
        return sheet.lines().stream().filter(Row.class::isInstance).map(Row.class::cast).map(Row::label).toList();
    }

    @Test
    void aPlainEnemyShowsOnlyItsSpeed() {
        EnemySheet sheet = this.hoverSheet("c", Rank.GRUNT);

        assertThat(labels(sheet)).containsExactly("Speed");
    }

    @Test
    void theHeaderShowsTheBodyGlyphNameAndRankBadge() {
        EnemySheet sheet = this.hoverSheet("s", Rank.BOSS);

        assertThat(sheet.lines().getFirst())
                .isEqualTo(new SheetLine.Header(Palette.ENEMY_SQUARE, RankBadge.SKULL, "Armored mob", "Boss"));
    }

    @Test
    void aTraitAndTheStatsItSetsShowAsOneRowInTheTraitsBoardColour() {
        EnemySheet sheet = this.hoverSheet("s", Rank.GRUNT);

        assertThat(sheet.lines()).contains(Row.trait(Palette.TRAIT_MARKER_PERCENT_RESIST, "Resist all", "-20%"),
                Row.trait(Palette.TRAIT_MARKER_CRITICAL_IMMUNE, "Crit immune", ""));
        assertThat(labels(sheet)).containsExactly("Speed", "Resist all", "Crit immune");
    }

    @Test
    void platingOnOneDamageTypeNamesItAndShowsThePointsAHitLoses() {
        EnemySheet sheet = this.hoverSheet("s", Rank.ELITE);

        assertThat(sheet.lines()).contains(Row.trait(Palette.TRAIT_MARKER_FLAT_RESIST, "Plating, physical", "-8/hit"),
                Row.trait(Palette.TRAIT_MARKER_PERCENT_RESIST, "Resist all", "-40%"));
    }

    @Test
    void aRankGivenStatNoTraitExplainsIsAPlainRow() {
        EnemySheet sheet = this.hoverSheet("s", Rank.BOSS);

        assertThat(sheet.lines()).contains(Row.plain(Glyph.DOT, "Freeze DR", ""));
    }

    @Test
    void anAdaptiveTraitIsShownAsTheMostItCanReach() {
        EnemySheet sheet = this.hoverSheet("c", Rank.ELITE);

        assertThat(sheet.lines()).contains(Row.trait(Palette.TRAIT_MARKER_PERCENT_RESIST, "Adaptive resist", "up to -40%"));
    }

    @Test
    void thePreviewEndsWithTheDescription() {
        EnemySheet sheet = this.hoverSheet("s", Rank.GRUNT);

        assertThat(sheet.lines().getLast()).isEqualTo(new SheetLine.Prose("Takes less damage. Immune to critical hits."));
    }

    @Test
    void theLiveViewShowsCurrentHealthEffectsAndFate() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.freeze(30, d -> {
        }));
        mob.doDamage(Damage.physical(4000));

        EnemySheet alive = EnemyStatText.live(mob.inspect());
        mob.doDamage(Damage.physical(1_000_000));
        EnemySheet killed = EnemyStatText.live(mob.inspect());

        assertThat(alive.lines()).contains(new SheetLine.HealthBar(60, 100, "$5", false),
                Row.effect(Palette.STATUS_MARKER_FREEZE, "Frozen", "1.5 s"), Row.plain(Glyph.CHEVRON, "Speed", "0 px/s"));
        assertThat(killed.lines()).contains(new SheetLine.HealthBar(0, 100, "Killed", true));
    }
}
