package td.stat;

/** A number on a stat sheet: the base it has unless content authors another, and the range it is clamped to. */
public interface StatKey {

    float defaultBase();

    float clamp(float value);
}
