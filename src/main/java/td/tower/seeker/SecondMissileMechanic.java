package td.tower.seeker;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.LinkedHashSet;
import java.util.Set;

/** Ruby or Shade: adds a second kind of missile to the nest, made in a fixed order beside Frost. */
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
        Set<Term> applies = new LinkedHashSet<>();
        if (this.kind.withers()) {
            applies.add(EffectKind.WITHERED);
        }
        if (this.kind.silencesAfterThaw()) {
            applies.add(EffectKind.SILENCED);
        }
        return Set.copyOf(applies);
    }

    @Override
    public TermText describe() {
        return TermText.of("adds ").plus(this.kind.term()).plus(" missiles to the ").plus(TowerTerm.NEST)
                .plus(", one in " + BroodSpec.frostOnly().withSecond(this.kind).oneIn());
    }
}
