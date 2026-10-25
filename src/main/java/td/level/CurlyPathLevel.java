package td.level;

import td.effect.ShieldTemplate;
import td.enemy.Ability;
import td.enemy.AbilityTerm;
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
     * The path's Menders ({@code warder}): from Elite they also ward the allies near them with a shield that takes
     * every damage type, so the answer is to deny heals and shields, not to switch damage type.
     */
    private static final RankedEnemy MENDER = EnemyCatalog.builtIn().ranked("mender").cloneAs("warder",
            (rank, mender) -> switch (rank) {
                case ELITE -> warding(mender, 30f);
                case BOSS -> warding(mender, 60f);
                default -> mender;
            });

    private static final ArcCornerSmoothing SMOOTHING = new ArcCornerSmoothing(0.22, 8);

    static final LevelDefinition DEFINITION = LevelDefinition.singlePath(
            "Curly Path",
            "A lane that loops once at the entrance, sweeps around a wide bend and hooks inward to leave over the top edge.",
            20, 13,
            List.of(
                    new Point(-1, 10), new Point(2, 10), new Point(3, 9), new Point(3, 3),
                    new Point(2, 2), new Point(1, 3), new Point(1, 9), new Point(2, 10),
                    new Point(11, 10), new Point(13, 11), new Point(15, 11), new Point(17, 10),
                    new Point(18, 8), new Point(18, 6), new Point(17, 4), new Point(15, 3),
                    new Point(13, 3), new Point(11, 4), new Point(10, 6), new Point(10, 8),
                    new Point(6, 8), new Point(6, 3), new Point(7, 2), new Point(11, 2), new Point(11, -1)),
            List.of(
                    new WaveDefinition("3 simple column 3 simple e simple e line 3 simple e simple e column 3 simple", Rank.GRUNT),
                    new WaveDefinition("drip 4 courier e 4 simple e drip 4 courier", Rank.GRUNT),
                    new WaveDefinition("line 3 armored e line 3 armored e line 3 armored e 4 simple", Rank.SOLDIER),
                    new WaveDefinition("ghost 2 e 8 simple e ghost 2 e 8 simple", Rank.SOLDIER),
                    new WaveDefinition("yeti 2 e yeti 2 e yeti", Rank.SOLDIER),
                    new WaveDefinition("grunt 5 simple 3 e elite drip 3 yeti 3 e grunt drip 4 courier e grunt 6 simple", Rank.GRUNT),
                    new WaveDefinition("flank courier 2 e flank courier 2 e line 4 simple 2 e flank courier 2 e flank courier",
                            Rank.SOLDIER),
                    new WaveDefinition("column 8 simple e veteran jammer e column 5 armored e column 6 simple", Rank.SOLDIER),
                    new WaveDefinition("line 3 dodger 2 e line 3 dodger 2 e line 3 dodger", Rank.SOLDIER),
                    new WaveDefinition("swarm 12 mite e column 10 mite e veteran fast e swarm 12 mite e column 10 simple e swarm 12 mite "
                            + "e line 6 courier e column 8 simple e veteran fast", Rank.SOLDIER),
                    new WaveDefinition("w8 column 12 mite elite ghost column 12 mite swarm 12 mite 2 e soldier line 4 simple column 8 mite "
                            + "elite ghost column 8 mite 2 e soldier column 10 simple column 12 mite soldier line 6 courier swarm 12 mite "
                            + "column 10 simple elite ghost swarm 12 mite", Rank.VETERAN),
                    new WaveDefinition("column 8 simple boss fast elite warder elite warder 3 e elite lizard e column 8 simple",
                            Rank.ELITE),
                    new WaveDefinition("drip 3 courier e column 6 simple e drip 3 courier", Rank.VETERAN),
                    new WaveDefinition("titan e elite ghost", Rank.VETERAN),
                    new WaveDefinition("elite column 3 titan 2 veteran flank shield 3 e soldier 3 plated armored "
                            + "e soldier column 10 simple", Rank.ELITE),
                    new WaveDefinition("line 3 dodger e veteran jammer", Rank.ELITE),
                    new WaveDefinition("2 titan veteran flank shield 3 e boss drip 3 yeti 3 e 9 fast warder warder 3 e "
                            + "column 12 mite ghost column 12 mite swarm 12 mite e column 10 simple ghost swarm 12 mite", Rank.ELITE),
                    new WaveDefinition("warden 8 e elite swarm 12 mite 2 e veteran 3 dodger elite warder e veteran "
                            + "column 10 simple e swarm 12 mite", Rank.BOSS)),
            STARTING_CREDITS, STARTING_LIVES, SMOOTHING)
            .withCustomRankedEnemies(List.of(MENDER));

    private CurlyPathLevel() {
    }

    private static EnemyDefinition warding(EnemyDefinition mender, float points) {
        return mender
                .withAdditionalAbilities(List.of(IdentifiedAbility.named("ward", new Ability(AbilityTerm.WARD,
                        new PeriodicTrigger(WARD_INTERVAL_TICKS),
                        new ApplyEffectAction(new ShieldTemplate(points, WARD_TICKS), new RadiusTarget(WARD_RADIUS))))));
    }
}
