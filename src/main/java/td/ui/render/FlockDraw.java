package td.ui.render;

/**
 * The birds of {@code look} circling an Aerie's top at ({@code centerX}, {@code centerY}), {@code count} of them;
 * {@code cellPixels} is a cell's size on the board. They circle on the wall clock ({@code seconds}) and come and go on
 * the tick clock: {@code arrived} 0..1 is how far the flock has come in, {@code leaving} how far it has scattered.
 * {@code seed} keeps one tower's birds flying their own way.
 */
public record FlockDraw(FlockLook look, int count, float centerX, float centerY, float cellPixels, float seconds,
                        int seed, float arrived, float leaving) implements TowerEffectDraw {
}
