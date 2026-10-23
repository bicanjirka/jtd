package td;

import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.stat.StatModifiers;
import td.tower.TowerCapability;
import td.tower.TowerModifiers;
import td.tower.TowerStat;
import td.tower.TowerStatus;
import td.tower.TowerTerm;
import td.util.TickRate;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * What the dev panel can put on a unit: the enemy effects and the tower buffs it offers, each built from
 * a length in seconds and one power number, whose meaning is the kind's own (stacks, percent, points).
 * Nothing it makes is credited to anyone.
 */
public final class DevEffects {

    private static final Map<EffectKind, EffectBuilder> ENEMY = new EnumMap<>(EffectKind.class);
    private static final Map<TowerTerm, BuffBuilder> TOWER = new EnumMap<>(TowerTerm.class);

    static {
        ENEMY.put(EffectKind.CHILL, (ticks, power) -> Effect.chill(Math.min(0.8f, power / 100f), ticks, none()));
        ENEMY.put(EffectKind.FREEZE, (ticks, power) -> Effect.freeze(ticks, none()));
        ENEMY.put(EffectKind.DAZED, (ticks, power) -> Effect.dazed(ticks, none()));
        ENEMY.put(EffectKind.BURN, (ticks, power) -> Effect.burn(pool(power), ticks, none()));
        ENEMY.put(EffectKind.POISON, (ticks, power) -> Effect.poison(pool(power), ticks, none()));
        ENEMY.put(EffectKind.SOULFIRE, (ticks, power) -> Effect.soulfire(pool(power), ticks, none()));
        ENEMY.put(EffectKind.BLEEDING, (ticks, power) -> Effect.bleeding(DamageUnits.ofPoints(power), ticks, none()));
        ENEMY.put(EffectKind.SHIELD, (ticks, power) -> Effect.shield(DamageUnits.ofPoints(power), ticks, none()));
        ENEMY.put(EffectKind.HEAL, (ticks, power) -> Effect.heal(DamageUnits.ofPoints(power), ticks, none()));
        ENEMY.put(EffectKind.INVISIBLE, (ticks, power) -> Effect.invisible(ticks, none()));
        ENEMY.put(EffectKind.SPRINTING, (ticks, power) -> Effect.sprinting(ticks, none()));
        ENEMY.put(EffectKind.HASTENED, (ticks, power) -> Effect.hastened(ticks, none()));
        ENEMY.put(EffectKind.REVEALED, (ticks, power) -> Effect.revealed(ticks, none()));
        ENEMY.put(EffectKind.VULNERABLE, (ticks, power) -> Effect.vulnerable(power, ticks, none()));
        ENEMY.put(EffectKind.SUNDERED, (ticks, power) -> Effect.sundered(power, ticks, none()));
        ENEMY.put(EffectKind.RESONATING, (ticks, power) -> Effect.resonating(power, ticks, none()));
        ENEMY.put(EffectKind.SATURATED, (ticks, power) -> Effect.saturated(power, ticks, none()));
        ENEMY.put(EffectKind.TOLL, (ticks, power) -> Effect.toll(power, ticks, none()));
        ENEMY.put(EffectKind.CORRODED, (ticks, power) -> Effect.corroded(power, ticks, none()));
        ENEMY.put(EffectKind.DRAGGED, (ticks, power) -> Effect.dragged(power, ticks, none()));
        ENEMY.put(EffectKind.FRAIL, (ticks, power) -> Effect.frail(power, ticks, none()));
        ENEMY.put(EffectKind.SCORCHED, (ticks, power) -> Effect.scorched(power));
        ENEMY.put(EffectKind.SICKENED, (ticks, power) -> Effect.sickened(power));
        ENEMY.put(EffectKind.SILENCED, (ticks, power) -> Effect.silenced(ticks, none()));
        ENEMY.put(EffectKind.ANCHORED, (ticks, power) -> Effect.anchored(ticks, none()));
        ENEMY.put(EffectKind.BRITTLE, (ticks, power) -> Effect.brittle(ticks, none()));
        ENEMY.put(EffectKind.CRACKED, (ticks, power) -> Effect.cracked(ticks, none()));
        ENEMY.put(EffectKind.TARRED, (ticks, power) -> Effect.tarred(ticks, none()));
        ENEMY.put(EffectKind.EXPOSED, (ticks, power) -> Effect.exposed(ticks, none()));
        ENEMY.put(EffectKind.MARKED, (ticks, power) -> Effect.marked(ticks, none()));

        TOWER.put(TowerTerm.HASTE, (ticks, power) -> TowerStatus.timed(TowerTerm.HASTE,
                TowerModifiers.attackSpeed(power / 100f), ticks));
        TOWER.put(TowerTerm.PATIENCE, (ticks, power) -> TowerStatus.nextShots(TowerTerm.PATIENCE, 1,
                StatModifiers.<TowerStat>none()).withCapability(TowerCapability.GUARANTEED_CRIT));
        TOWER.put(TowerTerm.CHARGED_SHOT, (ticks, power) -> TowerStatus.nextShots(TowerTerm.CHARGED_SHOT, 1,
                TowerModifiers.damageTimes(Math.max(1, power))));
    }

    private DevEffects() {
    }

    /** The enemy effects the panel offers, in the order it cycles them. */
    public static List<EffectKind> enemyKinds() {
        return List.copyOf(ENEMY.keySet());
    }

    /** The tower buffs the panel offers, in the order it cycles them. */
    public static List<TowerTerm> towerBuffs() {
        return List.copyOf(TOWER.keySet());
    }

    public static Effect enemyEffect(EffectKind kind, float seconds, int power) {
        return ENEMY.get(kind).build(ticks(seconds), power);
    }

    public static TowerStatus towerBuff(TowerTerm term, float seconds, int power) {
        return TOWER.get(term).build(ticks(seconds), power);
    }

    private static int ticks(float seconds) {
        return Math.max(1, Math.round(seconds * TickRate.TICKS_PER_SECOND));
    }

    private static Damage pool(int power) {
        return Damage.magic(DamageUnits.ofPoints(power));
    }

    private static DamageSink none() {
        return DamageSink.none();
    }

    private interface EffectBuilder {
        Effect build(int ticks, int power);
    }

    private interface BuffBuilder {
        TowerStatus build(int ticks, int power);
    }
}
