package td.ui;

import td.tower.TowerRank;
import td.util.ThreadConfined;

import javax.swing.JComponent;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.Serial;
import java.util.OptionalInt;

/**
 * A tower's XP against the ranks, full at Hero, with a tick at the XP the next node waits for. The
 * label above it says the same in words, so it reads without colour.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class XpBar extends JComponent {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int HEIGHT = 6;
    private static final Color FILL = new Color(150, 170, 200);
    private static final float SCALE_XP = TowerRank.HERO.xp();

    private int xp;
    private OptionalInt nextGateXp = OptionalInt.empty();

    XpBar() {
        setPreferredSize(new Dimension(1, HEIGHT));
        setMinimumSize(new Dimension(1, HEIGHT));
    }

    void show(int xp, OptionalInt nextGateXp) {
        if (this.xp != xp || !this.nextGateXp.equals(nextGateXp)) {
            this.xp = xp;
            this.nextGateXp = nextGateXp;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int width = getWidth();
        int height = getHeight();
        g2.setColor(FILL);
        g2.fillRect(0, 0, Math.round(width * Math.min(1f, this.xp / SCALE_XP)), height);
        g2.setColor(Hud.BORDER_IDLE);
        g2.drawRect(0, 0, width - 1, height - 1);
        this.nextGateXp.ifPresent(gate -> {
            int x = Math.round((width - 1) * Math.min(1f, gate / SCALE_XP));
            g2.setColor(Hud.FOREGROUND);
            g2.drawLine(x, 0, x, height - 1);
        });
        g2.dispose();
    }
}
