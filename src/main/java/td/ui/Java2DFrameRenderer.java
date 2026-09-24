package td.ui;

import td.ui.render.AuraDraw;
import td.ui.render.BeamDraw;
import td.ui.render.CannonballDraw;
import td.ui.render.CellDraw;
import td.ui.render.ConeDraw;
import td.ui.render.CritSparkDraw;
import td.ui.render.EffectPulseDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.EnemyOverlayDraw;
import td.ui.render.EnemyRingDraw;
import td.ui.render.IceCrystalDraw;
import td.ui.render.MissileDraw;
import td.ui.render.Palette;
import td.ui.render.PathMarkerBrightness;
import td.ui.render.PathMarkerDraw;
import td.ui.render.PathMarkerShape;
import td.ui.render.ProjectileDraw;
import td.ui.render.PulseDirection;
import td.ui.render.PulseDraw;
import td.ui.render.RankBadge;
import td.ui.render.RenderFrame;
import td.ui.render.SlotMarkDraw;
import td.ui.render.SplashDraw;
import td.ui.render.StatusMarkerDraw;
import td.ui.render.TowerEffectDraw;
import td.ui.render.TowerSpriteDraw;
import td.ui.render.TowerStatusDraw;
import td.ui.render.TraitMarkerDraw;
import td.ui.render.TurretHeadDraw;
import td.wave.PathColor;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
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
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a {@link RenderFrame} into {@code Graphics2D} calls: the only class that does, and the only
 * owner of colours, shapes and strokes for board content, all keyed off {@link Palette}.
 * <p>
 * {@code Panel*} components use AWT for layout and previews but never paint board content;
 * {@link #paintEnemies} and {@link #renderTowerIcon} exist for them.
 */
public final class Java2DFrameRenderer {

    private static final Color CELL_OK = Color.GRAY;
    private static final Color CELL_NOK = Color.RED;
    private static final Color CELL_RANGE = new Color(250, 250, 210, 150);
    private static final Color BOARD_BACKGROUND = Color.BLACK;
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
    /** Cone alpha when fired, before {@link #paintCone} fades it. */
    private static final float CINDER_CONE_BASE_ALPHA = 0.55f;

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
            case TOWER_SPLASH_BODY -> ringShape(size, 0.55f);
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
            case TOWER_SNIPER_BODY -> new Rectangle2D.Float(0, -size * 0.22f, size * 1.3f, size * 0.44f);
            case TOWER_SPLASH_BODY -> new Rectangle2D.Float(0, -size * 0.42f, size * 0.95f, size * 0.84f);
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

    private static Color colorFor(Palette palette) {
        return switch (palette) {
            case ENEMY_CIRCLE -> Color.CYAN;
            case ENEMY_GHOST -> Color.LIGHT_GRAY;
            case ENEMY_SQUARE -> Color.PINK;
            case ENEMY_TRIANGLE -> Color.YELLOW;
            case ENEMY_WARDEN, ENEMY_WARDEN_EGG -> new Color(139, 0, 0);
            case ENEMY_MENDER -> new Color(120, 220, 150);
            case TOWER_SNIPER_BODY -> Color.GREEN;
            case TOWER_SPLASH_BODY -> Color.RED;
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
            case TOWER_SNIPER_BEAM -> Color.GREEN;
            case TOWER_SPLASH_BEAM -> Color.RED;
            case TOWER_SPLASH_LINE, TOWER_SPLASH_FILL -> withAlpha(Color.RED, 80);
            case TOWER_SONAR_BEAM -> Color.YELLOW;
            case TOWER_PULSE_RING -> withAlpha(Color.ORANGE, 80);
            case TOWER_CINDER_CONE -> new Color(255, 90, 30);
            case PROJECTILE_CANNONBALL -> new Color(139, 90, 43);
            case PROJECTILE_MISSILE -> new Color(80, 180, 255);
            case STATUS_MARKER_SLOW -> new Color(120, 120, 255);
            case STATUS_MARKER_BURN -> new Color(255, 120, 40);
            case STATUS_MARKER_FREEZE -> new Color(150, 220, 255);
            case STATUS_MARKER_SHIELD -> new Color(220, 220, 100);
            case STATUS_MARKER_INVISIBLE -> new Color(180, 180, 180);
            case STATUS_MARKER_HEAL -> new Color(120, 220, 140);
            case STATUS_MARKER_OVERFLOW -> Color.WHITE;
            case FREEZE_CRYSTAL -> new Color(220, 245, 255);
            case CRIT_SPARK -> Color.WHITE;
            case SPAWN_BURST -> Color.WHITE;
            case RANK_BADGE_CHEVRON -> Color.WHITE;
            case RANK_BADGE_ELITE -> new Color(230, 190, 60);
            case RANK_BADGE_BOSS -> new Color(210, 210, 220);
            case TRAIT_MARKER_PERCENT_RESIST -> new Color(180, 150, 255);
            case TRAIT_MARKER_PHYSICAL_RESIST -> new Color(230, 150, 70);
            case TRAIT_MARKER_MAGIC_RESIST -> new Color(90, 190, 255);
            case TRAIT_MARKER_FLAT_RESIST -> new Color(140, 110, 200);
            case TRAIT_MARKER_CRITICAL_IMMUNE -> new Color(255, 210, 130);
            case TRAIT_MARKER_HURT_SPEED -> new Color(255, 140, 140);
            case TRAIT_MARKER_BURN_IMMUNE -> new Color(255, 150, 90);
            case TRAIT_MARKER_FREEZE_IMMUNE -> new Color(170, 225, 255);
            case TRAIT_MARKER_EFFECT_RESIST -> new Color(200, 200, 160);
            case TRAIT_MARKER_FREEZE_DIMINISHING -> new Color(120, 180, 220);
            case TRAIT_MARKER_OVERFLOW -> Color.LIGHT_GRAY;
            case DISRUPTION -> new Color(235, 90, 200);
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

        for (PathMarkerDraw marker : frame.pathMarkers()) {
            this.paintPathMarker(g2, marker);
        }
        for (CellDraw cell : frame.cells()) {
            this.paintCell(g2, cell, frame);
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
        if (body.badge() == RankBadge.NONE) {
            return;
        }
        float badgeSize = body.scale() * RANK_BADGE_SIZE_FRACTION;
        AffineTransform save = g2.getTransform();
        g2.translate(body.x(), body.y() - body.scale() * RANK_BADGE_OFFSET_FRACTION);
        switch (body.badge()) {
            case ONE_CHEVRON -> {
                g2.setColor(colorFor(Palette.RANK_BADGE_CHEVRON));
                this.paintUprightChevron(g2, badgeSize);
            }
            case TWO_CHEVRON -> {
                g2.setColor(colorFor(Palette.RANK_BADGE_CHEVRON));
                float spacing = body.scale() * RANK_BADGE_CHEVRON_SPACING_FRACTION;
                g2.translate(0, -spacing / 2);
                this.paintUprightChevron(g2, badgeSize);
                g2.translate(0, spacing);
                this.paintUprightChevron(g2, badgeSize);
            }
            case STAR -> {
                g2.setColor(colorFor(Palette.RANK_BADGE_ELITE));
                g2.fill(starShape(5, badgeSize, badgeSize * 0.45f));
            }
            case SKULL -> {
                g2.setColor(colorFor(Palette.RANK_BADGE_BOSS));
                g2.fill(skullShape(badgeSize));
            }
            case NONE -> {
            }
        }
        g2.setTransform(save);
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
        this.paintEnchantHalo(g2, sprite.enchantPulse(), bodySize);
        this.paintTowerBody(g2, sprite.palette(), bodySize);
        this.paintSlotPips(g2, sprite.slotMarks(), bodySize);
        this.paintSlotReadyChevrons(g2, sprite.slotMarks(), bodySize);
        g2.setTransform(save);
    }

    /**
     * A pulsing circle around a tower with a {@code SPECIAL} upgrade, the same on every body shape.
     */
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

    /** Pips below a tower, one per owned node, grouped by slot with a gap between groups. */
    private void paintSlotPips(Graphics2D g2, List<SlotMarkDraw> marks, float bodySize) {
        List<Color> pipColors = new ArrayList<>();
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
        }
        if (pipColors.isEmpty()) {
            return;
        }
        float pipRadius = bodySize * SLOT_PIP_RADIUS_FRACTION;
        float spacing = bodySize * SLOT_PIP_SPACING_FRACTION;
        float y = bodySize * SLOT_PIP_ROW_OFFSET_FRACTION;
        float x = -(pipColors.size() - 1) * spacing / 2f;
        for (Color color : pipColors) {
            if (color != null) {
                g2.setColor(color);
                g2.fill(new Ellipse2D.Float(x - pipRadius, y - pipRadius, pipRadius * 2, pipRadius * 2));
            }
            x += spacing;
        }
    }

    /** One chevron above a tower per slot with an affordable, ungated node. */
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
            case AuraDraw aura -> this.paintAura(g2, aura);
            case ConeDraw cone -> this.paintCone(g2, cone);
            case TowerStatusDraw status -> this.paintStatusMarker(g2,
                    new StatusMarkerDraw(status.palette(), status.x(), status.y(), status.scale()));
        }
    }


    /**
     * A wedge on the cone's heading that grows outward and fades as {@code cone.progress()} goes
     * from 0 to 1.
     */
    private void paintCone(Graphics2D g2, ConeDraw cone) {
        float currentRadius = cone.maxRadius() * cone.progress();
        if (currentRadius <= 0) {
            return;
        }
        float alpha = CINDER_CONE_BASE_ALPHA * (1f - cone.progress());
        AffineTransform save = g2.getTransform();
        g2.translate(cone.originX(), cone.originY());
        g2.rotate(cone.headingRadians());
        float halfWidthDegrees = (float) Math.toDegrees(cone.halfWidthRadians());
        g2.setColor(withAlpha(colorFor(cone.palette()), Math.round(alpha * 255)));
        g2.fill(new Arc2D.Float(-currentRadius, -currentRadius, currentRadius * 2, currentRadius * 2,
                -halfWidthDegrees, halfWidthDegrees * 2, Arc2D.PIE));
        g2.setTransform(save);
    }

    private void paintAura(Graphics2D g2, AuraDraw aura) {
        if (aura.radius() <= 0) {
            return;
        }
        Stroke defaultStroke = g2.getStroke();
        g2.setColor(withAlpha(colorFor(aura.palette()), Math.round(aura.alpha() * 255)));
        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(new Ellipse2D.Float(aura.centerX() - aura.radius(), aura.centerY() - aura.radius(), aura.radius() * 2, aura.radius() * 2));
        g2.setStroke(defaultStroke);
    }

    private void paintBeam(Graphics2D g2, BeamDraw beam) {
        Stroke defaultStroke = g2.getStroke();
        g2.setColor(colorFor(beam.palette()));
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
                    this.paintFilledCircle(g2, shell.palette(), shell.x(), shell.y(), PROJECTILE_SIZE);
            case MissileDraw missile -> this.paintMissile(g2, missile);
        }
    }

    private void paintMissile(Graphics2D g2, MissileDraw missile) {
        AffineTransform save = g2.getTransform();
        g2.translate(missile.x(), missile.y());
        g2.rotate(missile.facingRadians());
        g2.setColor(colorFor(missile.palette()));
        g2.fill(headArrowShape(PROJECTILE_SIZE));
        g2.setTransform(save);
    }
}
