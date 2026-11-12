package td.balance;

/**
 * One kind of answer a defence has, as a {@link ProbeRig} lands it: the axes an enemy's power is read on.
 */
enum PowerProbe {
    /** Heavy physical hits aimed at one enemy, some of them crits: armor, dodging, hiding and resilience answer. */
    SHOT("shot"),
    /** Heavy magic hits aimed at one enemy: magic resist, dodging and hiding answer. */
    BOLT("bolt"),
    /** Small physical hits on everyone near the enemy aimed at, never dodged: plating answers, a crowd suffers. */
    BLAST("blast"),
    /** A burn kept going on everyone on the board: insulation, magic resist and burn immunity answer. */
    BURN("burn");

    private final String label;

    PowerProbe(String label) {
        this.label = label;
    }

    String label() {
        return this.label;
    }
}
