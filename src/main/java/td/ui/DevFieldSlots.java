package td.ui;

import td.DevField;
import td.util.ThreadConfined;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Three labelled number fields that take on the {@link DevField fields} of the item chosen beside them. A slot the
 * item doesn't use stands empty but keeps its room, so the controls after it never move. What was typed for an item
 * comes back when it is chosen again.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class DevFieldSlots<T> {

    private static final int SLOTS = DevField.MOST_PER_ENTRY;
    private static final int FIELD_COLUMNS = 4;
    private static final int GAP = 4;

    private final Function<T, List<DevField>> fieldsOf;
    private final List<JLabel> labels = new ArrayList<>();
    private final List<HudTextField> fields = new ArrayList<>();
    private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final Map<T, List<String>> typed = new HashMap<>();

    private T shown;

    /** {@code items} are every item that may be chosen, so each slot is as wide as the widest label any of them has. */
    DevFieldSlots(List<T> items, Function<T, List<DevField>> fieldsOf, T initial) {
        this.fieldsOf = fieldsOf;
        this.panel.setOpaque(false);
        FontMetrics metrics = this.panel.getFontMetrics(Hud.LABEL_FONT);
        int labelWidth = items.stream().flatMap(item -> fieldsOf.apply(item).stream())
                .mapToInt(field -> metrics.stringWidth(field.label())).max().orElse(0);
        for (int slot = 0; slot < SLOTS; slot++) {
            JLabel label = new JLabel("", SwingConstants.RIGHT);
            label.setFont(Hud.LABEL_FONT);
            label.setForeground(Hud.FOREGROUND);
            HudTextField field = new HudTextField("", FIELD_COLUMNS);
            label.setPreferredSize(new Dimension(labelWidth, field.getPreferredSize().height));
            JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT, GAP, 0));
            holder.setOpaque(false);
            holder.add(label);
            holder.add(field);
            holder.setPreferredSize(holder.getPreferredSize());
            this.panel.add(holder);
            this.labels.add(label);
            this.fields.add(field);
        }
        this.show(initial);
    }

    JComponent component() {
        return this.panel;
    }

    /** The slots' text fields, so the panel can bind Enter and Esc on each. */
    List<HudTextField> textFields() {
        return List.copyOf(this.fields);
    }

    /** Takes on {@code item}'s fields, with what was last typed for it, or their starting values. */
    void show(T item) {
        if (item.equals(this.shown)) {
            return;
        }
        if (this.shown != null) {
            this.typed.put(this.shown, this.texts());
        }
        this.shown = item;
        List<DevField> wanted = this.fieldsOf.apply(item);
        List<String> texts = this.typed.getOrDefault(item, wanted.stream().map(DevField::initialText).toList());
        for (int slot = 0; slot < SLOTS; slot++) {
            boolean used = slot < wanted.size();
            this.labels.get(slot).setText(used ? wanted.get(slot).label() : "");
            this.labels.get(slot).setVisible(used);
            this.fields.get(slot).setText(used ? texts.get(slot) : "");
            this.fields.get(slot).setVisible(used);
        }
    }

    /** The shown item's labels, in order: what a reader of the row sees. */
    List<String> labels() {
        return this.labels.stream().filter(JComponent::isVisible).map(JLabel::getText).toList();
    }

    /** The numbers typed in the used slots, in order; empty after telling {@code problems} about one that isn't a number. */
    Optional<List<Float>> values(Consumer<String> problems) {
        List<Float> values = new ArrayList<>();
        for (int slot = 0; slot < this.fieldsOf.apply(this.shown).size(); slot++) {
            String text = this.fields.get(slot).getText().trim();
            try {
                values.add(Float.parseFloat(text));
            } catch (NumberFormatException e) {
                problems.accept("Not a number: " + text);
                return Optional.empty();
            }
        }
        return Optional.of(values);
    }

    private List<String> texts() {
        return this.fields.stream().map(HudTextField::getText).toList();
    }
}
