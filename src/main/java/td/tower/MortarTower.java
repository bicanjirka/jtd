package td.tower;

import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.projectile.CannonballProjectile;
import td.projectile.ProjectileStats;
import td.tower.buff.TowerBuff;
import td.tower.mortar.AirburstPerk;
import td.tower.mortar.BarragePerk;
import td.tower.mortar.BlastMark;
import td.tower.mortar.BombletSpec;
import td.tower.mortar.BracketDamagePerk;
import td.tower.mortar.BracketTracker;
import td.tower.mortar.BracketingPerk;
import td.tower.mortar.BunkerBusterPerk;
import td.tower.mortar.CarpetBombingPerk;
import td.tower.mortar.ClusterShellPerk;
import td.tower.mortar.FragmentationPerk;
import td.tower.mortar.HeavyShellPerk;
import td.tower.mortar.LongBatteryPerk;
import td.tower.mortar.MortarPerk;
import td.tower.mortar.MortarSpec;
import td.tower.mortar.NukeFlash;
import td.tower.mortar.PathLine;
import td.tower.mortar.PredictiveFirePerk;
import td.tower.mortar.RifledBarrelPerk;
import td.tower.mortar.SalvoSpec;
import td.tower.mortar.ShellType;
import td.tower.mortar.ShellTypePerk;
import td.tower.mortar.ShrapnelBoostPerk;
import td.tower.mortar.ShrapnelSpec;
import td.tower.mortar.ShrapnelStormPerk;
import td.tower.mortar.TacticalNukePerk;
import td.tower.mortar.WiderBlastPerk;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.BaseSlotPerks;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.OwnedPerks;
import td.tower.upgrade.PerkCatalogue;
import td.tower.upgrade.PurposeCondition;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.PathRuntime;
import td.util.ThreadConfined;
import td.wave.Path;
import td.wave.Vec2;
import td.zone.Zone;
import td.zone.ZoneEffects;
import td.zone.ZoneOwner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The Artillery: lobs a slow, unguided shell at the visible enemy furthest along the path, beyond a
 * dead zone around it. The shell flies to where the enemy was when fired, so a fast enemy dodges it.
 * The blast deals physical damage with distance falloff and cracks the plating of everything it
 * reaches.
 * <p>
 * Attuned, a shell that lands near the last one hits harder and wider (Bracketing). What each owned
 * node does lives in a {@link MortarPerk}: the tower reads one spec from them whenever it looks for a
 * target or lands a shell.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class MortarTower extends AbstractTower {

    public static final int PRICE = 30;
    public static final float DAMAGE_POINTS = 32f;
    public static final float RANGE = 4.5f;
    public static final float SPLASH_RADIUS_BASE = 1.75f;
    /** Pixels a tick: slow enough to watch a shell fly and for a fast enemy to dodge it. */
    public static final float SHELL_SPEED = 8f;
    /** Ticks a nuke's flash and ring last. */
    public static final int NUKE_FLASH_TICKS = 10;
    /** Ticks the mark of any blast stays on the board. */
    public static final int BLAST_MARK_TICKS = 6;

    private static final int COOLDOWN_MAX = 70;
    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;
    private static final int CRACKED_TICKS = 100;
    /** A shell's drawn size grows with the square root of how much harder than base it hits. */
    private static final float SHELL_SIZE_PER_DAMAGE_ROOT = 1f;
    private static final float MAX_SHELL_SIZE = 2.5f;
    private static final float NUKE_SHELL_SIZE = 1.5f;
    /** Shrapnel reaches this far past the main blast, as a multiple of its radius. */
    private static final float SHRAPNEL_RING_MULTIPLIER = 1.75f;
    /** What a bleed costs per cell travelled, as a share of the Mortar's damage. */
    private static final float BLEED_SHARE_PER_CELL = 0.08f;
    private static final int BLEED_TICKS = 80;
    /** Airburst: inside this share of the radius, squared, a blast takes no falloff: the inner half. */
    private static final double FLAT_CORE_SHARE = 0.25;
    /** Flight time depends on where the shell is going, so the aim is refined this many times. */
    private static final int PREDICTION_PASSES = 3;
    /** How far apart bomblets lie along the path, in cells, and how far from the impact the first one is. */
    private static final float BOMBLET_SPACING_CELLS = 0.8f;
    private static final float BOMBLET_SCATTER_CELLS = 0.8f;

    private static final String BRACKET_DEED = "Bracketed shells";
    private static final int BRACKETED_SHELLS_NEEDED = 15;

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none()
            .withAttune("Bracketing: a shell landing within 1.5 cells of the last gets +10% damage and radius, "
                    + "up to 3 steps; a shell elsewhere resets it")
            .withRangeThree(0.3f, "Long Battery: the dead zone grows to 2.5 cells");

    private static final UpgradeNode SIEGE_ROUNDS_1 = UpgradeTier.HEAD_1.node("mortar.head.siege_rounds.1",
            "Siege Rounds", PRICE)
            .withBuff(TowerBuff.damage(0.3f))
            .withExtraEffect("each Bracketing step gives +15% instead of +10%");
    private static final UpgradeNode SIEGE_ROUNDS_2 = UpgradeTier.HEAD_2.node("mortar.head.siege_rounds.2",
            "Siege Rounds II", PRICE)
            .withBuff(TowerBuff.damage(0.25f).withCritChance(0.1f))
            .withExtraEffect("+25% blast radius")
            .after(SIEGE_ROUNDS_1);
    private static final UpgradeNode HEAVY_SHELL = UpgradeTier.HEAD_3.node("mortar.head.siege_rounds.3",
            "Heavy Shell", PRICE)
            .withBuff(TowerBuff.damage(0.5f).withFireRate(-0.2f))
            .withGate(new PurposeCondition(BRACKET_DEED, BRACKETED_SHELLS_NEEDED))
            .withExtraEffect("a bigger, slower shell; enemies within 0.5 cells of the impact are Dazed 0.5s")
            .after(SIEGE_ROUNDS_2);
    private static final UpgradeNode TACTICAL_NUKE = UpgradeTier.HEAD_4.node("mortar.head.siege_rounds.4a",
            "Tactical Nuke", PRICE)
            .withExtraEffect("every 4th shell is a nuke: x4 damage over x1.5 the blast, leaving fallout for 4s over "
                    + "0.8 of the blast; it never carries a special")
            .after(HEAVY_SHELL);
    private static final UpgradeNode BUNKER_BUSTER = UpgradeTier.HEAD_4.node("mortar.head.siege_rounds.4b",
            "Bunker Buster", PRICE)
            .withExtraEffect("the enemy at the centre of the blast takes x3 damage and is Sundered; the blast "
                    + "shrinks 30%")
            .after(HEAVY_SHELL);

    private static final UpgradeNode FRAGMENTATION_ROUNDS_1 = UpgradeTier.HEAD_1.node(
            "mortar.head.fragmentation_rounds.1", "Fragmentation Rounds", PRICE)
            .withExtraEffect("a ring of shrapnel past the blast for 25% damage, cracking plating, reaching 0.25 "
                    + "cells further per Bracketing step");
    private static final UpgradeNode FRAGMENTATION_ROUNDS_2 = UpgradeTier.HEAD_2.node(
            "mortar.head.fragmentation_rounds.2", "Fragmentation Rounds II", PRICE)
            .withExtraEffect("shrapnel does 30% more of everything it does, and applies its shell's own effect")
            .after(FRAGMENTATION_ROUNDS_1);
    private static final UpgradeNode CLUSTER_SHELL = UpgradeTier.HEAD_3.node("mortar.head.fragmentation_rounds.3",
            "Cluster Shell", PRICE)
            .withGate(new PurposeCondition(BRACKET_DEED, BRACKETED_SHELLS_NEEDED))
            .withExtraEffect("4 bomblets scattered along the path around the impact, 40% damage each over 1 cell")
            .after(FRAGMENTATION_ROUNDS_2);
    private static final UpgradeNode CARPET_BOMBING = UpgradeTier.HEAD_4.node("mortar.head.fragmentation_rounds.4a",
            "Carpet Bombing", PRICE)
            .withExtraEffect("8 bomblets in a line along the path ahead of the impact instead")
            .after(CLUSTER_SHELL);
    private static final UpgradeNode SHRAPNEL_STORM = UpgradeTier.HEAD_4.node("mortar.head.fragmentation_rounds.4b",
            "Shrapnel Storm", PRICE)
            .withExtraEffect("what the shrapnel hits bleeds: physical damage for every cell it travels")
            .after(CLUSTER_SHELL);

    private static final UpgradeNode RIFLED_BARREL = UpgradeTier.EXTRA_1.node("mortar.extra.ballistics.1",
            "Rifled Barrel", PRICE)
            .withExtraEffect("shell speed +40%, drawn smaller with a streak");
    private static final UpgradeNode PREDICTIVE_FIRE = UpgradeTier.EXTRA_2.node("mortar.extra.ballistics.2",
            "Predictive Fire", PRICE)
            .withExtraEffect("aims where the enemy will be when the shell lands")
            .after(RIFLED_BARREL);
    private static final UpgradeNode AIRBURST = UpgradeTier.EXTRA_3.node("mortar.extra.ballistics.3", "Airburst",
            PRICE)
            .withExtraEffect("+25% blast radius, and full damage across the inner half of the blast")
            .after(PREDICTIVE_FIRE);
    private static final UpgradeNode BARRAGE = UpgradeTier.EXTRA_4.node("mortar.extra.ballistics.4", "Barrage",
            PRICE)
            .withExtraEffect("the reload takes twice as long, but each salvo is 3 shells 0.5s apart at full damage")
            .after(AIRBURST);

    private static final UpgradeNode NAPALM = UpgradeTier.SPECIAL.node("mortar.special.napalm", "Napalm", PRICE)
            .withExtraEffect("every 3rd shell is Napalm: its blast is magic and leaves burning ground, 1 cell wide, for 3s");
    private static final UpgradeNode TAR = UpgradeTier.SPECIAL.node("mortar.special.tar", "Tar", PRICE)
            .withExtraEffect("every 3rd shell is Tar: it leaves tar, 1.2 cells wide, for 4s");
    private static final UpgradeNode CRYO_SHELLS = UpgradeTier.SPECIAL.node("mortar.special.cryo_shells",
            "Cryo Shells", PRICE)
            .withExtraEffect("every 3rd shell is Cryo: it leaves frost ground, 1.2 cells wide, for 3s");

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, BASE_PERKS, HEAVY_SHELL,
                    CLUSTER_SHELL))
            .with(SIEGE_ROUNDS_1, SIEGE_ROUNDS_2, HEAVY_SHELL, TACTICAL_NUKE, BUNKER_BUSTER,
                    FRAGMENTATION_ROUNDS_1, FRAGMENTATION_ROUNDS_2, CLUSTER_SHELL, CARPET_BOMBING, SHRAPNEL_STORM,
                    RIFLED_BARREL, PREDICTIVE_FIRE, AIRBURST, BARRAGE, NAPALM, TAR, CRYO_SHELLS)
            .withChoice(ExclusiveChoice.oneOf(SIEGE_ROUNDS_1, FRAGMENTATION_ROUNDS_1))
            .withChoice(ExclusiveChoice.oneOf(TACTICAL_NUKE, BUNKER_BUSTER))
            .withChoice(ExclusiveChoice.oneOf(CARPET_BOMBING, SHRAPNEL_STORM))
            .withChoice(ExclusiveChoice.specials(NAPALM, TAR, CRYO_SHELLS));

    private static final PerkCatalogue<MortarPerk> PERKS = PerkCatalogue.<MortarPerk>empty()
            .with(StandardBaseSlot.ATTUNE_ID, BracketingPerk::new)
            .with(StandardBaseSlot.RANGE_3_ID, LongBatteryPerk::new)
            .with(SIEGE_ROUNDS_1.id(), BracketDamagePerk::new)
            .with(SIEGE_ROUNDS_2.id(), WiderBlastPerk::new)
            .with(HEAVY_SHELL.id(), HeavyShellPerk::new)
            .with(TACTICAL_NUKE.id(), TacticalNukePerk::new)
            .with(BUNKER_BUSTER.id(), BunkerBusterPerk::new)
            .with(FRAGMENTATION_ROUNDS_1.id(), FragmentationPerk::new)
            .with(FRAGMENTATION_ROUNDS_2.id(), ShrapnelBoostPerk::new)
            .with(CLUSTER_SHELL.id(), ClusterShellPerk::new)
            .with(CARPET_BOMBING.id(), CarpetBombingPerk::new)
            .with(SHRAPNEL_STORM.id(), ShrapnelStormPerk::new)
            .with(RIFLED_BARREL.id(), RifledBarrelPerk::new)
            .with(PREDICTIVE_FIRE.id(), PredictiveFirePerk::new)
            .with(AIRBURST.id(), AirburstPerk::new)
            .with(BARRAGE.id(), BarragePerk::new)
            .with(NAPALM.id(), () -> new ShellTypePerk(ShellType.NAPALM))
            .with(TAR.id(), () -> new ShellTypePerk(ShellType.TAR))
            .with(CRYO_SHELLS.id(), () -> new ShellTypePerk(ShellType.CRYO));

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final OwnedPerks<MortarPerk> perks = new OwnedPerks<>(PERKS);
    private final BracketTracker bracketing = new BracketTracker();
    private final ZoneOwner zoneOwner = this::applyEffect;
    private final List<BlastMark> blastMarks = new ArrayList<>();
    private final FireClock clock = new FireClock(COOLDOWN_MAX);
    private volatile NukeFlash nukeFlash;
    private int shellsFired;
    private int tickNow;
    private int salvoLeft = 0;
    private int salvoGap = 0;
    private EnemyMob currentTarget;

    public MortarTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.MORTAR, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX), context, x, y);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        this.perks.add(node);
    }

    /** Whom it may shell and how its shells fly and land, as the perks in {@code owned} make it. */
    private MortarSpec spec(List<MortarPerk> owned) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.context.getBoard().scale());
        MortarSpec spec = MortarSpec.from(view);
        for (MortarPerk perk : owned) {
            spec = perk.refineSpec(spec);
        }
        return spec;
    }

    private EnemyMob findTarget(MortarSpec spec) {
        List<EnemyMob> inReach = spec.reach().matching(this.context.enemies());
        return PreferringSelector.priority(new FurthestAlongPathSelector()).selectFrom(inReach).orElse(null);
    }

    public void doTick(int gameTime) {
        this.tickNow = gameTime;
        this.blastMarks.removeIf(mark -> gameTime - mark.startedAtTick() >= BLAST_MARK_TICKS);
        MortarSpec spec = this.spec(this.perks.all());
        if (this.salvoLeft > 0 && --this.salvoGap <= 0) {
            EnemyMob next = this.findTarget(spec);
            if (next != null) {
                this.currentTarget = next;
                this.fireAt(next, spec);
            }
            this.salvoLeft--;
            this.salvoGap = spec.salvo().gapTicks();
        }
        if (this.clock.isReady()) {
            this.currentTarget = this.findTarget(spec);
            if (this.currentTarget != null) {
                this.fireAt(this.currentTarget, spec);
                this.clock.spend(spec.salvo().reloadFactor());
                this.salvoLeft = spec.salvo().shells() - 1;
                this.salvoGap = spec.salvo().gapTicks();
            }
        }
        this.clock.advance(this.fireRateCurrent());
        if (this.currentTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.currentTarget.getX(), this.currentTarget.getY()));
        }
    }

    private void fireAt(EnemyMob target, MortarSpec spec) {
        this.shellsFired++;
        ShellType type = spec.shells().typeOf(this.shellsFired);
        Vec2 aim = this.aimPoint(target, spec);
        this.context.projectiles().add(new CannonballProjectile(this.centerX, this.centerY, aim.x(), aim.y(),
                this.shellStats(spec, type), type.look(), (x, y) -> this.onImpact(x, y, type)));
    }

    /** Where the shell is sent: the enemy where it stands, or with Predictive Fire where it will be on landing. */
    private Vec2 aimPoint(EnemyMob target, MortarSpec spec) {
        Vec2 aim = new Vec2(target.getX(), target.getY());
        if (!spec.leadsTarget()) {
            return aim;
        }
        float speed = SHELL_SPEED * spec.speedScale();
        for (int i = 0; i < PREDICTION_PASSES; i++) {
            int flightTicks = (int) Math.ceil(Math.hypot(aim.x() - this.centerX, aim.y() - this.centerY) / speed);
            aim = target.positionAfter(flightTicks);
        }
        return aim;
    }

    /** The shell as it flies and is drawn: its speed and size are stats, and its size follows how hard it hits. */
    private ProjectileStats shellStats(MortarSpec spec, ShellType type) {
        float size = Math.min(MAX_SHELL_SIZE, SHELL_SIZE_PER_DAMAGE_ROOT
                * (float) Math.sqrt((double) this.damageCurrent() / this.damageBase));
        float typeSize = type == ShellType.NUKE ? NUKE_SHELL_SIZE : 1f;
        return ProjectileStats.of(SHELL_SPEED * spec.speedScale()).withSize(size * spec.sizeScale() * typeSize)
                .withStreak(spec.speedScale() > 1f);
    }

    private float blastRadius(MortarSpec spec, float stepScale) {
        return SPLASH_RADIUS_BASE * this.context.getBoard().scale() * spec.blastScale() * stepScale;
    }

    /** One shell's landing: where, what it carries, and how wide and hard it hits after Bracketing. */
    private record Landing(Vec2 at, ShellType type, MortarSpec spec, Strength strength) {

        double x() {
            return this.at.x();
        }

        double y() {
            return this.at.y();
        }

        int step() {
            return this.strength.step();
        }

        float radius() {
            return this.strength.radius();
        }

        float damage() {
            return this.strength.damage();
        }
    }

    /** How hard and wide a landing hits: its Bracketing step, blast radius in pixels and damage in units. */
    private record Strength(int step, float radius, float damage) {
    }

    private void onImpact(double x, double y, ShellType type) {
        MortarSpec spec = this.spec(this.perks.all());
        int scale = this.context.getBoard().scale();
        int step = this.bracketing.land(x, y, spec.bracket(), scale);
        if (step > 0) {
            this.countDeedOfAttack();
        }
        boolean nuke = type == ShellType.NUKE;
        float radius = this.blastRadius(spec, (1f + step * spec.bracket().radiusStep())
                * (nuke ? spec.nuke().radiusFactor() : 1f));
        float damage = this.damageCurrent() * (1f + step * spec.bracket().damageStep())
                * (nuke ? spec.nuke().damageFactor() : 1f);
        Landing landing = new Landing(new Vec2(x, y), type, spec, new Strength(step, radius, damage));
        List<EnemyMob> hit = this.blast(landing);
        this.dazeAround(landing, hit);
        if (spec.shrapnel().active()) {
            this.shrapnel(landing, hit);
        }
        if (spec.bomblets().isActive()) {
            this.bomblets(landing);
        }
        if (nuke) {
            this.nukeFlash = new NukeFlash(x, y, radius, this.tickNow + 1);
        }
        this.blastMarks.add(new BlastMark(new Vec2(x, y), radius, this.tickNow + 1, type.look()));
        type.zone().ifPresent(zone -> this.context.zones().add(new Zone(zone.kind(), x, y,
                zone.radius(scale, radius), this.lengthened(zone.lifetimeTicks()), this.zoneStrength(zone.damageShare(), 1f),
                this.zoneOwner)));
    }

    private int zoneStrength(float damageShare, float scale) {
        return Math.round(this.damageCurrent() * damageShare * scale);
    }

    /** The main blast: damage with falloff and Cracked on everything inside, the centre hit harder. */
    private List<EnemyMob> blast(Landing landing) {
        MortarSpec spec = landing.spec();
        int scale = this.context.getBoard().scale();
        List<EnemyMob> hit = InRangeTargetQuery.everyone((int) Math.round(landing.x()), (int) Math.round(landing.y()),
                landing.radius()).matching(this.context.enemies());
        EnemyMob centre = spec.centre().isActive()
                ? this.enemyAtCentre(hit, landing.x(), landing.y(), spec.centre().radiusCells() * scale) : null;
        for (EnemyMob enemy : hit) {
            float falloff = falloff(landing.x(), landing.y(), landing.radius(), enemy, spec.flatCore());
            float centreFactor = enemy == centre ? spec.centre().damageFactor() : 1f;
            this.dealDamage(enemy, Damage.of(landing.type().damageType(),
                    Math.round(landing.damage() * falloff * centreFactor)));
            this.applyEffect(enemy, sink -> Effect.cracked(CRACKED_TICKS, sink));
            if (enemy == centre) {
                this.applyStacks(enemy, EffectKind.SUNDERED, spec.centre().sunderStacks());
            }
        }
        return hit;
    }

    /** How much of the damage reaches {@code enemy}: less the further from the centre, none past the radius. */
    private static float falloff(double x, double y, float radius, EnemyMob enemy, boolean flatCore) {
        double dx = x - enemy.getX();
        double dy = y - enemy.getY();
        double share = (dx * dx + dy * dy) / ((double) radius * radius);
        return flatCore && share <= FLAT_CORE_SHARE ? 1f : 1f - (float) share;
    }

    private void dazeAround(Landing landing, List<EnemyMob> hit) {
        if (!landing.spec().daze().isActive()) {
            return;
        }
        float dazeRadius = landing.spec().daze().radiusCells() * this.context.getBoard().scale();
        for (EnemyMob enemy : hit) {
            double dx = landing.x() - enemy.getX();
            double dy = landing.y() - enemy.getY();
            if (dx * dx + dy * dy <= (double) dazeRadius * dazeRadius) {
                this.applyEffect(enemy, sink -> Effect.dazed(landing.spec().daze().ticks(), sink));
            }
        }
    }

    /**
     * The ring of shrapnel past the main blast: a share of the shell's damage with no falloff, Cracked,
     * and, as the perks make it, the shell's own effect and a bleed. A shrapnel piece never leaves a zone.
     */
    private void shrapnel(Landing landing, List<EnemyMob> alreadyHit) {
        ShrapnelSpec shrapnel = landing.spec().shrapnel();
        int scale = this.context.getBoard().scale();
        float reach = landing.radius() * SHRAPNEL_RING_MULTIPLIER + shrapnel.reachCellsPerStep() * scale * landing.step();
        Damage piece = Damage.of(landing.type().damageType(),
                Math.round(landing.damage() * shrapnel.damageShare() * shrapnel.scale()));
        int crackedTicks = Math.round(CRACKED_TICKS * shrapnel.scale());
        for (EnemyMob enemy : InRangeTargetQuery.everyone((int) Math.round(landing.x()), (int) Math.round(landing.y()),
                reach).matching(this.context.enemies())) {
            if (alreadyHit.contains(enemy)) {
                continue;
            }
            this.dealDamage(enemy, piece);
            this.applyEffect(enemy, sink -> Effect.cracked(crackedTicks, sink));
            if (shrapnel.carriesShell()) {
                landing.type().zone().ifPresent(zone -> ZoneEffects.touch(zone.kind(),
                        this.zoneStrength(zone.damageShare(), shrapnel.scale()), this.zoneOwner, enemy));
            }
            if (shrapnel.bleeds()) {
                int perCell = Math.round(this.damageCurrent() * BLEED_SHARE_PER_CELL * shrapnel.scale());
                this.applyEffect(enemy, sink -> Effect.bleeding(perCell, BLEED_TICKS, sink));
            }
        }
    }

    /** Small blasts along the path: scattered around the impact, or in a line ahead of it. */
    private void bomblets(Landing landing) {
        BombletSpec bomblets = landing.spec().bomblets();
        int scale = this.context.getBoard().scale();
        double[] offsets = new double[bomblets.count()];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = bomblets.inLine() ? (i + 1) * BOMBLET_SPACING_CELLS * scale
                    : scatterOffset(i, offsets.length) * BOMBLET_SCATTER_CELLS * scale;
        }
        List<Path> paths = this.context.level().paths().stream().map(PathRuntime::path).toList();
        float radius = bomblets.radiusCells() * scale;
        int damage = Math.round(this.damageCurrent() * bomblets.damageShare());
        for (Vec2 at : PathLine.along(paths, landing.x(), landing.y(), offsets)) {
            for (EnemyMob enemy : InRangeTargetQuery.everyone((int) Math.round(at.x()), (int) Math.round(at.y()), radius)
                    .matching(this.context.enemies())) {
                this.dealDamage(enemy, Damage.of(landing.type().damageType(),
                        Math.round(damage * falloff(at.x(), at.y(), radius, enemy, false))));
                this.applyEffect(enemy, sink -> Effect.cracked(CRACKED_TICKS, sink));
            }
            this.blastMarks.add(new BlastMark(at, radius, this.tickNow + 1, landing.type().look()));
        }
    }

    /** Bomblet {@code index} of {@code count} scattered either side of the impact: -2, -1, 1, 2 for four. */
    private static double scatterOffset(int index, int count) {
        int half = count / 2;
        return index < half ? index - half : index - half + 1;
    }

    /** The enemy closest to where the shell landed, if it is within {@code reach} pixels of it. */
    private EnemyMob enemyAtCentre(List<EnemyMob> hit, double x, double y, float reach) {
        EnemyMob closest = null;
        double closestDistance2 = (double) reach * reach;
        for (EnemyMob enemy : hit) {
            double dx = x - enemy.getX();
            double dy = y - enemy.getY();
            double distance2 = dx * dx + dy * dy;
            if (distance2 <= closestDistance2) {
                closest = enemy;
                closestDistance2 = distance2;
            }
        }
        return closest;
    }

    public EnemyMob getCurrentTarget() {
        return this.currentTarget;
    }

    /** The blast's radius in pixels before Bracketing. */
    float getSplashRadius() {
        return this.blastRadius(this.spec(this.perks.all()), 1f);
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    /** The last landing and how tight its bracket is, for the ranging marker; empty when not attuned. */
    public Optional<BracketTracker.Marker> getRangingMarker() {
        return this.bracketing.marker();
    }

    /** The flash of the last nuke, for the frame build; empty before the first one. */
    public Optional<NukeFlash> getNukeFlash() {
        return Optional.ofNullable(this.nukeFlash);
    }

    /** Blasts that went off in the last few ticks, for the fading flash the board draws. */
    public List<BlastMark> getBlastMarks() {
        return List.copyOf(this.blastMarks);
    }

    /** A Barrage fires a salvo for a longer reload, so its rate is shells a second over the whole cycle. */
    @Override
    protected Optional<TowerStatLine> cadence() {
        SalvoSpec salvo = this.spec(this.perks.all()).salvo();
        if (!salvo.isSalvo()) {
            return super.cadence();
        }
        return Optional.of(new TowerStatLine(TowerStat.FIRE_RATE, TICKS_PER_SECOND / (this.coolDownMax + 1),
                (float) (salvo.shells() * TICKS_PER_SECOND * this.fireRateCurrent()
                        / ((this.coolDownMax + 1) * salvo.reloadFactor()))));
    }

    @Override
    protected List<TowerStatLine> ownStats() {
        MortarSpec spec = this.spec(this.perks.all());
        ProjectileStats shell = this.shellStats(spec, ShellType.PLAIN);
        float cellsPerSecond = shell.speed() * TICKS_PER_SECOND / this.context.getBoard().scale();
        return List.of(new TowerStatLine(TowerStat.SPLASH_RADIUS, SPLASH_RADIUS_BASE, SPLASH_RADIUS_BASE * spec.blastScale()),
                TowerStatLine.fixed(TowerStat.PROJECTILE_SPEED, cellsPerSecond),
                TowerStatLine.fixed(TowerStat.PROJECTILE_SIZE, shell.size()));
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        MortarSpec spec = this.spec(this.perks.all());
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Targets", "first"));
        lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Dead zone",
                Math.round(spec.reach().deadZone() / this.context.getBoard().scale() * 10f) / 10f + " cells"));
        lines.add(new BehaviourLine(BehaviourMarker.CRACKED, "Cracks plating", BehaviourLine.seconds(CRACKED_TICKS)));
        if (spec.bracket().active()) {
            int damageStep = Math.round(spec.bracket().damageStep() * 100);
            int radiusStep = Math.round(spec.bracket().radiusStep() * 100);
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Bracketing", damageStep == radiusStep
                    ? "+" + damageStep + "% a step" : "+" + damageStep + "% dmg, +" + radiusStep + "% radius"));
        }
        if (spec.daze().isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.DAZE, "Impact dazes",
                    BehaviourLine.seconds(spec.daze().ticks())));
        }
        if (spec.nuke().isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Every " + spec.nuke().every() + "th shell",
                    "nuke x" + Math.round(spec.nuke().damageFactor())));
        }
        if (spec.centre().isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Blast centre",
                    "x" + Math.round(spec.centre().damageFactor()) + ", sundered"));
        }
        if (spec.shrapnel().active()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Shrapnel",
                    BehaviourLine.percent(spec.shrapnel().damageShare() * spec.shrapnel().scale())
                            + (spec.shrapnel().carriesShell() ? ", shell effect" : "")));
        }
        if (spec.shrapnel().bleeds()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Shrapnel makes", "bleed"));
        }
        if (spec.leadsTarget()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Aims", "ahead of the enemy"));
        }
        if (spec.salvo().isSalvo()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Salvo", spec.salvo().shells() + " shells, "
                    + BehaviourLine.seconds(spec.salvo().gapTicks()) + " apart"));
        }
        if (spec.bomblets().isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Bomblets", spec.bomblets().count()
                    + (spec.bomblets().inLine() ? " in a line ahead" : " around the impact")));
        }
        List<ShellType> specials = spec.shells().specials();
        if (!specials.isEmpty()) {
            String pattern = specials.size() == 1 ? specials.getFirst().label()
                    : specials.stream().map(ShellType::label).collect(Collectors.joining(", ")) + ", plain";
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING,
                    specials.size() == 1 ? "Every 3rd shell" : "Shells in turn", pattern));
        }
        return lines;
    }

    @Override
    protected String description() {
        return "Lobs a slow, unguided shell.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitMortarTower(this);
    }
}
