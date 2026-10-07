package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** The enemies that each ask a defence for something: the plated, the fragile, the shielding, the immune. */
class RoleEnemyTest {

    private static GameWorld worldWithStraightPath() {
        GameWorld world = WorldFixtures.newWorldOnBoard(32, 1000, 1000);
        world.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(1000, 0))));
        return world;
    }

    private static DefinedEnemyMob spawn(GameWorld world, String id) {
        EnemyDefinition definition = world.getEnemyCatalog().get(id);
        DefinedEnemyMob mob = (DefinedEnemyMob) world.getEnemyCatalog().spawn(id, world, 0, definition.baseHealth(),
                definition.price(), Rank.GRUNT);
        world.enemies().add(mob);
        return mob;
    }

    @Test
    void everyNewEnemyIsInTheCatalogWithAGrowingLadder() {
        EnemyCatalog catalog = EnemyCatalog.builtIn();

        for (String id : List.of("juggernaut", "mite", "courier", "shieldbearer", "salamander", "yeti")) {
            assertThat(catalog.contains(id)).as(id).isTrue();
            assertThat(catalog.ranked(id).definitionFor(Rank.BOSS).baseHealth())
                    .as(id).isGreaterThan(catalog.ranked(id).definitionFor(Rank.GRUNT).baseHealth());
        }
    }

    @Test
    void theJuggernautsPlatingStopsAPhysicalHitButNotAMagicOne() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob juggernaut = spawn(world, "juggernaut");
        int hit = DamageUnits.ofPoints(20);

        Damage physical = juggernaut.doDamage(Damage.physical(hit));
        Damage magic = juggernaut.doDamage(Damage.magic(hit));

        assertThat(physical.amount()).isZero();
        assertThat(magic.amount()).isEqualTo(hit);
    }

    @Test
    void aMiteDiesToASingleSniperShot() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob mite = spawn(world, "mite");

        mite.doDamage(Damage.physical(DamageUnits.ofPoints(40)));

        assertThat(mite.isDead()).isTrue();
    }

    @Test
    void aCourierIsFarFasterThanASimpleMob() {
        EnemyCatalog catalog = EnemyCatalog.builtIn();

        assertThat(catalog.get("courier").baseSpeed()).isGreaterThan(catalog.get("c").baseSpeed() * 2);
        assertThat(catalog.get("courier").price()).isGreaterThan(catalog.get("c").price() * 4);
    }

    @Test
    void aShieldbearerShieldsItsAlliesAgainstPhysicalDamageOnlyAndNeverItself() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob bearer = spawn(world, "shieldbearer");
        DefinedEnemyMob ally = spawn(world, "c");

        for (int t = 1; t <= 100; t++) {
            bearer.doTick(t);
        }

        assertThat(ally.activeEffects()).filteredOn(effect -> effect.kind() == EffectKind.SHIELD)
                .extracting(Effect::shieldRestrictedTo)
                .containsExactly(Optional.of(DamageType.PHYSICAL));
        assertThat(bearer.activeEffectKinds()).doesNotContain(EffectKind.SHIELD);
    }

    @Test
    void aSalamanderCannotBeSetAlightButAMobNextToItCan() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob salamander = spawn(world, "salamander");
        DefinedEnemyMob plain = spawn(world, "c");

        salamander.applyEffect(Effect.burn(Damage.magic(100), 100, d -> {
        }));
        plain.applyEffect(Effect.burn(Damage.magic(100), 100, d -> {
        }));

        assertThat(salamander.hasEffect(EffectKind.BURN)).isFalse();
        assertThat(plain.hasEffect(EffectKind.BURN)).isTrue();
    }

    @Test
    void aYetiIsImmuneToFreezeAndResistsHalfOfAChill() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob yeti = spawn(world, "yeti");

        yeti.applyEffect(Effect.freeze(40, d -> {
        }));

        assertThat(yeti.hasEffect(EffectKind.FREEZE)).isFalse();
        assertThat(yeti.stats().value(EnemyStat.CHILL_RESIST)).isEqualTo(0.5f);
    }
}
