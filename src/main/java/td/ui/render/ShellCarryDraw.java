package td.ui.render;

import java.util.List;

/**
 * A Mortar of {@code tier} at ({@code centerX}, {@code centerY}) reloading, {@code reload} 0..1 of the way: the shell
 * being carried from its kind's place on the rack ({@code stock}) into the muzzle, which a long barrel lifts.
 * {@code load} is what this reload loads, front first; a {@code volley} loads each of them in turn.
 */
public record ShellCarryDraw(int tier, boolean longBarrel, float centerX, float centerY, float cellPixels,
                             float reload, boolean volley, List<LoadedShell> stock, List<LoadedShell> load)
        implements TowerEffectDraw {

    public ShellCarryDraw {
        stock = List.copyOf(stock);
        load = List.copyOf(load);
    }
}
