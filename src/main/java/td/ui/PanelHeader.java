package td.ui;

import td.economy.EconomyState;
import td.economy.StatusTerm;
import td.ui.render.SheetTooltip;
import td.ui.render.TooltipKey;
import td.util.ThreadConfined;
import td.wave.WaveProgress;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The strip across the top of the game: the title, the status icons centred between the title and the speed
 * buttons, and the speed buttons above the console. It owns the speed buttons and their visibility, not what a click does, and shows the
 * numbers it is given.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelHeader extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int STRIP_HEIGHT = 40;
    private static final int ICON_GAP = 10;

    private final List<StatusIcon> icons = Arrays.stream(StatusTerm.values()).map(StatusIcon::new).toList();
    private final StatusFlash flash = new StatusFlash();
    private final HudButton play = glyphButton("▶");
    private final HudButton pause = glyphButton("❚❚");
    private final HudButton fast = glyphButton("▶▶");
    private final HudButton superFast = glyphButton("▶▶▶");

    private TooltipLayer tooltips = null;
    private StatusReadout readout = StatusReadout.empty();
    private Runnable onPlay = () -> {
    };
    private Runnable onPause = () -> {
    };
    private Runnable onFast = () -> {
    };
    private Runnable onSuperFast = () -> {
    };

    public PanelHeader(String titleText) {
        this.setLayout(new BorderLayout());
        this.setBackground(Color.BLACK);
        this.setFocusable(false);
        this.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Hud.BORDER_IDLE));

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Dialog", Font.BOLD, 16));
        title.setForeground(Hud.FOREGROUND);
        // A grid bag centres each icon in the strip's height; a flow layout would hang them from the top.
        JPanel status = new JPanel(new GridBagLayout());
        status.setOpaque(false);
        GridBagConstraints each = new GridBagConstraints();
        each.insets = new Insets(0, ICON_GAP / 2, 0, ICON_GAP / 2);
        this.icons.forEach(icon -> status.add(icon, each));
        JPanel aboveBoard = new JPanel(new TitleAndCentre());
        aboveBoard.setOpaque(false);
        aboveBoard.add(title);
        aboveBoard.add(status);
        this.add(aboveBoard, BorderLayout.CENTER);
        this.add(speedButtons(), BorderLayout.EAST);

        this.setPreferredSize(new Dimension(0, STRIP_HEIGHT));
        this.setMinimumSize(new Dimension(0, STRIP_HEIGHT));
        this.play.addActionListener(this::playClicked);
        this.pause.setVisible(false);
        this.pause.addActionListener(this::pauseClicked);
        this.fast.addActionListener(this::fastClicked);
        this.superFast.addActionListener(this::superFastClicked);
    }

    /**
     * The title at the left and the status group centred in the space between the title and the right edge, where the
     * speed buttons begin; never squeezed.
     */
    private static final class TitleAndCentre implements LayoutManager {

        private static final int MARGIN = 10;

        @Override
        public void addLayoutComponent(String name, Component component) {
        }

        @Override
        public void removeLayoutComponent(Component component) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Dimension title = parent.getComponent(0).getPreferredSize();
            Dimension status = parent.getComponent(1).getPreferredSize();
            return new Dimension(MARGIN + title.width + status.width, Math.max(title.height, status.height));
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return this.preferredLayoutSize(parent);
        }

        @Override
        public void layoutContainer(Container parent) {
            Component title = parent.getComponent(0);
            Component status = parent.getComponent(1);
            Dimension titleSize = title.getPreferredSize();
            Dimension statusSize = status.getPreferredSize();
            title.setBounds(MARGIN, (parent.getHeight() - titleSize.height) / 2, titleSize.width, titleSize.height);
            int free = MARGIN + titleSize.width;
            int centred = free + (parent.getWidth() - free - statusSize.width) / 2;
            status.setBounds(Math.max(centred, free), (parent.getHeight() - statusSize.height) / 2,
                    statusSize.width, statusSize.height);
        }
    }

    private static HudButton glyphButton(String glyph) {
        HudButton button = new HudButton(glyph);
        button.setFont(Hud.GLYPH_FONT);
        return button;
    }

    private JPanel speedButtons() {
        this.play.setToolTipText("Play / start the next wave");
        this.pause.setToolTipText("Pause");
        this.fast.setToolTipText("Fast (single-steps one tick while paused)");
        this.superFast.setToolTipText("Super fast");
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        buttons.setPreferredSize(new Dimension(PanelGameConsole.WIDTH, STRIP_HEIGHT));
        GridBagConstraints at = new GridBagConstraints();
        at.gridy = 0;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.weightx = 0.1;
        at.insets = new Insets(0, 2, 0, 0);
        at.gridx = 0;
        buttons.add(this.play, at);
        buttons.add(this.pause, at);
        at.gridx = 1;
        buttons.add(this.fast, at);
        at.gridx = 2;
        buttons.add(this.superFast, at);
        return buttons;
    }

    /** Each icon's hover is a tooltip of what it counts and its value now, opening under it. */
    public void setTooltips(TooltipLayer tooltips) {
        for (StatusIcon icon : this.icons) {
            tooltips.watchBelow(icon, () -> Optional.of(new SheetTooltip(
                    TooltipKey.ofTerm("status", icon.term()), StatusSheetText.of(icon.term(), this.readout))));
        }
        this.tooltips = tooltips;
    }

    /** The next {@link #show} is a baseline, not a change: a level is starting or loading. */
    public void reset() {
        this.flash.reset();
        this.readout = StatusReadout.empty();
    }

    /** Shows the level's progress and the economy; skips what has not changed. */
    public void show(WaveProgress progress, boolean waveReady, boolean levelOver, EconomyState state) {
        this.show(new StatusReadout(progress.index(), progress.count(), waveReady, levelOver, state.lives(),
                state.score(), state.credits()));
    }

    private void show(StatusReadout next) {
        if (next.equals(this.readout)) {
            return;
        }
        this.readout = next;
        this.flash.observe(next, nowMillis());
        this.icons.forEach(icon -> icon.show(next));
        this.pulse();
        if (this.tooltips != null) {
            this.tooltips.refreshButtons();
        }
    }

    /** Called every render pulse: lets a flash fade, then rests. */
    public void pulse() {
        long now = nowMillis();
        for (StatusIcon icon : this.icons) {
            icon.flash(this.flash.strength(icon.term(), now));
        }
    }

    StatusIcon icon(StatusTerm term) {
        return this.icons.stream().filter(icon -> icon.term() == term).findFirst().orElseThrow();
    }

    private static long nowMillis() {
        return System.nanoTime() / 1_000_000L;
    }

    public void setPlaying(boolean playing) {
        this.play.setVisible(!playing);
        this.pause.setVisible(playing);
    }

    public void onPlay(Runnable r) {
        this.onPlay = r;
    }

    public void onPause(Runnable r) {
        this.onPause = r;
    }

    public void onFast(Runnable r) {
        this.onFast = r;
    }

    public void onSuperFast(Runnable r) {
        this.onSuperFast = r;
    }

    private void playClicked(ActionEvent evt) {
        this.onPlay.run();
    }

    private void pauseClicked(ActionEvent evt) {
        this.onPause.run();
    }

    private void fastClicked(ActionEvent evt) {
        this.onFast.run();
    }

    private void superFastClicked(ActionEvent evt) {
        this.onSuperFast.run();
    }
}
