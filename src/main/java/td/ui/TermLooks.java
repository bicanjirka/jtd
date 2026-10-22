package td.ui;

import td.economy.StatusTerm;
import td.effect.EffectKind;
import td.enemy.AbilityTerm;
import td.enemy.TraitTerm;
import td.term.Term;
import td.tower.TowerFactory;
import td.tower.TowerTerm;
import td.tower.upgrade.UpgradeNode;
import td.ui.render.IconFace;
import td.ui.render.Palette;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The face of every term and perk line with no live state. A term's art is keyed by its key; its look comes
 * from one exhaustive switch per term family, built into one map so no caller branches on a type.
 */
public final class TermLooks {

    private static final Map<String, TermLook> BY_KEY = build();
    private static final String[] NUMERALS = {"", "I", "II", "III", "IV"};

    private TermLooks() {
    }

    public static IconFace faceOf(Term term) {
        TermLook look = Objects.requireNonNull(BY_KEY.get(term.key()), () -> "No look for " + term.key());
        return IconFace.of(term.key(), monogram(term.title()), look.tone(), look.frame());
    }

    /** The face of a perk line of {@code type} whose art is {@code artNode}'s, at {@code level}; 0 shows no numeral. */
    public static IconFace faceOfLine(TowerFactory.Type type, UpgradeNode artNode, int level) {
        IconFace face = IconFace.of(artNode.id(), monogram(artNode.displayName()), perkTone(type, artNode),
                TowerSpriteFrameBuilder.slotPaletteFor(artNode.slot()));
        return level > 0 ? face.withNumeral(NUMERALS[level]) : face;
    }

    static String monogram(String title) {
        return title.length() <= 2 ? title : title.substring(0, 2);
    }

    private static Map<String, TermLook> build() {
        Map<String, TermLook> looks = new HashMap<>();
        Arrays.stream(EffectKind.values()).forEach(kind -> looks.put(kind.key(), effect(kind)));
        Arrays.stream(TowerTerm.values()).forEach(term -> looks.put(term.key(), tower(term)));
        Arrays.stream(TraitTerm.values()).forEach(term -> looks.put(term.key(), trait(term)));
        Arrays.stream(AbilityTerm.values()).forEach(term -> looks.put(term.key(), ability(term)));
        Arrays.stream(StatusTerm.values()).forEach(term -> looks.put(term.key(), status(term)));
        return Map.copyOf(looks);
    }

    private static TermLook effect(EffectKind kind) {
        return new TermLook(EnemyFrameBuilder.markerPaletteFor(kind), switch (kind) {
            case CHILL, FREEZE, DAZED, SILENCED, ANCHORED, DRAGGED, UNDERTOW -> Palette.TERM_FRAME_CONTROL;
            case BURN, POISON, BLEEDING, SOULFIRE -> Palette.TERM_FRAME_AFFLICTION;
            case VULNERABLE, SCORCHED, SICKENED, SUNDERED, RESONATING, FRACTURED, SATURATED, UNRAVELED, BRITTLE, TOLL, CORRODED, CONDEMNED, STILLED, DEAD_ZONE, FALLOUT, FRAIL, CRACKED, TARRED -> Palette.TERM_FRAME_WEAKNESS;
            case DOOM, BLIGHT, CONTAGION, RIME, ASH, INVERSION, SYMPATHY, RECKONING -> Palette.TERM_FRAME_HEX;
            case REVEALED, EXPOSED, MARKED, PRIORITY, CHARGED, UNVEILED -> Palette.TERM_FRAME_MARK;
            case SHIELD, INVISIBLE, HEAL, SPRINTING, HASTENED -> Palette.TERM_FRAME_BOON;
        });
    }

    private static TermLook tower(TowerTerm term) {
        return switch (term) {
            case STEADY_AIM -> new TermLook(Palette.TOWER_SNIPER_LASER, Palette.TERM_FRAME_TOWER_BUFF);
            case CRIT_STREAK -> new TermLook(Palette.CRIT_SPARK, Palette.TERM_FRAME_TOWER_BUFF);
            case FOCUS -> new TermLook(Palette.TOWER_SEEKER_BODY, Palette.TERM_FRAME_TOWER_BUFF);
            case FRENZY -> new TermLook(Palette.TERM_FRENZY, Palette.TERM_FRAME_TOWER_BUFF);
            case HASTE -> new TermLook(Palette.TERM_HASTE, Palette.TERM_FRAME_TOWER_BUFF);
            case CHARGED_SHOT -> new TermLook(Palette.STATUS_MARKER_CHARGED, Palette.TERM_FRAME_TOWER_BUFF);
            case PATIENCE -> new TermLook(Palette.TERM_PATIENCE, Palette.TERM_FRAME_TOWER_BUFF);
            case DEADEYE_ROUND -> new TermLook(Palette.TERM_DEADEYE, Palette.TERM_FRAME_TOWER_BUFF);
            case SILVER_ROUND -> new TermLook(Palette.TOWER_SNIPER_SILVER, Palette.TERM_FRAME_TOWER_BUFF);
            case BRACKETING -> new TermLook(Palette.TOWER_MORTAR_RANGING, Palette.TERM_FRAME_TOWER_BUFF);
            case NAPALM_SHELL -> new TermLook(Palette.PROJECTILE_NAPALM, Palette.TERM_FRAME_TOWER_BUFF);
            case TAR_SHELL -> new TermLook(Palette.TERM_TAR_SHELL, Palette.TERM_FRAME_TOWER_BUFF);
            case CRYO_SHELL -> new TermLook(Palette.PROJECTILE_FROST, Palette.TERM_FRAME_TOWER_BUFF);
            case NUKE_SHELL -> new TermLook(Palette.TERM_NUKE_SHELL, Palette.TERM_FRAME_TOWER_BUFF);
            case CRYO_MISSILE -> new TermLook(Palette.PROJECTILE_CRYO, Palette.TERM_FRAME_TOWER_BUFF);
            case ARCANE_MISSILE -> new TermLook(Palette.PROJECTILE_ARCANE, Palette.TERM_FRAME_TOWER_BUFF);
            case EMP_MISSILE -> new TermLook(Palette.PROJECTILE_EMP, Palette.TERM_FRAME_TOWER_BUFF);
            case TRACER_MISSILE -> new TermLook(Palette.PROJECTILE_TRACER, Palette.TERM_FRAME_TOWER_BUFF);
            case EVENT_HORIZON -> new TermLook(Palette.TERM_EVENT_HORIZON, Palette.TERM_FRAME_TOWER_BUFF);
            case RALLY -> new TermLook(Palette.TOWER_AURA_RALLY, Palette.TERM_FRAME_TOWER_BUFF);
            case AURA -> new TermLook(Palette.TERM_AURA, Palette.TERM_FRAME_TOWER_BUFF);
            case LEY_LINE -> new TermLook(Palette.TERM_LEY_LINE, Palette.TERM_FRAME_TOWER_BUFF);
            case NEST -> new TermLook(Palette.TOWER_SEEKER_BODY, Palette.TERM_FRAME_TOWER_BUFF);
            case JAMMED -> new TermLook(Palette.DISRUPTION, Palette.TERM_FRAME_TOWER_DEBUFF);
            case INTERFERENCE -> new TermLook(Palette.TERM_INTERFERENCE, Palette.TERM_FRAME_TOWER_DEBUFF);
            case STOKE -> new TermLook(Palette.TOWER_CINDER_CONE, Palette.TERM_FRAME_MECHANIC);
            case KINSHIP -> new TermLook(Palette.TERM_AURA, Palette.TERM_FRAME_MECHANIC);
            case PING -> new TermLook(Palette.TOWER_SONAR_PING, Palette.TERM_FRAME_MECHANIC);
            case SHATTER -> new TermLook(Palette.FREEZE_CRYSTAL, Palette.TERM_FRAME_MECHANIC);
            case ARC -> new TermLook(Palette.TOWER_SPLASH_ARC, Palette.TERM_FRAME_MECHANIC);
            case HEX -> new TermLook(Palette.HEX_RUNE, Palette.TERM_FRAME_MECHANIC);
            case SHRAPNEL -> new TermLook(Palette.TERM_SHRAPNEL, Palette.TERM_FRAME_MECHANIC);
            case BOMBLET -> new TermLook(Palette.TERM_SHRAPNEL, Palette.TERM_FRAME_MECHANIC);
        };
    }

    private static TermLook status(StatusTerm term) {
        return new TermLook(switch (term) {
            case WAVE -> Palette.TERM_STATUS_WAVE;
            case LIVES -> Palette.TERM_STATUS_LIVES;
            case SCORE -> Palette.TERM_STATUS_SCORE;
            case CASH -> Palette.TERM_STATUS_CASH;
        }, Palette.TERM_FRAME_STATUS);
    }

    private static TermLook trait(TraitTerm term) {
        return new TermLook(switch (term) {
            case REGENERATION -> Palette.TRAIT_MARKER_REGENERATION;
            case ADRENALINE -> Palette.TRAIT_MARKER_HURT_SPEED;
            case ELUSIVE -> Palette.TRAIT_MARKER_HURT_AGILITY;
            case ADAPTATION -> Palette.TRAIT_MARKER_PERCENT_RESIST;
            case JAM -> Palette.DISRUPTION;
        }, Palette.TERM_FRAME_TRAIT);
    }

    private static TermLook ability(AbilityTerm term) {
        return new TermLook(switch (term) {
            case BRACE, SHIELD_WALL, GUARD, AEGIS, HARDEN, PANIC, WARD, LAST_WARD, WARD_MINE -> Palette.STATUS_MARKER_SHIELD;
            case CAPACITOR, BULWARK, ARCANE_WARD -> Palette.STATUS_MARKER_CHARGED;
            case MEND, RECOVER, LAST_RITES, SHED_SKIN, HUNKER, HEALING_POOL -> Palette.STATUS_MARKER_HEAL;
            case BOLT, SIDESTEP, SECOND_WIND, HASTE_FIELD -> Palette.STATUS_MARKER_SPRINTING;
            case BLINDING_BURST -> Palette.DISRUPTION;
            case FADE, SHROUD, PHASE, VANISH -> Palette.STATUS_MARKER_INVISIBLE;
            case REINFORCE, SWARM, BROOD, SPLIT, LAY_EGG, HATCH -> Palette.TERM_SPAWN;
        }, Palette.TERM_FRAME_ABILITY);
    }

    private static Palette perkTone(TowerFactory.Type type, UpgradeNode node) {
        String id = node.id();
        if (id.startsWith("base.")) {
            return Palette.TERM_BASE_PERK;
        }
        if (type == TowerFactory.Type.SPLASH) {
            if (id.startsWith("splash.head.arc.") || id.equals("splash.special.thunderclap")
                    || id.equals("splash.special.static_charge") || id.equals("splash.special.thunderstrike")) {
                return Palette.TOWER_STORMCALLER_BODY;
            }
            if (id.startsWith("splash.head.hex.") || id.equals("splash.special.inversion")
                    || id.equals("splash.special.sympathy") || id.equals("splash.special.reckoning")) {
                return Palette.TOWER_HEXER_BODY;
            }
            return Palette.TOWER_SPLASH_BODY;
        }
        return TowerSpriteFrameBuilder.bodyPaletteFor(type);
    }
}
