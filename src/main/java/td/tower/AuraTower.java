package td.tower;

import td.damage.Damage;
import td.damage.DamageUnits;
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
import td.tower.aura.KinshipMechanic;
import td.tower.aura.LastLightMechanic;
import td.tower.aura.LightSpec;
import td.tower.aura.MentorMechanic;
import td.tower.aura.PrismMark;
import td.tower.aura.PrismMechanic;
import td.tower.aura.RevealedEnemy;
import td.tower.aura.RingleaderMechanic;
import td.tower.aura.SearchlightMechanic;
import td.tower.aura.Spotlight;
import td.tower.pulse.PulseSpec;
import td.tower.splash.SoulbrandMechanic;
import td.tower.splash.StaticMechanic;
import td.tower.splash.TagLedger;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static td.tower.upgrade.MechanicCatalogue.entry;

/**
 * The Beacon: a lantern that never attacks. Its light reveals every enemy in it, and every other tower whose centre
 * is in it is Lit and takes less of the jam and the blinding. It gives no damage, range or fire rate; what its nodes
 * add is a counter (Daylight, Glare, Sanctuary), a direction (the Spotlight), time (the Lessons) or every Lit type's
 * mark (Prism). Lights that overlap stack, each as if it stood alone.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class AuraTower extends AbstractTower {

    public static final int PRICE = 15;
    public static final float DAMAGE_POINTS = 0f;
    /** The light's radius; no node changes it. */
    public static final float RANGE = 2.5f;
    /** The share of the jam and the blinding a Lit tower still takes. */
    public static final float LIT_JAM_TAKEN = 0.66f;

    /** Steady Light halves what the bare light lets through. */
    private static final float STEADY_JAM_FACTOR = 0.5f;
    private static final int REVEAL_TICKS = Math.round(TickRate.TICKS_PER_SECOND);
    private static final int AFTERGLOW_TICKS = Math.round(2f * TickRate.TICKS_PER_SECOND);
    /** How long what the light holds an enemy to lasts once it leaves: Priority, Glare's Expose, Prism's Chill. */
    private static final int LINGER_TICKS = Math.round(TickRate.TICKS_PER_SECOND / 2f);
    /** How long a reveal is remembered: long enough for its ray to fade. */
    public static final int REVEAL_MEMORY_TICKS = Math.round(TickRate.TICKS_PER_SECOND);
    private static final int CHILL_TICKS = Math.round(TickRate.TICKS_PER_SECOND);

    private static final UpgradeNode STEADY_LIGHT = UpgradeNode.of("aura.steady_light", "Steady Light", 11)
            .withEmission(TowerModifiers.disruptionTaken(STEADY_JAM_FACTOR));
    private static final UpgradeNode AFTERGLOW = UpgradeNode.of("aura.afterglow", "Afterglow", 1)
            .withModifiers(TowerModifiers.revealTime(AFTERGLOW_TICKS));

    private static final UpgradeNode SEARCHLIGHT = UpgradeNode.of("aura.searchlight", "Searchlight", 35).inTier(1);
    private static final UpgradeNode LAST_LIGHT = UpgradeNode.of("aura.last_light", "Last Light", 35).inTier(1);
    private static final UpgradeNode RINGLEADER = UpgradeNode.of("aura.ringleader", "Ringleader", 35).inTier(1);
    private static final UpgradeNode KINSHIP = UpgradeNode.of("aura.kinship", "Kinship", 21).inTier(1);
    private static final UpgradeNode MENTOR = UpgradeNode.of("aura.mentor", "Mentor", 21).inTier(1);
    private static final UpgradeNode BREAKTHROUGH = UpgradeNode.of("aura.breakthrough", "Breakthrough", 21).inTier(1);

    private static final UpgradeNode DAYLIGHT = UpgradeNode.of("aura.daylight", "Daylight", 69).inTier(2);
    private static final UpgradeNode GLARE = UpgradeNode.of("aura.glare", "Glare", 69).inTier(2);

    private static final UpgradeNode SANCTUARY = UpgradeNode.of("aura.sanctuary", "Sanctuary", 50)
            .withEmission(TowerModifiers.disruptionTaken(0f))
            .inTier(3);
    private static final UpgradeNode PRISM = UpgradeNode.of("aura.prism", "Prism", 89).inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(List.of(STEADY_LIGHT, AFTERGLOW))
            .with(SEARCHLIGHT, LAST_LIGHT, RINGLEADER, KINSHIP, MENTOR, BREAKTHROUGH,
                    DAYLIGHT, GLARE,
                    SANCTUARY, PRISM)
            .withCluster(UpgradeCluster.of("Spotlight", SEARCHLIGHT, LAST_LIGHT, RINGLEADER))
            .withCluster(UpgradeCluster.of("Lessons", KINSHIP, MENTOR, BREAKTHROUGH))
            .withCluster(UpgradeCluster.of("Edge of light", DAYLIGHT, GLARE));

    private static final MechanicCatalogue<AuraMechanic> MECHANICS = MechanicCatalogue.of(
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
            return switch (tower.fork()) {
                case NONE -> Optional.empty();
                case STORMCALLER -> Optional.of(PrismMark.STATIC);
                case HEXER -> Optional.of(PrismMark.SOULBRAND);
            };
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
            return Optional.of(PrismMark.CHILL);
        }

        @Override
        public Optional<PrismMark> visitCinderTower(CinderTower tower) {
            return Optional.of(PrismMark.SCORCH);
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
    /** The enemies inside at the last tick: one entering gets Prism's stacks. Membership only. */
    private Set<EnemyMob> lastInside = Collections.newSetFromMap(new IdentityHashMap<>());
    /** The enemies this light's Static and Soulbrand tag, one stack per light, settled by the Burst's rules. */
    private TagLedger lightStatic = new TagLedger();
    private TagLedger lightSoulbrand = new TagLedger();

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
        return TowerModifiers.disruptionTaken(LIT_JAM_TAKEN).plus(this.upgrades().totalEmission());
    }

    /** Another tower whose centre is in the light. Never itself, and never another Beacon. */
    private boolean lights(Tower other) {
        if (other == this || other.getType() == TowerFactory.Type.AURA) {
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
        List<Tower> lit = new ArrayList<>();
        for (Tower tower : this.context.towers().all()) {
            if (this.lights(tower)) {
                lit.add(tower);
            }
        }
        return lit;
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
            case MENTOR -> earner == this.apprentice().orElse(null) ? MentorMechanic.XP : 0f;
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
        Tower least = null;
        for (Tower tower : this.litTowers()) {
            if (least == null || tower.experience().xp() < least.experience().xp()) {
                least = tower;
            }
        }
        return Optional.ofNullable(least);
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

    /** Whether its light Exposes what stands in it. */
    public boolean glares() {
        return this.spec().edge() == LightSpec.Edge.GLARE;
    }

    /** The enemy the Spotlight holds now, if any. */
    public Optional<EnemyMob> heldEnemy() {
        return this.held;
    }

    /** The hidden enemies the light revealed as they entered, over the last {@link #REVEAL_MEMORY_TICKS}. */
    public List<RevealedEnemy> reveals() {
        return this.reveals;
    }

    /** The marks Prism carries now, one per Lit tower type that leaves one, in the pane's order; none without it. */
    public List<PrismMark> prismMarks() {
        if (!this.spec().prism()) {
            return List.of();
        }
        Set<PrismMark> marks = EnumSet.noneOf(PrismMark.class);
        for (Tower tower : this.litTowers()) {
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
    public void doTick(int gameTime) {
        LightSpec spec = this.spec();
        if (spec.edge() == LightSpec.Edge.DAYLIGHT) {
            this.context.daylight().add(this.centerX, this.centerY, this.rangeReal());
        }
        List<EnemyMob> inside = InRangeTargetQuery.everyone(this.centerX, this.centerY, this.rangeReal())
                .matching(this.context.enemies());
        int revealTicks = Math.round(this.stats().value(TowerStat.REVEAL_TIME));
        List<RevealedEnemy> revealed = new ArrayList<>();
        for (RevealedEnemy earlier : this.reveals) {
            if (gameTime - earlier.atTick() < REVEAL_MEMORY_TICKS && !earlier.enemy().isDead()) {
                revealed.add(earlier);
            }
        }
        List<PrismMark> marks = this.prismMarks();
        Set<EnemyMob> nowInside = Collections.newSetFromMap(new IdentityHashMap<>());
        for (EnemyMob enemy : inside) {
            nowInside.add(enemy);
            boolean entered = !this.lastInside.contains(enemy);
            if (entered && enemy.isHidden()) {
                revealed.add(new RevealedEnemy(enemy, gameTime));
            }
            this.reveal(enemy, revealTicks);
            if (spec.edge() == LightSpec.Edge.GLARE) {
                this.applyEffect(enemy, sink -> Effect.exposed(LINGER_TICKS, sink));
            }
            marks.forEach(mark -> this.carry(mark, enemy, entered));
        }
        this.lastInside = nowInside;
        this.reveals = List.copyOf(revealed);
        this.hold(spec.spotlight(), inside);
        this.settleTags();
        if (spec.lesson() == LightSpec.Lesson.BREAKTHROUGH) {
            this.teachBreakthroughs(gameTime);
        }
    }

    /** The beam keeps its enemy while it lives and stays in the light, then picks again. */
    private void hold(Spotlight spotlight, List<EnemyMob> inside) {
        if (!spotlight.isActive()) {
            this.held = Optional.empty();
            return;
        }
        Optional<EnemyMob> keep = this.held.filter(enemy -> !enemy.isDead() && inside.contains(enemy));
        Optional<EnemyMob> next = keep.isPresent() ? keep
                : spotlight.selector(this.context.getBoard().scale()).selectFrom(inside);
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
                if (entered || enemy.effectStacks(EffectKind.SUNDERED) == 0) {
                    this.applyStacks(enemy, StackingDebuff.SUNDERED, mark.stacks());
                }
            }
            case SCORCH -> {
                if (entered || enemy.effectStacks(EffectKind.SCORCHED) == 0) {
                    this.applyEffect(enemy, sink -> Effect.scorched(mark.stacks()));
                }
            }
            case TOLL -> {
                if (entered || enemy.effectStacks(EffectKind.TOLL) == 0) {
                    this.applyEffect(enemy, sink -> Effect.toll(mark.stacks(), PulseSpec.TOLL_CAP));
                }
            }
            case CRACK -> this.applyEffect(enemy, sink -> Effect.cracked(MortarTower.CRACKED_TICKS, sink));
            case CHILL -> this.applyEffect(enemy, sink -> Effect.chill(PrismMark.CHILL_STRENGTH, CHILL_TICKS, sink));
            case STATIC -> this.lightStatic.tag(enemy);
            case SOULBRAND -> this.lightSoulbrand.tag(enemy);
        }
    }

    /** The light's Static discharges once badly hurt and its Soulbrand bursts once killed, as the Burst's do. */
    private void settleTags() {
        this.lightStatic.take(enemy -> enemy.isDead() || enemy.getHealthFraction() <= StaticMechanic.DISCHARGE_AT)
                .stream()
                .filter(enemy -> !enemy.isDead())
                .forEach(enemy -> this.dealDamage(enemy,
                        Damage.magic(DamageUnits.ofPoints(StaticMechanic.DISCHARGE_POINTS)),
                        this.stats().attack().withoutCrits()));
        this.lightSoulbrand.take(EnemyMob::isDead).stream()
                .filter(EnemyMob::wasKilled)
                .forEach(this::burstSoul);
    }

    private void burstSoul(EnemyMob dead) {
        float reach = SoulbrandMechanic.BURST_CELLS * this.context.getBoard().scale();
        InRangeTargetQuery.everyone((int) dead.getX(), (int) dead.getY(), reach)
                .matching(this.context.enemies()).stream()
                .filter(enemy -> enemy != dead && !enemy.isDead())
                .forEach(enemy -> this.dealDamage(enemy, Damage.magic(DamageUnits.ofPoints(SoulbrandMechanic.BURST_POINTS)),
                        this.stats().attack().withoutCrits()));
    }

    /** Breakthrough: a Lit tower whose tier rose this tick teaches every other Lit tower. */
    private void teachBreakthroughs(int gameTime) {
        List<Tower> lit = this.litTowers();
        for (Tower opener : lit) {
            if (opener.tierRise().ticksSince(gameTime, 0).isPresent()) {
                lit.stream().filter(tower -> tower != opener).forEach(tower -> tower.earnXp(BreakthroughMechanic.XP));
            }
        }
    }

    /** The beam lets go and the light's tags are dropped; what an enemy carries is its own. */
    @Override
    protected void endFight() {
        this.held = Optional.empty();
        this.reveals = List.of();
        this.lastInside = Collections.newSetFromMap(new IdentityHashMap<>());
        this.lightStatic = new TagLedger();
        this.lightSoulbrand = new TagLedger();
    }

    @Override
    protected List<AppliedEffect> baseApplies() {
        float seconds = this.stats().value(TowerStat.REVEAL_TIME) / TickRate.TICKS_PER_SECOND;
        return List.of(new AppliedEffect(EffectKind.REVEALED, List.of(new AppliedEffect.Source(this.displayName(),
                        TermText.of("every enemy in the ").plus(TowerTerm.LIGHT)
                                .plus(", while inside and " + TermNumbers.seconds(seconds) + " after")))),
                new AppliedEffect(TowerTerm.LIT, List.of(new AppliedEffect.Source(this.displayName(),
                        TermText.of("every other tower in the ").plus(TowerTerm.LIGHT).plus(": ")
                                .plus(TowerStat.DISRUPTION_TAKEN.changeText(this.light().get(TowerStat.DISRUPTION_TAKEN)))))));
    }

    @Override
    protected String description() {
        return "Never attacks. Its light shows the hidden and shields the towers in it from jam; a Beacon never "
                + "lights another Beacon, and lights that overlap stack.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitAuraTower(this);
    }

    private record FoldedSpec(List<AuraMechanic> mechanics, LightSpec spec) {
    }
}
