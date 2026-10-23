package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.WorldFixtures;
import td.level.BuiltInLevelCatalog;
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
        return spawn(world, id, Rank.GRUNT);
    }

    private static DefinedEnemyMob spawn(GameWorld world, String id, Rank rank) {
        EnemyDefinition definition = world.getEnemyCatalog().get(id, rank);
        DefinedEnemyMob mob = (DefinedEnemyMob) world.getEnemyCatalog().spawn(id, world, 0, definition.baseHealth(),
                definition.price(), rank);
        world.enemies().add(mob);
        return mob;
    }

    @Test
    void everyNewEnemyIsInTheCatalogWithAGrowingLadder() {
        EnemyCatalog catalog = EnemyCatalog.builtIn();

        for (String id : List.of("juggernaut", "mite", "courier", "shieldbearer", "salamander", "yeti",
                "skirmisher")) {
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
        assertThat(magic.amount()).isPositive();
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
        assertThat(catalog.get("courier").price()).isGreaterThan(catalog.get("c").price() * 2);
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
    void anEliteMenderOnCurlyPathWardsItsAlliesWithAShieldThatStopsEveryDamageType() {
        GameWorld world = worldWithStraightPath();
        new BuiltInLevelCatalog().levels().getFirst().customRankedEnemies().forEach(world.getEnemyCatalog()::register);
        DefinedEnemyMob mender = spawn(world, "mender", Rank.ELITE);
        DefinedEnemyMob ally = spawn(world, "c", Rank.GRUNT);

        for (int t = 1; t <= 100; t++) {
            mender.doTick(t);
            ally.doTick(t);
        }

        assertThat(ally.activeEffects()).filteredOn(effect -> effect.kind() == EffectKind.SHIELD)
                .extracting(Effect::shieldRestrictedTo)
                .containsExactly(Optional.empty());
    }

    @Test
    void aSalamanderCannotBeSetAlightButAMobNextToItCan() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob salamander = spawn(world, "salamander");
        DefinedEnemyMob plain = spawn(world, "c");

        salamander.applyEffect(Effect.burn(Damage.magic(100), 100, DamageSink.none()));
        plain.applyEffect(Effect.burn(Damage.magic(100), 100, DamageSink.none()));

        assertThat(salamander.hasEffect(EffectKind.BURN)).isFalse();
        assertThat(plain.hasEffect(EffectKind.BURN)).isTrue();
    }

    @Test
    void aYetiIsImmuneToFreezeAndResistsHalfOfAChill() {
        GameWorld world = worldWithStraightPath();
        DefinedEnemyMob yeti = spawn(world, "yeti");

        yeti.applyEffect(Effect.freeze(40, DamageSink.none()));

        assertThat(yeti.hasEffect(EffectKind.FREEZE)).isFalse();
        assertThat(yeti.stats().value(EnemyStat.CHILL_RESIST)).isEqualTo(0.5f);
    }

    @Test
    void theCourierIsNimble() {
        DefinedEnemyMob courier = spawn(worldWithStraightPath(), "courier");

        assertThat(courier.inspect().stat(EnemyStat.AGILITY)).isEqualTo(25f);
    }

    @Test
    void aSkirmisherSoldierSprintsOffAfterItDodges() {
        GameWorld world = WorldFixtures.newWorldOnBoard(() -> 0.0, 32, 1000, 1000);
        world.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(1000, 0))));
        EnemyDefinition soldier = world.getEnemyCatalog().ranked("skirmisher").definitionFor(Rank.SOLDIER);
        DefinedEnemyMob skirmisher = (DefinedEnemyMob) world.getEnemyCatalog().spawn("skirmisher", world, 0,
                soldier.baseHealth(), soldier.price(), Rank.SOLDIER);
        world.enemies().add(skirmisher);
        skirmisher.doTick(1);
        float walking = skirmisher.getSpeed();

        boolean dodged = skirmisher.takeHit(Damage.physical(100), AttackProfile.none().asAimed()).dodged();
        skirmisher.doTick(2);

        assertThat(dodged).isTrue();
        assertThat(skirmisher.hasEffect(EffectKind.SPRINTING)).isTrue();
        assertThat(skirmisher.getSpeed()).isGreaterThan(walking);
    }
}
