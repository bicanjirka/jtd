package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.Rank;

/**
 * The line under which a crit executes an enemy: one ranked below {@code sparedFrom} with less than {@code below} of its
 * health left. The mechanic that executes and the selector that looks for whom it can execute read the same line.
 */
public record ExecuteThreshold(float below, Rank sparedFrom) {

    /** The Executioner's line. */
    public static final ExecuteThreshold EXECUTIONER = new ExecuteThreshold(0.15f, Rank.ELITE);

    /** Whether {@code enemy} is under the line now. */
    public boolean executes(EnemyMob enemy) {
        return !this.spares(enemy) && enemy.getHealthFraction() < this.below;
    }

    /** Whether a hit of {@code damage}, before armor, would leave {@code enemy} under the line or dead. */
    public boolean leftUnderBy(EnemyMob enemy, int damage) {
        int health = enemy.getHealth();
        if (this.spares(enemy) || health <= 0) {
            return false;
        }
        return enemy.getHealthFraction() * (health - damage) / health < this.below;
    }

    private boolean spares(EnemyMob enemy) {
        return enemy.getRank().compareTo(this.sparedFrom) >= 0;
    }
}
