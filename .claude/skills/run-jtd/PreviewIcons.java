import td.effect.EffectKind;
import td.enemy.AbilityTerm;
import td.enemy.TraitTerm;
import td.term.Term;
import td.tower.AbstractTower;
import td.tower.TowerFactory;
import td.tower.TowerTerm;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.ui.TermIconPainter;
import td.ui.TermLooks;
import td.ui.render.IconFace;
import td.util.GameHost;
import td.util.GameWorld;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Headless contact sheet of every term's icon face, at 40 px and at 20 px with its title under it, in a section
 * per term family and one for the perk lines. For "does this symbol read" checks without starting the game.
 * <p>
 * {@code java -cp "$CP" PreviewIcons [out.png]}
 */
public class PreviewIcons {

    private static final int COLUMNS = 8;
    private static final int CELL_W = 150;
    private static final int CELL_H = 62;
    private static final int HEADER = 26;
    private static final Set<UpgradeTier> ART_TIERS = Set.of(UpgradeTier.RANGE_1, UpgradeTier.ATTUNE,
            UpgradeTier.HEAD_1, UpgradeTier.EXTRA_1, UpgradeTier.HEAD_4, UpgradeTier.SPECIAL);

    private record Entry(String title, IconFace face) {
    }

    private record Section(String name, List<Entry> entries) {
    }

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : ".claude/skills/run-jtd/shots/icons.png";
        List<Section> sections = new ArrayList<>();
        sections.add(terms("Effects", Arrays.asList(EffectKind.values())));
        sections.add(terms("Tower terms", Arrays.asList(TowerTerm.values())));
        sections.add(terms("Traits", Arrays.asList(TraitTerm.values())));
        sections.add(terms("Abilities", Arrays.asList(AbilityTerm.values())));
        sections.add(terms("Status", Arrays.asList(td.economy.StatusTerm.values())));
        sections.add(lines());
        render(sections, out);
        System.out.println("Wrote " + out);
    }

    private static Section terms(String name, List<? extends Term> terms) {
        return new Section(name, terms.stream().map(t -> new Entry(t.title(), TermLooks.faceOf(t))).toList());
    }

    private static Section lines() {
        List<Entry> entries = new ArrayList<>();
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            AbstractTower tower = (AbstractTower) TowerFactory.createTower(type, new GameWorld(GameHost.noOp()), 0, 0);
            for (UpgradeNode node : tower.upgradeTree().nodes()) {
                if (ART_TIERS.contains(node.tier())) {
                    entries.add(new Entry(node.displayName(), TermLooks.faceOfLine(type, node, node.tier().level())));
                }
            }
        }
        return new Section("Perk lines", entries);
    }

    private static void render(List<Section> sections, String out) throws Exception {
        int height = 0;
        for (Section section : sections) {
            height += HEADER + ((section.entries().size() + COLUMNS - 1) / COLUMNS) * CELL_H;
        }
        BufferedImage image = new BufferedImage(COLUMNS * CELL_W, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        int y = 0;
        for (Section section : sections) {
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
            g.drawString(section.name(), 6, y + 18);
            y += HEADER;
            for (int i = 0; i < section.entries().size(); i++) {
                Entry entry = section.entries().get(i);
                int x = (i % COLUMNS) * CELL_W + 6;
                int top = y + (i / COLUMNS) * CELL_H;
                Graphics2D big = (Graphics2D) g.create(x, top + 2, 44, 44);
                TermIconPainter.paint(big, entry.face(), 40);
                big.dispose();
                Graphics2D small = (Graphics2D) g.create(x + 50, top + 2, 24, 40);
                TermIconPainter.paint(small, entry.face(), 20);
                small.dispose();
                g.setColor(new Color(210, 210, 210));
                g.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
                g.drawString(entry.title(), x, top + 58);
            }
            y += ((section.entries().size() + COLUMNS - 1) / COLUMNS) * CELL_H;
        }
        g.dispose();
        File file = new File(out);
        file.getParentFile().mkdirs();
        ImageIO.write(image, "png", file);
    }
}
