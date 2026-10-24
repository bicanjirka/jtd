package td.enemy;

/**
 * Who a trace line is about: what never changes over an enemy's life.
 *
 * @param maxHealth in units
 */
public record MobLabel(long id, String name, Rank rank, int maxHealth) {
}
