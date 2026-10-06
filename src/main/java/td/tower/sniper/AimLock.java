package td.tower.sniper;

/**
 * What a shot knows about the target it is fired at.
 *
 * @param stacks how many shots in a row, up to the cap, the Sniper has already fired at it
 * @param fresh  whether it is a different target from the last shot's
 */
public record AimLock(int stacks, boolean fresh) {
}
