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
        return Set.copyOf(applies);
    }

    @Override
    public TermText describe() {
        TermText missiles = TermText.of("adds ").plus(this.kind.term()).plus(" missiles");
        if (this.kind.shadow()) {
            missiles = missiles.plus(", which deal ").plus(TowerTerm.SHADOW).plus(" damage,");
        }
        return missiles.plus(" to the ").plus(TowerTerm.NEST)
                .plus(", one in " + BroodSpec.frostOnly().withSecond(this.kind).oneIn());
    }
}
