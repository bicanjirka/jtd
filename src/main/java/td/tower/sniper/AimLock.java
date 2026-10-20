package td.tower.sniper;

/**
 * What a shot knows about the target it is fired at.
 *
 * @param stacks how many shots before it, up to the cap, have built Steady Aim
 * @param fresh  whether it is a different target from the last shot's
 */
public record AimLock(int stacks, boolean fresh) {
}
