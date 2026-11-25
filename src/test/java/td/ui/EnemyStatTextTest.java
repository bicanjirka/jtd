package td.ui;

import org.junit.jupiter.api.Test;
import td.damage.AttackOrigin;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.ShieldTemplate;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyDefinition;
import td.enemy.Rank;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.stat.StatWording;
import td.ui.render.InfoSheet;
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

    private static final int CELL = 40;
    private static final StatWording WORDING = new StatWording(CELL);
    private static final float ARMOR = 25f;

    private final GameWorld world = WorldFixtures.newWorld();

    private InfoSheet hoverSheet(EnemyDefinition definition, Rank rank) {
        return PanelEnemy.previewMob(definition, rank, this.world).accept(new EnemyInfoText(WORDING));
    }

    private InfoSheet hoverSheet(EnemyDefinition definition) {
        return this.hoverSheet(definition, Rank.GRUNT);
    }

    private DefinedEnemyMob anEnemy() {
        return EnemyFixtures.plainMob(this.world, 100, 5);
    }

    private DefinedEnemyMob armored() {
        return EnemyFixtures.liveMob(this.world, EnemyFixtures.simpleDefinition("armored").withArmor(ARMOR));
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
    void aStatBetterForThePlayerThanItsBaseIsBetter() {
        DefinedEnemyMob mob = this.armored();
        mob.applyEffect(Effect.sundered(2));

        InfoSheet sheet = EnemyStatText.live(mob.inspect(), WORDING);

        assertThat(row(sheet, "Armor").orElseThrow().trend()).isEqualTo(Trend.BETTER);
    }

    @Test
    void aChilledEnemysSpeedIsBetterForThePlayer() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.chill(0.4f, 100, DamageSink.none()));

        InfoSheet sheet = EnemyStatText.live(mob.inspect(), WORDING);

        assertThat(row(sheet, "Move speed").orElseThrow().trend()).isEqualTo(Trend.BETTER);
    }

    @Test
    void aStackingDebuffShowsItsStacks() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.sundered(3));

        InfoSheet sheet = EnemyStatText.live(mob.inspect(), WORDING);

        assertThat(iconOf(sheet, "Debuffs", "effect.sundered").face().corner()).isEqualTo("3");
    }

    private static Optional<Row> effectRow(InfoSheet sheet, String pictogram, String label) {
        return iconOf(sheet, "Debuffs", pictogram).tooltip().lines().stream()
                .filter(Row.class::isInstance).map(Row.class::cast)
                .filter(row -> row.label().equals(label)).findFirst();
    }

    @Test
    void aCountShowsHowLongItsHoldRunsAndOnceFadingWhenItNextLosesAStack() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.sundered(2));
        InfoSheet holding = EnemyStatText.live(mob.inspect(), WORDING);
        for (int tick = 1; tick <= Effect.HOLD_TICKS + 1; tick++) {
            mob.doTick(tick);
        }

        InfoSheet fading = EnemyStatText.live(mob.inspect(), WORDING);

        assertThat(effectRow(holding, "effect.sundered", "Held for")).isPresent();
        assertThat(effectRow(fading, "effect.sundered", "Next stack lost in")).isPresent();
        assertThat(effectRow(fading, "effect.sundered", "Held for")).isEmpty();
    }

    @Test
    void aCountASourceHoldsShowsItIsHeldWithNoTime() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.holdStacks(EffectKind.TOLL, 1, 3);
        mob.doTick(1);

        InfoSheet sheet = EnemyStatText.live(mob.inspect(), WORDING);

        assertThat(effectRow(sheet, "effect.toll", "Held")).hasValueSatisfying(
                row -> assertThat(row.value()).isEqualTo("while its source lasts"));
    }

    @Test
    void aDebuffInSharesShowsHowManyTowersKeepItAndWhenTheNextShareEnds() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.cracked(1, 100, DamageSink.none()), AttackOrigin.fresh());
        mob.applyEffect(Effect.cracked(1, 40, DamageSink.none()), AttackOrigin.fresh());

        InfoSheet sheet = EnemyStatText.live(mob.inspect(), WORDING);

        assertThat(effectRow(sheet, "effect.cracked", "From")).hasValueSatisfying(
                row -> assertThat(row.value()).isEqualTo("2 towers"));
        assertThat(effectRow(sheet, "effect.cracked", "Next share ends in")).hasValueSatisfying(
                row -> assertThat(row.value()).isEqualTo("2"));
    }

    @Test
    void aFreezeWhoseMeterRunsStaysFadedAndSaysWhatTheNextLasts() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.freeze(20, DamageSink.none()));
        for (int tick = 1; tick <= 21; tick++) {
            mob.doTick(tick);
        }

        InfoSheet sheet = EnemyStatText.live(mob.inspect(), WORDING);

        SheetIcon freeze = iconOf(sheet, "Debuffs", "effect.freeze");
        assertThat(freeze.face().faded()).isTrue();
        assertThat(freeze.tooltip().lines()).anySatisfy(line -> assertThat(line).isInstanceOfSatisfying(
                SheetLine.Sentence.class, sentence -> assertThat(sentence.text().plain())
                        .startsWith("The next one lasts ")));
    }

    @Test
    void theLiveViewShowsCurrentHealthAndFate() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.doDamage(Damage.physical(4000));

        InfoSheet alive = EnemyStatText.live(mob.inspect(), WORDING);
        mob.doDamage(Damage.physical(1_000_000));
        InfoSheet killed = EnemyStatText.live(mob.inspect(), WORDING);

        assertThat(healthBar(alive).health()).isEqualTo(60);
        assertThat(healthBar(alive).fate()).isEmpty();
        assertThat(healthBar(alive).tallies()).extracting(SheetLine.Tally::value).containsExactly("5", "1");
        assertThat(healthBar(killed).health()).isZero();
        assertThat(healthBar(killed).fate()).contains("Killed");
    }

    @Test
    void theHealthBarCarriesWhatItsShieldsHoldByTheDamageTheyTake() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.shield(500f, 100, DamageSink.none()).from(AttackOrigin.fresh()));
        mob.applyEffect(ShieldTemplate.physicalOnly(3f, 100).toEffect(DamageSink.none()).from(AttackOrigin.fresh()));

        InfoSheet sheet = EnemyStatText.live(mob.inspect(), WORDING);

        assertThat(healthBar(sheet).shields()).isEqualTo(new SheetLine.Shields(5f, 3f, 0f));
    }

    @Test
    void aShieldsHoverListsEachSourcesShieldWithWhatItTakesAndHowLongItHas() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(ShieldTemplate.magicOnly(3f, 40).toEffect(DamageSink.none()).from(AttackOrigin.fresh()));
        mob.applyEffect(ShieldTemplate.untilBroken(2f).toEffect(DamageSink.none()).from(AttackOrigin.fresh()));

        InfoSheet hover = icons(EnemyStatText.live(mob.inspect(), WORDING), "Buffs").getFirst().tooltip();

        assertThat(hover.lines()).contains(
                Row.plain(Glyph.DOT, "Holds magic", "3 / 3").withGloss("hp", "2 s"),
                Row.plain(Glyph.DOT, "Holds", "2 / 2").withGloss("hp", "until broken"));
    }

    @Test
    void agilityReadsAsPointsWithItsDodgeChance() {
        InfoSheet sheet = this.hoverSheet(EnemyFixtures.simpleDefinition("nimble").withAgility(25f));

        assertThat(row(sheet, "Agility").orElseThrow().value()).isEqualTo("25");
        assertThat(row(sheet, "Agility").orElseThrow().meaning()).isEqualTo("dodges 20%");
    }

    @Test
    void negativeAgilityReadsAsWhatItMultipliesCritChanceBy() {
        InfoSheet sheet = this.hoverSheet(EnemyFixtures.simpleDefinition("clumsy").withAgility(-50f));

        assertThat(row(sheet, "Agility").orElseThrow().meaning()).isEqualTo("x1.5 crit chance");
    }

    @Test
    void theLiveViewCountsTheHitsAnEnemyDodged() {
        GameWorld lucky = WorldFixtures.newWorld(() -> 0.0);
        DefinedEnemyMob nimble = EnemyFixtures.liveMob(lucky, EnemyFixtures.simpleDefinition("nimble").withAgility(25f));
        InfoSheet before = EnemyStatText.live(nimble.inspect(), WORDING);

        nimble.takeHit(Damage.physical(10), AttackProfile.none().asAimed());
        nimble.takeHit(Damage.physical(10), AttackProfile.none().asAimed());
        InfoSheet after = EnemyStatText.live(nimble.inspect(), WORDING);

        assertThat(labels(before)).doesNotContain("Dodged");
        assertThat(after.lines()).contains(Row.plain(Glyph.DOT, "Dodged", "2").withGloss("hits", ""));
    }

    @Test
    void aLevelEndingEnemyCostsAllLives() {
        InfoSheet sheet = this.hoverSheet(EnemyFixtures.simpleDefinition("final").withLivesOnLeak(EnemyDefinition.ALL_LIVES));

        assertThat(healthBar(sheet).tallies()).extracting(SheetLine.Tally::value).last().isEqualTo("all");
    }

    private static SheetLine.HealthBar healthBar(InfoSheet sheet) {
        return sheet.lines().stream().filter(SheetLine.HealthBar.class::isInstance).map(SheetLine.HealthBar.class::cast)
                .findFirst().orElseThrow();
    }
}
