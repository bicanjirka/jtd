package td.tower.seeker;

import td.damage.DamageUnits;
import td.effect.IceKind;
import td.projectile.MissileLook;
import td.tower.TowerTerm;

/**
 * A kind of missile the Aerie's nest holds, fixed as the missile is drawn: what its hit deals and what its freeze is
 * made of. Everything a kind does differently is here, so the tower never compares kinds.
 */
public enum MissileKind {
    /** The plain missile: its freeze is ice. */
    FROST(TowerTerm.FROST_MISSILE, 0f, IceKind.FROST, MissileLook.STANDARD, false, false),
    /** More damage, and a freeze in ruby that holds the freeze-immune. */
    RUBY(TowerTerm.RUBY, 20f, IceKind.RUBY, MissileLook.RUBY, false, false),
    /** More damage, as Shadow, past every Shield; and no regeneration for as long as its freeze. */
    SHADE(TowerTerm.SHADE, 20f, IceKind.SHADE, MissileLook.SHADE, true, true);

    private final TowerTerm term;
    private final float damageBonus;
    private final IceKind ice;
    private final MissileLook look;
    private final boolean shadow;
    private final boolean withers;

    MissileKind(TowerTerm term, float damageBonus, IceKind ice, MissileLook look, boolean shadow, boolean withers) {
        this.term = term;
        this.damageBonus = damageBonus;
        this.ice = ice;
        this.look = look;
        this.shadow = shadow;
        this.withers = withers;
    }

    public TowerTerm term() {
        return this.term;
    }

    /** The points its hit deals on top of the tower's damage. */
    public float damageBonus() {
        return this.damageBonus;
    }

    /** What its hit deals with the tower's {@code damage}, before the hit's adjustment. */
    public float damageWith(float damage) {
        return damage + DamageUnits.ofPoints(this.damageBonus);
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

    /** Whether its hit stops its enemy's regeneration for as long as the freeze it brings. */
    public boolean withers() {
        return this.withers;
    }
}
