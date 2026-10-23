package td.ui;

import td.DevControls;
import td.DevEffects;
import td.DevSelection;
import td.effect.EffectKind;
import td.enemy.Rank;
import td.tower.TowerTerm;
import td.ui.render.InfoSheet;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.wave.PathColor;
import td.wave.WaveScriptSuggestions.Result;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The hidden dev panel (Ctrl+Shift+D) for playtesting: set the economy, spawn any wave script,
 * move through the level, dial the game speed by ticks per second, push the selected tower or enemy about
 * and show the cell grid. Board changes go to {@link DevControls}; changes to the level's lifecycle
 * go up through the {@code onX} setters.
 * <p>
 * Every control has an Alt shortcut, which {@link #runShortcut} runs for the frame. A shortcut on a
 * field gives it the focus; Enter applies it and hands the keys back to the board, as Esc does.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelDev extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Rank[] RANKS = Rank.values();
    private static final Color PROBLEM = new Color(255, 140, 120);
    private static final Color MUTED = new Color(150, 170, 150);
    private static final String CARD_PREVIEW = "preview";
    private static final String CARD_PROBLEM = "problem";
    private static final String CARD_NOTHING = "nothing";
    private static final String CARD_TOWER = "tower";
    private static final String CARD_ENEMY = "enemy";
    private static final List<EffectKind> ENEMY_EFFECTS = DevEffects.enemyKinds();
    private static final List<TowerTerm> TOWER_BUFFS = DevEffects.towerBuffs();
    /** Space between sections, and between unrelated controls in a row. */
    private static final int ROW_GAP = 3;
    private static final int CONTROL_GAP = 12;
    /** The preview strip is as tall as its enemy slots plus their padding. */
    private static final int PREVIEW_WIDTH = 160;
    private static final int PREVIEW_HEIGHT = 34;

    private final HudTextField credits = new HudTextField("10000", 6);
    private final HudTextField lives = new HudTextField("20", 4);
    private final HudTextField script = new HudTextField("3 simple", 14);
    private final HudTextField jumpTo = new HudTextField("1", 3);
    private final HudTextField xp = new HudTextField("150", 5);
    private final HudTextField enemyHealth = new HudTextField("100", 4);
    private final HudTextField effectSeconds = new HudTextField("5", 3);
    private final HudTextField effectPower = new HudTextField("3", 4);
    private final HudTextField buffSeconds = new HudTextField("10", 3);
    private final HudTextField buffPower = new HudTextField("50", 4);
    private final HudButton effectButton = new HudButton("");
    private final HudButton buffButton = new HudButton("");
    private final JLabel towerSelected = label("");
    private final JLabel enemySelected = label("");
    private final CardLayout selectionCards = new CardLayout();
    private final JPanel selectionArea = new JPanel(this.selectionCards);
    private final HudButton rankButton = new HudButton("");
    private final HudButton pathButton = new HudButton("");
    private final HudToggleButton cellGridToggle = toggle("Cell grid");
    private final PathWaveRow preview;
    private final JLabel problem = label("");
    private final CardLayout previewCards = new CardLayout();
    private final JPanel previewArea = new JPanel(this.previewCards);
    private final JLabel pointerCell = label("");
    private final JLabel clickedCell = label("");
    private final JLabel speed = label("");
    private final JLabel status = label(" ");
    private final List<Shortcut> shortcuts = new ArrayList<>();

    private GameWorld world;
    private DevControls dev;
    private Rank rank = Rank.GRUNT;
    private int path = 0;
    private int effectIndex = 0;
    private int buffIndex = 0;
    private DevSelection shownSelection = DevSelection.none();
    private Runnable onSkipWave = () -> {
    };
    private IntConsumer onJumpToWave = wave -> {
    };
    private Runnable onRestart = () -> {
    };
    private Runnable onStep = () -> {
    };
    private IntConsumer onNudgeSpeed = delta -> {
    };
    private Runnable onResetTower = () -> {
    };
    private Runnable onSaveGame = () -> {
    };
    private Runnable onLoadGame = () -> {
    };
    private Consumer<Boolean> onCellGrid = shown -> {
    };
    private Consumer<String> onInfoText = text -> {
    };
    private Runnable onReleaseFocus = () -> {
    };

    /** {@code onEnemyHover} gets the sheet of the preview enemy under the pointer. */
    public PanelDev(Consumer<InfoSheet> onEnemyHover) {
        this.preview = new PathWaveRow(onEnemyHover);
        initComponents();
    }

    public void setGameWorld(GameWorld world, DevControls dev) {
        this.world = world;
        this.dev = dev;
        this.preview.setGameWorld(world);
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

    /** Called with how many ticks per second faster the game should run; negative slows it. */
    public void onNudgeSpeed(IntConsumer listener) {
        this.onNudgeSpeed = listener;
    }

    public void onResetTower(Runnable listener) {
        this.onResetTower = listener;
    }

    public void onSaveGame(Runnable listener) {
        this.onSaveGame = listener;
    }

    public void onLoadGame(Runnable listener) {
        this.onLoadGame = listener;
    }

    /** Called with whether the cell grid should show. */
    public void onCellGrid(Consumer<Boolean> listener) {
        this.onCellGrid = listener;
    }

    /** Called with text for the info pane: the help, or a wave script's whole problem. */
    public void onInfoText(Consumer<String> listener) {
        this.onInfoText = listener;
    }

    /** Called when Esc leaves a field, so the board's keys work again. */
    public void onReleaseFocus(Runnable listener) {
        this.onReleaseFocus = listener;
    }

    /** Whether one of this panel's fields has the keyboard focus. */
    public boolean holdsFocus() {
        Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return owner != null && SwingUtilities.isDescendingFrom(owner, this);
    }

    public boolean isCellGridOn() {
        return this.cellGridToggle.isSelected();
    }

    /** Reads the installed level again: its paths bound and colour the path picker. */
    public void refresh() {
        int paths = this.dev == null ? 1 : this.dev.pathCount();
        this.path = Math.floorMod(this.path, paths);
        this.rankButton.setText("Rank: " + this.rank.name().toLowerCase(Locale.ROOT));
        this.pathButton.setText("Path " + (this.path + 1) + "/" + paths);
        this.pathButton.setForeground(this.pathColor());
        this.effectButton.setText(ENEMY_EFFECTS.get(this.effectIndex).title());
        this.buffButton.setText(TOWER_BUFFS.get(this.buffIndex).title());
        this.refreshPreview();
    }

    public void showMessage(String message) {
        this.status.setText(message);
    }

    public void showPointerCell(int cellX, int cellY) {
        this.pointerCell.setText("Pointer  " + cellX + ", " + cellY);
    }

    public void clearPointerCell() {
        this.pointerCell.setText("Pointer  -");
    }

    public void showClickedCell(int cellX, int cellY) {
        this.clickedCell.setText("Clicked  " + cellX + ", " + cellY);
    }

    /** The game's live speed; 0 while paused. */
    public void showTicksPerSecond(int ticksPerSecond) {
        this.speed.setText(ticksPerSecond + " ticks/s");
    }

    /** Animates the wave preview on the render pulse. */
    public void doTick(int gameTime) {
        this.preview.doTick(gameTime);
        this.refreshSelection();
    }

    /** Shows the card for what is selected now, and names it; only a change repaints. */
    private void refreshSelection() {
        if (this.dev == null) {
            return;
        }
        DevSelection selection = this.dev.selection();
        if (selection.equals(this.shownSelection)) {
            return;
        }
        this.shownSelection = selection;
        boolean tower = selection.kind() == DevSelection.Kind.TOWER;
        this.towerSelected.setText(tower ? "Tower: " + selection.description() : "");
        this.enemySelected.setText(tower ? "" : "Enemy: " + selection.description());
        this.selectionCards.show(this.selectionArea, switch (selection.kind()) {
            case NONE -> CARD_NOTHING;
            case TOWER -> CARD_TOWER;
            case ENEMY -> CARD_ENEMY;
        });
    }

    /**
     * Runs the control bound to {@code stroke}, if any. Only while the panel shows, so a hidden
     * panel takes no keys.
     *
     * @return whether a control ran
     */
    public boolean runShortcut(KeyStroke stroke) {
        if (!this.isShowing()) {
            return false;
        }
        Optional<Shortcut> match = this.shortcuts.stream().filter(s -> s.stroke().equals(stroke)).findFirst();
        match.ifPresent(s -> s.action().run());
        return match.isPresent();
    }

    /** Every shortcut and what the panel's controls do, for the info pane. */
    String helpText() {
        StringBuilder text = new StringBuilder("""
                Dev panel - ctrl+shift+d
                Alt+key presses a control. On
                a field it takes the focus;
                enter applies it, and enter
                or esc gives the keys back
                to the board.
                """);
        String group = "";
        for (Shortcut shortcut : this.shortcuts) {
            if (!shortcut.group().equals(group)) {
                group = shortcut.group();
                text.append('\n').append(group).append('\n');
            }
            text.append(keyName(shortcut.stroke())).append("  ").append(shortcut.name()).append('\n');
        }
        text.append("""

                Bare keys, while open:
                n  skip wave
                x  spawn next catalog enemy
                c  +1000 credits

                The script is a level's wave
                script, as in "3 elite swarm
                4 simple". The preview updates as
                you type; hover it to inspect.
                The script field suggests the
                next token: tab or right takes
                the grey completion, down opens
                the list, enter takes the
                highlighted row, a click takes
                any row, esc closes it.
                Health % applies to enemies
                that walk in after it is set.
                Kill all pays bounty and XP,
                clear all pays nothing.

                Selected: click a tower or an
                enemy on the board. A tower
                takes XP, a buff and a reset
                (reset rebuilds it with no
                upgrades or XP). An enemy takes
                an effect, a clear, a kill (pays
                bounty and XP), a heal to full
                or a removal (pays nothing).
                Power is the effect's own
                number: stacks, percent or
                points.""");
        return text.toString();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBackground(Hud.BACKGROUND);
        setBorder(Hud.panelBorder("Dev · ctrl+shift+d · alt+h help"));

        this.submitOnEnter(this.credits, this::setCredits);
        this.submitOnEnter(this.lives, this::setLives);
        this.submitOnEnter(this.script, this::spawn);
        this.submitOnEnter(this.jumpTo, this::jump);
        this.submitOnEnter(this.xp, this::grantXp);
        this.submitOnEnter(this.enemyHealth, this::setEnemyHealth);
        this.submitOnEnter(this.effectSeconds, this::applyEffect);
        this.submitOnEnter(this.effectPower, this::applyEffect);
        this.submitOnEnter(this.buffSeconds, this::applyBuff);
        this.submitOnEnter(this.buffPower, this::applyBuff);
        this.effectButton.addActionListener(evt -> {
            this.effectIndex = (this.effectIndex + 1) % ENEMY_EFFECTS.size();
            this.refresh();
        });
        this.buffButton.addActionListener(evt -> {
            this.buffIndex = (this.buffIndex + 1) % TOWER_BUFFS.size();
            this.refresh();
        });
        this.rankButton.addActionListener(evt -> {
            this.rank = RANKS[(this.rank.ordinal() + 1) % RANKS.length];
            this.refresh();
        });
        this.pathButton.addActionListener(evt -> {
            this.path++;
            this.refresh();
        });
        this.script.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                PanelDev.this.refreshPreview();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                PanelDev.this.refreshPreview();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                PanelDev.this.refreshPreview();
            }
        });
        this.cellGridToggle.addActionListener(evt -> this.onCellGrid.accept(this.cellGridToggle.isSelected()));
        for (HudTextField field : List.of(this.credits, this.lives, this.script, this.jumpTo, this.xp,
                this.enemyHealth, this.effectSeconds, this.effectPower, this.buffSeconds, this.buffPower)) {
            releaseFocusOnEscape(field);
        }
        new WaveScriptCompleter(this.script,
                (text, caret) -> this.dev == null ? Result.none() : this.dev.suggestWaveScript(text, caret));

        HudButton setCredits = button("Set", this::setCredits);
        HudButton setLives = button("Set", this::setLives);
        HudToggleButton infiniteLives = toggle("∞ Lives");
        infiniteLives.addActionListener(evt -> this.dev.setInfiniteLives(infiniteLives.isSelected()));
        HudToggleButton freeBuild = toggle("Free build");
        freeBuild.addActionListener(evt -> this.dev.setFreeBuild(freeBuild.isSelected()));
        HudToggleButton ignoreGates = toggle("Ignore gates");
        ignoreGates.addActionListener(evt -> this.dev.setUpgradeGatesIgnored(ignoreGates.isSelected()));
        this.section(0, "Economy", row(label("Credits"), this.credits, setCredits, gap(),
                label("Lives"), this.lives, setLives, gap(), infiniteLives, freeBuild, ignoreGates));

        HudButton skip = button("Skip wave", () -> this.onSkipWave.run());
        HudButton step = button("Step", () -> this.onStep.run());
        HudButton restart = button("Restart", () -> this.onRestart.run());
        HudButton killAll = button("Kill all", () -> this.dev.killAll());
        HudButton clearAll = button("Clear all", () -> this.dev.clearAll());
        HudButton save = button("Save", () -> this.onSaveGame.run());
        HudButton load = button("Load", () -> this.onLoadGame.run());
        this.section(1, "Level", row(skip, gap(), label("Wave"), this.jumpTo, button("Go", this::jump), gap(),
                killAll, clearAll, gap(), label("Health %"), this.enemyHealth, button("Set", this::setEnemyHealth),
                gap(), save, load, gap(), restart));

        HudButton slowerByFive = button("-5", () -> this.onNudgeSpeed.accept(-5));
        HudButton slower = button("-1", () -> this.onNudgeSpeed.accept(-1));
        HudButton faster = button("+1", () -> this.onNudgeSpeed.accept(1));
        HudButton fasterByFive = button("+5", () -> this.onNudgeSpeed.accept(5));
        // Sized for the widest rate, so the buttons after it stay put as the digits change.
        this.speed.setText("0000 ticks/s");
        this.speed.setPreferredSize(this.speed.getPreferredSize());
        this.speed.setHorizontalAlignment(SwingConstants.CENTER);
        this.section(2, "Speed", row(step, gap(), slowerByFive, slower, this.speed, faster, fasterByFive));

        this.problem.setForeground(PROBLEM);
        this.previewArea.setOpaque(false);
        this.previewArea.add(this.preview, CARD_PREVIEW);
        this.previewArea.add(this.problem, CARD_PROBLEM);
        this.previewArea.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        widthFollowsPanel(this.problem);
        widthFollowsPanel(this.status);
        JPanel spawnRow = new JPanel(new GridBagLayout());
        spawnRow.setOpaque(false);
        spawnRow.add(row(this.script, this.rankButton, this.pathButton, button("Spawn", this::spawn)));
        GridBagConstraints previewCell = new GridBagConstraints();
        previewCell.weightx = 1.0;
        previewCell.fill = GridBagConstraints.HORIZONTAL;
        previewCell.insets = new Insets(0, CONTROL_GAP - 4, 0, 0);
        spawnRow.add(this.previewArea, previewCell);
        this.section(3, "Spawn", spawnRow);

        HudButton grant = button("Grant", this::grantXp);
        HudButton reset = button("Reset", () -> this.onResetTower.run());
        HudButton applyBuff = button("Apply", this::applyBuff);
        HudButton applyEffect = button("Apply", this::applyEffect);
        HudButton clearEffects = button("Clear", () -> this.showMessage(this.dev.clearEnemyEffects()));
        HudButton kill = button("Kill", () -> this.showMessage(this.dev.killSelectedEnemy()));
        HudButton heal = button("Heal", () -> this.showMessage(this.dev.healSelectedEnemy()));
        HudButton remove = button("Remove", () -> this.showMessage(this.dev.removeSelectedEnemy()));
        this.selectionArea.setOpaque(false);
        this.selectionArea.add(row(label("Select a tower or an enemy on the board first.")), CARD_NOTHING);
        this.selectionArea.add(row(this.towerSelected, gap(), label("XP"), this.xp, grant, gap(),
                this.buffButton, label("Secs"), this.buffSeconds, label("Power"), this.buffPower, applyBuff, gap(),
                reset), CARD_TOWER);
        this.selectionArea.add(row(this.enemySelected, gap(), this.effectButton, label("Secs"),
                this.effectSeconds, label("Power"), this.effectPower, applyEffect, clearEffects, gap(), kill, heal,
                remove), CARD_ENEMY);
        this.selectionCards.show(this.selectionArea, CARD_NOTHING);
        this.section(4, "Selected", this.selectionArea);

        this.pointerCell.setForeground(MUTED);
        this.clickedCell.setForeground(MUTED);
        this.clearPointerCell();
        this.clickedCell.setText("Clicked  -");
        HudButton help = button("? Help", () -> this.onInfoText.accept(this.helpText()));
        this.status.setForeground(Hud.FOREGROUND);
        JPanel boardRow = new JPanel(new GridBagLayout());
        boardRow.setOpaque(false);
        boardRow.add(row(this.cellGridToggle, this.pointerCell, this.clickedCell));
        GridBagConstraints statusCell = new GridBagConstraints();
        statusCell.weightx = 1.0;
        statusCell.fill = GridBagConstraints.HORIZONTAL;
        statusCell.insets = new Insets(0, CONTROL_GAP, 0, CONTROL_GAP);
        boardRow.add(this.status, statusCell);
        boardRow.add(help, new GridBagConstraints());
        this.section(5, "Board", boardRow);

        this.bind("Economy", alt(KeyEvent.VK_C), "credits", focus(this.credits));
        this.bind("Economy", alt(KeyEvent.VK_L), "lives", focus(this.lives));
        this.bind("Economy", alt(KeyEvent.VK_I), "infinite lives", infiniteLives::doClick);
        this.bind("Economy", alt(KeyEvent.VK_B), "free build", freeBuild::doClick);
        this.bind("Level", alt(KeyEvent.VK_N), "skip wave", skip::doClick);
        this.bind("Level", altShift(KeyEvent.VK_R), "restart level", restart::doClick);
        this.bind("Level", alt(KeyEvent.VK_J), "jump to wave", focus(this.jumpTo));
        this.bind("Level", alt(KeyEvent.VK_K), "kill all", killAll::doClick);
        this.bind("Level", altShift(KeyEvent.VK_K), "clear all", clearAll::doClick);
        this.bind("Level", alt(KeyEvent.VK_E), "enemy health %", focus(this.enemyHealth));
        this.bind("Level", alt(KeyEvent.VK_V), "save the game between waves", save::doClick);
        this.bind("Level", alt(KeyEvent.VK_O), "load the saved game", load::doClick);
        this.bind("Speed", alt(KeyEvent.VK_T), "step one tick", step::doClick);
        this.bind("Speed", altShift(KeyEvent.VK_COMMA), "5 ticks/s slower", slowerByFive::doClick);
        this.bind("Speed", alt(KeyEvent.VK_COMMA), "1 tick/s slower", slower::doClick);
        this.bind("Speed", alt(KeyEvent.VK_PERIOD), "1 tick/s faster", faster::doClick);
        this.bind("Speed", altShift(KeyEvent.VK_PERIOD), "5 ticks/s faster", fasterByFive::doClick);
        this.bind("Spawn", alt(KeyEvent.VK_S), "wave script", focus(this.script));
        this.bind("Spawn", alt(KeyEvent.VK_R), "next rank", this.rankButton::doClick);
        this.bind("Spawn", alt(KeyEvent.VK_P), "next path", this.pathButton::doClick);
        this.bind("Economy", alt(KeyEvent.VK_U), "ignore upgrade gates", ignoreGates::doClick);
        this.bind("Tower", alt(KeyEvent.VK_X), "XP to grant", focus(this.xp));
        this.bind("Tower", altShift(KeyEvent.VK_X), "reset", reset::doClick);
        this.bind("Tower", alt(KeyEvent.VK_Y), "next buff", this.buffButton::doClick);
        this.bind("Tower", altShift(KeyEvent.VK_Y), "apply the buff", applyBuff::doClick);
        this.bind("Enemy", alt(KeyEvent.VK_F), "next effect", this.effectButton::doClick);
        this.bind("Enemy", altShift(KeyEvent.VK_F), "apply the effect", applyEffect::doClick);
        this.bind("Enemy", alt(KeyEvent.VK_D), "clear its effects", clearEffects::doClick);
        this.bind("Enemy", alt(KeyEvent.VK_Q), "kill", kill::doClick);
        this.bind("Enemy", alt(KeyEvent.VK_W), "heal to full", heal::doClick);
        this.bind("Enemy", altShift(KeyEvent.VK_Q), "remove", remove::doClick);
        this.bind("Board", alt(KeyEvent.VK_G), "cell grid", this.cellGridToggle::doClick);
        this.bind("Board", alt(KeyEvent.VK_H), "this help", help::doClick);

        this.refresh();
    }

    private void refreshPreview() {
        if (this.dev == null) {
            return;
        }
        DevControls.WaveScriptCheck check = this.dev.checkWaveScript(this.script.getText(), this.rank, this.path);
        check.wave().ifPresent(this.preview::setWave);
        this.problem.setText(check.problem());
        this.previewCards.show(this.previewArea, check.wave().isPresent() ? CARD_PREVIEW : CARD_PROBLEM);
    }

    private Color pathColor() {
        if (this.world == null || this.path >= this.world.level().pathCount()) {
            return Hud.FOREGROUND;
        }
        PathColor color = this.world.level().paths().get(this.path).color();
        return new Color(color.r(), color.g(), color.b());
    }

    /**
     * Enter runs {@code action}; once it applies, the keys go back to the board, so a typed
     * command reads like a shortcut. A field whose text didn't apply keeps the focus.
     */
    private void submitOnEnter(HudTextField field, BooleanSupplier action) {
        field.addActionListener(evt -> {
            if (action.getAsBoolean()) {
                this.onReleaseFocus.run();
            }
        });
    }

    private boolean setCredits() {
        OptionalInt amount = this.parsed(this.credits);
        amount.ifPresent(credits -> {
            this.dev.setCredits(credits);
            this.showMessage("Credits set to " + credits);
        });
        return amount.isPresent();
    }

    private boolean setLives() {
        OptionalInt amount = this.parsed(this.lives);
        amount.ifPresent(count -> {
            this.dev.setLives(count);
            this.showMessage("Lives set to " + count);
        });
        return amount.isPresent();
    }

    private boolean setEnemyHealth() {
        OptionalInt percent = this.parsed(this.enemyHealth);
        percent.ifPresent(value -> this.showMessage(this.dev.setEnemyHealthPercent(value)));
        return percent.isPresent();
    }

    /** A script with a problem puts the whole problem in the info pane, where it wraps. */
    private boolean spawn() {
        DevControls.WaveScriptCheck check = this.dev.checkWaveScript(this.script.getText(), this.rank, this.path);
        if (check.wave().isEmpty()) {
            this.onInfoText.accept("Wave script problem\n\n" + check.problem());
            this.showMessage("Nothing spawned: the problem is in Info");
            return false;
        }
        this.showMessage(this.dev.spawnWave(this.script.getText(), this.rank, this.path));
        return true;
    }

    private boolean jump() {
        OptionalInt wave = this.parsed(this.jumpTo);
        wave.ifPresent(this.onJumpToWave::accept);
        return wave.isPresent();
    }

    private boolean grantXp() {
        OptionalInt amount = this.parsed(this.xp);
        amount.ifPresent(granted -> this.showMessage(this.dev.grantXpToSelected(granted)));
        return amount.isPresent();
    }

    private boolean applyEffect() {
        OptionalInt seconds = this.parsed(this.effectSeconds);
        OptionalInt power = this.parsed(this.effectPower);
        if (seconds.isEmpty() || power.isEmpty()) {
            return false;
        }
        this.showMessage(this.dev.applyEffectToEnemy(ENEMY_EFFECTS.get(this.effectIndex), seconds.getAsInt(),
                power.getAsInt()));
        return true;
    }

    private boolean applyBuff() {
        OptionalInt seconds = this.parsed(this.buffSeconds);
        OptionalInt power = this.parsed(this.buffPower);
        if (seconds.isEmpty() || power.isEmpty()) {
            return false;
        }
        this.showMessage(this.dev.grantBuffToSelectedTower(TOWER_BUFFS.get(this.buffIndex), seconds.getAsInt(),
                power.getAsInt()));
        return true;
    }

    private OptionalInt parsed(HudTextField field) {
        try {
            return OptionalInt.of(Integer.parseInt(field.getText().trim()));
        } catch (NumberFormatException e) {
            this.showMessage("Not a number: " + field.getText());
            return OptionalInt.empty();
        }
    }

    private void bind(String group, KeyStroke stroke, String name, Runnable action) {
        this.shortcuts.add(new Shortcut(group, stroke, name, action));
    }

    private static Runnable focus(HudTextField field) {
        return () -> {
            field.requestFocusInWindow();
            field.selectAll();
        };
    }

    private void releaseFocusOnEscape(HudTextField field) {
        field.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "releaseFocus");
        field.getActionMap().put("releaseFocus", new AbstractAction() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent evt) {
                PanelDev.this.onReleaseFocus.run();
            }
        });
    }

    /**
     * A long text ends in an ellipsis instead of widening the panel, which would push the window
     * past what the board's layout can fit.
     */
    private static void widthFollowsPanel(JLabel label) {
        label.setPreferredSize(new Dimension(1, label.getPreferredSize().height));
    }

    private static KeyStroke alt(int keyCode) {
        return KeyStroke.getKeyStroke(keyCode, InputEvent.ALT_DOWN_MASK);
    }

    private static KeyStroke altShift(int keyCode) {
        return KeyStroke.getKeyStroke(keyCode, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
    }

    private static String keyName(KeyStroke stroke) {
        boolean shift = (stroke.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0;
        return "alt+" + (shift ? "shift+" : "") + KeyEvent.getKeyText(stroke.getKeyCode()).toLowerCase(Locale.ROOT);
    }

    /** One row of the panel: a muted caption naming the section, then its controls. */
    private void section(int y, String caption, JComponent content) {
        JLabel title = label(caption);
        title.setForeground(MUTED);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(y == 0 ? 0 : ROW_GAP, 2, 0, CONTROL_GAP - 4);
        add(title, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = y;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(y == 0 ? 0 : ROW_GAP, 0, 0, 0);
        add(content, c);
    }

    private static JPanel row(Component... components) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        row.setOpaque(false);
        for (Component component : components) {
            row.add(component);
        }
        return row;
    }

    /** Extra room in a row between controls that do unrelated things. */
    private static Component gap() {
        JPanel gap = new JPanel();
        gap.setOpaque(false);
        gap.setPreferredSize(new Dimension(CONTROL_GAP, 1));
        return gap;
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

    private static HudToggleButton toggle(String text) {
        HudToggleButton toggle = new HudToggleButton();
        toggle.setText(text);
        return toggle;
    }

    private record Shortcut(String group, KeyStroke stroke, String name, Runnable action) {
    }
}
