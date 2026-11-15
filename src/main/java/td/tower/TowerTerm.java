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
    FOCUS("Focus", Side.BUFF, "hits on one target build it; it starts over on a new target"),
    FRENZY("Frenzy", Side.BUFF, "faster attacks for the next few shots; a second one starts them over"),
    HASTE("Haste", Side.BUFF, "faster attacks for a while"),
    FOURTH_SHOT("Fourth Shot", Side.BUFF, "a shot that is sure to crit"),
    SILVER_ROUND("Silver Round", Side.BUFF, "a shot of magic damage that ignores some magic resist"),
    HOLLOW_POINT("Hollow Point", Side.BUFF, "a shot that hits harder"),
    MAGAZINE("Magazine", Side.BUFF, "the shells it has ready, in the order it fires them; a new one at the back draws its kind by lot, each kind as likely as its weight among the kinds it owns"),
    HEAVY_SHELL("Heavy", Side.BUFF, "a shell that hits harder than a Normal one in the same blast, and flies slower"),
    NUKE_SHELL("Nuke", Side.BUFF, "two Heavies fused: their damage together in a wider blast that leaves fallout applying {}, {} and {}", EffectKind.DEAD_ZONE, EffectKind.SICKENED, EffectKind.POISON),
    PAYLOAD("Payload", Side.BUFF, "a special shell: half a Normal one's damage, and ground it leaves where it lands"),
    NAPALM_SHELL("Napalm", Side.BUFF, "a payload whose blast is magic and leaves flames that apply {}", EffectKind.BURN),
    TAR_SHELL("Tar Shell", Side.BUFF, "a payload that leaves tar applying {} and {}", EffectKind.TARRED, EffectKind.POISON),
    CRYO_SHELL("Cryo", Side.BUFF, "a payload that leaves frost applying {}, and {} to an enemy that stays", EffectKind.CHILL, EffectKind.FREEZE),
    FLARE("Flare", Side.BUFF, "a shell that leaves a flare lasting until the wave ends; every enemy that enters it gains {}", EffectKind.REVEALED),
    BARRAGE("Barrage", Side.BUFF, "the front shells loaded together and fired one after another at one target"),
    CARPET_BOMBING("Carpet Bombing", Side.BUFF, "the front shells loaded together and laid along the path, from the target back toward where the enemies come from"),
    CRYO_MISSILE("Cryo Missile", Side.BUFF, "a missile that applies {}", EffectKind.FREEZE),
    ARCANE_MISSILE("Arcane Missile", Side.BUFF, "a missile that applies {} instead of {}", EffectKind.UNRAVELED, EffectKind.FREEZE),
    EMP_MISSILE("EMP Missile", Side.BUFF, "a missile that removes {} and {} and applies {}", EffectKind.SHIELD, EffectKind.HEAL, EffectKind.SILENCED),
    TRACER_MISSILE("Tracer Missile", Side.BUFF, "a missile that applies {} and {}", EffectKind.REVEALED, EffectKind.MARKED),
    RALLY("Rally", Side.BUFF, "faster attacks for a while, from a Beacon's rally spot"),
    AURA("Aura", Side.BUFF, "a Beacon in range raises its damage and range, and takes some of the jam enemies put on it"),
    NEST("Nest", Side.BUFF, "missiles stored to launch one after another when a target comes"),
    JAMMED("Jammed", Side.DEBUFF, "an enemy nearby slows its attacks and shortens its range, or its blast makes the tower miss"),
    FLAME("Flame", Side.BUFF, "fuel a second the stream pours into the {} of each enemy in the cone", EffectKind.BURN),
    STREAM("Stream", Side.BUFF, "how long it pours before it reloads; it keeps its target the whole stream while that enemy "
            + "lives and stays in range, and with no enemy left in range it runs its time out into the air"),
    CORE("Core", Side.BUFF, "the middle of the cone's width, where its target always stands; it pours more"),
    DRAGONS_BREATH("Dragon's Breath", Side.BUFF, "a longer stream with a short reload, at a weaker flame"),
    FLASHOVER("Flashover", Side.BUFF, "a hot {} in the stream spreads: the enemies beside it take part of the flame too",
            EffectKind.BURN),
    THERMAL_SHOCK("Thermal Shock", Side.BUFF, "more flame into an enemy another tower holds with {}, {} or {}",
            EffectKind.CHILL, EffectKind.FREEZE, EffectKind.DAZED),
    MELTDOWN("Meltdown", Side.BUFF, "an enemy's armor and plating add to the {} it takes, a share for each point",
            EffectKind.BURN),
    SUNDIAL("Sundial", Side.BUFF, "the stone's shadow, turning once around it every few seconds, past the field; every "
            + "enemy it passes takes an aimed hit and a stack of {}", EffectKind.TOLL),
    FRAILTY("Frailty", Side.BUFF, "each stack of {} makes the enemy take more damage from every source", EffectKind.TOLL),
    SHADOW("Shadow", Side.BUFF, "magic damage that goes past {} and insulation; magic resist still holds",
            EffectKind.SHIELD),
    KINSHIP("Kinship", Side.BUFF, "towers a Beacon buffs earn more XP for each other tower type in range"),
    SHATTER("Shatter", Side.BUFF, "an enemy with {} breaks apart and hurts the enemies around it", EffectKind.FREEZE),
    ARC("Arc", Side.BUFF, "lightning a primed stone carries from where it lands, jumping from enemy to enemy as magic"),
    HEX("Hex", Side.BUFF, "a curse a Hexer casts with its Souls; each kind does its own thing"),
    STATIC("Static", Side.BUFF, "every enemy its stones or their lightning damage carries a stack, one per Stormcaller; badly hurt, the enemy discharges into itself for each stack and pays each Stormcaller that tagged it in its currency"),
    SOULBRAND("Soulbrand", Side.BUFF, "every enemy its stones damage carries a stack, one per Hexer; when it dies it bursts for each stack, and pays each Hexer that tagged it in its currency"),
    SPARK("Spark", Side.BUFF, "the Stormcaller's currency: its stones earn it by the damage they deal, its passive by the enemies that discharge; all of it goes into each of its abilities at once"),
    SOUL("Soul", Side.BUFF, "the Hexer's currency: its stones earn it by the damage they deal, its passive by the enemies that die; all of it goes into each of its abilities at once"),
    THUNDERCLAP("Thunderclap", Side.BUFF, "a discharge into every enemy in range, as magic"),
    THUNDERSTRIKE("Thunderstrike", Side.BUFF, "lightning onto the healthiest enemy in range, as magic");

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
