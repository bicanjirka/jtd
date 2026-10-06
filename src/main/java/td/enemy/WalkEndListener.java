package td.enemy;

/** Told on the game-loop thread when a mob's walk ends, killed or leaked. */
public interface WalkEndListener {

    void walkEnded(EnemyWalk walk);
}
