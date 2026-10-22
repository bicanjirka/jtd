package td.tower;

import td.effect.EffectKind;
import td.term.Side;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.sniper.AimFocus;

import java.util.List;
import java.util.Locale;

/**
 * A buff, debuff or named mechanic of a tower, named for the player. A sentence mentions only effect kinds; its
 * {@code {}} slots take them in order.
 */
public enum TowerTerm implements Term {
    STEADY_AIM("Steady Aim", Side.BUFF, "each shot adds a stack, kept across targets and kills; " + TermNumbers.seconds(AimFocus.STACK_SECONDS)
            + " after the last shot a stack fades, then the next"),
    CRIT_STREAK("Crit Streak", Side.BUFF, "crits in a row make the next crit hit harder"),
    FOCUS("Focus", Side.BUFF, "hits on one target build it; it starts over on a new target"),
    FRENZY("Frenzy", Side.BUFF, "faster attacks for the next few shots"),
    HASTE("Haste", Side.BUFF, "faster attacks for a while"),
    CHARGED_SHOT("Charged Shot", Side.BUFF, "the next shot hits far harder and ignores armor and plating"),
    PATIENCE("Patience", Side.BUFF, "the next shot is a crit"),
    DEADEYE_ROUND("Deadeye Round", Side.BUFF, "a shot that is a crit and hits harder"),
    SILVER_ROUND("Silver Round", Side.BUFF, "a shot of magic damage that ignores some magic resist"),
    BRACKETING("Bracketing", Side.BUFF, "each shell landing near the last hits harder and wider, a step at a time; a shell elsewhere clears it"),
    NAPALM_SHELL("Napalm Shell", Side.BUFF, "a shell whose blast is magic and leaves flames that apply {}", EffectKind.BURN),
    TAR_SHELL("Tar Shell", Side.BUFF, "a shell that leaves tar applying {} and {}", EffectKind.TARRED, EffectKind.POISON),
    CRYO_SHELL("Cryo Shell", Side.BUFF, "a shell that leaves frost applying {}, and {} to an enemy that stays", EffectKind.CHILL, EffectKind.FREEZE),
    NUKE_SHELL("Nuke Shell", Side.BUFF, "a shell that hits far harder and wider and leaves {}", EffectKind.FALLOUT),
    CRYO_MISSILE("Cryo Missile", Side.BUFF, "a missile that applies {}", EffectKind.FREEZE),
    ARCANE_MISSILE("Arcane Missile", Side.BUFF, "a missile that applies {} instead of {}", EffectKind.UNRAVELED, EffectKind.FREEZE),
    EMP_MISSILE("EMP Missile", Side.BUFF, "a missile that removes {} and {} and applies {}", EffectKind.SHIELD, EffectKind.HEAL, EffectKind.SILENCED),
    TRACER_MISSILE("Tracer Missile", Side.BUFF, "a missile that applies {} and {}", EffectKind.REVEALED, EffectKind.MARKED),
    EVENT_HORIZON("Event Horizon", Side.BUFF, "each death inside the field widens it until the wave ends"),
    RALLY("Rally", Side.BUFF, "faster attacks for a while, from a Beacon's rally spot"),
    AURA("Aura", Side.BUFF, "a Beacon in range raises its damage and range, and takes some of the jam enemies put on it"),
    LEY_LINE("Ley Line", Side.BUFF, "a beam between two Obelisks; an enemy on it gets both fields' rules"),
    NEST("Nest", Side.BUFF, "missiles stored to launch one after another when a target comes"),
    JAMMED("Jammed", Side.DEBUFF, "an enemy nearby slows its attacks and shortens its range, or its blast makes the tower miss"),
    INTERFERENCE("Interference", Side.DEBUFF, "other Obelisk fields overlapping this one take some of its damage away"),
    STOKE("Stoke", Side.DEBUFF, "each wave on an enemy with this tower's {} raises that {} a step; the steps go when it ends", EffectKind.BURN, EffectKind.BURN),
    KINSHIP("Kinship", Side.BUFF, "towers a Beacon buffs earn more XP for each other tower type in range"),
    PING("Ping", Side.BUFF, "each revolution, the Radar applies {} to the enemy with the most health its beam passes", EffectKind.EXPOSED),
    SHATTER("Shatter", Side.BUFF, "an enemy with {} breaks apart and hurts the enemies around it", EffectKind.FREEZE),
    ARC("Arc", Side.BUFF, "lightning that jumps from a blast to enemies past its edge, as magic"),
    HEX("Hex", Side.BUFF, "a curse a Burst casts instead of a blast; each kind does its own thing"),
    SHRAPNEL("Shrapnel", Side.BUFF, "fragments that fly past a shell's blast"),
    BOMBLET("Bomblet", Side.BUFF, "a small bomb a shell drops on the path");

    private final String title;
    private final Side side;
    private final String sentence;
    private final List<EffectKind> mentions;

    TowerTerm(String title, Side side, String sentence, EffectKind... mentions) {
        this.title = title;
        this.side = side;
        this.sentence = sentence;
        this.mentions = List.of(mentions);
    }

    @Override
    public String key() {
        return "tower." + this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String title() {
        return this.title;
    }

    @Override
    public TermText describe() {
        return TermText.format(this.sentence, this.mentions.toArray(EffectKind[]::new));
    }

    /** Whether it helps the tower that carries it or hurts it. */
    public Side side() {
        return this.side;
    }
}
