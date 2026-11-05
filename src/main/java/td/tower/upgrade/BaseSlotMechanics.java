package td.tower.upgrade;

/**
 * What a tower adds to the shared base nodes: what comes with Attune, and the range bonus and effects of Range III.
 *
 * @param attune          what Attune does for this tower
 * @param rangeThreeBonus the range bonus of Range III, a share of the base range
 * @param rangeThree      what else Range III does for this tower
 */
public record BaseSlotMechanics(NodeEffects attune, float rangeThreeBonus, NodeEffects rangeThree) {

    public static BaseSlotMechanics none() {
        return new BaseSlotMechanics(NodeEffects.none(), StandardBaseSlot.RANGE_STEP_BONUS,
                NodeEffects.none());
    }

    public BaseSlotMechanics withAttune(NodeEffects attune) {
        return new BaseSlotMechanics(attune, this.rangeThreeBonus, this.rangeThree);
    }

    public BaseSlotMechanics withRangeThree(float rangeThreeBonus, NodeEffects rangeThree) {
        return new BaseSlotMechanics(this.attune, rangeThreeBonus, rangeThree);
    }
}
