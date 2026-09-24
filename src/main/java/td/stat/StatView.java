package td.stat;

/** Read access to resolved stat values. */
@FunctionalInterface
public interface StatView {

    float value(EnemyStat stat);
}
