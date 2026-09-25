package td.ui;

import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyMobVisitor;
import td.ui.render.InfoSheet;

/** The info-panel sheet for an enemy under the pointer in the wave preview. */
final class EnemyInfoText implements EnemyMobVisitor<InfoSheet> {

    @Override
    public InfoSheet visitDefined(DefinedEnemyMob mob) {
        return EnemyStatText.preview(mob.inspect());
    }
}
