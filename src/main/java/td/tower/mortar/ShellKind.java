package td.tower.mortar;

import td.damage.DamageType;
import td.damage.DamageUnits;
import td.effect.EffectKind;
import td.projectile.ShellLook;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerStat;
import td.tower.TowerTerm;
import td.util.TickRate;
import td.zone.Zone;
import td.zone.ZoneKind;
import td.zone.ZonePulse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One kind of shell in the Mortar's magazine: how it looks, how hard and how wide it hits against a Normal shell, how
 * fast it flies, and the ground it leaves where it lands. Every kind hits for the Mortar's damage plus points of its
 * own, so whatever raises the Mortar's damage raises every kind alike; a Normal's and a Heavy's own are stats.
 */
public enum ShellKind {
    /** What it deals past the Mortar's damage is its Normal damage, a stat. */
    NORMAL(ShellLook.NORMAL, DamageType.PHYSICAL, 0f, Blast.NORMAL, 1f, Optional.empty()),
    /** What it deals past the Mortar's damage is its Heavy damage, a stat, and its blast widens by its Heavy blast. */
    HEAVY(ShellLook.HEAVY, DamageType.PHYSICAL, 0f, Blast.NORMAL, 0.75f, Optional.empty()),
    /**
     * Two Heavies fused: a harder hit than a Heavy, a blast of its own, fallout over most of it, and a flash that gives
     * every enemy on the board Radiation.
     */
    NUKE(ShellLook.NUKE, DamageType.PHYSICAL, 0f, new Blast(1.6f, 1.6f, 0.75f), 0.75f,
            Optional.of(Ground.ofBlast(ZoneKind.FALLOUT, 0.8f, 140, 0f))),
    /** A payload deals magic: its blast burns, and the ground where it lands too. */
    NAPALM(ShellLook.NAPALM, DamageType.MAGIC, 0f, Blast.NORMAL, 1.25f,
            Optional.of(Ground.fixed(ZoneKind.BURNING_GROUND, 1f, 60, 35f))),
    TAR(ShellLook.TAR, DamageType.MAGIC, 0f, Blast.NORMAL, 1.25f,
            Optional.of(Ground.fixed(ZoneKind.TAR, 1.2f, 60, 18f).withSlow(0.4f, 1f))),
    CRYO(ShellLook.CRYO, DamageType.MAGIC, 0f, Blast.NORMAL, 1.25f,
            Optional.of(Ground.fixed(ZoneKind.FROST_GROUND, 1.6f, 100, 2.5f).withSlow(0.4f, 2f).withFreeze(4, 1.5f))),
    /** A Normal shell in all but the flare it leaves, which burns until the wave is over. */
    FLARE(ShellLook.FLARE, DamageType.PHYSICAL, 0f, Blast.NORMAL, 1f,
            Optional.of(Ground.fixed(ZoneKind.FLARE, 1.5f, Zone.UNTIL_WAVE_END, 0f)));

    /** How many Heavies' damage a Nuke deals: less than the two it is made of. */
    public static final float NUKE_HEAVIES = 1.7f;

    /** The magic damage a second of the Radiation a Nuke's flash gives, as it starts. */
    public static final float RADIATION_POINTS_PER_SECOND = 8f;
    /** How long that Radiation takes to fall to half. */
    public static final int RADIATION_HALF_LIFE_TICKS = Math.round(2 * TickRate.TICKS_PER_SECOND);

    private final ShellLook look;
    private final DamageType damageType;
    private final float extraPoints;
    private final Blast blast;
    private final float speedFactor;
    private final Optional<Ground> ground;

    ShellKind(ShellLook look, DamageType damageType, float extraPoints, Blast blast, float speedFactor,
            Optional<Ground> ground) {
        this.look = look;
        this.damageType = damageType;
        this.extraPoints = extraPoints;
        this.blast = blast;
        this.speedFactor = speedFactor;
        this.ground = ground;
    }

    public ShellLook look() {
        return this.look;
    }

    public DamageType damageType() {
        return this.damageType;
    }

    /** How its blast differs from a Normal shell's. */
    public Blast blast() {
        return this.blast;
    }

    /** How fast it flies, as a share of a Normal shell's speed. */
    public float speedFactor() {
        return this.speedFactor;
    }

    public Optional<Ground> ground() {
        return this.ground;
    }

    /** Whether it flashes as it lands, giving every enemy on the board Radiation: a Nuke. */
    public boolean irradiatesTheBoard() {
        return this == NUKE;
    }

    /** What its flash gives every enemy on the board, said after "its flash gives every enemy on the board". */
    public TermText flashText() {
        return TermText.of(EffectKind.IRRADIATED).plus(" of " + TermNumbers.number(RADIATION_POINTS_PER_SECOND)
                + " magic damage a second, halving every "
                + TermNumbers.seconds(RADIATION_HALF_LIFE_TICKS / TickRate.TICKS_PER_SECOND));
    }

    /** The stat that says how long loading it takes: a Flare loads as a Normal, Napalm, Tar and Cryo as Payloads. */
    public TowerStat loadStat() {
        return switch (this) {
            case NORMAL, FLARE -> TowerStat.NORMAL_LOAD;
            case NAPALM, TAR, CRYO -> TowerStat.PAYLOAD_LOAD;
            case HEAVY -> TowerStat.HEAVY_LOAD;
            case NUKE -> TowerStat.NUKE_LOAD;
        };
    }

    /** A Normal or a Flare: a shell that loads as a Normal, and chains when loaded right after another. */
    public boolean isLight() {
        return this == NORMAL || this == FLARE;
    }

    /** Napalm, Tar or Cryo: the kinds a Payload node adds. */
    public boolean isPayload() {
        return this == NAPALM || this == TAR || this == CRYO;
    }

    /**
     * The points it deals past the Mortar's damage; a Normal's and a Flare's are the Mortar's Normal damage, a Heavy's
     * its Heavy damage, a Nuke's {@link #NUKE_HEAVIES} Heavies'.
     */
    public float extraPoints() {
        return this.extraPoints;
    }

    /**
     * What it deals, in hundredths of a point, for a Mortar that deals {@code base}, whose Normal deals
     * {@code normalExtra} more and whose Heavy deals {@code heavyExtra} more: a Flare deals as a Normal, a Nuke as
     * {@link #NUKE_HEAVIES} Heavies, any other kind its own points more.
     */
    public int damage(float base, float normalExtra, float heavyExtra) {
        float heavy = base + heavyExtra;
        return Math.round(switch (this) {
            case NORMAL, FLARE -> base + normalExtra;
            case HEAVY -> heavy;
            case NUKE -> NUKE_HEAVIES * heavy;
            case NAPALM, TAR, CRYO -> base + DamageUnits.ofPoints(this.extraPoints);
        });
    }

    /**
     * The cells its core and edge each reach past its blast's, for a Mortar whose Heavy blasts {@code heavyCells} wider
     * and whose payloads {@code payloadCells} wider.
     */
    public float wideningCells(float heavyCells, float payloadCells) {
        return switch (this) {
            case HEAVY -> heavyCells;
            case NAPALM, TAR, CRYO -> payloadCells;
            case NORMAL, NUKE, FLARE -> 0f;
        };
    }

    /** What it hits for, said after "each deals": "the Mortar's damage", "the Mortar's damage +4 as magic". */
    public String damageText() {
        return "the Mortar's damage" + (this.extraPoints == 0f ? "" : " " + TermNumbers.signed(this.extraPoints))
                + (this.damageType == DamageType.MAGIC ? " as magic" : "")
                + (this.isPayload() ? " (no Normal bonus)" : "");
    }

    /** Its name as the player reads it: "Normal", or its term. */
    public TermText mention() {
        return this.term().map(TermText::of).orElse(TermText.of(this.title()));
    }

    /** Its name, plain: "Normal", or its term's title. */
    public String title() {
        return this.term().map(TowerTerm::title).orElse("Normal");
    }

    /** The term that names it; a Normal shell has none. */
    public Optional<TowerTerm> term() {
        return switch (this) {
            case NORMAL -> Optional.empty();
            case HEAVY -> Optional.of(TowerTerm.HEAVY_SHELL);
            case NUKE -> Optional.of(TowerTerm.NUKE_SHELL);
            case NAPALM -> Optional.of(TowerTerm.NAPALM_SHELL);
            case TAR -> Optional.of(TowerTerm.TAR_SHELL);
            case CRYO -> Optional.of(TowerTerm.CRYO_SHELL);
            case FLARE -> Optional.of(TowerTerm.FLARE);
        };
    }

    /** The effects its blast and ground put on enemies, past the Crack every shell leaves. */
    public Set<EffectKind> applies() {
        return switch (this) {
            case NORMAL, HEAVY -> Set.of();
            case NUKE -> Set.of(EffectKind.DEAD_ZONE, EffectKind.SICKENED, EffectKind.IRRADIATED);
            case NAPALM -> Set.of(EffectKind.BURN);
            case TAR -> Set.of(EffectKind.TARRED, EffectKind.POISON);
            case CRYO -> Set.of(EffectKind.CHILL, EffectKind.FREEZE);
            case FLARE -> Set.of(EffectKind.REVEALED);
        };
    }

    /**
     * Where it lands, said after "each": "leaves tar 1.2 cells wide for 3 s that, every 0.5 s, adds 15.5 to Poison and
     * slows by 40% for 1 s".
     */
    public TermText groundText() {
        Ground left = this.ground.orElseThrow();
        String wide = TermNumbers.cells(left.radiusCells()) + " wide";
        return switch (this) {
            case NAPALM -> TermText.of("leaves flames " + wide + " for " + seconds(left)).plus(pulseText(left));
            case TAR -> TermText.of("leaves tar " + wide + " for " + seconds(left)).plus(pulseText(left));
            case CRYO -> TermText.of("leaves frost " + wide + " for " + seconds(left)).plus(pulseText(left));
            case FLARE -> TermText.of("leaves a flare " + wide + " that lasts until the wave ends");
            case NUKE -> TermText.of("leaves fallout over " + TermNumbers.percent(left.blastShare()) + " of its blast for "
                    + seconds(left));
            case NORMAL, HEAVY -> throw new IllegalStateException(this + " leaves no ground");
        };
    }

    private static String seconds(Ground ground) {
        return seconds(ground.lifetimeTicks());
    }

    private static String seconds(int ticks) {
        return TermNumbers.seconds(ticks / TickRate.TICKS_PER_SECOND);
    }

    /** What each pulse of {@code ground} does, from " that, every 0.5 s, " on; empty for one that does nothing. */
    private static TermText pulseText(Ground ground) {
        ZonePulse pulse = ground.pulse();
        List<TermText> parts = new ArrayList<>();
        String points = TermNumbers.number(ground.pulsePoints());
        if (pulse.strength() > 0) {
            ground.kind().fuels().ifPresent(pool -> parts.add(TermText.of("adds " + points + " to ").plus(pool)));
            if (ground.kind().hitsDirectly()) {
                parts.add(TermText.of("deals " + points + " magic damage"));
            }
        }
        if (pulse.slow().slows()) {
            ground.kind().slows().ifPresent(by -> parts.add(TermText.of("slows by "
                    + TermNumbers.percent(pulse.slow().strength()) + " (").plus(by).plus(") for "
                    + seconds(pulse.slow().holdTicks()))));
        }
        if (parts.isEmpty()) {
            return TermText.empty();
        }
        TermText text = TermText.of(" that, every " + seconds(ZonePulse.PULSE_TICKS) + ", ");
        for (int i = 0; i < parts.size(); i++) {
            text = text.plus(i == 0 ? "" : i == parts.size() - 1 ? " and " : ", ").plus(parts.get(i));
        }
        ZonePulse.Freeze freeze = pulse.freeze();
        return freeze.freezes() ? text.plus(", and an enemy that stays " + seconds(freeze.ticksInside()) + " gets ")
                .plus(EffectKind.FREEZE).plus(" for " + seconds(freeze.ticks())) : text;
    }

    /**
     * A blast against a Normal shell's: its full-damage zone and its edge are each wider by so many cells (narrower when
     * negative), and its edge deals {@code edgeShare} of the shell's damage; an edge no wider than the zone is none.
     */
    public record Blast(float extraCoreCells, float extraEdgeCells, float edgeShare) {

        static final Blast NORMAL = new Blast(0f, 0f, 0.5f);
    }

    /**
     * The ground a shell leaves where it lands: {@code radiusCells} wide, or {@code blastShare} of its blast's edge when
     * that is above zero. Each pulse puts its points on what stands in it as its kind takes them (a pool's fuel, or
     * magic damage at once), holds its slow on, and freezes what stays.
     *
     * @param lifetimeTicks how long it lasts
     * @param pulsePoints   what each pulse deals, in points of magic damage; 0 for none
     */
    public record Ground(ZoneKind kind, float radiusCells, float blastShare, int lifetimeTicks, float pulsePoints,
                         ZonePulse.Slow slow, ZonePulse.Freeze freeze) {

        static Ground fixed(ZoneKind kind, float radiusCells, int lifetimeTicks, float pulsePoints) {
            return new Ground(kind, radiusCells, 0f, lifetimeTicks, pulsePoints, ZonePulse.Slow.none(),
                    ZonePulse.Freeze.none());
        }

        static Ground ofBlast(ZoneKind kind, float blastShare, int lifetimeTicks, float pulsePoints) {
            return new Ground(kind, 0f, blastShare, lifetimeTicks, pulsePoints, ZonePulse.Slow.none(),
                    ZonePulse.Freeze.none());
        }

        /** The same ground, each pulse holding a slow of {@code strength} on for {@code seconds}. */
        Ground withSlow(float strength, float seconds) {
            return new Ground(this.kind, this.radiusCells, this.blastShare, this.lifetimeTicks, this.pulsePoints,
                    new ZonePulse.Slow(strength, Math.round(seconds * TickRate.TICKS_PER_SECOND)), this.freeze);
        }

        /** The same ground, freezing for {@code seconds} an enemy that stays {@code pulses} pulses in a row. */
        Ground withFreeze(int pulses, float seconds) {
            return new Ground(this.kind, this.radiusCells, this.blastShare, this.lifetimeTicks, this.pulsePoints,
                    this.slow, new ZonePulse.Freeze(pulses, Math.round(seconds * TickRate.TICKS_PER_SECOND)));
        }

        /** What each of its pulses does to an enemy inside. */
        public ZonePulse pulse() {
            return new ZonePulse(DamageUnits.ofPoints(this.pulsePoints), this.slow, this.freeze);
        }

        /** Whether it lasts until the wave is over rather than for a time. */
        public boolean lastsTheWave() {
            return this.lifetimeTicks == Zone.UNTIL_WAVE_END;
        }
    }
}
