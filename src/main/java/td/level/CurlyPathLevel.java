package td.level;

import td.effect.ShieldTemplate;
import td.enemy.Ability;
import td.enemy.AbilityTerm;
import td.enemy.ApplyEffectAction;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.IdentifiedAbility;
import td.enemy.PeriodicTrigger;
import td.enemy.PulseMovement;
import td.enemy.RadiusTarget;
import td.enemy.Rank;
import td.enemy.RankedEnemy;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.ArcCornerSmoothing;

import java.util.List;
import java.util.stream.Stream;

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
    private static final RankedEnemy WARDER = EnemyCatalog.builtIn().ranked("mender").cloneAs("warder",
            (rank, mender) -> switch (rank) {
                case ELITE -> warding(mender, 30f);
                case BOSS -> warding(mender, 60f);
                default -> mender;
            });

    /** Its source's ladder and abilities on a tougher, slower body. */
    private static final RankedEnemy BULKY = EnemyCatalog.builtIn().ranked("simple").cloneAs("bulky",
            simple -> simple
                    .withDisplayName("Bulky mob")
                    .withDescription("A simple, yet bulky soldier.")
                    .withMovement(new PulseMovement())
                    .withHealthAndPrice(simple.baseHealth() * 3, simple.price())
                    .withBaseSpeed(simple.baseSpeed() / 2f));

    private static final ArcCornerSmoothing SMOOTHING = new ArcCornerSmoothing(0.22, 8);

    static final LevelDefinition DEFINITION = LevelDefinition.singlePath(
            "Curly Path",
            "A lane that loops once at the entrance, sweeps around a wide bend and hooks inward to leave over the top edge.",
            20, 13,
            List.of(
                    new Point(-1, 10), new Point(2, 10), new Point(3, 9), new Point(3, 2),
                    new Point(2, 1), new Point(1, 2), new Point(1, 9), new Point(2, 10),
                    new Point(11, 10), new Point(13, 11), new Point(15, 11), new Point(17, 10),
                    new Point(18, 8), new Point(18, 6), new Point(17, 4), new Point(15, 3),
                    new Point(13, 3), new Point(11, 4), new Point(10, 6), new Point(10, 8),
                    new Point(6, 8), new Point(6, 3), new Point(7, 2), new Point(11, 2), new Point(11, -1)),
            List.of(
                    new WaveDefinition("3 simple column 3 simple e bulky e line 3 simple e bulky e column 3 simple", Rank.GRUNT),
                    new WaveDefinition("drip 4 courier e 4 simple e 4 courier e soldier plated courier e soldier bulky", Rank.GRUNT),
                    new WaveDefinition("3 armored e line 3 armored mender 4 e 12 simple", Rank.SOLDIER),
                    new WaveDefinition("golem e ghost 2 e 8 simple e ghost 2 e 8 simple", Rank.SOLDIER),
                    new WaveDefinition("yeti 2 e yeti 2 e yeti e 6 golem", Rank.SOLDIER),
                    new WaveDefinition("5 simple 3 e veteran 3 yeti 3 e drip 4 courier e 6 bulky", Rank.SOLDIER),
                    new WaveDefinition("3 flank bulky 3 flank ghost 3 flank bulky 3 flank ghost 3 flank courier", Rank.SOLDIER),
                    new WaveDefinition("veteran magma e 2 bulky 8 simple e veteran jammer e 7 armored e 6 flank simple mender", Rank.SOLDIER),
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
            .withCustomRankedEnemies(Stream.concat(Stream.of(WARDER, BULKY), WardenEnemy.chain().stream()).toList());

    private CurlyPathLevel() {
    }

    private static EnemyDefinition warding(EnemyDefinition mender, float points) {
        return mender
                .withAdditionalAbilities(List.of(IdentifiedAbility.named("ward", new Ability(AbilityTerm.WARD,
                        new PeriodicTrigger(WARD_INTERVAL_TICKS),
                        new ApplyEffectAction(new ShieldTemplate(points, WARD_TICKS), new RadiusTarget(WARD_RADIUS))))));
    }
}
