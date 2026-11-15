package td.ui;

import td.damage.DamageUnits;
import td.stat.StatBreakdown;
import td.stat.StatWording;
import td.term.Side;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.AppliedEffect;
import td.tower.NodeProgress;
import td.tower.NodeWords;
import td.tower.TowerBuffState;
import td.tower.TowerInspection;
import td.tower.TowerStatLine;
import td.tower.TowerTerm;
import td.tower.ability.Ability;
import td.tower.ability.AbilityBar;
import td.tower.ability.AbilityState;
import td.tower.mortar.MagazineView;
import td.tower.mortar.ShellKind;
import td.tower.seeker.MissileKind;
import td.tower.seeker.NestView;
import td.tower.upgrade.UpgradeBonus;
import td.tower.upgrade.UpgradeLine;
import td.tower.upgrade.UpgradeNode;
import td.ui.render.IconFace;
import td.ui.render.InfoSheet;
import td.ui.render.LoadedMissile;
import td.ui.render.LoadedShell;
import td.ui.render.Palette;
import td.ui.render.SheetIcon;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.ui.render.SheetTooltip;
import td.ui.render.TooltipKey;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns a {@link TowerInspection} into the tower pane: stat rows, what it gives, then icon rows (Perks, Buffs,
 * Debuffs, Applies) with the abilities and their bars after the Perks, then its record. The shop shows the tower as authored; the status shows it as it is now.
 * Pure: no Swing, no live tower.
 */
final class TowerSheetText {

    private static final int TIME_LABEL_BELOW_TICKS = Math.round(10 * TickRate.TICKS_PER_SECOND);
    static final String PLACING_HINT = "Click a free cell to build it. Esc cancels.";
    /** No tower row reads in pixels, so tower words never need the board's cell size. */
    static final StatWording WORDING = new StatWording(Float.NaN);
    /** What follows a change a tower hands out rather than takes. */
    private static final String GIVEN = "nearby";

    private TowerSheetText() {
    }

    /**
     * The sheet while placing: price, authored stats, whom it targets, what it applies, what no row says, then how to
     * place it and how to stop.
     */
    static InfoSheet shop(TowerInspection tower) {
        return shop(tower, Optional.of(new SheetLine.Prose(TermText.of(PLACING_HINT))));
    }

    /** A toolbar button's tooltip: the shop sheet, every row, without how to place it. */
    static InfoSheet shopTooltip(TowerInspection tower) {
        return shop(tower, Optional.empty());
    }

    private static InfoSheet shop(TowerInspection tower, Optional<SheetLine> closing) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(title(tower, TermNumbers.cash(tower.price())));
        lines.add(new SheetLine.Gap());
        lines.addAll(StatRowText.catalog(breakdowns(tower), WORDING));
        gives(tower, lines);
        targets(tower).ifPresent(lines::add);
        iconRow("Applies", appliesIcons(tower)).ifPresent(lines::add);
        if (!tower.description().isEmpty()) {
            lines.add(new SheetLine.Gap());
            lines.add(new SheetLine.Prose(TermText.of(tower.description())));
        }
        closing.ifPresent(line -> {
            lines.add(new SheetLine.Gap());
            lines.add(line);
        });
        return new InfoSheet(lines);
    }

    /**
     * A placed tower: current stats, whom it targets, its perks, its abilities with their bars, buffs, debuffs and
     * applies, then its record.
     */
    static InfoSheet status(TowerInspection tower) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(title(tower, ""));
        lines.add(new SheetLine.Gap());
        lines.addAll(StatRowText.live(breakdowns(tower), WORDING));
        gives(tower, lines);
        targets(tower).ifPresent(lines::add);
        iconRow("Perks", perkIcons(tower)).ifPresent(lines::add);
        tower.abilities().forEach(ability -> lines.add(abilityRow(ability)));
        tower.magazine().ifPresent(magazine -> lines.addAll(magazineLines(magazine)));
        tower.nest().filter(nest -> nest.chances().size() > 1).ifPresent(nest -> lines.addAll(nestLines(nest)));
        iconRow("Buffs", conditionIcons(tower, Side.BUFF)).ifPresent(lines::add);
        iconRow("Debuffs", conditionIcons(tower, Side.DEBUFF)).ifPresent(lines::add);
        iconRow("Applies", appliesIcons(tower)).ifPresent(lines::add);
        lines.add(Row.plain(Glyph.FILLED_DIAMOND, "XP", TermNumbers.number(tower.xp())));
        lines.add(Row.plain(Glyph.SKULL, "Kills", TermNumbers.number(tower.kills()) + " · "
                + TermNumbers.number(DamageUnits.inPoints(tower.damageDealt()))).withGloss("dmg", ""));
        return new InfoSheet(lines);
    }

    private static List<StatBreakdown> breakdowns(TowerInspection tower) {
        return tower.stats().stream().map(TowerStatLine::breakdown).toList();
    }

    /** What a tower hands the towers around it, after a gap, each a change in its stat's own row. */
    private static void gives(TowerInspection tower, List<SheetLine> lines) {
        if (tower.gives().isNone()) {
            return;
        }
        lines.add(new SheetLine.Gap());
        tower.gives().stats().forEach(stat -> lines.add(StatRowText.change(stat, tower.gives().get(stat), WORDING)
                .withGloss("", GIVEN)));
    }

    private static SheetLine.TowerHeader title(TowerInspection tower, String value) {
        return new SheetLine.TowerHeader(TowerSpriteFrameBuilder.lookFor(tower.type()),
                tower.name() + " tower", value);
    }

    private static Optional<SheetLine> targets(TowerInspection tower) {
        return tower.targeting().isEmpty() ? Optional.empty()
                : Optional.of(Row.plain(Glyph.DOT, "Targets", tower.targeting()));
    }

    private static Optional<SheetLine> iconRow(String label, List<SheetIcon> icons) {
        return icons.isEmpty() ? Optional.empty() : Optional.of(new SheetLine.IconRow(label, icons));
    }

    private static List<SheetIcon> perkIcons(TowerInspection tower) {
        return tower.lines().stream().map(line -> perkIcon(tower, line)).toList();
    }

    private static SheetIcon perkIcon(TowerInspection tower, UpgradeLine line) {
        IconFace face = TermLooks.faceOfLine(tower.type(), line.artNode(), line.level());
        IconFace shown = progressOf(tower, line).map(progress -> face.withCorner(progress.count() + "/" + progress.of()))
                .orElse(face);
        List<SheetLine> rows = new ArrayList<>();
        rows.add(new SheetLine.IconTitle(face, line.tip().displayName(), face.tone(), ""));
        for (UpgradeNode node : line.owned()) {
            Palette slot = TowerSpriteFrameBuilder.tierPaletteFor(node.tier());
            NodeWords words = tower.words().get(node.id());
            if (line.owned().size() > 1 && words != null) {
                rows.add(Row.toned(Glyph.PIP, slot, words.name(), ""));
            }
            if (words == null) {
                continue;
            }
            words.stats().forEach(bonus -> rows.add(bonusRow(bonus)));
            words.sentences().forEach(text -> rows.add(SheetLine.Sentence.of(text.asSentence())));
            words.chosenOver().forEach(name -> rows.add(SheetLine.Sentence.marked(Glyph.LOCK, slot,
                    TermText.of("chosen over " + name))));
        }
        return new SheetIcon(shown, TooltipKey.of("perk", line.first().id()), new InfoSheet(rows));
    }

    private static Optional<NodeProgress> progressOf(TowerInspection tower, UpgradeLine line) {
        NodeProgress found = null;
        for (NodeProgress progress : tower.progress()) {
            if (line.owned().stream().anyMatch(node -> node.id().equals(progress.nodeId()))) {
                found = progress;
            }
        }
        return Optional.ofNullable(found);
    }

    /** An ability and its bar; its hover gives the bar in units, what a cast costs and what the ability does. */
    private static SheetLine.AbilityRow abilityRow(AbilityState state) {
        Ability ability = state.ability();
        AbilityBar bar = state.bar();
        IconFace face = TermLooks.faceOf(ability.term());
        IconFace currency = TermLooks.faceOf(ability.currency());
        float fill = bar.fill().inUnits();
        List<SheetLine> rows = new ArrayList<>();
        rows.add(Row.faced(currency, ability.currency().title(),
                TermNumbers.number(fill) + " / " + TermNumbers.whole(bar.capacity())));
        rows.add(Row.plain(Glyph.DOT, "Cost", TermNumbers.whole(bar.cost())));
        InfoSheet hover = TermSheets.of(ability.term(), ability.term().describe(), rows);
        return new SheetLine.AbilityRow(face, ability.term().title(), currency, bar.capacity(), bar.cost(), fill,
                new SheetTooltip(TooltipKey.ofTerm("ability", ability.term()), hover));
    }

    /** The magazine's slots and strip, then each kind's share in words, every named kind a mention. */
    private static List<SheetLine> magazineLines(MagazineView magazine) {
        List<SheetLine.ShellShare> shares = magazine.chances().entrySet().stream()
                .map(entry -> new SheetLine.ShellShare(shellOf(entry.getKey()), shareTone(entry.getKey()),
                        entry.getValue()))
                .toList();
        TermText chances = chancesText(magazine.chances());
        List<SheetLine> rows = new ArrayList<>();
        magazine.chances().forEach((kind, share) -> rows.add(Row.toned(Glyph.DOT, shareTone(kind),
                kind.title(), TermNumbers.percent(share))));
        InfoSheet hover = TermSheets.of(TowerTerm.MAGAZINE, TowerTerm.MAGAZINE.describe(), rows);
        SheetLine.MagazineRow row = new SheetLine.MagazineRow(magazine.shells().stream().map(TowerSheetText::shellOf)
                .toList(), magazine.loading(), shares,
                new SheetTooltip(TooltipKey.ofTerm("magazine", TowerTerm.MAGAZINE), hover));
        return List.of(row, SheetLine.Sentence.of(chances));
    }

    /** The nest's slots and strip once it draws more than one kind, then each kind's share in words. */
    private static List<SheetLine> nestLines(NestView nest) {
        List<SheetLine.MissileShare> shares = nest.chances().entrySet().stream()
                .map(entry -> new SheetLine.MissileShare(missileOf(entry.getKey()), entry.getValue()))
                .toList();
        TermText chances = TermText.empty();
        List<SheetLine> rows = new ArrayList<>();
        for (Map.Entry<MissileKind, Float> entry : nest.chances().entrySet()) {
            if (!chances.spans().isEmpty()) {
                chances = chances.plus(" · ");
            }
            chances = chances.plus(entry.getKey().term()).plus(" " + TermNumbers.percent(entry.getValue()));
            rows.add(Row.toned(Glyph.DOT, MissileArt.paletteOf(missileOf(entry.getKey())), entry.getKey().term().title(),
                    TermNumbers.percent(entry.getValue())));
        }
        InfoSheet hover = TermSheets.of(TowerTerm.NEST, TowerTerm.NEST.describe(), rows);
        SheetLine.NestRow row = new SheetLine.NestRow(nest.missiles().stream().map(TowerSheetText::missileOf).toList(),
                shares, new SheetTooltip(TooltipKey.ofTerm("nest", TowerTerm.NEST), hover));
        return List.of(row, SheetLine.Sentence.of(chances));
    }

    private static LoadedMissile missileOf(MissileKind kind) {
        return ProjectileFrameBuilder.loadedMissileOf(kind.look());
    }

    /** "Normal 60% · Heavy 40%". */
    static TermText chancesText(Map<ShellKind, Float> chances) {
        TermText text = TermText.empty();
        for (Map.Entry<ShellKind, Float> entry : chances.entrySet()) {
            if (!text.spans().isEmpty()) {
                text = text.plus(" · ");
            }
            text = text.plus(entry.getKey().mention()).plus(" " + TermNumbers.percent(entry.getValue()));
        }
        return text;
    }

    private static LoadedShell shellOf(ShellKind kind) {
        return ProjectileFrameBuilder.loadedShellOf(kind.look());
    }

    /** A kind's colour in the magazine's strip: the colour its shell is drawn in. */
    private static Palette shareTone(ShellKind kind) {
        return switch (kind) {
            case NORMAL -> Palette.SHELL_NORMAL;
            case HEAVY -> Palette.TERM_HEAVY_SHELL;
            case NUKE -> Palette.BLAST_NUKE_RING;
            case NAPALM -> Palette.PROJECTILE_NAPALM;
            case TAR -> Palette.ZONE_TAR;
            case CRYO -> Palette.PROJECTILE_FROST;
            case FLARE -> Palette.ZONE_FLARE;
        };
    }

    private static List<SheetIcon> conditionIcons(TowerInspection tower, Side side) {
        return tower.conditions().stream().filter(state -> state.side() == side)
                .map(TowerSheetText::conditionIcon).toList();
    }

    private static SheetIcon conditionIcon(TowerBuffState state) {
        IconFace face = TermLooks.faceOf(state.term());
        if (state.stacks() > 0) {
            face = face.withCorner(Integer.toString(state.stacks()));
        }
        List<SheetLine> rows = new ArrayList<>();
        if (state.remainingTicks().isPresent()) {
            int remaining = state.remainingTicks().getAsInt();
            if (state.clockTicks() > 0) {
                face = face.withElapsed(1f - (float) remaining / state.clockTicks());
            }
            if (remaining < TIME_LABEL_BELOW_TICKS) {
                face = face.withBelow((int) Math.ceil(remaining / (double) TickRate.TICKS_PER_SECOND) + "s");
            }
            rows.add(Row.plain(Glyph.DOT, "Time left", TermNumbers.number(remaining / (float) TickRate.TICKS_PER_SECOND))
                    .withGloss("s", ""));
        }
        if (state.stacks() > 0) {
            rows.add(Row.plain(Glyph.DOT, "Stacks", TermNumbers.number(state.stacks())));
        }
        state.changes().stats().forEach(stat -> rows.add(StatRowText.change(stat, state.changes().get(stat), WORDING)));
        return new SheetIcon(face, TooltipKey.ofTerm("condition", state.term()),
                TermSheets.of(state.term(), state.detail(), rows));
    }

    private static List<SheetIcon> appliesIcons(TowerInspection tower) {
        return tower.applies().stream().map(TowerSheetText::appliesIcon).toList();
    }

    private static SheetIcon appliesIcon(AppliedEffect applied) {
        List<SheetLine> rows = new ArrayList<>();
        rows.add(new SheetLine.Divider());
        applied.sources().forEach(source -> rows.add(SheetLine.Sentence.of(
                TermText.of(source.name() + ": ").plus(source.how()).asSentence())));
        return new SheetIcon(TermLooks.faceOf(applied.term()), TooltipKey.ofTerm("applies", applied.term()),
                TermSheets.of(applied.term(), applied.term().describe(), rows));
    }

    /** An owned node's change: the change only, or what it hands the towers around it. */
    static Row bonusRow(UpgradeBonus bonus) {
        Row row = StatRowText.change(bonus.stat(), bonus.change(), WORDING);
        return bonus.given() ? row.withGloss("", GIVEN) : row;
    }
}
