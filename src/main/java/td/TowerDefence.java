package td;

import td.cell.Cell;
import td.cell.CellNormal;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.ui.GameBoard;
import td.util.Cache;
import td.util.Context;
import td.util.ContextListener;
import td.wave.Path;
import td.wave.Wave;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Hlavni okno programu TowerDefence<br>
 * Inspirovano HexTD
 *
 * @author Juras
 *
 */
public class TowerDefence extends JFrame implements Runnable, ContextListener {

    private static final long serialVersionUID = 1L;
    private static final String NAME = "Tower Defence";
    private static final String VERSION = "1.3";

    private static final int TICKTIME = 50;
    private static final int FASTTICKTIME = 15;
    private static final int SUPERFASTTICKTIME = 3;

    private final Context context;
    private final List<Tower> towers;
    private final GameBoard gameBoard;
    private BufferedImage backGround;
    private Cell[][] cellGrid;
    private final String statusMessage = """
            Welcome to TowerDefence
            Shortcuts:
            
            q - build triangle
            w - build circle
            e - build spiral
            r - build star
            t - build jing-jang
            p - pause
            f - slow speed
            s - star wave""";

    private boolean startWave = false;
    private boolean waveReady = true;
    private boolean placingTower = false;
    private TowerFactory.type placingTowerType;
    private float placingTowerRange = 0;

    private final Object gameTimeLock = new Object();
    private int gameTime;
    private final Object paintLock = new Object();
    private boolean painting = false;
    private int tickTime = TICKTIME;
    private boolean paused = false;
    private boolean gameStopped = false;
    private int[] highlitedCell;
    private int wave = 0;
    private List<Wave> waves;

    /* TODO
     * dopsat do vezi, aby vedely, kolik daly dmg, kolik jich zabily, jaky jsou borci
     * vyhledavani v context.enemies[] resit pres iteratory a udelat ho privatni. Pri ziskavani iteratoru si rict, podle ceho maji byt setrideny (rychlost, progression, vzdalenost od bodu, pocet zivotu, ...) / delat to pres setridena pole
     * vyladit nepratele, protoze se tam opakuje neustale kod. Udelat metodu na to, kdyz chcipaj
     *
     */
    private javax.swing.JButton jButton_play;
    private javax.swing.JButton jButton_pause;
    private javax.swing.JButton jButton_fast;
    private javax.swing.JButton jButton_superFast;
    private javax.swing.JLabel jLabel_waveText;
    private javax.swing.JLabel jLabel_gameLostText;
    private javax.swing.JLabel jLabel_gameWonText;
    private javax.swing.JLabel jLabel_creditsText;
    private javax.swing.JLabel jLabel_livesText;
    private javax.swing.JLabel jLabel_name;
    private javax.swing.JLabel jLabel_scoreText;
    private javax.swing.JLabel jLabel_credits;
    private javax.swing.JLabel jLabel_lives;
    private javax.swing.JLabel jLabel_score;
    private javax.swing.JLabel jLabel_wave;
    private td.ui.PanelTowerSelector panelTowerSelector;
    private javax.swing.JPanel jPanel_gameLost;
    private javax.swing.JPanel jPanel_gameWon;
    private javax.swing.JPanel jPanel_board;
    private javax.swing.JPanel jPanel_console;
    private javax.swing.JPanel jPanel_gameButtons;
    private javax.swing.JPanel jPanel_gameInfo;
    private td.ui.PanelTowerInfo panelTowerInfo;
    private td.ui.PanelWaveInfo panelWaveInfo;

    {
        this.setLayout(null);
        //this.setSize(900, 600);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle(NAME);
        this.setFocusable(true);
        this.setVisible(true);
    }

    public TowerDefence() {

        System.out.println("Starting TowerDefence.");
        this.context = new Context(this);
        this.towers = this.context.towers;
        this.context.addContextListener(this);
        Cache cache = this.context.getCache();
        //nahraje obrazek pozadi
        if (cache.hasBufImg("bg")) {
            this.backGround = cache.getBufImg("bg");
        } else {
            //TODO prazdny obdelnik
        }
        //inicializuje vsechny komponenty
        this.gameBoard = new GameBoard(this, this.context);
        initComponents();
        this.panelWaveInfo.setContext(this.context);
        this.panelTowerSelector.doInit(this.context, this);
        this.panelTowerInfo.setContext(this.context);

        this.jPanel_board.add(this.gameBoard);

        loadTestLevel();
        Thread th = new Thread(this);
        th.start();
    }

    @Override
    public void run() {
        long oldTime, newTime, sleepTime;
        oldTime = System.nanoTime();
        int time;
        boolean stillPainting;

        //herni cas, zacina na 0 a s kazdym tiknutim roste o 1
        this.gameTime = 0;
        //nekonecna smycka
        while (true) {
            if (!this.gameStopped) {
                if (!this.paused) {
                    synchronized (this.gameTimeLock) {
                        time = ++this.gameTime;
                    }
                    this.doTick(time);
                }
                synchronized (this.paintLock) {
                    stillPainting = this.painting;
                }
                if (!stillPainting) {
                    //this.repaint();
                    this.gameBoard.repaint();
                }
            }
            newTime = System.nanoTime();
            sleepTime = oldTime + this.tickTime * 1_000_000L - newTime;
            //System.out.println("Sleeping for "+(sleepTime/1_000_000)+"ms");
            if (sleepTime < 0) {
                oldTime = newTime;
                sleepTime = 2_000_000L;
            }
            oldTime = oldTime + this.tickTime * 1_000_000L;
            try {
                //uspi vlakno na ~tickTime ms
                Thread.sleep(sleepTime / 1_000_000L, (int) (sleepTime % 1_000_000L));

            } catch (InterruptedException ex) {
                // do nothing
            }
        }
    }

    /**
     * Testovaci level<br>
     * Je vyvazen a odladen na stredne tezkou obtiznost<br>
     * Nacitani dat je udelano, aby se v pozdejsich verzich daly nacitat lvly <b>ze souboru</b>
     */
    private void loadTestLevel() {

        int width = 20;        //sirka hraciho planu
        int height = 15;    //vyska hraciho planu
        this.cellGrid = new Cell[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                this.cellGrid[i][j] = new CellNormal(i * this.context.scale, j * this.context.scale, this.context);
            }
        }
        this.gameBoard.recalculateBoard(width, height);
        this.context.maxX = (this.cellGrid.length) * this.context.scale - 1;
        this.context.maxY = (this.cellGrid[0].length) * this.context.scale - 1;
        this.waves = new ArrayList<Wave>();
        Path path = this.context.getPath();
        //int[] pathx = {3,3,3,3,3,3,3,3,3,3,4,5,6,7,8,9,10,11,11,11,11,12,13,14,14,14,14,14,14,15,16,16,16,16,15,14,13,12,11,10,9,9,9,9,9};
        //int[] pathy = {-1,0,1,2,3,4,5,6,7,8,8,8,8,8,8,8,8,8,9,10,11,11,11,11,10,9,8,7,6,6,6,5,4,3,3,3,3,3,3,3,3,2,1,0,-1};
        int[] pathx = {-1, 0, 1, 2, 3, 4, 5, 5, 6, 7, 7, 7, 7, 7, 7, 7, 6, 5, 4, 4, 3, 3, 3, 3, 4, 5, 6, 6, 7, 8, 9, 10, 11, 11, 11, 12, 13, 14, 14, 14, 15, 16, 17, 17, 17, 17, 16, 15, 15, 15, 15, 14, 13, 12, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        int[] pathy = {11, 11, 11, 11, 11, 11, 11, 12, 12, 12, 11, 10, 9, 8, 7, 6, 6, 6, 6, 5, 5, 4, 3, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 5, 5, 5, 5, 4, 3, 3, 3, 3, 4, 5, 6, 6, 6, 7, 8, 9, 9, 9, 9, 10, 11, 12, 12, 12, 12, 12, 12, 12, 12, 12};
        for (int i = 0; i < pathx.length; i++) {
            path.addStep(pathx[i], pathy[i]);
        }
        path.finalise(this.cellGrid);
        this.wave = 0;

//		this.makeWave("c 2 e 2 g 3 s 4 t 1 g", 50, 0, 2); //test wave
        this.makeWave("c e c e c e c e c", 251, 2, 1);                //5x2=10
        this.makeWave("c e 2 c e 3 c e 4 c", 377, 3, 1);            //10x3=30
        this.makeWave("c e c", 812, 10, 2);                            //2x10=20
        this.makeWave("4 c 2 e 2 s", 747, 5, 1);                    //6x5=30
        this.makeWave("c c e s", 1109, 15, 3);                        //3x15=45
        this.makeWave("10 c", 953, 2, 1);                            //10x2=20
        this.makeWave("3 s e 4 c t e s t", 1117, 4, 2);                //10x4=40
        this.makeWave("2 c e e t", 2193, 15, 4);                    //3x15=45
        this.makeWave("g 2 e 2 s", 1493, 10, 2);                    //3x10=30
        this.makeWave("s t s c g c t c s g t c s g c t s g t c", 1476, 2, 2);    //20x2=40
        this.makeWave("g c g", 3789, 15, 4);                        //3x15=45
        this.makeWave("6 g 2 e 4 t", 3088, 7, 3);                    //10x7=70
        this.makeWave("c e c e c e c e c", 2912, 1, 2);                //5x1=5
        this.makeWave("2 s 3 t 2 g 4 e c", 3242, 10, 3);            //8x10=80
        this.makeWave("s 4 e t", 4014, 50, 6);                        //2x50=100
        this.makeWave("c 5 e 3 g 3 e 3 s 3 t", 4016, 4, 4);            //10x4=40
        this.makeWave("s", 4751, 0, 8);

        this.context.setCredits(50/*+10+30+20+30+45+20+40+45+30+40+45+70+5+80+100+40*/);
        this.jPanel_gameLost.setVisible(false);
        this.jPanel_gameWon.setVisible(false);
        this.startLevel();
    }

    /**
     * Metoda ulehcujici praci v <b>loadTestLevel()</b>
     *
     * @param s     - vycet co vlna obsahuje
     * @param hp    - kolik zivotu ma kazdy Enemy
     * @param price - kolik penez za zabiti
     * @param lvl   - cislo vlny
     * @see TowerDefence#loadTestLevel()
     */
    private void makeWave(String s, int hp, int price, int lvl) {
        Wave wave = new Wave(this.context, hp, price, lvl);
        wave.addEnemiesFromNames(s.split(" "));
        this.waves.add(wave);
    }

    /**
     * Nastavi text v informacnim panelu a zrusi vybranou vez z ukazatele
     *
     * @param s - text do informacniho panelu
     */
    public void setInfoText(String s) {
        this.unSelectTower();
        this.panelTowerInfo.setExternalText(s);
    }

    /**
     * Kontroluje pocet zbylych nepratel<br>
     * Kdyz 0, zpristupni nebo vysle dalsi vlnu - v zavislosti na <b>Auto Start</b>, nebo zavola vyhru ci prohru
     *
     * @param enemiesLeft - pocet zbylych nepratel
     */
    public void enemyDied(int enemiesLeft) {
        if (enemiesLeft == 0 && this.wave < this.waves.size()) {
            this.waveReady = true;
            this.jButton_play.setVisible(true);
            this.jButton_pause.setVisible(false);
        } else if (enemiesLeft == 0 && this.wave >= this.waves.size()) {
            this.gameWon();
        }
    }

    /**
     * Komunikuje s panelem s vlnama a posila mu informace o vlne aktualni a nasledujici
     */
    private void setWavePreview() {
        this.panelWaveInfo.clearWaves();
        if (this.wave - 1 >= 0) {
            this.panelWaveInfo.setWaveCur(this.wave, this.waves.get(this.wave - 1));
        }
        if (this.wave < this.waves.size()) {
            this.panelWaveInfo.setWaveNext(this.wave + 1, this.waves.get(this.wave));
        }
    }

    /**
     * Vysle dalsi vlnu a aktualizuje veskere infomace
     */
    private void nextWave() {
        if (this.waveReady && this.wave < this.waves.size()) {
            this.startWave = false;
            this.waveReady = false;
            //System.out.println("Sending next wave: "+this.wave);
//            this.jButton_nextWave.setEnabled(false);
            Wave tempWave = this.waves.get(this.wave);
            this.context.enemies = tempWave.getEnemies();
            this.context.startWave(tempWave);
            this.wave++;
            this.setWavePreview();
            this.updateInfo();
        }
    }

    /**
     * Aktualizuje vlny, penize, zivoty a score
     */
    private void updateInfo() {
        this.jLabel_wave.setText("  " + this.wave + "/" + this.waves.size());
        this.jLabel_credits.setText("$" + this.context.getCredits());
        this.jLabel_lives.setText("" + this.context.getLives());
        this.jLabel_score.setText("" + this.context.getScore());
    }

    public void moneyChanged() {
        this.updateInfo();
    }

    public void livesChanged() {
        this.updateInfo();
        if (this.context.getLives() <= 0) {
            this.gameLost();
        }
    }

    /**
     * Prohra<br>
     * Zobrazi panel a text o prohre
     */
    private void gameLost() {
        this.gameStopped = true;
        //this.gameBoard.setVisible(false);
        //this.jPanel_gameWon.setVisible(false);
        this.jPanel_gameLost.setVisible(true);
    }

    /**
     * Vyhra<br>
     * Zobrazi panel a text o vyhre
     */
    private void gameWon() {
        this.gameStopped = true;
        //this.gameBoard.setVisible(false);
        this.jPanel_gameWon.setVisible(true);
        //this.jPanel_gameLost.setVisible(false);
    }

    /**
     * Zacatek levelu
     */
    public void startLevel() {
        this.setWavePreview();
        this.waveReady = true;
    }

    /**
     * Provede metodu <b>doTick(int t)</b> na nepratelich a vezich
     *
     * @param time - cas, ve kterem tiknuti probiha
     */
    public void doTick(int time) {
        if (this.startWave) {
            this.nextWave();
        }
        if (this.context.enemies != null) {
            for (int i = 0; i < this.context.enemies.length; i++) {
                this.context.enemies[i].doTick(time);
            }
        }
        for (int i = 0; i < this.towers.size(); i++) {
            this.towers.get(i).doTick(time);
        }
        this.panelWaveInfo.doTick(time);
    }

    /**
     * Zmena rychlosti tikani
     */
    private void toggleGameSpeed(int speed) {
        synchronized (this.gameTimeLock) {
            this.tickTime = speed;
        }
    }

    /**
     * Obhospodaruje prekreslovani herni plochy<br>
     * Vykresluje pozadi<br>
     * Animuje pohyb ukazatele po plose pri stavbe vezi<br>
     * Vykresluje nepratele, veze a efekty strelby vezi
     *
     * @param g2 - grafika komponenty
     */
    public void paintBoard(Graphics2D g2) {
        synchronized (this.paintLock) {
            this.painting = true;
        }
        int time;

        synchronized (this.gameTimeLock) {
            time = this.gameTime;
        }

        //nastavi hezci vykreslovani grafiky
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        g2.drawImage(this.backGround, 0, 0, null);

        if (this.cellGrid != null) {
            for (int i = 0; i < this.cellGrid.length; i++) {
                for (int j = 0; j < this.cellGrid[0].length; j++) {
                    this.cellGrid[i][j].paintEffect(g2);
                }
            }
        }

        if (this.context.enemies != null) {
            for (int i = 0; i < this.context.enemies.length; i++) {
                this.context.enemies[i].paint(g2, time);
            }
        }

        for (int i = 0; i < this.towers.size(); i++) {
            this.towers.get(i).paint(g2, time);
        }

        for (int i = 0; i < this.towers.size(); i++) {
            this.towers.get(i).paintEffect(g2, time);
        }

        synchronized (this.paintLock) {
            this.painting = false;
        }
    }

    /**
     * Zacne pokladat vez
     *
     * @param t - typ veze
     * @param r - dostrel veze
     */
    public void startPlacing(TowerFactory.type t, float r) {
        this.placingTower = true;
        this.placingTowerType = t;
        this.placingTowerRange = r;
    }

    /**
     * Zrusi oznaceni veze, nastavi defaultni text informacniho panelu
     */
    public void unSelectTower() {
        this.unHighlightCell();
        this.panelTowerInfo.unselectTower();
        this.panelTowerInfo.setExternalText(this.statusMessage);
    }

    /**
     * Zrusi vez v hernim policku a zpristupni ho dalsimu staveni
     *
     * @param x - sloupec herniho policka
     * @param y - radek herniho policka
     */
    public void clearCell(int x, int y) {
        Cell cell = this.cellGrid[x][y];
        cell.unSetTower();
        cell.enable(true);
    }

    /**
     * Zrusi zvyrazneni herniho policka pokud je nejake zvyraznene
     */
    private void unHighlightCell() {
        if (this.highlitedCell != null) {
            this.cellGrid[this.highlitedCell[0]][this.highlitedCell[1]].setHighlight(Cell.highlightType.none);
            this.highlitedCell = null;
        }
    }

    /**
     * Zvyrazni herni policko - pokladani veze
     *
     * @param screenx - souradnice x kurzoru mysi
     * @param screeny - souradnice y kurzoru mysi
     */
    private void highlightCell(int screenx, int screeny) {
        this.unHighlightCell();
        //vypocet kde je mys na herni plose
        int x = screenx - this.gameBoard.getX();
        int y = screeny - this.gameBoard.getY();
        Cell cell;
        //pokud je na herni plose
        if (x >= 0 && x < this.gameBoard.getWidth()) {
            if (y >= 0 && y < this.gameBoard.getHeight()) {
                int[] tempInt = new int[2];
                tempInt[0] = x / this.context.scale;
                tempInt[1] = y / this.context.scale;
                //abychom vedeli jake herni policko je prave zvyraznene
                this.highlitedCell = tempInt;
                cell = this.cellGrid[x / this.context.scale][y / this.context.scale];
                cell.setHighlight(Cell.highlightType.place);
                cell.setHighlightRange(this.placingTowerRange);
            }
        }
    }

    /**
     * Klik mysi
     *
     * @param screenx  - souradnice x kurzoru mysi
     * @param screeny- souradnice y kurzoru mysi
     */
    private void mouseClicked(int screenx, int screeny) {

        //vypocet kde je mys na herni plose
        int x = screenx - this.gameBoard.getX();
        int y = screeny - this.gameBoard.getY();
        Cell cell;
        this.unSelectTower();
        //pokud je mys na herni plose
        if (x >= 0 && x < this.gameBoard.getWidth()) {
            if (y >= 0 && y < this.gameBoard.getHeight()) {
                //System.out.println("TowerDefence::mouseClicked: inside board " + x + ", " + y);
                //na toto herni policko sme klikli
                cell = this.cellGrid[x / this.context.scale][y / this.context.scale];
                //na hernim policku jiz vez stoji
                if (cell.hasTower()) {
                    this.panelTowerInfo.setTower(cell.getTower());
                    int[] tempInt = new int[2];
                    tempInt[0] = x / this.context.scale;
                    tempInt[1] = y / this.context.scale;
                    this.highlitedCell = tempInt;
                    //oznacime policko
                    cell.setHighlight(Cell.highlightType.select);
                } else
                    //pokladame vez
                    if (this.placingTower) {
                        if (cell.buildable()) {
                            if (this.context.doPay(this.placingTowerType.price)) {
                                //postavime vez
                                Tower tempTower = TowerFactory.createTower(this.placingTowerType, this.context, x / this.context.scale, y / this.context.scale);
                                this.context.addTower(tempTower);
                                cell.setTower(tempTower);
                                cell.enable(false);
                            }
                            this.placingTower = false;
                            this.panelTowerSelector.stopPlacing();
                        }
                    }
                //return;
            }
        }
        if (this.placingTower) {
            this.placingTower = false;
            this.panelTowerSelector.stopPlacing();
            //this.unHighlightCell();
        }
    }

    private void playPause(boolean play) {
        this.paused = !play;
        this.jButton_play.setVisible(!play);
        this.jButton_pause.setVisible(play);
    }

    /**
     * Stiskli jsme klavesu
     *
     * @param key - stisknuta klavesa
     */
    private void keyTyped(char key) {
        switch (key) {
            case 'q' -> this.panelTowerSelector.doPlace(0);
            case 'w' -> this.panelTowerSelector.doPlace(1);
            case 'e' -> this.panelTowerSelector.doPlace(2);
            case 'r' -> this.panelTowerSelector.doPlace(3);
            case 't' -> this.panelTowerSelector.doPlace(4);
            case 'p' -> this.playPause(this.paused);
            case 'f' -> this.toggleGameSpeed(this.tickTime += 10);            //TODO
            case 's' -> this.nextWave();
            case KeyEvent.VK_ESCAPE -> {
                if (this.placingTower) {
                    this.placingTower = false;
                    this.panelTowerSelector.stopPlacing();
                    this.unHighlightCell();
                }
            }
            default -> {
            }
        }
    }

    /**
     * Inicializace veskereho interface, volano z konstruktoru
     */
    private void initComponents() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Error setting native LAF: " + e);
        }

        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(KeyEvent evt) {
                formKeyTyped(evt);
            }
        });

        java.awt.GridBagConstraints gridBagConstraints;

        jPanel_board = new javax.swing.JPanel();
        jPanel_gameLost = new javax.swing.JPanel();
        jLabel_gameLostText = new javax.swing.JLabel();
        jPanel_gameWon = new javax.swing.JPanel();
        jLabel_gameWonText = new javax.swing.JLabel();
        jPanel_console = new javax.swing.JPanel();
        jLabel_name = new javax.swing.JLabel();
        jPanel_gameInfo = new javax.swing.JPanel();
        jLabel_waveText = new javax.swing.JLabel();
        jLabel_wave = new javax.swing.JLabel();
        jLabel_livesText = new javax.swing.JLabel();
        jLabel_lives = new javax.swing.JLabel();
        jLabel_scoreText = new javax.swing.JLabel();
        jLabel_score = new javax.swing.JLabel();
        jLabel_creditsText = new javax.swing.JLabel();
        jLabel_credits = new javax.swing.JLabel();
        jPanel_gameButtons = new javax.swing.JPanel();
        jButton_play = new javax.swing.JButton();
        jButton_pause = new javax.swing.JButton();
        jButton_fast = new javax.swing.JButton();
        jButton_superFast = new javax.swing.JButton();
        panelTowerInfo = new td.ui.PanelTowerInfo();
        panelWaveInfo = new td.ui.PanelWaveInfo();
        panelTowerSelector = new td.ui.PanelTowerSelector();


        getContentPane().setLayout(new java.awt.GridBagLayout());


        jPanel_board.setLayout(new java.awt.GridBagLayout());

        jPanel_board.setBackground(new java.awt.Color(0, 0, 0));
        jPanel_board.setForeground(new java.awt.Color(220, 255, 220));
        jPanel_board.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanel_boardMouseClicked(evt);
            }
        });
        jPanel_board.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jPanel_boardMouseMoved(evt);
            }
        });

        jPanel_gameLost.setLayout(new java.awt.GridBagLayout());

        jPanel_gameLost.setBackground(new java.awt.Color(0, 0, 0, 80));
        //jPanel_gameLost.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_gameLostText.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_gameLostText.setFont(new java.awt.Font("SansSerif", 1, 18));
        jLabel_gameLostText.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_gameLostText.setText("Game Over!");
        jPanel_gameLost.add(jLabel_gameLostText, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        jPanel_board.add(jPanel_gameLost, gridBagConstraints);

        jPanel_gameWon.setLayout(new java.awt.GridBagLayout());

        jPanel_gameWon.setBackground(new java.awt.Color(0, 0, 0, 80));
        //jPanel_gameWon.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_gameWonText.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_gameWonText.setFont(new java.awt.Font("SansSerif", 1, 18));
        jLabel_gameWonText.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_gameWonText.setText("Congratulations!");
        jPanel_gameWon.add(jLabel_gameWonText, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        jPanel_board.add(jPanel_gameWon, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        getContentPane().add(jPanel_board, gridBagConstraints);


        jPanel_console.setLayout(new java.awt.GridBagLayout());

        jPanel_console.setBackground(new java.awt.Color(0, 0, 0));
        jPanel_console.setFocusable(false);
        jPanel_console.setMaximumSize(new java.awt.Dimension(200, 2147483647));
        jPanel_console.setMinimumSize(new java.awt.Dimension(200, 263));
        jPanel_console.setPreferredSize(new java.awt.Dimension(200, 402));

        jLabel_name.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_name.setFont(new java.awt.Font("Dialog", 1, 16));
        jLabel_name.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_name.setText(NAME + " v" + VERSION);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 4, 10);
        jPanel_console.add(jLabel_name, gridBagConstraints);

        jPanel_gameInfo.setLayout(new java.awt.GridBagLayout());

        jPanel_gameInfo.setBackground(new java.awt.Color(0, 0, 0));
        jPanel_gameInfo.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Status", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 11), new java.awt.Color(220, 255, 220)));
        jPanel_gameInfo.setForeground(new java.awt.Color(220, 255, 220));
        jPanel_gameInfo.setFocusable(false);
        jLabel_waveText.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_waveText.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_waveText.setText("Wave:");
        jLabel_waveText.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel_gameInfo.add(jLabel_waveText, gridBagConstraints);

        jLabel_wave.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_wave.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_wave.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel_wave.setText("xx/xx");
        jLabel_wave.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel_gameInfo.add(jLabel_wave, gridBagConstraints);

        jLabel_livesText.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_livesText.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_livesText.setText("Lives:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        jPanel_gameInfo.add(jLabel_livesText, gridBagConstraints);

        jLabel_lives.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_lives.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_lives.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel_lives.setText("xx");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        jPanel_gameInfo.add(jLabel_lives, gridBagConstraints);

        jLabel_scoreText.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_scoreText.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_scoreText.setText("Score:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        jPanel_gameInfo.add(jLabel_scoreText, gridBagConstraints);

        jLabel_score.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_score.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_score.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel_score.setText("0000000");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        jPanel_gameInfo.add(jLabel_score, gridBagConstraints);

        jLabel_creditsText.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_creditsText.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_creditsText.setText("Cash:");
        jLabel_creditsText.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        jPanel_gameInfo.add(jLabel_creditsText, gridBagConstraints);

        jLabel_credits.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_credits.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_credits.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel_credits.setText("000000");
        jLabel_credits.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        jPanel_gameInfo.add(jLabel_credits, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        jPanel_console.add(jPanel_gameInfo, gridBagConstraints);

        jPanel_gameButtons.setLayout(new java.awt.GridBagLayout());

        jPanel_gameButtons.setBackground(new java.awt.Color(0, 0, 0));
        jPanel_gameButtons.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel_gameButtons.setFocusable(false);
        jButton_play.setBackground(new java.awt.Color(0, 0, 0));
        jButton_play.setForeground(new java.awt.Color(0, 0, 0));
        jButton_play.setText(">");
        jButton_play.setFocusable(false);
        jButton_play.addActionListener(this::jButton_playActionPerformed);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        jPanel_gameButtons.add(jButton_play, gridBagConstraints);

        jButton_pause.setBackground(new java.awt.Color(0, 0, 0));
        jButton_pause.setForeground(new java.awt.Color(0, 0, 0));
        jButton_pause.setText("||");
        jButton_pause.setFocusable(false);
        jButton_pause.setVisible(false);
        jButton_pause.addActionListener(this::jButton_pauseActionPerformed);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        jPanel_gameButtons.add(jButton_pause, gridBagConstraints);

        jButton_fast.setBackground(new java.awt.Color(0, 0, 0));
        jButton_fast.setForeground(new java.awt.Color(0, 0, 0));
        jButton_fast.setText(">>");
        jButton_fast.setFocusable(false);
        jButton_fast.addActionListener(this::jButton_fastActionPerformed);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(5, 2, 0, 0);
        jPanel_gameButtons.add(jButton_fast, gridBagConstraints);

        jButton_superFast.setBackground(new java.awt.Color(0, 0, 0));
        jButton_superFast.setForeground(new java.awt.Color(0, 0, 0));
        jButton_superFast.setText(">>>");
        jButton_superFast.setFocusable(false);
        jButton_superFast.addActionListener(this::jButton_superFastActionPerformed);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(5, 2, 0, 0);
        jPanel_gameButtons.add(jButton_superFast, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.01;
        jPanel_console.add(jPanel_gameButtons, gridBagConstraints);

        panelTowerInfo.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        jPanel_console.add(panelTowerInfo, gridBagConstraints);

        panelWaveInfo.setMinimumSize(null);
        panelWaveInfo.setName("Waving :)");
        panelWaveInfo.setPreferredSize(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        jPanel_console.add(panelWaveInfo, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jPanel_console, gridBagConstraints);


        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        getContentPane().add(panelTowerSelector, gridBagConstraints);

    }

    private void jButton_playActionPerformed(java.awt.event.ActionEvent evt) {
        this.toggleGameSpeed(TICKTIME);
        if (this.waveReady && !this.paused) this.startWave = true;
        this.playPause(true);
    }

    private void jButton_pauseActionPerformed(java.awt.event.ActionEvent evt) {
        this.playPause(false);
    }

    private void jButton_fastActionPerformed(java.awt.event.ActionEvent evt) {
        this.jButton_play.setVisible(true);
        this.jButton_pause.setVisible(false);
        this.toggleGameSpeed(FASTTICKTIME);            //TODO kdyz je pauza, tak po stisknuti hra jednou tikne
    }

    private void jButton_superFastActionPerformed(java.awt.event.ActionEvent evt) {
        this.jButton_play.setVisible(true);
        this.jButton_pause.setVisible(false);
        this.toggleGameSpeed(SUPERFASTTICKTIME);
    }

    private void jPanel_boardMouseClicked(java.awt.event.MouseEvent evt) {
        this.mouseClicked(evt.getX(), evt.getY());
    }

    private void jPanel_boardMouseMoved(java.awt.event.MouseEvent evt) {
        this.requestFocusInWindow();
        if (this.placingTower) {
            this.highlightCell(evt.getX(), evt.getY());
        }
    }

    private void formKeyTyped(KeyEvent evt) {
        this.keyTyped(evt.getKeyChar());
    }

}
