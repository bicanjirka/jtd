package td.ui;

import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyMobVisitor;
import td.stat.StatWording;
import td.ui.render.InfoSheet;

/** The tooltip of an enemy under the pointer in the wave preview. */
final class EnemyInfoText implements EnemyMobVisitor<InfoSheet> {

    private final StatWording wording;

    /** {@code wording} reads speeds in the game board's cells. */
    EnemyInfoText(StatWording wording) {
        this.wording = wording;
    }

    @Override
    public InfoSheet visitDefined(DefinedEnemyMob mob) {
        return EnemyStatText.preview(mob.inspect(), this.wording);
    }
}
