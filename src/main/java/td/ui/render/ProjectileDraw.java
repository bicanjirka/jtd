package td.ui.render;

/** A shell or missile currently in flight - see {@link CannonballDraw}/{@link MissileDraw}. */
public sealed interface ProjectileDraw permits CannonballDraw, MissileDraw {
}
