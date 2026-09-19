package td.ui.render;

/**
 * The glyph drawn above an enemy's body naming its rank - never a number, anywhere the player
 * can see it. {@link #NONE} is Grunt's own badge: the unranked default carries no glyph at all.
 * Fixed, one per {@code td.enemy.Rank}: {@code GRUNT -> NONE}, {@code SOLDIER -> ONE_CHEVRON},
 * {@code VETERAN -> TWO_CHEVRON}, {@code ELITE -> STAR}, {@code BOSS -> SKULL} - see
 * {@code td.ui.EnemyFrameBuilder}'s mapping.
 */
public enum RankBadge {
    NONE,
    ONE_CHEVRON,
    TWO_CHEVRON,
    STAR,
    SKULL
}
