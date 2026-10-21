package td.tower.splash;

import td.effect.EffectKind;
import td.term.Term;
import td.tower.upgrade.PerkText;
import td.util.TickRate;

import java.util.Set;

/** Thunderstrike: every sixth shot calls lightning onto the healthiest enemy in range. */
public final class ThunderstrikePerk implements SplashPerk {

    public static final int INTERVAL = 6;
    private static final float FACTOR = 4f;

    @Override
    public SplashShot shape(SplashShot shot, ShotContext context) {
        return context.shotNumber() % INTERVAL == 0 ? shot.withThunderstrike(FACTOR) : shot;
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.DAZED);
    }

    @Override
    public String describe() {
        return "every " + PerkText.ordinal(INTERVAL) + " shot calls lightning onto the healthiest enemy in range: " + PerkText.times(FACTOR)
                + " the blast as magic and Dazed " + PerkText.seconds(Math.round(SplashShot.DAZE_SECONDS * TickRate.TICKS_PER_SECOND))
                + "; the shot's arcs start there at full damage; counts as a crit";
    }
}
