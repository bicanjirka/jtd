package td.level;

import java.util.List;

/**
 * Source of the levels the game offers on its level-select screen. Today the
 * only implementation is {@link BuiltInLevelCatalog}; a future file-based
 * catalog implements this same interface and reports a broken level file via
 * {@link td.util.GameStartupException}, the project's one fatal-startup
 * boundary, rather than a checked exception here.
 */
public interface LevelCatalog {

    List<LevelDefinition> levels();

}
