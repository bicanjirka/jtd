package td.effect;

import org.junit.jupiter.api.Test;
import td.stat.EnemyStat;
import td.stat.StatModifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EffectKindTest {

    /**
     * Every kind as the old switch methods classified it: kind, category, resisted by, max stacks,
     * paced by spirit, stops, burning, spreads with a curse, shared by Sympathy, field effect,
     * decaying, fuel pool, stack debuff. Scorched and Sickened now cap at 100 and Soulfire decays.
     */
    private static final List<String> OLD_CLASSIFICATION = List.of(
            "CHILL,SOFT_CC,CHILL_RESIST,0,true,false,false,false,true,false,true,false,false",
            "BURN,DAMAGE_OVER_TIME,BURN_RESIST,0,false,false,true,false,false,false,true,true,false",
            "FREEZE,HARD_CC,FREEZE_RESIST,0,false,true,false,false,false,false,false,false,false",
            "SHIELD,RESTORATIVE,-,0,false,false,false,false,false,false,false,false,false",
            "INVISIBLE,STEALTH,-,0,false,false,false,false,false,false,false,false,false",
            "HEAL,RESTORATIVE,-,0,false,false,false,false,false,false,false,false,false",
            "VULNERABLE,DEBUFF,-,3,true,false,false,true,true,false,false,false,false",
            "REVEALED,STEALTH,-,0,true,false,false,false,false,false,false,false,false",
            "POISON,DAMAGE_OVER_TIME,-,0,false,false,false,true,false,false,true,true,false",
            "SCORCHED,DEBUFF,-,100,false,false,false,true,false,false,true,false,true",
            "SICKENED,DEBUFF,-,100,false,false,false,true,false,false,true,false,true",
            "SUNDERED,DEBUFF,-,10,true,false,false,true,true,false,false,false,false",
            "EXPOSED,SPOTTED,-,0,true,false,false,true,true,false,false,false,false",
            "MARKED,SPOTTED,-,0,true,false,false,false,false,false,false,false,false",
            "PRIORITY,SPOTTED,-,0,true,false,false,false,false,false,false,false,false",
            "RESONATING,DEBUFF,-,3,true,false,false,true,false,false,false,false,false",
            "FRACTURED,DEBUFF,-,5,false,false,false,true,false,false,true,false,true",
            "DAZED,HARD_CC,-,0,false,true,false,false,false,false,false,false,false",
            "SATURATED,DEBUFF,-,3,false,false,false,false,false,false,false,false,false",
            "CHARGED,SPOTTED,-,0,true,false,false,false,false,false,false,false,false",
            "DOOM,HEX,-,0,true,false,false,false,false,false,false,false,false",
            "BLIGHT,HEX,-,0,true,false,false,false,false,false,false,false,false",
            "CONTAGION,HEX,-,0,true,false,false,false,false,false,false,false,false",
            "RIME,HEX,-,0,true,false,false,false,false,false,false,false,false",
            "ASH,HEX,-,0,true,false,false,false,false,false,false,false,false",
            "INVERSION,HEX,-,0,true,false,false,false,false,false,false,false,false",
            "SYMPATHY,HEX,-,0,true,false,false,false,false,false,false,false,false",
            "RECKONING,HEX,-,0,true,false,false,false,false,false,false,false,false",
            "SILENCED,SOFT_CC,-,0,false,false,false,false,false,true,false,false,false",
            "ANCHORED,SOFT_CC,-,0,false,false,false,false,false,true,false,false,false",
            "UNRAVELED,DEBUFF,-,5,true,false,false,true,true,false,false,false,false",
            "BRITTLE,DEBUFF,-,0,true,false,false,false,false,false,false,false,false",
            "TOLL,DEBUFF,-,5,false,false,false,false,false,false,false,false,false",
            "CORRODED,DEBUFF,-,0,false,false,false,false,false,true,false,false,false",
            "UNDERTOW,SOFT_CC,-,0,false,false,false,false,false,true,false,false,false",
            "DEAD_ZONE,DEBUFF,-,0,false,false,false,false,false,true,false,false,false",
            "FALLOUT,DEBUFF,-,0,false,false,false,false,false,false,false,false,false",
            "KILL_ZONE,DEBUFF,-,0,false,false,false,false,false,true,false,false,false",
            "CRACKED,DEBUFF,-,0,true,false,false,false,false,false,false,false,false",
            "TARRED,DEBUFF,-,0,false,false,false,false,false,false,false,false,false",
            "BLEEDING,DAMAGE_OVER_TIME,-,0,false,false,false,false,false,false,false,false,false",
            "SOULFIRE,DAMAGE_OVER_TIME,-,0,false,false,true,false,false,false,true,true,false");

    @Test
    void everyKindReproducesItsOldClassification() {
        assertThat(OLD_CLASSIFICATION).hasSize(EffectKind.values().length);
        for (String row : OLD_CLASSIFICATION) {
            String[] cell = row.split(",");
            EffectKind kind = EffectKind.valueOf(cell[0]);
            String now = String.join(",", kind.name(), kind.category().name(),
                    kind.resistedBy().map(Enum::name).orElse("-"), String.valueOf(kind.maxStacks()),
                    String.valueOf(kind.isPacedBySpirit()), String.valueOf(kind.stopsEnemy()),
                    String.valueOf(kind.isBurning()), String.valueOf(kind.spreadsWithCurse()),
                    String.valueOf(kind.sharedBySympathy()), String.valueOf(kind.isFieldEffect()),
                    String.valueOf(kind.isDecaying()), String.valueOf(kind.isFuelPool()),
                    String.valueOf(kind.isStackDebuff()));

            assertThat(now).as(kind.name()).isEqualTo(row);
        }
    }

    @Test
    void modifiersAtScaleWithStacksUpToTheCap() {
        assertThat(EffectKind.SUNDERED.modifiersAt(3).get(EnemyStat.ARMOR)).isEqualTo(StatModifier.flat(-15f));
        assertThat(EffectKind.VULNERABLE.modifiersAt(0).isNone()).isTrue();
        assertThat(EffectKind.TARRED.modifiersAt(1).get(EnemyStat.MOVE_SPEED)).isEqualTo(StatModifier.times(0.6f));
        assertThat(EffectKind.SCORCHED.modifiersAt(500).get(EnemyStat.RESILIENCE)).isEqualTo(StatModifier.flat(-100f));
    }
}
