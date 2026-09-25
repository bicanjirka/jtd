package td.tower.upgrade;

/**
 * One thing an upgrade node grants, short enough for one row: {@code "Damage"} and
 * {@code "+30%"}, or an extra effect in words with no value.
 */
public record UpgradeBonus(String label, String value) {
}
