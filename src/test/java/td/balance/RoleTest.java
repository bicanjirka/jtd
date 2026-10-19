package td.balance;

import org.junit.jupiter.api.Test;
import td.GameEngine;
import td.economy.EconomyDelta;
import td.level.BuiltInLevelCatalog;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.util.GameHost;
import td.util.RandomSource;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void everyNodeARoleBansIsANodeSomeTowerOffers() {
        GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(1));
        engine.loadLevel(new BuiltInLevelCatalog().levels().getFirst());
        Set<String> offered = Arrays.stream(TowerFactory.Type.values())
                .flatMap(type -> TowerFactory.createTower(type, engine.getGameWorld(), 0, 0).upgradeTree().nodes()
                        .stream())
                .map(UpgradeNode::id).collect(Collectors.toSet());

        Set<String> banned = Arrays.stream(Role.values()).flatMap(role -> role.bannedNodes().stream())
                .collect(Collectors.toSet());

        assertThat(offered).containsAll(banned);
    }

    @Test
    void aWardObeliskDoesTheAntiSustainJobAndAPlainOneDoesNot() {
        GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(1));
        engine.loadLevel(new BuiltInLevelCatalog().levels().getFirst());
        Tower plain = TowerFactory.createTower(TowerFactory.Type.PULSE, engine.getGameWorld(), 0, 0);
        Tower ward = TowerFactory.createTower(TowerFactory.Type.PULSE, engine.getGameWorld(), 0, 0);
        engine.getGameWorld().economy().apply(EconomyDelta.credits(1000));
        ward.upgradeTree().nodes().stream().filter(node -> node.id().equals("pulse.head.ward.1")).findFirst()
                .ifPresent(sight -> ward.upgradeTree().pathTo(sight).forEach(ward::buyUpgrade));

        boolean plainDoes = Role.ANTI_SUSTAIN.doneBy(plain);
        boolean wardDoes = Role.ANTI_SUSTAIN.doneBy(ward);

        assertThat(plainDoes).isFalse();
        assertThat(wardDoes).isTrue();
    }
}
