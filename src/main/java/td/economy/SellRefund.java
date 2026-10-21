package td.economy;

/** What selling a tower gives back of everything paid for it. */
public final class SellRefund {

    public static final float SHARE = 0.75f;

    private SellRefund() {
    }

    public static int of(int paid) {
        return (int) Math.round(SHARE * paid);
    }
}
