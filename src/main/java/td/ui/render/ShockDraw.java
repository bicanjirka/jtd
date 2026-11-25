package td.ui.render;

/**
 * An enemy a spell's lightning just hit, shocked as in a cartoon: its {@code body} gone black with an electric rim,
 * the {@code skeleton} showing through on a crit or a Thunderstrike. {@code progress} runs 0..1 over the shock;
 * {@code seed} picks its flicker.
 */
public record ShockDraw(EnemyBodyDraw body, boolean skeleton, float progress, int seed) implements TowerEffectDraw {
}
