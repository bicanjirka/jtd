package td.level;

import java.util.List;

/**
 * The levels defined in Java code, each in its own class so editing one level touches one file. The order is the
 * level-select screen's: the card colours run round the hue wheel, and difficulty is tuned to fit it, not the reverse.
 */
public class BuiltInLevelCatalog implements LevelCatalog {

    @Override
    public List<LevelDefinition> levels() {
        return List.of(AmberHiveLevel.DEFINITION, LabyrinthLevel.DEFINITION, LastHourLevel.DEFINITION,
                StormRidgeLevel.DEFINITION, RiftLevel.DEFINITION, IronCrossingLevel.DEFINITION);
    }
}
