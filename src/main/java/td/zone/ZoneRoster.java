package td.zone;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.tower.targeting.InRangeTargetQuery;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The zones on the board. Every zone pulses on the same ticks, and a zone also pulses the tick it
 * is made. Two zones of one kind never stack: an enemy standing in both takes the pulse of the one
 * that was there first.
 */
public final class ZoneRoster {

    /** Zones pulse together this often: twice a second. */
    static final int PULSE_TICKS = Math.round(TickRate.TICKS_PER_SECOND / 2f);

    private final EnemyRegistry enemies;
    private final List<Zone> zones = new CopyOnWriteArrayList<>();

    public ZoneRoster(EnemyRegistry enemies) {
        this.enemies = enemies;
    }

    public void add(Zone zone) {
        this.zones.add(zone);
    }

    /** Zones in the order they were made. */
    public List<Zone> zones() {
        return Collections.unmodifiableList(this.zones);
    }

    public void clear() {
        this.zones.clear();
    }

    /** Pulses every zone due to, ages them all, and drops the ones whose life is over. */
    public void doTick(int gameTime) {
        if (this.zones.isEmpty()) {
            return;
        }
        Map<EnemyMob, Set<ZoneKind>> touched = new IdentityHashMap<>();
        List<Zone> ended = new ArrayList<>();
        for (Zone zone : this.zones) {
            if (zone.ageTicks() == 0 || gameTime % PULSE_TICKS == 0) {
                this.pulse(zone, touched);
            }
            zone.age();
            if (zone.isExpired()) {
                ended.add(zone);
            }
        }
        this.zones.removeAll(ended);
    }

    private void pulse(Zone zone, Map<EnemyMob, Set<ZoneKind>> touched) {
        List<EnemyMob> inside = InRangeTargetQuery.everyone((int) Math.round(zone.x()), (int) Math.round(zone.y()),
                zone.radius()).matching(this.enemies);
        for (EnemyMob enemy : inside) {
            if (touched.computeIfAbsent(enemy, e -> EnumSet.noneOf(ZoneKind.class)).add(zone.kind())) {
                zone.touch(enemy);
            }
        }
        zone.keepOnly(inside);
    }
}
