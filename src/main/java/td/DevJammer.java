package td;

import td.stat.DisruptionAura;
import td.tower.Tower;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The jams the dev panel holds on towers. Each tick it lays each one into the world's disruption, which the enemies
 * have just filled and the towers are about to sample, so a tower takes it as it takes a nearby enemy's: through its
 * disruption taken, and shown as Jammed.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
final class DevJammer {

    private final GameWorld world;
    private final Map<Tower, Held> held = new IdentityHashMap<>();

    DevJammer(GameWorld world) {
        this.world = world;
    }

    /** Holds {@code aura} on {@code tower} for the next {@code ticks}, in place of any jam it held. */
    void jam(Tower tower, int ticks, DisruptionAura aura) {
        this.held.put(tower, new Held(ticks, aura));
    }

    void doTick() {
        if (this.held.isEmpty()) {
            return;
        }
        this.held.keySet().retainAll(this.world.towers().all());
        this.held.replaceAll((tower, jam) -> {
            this.world.disruptions().add(tower.getX(), tower.getY(), jam.aura());
            return new Held(jam.ticksLeft() - 1, jam.aura());
        });
        this.held.values().removeIf(jam -> jam.ticksLeft() <= 0);
    }

    private record Held(int ticksLeft, DisruptionAura aura) {
    }
}
