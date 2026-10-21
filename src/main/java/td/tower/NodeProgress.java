package td.tower;

/** How far a counting node is toward its next go: {@code count} of {@code of}. */
public record NodeProgress(String nodeId, int count, int of) {
}
