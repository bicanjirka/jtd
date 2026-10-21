package td.enemy;

import td.term.Term;
import td.term.TermText;

import java.util.Locale;

/** An ability an enemy fires, named for the player. */
public enum AbilityTerm implements Term {
    BRACE("Brace", "shields itself against physical hits once badly hurt"),
    CAPACITOR("Capacitor", "builds a charge that shields it against magic, again and again"),
    BULWARK("Bulwark", "shields itself against magic once badly hurt"),
    SHIELD_WALL("Shield Wall", "shields the allies near it against physical hits"),
    GUARD("Guard", "shields itself against physical hits, again and again"),
    ARCANE_WARD("Arcane Ward", "wards itself against magic from time to time"),
    AEGIS("Aegis", "shields the allies near it once badly hurt"),
    HARDEN("Harden", "answers each crit it survives with a shield"),
    PANIC("Panic", "throws up a shield once badly hurt"),
    WARD("Ward", "shields the allies near it from time to time"),
    MEND("Mend", "heals the allies near it"),
    RECOVER("Recover", "heals itself when it is left alone"),
    LAST_RITES("Last Rites", "heals the allies near it as it dies"),
    SHED_SKIN("Shed Skin", "sheds its skin to heal once badly hurt"),
    HUNKER("Hunker", "hunkers down to heal once badly hurt"),
    BOLT("Bolt", "bursts into speed the first time it is hit"),
    SIDESTEP("Sidestep", "speeds off each time it dodges"),
    SECOND_WIND("Second Wind", "speeds up once badly hurt"),
    FADE("Fade", "turns invisible the first time it is hit"),
    SHROUD("Shroud", "hides the allies near it"),
    PHASE("Phase", "turns invisible from time to time"),
    VANISH("Vanish", "turns invisible each time it dodges"),
    REINFORCE("Reinforce", "calls reinforcements"),
    SWARM("Swarm", "calls spawnlings once badly hurt"),
    BROOD("Brood", "bursts into broodlings when it dies"),
    SPLIT("Split", "splits in two when it dies"),
    LAY_EGG("Lay Egg", "leaves an egg when it dies"),
    HATCH("Hatch", "hatches unless it is destroyed in time");

    private final String title;
    private final String sentence;

    AbilityTerm(String title, String sentence) {
        this.title = title;
        this.sentence = sentence;
    }

    @Override
    public String key() {
        return "ability." + this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String title() {
        return this.title;
    }

    @Override
    public TermText describe() {
        return TermText.of(this.sentence);
    }
}
