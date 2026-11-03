package td.enemy;

/**
 * The closed set of body shapes an {@link EnemyDefinition} is drawn as; the renderer owns every
 * shape and colour. A new case needs its palette role and render case in the same change: the
 * renderer's switch is not compiler-checked, so a missing case fails only when first drawn.
 */
public enum BodyArchetype {
    CIRCLE,
    SQUARE,
    TRIANGLE,
    GHOST,
    WARDEN,
    WARDEN_EGG,
    MENDER,
    JUGGERNAUT,
    MITE,
    COURIER,
    SHIELDBEARER,
    SALAMANDER,
    YETI,
    MAGMA,
    GOLEM,
    SKIRMISHER,
    JAMMER,
    REAVER,
    COLOSSUS,
    MOLTEN_CORE,
    EMBER,
    MONOLITH,
    ECLIPSE,
    MIRAGE,
    MIRROR_IMAGE,
    HERALD,
    TALOS,
    MINOTAUR,
    BRONZE_BULL,
    DRONE,
    HIVE_GUARD,
    NURSE,
    LARVA,
    COCOON,
    HORNET,
    THUNDER,
    LIGHTNING,
    RAINCLOUD,
    RAINDROP,
    GALE
}
