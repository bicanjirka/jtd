package td.ui.render;

public sealed interface TowerEffectDraw permits BeamDraw, SplashDraw, PulseDraw, RingDraw, StreamDraw, TowerStatusDraw,
        NestDraw, FlashDraw, LeyLineDraw, RefundMarkDraw, TracerDraw, CannonballDraw {
}
