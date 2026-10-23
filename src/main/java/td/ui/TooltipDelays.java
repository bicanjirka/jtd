package td.ui;

/**
 * How long a {@link TooltipLayer} waits before it opens or closes a tooltip.
 *
 * @param openMs  how long the pointer rests on a target before its tooltip opens
 * @param closeMs how long the pointer stays off sources and tooltips before they close
 */
public record TooltipDelays(int openMs, int closeMs) {

    public static TooltipDelays standard() {
        return new TooltipDelays(150, 250);
    }
}
