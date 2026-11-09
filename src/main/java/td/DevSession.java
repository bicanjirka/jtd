package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.board.BoardGeometry;
import td.save.GameSave;
import td.save.GameSaveText;
import td.tower.Tower;
import td.ui.PanelDev;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * What the dev panel's game commands do: skipping and jumping waves, saving and loading the level's one saved
 * game, and rebuilding the selected tower. Each answers on the panel. Opening the panel and stepping the loop
 * are the window's.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class DevSession {

    private static final Logger LOG = LoggerFactory.getLogger(DevSession.class);
    private static final String SAVE_DIRECTORY = "saves";

    private final GameEngine engine;
    private final DevControls devControls;
    private final PanelDev panel;
    private final Runnable unSelectTower;
    private final Consumer<Tower> selectTower;
    private final Consumer<GameSave> restore;

    /**
     * @param restore puts a read save in play; it tears the level down, so it is the window's, which owns the loop
     */
    DevSession(GameEngine engine, DevControls devControls, PanelDev panel, Runnable unSelectTower,
            Consumer<Tower> selectTower, Consumer<GameSave> restore) {
        this.engine = engine;
        this.devControls = devControls;
        this.panel = panel;
        this.unSelectTower = unSelectTower;
        this.selectTower = selectTower;
        this.restore = restore;
    }

    void skipWave() {
        this.engine.requestSkipWave();
    }

    void jumpToWave(int number) {
        int next = this.engine.requestJumpToWave(number);
        this.panel.showMessage(next == 0 ? "No level in play" : "Wave " + next + " is next");
    }

    /** Between waves, the board as it is; during a wave, the wave as it was sent. */
    void save(String levelName) {
        Optional<GameSave> board = this.engine.save();
        Optional<GameSave> save = board.or(this.engine::checkpoint);
        if (save.isEmpty()) {
            this.panel.showMessage("No level in play");
            return;
        }
        Path file = saveFile(levelName);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GameSaveText.write(save.get()));
            int wave = save.get().nextWave();
            this.panel.showMessage((board.isPresent() ? "Saved the board before wave " + wave
                    : "Saved wave " + wave + " as it was sent") + " to " + file);
        } catch (IOException e) {
            LOG.warn("Could not save the game to {}", file, e);
            this.panel.showMessage("Could not save: " + e.getMessage());
        }
    }

    void load(String levelName) {
        Path file = saveFile(levelName);
        if (!Files.exists(file)) {
            this.panel.showMessage("No save at " + file);
            return;
        }
        GameSaveText.Read read;
        try {
            read = GameSaveText.read(Files.readString(file));
        } catch (IOException e) {
            LOG.warn("Could not read the save {}", file, e);
            this.panel.showMessage("Could not read: " + e.getMessage());
            return;
        }
        if (read.save().isEmpty()) {
            this.panel.showMessage(read.problem());
            return;
        }
        this.unSelectTower.run();
        this.restore.accept(read.save().get());
    }

    /**
     * Sells the selected tower and builds a fresh one of its type on its cell, through the same
     * input path a player uses, then selects it: its upgrades and XP start over.
     */
    void rebuildSelectedTower() {
        Optional<Tower> selected = this.devControls.selectedTower();
        if (selected.isEmpty()) {
            this.panel.showMessage("Select a tower first");
            return;
        }
        Tower tower = selected.get();
        GameWorld world = this.engine.getGameWorld();
        BoardGeometry board = world.getBoard();
        int scale = board.scale();
        int boardX = board.cellX(tower.getX()) * scale + scale / 2;
        int boardY = board.cellY(tower.getY()) * scale + scale / 2;
        this.unSelectTower.run();
        world.towers().sell(tower);
        this.engine.startPlacing(tower.getType(), 0f);
        this.engine.mouseClicked(boardX, boardY).ifPresent(this.selectTower);
        this.panel.showMessage("Rebuilt fresh: upgrades and XP reset");
    }

    /** Where the level's one saved game lives. */
    private static Path saveFile(String levelName) {
        return Path.of(SAVE_DIRECTORY, levelName + ".save");
    }
}
