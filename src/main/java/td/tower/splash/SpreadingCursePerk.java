package td.tower.splash;

/** Spreading Curse: adds Hex of Contagion to the pool. */
public final class SpreadingCursePerk implements SplashPerk {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withHexes(spec.hexes().withHex(new ContagionHex()));
    }
}
