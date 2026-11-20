package td.tower.mortar;

import td.damage.DamageType;
import td.effect.EffectKind;
import td.projectile.ShellLook;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.util.TickRate;
import td.zone.Zone;
import td.zone.ZoneKind;

import java.util.Optional;
import java.util.Set;

/**
 * One kind of shell in the Mortar's magazine: how it looks, how hard and how wide it hits against a Normal shell, how
 * fast it flies, and the ground it leaves where it lands.
 */
public enum ShellKind {
    NORMAL(ShellLook.NORMAL, DamageType.PHYSICAL, Blast.NORMAL, 1f, Optional.empty()),
    /** Its damage is the Mortar's Heavy multiplier, a stat. */
    HEAVY(ShellLook.HEAVY, DamageType.PHYSICAL, Blast.NORMAL, 0.75f, Optional.empty()),
    /** Two Heavies fused: their damage together, a blast of its own, and fallout over most of it. */
    NUKE(ShellLook.NUKE, DamageType.PHYSICAL, new Blast(1f, 1f, 0.75f), 0.7f,
            Optional.of(Ground.ofBlast(ZoneKind.FALLOUT, 0.8f, 80, 0.39f))),
    /** The Mortar's one way to deal magic: its blast burns, and the ground where it lands too. */
    NAPALM(ShellLook.NAPALM, DamageType.MAGIC, Blast.NORMAL, 1.25f,
            Optional.of(Ground.fixed(ZoneKind.BURNING_GROUND, 1f, 60, 1f))),
    TAR(ShellLook.TAR, DamageType.PHYSICAL, Blast.NORMAL, 1.25f, Optional.of(Ground.fixed(ZoneKind.TAR, 1.2f, 60, 0.45f))),
    CRYO(ShellLook.CRYO, DamageType.PHYSICAL, Blast.NORMAL, 1.25f,
            Optional.of(Ground.fixed(ZoneKind.FROST_GROUND, 1.6f, 100, 0f))),
    /** A small blast with no edge, and a flare that burns until the wave is over. */
    FLARE(ShellLook.FLARE, DamageType.PHYSICAL, new Blast(-0.25f, -1f, 0f), 1.25f,
            Optional.of(Ground.fixed(ZoneKind.FLARE, 1.5f, Zone.UNTIL_WAVE_END, 0f)));

    /** What a payload shell deals, a share of a Normal one's damage. */
    public static final float PAYLOAD_DAMAGE_SHARE = 0.5f;

    private final ShellLook look;
    private final DamageType damageType;
    private final Blast blast;
    private final float speedFactor;
    private final Optional<Ground> ground;

    ShellKind(ShellLook look, DamageType damageType, Blast blast, float speedFactor, Optional<Ground> ground) {
        this.look = look;
        this.damageType = damageType;
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

    /** Napalm, Tar or Cryo: the kinds a Payload node adds. */
    public boolean isPayload() {
        return this == NAPALM || this == TAR || this == CRYO;
    }

    /**
     * What it deals, as a share of the Mortar's damage, for a Heavy that deals {@code heavyMultiplier} times a Normal.
     */
    public float damageFactor(float heavyMultiplier) {
        return switch (this) {
            case NORMAL, FLARE -> 1f;
            case HEAVY -> heavyMultiplier;
            case NUKE -> 2f * heavyMultiplier;
            case NAPALM, TAR, CRYO -> PAYLOAD_DAMAGE_SHARE;
        };
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
            case NUKE -> Set.of(EffectKind.DEAD_ZONE, EffectKind.SICKENED, EffectKind.POISON);
            case NAPALM -> Set.of(EffectKind.BURN);
            case TAR -> Set.of(EffectKind.TARRED, EffectKind.POISON);
            case CRYO -> Set.of(EffectKind.CHILL, EffectKind.FREEZE);
            case FLARE -> Set.of(EffectKind.REVEALED);
        };
    }

    /** Where it lands, said after "each": "leaves tar 1.2 cells wide for 3 s". */
    public TermText groundText() {
        Ground left = this.ground.orElseThrow();
        String wide = TermNumbers.cells(left.radiusCells()) + " wide";
        return switch (this) {
            case NAPALM -> TermText.of("blasts as magic and leaves flames " + wide + " for " + seconds(left));
            case TAR -> TermText.of("leaves tar " + wide + " for " + seconds(left));
            case CRYO -> TermText.of("leaves frost " + wide + " for " + seconds(left));
            case FLARE -> TermText.of("leaves a flare " + wide + " that lasts until the wave ends");
            case NUKE -> TermText.of("leaves fallout over " + TermNumbers.percent(left.blastShare()) + " of its blast for "
                    + seconds(left));
            case NORMAL, HEAVY -> throw new IllegalStateException(this + " leaves no ground");
        };
    }

    private static String seconds(Ground ground) {
        return TermNumbers.seconds(ground.lifetimeTicks() / TickRate.TICKS_PER_SECOND);
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
     * that is above zero.
     *
     * @param lifetimeTicks how long it lasts
     * @param damageShare   a burn's or poison's fuel a pulse, as a share of the Mortar's damage
     */
    public record Ground(ZoneKind kind, float radiusCells, float blastShare, int lifetimeTicks, float damageShare) {

        static Ground fixed(ZoneKind kind, float radiusCells, int lifetimeTicks, float damageShare) {
            return new Ground(kind, radiusCells, 0f, lifetimeTicks, damageShare);
        }

        static Ground ofBlast(ZoneKind kind, float blastShare, int lifetimeTicks, float damageShare) {
            return new Ground(kind, 0f, blastShare, lifetimeTicks, damageShare);
        }

        /** Whether it lasts until the wave is over rather than for a time. */
        public boolean lastsTheWave() {
            return this.lifetimeTicks == Zone.UNTIL_WAVE_END;
        }
    }
}
