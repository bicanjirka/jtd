package td.tower.seeker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Which missile carries which payload. Before the last Mixed Payloads node, every third missile
 * carries the next owned payload in turn; with it, every missile carries one, cycling through all
 * four, each stronger. A missile with a payload does not freeze unless the payload is Cryo.
 *
 * @param owned        the payloads bought, in the order bought
 * @param everyMissile whether every missile carries one
 */
public record PayloadPlan(List<Payload> owned, boolean everyMissile) {

    private static final int EVERY = 3;
    private static final float FULL_RACK_STRENGTH = 1.25f;
    private static final List<Payload> FULL_RACK = List.of(new CryoPayload(), new ArcanePayload(), new EmpPayload(),
            new TracerPayload());

    public PayloadPlan {
        owned = List.copyOf(owned);
    }

    /** No payloads: every missile freezes. */
    public static PayloadPlan none() {
        return new PayloadPlan(List.of(), false);
    }

    /** This plan with {@code payload} added after the others. */
    public PayloadPlan with(Payload payload) {
        List<Payload> grown = new ArrayList<>(this.owned);
        grown.add(payload);
        return new PayloadPlan(grown, this.everyMissile);
    }

    public PayloadPlan forEveryMissile() {
        return new PayloadPlan(this.owned, true);
    }

    /** What the {@code missileNumber}th missile the Seeker has launched, counting from 1, carries. */
    public Optional<PayloadLoad> loadFor(int missileNumber) {
        if (this.everyMissile) {
            return Optional.of(new PayloadLoad(FULL_RACK.get((missileNumber - 1) % FULL_RACK.size()),
                    FULL_RACK_STRENGTH));
        }
        if (this.owned.isEmpty() || missileNumber % EVERY != 0) {
            return Optional.empty();
        }
        int turn = missileNumber / EVERY - 1;
        return Optional.of(new PayloadLoad(this.owned.get(turn % this.owned.size()), 1f));
    }
}
