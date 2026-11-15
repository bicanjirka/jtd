package td.tower.seeker;

import td.effect.IceKind;
import td.projectile.MissileLook;
import td.tower.TowerTerm;

/**
 * A kind of missile the Aerie's nest holds, fixed as the missile is drawn: what its hit deals and what its freeze is
 * made of. Everything a kind does differently is here, so the tower never compares kinds.
 */
public enum MissileKind {
    /** The plain missile: its freeze is ice. */
    FROST(TowerTerm.FROST_MISSILE, 1f, IceKind.FROST, MissileLook.STANDARD, false, false),
    /** Twice the damage, and a freeze in ruby that holds the freeze-immune. */
    RUBY(TowerTerm.RUBY, 2f, IceKind.RUBY, MissileLook.RUBY, false, false),
    /** Shadow damage, past every Shield, and a freeze that leaves its enemy Silenced as long again once it thaws. */
    SHADE(TowerTerm.SHADE, 1f, IceKind.SHADE, MissileLook.SHADE, true, true);

    private final TowerTerm term;
    private final float damageFactor;
    private final IceKind ice;
    private final MissileLook look;
    private final boolean shadow;
    private final boolean silencesAfterThaw;

    MissileKind(TowerTerm term, float damageFactor, IceKind ice, MissileLook look, boolean shadow,
                boolean silencesAfterThaw) {
        this.term = term;
        this.damageFactor = damageFactor;
        this.ice = ice;
        this.look = look;
        this.shadow = shadow;
        this.silencesAfterThaw = silencesAfterThaw;
    }

    public TowerTerm term() {
        return this.term;
    }

    /** Its hit's damage as a multiple of the tower's. */
    public float damageFactor() {
        return this.damageFactor;
    }

    public IceKind ice() {
        return this.ice;
    }

    public MissileLook look() {
        return this.look;
    }

    /** Whether it deals Shadow damage, which goes past Shield and insulation. */
    public boolean shadow() {
        return this.shadow;
    }

    /** Whether its enemy stays Silenced, once it thaws, for as long as it was frozen. */
    public boolean silencesAfterThaw() {
        return this.silencesAfterThaw;
    }
}
