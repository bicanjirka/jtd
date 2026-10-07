package td.tower.upgrade;

/**
 * What a tower adds to the shared base nodes: a passive that comes with Attune, a perk that comes with
 * Awaken, and the bonus and perk of Range III.
 *
 * @param attuneText      what Attune does for this tower, empty for nothing
 * @param awakenText      what Awaken does for this tower, empty for nothing
 * @param rangeThreeBonus the range bonus of Range III
 * @param rangeThreeText  what else Range III does for this tower, empty for nothing
 */
public record BaseSlotPerks(String attuneText, String awakenText, float rangeThreeBonus, String rangeThreeText) {

    public static BaseSlotPerks none() {
        return new BaseSlotPerks("", "", StandardBaseSlot.RANGE_STEP_BONUS, "");
    }

    public BaseSlotPerks withAttune(String attuneText) {
        return new BaseSlotPerks(attuneText, this.awakenText, this.rangeThreeBonus, this.rangeThreeText);
    }

    public BaseSlotPerks withAwaken(String awakenText) {
        return new BaseSlotPerks(this.attuneText, awakenText, this.rangeThreeBonus, this.rangeThreeText);
    }

    public BaseSlotPerks withRangeThree(float rangeThreeBonus, String rangeThreeText) {
        return new BaseSlotPerks(this.attuneText, this.awakenText, rangeThreeBonus, rangeThreeText);
    }
}
