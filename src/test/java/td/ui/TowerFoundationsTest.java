package td.ui;

import org.junit.jupiter.api.Test;
import td.ui.render.LoadedShell;
import td.ui.render.TowerCue;
import td.ui.render.TowerLook;
import td.ui.render.TowerPose;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TowerFoundationsTest {

    @Test
    void aTableMissingALookIsRejectedByName() {
        Map<TowerLook, String> rows = new EnumMap<>(TowerLook.class);
        for (TowerLook look : TowerLook.values()) {
            rows.put(look, look.name());
        }
        rows.remove(TowerLook.SEEKER);

        assertThatThrownBy(() -> TowerFoundations.checkedComplete(rows))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(TowerLook.SEEKER.name());
    }

    @Test
    void everyLooksFoundationNeverShrinksAndEndsWiderThanItStarts() {
        for (TowerLook look : TowerLook.values()) {
            for (int tier = 1; tier < TowerFoundations.TIERS; tier++) {
                TowerFoundations.Foundation lower = TowerFoundations.of(look, tier - 1);
                TowerFoundations.Foundation upper = TowerFoundations.of(look, tier);

                assertThat(upper.footprint()).as("%s tier %d", look, tier).isGreaterThanOrEqualTo(lower.footprint());
                assertThat(upper.parts()).as("%s tier %d", look, tier).isNotEmpty();
            }
            assertThat(TowerFoundations.of(look, TowerFoundations.TIERS - 1).footprint()).as(look.name())
                    .isGreaterThan(TowerFoundations.of(look, 0).footprint());
        }
    }

    @Test
    void aLookWhoseBodyIsStillBuildsTheSameBodyWhateverItHolds() {
        for (TowerLook look : TowerLook.values()) {
            if (!TowerFoundations.stillBody(look)) {
                continue;
            }
            for (int tier = 0; tier < TowerFoundations.TIERS; tier++) {
                TowerFoundations.Foundation idle = TowerFoundations.of(look, tier);
                for (TowerPose pose : poses()) {
                    TowerFoundations.Foundation posed = TowerFoundations.of(look, tier, pose);
                    assertThat(outlines(posed.parts())).as("%s tier %d body in %s", look, tier, pose)
                            .isEqualTo(outlines(idle.parts()));
                    assertThat(outlines(posed.front())).as("%s tier %d front in %s", look, tier, pose)
                            .isEqualTo(outlines(idle.front()));
                }
            }
        }
    }

    @Test
    void everyLookIsBuiltAtEveryTierInEveryPose() {
        for (TowerLook look : TowerLook.values()) {
            for (int tier = 0; tier < TowerFoundations.TIERS; tier++) {
                for (TowerPose pose : poses()) {
                    assertThat(TowerFoundations.of(look, tier, pose).parts()).as("%s tier %d in %s", look, tier, pose)
                            .isNotEmpty();
                }
            }
        }
    }

    @Test
    void aLookThatAimsShootsFromPastItsCentreAndOneThatDoesNotFromItsTop() {
        for (TowerLook look : TowerLook.values()) {
            TowerFoundations.Foundation built = TowerFoundations.of(look, 0);
            if (built.aims()) {
                assertThat(built.muzzle()).as(look.name()).isPositive();
            } else {
                assertThat(built.muzzle()).as(look.name()).isZero();
                assertThat(built.muzzleHeight()).as(look.name()).isEqualTo(built.top());
            }
        }
    }

    @Test
    void posesThatDifferByLessThanAStepShareOneBuild() {
        TowerFoundations.Foundation one = TowerFoundations.of(TowerLook.MORTAR, 1, TowerPose.idle().refilling(0.52f));
        TowerFoundations.Foundation other = TowerFoundations.of(TowerLook.MORTAR, 1, TowerPose.idle().refilling(0.46f));

        assertThat(one).isSameAs(other);
    }

    @Test
    void aTierAboveTheLastWearsTheLastsFoundation() {
        TowerFoundations.Foundation last = TowerFoundations.of(TowerLook.SNIPER, TowerFoundations.TIERS - 1);

        assertThat(TowerFoundations.of(TowerLook.SNIPER, TowerFoundations.TIERS + 2)).isSameAs(last);
    }

    /** One pose per cue, every shell, and the moments of a shot. */
    private static List<TowerPose> poses() {
        List<TowerPose> poses = new ArrayList<>();
        for (TowerCue cue : TowerCue.values()) {
            poses.add(TowerPose.idle().with(cue));
        }
        for (LoadedShell shell : LoadedShell.values()) {
            poses.add(TowerPose.idle().withShell(shell).with(TowerCue.VOLLEY));
        }
        poses.add(TowerPose.idle().fired());
        poses.add(TowerPose.idle().refilling(0f));
        poses.add(TowerPose.idle().reloading(0.5f));
        poses.add(TowerPose.idle().withStacks(3));
        poses.add(TowerPose.idle().withPanes(List.of(TowerLook.SNIPER, TowerLook.MORTAR)));
        return poses;
    }

    private static List<String> outlines(List<TowerFoundations.Part> parts) {
        return parts.stream().map(part -> part.kind() + " " + part.z0() + " " + part.height() + " "
                + part.shape().getBounds2D()).toList();
    }
}
