package td.damage;

/**
 * How damage reaches an enemy. A {@code HIT} is an attack landing: a shot, a blast, a beam pass, a
 * zap. {@code PERIODIC} is everything that ticks: a burn or poison pulse, a field's tick. Only a
 * hit can crit or spend a mark.
 */
public enum Delivery {
    HIT,
    PERIODIC
}
