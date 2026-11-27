package td.ui.render;

public sealed interface ProjectileDraw extends BoardDraw permits CannonballDraw, MissileDraw, SmokeDraw {
}
