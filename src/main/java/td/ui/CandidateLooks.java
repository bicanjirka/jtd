package td.ui;

import td.enemy.DefinedEnemyMob;
import td.enemy.Rank;
import td.stat.StatWording;
import td.term.Term;
import td.ui.render.IconFace;
import td.ui.render.Palette;
import td.ui.render.RankBadge;
import td.ui.render.SheetTooltip;
import td.ui.render.TooltipKey;
import td.util.GameHost;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.wave.Path;
import td.wave.WaveScriptSuggestions.Candidate;
import td.wave.WaveScriptSuggestions.Subject;

import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;

/**
 * How a suggested word shows in a list: a term with its icon and its card, a rank with its badge, an enemy with the
 * glyph every enemy shares and the card its preview opens. A word of the grammar has neither.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class CandidateLooks {

    private static final int GLYPH_ICON = 11;
    private static final float RANK_BADGE_SIZE = 4.5f;
    private static final RowLook ENEMY = enemyLook();

    private final Java2DFrameRenderer glyphRenderer = new Java2DFrameRenderer();
    private final GameWorld previewWorld = new GameWorld(GameHost.noOp());
    private final IntSupplier cellPixels;

    /** {@code cellPixels} is the game board's cell size, which an enemy card's speeds read in. */
    CandidateLooks(IntSupplier cellPixels) {
        this.cellPixels = cellPixels;
        this.previewWorld.setPath(new Path(List.of()));
    }

    Optional<RowLook> look(Candidate candidate) {
        return switch (candidate.subject()) {
            case Subject.SyntaxSubject _ -> Optional.empty();
            case Subject.RankSubject(Rank rank) -> Optional.of(this.rank(rank));
            case Subject.TermSubject(Term term) -> {
                IconFace face = TermLooks.faceOf(term);
                yield Optional.of(RowLook.ofTerm(face, face.frame()));
            }
            case Subject.EnemySubject _ -> Optional.of(ENEMY);
        };
    }

    Optional<SheetTooltip> tooltip(Candidate candidate) {
        return switch (candidate.subject()) {
            case Subject.SyntaxSubject _, Subject.RankSubject _ -> Optional.empty();
            case Subject.TermSubject(Term term) -> Optional.of(new SheetTooltip(TooltipKey.ofTerm("suggestion", term),
                    TermSheets.of(TermLooks.faceOf(term), term, term.describe(), List.of())));
            case Subject.EnemySubject enemy -> {
                DefinedEnemyMob mob = PanelEnemy.previewMob(enemy.definition(), enemy.rank(), this.previewWorld);
                yield Optional.of(new SheetTooltip(TooltipKey.of("suggestion", candidate.text()),
                        mob.accept(new EnemyInfoText(new StatWording(this.cellPixels.getAsInt())))));
            }
        };
    }

    /** A rank shows its badge, in the badge's colour; a grunt has none. */
    RowLook rank(Rank rank) {
        RankBadge badge = EnemyFrameBuilder.badgeFor(rank);
        Color color = Java2DFrameRenderer.rankBadgePalette(badge).map(Java2DFrameRenderer::colorFor)
                .orElse(Hud.FOREGROUND);
        return new RowLook(new PaintedIcon(GLYPH_ICON, GLYPH_ICON,
                g2 -> this.glyphRenderer.paintRankBadgeGlyph(g2, badge, RANK_BADGE_SIZE)), color, color);
    }

    /** Every enemy is written in the usual colour; the glyph only sets enemies apart from the other words. */
    private static RowLook enemyLook() {
        RowLook look = RowLook.ofTerm(IconFace.of(ScriptPictograms.ENEMY, "En", Palette.TERM_FRAME_TRAIT,
                Palette.TERM_FRAME_TRAIT), Palette.TERM_FRAME_TRAIT);
        return new RowLook(look.icon(), Hud.FOREGROUND, look.hint());
    }
}
