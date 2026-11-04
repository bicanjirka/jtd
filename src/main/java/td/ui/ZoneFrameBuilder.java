package td.ui;

import td.ui.render.Palette;
import td.ui.render.ZoneDraw;
import td.ui.render.ZonePatchDraw;
import td.zone.Zone;
import td.zone.ZoneRoster;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Describes the zones on the board as one patch per kind. */
final class ZoneFrameBuilder {

    /** The last share of a zone's life, over which it shrinks away: a fade would stack where it overlaps its kind. */
    private static final float ENDING_SHARE = 0.25f;

    private ZoneFrameBuilder() {
    }

    static List<ZonePatchDraw> build(ZoneRoster roster, double animationSeconds) {
        Map<Palette, List<ZoneDraw>> discsByLook = new EnumMap<>(Palette.class);
        for (Zone zone : roster.zones()) {
            discsByLook.computeIfAbsent(paletteFor(zone), look -> new ArrayList<>()).add(discOf(zone, animationSeconds));
        }
        return discsByLook.entrySet().stream()
                .map(entry -> new ZonePatchDraw(entry.getKey(), List.copyOf(entry.getValue()),
                        ZoneOutlineTracer.trace(entry.getValue())))
                .toList();
    }

    private static ZoneDraw discOf(Zone zone, double animationSeconds) {
        float fade = Math.min(1f, zone.lifeLeft() / ENDING_SHARE);
        return new ZoneDraw((float) zone.x(), (float) zone.y(), zone.radius() * fade, fade, (float) animationSeconds);
    }

    private static Palette paletteFor(Zone zone) {
        return switch (zone.kind()) {
            case BURNING_GROUND -> Palette.ZONE_BURNING;
            case TAR -> Palette.ZONE_TAR;
            case FROST_GROUND -> Palette.ZONE_FROST;
            case FALLOUT -> Palette.ZONE_FALLOUT;
            case MINE -> Palette.ZONE_MINE;
            case CURSED_CLOUD -> Palette.ZONE_CLOUD;
            case HEALING_GROUND -> Palette.ZONE_HEALING;
            case HASTE_GROUND -> Palette.ZONE_HASTE;
            case WARD_MINE -> Palette.ZONE_WARD;
            case BLINDING_FIELD -> Palette.ZONE_BLINDING;
        };
    }
}
