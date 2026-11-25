package td.ui.render;

/**
 * How a shell's detonation is drawn: a Normal's small burst, a Heavy's brief fireball as its pick marks it (bare,
 * throwing shrapnel, or with a concussion's pressure wave), each payload's own burst, a nuke's mushroom cloud.
 */
public enum BlastLook {
    NORMAL,
    HEAVY,
    SHRAPNEL,
    CONCUSSION,
    NAPALM,
    TAR,
    CRYO,
    NUKE
}
