package td.tower.splash;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

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
    public TermText describe() {
        return TermText.of("every " + TermNumbers.ordinal(INTERVAL)
                + " shot calls lightning onto the enemy in range with the most health: " + TermNumbers.times(FACTOR)
                + " the blast as magic, counted as a crit, and ").plus(EffectKind.DAZED)
                .plus(" for " + TermNumbers.seconds(SplashShot.DAZE_SECONDS) + "; the shot's ").plus(TowerTerm.ARC)
                .plus(" starts there at full damage");
    }
}
