package td.tower.sniper;

/** Tradecraft: a shot at a target far out hits harder. */
public final class LongShotPerk implements SniperPerk {

    private static final float FAR_FROM = 2f / 3f;
    private static final float DAMAGE_FACTOR = 1.25f;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        return context.rangeShare() > FAR_FROM ? shot.scaledBy(DAMAGE_FACTOR) : shot;
    }
}
