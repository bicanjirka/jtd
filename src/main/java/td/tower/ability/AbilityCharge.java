package td.tower.ability;

import td.util.ThreadConfined;

/**
 * One owned ability charging up. It lives with the mechanic that brought it, so a mechanic renewed for a new wave
 * starts its bar over.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class AbilityCharge {

    private final Ability ability;
    private AbilityBar bar;

    public AbilityCharge(Ability ability) {
        this.ability = ability;
        this.bar = AbilityBar.atWaveStart(ability.cost());
    }

    public Ability ability() {
        return this.ability;
    }

    /** Adds what the tower earned; what the bar cannot hold is lost. */
    public void earn(AbilityUnits earned) {
        this.bar = this.bar.earning(earned);
    }

    /** Whether one cast's cost is in. */
    public boolean isReady() {
        return this.bar.isReady();
    }

    /** Pays one cast's cost; call it only for a cast that happened. */
    public void spend() {
        this.bar = this.bar.afterCast();
    }

    public AbilityState state() {
        return new AbilityState(this.ability, this.bar);
    }
}
