package td.ui;

/**
 * Which dev panel cheats the status bar shows.
 *
 * @param livesUnlimited whether a leak takes no lives
 * @param freeBuild      whether buying costs nothing
 */
record StatusCheats(boolean livesUnlimited, boolean freeBuild) {

    private static final StatusCheats NONE = new StatusCheats(false, false);

    static StatusCheats none() {
        return NONE;
    }
}
