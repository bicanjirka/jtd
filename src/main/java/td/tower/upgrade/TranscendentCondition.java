package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

import java.util.List;

/**
 * Transcendent's gate: an owned special and one of the tree's level III heads. A gate rather than
 * a prerequisite, so Transcendent is offered from Awaken on and says what is still missing.
 */
public record TranscendentCondition(List<String> levelThreeIds) implements UpgradeCondition {

    public TranscendentCondition {
        levelThreeIds = List.copyOf(levelThreeIds);
    }

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return ownsSpecial(tower) && this.ownsLevelThree(tower);
    }

    @Override
    public String describe() {
        return "a special and a level III head";
    }

    /** Short enough to share a button row with the node's name. */
    @Override
    public String progress(Tower tower, GameWorld context) {
        boolean special = ownsSpecial(tower);
        boolean levelThree = this.ownsLevelThree(tower);
        if (special && levelThree) {
            return "special and head III";
        }
        if (!special && !levelThree) {
            return "needs special, head III";
        }
        return special ? "needs head III" : "needs special";
    }

    private static boolean ownsSpecial(Tower tower) {
        return tower.upgrades().countIn(UpgradeSlot.SPECIAL) > 0;
    }

    private boolean ownsLevelThree(Tower tower) {
        return this.levelThreeIds.stream().anyMatch(tower.upgrades()::owns);
    }
}
