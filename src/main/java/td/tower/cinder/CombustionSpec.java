package td.tower.cinder;

/**
 * Combustion: a pool that reaches its cap bursts for {@code share} of what it still holds onto the
 * enemies within {@code radiusCells} of it.
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
