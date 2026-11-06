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
        ControlFace face = ControlFace.button().withSelectedColor(VOICE);
        DefaultButtonModel model = new DefaultButtonModel();

        Color idle = Hud.borderFor(face, model, true);
        model.setSelected(true);
        Color selected = Hud.borderFor(face, model, true);

        assertThat(idle).isEqualTo(Hud.BORDER_IDLE);
        assertThat(selected).isEqualTo(VOICE);
    }

    @Test
    void aSelectedButtonIsFilledWithItsColourUnderDarkText() {
        ControlFace face = ControlFace.button().withSelectedColor(VOICE);
        DefaultButtonModel model = new DefaultButtonModel();

        model.setSelected(true);

        assertThat(Hud.fillFor(face, model, true)).isEqualTo(VOICE);
        assertThat(Hud.inkFor(face, model, true, Hud.FOREGROUND)).isEqualTo(Hud.BACKGROUND);
    }

    @Test
    void aSelectedButtonWithNoColourOfItsOwnIsFilledWithTheTextColour() {
        DefaultButtonModel model = new DefaultButtonModel();

        model.setSelected(true);

        assertThat(Hud.fillFor(ControlFace.button(), model, true)).isEqualTo(Hud.FOREGROUND);
    }

    @Test
    void aSelectedButtonHeldDownKeepsLightTextOnThePressedFill() {
        ControlFace face = ControlFace.button();
        DefaultButtonModel model = new DefaultButtonModel();
        model.setSelected(true);

        model.setArmed(true);
        model.setPressed(true);

        assertThat(Hud.fillFor(face, model, true)).isEqualTo(Hud.FILL_PRESSED);
        assertThat(Hud.inkFor(face, model, true, Hud.FOREGROUND)).isEqualTo(Hud.FOREGROUND);
    }

    @Test
    void aSelectedTileKeepsItsQuietFillAndItsText() {
        ControlFace tile = ControlFace.tile(VOICE);
        DefaultButtonModel model = new DefaultButtonModel();

        model.setSelected(true);

        assertThat(Hud.fillFor(tile, model, true)).isEqualTo(Hud.FILL_SELECTED);
        assertThat(Hud.inkFor(tile, model, true, Hud.FOREGROUND)).isEqualTo(Hud.FOREGROUND);
    }

    @Test
    void aDisabledSelectedButtonGoesDimLikeAnyDisabledControl() {
        ControlFace face = ControlFace.button();
        DefaultButtonModel model = new DefaultButtonModel();

        model.setSelected(true);

        assertThat(Hud.fillFor(face, model, false)).isEqualTo(Hud.BACKGROUND);
        assertThat(Hud.inkFor(face, model, false, Hud.FOREGROUND)).isEqualTo(Hud.TEXT_DISABLED);
    }
}
