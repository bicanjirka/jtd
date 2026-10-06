package td.tower.upgrade;

/**
 * What a tower adds to the shared base nodes: a passive that comes with Attune, and the bonus and
 * perk of Range III.
 *
 * @param attuneText      what Attune does for this tower, empty for nothing
 * @param rangeThreeBonus the range bonus of Range III
 * @param rangeThreeText  what else Range III does for this tower, empty for nothing
 */
public record BaseSlotPerks(String attuneText, float rangeThreeBonus, String rangeThreeText) {

    public static BaseSlotPerks none() {
        return new BaseSlotPerks("", StandardBaseSlot.RANGE_STEP_BONUS, "");
    }

    public BaseSlotPerks withAttune(String attuneText) {
        return new BaseSlotPerks(attuneText, this.rangeThreeBonus, this.rangeThreeText);
    }

    public BaseSlotPerks withRangeThree(float rangeThreeBonus, String rangeThreeText) {
        return new BaseSlotPerks(this.attuneText, rangeThreeBonus, rangeThreeText);
    }
}
