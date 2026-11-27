package td.tower.aura;

/** What Prism's light carries toward one Lit tower: the bearing of the tower from the lantern, and its mark. */
public record PrismArc(float angleRadians, PrismMark mark) {
}
