package td.zone;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.stat.DisruptionPenalty;
import td.tower.targeting.InRangeTargetQuery;
import td.util.ThreadConfined;
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
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class ZoneRoster {

    /** Zones pulse together this often: twice a second. */
    static final int PULSE_TICKS = Math.round(TickRate.TICKS_PER_SECOND / 2f);

    private final EnemyRegistry enemies;
    private final List<Zone> zones = new CopyOnWriteArrayList<>();
    // Game loop: from a wave's clear until the next starts, every zone fades, a shell landing late included.
    private boolean clearing = false;
    // The tick the wave out started on, which every pulse counts from.
    private int waveStart = 0;

    public ZoneRoster(EnemyRegistry enemies) {
        this.enemies = enemies;
    }

    public void add(Zone zone) {
        if (this.clearing) {
            zone.clear();
        }
        this.zones.add(zone);
    }

    /** Zones in the order they were made. */
    public List<Zone> zones() {
        return Collections.unmodifiableList(this.zones);
    }

    /** Takes {@code zone} off the board, as a tower does with a mine it replaces. */
    public void remove(Zone zone) {
        this.zones.remove(zone);
    }

    public void clear() {
        this.zones.clear();
        this.clearing = false;
        this.waveStart = 0;
    }

    /** The wave is over: every zone fades out over a moment, and one made after fades too. */
    public void endWave() {
        this.clearing = true;
        this.zones.forEach(Zone::clear);
    }

    /** A wave starts on tick {@code gameTime} on bare ground: what the last left is gone, and pulses count from now. */
    public void startWave(int gameTime) {
        this.zones.clear();
        this.clearing = false;
        this.waveStart = gameTime;
    }

    /**
     * What the blinding fields over {@code (x, y)} do to a tower there. Fields never stack: the one that
     * blinds most counts.
     */
    public DisruptionPenalty disruptionAt(double x, double y) {
        DisruptionPenalty worst = DisruptionPenalty.none();
        for (Zone zone : this.zones) {
            if (zone.kind() == ZoneKind.BLINDING_FIELD && zone.penalty().missChance() > worst.missChance()
                    && zone.covers(x, y)) {
                worst = zone.penalty();
            }
        }
        return worst;
    }

    /** Pulses every zone due to, ages them all, and drops the ones whose life is over. */
    public void doTick(int gameTime) {
        if (this.zones.isEmpty()) {
            return;
        }
        Map<EnemyMob, Set<ZoneKind>> touched = new IdentityHashMap<>();
        List<Zone> ended = new ArrayList<>();
        for (Zone zone : this.zones) {
            if (zone.kind() == ZoneKind.MINE || zone.kind() == ZoneKind.WARD_MINE) {
                if (zone.isArmed()) {
                    this.stepOn(zone);
                }
            } else if (zone.kind() != ZoneKind.BLINDING_FIELD && (zone.ageTicks() == 0
                    || (gameTime - this.waveStart) % PULSE_TICKS == 0)) {
                this.pulse(zone, touched);
            }
            zone.age();
            if (zone.isExpired()) {
                ended.add(zone);
            }
        }
        this.zones.removeAll(ended);
    }

    /** A mine is looked at every tick, so nothing walks across it between two pulses. */
    private void stepOn(Zone zone) {
        List<EnemyMob> inside = InRangeTargetQuery.everyone((int) Math.round(zone.x()), (int) Math.round(zone.y()),
                zone.radius()).matching(this.enemies);
        if (!inside.isEmpty()) {
            zone.detonate(inside.getFirst());
        }
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
