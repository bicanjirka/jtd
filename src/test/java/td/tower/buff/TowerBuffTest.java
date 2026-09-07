package td.tower.buff;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TowerBuffTest {

    @Test
    void noneIsTheIdentityElementOnBothSides() {
        TowerBuff buff = TowerBuff.amplifying(0.2f);

        assertThat(TowerBuff.none().combine(buff)).isEqualTo(buff);
        assertThat(buff.combine(TowerBuff.none())).isEqualTo(buff);
    }

    @Test
    void combineIsAdditive() {
        TowerBuff a = TowerBuff.amplifying(0.1f);
        TowerBuff b = TowerBuff.amplifying(0.3f);

        TowerBuff combined = a.combine(b);

        assertThat(combined.damageBonus()).isEqualTo(0.4f);
        assertThat(combined.rangeBonus()).isEqualTo(0.4f);
    }

    @Test
    void reducingAnEmptyStreamYieldsNone() {
        TowerBuff reduced = List.<TowerBuff>of().stream().reduce(TowerBuff.none(), TowerBuff::combine);

        assertThat(reduced).isEqualTo(TowerBuff.none());
    }

    @Test
    void reducingThreeEqualBuffsStacksThemAdditively() {
        List<TowerBuff> buffs = List.of(
                TowerBuff.amplifying(0.2f), TowerBuff.amplifying(0.2f), TowerBuff.amplifying(0.2f));

        TowerBuff reduced = buffs.stream().reduce(TowerBuff.none(), TowerBuff::combine);

        assertThat(reduced.damageBonus()).isCloseTo(0.6f, org.assertj.core.data.Offset.offset(1e-6f));
    }

    @Test
    void damageForAndRangeForApplyTheBonusAsAMultiplier() {
        TowerBuff buff = TowerBuff.amplifying(0.5f);

        assertThat(buff.damageFor(100)).isEqualTo(150);
        assertThat(buff.rangeFor(2f)).isEqualTo(3f);
    }
}
