package td.tower.cinder;

/**
 * Combustion: an enemy whose burning pools together grow hotter than one wave could make them bursts for
 * {@code share} of what they still hold onto the enemies within {@code radiusCells} of it.
 */
public record CombustionSpec(float share, float radiusCells) {

    public static CombustionSpec none() {
        return new CombustionSpec(0f, 0f);
    }

    public static CombustionSpec of(float share, float radiusCells) {
        return new CombustionSpec(share, radiusCells);
    }

    public boolean isActive() {
        return this.share > 0f;
    }
}
