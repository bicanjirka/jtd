package td.tower.cinder;

import td.effect.PoolTuning;
import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Cinder aims at, how wide and fast its wave is and what its burn does to an enemy: the base
 * rules reshaped by each perk it owns. Each {@code withX} is a copy.
 *
 * @param reach         the visible enemies it may aim at
 * @param coneScale     the wave's width as a multiple of the base
 * @param ring          whether the wave is a full ring rather than a cone
 * @param waveSpeed     how many times as fast as the base the wave travels
 * @param burnScale     the burn's duration as a multiple of the base
 * @param stoke         how repeated waves raise a burn
 * @param bellows       whether an Aura's fire-rate buff on the Cinder also widens its cone
 * @param soulfire      whether every other wave is Soulfire
 * @param linger        the patch of burning ground each wave leaves
 * @param look          the colour of its waves
 * @param searing       whether a wave that hits a burning enemy, and every ignition, makes it Vulnerable
 * @param thermalShock  whether a burning enemy that freezes chills its neighbours
 * @param critScorch    how many Scorched stacks a critical ignition adds
 * @param combustion    what a pool that reaches its cap does to its neighbours
 * @param everburnFloor the share of its strongest application a pool never decays below in range, 0 for none
 * @param tuning        what its pools hold, how fast they mark, what a freeze does to them and what they cost
 */
public record CinderSpec(Reach reach, float coneScale, boolean ring, float waveSpeed, float burnScale,
                         StokeSpec stoke, boolean bellows, boolean soulfire, LingerSpec linger, FlameLook look,
                         boolean searing, boolean thermalShock, int critScorch, CombustionSpec combustion,
                         float everburnFloor, PoolTuning tuning) {

    /** The nearest visible enemy in range, an ordinary cone, a plain burn and no passives. */
    public static CinderSpec from(Viewpoint view) {
        return new CinderSpec(Reach.visible(view), 1f, false, 1f, 1f, StokeSpec.none(), false, false,
                LingerSpec.none(), FlameLook.ORANGE, false, false, 0, CombustionSpec.none(), 0f,
                PoolTuning.standard());
    }

    public CinderSpec withConeScaledBy(float factor) {
        return new CinderSpec(this.reach, this.coneScale * factor, this.ring, this.waveSpeed, this.burnScale,
                this.stoke, this.bellows, this.soulfire, this.linger, this.look, this.searing, this.thermalShock,
                this.critScorch, this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withRing() {
        return new CinderSpec(this.reach, this.coneScale, true, this.waveSpeed, this.burnScale, this.stoke,
                this.bellows, this.soulfire, this.linger, this.look, this.searing, this.thermalShock,
                this.critScorch, this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withWaveSpeedScaledBy(float factor) {
        return new CinderSpec(this.reach, this.coneScale, this.ring, this.waveSpeed * factor, this.burnScale,
                this.stoke, this.bellows, this.soulfire, this.linger, this.look, this.searing, this.thermalShock,
                this.critScorch, this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withBurnScaledBy(float factor) {
        return new CinderSpec(this.reach, this.coneScale, this.ring, this.waveSpeed, this.burnScale * factor,
                this.stoke, this.bellows, this.soulfire, this.linger, this.look, this.searing, this.thermalShock,
                this.critScorch, this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withStoke(StokeSpec stoke) {
        return new CinderSpec(this.reach, this.coneScale, this.ring, this.waveSpeed, this.burnScale, stoke,
                this.bellows, this.soulfire, this.linger, this.look, this.searing, this.thermalShock,
                this.critScorch, this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withBellows() {
        return new CinderSpec(this.reach, this.coneScale, this.ring, this.waveSpeed, this.burnScale, this.stoke,
                true, this.soulfire, this.linger, this.look, this.searing, this.thermalShock, this.critScorch,
                this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withSoulfire() {
        return new CinderSpec(this.reach, this.coneScale, this.ring, this.waveSpeed, this.burnScale, this.stoke,
                this.bellows, true, this.linger, this.look, this.searing, this.thermalShock, this.critScorch,
                this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withLinger(LingerSpec linger) {
        return new CinderSpec(this.reach, this.coneScale, this.ring, this.waveSpeed, this.burnScale, this.stoke,
                this.bellows, this.soulfire, linger, this.look, this.searing, this.thermalShock, this.critScorch,
                this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withLook(FlameLook look) {
        return new CinderSpec(this.reach, this.coneScale, this.ring, this.waveSpeed, this.burnScale, this.stoke,
                this.bellows, this.soulfire, this.linger, look, this.searing, this.thermalShock, this.critScorch,
                this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withSearing() {
        return new CinderSpec(this.reach, this.coneScale, this.ring, this.waveSpeed, this.burnScale, this.stoke,
                this.bellows, this.soulfire, this.linger, this.look, true, this.thermalShock, this.critScorch,
                this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withThermalShock() {
        return new CinderSpec(this.reach, this.coneScale, this.ring, this.waveSpeed, this.burnScale, this.stoke,
                this.bellows, this.soulfire, this.linger, this.look, this.searing, true, this.critScorch,
                this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withCritScorch(int critScorch) {
        return new CinderSpec(this.reach, this.coneScale, this.ring, this.waveSpeed, this.burnScale, this.stoke,
                this.bellows, this.soulfire, this.linger, this.look, this.searing, this.thermalShock, critScorch,
                this.combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withCombustion(CombustionSpec combustion) {
        return new CinderSpec(this.reach, this.coneScale, this.ring, this.waveSpeed, this.burnScale, this.stoke,
                this.bellows, this.soulfire, this.linger, this.look, this.searing, this.thermalShock,
                this.critScorch, combustion, this.everburnFloor, this.tuning);
    }

    public CinderSpec withEverburnFloor(float everburnFloor) {
        return new CinderSpec(this.reach, this.coneScale, this.ring, this.waveSpeed, this.burnScale, this.stoke,
                this.bellows, this.soulfire, this.linger, this.look, this.searing, this.thermalShock,
                this.critScorch, this.combustion, everburnFloor, this.tuning);
    }

    public CinderSpec withTuning(PoolTuning tuning) {
        return new CinderSpec(this.reach, this.coneScale, this.ring, this.waveSpeed, this.burnScale, this.stoke,
                this.bellows, this.soulfire, this.linger, this.look, this.searing, this.thermalShock,
                this.critScorch, this.combustion, this.everburnFloor, tuning);
    }

    public Viewpoint view() {
        return this.reach.view();
    }
}
