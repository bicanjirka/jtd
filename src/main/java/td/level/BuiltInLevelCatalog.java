package td.level;

import java.util.List;

/** The levels defined in Java code, each in its own class so editing one level touches one file. */
public class BuiltInLevelCatalog implements LevelCatalog {

    @Override
    public List<LevelDefinition> levels() {
        return List.of(CurlyPathLevel.DEFINITION, ZigZagPathLevel.DEFINITION, TwistedHourglassLevel.DEFINITION);
    }
}
