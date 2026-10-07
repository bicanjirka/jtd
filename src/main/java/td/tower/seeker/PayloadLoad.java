package td.tower.seeker;

/**
 * What one missile carries.
 *
 * @param payload  the payload
 * @param strength a multiple of the base payload
 */
public record PayloadLoad(Payload payload, float strength) {
}
