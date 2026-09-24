package td.level;

/** Where the loaded level stands. Once not {@link #PLAYING}, only loading a level changes it. */
public enum LevelOutcome {
    PLAYING,
    WON,
    LOST;

    public boolean isOver() {
        return this != PLAYING;
    }
}
