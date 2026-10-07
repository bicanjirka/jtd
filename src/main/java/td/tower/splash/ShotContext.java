package td.tower.splash;

/**
 * What a perk may know about a shot it shapes.
 *
 * @param shotNumber how many shots the Splash has fired, this one included
 */
public record ShotContext(int shotNumber) {
}
