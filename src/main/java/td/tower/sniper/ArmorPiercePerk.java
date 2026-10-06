package td.tower.sniper;

/** Shots ignore a flat amount of armor. */
public final class ArmorPiercePerk implements SniperPerk {

    private final float armorIgnored;

    public ArmorPiercePerk(float armorIgnored) {
        this.armorIgnored = armorIgnored;
    }

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        return shot.withAttack(attack -> attack.withArmorPenetration(attack.armorPenetration(),
                attack.armorPenetrationFlat() + this.armorIgnored));
    }
}
