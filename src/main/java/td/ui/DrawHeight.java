package td.ui;

import td.ui.render.AffixPipsDraw;
import td.ui.render.BeamDraw;
import td.ui.render.BlastDraw;
import td.ui.render.BlastGroundDraw;
import td.ui.render.BlastLook;
import td.ui.render.BoardDraw;
import td.ui.render.BulletDraw;
import td.ui.render.CannonballDraw;
import td.ui.render.CasingDraw;
import td.ui.render.CritSparkDraw;
import td.ui.render.CurseDraw;
import td.ui.render.EffectPulseDraw;
import td.ui.render.EmbersDraw;
import td.ui.render.EnemyBarsDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyBurstDraw;
import td.ui.render.EnemyDotsDraw;
import td.ui.render.EnemyEggDraw;
import td.ui.render.EnemyEmissionDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.EnemyIceDraw;
import td.ui.render.EnemyRingDraw;
import td.ui.render.EnemyTintDraw;
import td.ui.render.ExecutionDraw;
import td.ui.render.FlameDraw;
import td.ui.render.FlockDraw;
import td.ui.render.FootDustDraw;
import td.ui.render.HexRuneDraw;
import td.ui.render.IceWakeDraw;
import td.ui.render.LaserDraw;
import td.ui.render.LightEdgeDraw;
import td.ui.render.LitEnemyDraw;
import td.ui.render.MissMarkDraw;
import td.ui.render.MissileDraw;
import td.ui.render.MuzzleBlastDraw;
import td.ui.render.NestDraw;
import td.ui.render.Palette;
import td.ui.render.PumpSteamDraw;
import td.ui.render.RankBadgeDraw;
import td.ui.render.RefundMarkDraw;
import td.ui.render.RingDraw;
import td.ui.render.ShadowDraw;
import td.ui.render.ShellCarryDraw;
import td.ui.render.ShockDraw;
import td.ui.render.SilenceMarkDraw;
import td.ui.render.SmokeDraw;
import td.ui.render.SpellBlastDraw;
import td.ui.render.SpellOrbDraw;
import td.ui.render.SplashDraw;
import td.ui.render.SteamDraw;
import td.ui.render.StreamDraw;
import td.ui.render.ThunderboltDraw;
import td.ui.render.TollMarkDraw;
import td.ui.render.TowerGroundDraw;
import td.ui.render.TowerSpriteDraw;
import td.ui.render.TowerStatusDraw;
import td.ui.render.TracerDraw;
import td.ui.render.UpgradeArrowDraw;
import td.ui.render.WardOutlineDraw;
import td.ui.render.ZoneDetailDraw;
import td.ui.render.ZoneGroundPieceDraw;

/**
 * How high a board visual stands, lowest first: the board paints height by height and, inside one, from the far edge
 * to the near one. What belongs to a tower or an enemy stands at its foot, so it is painted with it.
 */
enum DrawHeight {
    /** Flat on the ground, under everything that stands. */
    GROUND,
    /** Towers, enemies and what is on them or moves at them. */
    STANDING,
    /** What flies or rises a little: shots, beams, flames, a shell's blast. */
    LOW_AIR,
    /** What the player reads off an enemy or a tower: bars, badges, marks. */
    SIGNAL,
    /** What towers over the board: the nest and its birds, missiles, a bolt from the sky, a nuke's cloud. */
    HIGH_AIR;

    static DrawHeight of(BoardDraw draw) {
        return switch (draw) {
            // The kinds every frame holds most of come first: the switch tries its cases in turn.
            case TowerGroundDraw _ -> GROUND;
            case TowerSpriteDraw _, EnemyBodyDraw _ -> STANDING;
            case EnemyBarsDraw _, EnemyDotsDraw _, RankBadgeDraw _, AffixPipsDraw _, HexRuneDraw _, MissMarkDraw _,
                 TollMarkDraw _, SilenceMarkDraw _, TowerStatusDraw _, RefundMarkDraw _, UpgradeArrowDraw _ -> SIGNAL;
            case EnemyRingDraw ring -> ring.palette() == Palette.SELECTION ? SIGNAL : GROUND;
            case BlastGroundDraw _, SplashDraw _, RingDraw _, ShadowDraw _, WardOutlineDraw _, IceWakeDraw _,
                 SpellBlastDraw _, LightEdgeDraw _ -> GROUND;
            case ZoneDetailDraw _, ZoneGroundPieceDraw _, EnemyFadeDraw _, EnemyEmissionDraw _, EnemyTintDraw _,
                 EnemyIceDraw _, EnemyEggDraw _, EnemyBurstDraw _, EffectPulseDraw _, CritSparkDraw _, ShockDraw _,
                 ExecutionDraw _, LitEnemyDraw _, CasingDraw _, SteamDraw _, ShellCarryDraw _, MuzzleBlastDraw _,
                 FootDustDraw _, PumpSteamDraw _, EmbersDraw _ -> STANDING;
            case BeamDraw _, TracerDraw _, BulletDraw _, LaserDraw _, StreamDraw _, FlameDraw _, CannonballDraw _,
                 SpellOrbDraw _, CurseDraw _ -> LOW_AIR;
            case BlastDraw blast -> blast.look() == BlastLook.NUKE ? HIGH_AIR : LOW_AIR;
            case NestDraw _, FlockDraw _, MissileDraw _, SmokeDraw _, ThunderboltDraw _ -> HIGH_AIR;
        };
    }
}
