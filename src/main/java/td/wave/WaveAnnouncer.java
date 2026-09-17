package td.wave;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The "a wave just started" broadcast - orthogonal to the economy, enemy roster, and tower
 * roster, despite having lived bundled alongside them in GameWorld. SonarTower is the only
 * subscriber today, clearing the hit markers its scan left on the previous wave's enemies.
 */
public class WaveAnnouncer {

    private final List<WaveStartListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(WaveStartListener l) {
        this.listeners.add(l);
    }

    public void removeListener(WaveStartListener l) {
        this.listeners.remove(l);
    }

    public void announce() {
        for (WaveStartListener l : this.listeners) {
            l.waveStarted();
        }
    }
}
