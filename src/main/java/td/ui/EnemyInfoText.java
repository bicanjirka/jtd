package td.ui;

import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyMobVisitor;

/** The info-panel text for an enemy under the pointer in the wave preview. */
final class EnemyInfoText implements EnemyMobVisitor<String> {

    @Override
    public String visitDefined(DefinedEnemyMob mob) {
        return EnemyStatText.preview(mob.inspect());
    }
}
