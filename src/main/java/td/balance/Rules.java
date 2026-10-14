package td.balance;

import td.GameEngine;
import td.level.LevelDefinition;

/**
 * Changes to a level's rules for a play, none by default: tougher or weaker enemies, more or fewer starting
 * credits and lives, richer or poorer kills, waves spread out or packed tight. They are how a margin is found and
 * how a level's knobs are swept, never how a level is reported.
 *
 * @param enemyHealthPercent  every enemy's health, spawns included, as a percentage of what it is authored with
 * @param creditBonus         added to the level's starting credits; negative takes away, down to none
 * @param livesBonus          added to the level's starting lives; negative takes away, down to one
 * @param bountyPercent       what every kill pays, as a percentage of the authored bounty
 * @param spawnSpacingPercent the gaps between a wave's spawns, as a percentage of the authored ones
 */
public record Rules(int enemyHealthPercent, int creditBonus, int livesBonus, int bountyPercent,
                    int spawnSpacingPercent) {

    public static Rules standard() {
        return new Rules(100, 0, 0, 100, 100);
    }

    public Rules withEnemyHealthPercent(int percent) {
        return new Rules(percent, this.creditBonus, this.livesBonus, this.bountyPercent, this.spawnSpacingPercent);
    }

    public Rules withLivesBonus(int bonus) {
        return new Rules(this.enemyHealthPercent, this.creditBonus, bonus, this.bountyPercent, this.spawnSpacingPercent);
    }

    public Rules withBountyPercent(int percent) {
        return new Rules(this.enemyHealthPercent, this.creditBonus, this.livesBonus, percent, this.spawnSpacingPercent);
    }

    public Rules withSpawnSpacingPercent(int percent) {
        return new Rules(this.enemyHealthPercent, this.creditBonus, this.livesBonus, this.bountyPercent, percent);
    }

    public boolean isStandard() {
        return this.equals(standard());
    }

    LevelDefinition applyTo(LevelDefinition level) {
        return level.withStartingCredits(Math.max(0, level.startingCredits() + this.creditBonus))
                .withStartingLives(Math.max(1, level.startingLives() + this.livesBonus));
    }

    void applyTo(GameEngine engine) {
        engine.getGameWorld().playtestRules().setEnemyHealthPercent(this.enemyHealthPercent);
        engine.getGameWorld().playtestRules().setBountyPercent(this.bountyPercent);
        engine.getGameWorld().playtestRules().setSpawnSpacingPercent(this.spawnSpacingPercent);
    }
}
