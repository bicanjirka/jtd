package td.ui;

import java.util.List;

/**
 * Where one tower's Upgrades panel puts its nodes, by hand: each tier's rows from the bottom, and in each row every
 * single node or whole cluster at a slot. A slot is a node's width plus its gap, counted from the left; a cluster's
 * members take the slots from its own on. {@link UpgradeTreeLayout} turns it into places.
 *
 * @param tiers every tier's rows, tier 0 first
 */
record UpgradeTreePlan(List<Tier> tiers) {

    UpgradeTreePlan {
        tiers = List.copyOf(tiers);
    }

    /** One tier's rows, the bottom one first. */
    record Tier(List<Row> rows) {

        Tier {
            rows = List.copyOf(rows);
        }
    }

    record Row(List<Slot> slots) {

        Row {
            slots = List.copyOf(slots);
        }
    }

    /** A node by its id, or a cluster by its name, from slot {@code at}. */
    record Slot(String key, boolean cluster, float at) {
    }

    static UpgradeTreePlan of(Tier... tiers) {
        return new UpgradeTreePlan(List.of(tiers));
    }

    static Tier tier(Row... rows) {
        return new Tier(List.of(rows));
    }

    static Row row(Slot... slots) {
        return new Row(List.of(slots));
    }

    static Slot node(String id, float at) {
        return new Slot(id, false, at);
    }

    static Slot cluster(String name, float at) {
        return new Slot(name, true, at);
    }
}
