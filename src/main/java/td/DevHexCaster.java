package td;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.tower.splash.HexCaster;
import td.tower.splash.HexJump;
import td.tower.splash.HexKeeper;
import td.tower.splash.HexSpec;
import td.util.GameWorld;
import td.util.ThreadConfined;

/**
 * The dev panel as a Hexer with no tower: the hexes it puts on are paid out by the Hexer's own rules, at a bare
 * Hexer's strength, and what they deal is periodic damage from the panel.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
final class DevHexCaster implements HexCaster {

    private static final HexSpec SPEC = HexSpec.bare();

    private final GameWorld world;
    private final AttackProfile attack;
    private final HexKeeper keeper = new HexKeeper(this);

    DevHexCaster(GameWorld world, AttackProfile attack) {
        this.world = world;
        this.attack = attack;
    }

    /** A hex of {@code kind} the panel just put on {@code enemy}, paid out over the length it was put on for. */
    void record(EnemyMob enemy, EffectKind kind) {
        enemy.activeEffects().stream().filter(effect -> effect.kind() == kind).findFirst()
                .ifPresent(effect -> this.keeper.record(enemy, kind, effect.authoredDurationTicks()));
    }

    void doTick() {
        if (!this.keeper.isEmpty()) {
            this.keeper.doTick(SPEC);
        }
    }

    @Override
    public void dealPeriodicDamage(EnemyMob target, Damage damage) {
        target.doDamage(damage, this.attack);
    }

    @Override
    public float distance(float cells) {
        return cells * this.world.getBoard().scale();
    }

    @Override
    public EnemyRegistry enemies() {
        return this.world.enemies();
    }

    @Override
    public void jump(HexJump jump) {
        throw new IllegalStateException("The panel's hexes never jump: its spec has no Contagion");
    }
}
