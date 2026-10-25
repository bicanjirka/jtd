package td.ui;

import td.TickSpeed;
import td.economy.EconomyState;
import td.economy.StatusTerm;
import td.level.LevelOutcome;
import td.term.TermText;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.SheetLine;
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
 * The strip across the top of the game: the title and the menu button, the status icons centred between them and the
 * speed buttons, and the speed buttons above the console. The speed buttons are a radio showing the speed it is told;
 * a click only reports, so what a click does and which one is selected stay with the owner. It shows the numbers it
 * is given.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelHeader extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int STRIP_HEIGHT = 40;
    private static final int ICON_GAP = 10;

    private final List<StatusIcon> icons = Arrays.stream(StatusTerm.values()).map(StatusIcon::new).toList();
    private final StatusFlash flash = new StatusFlash();
    private final HudToggleButton pause = speedButton("❚❚", GameKey.PAUSE);
    private final HudToggleButton play = speedButton("▶", GameKey.PLAY);
    private final HudToggleButton fast = speedButton("▶▶", GameKey.FAST);
    private final HudToggleButton superFast = speedButton("▶▶▶", GameKey.SUPER_FAST);
    private final HudButton menu = new HudButton("Menu");

    private TooltipLayer tooltips = null;
    private StatusReadout readout = StatusReadout.empty();
    private RoundStrip rounds = RoundStrip.empty();
    private Runnable onPlay = () -> {
    };
    private Runnable onPause = () -> {
    };
    private Runnable onFast = () -> {
    };
    private Runnable onSuperFast = () -> {
    };
    private Runnable onMenu = () -> {
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
        this.menu.setFace(ControlFace.button().withChip(GameKey.MENU.chip()));
        aboveBoard.add(title);
        aboveBoard.add(status);
        aboveBoard.add(this.menu);
        this.add(aboveBoard, BorderLayout.CENTER);
        this.add(speedButtons(), BorderLayout.EAST);

        this.setPreferredSize(new Dimension(0, STRIP_HEIGHT));
        this.setMinimumSize(new Dimension(0, STRIP_HEIGHT));
        this.play.addActionListener(this::playClicked);
        this.pause.addActionListener(this::pauseClicked);
        this.fast.addActionListener(this::fastClicked);
        this.superFast.addActionListener(this::superFastClicked);
        this.menu.addActionListener(evt -> this.onMenu.run());
    }

    /**
     * The title at the left with the menu button after it, and the status group centred in the space between the menu
     * button and the right edge, where the speed buttons begin; never squeezed.
     */
    private static final class TitleAndCentre implements LayoutManager {

        private static final int MARGIN = 10;
        private static final int MENU_GAP = 10;

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
            Dimension menu = parent.getComponent(2).getPreferredSize();
            return new Dimension(MARGIN + title.width + MENU_GAP + menu.width + status.width,
                    Math.max(Math.max(title.height, status.height), menu.height));
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return this.preferredLayoutSize(parent);
        }

        @Override
        public void layoutContainer(Container parent) {
            Component title = parent.getComponent(0);
            Component status = parent.getComponent(1);
            Component menu = parent.getComponent(2);
            Dimension titleSize = title.getPreferredSize();
            Dimension statusSize = status.getPreferredSize();
            Dimension menuSize = menu.getPreferredSize();
            title.setBounds(MARGIN, (parent.getHeight() - titleSize.height) / 2, titleSize.width, titleSize.height);
            int menuX = MARGIN + titleSize.width + MENU_GAP;
            menu.setBounds(menuX, (parent.getHeight() - menuSize.height) / 2, menuSize.width, menuSize.height);
            int free = menuX + menuSize.width;
            int centred = free + (parent.getWidth() - free - statusSize.width) / 2;
            status.setBounds(Math.max(centred, free), (parent.getHeight() - statusSize.height) / 2,
                    statusSize.width, statusSize.height);
        }
    }

    private static HudToggleButton speedButton(String glyph, GameKey key) {
        HudToggleButton button = new HudToggleButton();
        button.setText(glyph);
        button.setFont(Hud.GLYPH_FONT);
        button.setFace(ControlFace.button().withChip(key.chip()));
        return button;
    }

    private JPanel speedButtons() {
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        buttons.setPreferredSize(new Dimension(PanelGameConsole.WIDTH, STRIP_HEIGHT));
        GridBagConstraints at = new GridBagConstraints();
        at.gridy = 0;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.weightx = 0.1;
        at.insets = new Insets(0, 2, 0, 0);
        at.gridx = 0;
        buttons.add(this.pause, at);
        at.gridx = 1;
        buttons.add(this.play, at);
        at.gridx = 2;
        buttons.add(this.fast, at);
        at.gridx = 3;
        buttons.add(this.superFast, at);
        return buttons;
    }

    /**
     * Each icon's hover is a tooltip of what it counts and its value now, opening under it; each speed button's, what
     * it does; the menu button's, where it goes.
     */
    public void setTooltips(TooltipLayer tooltips) {
        for (StatusIcon icon : this.icons) {
            tooltips.watchBelow(icon, () -> Optional.of(new SheetTooltip(
                    TooltipKey.ofTerm("status", icon.term()), StatusSheetText.of(icon.term(), this.readout, this.rounds))));
        }
        tooltips.watchButton(this.pause, () -> tooltip("Pause",
                "Pauses the game. While paused or between waves, plays on at the last speed and sends the next wave."));
        tooltips.watchButton(this.play, () -> tooltip("Play", "Normal speed. Sends the next wave if it is waiting."));
        tooltips.watchButton(this.fast, () -> tooltip("Fast", "Fast speed. Sends the next wave if it is waiting."));
        tooltips.watchButton(this.superFast, () -> tooltip("Super fast",
                "The fastest speed. Sends the next wave if it is waiting."));
        tooltips.watchBelow(this.menu, () -> tooltip("Menu",
                "Back to the level select. Asks first while the level is still being played."));
        this.tooltips = tooltips;
    }

    private static Optional<SheetTooltip> tooltip(String name, String what) {
        return Optional.of(new SheetTooltip(TooltipKey.of("header", name), new InfoSheet(List.of(
                new SheetLine.Title(SheetLine.Glyph.CHEVRON, Palette.TERM_STATUS_WAVE, name, ""),
                new SheetLine.Prose(TermText.of(what))))));
    }

    /** The next {@link #show} is a baseline, not a change: a level is starting or loading. */
    public void reset() {
        this.flash.reset();
        this.icon(StatusTerm.WAVE).resetRounds();
        this.readout = StatusReadout.empty();
        this.rounds = RoundStrip.empty();
    }

    /** Shows the level's progress and the economy; skips what has not changed. */
    public void show(WaveProgress progress, boolean waveReady, LevelOutcome outcome, EconomyState state) {
        this.show(new StatusReadout(progress.index(), progress.count(), waveReady, outcome.isOver(), state.lives(),
                state.score(), state.credits()), RoundStrip.of(progress, waveReady, outcome));
    }

    private void show(StatusReadout next, RoundStrip nextRounds) {
        if (next.equals(this.readout) && nextRounds.equals(this.rounds)) {
            return;
        }
        this.readout = next;
        this.rounds = nextRounds;
        this.flash.observe(next, nowMillis());
        this.icons.forEach(icon -> icon.show(next));
        this.icon(StatusTerm.WAVE).showRounds(nextRounds, nowMillis());
        this.pulse();
        if (this.tooltips != null) {
            this.tooltips.refreshButtons();
        }
    }

    /** Flashes the cash icon: a purchase was refused for want of cash. */
    public void cashRefused() {
        this.flash.refused(nowMillis());
        this.pulse();
    }

    /** Called every render pulse: lets a flash fade and the round skulls move. */
    public void pulse() {
        long now = nowMillis();
        for (StatusIcon icon : this.icons) {
            icon.flash(this.flash.strength(icon.term(), now));
        }
        this.icon(StatusTerm.WAVE).animate(now);
    }

    StatusIcon icon(StatusTerm term) {
        return this.icons.stream().filter(icon -> icon.term() == term).findFirst().orElseThrow();
    }

    private static long nowMillis() {
        return System.nanoTime() / 1_000_000L;
    }

    /** Selects the button of {@code speed}; none for a rate between the presets, as the dev panel can dial. */
    public void showSpeed(TickSpeed speed) {
        this.pause.setSelected(speed.equals(TickSpeed.PAUSED));
        this.play.setSelected(speed.equals(TickSpeed.NORMAL));
        this.fast.setSelected(speed.equals(TickSpeed.FAST));
        this.superFast.setSelected(speed.equals(TickSpeed.SUPER_FAST));
    }

    /** Once a level is won or lost the speed buttons, and their chips, go dim. */
    public void setSpeedEnabled(boolean enabled) {
        for (HudToggleButton button : List.of(this.pause, this.play, this.fast, this.superFast)) {
            button.setEnabled(enabled);
        }
    }

    public void onMenu(Runnable r) {
        this.onMenu = r;
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
