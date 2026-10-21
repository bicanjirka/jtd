package td.tower.cinder;

import td.effect.PoolTuning;
import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Cinder aims at, what shape its wave is and what its burn does to an enemy: the base rules
 * reshaped by each mechanic it owns. Each {@code withX} is a copy.
 *
 * @param reach         the visible enemies it may aim at
 * @param ring          whether the wave is a full ring rather than a cone
 * @param stoke         how repeated waves raise a burn
 * @param bellows       whether an Aura's attack-speed buff on the Cinder also widens its cone
 * @param soulfire      whether every other wave is Soulfire
 * @param linger        the patch of burning ground each wave leaves
 * @param look          the colour of its waves
 * @param searingIntervalTicks how often at most a wave that hits a burning enemy, and every ignition, makes it Vulnerable;
 *                             0 for never
 * @param thermalShock  whether a burning enemy that freezes chills its neighbours
 * @param critScorch    how many Scorched stacks a critical ignition adds
 * @param combustion    what a burn grown hotter than one wave could make it does to its neighbours
 * @param everburnFloor the share of its strongest application a pool never decays below in range, 0 for none
 * @param tuning        what its pools hold, how fast they mark, what a freeze does to them and what they cost
 */
public record CinderSpec(Reach reach, boolean ring, StokeSpec stoke, boolean bellows, boolean soulfire, LingerSpec linger, FlameLook look,
                         int searingIntervalTicks, boolean thermalShock, int critScorch, CombustionSpec combustion,
                         float everburnFloor, PoolTuning tuning) {

    /** The nearest visible enemy in range, an ordinary cone, a plain burn and no passives. */
    public static CinderSpec from(Viewpoint view) {
        return new CinderSpec(Reach.visible(view), false, StokeSpec.none(), false, false,
                LingerSpec.none(), FlameLook.ORANGE, 0, false, 0, CombustionSpec.none(), 0f,
                PoolTuning.standard());
    }

    public CinderSpec withRing() {
        return new CinderSpec(this.reach, true, this.stoke,
                this.bellows, this.soulfire, this.linger, this.look, this.searingIntervalTicks, this.thermalShock,
                this.critScorch, this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withStoke(StokeSpec stoke) {
        return new CinderSpec(this.reach, this.ring, stoke,
                this.bellows, this.soulfire, this.linger, this.look, this.searingIntervalTicks, this.thermalShock,
                this.critScorch, this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withBellows() {
        return new CinderSpec(this.reach, this.ring, this.stoke,
                true, this.soulfire, this.linger, this.look, this.searingIntervalTicks, this.thermalShock, this.critScorch,
                this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withSoulfire() {
        return new CinderSpec(this.reach, this.ring, this.stoke,
                this.bellows, true, this.linger, this.look, this.searingIntervalTicks, this.thermalShock, this.critScorch,
                this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withLinger(LingerSpec linger) {
        return new CinderSpec(this.reach, this.ring, this.stoke,
                this.bellows, this.soulfire, linger, this.look, this.searingIntervalTicks, this.thermalShock, this.critScorch,
                this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withLook(FlameLook look) {
        return new CinderSpec(this.reach, this.ring, this.stoke,
                this.bellows, this.soulfire, this.linger, look, this.searingIntervalTicks, this.thermalShock, this.critScorch,
                this.combustion, this.everburnFloor, this.tuning);
    }

    /** Whether it makes the enemies it burns Vulnerable. */
    public boolean searing() {
        return this.searingIntervalTicks > 0;
    }

    public CinderSpec withSearing(int intervalTicks) {
        return new CinderSpec(this.reach, this.ring, this.stoke,
                this.bellows, this.soulfire, this.linger, this.look, intervalTicks, this.thermalShock, this.critScorch,
                this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withThermalShock() {
        return new CinderSpec(this.reach, this.ring, this.stoke,
                this.bellows, this.soulfire, this.linger, this.look, this.searingIntervalTicks, true, this.critScorch,
                this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withCritScorch(int critScorch) {
        return new CinderSpec(this.reach, this.ring, this.stoke,
                this.bellows, this.soulfire, this.linger, this.look, this.searingIntervalTicks, this.thermalShock, critScorch,
                this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withCombustion(CombustionSpec combustion) {
        return new CinderSpec(this.reach, this.ring, this.stoke,
                this.bellows, this.soulfire, this.linger, this.look, this.searingIntervalTicks, this.thermalShock,
                this.critScorch, combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withEverburnFloor(float everburnFloor) {
        return new CinderSpec(this.reach, this.ring, this.stoke,
                this.bellows, this.soulfire, this.linger, this.look, this.searingIntervalTicks, this.thermalShock,
                this.critScorch, this.combustion, everburnFloor, this.tuning);
    }

    public CinderSpec withTuning(PoolTuning tuning) {
        return new CinderSpec(this.reach, this.ring, this.stoke,
                this.bellows, this.soulfire, this.linger, this.look, this.searingIntervalTicks, this.thermalShock,
                this.critScorch, this.combustion, this.everburnFloor, tuning);
    }

    public Viewpoint view() {
        return this.reach.view();
    }
}
