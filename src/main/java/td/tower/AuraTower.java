package td.tower;

import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.StackingDebuff;
import td.enemy.EnemyMob;
import td.enemy.EnemyWalk;
import td.stat.StatModifiers;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.aura.AuraMechanic;
import td.tower.aura.BreakthroughMechanic;
import td.tower.aura.DaylightMechanic;
import td.tower.aura.GlareMechanic;
import td.tower.aura.HeadStartMechanic;
import td.tower.aura.KinshipMechanic;
import td.tower.aura.LanternMechanic;
import td.tower.aura.LastLightMechanic;
import td.tower.aura.LightSpec;
import td.tower.aura.MentorMechanic;
import td.tower.aura.PrismArc;
import td.tower.aura.PrismMark;
import td.tower.aura.PrismMechanic;
import td.tower.aura.RevealedEnemy;
import td.tower.aura.RingleaderMechanic;
import td.tower.aura.SearchlightMechanic;
import td.tower.aura.SelfLightMechanic;
import td.tower.aura.Spotlight;
import td.tower.aura.SteadyLightMechanic;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import static td.tower.upgrade.MechanicCatalogue.entry;

/**
 * The Beacon: a lantern that never attacks. Every other tower whose centre is in its light is Lit; the Lantern node
 * makes the light reveal every enemy in it and a Lit tower take less of the jam and the blinding, and without it Lit
 * gives nothing. It gives no damage, range or fire rate; what its nodes add is a counter (Daylight, Glare,
 * Sanctuary), a direction (the Spotlight), time (the Lessons) or every Lit type's mark (Prism). Lights that overlap
 * stack, each as if it stood alone.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class AuraTower extends AbstractTower {

    public static final int PRICE = 30;
    public static final float DAMAGE_POINTS = 0f;
    /** The light's radius; no node changes it. */
    public static final float RANGE = 2.5f;

    private static final int REVEAL_TICKS = Math.round(TickRate.TICKS_PER_SECOND);
    private static final int AFTERGLOW_TICKS = Math.round(2f * TickRate.TICKS_PER_SECOND);
    /** How long what the light holds an enemy to lasts once it leaves: Priority, Glare's Expose, Prism's timed marks. */
    private static final int LINGER_TICKS = Math.round(TickRate.TICKS_PER_SECOND / 2f);

    private static final UpgradeNode LANTERN = UpgradeNode.of("aura.lantern", "Lantern", 10);
    private static final UpgradeNode AFTERGLOW = UpgradeNode.of("aura.afterglow", "Afterglow", 5)
            .withModifiers(TowerModifiers.revealTime(AFTERGLOW_TICKS));

    private static final UpgradeNode SEARCHLIGHT = UpgradeNode.of("aura.searchlight", "Searchlight", 5).inTier(1);
    private static final UpgradeNode LAST_LIGHT = UpgradeNode.of("aura.last_light", "Last Light", 5).inTier(1);
    private static final UpgradeNode RINGLEADER = UpgradeNode.of("aura.ringleader", "Ringleader", 5).inTier(1);
    private static final UpgradeNode KINSHIP = UpgradeNode.of("aura.kinship", "Kinship", 5).inTier(1);
    private static final UpgradeNode MENTOR = UpgradeNode.of("aura.mentor", "Mentor", 5).inTier(1);
    private static final UpgradeNode BREAKTHROUGH = UpgradeNode.of("aura.breakthrough", "Breakthrough", 5).inTier(1);

    private static final UpgradeNode DAYLIGHT = UpgradeNode.of("aura.daylight", "Daylight", 10).inTier(2);
    private static final UpgradeNode GLARE = UpgradeNode.of("aura.glare", "Glare", 10).inTier(2);

    private static final UpgradeNode SANCTUARY = UpgradeNode.of("aura.sanctuary", "Sanctuary", 15)
            .withEmission(TowerModifiers.disruptionTaken(0f))
            .inTier(3);
    private static final UpgradeNode PRISM = UpgradeNode.of("aura.prism", "Prism", 20).inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(List.of(LANTERN, AFTERGLOW))
            .with(SEARCHLIGHT, LAST_LIGHT, RINGLEADER, KINSHIP, MENTOR, BREAKTHROUGH,
                    DAYLIGHT, GLARE,
                    SANCTUARY, PRISM)
            .withCluster(UpgradeCluster.of("Spotlight", SEARCHLIGHT, LAST_LIGHT, RINGLEADER))
            .withCluster(UpgradeCluster.of("Lessons", KINSHIP, MENTOR, BREAKTHROUGH))
            .withCluster(UpgradeCluster.of("Edge of light", DAYLIGHT, GLARE))
            .withTiers(List.of(UpgradeTier.open(), UpgradeTier.gated(60, 0).withMechanics(),
                    UpgradeTier.gated(120, 0).withMechanics(), UpgradeTier.gated(200, 0).withMechanics()));

    private static final MechanicCatalogue<AuraMechanic> MECHANICS = MechanicCatalogue.of(
            entry(LANTERN.id(), LanternMechanic::new),
            entry(UpgradeTree.tierNodeId(1), SteadyLightMechanic::new),
            entry(UpgradeTree.tierNodeId(2), SelfLightMechanic::new),
            entry(UpgradeTree.tierNodeId(3), HeadStartMechanic::new),
            entry(SEARCHLIGHT.id(), SearchlightMechanic::new),
            entry(LAST_LIGHT.id(), LastLightMechanic::new),
            entry(RINGLEADER.id(), RingleaderMechanic::new),
            entry(KINSHIP.id(), KinshipMechanic::new),
            entry(MENTOR.id(), MentorMechanic::new),
            entry(BREAKTHROUGH.id(), BreakthroughMechanic::new),
            entry(DAYLIGHT.id(), DaylightMechanic::new),
            entry(GLARE.id(), GlareMechanic::new),
            entry(PRISM.id(), PrismMechanic::new));

    /** The mark each kind of tower leaves, as Prism carries it; none for a tower that leaves none. */
    private static final TowerVisitor<Optional<PrismMark>> MARK_OF = new TowerVisitor<>() {
        @Override
        public Optional<PrismMark> visitSniperTower(SniperTower tower) {
            return Optional.of(PrismMark.SUNDER);
        }

        @Override
        public Optional<PrismMark> visitSplashTower(SplashTower tower) {
            return Optional.of(PrismMark.VULNERABLE);
        }

        @Override
        public Optional<PrismMark> visitPulseTower(PulseTower tower) {
            return Optional.of(PrismMark.TOLL);
        }

        @Override
        public Optional<PrismMark> visitAuraTower(AuraTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<PrismMark> visitMortarTower(MortarTower tower) {
            return Optional.of(PrismMark.CRACK);
        }

        @Override
        public Optional<PrismMark> visitSeekerTower(SeekerTower tower) {
            return Optional.of(PrismMark.UNRAVEL);
        }

        @Override
        public Optional<PrismMark> visitCinderTower(CinderTower tower) {
            return Optional.of(PrismMark.SINGE);
        }
    };

    private static final TowerVisitor<Optional<AuraTower>> AS_BEACON = new TowerVisitor<>() {
        @Override
        public Optional<AuraTower> visitSniperTower(SniperTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<AuraTower> visitSplashTower(SplashTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<AuraTower> visitPulseTower(PulseTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<AuraTower> visitAuraTower(AuraTower tower) {
            return Optional.of(tower);
        }

        @Override
        public Optional<AuraTower> visitMortarTower(MortarTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<AuraTower> visitSeekerTower(SeekerTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<AuraTower> visitCinderTower(CinderTower tower) {
            return Optional.empty();
        }
    };

    private final OwnedMechanics<AuraMechanic> mechanics = new OwnedMechanics<>(MECHANICS);
    // Both threads ask for the spec; the fold is cached per mechanic list, which a purchase replaces whole.
    private volatile FoldedSpec folded = new FoldedSpec(List.of(), LightSpec.bare());
    /** The enemy the Spotlight holds; seen by the frame build, so published. */
    private volatile Optional<EnemyMob> held = Optional.empty();
    /** The hidden enemies the light revealed in the last moment, for the reveal ray; published whole. */
    private volatile List<RevealedEnemy> reveals = List.of();
    /** The enemies in the light at the last tick, for the frame build to light; published whole. */
    private volatile List<EnemyMob> inLight = List.of();
    /** The tick the light last stirred: it was built, or the towers it lights or what its nodes make it do changed. */
    private volatile OptionalInt stirredAt = OptionalInt.empty();
    /** What Prism carries toward each Lit tower at the last tick, for the frame build; published whole. */
    private volatile List<PrismArc> arcs = List.of();
    private List<Tower> litBefore = List.of();
    private LightSpec specBefore = LightSpec.bare();
    /** The enemies inside at the last tick: one entering gets Prism's stacks. Membership only. */
    private Set<EnemyMob> lastInside = Collections.newSetFromMap(new IdentityHashMap<>());

    public AuraTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.AURA, new TowerBaseStats(DAMAGE_POINTS, RANGE, 0)
                .with(TowerStat.REVEAL_TIME, REVEAL_TICKS), context, x, y);
    }

    @Override
    protected OwnedMechanics<AuraMechanic> ownedMechanics() {
        return this.mechanics;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    /** What the owned mechanics make the light do, folded once per set of mechanics. */
    private LightSpec spec() {
        List<AuraMechanic> owned = this.mechanics.all();
        FoldedSpec last = this.folded;
        if (last.mechanics() == owned) {
            return last.spec();
        }
        LightSpec spec = LightSpec.bare();
        for (AuraMechanic mechanic : owned) {
            spec = mechanic.refineSpec(spec);
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

    @Override
    protected StatModifiers<TowerStat> gives() {
        return this.light();
    }

    /** What a Lit tower takes from this light: the share of the jam and the blinding it lets through. */
    public StatModifiers<TowerStat> light() {
        return TowerModifiers.disruptionTaken(this.spec().jamTaken()).plus(this.upgrades().totalEmission());
    }

    /** Another tower whose centre is in the light, and itself once Self Light lets it. Never another Beacon. */
    private boolean lights(Tower other) {
        if (other == this) {
            return this.spec().litsSelf();
        }
        if (other.getType() == TowerFactory.Type.AURA) {
            return false;
        }
        int dx = this.centerX - other.getX();
        int dy = this.centerY - other.getY();
        return (dx * dx + dy * dy) < this.rangeReal2();
    }

    @Override
    public StatModifiers<TowerStat> buffFor(Tower other) {
        return this.lights(other) ? this.light() : StatModifiers.none();
    }

    /** The towers in the light, in the order they were built, derived on each call rather than tracked. */
    public List<Tower> litTowers() {
        return this.context.towers().all().stream().filter(this::lights).toList();
    }

    /** The Beacon has no reach on the path: it earns through the towers it lights. */
    @Override
    public boolean reached(EnemyWalk walk) {
        return false;
    }

    /** Kinship or Mentor, as the Lessons pick makes it; nothing for a tower out of the light. */
    @Override
    public float xpBonusFor(Tower earner) {
        if (!this.lights(earner)) {
            return 0f;
        }
        return switch (this.spec().lesson()) {
            case KINSHIP -> KinshipMechanic.xpFor(this.otherTypesFor(earner));
            case MENTOR -> this.apprentice().filter(apprentice -> apprentice == earner).isPresent() ? MentorMechanic.XP : 0f;
            case NONE, BREAKTHROUGH -> 0f;
        };
    }

    /** The tower types in the light other than {@code earner}'s. */
    private int otherTypesFor(Tower earner) {
        // Membership only: the count is all that is read.
        Set<TowerFactory.Type> types = EnumSet.noneOf(TowerFactory.Type.class);
        for (Tower tower : this.litTowers()) {
            types.add(tower.getType());
        }
        types.remove(earner.getType());
        return types.size();
    }

    /** The Lit tower with the least XP, the earliest built on a tie; empty when the light holds none. */
    private Optional<Tower> apprentice() {
        return this.litTowers().stream().min(Comparator.comparingInt(tower -> tower.experience().xp()));
    }

    /** Whether the light reads which tower types it holds (Kinship, Prism), so the lantern shows them. */
    public boolean readsTowerTypes() {
        LightSpec spec = this.spec();
        return spec.lesson() == LightSpec.Lesson.KINSHIP || spec.prism();
    }

    /** Whether its light carries Daylight, where an enemy gives its allies no aura. */
    public boolean givesDaylight() {
        return this.spec().edge() == LightSpec.Edge.DAYLIGHT;
    }

    /** The enemy the Spotlight holds now, if any. */
    public Optional<EnemyMob> heldEnemy() {
        return this.held;
    }

    /** The hidden enemies the light revealed as they entered, over the last {@link MomentMemory#TICKS}. */
    public List<RevealedEnemy> reveals() {
        return this.reveals;
    }

    /** The enemies in the light as of the last tick; the frame build lights each. */
    public List<EnemyMob> enemiesInLight() {
        return this.inLight;
    }

    /** The tick the light last stirred, which the frame build shows as the edge of the light for a moment. */
    public OptionalInt stirredAt() {
        return this.stirredAt;
    }

    /** What Prism's light carried toward each Lit tower that leaves a mark at the last tick; none without Prism. */
    public List<PrismArc> prismArcs() {
        return this.arcs;
    }

    private List<PrismArc> arcsToward(List<Tower> lit) {
        List<PrismArc> toward = new ArrayList<>();
        for (Tower tower : lit) {
            tower.accept(MARK_OF).ifPresent(mark -> toward.add(new PrismArc(
                    (float) Math.atan2(tower.getY() - this.centerY, tower.getX() - this.centerX), mark)));
        }
        return List.copyOf(toward);
    }

    /** The marks Prism carries now, one per Lit tower type that leaves one, in the pane's order; none without it. */
    public List<PrismMark> prismMarks() {
        return this.marksOf(this.litTowers());
    }

    private List<PrismMark> marksOf(List<Tower> lit) {
        if (!this.spec().prism()) {
            return List.of();
        }
        Set<PrismMark> marks = EnumSet.noneOf(PrismMark.class);
        for (Tower tower : lit) {
            tower.accept(MARK_OF).ifPresent(marks::add);
        }
        return List.copyOf(marks);
    }

    /** What {@code tower}'s light carries onto the enemies in it now: Prism's marks; nothing for any other tower. */
    public static List<Term> carriedBy(Tower tower) {
        return tower.accept(AS_BEACON).map(beacon -> beacon.prismMarks().stream().map(PrismMark::term).toList())
                .orElse(List.of());
    }

    /**
     * The light's tick: Daylight's circle, the reveal and Glare on every enemy inside, the Spotlight's hold, Prism's
     * marks and what its tags settle, and Breakthrough's lessons.
     */
    @Override
    protected void act(int gameTime) {
        LightSpec spec = this.spec();
        List<Tower> lit = this.litTowers();
        if (spec.edge() == LightSpec.Edge.DAYLIGHT) {
            this.context.daylight().add(this.centerX, this.centerY, this.rangeReal());
        }
        List<EnemyMob> inside = InRangeTargetQuery.everyone(this.centerX, this.centerY, this.rangeReal())
                .matching(this.context.enemies());
        int revealTicks = Math.round(this.stats().value(TowerStat.REVEAL_TIME));
        List<RevealedEnemy> revealed = new ArrayList<>();
        for (RevealedEnemy earlier : this.reveals) {
            if (!MomentMemory.forgets(earlier.atTick(), gameTime) && !earlier.enemy().isDead()) {
                revealed.add(earlier);
            }
        }
        List<PrismMark> marks = this.marksOf(lit);
        Set<EnemyMob> nowInside = Collections.newSetFromMap(new IdentityHashMap<>());
        for (EnemyMob enemy : inside) {
            nowInside.add(enemy);
            boolean entered = !this.lastInside.contains(enemy);
            if (spec.reveals()) {
                if (entered && enemy.isHidden()) {
                    revealed.add(new RevealedEnemy(enemy, gameTime));
                }
                this.reveal(enemy, revealTicks);
            }
            if (spec.edge() == LightSpec.Edge.GLARE) {
                this.applyEffect(enemy, sink -> Effect.exposed(LINGER_TICKS, sink));
            }
            marks.forEach(mark -> this.carry(mark, enemy, entered));
        }
        this.lastInside = nowInside;
        this.inLight = this.touchesEnemies(spec) ? List.copyOf(inside) : List.of();
        this.reveals = List.copyOf(revealed);
        this.hold(spec.spotlight(), inside);
        if (spec.lesson() == LightSpec.Lesson.BREAKTHROUGH) {
            this.teachBreakthroughs(gameTime, lit);
        }
        this.arcs = spec.prism() ? this.arcsToward(lit) : List.of();
        if (spec.xpFloor() > 0) {
            this.liftToFloor(spec.xpFloor(), lit);
        }
        if (this.stirredAt.isEmpty() || !lit.equals(this.litBefore) || !spec.equals(this.specBefore)) {
            this.stirredAt = OptionalInt.of(gameTime);
        }
        this.litBefore = lit;
        this.specBefore = spec;
    }

    /** Head Start: a Lit tower under the floor earns what it lacks. */
    private void liftToFloor(int floor, List<Tower> lit) {
        for (Tower tower : lit) {
            int lacking = floor - tower.experience().xp();
            if (lacking > 0) {
                tower.earnXp(lacking);
            }
        }
    }

    /** The beam keeps its enemy while it lives and stays in the light, then picks again. */
    private void hold(Spotlight spotlight, List<EnemyMob> inside) {
        if (!spotlight.isActive()) {
            this.held = Optional.empty();
            return;
        }
        Optional<EnemyMob> keep = this.held.filter(enemy -> !enemy.isDead() && inside.contains(enemy));
        Optional<EnemyMob> next = keep.or(() -> spotlight.selector(this.cellPixels()).selectFrom(inside));
        next.ifPresent(enemy -> this.applyEffect(enemy, sink -> Effect.priority(LINGER_TICKS, sink)));
        this.held = next;
    }

    /**
     * The light's stack of a mark that stacks lands on top of what the enemy carries, up to the mark's own cap, as it
     * enters, and again while it stays once the mark has worn off it; a timed mark is renewed while it stays.
     */
    private void carry(PrismMark mark, EnemyMob enemy, boolean entered) {
        switch (mark) {
            case SUNDER -> {
                if (landsAgain(enemy, EffectKind.SUNDERED, entered)) {
                    this.applyStacks(enemy, StackingDebuff.SUNDERED, mark.stacks());
                }
            }
            case UNRAVEL -> {
                if (landsAgain(enemy, EffectKind.UNRAVELED, entered)) {
                    this.applyStacks(enemy, StackingDebuff.UNRAVELED, mark.stacks());
                }
            }
            case TOLL -> {
                if (landsAgain(enemy, EffectKind.TOLL, entered)) {
                    this.applyEffect(enemy, _ -> Effect.toll(mark.stacks()));
                }
            }
            case CRACK -> this.applyEffect(enemy, sink -> Effect.cracked(mark.stacks(), MortarTower.CRACKED_TICKS, sink));
            case SINGE -> this.applyEffect(enemy, sink -> Effect.singed(LINGER_TICKS, sink));
            case VULNERABLE -> this.applyEffect(enemy, sink -> Effect.vulnerable(mark.stacks(), LINGER_TICKS, sink));
        }
    }

    /** A stacking mark lands as the enemy enters, and again while it stays once the mark has worn off it. */
    private static boolean landsAgain(EnemyMob enemy, EffectKind mark, boolean entered) {
        return entered || enemy.effectStacks(mark) == 0;
    }

    /** Breakthrough: a Lit tower whose tier rose this tick, beyond what Head Start gives, teaches every other Lit tower. */
    private void teachBreakthroughs(int gameTime, List<Tower> lit) {
        int floor = this.spec().xpFloor();
        for (Tower opener : lit) {
            TierRise rise = opener.tierRise();
            // A tier the light's own XP floor opens teaches no one, or building and selling towers would farm XP.
            if (rise.ticksSince(gameTime, 0).isPresent() && opener.upgradeTree().tiers().get(rise.to()).xp() > floor) {
                lit.stream().filter(tower -> tower != opener).forEach(tower -> tower.earnXp(BreakthroughMechanic.XP));
            }
        }
    }

    /** Whether the light does anything to an enemy in it, which is what shows it lit. */
    private boolean touchesEnemies(LightSpec spec) {
        return spec.reveals() || spec.edge() != LightSpec.Edge.NONE || spec.prism() || spec.spotlight().isActive();
    }

    /** The beam lets go and the light's tags are dropped; what an enemy carries is its own. */
    @Override
    protected void endFight() {
        this.held = Optional.empty();
        this.reveals = List.of();
        this.inLight = List.of();
        this.arcs = List.of();
        this.lastInside = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    @Override
    protected List<AppliedEffect> baseApplies() {
        LightSpec spec = this.spec();
        List<AppliedEffect> applied = new ArrayList<>();
        if (spec.reveals()) {
            float seconds = this.stats().value(TowerStat.REVEAL_TIME) / TickRate.TICKS_PER_SECOND;
            applied.add(new AppliedEffect(EffectKind.REVEALED, List.of(new AppliedEffect.Source(this.displayName(),
                    TermText.of("every enemy in the ").plus(TowerTerm.LIGHT)
                            .plus(", while inside and " + TermNumbers.seconds(seconds) + " after")))));
        }
        applied.add(new AppliedEffect(TowerTerm.LIT, List.of(new AppliedEffect.Source(this.displayName(),
                    TermText.of("every other tower in the ").plus(TowerTerm.LIGHT).plus(": ")
                            .plus(TowerStat.DISRUPTION_TAKEN.changeText(this.light().get(TowerStat.DISRUPTION_TAKEN)))))));
        return applied;
    }

    @Override
    protected String description() {
        return "Never attacks. Its light reaches the towers in it, but they gain nothing and the hidden stay hidden until its "
                + "Lantern is bought, which shows the hidden and shields those towers from jam; a Beacon never lights another Beacon, and lights that overlap stack.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitAuraTower(this);
    }

    private record FoldedSpec(List<AuraMechanic> mechanics, LightSpec spec) {
    }
}
