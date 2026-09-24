package td.ui;

import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyMobVisitor;
import td.ui.render.EnemySheet;

/** The info-panel sheet for an enemy under the pointer in the wave preview. */
final class EnemyInfoText implements EnemyMobVisitor<EnemySheet> {

    @Override
    public EnemySheet visitDefined(DefinedEnemyMob mob) {
        return EnemyStatText.preview(mob.inspect());
    }
}
