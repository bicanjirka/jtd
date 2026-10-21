package td.ui;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.AbilityTerm;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyFactory;
import td.enemy.EnemyInspection;
import td.enemy.EnemyMark;
import td.enemy.Rank;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
import td.term.TermText;
import td.tower.TowerTerm;
import td.ui.render.IconFace;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.RankBadge;
import td.ui.render.SheetIcon;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.ui.render.SheetLine.Trend;
import td.util.GameWorld;

import java.util.List;
import java.util.Optional;

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

    private static Optional<Row> row(InfoSheet sheet, String label) {
        return sheet.lines().stream().filter(Row.class::isInstance).map(Row.class::cast)
                .filter(row -> row.label().equals(label)).findFirst();
    }

    private static List<SheetIcon> icons(InfoSheet sheet, String label) {
        return sheet.lines().stream().filter(SheetLine.IconRow.class::isInstance).map(SheetLine.IconRow.class::cast)
                .filter(row -> row.label().equals(label)).flatMap(row -> row.icons().stream()).toList();
    }

    private static SheetIcon iconOf(InfoSheet sheet, String label, String pictogram) {
        return icons(sheet, label).stream().filter(icon -> icon.face().pictogram().equals(pictogram)).findFirst()
                .orElseThrow();
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
    void aStatOffItsDefaultIsARowWhateverSetsIt() {
        InfoSheet sheet = this.hoverSheet("s", Rank.GRUNT);

        assertThat(row(sheet, "Armor")).isPresent();
        assertThat(row(sheet, "Magic resist")).isPresent();
    }

    @Test
    void aStatBetterForThePlayerThanItsBaseIsBetter() {
        DefinedEnemyMob mob = this.spawn("s", Rank.GRUNT);
        mob.applyEffect(Effect.sundered(2, 100, DamageSink.none()));

        InfoSheet sheet = EnemyStatText.live(mob.inspect());

        assertThat(row(sheet, "Armor").orElseThrow().trend()).isEqualTo(Trend.BETTER);
    }

    @Test
    void aChilledEnemysSpeedIsBetterForThePlayer() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.chill(0.4f, 100, DamageSink.none()));

        InfoSheet sheet = EnemyStatText.live(mob.inspect());

        assertThat(row(sheet, "Speed").orElseThrow().trend()).isEqualTo(Trend.BETTER);
    }

    @Test
    void aStatRowOpensItsBreakdown() {
        DefinedEnemyMob mob = this.spawn("s", Rank.GRUNT);
        mob.applyEffect(Effect.sundered(2, 100, DamageSink.none()));

        InfoSheet sheet = EnemyStatText.live(mob.inspect());
        Row armor = row(sheet, "Armor").orElseThrow();

        InfoSheet breakdown = armor.tooltip().orElseThrow().sheet();
        assertThat(armor.tooltip().orElseThrow().key().id()).isEqualTo("stat:ARMOR");
        assertThat(breakdown.lines()).contains(Row.plain(Glyph.DOT, "Base", "25"), Row.plain(Glyph.DOT, "Now", "15"));
        assertThat(breakdown.lines()).anySatisfy(line -> assertThat(line).isInstanceOfSatisfying(
                SheetLine.Sentence.class, sentence -> {
                    assertThat(sentence.text().plain()).isEqualTo("Sunder: -10");
                    assertThat(sentence.text().mentions()).containsExactly(EffectKind.SUNDERED);
                }));
    }

    @Test
    void traitsAbilitiesAndImmunitiesShareTheTraitsRow() {
        DefinedEnemyMob mob = this.spawn("yeti", Rank.ELITE);

        InfoSheet sheet = EnemyStatText.live(mob.inspect());

        assertThat(iconOf(sheet, "Traits", "effect.freeze").face().ward()).isEqualTo(IconFace.Ward.IMMUNE);
        assertThat(icons(sheet, "Traits")).extracting(icon -> icon.face().pictogram())
                .contains(AbilityTerm.HUNKER.key());
    }

    @Test
    void anImmunityAnEffectCausesNamesThatEffect() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.freeze(30, DamageSink.none()));

        InfoSheet sheet = EnemyStatText.live(mob.inspect());

        SheetIcon burn = iconOf(sheet, "Traits", "effect.burn");
        assertThat(burn.face().ward()).isEqualTo(IconFace.Ward.IMMUNE);
        assertThat(burn.tooltip().lines()).anySatisfy(line -> assertThat(line).isInstanceOfSatisfying(
                SheetLine.Sentence.class, sentence -> assertThat(sentence.text().mentions())
                        .contains(EffectKind.FREEZE)));
    }

    @Test
    void buffsAndDebuffsSplitBySide() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.shield(50f, 100, DamageSink.none()));
        mob.applyEffect(Effect.chill(0.4f, 100, DamageSink.none()));

        InfoSheet sheet = EnemyStatText.live(mob.inspect());

        assertThat(icons(sheet, "Buffs")).extracting(icon -> icon.face().pictogram()).containsExactly("effect.shield");
        assertThat(icons(sheet, "Debuffs")).extracting(icon -> icon.face().pictogram())
                .containsExactly("effect.chill");
    }

    @Test
    void aStackingDebuffShowsItsStacks() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.sundered(3, 100, DamageSink.none()));

        InfoSheet sheet = EnemyStatText.live(mob.inspect());

        assertThat(iconOf(sheet, "Debuffs", "effect.sundered").face().corner()).isEqualTo("3");
    }

    @Test
    void aTimedEffectSweepsAndShowsWholeSecondsUnderTen() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.freeze(30, DamageSink.none()));

        InfoSheet sheet = EnemyStatText.live(mob.inspect());

        IconFace face = iconOf(sheet, "Debuffs", "effect.freeze").face();
        assertThat(face.below()).endsWith("s").isNotEmpty();
        assertThat(face.elapsed()).isGreaterThanOrEqualTo(0f);
    }

    @Test
    void aPoolShowsABarAndNoClock() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.burn(Damage.magic(100), 100, DamageSink.none()));

        InfoSheet sheet = EnemyStatText.live(mob.inspect());

        IconFace face = iconOf(sheet, "Debuffs", "effect.burn").face();
        assertThat(face.bar()).isPresent();
        assertThat(face.below()).isEmpty();
    }

    @Test
    void aFreezeWhoseDiminishingReturnsRunStaysFadedAndSaysWhatTheNextLasts() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.freeze(1, DamageSink.none()));
        mob.doTick(10);
        mob.doTick(11);

        InfoSheet sheet = EnemyStatText.live(mob.inspect());

        SheetIcon freeze = iconOf(sheet, "Debuffs", "effect.freeze");
        assertThat(freeze.face().faded()).isTrue();
        assertThat(freeze.tooltip().lines()).anySatisfy(line -> assertThat(line).isInstanceOfSatisfying(
                SheetLine.Sentence.class, sentence -> assertThat(sentence.text().plain())
                        .startsWith("The next one lasts ")));
    }

    @Test
    void stokeFromTwoTowersIsOneIconWithTheHighestSteps() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        EnemyInspection marked = mob.inspect().withMarks(List.of(
                new EnemyMark(TowerTerm.STOKE, 1, 3, "Scorcher"), new EnemyMark(TowerTerm.STOKE, 2, 3, "Scorcher")));

        InfoSheet sheet = EnemyStatText.live(marked);

        List<SheetIcon> stoke = icons(sheet, "Debuffs");
        assertThat(stoke).hasSize(1);
        assertThat(stoke.getFirst().face().corner()).isEqualTo("2");
    }

    @Test
    void thePreviewShowsStatsTraitsAndTheRoleSentenceButNoBuffsOrDebuffs() {
        InfoSheet sheet = this.hoverSheet("yeti", Rank.GRUNT);

        assertThat(icons(sheet, "Traits")).isNotEmpty();
        assertThat(icons(sheet, "Buffs")).isEmpty();
        assertThat(icons(sheet, "Debuffs")).isEmpty();
        assertThat(sheet.lines().getLast()).isInstanceOf(SheetLine.Prose.class);
    }

    @Test
    void thePreviewEndsWithTheDescription() {
        InfoSheet sheet = this.hoverSheet("s", Rank.GRUNT);

        assertThat(sheet.lines().getLast()).isEqualTo(
                new SheetLine.Prose(TermText.of("Heavy plates blunt physical hits; magic goes through.")));
    }

    @Test
    void theLiveViewShowsCurrentHealthAndFate() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.doDamage(Damage.physical(4000));

        InfoSheet alive = EnemyStatText.live(mob.inspect());
        mob.doDamage(Damage.physical(1_000_000));
        InfoSheet killed = EnemyStatText.live(mob.inspect());

        assertThat(alive.lines()).contains(new SheetLine.HealthBar(60, 100, "$5", false));
        assertThat(killed.lines()).contains(new SheetLine.HealthBar(0, 100, "Killed", true));
    }

    @Test
    void agilityReadsAsPointsWithItsDodgeChance() {
        InfoSheet sheet = this.hoverSheet("courier", Rank.GRUNT);

        assertThat(row(sheet, "Agility").orElseThrow().value()).isEqualTo("25, dodges 20%");
    }

    @Test
    void theLiveViewCountsTheHitsAnEnemyDodged() {
        GameWorld lucky = WorldFixtures.newWorld(() -> 0.0);
        DefinedEnemyMob courier = (DefinedEnemyMob) EnemyFactory.getEnemy("courier", lucky, 0, 100, 5, Rank.GRUNT);
        InfoSheet before = EnemyStatText.live(courier.inspect());

        courier.takeHit(Damage.physical(10), AttackProfile.none().asAimed());
        courier.takeHit(Damage.physical(10), AttackProfile.none().asAimed());
        InfoSheet after = EnemyStatText.live(courier.inspect());

        assertThat(labels(before)).doesNotContain("Dodged");
        assertThat(after.lines()).contains(Row.plain(Glyph.DOT, "Dodged", "2 hits"));
    }

    @Test
    void armorAndMagicResistReadAsPoints() {
        assertThat(EnemyStatText.statText(EnemyStat.ARMOR, 25f))
                .isEqualTo(new EnemyStatText.StatText("Armor", "25"));
        assertThat(EnemyStatText.statText(EnemyStat.MAGIC_RESIST, 67f))
                .isEqualTo(new EnemyStatText.StatText("Magic resist", "67"));
    }

    @Test
    void resilienceReadsAsCritsGainedOrLostAndAHundredIsCritImmune() {
        assertThat(EnemyStatText.statText(EnemyStat.RESILIENCE, -50f))
                .isEqualTo(new EnemyStatText.StatText("Resilience", "+50% crits"));
        assertThat(EnemyStatText.statText(EnemyStat.RESILIENCE, 30f))
                .isEqualTo(new EnemyStatText.StatText("Resilience", "-30% crits"));
        assertThat(EnemyStatText.statText(EnemyStat.RESILIENCE, 100f))
                .isEqualTo(new EnemyStatText.StatText("Crit immune", ""));
    }

    @Test
    void spiritThatChangesTheDebuffPaceSaysSoInWords() {
        assertThat(EnemyStatText.statText(EnemyStat.SPIRIT, 50f).value())
                .isEqualTo("+50% heals, debuffs 1.5x faster");
        assertThat(EnemyStatText.statText(EnemyStat.SPIRIT, -50f).value())
                .isEqualTo("-50% heals, debuffs 2x slower");
        assertThat(EnemyStatText.statText(EnemyStat.SPIRIT, -1f).value()).isEqualTo("-1% heals");
    }
}
