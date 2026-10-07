package td.ui;

import td.ui.render.BeamDraw;
import td.ui.render.CannonballDraw;
import td.ui.render.CellDraw;
import td.ui.render.CellGridDraw;
import td.ui.render.ConeDraw;
import td.ui.render.CritSparkDraw;
import td.ui.render.EffectPulseDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.EnemyOverlayDraw;
import td.ui.render.EnemyRingDraw;
import td.ui.render.FlashDraw;
import td.ui.render.HexGlyph;
import td.ui.render.HexRuneDraw;
import td.ui.render.IceCrystalDraw;
import td.ui.render.MissileDraw;
import td.ui.render.NestDraw;
import td.ui.render.Palette;
import td.ui.render.PathMarkerBrightness;
import td.ui.render.PathMarkerDraw;
import td.ui.render.PathMarkerShape;
import td.ui.render.ProjectileDraw;
import td.ui.render.PulseDirection;
import td.ui.render.PulseDraw;
import td.ui.render.RankBadge;
import td.ui.render.RenderFrame;
import td.ui.render.RingDraw;
import td.ui.render.SheetLine;
import td.ui.render.SlotMarkDraw;
import td.ui.render.SmokeDraw;
import td.ui.render.SplashDraw;
import td.ui.render.StatusMarkerDraw;
import td.ui.render.TowerEffectDraw;
import td.ui.render.TowerSpriteDraw;
import td.ui.render.TowerStatusDraw;
import td.ui.render.TraitMarkerDraw;
import td.ui.render.TurretHeadDraw;
import td.ui.render.ZoneDraw;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns a {@link RenderFrame} into {@code Graphics2D} calls: the only class that does, and the only
 * owner of colours, shapes and strokes for board content, all keyed off {@link Palette}.
 * <p>
 * {@code Panel*} components use AWT for layout and previews but never paint board content;
 * {@link #paintEnemies}, {@link #renderTowerIcon} and the {@code paint*Glyph} methods exist for
 * them.
 */
public final class Java2DFrameRenderer {

    private static final Color CELL_OK = Color.GRAY;
    private static final Color CELL_NOK = Color.RED;
    private static final Color CELL_RANGE = new Color(250, 250, 210, 150);
    private static final Color BOARD_BACKGROUND = Color.BLACK;
    /** The HUD's dark grey-green, so the dev grid reads as part of the panel chrome. */
    private static final Color CELL_GRID_LINE = new Color(48, 60, 48);
    private static final Color CELL_BLOCKED = new Color(26, 34, 26);
    /** An info-panel glyph for a stat nothing on the board marks. */
    private static final Color UNMARKED_GLYPH = new Color(150, 170, 150);
    private static final float HOLLOW_GLYPH_STROKE_WIDTH = 1.3f;
    /** A dot is a small mark, not a body. */
    private static final float DOT_GLYPH_SIZE_FRACTION = 0.4f;
    /** Share of a cell a tower body fills. */
    private static final float TOWER_BODY_SIZE_FRACTION = 0.42f;

    /** Small, so the toolbar reads as a row of glyphs. */
    private static final float ICON_BODY_SIZE_FRACTION = 0.25f;
    /** Slot pip radius and spacing, as fractions of the body size. */
    private static final float SLOT_PIP_RADIUS_FRACTION = 0.11f;
    private static final float SLOT_PIP_SPACING_FRACTION = 0.3f;
    private static final float SLOT_PIP_ROW_OFFSET_FRACTION = 0.95f;
    /** Extra gap between one slot's pips and the next. */
    private static final float SLOT_GROUP_GAP_FRACTION = 0.16f;
    private static final float SLOT_CHEVRON_SIZE_FRACTION = 0.16f;
    private static final float SLOT_CHEVRON_ROW_OFFSET_FRACTION = 1.05f;
    private static final float SLOT_CHEVRON_SPACING_FRACTION = 0.45f;
    /** Halo radius relative to the body; every body shape stays within its size. */
    private static final float ENCHANT_HALO_RADIUS_FRACTION = 1.3f;
    private static final float ENCHANT_HALO_STROKE_WIDTH = 2.0f;
    /** Outside the enchant halo, so a tower wearing both shows both. */
    private static final float TRANSCENDENT_HALO_RADIUS_FRACTION = 1.5f;
    private static final float TRANSCENDENT_HALO_STROKE_WIDTH = 1.6f;
    private static final int TRANSCENDENT_HALO_DASHES = 8;
    private static final int TRANSCENDENT_HALO_ALPHA = 200;
    private static final float TRANSCENDENT_PIP_SCALE = 1.6f;
    /** Rank pips stack up the body's left side, apart from the slot pips below it. */
    private static final float RANK_PIP_RADIUS_FRACTION = 0.09f;
    private static final float RANK_PIP_SPACING_FRACTION = 0.32f;
    private static final float RANK_PIP_COLUMN_OFFSET_FRACTION = 0.95f;
    private static final float RANK_UP_GLOW_MAX_RADIUS_FRACTION = 1.9f;
    private static final float RANK_UP_GLOW_STROKE_WIDTH = 2.5f;
    /** Turret head size relative to a cell. */
    private static final float TOWER_HEAD_SIZE_FRACTION = 0.24f;

    private static final float SONAR_ARC_DEGREES = 62f;
    /**
     * Wedge radius relative to the head size; keeps the wedge just inside the cell at any scale.
     */
    private static final float SONAR_RADIUS_FACTOR = 1.85f;

    private static final int SONAR_TRAIL_STEPS = 4;
    private static final int SONAR_TRAIL_ALPHA = 150;
    /** Badge size and offset relative to body scale; smaller badges were unreadable. */
    private static final float RANK_BADGE_SIZE_FRACTION = 0.6f;
    private static final float RANK_BADGE_OFFSET_FRACTION = 1.7f;
    private static final float RANK_BADGE_CHEVRON_SPACING_FRACTION = 0.55f;
    private static final float PROJECTILE_SIZE = 5f;
    private static final float SHELL_STREAK_ALPHA = 0.5f;
    private static final int ZONE_FILL_ALPHA = 80;
    private static final int ZONE_EDGE_ALPHA = 170;
    /** A zone fades over its last quarter of life. */
    private static final float ZONE_FADE_SHARE_INVERSE = 4f;
    private static final int ZONE_FLAMES = 6;
    private static final int ZONE_BUBBLES = 3;
    private static final int ZONE_CRYSTAL_ARMS = 3;
    private static final int ZONE_FALLOUT_BLADES = 3;
    /** A banked missile is drawn this share of a missile in flight. */
    private static final float NEST_MISSILE_SCALE = 0.6f;
    private static final Font MARKER_COUNT_FONT = Hud.LABEL_FONT.deriveFont(9f);
    /** Flame wave alpha, the same from launch to burn-out. */
    private static final float CINDER_CONE_ALPHA = 0.45f;
    /** How deep the wave's band is, as a share of the cone's full reach. */
    private static final float CINDER_CONE_BAND_FRACTION = 0.35f;

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


    private static Shape enemyShape(Palette palette, float scale) {
        return switch (palette) {
            case ENEMY_CIRCLE, ENEMY_GHOST -> circleShape(scale);
            case ENEMY_SQUARE -> new Rectangle2D.Float(-scale, -scale, scale * 2, scale * 2);
            case ENEMY_TRIANGLE -> triangleShape(scale, true);
            case ENEMY_WARDEN -> starShape(8, scale, scale * 0.55f);
            case ENEMY_WARDEN_EGG -> wardenEggShape(scale);
            case ENEMY_MENDER -> crossShape(scale);
            default -> throw new IllegalStateException("Not an enemy palette: " + palette);
        };
    }

    /** A cross: two rectangles unioned. */
    private static Shape crossShape(float scale) {
        float arm = scale * 0.55f;
        Area cross = new Area(new Rectangle2D.Float(-scale, -arm, scale * 2, arm * 2));
        cross.add(new Area(new Rectangle2D.Float(-arm, -scale, arm * 2, scale * 2)));
        return cross;
    }

    /**
     * A tall, spiked ovoid in the same star language as the boss body, so it reads as that boss's
     * egg.
     */
    private static Shape wardenEggShape(float scale) {
        Shape star = starShape(6, scale, scale * 0.65f);
        AffineTransform elongate = AffineTransform.getScaleInstance(0.8, 1.3);
        return elongate.createTransformedShape(star);
    }

    private static Shape circleShape(float scale) {
        return new Ellipse2D.Float(-scale, -scale, scale * 2, scale * 2);
    }

    private static Shape triangleShape(float scale, boolean up) {
        int u = up ? 1 : -1;
        double point = (Math.sqrt(3) * scale) / 2;
        GeneralPath p = new GeneralPath();
        p.moveTo(0.0f, -scale * u);
        p.lineTo(-point * u, scale / 2 * u);
        p.lineTo(point * u, scale / 2 * u);
        p.closePath();
        return p;
    }


    /**
     * One flat symbol per tower, naming what it does. Each is one closed {@link Shape}, painted the
     * same way at any size.
     */
    private static Shape towerBodyShape(Palette palette, float size) {
        return switch (palette) {
            case TOWER_SNIPER_BODY -> triangleShape(size, true);
            case TOWER_SPLASH_BODY, TOWER_STORMCALLER_BODY, TOWER_HEXER_BODY -> ringShape(size, 0.55f);
            case TOWER_SONAR_BODY -> spiralShape(size);
            case TOWER_PULSE_BODY -> starShape(5, size, size * 0.45f);
            case TOWER_AURA_BODY -> pulsarShape(size);
            case TOWER_MORTAR_BODY -> diamondShape(size);
            case TOWER_SEEKER_BODY -> kiteShape(size);
            case TOWER_CINDER_BODY -> flameShape(size);
            default -> throw new IllegalStateException("Not a tower body palette: " + palette);
        };
    }


    /** A rotated square. */
    private static Shape diamondShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(0, -size);
        p.lineTo(size, 0);
        p.lineTo(0, size);
        p.lineTo(-size, 0);
        p.closePath();
        return p;
    }

    /** A concave arrowhead. */
    private static Shape kiteShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(size, 0);
        p.lineTo(-size * 0.6f, -size * 0.7f);
        p.lineTo(-size * 0.2f, 0);
        p.lineTo(-size * 0.6f, size * 0.7f);
        p.closePath();
        return p;
    }

    /** A teardrop. */
    private static Shape flameShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(0, -size);
        p.curveTo(size * 0.9f, -size * 0.2f, size * 0.6f, size * 0.5f, 0, size);
        p.curveTo(-size * 0.6f, size * 0.5f, -size * 0.9f, -size * 0.2f, 0, -size);
        p.closePath();
        return p;
    }

    /** One angular shard, a part of {@link #crystalShape}. */
    private static Shape shardShape(float size, double rotationRadians) {
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
    private static Shape crystalShape(float size) {
        Area crystal = new Area(shardShape(size, 0));
        crystal.add(new Area(shardShape(size * 0.75f, 2.1)));
        crystal.add(new Area(shardShape(size * 0.6f, -2.4)));
        return crystal;
    }

    /** A ring with {@code innerFraction} of {@code size} cut out. */
    private static Shape ringShape(float size, float innerFraction) {
        Area ring = new Area(circleShape(size));
        ring.subtract(new Area(circleShape(size * innerFraction)));
        return ring;
    }

    /**
     * An Archimedean spiral stroked into a closed ribbon, so it fills as a line rather than a blob.
     */
    private static Shape spiralShape(float size) {
        int turns = 2;
        int steps = 64 * turns;
        // Thin relative to the gap between consecutive turns (which is maxRadius/turns): at a
        // toolbar icon's size a thicker ribbon closes those gaps and the spiral reads as a disc.
        float strokeWidth = size * 0.16f;
        // Stop short of `size` so the stroke's own width stays inside the tower's footprint.
        float maxRadius = size - strokeWidth / 2;
        GeneralPath p = new GeneralPath();
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double angle = t * turns * 2 * Math.PI;
            double radius = maxRadius * t;
            float x = (float) (Math.cos(angle) * radius);
            float y = (float) (Math.sin(angle) * radius);
            if (i == 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        Shape ribbon = new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(p);
        // Area, not the stroked outline directly: filling that outline under the default
        // non-zero winding rule floods the region the spiral encloses and the symbol comes out
        // as a plain disc. Area resolves it to the ribbon actually swept.
        return new Area(ribbon);
    }

    /** A bright core inside a detached ring. */
    private static Shape pulsarShape(float size) {
        Area pulsar = new Area(ringShape(size, 0.72f));
        pulsar.add(new Area(circleShape(size * 0.36f)));
        return pulsar;
    }

    private static Shape starShape(int points, float outerRadius, float innerRadius) {
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

    /**
     * Aiming heads are drawn pointing along {@code +X}, so rotating the graphics aims them.
     * Spinning heads must reach past their base's outline, or the spin is invisible. The sonar head
     * is not handled here: it has {@link #paintSonarSweep}.
     */
    private static Shape turretHeadShape(Palette palette, float size) {
        return switch (palette) {
            case TOWER_SNIPER_BODY, TOWER_SNIPER_GOLD_BARREL ->
                    new Rectangle2D.Float(0, -size * 0.22f, size * 1.3f, size * 0.44f);
            case TOWER_SPLASH_BODY, TOWER_STORMCALLER_BODY, TOWER_HEXER_BODY ->
                    new Rectangle2D.Float(0, -size * 0.42f, size * 0.95f, size * 0.84f);
            case TOWER_PULSE_BODY -> {
                Shape moon = starShape(5, size * 0.9f, size * 0.9f * 0.45f);
                yield AffineTransform.getTranslateInstance(size * 2.0, 0).createTransformedShape(moon);
            }
            case TOWER_AURA_BODY -> circleShape(size);
            case TOWER_MORTAR_BODY, TOWER_SEEKER_BODY, TOWER_CINDER_BODY -> headArrowShape(size);
            default -> throw new IllegalStateException("Not a tower head palette: " + palette);
        };
    }

    /** A forward arrowhead along {@code +X}. */
    private static Shape headArrowShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(size * 1.3f, 0);
        p.lineTo(0, -size * 0.5f);
        p.lineTo(0, size * 0.5f);
        p.closePath();
        return p;
    }

    /**
     * One band of the sweep's trail. {@link Arc2D} angles are y-up, so the trail behind the beam
     * has negative angles.
     */
    private static Shape sonarWedge(float radius, float fromDegrees, float extentDegrees) {
        return new Arc2D.Float(-radius, -radius, radius * 2, radius * 2,
                fromDegrees, extentDegrees, Arc2D.PIE);
    }

    static Color colorFor(Palette palette) {
        return switch (palette) {
            case ENEMY_CIRCLE -> Color.CYAN;
            case ENEMY_GHOST -> Color.LIGHT_GRAY;
            case ENEMY_SQUARE -> Color.PINK;
            case ENEMY_TRIANGLE -> Color.YELLOW;
            case ENEMY_WARDEN, ENEMY_WARDEN_EGG -> new Color(139, 0, 0);
            case ENEMY_MENDER -> new Color(120, 220, 150);
            case TOWER_SNIPER_BODY -> Color.GREEN;
            case TOWER_SPLASH_BODY -> Color.RED;
            case TOWER_STORMCALLER_BODY -> new Color(80, 150, 255);
            case TOWER_HEXER_BODY -> new Color(170, 80, 230);
            case TOWER_SONAR_BODY -> Color.YELLOW;
            case TOWER_PULSE_BODY -> Color.ORANGE;
            case TOWER_AURA_BODY -> Color.WHITE;
            case TOWER_MORTAR_BODY -> new Color(139, 90, 43);
            case TOWER_SEEKER_BODY -> new Color(80, 180, 255);
            case TOWER_CINDER_BODY -> new Color(255, 90, 30);
            case TOWER_AURA_RING -> Color.WHITE;
            case TOWER_AURA_LINK -> withAlpha(Color.WHITE, 60);
            case TOWER_UPGRADE_BASE -> new Color(140, 255, 140);
            case TOWER_UPGRADE_HEAD -> new Color(255, 200, 60);
            case TOWER_UPGRADE_SPECIAL -> new Color(200, 100, 255);
            case TOWER_TRANSCENDENT -> new Color(255, 205, 70);
            case TOWER_RANK -> new Color(205, 215, 235);
            case TOWER_SNIPER_BEAM -> Color.GREEN;
            case TOWER_SNIPER_LASER -> new Color(255, 60, 60);
            case TOWER_SNIPER_SILVER -> new Color(170, 210, 255);
            case TOWER_SNIPER_TRACER -> Color.WHITE;
            case TOWER_SNIPER_GOLD_BARREL -> new Color(255, 205, 70);
            case TOWER_SPLASH_BEAM -> Color.RED;
            case TOWER_SPLASH_FLASH -> withAlpha(new Color(255, 245, 210), 170);
            case TOWER_SPLASH_DETONATION -> new Color(255, 200, 90);
            case TOWER_SPLASH_DETONATION_DARK -> new Color(120, 30, 20);
            case TOWER_SPLASH_ARC -> new Color(170, 210, 255);
            case TOWER_SPLASH_CAST -> new Color(200, 120, 255);
            case HEX_RUNE -> new Color(225, 170, 255);
            case TOWER_SONAR_BEAM -> Color.YELLOW;
            case TOWER_SONAR_MAGIC_BEAM -> new Color(90, 150, 255);
            case TOWER_SONAR_PING -> new Color(255, 240, 150);
            case TOWER_SONAR_CROSSHAIR -> new Color(255, 120, 60);
            case TOWER_PULSE_RING -> withAlpha(Color.ORANGE, 80);
            case TOWER_PULSE_RIPPLE -> new Color(255, 190, 90);
            case TOWER_PULSE_RIPPLE_NULL -> new Color(180, 110, 255);
            case TOWER_PULSE_RIPPLE_UNDERTOW -> new Color(90, 150, 255);
            case TOWER_PULSE_RIPPLE_CORROSION -> new Color(120, 220, 90);
            case TOWER_MORTAR_RANGING -> new Color(255, 190, 90);
            case ZONE_BURNING -> new Color(255, 120, 30);
            case ZONE_TAR -> new Color(35, 28, 25);
            case ZONE_FROST -> new Color(170, 225, 255);
            case ZONE_FALLOUT -> new Color(150, 220, 60);
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
            case STATUS_MARKER_RIME -> new Color(170, 230, 255);
            case STATUS_MARKER_ASH -> new Color(255, 120, 40);
            case STATUS_MARKER_INVERSION -> new Color(255, 70, 120);
            case STATUS_MARKER_SYMPATHY -> new Color(120, 150, 255);
            case STATUS_MARKER_RECKONING -> new Color(255, 200, 60);
            case STATUS_MARKER_SILENCED -> new Color(170, 120, 255);
            case STATUS_MARKER_ANCHORED -> new Color(150, 150, 170);
            case STATUS_MARKER_UNRAVELED -> new Color(220, 120, 255);
            case STATUS_MARKER_BRITTLE -> new Color(200, 240, 255);
            case STATUS_MARKER_TOLL -> new Color(255, 170, 70);
            case STATUS_MARKER_CORRODED -> new Color(120, 220, 90);
            case STATUS_MARKER_UNDERTOW -> new Color(80, 130, 255);
            case STATUS_MARKER_DEAD_ZONE -> new Color(130, 130, 130);
            case STATUS_MARKER_KILL_ZONE -> new Color(255, 80, 80);
            case STATUS_MARKER_CRACKED -> new Color(210, 170, 120);
            case STATUS_MARKER_TARRED -> new Color(70, 55, 45);
            case STATUS_MARKER_BLEEDING -> new Color(200, 30, 50);
            case STATUS_MARKER_SOULFIRE -> new Color(90, 150, 255);
            case STATUS_MARKER_SHIELD -> new Color(220, 220, 100);
            case STATUS_MARKER_INVISIBLE -> new Color(180, 180, 180);
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
            case STATUS_MARKER_RESONATING -> new Color(90, 160, 255);
            case STATUS_MARKER_FRACTURED -> new Color(160, 110, 220);
            case STATUS_MARKER_OVERFLOW -> Color.WHITE;
            case FREEZE_CRYSTAL -> new Color(220, 245, 255);
            case CRIT_SPARK -> Color.WHITE;
            case SPAWN_BURST -> Color.WHITE;
            case RANK_BADGE_CHEVRON -> Color.WHITE;
            case RANK_BADGE_ELITE -> new Color(230, 190, 60);
            case RANK_BADGE_BOSS -> new Color(210, 210, 220);
            case TRAIT_MARKER_PERCENT_RESIST -> new Color(180, 150, 255);
            case TRAIT_MARKER_PHYSICAL_RESIST, DAMAGE_PHYSICAL -> new Color(230, 150, 70);
            case TRAIT_MARKER_MAGIC_RESIST, DAMAGE_MAGIC -> new Color(90, 190, 255);
            case TRAIT_MARKER_FLAT_RESIST -> new Color(140, 110, 200);
            case TRAIT_MARKER_CRITICAL_IMMUNE -> new Color(255, 210, 130);
            case TRAIT_MARKER_HURT_SPEED -> new Color(255, 140, 140);
            case TRAIT_MARKER_BURN_IMMUNE -> new Color(255, 150, 90);
            case TRAIT_MARKER_FREEZE_IMMUNE -> new Color(170, 225, 255);
            case TRAIT_MARKER_EFFECT_RESIST -> new Color(200, 200, 160);
            case TRAIT_MARKER_OVERFLOW -> Color.LIGHT_GRAY;
            case DISRUPTION -> new Color(235, 90, 200);
            case SELECTION -> new Color(255, 255, 255);
            case UPGRADE_GATE_MET -> new Color(140, 255, 140);
            case UPGRADE_GATE_UNMET -> new Color(255, 120, 120);
        };
    }

    private static Color withAlpha(Color base, int alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.min(255, Math.max(alpha, 0)));
    }

    private static Color healthColor(Color base, float healthFraction) {
        return withAlpha(base, Math.round(healthFraction * 255));
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
        // paint() skips super.paint(), so this fill is what clears the previous frame.
        g2.setColor(BOARD_BACKGROUND);
        g2.fillRect(0, 0, frame.maxX(), frame.maxY());

        frame.cellGrid().ifPresent(grid -> this.paintCellGrid(g2, grid, frame));
        for (PathMarkerDraw marker : frame.pathMarkers()) {
            this.paintPathMarker(g2, marker);
        }
        for (CellDraw cell : frame.cells()) {
            this.paintCell(g2, cell, frame);
        }
        for (ZoneDraw zone : frame.zones()) {
            this.paintZone(g2, zone);
        }
        for (EnemyDraw enemy : frame.enemies()) {
            this.paintEnemy(g2, enemy);
        }
        for (StatusMarkerDraw marker : frame.statusMarkers()) {
            this.paintStatusMarker(g2, marker);
        }
        for (CritSparkDraw spark : frame.critSparks()) {
            this.paintCritSpark(g2, spark);
        }
        for (EnemyOverlayDraw overlay : frame.enemyOverlays()) {
            this.paintEnemyOverlay(g2, overlay);
        }
        for (TowerSpriteDraw sprite : frame.towerSprites()) {
            this.paintTowerSprite(g2, sprite, frame.scale());
        }
        for (TurretHeadDraw head : frame.towerHeads()) {
            this.paintTurretHead(g2, head, frame.scale());
        }
        for (TowerEffectDraw effect : frame.towerEffects()) {
            this.paintTowerEffect(g2, effect);
        }
        for (ProjectileDraw projectile : frame.projectiles()) {
            this.paintProjectile(g2, projectile);
        }
    }

    /**
     * A translucent patch with a pattern of its own: flames that flicker, glossy tar with bubbles,
     * frost with crossed crystal lines. It fades over its last quarter of life.
     */
    private void paintZone(Graphics2D g2, ZoneDraw zone) {
        Color base = colorFor(zone.palette());
        float fade = Math.min(1f, zone.life() * ZONE_FADE_SHARE_INVERSE);
        float cx = zone.centerX();
        float cy = zone.centerY();
        float r = zone.radius();
        Ellipse2D disc = new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2);
        g2.setColor(withAlpha(base, Math.round(ZONE_FILL_ALPHA * fade)));
        g2.fill(disc);
        Stroke defaultStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(withAlpha(base, Math.round(ZONE_EDGE_ALPHA * fade)));
        g2.draw(disc);
        switch (zone.palette()) {
            case ZONE_BURNING -> this.paintZoneFlames(g2, zone, fade);
            case ZONE_TAR -> this.paintZoneTarGloss(g2, zone, fade);
            case ZONE_FROST -> this.paintZoneCrystals(g2, zone, fade);
            case ZONE_FALLOUT -> this.paintZoneFallout(g2, zone, fade);
            default -> {
            }
        }
        g2.setStroke(defaultStroke);
    }

    private void paintZoneFlames(Graphics2D g2, ZoneDraw zone, float fade) {
        for (int i = 0; i < ZONE_FLAMES; i++) {
            double angle = i * 2 * Math.PI / ZONE_FLAMES + i;
            float distance = zone.radius() * (0.25f + 0.5f * ((i * 7) % ZONE_FLAMES) / ZONE_FLAMES);
            float flicker = 0.5f + 0.5f * (float) Math.sin(zone.phase() * 9 + i * 1.7);
            float size = zone.radius() * (0.12f + 0.12f * flicker);
            float fx = zone.centerX() + (float) Math.cos(angle) * distance;
            float fy = zone.centerY() + (float) Math.sin(angle) * distance;
            g2.setColor(withAlpha(new Color(255, 200, 60), Math.round(200 * flicker * fade)));
            g2.fill(new Ellipse2D.Float(fx - size, fy - size * 1.4f, size * 2, size * 2.8f));
        }
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

    private void paintZoneCrystals(Graphics2D g2, ZoneDraw zone, float fade) {
        g2.setColor(withAlpha(new Color(235, 250, 255), Math.round(170 * fade)));
        float r = zone.radius() * 0.7f;
        for (int i = 0; i < ZONE_CRYSTAL_ARMS; i++) {
            double angle = i * Math.PI / ZONE_CRYSTAL_ARMS;
            float dx = (float) Math.cos(angle) * r;
            float dy = (float) Math.sin(angle) * r;
            g2.draw(new Line2D.Float(zone.centerX() - dx, zone.centerY() - dy, zone.centerX() + dx, zone.centerY() + dy));
        }
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
        Color highlightColor = cell.buildable() ? CELL_OK : CELL_NOK;

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

    /** Draws enemy bodies alone, for the wave preview. */
    public void paintEnemies(Graphics2D g2, List<EnemyDraw> enemies) {
        for (EnemyDraw enemy : enemies) {
            this.paintEnemy(g2, enemy);
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
        }
    }

    private void paintEnemyBody(Graphics2D g2, EnemyBodyDraw body) {
        Color color = colorFor(body.palette());
        AffineTransform save = g2.getTransform();
        g2.translate(body.x(), body.y());
        g2.rotate(body.facingRadians());
        Shape shape = enemyShape(body.palette(), body.scale());
        // Cloaking is a further alpha reduction on top of the health-fraction one below, not a
        // replacement for it - a badly wounded, cloaked mob still reads as both at once.
        float visibility = 1f - body.cloakProgress();
        g2.setColor(scaleAlpha(color, visibility));
        g2.draw(shape);
        g2.setColor(scaleAlpha(healthColor(color, body.healthFraction()), visibility));
        g2.fill(shape);
        g2.setTransform(save);
        this.paintRankBadge(g2, body);
    }

    /** Drawn upright above the body, never rotated with it. */
    private void paintRankBadge(Graphics2D g2, EnemyBodyDraw body) {
        AffineTransform save = g2.getTransform();
        g2.translate(body.x(), body.y() - body.scale() * RANK_BADGE_OFFSET_FRACTION);
        this.paintRankBadgeGlyph(g2, body.badge(), body.scale() * RANK_BADGE_SIZE_FRACTION);
        g2.setTransform(save);
    }

    /** A rank badge centred on the origin, as above an enemy on the board. */
    void paintRankBadgeGlyph(Graphics2D g2, RankBadge badge, float badgeSize) {
        rankBadgePalette(badge).ifPresent(palette -> g2.setColor(colorFor(palette)));
        AffineTransform save = g2.getTransform();
        switch (badge) {
            case ONE_CHEVRON -> this.paintUprightChevron(g2, badgeSize);
            case TWO_CHEVRON -> {
                float spacing = badgeSize * RANK_BADGE_CHEVRON_SPACING_FRACTION / RANK_BADGE_SIZE_FRACTION;
                g2.translate(0, -spacing / 2);
                this.paintUprightChevron(g2, badgeSize);
                g2.translate(0, spacing);
                this.paintUprightChevron(g2, badgeSize);
            }
            case STAR -> g2.fill(starShape(5, badgeSize, badgeSize * 0.45f));
            case SKULL -> g2.fill(skullShape(badgeSize));
            case NONE -> {
            }
        }
        g2.setTransform(save);
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
    void paintEnemyGlyph(Graphics2D g2, Palette body, float size) {
        g2.setColor(colorFor(body));
        g2.fill(enemyShape(body, size));
    }

    /**
     * An info-panel row's glyph centred on the origin: the board's own marks (trait and effect
     * diamonds, crit spark, slot pip, tower body), or a neutral mark for a stat nothing on the
     * board marks. {@link SheetLine.Glyph#TOWER_BODY} takes its shape from {@code tone}.
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
            case TOWER_BODY -> g2.fill(towerBodyShape(tone.orElseThrow(), size));
        }
        g2.setStroke(defaultStroke);
    }

    /** A tick mark, to be stroked. */
    private static Shape checkShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(-size, 0);
        p.lineTo(-size * 0.3f, size * 0.7f);
        p.lineTo(size, -size * 0.7f);
        return p;
    }

    /** Two crossing strokes, to be stroked. */
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

    /** Rotates the chevron to point up, around the current origin. */
    private void paintUprightChevron(Graphics2D g2, float size) {
        AffineTransform save = g2.getTransform();
        g2.rotate(-Math.PI / 2);
        g2.fill(chevronShape(size));
        g2.setTransform(save);
    }

    private void paintEnemyFade(Graphics2D g2, EnemyFadeDraw fade) {
        Color color = withAlpha(colorFor(fade.palette()), Math.round((1f - fade.fadeProgress()) * 255f));
        AffineTransform save = g2.getTransform();
        g2.translate(fade.x(), fade.y());
        g2.rotate(fade.facingRadians());
        g2.setColor(color);
        float grownScale = fade.scale() + fade.growth();
        switch (fade.palette()) {
            case ENEMY_CIRCLE, ENEMY_GHOST -> g2.draw(circleShape(grownScale));
            case ENEMY_SQUARE -> {
                g2.draw(new Rectangle2D.Float(-grownScale, -fade.scale(), grownScale * 2, fade.scale() * 2));
                g2.draw(new Rectangle2D.Float(-fade.scale(), -grownScale, fade.scale() * 2, grownScale * 2));
            }
            case ENEMY_TRIANGLE -> {
                g2.draw(triangleShape(grownScale, false));
                g2.draw(triangleShape(grownScale, true));
            }
            case ENEMY_WARDEN -> g2.draw(starShape(8, grownScale, grownScale * 0.55f));
            case ENEMY_WARDEN_EGG -> g2.draw(wardenEggShape(grownScale));
            case ENEMY_MENDER -> g2.draw(crossShape(grownScale));
            default -> throw new IllegalStateException("Not an enemy palette: " + fade.palette());
        }
        g2.setTransform(save);
    }

    /** A small filled diamond for an active status effect. */
    private void paintStatusMarker(Graphics2D g2, StatusMarkerDraw marker) {
        AffineTransform save = g2.getTransform();
        g2.translate(marker.x(), marker.y());
        g2.setColor(colorFor(marker.palette()));
        g2.fill(diamondShape(marker.scale()));
        if (marker.hiddenCount() > 0) {
            g2.setColor(Color.WHITE);
            g2.setFont(MARKER_COUNT_FONT);
            g2.drawString("+" + marker.hiddenCount(), marker.scale() * 1.4f, marker.scale() * 0.8f);
        }
        g2.setTransform(save);
    }

    /**
     * A fading four-point sparkle where a critical hit landed, distinct from the five-point star.
     */
    private void paintCritSpark(Graphics2D g2, CritSparkDraw spark) {
        AffineTransform save = g2.getTransform();
        g2.translate(spark.x(), spark.y());
        g2.setColor(withAlpha(colorFor(spark.palette()), Math.round((1f - spark.fadeProgress()) * 255f)));
        float sparkScale = spark.scale() * (1f + spark.fadeProgress());
        g2.fill(starShape(4, sparkScale, sparkScale * 0.25f));
        g2.setTransform(save);
    }

    private void paintEnemyOverlay(Graphics2D g2, EnemyOverlayDraw overlay) {
        switch (overlay) {
            case EnemyRingDraw ring -> this.paintEnemyRing(g2, ring);
            case EffectPulseDraw pulse -> this.paintEffectPulse(g2, pulse);
            case TraitMarkerDraw marker -> this.paintTraitMarker(g2, marker);
            case IceCrystalDraw crystal -> this.paintIceCrystal(g2, crystal);
            case HexRuneDraw rune -> this.paintHexRune(g2, rune);
        }
    }

    /**
     * Translucent ice with near-white facets, so it reads as solid ice rather than a blue marker.
     */
    private void paintIceCrystal(Graphics2D g2, IceCrystalDraw crystal) {
        AffineTransform save = g2.getTransform();
        g2.translate(crystal.centerX(), crystal.centerY());
        Shape shape = crystalShape(crystal.scale());
        g2.setColor(withAlpha(colorFor(crystal.palette()), 130));
        g2.fill(shape);
        Stroke defaultStroke = g2.getStroke();
        g2.setColor(withAlpha(Color.WHITE, 210));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(shape);
        g2.setStroke(defaultStroke);
        g2.setTransform(save);
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
            case RIME -> {
                for (int i = 0; i < 3; i++) {
                    double angle = Math.PI / 3 * i;
                    float dx = (float) Math.cos(angle) * s;
                    float dy = (float) Math.sin(angle) * s;
                    path.moveTo(-dx, -dy);
                    path.lineTo(dx, dy);
                }
            }
            case ASH -> {
                path.moveTo(0, -s);
                path.quadTo(s, 0, s * 0.5f, s);
                path.lineTo(-s * 0.5f, s);
                path.quadTo(-s, 0, 0, -s);
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

    /** A hollow diamond for a permanent trait; filled diamonds are timed effects. */
    private void paintTraitMarker(Graphics2D g2, TraitMarkerDraw marker) {
        AffineTransform save = g2.getTransform();
        g2.translate(marker.x(), marker.y());
        g2.setColor(colorFor(marker.palette()));
        g2.draw(diamondShape(marker.scale()));
        g2.setTransform(save);
    }

    /** A thin ring around an enemy: a shield bubble or aura reach. */
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

    /**
     * A ring growing from nothing (gain, cast, spawn) or shrinking to nothing (loss), fading as it
     * goes. {@code pulse.radius()} is its full size.
     */
    private void paintEffectPulse(Graphics2D g2, EffectPulseDraw pulse) {
        float radius = pulse.direction() == PulseDirection.OUTWARD
                ? pulse.radius() * pulse.progress()
                : pulse.radius() * (1f - pulse.progress());
        if (radius <= 0) {
            return;
        }
        Stroke defaultStroke = g2.getStroke();
        g2.setColor(withAlpha(colorFor(pulse.palette()), Math.round((1f - pulse.progress()) * 255)));
        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(new Ellipse2D.Float(pulse.centerX() - radius, pulse.centerY() - radius, radius * 2, radius * 2));
        g2.setStroke(defaultStroke);
    }

    /**
     * Renders one tower's symbol as a toolbar icon, with the same {@link #towerBodyShape} as the
     * board so the two cannot drift.
     * <p>
     * One solid colour and no turret head, since a two-tone glyph smudges at icon size. Transparent
     * background; the control paints its own face.
     */
    public BufferedImage renderTowerIcon(Palette palette, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(size / 2.0, size / 2.0);
        g2.setColor(colorFor(palette));
        g2.fill(towerBodyShape(palette, size * ICON_BODY_SIZE_FRACTION));
        g2.dispose();
        return image;
    }

    private void paintTowerSprite(Graphics2D g2, TowerSpriteDraw sprite, int scale) {
        if (sprite.selected()) {
            float r = sprite.rangeReal();
            g2.setColor(Color.PINK);
            g2.draw(new Ellipse2D.Float(sprite.centerX() - r, sprite.centerY() - r, r * 2, r * 2));
        }
        AffineTransform save = g2.getTransform();
        g2.translate(sprite.centerX(), sprite.centerY());
        float bodySize = scale * TOWER_BODY_SIZE_FRACTION;
        if (sprite.transcendent()) {
            this.paintTranscendentHalo(g2, sprite.haloTurn(), bodySize);
        }
        this.paintEnchantHalo(g2, sprite.enchantPulse(), bodySize);
        this.paintTowerBody(g2, sprite.palette(), bodySize);
        this.paintSlotPips(g2, sprite.slotMarks(), sprite.transcendent(), bodySize);
        this.paintSlotReadyChevrons(g2, sprite.slotMarks(), bodySize);
        this.paintRankPips(g2, sprite.rank(), bodySize);
        if (sprite.rankUpProgress() >= 0f) {
            this.paintRankUpGlow(g2, sprite.rankUpProgress(), bodySize);
        }
        g2.setTransform(save);
    }

    /**
     * A pulsing circle around a tower with a {@code SPECIAL} upgrade, the same on every body shape.
     */
    /** One small diamond per rank above Recruit, stacked upward beside the body. */
    private void paintRankPips(Graphics2D g2, int rank, float bodySize) {
        if (rank == 0) {
            return;
        }
        g2.setColor(colorFor(Palette.TOWER_RANK));
        float radius = bodySize * RANK_PIP_RADIUS_FRACTION;
        float spacing = bodySize * RANK_PIP_SPACING_FRACTION;
        float x = -bodySize * RANK_PIP_COLUMN_OFFSET_FRACTION;
        for (int i = 0; i < rank; i++) {
            AffineTransform save = g2.getTransform();
            g2.translate(x, (rank - 1) * spacing / 2f - i * spacing);
            g2.fill(diamondShape(radius * 1.3f));
            g2.setTransform(save);
        }
    }

    /** A ring that widens and fades as {@code progress} runs from 0 to 1. */
    private void paintRankUpGlow(Graphics2D g2, float progress, float bodySize) {
        float radius = bodySize * (0.7f + (RANK_UP_GLOW_MAX_RADIUS_FRACTION - 0.7f) * progress);
        Stroke previousStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(RANK_UP_GLOW_STROKE_WIDTH));
        g2.setColor(withAlpha(colorFor(Palette.TOWER_RANK), Math.round(230 * (1f - progress))));
        g2.draw(new Ellipse2D.Float(-radius, -radius, radius * 2, radius * 2));
        g2.setStroke(previousStroke);
    }

    /** A ring of gold dashes, turned {@code turn} of the way round. */
    private void paintTranscendentHalo(Graphics2D g2, float turn, float bodySize) {
        float radius = bodySize * TRANSCENDENT_HALO_RADIUS_FRACTION;
        Stroke previousStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(TRANSCENDENT_HALO_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(withAlpha(colorFor(Palette.TOWER_TRANSCENDENT), TRANSCENDENT_HALO_ALPHA));
        double dash = 360.0 / TRANSCENDENT_HALO_DASHES;
        for (int i = 0; i < TRANSCENDENT_HALO_DASHES; i++) {
            g2.draw(new Arc2D.Float(-radius, -radius, radius * 2, radius * 2,
                    (float) (turn * 360.0 + i * dash), (float) (dash * 0.55), Arc2D.OPEN));
        }
        g2.setStroke(previousStroke);
    }

    private void paintEnchantHalo(Graphics2D g2, float pulse, float bodySize) {
        if (pulse <= 0f) {
            return;
        }
        float radius = bodySize * ENCHANT_HALO_RADIUS_FRACTION;
        Stroke previousStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(ENCHANT_HALO_STROKE_WIDTH));
        g2.setColor(withAlpha(colorFor(Palette.TOWER_UPGRADE_SPECIAL), Math.round(80 + pulse * 150)));
        g2.draw(new Ellipse2D.Float(-radius, -radius, radius * 2, radius * 2));
        g2.setStroke(previousStroke);
    }

    /**
     * Pips below a tower, one per owned node, grouped by slot with a gap between groups. A
     * Transcendent tower's last base pip, Transcendent's own, is a gold diamond: the head's pips
     * are gold too, so the shape tells them apart.
     */
    private void paintSlotPips(Graphics2D g2, List<SlotMarkDraw> marks, boolean transcendent, float bodySize) {
        List<Color> pipColors = new ArrayList<>();
        int crownIndex = -1;
        for (SlotMarkDraw mark : marks) {
            if (mark.level() == 0) {
                continue;
            }
            if (!pipColors.isEmpty()) {
                pipColors.add(null);
            }
            Color color = colorFor(mark.palette());
            for (int i = 0; i < mark.level(); i++) {
                pipColors.add(color);
            }
            if (transcendent && mark.palette() == Palette.TOWER_UPGRADE_BASE) {
                crownIndex = pipColors.size() - 1;
                pipColors.set(crownIndex, colorFor(Palette.TOWER_TRANSCENDENT));
            }
        }
        if (pipColors.isEmpty()) {
            return;
        }
        float pipRadius = bodySize * SLOT_PIP_RADIUS_FRACTION;
        float spacing = bodySize * SLOT_PIP_SPACING_FRACTION;
        float y = bodySize * SLOT_PIP_ROW_OFFSET_FRACTION;
        float x = -(pipColors.size() - 1) * spacing / 2f;
        for (int i = 0; i < pipColors.size(); i++) {
            Color color = pipColors.get(i);
            if (color != null) {
                g2.setColor(color);
                if (i == crownIndex) {
                    AffineTransform save = g2.getTransform();
                    g2.translate(x, y);
                    g2.fill(diamondShape(pipRadius * TRANSCENDENT_PIP_SCALE));
                    g2.setTransform(save);
                } else {
                    g2.fill(new Ellipse2D.Float(x - pipRadius, y - pipRadius, pipRadius * 2, pipRadius * 2));
                }
            }
            x += spacing;
        }
    }

    private void paintSlotReadyChevrons(Graphics2D g2, List<SlotMarkDraw> marks, float bodySize) {
        List<Palette> ready = marks.stream().filter(SlotMarkDraw::ready).map(SlotMarkDraw::palette).toList();
        if (ready.isEmpty()) {
            return;
        }
        float size = bodySize * SLOT_CHEVRON_SIZE_FRACTION;
        float spacing = bodySize * SLOT_CHEVRON_SPACING_FRACTION;
        float y = -bodySize * SLOT_CHEVRON_ROW_OFFSET_FRACTION;
        float x = -(ready.size() - 1) * spacing / 2f;
        Shape upChevron = AffineTransform.getRotateInstance(-Math.PI / 2).createTransformedShape(chevronShape(size));
        AffineTransform save = g2.getTransform();
        for (Palette palette : ready) {
            g2.setColor(colorFor(palette));
            g2.translate(x, y);
            g2.fill(upChevron);
            g2.setTransform(save);
            x += spacing;
        }
    }

    /** Draws a tower body at the origin; the caller has translated to the tower's centre. */
    private void paintTowerBody(Graphics2D g2, Palette palette, float size) {
        Shape shape = towerBodyShape(palette, size);
        Color color = colorFor(palette);
        g2.setColor(withAlpha(color, 130));
        g2.fill(shape);
        g2.setColor(color);
        g2.draw(shape);
    }

    private void paintTurretHead(Graphics2D g2, TurretHeadDraw head, int scale) {
        AffineTransform save = g2.getTransform();
        g2.translate(head.centerX(), head.centerY());
        g2.rotate(head.headingRadians());
        float size = scale * TOWER_HEAD_SIZE_FRACTION * head.scale();
        if (head.palette() == Palette.TOWER_SONAR_BODY) {
            this.paintSonarSweep(g2, size);
        } else {
            this.paintHeadShape(g2, head.palette(), size);
        }
        g2.setTransform(save);
    }

    /**
     * The sonar head: sub-wedges of decreasing alpha behind a bright edge, approximating the
     * angular gradient Java2D lacks. Translucent, so the body shows through.
     */
    private void paintSonarSweep(Graphics2D g2, float size) {
        Color color = colorFor(Palette.TOWER_SONAR_BODY);
        float radius = size * SONAR_RADIUS_FACTOR;
        float stepDegrees = SONAR_ARC_DEGREES / SONAR_TRAIL_STEPS;

        for (int step = 0; step < SONAR_TRAIL_STEPS; step++) {
            int alpha = Math.round(SONAR_TRAIL_ALPHA * (SONAR_TRAIL_STEPS - step) / (float) SONAR_TRAIL_STEPS);
            g2.setColor(withAlpha(color, alpha));
            g2.fill(sonarWedge(radius, -(step + 1) * stepDegrees, stepDegrees));
        }

        Stroke previous = g2.getStroke();
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Float(0, 0, radius, 0));
        g2.setStroke(previous);
    }

    /** Draws a head at the origin; the caller has translated and rotated. */
    private void paintHeadShape(Graphics2D g2, Palette palette, float size) {
        Shape shape = turretHeadShape(palette, size);
        Color color = colorFor(palette);
        g2.setColor(withAlpha(color, 200));
        g2.fill(shape);
        g2.setColor(color);
        g2.draw(shape);
    }

    private void paintTowerEffect(Graphics2D g2, TowerEffectDraw effect) {
        switch (effect) {
            case BeamDraw beam -> this.paintBeam(g2, beam);
            case SplashDraw splash ->
                    this.paintFilledCircle(g2, splash.palette(), splash.centerX(), splash.centerY(), splash.radius());
            case PulseDraw pulse ->
                    this.paintFilledCircle(g2, pulse.palette(), pulse.centerX(), pulse.centerY(), pulse.radius());
            case RingDraw ring -> this.paintRing(g2, ring);
            case FlashDraw flash -> this.paintFlash(g2, flash);
            case ConeDraw cone -> this.paintCone(g2, cone);
            case NestDraw nest -> this.paintNest(g2, nest);
            case TowerStatusDraw status -> this.paintStatusMarker(g2,
                    new StatusMarkerDraw(status.palette(), status.x(), status.y(), status.scale()));
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
        g2.setColor(withAlpha(colorFor(cone.palette()), Math.round(CINDER_CONE_ALPHA * 255)));
        Area band = new Area(wedge(frontRadius, cone.halfWidthRadians()));
        if (trailingRadius > 0) {
            band.subtract(new Area(wedge(trailingRadius, cone.halfWidthRadians())));
        }
        g2.fill(band);
        g2.setTransform(save);
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

    private void paintShell(Graphics2D g2, CannonballDraw shell) {
        float radius = PROJECTILE_SIZE * shell.size();
        if (shell.tailX() != shell.x() || shell.tailY() != shell.y()) {
            Stroke defaultStroke = g2.getStroke();
            g2.setColor(scaleAlpha(colorFor(shell.palette()), SHELL_STREAK_ALPHA));
            g2.setStroke(new BasicStroke(radius, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Float(shell.tailX(), shell.tailY(), shell.x(), shell.y()));
            g2.setStroke(defaultStroke);
        }
        this.paintFilledCircle(g2, shell.palette(), shell.x(), shell.y(), radius);
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
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, smoke.alpha()))));
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
