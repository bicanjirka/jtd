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
 * A buff, debuff or named mechanic of a tower, named for the player. A sentence mentions effect kinds, and tower terms
 * declared above it; its {@code {}} slots take them in order.
 */
public enum TowerTerm implements Term {
    STEADY_AIM("Steady Aim", Side.BUFF, "each shot adds a stack, kept across targets and kills; " + TermNumbers.seconds(AimFocus.STACK_SECONDS)
            + " after the last shot a stack fades, then the next"),
    FRENZY("Frenzy", Side.BUFF, "faster attacks for the next few shots; a second one starts them over"),
    HASTE("Haste", Side.BUFF, "faster attacks for a while"),
    FOURTH_SHOT("Fourth Shot", Side.BUFF, "a shot that is sure to crit"),
    SILVER_ROUND("Silver Round", Side.BUFF, "a shot of magic damage that ignores some magic resist"),
    HOLLOW_POINT("Hollow Point", Side.BUFF, "a shot that hits harder"),
    HEAVY_SHELL("Heavy", Side.BUFF, "a shell that hits harder than a Normal one and flies slower"),
    NUKE_SHELL("Nuke", Side.BUFF, "two {} shells fused: a harder hit in a wider blast that leaves fallout applying {} and {}, and a flash that gives every enemy on the board {}", HEAVY_SHELL, EffectKind.DEAD_ZONE, EffectKind.SICKENED, EffectKind.IRRADIATED),
    PAYLOAD("Payload", Side.BUFF, "a special shell: the Mortar's damage as magic, and ground it leaves where it lands"),
    NAPALM_SHELL("Napalm", Side.BUFF, "a {} that leaves flames that apply {}", PAYLOAD, EffectKind.BURN),
    TAR_SHELL("Tar Shell", Side.BUFF, "a {} that leaves tar applying {} and {}", PAYLOAD, EffectKind.TARRED, EffectKind.POISON),
    CRYO_SHELL("Cryo", Side.BUFF, "a {} that leaves frost dealing magic damage and applying {}, and {} to an enemy that stays", PAYLOAD, EffectKind.CHILL, EffectKind.FREEZE),
    FLARE("Flare", Side.BUFF, "a Normal shell that also leaves a flare lasting until the wave ends; every enemy that enters it gains {}", EffectKind.REVEALED),
    BARRAGE("Barrage", Side.BUFF, "the front shells loaded together and fired one after another at one target"),
    CARPET_BOMBING("Carpet Bombing", Side.BUFF, "the front shells loaded together, a Normal between each two, and laid along the path with the middle one on the target"),
    NEST("Nest", Side.BUFF, "the missiles it holds ready: it loads one whenever it has room, so it is full at every "
            + "wave start, and with an enemy in range they launch one after another, each picking its own target"),
    SHADOW("Shadow", Side.BUFF, "magic damage that goes past {} and insulation; magic resist still holds",
            EffectKind.SHIELD),
    FROST_MISSILE("Frost", Side.BUFF, "the plain missile: it applies {} to the enemy it hits", EffectKind.FREEZE),
    RUBY("Ruby", Side.BUFF, "a missile that deals more damage, and whose {} also holds an enemy immune to it",
            EffectKind.FREEZE),
    SHADE("Shade", Side.BUFF, "a missile that deals {} damage and more of it, applies {} for as long as its {}",
            SHADOW, EffectKind.WITHERED, EffectKind.FREEZE),
    SWARM("Swarm", Side.BUFF, "missiles at once, one at each enemy in range, soonest out first, and round again while "
            + "missiles are left"),
    HATCHING("Hatching", Side.BUFF, "missiles that hatch on top of the {}, one after another, and launch first while it "
            + "keeps loading", NEST),
    ICE_WAKE("Ice Wake", Side.BUFF, "while a {} the tower landed holds, every enemy walking close past the ice gains {}",
            EffectKind.FREEZE, EffectKind.CHILL),
    LIGHT("Light", Side.BUFF, "a Beacon's circle: every enemy in it gains {}, while inside and a moment after, and "
            + "every other tower whose centre is in it takes less of the jam; lights that overlap stack", EffectKind.REVEALED),
    LIT("Lit", Side.BUFF, "in a Beacon's {}: it takes less of the jam and the blinding enemies put on it", LIGHT),
    SPOTLIGHT("Spotlight", Side.BUFF, "the lantern's beam holds one enemy in the light until it dies or leaves, then "
            + "picks again; the held enemy has {}, so every tower that picks one target turns to it",
            EffectKind.PRIORITY),
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
    KINSHIP("Kinship", Side.BUFF, "towers in a Beacon's {} learn faster for every other kind of tower in the same light",
            LIGHT),
    MENTOR("Mentor", Side.BUFF, "the tower in the light that has learned least learns twice as fast"),
    BREAKTHROUGH("Breakthrough", Side.BUFF, "a tower in the light opening a tier teaches every other tower in it a little"),
    DAYLIGHT("Daylight", Side.BUFF, "an enemy in the light gives its allies no aura: no shroud, command, mending, ward "
            + "or jam reaches out of it"),
    PRISM("Prism", Side.BUFF, "the light carries the mark of every kind of tower it lights: every enemy inside holds "
            + "that tower's debuff, which fades by its own rule once it leaves"),
    SHATTER("Shatter", Side.BUFF, "as a {} the tower landed ends, the ice bursts on the enemies around it",
            EffectKind.FREEZE),
    ARC("Arc", Side.BUFF, "lightning a primed stone carries from where it lands, jumping from enemy to enemy as magic"),
    SPARK("Spark", Side.BUFF, "the Stormcaller's currency: its stones earn it by the damage they deal, its passive by the enemies that discharge; all of it goes into each of its abilities at once"),
    SOUL("Soul", Side.BUFF, "the Hexer's currency: its stones earn it by the damage they deal, its passive by the enemies that die; all of it goes into each of its abilities at once"),
    HEX("Hex", Side.BUFF, "a curse a Hexer casts with its {}; each kind does its own thing", SOUL),
    STATIC("Static", Side.BUFF, "every enemy its stones or their lightning damage carries a stack, one per Stormcaller; badly hurt, the enemy discharges into itself for each stack and pays each Stormcaller that tagged it in {}", SPARK),
    SOULBRAND("Soulbrand", Side.BUFF, "every enemy its stones damage carries a stack, one per Hexer; when it dies it bursts for each stack, and pays each Hexer that tagged it in {}", SOUL),
    SHOCKWAVE("Shockwave", Side.BUFF, "a discharge into every enemy in range, as magic"),
    THUNDERSTRIKE("Thunderstrike", Side.BUFF, "lightning onto the healthiest enemy in range, as magic");

    private final String title;
    private final Side side;
    private final String sentence;
    private final List<Term> mentions;

    TowerTerm(String title, Side side, String sentence, Term... mentions) {
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
        return TermText.format(this.sentence, this.mentions.toArray(Term[]::new));
    }

    /** Whether it helps the tower that carries it or hurts it. */
    public Side side() {
        return this.side;
    }
}
