package td.enemy;

import td.stat.DisruptionAura;

/** The Jammer ({@code j}): slows and blinds every tower near it. */
final class JammerEnemy {

    private JammerEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(EnemyDefinition.of("j", "Jammer", 120, 6, 1.0f, BodyArchetype.SQUARE)
                .withDescription("Jams every tower near it: they fire more slowly and see less far while it's in range.")
                .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
                .withDisruption(new DisruptionAura(100f, 0.3f, 0.2f)));
    }
}
