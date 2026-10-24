package td.tower.upgrade;

import td.term.TermText;
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
    public boolean isSatisfied(Tower tower, UpgradeState owned, GameWorld context) {
        return ownsSpecial(owned) && this.ownsLevelThree(owned);
    }

    @Override
    public TermText describe() {
        return TermText.of("a special and a level III head");
    }

    /** Short enough to share a button row with the node's name. */
    @Override
    public TermText progress(Tower tower, GameWorld context) {
        boolean special = ownsSpecial(tower.upgrades());
        boolean levelThree = this.ownsLevelThree(tower.upgrades());
        if (special && levelThree) {
            return TermText.of("special and head III");
        }
        if (!special && !levelThree) {
            return TermText.of("special, head III");
        }
        return TermText.of(special ? "needs head III" : "needs special");
    }

    /** Half for each of the two it needs. */
    @Override
    public float progressShare(Tower tower, GameWorld context) {
        return ((ownsSpecial(tower.upgrades()) ? 1 : 0) + (this.ownsLevelThree(tower.upgrades()) ? 1 : 0)) / 2f;
    }

    private static boolean ownsSpecial(UpgradeState owned) {
        return owned.countIn(UpgradeSlot.SPECIAL) > 0;
    }

    private boolean ownsLevelThree(UpgradeState owned) {
        return this.levelThreeIds.stream().anyMatch(owned::owns);
    }
}
