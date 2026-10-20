package td.level;

import td.effect.ShieldTemplate;
import td.enemy.Ability;
import td.enemy.ApplyEffectAction;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.IdentifiedAbility;
import td.enemy.PeriodicTrigger;
import td.enemy.RadiusTarget;
import td.enemy.Rank;
import td.enemy.RankedEnemy;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.ArcCornerSmoothing;

import java.util.List;

final class CurlyPathLevel {

    private static final int STARTING_CREDITS = 50;
    private static final int STARTING_LIVES = 5;

    private static final int WARD_INTERVAL_TICKS = 100;
    private static final int WARD_TICKS = 60;
    private static final float WARD_RADIUS = 80f;

    /**
     * The path's Menders ({@code mender}): from Elite they also ward the allies near them with a shield that takes
     * every damage type, so the answer is to deny heals and shields, not to switch damage type.
     */
    private static final RankedEnemy MENDER = EnemyCatalog.builtIn().ranked("m").cloneAs("mender",
            (rank, mender) -> switch (rank) {
                case ELITE -> warding(mender, 30f);
                case BOSS -> warding(mender, 60f);
                default -> mender;
            });

    private static final ArcCornerSmoothing SMOOTHING = new ArcCornerSmoothing(0.22, 8);

    static final LevelDefinition DEFINITION = LevelDefinition.singlePath(
            "Curly Path",
            "A lane that spirals through two tight loops before unwinding into a steady zigzag, creating a curly visuals.",
            20, 13,
            List.of(
                    new Point(-1, 11), new Point(2, 11), new Point(2, 2), new Point(6, 2),
                    new Point(6, 8), new Point(3, 8), new Point(3, 5), new Point(7, 5),
                    new Point(7, 10), new Point(10, 10), new Point(10, 4), new Point(8, 4),
                    new Point(8, 7), new Point(13, 7), new Point(13, 2), new Point(16, 2),
                    new Point(16, 11), new Point(18, 11), new Point(18, 7), new Point(20, 7)),
            List.of(
                    new WaveDefinition("column 6 c", Rank.GRUNT),
                    new WaveDefinition("drip 4 courier", Rank.GRUNT),
                    new WaveDefinition("line 3 s e line 3 s", Rank.SOLDIER),
                    new WaveDefinition("g 2 e 6 c", Rank.SOLDIER),
                    new WaveDefinition("yeti 2 e yeti 2 e yeti", Rank.SOLDIER),
                    new WaveDefinition("grunt 5 c 3 e elite drip 3 yeti", Rank.GRUNT),
                    new WaveDefinition("flank courier 2 e flank courier", Rank.SOLDIER),
                    new WaveDefinition("column 4 c e veteran j e column 3 s", Rank.SOLDIER),
                    new WaveDefinition("line 3 skirmisher 2 e line 3 skirmisher", Rank.SOLDIER),
                    new WaveDefinition("swarm 12 mite e column 10 mite e veteran t e swarm 12 mite", Rank.SOLDIER),
                    new WaveDefinition("w8 column 12 mite elite g column 12 mite swarm 12 mite 2 e soldier line 4 c column 8 mite "
                            + "elite g column 8 mite", Rank.VETERAN),
                    new WaveDefinition("boss t elite mender elite mender 3 e elite salamander", Rank.ELITE),
                    new WaveDefinition("drip 3 courier", Rank.VETERAN),
                    new WaveDefinition("juggernaut e elite g", Rank.VETERAN),
                    new WaveDefinition("elite column 3 juggernaut 2 veteran flank shieldbearer 3 e soldier 3 armored s",
                            Rank.ELITE),
                    new WaveDefinition("line 3 skirmisher e veteran j", Rank.ELITE),
                    new WaveDefinition("2 juggernaut veteran flank shieldbearer 3 e boss drip 3 yeti 3 e 6 t mender mender 3 e "
                            + "column 12 mite g column 12 mite", Rank.ELITE),
                    new WaveDefinition("warden1 8 e elite swarm 12 mite 2 e veteran 3 skirmisher elite mender",
                            Rank.BOSS)),
            STARTING_CREDITS, STARTING_LIVES, SMOOTHING)
            .withCustomRankedEnemies(List.of(MENDER));

    private CurlyPathLevel() {
    }

    private static EnemyDefinition warding(EnemyDefinition mender, float points) {
        return mender.withDescription(mender.description() + " On this path it also wards every other ally near it "
                        + "with a shield that stops any damage.")
                .withAdditionalAbilities(List.of(IdentifiedAbility.named("ward", new Ability(
                        new PeriodicTrigger(WARD_INTERVAL_TICKS),
                        new ApplyEffectAction(new ShieldTemplate(points, WARD_TICKS), new RadiusTarget(WARD_RADIUS))))));
    }
}
