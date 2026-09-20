package td.level;

import java.util.List;

/**
 * The Java-code source of levels - the only {@link LevelCatalog} implementation
 * that exists today. A future file-based catalog implements the same interface.
 * Each level's own definition lives in its own class ({@link ClassicLoopLevel},
 * {@link ZigzagGauntletLevel}, {@link WildBezierSweepLevel}) so a change to one level's content
 * touches one file, not this shared list.
 */
public class BuiltInLevelCatalog implements LevelCatalog {

    @Override
    public List<LevelDefinition> levels() {
        return List.of(ClassicLoopLevel.DEFINITION, ZigzagGauntletLevel.DEFINITION, WildBezierSweepLevel.DEFINITION);
    }
}
