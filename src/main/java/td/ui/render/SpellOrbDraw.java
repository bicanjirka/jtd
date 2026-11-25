package td.ui.render;

/**
 * A spell thrown in place of a stone, flying almost straight: {@code x, y} its point on the ground, where its shadow
 * lies, drawn {@code lift} pixels above it, {@code radius} pixels across its head, with its tail behind it.
 * {@code seconds} is the wall clock its crackle and swirl run on.
 */
public record SpellOrbDraw(SpellOrbLook look, float x, float y, float lift, float radius, ShellTrail trail,
                           float seconds) implements TowerEffectDraw {
}
