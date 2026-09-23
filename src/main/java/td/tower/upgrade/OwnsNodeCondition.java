package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

/**
 * Satisfied once the tower already owns the node with this id, anywhere in its tree - the
 * structural prerequisite a chain's next level, or a slot's shared {@code Awaken} gate, is
 * expressed with. See {@code StandardBaseSlot.opens}.
 */
record OwnsNodeCondition(String nodeId) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return tower.upgrades().owns(this.nodeId);
    }

    @Override
    public String describe() {
        return "requires " + this.nodeId;
    }
}
