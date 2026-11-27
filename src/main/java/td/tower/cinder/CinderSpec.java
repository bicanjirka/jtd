package td.tower.cinder;

import td.effect.EffectKind;
import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Scorcher may aim at, which fire it pours and what it pours more of: the base rules reshaped by each
 * mechanic it owns. Each {@code withX} is a copy.
 *
 * @param reach         the visible enemies it may aim at
 * @param fire          the pool its stream feeds
 * @param core          how wide its Core is and how much more it pours
 * @param singe         whether its stream Singes what it touches
 * @param look          the colour of its flame's core
 * @param dragonsBreath whether its stream is Dragon's Breath, for the look
 * @param flashover     whether a hot burn in the stream spreads to the enemies beside it
 * @param meltdown      what each point of an enemy's armor and plating adds to the fuel it takes, 0 for nothing
 * @param thermalShock  how much more it pours into a held enemy, 1 for no more
 */
public record CinderSpec(Reach reach, EffectKind fire, CoreSpec core, boolean singe, FlameLook look,
                         boolean dragonsBreath, boolean flashover, float meltdown, float thermalShock) {

    /** The nearest visible enemy in range, a plain burn, no Core and no Singe. */
    public static CinderSpec from(Viewpoint view) {
        return new CinderSpec(Reach.visible(view), EffectKind.BURN, CoreSpec.none(), false, FlameLook.ORANGE,
                false, false, 0f, 1f);
    }

    public CinderSpec withFire(EffectKind fire) {
        return new CinderSpec(this.reach, fire, this.core, this.singe, this.look, this.dragonsBreath,
                this.flashover, this.meltdown, this.thermalShock);
    }

    public CinderSpec withCore(CoreSpec core) {
        return new CinderSpec(this.reach, this.fire, core, this.singe, this.look, this.dragonsBreath,
                this.flashover, this.meltdown, this.thermalShock);
    }

    public CinderSpec withSinge() {
        return new CinderSpec(this.reach, this.fire, this.core, true, this.look, this.dragonsBreath,
                this.flashover, this.meltdown, this.thermalShock);
    }

    public CinderSpec withLook(FlameLook look) {
        return new CinderSpec(this.reach, this.fire, this.core, this.singe, look, this.dragonsBreath,
                this.flashover, this.meltdown, this.thermalShock);
    }

    public CinderSpec withDragonsBreath() {
        return new CinderSpec(this.reach, this.fire, this.core, this.singe, this.look, true, this.flashover,
                this.meltdown, this.thermalShock);
    }

    public CinderSpec withFlashover() {
        return new CinderSpec(this.reach, this.fire, this.core, this.singe, this.look, this.dragonsBreath, true,
                this.meltdown, this.thermalShock);
    }

    public CinderSpec withMeltdown(float perPoint) {
        return new CinderSpec(this.reach, this.fire, this.core, this.singe, this.look, this.dragonsBreath,
                this.flashover, perPoint, this.thermalShock);
    }

    public CinderSpec withThermalShock(float factor) {
        return new CinderSpec(this.reach, this.fire, this.core, this.singe, this.look, this.dragonsBreath,
                this.flashover, this.meltdown, factor);
    }

    /** Whether its fire is Soulfire. */
    public boolean soulfire() {
        return this.fire == EffectKind.SOULFIRE;
    }
}
