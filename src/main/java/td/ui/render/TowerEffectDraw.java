package td.ui.render;

public sealed interface TowerEffectDraw extends BoardDraw permits BeamDraw, SplashDraw, RingDraw, ShadowDraw, WardOutlineDraw, StreamDraw, TowerStatusDraw,
        NestDraw, FlockDraw, IceWakeDraw, RefundMarkDraw, TracerDraw, CannonballDraw,
        CasingDraw, SteamDraw, ShellCarryDraw, MuzzleBlastDraw, FootDustDraw, PumpSteamDraw, EmbersDraw, BulletDraw,
        LaserDraw, ExecutionDraw, FlameDraw, BlastDraw, SpellOrbDraw, CurseDraw, ShockDraw, ThunderboltDraw, SpellBlastDraw,
        LightEdgeDraw, LitEnemyDraw {
}
