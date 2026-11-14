package td.ui;

import td.term.Term;
import td.ui.render.AffixPipsDraw;
import td.ui.render.BeamDraw;
import td.ui.render.CannonballDraw;
import td.ui.render.CellDraw;
import td.ui.render.CellGridDraw;
import td.ui.render.ConeDraw;
import td.ui.render.CritSparkDraw;
import td.ui.render.EffectPulseDraw;
import td.ui.render.EnemyBarsDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyBurstDraw;
import td.ui.render.EnemyDotsDraw;
import td.ui.render.EnemyDraw;
import td.ui.render.EnemyEggDraw;
import td.ui.render.EnemyEmissionDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.EnemyIceDraw;
import td.ui.render.EnemyLook;
import td.ui.render.EnemyOverlayDraw;
import td.ui.render.EnemyRingDraw;
import td.ui.render.EnemyTintDraw;
import td.ui.render.FlashDraw;
import td.ui.render.HexGlyph;
import td.ui.render.HexRuneDraw;
import td.ui.render.LeyLineDraw;
import td.ui.render.MissMarkDraw;
import td.ui.render.MissileDraw;
import td.ui.render.NestDraw;
import td.ui.render.Palette;
import td.ui.render.PathMarkerBrightness;
import td.ui.render.PathMarkerDraw;
import td.ui.render.PathMarkerShape;
import td.ui.render.ProjectileDraw;
import td.ui.render.PulseDraw;
import td.ui.render.RankBadge;
import td.ui.render.RankBadgeDraw;
import td.ui.render.RefundMarkDraw;
import td.ui.render.RenderFrame;
import td.ui.render.RingDraw;
import td.ui.render.SheetLine;
import td.ui.render.SmokeDraw;
import td.ui.render.SplashDraw;
import td.ui.render.TowerEffectDraw;
import td.ui.render.TowerLook;
import td.ui.render.TowerSpriteDraw;
import td.ui.render.TowerStatusDraw;
import td.ui.render.TracerDraw;
import td.ui.render.ZoneArcDraw;
import td.ui.render.ZoneDraw;
import td.ui.render.ZonePatchDraw;
import td.wave.PathColor;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

/**
 * Turns a {@link RenderFrame} into {@code Graphics2D} calls: the only class that does, and the only
 * painter of board content, with colours and shapes keyed off {@link Palette}, {@link EnemyLooks} and {@link TowerLooks}.
 * <p>
 * {@code Panel*} components use AWT for layout and previews but never paint board content;
 * {@link #paintEnemies}, {@link #renderTowerIcon} and the {@code paint*Glyph} methods exist for
 * them.
 */
public final class Java2DFrameRenderer {

    private static final Color CELL_OK = Color.GRAY;
    private static final Color CELL_NOK = Color.RED;
    /** A free cell the credits can't build the tower on yet: amber, apart from the red of a cell nothing builds on. */
    private static final Color CELL_SHORT = new Color(255, 170, 60);
    private static final Color CELL_RANGE = new Color(250, 250, 210, 150);
    /** The HUD's own border, so the dev grid reads as part of the panel chrome. */
    private static final Color CELL_GRID_LINE = Hud.BORDER_DISABLED;
    /** A Ley Line's width, and the dash it is drawn with once the player has pinned it. */
    private static final float LEY_LINE_STROKE = 2f;
    /** A tracer is drawn in this many pieces; its back end at this share of the opacity and width of its head. */
    private static final int TRACER_PIECES = 6;
    private static final float TRACER_TAIL_ALPHA = 0.25f;
    private static final float TRACER_TAIL_WIDTH = 0.65f;
    private static final float[] LEY_LINE_DASH = {6f, 5f};
    private static final Color CELL_BLOCKED = new Color(39, 39, 40);
    /** An info-panel glyph for a stat nothing on the board marks. */
    private static final Color UNMARKED_GLYPH = Hud.TEXT_PROSE;
    private static final float HOLLOW_GLYPH_STROKE_WIDTH = 1.3f;
    /** A dot is a small mark, not a body. */
    private static final float DOT_GLYPH_SIZE_FRACTION = 0.4f;

    /** How far a tower's icon reaches from its centre, in its own cells. */
    private static final float ICON_REACH_CELLS = 0.40f;
    /** The upgrade arrow, as shares of a cell: its size, and its centre's inset from the cell's top-left corner. */
    private static final float UPGRADE_ARROW_SIZE = 0.085f;
    private static final float UPGRADE_ARROW_INSET = 0.15f;
    private static final float UPGRADE_ARROW_OUTLINE = 0.045f;
    private static final Color UPGRADE_ARROW_EDGE = new Color(13, 14, 16);

    /** How far apart a badge's two chevrons sit, as a share of its size. */
    private static final float RANK_BADGE_CHEVRON_SPACING = 1f;
    private static final float RANK_BADGE_CHEVRON_RISE = 0.4f;
    private static final float RANK_BADGE_CHEVRON_STROKE = 0.32f;
    private static final float ENEMY_DOT_RADIUS = 1.3f;
    private static final float ENEMY_DOT_SPACING = 4.1f;
    /** Set on screen at the default window; below that zoom a bar shrinks with the board. */
    private static final float ENEMY_BAR_HEIGHT = 1.64f;
    private static final float ENEMY_BAR_GAP = 0.82f;
    private static final float ENEMY_BAR_TRACK_MARGIN = 0.82f;
    private static final Color ENEMY_BAR_TRACK = new Color(0, 0, 0, 191);
    /** Opaque, so a pictogram's knock-outs read as holes rather than tinted ink. */
    private static final Color AFFIX_PIP_DISC = new Color(13, 14, 16);
    private static final float AFFIX_PIP_SYMBOL_SHARE = 0.8f;
    private static final int EFFECT_PULSE_ALPHA = 130;
    private static final float EFFECT_PULSE_STROKE_WIDTH = 1.2f;
    private static final float STANDING_ONE_STEP_HEIGHT = 3f;
    /** The lit rim's width, as a share of the height it belongs to. */
    private static final float STANDING_RIM = 0.3f;
    private static final float STANDING_RIM_MIN = 1f;
    private static final float PROJECTILE_SIZE = 5f;
    private static final float SHELL_STREAK_ALPHA = 0.5f;
    /** How much of a shell's radius the lit part keeps; the rest is its shaded edge, bottom right. */
    private static final float SHELL_LIT_SHARE = 0.78f;
    private static final float SHELL_GLINT_SHARE = 0.28f;
    /** A shell's shadow on the ground, as wide as this many of the shell's radius and this much as tall. */
    private static final float SHELL_SHADOW_WIDTH = 1.4f;
    private static final float SHELL_SHADOW_FLATTENING = 0.55f;
    private static final int ZONE_FILL_ALPHA = 80;
    private static final int ZONE_EDGE_ALPHA = 170;
    /** Round caps, so the arcs of a patch's outline join without a notch where two rims cross. */
    private static final Stroke ZONE_EDGE_STROKE = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final int ZONE_FLAMES = 6;
    private static final int ZONE_BUBBLES = 3;
    private static final int ZONE_CRYSTALS = 5;
    private static final Tones ZONE_ICE = Tones.of(new Color(170, 225, 255));
    private static final Color ZONE_FLAME_OUTER = new Color(255, 130, 40);
    private static final Color ZONE_FLAME_HEART = new Color(255, 220, 90);
    private static final Tones ZONE_MINE_CASING = Tones.of(new Color(72, 74, 88));
    private static final Color ZONE_MINE_LIT = new Color(255, 70, 70);
    private static final Color ZONE_MINE_UNLIT = new Color(110, 30, 30);
    private static final int ZONE_FALLOUT_BLADES = 3;
    /** How far toward white the innermost Stoke core goes. */
    private static final float CINDER_CORE_BRIGHTEN = 0.8f;
    private static final int ZONE_WISPS = 4;
    /** A banked missile is drawn this share of a missile in flight. */
    private static final float NEST_MISSILE_SCALE = 0.6f;
    private static final Font MISS_MARK_FONT = Hud.LABEL_FONT.deriveFont(10f);
    private static final String MISS_TEXT = "miss";
    private static final float MISS_MARK_RISE = 10f;
    private static final float MISS_MARK_GAP = 3f;
    /** Flame wave alpha, the same from launch to burn-out. */
    private static final float CINDER_CONE_ALPHA = 0.45f;
    /** How deep the wave's band is, as a share of the cone's full reach. */
    private static final float CINDER_CONE_BAND_FRACTION = 0.35f;

    private final GroundPainter groundPainter = new GroundPainter();

    private static Shape markerShape(PathMarkerShape shape, float size) {
        return switch (shape) {
            case DOT -> circleShape(size);
            case CHEVRON -> chevronShape(size);
        };
    }

    /** A sideways arrowhead, shared by path markers and rank stripes. */
    private static Shape chevronShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(-size, -size);
        p.lineTo(size, 0);
        p.lineTo(-size, size);
        p.lineTo(-size * 0.4f, 0);
        p.closePath();
        return p;
    }

    /** A skull built from {@link Area} operations: cranium and jaw, minus eyes and nose. */
    private static Shape skullShape(float scale) {
        Area skull = new Area(circleShape(scale));
        Shape jaw = new Rectangle2D.Float(-scale * 0.55f, scale * 0.1f, scale * 1.1f, scale * 0.55f);
        skull.add(new Area(jaw));
        float eyeRadius = scale * 0.22f;
        Shape leftEye = new Ellipse2D.Float(-scale * 0.5f, -scale * 0.15f, eyeRadius * 2, eyeRadius * 2);
        Shape rightEye = new Ellipse2D.Float(scale * 0.5f - eyeRadius * 2, -scale * 0.15f, eyeRadius * 2, eyeRadius * 2);
        skull.subtract(new Area(leftEye));
        skull.subtract(new Area(rightEye));
        GeneralPath nose = new GeneralPath();
        nose.moveTo(0, scale * 0.05f);
        nose.lineTo(-scale * 0.12f, scale * 0.32f);
        nose.lineTo(scale * 0.12f, scale * 0.32f);
        nose.closePath();
        skull.subtract(new Area(nose));
        return skull;
    }


    /** A carved rune down a slab's face. */
    static Shape runeShape(float scale) {
        Area rune = new Area(new Rectangle2D.Float(-scale * 0.06f, -scale * 0.6f, scale * 0.12f, scale * 1.3f));
        rune.add(new Area(new Rectangle2D.Float(-scale * 0.28f, -scale * 0.3f, scale * 0.56f, scale * 0.1f)));
        rune.add(new Area(new Rectangle2D.Float(-scale * 0.2f, scale * 0.2f, scale * 0.4f, scale * 0.1f)));
        return rune;
    }

    static Shape circleShape(float scale) {
        return new Ellipse2D.Float(-scale, -scale, scale * 2, scale * 2);
    }

    static Shape triangleShape(float scale, boolean up) {
        int u = up ? 1 : -1;
        double point = (Math.sqrt(3) * scale) / 2;
        GeneralPath p = new GeneralPath();
        p.moveTo(0.0f, -scale * u);
        p.lineTo(-point * u, scale / 2 * u);
        p.lineTo(point * u, scale / 2 * u);
        p.closePath();
        return p;
    }


    /** A rotated square. */
    static Shape diamondShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(0, -size);
        p.lineTo(size, 0);
        p.lineTo(0, size);
        p.lineTo(-size, 0);
        p.closePath();
        return p;
    }

    /** One angular shard, a part of {@link #crystalShape}. */
    static Shape shardShape(float size, double rotationRadians) {
        GeneralPath p = new GeneralPath();
        p.moveTo(0, -size);
        p.lineTo(size * 0.35f, -size * 0.2f);
        p.lineTo(size * 0.15f, size);
        p.lineTo(-size * 0.15f, size);
        p.lineTo(-size * 0.35f, -size * 0.2f);
        p.closePath();
        return AffineTransform.getRotateInstance(rotationRadians).createTransformedShape(p);
    }

    /** Three overlapping shards, faceted so ice reads unlike any smooth body. */
    static Shape crystalShape(float size) {
        Area crystal = new Area(shardShape(size, 0));
        crystal.add(new Area(shardShape(size * 0.75f, 2.1)));
        crystal.add(new Area(shardShape(size * 0.6f, -2.4)));
        return crystal;
    }

    /** A ring with {@code innerFraction} of {@code size} cut out. */
    static Shape ringShape(float size, float innerFraction) {
        Area ring = new Area(circleShape(size));
        ring.subtract(new Area(circleShape(size * innerFraction)));
        return ring;
    }

    static Shape starShape(int points, float outerRadius, float innerRadius) {
        GeneralPath p = new GeneralPath();
        for (int i = 0; i < points * 2; i++) {
            double angle = Math.PI * i / points - Math.PI / 2;
            float r = (i % 2 == 0) ? outerRadius : innerRadius;
            float x = (float) (Math.cos(angle) * r);
            float y = (float) (Math.sin(angle) * r);
            if (i == 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        p.closePath();
        return p;
    }

    /** A forward arrowhead along {@code +X}. */
    static Shape headArrowShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(size * 1.3f, 0);
        p.lineTo(0, -size * 0.5f);
        p.lineTo(0, size * 0.5f);
        p.closePath();
        return p;
    }

    static Color colorFor(Palette palette) {
        return switch (palette) {
            case TOWER_SNIPER_BODY -> TowerLooks.color(TowerLook.SNIPER);
            case TOWER_SPLASH_BODY -> TowerLooks.color(TowerLook.SPLASH);
            case TOWER_STORMCALLER_BODY -> TowerLooks.color(TowerLook.STORMCALLER);
            case TOWER_HEXER_BODY -> TowerLooks.color(TowerLook.HEXER);
            case TOWER_PULSE_BODY -> TowerLooks.color(TowerLook.PULSE);
            case TOWER_AURA_BODY -> TowerLooks.color(TowerLook.AURA);
            case TOWER_MORTAR_BODY -> TowerLooks.color(TowerLook.MORTAR);
            case TOWER_SEEKER_BODY -> TowerLooks.color(TowerLook.SEEKER);
            case TOWER_CINDER_BODY -> TowerLooks.color(TowerLook.CINDER);
            case TOWER_AURA_RING -> Color.WHITE;
            case TOWER_AURA_RALLY -> new Color(255, 170, 60);
            case TOWER_AURA_MENTEE -> new Color(255, 215, 90);
            case TOWER_UPGRADE_TIER_3 -> new Color(127, 212, 255);
            case TOWER_UPGRADE_TIER_0 -> new Color(225, 225, 230);
            case TOWER_UPGRADE_TIER_1 -> new Color(255, 200, 60);
            case TOWER_UPGRADE_TIER_2 -> new Color(200, 100, 255);
            case TOWER_UPGRADE_READY -> new Color(0x6e, 0xe0, 0x8a);
            case TOWER_SNIPER_LASER -> new Color(255, 60, 60);
            case TOWER_SNIPER_SILVER -> new Color(170, 210, 255);
            case TOWER_SNIPER_TRACER -> Color.WHITE;
            case TOWER_SPLASH_BEAM -> Color.RED;
            case TOWER_SPLASH_FLASH -> withAlpha(new Color(255, 245, 210), 170);
            case TOWER_SPLASH_DETONATION -> new Color(255, 200, 90);
            case TOWER_SPLASH_DETONATION_DARK -> new Color(120, 30, 20);
            case TOWER_SPLASH_ARC -> new Color(170, 210, 255);
            case TOWER_STORMCALLER_BEAM -> new Color(80, 150, 255);
            case TOWER_STORMCALLER_FLASH -> withAlpha(new Color(210, 230, 255), 170);
            case TOWER_STORMCALLER_DETONATION -> new Color(130, 190, 255);
            case TOWER_STORMCALLER_DETONATION_DARK -> new Color(20, 40, 120);
            case TOWER_HEXER_BEAM -> new Color(170, 80, 230);
            case TOWER_HEXER_FLASH -> withAlpha(new Color(240, 215, 255), 170);
            case TOWER_HEXER_DETONATION -> new Color(200, 130, 255);
            case TOWER_HEXER_DETONATION_DARK -> new Color(70, 20, 110);
            case TOWER_SPLASH_CAST -> new Color(200, 120, 255);
            case HEX_RUNE -> new Color(225, 170, 255);
            case TOWER_PULSE_RING -> withAlpha(Color.ORANGE, 80);
            case TOWER_PULSE_RIPPLE -> new Color(205, 205, 215);
            case TOWER_PULSE_RIPPLE_WARD -> new Color(180, 110, 255);
            case TOWER_PULSE_RIPPLE_GRAVITY -> new Color(60, 100, 230);
            case TOWER_PULSE_RIM_BITE -> new Color(235, 60, 60);
            case TOWER_PULSE_WELL -> withAlpha(new Color(20, 30, 120), 60);
            case TOWER_MORTAR_RANGING -> new Color(255, 190, 90);
            case ZONE_BURNING -> new Color(255, 120, 30);
            case ZONE_TAR -> new Color(98, 82, 116);
            case ZONE_FROST -> new Color(170, 225, 255);
            case ZONE_FALLOUT -> new Color(150, 220, 60);
            case ZONE_MINE -> new Color(255, 60, 60);
            case ZONE_CLOUD -> new Color(170, 80, 230);
            case ZONE_HEALING -> new Color(90, 220, 120);
            case ZONE_HASTE -> new Color(130, 240, 180);
            case ZONE_WARD -> new Color(120, 190, 255);
            case ZONE_BLINDING -> new Color(190, 90, 230);
            case TOWER_PULSE_ZAP -> new Color(255, 245, 190);
            case TOWER_CINDER_CONE -> new Color(255, 90, 30);
            case TOWER_CINDER_CONE_WHITE -> new Color(255, 245, 225);
            case TOWER_CINDER_CONE_SOUL -> new Color(70, 140, 255);
            case TOWER_CINDER_CONE_SEARING -> new Color(190, 20, 20);
            case PROJECTILE_CANNONBALL -> new Color(139, 90, 43);
            case PROJECTILE_NAPALM -> new Color(255, 130, 30);
            case PROJECTILE_TAR -> new Color(75, 60, 90);
            case PROJECTILE_FROST -> new Color(170, 225, 255);
            case PROJECTILE_NUKE -> new Color(245, 245, 235);
            case PROJECTILE_SHADOW -> new Color(0x6a, 0x70, 0x84, 150);
            case TOWER_MORTAR_NUKE_FLASH -> new Color(255, 255, 240);
            case PROJECTILE_MISSILE -> new Color(80, 180, 255);
            case PROJECTILE_SMOKE -> new Color(190, 200, 210);
            case PROJECTILE_CRYO -> new Color(170, 235, 255);
            case PROJECTILE_ARCANE -> new Color(190, 90, 255);
            case PROJECTILE_EMP -> new Color(255, 235, 60);
            case PROJECTILE_TRACER -> new Color(255, 70, 70);
            case STATUS_MARKER_CHILL -> new Color(120, 120, 255);
            case STATUS_MARKER_BURN -> new Color(255, 120, 40);
            case STATUS_MARKER_FREEZE -> new Color(150, 220, 255);
            case STATUS_MARKER_DAZED -> new Color(255, 235, 120);
            case STATUS_MARKER_SATURATED -> new Color(255, 150, 90);
            case STATUS_MARKER_CHARGED -> new Color(140, 200, 255);
            case STATUS_MARKER_DOOM -> new Color(190, 90, 255);
            case STATUS_MARKER_BLIGHT -> new Color(140, 200, 60);
            case STATUS_MARKER_CONTAGION -> new Color(210, 120, 170);
            case STATUS_MARKER_MALAISE -> new Color(60, 180, 170);
            case STATUS_MARKER_INVERSION -> new Color(255, 70, 120);
            case STATUS_MARKER_SYMPATHY -> new Color(120, 150, 255);
            case STATUS_MARKER_RECKONING -> new Color(255, 200, 60);
            case STATUS_MARKER_SILENCED -> new Color(170, 120, 255);
            case STATUS_MARKER_ANCHORED -> new Color(150, 150, 170);
            case STATUS_MARKER_UNRAVELED -> new Color(220, 120, 255);
            case STATUS_MARKER_BRITTLE -> new Color(200, 240, 255);
            case STATUS_MARKER_TOLL -> new Color(255, 170, 70);
            case STATUS_MARKER_CORRODED -> new Color(120, 220, 90);
            case STATUS_MARKER_DRAGGED -> new Color(110, 120, 200);
            case STATUS_MARKER_CONDEMNED -> new Color(255, 230, 120);
            case STATUS_MARKER_STILLED -> new Color(40, 60, 170);
            case STATUS_MARKER_DEAD_ZONE -> new Color(130, 130, 130);
            case STATUS_MARKER_FRAIL -> new Color(255, 80, 80);
            case STATUS_MARKER_CRACKED -> new Color(210, 170, 120);
            case STATUS_MARKER_TARRED -> new Color(70, 55, 45);
            case STATUS_MARKER_BLEEDING -> new Color(200, 30, 50);
            case STATUS_MARKER_SOULFIRE -> new Color(90, 150, 255);
            case STATUS_MARKER_SHIELD -> new Color(220, 220, 100);
            case STATUS_MARKER_INVISIBLE -> new Color(180, 180, 180);
            case STATUS_MARKER_SPRINTING -> new Color(130, 240, 180);
            case STATUS_MARKER_SPELLPROOF -> new Color(170, 120, 255);
            case STATUS_MARKER_UNYIELDING -> new Color(255, 200, 60);
            case STATUS_MARKER_HEAL -> new Color(120, 220, 140);
            case STATUS_MARKER_VULNERABLE -> new Color(235, 80, 150);
            case STATUS_MARKER_REVEALED -> new Color(200, 160, 255);
            case STATUS_MARKER_POISON -> new Color(170, 210, 40);
            case STATUS_MARKER_SCORCHED -> new Color(170, 80, 40);
            case STATUS_MARKER_SICKENED -> new Color(110, 140, 70);
            case STATUS_MARKER_SUNDERED -> new Color(190, 190, 200);
            case STATUS_MARKER_EXPOSED -> new Color(255, 215, 80);
            case STATUS_MARKER_MARKED -> new Color(255, 80, 80);
            case STATUS_MARKER_PRIORITY -> new Color(255, 150, 60);
            case CRIT_SPARK -> Color.WHITE;
            case MISS_MARK -> new Color(190, 190, 190);
            case FREEZE_CRYSTAL -> new Color(220, 245, 255);
            case SPAWN_BURST -> Color.WHITE;
            case RANK_BADGE_CHEVRON -> Color.WHITE;
            case RANK_BADGE_ELITE -> new Color(230, 190, 60);
            case RANK_BADGE_BOSS -> new Color(210, 210, 220);
            case TRAIT_ICON_PERCENT_RESIST -> new Color(180, 150, 255);
            case DAMAGE_PHYSICAL -> new Color(230, 150, 70);
            case DAMAGE_MAGIC -> new Color(90, 190, 255);
            case TRAIT_ICON_HURT_SPEED -> new Color(255, 140, 140);
            case TRAIT_ICON_HURT_AGILITY -> new Color(200, 250, 120);
            case TRAIT_ICON_REGENERATION -> new Color(110, 220, 120);
            case TRAIT_ICON_HIVEMIND -> new Color(255, 190, 50);
            case TRAIT_ICON_CRUMBLE -> new Color(215, 180, 110);
            case AFFIXED -> new Color(0, 195, 145);
            case DISRUPTION -> new Color(235, 90, 200);
            case TERM_FRAME_CONTROL -> new Color(90, 170, 255);
            case TERM_FRAME_AFFLICTION -> new Color(255, 120, 60);
            case TERM_FRAME_WEAKNESS -> new Color(185, 120, 255);
            case TERM_FRAME_HEX -> new Color(255, 80, 200);
            case TERM_FRAME_MARK -> new Color(255, 210, 80);
            case TERM_FRAME_BOON -> new Color(203, 203, 207);
            case TERM_FRAME_TRAIT -> new Color(170, 178, 190);
            case TERM_FRAME_ABILITY -> new Color(215, 160, 90);
            case TERM_FRAME_TOWER_BUFF -> new Color(217, 217, 221);
            case TERM_FRAME_TOWER_DEBUFF -> new Color(235, 90, 200);
            case TERM_FRAME_MECHANIC -> new Color(216, 216, 220);
            case TERM_FRAME_STATUS -> new Color(173, 173, 176);
            case TERM_STATUS_WAVE -> new Color(231, 231, 236);
            case TERM_STATUS_LIVES -> new Color(255, 110, 130);
            case TERM_STATUS_SCORE -> new Color(255, 210, 80);
            case TERM_STATUS_CASH -> new Color(205, 205, 209);
            case ROUND_SKULL -> new Color(226, 220, 200);
            case ROUND_SKULL_VETERAN -> new Color(130, 200, 255);
            case ROUND_SKULL_ELITE -> new Color(255, 196, 60);
            case ROUND_SKULL_BOSS -> new Color(200, 120, 255);
            case ROUND_SKULL_UNIQUE -> new Color(255, 70, 150);
            case ROUND_CLEARED -> new Color(116, 116, 118);
            case ROUND_LEAKED -> new Color(225, 70, 70);
            case ROUND_CURRENT -> new Color(231, 231, 236);
            case TERM_IMMUNE -> new Color(255, 96, 86);
            case TERM_SPAWN -> new Color(220, 200, 160);
            case TERM_FRENZY -> new Color(255, 140, 60);
            case TERM_HASTE -> new Color(255, 220, 90);
            case TERM_FOURTH_SHOT -> new Color(255, 90, 90);
            case TERM_TAR_SHELL -> new Color(130, 110, 150);
            case TERM_NUKE_SHELL -> new Color(230, 240, 150);
            case TERM_EVENT_HORIZON -> new Color(120, 110, 255);
            case TERM_AURA -> new Color(255, 235, 170);
            case TERM_LEY_LINE -> new Color(255, 170, 40);
            case TERM_INTERFERENCE -> new Color(200, 140, 90);
            case TERM_SHRAPNEL -> new Color(190, 160, 120);
            case TERM_BASE_PERK -> new Color(231, 231, 236);
            case SELECTION -> new Color(255, 255, 255);
            case UPGRADE_GATE_MET -> Hud.FOREGROUND;
            case UPGRADE_GATE_UNMET -> new Color(255, 120, 120);
        };
    }

    private static Color withAlpha(Color base, int alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.clamp(alpha, 0, 255));
    }

    /** Multiplies {@code base}'s alpha by {@code factor}, clamped, so fades compose. */
    private static Color scaleAlpha(Color base, float factor) {
        return withAlpha(base, Math.round(base.getAlpha() * factor));
    }

    public void paint(Graphics2D g2, RenderFrame frame) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        // Board art is all vector; set so this Graphics2D matches icon rendering, which needs it
        // under HiDPI scaling.
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        // paint() skips super.paint(), so the ground's fill is what clears the previous frame.
        this.groundPainter.paint(g2, frame.ground(), frame.scale(), frame.maxX(), frame.maxY());

        frame.cellGrid().ifPresent(grid -> this.paintCellGrid(g2, grid, frame));
        for (PathMarkerDraw marker : frame.pathMarkers()) {
            this.paintPathMarker(g2, marker);
        }
        for (CellDraw cell : frame.cells()) {
            this.paintCell(g2, cell, frame);
        }
        for (ZonePatchDraw patch : frame.zones()) {
            this.paintZonePatch(g2, patch);
        }
        for (TowerSpriteDraw sprite : frame.towerSprites()) {
            this.paintTowerGround(g2, sprite, frame.scale());
        }
        for (EnemyDraw enemy : frame.enemies()) {
            this.paintEnemy(g2, enemy);
        }
        for (CritSparkDraw spark : frame.critSparks()) {
            this.paintCritSpark(g2, spark);
        }
        for (TowerSpriteDraw sprite : frame.towerSprites()) {
            TowerSprites.paint(g2, sprite, frame.scale());
        }
        // A tall tower may stand over an enemy north of it; its bars and marks still show where it is and how it fares.
        for (EnemyOverlayDraw overlay : frame.enemyOverlays()) {
            this.paintEnemyOverlay(g2, overlay);
        }
        for (TowerSpriteDraw sprite : frame.towerSprites()) {
            if (sprite.upgradeReady()) {
                this.paintUpgradeArrow(g2, sprite, frame.scale());
            }
        }
        for (TowerEffectDraw effect : frame.towerEffects()) {
            this.paintTowerEffect(g2, effect);
        }
        for (ProjectileDraw projectile : frame.projectiles()) {
            this.paintProjectile(g2, projectile);
        }
    }

    /**
     * A translucent patch with a pattern of its own on each disc: flames that flicker, glossy tar with bubbles, frost
     * with crossed crystal lines. The discs go on one path and fill once, and the outline's arcs on another and stroke
     * once, so neither grows darker where its parts overlap.
     */
    private void paintZonePatch(Graphics2D g2, ZonePatchDraw patch) {
        Color base = colorFor(patch.palette());
        Path2D ground = new Path2D.Float(Path2D.WIND_NON_ZERO);
        for (ZoneDraw zone : patch.discs()) {
            float r = zone.radius();
            ground.append(new Ellipse2D.Float(zone.centerX() - r, zone.centerY() - r, r * 2, r * 2), false);
        }
        g2.setColor(withAlpha(base, ZONE_FILL_ALPHA));
        g2.fill(ground);
        Stroke defaultStroke = g2.getStroke();
        g2.setStroke(ZONE_EDGE_STROKE);
        g2.setColor(withAlpha(base, ZONE_EDGE_ALPHA));
        Path2D edge = new Path2D.Float(Path2D.WIND_NON_ZERO);
        for (ZoneArcDraw arc : patch.edge()) {
            float r = arc.radius();
            edge.append(new Arc2D.Float(arc.centerX() - r, arc.centerY() - r, r * 2, r * 2,
                    (float) -Math.toDegrees(arc.startRadians()), (float) -Math.toDegrees(arc.sweepRadians()), Arc2D.OPEN), false);
        }
        g2.draw(edge);
        for (ZoneDraw zone : patch.discs()) {
            this.paintZoneDetail(g2, patch.palette(), zone);
        }
        g2.setStroke(defaultStroke);
    }

    private void paintZoneDetail(Graphics2D g2, Palette look, ZoneDraw zone) {
        float fade = zone.fade();
        switch (look) {
            case ZONE_BURNING -> this.paintZoneFlames(g2, zone, fade);
            case ZONE_TAR -> this.paintZoneTarGloss(g2, zone, fade);
            case ZONE_FROST -> this.paintZoneCrystals(g2, zone, fade);
            case ZONE_FALLOUT -> this.paintZoneFallout(g2, zone, fade);
            case ZONE_MINE, ZONE_WARD -> this.paintZoneMine(g2, zone, fade);
            case ZONE_CLOUD -> this.paintZoneCloud(g2, zone, fade);
            default -> {
            }
        }
    }

    /** Tongues of flame that rise up the screen, each an outer flame with a hotter heart, flickering. */
    private void paintZoneFlames(Graphics2D g2, ZoneDraw zone, float fade) {
        for (int i = 0; i < ZONE_FLAMES; i++) {
            double angle = i * 2 * Math.PI / ZONE_FLAMES + i;
            float distance = zone.radius() * (0.25f + 0.5f * ((i * 7) % ZONE_FLAMES) / ZONE_FLAMES);
            float flicker = 0.5f + 0.5f * (float) Math.sin(zone.phase() * 9 + i * 1.7);
            float size = zone.radius() * (0.12f + 0.12f * flicker);
            float fx = zone.centerX() + (float) Math.cos(angle) * distance;
            float fy = zone.centerY() + (float) Math.sin(angle) * distance;
            int alpha = Math.round(230 * (0.45f + 0.55f * flicker) * fade);
            g2.setColor(withAlpha(ZONE_FLAME_OUTER, alpha));
            g2.fill(flameTongue(fx, fy, size));
            g2.setColor(withAlpha(ZONE_FLAME_HEART, alpha));
            g2.fill(flameTongue(fx, fy, size * 0.55f));
        }
    }

    /** A flame standing on {@code baseY}: round at its foot, drawn up to a point. */
    private static Shape flameTongue(float x, float baseY, float size) {
        Path2D.Float tongue = new Path2D.Float();
        tongue.moveTo(x, baseY - size * 2.6f);
        tongue.quadTo(x + size * 1.3f, baseY - size * 0.9f, x + size * 0.75f, baseY - size * 0.2f);
        tongue.quadTo(x, baseY + size * 0.5f, x - size * 0.75f, baseY - size * 0.2f);
        tongue.quadTo(x - size * 1.3f, baseY - size * 0.9f, x, baseY - size * 2.6f);
        tongue.closePath();
        return tongue;
    }

    private void paintZoneTarGloss(Graphics2D g2, ZoneDraw zone, float fade) {
        float r = zone.radius();
        g2.setColor(withAlpha(new Color(130, 120, 150), Math.round(90 * fade)));
        g2.fill(new Ellipse2D.Float(zone.centerX() - r * 0.6f, zone.centerY() - r * 0.65f, r * 0.7f, r * 0.35f));
        for (int i = 0; i < ZONE_BUBBLES; i++) {
            float pulse = (zone.phase() * 0.6f + i * 0.37f) % 1f;
            float bx = zone.centerX() + r * 0.5f * (float) Math.cos(i * 2.4);
            float by = zone.centerY() + r * 0.5f * (float) Math.sin(i * 2.4);
            float size = 1.5f + 2.5f * pulse;
            g2.setColor(withAlpha(new Color(90, 80, 100), Math.round(140 * (1f - pulse) * fade)));
            g2.draw(new Ellipse2D.Float(bx - size, by - size, size * 2, size * 2));
        }
    }

    /** Three fan blades turning slowly: the radiation sign. */
    private void paintZoneFallout(Graphics2D g2, ZoneDraw zone, float fade) {
        float r = zone.radius() * 0.7f;
        g2.setColor(withAlpha(new Color(30, 40, 10), Math.round(150 * fade)));
        for (int i = 0; i < ZONE_FALLOUT_BLADES; i++) {
            double start = Math.toDegrees(zone.phase() * 0.8 + i * 2 * Math.PI / ZONE_FALLOUT_BLADES);
            g2.fill(new Arc2D.Float(zone.centerX() - r, zone.centerY() - r, r * 2, r * 2, (float) start, 60f, Arc2D.PIE));
        }
    }

    /** A dark casing standing on the ground, with a red light that blinks. */
    private void paintZoneMine(Graphics2D g2, ZoneDraw zone, float fade) {
        float r = zone.radius() * 0.45f;
        Composite composite = g2.getComposite();
        AffineTransform save = g2.getTransform();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.clamp(fade, 0f, 1f)));
        g2.translate(zone.centerX(), zone.centerY());
        Shape casing = new Ellipse2D.Float(-r, -r, r * 2, r * 2);
        paintStanding(g2, casing, ZONE_MINE_CASING, 0, r * 0.45f);
        g2.setColor(ZONE_MINE_CASING.body());
        g2.fill(casing);
        boolean lit = Math.sin(zone.phase() * 8) > 0;
        float light = r * 0.4f;
        g2.setColor(lit ? ZONE_MINE_LIT : ZONE_MINE_UNLIT);
        g2.fill(new Ellipse2D.Float(-light, -light, light * 2, light * 2));
        g2.setTransform(save);
        g2.setComposite(composite);
    }

    /** Wisps that drift round the middle. */
    private void paintZoneCloud(Graphics2D g2, ZoneDraw zone, float fade) {
        for (int i = 0; i < ZONE_WISPS; i++) {
            double angle = zone.phase() * 0.7 + i * 2 * Math.PI / ZONE_WISPS;
            float distance = zone.radius() * 0.45f;
            float size = zone.radius() * 0.28f;
            float wx = zone.centerX() + (float) Math.cos(angle) * distance;
            float wy = zone.centerY() + (float) Math.sin(angle) * distance;
            g2.setColor(withAlpha(new Color(210, 150, 255), Math.round(110 * fade)));
            g2.fill(new Ellipse2D.Float(wx - size, wy - size, size * 2, size * 2));
        }
    }

    /** Shards of ice standing on the frost, each lit on its left face. */
    private void paintZoneCrystals(Graphics2D g2, ZoneDraw zone, float fade) {
        Composite composite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.clamp(fade, 0f, 1f)));
        for (int i = 0; i < ZONE_CRYSTALS; i++) {
            double angle = i * 2 * Math.PI / ZONE_CRYSTALS + 0.6;
            float distance = zone.radius() * (i % 2 == 0 ? 0.55f : 0.22f);
            float x = zone.centerX() + (float) Math.cos(angle) * distance;
            float y = zone.centerY() + (float) Math.sin(angle) * distance;
            float size = zone.radius() * (0.16f + 0.05f * (i % 3));
            g2.setColor(ZONE_ICE.shade());
            g2.fill(iceShard(x, y, size, 1f));
            g2.setColor(ZONE_ICE.body());
            g2.fill(iceShard(x, y, size, 0f));
            g2.setColor(ZONE_ICE.highlight());
            g2.fill(iceShard(x, y, size, -1f));
        }
        g2.setComposite(composite);
    }

    /** One face of a shard whose foot is at {@code x, y}: the left face, the right face, or both as one. */
    private static Shape iceShard(float x, float y, float size, float face) {
        Path2D.Float shard = new Path2D.Float();
        shard.moveTo(x + size * 0.1f, y - size * 2.2f);
        if (face >= 0f) {
            shard.lineTo(x + size * 0.6f, y - size * 0.6f);
        }
        shard.lineTo(x, y + size * 0.3f);
        if (face <= 0f) {
            shard.lineTo(x - size * 0.55f, y - size * 0.5f);
        }
        shard.closePath();
        return shard;
    }

    /** One-pixel lines on each cell's right and bottom edge only, so neighbours share one line. */
    private void paintCellGrid(Graphics2D g2, CellGridDraw grid, RenderFrame frame) {
        int scale = frame.scale();
        g2.setColor(CELL_BLOCKED);
        for (CellGridDraw.BlockedCell cell : grid.blockedCells()) {
            g2.fillRect(cell.x(), cell.y(), scale - 1, scale - 1);
        }
        g2.setColor(CELL_GRID_LINE);
        for (int x = scale - 1; x <= frame.maxX(); x += scale) {
            g2.drawLine(x, 0, x, frame.maxY());
        }
        for (int y = scale - 1; y <= frame.maxY(); y += scale) {
            g2.drawLine(0, y, frame.maxX(), y);
        }
    }

    private void paintCell(Graphics2D g2, CellDraw cell, RenderFrame frame) {
        switch (cell.highlight()) {
            case PLACE -> this.paintPlaceHighlight(g2, cell, frame);
            case SELECT -> this.paintSelectHighlight(g2, cell, frame);
            case NONE -> {
            }
        }
    }

    private void paintPlaceHighlight(Graphics2D g2, CellDraw cell, RenderFrame frame) {
        int scale = frame.scale();
        int x = cell.x();
        int y = cell.y();
        Color highlightColor = !cell.buildable() ? CELL_NOK : cell.affordable() ? CELL_OK : CELL_SHORT;

        this.paintRangeCircle(g2, cell, frame);

        g2.setColor(withAlpha(highlightColor, 80));
        g2.drawLine(x, 0, x, frame.maxY());
        g2.drawLine(x + scale, 0, x + scale, frame.maxY());
        g2.drawLine(0, y, frame.maxX(), y);
        g2.drawLine(0, y + scale, frame.maxX(), y + scale);
        g2.fillRect(x, y, scale, scale);

        g2.setColor(withAlpha(highlightColor, 140));
        g2.drawRoundRect(x - scale, y - scale, scale * 3, scale * 3, 40, 40);

        g2.setColor(highlightColor);
        g2.drawRect(x, y, scale, scale);
    }

    private void paintRangeCircle(Graphics2D g2, CellDraw cell, RenderFrame frame) {
        int scale = frame.scale();
        float realRange = cell.rangeCells() * scale;
        float halfScale = (float) scale / 2;
        g2.setColor(CELL_RANGE);
        g2.draw(new Ellipse2D.Float(cell.x() - realRange + halfScale, cell.y() - realRange + halfScale, realRange * 2, realRange * 2));
    }

    private void paintSelectHighlight(Graphics2D g2, CellDraw cell, RenderFrame frame) {
        int scale = frame.scale();
        int x = cell.x();
        int y = cell.y();
        g2.setColor(withAlpha(CELL_OK, 80));
        g2.fillRect(x, y, scale, scale);
        g2.setColor(CELL_OK);
        g2.drawRect(x - 1, y - 1, scale + 1, scale + 1);
    }

    private void paintPathMarker(Graphics2D g2, PathMarkerDraw marker) {
        AffineTransform save = g2.getTransform();
        g2.translate(marker.x(), marker.y());
        g2.rotate(marker.facingRadians());
        PathColor color = marker.color();
        g2.setColor(withAlpha(new Color(color.r(), color.g(), color.b()), pathMarkerAlpha(marker.brightness())));
        g2.fill(markerShape(marker.shape(), marker.size()));
        g2.setTransform(save);
    }

    /** Dim for the trail, bright for the moving chevrons. */
    private static int pathMarkerAlpha(PathMarkerBrightness brightness) {
        return switch (brightness) {
            case STATIC -> 40;
            case MOVING -> 100;
        };
    }

    /** Draws enemy bodies and their rank badges and affix pips alone, for the wave preview. */
    public void paintEnemies(Graphics2D g2, List<EnemyDraw> enemies, List<EnemyOverlayDraw> badges) {
        for (EnemyDraw enemy : enemies) {
            this.paintEnemy(g2, enemy);
        }
        for (EnemyOverlayDraw badge : badges) {
            this.paintEnemyOverlay(g2, badge);
        }
    }

    /** Draws path markers alone, for a lane's colour swatch. */
    public void paintPathMarkers(Graphics2D g2, List<PathMarkerDraw> markers) {
        for (PathMarkerDraw marker : markers) {
            this.paintPathMarker(g2, marker);
        }
    }

    private void paintEnemy(Graphics2D g2, EnemyDraw enemy) {
        switch (enemy) {
            case EnemyBodyDraw body -> this.paintEnemyBody(g2, body);
            case EnemyFadeDraw fade -> this.paintEnemyFade(g2, fade);
            case EnemyEmissionDraw emission -> EnemyEffects.paintEmission(g2, emission);
        }
    }

    private void paintEnemyBody(Graphics2D g2, EnemyBodyDraw body) {
        EnemySprites.paint(g2, body);
    }

    /**
     * What makes a flat shape stand on the board, painted under its top: its side, falling straight down the screen
     * whichever way the shape faces, and a lit rim along its top-left edge. The caller is at the shape's centre.
     */
    private static void paintStanding(Graphics2D g2, Shape top, Tones tones, double facingRadians, float height) {
        AffineTransform centre = g2.getTransform();
        g2.setColor(tones.shade());
        // A low side is one copy of the top; a taller one needs a copy between, or a thin shape would leave a gap.
        int steps = height > STANDING_ONE_STEP_HEIGHT ? 2 : 1;
        for (int step = steps; step >= 1; step--) {
            g2.translate(0, height * step / steps);
            g2.rotate(facingRadians);
            g2.fill(top);
            g2.setTransform(centre);
        }
        float rim = Math.max(STANDING_RIM_MIN, height * STANDING_RIM);
        g2.setColor(tones.highlight());
        g2.translate(-rim, -rim);
        g2.rotate(facingRadians);
        g2.fill(top);
        g2.setTransform(centre);
    }

    /** Drawn upright above the body, never rotated with it. */
    private void paintRankBadge(Graphics2D g2, RankBadgeDraw badge) {
        AffineTransform save = g2.getTransform();
        Composite composite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, badge.alpha()));
        g2.translate(badge.x(), badge.y());
        this.paintRankBadgeGlyph(g2, badge.badge(), badge.size());
        g2.setComposite(composite);
        g2.setTransform(save);
    }

    /** Each pip a dark disc with its affix's pictogram in one colour, so the shape alone tells them apart. */
    private void paintAffixPips(Graphics2D g2, AffixPipsDraw pips) {
        Color ink = scaleAlpha(colorFor(Palette.AFFIXED), pips.alpha());
        Color disc = scaleAlpha(AFFIX_PIP_DISC, pips.alpha());
        float radius = pips.size();
        for (int i = 0; i < pips.affixes().size(); i++) {
            float cx = pips.x() + i * pips.step();
            g2.setColor(disc);
            g2.fill(new Ellipse2D.Float(cx - radius, pips.y() - radius, radius * 2, radius * 2));
            Term affix = pips.affixes().get(i);
            Graphics2D symbol = (Graphics2D) g2.create();
            try {
                float side = radius * AFFIX_PIP_SYMBOL_SHARE * 2;
                symbol.translate(cx - side / 2, pips.y() - side / 2);
                symbol.scale(side / Pictogram.UNIT, side / Pictogram.UNIT);
                PictogramCatalogue.of(affix.key()).ifPresent(art -> TermIconPainter.paintLayers(symbol, art, ink, disc));
            } finally {
                symbol.dispose();
            }
        }
    }

    /** A rank badge centred on the origin, as above an enemy on the board. */
    void paintRankBadgeGlyph(Graphics2D g2, RankBadge badge, float badgeSize) {
        rankBadgePalette(badge).ifPresent(palette -> g2.setColor(colorFor(palette)));
        AffineTransform save = g2.getTransform();
        switch (badge) {
            case ONE_CHEVRON -> paintRankChevron(g2, badgeSize);
            case TWO_CHEVRON -> {
                float spacing = badgeSize * RANK_BADGE_CHEVRON_SPACING;
                g2.translate(0, -spacing / 2);
                paintRankChevron(g2, badgeSize);
                g2.translate(0, spacing);
                paintRankChevron(g2, badgeSize);
            }
            case STAR -> g2.fill(starShape(5, badgeSize, badgeSize * 0.45f));
            case SKULL -> g2.fill(skullShape(badgeSize));
            case NONE -> {
            }
        }
        g2.setTransform(save);
    }

    /** A path's chevron centred on the origin, pointing the way its markers run. */
    void paintPathGlyph(Graphics2D g2, Color color, float size) {
        g2.setColor(color);
        g2.fill(chevronShape(size));
    }

    /** The colour a rank badge is drawn in; a grunt has no badge. */
    static Optional<Palette> rankBadgePalette(RankBadge badge) {
        return switch (badge) {
            case ONE_CHEVRON, TWO_CHEVRON -> Optional.of(Palette.RANK_BADGE_CHEVRON);
            case STAR -> Optional.of(Palette.RANK_BADGE_ELITE);
            case SKULL -> Optional.of(Palette.RANK_BADGE_BOSS);
            case NONE -> Optional.empty();
        };
    }

    /** An enemy's body centred on the origin, upright and at full health. */
    void paintEnemyGlyph(Graphics2D g2, EnemyLook body, float size) {
        EnemyLooks.Row row = EnemyLooks.of(body);
        g2.setColor(row.color());
        g2.fill(row.body().at(size));
        if (row.detail().isPresent()) {
            EnemyLooks.Detail detail = row.detail().get();
            g2.setColor(detail.color());
            g2.fill(detail.shape().at(size));
        }
    }

    /** A tower's icon as an info-panel glyph, centred on the origin and reaching {@code size} from it. */
    void paintTowerGlyph(Graphics2D g2, TowerLook look, float size) {
        TowerIcons.paint(g2, look, size / ICON_REACH_CELLS);
    }

    /**
     * An info-panel row's glyph centred on the origin: the board's own marks (trait and effect
     * diamonds, crit spark, slot pip), or a neutral mark for a stat nothing on the board marks.
     */
    void paintRowGlyph(Graphics2D g2, SheetLine.Glyph glyph, Optional<Palette> tone, float size) {
        g2.setColor(tone.map(Java2DFrameRenderer::colorFor).orElse(UNMARKED_GLYPH));
        Stroke defaultStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(HOLLOW_GLYPH_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (glyph) {
            case HOLLOW_DIAMOND -> g2.draw(diamondShape(size));
            case FILLED_DIAMOND -> g2.fill(diamondShape(size));
            case DOT -> g2.fill(circleShape(size * DOT_GLYPH_SIZE_FRACTION));
            case CHEVRON -> g2.fill(chevronShape(size * 0.75f));
            case RING -> g2.draw(circleShape(size * 0.8f));
            case SPARK -> g2.fill(starShape(4, size, size * 0.3f));
            case SKULL -> g2.fill(skullShape(size * 0.85f));
            case PIP -> g2.fill(circleShape(size * 0.6f));
            case CHECK -> g2.draw(checkShape(size * 0.8f));
            case CROSS -> g2.draw(crossStrokesShape(size * 0.6f));
            case LOCK -> {
                g2.fill(lockBodyShape(size * 0.75f));
                g2.draw(lockShackleShape(size * 0.75f));
            }
            case RING_CENTRE -> {
                g2.draw(circleShape(size * 0.8f));
                g2.fill(circleShape(size * 0.25f));
            }
            case BROKEN_RING -> g2.draw(new Arc2D.Float(-size * 0.8f, -size * 0.8f, size * 1.6f, size * 1.6f, 120f, 300f,
                    Arc2D.OPEN));
            case WEDGE -> g2.fill(new Arc2D.Float(-size, -size * 0.6f, size * 2f, size * 2f, 55f, 70f, Arc2D.PIE));
            case CASH -> CashMark.paint(g2, 0f, 0f, size * 2f);
            case PIERCE -> {
                g2.draw(new Line2D.Float(size * 0.2f, -size * 0.8f, size * 0.2f, size * 0.8f));
                g2.draw(new Line2D.Float(-size * 0.9f, 0f, size * 0.8f, 0f));
                g2.fill(AffineTransform.getTranslateInstance(size * 0.6f, 0f)
                        .createTransformedShape(chevronShape(size * 0.45f)));
            }
            case CROSSHAIR, CROSSHAIR_FILLED -> {
                g2.draw(circleShape(size * 0.65f));
                g2.draw(new Line2D.Float(-size, 0f, -size * 0.35f, 0f));
                g2.draw(new Line2D.Float(size * 0.35f, 0f, size, 0f));
                g2.draw(new Line2D.Float(0f, -size, 0f, -size * 0.35f));
                g2.draw(new Line2D.Float(0f, size * 0.35f, 0f, size));
                if (glyph == SheetLine.Glyph.CROSSHAIR_FILLED) {
                    g2.fill(circleShape(size * 0.25f));
                }
            }
            case HOLLOW_SPARK -> g2.draw(starShape(4, size, size * 0.3f));
            case HOLLOW_SHIELD -> g2.draw(shieldShape(size * 0.85f));
            case FILLED_SHIELD -> g2.fill(shieldShape(size * 0.85f));
            case CRACKED_SHIELD -> {
                g2.draw(shieldShape(size * 0.85f));
                Path2D.Float crack = new Path2D.Float();
                crack.moveTo(-size * 0.1f, -size * 0.8f);
                crack.lineTo(size * 0.15f, -size * 0.2f);
                crack.lineTo(-size * 0.15f, size * 0.15f);
                crack.lineTo(size * 0.05f, size * 0.6f);
                g2.draw(crack);
            }
            case PLUS -> {
                g2.fill(new Rectangle2D.Float(-size * 0.25f, -size * 0.8f, size * 0.5f, size * 1.6f));
                g2.fill(new Rectangle2D.Float(-size * 0.8f, -size * 0.25f, size * 1.6f, size * 0.5f));
            }
            case COMET -> {
                g2.fill(AffineTransform.getTranslateInstance(size * 0.4f, -size * 0.4f)
                        .createTransformedShape(circleShape(size * 0.45f)));
                g2.draw(new Line2D.Float(-size * 0.8f, size * 0.8f, size * 0.1f, -size * 0.1f));
            }
        }
        g2.setStroke(defaultStroke);
    }

    /** A heater shield: flat top, sides curving to a point. */
    private static Shape shieldShape(float size) {
        Path2D.Float shield = new Path2D.Float();
        shield.moveTo(-size, -size);
        shield.lineTo(size, -size);
        shield.lineTo(size, -size * 0.1f);
        shield.quadTo(size * 0.9f, size * 0.7f, 0f, size * 1.1f);
        shield.quadTo(-size * 0.9f, size * 0.7f, -size, -size * 0.1f);
        shield.closePath();
        return shield;
    }

    /** A tick mark, to be stroked. */
    private static Shape checkShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(-size, 0);
        p.lineTo(-size * 0.3f, size * 0.7f);
        p.lineTo(size, -size * 0.7f);
        return p;
    }

    /** A padlock's body: the lower part of the glyph box. */
    private static Shape lockBodyShape(float size) {
        return new RoundRectangle2D.Float(-size * 0.8f, -size * 0.1f, size * 1.6f, size * 1.1f, size * 0.3f, size * 0.3f);
    }

    /** A padlock's shackle: an arch standing on the body. */
    private static Shape lockShackleShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(-size * 0.5f, -size * 0.1f);
        p.lineTo(-size * 0.5f, -size * 0.45f);
        p.quadTo(-size * 0.5f, -size, 0, -size);
        p.quadTo(size * 0.5f, -size, size * 0.5f, -size * 0.45f);
        p.lineTo(size * 0.5f, -size * 0.1f);
        return p;
    }

    private static Shape crossStrokesShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(-size, -size);
        p.lineTo(size, size);
        p.moveTo(size, -size);
        p.lineTo(-size, size);
        return p;
    }

    /** An open stroke: a filled arrowhead is the loudest mark on a board, and two of them merge into one. */
    private static void paintRankChevron(Graphics2D g2, float size) {
        GeneralPath chevron = new GeneralPath();
        chevron.moveTo(-size, size * RANK_BADGE_CHEVRON_RISE);
        chevron.lineTo(0, -size * RANK_BADGE_CHEVRON_RISE);
        chevron.lineTo(size, size * RANK_BADGE_CHEVRON_RISE);
        Stroke stroke = g2.getStroke();
        g2.setStroke(new BasicStroke(size * RANK_BADGE_CHEVRON_STROKE));
        g2.draw(chevron);
        g2.setStroke(stroke);
    }

    private void paintEnemyFade(Graphics2D g2, EnemyFadeDraw fade) {
        EnemyLooks.Row row = EnemyLooks.of(fade.look());
        AffineTransform save = g2.getTransform();
        g2.translate(fade.x(), fade.y());
        g2.rotate(fade.facingRadians());
        g2.setColor(withAlpha(row.color(), Math.round((1f - fade.fadeProgress()) * 255f)));
        for (Shape outline : row.fade().at(fade.scale(), fade.scale() + fade.growth())) {
            g2.draw(outline);
        }
        g2.setTransform(save);
    }

    /** A fading four-point sparkle where a critical hit landed, distinct from the five-point star, turned its own way. */
    private void paintCritSpark(Graphics2D g2, CritSparkDraw spark) {
        AffineTransform save = g2.getTransform();
        g2.translate(spark.x(), spark.y());
        g2.setColor(withAlpha(colorFor(spark.palette()), Math.round((1f - spark.fadeProgress()) * 255f)));
        float sparkScale = spark.scale() * (1f + spark.fadeProgress());
        g2.rotate(spark.rotation());
        g2.fill(starShape(4, sparkScale, sparkScale * 0.25f));
        g2.setTransform(save);
    }

    private void paintEnemyOverlay(Graphics2D g2, EnemyOverlayDraw overlay) {
        switch (overlay) {
            case EnemyRingDraw ring -> this.paintEnemyRing(g2, ring);
            case EffectPulseDraw pulse -> this.paintEffectPulse(g2, pulse);
            case EnemyIceDraw ice -> EnemyEffects.paintIce(g2, ice);
            case HexRuneDraw rune -> this.paintHexRune(g2, rune);
            case MissMarkDraw miss -> this.paintMissMark(g2, miss);
            case EnemyBarsDraw bars -> this.paintEnemyBars(g2, bars);
            case EnemyDotsDraw dots -> this.paintEnemyDots(g2, dots);
            case RankBadgeDraw badge -> this.paintRankBadge(g2, badge);
            case AffixPipsDraw pips -> this.paintAffixPips(g2, pips);
            case EnemyEggDraw egg -> EnemyEffects.paintEgg(g2, egg);
            case EnemyBurstDraw burst -> EnemyEffects.paintBurst(g2, burst);
            case EnemyEmissionDraw emission -> EnemyEffects.paintEmission(g2, emission);
            case EnemyTintDraw tint -> EnemyEffects.paintTint(g2, tint);
        }
    }

    /** One dark track behind every bar, so the stack reads as one element on any floor; each fill shrinks to the left. */
    private void paintEnemyBars(Graphics2D g2, EnemyBarsDraw bars) {
        float left = bars.x() - bars.width() / 2f;
        int rows = bars.bars().size();
        g2.setColor(scaleAlpha(ENEMY_BAR_TRACK, bars.alpha()));
        g2.fill(new Rectangle2D.Float(left - ENEMY_BAR_TRACK_MARGIN, bars.y() - ENEMY_BAR_TRACK_MARGIN,
                bars.width() + ENEMY_BAR_TRACK_MARGIN * 2,
                rows * ENEMY_BAR_HEIGHT + (rows - 1) * ENEMY_BAR_GAP + ENEMY_BAR_TRACK_MARGIN * 2));
        for (int i = 0; i < rows; i++) {
            EnemyBarsDraw.Bar bar = bars.bars().get(i);
            g2.setColor(scaleAlpha(EnemyLooks.barColor(bar.look()), bars.alpha()));
            g2.fill(new Rectangle2D.Float(left, bars.y() + i * (ENEMY_BAR_HEIGHT + ENEMY_BAR_GAP),
                    bars.width() * bar.fill(), ENEMY_BAR_HEIGHT));
        }
    }

    private void paintEnemyDots(Graphics2D g2, EnemyDotsDraw dots) {
        float left = dots.x() - ENEMY_DOT_SPACING * (dots.dots().size() - 1) / 2f;
        for (int i = 0; i < dots.dots().size(); i++) {
            g2.setColor(scaleAlpha(EnemyLooks.dotColor(dots.dots().get(i)), dots.alpha()));
            g2.fill(new Ellipse2D.Float(left + ENEMY_DOT_SPACING * i - ENEMY_DOT_RADIUS, dots.y() - ENEMY_DOT_RADIUS,
                    ENEMY_DOT_RADIUS * 2, ENEMY_DOT_RADIUS * 2));
        }
    }

    /** A grey "miss" beside the enemy that dodged, rising a little as it fades. */
    private void paintMissMark(Graphics2D g2, MissMarkDraw miss) {
        g2.setFont(MISS_MARK_FONT);
        g2.setColor(withAlpha(colorFor(Palette.MISS_MARK), Math.round((1f - miss.fadeProgress()) * 255f)));
        float baseline = miss.y() + g2.getFontMetrics().getAscent() / 2f - MISS_MARK_RISE * miss.fadeProgress();
        g2.drawString(MISS_TEXT, miss.x() + MISS_MARK_GAP, baseline);
    }

    /** A hex's rune: a thin glyph in the rune colour, its shape saying what the hex does. */
    private void paintHexRune(Graphics2D g2, HexRuneDraw rune) {
        AffineTransform save = g2.getTransform();
        Stroke defaultStroke = g2.getStroke();
        g2.translate(rune.x(), rune.y());
        g2.setColor(colorFor(Palette.HEX_RUNE));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(runeShape(rune.glyph(), rune.scale()));
        g2.setStroke(defaultStroke);
        g2.setTransform(save);
    }

    private static Shape runeShape(HexGlyph glyph, float s) {
        Path2D.Float path = new Path2D.Float();
        switch (glyph) {
            case DOOM -> {
                path.moveTo(-s, -s);
                path.lineTo(s, -s);
                path.lineTo(-s, s);
                path.lineTo(s, s);
                path.closePath();
            }
            case BLIGHT -> {
                path.moveTo(0, -s);
                path.quadTo(s, 0, 0, s);
                path.quadTo(-s, 0, 0, -s);
            }
            case CONTAGION -> {
                path.moveTo(0, -s);
                path.lineTo(s, s);
                path.lineTo(-s, s);
                path.closePath();
                path.append(new Ellipse2D.Float(-s * 0.3f, -s * 1.3f, s * 0.6f, s * 0.6f), false);
                path.append(new Ellipse2D.Float(s * 0.7f, s * 0.7f, s * 0.6f, s * 0.6f), false);
                path.append(new Ellipse2D.Float(-s * 1.3f, s * 0.7f, s * 0.6f, s * 0.6f), false);
            }
            case MALAISE -> {
                path.append(new Ellipse2D.Float(-s, -s, s * 2, s * 2), false);
                path.moveTo(0, -s * 0.65f);
                path.lineTo(0, 0);
                path.lineTo(s * 0.5f, 0);
            }
            case INVERSION -> {
                path.moveTo(-s, -s);
                path.lineTo(s, -s);
                path.lineTo(0, s);
                path.closePath();
            }
            case SYMPATHY -> {
                path.append(new Ellipse2D.Float(-s * 1.2f, -s * 0.6f, s * 1.2f, s * 1.2f), false);
                path.append(new Ellipse2D.Float(0, -s * 0.6f, s * 1.2f, s * 1.2f), false);
            }
            case RECKONING -> {
                path.append(new Ellipse2D.Float(-s * 0.4f, -s * 0.4f, s * 0.8f, s * 0.8f), false);
                for (int i = 0; i < 4; i++) {
                    double angle = Math.PI / 4 + Math.PI / 2 * i;
                    float dx = (float) Math.cos(angle);
                    float dy = (float) Math.sin(angle);
                    path.moveTo(dx * s * 0.6f, dy * s * 0.6f);
                    path.lineTo(dx * s * 1.3f, dy * s * 1.3f);
                }
            }
        }
        return path;
    }

    /** A small filled diamond beside a tower for a status it carries. */
    private void paintTowerStatus(Graphics2D g2, TowerStatusDraw status) {
        AffineTransform save = g2.getTransform();
        g2.translate(status.x(), status.y());
        g2.setColor(colorFor(status.palette()));
        g2.fill(diamondShape(status.scale()));
        g2.setTransform(save);
    }

    /** A thin ring around an enemy: its selection or an aura's reach. */
    private void paintEnemyRing(Graphics2D g2, EnemyRingDraw ring) {
        if (ring.radius() <= 0) {
            return;
        }
        Stroke defaultStroke = g2.getStroke();
        g2.setColor(withAlpha(colorFor(ring.palette()), Math.round(ring.alpha() * 255)));
        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(new Ellipse2D.Float(ring.centerX() - ring.radius(), ring.centerY() - ring.radius(), ring.radius() * 2, ring.radius() * 2));
        g2.setStroke(defaultStroke);
    }

    /** A ring growing from nothing as it fades, for a cast or a spawn; quiet, since many can fire at once. */
    private void paintEffectPulse(Graphics2D g2, EffectPulseDraw pulse) {
        float radius = pulse.radius() * pulse.progress();
        if (radius <= 0) {
            return;
        }
        Stroke defaultStroke = g2.getStroke();
        g2.setColor(withAlpha(colorFor(pulse.palette()), Math.round((1f - pulse.progress()) * EFFECT_PULSE_ALPHA)));
        g2.setStroke(new BasicStroke(EFFECT_PULSE_STROKE_WIDTH));
        g2.draw(new Ellipse2D.Float(pulse.centerX() - radius, pulse.centerY() - radius, radius * 2, radius * 2));
        g2.setStroke(defaultStroke);
    }

    /**
     * Renders one tower's icon for the toolbar: its mark in its colour, the part that moves in a lighter tone.
     * Transparent background; the control paints its own face.
     */
    public BufferedImage renderTowerIcon(TowerLook look, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(size / 2.0, size / 2.0);
        TowerIcons.paint(g2, look, size);
        g2.dispose();
        return image;
    }

    /** What a tower lays on the ground: its range ring when selected, and its shadow. */
    private void paintTowerGround(Graphics2D g2, TowerSpriteDraw sprite, int scale) {
        if (sprite.selected()) {
            float r = sprite.rangeReal();
            g2.setColor(Color.PINK);
            g2.draw(new Ellipse2D.Float(sprite.centerX() - r, sprite.centerY() - r, r * 2, r * 2));
        }
        TowerSprites.paintShadow(g2, sprite, scale);
    }

    /** One green up arrow in the cell's top-left corner, the same whichever tier the upgrade is in. */
    private void paintUpgradeArrow(Graphics2D g2, TowerSpriteDraw sprite, int scale) {
        AffineTransform save = g2.getTransform();
        g2.translate(sprite.centerX() - scale * (0.5f - UPGRADE_ARROW_INSET),
                sprite.centerY() - scale * (0.5f - UPGRADE_ARROW_INSET));
        g2.scale(scale, scale);
        float a = UPGRADE_ARROW_SIZE;
        float shaft = a * 0.36f;
        Path2D arrow = new Path2D.Float();
        arrow.moveTo(0, -a);
        arrow.lineTo(a * 0.9f, 0);
        arrow.lineTo(shaft, 0);
        arrow.lineTo(shaft, a * 0.9f);
        arrow.lineTo(-shaft, a * 0.9f);
        arrow.lineTo(-shaft, 0);
        arrow.lineTo(-a * 0.9f, 0);
        arrow.closePath();
        Stroke previous = g2.getStroke();
        g2.setStroke(new BasicStroke(UPGRADE_ARROW_OUTLINE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(UPGRADE_ARROW_EDGE);
        g2.draw(arrow);
        g2.setStroke(previous);
        g2.setColor(colorFor(Palette.TOWER_UPGRADE_READY));
        g2.fill(arrow);
        g2.setTransform(save);
    }

    private void paintTowerEffect(Graphics2D g2, TowerEffectDraw effect) {
        switch (effect) {
            case BeamDraw beam -> this.paintBeam(g2, beam);
            case TracerDraw tracer -> this.paintTracer(g2, tracer);
            case LeyLineDraw line -> this.paintLeyLine(g2, line);
            case SplashDraw splash ->
                    this.paintFilledCircle(g2, splash.palette(), splash.centerX(), splash.centerY(), splash.radius());
            case PulseDraw pulse ->
                    this.paintFilledCircle(g2, pulse.palette(), pulse.centerX(), pulse.centerY(), pulse.radius());
            case RingDraw ring -> this.paintRing(g2, ring);
            case FlashDraw flash -> this.paintFlash(g2, flash);
            case ConeDraw cone -> this.paintCone(g2, cone);
            case NestDraw nest -> this.paintNest(g2, nest);
            case TowerStatusDraw status -> this.paintTowerStatus(g2, status);
            case RefundMarkDraw mark -> CashMark.paint(g2, mark.x(), mark.y(), mark.height());
        }
    }


    /**
     * A band between two arcs on the cone's heading, its outer arc at the wave's front as
     * {@code cone.progress()} goes from 0 to 1. The trailing arc keeps the band clear of the tower.
     */
    private void paintCone(Graphics2D g2, ConeDraw cone) {
        float frontRadius = cone.maxRadius() * cone.progress();
        if (frontRadius <= 0) {
            return;
        }
        float trailingRadius = Math.max(0f, frontRadius - cone.maxRadius() * CINDER_CONE_BAND_FRACTION);
        AffineTransform save = g2.getTransform();
        g2.translate(cone.originX(), cone.originY());
        g2.rotate(cone.headingRadians());
        Color flame = colorFor(cone.palette());
        g2.setColor(withAlpha(flame, Math.round(CINDER_CONE_ALPHA * 255)));
        g2.fill(coneBand(frontRadius, trailingRadius, cone.halfWidthRadians()));
        for (int step = 1; step <= cone.stokeSteps(); step++) {
            float share = step / (cone.stokeSteps() + 1f);
            g2.setColor(withAlpha(brightened(flame, share * CINDER_CORE_BRIGHTEN), Math.round(CINDER_CONE_ALPHA * 255)));
            g2.fill(coneBand(frontRadius, trailingRadius, cone.halfWidthRadians() * (1f - share)));
        }
        g2.setTransform(save);
    }

    private static Area coneBand(float frontRadius, float trailingRadius, double halfWidthRadians) {
        Area band = new Area(wedge(frontRadius, halfWidthRadians));
        if (trailingRadius > 0) {
            band.subtract(new Area(wedge(trailingRadius, halfWidthRadians)));
        }
        return band;
    }

    /** {@code color} a share of the way to white. */
    private static Color brightened(Color color, float share) {
        return new Color(color.getRed() + Math.round((255 - color.getRed()) * share),
                color.getGreen() + Math.round((255 - color.getGreen()) * share),
                color.getBlue() + Math.round((255 - color.getBlue()) * share));
    }

    /** A pie slice centred on the origin along the x axis. */
    private static Shape wedge(float radius, double halfWidthRadians) {
        float halfWidthDegrees = (float) Math.toDegrees(halfWidthRadians);
        return new Arc2D.Float(-radius, -radius, radius * 2, radius * 2, -halfWidthDegrees, halfWidthDegrees * 2,
                Arc2D.PIE);
    }

    private void paintFlash(Graphics2D g2, FlashDraw flash) {
        g2.setColor(withAlpha(colorFor(flash.palette()), Math.round(flash.alpha() * 255)));
        g2.fill(new Ellipse2D.Float(flash.centerX() - flash.radius(), flash.centerY() - flash.radius(),
                flash.radius() * 2, flash.radius() * 2));
    }

    private void paintRing(Graphics2D g2, RingDraw ring) {
        if (ring.radius() <= 0) {
            return;
        }
        Stroke defaultStroke = g2.getStroke();
        g2.setColor(withAlpha(colorFor(ring.palette()), Math.round(ring.alpha() * 255)));
        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(new Ellipse2D.Float(ring.centerX() - ring.radius(), ring.centerY() - ring.radius(), ring.radius() * 2, ring.radius() * 2));
        g2.setStroke(defaultStroke);
    }

    private void paintBeam(Graphics2D g2, BeamDraw beam) {
        Stroke defaultStroke = g2.getStroke();
        g2.setColor(scaleAlpha(colorFor(beam.palette()), beam.alpha()));
        g2.setStroke(new BasicStroke(beam.strokeWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
        g2.draw(new Line2D.Float(beam.fromX(), beam.fromY(), beam.toX(), beam.toY()));
        g2.setStroke(defaultStroke);
    }


    /** The streak in short pieces, each brighter, wider and nearer the head colour than the one behind it. */
    private void paintTracer(Graphics2D g2, TracerDraw tracer) {
        Stroke defaultStroke = g2.getStroke();
        Color tail = colorFor(tracer.tail());
        Color head = colorFor(tracer.head());
        float dx = tracer.toX() - tracer.fromX();
        float dy = tracer.toY() - tracer.fromY();
        for (int i = 0; i < TRACER_PIECES; i++) {
            float share = i / (TRACER_PIECES - 1f);
            float a = (float) i / TRACER_PIECES;
            float b = (i + 1f) / TRACER_PIECES;
            g2.setColor(scaleAlpha(Hud.mix(tail, head, share), tracer.alpha() * (TRACER_TAIL_ALPHA + (1f - TRACER_TAIL_ALPHA) * share)));
            g2.setStroke(new BasicStroke(tracer.strokeWidth() * (TRACER_TAIL_WIDTH + (1f - TRACER_TAIL_WIDTH) * share),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
            g2.draw(new Line2D.Float(tracer.fromX() + dx * a, tracer.fromY() + dy * a,
                    tracer.fromX() + dx * b, tracer.fromY() + dy * b));
        }
        g2.setStroke(defaultStroke);
    }

    private void paintLeyLine(Graphics2D g2, LeyLineDraw line) {
        Stroke defaultStroke = g2.getStroke();
        g2.setColor(scaleAlpha(colorFor(line.palette()), line.alpha()));
        g2.setStroke(line.pinned()
                ? new BasicStroke(LEY_LINE_STROKE, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, LEY_LINE_DASH, 0f)
                : new BasicStroke(LEY_LINE_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
        g2.draw(new Line2D.Float(line.fromX(), line.fromY(), line.toX(), line.toY()));
        g2.setStroke(defaultStroke);
    }

    private void paintFilledCircle(Graphics2D g2, Palette palette, float centerX, float centerY, float radius) {
        g2.setColor(colorFor(palette));
        g2.fill(new Ellipse2D.Float(centerX - radius, centerY - radius, radius * 2, radius * 2));
    }

    private void paintProjectile(Graphics2D g2, ProjectileDraw projectile) {
        switch (projectile) {
            case CannonballDraw shell ->
                    this.paintShell(g2, shell);
            case MissileDraw missile -> this.paintMissile(g2, missile);
            case SmokeDraw smoke -> this.paintSmoke(g2, smoke);
        }
    }

    /** The shadow on the ground, then the shell over it, lifted along its arc. */
    private void paintShell(Graphics2D g2, CannonballDraw shell) {
        float radius = PROJECTILE_SIZE * shell.size();
        float shadowX = radius * SHELL_SHADOW_WIDTH * shell.shadowScale();
        float shadowY = shadowX * SHELL_SHADOW_FLATTENING;
        g2.setColor(colorFor(Palette.PROJECTILE_SHADOW));
        g2.fill(new Ellipse2D.Float(shell.x() - shadowX, shell.y() - shadowY, shadowX * 2, shadowY * 2));
        float x = shell.x();
        float y = shell.y() - shell.lift();
        if (shell.tailX() != x || shell.tailY() != y) {
            Stroke defaultStroke = g2.getStroke();
            g2.setColor(scaleAlpha(colorFor(shell.palette()), SHELL_STREAK_ALPHA));
            g2.setStroke(new BasicStroke(radius, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Float(shell.tailX(), shell.tailY(), x, y));
            g2.setStroke(defaultStroke);
        }
        Tones tones = Tones.of(colorFor(shell.palette()));
        g2.setColor(tones.shade());
        g2.fill(new Ellipse2D.Float(x - radius, y - radius, radius * 2, radius * 2));
        float lit = radius * SHELL_LIT_SHARE;
        float shift = radius - lit;
        g2.setColor(tones.body());
        g2.fill(new Ellipse2D.Float(x - radius, y - radius, lit * 2 + shift, lit * 2 + shift));
        float glint = radius * SHELL_GLINT_SHARE;
        g2.setColor(tones.highlight());
        g2.fill(new Ellipse2D.Float(x - radius * 0.62f, y - radius * 0.62f, glint * 2, glint * 2));
    }

    private void paintMissile(Graphics2D g2, MissileDraw missile) {
        AffineTransform save = g2.getTransform();
        g2.translate(missile.x(), missile.y());
        g2.rotate(missile.facingRadians());
        g2.setColor(colorFor(missile.palette()));
        g2.fill(headArrowShape(PROJECTILE_SIZE * missile.size()));
        g2.setTransform(save);
    }

    private void paintSmoke(Graphics2D g2, SmokeDraw smoke) {
        Composite save = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.clamp(smoke.alpha(), 0f, 1f)));
        this.paintFilledCircle(g2, smoke.palette(), smoke.x(), smoke.y(), smoke.radius());
        g2.setComposite(save);
    }

    /** The banked missiles, small and evenly spread, each pointing the way it circles. */
    private void paintNest(Graphics2D g2, NestDraw nest) {
        AffineTransform save = g2.getTransform();
        g2.setColor(colorFor(nest.palette()));
        for (int i = 0; i < nest.count(); i++) {
            double angle = nest.phaseRadians() + Math.PI * 2 * i / nest.count();
            g2.translate(nest.centerX() + Math.cos(angle) * nest.orbitRadius(),
                    nest.centerY() + Math.sin(angle) * nest.orbitRadius());
            g2.rotate(angle + Math.PI / 2);
            g2.fill(headArrowShape(PROJECTILE_SIZE * NEST_MISSILE_SCALE));
            g2.setTransform(save);
        }
    }
}
