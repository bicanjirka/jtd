package td.ui;

import org.junit.jupiter.api.Test;
import td.damage.AttackOrigin;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.ShieldTemplate;
import td.enemy.Ability;
import td.enemy.AbilityTerm;
import td.enemy.ApplyEffectAction;
import td.enemy.BodyArchetype;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyInspection;
import td.enemy.EnemyMark;
import td.enemy.HealthThresholdTrigger;
import td.enemy.ImmunityTrait;
import td.enemy.Rank;
import td.enemy.SelfTarget;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
import td.stat.StatWording;
import td.term.TermText;
import td.tower.TowerTerm;
import td.ui.render.EnemyLook;
import td.ui.render.IconFace;
import td.ui.render.InfoSheet;
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
    void aPlainEnemyShowsOnlyItsSpeed() {
        InfoSheet sheet = this.hoverSheet(EnemyFixtures.simpleDefinition("plain"));

        assertThat(labels(sheet)).containsExactly("Move speed");
    }

    @Test
    void theHeaderShowsTheBodyGlyphNameAndRankBadge() {
        InfoSheet sheet = this.hoverSheet(EnemyDefinition.of("brick", "Brick", 100, 5, 1f, BodyArchetype.SQUARE), Rank.BOSS);

        assertThat(sheet.lines().getFirst())
                .isEqualTo(new SheetLine.EnemyHeader(EnemyLook.SQUARE, RankBadge.SKULL, "Brick", "Boss"));
    }

    @Test
    void aStatOffItsDefaultIsARowWhateverSetsIt() {
        InfoSheet sheet = this.hoverSheet(EnemyFixtures.simpleDefinition("warded").withArmor(ARMOR).withMagicResist(ARMOR));

        assertThat(row(sheet, "Armor")).isPresent();
        assertThat(row(sheet, "Magic resist")).isPresent();
    }

    @Test
    void aStatBetterForThePlayerThanItsBaseIsBetter() {
        DefinedEnemyMob mob = this.armored();
        mob.applyEffect(Effect.sundered(2, 100, DamageSink.none()));

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
    void aStatRowOpensItsBreakdown() {
        DefinedEnemyMob mob = this.armored();
        mob.applyEffect(Effect.sundered(2, 100, DamageSink.none()));
        int now = Math.round(mob.stats().value(EnemyStat.ARMOR));

        InfoSheet sheet = EnemyStatText.live(mob.inspect(), WORDING);
        Row armor = row(sheet, "Armor").orElseThrow();

        InfoSheet breakdown = armor.tooltip().orElseThrow().sheet();
        assertThat(armor.tooltip().orElseThrow().key().id()).isEqualTo("stat:" + EnemyStat.ARMOR.key());
        assertThat(breakdown.lines())
                .contains(Row.plain(Glyph.DOT, "Base", "25"), Row.plain(Glyph.DOT, "Now", String.valueOf(now)));
        assertThat(breakdown.lines()).contains(Row.faced(TermLooks.faceOf(EffectKind.SUNDERED),
                EffectKind.SUNDERED.title() + " \u00d72", "-" + (25 - now)));
    }

    @Test
    void traitsAbilitiesAndImmunitiesShareTheTraitsRow() {
        DefinedEnemyMob mob = EnemyFixtures.liveMob(this.world, EnemyFixtures.simpleDefinition("hunkered")
                .withTraits(List.of(new ImmunityTrait(EffectKind.FREEZE)))
                .withAbilities(List.of(new Ability(AbilityTerm.HUNKER, new HealthThresholdTrigger(0.5f),
                        new ApplyEffectAction(new ShieldTemplate(10f, 100), new SelfTarget())))));

        InfoSheet sheet = EnemyStatText.live(mob.inspect(), WORDING);

        assertThat(iconOf(sheet, "Traits", "effect.freeze").face().ward()).isEqualTo(IconFace.Ward.IMMUNE);
        assertThat(icons(sheet, "Traits")).extracting(icon -> icon.face().pictogram())
                .contains(AbilityTerm.HUNKER.key());
    }

    @Test
    void anImmunityAnEffectCausesNamesThatEffect() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.revealed(30, DamageSink.none()));

        InfoSheet sheet = EnemyStatText.live(mob.inspect(), WORDING);

        SheetIcon cloak = iconOf(sheet, "Traits", "effect.invisible");
        assertThat(cloak.face().ward()).isEqualTo(IconFace.Ward.IMMUNE);
        assertThat(cloak.tooltip().lines()).anySatisfy(line -> assertThat(line).isInstanceOfSatisfying(
                SheetLine.Sentence.class, sentence -> assertThat(sentence.text().mentions())
                        .contains(EffectKind.REVEALED)));
    }

    @Test
    void buffsAndDebuffsSplitBySide() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.shield(50f, 100, DamageSink.none()));
        mob.applyEffect(Effect.chill(0.4f, 100, DamageSink.none()));

        InfoSheet sheet = EnemyStatText.live(mob.inspect(), WORDING);

        assertThat(icons(sheet, "Buffs")).extracting(icon -> icon.face().pictogram()).containsExactly("effect.shield");
        assertThat(icons(sheet, "Debuffs")).extracting(icon -> icon.face().pictogram())
                .containsExactly("effect.chill");
    }

    @Test
    void aStackingDebuffShowsItsStacks() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.sundered(3, 100, DamageSink.none()));

        InfoSheet sheet = EnemyStatText.live(mob.inspect(), WORDING);

        assertThat(iconOf(sheet, "Debuffs", "effect.sundered").face().corner()).isEqualTo("3");
    }

    @Test
    void aTimedEffectSweepsAndShowsWholeSecondsUnderTen() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.freeze(30, DamageSink.none()));

        InfoSheet sheet = EnemyStatText.live(mob.inspect(), WORDING);

        IconFace face = iconOf(sheet, "Debuffs", "effect.freeze").face();
        assertThat(face.below()).endsWith("s").isNotEmpty();
        assertThat(face.elapsed()).isGreaterThanOrEqualTo(0f);
    }

    @Test
    void aPoolShowsABarAndNoClock() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.burn(Damage.magic(100), 100, DamageSink.none()));

        InfoSheet sheet = EnemyStatText.live(mob.inspect(), WORDING);

        IconFace face = iconOf(sheet, "Debuffs", "effect.burn").face();
        assertThat(face.bar()).isPresent();
        assertThat(face.below()).isEmpty();
    }

    @Test
    void aFreezeWhoseDiminishingReturnsRunStaysFadedAndSaysWhatTheNextLasts() {
        DefinedEnemyMob mob = this.anEnemy();
        mob.applyEffect(Effect.freeze(1, DamageSink.none()));
        mob.doTick(10);
        mob.doTick(11);

        InfoSheet sheet = EnemyStatText.live(mob.inspect(), WORDING);

        SheetIcon freeze = iconOf(sheet, "Debuffs", "effect.freeze");
        assertThat(freeze.face().faded()).isTrue();
        assertThat(freeze.tooltip().lines()).anySatisfy(line -> assertThat(line).isInstanceOfSatisfying(
                SheetLine.Sentence.class, sentence -> assertThat(sentence.text().plain())
                        .startsWith("The next one lasts ")));
    }

    @Test
    void stokeFromTwoTowersIsOneIconWithTheHighestSteps() {
        DefinedEnemyMob mob = this.anEnemy();
        EnemyInspection marked = mob.inspect().withMarks(List.of(
                new EnemyMark(TowerTerm.STOKE, 1, 3, "Scorcher"), new EnemyMark(TowerTerm.STOKE, 2, 3, "Scorcher")));

        InfoSheet sheet = EnemyStatText.live(marked, WORDING);

        List<SheetIcon> stoke = icons(sheet, "Debuffs");
        assertThat(stoke).hasSize(1);
        assertThat(stoke.getFirst().face().corner()).isEqualTo("2");
    }

    @Test
    void thePreviewShowsStatsTraitsAndTheRoleSentenceButNoBuffsOrDebuffs() {
        InfoSheet sheet = this.hoverSheet(EnemyFixtures.simpleDefinition("resistant")
                .withTraits(List.of(new ImmunityTrait(EffectKind.CHILL))).withDescription("Shrugs off cold."));

        assertThat(icons(sheet, "Traits")).isNotEmpty();
        assertThat(icons(sheet, "Buffs")).isEmpty();
        assertThat(icons(sheet, "Debuffs")).isEmpty();
        assertThat(sheet.lines().getLast()).isInstanceOf(SheetLine.Prose.class);
    }

    @Test
    void thePreviewEndsWithTheDescription() {
        InfoSheet sheet = this.hoverSheet(EnemyFixtures.simpleDefinition("described").withDescription("Walks the path."));

        assertThat(sheet.lines().getLast()).isEqualTo(new SheetLine.Prose(TermText.of("Walks the path.")));
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
    void theRegenerationHoverOfAHealedEnemyWithSpiritMarksSpiritAsItsOwnLine() {
        DefinedEnemyMob mob = EnemyFixtures.liveMob(this.world, EnemyFixtures.simpleDefinition("blessed").withSpirit(60f));
        mob.applyEffect(Effect.heal(100, 40, DamageSink.none()));

        Row regeneration = row(EnemyStatText.live(mob.inspect(), WORDING), "Regeneration").orElseThrow();

        assertThat(regeneration.tooltip().orElseThrow().sheet().lines()).anySatisfy(line -> assertThat(line)
                .isInstanceOfSatisfying(Row.class, spirit -> assertThat(spirit.label()).isEqualTo(EnemyStat.SPIRIT.title())));
    }

    @Test
    void agilityReadsAsPointsWithItsDodgeChance() {
        InfoSheet sheet = this.hoverSheet(EnemyFixtures.simpleDefinition("nimble").withAgility(25f));

        assertThat(row(sheet, "Agility").orElseThrow().value()).isEqualTo("25");
        assertThat(row(sheet, "Agility").orElseThrow().meaning()).isEqualTo("dodges 20%");
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

    @Test
    void statRowsFollowTheirGroupsWithAGapBetweenTwo() {
        InfoSheet sheet = this.hoverSheet(EnemyFixtures.simpleDefinition("mixed").withSpirit(20f).withArmor(ARMOR));

        assertThat(labels(sheet)).containsSubsequence("Move speed", "Armor", "Spirit");
        int armor = sheet.lines().indexOf(row(sheet, "Armor").orElseThrow());
        assertThat(sheet.lines().get(armor - 1)).isInstanceOf(SheetLine.Gap.class);
    }

    private static SheetLine.HealthBar healthBar(InfoSheet sheet) {
        return sheet.lines().stream().filter(SheetLine.HealthBar.class::isInstance).map(SheetLine.HealthBar.class::cast)
                .findFirst().orElseThrow();
    }
}
