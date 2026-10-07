package td.tower.splash;

/** Hex: the Splash becomes the Hexer, and its pool starts with Hex of Doom. */
public final class HexOfDoomPerk implements SplashPerk {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withHexes(spec.hexes().withHex(new DoomHex()));
    }
}
