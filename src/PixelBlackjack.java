/*
 * LANTERN POND BLACKJACK - a pixel-art blackjack game using ONLY Java's built-in libraries (Swing / Java2D).
 *
 * Compile:  javac PixelBlackjack.java
 * Run:      java PixelBlackjack
 *
 * HOW THE PIXEL LOOK WORKS
 *   The art is drawn onto a tiny 480x270 canvas (CW x CH) and scaled up exactly 4x (K) to 1920x1080 with
 *   nearest-neighbor scaling, so every rect and sprite is chunky. The table scene itself is laid out in a
 *   320x240 "design space" (W x H) that sits in the middle of that canvas (offset OX, OY); all game
 *   coordinates in this file are in design space. Text is drawn AFTER the scale-up, at full 1920x1080
 *   resolution, so the fonts stay sharp.
 *
 * FONTS (optional but recommended)
 *   Put the two font files next to PixelBlackjack.java (or in a folder called "fonts"):
 *     - "Arcade Classic Regular"  e.g. ARCADECLASSIC.TTF   -> body text (BODY_FONT_KEY)
 *     - "PIXEL GAME"              e.g. PIXELGAME.TTF       -> headers   (HEAD_FONT_KEY)
 *   Any .ttf/.otf whose file name contains those words is picked up, and so are fonts you installed on
 *   your system. If a font is missing, the game falls back to a built-in pixel font, so it always runs.
 *   If the text looks too big/small with your font files, change BODY_EM / RANK_EM / the header sizes.
 *
 * CONTROLS
 *   Mouse: click the buttons / chips.
 *   Keys:  H = hit, S = stand, D = double, P = split, SPACE/ENTER = deal / next round,
 *          1 / 2 on the title screen = solo / with AI, ESC = back to title (ESC on the title = quit),
 *          F11 = toggle fullscreen.
 *
 * WHERE TO TWEAK  (search for these names)
 *   TWEAKABLES block        - starting money, min bet, deck count, chip values/colors, seat positions
 *   paintBackground()       - the whole static scenery (sky, palms, pond, table...)
 *   celebrate() / humanFx() / bustFx() - win / lose / blackjack effects
 *   botHit()                - AI strategy
 *   settle()                - payouts (blackjack pays 3:2)
 *   split()                 - splitting rules
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

public class PixelBlackjack extends JPanel implements ActionListener, MouseListener, MouseMotionListener, KeyListener {

    // =====================================================================================
    //  TWEAKABLES
    // =====================================================================================
    static final int W = 320, H = 240;          // "design space": the table scene is laid out in this area
    static final int CW = 480, CH = 270;        // full 16:9 pixel canvas that the design space sits in
    static final int K = 4;                     // canvas -> screen scale. 480x270 * 4 = 1920x1080
    static final int OX = (CW - W) / 2, OY = CH - H;   // where the design space sits in the canvas (80, 30)
    static final int NW = CW * K, NH = CH * K;
    static final boolean START_FULLSCREEN = true;      // F11 toggles fullscreen / window while playing

    // fonts: matched against .ttf/.otf file names (and installed font names), ignoring spaces and case
    static final String BODY_FONT_KEY = "arcadeclassic";   // "Arcade Classic Regular"
    static final String HEAD_FONT_KEY = "pixelgame";       // "PIXEL GAME"
    static final float BODY_EM = 6;             // body text size in canvas pixels (6 => 24 px on a 1080p screen)
    static final float RANK_EM = 4;             // tiny text: card ranks and chip labels
    static final float TITLE_EM = 16, TITLE2_EM = 24;      // title screen header sizes
    static final float BANNER_EM = 18, BANNER_SMALL_EM = 12;   // "YOU WIN!" etc. (long text uses the small one)
    static final float DEFEAT_EM = 24;
    static final int START_MONEY = 500;
    static final int MIN_BET = 10;
    static final int DECKS = 2;                 // 2 decks = 104 cards
    static final int[] BOT_BETS = {10, 25, 50, 100};

    // AI mode seats (left to right). Human sits at HUMAN_SEAT_AI.
    static final String[] AI_NAMES = {"BOT 1", "BOT 2", "YOU", "BOT 3", "BOT 4"};
    static final int[] SEAT_X_AI = {52, 106, 160, 214, 268};
    static final int[] SEAT_Y_AI = {140, 150, 158, 150, 140};
    static final int HUMAN_SEAT_AI = 2;

    static final int CARD_W = 14, CARD_H = 20, CARD_STEP = 10;   // CARD_STEP = horizontal spacing inside a hand
    static final int SHOE_X = 244, SHOE_Y = 102;                 // cards fly in from here

    static final Color GOLD = new Color(240, 190, 60);
    static final Color RED = new Color(230, 60, 60);
    static final Color GREEN = new Color(90, 220, 110);
    static final Color GRAY = new Color(170, 170, 190);
    static final Color WHITE = new Color(245, 240, 225);
    static final Color OUTLINE = new Color(0, 0, 0);

    // betting chips: value, label, ring color, face color, label color, edge-notch color
    static class Chip {
        int value; String label; Color ring, face, ink, spot;
        Chip(int v, String l, Color r, Color f, Color i, Color s) { value = v; label = l; ring = r; face = f; ink = i; spot = s; }
    }
    static final Chip[] CHIPS = {
        new Chip(10, "10", new Color(70, 120, 220), new Color(230, 235, 250), new Color(30, 50, 120), Color.WHITE),
        new Chip(50, "50", new Color(210, 50, 60), new Color(250, 225, 225), new Color(130, 20, 30), Color.WHITE),
        new Chip(100, "100", new Color(40, 150, 70), new Color(220, 245, 225), new Color(20, 90, 40), Color.WHITE),
        new Chip(500, "500", new Color(110, 60, 160), new Color(235, 220, 250), new Color(70, 30, 110), Color.WHITE),
        new Chip(1000, "1K", new Color(240, 190, 50), new Color(255, 240, 180), new Color(120, 80, 10), new Color(90, 55, 10))};

    // 5x5 suit sprites: spade, heart, diamond, club
    static final String[][] SUIT = {
        {"..#..", ".###.", "#####", "#####", "..#.."},
        {".#.#.", "#####", "#####", ".###.", "..#.."},
        {"..#..", ".###.", "#####", ".###.", "..#.."},
        {"..#..", ".###.", "#.#.#", "#####", "..#.."}};
    static final String[] FROG = {
        "..ww....ww..", ".wkw....wkw.", ".gggggggggg.", "gggggggggggg",
        "ggllllllllgg", ".gllllllllg.", ".gggggggggg.", "ggg......ggg"};
    static final String[] SKULL = {
        ".#####.", "#######", "##.#.##", "##.#.##", "#######", ".#####.", ".#.#.#."};

    // =====================================================================================
    //  PIXEL FONT  (5x7 glyphs, proportional spacing - each glyph is trimmed to its own width)
    // =====================================================================================
    static class Glyph {
        String[] rows; int min = 9, max = -1;
        Glyph(String[] r) {
            rows = r;
            for (String row : r) for (int c = 0; c < row.length(); c++) if (row.charAt(c) == '#') { min = Math.min(min, c); max = Math.max(max, c); }
            if (max < 0) { min = 0; max = 1; }
        }
        int adv() { return max - min + 2; }   // glyph width + 1px gap
    }
    static final Map<Character, Glyph> FONT = new HashMap<>();
    static void gl(char c, String... rows) { FONT.put(c, new Glyph(rows)); }
    static {
        gl('A', ".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        gl('B', "####.", "#...#", "#...#", "####.", "#...#", "#...#", "####.");
        gl('C', ".####", "#....", "#....", "#....", "#....", "#....", ".####");
        gl('D', "####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####.");
        gl('E', "#####", "#....", "#....", "####.", "#....", "#....", "#####");
        gl('F', "#####", "#....", "#....", "####.", "#....", "#....", "#....");
        gl('G', ".####", "#....", "#....", "#.###", "#...#", "#...#", ".###.");
        gl('H', "#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        gl('I', "###", ".#.", ".#.", ".#.", ".#.", ".#.", "###");
        gl('J', "..###", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##..");
        gl('K', "#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#");
        gl('L', "#....", "#....", "#....", "#....", "#....", "#....", "#####");
        gl('M', "#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#");
        gl('N', "#...#", "##..#", "##..#", "#.#.#", "#..##", "#..##", "#...#");
        gl('O', ".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        gl('P', "####.", "#...#", "#...#", "####.", "#....", "#....", "#....");
        gl('Q', ".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#");
        gl('R', "####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#");
        gl('S', ".####", "#....", "#....", ".###.", "....#", "....#", "####.");
        gl('T', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#..");
        gl('U', "#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        gl('V', "#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#..");
        gl('W', "#...#", "#...#", "#...#", "#.#.#", "#.#.#", "##.##", "#...#");
        gl('X', "#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#");
        gl('Y', "#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#..");
        gl('Z', "#####", "....#", "...#.", "..#..", ".#...", "#....", "#####");
        gl('0', ".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###.");
        gl('1', "..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###.");
        gl('2', ".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####");
        gl('3', ".###.", "#...#", "....#", "..##.", "....#", "#...#", ".###.");
        gl('4', "...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#.");
        gl('5', "#####", "#....", "####.", "....#", "....#", "#...#", ".###.");
        gl('6', ".###.", "#....", "#....", "####.", "#...#", "#...#", ".###.");
        gl('7', "#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#...");
        gl('8', ".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###.");
        gl('9', ".###.", "#...#", "#...#", ".####", "....#", "....#", ".###.");
        gl('$', "..#..", ".####", "#.#..", ".###.", "..#.#", "####.", "..#..");
        gl('+', ".....", "..#..", "..#..", "#####", "..#..", "..#..", ".....");
        gl('-', ".....", ".....", ".....", "#####", ".....", ".....", ".....");
        gl('.', ".....", ".....", ".....", ".....", ".....", ".##..", ".##..");
        gl(':', ".....", ".##..", ".##..", ".....", ".##..", ".##..", ".....");
        gl('!', "..#..", "..#..", "..#..", "..#..", "..#..", ".....", "..#..");
        gl('\'', "..#..", "..#..", ".#...", ".....", ".....", ".....", ".....");
        gl(',', ".....", ".....", ".....", ".....", ".##..", "..#..", ".#...");
        gl('/', "....#", "....#", "...#.", "..#..", ".#...", "#....", "#....");
    }

    // =====================================================================================
    //  FONT LOADING
    // =====================================================================================
    static Font bodyFont, headFont;                      // null => the built-in pixel font is used instead
    static final Map<String, Font> FONT_CACHE = new HashMap<>();
    static final java.awt.font.FontRenderContext FRC = new java.awt.font.FontRenderContext(null, false, false);

    static String norm(String s) { return s.toLowerCase().replaceAll("[^a-z0-9]", ""); }

    static Font findFont(String key) {
        List<File> dirs = new ArrayList<>();
        dirs.add(new File(".")); dirs.add(new File("fonts"));
        try {
            File here = new File(PixelBlackjack.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!here.isDirectory()) here = here.getParentFile();
            dirs.add(here); dirs.add(new File(here, "fonts"));
        } catch (Exception ignored) {}
        for (File d : dirs) {
            File[] fs = d.listFiles();
            if (fs == null) continue;
            for (File f : fs) {
                String n = f.getName().toLowerCase();
                if ((n.endsWith(".ttf") || n.endsWith(".otf")) && norm(n).contains(key)) {
                    try { return Font.createFont(Font.TRUETYPE_FONT, f); }
                    catch (Exception e) { System.out.println("Could not load font " + f + ": " + e); }
                }
            }
        }
        for (String fam : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
            if (norm(fam).contains(key)) return new Font(fam, Font.PLAIN, 12);
        return null;
    }

    static void loadFonts() {
        bodyFont = findFont(BODY_FONT_KEY); headFont = findFont(HEAD_FONT_KEY);
        System.out.println("Body font   (" + BODY_FONT_KEY + "): " + (bodyFont != null ? bodyFont.getFontName() : "not found - using built-in pixel font"));
        System.out.println("Header font (" + HEAD_FONT_KEY + "): " + (headFont != null ? headFont.getFontName() : "not found - using built-in pixel font"));
    }

    static Font sized(Font base, float px) {
        String k = base.getFontName() + "@" + px;
        Font f = FONT_CACHE.get(k);
        if (f == null) { f = base.deriveFont(px); FONT_CACHE.put(k, f); }
        return f;
    }
    static float rankEm() { return bodyFont != null ? RANK_EM : 6f; }

    // =====================================================================================
    //  DATA CLASSES
    // =====================================================================================
    static class Card {
        int rank, suit; long born;
        Card(int r, int s) { rank = r; suit = s; }
        int value() { return rank > 10 ? 10 : rank == 1 ? 11 : rank; }
        String label() {
            switch (rank) { case 1: return "A"; case 11: return "J"; case 12: return "Q"; case 13: return "K"; default: return "" + rank; }
        }
    }

    static class Seat {
        String name = "", baseName; boolean human, doubled, done, bust, split;
        int money, bet, x, y, baseX, lastNet; String result = "";
        Seat parent;                                  // set on the extra hand created by a split
        List<Card> cards = new ArrayList<>();
        int value() {
            int t = 0, aces = 0;
            for (Card c : cards) { t += c.value(); if (c.rank == 1) aces++; }
            while (t > 21 && aces > 0) { t -= 10; aces--; }
            return t;
        }
        boolean soft() {
            int t = 0, aces = 0;
            for (Card c : cards) { t += c.value(); if (c.rank == 1) aces++; }
            while (t > 21 && aces > 0) { t -= 10; aces--; }
            return aces > 0;
        }
        boolean isBlackjack() { return !split && cards.size() == 2 && value() == 21; }
        void reset() {
            cards.clear(); bet = 0; doubled = done = bust = split = false; result = ""; lastNet = 0;
            if (baseName != null) { name = baseName; x = baseX; }
        }
    }

    static class P { float x, y, vx, vy, g; int life, size; Color c; }
    static class FT { String s; float x, y; int life; Color c; }
    static class Banner { String text; Color c; int life, max; }
    static class Btn {
        String label; int x, y, w, h; boolean on; Runnable act; Chip chip;
        Btn(String l, int x, int y, int w, int h, boolean on, Runnable a) { label = l; this.x = x; this.y = y; this.w = w; this.h = h; this.on = on; act = a; }
        boolean hit(int px, int py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }

    // =====================================================================================
    //  GAME STATE
    // =====================================================================================
    enum State { MENU, BETTING, DEALING, TURNS, DEALER, SETTLE, GAME_OVER }
    State state = State.MENU;
    boolean aiMode;
    final List<Seat> seats = new ArrayList<>();
    final Seat dealer = new Seat();
    Seat human;                                    // the player's main hand (holds the money)
    final List<Card> shoe = new ArrayList<>();
    int currentBet = 50, lastBet = 50, turn, dealStep, delay, gameOverTimer, fade, rounds, bestMoney;
    boolean holeRevealed;
    long tick;
    int mx = -1, my = -1;

    // effects
    final List<P> parts = new ArrayList<>();
    final List<FT> texts = new ArrayList<>();
    Banner banner;
    int shake, frogHop;
    float flash; Color flashColor = RED;
    final float[][] flies = new float[18][3];

    final Random rnd = new Random();
    final BufferedImage bg = new BufferedImage(CW, CH, BufferedImage.TYPE_INT_RGB);      // static scenery
    final BufferedImage frame = new BufferedImage(CW, CH, BufferedImage.TYPE_INT_RGB);   // pixel-art layer (480x270)
    final BufferedImage hires = new BufferedImage(NW, NH, BufferedImage.TYPE_INT_RGB);   // final 1920x1080 picture
    static JFrame window; static PixelBlackjack panel; static boolean fullscreen;

    // =====================================================================================
    //  SETUP
    // =====================================================================================
    public PixelBlackjack() {
        setPreferredSize(new Dimension(1280, 720)); setBackground(Color.BLACK);
        setFocusable(true);
        addMouseListener(this); addMouseMotionListener(this); addKeyListener(this);
        for (float[] f : flies) { f[0] = -OX + rnd.nextInt(CW); f[1] = 20 + rnd.nextInt(110); f[2] = rnd.nextFloat() * 6.28f; }
        dealer.x = dealer.baseX = 160; dealer.y = 110; dealer.name = dealer.baseName = "DEALER";
        paintBackground();
        new javax.swing.Timer(16, this).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            loadFonts();
            panel = new PixelBlackjack();
            showWindow(START_FULLSCREEN);
        });
    }

    /** Opens the game fullscreen (1920x1080 on a 1080p monitor) or as a normal 1280x720 window. */
    static void showWindow(boolean full) {
        GraphicsDevice dev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (window != null) { dev.setFullScreenWindow(null); window.dispose(); }
        window = new JFrame("Lantern Pond Blackjack");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.getContentPane().setBackground(Color.BLACK);
        window.setUndecorated(full);
        window.add(panel);
        if (full) {
            if (dev.isFullScreenSupported()) dev.setFullScreenWindow(window);
            else { window.setExtendedState(JFrame.MAXIMIZED_BOTH); window.setVisible(true); }
        } else {
            window.pack(); window.setLocationRelativeTo(null); window.setVisible(true);
        }
        fullscreen = full;
        panel.requestFocusInWindow();
    }

    void startGame(boolean ai) {
        aiMode = ai; seats.clear(); parts.clear(); texts.clear(); banner = null; shake = 0; flash = 0;
        if (ai) {
            for (int i = 0; i < AI_NAMES.length; i++) {
                Seat s = new Seat(); s.name = s.baseName = AI_NAMES[i]; s.x = s.baseX = SEAT_X_AI[i]; s.y = SEAT_Y_AI[i];
                s.money = START_MONEY; s.human = (i == HUMAN_SEAT_AI);
                seats.add(s); if (s.human) human = s;
            }
        } else {
            Seat s = new Seat(); s.name = s.baseName = "YOU"; s.x = s.baseX = 160; s.y = 158; s.money = START_MONEY; s.human = true;
            seats.add(s); human = s;
        }
        rounds = 0; bestMoney = START_MONEY; lastBet = 50;
        buildShoe(); enterBetting();
    }

    void enterBetting() {
        seats.removeIf(s -> s.parent != null);       // drop the extra split hand
        for (Seat s : seats) s.reset();
        dealer.reset(); holeRevealed = false; gameOverTimer = 0; delay = 0;
        currentBet = Math.min(lastBet, human.money);
        state = State.BETTING;
    }

    void buildShoe() {
        shoe.clear();
        for (int d = 0; d < DECKS; d++) for (int s = 0; s < 4; s++) for (int r = 1; r <= 13; r++) shoe.add(new Card(r, s));
        Collections.shuffle(shoe, rnd);
    }

    Card draw() { if (shoe.isEmpty()) buildShoe(); return shoe.remove(shoe.size() - 1); }
    void deal(Seat s) { Card c = draw(); c.born = tick; s.cards.add(c); }

    // =====================================================================================
    //  GAME FLOW
    // =====================================================================================
    void startRound() {
        if (state != State.BETTING || currentBet < MIN_BET || currentBet > human.money) return;
        if (shoe.size() < (aiMode ? 45 : 25)) { buildShoe(); floatText("SHUFFLING", SHOE_X + 7, SHOE_Y - 8, WHITE); }
        human.bet = currentBet; human.money -= currentBet; lastBet = currentBet;
        for (Seat s : seats) {
            if (s.human) continue;
            if (s.money < MIN_BET) { s.money = START_MONEY; floatText("REBUY", s.x, s.y - 16, GRAY); }
            s.bet = Math.min(BOT_BETS[rnd.nextInt(BOT_BETS.length)], s.money);
            s.money -= s.bet;
        }
        rounds++; dealStep = 0; state = State.DEALING; delay = 10;
    }

    void nextRound() {
        if (state != State.SETTLE) return;
        gameOverTimer = 0;
        if (human.money < MIN_BET) { toGameOver(); return; }
        enterBetting();
    }

    void toGameOver() { state = State.GAME_OVER; fade = 0; shake = 24; flash = 0.8f; flashColor = RED; }

    void update() {
        tick++;
        updateEffects();
        if (state == State.GAME_OVER) { fade = Math.min(fade + 1, 120); return; }
        if (delay > 0) { delay--; return; }
        switch (state) {
            case DEALING: stepDealing(); break;
            case TURNS: stepTurns(); break;
            case DEALER: stepDealer(); break;
            case SETTLE: if (gameOverTimer > 0 && --gameOverTimer == 0) toGameOver(); break;
            default: break;
        }
    }

    void stepDealing() {
        int n = seats.size() + 1;
        if (dealStep >= n * 2) { afterDeal(); return; }
        int idx = dealStep % n;
        deal(idx < seats.size() ? seats.get(idx) : dealer);
        dealStep++; delay = 9;
    }

    void afterDeal() {
        for (Seat s : seats) if (s.isBlackjack()) s.done = true;
        if (dealer.isBlackjack()) {                       // dealer peeks
            for (Seat s : seats) s.done = true;
            holeRevealed = true; state = State.DEALER; delay = 30;
            banner("DEALER BLACKJACK", RED, 70);
            return;
        }
        turn = 0; state = State.TURNS; delay = 20;
    }

    Seat curHand() { return turn < seats.size() ? seats.get(turn) : null; }
    boolean humanTurn() { Seat s = curHand(); return state == State.TURNS && s != null && s.human && !s.done; }
    boolean canAct() { return humanTurn() && delay == 0; }

    void stepTurns() {
        while (turn < seats.size() && seats.get(turn).done) turn++;
        if (turn >= seats.size()) { state = State.DEALER; delay = 25; return; }
        Seat s = seats.get(turn);
        if (s.human) return;                               // wait for the player
        if (botHit(s)) { deal(s); afterHit(s); delay = 28; } else { s.done = true; delay = 16; }
    }

    boolean botHit(Seat s) {
        int v = s.value(), up = dealer.cards.get(0).value();
        if (s.soft()) return v <= 17;
        if (v >= 17) return false;
        if (v >= 12 && up >= 2 && up <= 6) return false;
        return true;
    }

    void afterHit(Seat s) {
        int v = s.value();
        if (v > 21) {
            s.bust = true; s.done = true;
            if (s.human) bustFx(s); else floatText("BUST", s.x, s.y - 16, RED);
        } else if (v == 21) s.done = true;
    }

    void stepDealer() {
        if (!holeRevealed) { holeRevealed = true; delay = 30; return; }
        boolean live = false;
        for (Seat s : seats) if (!s.bust) live = true;
        if (live && dealer.value() < 17) { deal(dealer); delay = 30; return; }   // dealer stands on all 17s
        settle();
    }

    void settle() {
        int dv = dealer.value(); boolean dbj = dealer.isBlackjack(), dbust = dv > 21;
        int humanNet = 0, humanHands = 0; String humanResult = "";
        for (Seat s : seats) {
            int pv = s.value(), payout = 0; String r;
            if (s.bust) r = "BUST";
            else if (s.isBlackjack()) { if (dbj) { payout = s.bet; r = "PUSH"; } else { payout = (int) (s.bet * 2.5); r = "BLACKJACK"; } }
            else if (dbj) r = "LOSE";
            else if (dbust || pv > dv) { payout = s.bet * 2; r = "WIN"; }
            else if (pv == dv) { payout = s.bet; r = "PUSH"; }
            else r = "LOSE";
            Seat bank = s.parent != null ? s.parent : s;     // split hand pays into the main hand's purse
            bank.money += payout; s.result = r; s.lastNet = payout - s.bet;
            celebrate(s, r, s.lastNet);
            if (s.human) { humanNet += s.lastNet; humanHands++; humanResult = r; }
        }
        if (humanHands > 1) humanResult = humanNet > 0 ? "WIN" : humanNet < 0 ? "LOSE" : "PUSH";
        humanFx(humanResult);
        bestMoney = Math.max(bestMoney, human.money);
        state = State.SETTLE; delay = 0;
        if (human.money < MIN_BET) gameOverTimer = 130;
    }

    // ---- player actions ----
    void hit() { if (!canAct()) return; Seat s = curHand(); deal(s); afterHit(s); delay = 12; }
    void stand() { if (!canAct()) return; curHand().done = true; delay = 15; }
    void dbl() {
        Seat s = curHand();
        if (!canAct() || s.cards.size() != 2 || human.money < s.bet) return;
        human.money -= s.bet; s.bet *= 2; s.doubled = true;
        deal(s); afterHit(s); s.done = true; delay = 20;
    }
    boolean canSplit() {
        Seat s = curHand();
        return canAct() && s.cards.size() == 2 && !s.split && s.cards.get(0).value() == s.cards.get(1).value() && human.money >= s.bet;
    }
    void split() {
        if (!canSplit()) return;
        Seat s = curHand();
        human.money -= s.bet;
        Seat n = new Seat(); n.human = true; n.parent = s; n.y = s.y; n.bet = s.bet;
        n.split = s.split = true;
        boolean aces = s.cards.get(0).rank == 1;
        n.cards.add(s.cards.remove(1));
        s.name = "HAND 1"; n.name = "HAND 2";
        s.x = s.baseX - 22; n.x = s.baseX + 22;
        seats.add(turn + 1, n);                            // plays right after the first hand
        deal(s); deal(n);
        for (Seat h : new Seat[]{s, n}) if (aces || h.value() == 21) h.done = true;   // split aces get one card only
        delay = 22;
    }
    void addBet(int n) { currentBet = Math.min(human.money, currentBet + n); }

    // =====================================================================================
    //  EFFECTS
    // =====================================================================================
    void spawn(float x, float y, int n, Color[] cols, float speed, float grav, int size) {
        for (int i = 0; i < n; i++) {
            P p = new P(); p.x = x; p.y = y;
            double a = rnd.nextDouble() * Math.PI * 2, sp = rnd.nextDouble() * speed;
            p.vx = (float) (Math.cos(a) * sp); p.vy = (float) (Math.sin(a) * sp - speed * 0.8);
            p.g = grav; p.life = 40 + rnd.nextInt(40); p.c = cols[rnd.nextInt(cols.length)]; p.size = size;
            parts.add(p);
        }
    }
    void floatText(String s, float x, float y, Color c) { FT t = new FT(); t.s = s; t.x = x; t.y = y; t.c = c; t.life = 70; texts.add(t); }
    void banner(String s, Color c, int life) { banner = new Banner(); banner.text = s; banner.c = c; banner.life = banner.max = life; }

    void bustFx(Seat s) {
        shake = 16; flash = 0.5f; flashColor = RED;
        banner("BUST!", RED, 70);
        spawn(s.x, s.y + 10, 30, new Color[]{RED, new Color(120, 20, 30), new Color(255, 120, 60)}, 2.5f, 0.1f, 2);
    }

    // per-hand feedback (floating amount; bots also get a few coins)
    void celebrate(Seat s, String r, int net) {
        String amt = net == 0 ? "PUSH" : (net > 0 ? "+$" : "-$") + Math.abs(net);
        floatText(amt, s.x, s.y - 16, net > 0 ? GOLD : net < 0 ? RED : GRAY);
        if (!s.human && net > 0) spawn(s.x, s.y + 10, 10, new Color[]{GOLD, new Color(255, 225, 110), new Color(200, 140, 30)}, 2f, 0.12f, 2);
    }

    // the big screen effect for the player's overall result this round
    void humanFx(String r) {
        int x = human.baseX, y = human.y + 10;
        Color[] coins = {GOLD, new Color(255, 225, 110), new Color(200, 140, 30)};
        switch (r) {
            case "WIN":
                spawn(x, y, 45, coins, 3.5f, 0.12f, 2);
                banner("YOU WIN!", GOLD, 80); frogHop = 45; flash = 0.35f; flashColor = new Color(255, 230, 120); break;
            case "BLACKJACK":
                spawn(x, y, 70, coins, 4f, 0.12f, 2);
                spawn(x, y, 60, new Color[]{RED, GREEN, new Color(80, 160, 255), GOLD, new Color(240, 100, 220)}, 4.5f, 0.1f, 2);
                banner("BLACKJACK!", new Color(110, 220, 255), 100); frogHop = 80; flash = 0.5f; flashColor = new Color(255, 240, 150); break;
            case "PUSH": banner("PUSH", GRAY, 50); break;
            case "LOSE":
                shake = 18; flash = 0.5f; flashColor = RED; banner("YOU LOSE", RED, 70);
                spawn(x, y, 30, new Color[]{RED, new Color(120, 20, 30), new Color(70, 70, 90)}, 2.5f, 0.1f, 2); break;
            default: break;   // BUST: already handled in bustFx
        }
    }

    void updateEffects() {
        if (shake > 0) shake--;
        if (flash > 0) flash = Math.max(0, flash - 0.03f);
        if (frogHop > 0) frogHop--; else if (rnd.nextInt(500) == 0) frogHop = 18;
        for (Iterator<P> it = parts.iterator(); it.hasNext(); ) {
            P p = it.next(); p.x += p.vx; p.y += p.vy; p.vy += p.g;
            if (--p.life <= 0 || p.y > H + 5) it.remove();
        }
        for (Iterator<FT> it = texts.iterator(); it.hasNext(); ) {
            FT t = it.next(); t.y -= 0.35f; if (--t.life <= 0) it.remove();
        }
        if (banner != null && --banner.life <= 0) banner = null;
        for (float[] f : flies) {
            f[2] += 0.03f; f[0] += (float) Math.cos(f[2]) * 0.25f; f[1] += (float) Math.sin(f[2] * 1.3f) * 0.18f;
            if (f[0] < -OX) f[0] = W + OX; if (f[0] > W + OX) f[0] = -OX;
        }
    }

    // =====================================================================================
    //  RENDERING
    // =====================================================================================
    @Override protected void paintComponent(Graphics gr) {
        super.paintComponent(gr);
        renderAll();
        Graphics2D g2 = (Graphics2D) gr;
        double s = Math.min(getWidth() / (double) NW, getHeight() / (double) NH);   // letterbox if not 16:9
        int w = (int) Math.round(NW * s), h = (int) Math.round(NH * s);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, w == NW ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(hires, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
    }

    /** Draws one full frame into `hires` (1920x1080). */
    void renderAll() {
        tq.clear(); tq2.clear(); cur = tq;
        // 1) pixel-art layer at 480x270
        Graphics2D g = frame.createGraphics();
        g.setColor(Color.BLACK); g.fillRect(0, 0, CW, CH);
        shx = shy = 0;
        if (shake > 0) { int s = Math.max(1, shake / 5); shx = rnd.nextInt(2 * s + 1) - s; shy = rnd.nextInt(2 * s + 1) - s; }
        g.translate(shx, shy);
        g.drawImage(bg, 0, 0, null);
        g.translate(OX, OY);                                   // from here on: design space (320x240)
        drawWaterFx(g); drawFrogs(g); drawLantern(g);
        if (state == State.MENU) drawMenu(g); else drawTable(g);
        drawParticles(g); drawTexts(g); drawBanner(g); drawFireflies(g);
        g.dispose();
        // 2) scale up 4x, then draw all text at full resolution on top
        Graphics2D h = hires.createGraphics();
        h.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        h.drawImage(frame, 0, 0, NW, NH, null);
        flushText(h, tq, OX + shx, OY + shy);
        // 3) full-screen overlays
        if (flash > 0) { h.setColor(new Color(flashColor.getRed(), flashColor.getGreen(), flashColor.getBlue(), (int) (flash * 140))); h.fillRect(0, 0, NW, NH); }
        if (state == State.GAME_OVER) {
            cur = tq2; h.scale(K, K); drawGameOver(h); h.setTransform(new AffineTransform());
            flushText(h, tq2, OX, OY);
        }
        h.dispose();
    }

    // ---------- helpers ----------
    static Color lerp(Color a, Color b, float t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t), (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
    void sprite(Graphics2D g, String[] rows, String keys, Color[] cols, int x, int y, int sc) {
        for (int j = 0; j < rows.length; j++) for (int i = 0; i < rows[j].length(); i++) {
            int k = keys.indexOf(rows[j].charAt(i));
            if (k >= 0) { g.setColor(cols[k]); g.fillRect(x + i * sc, y + j * sc, sc, sc); }
        }
    }

    // ---------- text: queued while drawing, then rendered at full resolution ----------
    static class TextCmd { String s; float x, y, em; Color c, out; boolean head; }
    final List<TextCmd> tq = new ArrayList<>(), tq2 = new ArrayList<>();   // scene text / overlay text
    List<TextCmd> cur = tq;
    int shx, shy;

    int glyphWidth(String s) {                                  // width of the built-in font, in its own pixels
        int w = 0;
        for (char ch : s.toUpperCase().toCharArray()) { Glyph gl = FONT.get(ch); w += (gl == null ? 3 : gl.adv()); }
        return Math.max(0, w - 1);
    }
    /** Width of text in canvas pixels. em = text size in canvas pixels. */
    float tw(String s, float em, boolean head) {
        Font base = head ? headFont : bodyFont;
        if (base != null) return (float) (sized(base, em * K).getStringBounds(s, FRC).getWidth() / K);
        return glyphWidth(s) * em / 6f;
    }
    /** x = left edge, y = BASELINE (design-space canvas pixels). out = outline color or null. */
    void textO(Graphics2D g, String s, float x, float y, Color c, Color out, float em, boolean head) {
        TextCmd t = new TextCmd(); t.s = s; t.x = x; t.y = y; t.c = c; t.out = out; t.em = em; t.head = head; cur.add(t);
    }
    void text(Graphics2D g, String s, int x, int y, Color c) { textO(g, s, x, y, c, OUTLINE, BODY_EM, false); }
    void ctext(Graphics2D g, String s, int cx, int y, Color c) { textO(g, s, cx - tw(s, BODY_EM, false) / 2, y, c, OUTLINE, BODY_EM, false); }
    void htext(Graphics2D g, String s, int cx, int y, Color c, float em) { textO(g, s, cx - tw(s, em, true) / 2, y, c, OUTLINE, em, true); }

    void flushText(Graphics2D g, List<TextCmd> list, float dx, float dy) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        for (TextCmd t : list) {
            Font base = t.head ? headFont : bodyFont;
            float px = (t.x + dx) * K, py = (t.y + dy) * K;
            if (base != null) {
                g.setFont(sized(base, t.em * K));
                int ix = Math.round(px), iy = Math.round(py), o = Math.max(1, Math.round(t.em * K / 8f));   // outline = one font pixel
                if (t.out != null) {
                    g.setColor(t.out);
                    g.drawString(t.s, ix - o, iy); g.drawString(t.s, ix + o, iy); g.drawString(t.s, ix, iy - o); g.drawString(t.s, ix, iy + o); g.drawString(t.s, ix + o, iy + o);
                }
                g.setColor(t.c); g.drawString(t.s, ix, iy);
            } else {                                              // built-in pixel font fallback
                AffineTransform old = g.getTransform();
                g.translate(px, py); float f = t.em * K / 6f; g.scale(f, f);
                int o = 1;
                if (t.out != null) { g.setColor(t.out); glyphs(g, t.s, -o, -7, 1); glyphs(g, t.s, o, -7, 1); glyphs(g, t.s, 0, -7 - o, 1); glyphs(g, t.s, 0, -7 + o, 1); glyphs(g, t.s, o, -7 + o, 1); }
                g.setColor(t.c); glyphs(g, t.s, 0, -7, 1);
                g.setTransform(old);
            }
        }
        list.clear();
    }
    void glyphs(Graphics2D g, String s, int x, int top, int sc) {
        for (char ch : s.toUpperCase().toCharArray()) {
            Glyph gl = FONT.get(ch);
            if (gl == null) { x += 3 * sc; continue; }
            for (int r = 0; r < 7; r++) {
                String row = gl.rows[r];
                for (int c = gl.min; c <= gl.max && c < row.length(); c++) if (row.charAt(c) == '#') g.fillRect(x + (c - gl.min) * sc, top + r * sc, sc, sc);
            }
            x += gl.adv() * sc;
        }
    }

    // ---------- static scenery (drawn once) ----------
    void paintBackground() {
        Graphics2D g = bg.createGraphics();
        g.translate(OX, OY);                                  // paint in design space; the canvas is wider/taller
        final int X0 = -OX, X1 = W + OX;                      // left / right edge of the canvas in design space
        Random r = new Random(7);
        for (int y = -OY; y < 62; y += 2) { g.setColor(lerp(new Color(8, 8, 30), new Color(30, 28, 76), (y + OY) / (62f + OY))); g.fillRect(X0, y, CW, 2); }
        g.setColor(new Color(200, 200, 255)); for (int i = 0; i < 60; i++) g.fillRect(X0 + r.nextInt(CW), -OY + r.nextInt(34 + OY), 1, 1);
        // far tree line
        g.setColor(new Color(6, 18, 22)); for (int i = X0 - 6; i < X1; i += 14) { int h = 12 + r.nextInt(16); g.fillOval(i, 52 - h, 26, h * 2); }
        palm(g, -50, 62, -66, 14); palm(g, 54, 62, 36, 12); palm(g, 252, 62, 266, 10); palm(g, 372, 62, 388, 16);
        // bushes + bank
        for (int i = X0 - 10; i < X1; i += 18) { g.setColor(new Color(10, 42, 30)); g.fillOval(i, 46 + r.nextInt(6), 26, 22); g.setColor(new Color(16, 62, 40)); g.fillOval(i + 6, 52 + r.nextInt(4), 16, 12); }
        g.setColor(new Color(14, 52, 34)); g.fillRect(X0, 58, CW, 6);
        g.setColor(new Color(30, 100, 55)); for (int i = 0; i < 90; i++) g.fillRect(X0 + r.nextInt(CW), 55 + r.nextInt(6), 1, 2);
        // pond
        for (int y = 64; y < 108; y++) { g.setColor(lerp(new Color(8, 22, 62), new Color(18, 48, 104), (y - 64) / 44f)); g.fillRect(X0, y, CW, 1); }
        g.setColor(new Color(60, 110, 190)); for (int i = 0; i < 105; i++) g.fillRect(X0 + r.nextInt(CW), 66 + r.nextInt(38), 5 + r.nextInt(12), 1);
        g.setColor(new Color(40, 130, 60));
        int[][] pads = {{30, 84, 14}, {56, 72, 10}, {200, 92, 14}, {240, 76, 12}, {286, 90, 14}, {110, 70, 9}, {-40, 80, 14}, {-68, 70, 10}, {350, 84, 14}, {378, 72, 12}};
        for (int[] p : pads) { g.fillOval(p[0], p[1], p[2], p[2] / 3 + 1); }
        g.setColor(new Color(220, 50, 60)); g.fillRect(58, 70, 3, 3); g.fillRect(296, 82, 3, 3); g.fillRect(-38, 78, 3, 3);
        g.setColor(WHITE); g.fillRect(59, 71, 1, 1); g.fillRect(297, 83, 1, 1); g.fillRect(-37, 79, 1, 1);
        // reeds
        g.setColor(new Color(30, 110, 60));
        for (int x0 : new int[]{288, 350}) for (int i = 0; i < 14; i++) { int x = x0 + i * 2 + r.nextInt(2), top = 14 + r.nextInt(30); g.fillRect(x, top, 1, 64 - top); }
        for (int x0 : new int[]{2, -70}) for (int i = 0; i < 6; i++) { int x = x0 + i * 2, top = 70 + r.nextInt(10); g.fillRect(x, top, 1, 100 - top); }
        // railing + deck
        g.setColor(new Color(52, 26, 22)); g.fillRect(X0, 108, CW, H - 108);
        g.setColor(new Color(38, 18, 16)); for (int y = 118; y < H; y += 12) g.fillRect(X0, y, CW, 1);
        g.setColor(new Color(90, 46, 22)); for (int x = X0; x < X1; x += 12) g.fillRect(x, 100, 4, 10);
        g.setColor(new Color(122, 66, 32)); g.fillRect(X0, 96, CW, 8);
        g.setColor(new Color(166, 104, 52)); g.fillRect(X0, 96, CW, 1);
        g.setColor(new Color(70, 35, 18)); g.fillRect(X0, 103, CW, 1);
        // dealer chair
        g.setColor(new Color(210, 150, 40)); g.fillRoundRect(140, 54, 40, 50, 16, 16);
        g.setColor(new Color(150, 28, 42)); g.fillRoundRect(144, 58, 32, 44, 12, 12);
        g.setClip(144, 58, 32, 44); g.setColor(new Color(185, 55, 68)); for (int i = 0; i < 6; i++) { g.drawLine(146 + i * 5, 60, 146 + i * 5 - 14, 90); g.drawLine(146 + i * 5 - 14, 60, 146 + i * 5, 90); }
        g.setClip(null);
        // side stools
        g.setColor(new Color(200, 140, 40)); g.fillRoundRect(-12, 168, 40, 44, 14, 14); g.fillRoundRect(292, 168, 40, 44, 14, 14);
        g.setColor(new Color(130, 24, 40)); g.fillRoundRect(-8, 172, 32, 36, 12, 12); g.fillRoundRect(296, 172, 32, 36, 12, 12);
        // table
        int cx = 160, cy = 150, rx = 152, ry = 62;
        g.setColor(new Color(60, 30, 16)); g.fillOval(cx - rx - 2, cy - ry - 2, rx * 2 + 4, ry * 2 + 4);
        g.setColor(new Color(132, 76, 38)); g.fillOval(cx - rx, cy - ry, rx * 2, ry * 2);
        g.setColor(new Color(170, 110, 56)); g.drawArc(cx - rx + 2, cy - ry + 2, rx * 2 - 4, ry * 2 - 4, 20, 140);
        g.setColor(new Color(12, 66, 34)); g.fillOval(cx - 140, cy - 52, 280, 104);
        g.setColor(new Color(22, 104, 52)); g.fillOval(cx - 138, cy - 50, 276, 100);
        g.setColor(new Color(18, 92, 46));
        for (int i = 0; i < 700; i++) { int x = cx - 138 + r.nextInt(276), y = cy - 50 + r.nextInt(100); double ex = (x - cx) / 136.0, ey = (y - cy) / 48.0; if (ex * ex + ey * ey < 1) g.fillRect(x, y, 1, 1); }
        for (int a = 0; a < 360; a += 12) {
            int x = (int) (cx + (rx - 7) * Math.cos(Math.toRadians(a))), y = (int) (cy + (ry - 6) * Math.sin(Math.toRadians(a)));
            g.setColor(new Color(230, 180, 50)); g.fillRect(x - 1, y - 1, 3, 3); g.setColor(new Color(255, 235, 150)); g.fillRect(x - 1, y - 1, 1, 1);
        }
        for (int a = 20; a < 360; a += 60) {
            int x = (int) (cx + (rx - 7) * Math.cos(Math.toRadians(a))), y = (int) (cy + (ry - 6) * Math.sin(Math.toRadians(a)));
            g.setColor(new Color(110, 70, 20)); g.fillOval(x - 5, y - 4, 10, 8); g.setColor(new Color(235, 185, 55)); g.fillOval(x - 4, y - 3, 8, 6);
        }
        // chip tray, chip pile, shoe
        g.setColor(new Color(60, 36, 14)); g.fillRect(190, 112, 44, 14); g.setColor(GOLD); g.drawRect(190, 112, 43, 13);
        Color[] chips = {new Color(230, 120, 30), new Color(40, 170, 70), new Color(50, 90, 220), new Color(230, 120, 30), new Color(210, 210, 220), new Color(210, 40, 50)};
        for (int i = 0; i < 6; i++) { g.setColor(chips[i]); g.fillRect(193 + i * 6, 114, 5, 10); g.setColor(WHITE); g.fillRect(193 + i * 6, 114, 5, 1); }
        Color[] pile = {new Color(50, 90, 220), WHITE, new Color(230, 120, 30)};
        for (int c = 0; c < 3; c++) for (int k = 0; k < 3; k++) { g.setColor(pile[c]); g.fillRect(84 + c * 6, 116 - k * 2, 5, 2); }
        g.setColor(new Color(30, 40, 110)); g.fillRect(SHOE_X - 3, SHOE_Y - 3, CARD_W + 6, CARD_H + 6);
        g.setColor(GOLD); g.drawRect(SHOE_X - 3, SHOE_Y - 3, CARD_W + 5, CARD_H + 5);
        g.dispose();
    }

    void palm(Graphics2D g, int bx, int by, int tx, int ty) {
        g.setColor(new Color(88, 54, 30));
        for (int i = 0; i <= 30; i++) { float t = i / 30f; g.fillRect((int) (bx + (tx - bx) * t + Math.sin(t * 3) * 2), (int) (by + (ty - by) * t), 4, 3); }
        for (int k = 0; k < 7; k++) {
            double a = Math.toRadians(20 + k * 23);
            for (int i = 0; i < 18; i++) {
                g.setColor(i % 5 < 3 ? new Color(24, 102, 50) : new Color(14, 70, 38));
                g.fillRect((int) (tx + Math.cos(a) * i * 1.4), (int) (ty - Math.sin(a) * i * 0.7 + i * i * 0.05), 3, 2);
            }
        }
    }

    // ---------- animated scenery ----------
    void drawWaterFx(Graphics2D g) {
        for (int i = 0; i < 14; i++) {
            int x = (int) ((i * 37 + tick / 6) % CW) - OX, y = 68 + (i * 11) % 34;
            g.setColor(new Color(130, 180, 255, 40 + (int) (40 * Math.sin(tick * 0.05 + i))));
            g.fillRect(x, y, 8, 1);
        }
    }

    void drawFrogs(Graphics2D g) {
        int off = frogHop > 0 ? (int) (Math.abs(Math.sin(frogHop * 0.22)) * (frogHop > 20 ? 10 : 4)) : 0;
        Color[] pal = {WHITE, Color.BLACK, new Color(70, 170, 60), new Color(150, 215, 100)};
        sprite(g, FROG, "wkgl", pal, 34, 48 - off, 2);
        sprite(g, FROG, "wkgl", pal, 262, 48 - off, 2);
    }

    void drawLantern(Graphics2D g) {
        float fl = 0.85f + 0.15f * (float) Math.sin(tick * 0.25) + rnd.nextFloat() * 0.05f;
        for (int i = 0; i < 9; i++) { int r = 40 - i * 4; g.setColor(new Color(255, 190, 70, (int) ((6 + i) * fl))); g.fillOval(160 - r, 25 - r, r * 2, r * 2); }
        for (int i = 0; i < 4; i++) {
            int half = 70 - i * 14;
            g.setColor(new Color(255, 215, 110, (int) (14 * fl)));
            g.fillPolygon(new int[]{155, 165, 160 + half, 160 - half}, new int[]{36, 36, 205, 205}, 4);
        }
        g.setColor(new Color(60, 40, 30)); g.fillRect(160, -OY, 1, 10 + OY);
        g.setColor(new Color(150, 90, 40)); g.fillRect(157, 7, 7, 3);
        g.setColor(new Color(120, 50, 20)); g.fillRect(152, 12, 17, 4); g.fillRect(154, 10, 13, 2);
        g.setColor(new Color(235, 150, 40)); g.fillRect(154, 16, 13, 18);
        g.setColor(new Color(255, 225, 120)); g.fillRect(157, 18, 7, 14);
        g.setColor(new Color(255, 250, 200)); g.fillRect(159, 22, 3, 6);
        g.setColor(new Color(140, 70, 20)); g.fillRect(154, 16, 1, 18); g.fillRect(166, 16, 1, 18); g.fillRect(154, 25, 13, 1);
        g.setColor(new Color(120, 50, 20)); g.fillRect(152, 34, 17, 3);
    }

    void drawFireflies(Graphics2D g) {
        for (float[] f : flies) {
            int a = (int) (110 + 110 * Math.sin(tick * 0.08 + f[2] * 3));
            g.setColor(new Color(220, 255, 120, Math.max(0, Math.min(255, a))));
            g.fillRect((int) f[0], (int) f[1], 2, 2);
        }
    }

    void drawParticles(Graphics2D g) {
        for (P p : parts) {
            int a = Math.min(255, p.life * 12);
            g.setColor(new Color(p.c.getRed(), p.c.getGreen(), p.c.getBlue(), a));
            g.fillRect((int) p.x, (int) p.y, p.size, p.size);
        }
    }

    void drawTexts(Graphics2D g) {
        for (FT t : texts) {
            int a = Math.min(255, t.life * 8);
            textO(g, t.s, t.x - tw(t.s, BODY_EM, false) / 2, t.y, new Color(t.c.getRed(), t.c.getGreen(), t.c.getBlue(), a), new Color(0, 0, 0, a), BODY_EM, false);
        }
    }

    void drawBanner(Graphics2D g) {
        if (banner == null) return;
        int age = banner.max - banner.life;
        float base = banner.text.length() > 11 ? BANNER_SMALL_EM : BANNER_EM;
        float s = 1f + Math.max(0, 8 - age) * 0.12f, a = banner.life < 15 ? banner.life / 15f : 1f, em = base * s;
        Color c = new Color(banner.c.getRed(), banner.c.getGreen(), banner.c.getBlue(), (int) (255 * a));
        textO(g, banner.text, 160 - tw(banner.text, em, true) / 2, 76 + em * 0.4f, c, new Color(0, 0, 0, (int) (230 * a)), em, true);
    }

    // ---------- cards & seats ----------
    void drawCard(Graphics2D g, Card c, int tx, int ty, boolean up) {
        float t = Math.min(1f, (tick - c.born) / 10f), e = 1 - (1 - t) * (1 - t);
        int x = Math.round(SHOE_X + (tx - SHOE_X) * e), y = Math.round(SHOE_Y + (ty - SHOE_Y) * e);
        g.setColor(new Color(0, 0, 0, 80)); g.fillRect(x + 1, y + 1, CARD_W, CARD_H);
        if (up) {
            g.setColor(new Color(250, 245, 230)); g.fillRect(x, y, CARD_W, CARD_H);
            g.setColor(new Color(50, 40, 40)); g.drawRect(x, y, CARD_W - 1, CARD_H - 1);
            Color col = (c.suit == 1 || c.suit == 2) ? new Color(200, 35, 50) : new Color(30, 30, 45);
            String lab = c.label();
            float re = rankEm(); float rw = tw(lab, re, false);
            textO(g, lab, x + (rw > 8 ? 1 : 2), y + (bodyFont != null ? 2 + re * 0.9f : 9), col, null, re, false);
            sprite(g, SUIT[c.suit], "#", new Color[]{col}, x + 4, y + 12, 1);
        } else {
            g.setColor(new Color(40, 50, 140)); g.fillRect(x, y, CARD_W, CARD_H);
            g.setColor(new Color(220, 220, 240)); g.drawRect(x, y, CARD_W - 1, CARD_H - 1);
            g.setColor(new Color(100, 110, 210));
            for (int i = 0; i < 5; i++) for (int j = 0; j < 8; j++) if ((i + j) % 2 == 0) g.fillRect(x + 2 + i * 2, y + 2 + j * 2, 2, 2);
        }
    }

    void drawSeat(Graphics2D g, Seat s, boolean isDealer) {
        int n = s.cards.size();
        boolean active = !isDealer && state == State.TURNS && seats.indexOf(s) == turn && !s.done;
        if (!isDealer && n == 0) { g.setColor(new Color(214, 170, 50, 150)); g.drawOval(s.x - 19, s.y + 2, 38, 22); }
        if (!isDealer) {
            Color nc = active ? (tick / 8 % 2 == 0 ? WHITE : GOLD) : (s.human ? GOLD : GRAY);
            ctext(g, s.name, s.x, s.y - 5, nc);
            if (active) { g.setColor(GOLD); g.fillRect(s.x - 1, s.y - 16, 3, 1); g.fillRect(s.x, s.y - 15, 1, 1); }
        }
        int w = CARD_W + (Math.max(n, 1) - 1) * CARD_STEP, sx = s.x - w / 2;
        if (state == State.SETTLE && n > 0 && !isDealer) {
            Color gl = null; float p = 0.5f + 0.5f * (float) Math.sin(tick * 0.2);
            if (s.result.equals("WIN") || s.result.equals("BLACKJACK")) gl = new Color(255, 220, 80, (int) (50 + 60 * p));
            else if (s.result.equals("LOSE") || s.result.equals("BUST")) gl = new Color(220, 40, 50, 60);
            if (gl != null) { g.setColor(gl); g.fillRoundRect(sx - 3, s.y - 3, w + 6, CARD_H + 6, 4, 4); }
        }
        for (int i = 0; i < n; i++) drawCard(g, s.cards.get(i), sx + i * CARD_STEP, s.y, !(isDealer && i == 1 && !holeRevealed));
        if (n > 0) {
            String v; Color c = WHITE;
            if (isDealer) {
                int val = holeRevealed ? s.value() : s.cards.get(0).value();
                v = "DEALER " + val;
                if (holeRevealed && val > 21) { v = "DEALER BUST"; c = RED; }
                else if (holeRevealed && s.isBlackjack()) { v = "DEALER BJ"; c = GOLD; }
            } else {
                v = "" + s.value();
                if (s.bust) c = RED; else if (s.isBlackjack()) { v = "21 BJ!"; c = GOLD; }
            }
            ctext(g, v, s.x, s.y + CARD_H + 9, c);
        }
        if (!isDealer) {
            if (state == State.SETTLE && !s.result.isEmpty()) {
                String t = s.result + (s.lastNet == 0 ? "" : (s.lastNet > 0 ? " +$" : " -$") + Math.abs(s.lastNet));
                ctext(g, t, s.x, s.y + CARD_H + 18, s.lastNet > 0 ? GOLD : s.lastNet < 0 ? RED : GRAY);
            } else if (state == State.BETTING && s.human) ctext(g, "BET $" + currentBet, s.x, s.y + CARD_H + 18, new Color(255, 240, 160));
            else if (s.bet > 0) ctext(g, "BET $" + s.bet, s.x, s.y + CARD_H + 18, new Color(255, 240, 160));
        }
    }

    void drawTable(Graphics2D g) {
        for (Seat s : seats) drawSeat(g, s, false);
        drawSeat(g, dealer, true);
        ctext(g, "" + shoe.size(), SHOE_X + 7, SHOE_Y + 16, WHITE);          // cards left in the shoe
        // bottom bar
        g.setColor(new Color(28, 14, 8, 238)); g.fillRect(-OX, 208, CW, 32);
        g.setColor(new Color(200, 150, 50)); g.fillRect(-OX, 208, CW, 1); g.setColor(new Color(110, 70, 25)); g.fillRect(-OX, 209, CW, 1);
        int totalBet = 0; for (Seat s : seats) if (s.human) totalBet += s.bet;
        text(g, "COINS $" + human.money, 6, 222, GOLD);
        text(g, "BET $" + (state == State.BETTING ? currentBet : totalBet), 6, 233, WHITE);
        if (state != State.GAME_OVER) for (Btn b : buttons()) drawButton(g, b);
        String msg = null;
        if (state == State.DEALING) msg = "DEALING...";
        else if (state == State.DEALER) msg = "DEALER'S TURN";
        else if (state == State.TURNS && !humanTurn()) msg = curHand() == null ? "" : curHand().name + " IS PLAYING...";
        if (msg != null) ctext(g, msg, 190, 228, GRAY);
    }

    void drawButton(Graphics2D g, Btn b) {
        if (b.chip != null) { drawChip(g, b); return; }
        boolean hover = b.on && b.hit(mx, my);
        Color base = !b.on ? new Color(45, 35, 30) : hover ? new Color(120, 72, 32) : new Color(84, 48, 22);
        g.setColor(base); g.fillRect(b.x, b.y, b.w, b.h);
        g.setColor(b.on ? new Color(220, 170, 70) : new Color(80, 66, 50)); g.fillRect(b.x, b.y, b.w, 1); g.fillRect(b.x, b.y, 1, b.h);
        g.setColor(new Color(30, 15, 6)); g.fillRect(b.x, b.y + b.h - 1, b.w, 1); g.fillRect(b.x + b.w - 1, b.y, 1, b.h);
        ctext(g, b.label, b.x + b.w / 2, b.y + b.h / 2 + 4, b.on ? (hover ? WHITE : GOLD) : new Color(120, 110, 100));
    }

    /** A round poker chip: coloured rim with edge notches, light face, value in the middle. */
    void drawChip(Graphics2D g, Btn b) {
        boolean hover = b.on && b.hit(mx, my);
        Chip c = b.chip;
        int cx = b.x + 12, cy = b.y + 12 - (hover ? 1 : 0), r = 12;
        Color ring = c.ring, face = c.face, ink = c.ink, spot = c.spot;
        if (!b.on) {
            Color gr = new Color(70, 62, 60);
            ring = lerp(ring, gr, 0.65f); face = lerp(face, gr, 0.65f); ink = lerp(ink, gr, 0.4f); spot = lerp(spot, gr, 0.65f);
        }
        g.setColor(new Color(15, 8, 4)); g.fillOval(cx - r - 1, cy - r - 1, 2 * r + 2, 2 * r + 2);
        g.setColor(ring); g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
        g.setColor(spot);
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4; int px = (int) Math.round(cx + Math.cos(a) * 11), py = (int) Math.round(cy + Math.sin(a) * 11);
            int sz = k % 2 == 0 ? 3 : 2; g.fillRect(px - sz / 2, py - sz / 2, sz, sz);
        }
        g.setColor(lerp(ring, Color.BLACK, 0.35f)); g.fillOval(cx - 10, cy - 10, 20, 20);
        g.setColor(face); g.fillOval(cx - 9, cy - 9, 18, 18);
        float re = rankEm();
        textO(g, c.label, cx - tw(c.label, re, false) / 2, cy + (bodyFont != null ? re * 0.45f : 4), ink, null, re, false);
        if (hover) { g.setColor(WHITE); g.drawOval(cx - r - 1, cy - r - 1, 2 * r + 2, 2 * r + 2); }
    }

    void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 20, 120)); g.fillRect(-OX, -OY, CW, CH);
        int bob = (int) (Math.sin(tick * 0.06) * 2);
        htext(g, "LANTERN POND", 160, 50 + bob, GOLD, TITLE_EM);
        htext(g, "BLACKJACK", 160, 88 + bob, WHITE, TITLE2_EM);
        sprite(g, SUIT[0], "#", new Color[]{WHITE}, 10, 62 + bob, 4);
        sprite(g, SUIT[1], "#", new Color[]{RED}, 290, 62 + bob, 4);
        ctext(g, "A PIXEL CARD GAME", 160, 104, GRAY);
        for (Btn b : buttons()) drawButton(g, b);
        ctext(g, "H HIT  S STAND  D DOUBLE  P SPLIT  SPACE NEXT", 160, 204, GRAY);
        ctext(g, DECKS + " DECKS - BLACKJACK PAYS 3 TO 2 - START WITH $" + START_MONEY, 160, 216, new Color(140, 140, 170));
        ctext(g, "F11 FULLSCREEN   ESC MENU / QUIT", 160, 228, new Color(140, 140, 170));
    }

    /** Drawn at full resolution under a K-times scale (canvas coordinates). */
    void drawGameOver(Graphics2D g) {
        g.setColor(new Color(40, 0, 0, Math.min(215, fade * 4))); g.fillRect(0, 0, CW, CH);
        if (fade < 20) return;
        g.translate(OX, OY);
        sprite(g, SKULL, "#", new Color[]{new Color(230, 225, 210)}, 146, 30, 4);
        htext(g, "DEFEAT", 160, 104, lerp(new Color(150, 20, 30), RED, 0.5f + 0.5f * (float) Math.sin(tick * 0.1)), DEFEAT_EM);
        ctext(g, "YOU LOST ALL YOUR COINS", 160, 120, GRAY);
        ctext(g, "ROUNDS PLAYED: " + rounds + "   BEST: $" + bestMoney, 160, 134, WHITE);
        if (fade > 40) for (Btn b : buttons()) drawButton(g, b);
    }

    // =====================================================================================
    //  BUTTONS + INPUT
    // =====================================================================================
    List<Btn> buttons() {
        List<Btn> b = new ArrayList<>();
        switch (state) {
            case MENU:
                b.add(new Btn("SOLO - YOU VS DEALER", 50, 112, 220, 22, true, () -> startGame(false)));
                b.add(new Btn("WITH AI - 4 BOTS", 50, 140, 220, 22, true, () -> startGame(true)));
                b.add(new Btn("QUIT", 50, 168, 220, 22, true, () -> System.exit(0)));
                break;
            case BETTING: {
                boolean room = currentBet < human.money;
                for (int i = 0; i < CHIPS.length; i++) {
                    Chip c = CHIPS[i];
                    Btn chip = new Btn(c.label, 82 + i * 27, 212, 24, 24, room, () -> addBet(c.value));
                    chip.chip = c; b.add(chip);
                }
                b.add(new Btn("CLEAR", 218, 215, 36, 18, currentBet > 0, () -> currentBet = 0));
                b.add(new Btn("DEAL", 258, 215, 48, 18, currentBet >= MIN_BET, this::startRound));
                break;
            }
            case TURNS:
                if (humanTurn()) {
                    boolean on = delay == 0; Seat s = curHand();
                    b.add(new Btn("HIT", 82, 215, 40, 18, on, this::hit));
                    b.add(new Btn("STAND", 126, 215, 46, 18, on, this::stand));
                    b.add(new Btn("DOUBLE", 176, 215, 52, 18, on && s.cards.size() == 2 && human.money >= s.bet, this::dbl));
                    b.add(new Btn("SPLIT", 232, 215, 46, 18, canSplit(), this::split));
                }
                break;
            case SETTLE: b.add(new Btn("NEXT ROUND", 200, 215, 90, 18, true, this::nextRound)); break;
            case GAME_OVER:
                b.add(new Btn("TRY AGAIN", 100, 150, 120, 22, true, () -> startGame(aiMode)));
                b.add(new Btn("MENU", 100, 178, 120, 22, true, () -> state = State.MENU));
                break;
            default: break;
        }
        return b;
    }

    // window pixels -> design-space coordinates (accounts for letterboxing when the window isn't 16:9)
    int vx(MouseEvent e) {
        double s = Math.min(getWidth() / (double) NW, getHeight() / (double) NH), ox = (getWidth() - NW * s) / 2;
        return (int) Math.floor((e.getX() - ox) / s / K - OX);
    }
    int vy(MouseEvent e) {
        double s = Math.min(getWidth() / (double) NW, getHeight() / (double) NH), oy = (getHeight() - NH * s) / 2;
        return (int) Math.floor((e.getY() - oy) / s / K - OY);
    }

    @Override public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        int x = vx(e), y = vy(e);
        for (Btn b : buttons()) if (b.on && b.hit(x, y)) { b.act.run(); break; }
    }
    @Override public void mouseMoved(MouseEvent e) { mx = vx(e); my = vy(e); }
    @Override public void mouseDragged(MouseEvent e) { mx = vx(e); my = vy(e); }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) { mx = my = -1; }

    @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_H: hit(); break;
            case KeyEvent.VK_S: stand(); break;
            case KeyEvent.VK_D: dbl(); break;
            case KeyEvent.VK_P: split(); break;
            case KeyEvent.VK_1: if (state == State.MENU) startGame(false); break;
            case KeyEvent.VK_2: if (state == State.MENU) startGame(true); break;
            case KeyEvent.VK_SPACE: case KeyEvent.VK_ENTER:
                if (state == State.BETTING) startRound();
                else if (state == State.SETTLE) nextRound();
                else if (state == State.GAME_OVER && fade > 40) startGame(aiMode);
                break;
            case KeyEvent.VK_ESCAPE: if (state != State.MENU) state = State.MENU; else System.exit(0); break;
            case KeyEvent.VK_F11: SwingUtilities.invokeLater(() -> showWindow(!fullscreen)); break;
            default: break;
        }
    }
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    @Override public void actionPerformed(ActionEvent e) { update(); repaint(); }
}
