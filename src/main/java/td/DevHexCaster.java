package td;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.tower.splash.DoomHex;
import td.tower.splash.HexCaster;
import td.tower.splash.HexKeeper;
import td.tower.splash.HexSpec;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.function.Function;

/**
 * The dev panel as a Hexer with no tower: the hexes it puts on are paid out by the Hexer's own rules, at a bare
 * Hexer's strength and reach, and what they deal is periodic damage from the panel.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
final class DevHexCaster implements HexCaster {

    /** A Reckoning starts the Dooms it releases again at a Doom's own length. */
    private static final HexSpec SPEC = HexSpec.none().withHex(new DoomHex());

    private final GameWorld world;
    private final AttackProfile attack;
    private final HexKeeper keeper = new HexKeeper(this);

    DevHexCaster(GameWorld world, AttackProfile attack) {
        this.world = world;
        this.attack = attack;
    }

    /** A hex of {@code kind} the panel just put on {@code enemy}. */
    void record(EnemyMob enemy, EffectKind kind) {
        this.keeper.record(enemy, kind);
    }

    void doTick(int time) {
        this.keeper.doTick(time, SPEC);
    }

    @Override
    public void applyEffect(EnemyMob target, Function<DamageSink, Effect> effect) {
        target.applyEffect(effect.apply(damage -> this.dealPeriodicDamage(target, damage)));
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
    public void leaveCloud(EnemyMob dead) {
        throw new IllegalStateException("The panel's hexes leave no cloud: its spec has no Mastery");
    }
}
