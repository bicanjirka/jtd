package td.util;

import td.TowerDefence;
import td.enemy.EnemyMob;
import td.tower.Tower;
import td.wave.Path;
import td.wave.PathNormal;
import td.wave.Wave;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Herni kontext, model, obsahuje informace o deni ve hre
 * a obhospodarovava je
 *
 * @author Juras
 *
 */
public class Context {

    /**
     * Rozliseni herniho policka v px
     */
    public int scale = 32;
    /**
     * Souradnice nejvzdalenejsiho bodu herni plochy
     */
    public int maxX, maxY;
    /**
     * Pole nepratel aktualni vlny
     */
    public EnemyMob[] enemies;
    /**
     * Vsechny postavene veze na herni plose
     */
    public List<Tower> towers;
    private final TowerDefence mainApp;
    private int enemyCount = 0;
    private Path path;

    private final int livesMax = 5;
    private int lives = 5;
    private int credits = 0;
    private int score = 0;
    /**
     * Testovani zpusobovaneho zraneni, slouzi k ladeni hry
     */
    //public long dmg = 0; //TEST damage

    private final List<ContextListener> contextListeners;
    private final List<TowerListener> towerListeners;
    private final List<WaveStartListener> waveListeners;

    private final Cache cache;

    /**
     * Vytvori herni kontext s referenci na hlavni aplikaci programu
     *
     * @param mainApp - hlavni program
     */
    public Context(TowerDefence mainApp) {
        this.mainApp = mainApp;
        this.contextListeners = new CopyOnWriteArrayList<ContextListener>();
        this.towerListeners = new CopyOnWriteArrayList<TowerListener>();
        this.waveListeners = new CopyOnWriteArrayList<WaveStartListener>();
        this.towers = new CopyOnWriteArrayList<Tower>();
        this.path = new PathNormal(this.scale);
        this.cache = Cache.getInstance();
    }

    /**
     * Nastavi pocet nepratel zadane vlny
     *
     * @param w - pozadovana vlna
     */
    public void startWave(Wave w) {
        this.setEnemyCount(w.enemyCount());
        this.fireWaveStartedEvent();
    }

    public void addWaveStartListener(WaveStartListener l) {
        this.waveListeners.add(l);
    }

    public void removeWaveStartListener(WaveStartListener l) {
        this.waveListeners.remove(l);
    }

    private void fireWaveStartedEvent() {
        for (WaveStartListener l : this.waveListeners) {
            l.waveStarted();
        }
    }

    /**
     * Nastavi pocet nepratel
     *
     * @param c - pocet nepratel
     */
    public void setEnemyCount(int c) {
        this.enemyCount = c;
    }

    /**
     * Snizi pocet zbylich nepratel o 1 a preda hlavni aplikaci
     * zpravu o smrti a aktualni pocet
     */
    public void removeEnemy() {
        this.enemyCount--;
        this.mainApp.enemyDied(this.enemyCount);
    }

    /**
     * Zrusi vsechny nepratele a nastavi pocet zbylich na 0
     */
    public void removeAllEnemies() {
        this.enemyCount = 0;
        this.enemies = new EnemyMob[0];
        this.mainApp.enemyDied(this.enemyCount);
    }

    /**
     * Preda hlavni aplikaci pozadavek na zobrazeni textu
     *
     * @param s - text
     */
    public void setInfoText(String s) {
        this.mainApp.setInfoText(s);
    }

    /**
     * Pricte k hracovu <b>score</b> pozadovany obnos
     *
     * @param s - score
     */
    public void addScore(int s) {
        if (s < 0) {
            System.out.println("Context::addScore: Adding negative score? " + s);
        }
        this.score += s;
    }

    /**
     * Odecte od hracova <b>score</b> pozadovany obnos
     *
     * @param s - score
     */
    public void deductScore(int s) {
        if (s < 0) {
            System.out.println("Context::deductScore: Deducting negative score? " + s);
        }
        this.score -= s;
    }

    /**
     * Vrati, jake je aktualni hracovo score
     *
     * @return - score
     */
    public int getScore() {
        return this.score;
    }

    /**
     * Vynuluje score
     */
    public void resetScore() {
        this.score = 0;
    }

    /**
     * Vrati, kolik ma prave hrac penez
     *
     * @return - penize
     */
    public int getCredits() {
        return this.credits;
    }

    /**
     * Nastavi obnos penez
     *
     * @param credits - penize
     */
    public void setCredits(int credits) {
        this.credits = credits;
        this.fireMoneyChangedEvent();
    }

    /**
     * Zjisti, jestli ma hrac poterbny obnos penez
     *
     * @param amount - kolik ma mit
     * @return - jestli je ma
     */
    public boolean canPay(int amount) {
        //System.out.println("Context::canPay: checking "+amount);
        return (this.credits >= amount);
    }

    /**
     * Zjisti, jestli ma hrac pozadovany obnos penez a zaroven
     * provede odecet pokud je ma
     *
     * @param amount - kolik chceme odecist
     * @return - jestli jsme odecetli
     */
    public boolean doPay(int amount) {
        //System.out.println("Context::doPay: checking "+amount);
        if (this.canPay(amount)) {
            this.credits -= amount;
            //System.out.println("Context::doPay: credits: "+this.credits);
            this.fireMoneyChangedEvent();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Pricte penize k celkovemu obnosu
     *
     * @param amount - kolik pricitame
     */
    public void doReceive(int amount) {
        this.credits += amount;
        this.fireMoneyChangedEvent();
    }

    /**
     * Pridavame novou vez
     *
     * @param t - vez
     */
    public void addTower(Tower t) {
        this.towers.add(t);
        this.fireTowerAddedEvent(t);
    }

    /**
     * Vymazeme existujici vez
     *
     * @param t - vez
     */
    public void sellTower(Tower t) {
        int cellX = t.getX() / this.scale;
        int cellY = t.getY() / this.scale;
        this.mainApp.clearCell(cellX, cellY);
        t.doCleanup();
        this.towers.remove(t);
        this.doReceive(t.getSellPrice());
        this.fireTowerRemovedEvent(t);
    }

    /**
     * Vycisti seznam vsech vezi
     */
    public void clearTowers() {
        for (Tower t : this.towers) {
            int cellX = t.getX() / this.scale;
            int cellY = t.getY() / this.scale;
            this.mainApp.clearCell(cellX, cellY);
            t.doCleanup();
            this.towers.remove(t);
            this.fireTowerRemovedEvent(t);
        }
    }

    public void addTowerListener(TowerListener l) {
        this.towerListeners.add(l);
    }

    public void removeTowerListener(TowerListener l) {
        this.towerListeners.remove(l);
    }

    private void fireTowerAddedEvent(Tower t) {
        for (TowerListener l : this.towerListeners) {
            l.towerBuild(t);
        }
    }

    private void fireTowerRemovedEvent(Tower t) {
        for (TowerListener l : this.towerListeners) {
            l.towerRemoved(t);
        }
    }

    /**
     * Prida listener
     *
     * @param l - ContextListener
     * @see ContextListener
     */
    public void addContextListener(ContextListener l) {
        this.contextListeners.add(l);
    }

    /**
     * Odebere listener
     *
     * @param l - ContextListener
     * @see ContextListener
     */
    public void removeContextListener(ContextListener l) {
        this.contextListeners.remove(l);
    }

    /**
     * Vyvola udalost zmeny penez
     *
     * @see ContextListener
     */
    private void fireMoneyChangedEvent() {
        for (ContextListener l : this.contextListeners) {
            l.moneyChanged();
        }
    }

    /**
     * Vyvola udalost zmeny zivotu
     *
     * @see ContextListener
     */
    private void fireLivesChangedEvent() {
        for (ContextListener l : this.contextListeners) {
            l.livesChanged();
        }
    }

    /**
     * Vrati cestu
     *
     * @return - cesta
     */
    public Path getPath() {
        return this.path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    /**
     * Vrati zivoty
     *
     * @return - zivoty
     */
    public int getLives() {
        return lives;
    }

    /**
     * Odebere jeden zivot
     */
    public void removeLife() {
        this.lives--;
        this.fireLivesChangedEvent();
    }

    /**
     * Resetuje zivoty na jejich maximalni hodnotu
     */
    public void resetLives() {
        this.lives = this.livesMax;
        this.fireLivesChangedEvent();
    }

    /**
     * Vrati uloziste (cache)
     *
     * @return - cache
     * @see Cache
     */
    public Cache getCache() {
        return this.cache;
    }

}