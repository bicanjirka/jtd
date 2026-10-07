package td.tower.splash;

/** Rime Coven or Ash Coven: adds its hex to the pool. */
public final class CovenPerk implements SplashPerk {

    private final CovenHex hex;

    private CovenPerk(CovenHex hex) {
        this.hex = hex;
    }

    public static CovenPerk rime() {
        return new CovenPerk(CovenHex.rime());
    }

    public static CovenPerk ash() {
        return new CovenPerk(CovenHex.ash());
    }

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withHexes(spec.hexes().withHex(this.hex));
    }
}
