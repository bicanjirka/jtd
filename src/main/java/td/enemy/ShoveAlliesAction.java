package td.enemy;

/**
 * Shoves every other enemy within {@code radiusCells} of the caster {@code distanceCells} further along its own path,
 * never past its end: a gust at their backs.
 */
public record ShoveAlliesAction(float radiusCells, float distanceCells) implements AbilityAction {

    public ShoveAlliesAction {
        if (radiusCells <= 0f || distanceCells <= 0f) {
            throw new IllegalArgumentException("A shove needs a reach and a distance, was " + radiusCells + " and "
                    + distanceCells);
        }
    }

    @Override
    public boolean appliesEffects() {
        return false;
    }
}
