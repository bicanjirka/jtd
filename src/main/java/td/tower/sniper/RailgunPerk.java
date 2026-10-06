package td.tower.sniper;

/** Railgun: the shot pierces every enemy on the line through its target. */
public final class RailgunPerk implements SniperPerk {

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        return shot.withPiercing();
    }
}
