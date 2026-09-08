package td.ui;

import td.level.LevelDefinition;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.List;
import java.util.function.Consumer;

/**
 * The landing screen shown before any level is loaded: one clickable card per
 * level, its name bold with its description underneath. Presentation only -
 * clicking a card just invokes the callback given at construction, leaving it
 * to {@link td.TowerDefense} to actually load and start that level.
 */
public class PanelLevelSelect extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Color BACKGROUND = new Color(0, 0, 0);
    private static final Color FOREGROUND = new Color(220, 255, 220);
    private static final Color CARD_BACKGROUND = new Color(30, 30, 30);
    private static final Color CARD_HOVER_BACKGROUND = new Color(50, 50, 50);

    public PanelLevelSelect(List<LevelDefinition> levels, Consumer<LevelDefinition> onLevelSelected) {
        this.setLayout(new BorderLayout());
        this.setBackground(BACKGROUND);
        this.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("Tower Defense", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(FOREGROUND);
        title.setBorder(new EmptyBorder(0, 0, 16, 0));
        this.add(title, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(0, 1, 0, 12));
        cards.setBackground(BACKGROUND);
        for (LevelDefinition level : levels) {
            cards.add(buildCard(level, onLevelSelected));
        }
        this.add(cards, BorderLayout.CENTER);
    }

    private static JPanel buildCard(LevelDefinition level, Consumer<LevelDefinition> onLevelSelected) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FOREGROUND),
                new EmptyBorder(12, 16, 12, 16)));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel name = new JLabel(level.name());
        name.setFont(new Font("SansSerif", Font.BOLD, 16));
        name.setForeground(FOREGROUND);
        card.add(name, BorderLayout.NORTH);

        JLabel description = new JLabel(level.description());
        description.setForeground(FOREGROUND);
        card.add(description, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onLevelSelected.accept(level);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(CARD_HOVER_BACKGROUND);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(CARD_BACKGROUND);
            }
        });

        return card;
    }
}
