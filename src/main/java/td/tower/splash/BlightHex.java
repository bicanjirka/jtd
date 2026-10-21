package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.term.TermText;
import td.tower.targeting.MostNeighboursSelector;

import java.util.Optional;

/** Hex of Blight: poisons the enemy for as long as the hex lasts. Picks the most crowded enemy without Blight. */
public final class BlightHex implements Hex {

    @Override
    public EffectKind kind() {
        return EffectKind.BLIGHT;
    }

    @Override
    public Optional<EnemyMob> target(HexScene scene) {
        return new MostNeighboursSelector(scene.blastRadius()).selectFrom(scene.candidates().stream()
                .filter(enemy -> !enemy.hasEffect(EffectKind.BLIGHT))
                .toList());
    }

    @Override
    public void cast(EnemyMob target, HexSpec spec, HexActions actions) {
        int ticks = this.ticksOn(target);
        actions.curse(target, EffectKind.BLIGHT, ticks);
        actions.poison(target, spec.blightShare(), ticks);
    }

    @Override
    public TermText describe() {
        return TermText.of(EffectKind.BLIGHT, TermText.of("the enemy has ").plus(EffectKind.POISON).plus(" for as long as it lasts"));
    }
}
