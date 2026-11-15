package td.tower.seeker;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Ruby or Shade: adds a second kind of missile to the nest, drawn by lot beside Frost. */
public final class SecondMissileMechanic implements SeekerMechanic {

    private final MissileKind kind;

    public SecondMissileMechanic(MissileKind kind) {
        if (kind == MissileKind.FROST) {
            throw new IllegalArgumentException("Frost is no second missile");
        }
        this.kind = kind;
    }

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withBrood(spec.brood().withSecond(this.kind));
    }

    @Override
    public Set<Term> applies() {
        return this.kind.silencesAfterThaw() ? Set.of(EffectKind.SILENCED) : Set.of();
    }

    @Override
    public TermText describe() {
        return TermText.of("adds ").plus(this.kind.term()).plus(" missiles to the ").plus(TowerTerm.NEST)
                .plus(", one in " + Math.round(1f / BroodSpec.frostOnly().withSecond(this.kind).secondShare()));
    }
}
