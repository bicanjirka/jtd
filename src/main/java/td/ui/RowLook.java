package td.ui;

import td.ui.render.IconFace;
import td.ui.render.Palette;

import javax.swing.Icon;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** How one choice shows in a list row and on its button: its icon, its title's colour and its hint's. */
record RowLook(Icon icon, Color text, Color hint) {

    private static final Color DIM = new Color(116, 116, 118);
    private static final float DIM_ICON_ALPHA = 0.35f;

    /** A term's row: its icon at mention size, its title in its tone and its hint in {@code hint}'s ink. */
    static RowLook ofTerm(IconFace face, Palette hint) {
        return new RowLook(new PaintedIcon(TermIconPainter.MENTION, TermIconPainter.MENTION, g2 -> {
            g2.translate(-TermIconPainter.MENTION / 2.0, -TermIconPainter.MENTION / 2.0);
            TermIconPainter.paint(g2, face, TermIconPainter.MENTION);
        }), TermIconPainter.ink(face.tone()), TermIconPainter.ink(hint));
    }

    /** The same row greyed out, for a choice that can't be taken. */
    RowLook dimmed() {
        return new RowLook(new DimmedIcon(this.icon), DIM, DIM);
    }

    private record DimmedIcon(Icon icon) implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(DIM_ICON_ALPHA));
            this.icon.paintIcon(c, g2, x, y);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return this.icon.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return this.icon.getIconHeight();
        }
    }
}
