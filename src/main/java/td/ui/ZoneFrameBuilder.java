package td.ui;

import td.ui.render.Palette;
import td.ui.render.ZoneDraw;
import td.zone.Zone;
import td.zone.ZoneRoster;

import java.util.ArrayList;
import java.util.List;

/** Describes every zone on the board, oldest first, so a newer patch is drawn over an older one. */
final class ZoneFrameBuilder {

    private ZoneFrameBuilder() {
    }

    static List<ZoneDraw> build(ZoneRoster roster, double animationSeconds) {
        List<ZoneDraw> draws = new ArrayList<>();
        for (Zone zone : roster.zones()) {
            draws.add(new ZoneDraw(paletteFor(zone), (float) zone.x(), (float) zone.y(), zone.radius(),
                    zone.lifeLeft(), (float) animationSeconds));
        }
        return draws;
    }

    private static Palette paletteFor(Zone zone) {
        return switch (zone.kind()) {
            case BURNING_GROUND -> Palette.ZONE_BURNING;
            case TAR -> Palette.ZONE_TAR;
            case FROST_GROUND -> Palette.ZONE_FROST;
            case FALLOUT -> Palette.ZONE_FALLOUT;
            case MINE -> Palette.ZONE_MINE;
            case CURSED_CLOUD -> Palette.ZONE_CLOUD;
        };
    }
}
