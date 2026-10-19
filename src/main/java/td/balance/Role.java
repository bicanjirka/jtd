package td.balance;

import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * A job the defence does, by the towers and upgrade nodes that do it: what a signature wave asks for, and what a
 * player is denied to show that the wave asks for it. A tower that does two jobs through its two heads is banned
 * by node, so banning one job leaves it the other.
 */
enum Role {

    SINGLE_TARGET(EnumSet.of(TowerFactory.Type.SNIPER, TowerFactory.Type.SEEKER), Set.of()),
    AREA(EnumSet.of(TowerFactory.Type.SPLASH, TowerFactory.Type.MORTAR, TowerFactory.Type.CINDER,
            TowerFactory.Type.SONAR, TowerFactory.Type.PULSE), Set.of()),
    /** Magic damage and armor breaking: the Mortar goes whole, since every shell cracks plating. */
    ANTI_PHYSICAL(EnumSet.of(TowerFactory.Type.SEEKER, TowerFactory.Type.CINDER, TowerFactory.Type.SONAR,
            TowerFactory.Type.PULSE, TowerFactory.Type.MORTAR),
            Set.of("sniper.extra.tradecraft.3", "sniper.head.marksmans_eye.4b",
                    "splash.head.arc.1", "splash.head.hex.1", "splash.special.thunderstrike")),
    /** The Obelisk's base drag stays: it comes with every field, and is the least of what holds an enemy. */
    CONTROL(EnumSet.of(TowerFactory.Type.SEEKER),
            Set.of("pulse.head.gravity.1", "splash.head.arc.3", "splash.special.thunderstrike",
                    "mortar.head.siege_rounds.3", "mortar.special.tar", "mortar.special.cryo_shells",
                    "splash.head.hex.4a", "cinder.special.thermal_shock")),
    ANTI_SUSTAIN(EnumSet.noneOf(TowerFactory.Type.class),
            Set.of("pulse.head.ward.1", "pulse.special.soul_drain", "mortar.special.tar",
                    "mortar.head.siege_rounds.4a", "splash.head.hex.1", "splash.special.inversion",
                    "cinder.extra.fuel.2", "cinder.head.white_flame.3", "seeker.extra.mixed_payloads.2",
                    "seeker.special.nullifier", "sonar.extra.frequency.4", "sniper.head.marksmans_eye.3"));

    private final Set<TowerFactory.Type> types;
    /** Nodes that do the job; the nodes after one in its line do it too, since they can't be had without it. */
    private final Set<String> nodes;

    Role(Set<TowerFactory.Type> types, Set<String> nodes) {
        this.types = types;
        this.nodes = nodes;
    }

    static Role named(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "single", "single-target" -> SINGLE_TARGET;
            case "area" -> AREA;
            case "physical", "anti-physical" -> ANTI_PHYSICAL;
            case "control" -> CONTROL;
            case "sustain", "anti-sustain" -> ANTI_SUSTAIN;
            default -> throw new IllegalArgumentException("Unknown role " + name
                    + "; use single, area, physical, control or sustain");
        };
    }

    String label() {
        return this.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** The tower types a player denied this role may still build. */
    Set<TowerFactory.Type> allowedTypes() {
        EnumSet<TowerFactory.Type> allowed = EnumSet.allOf(TowerFactory.Type.class);
        allowed.removeAll(this.types);
        return allowed;
    }

    /** The nodes a player denied this role may not buy, where its tower may still be built. */
    Set<String> bannedNodes() {
        return this.nodes;
    }

    /** Whether {@code tower} does this job as it stands: its type does, or a node it owns does. */
    boolean doneBy(Tower tower) {
        return this.types.contains(tower.getType())
                || tower.upgrades().owned().stream().map(UpgradeNode::id).anyMatch(this.nodes::contains);
    }
}
