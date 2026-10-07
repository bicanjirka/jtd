package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.AuraTower;
import td.tower.SniperTower;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class BuffedTowersConditionTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void countsOnlyTheOtherTowersTheTowerBuffs() {
        AuraTower aura = new AuraTower(this.context, 0, 0);
        this.context.towers().add(aura);
        this.context.towers().add(new SniperTower(this.context, 0, 0));
        this.context.towers().add(new SniperTower(this.context, 0, 0));
        this.context.towers().add(new SniperTower(this.context, 30, 30));

        assertThat(new BuffedTowersCondition(2).isSatisfied(aura, this.context)).isTrue();
        assertThat(new BuffedTowersCondition(3).isSatisfied(aura, this.context)).isFalse();
    }

    @Test
    void aTowerThatBuffsNobodyNeverSatisfiesIt() {
        SniperTower sniper = new SniperTower(this.context, 0, 0);
        this.context.towers().add(sniper);
        this.context.towers().add(new SniperTower(this.context, 0, 0));

        assertThat(new BuffedTowersCondition(1).isSatisfied(sniper, this.context)).isFalse();
    }

    @Test
    void describesAndShowsItsProgress() {
        AuraTower aura = new AuraTower(this.context, 0, 0);
        this.context.towers().add(aura);
        this.context.towers().add(new SniperTower(this.context, 0, 0));

        assertThat(new BuffedTowersCondition(4).describe()).isEqualTo("buffing 4 towers");
        assertThat(new BuffedTowersCondition(4).progress(aura, this.context)).isEqualTo("buffing 1/4 towers");
    }
}
