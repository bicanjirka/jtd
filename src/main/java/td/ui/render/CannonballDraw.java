package td.ui.render;

import java.util.Optional;

/**
 * A lobbed shell in flight, position already interpolated: {@code x, y} is its point on the ground, where its
 * shadow lies, and it is drawn {@code lift} pixels above it. The shadow shrinks to {@code shadowScale} of its
 * size as the shell climbs. {@code size} is a multiple of the standard shell. A shell with a streak trails a
 * line to where it was drawn a tick ago; without one the tail is the head. A shell of a {@code kind} is drawn as that
 * kind is in a barrel's bore; without one it is a plain lit ball in its palette. A burning, freezing or glowing shell
 * draws its {@code trail} behind it.
 */
public record CannonballDraw(Palette palette, float x, float y, float lift, float shadowScale, float size,
                             float tailX, float tailY, Optional<LoadedShell> kind,
                             Optional<ShellTrail> trail) implements ProjectileDraw,
        TowerEffectDraw {
}
