package td.tower.sniper;

import td.tower.targeting.TargetSelector;

/**
 * Who a special makes the Sniper aim at.
 *
 * @param selector how it picks among the enemies in range
 * @param label    what the info panel says
 */
public record SniperAim(TargetSelector selector, String label) {
}
