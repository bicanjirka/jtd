package td.enemy;

import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;

/** Nimble: agility in points, so an aimed hit misses with chance {@code agility / (agility + 100)}. */
public record AgilityTrait(float agility) implements Trait {

    @Override
    public StatModifiers<EnemyStat> modifiers(TraitContext context) {
        return StatModifiers.of(EnemyStat.AGILITY, StatModifier.flat(this.agility));
    }

    @Override
    public TraitLine describe() {
        return new TraitLine(this.marker(), "Nimble", "+" + Math.round(this.agility) + " agility");
    }

    @Override
    public TraitMarker marker() {
        return TraitMarker.AGILITY;
    }
}
