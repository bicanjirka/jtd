package td.enemy;

import td.effect.EffectKind;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * One active effect as a trace sees it, at one moment.
 *
 * @param stacks    its stacks; {@code 0} for a kind that does not stack
 * @param remaining ticks left; empty for a level or pool that fades instead
 * @param level     a shield's pool in points, or how full a fading level or pool is; empty for every other kind
 */
public record EffectState(EffectKind kind, int stacks, OptionalInt remaining, OptionalDouble level) {
}
