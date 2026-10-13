package td.balance;

/**
 * A headless player: given a turn, it spends. One instance plays one game, so it may remember what it did.
 */
@FunctionalInterface
public interface Player {

    /** Called before each round and every few ticks during one; may do nothing. */
    void act(Turn turn);
}
