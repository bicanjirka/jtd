package td.ui;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.tower.AbstractTower;
import td.tower.TowerFactory;
import td.tower.TowerTerm;
import td.tower.upgrade.UpgradeNode;
import td.ui.render.IconFace;
import td.ui.render.Palette;
import td.util.GameHost;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class TermLooksTest {

    private static UpgradeNode nodeOf(TowerFactory.Type type, String name) {
        AbstractTower tower = (AbstractTower) TowerFactory.createTower(type, new GameWorld(GameHost.noOp()), 0, 0);
        return tower.upgradeTree().nodes().stream().filter(node -> node.displayName().equals(name)).findFirst()
                .orElseThrow();
    }

    @Test
    void anEffectsToneIsItsBoardMarker() {
        IconFace face = TermLooks.faceOf(EffectKind.CHILL);

        assertThat(face.tone()).isEqualTo(EnemyFrameBuilder.markerPaletteFor(EffectKind.CHILL));
        assertThat(face.pictogram()).isEqualTo("effect.chill");
    }

    @Test
    void anEffectsFrameIsItsGroup() {
        assertThat(TermLooks.faceOf(EffectKind.CHILL).frame()).isEqualTo(Palette.TERM_FRAME_CONTROL);
        assertThat(TermLooks.faceOf(EffectKind.BURN).frame()).isEqualTo(Palette.TERM_FRAME_AFFLICTION);
        assertThat(TermLooks.faceOf(EffectKind.SHIELD).frame()).isEqualTo(Palette.TERM_FRAME_BOON);
        assertThat(TermLooks.faceOf(EffectKind.EXPOSED).frame()).isEqualTo(Palette.TERM_FRAME_MARK);
        assertThat(TermLooks.faceOf(EffectKind.DOOM).frame()).isEqualTo(Palette.TERM_FRAME_HEX);
        assertThat(TermLooks.faceOf(EffectKind.SUNDERED).frame()).isEqualTo(Palette.TERM_FRAME_WEAKNESS);
    }

    @Test
    void aTowerBuffAndDebuffHaveTheirFrames() {
        assertThat(TermLooks.faceOf(TowerTerm.STEADY_AIM).frame()).isEqualTo(Palette.TERM_FRAME_TOWER_BUFF);
        assertThat(TermLooks.faceOf(TowerTerm.JAMMED).frame()).isEqualTo(Palette.TERM_FRAME_TOWER_DEBUFF);
        assertThat(TermLooks.faceOf(TowerTerm.STOKE).frame()).isEqualTo(Palette.TERM_FRAME_MECHANIC);
    }

    @Test
    void aBurstArcLineTakesTheStormcallerBody() {
        UpgradeNode arc = nodeOf(TowerFactory.Type.SPLASH, "Arc");

        IconFace face = TermLooks.faceOfLine(TowerFactory.Type.SPLASH, arc, 1);

        assertThat(face.tone()).isEqualTo(Palette.TOWER_STORMCALLER_BODY);
    }

    @Test
    void aLineFaceShowsItsLevelAsARomanNumeral() {
        UpgradeNode attune = nodeOf(TowerFactory.Type.SNIPER, "Attune");

        assertThat(TermLooks.faceOfLine(TowerFactory.Type.SNIPER, attune, 3).numeral()).isEqualTo("III");
        assertThat(TermLooks.faceOfLine(TowerFactory.Type.SNIPER, attune, 0).numeral()).isEmpty();
    }
}
