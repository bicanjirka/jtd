package td.balance;

import td.GameEngine;
import td.level.LevelDefinition;

/**
 * Changes to a level's rules for a play, none by default: tougher or weaker enemies, more or fewer starting
 * credits. They are how a margin is found, never how a level is reported.
 *
 * @param enemyHealthPercent every enemy's health, spawns included, as a percentage of what it is authored with
 * @param creditBonus        added to the level's starting credits; negative takes away, down to none
 */
public record Rules(int enemyHealthPercent, int creditBonus) {

    public static Rules standard() {
        return new Rules(100, 0);
    }

    public Rules withEnemyHealthPercent(int percent) {
        return new Rules(percent, this.creditBonus);
    }

    public Rules withCreditBonus(int bonus) {
        return new Rules(this.enemyHealthPercent, bonus);
    }

    public boolean isStandard() {
        return this.equals(standard());
    }

    LevelDefinition applyTo(LevelDefinition level) {
        return level.withStartingCredits(Math.max(0, level.startingCredits() + this.creditBonus));
    }

    void applyTo(GameEngine engine) {
        engine.getGameWorld().playtestRules().setEnemyHealthPercent(this.enemyHealthPercent);
    }
}
