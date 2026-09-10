package td.wave;

/**
 * One authored wave: its contents as a token string in the wave mini-language (see the table
 * in CLAUDE.md), and the three numbers every enemy in it is built from.
 *
 * @param enemies space-separated tokens, e.g. {@code "3 s e 4 c"}
 * @param hp      base health each enemy is given, before per-type adjustment
 * @param price   bounty per kill, and the score penalty if one leaks
 * @param level   difficulty tier - scales body size, and each type's own twist (a square's
 *                resistance, a triangle's top speed, a corpse's fade duration)
 */
public record WaveDefinition(String enemies, int hp, int price, int level) {
}
