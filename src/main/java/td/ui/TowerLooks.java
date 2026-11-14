package td.ui;

import td.ui.render.TowerLook;

import java.awt.Color;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The table of tower looks: one row per {@link TowerLook}, checked complete when this class loads. */
final class TowerLooks {

    private static final Map<TowerLook, Row> ROWS = checkedComplete(rows());

    private TowerLooks() {
    }

    /**
     * A tower's colour. {@code tiers} colours its upgrade tiers from the bottom; a tier above the last takes the
     * last's. Its board look is its {@link TowerFoundations} row, its icon its {@link TowerIcons} row.
     */
    record Row(Color color, List<Color> tiers) {

        Row {
            tiers = List.copyOf(tiers);
        }
    }

    static Row of(TowerLook look) {
        return ROWS.get(look);
    }

    static Color color(TowerLook look) {
        return of(look).color();
    }

    /** The colour of upgrade tier {@code tier} of a tower in {@code look}. */
    static Color tierColor(TowerLook look, int tier) {
        List<Color> tiers = of(look).tiers();
        return tiers.get(Math.min(tier, tiers.size() - 1));
    }

    static Map<TowerLook, Row> checkedComplete(Map<TowerLook, Row> rows) {
        Set<TowerLook> missing = EnumSet.allOf(TowerLook.class);
        missing.removeAll(rows.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Tower looks without a row: " + missing);
        }
        return rows;
    }

    private static Map<TowerLook, Row> rows() {
        Map<TowerLook, Row> rows = new EnumMap<>(TowerLook.class);
        List<Color> storm = ramp(0xe2746c, 0x8496ff, 0xc0c9ff, 0xe0dee8);
        rows.put(TowerLook.SNIPER, new Row(new Color(0xa6d674), ramp(0xa6d674, 0x5cc8b0, 0x93dbcb, 0xc9e8ce)));
        // Before the fork the Burst grows toward the Stormcaller, its first side.
        rows.put(TowerLook.SPLASH, new Row(new Color(0xe2746c), storm));
        rows.put(TowerLook.STORMCALLER, new Row(new Color(0x8496ff), storm));
        rows.put(TowerLook.HEXER, new Row(new Color(0xb88ae8), ramp(0xe2746c, 0xb88ae8, 0xdbc3f3, 0xeddce2)));
        Row pulse = new Row(new Color(0x8f86b8), ramp(0x8f86b8, 0xa89fd2, 0xc6c0e1, 0xe2dad8));
        rows.put(TowerLook.PULSE, pulse);
        // A rune changes the stone's mark, never its colour.
        rows.put(TowerLook.BURDEN, pulse);
        rows.put(TowerLook.GRAVITY, pulse);
        rows.put(TowerLook.UMBRA, pulse);
        rows.put(TowerLook.AURA, new Row(new Color(0xd8b866), ramp(0xd8b866, 0xecd48c, 0xf2e3b3, 0xf8ecc2)));
        rows.put(TowerLook.MORTAR, new Row(new Color(0xc39062), ramp(0xc39062, 0xe0b070, 0xebcba1, 0xf5e0b8)));
        List<Color> seeker = ramp(0x5cc4e2, 0x8fd0f2, 0xb5e0f6, 0xdaeae3);
        rows.put(TowerLook.SEEKER, new Row(new Color(0x5cc4e2), seeker));
        rows.put(TowerLook.RUBY, new Row(new Color(0xd0405a), seeker));
        rows.put(TowerLook.SHADE, new Row(new Color(0xa6e8c8), seeker));
        List<Color> cinder = ramp(0xea862a, 0xf0a040, 0xf5c081, 0xfadaa8);
        rows.put(TowerLook.CINDER, new Row(new Color(0xea862a), cinder));
        rows.put(TowerLook.SOULFIRE, new Row(new Color(0xb9a0ff), cinder));
        return rows;
    }

    private static List<Color> ramp(int... rgbs) {
        return Arrays.stream(rgbs).mapToObj(Color::new).toList();
    }
}
