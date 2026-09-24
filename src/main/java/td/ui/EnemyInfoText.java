package td.ui;

import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMobVisitor;
import td.enemy.Rank;

/** The info-panel text for an enemy under the pointer. */
final class EnemyInfoText implements EnemyMobVisitor<String> {

    @Override
    public String visitDefined(DefinedEnemyMob mob) {
        EnemyDefinition definition = mob.definition();
        return definition.displayName() + "\n\n" + definition.description() + "\n\nRank: " + titleCase(mob.getRank())
                + "   Health: " + definition.baseHealth() + "   Bounty: " + definition.price();
    }

    private static String titleCase(Rank rank) {
        String name = rank.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
