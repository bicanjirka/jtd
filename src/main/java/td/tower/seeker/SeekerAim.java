package td.tower.seeker;

import td.tower.targeting.TargetSelector;

/**
 * How the Seeker picks among the enemies in its reach.
 *
 * @param selector the pick
 * @param label    what the info rows call it
 */
public record SeekerAim(TargetSelector selector, String label) {
}
