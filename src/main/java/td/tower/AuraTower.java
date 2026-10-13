package td.tower;

import td.enemy.EnemyWalk;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.tower.aura.ApprenticePerk;
import td.tower.aura.AuraPerk;
import td.tower.aura.AuraSpec;
import td.tower.aura.ChosenPerk;
import td.tower.aura.FarKinshipPerk;
import td.tower.aura.KinshipPerk;
import td.tower.aura.KinshipStrengthPerk;
import td.tower.aura.RallyPerk;
import td.tower.aura.RallyRule;
import td.tower.aura.SharedLessonsPerk;
import td.tower.aura.TutelagePerk;
import td.tower.mortar.PathLine;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.BaseSlotPerks;
import td.tower.upgrade.BuffedTowersCondition;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.NeighbourOfTypeCondition;
import td.tower.upgrade.OwnedPerks;
import td.tower.upgrade.PerkCatalogue;
import td.tower.upgrade.PerkText;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.PathRuntime;
import td.util.ThreadConfined;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Never attacks; contributes stat modifiers to other towers whose centre is within its range.
 * Several auras stack additively.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class AuraTower extends AbstractTower {

    public static final int PRICE = 20;
    public static final float DAMAGE_POINTS = 0f;
    public static final float RANGE = 1.5f;
    public static final float DEFAULT_POWER = 0.2f;

    /** The share of a jammer's effect that still reaches a tower in range. */
    private static final float DISRUPTION_LEFT = 0.5f;
    /** What the aura must buff before its third head level is offered; Keen Edge also needs an aura beside it. */
    private static final int HEAD_THREE_BUFFED_TOWERS = 4;
    /** Rally: the spot's size, what stepping on it gives, for how long, and how soon it can be used again. */

    private static final UpgradeNode AMPLIFYING_CORE_1 = UpgradeTier.HEAD_1.node("aura.head.amplifying_core.1",
            "Amplifying Core", PRICE)
            .withModifiers(TowerModifiers.auraPower(0.1f));
    private static final UpgradeNode AMPLIFYING_CORE_2 = UpgradeTier.HEAD_2.node("aura.head.amplifying_core.2",
            "Amplifying Core II", PRICE)
            .after(AMPLIFYING_CORE_1)
            .withModifiers(TowerModifiers.auraPower(0.1f))
            .withEmission(TowerModifiers.attackSpeed(0.1f));
    private static final UpgradeNode KEEN_EDGE = UpgradeTier.HEAD_3.node("aura.head.amplifying_core.3", "Keen Edge",
            PRICE)
            .withGate(new BuffedTowersCondition(HEAD_THREE_BUFFED_TOWERS)
                    .and(new NeighbourOfTypeCondition(TowerFactory.Type.AURA)))
            .withEmission(TowerModifiers.critMultiplier(0.5f))
            .after(AMPLIFYING_CORE_2);
    private static final UpgradeNode BROADCAST_1 = UpgradeTier.HEAD_1.node("aura.head.broadcast.1",
            "Broadcast", PRICE)
            .withModifiers(TowerModifiers.range(0.45f));
    private static final UpgradeNode BROADCAST_2 = UpgradeTier.HEAD_2.node("aura.head.broadcast.2",
            "Broadcast II", PRICE)
            .after(BROADCAST_1)
            .withEmission(TowerModifiers.buffDuration(1f).plus(TowerModifiers.disruptionTaken(0.5f)));
    private static final UpgradeNode CONDUIT = UpgradeTier.HEAD_3.node("aura.head.broadcast.3", "Conduit", PRICE)
            .withGate(new BuffedTowersCondition(HEAD_THREE_BUFFED_TOWERS))
            .withEmission(TowerModifiers.effectDuration(0.3f))
            .after(BROADCAST_2);
    private static final UpgradeNode TUTELAGE_1 = UpgradeTier.EXTRA_1.node("aura.extra.tutelage.1", "Tutelage", PRICE);
    private static final UpgradeNode SHARED_LESSONS = UpgradeTier.EXTRA_2.node("aura.extra.tutelage.2",
            "Shared Lessons", PRICE)
            .after(TUTELAGE_1);
    private static final UpgradeNode APPRENTICE = UpgradeTier.EXTRA_3.node("aura.extra.tutelage.3", "Apprentice",
            PRICE)
            .after(SHARED_LESSONS);
    private static final UpgradeNode WITHERING_FIELD = UpgradeTier.SPECIAL.node("aura.special.withering_field",
            "Withering Field", PRICE)
            .withEmission(TowerModifiers.witherEvery(200));
    private static final UpgradeNode CHOSEN = UpgradeTier.SPECIAL.node("aura.special.chosen", "Chosen", PRICE);
    private static final UpgradeNode RALLY = UpgradeTier.SPECIAL.node("aura.special.rally", "Rally", PRICE);

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none();

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, RANGE, BASE_PERKS))
            .with(AMPLIFYING_CORE_1, AMPLIFYING_CORE_2, KEEN_EDGE, BROADCAST_1, BROADCAST_2, CONDUIT, TUTELAGE_1, SHARED_LESSONS, APPRENTICE,
                    WITHERING_FIELD, CHOSEN, RALLY)
            .withChoice(ExclusiveChoice.oneOf(AMPLIFYING_CORE_1, BROADCAST_1))
            .withChoice(ExclusiveChoice.oneOf(WITHERING_FIELD, CHOSEN, RALLY));

    private static final PerkCatalogue<AuraPerk> PERKS = PerkCatalogue.<AuraPerk>empty()
            .with(StandardBaseSlot.ATTUNE_ID, KinshipPerk::new)
            .with(BROADCAST_1.id(), FarKinshipPerk::new)
            .with(AMPLIFYING_CORE_1.id(), KinshipStrengthPerk::new)
            .with(TUTELAGE_1.id(), TutelagePerk::new)
            .with(SHARED_LESSONS.id(), SharedLessonsPerk::new)
            .with(APPRENTICE.id(), ApprenticePerk::new)
            .with(CHOSEN.id(), ChosenPerk::new)
            .with(RALLY.id(), RallyPerk::new);

    private final OwnedPerks<AuraPerk> perks = new OwnedPerks<>(PERKS);
    // Both threads ask for the spec; the fold is cached per perk list, which a purchase replaces whole.
    private volatile FoldedSpec folded = new FoldedSpec(List.of(), AuraSpec.none());
    private Tower lastChosen;
    private int rallyReadyAt;
    private Vec2 rallySpot;

    public AuraTower(GameWorld context, int x, int y) {
        this(context, x, y, DEFAULT_POWER);
    }

    /** An aura with a non-default buff strength. */
    public AuraTower(GameWorld context, int x, int y, float power) {
        super(TowerFactory.Type.AURA, new TowerBaseStats(DAMAGE_POINTS, RANGE, 0).with(TowerStat.AURA_POWER, power),
                context, x, y);
    }

    @Override
    protected PerkCatalogue<AuraPerk> perkCatalogue() {
        return PERKS;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        this.perks.add(node);
    }

    /** What the owned perks make this aura do, folded once per set of perks. */
    private AuraSpec spec() {
        List<AuraPerk> owned = this.perks.all();
        FoldedSpec last = this.folded;
        if (last.perks() == owned) {
            return last.spec();
        }
        AuraSpec spec = AuraSpec.none();
        for (AuraPerk perk : owned) {
            spec = perk.refineSpec(spec);
        }
        this.folded = new FoldedSpec(owned, spec);
        return spec;
    }

    @Override
    public boolean emitsBuffs() {
        return true;
    }

    @Override
    protected boolean isPassive() {
        return true;
    }

    /** What this aura gives a tower that gets no Kinship strength from it. */
    public StatModifiers<TowerStat> buff() {
        return this.emissionAt(this.stats().value(TowerStat.AURA_POWER), this.spec());
    }

    private StatModifiers<TowerStat> emissionAt(float strength, AuraSpec spec) {
        StatModifiers<TowerStat> emission = TowerModifiers.damagePercent(strength)
                .plus(TowerModifiers.rangePercent(strength))
                .plus(TowerModifiers.disruptionTaken(DISRUPTION_LEFT))
                .plus(this.upgrades().totalEmission());
        return spec.chosen() ? emission.scaledBy(spec.chosenStrength()) : emission;
    }

    /** Another tower within range. Never itself, and never another aura. */
    private boolean inRange(Tower other) {
        if (other == this || other.getType() == TowerFactory.Type.AURA) {
            return false;
        }
        int dx = this.centerX - other.getX();
        int dy = this.centerY - other.getY();
        return (dx * dx + dy * dy) < this.rangeReal2();
    }

    /** What the glow lines to the buffed towers say the aura is: its reach, its chosen hero, or its strength. */
    public GrantLook grantLook() {
        AuraSpec spec = this.spec();
        if (spec.chosen()) {
            return GrantLook.CHOSEN;
        }
        return spec.kinship().extraReachCells() > 0f ? GrantLook.REACH : GrantLook.PLAIN;
    }

    /** Whether this aura marks the buffed tower with the least XP; the mark is {@link #apprentice()}. */
    public boolean mentorsApprentice() {
        return this.spec().apprenticeXp() > 0f;
    }

    /** How the aura draws its links. */
    public enum GrantLook {
        PLAIN, REACH, CHOSEN
    }

    /** A tower in range, or with Chosen only the most experienced one. */
    private boolean buffs(Tower other) {
        return this.inRange(other) && (!this.spec().chosen() || other == this.chosen());
    }

    /** The tower in range with the most XP, the earliest built on a tie; null when none is in range. */
    public Tower chosen() {
        Tower most = null;
        for (Tower tower : this.context.towers().all()) {
            if (this.inRange(tower) && (most == null || tower.experience().xp() > most.experience().xp())) {
                most = tower;
            }
        }
        return most;
    }

    /** An aura has no reach on the path: it earns through the towers it buffs. */
    @Override
    public boolean reached(EnemyWalk walk) {
        return false;
    }

    @Override
    public StatModifiers<TowerStat> buffFor(Tower other) {
        if (!this.buffs(other)) {
            return StatModifiers.none();
        }
        AuraSpec spec = this.spec();
        float strength = this.stats().value(TowerStat.AURA_POWER) + this.kinshipStrengthFor(other, spec);
        return this.emissionAt(strength, spec);
    }

    /** The other tower types Kinship counts for {@code other}: not its own, not an aura, within reach. */
    private int kinshipTypesFor(Tower other) {
        Set<TowerFactory.Type> types = this.typesInReach();
        types.remove(other.getType());
        return types.size();
    }

    /** Kinship's reach is the aura's range and whatever its rule adds. */
    private Set<TowerFactory.Type> typesInReach() {
        Set<TowerFactory.Type> types = EnumSet.noneOf(TowerFactory.Type.class);
        double reach = this.rangeReal() + this.spec().kinship().extraReachCells() * this.context.getBoard().scale();
        for (Tower tower : this.context.towers().all()) {
            double dx = this.centerX - tower.getX();
            double dy = this.centerY - tower.getY();
            if (tower != this && tower.getType() != TowerFactory.Type.AURA && dx * dx + dy * dy < reach * reach) {
                types.add(tower.getType());
            }
        }
        return types;
    }

    /** What Kinship adds to the buff's strength for {@code other}, when its rule strengthens it. */
    private float kinshipStrengthFor(Tower other, AuraSpec spec) {
        if (!spec.kinship().active() || spec.kinship().strengthPerType() == 0f) {
            return 0f;
        }
        return spec.kinship().strengthFor(this.kinshipTypesFor(other));
    }

    /** Kinship, Tutelage and the Apprentice mark, added together; nothing for a tower this aura does not buff. */
    @Override
    public float xpBonusFor(Tower earner) {
        if (!this.buffs(earner)) {
            return 0f;
        }
        AuraSpec spec = this.spec();
        float bonus = spec.tutelageXp();
        if (spec.kinship().active()) {
            bonus += spec.kinship().xpFor(this.kinshipTypesFor(earner));
        }
        if (spec.apprenticeXp() > 0f && earner == this.apprentice()) {
            bonus += spec.apprenticeXp();
        }
        return bonus;
    }

    @Override
    public List<Tower> xpSharedWith(List<Tower> earners) {
        if (!this.spec().sharesLessons() || earners.stream().noneMatch(this::buffs)) {
            return List.of();
        }
        return this.buffedTowers();
    }

    /** The buffed tower with the least XP, the earliest built on a tie; null when this aura buffs none. */
    public Tower apprentice() {
        Tower least = null;
        for (Tower tower : this.buffedTowers()) {
            if (least == null || tower.experience().xp() < least.experience().xp()) {
                least = tower;
            }
        }
        return least;
    }

    /** The towers this aura buffs, derived on each call rather than tracked. */
    public List<Tower> buffedTowers() {
        return this.context.towers().all().stream().filter(this::buffs).toList();
    }

    /**
     * Chosen follows whoever has the most XP, and the buffs are derived, so a change of hero republishes
     * the stats; Rally watches its spot.
     */
    public void doTick(int gameTime) {
        AuraSpec spec = this.spec();
        if (spec.chosen()) {
            Tower chosen = this.chosen();
            if (chosen != this.lastChosen) {
                this.lastChosen = chosen;
                this.context.towers().all().forEach(Tower::recalculateStats);
            }
        }
        RallyRule rally = spec.rally();
        if (rally.isActive() && gameTime >= this.rallyReadyAt && this.enemyOnRallySpot(rally)) {
            this.rallyReadyAt = gameTime + rally.cooldownTicks();
            for (Tower buffed : this.buffedTowers()) {
                buffed.receiveStatus(TowerStatus.timed("aura.rally", TowerModifiers.attackSpeed(rally.attackSpeed()), rally.ticks()));
            }
        }
    }

    private boolean enemyOnRallySpot(RallyRule rally) {
        Optional<Vec2> spot = this.rallySpot();
        if (spot.isEmpty()) {
            return false;
        }
        float radius = rally.spotCells() * this.context.getBoard().scale();
        return InRangeTargetQuery.everyone((int) Math.round(spot.get().x()), (int) Math.round(spot.get().y()), radius)
                .matching(this.context.enemies()).stream().anyMatch(enemy -> !enemy.isDead());
    }

    /** Rally's spot: the point of the path nearest the aura, found once; empty when the level has no path. */
    public Optional<Vec2> rallySpot() {
        if (this.rallySpot == null) {
            List<td.wave.Path> paths = this.context.level().paths().stream().map(PathRuntime::path).toList();
            this.rallySpot = PathLine.along(paths, this.centerX, this.centerY, new double[]{0.0}).stream()
                    .findFirst().orElse(null);
        }
        return Optional.ofNullable(this.rallySpot);
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        AuraSpec spec = this.spec();
        StatModifiers<TowerStat> emission = this.upgrades().totalEmission();
        String bonus = "+" + BehaviourLine.percent(this.stats().value(TowerStat.AURA_POWER));
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Nearby damage", bonus));
        lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Nearby range", bonus));
        lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Nearby disruption", "-" + BehaviourLine.percent(1f - DISRUPTION_LEFT)));
        float attackSpeed = emission.flat(TowerStat.ATTACK_SPEED);
        if (attackSpeed > 0f) {
            lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Nearby attack speed", "+" + BehaviourLine.percent(attackSpeed)));
        }
        StatModifier wither = emission.get(TowerStat.WITHER_INTERVAL);
        if (wither.hasSetValue()) {
            lines.add(new BehaviourLine(BehaviourMarker.VULNERABLE, "Buffed hits",
                    "vulnerable every " + BehaviourLine.seconds(Math.round(wither.setTo()))));
        }
        if (spec.chosen()) {
            lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Chosen",
                    PerkText.times(spec.chosenStrength()) + ", the most experienced only"));
        }
        if (spec.rally().isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Rally", "+" + BehaviourLine.percent(spec.rally().attackSpeed())
                    + " attack speed for " + PerkText.seconds(spec.rally().ticks())));
        }
        if (this.isPlaced()) {
            lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Buffing", this.buffedTowers().size() + " towers"));
            if (spec.kinship().active()) {
                lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Kinship", this.typesInReach().size() + " tower types"));
            }
        }
        this.perks.all().forEach(perk -> perk.behaviour().ifPresent(lines::add));
        return lines;
    }

    @Override
    protected String description() {
        return "Never attacks. Several auras stack, but an aura never buffs another aura.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitAuraTower(this);
    }

    private record FoldedSpec(List<AuraPerk> perks, AuraSpec spec) {
    }
}
