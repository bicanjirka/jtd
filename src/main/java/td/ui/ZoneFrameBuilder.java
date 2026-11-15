package td.ui;

import td.tower.AuraTower;
import td.tower.CinderTower;
import td.tower.MortarTower;
import td.tower.PulseTower;
import td.tower.SeekerTower;
import td.tower.SniperTower;
import td.tower.SplashTower;
import td.tower.Tower;
import td.tower.TowerVisitor;
import td.tower.pulse.StoneLook;
import td.ui.render.Palette;
import td.ui.render.ZoneDraw;
import td.ui.render.ZonePatchDraw;
import td.zone.Zone;
import td.zone.ZoneRoster;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Describes the zones on the board as one patch per kind, and every Obelisk field as one patch per stone: a field is a
 * tower's range, not a zone, but it lies on the ground as one.
 */
final class ZoneFrameBuilder {

    /** The last share of a zone's life, over which it shrinks away: a fade would stack where it overlaps its kind. */
    private static final float ENDING_SHARE = 0.25f;

    private static final TowerVisitor<Optional<Field>> FIELD = new TowerVisitor<>() {
        public Optional<Field> visitSniperTower(SniperTower tower) {
            return Optional.empty();
        }

        public Optional<Field> visitSplashTower(SplashTower tower) {
            return Optional.empty();
        }

        public Optional<Field> visitPulseTower(PulseTower tower) {
            return Optional.of(new Field(fieldPaletteFor(tower.stoneLook()), tower.getX(), tower.getY(),
                    tower.getRangeReal()));
        }

        public Optional<Field> visitAuraTower(AuraTower tower) {
            return Optional.empty();
        }

        public Optional<Field> visitMortarTower(MortarTower tower) {
            return Optional.empty();
        }

        public Optional<Field> visitSeekerTower(SeekerTower tower) {
            return Optional.empty();
        }

        public Optional<Field> visitCinderTower(CinderTower tower) {
            return Optional.empty();
        }
    };

    private ZoneFrameBuilder() {
    }

    static List<ZonePatchDraw> build(ZoneRoster roster, List<Tower> towers, double animationSeconds) {
        Map<Palette, List<ZoneDraw>> discsByLook = new EnumMap<>(Palette.class);
        for (Zone zone : roster.zones()) {
            discsByLook.computeIfAbsent(paletteFor(zone), look -> new ArrayList<>()).add(discOf(zone, animationSeconds));
        }
        for (Tower tower : towers) {
            tower.accept(FIELD).ifPresent(field -> discsByLook.computeIfAbsent(field.palette(), look -> new ArrayList<>())
                    .add(new ZoneDraw(field.x(), field.y(), field.radius(), 1f, (float) animationSeconds)));
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

    /** The ground an Obelisk's field lies on: each stone its own patch. */
    static Palette fieldPaletteFor(StoneLook look) {
        return switch (look) {
            case PLAIN -> Palette.ZONE_FIELD;
            case BURDEN -> Palette.ZONE_FIELD_BURDEN;
            case GRAVITY -> Palette.ZONE_FIELD_GRAVITY;
            case UMBRA -> Palette.ZONE_FIELD_UMBRA;
        };
    }

    private static Palette paletteFor(Zone zone) {
        return switch (zone.kind()) {
            case BURNING_GROUND -> Palette.ZONE_BURNING;
            case TAR -> Palette.ZONE_TAR;
            case FROST_GROUND -> Palette.ZONE_FROST;
            case FALLOUT -> Palette.ZONE_FALLOUT;
            case FLARE -> Palette.ZONE_FLARE;
            case HEALING_GROUND -> Palette.ZONE_HEALING;
            case HASTE_GROUND -> Palette.ZONE_HASTE;
            case WARD_MINE -> Palette.ZONE_WARD;
            case BLINDING_FIELD -> Palette.ZONE_BLINDING;
        };
    }

    private record Field(Palette palette, float x, float y, float radius) {
    }
}
