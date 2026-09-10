package td.ui;

import td.ui.render.AuraDraw;
import td.ui.render.BeamDraw;
import td.ui.render.CellDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.Palette;
import td.ui.render.PathMarkerDraw;
import td.ui.render.PathMarkerShape;
import td.ui.render.PulseDraw;
import td.ui.render.RenderFrame;
import td.ui.render.SplashDraw;
import td.ui.render.TowerEffectDraw;
import td.ui.render.TowerSpriteDraw;
import td.ui.render.TurretHeadDraw;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
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
        for (TowerSpriteDraw sprite : frame.towerSprites()) {
            this.paintTowerSprite(g2, sprite, frame.scale());
        }
        for (TurretHeadDraw head : frame.towerHeads()) {
            this.paintTurretHead(g2, head, frame.scale());
        }
        for (TowerEffectDraw effect : frame.towerEffects()) {
            this.paintTowerEffect(g2, effect);
        }
    }

    // --- cells ---------------------------------------------------------

    private void paintCell(Graphics2D g2, CellDraw cell, RenderFrame frame) {
        switch (cell.highlight()) {
            case place -> this.paintPlaceHighlight(g2, cell, frame);
            case select -> this.paintSelectHighlight(g2, cell, frame);
            case none -> {
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

    // --- path markers ---------------------------------------------------------

    private void paintPathMarker(Graphics2D g2, PathMarkerDraw marker) {
        AffineTransform save = g2.getTransform();
        g2.translate(marker.x(), marker.y());
        g2.rotate(marker.facingRadians());
        g2.setColor(colorFor(marker.palette()));
        g2.fill(markerShape(marker.shape(), marker.size()));
        g2.setTransform(save);
    }

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

    // --- enemies ---------------------------------------------------------

    /** Draws just enemy bodies/fades with no board around them - used by the wave-preview strip. */
    public void paintEnemies(Graphics2D g2, List<EnemyDraw> enemies) {
        for (EnemyDraw enemy : enemies) {
            this.paintEnemy(g2, enemy);
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
            default -> throw new IllegalStateException("Not an enemy palette: " + fade.palette());
        }
        g2.setTransform(save);
    }

    private static Shape enemyShape(Palette palette, float scale) {
        return switch (palette) {
            case ENEMY_CIRCLE, ENEMY_GHOST -> circleShape(scale);
            case ENEMY_SQUARE -> new Rectangle2D.Float(-scale, -scale, scale * 2, scale * 2);
            case ENEMY_TRIANGLE -> triangleShape(scale, true);
            default -> throw new IllegalStateException("Not an enemy palette: " + palette);
        };
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

    // --- towers ---------------------------------------------------------

    /** How much of a cell a tower body fills - leaves a small margin, same spirit as enemy bodies. */
    private static final float TOWER_BODY_SIZE_FRACTION = 0.42f;

    /** A representative static pose for a toolbar icon's head - pointing up on screen. */
    private static final double ICON_HEAD_HEADING_RADIANS = -Math.PI / 2;

    /**
     * Rasterizes one tower's base+head into a standalone icon - used for the toolbar's
     * {@code JToggleButton} icons, which need a Swing {@code Icon} rather than a live paint.
     * The board and the toolbar share the same {@link #paintTowerBody}/head-shape calls at two
     * different sizes, so a tower never needs separate board/icon art. The head is drawn at a
     * fixed representative heading and neutral (non-pulsing) scale, since a static icon has no
     * target to aim at and no animation clock.
     */
    public BufferedImage renderTowerIcon(Palette palette, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(size / 2.0, size / 2.0);
        this.paintTowerBody(g2, palette, size * TOWER_BODY_SIZE_FRACTION);
        g2.rotate(ICON_HEAD_HEADING_RADIANS);
        this.paintHeadShape(g2, palette, size * TOWER_HEAD_SIZE_FRACTION);
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
        this.paintTowerBody(g2, sprite.palette(), scale * TOWER_BODY_SIZE_FRACTION);
        g2.setTransform(save);
    }

    /**
     * Draws a tower's body centred on the origin - the caller has already translated {@code g2}
     * to the tower's centre, matching {@link #paintEnemyBody}'s contract. {@code TOWER_UPGRADE_BODY}
     * is genuinely two-tone (it buffs neighbouring towers rather than attacking) so it paints
     * itself rather than going through the shared single-{@link Shape} outline+fill path.
     */
    private void paintTowerBody(Graphics2D g2, Palette palette, float size) {
        if (palette == Palette.TOWER_UPGRADE_BODY) {
            this.paintUpgradeBody(g2, size);
            return;
        }
        Shape shape = towerBodyShape(palette, size);
        Color color = colorFor(palette);
        g2.setColor(withAlpha(color, 130));
        g2.fill(shape);
        g2.setColor(color);
        g2.draw(shape);
    }

    private static Shape towerBodyShape(Palette palette, float size) {
        return switch (palette) {
            case TOWER_ONE_BODY -> diamondShape(size);
            case TOWER_TWO_BODY -> ringShape(size);
            case TOWER_THREE_BODY -> pinwheelShape(3, size);
            case TOWER_FOUR_BODY -> starShape(5, size, size * 0.45f);
            default -> throw new IllegalStateException("Not an attacking tower body palette: " + palette);
        };
    }

    private static Shape diamondShape(float size) {
        GeneralPath p = new GeneralPath();
        p.moveTo(0, -size);
        p.lineTo(size * 0.55f, 0);
        p.lineTo(0, size);
        p.lineTo(-size * 0.55f, 0);
        p.closePath();
        return p;
    }

    private static Shape ringShape(float size) {
        Area ring = new Area(circleShape(size));
        float inner = size * 0.55f;
        ring.subtract(new Area(circleShape(inner)));
        return ring;
    }

    /** A rotationally-symmetric blade pinwheel - {@code blades} lets a future tower reuse this at a different count. */
    private static Shape pinwheelShape(int blades, float size) {
        Area pin = new Area();
        Shape blade = new Ellipse2D.Float(0, -size * 0.18f, size, size * 0.36f);
        for (int i = 0; i < blades; i++) {
            AffineTransform rotate = AffineTransform.getRotateInstance(2 * Math.PI * i / blades);
            pin.add(new Area(rotate.createTransformedShape(blade)));
        }
        return pin;
    }

    /** A classic N-pointed star polygon - {@code points}/radii let a future tower reuse this at a different count. */
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

    /** How big a turret head is drawn relative to a cell - smaller than the base it sits on. */
    private static final float TOWER_HEAD_SIZE_FRACTION = 0.24f;

    private void paintTurretHead(Graphics2D g2, TurretHeadDraw head, int scale) {
        AffineTransform save = g2.getTransform();
        g2.translate(head.centerX(), head.centerY());
        g2.rotate(head.headingRadians());
        this.paintHeadShape(g2, head.palette(), scale * TOWER_HEAD_SIZE_FRACTION * head.scale());
        g2.setTransform(save);
    }

    /** Draws a turret head shape centred on the origin - the caller has already translated/rotated {@code g2}. */
    private void paintHeadShape(Graphics2D g2, Palette palette, float size) {
        Shape shape = turretHeadShape(palette, size);
        Color color = colorFor(palette);
        g2.setColor(withAlpha(color, 200));
        g2.fill(shape);
        g2.setColor(color);
        g2.draw(shape);
    }

    /**
     * The two aiming towers' heads (one/two) are authored pointing along {@code +X} (heading
     * {@code 0}) so {@link Graphics2D#rotate(double)} alone aims them correctly - see
     * {@link TurretHeadDraw}'s doc comment. The two spinning towers (three/four) have no such
     * concern - any starting phase looks equally valid while continuously spinning - but their
     * heads still need to reach *past* their own (larger) base's silhouette to actually read as
     * moving: a same-shape-family head entirely contained within the base's footprint turned
     * out to be visually indistinguishable from standing still, since both are the same hue.
     * TOWER_THREE gets a thin sweep arm through the centre (a "radar hand"); TOWER_FOUR gets a
     * small star orbiting off-centre (a "moon") - both echo their base's own shape family
     * (rectangle/star) while clearing the base's edge. TOWER_UPGRADE's head ignores rotation
     * entirely (it pulses via {@link TurretHeadDraw#scale()} instead) so a plain circle needs
     * no special orientation.
     */
    private static Shape turretHeadShape(Palette palette, float size) {
        return switch (palette) {
            case TOWER_ONE_BODY -> new Rectangle2D.Float(0, -size * 0.22f, size * 1.3f, size * 0.44f);
            case TOWER_TWO_BODY -> new Rectangle2D.Float(0, -size * 0.42f, size * 0.95f, size * 0.84f);
            case TOWER_THREE_BODY -> new Rectangle2D.Float(-size * 2.4f, -size * 0.16f, size * 4.8f, size * 0.32f);
            case TOWER_FOUR_BODY -> {
                Shape moon = starShape(5, size * 0.9f, size * 0.9f * 0.45f);
                yield AffineTransform.getTranslateInstance(size * 2.0, 0).createTransformedShape(moon);
            }
            case TOWER_UPGRADE_BODY -> circleShape(size);
            default -> throw new IllegalStateException("Not a tower head palette: " + palette);
        };
    }

    private void paintUpgradeBody(Graphics2D g2, float size) {
        Color base = colorFor(Palette.TOWER_UPGRADE_BODY);
        Shape circle = circleShape(size);
        g2.setColor(withAlpha(base, 130));
        g2.fill(circle);
        g2.setColor(base);
        g2.draw(circle);
        float dot = size * 0.34f;
        g2.setColor(Color.DARK_GRAY);
        g2.fill(new Ellipse2D.Float(-dot * 0.4f, -size * 0.4f, dot * 2, dot * 2));
    }

    private void paintTowerEffect(Graphics2D g2, TowerEffectDraw effect) {
        switch (effect) {
            case BeamDraw beam -> this.paintBeam(g2, beam);
            case SplashDraw splash -> this.paintFilledCircle(g2, splash.palette(), splash.centerX(), splash.centerY(), splash.radius());
            case PulseDraw pulse -> this.paintFilledCircle(g2, pulse.palette(), pulse.centerX(), pulse.centerY(), pulse.radius());
            case AuraDraw aura -> this.paintAura(g2, aura);
        }
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

    // --- shared ---------------------------------------------------------

    private static Color colorFor(Palette palette) {
        return switch (palette) {
            case ENEMY_CIRCLE -> Color.CYAN;
            case ENEMY_GHOST -> Color.LIGHT_GRAY;
            case ENEMY_SQUARE -> Color.PINK;
            case ENEMY_TRIANGLE -> Color.YELLOW;
            case TOWER_ONE_BODY -> Color.GREEN;
            case TOWER_TWO_BODY -> Color.RED;
            case TOWER_THREE_BODY -> Color.YELLOW;
            case TOWER_FOUR_BODY -> Color.ORANGE;
            case TOWER_UPGRADE_BODY -> Color.WHITE;
            case TOWER_UPGRADE_AURA -> Color.WHITE;
            case TOWER_ONE_BEAM -> Color.GREEN;
            case TOWER_TWO_BEAM -> Color.RED;
            case TOWER_TWO_SPLASH_LINE, TOWER_TWO_SPLASH_FILL -> withAlpha(Color.RED, 80);
            case TOWER_THREE_BEAM -> Color.YELLOW;
            case TOWER_FOUR_PULSE -> withAlpha(Color.ORANGE, 80);
            case PATH_MARKER_MOVING -> withAlpha(Color.WHITE, 200);
            case PATH_MARKER_STATIC -> withAlpha(Color.WHITE, 70);
        };
    }

    private static Color withAlpha(Color base, int alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.min(255, Math.max(alpha, 0)));
    }

    private static Color healthColor(Color base, float healthFraction) {
        return withAlpha(base, Math.round(healthFraction * 255));
    }
}
