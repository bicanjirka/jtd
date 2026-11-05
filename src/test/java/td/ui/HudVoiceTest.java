package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.DefaultButtonModel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class HudVoiceTest {

    private static final Color VOICE = new Color(10, 20, 30);

    @Test
    void aPanelsTitleTakesTheColourItIsGiven() {
        JPanel panel = new JPanel();
        panel.setBorder(Hud.panelBorder("Title"));

        Hud.setPanelTitleColor(panel, VOICE);

        assertThat(((TitledBorder) panel.getBorder()).getTitleColor()).isEqualTo(VOICE);
    }

    @Test
    void aSelectedButtonWearsItsFacesSelectedBorderOnlyWhileSelected() {
        ControlFace face = ControlFace.button().withSelectedBorder(VOICE);
        DefaultButtonModel model = new DefaultButtonModel();

        Color idle = Hud.borderFor(face, model, true);
        model.setSelected(true);
        Color selected = Hud.borderFor(face, model, true);

        assertThat(idle).isEqualTo(Hud.BORDER_IDLE);
        assertThat(selected).isEqualTo(VOICE);
    }
}
