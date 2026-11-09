package td.level;

import td.board.BoardGround;
import td.board.GroundMotif;
import td.effect.EffectKind;
import td.effect.HealTemplate;
import td.effect.ShieldTemplate;
import td.effect.SprintTemplate;
import td.enemy.Ability;
import td.enemy.AbilityTerm;
import td.enemy.ApplyEffectAction;
import td.enemy.BodyArchetype;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.HealthThresholdTrigger;
import td.enemy.HurtSpeedTrait;
import td.enemy.IdentifiedAbility;
import td.enemy.IdentifiedTrait;
import td.enemy.ImmunityTrait;
import td.enemy.OnceTrigger;
import td.enemy.PeriodicTrigger;
import td.enemy.Rank;
import td.enemy.RankedEnemy;
import td.enemy.SelfTarget;
import td.enemy.StrideMovement;
import td.enemy.SwingCurve;
import td.enemy.SwingMovement;
import td.stat.DisruptionAura;
import td.wave.PathColor;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.WaveDefinition;

import java.util.List;

/**
 * The Labyrinth: one lane fills every cell of a small board but the nine at its heart, so the defence is whatever
 * nine towers, in whatever order, the player chooses to raise there. Three rounds: a long one that pays for the
 * defence, a round of Bosses around a guardian whose shield only a tower's answer to shields gets through, and the
 * Minotaur.
 */
final class LabyrinthLevel {

    private static final String LEVEL_NAME = "The Labyrinth";
    private static final String LEVEL_DESCRIPTION = "Every cell is corridor but the nine at its heart. Build there - "
            + "and choose well, for something waits at the end of the maze.";
    private static final int STARTING_CREDITS = 150;
    private static final int STARTING_LIVES = 5;

    /** A Runner in bronze: fireproof and thick-skinned, so it asks for more than a Scorcher. */
    private static final RankedEnemy BRONZE_BULL = EnemyCatalog.builtIn().ranked("runner").cloneAs("bronzeBull",
            fast -> fast
                    .withDisplayName("Bronze Bull")
                    .withArchetype(BodyArchetype.BRONZE_BULL)
                    .withMovement(new StrideMovement((float) Math.toRadians(15.0), 12))
                    .withDescription("A bull cast in bronze, faster the more it is dented.")
                    .withHealthAndPrice(fast.baseHealth() / 2, fast.price())
                    .withPlating(3f)
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("bronze",
                            new ImmunityTrait(EffectKind.BURN)))));

    /**
     * The bronze guardian: a body that breaks easily under a crit, inside a shield too big to shoot through and
     * recast again and again. A tower that strips, pierces, blocks or inverts shields is the way in.
     */
    private static final RankedEnemy TALOS = RankedEnemy.unique(
            EnemyDefinition.of("talos", "Talos", 3000, 60, 0.35f, BodyArchetype.TALOS)
                    .withDescription("The bronze guardian of the island. One vein of ichor runs through it, "
                            + "and the shield never stops.")
                    .withMovement(new SwingMovement((float) Math.toRadians(8.0), 40, SwingCurve.SMOOTH))
                    .withStats(30f, 30f, 0f, 0f, -60f, 0f, 0f)
                    .withIdentifiedTraits(List.of(IdentifiedTrait.anonymous(new ImmunityTrait(EffectKind.DAZED))))
                    .withIdentifiedAbilities(List.of(
                            IdentifiedAbility.anonymous(new Ability(AbilityTerm.BRONZE_SHELL, new OnceTrigger(1),
                                    new ApplyEffectAction(ShieldTemplate.untilBroken(6000f), new SelfTarget()))),
                            IdentifiedAbility.anonymous(new Ability(AbilityTerm.BRONZE_SHELL, new PeriodicTrigger(240),
                                    new ApplyEffectAction(ShieldTemplate.untilBroken(3000f), new SelfTarget()))))));

    /**
     * The Minotaur: thick-hided and hard to slow, bellowing so the towers near it falter, charging from time to
     * time and faster the more it bleeds, and once badly hurt a frenzy that heals it and sends it running.
     */
    private static final RankedEnemy MINOTAUR = RankedEnemy.unique(
            EnemyDefinition.of("minotaur", "The Minotaur", 12000, 0, 0.5f, BodyArchetype.MINOTAUR)
                    .withDescription("Born of a queen and a bull, kept at the heart of the maze. It knows every turn.")
                    .withMovement(new StrideMovement((float) Math.toRadians(12.0), 18))
                    .withStats(40f, 15f, 8f, 0f, 60f, 0f, 40f)
                    .withTenacity(100f)
                    .withDisruption(new DisruptionAura(110f, 0.35f, 0.8f))
                    .withIdentifiedTraits(List.of(
                            IdentifiedTrait.anonymous(new HurtSpeedTrait(1.6f))))
                    .withIdentifiedAbilities(List.of(
                            IdentifiedAbility.anonymous(new Ability(AbilityTerm.CHARGE, new PeriodicTrigger(200),
                                    new ApplyEffectAction(new SprintTemplate(30), new SelfTarget()))),
                            IdentifiedAbility.anonymous(new Ability(AbilityTerm.BLOODLUST, new HealthThresholdTrigger(0.5f),
                                    new ApplyEffectAction(new HealTemplate(15f, 100), new SelfTarget()))),
                            IdentifiedAbility.anonymous(new Ability(AbilityTerm.BLOODLUST, new HealthThresholdTrigger(0.5f),
                                    new ApplyEffectAction(new SprintTemplate(60), new SelfTarget()))))));

    /** Square corners, as a maze's are: a rounded one would leave a corner cell buildable. */
    private static final PathColor STONE_COLOR = PathColor.of(200, 170, 120);
    private static final PathDefinition MAZE_PATH = PathDefinition.of(
                    List.of(
                            new Point(1, -1), new Point(1, 5), new Point(2, 5), new Point(2, 2), new Point(7, 2),
                            new Point(7, 1), new Point(2, 1), new Point(2, 0), new Point(8, 0), new Point(8, 8),
                            new Point(7, 8), new Point(7, 3), new Point(6, 3), new Point(6, 6), new Point(1, 6),
                            new Point(1, 7), new Point(6, 7), new Point(6, 8), new Point(0, 8), new Point(0, 0),
                            new Point(-1, 0)),
                    List.of(
                            new WaveDefinition("10 walker 3 knight 8 walker 4 runner e 6 soldier walker 3 soldier knight "
                                    + "2 soldier ghost 6 soldier walker soldier golem e 4 soldier runner 6 veteran walker "
                                    + "3 veteran knight veteran mender 2 veteran ghost 6 veteran walker veteran golem e "
                                    + "4 veteran runner veteran magma 6 veteran walker 2 veteran knight e 4 elite walker elite golem elite knight "
                                    + "4 elite walker elite magma", Rank.GRUNT),
                            new WaveDefinition("w220 2 magma 3 golem 4 bronzeBull e talos e 4 bronzeBull 3 golem 2 magma", Rank.BOSS),
                            new WaveDefinition("4 bronzeBull e 2 golem e minotaur e 4 bronzeBull", Rank.ELITE)))
            .withColor(STONE_COLOR);

    static final LevelDefinition DEFINITION = LevelDefinition
            .of(LEVEL_NAME, 9, 9, List.of(MAZE_PATH))
            .withDescription(LEVEL_DESCRIPTION)
            .withStartingCredits(STARTING_CREDITS)
            .withStartingLives(STARTING_LIVES)
            .withGround(BoardGround.of(0x0D0B06, GroundMotif.FLAGSTONES, 0x17130B))
            .withCustomRankedEnemies(List.of(BRONZE_BULL, TALOS, MINOTAUR));

    private LabyrinthLevel() {
    }
}
