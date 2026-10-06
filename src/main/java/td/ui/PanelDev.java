package td.ui;

import td.DevControls;
import td.enemy.Rank;
import td.util.ThreadConfined;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Component;
import java.awt.FlowLayout;
import java.io.Serial;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The hidden dev panel (Ctrl+Shift+D) for playtesting: set the economy, spawn any wave script,
 * move through the level, and push the selected tower along. Board changes go to
 * {@link DevControls}; changes to the level's lifecycle go up through the {@code onX} setters.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelDev extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Rank[] RANKS = Rank.values();

    private final HudTextField credits = new HudTextField("10000", 6);
    private final HudTextField lives = new HudTextField("20", 3);
    private final HudTextField script = new HudTextField("3 c", 18);
    private final HudTextField jumpTo = new HudTextField("1", 3);
    private final HudTextField xp = new HudTextField("150", 4);
    private final HudButton rankButton = new HudButton("");
    private final HudButton pathButton = new HudButton("");
    private final JLabel status = label("");

    private DevControls dev;
    private Rank rank = Rank.GRUNT;
    private int path = 0;
    private Runnable onSkipWave = () -> {
    };
    private IntConsumer onJumpToWave = wave -> {
    };
    private Runnable onRestart = () -> {
    };
    private Runnable onStep = () -> {
    };
    private Runnable onResetTower = () -> {
    };

    public PanelDev() {
        initComponents();
    }

    public void setDevControls(DevControls dev) {
        this.dev = dev;
        this.refresh();
    }

    public void onSkipWave(Runnable listener) {
        this.onSkipWave = listener;
    }

    public void onJumpToWave(IntConsumer listener) {
        this.onJumpToWave = listener;
    }

    public void onRestart(Runnable listener) {
        this.onRestart = listener;
    }

    public void onStep(Runnable listener) {
        this.onStep = listener;
    }

    public void onResetTower(Runnable listener) {
        this.onResetTower = listener;
    }

    /** Reads the installed level again: its path count bounds the path picker. */
    public void refresh() {
        int paths = this.dev == null ? 1 : this.dev.pathCount();
        this.path = Math.floorMod(this.path, paths);
        this.rankButton.setText("Rank: " + this.rank.name().toLowerCase(Locale.ROOT));
        this.pathButton.setText("Path " + (this.path + 1) + "/" + paths);
    }

    public void showMessage(String message) {
        this.status.setText(message);
    }

    private void initComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Hud.BACKGROUND);
        setBorder(Hud.panelBorder("Dev · ctrl+shift+d"));

        this.credits.addActionListener(evt -> this.setCredits());
        this.lives.addActionListener(evt -> this.setLives());
        this.script.addActionListener(evt -> this.spawn());
        this.jumpTo.addActionListener(evt -> this.jump());
        this.xp.addActionListener(evt -> this.grantXp());
        this.rankButton.addActionListener(evt -> {
            this.rank = RANKS[(this.rank.ordinal() + 1) % RANKS.length];
            this.refresh();
        });
        this.pathButton.addActionListener(evt -> {
            this.path++;
            this.refresh();
        });
        this.refresh();

        add(row(label("Credits"), this.credits, button("Set", this::setCredits),
                label("Lives"), this.lives, button("Set", this::setLives),
                toggle("∞ Lives", on -> this.dev.setInfiniteLives(on)),
                toggle("Free build", on -> this.dev.setFreeBuild(on))));
        add(row(label("Spawn"), this.script, this.rankButton, this.pathButton, button("Go", this::spawn)));
        add(row(button("Skip wave", () -> this.onSkipWave.run()),
                label("Jump to"), this.jumpTo, button("Go", this::jump),
                button("Restart", () -> this.onRestart.run()),
                button("Kill all", () -> this.dev.killAll()),
                button("Clear all", () -> this.dev.clearAll()),
                button("Step", () -> this.onStep.run())));
        add(row(label("Tower: XP"), this.xp, button("Grant", this::grantXp),
                toggle("Ignore gates", on -> this.dev.setUpgradeGatesIgnored(on)),
                button("Reset", () -> this.onResetTower.run()), this.status));
    }

    private void setCredits() {
        this.parsed(this.credits).ifPresent(amount -> {
            this.dev.setCredits(amount);
            this.showMessage("Credits set to " + amount);
        });
    }

    private void setLives() {
        this.parsed(this.lives).ifPresent(amount -> {
            this.dev.setLives(amount);
            this.showMessage("Lives set to " + amount);
        });
    }

    private void spawn() {
        this.showMessage(this.dev.spawnWave(this.script.getText(), this.rank, this.path));
    }

    private void jump() {
        this.parsed(this.jumpTo).ifPresent(wave -> this.onJumpToWave.accept(wave));
    }

    private void grantXp() {
        this.parsed(this.xp).ifPresent(amount -> this.showMessage(this.dev.grantXpToSelected(amount)));
    }

    private OptionalInt parsed(HudTextField field) {
        try {
            return OptionalInt.of(Integer.parseInt(field.getText().trim()));
        } catch (NumberFormatException e) {
            this.showMessage("Not a number: " + field.getText());
            return OptionalInt.empty();
        }
    }

    private static JPanel row(Component... components) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        row.setOpaque(false);
        for (Component component : components) {
            row.add(component);
        }
        return row;
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Hud.LABEL_FONT);
        label.setForeground(Hud.FOREGROUND);
        return label;
    }

    private static HudButton button(String text, Runnable action) {
        HudButton button = new HudButton(text);
        button.addActionListener(evt -> action.run());
        return button;
    }

    private static HudToggleButton toggle(String text, Consumer<Boolean> action) {
        HudToggleButton toggle = new HudToggleButton();
        toggle.setText(text);
        toggle.addActionListener(evt -> action.accept(toggle.isSelected()));
        return toggle;
    }

    private interface Switch {
        void set(boolean on);
    }
}
