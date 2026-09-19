package td.ui;

import td.ui.render.AuraDraw;
import td.ui.render.BeamDraw;
import td.ui.render.CannonballDraw;
import td.ui.render.CellDraw;
import td.ui.render.ConeDraw;
import td.ui.render.CritSparkDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.MissileDraw;
import td.ui.render.Palette;
import td.ui.render.PathMarkerBrightness;
import td.ui.render.PathMarkerDraw;
import td.ui.render.PathMarkerShape;
import td.ui.render.ProjectileDraw;
import td.ui.render.PulseDraw;
import td.ui.render.RenderFrame;
import td.ui.render.SplashDraw;
import td.ui.render.StatusMarkerDraw;
import td.ui.render.TowerEffectDraw;
import td.ui.render.TowerSpriteDraw;
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
import java.util.List;

/**
 * Turns an AWT-free {@link RenderFrame} into {@code Graphics2D} calls - the only class that
 * does, and the only owner of a colour, shape or stroke choice for board content. Every one
 * of those is keyed off {@link Palette} rather than off any domain type, so a future backend
 * needs to understand the draw commands and nothing else.
 * <p>
 * The {@code Panel*} Swing components in this package import {@code java.awt} too, for layout
 * and for their own small previews; what they must not do is paint board content themselves.
 * {@link #paintEnemies} and {@link #renderTowerIcon} exist so they don't have to.
 */
public final class Java2DFrameRenderer {

    private static final Color CELL_OK = Color.GRAY;
    private static final Color CELL_NOK = Color.RED;
    private static final Color CELL_RANGE = new Color(250, 250, 210, 150);
    private static final Color BOARD_BACKGROUND = Color.BLACK;
    /**
     * How much of a cell a tower body fills - leaves a small margin, same spirit as enemy bodies.
     */
    private static final float TOWER_BODY_SIZE_FRACTION = 0.42f;

    // --- cells ---------------------------------------------------------
    /**
     * Half the icon, so the toolbar reads as a row of small glyphs rather than filled chips.
     */
    private static final float ICON_BODY_SIZE_FRACTION = 0.25f;
    /**
     * How far outside the body the specialization ring sits - every body shape's own extent stays within {@code bodySize}.
     */
    private static final float UPGRADE_ACCENT_RADIUS_FRACTION = 1.3f;
    private static final float UPGRADE_ACCENT_STROKE_WIDTH = 2.0f;
    /**
     * How big a turret head is drawn relative to a cell - smaller than the base it sits on.
     */
    private static final float TOWER_HEAD_SIZE_FRACTION = 0.24f;

    // --- path markers ---------------------------------------------------------
    /**
     * How wide the sonar wedge opens, in degrees.
     */
    private static final float SONAR_ARC_DEGREES = 62f;
    /**
     * The wedge's radius as a multiple of the nominal head size. Not a free choice: with
     * {@link #TOWER_HEAD_SIZE_FRACTION} at 0.24 of a cell this works out at 0.44 of a cell,
     * which keeps the wedge just inside the tile's half-width at any board scale.
     */
    private static final float SONAR_RADIUS_FACTOR = 1.85f;

    // --- enemies ---------------------------------------------------------
    /**
     * How many bands the trail fades through, and how bright the band at the leading edge is.
     */
    private static final int SONAR_TRAIL_STEPS = 4;
    private static final int SONAR_TRAIL_ALPHA = 150;
    /**
     * How big a projectile is drawn - smaller than a tower's own head, since it's the shot, not the gun.
     */
    private static final float PROJECTILE_SIZE = 5f;

    private static Shape markerShape(PathMarkerShape shape, float size) {
        return switch (shape) {
            case DOT -> circleShape(size);
            case CHEVRON -> {
                GeneralPath p = new GeneralPath();
                p.moveTo(-size, -size);
                p.lineTo(size, 0);
                p.lineTo(-size, size);
                p.lineTo(-size * 0.4f, 0);
                p.closePath();
                yield p;
            }
        };
    }

    private static Shape enemyShape(Palette palette, float scale) {
        return switch (palette) {
            case ENEMY_CIRCLE, ENEMY_GHOST -> circleShape(scale);
            case ENEMY_SQUARE -> new Rectangle2D.Float(-scale, -scale, scale * 2, scale * 2);
            case ENEMY_TRIANGLE -> triangleShape(scale, true);
            case ENEMY_WARDEN -> starShape(8, scale, scale * 0.55f);
            case ENEMY_WARDEN_EGG -> wardenEggShape(scale);
            default -> throw new IllegalStateException("Not an enemy palette: " + palette);
        };
    }

    /**
     * A spiked, egg-shaped silhouette for the Warden boss's own egg stage - the same
     * {@link #starShape} language as {@code ENEMY_WARDEN}'s adult body (six points rather than
     * eight, and slightly softer spikes, reading as a smaller/younger sibling of the same
     * creature) stretched taller and narrower into an ovoid instead of {@code ENEMY_WARDEN}'s
     * flat star, so it reads as an egg belonging to that boss rather than a second copy of it.
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

    // --- status markers ---------------------------------------------------------

    /**
     * One flat symbol per tower, each naming what the tower does rather than decorating it:
     * a triangle, a circle, a spiral, a star, a pulsar, a diamond, a kite and a flame. Every
     * one is a single closed {@link Shape} so they all go through the same fill-then-outline
     * paint, at any size.
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

    // --- towers ---------------------------------------------------------

    /**
     * A rotated square - a heavy, armoured silhouette for the artillery-style tower.
     */
    private static Shape diamondShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(0, -size);
        p.lineTo(size, 0);
        p.lineTo(0, size);
        p.lineTo(-size, 0);
        p.closePath();
        return p;
    }

    /**
     * A concave arrowhead - a guided-munition silhouette for the homing-missile tower.
     */
    private static Shape kiteShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(size, 0);
        p.lineTo(-size * 0.6f, -size * 0.7f);
        p.lineTo(-size * 0.2f, 0);
        p.lineTo(-size * 0.6f, size * 0.7f);
        p.closePath();
        return p;
    }

    /**
     * A teardrop silhouette for the flame-cone tower.
     */
    private static Shape flameShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(0, -size);
        p.curveTo(size * 0.9f, -size * 0.2f, size * 0.6f, size * 0.5f, 0, size);
        p.curveTo(-size * 0.6f, size * 0.5f, -size * 0.9f, -size * 0.2f, 0, -size);
        p.closePath();
        return p;
    }

    /**
     * An annulus - {@code innerFraction} of {@code size} is cut out of the middle.
     */
    private static Shape ringShape(float size, float innerFraction) {
        Area ring = new Area(circleShape(size));
        ring.subtract(new Area(circleShape(size * innerFraction)));
        return ring;
    }

    /**
     * An Archimedean spiral, stroked into a closed ribbon so it can be filled like every other
     * body shape - an open path would fill as a blob rather than as a line.
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

    /**
     * A bright core inside a detached ring - the passive buff tower's "pulsar".
     */
    private static Shape pulsarShape(float size) {
        Area pulsar = new Area(ringShape(size, 0.72f));
        pulsar.add(new Area(circleShape(size * 0.36f)));
        return pulsar;
    }

    /**
     * A classic N-pointed star polygon - {@code points}/radii let a future tower reuse this at a different count.
     */
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
     * The two aiming towers' heads (one/two) are authored pointing along {@code +X} (heading
     * {@code 0}) so {@link Graphics2D#rotate(double)} alone aims them correctly - see
     * {@link TurretHeadDraw}'s doc comment. The two spinning towers (three/four) have no such
     * concern - any starting phase looks equally valid while continuously spinning - but their
     * heads still need to reach *past* their own (larger) base's silhouette to actually read as
     * moving: a same-shape-family head entirely contained within the base's footprint turned
     * out to be visually indistinguishable from standing still, since both are the same hue.
     * TOWER_FOUR gets a small star orbiting off-centre (a "moon"), echoing its base's own shape
     * family while clearing the base's edge. TOWER_AURA's head ignores rotation entirely (it
     * pulses via {@link TurretHeadDraw#scale()} instead) so a plain circle needs no special
     * orientation.
     * <p>
     * TOWER_THREE is deliberately absent: its head is the sonar scan that decides what the tower
     * shoots, and it needs more than one shape to read as a sweep, so it has its own
     * {@link #paintSonarSweep} and reaching this method for it is a bug.
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

    /**
     * A plain forward-pointing arrowhead, authored along {@code +X} like every other aiming
     * head - a simple facing indicator for the three new towers. {@code CinderTower}'s actual
     * cone is a separate {@code TowerEffectDraw}, not part of this shape (see `TODO.md`).
     */
    private static Shape headArrowShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(size * 1.3f, 0);
        p.lineTo(0, -size * 0.5f);
        p.lineTo(0, size * 0.5f);
        p.closePath();
        return p;
    }

    /**
     * One band of the sweep's trail. Angles run counterclockwise in {@link Arc2D}'s y-up
     * convention, so the trail - which lies clockwise of the beam, the way it has just come -
     * is at negative angles.
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
            // Same exact colour as the adult Warden, not just a related tint - it's the same
            // creature's egg, and the two never appear on screen at once (the egg only spawns
            // after the Warden that laid it has died).
            case ENEMY_WARDEN, ENEMY_WARDEN_EGG -> new Color(139, 0, 0);
            case TOWER_SNIPER_BODY -> Color.GREEN;
            case TOWER_SPLASH_BODY -> Color.RED;
            case TOWER_SONAR_BODY -> Color.YELLOW;
            case TOWER_PULSE_BODY -> Color.ORANGE;
            case TOWER_AURA_BODY -> Color.WHITE;
            case TOWER_MORTAR_BODY -> new Color(139, 90, 43);
            case TOWER_SEEKER_BODY -> new Color(80, 180, 255);
            case TOWER_CINDER_BODY -> new Color(255, 90, 30);
            case TOWER_AURA_RING -> Color.WHITE;
            case TOWER_UPGRADE_PATH_A -> new Color(255, 200, 60);
            case TOWER_UPGRADE_PATH_B -> new Color(100, 180, 255);
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
            case STATUS_MARKER_OVERFLOW -> Color.WHITE;
            case CRIT_SPARK -> Color.WHITE;
        };
    }

    private static Color withAlpha(Color base, int alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.min(255, Math.max(alpha, 0)));
    }

    private static Color healthColor(Color base, float healthFraction) {
        return withAlpha(base, Math.round(healthFraction * 255));
    }

    public void paint(Graphics2D g2, RenderFrame frame) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        // Board art is all vector now, so nothing here goes through drawImage and this hint
        // changes no pixel on the board today. It is kept so a Graphics2D handed to this
        // renderer is configured consistently with SharpImageIcon, which still needs it:
        // without it, drawImage falls back to nearest-neighbor under a non-1:1 transform
        // (e.g. Swing's per-monitor HiDPI scale on Windows) and looks blocky.
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        // GameBoard.paint() overrides Swing's own painting wholesale (no super.paint() call),
        // so this fill is also the only thing clearing the previous frame - not just cosmetic.
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

    /**
     * The dim-trail-vs-bright-chevron alpha the two path-marker layers have always had - now
     * combined with a path's own {@link PathColor} rather than baked into one fixed hue.
     */
    private static int pathMarkerAlpha(PathMarkerBrightness brightness) {
        return switch (brightness) {
            case STATIC -> 40;
            case MOVING -> 100;
        };
    }

    /**
     * Draws just enemy bodies/fades with no board around them - used by the wave-preview strip.
     */
    public void paintEnemies(Graphics2D g2, List<EnemyDraw> enemies) {
        for (EnemyDraw enemy : enemies) {
            this.paintEnemy(g2, enemy);
        }
    }

    /**
     * Paints a set of path markers directly, the same reuse-outside-the-board-frame shape
     * {@link #paintEnemies} already gives {@code PanelEnemy} - used by {@code PathWaveRow}'s
     * own small per-path color swatch, so "which lane is this" reads in the exact dot/chevron
     * language the board itself draws its path trail with.
     */
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
        g2.setColor(color);
        g2.draw(shape);
        g2.setColor(healthColor(color, body.healthFraction()));
        g2.fill(shape);
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
            default -> throw new IllegalStateException("Not an enemy palette: " + fade.palette());
        }
        g2.setTransform(save);
    }

    /**
     * A small filled diamond naming an active status effect - deliberately not a shape rotated or fill-then-outline like a body, since it's already a small glyph at a fixed pose.
     */
    private void paintStatusMarker(Graphics2D g2, StatusMarkerDraw marker) {
        AffineTransform save = g2.getTransform();
        g2.translate(marker.x(), marker.y());
        g2.setColor(colorFor(marker.palette()));
        g2.fill(diamondShape(marker.scale()));
        g2.setTransform(save);
    }

    /**
     * A brief, fading four-point sparkle at the point a critical hit landed - a sharper star
     * than {@link Palette#TOWER_PULSE_BODY}'s five-point one, so the two don't read as the
     * same glyph at a glance. Grows slightly and fades out over its duration, the same "small
     * timed animation" shape {@link #paintEnemyFade} uses for a death fade.
     */
    private void paintCritSpark(Graphics2D g2, CritSparkDraw spark) {
        AffineTransform save = g2.getTransform();
        g2.translate(spark.x(), spark.y());
        g2.setColor(withAlpha(colorFor(spark.palette()), Math.round((1f - spark.fadeProgress()) * 255f)));
        float sparkScale = spark.scale() * (1f + spark.fadeProgress());
        g2.fill(starShape(4, sparkScale, sparkScale * 0.25f));
        g2.setTransform(save);
    }

    /**
     * Rasterizes one tower's symbol into a standalone icon - used for the toolbar's
     * {@code JToggleButton} icons, which need a Swing {@code Icon} rather than a live paint.
     * It uses the same {@link #towerBodyShape} the board does, so a tower's icon cannot drift
     * from the symbol it shows once placed.
     * <p>
     * Unlike the board, the symbol is painted as one flat colour rather than a translucent
     * fill under a brighter outline: at this size a two-tone shape muddies into a smudge, and
     * a single solid glyph is what actually reads. The turret head is left off for the same
     * reason - on the board it carries information, where an aiming tower is pointing, but an
     * icon has no target and no animation clock.
     * <p>
     * The glyph is drawn on a transparent background: the control it sits on paints its own
     * dark face (see {@link Hud}), so the icon does not have to carry a scrap of board with it
     * to stay legible.
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
        this.paintTowerBody(g2, sprite.palette(), bodySize);
        sprite.accent().ifPresent(accent -> this.paintUpgradeAccent(g2, accent, bodySize));
        g2.setTransform(save);
    }

    /**
     * A thin ring just outside a tower's body, marking a permanently-chosen upgrade path (see
     * {@code td.tower.upgrade}). Deliberately shape-agnostic - always a circle, regardless of
     * the body's own triangle/ring/spiral/star/pulsar - so it reads the same way on every
     * tower and never needs updating when a body shape changes.
     */
    private void paintUpgradeAccent(Graphics2D g2, Palette accent, float bodySize) {
        float radius = bodySize * UPGRADE_ACCENT_RADIUS_FRACTION;
        Stroke previousStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(UPGRADE_ACCENT_STROKE_WIDTH));
        g2.setColor(colorFor(accent));
        g2.draw(new Ellipse2D.Float(-radius, -radius, radius * 2, radius * 2));
        g2.setStroke(previousStroke);
    }

    /**
     * Draws a tower's body centred on the origin - the caller has already translated {@code g2}
     * to the tower's centre, matching {@link #paintEnemyBody}'s contract. Every tower, the
     * passive aura one included, is a single {@link Shape} painted the same way.
     */
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
     * The sonar head, which unlike every other head is not one solid shape. A radar sweep is
     * read almost entirely from its fading trail, so this paints the wedge as a few sub-wedges
     * of decreasing alpha behind a bright leading edge - the cheap approximation of an angular
     * gradient, which Java2D has no paint for. Keeping it translucent also lets the tower's own
     * body read through as the scope being swept, rather than being covered by a blob of the
     * same colour.
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
        // the leading edge, lying on the beam's exact heading - the part the eye tracks
        g2.draw(new Line2D.Float(0, 0, radius, 0));
        g2.setStroke(previous);
    }

    /**
     * Draws a turret head shape centred on the origin - the caller has already translated/rotated {@code g2}.
     */
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
        }
    }

    // --- projectiles ---------------------------------------------------------

    /**
     * A symmetric pie wedge centred on {@code cone}'s heading - the same shape {@code InWedgeTargetQuery} tests against.
     */
    private void paintCone(Graphics2D g2, ConeDraw cone) {
        AffineTransform save = g2.getTransform();
        g2.translate(cone.originX(), cone.originY());
        g2.rotate(cone.headingRadians());
        float halfWidthDegrees = (float) Math.toDegrees(cone.halfWidthRadians());
        g2.setColor(withAlpha(colorFor(cone.palette()), Math.round(cone.alpha() * 255)));
        g2.fill(new Arc2D.Float(-cone.radius(), -cone.radius(), cone.radius() * 2, cone.radius() * 2,
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

    // --- shared ---------------------------------------------------------

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
