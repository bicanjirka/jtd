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

    /**
     * Half the icon, so the toolbar reads as a row of small glyphs rather than filled chips.
     */
    private static final float ICON_BODY_SIZE_FRACTION = 0.25f;
    /**
     * A slot pip's radius and centre-to-centre spacing, both fractions of {@code bodySize} - see
     * {@link #paintSlotMarks}.
     */
    private static final float SLOT_PIP_RADIUS_FRACTION = 0.11f;
    private static final float SLOT_PIP_SPACING_FRACTION = 0.3f;
    /**
     * How far below the body centre the pip row sits.
     */
    private static final float SLOT_PIP_ROW_OFFSET_FRACTION = 0.95f;
    /**
     * A gap between one slot's pips and the next slot's, on top of the ordinary pip spacing.
     */
    private static final float SLOT_GROUP_GAP_FRACTION = 0.16f;
    /**
     * A ready chevron's size and how far above the body centre its row sits.
     */
    private static final float SLOT_CHEVRON_SIZE_FRACTION = 0.16f;
    private static final float SLOT_CHEVRON_ROW_OFFSET_FRACTION = 1.05f;
    private static final float SLOT_CHEVRON_SPACING_FRACTION = 0.45f;
    /**
     * How far outside the body the SPECIAL-slot enchant halo sits - every body shape's own
     * extent stays within {@code bodySize}, the same margin the accent ring this halo replaced
     * used.
     */
    private static final float ENCHANT_HALO_RADIUS_FRACTION = 1.3f;
    private static final float ENCHANT_HALO_STROKE_WIDTH = 2.0f;
    /**
     * How big a turret head is drawn relative to a cell - smaller than the base it sits on.
     */
    private static final float TOWER_HEAD_SIZE_FRACTION = 0.24f;

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

    /**
     * How many bands the trail fades through, and how bright the band at the leading edge is.
     */
    private static final int SONAR_TRAIL_STEPS = 4;
    private static final int SONAR_TRAIL_ALPHA = 150;
    /**
     * A rank badge's glyph size and vertical offset, both as a fraction of the mob's own body
     * scale - so the badge scales with the mob's size rather than needing a fixed pixel offset
     * that would look wrong at a different board scale. Sized and offset generously (larger than
     * a first pass used) after a screenshot showed a smaller badge reading as a barely-visible
     * sliver overlapping the body's own edge rather than a legible glyph sitting above it.
     */
    private static final float RANK_BADGE_SIZE_FRACTION = 0.6f;
    private static final float RANK_BADGE_OFFSET_FRACTION = 1.7f;
    private static final float RANK_BADGE_CHEVRON_SPACING_FRACTION = 0.55f;
    /**
     * How big a projectile is drawn - smaller than a tower's own head, since it's the shot, not the gun.
     */
    private static final float PROJECTILE_SIZE = 5f;
    /**
     * A Cinder wave's alpha at the moment it fires, before {@link #paintCone} fades it out as it
     * travels farther - configurable here rather than inline, per that feature's own ask.
     */
    private static final float CINDER_CONE_BASE_ALPHA = 0.55f;

    private static Shape markerShape(PathMarkerShape shape, float size) {
        return switch (shape) {
            case DOT -> circleShape(size);
            case CHEVRON -> chevronShape(size);
        };
    }

    /**
     * A single sideways arrowhead, shared by the path trail's moving marker and a Soldier/
     * Veteran rank badge's stripe - both read as "direction of travel" / "a stripe of rank" with
     * the same glyph, at whatever size and however many are stacked.
     */
    private static Shape chevronShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(-size, -size);
        p.lineTo(size, 0);
        p.lineTo(-size, size);
        p.lineTo(-size * 0.4f, 0);
        p.closePath();
        return p;
    }

    /**
     * A skull silhouette for the Boss rank badge - the first representational glyph in this
     * package's vocabulary (everything else is a geometric primitive: circle, square, triangle,
     * spiral, star, pulsar). Built from {@link Area} boolean ops rather than a single closed
     * path: a rounded cranium fused with a jaw, minus two eye sockets and a nose notch.
     */
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

    /**
     * A plus/cross silhouette for the Mender - two overlapping rectangles unioned via
     * {@link Area}, the same additive technique {@link #skullShape} uses subtractively.
     */
    private static Shape crossShape(float scale) {
        float arm = scale * 0.55f;
        Area cross = new Area(new Rectangle2D.Float(-scale, -arm, scale * 2, arm * 2));
        cross.add(new Area(new Rectangle2D.Float(-arm, -scale, arm * 2, scale * 2)));
        return cross;
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
     * One elongated, angular shard - a building block for {@link #crystalShape}, not used on
     * its own.
     */
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

    /**
     * A small cluster of three overlapping shards at different rotations and sizes - the
     * faceted ice-crystal overlay drawn over a frozen enemy, distinct from every smooth body
     * silhouette and ring already in use.
     */
    private static Shape crystalShape(float size) {
        Area crystal = new Area(shardShape(size, 0));
        crystal.add(new Area(shardShape(size * 0.75f, 2.1)));
        crystal.add(new Area(shardShape(size * 0.6f, -2.4)));
        return crystal;
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
            case TRAIT_MARKER_FLAT_RESIST -> new Color(140, 110, 200);
            case TRAIT_MARKER_CRITICAL_IMMUNE -> new Color(255, 210, 130);
            case TRAIT_MARKER_HURT_SPEED -> new Color(255, 140, 140);
            case TRAIT_MARKER_BURN_IMMUNE -> new Color(255, 150, 90);
            case TRAIT_MARKER_FREEZE_IMMUNE -> new Color(170, 225, 255);
            case TRAIT_MARKER_OVERFLOW -> Color.LIGHT_GRAY;
        };
    }

    private static Color withAlpha(Color base, int alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.min(255, Math.max(alpha, 0)));
    }

    private static Color healthColor(Color base, float healthFraction) {
        return withAlpha(base, Math.round(healthFraction * 255));
    }

    /**
     * Scales {@code base}'s existing alpha by {@code factor} rather than replacing it, so a
     * cloak fade composes with whatever alpha (e.g. {@link #healthColor}'s) was already there.
     * Clamped the same way {@link #withAlpha} clamps its own {@code int} argument - {@code
     * factor} is expected in {@code [0, 1]}, but a caller passing a progress value derived from
     * elsewhere should not be able to crash the renderer if that value is ever slightly out of
     * range.
     */
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

    /**
     * A rank badge is drawn upright - translated to just above the body, but never rotated with
     * {@link EnemyBodyDraw#facingRadians()} - an insignia reads best right-side up regardless of
     * which way its wearer is facing, the same reasoning {@code paintUpgradeAccent}'s ring
     * already follows for a tower's upgrade path.
     */
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

    /**
     * {@link #chevronShape} points along {@code +X} (its path-marker orientation); a rank stripe
     * reads as a conventional military chevron pointing up instead, so this rotates it -90°
     * around whatever point {@code g2} is already translated to.
     */
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

    private void paintEnemyOverlay(Graphics2D g2, EnemyOverlayDraw overlay) {
        switch (overlay) {
            case EnemyRingDraw ring -> this.paintEnemyRing(g2, ring);
            case EffectPulseDraw pulse -> this.paintEffectPulse(g2, pulse);
            case TraitMarkerDraw marker -> this.paintTraitMarker(g2, marker);
            case IceCrystalDraw crystal -> this.paintIceCrystal(g2, crystal);
        }
    }

    /**
     * A faceted ice-crystal cluster encasing a frozen enemy - a translucent icy fill plus a
     * near-white facet-line stroke, so it reads as "solid ice" rather than another status-marker
     * dot in the same blue family as {@code STATUS_MARKER_SLOW}.
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

    /**
     * A small hollow diamond naming an always-on trait - the same {@link #diamondShape} the
     * timed status row's {@link #paintStatusMarker} fills, stroked instead so the two rows read
     * as different kinds of thing (permanent vs. timed) at a glance.
     */
    private void paintTraitMarker(Graphics2D g2, TraitMarkerDraw marker) {
        AffineTransform save = g2.getTransform();
        g2.translate(marker.x(), marker.y());
        g2.setColor(colorFor(marker.palette()));
        g2.draw(diamondShape(marker.scale()));
        g2.setTransform(save);
    }

    /**
     * A thin stroked ring around an enemy - a shield bubble or a support-aura reach indicator.
     * Shaped exactly like {@link #paintAura}'s tower-side ring, just centred on an enemy instead.
     */
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
     * A ring that grows outward from nothing (a gain, a cast, a spawn burst) or shrinks inward
     * to nothing (a loss) as {@code progress} advances 0..1, fading out at the same rate -
     * {@code pulse.radius()} is the ring's target/starting size, never its current one.
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
        this.paintEnchantHalo(g2, sprite.enchantPulse(), bodySize);
        this.paintTowerBody(g2, sprite.palette(), bodySize);
        this.paintSlotPips(g2, sprite.slotMarks(), bodySize);
        this.paintSlotReadyChevrons(g2, sprite.slotMarks(), bodySize);
        g2.setTransform(save);
    }

    /**
     * A pulsing ring just outside a tower's body, shown once anything is owned in its
     * {@code SPECIAL} slot (see {@code td.tower.upgrade}) - the same "function of elapsed time"
     * pulse the Aura tower's own animation already uses (see
     * {@code TowerSpriteFrameBuilder.enchantPulseFor}), so a specialized tower reads as
     * enchanted the same way an Aura tower already reads as pulsing. Deliberately
     * shape-agnostic - always a circle, regardless of the body's own triangle/ring/spiral/
     * star/pulsar - so it reads the same way on every tower and never needs updating when a
     * body shape changes.
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

    /**
     * A row of small filled pips just below a tower's body, one per node owned in each slot -
     * "what has this tower already bought." Grouped by slot in slot order, coloured by each
     * slot's own {@link Palette} role, with a one-spacing gap between groups (an undrawn pip
     * slot, not a separate constant) rather than one contiguous row a player would have to
     * count carefully to tell slots apart.
     */
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

    /**
     * One small up-pointing chevron just above a tower's body per slot that's currently
     * ready - offers a node whose gate is met and which is affordable right now. Reuses
     * {@link #chevronShape}, rotated to point up, rather than a second arrowhead shape - "what
     * could this tower buy right now," distinct from the pip row's "what has it already bought."
     */
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


    /**
     * A symmetric pie wedge centred on {@code cone}'s heading - the same shape
     * {@code InWedgeTargetQuery} tests against - growing outward from nothing and fading out as
     * {@code cone.progress()} advances 0..1, a "gout of flame" travelling outward and
     * dissipating rather than a static area-of-effect marker.
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
