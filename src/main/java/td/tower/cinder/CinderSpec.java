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
 * @param core          the band its Core pours more in, if it has one
 * @param middle        the band its middle pours more in, if it has one
 * @param singe         whether its stream Singes what it touches
 * @param dragonsBreath whether its stream is Dragon's Breath, for the look
 * @param heatstroke     whether a hot burn in the stream dazes its enemy
 * @param meltdown      what each point of an enemy's armor and plating adds to the fuel it takes, 0 for nothing
 * @param thermalShock  how much more it pours into a held enemy, 1 for no more
 */
public record CinderSpec(Reach reach, EffectKind fire, FlameBand core, FlameBand middle, boolean singe,
                         boolean dragonsBreath, boolean heatstroke, float meltdown, float thermalShock) {

    /** The nearest visible enemy in range, a plain burn, no bands and no Singe. */
    public static CinderSpec from(Viewpoint view) {
        return new CinderSpec(Reach.visible(view), EffectKind.BURN, FlameBand.none(), FlameBand.none(), false,
                false, false, 0f, 1f);
    }

    public CinderSpec withFire(EffectKind fire) {
        return new CinderSpec(this.reach, fire, this.core, this.middle, this.singe, this.dragonsBreath,
                this.heatstroke, this.meltdown, this.thermalShock);
    }

    public CinderSpec withCore(FlameBand core) {
        return new CinderSpec(this.reach, this.fire, core, this.middle, this.singe, this.dragonsBreath,
                this.heatstroke, this.meltdown, this.thermalShock);
    }

    public CinderSpec withSinge() {
        return new CinderSpec(this.reach, this.fire, this.core, this.middle, true, this.dragonsBreath,
                this.heatstroke, this.meltdown, this.thermalShock);
    }

    public CinderSpec withMiddle(FlameBand middle) {
        return new CinderSpec(this.reach, this.fire, this.core, middle, this.singe, this.dragonsBreath,
                this.heatstroke, this.meltdown, this.thermalShock);
    }

    public CinderSpec withDragonsBreath() {
        return new CinderSpec(this.reach, this.fire, this.core, this.middle, this.singe, true, this.heatstroke,
                this.meltdown, this.thermalShock);
    }

    public CinderSpec withHeatstroke() {
        return new CinderSpec(this.reach, this.fire, this.core, this.middle, this.singe, this.dragonsBreath, true,
                this.meltdown, this.thermalShock);
    }

    public CinderSpec withMeltdown(float perPoint) {
        return new CinderSpec(this.reach, this.fire, this.core, this.middle, this.singe, this.dragonsBreath,
                this.heatstroke, perPoint, this.thermalShock);
    }

    public CinderSpec withThermalShock(float factor) {
        return new CinderSpec(this.reach, this.fire, this.core, this.middle, this.singe, this.dragonsBreath,
                this.heatstroke, this.meltdown, factor);
    }

    /**
     * How many times the cone's Flame it pours into an enemy {@code offAxis} radians off its heading, in a cone
     * {@code halfWidth} radians wide either side: once, and the more of every band that holds it.
     */
    public float flameAt(double offAxis, double halfWidth) {
        return 1f + this.core.moreAt(offAxis, halfWidth) + this.middle.moreAt(offAxis, halfWidth);
    }

    /** Whether its fire is Soulfire. */
    public boolean soulfire() {
        return this.fire == EffectKind.SOULFIRE;
    }
}
