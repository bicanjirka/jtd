package td.level;

import java.util.List;

/**
 * Source of the levels on the level-select screen. An implementation reports broken content with
 * {@link td.util.GameStartupException}.
 */
public interface LevelCatalog {

    List<LevelDefinition> levels();

}
