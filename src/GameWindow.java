import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;

// The main window of the game. It shows the table and the buttons.
// It implements ActionListener so it can react when a button is clicked.
public class GameWindow extends JFrame implements ActionListener {
   private static final Color TABLE_GREEN = new Color(20, 110, 65);
   private static final Color SIDE_GREEN = new Color(12, 70, 42);
   private static final Color BAR_COLOR = new Color(25, 25, 28);
   private static final Color GOLD = new Color(235, 195, 90);
   private static final Color WIN_GREEN = new Color(110, 230, 140);
   private static final Color LOSE_RED = new Color(255, 110, 110);
   private static final Color SOFT_WHITE = new Color(200, 230, 210);

   private BlackjackGame game;
   private int pendingBet;   // the bet the player is building before pressing DEAL

   // Labels that show information
   private JLabel balanceLabel;
   private JLabel betLabel;
   private JLabel statsLabel;
   private JLabel dealerLabel;
   private JLabel playerLabel;
   private JLabel splitLabel;
   private JLabel messageLabel;
   private JLabel deckLabel;
   private JLabel modeLabel;
   private JTextArea historyArea;

   // Areas where the cards are shown
   private JPanel dealerCardsPanel;
   private JPanel playerCardsPanel;
   private JPanel splitCardsPanel;
   private JPanel aiPlayersPanel;
   private JPanel dealerSeat;
   private JPanel middlePanel;
   private ArrayList<JLabel> seatLabels;
   private ArrayList<JPanel> seatCardsPanels;

   // Buttons
   private JButton chip10Button;
   private JButton chip25Button;
   private JButton chip50Button;
   private JButton chip100Button;
   private JButton clearButton;
   private JButton dealButton;
   private JButton hitButton;
   private JButton standButton;
   private JButton doubleButton;
   private JButton splitButton;
   private JButton newGameButton;
   private JButton howToPlayButton;

   private Timer dealerTimer;   // makes the dealer draw one card at a time
   private Timer aiTimer;

   public GameWindow(String playerName) {
      this(playerName, "Vs AI Table");
   }

   public GameWindow(String playerName, String mode) {
      this(playerName, mode, 2);
   }

   public GameWindow(String playerName, String mode, int humanSeat) {
      boolean vsAi = "Vs AI Table".equalsIgnoreCase(mode);
      game = new BlackjackGame(playerName, vsAi, humanSeat);
      pendingBet = 0;

      setTitle("Blackjack");
      setSize(1150, 760);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setLocationRelativeTo(null);
      setLayout(new BorderLayout());

      add(createTopBar(), BorderLayout.NORTH);
      add(createTable(), BorderLayout.CENTER);
      add(createSidePanel(), BorderLayout.EAST);
      add(createButtonBar(), BorderLayout.SOUTH);

      // Every 800 milliseconds the timer calls actionPerformed() during the dealer's turn.
      dealerTimer = new Timer(800, this);
      aiTimer = new Timer(600, event -> {
         game.playAiStep();
         updateScreen();
      });

      modeLabel.setText(mode.toUpperCase());

      updateScreen();
   }

   // ---------- Building the window ----------

   // Top bar: the title on the left, balance, bet and stats on the right.
   private JPanel createTopBar() {
      JPanel bar = new JPanel(new BorderLayout());
      bar.setBackground(BAR_COLOR);
      bar.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));

      JLabel title = createLabel("BLACKJACK", 28, GOLD);
      title.setFont(new Font("Georgia", Font.BOLD, 30));

      JPanel info = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 8));
      info.setBackground(BAR_COLOR);
      balanceLabel = createLabel("", 18, GOLD);
      betLabel = createLabel("", 18, Color.WHITE);
      statsLabel = createLabel("", 16, Color.LIGHT_GRAY);
      modeLabel = createLabel("", 12, SOFT_WHITE);
      info.add(modeLabel);
      info.add(balanceLabel);
      info.add(betLabel);
      info.add(statsLabel);

      bar.add(title, BorderLayout.WEST);
      bar.add(info, BorderLayout.EAST);
      return bar;
   }

   // The table is laid out like a casino table: dealer at the top, four seats
   // around the arc, and the human player at the bottom.
   private JPanel createTable() {
      CasinoTablePanel table = new CasinoTablePanel();

      dealerLabel = createLabel("DEALER", 18, Color.WHITE);
      dealerCardsPanel = createCardsArea();
      dealerSeat = createHandArea(dealerLabel, dealerCardsPanel);
      table.add(dealerSeat);
      splitLabel = createLabel("", 10, SOFT_WHITE);
      splitCardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, -3, 0));
      splitCardsPanel.setOpaque(false);
      aiPlayersPanel = createSeatRow();
      table.add(aiPlayersPanel);

      middlePanel = new JPanel(new GridLayout(2, 1));
      middlePanel.setOpaque(false);
      messageLabel = createLabel("", 30, GOLD);
      messageLabel.setFont(new Font("Georgia", Font.BOLD, 32));
      messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
      messageLabel.setVerticalAlignment(SwingConstants.BOTTOM);
      JLabel rulesLabel = createLabel("BLACKJACK PAYS 3 TO 2   |   DEALER STANDS ON 17", 14, SOFT_WHITE);
      rulesLabel.setHorizontalAlignment(SwingConstants.CENTER);
      rulesLabel.setVerticalAlignment(SwingConstants.TOP);
      rulesLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
      middlePanel.add(messageLabel);
      middlePanel.add(rulesLabel);
      table.add(middlePanel);

      playerLabel = seatLabels.get(game.getHumanSeat());
      playerCardsPanel = seatCardsPanels.get(game.getHumanSeat());
      return table;
   }

   private JPanel createSeatRow() {
      JPanel panel = new JPanel(new GridLayout(1, game.isVsAiTable() ? 5 : 1, 5, 0));
      panel.setOpaque(false);
      panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
      seatLabels = new ArrayList<>();
      seatCardsPanels = new ArrayList<>();
      for (Participant participant : game.getSeats()) {
         JPanel seat = new JPanel(new BorderLayout(0, 2));
         seat.setOpaque(false);
         JLabel label = createLabel("", 12, SOFT_WHITE);
         label.setHorizontalAlignment(SwingConstants.CENTER);
         JPanel cards = new JPanel(new FlowLayout(FlowLayout.CENTER, -3, 1));
         cards.setOpaque(false);
         seatLabels.add(label);
         seatCardsPanels.add(cards);
         seat.add(label, BorderLayout.NORTH);
         seat.add(cards, BorderLayout.CENTER);
         if (participant == game.getPlayer()) {
            JPanel splitArea = new JPanel(new BorderLayout());
            splitArea.setOpaque(false);
            splitLabel.setHorizontalAlignment(SwingConstants.CENTER);
            splitArea.add(splitLabel, BorderLayout.NORTH);
            splitArea.add(splitCardsPanel, BorderLayout.CENTER);
            seat.add(splitArea, BorderLayout.SOUTH);
         }
         panel.add(seat);
      }
      return panel;
   }

   // One hand on the table: the name and score on top, the cards below.
   private JPanel createHandArea(JLabel label, JPanel cardsPanel) {
      JPanel area = new JPanel(new BorderLayout());
      area.setOpaque(false);
      label.setHorizontalAlignment(SwingConstants.CENTER);
      area.add(label, BorderLayout.NORTH);
      area.add(cardsPanel, BorderLayout.CENTER);
      return area;
   }

   private JPanel createCardsArea() {
      JPanel cardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
      cardsPanel.setOpaque(false);
      return cardsPanel;
   }

   // Right side: the list of recent rounds and the cards left in the deck.
   private JPanel createSidePanel() {
      JPanel side = new JPanel(new BorderLayout(0, 12));
      side.setBackground(SIDE_GREEN);
      side.setPreferredSize(new Dimension(260, 0));
      side.setBorder(BorderFactory.createEmptyBorder(20, 18, 20, 18));

      JLabel title = createLabel("RECENT ROUNDS", 16, GOLD);

      historyArea = new JTextArea();
      historyArea.setEditable(false);
      historyArea.setLineWrap(true);
      historyArea.setWrapStyleWord(true);
      historyArea.setBackground(SIDE_GREEN);
      historyArea.setForeground(Color.WHITE);
      historyArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));

      deckLabel = createLabel("", 14, SOFT_WHITE);

      side.add(title, BorderLayout.NORTH);
      side.add(historyArea, BorderLayout.CENTER);
      side.add(deckLabel, BorderLayout.SOUTH);
      return side;
   }

   // Bottom bar: chip buttons on the left, game buttons on the right.
   private JPanel createButtonBar() {
      JPanel bar = new JPanel(new BorderLayout());
      bar.setBackground(BAR_COLOR);
      bar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

      JPanel chipArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
      chipArea.setBackground(BAR_COLOR);
      chip10Button = createButton("10", new Color(200, 45, 55));
      chip25Button = createButton("25", new Color(35, 140, 75));
      chip50Button = createButton("50", new Color(40, 95, 190));
      chip100Button = createButton("100", new Color(60, 60, 66));
      clearButton = createButton("CLEAR", new Color(95, 95, 100));
      chipArea.add(chip10Button);
      chipArea.add(chip25Button);
      chipArea.add(chip50Button);
      chipArea.add(chip100Button);
      chipArea.add(clearButton);

      JPanel actionArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
      actionArea.setBackground(BAR_COLOR);
      dealButton = createButton("DEAL", new Color(200, 150, 40));
      hitButton = createButton("HIT", new Color(35, 150, 90));
      standButton = createButton("STAND", new Color(190, 50, 55));
      doubleButton = createButton("DOUBLE", new Color(45, 105, 190));
      splitButton = createButton("SPLIT", new Color(125, 85, 180));
      newGameButton = createButton("NEW GAME", new Color(200, 150, 40));
      howToPlayButton = createButton("HOW TO PLAY", new Color(95, 95, 100));
      actionArea.add(dealButton);
      actionArea.add(hitButton);
      actionArea.add(standButton);
      actionArea.add(doubleButton);
      actionArea.add(splitButton);
      actionArea.add(newGameButton);
      actionArea.add(howToPlayButton);

      bar.add(chipArea, BorderLayout.WEST);
      bar.add(actionArea, BorderLayout.EAST);
      return bar;
   }

   // Makes a flat colored button. Every button uses this window as its ActionListener.
   private JButton createButton(String text, Color color) {
      JButton button = new JButton(text);
      button.setFont(new Font("Segoe UI", Font.BOLD, 15));
      button.setBackground(color);
      button.setForeground(Color.WHITE);
      button.setFocusPainted(false);
      button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
      button.setCursor(new Cursor(Cursor.HAND_CURSOR));
      button.addActionListener(this);
      return button;
   }

   private JLabel createLabel(String text, int size, Color color) {
      JLabel label = new JLabel(text);
      label.setFont(new Font("Segoe UI", Font.BOLD, size));
      label.setForeground(color);
      return label;
   }

   // ---------- Reacting to clicks ----------

   // Called by Java every time a button is clicked or the dealer timer ticks.
   @Override
   public void actionPerformed(ActionEvent e) {
      Object source = e.getSource();

      if (source == chip10Button) {
         addChip(10);
      } else if (source == chip25Button) {
         addChip(25);
      } else if (source == chip50Button) {
         addChip(50);
      } else if (source == chip100Button) {
         addChip(100);
      } else if (source == clearButton) {
         pendingBet = 0;
      } else if (source == dealButton) {
         deal();
      } else if (source == hitButton) {
         game.hit();
      } else if (source == standButton) {
         game.stand();
      } else if (source == doubleButton) {
         doubleDown();
      } else if (source == splitButton) {
         split();
      } else if (source == newGameButton) {
         game.resetGame();
         pendingBet = 0;
      } else if (source == howToPlayButton) {
         showHowToPlay();
      } else if (source == dealerTimer) {
         dealerStep();
      }

      updateScreen();
   }

   // Adds a chip to the bet, but only if the player has enough chips.
   private void addChip(int value) {
      try {
         game.getPlayer().validateBet(pendingBet + value);
         pendingBet += value;
      } catch (InvalidBetException ex) {
         showWarning(ex.getMessage());
      }
   }

   private void deal() {
      if (game.needsReshuffle()) {
         showReshuffleScreen();
         return;
      }
      try {
         game.startRound(pendingBet);
      } catch (InvalidBetException ex) {
         showWarning(ex.getMessage());
      }
   }

   private void showReshuffleScreen() {
      final javax.swing.JDialog loading = new javax.swing.JDialog(this, "", true);
      loading.setUndecorated(true);
      loading.setSize(330, 145);
      loading.setLocationRelativeTo(this);
      JPanel panel = new JPanel(new BorderLayout());
      panel.setBackground(SIDE_GREEN);
      panel.setBorder(BorderFactory.createLineBorder(GOLD, 2));
      JLabel title = createLabel("RE-SHUFFLING THE DECK", 19, GOLD);
      title.setHorizontalAlignment(SwingConstants.CENTER);
      JLabel detail = createLabel("Please wait...", 14, SOFT_WHITE);
      detail.setHorizontalAlignment(SwingConstants.CENTER);
      panel.add(title, BorderLayout.CENTER);
      panel.add(detail, BorderLayout.SOUTH);
      loading.add(panel);

      Timer reshuffleTimer = new Timer(950, event -> {
         game.reshuffle();
         loading.dispose();
         try {
            game.startRound(pendingBet);
         } catch (InvalidBetException ex) {
            showWarning(ex.getMessage());
         }
      });
      reshuffleTimer.setRepeats(false);
      reshuffleTimer.start();
      loading.setVisible(true);
   }

   private void doubleDown() {
      try {
         game.doubleDown();
      } catch (InvalidBetException ex) {
         showWarning(ex.getMessage());
      }
   }

   private void split() {
      try {
         game.split();
      } catch (InvalidBetException ex) {
         showWarning(ex.getMessage());
      }
   }

   // One step of the dealer's turn: take a card, or finish the round.
   private void dealerStep() {
      if (game.dealerShouldHit()) {
         game.dealerHit();
      } else {
         dealerTimer.stop();
         game.finishRound();
      }
   }

   private void showWarning(String message) {
      JOptionPane.showMessageDialog(this, message, "Invalid Bet", JOptionPane.WARNING_MESSAGE);
   }

   // Shows a step-by-step guide. Public so Main can show it when the game starts.
   public void showHowToPlay() {
      String guide = "GOAL\n"
            + "Get a hand closer to 21 than the dealer, without going over 21.\n"
            + "You start with " + BlackjackGame.STARTING_CHIPS + " chips.\n\n"
            + "CARD VALUES\n"
            + "- Number cards (2 to 10) are worth their number.\n"
            + "- J, Q and K are worth 10.\n"
            + "- An Ace is worth 11, or 1 if 11 would make you go over 21.\n\n"
            + "STEP 1 - PLACE YOUR BET\n"
            + "Click the chip buttons (10, 25, 50, 100) to add to your bet.\n"
            + "Click CLEAR to start your bet over.\n\n"
            + "STEP 2 - DEAL\n"
            + "Click DEAL. You and the dealer get 2 cards each.\n"
            + "One of the dealer's cards is face down (shown as ?).\n\n"
            + "STEP 3 - YOUR TURN\n"
            + "- HIT: take another card. You can hit as many times as you want.\n"
            + "- STAND: keep your cards and end your turn.\n"
            + "- DOUBLE: double your bet, take exactly 1 more card, then stand.\n"
            + "  (Only allowed with your first 2 cards.)\n"
            + "- SPLIT: if your first two cards have the same rank, split them into\n"
            + "  two hands. Each hand gets its own equal bet and is played separately.\n"
            + "If your total goes over 21, you BUST and lose your bet.\n\n"
            + "STEP 4 - DEALER'S TURN\n"
            + "The dealer shows the hidden card, then must keep taking cards\n"
            + "until the total is 17 or more.\n\n"
            + "STEP 5 - RESULTS\n"
            + "- Win: you get your bet back plus the same amount.\n"
            + "- Blackjack (21 with your first 2 cards): you win 1.5 times your bet.\n"
            + "- Push (same total as the dealer): you get your bet back.\n"
            + "- Lose: the dealer keeps your bet.\n\n"
            + "Your bet stays the same for the next round. Just click DEAL again.\n"
            + "If you run out of chips, click NEW GAME to start over.";
      JOptionPane.showMessageDialog(this, guide, "How to Play", JOptionPane.INFORMATION_MESSAGE);
   }

   // ---------- Updating the screen ----------

   // Refreshes every label, card and button so the screen matches the game.
   private void updateScreen() {
      Player player = game.getPlayer();

      // Keep the same bet for the next round, but never more than the chips left.
      if (!game.isRoundActive() && pendingBet > player.getChips()) {
         pendingBet = player.getChips();
      }

      int shownBet = pendingBet;
      if (game.isRoundActive()) {
         shownBet = player.getBet();
      }

      balanceLabel.setText("Balance: " + player.getChips());
      betLabel.setText("Bet: " + shownBet);
      statsLabel.setText("Wins " + game.getWins() + "   Losses " + game.getLosses() + "   Pushes " + game.getPushes());
      deckLabel.setText("Cards left in deck: " + game.getCardsLeft());

      showHand(dealerLabel, dealerCardsPanel, game.getDealer(), false);
      showSeatHands();
      showSplitHand();
      showMessage();
      showHistory();
      updateButtons();

      if (game.isDealerTurn() && !dealerTimer.isRunning()) {
         dealerTimer.start();
      }
      if (game.isAiTurn() && !aiTimer.isRunning()) aiTimer.start();
      if (!game.isAiTurn() && aiTimer.isRunning()) aiTimer.stop();
   }

   private void showSeatHands() {
      ArrayList<Participant> seats = game.getSeats();
      for (int i = 0; i < seats.size(); i++) {
         JLabel label = seatLabels.get(i);
         label.setForeground(game.isRoundActive() && game.getActiveSeat() == seats.get(i) ? GOLD : SOFT_WHITE);
         showHand(label, seatCardsPanels.get(i), seats.get(i), true);
      }
      aiPlayersPanel.revalidate();
      aiPlayersPanel.repaint();
   }

   // Works for both the Player and the Dealer, because both are Participants.
   // getScoreText() runs the Dealer's version or the Player's version (polymorphism).
   private void showHand(JLabel label, JPanel cardsPanel, Participant participant, boolean compact) {
      ArrayList<Card> cards = participant.getHand().getCards();

      // Reuse existing card panels. This prevents old cards from flipping again
      // every time the screen refreshes after HIT, STAND, or a dealer timer tick.
      while (cardsPanel.getComponentCount() > cards.size()) {
         cardsPanel.remove(cardsPanel.getComponentCount() - 1);
      }
      for (int i = 0; i < cards.size(); i++) {
         if (i < cardsPanel.getComponentCount()) {
            CardPanel cardPanel = (CardPanel) cardsPanel.getComponent(i);
            cardPanel.updateCard(cards.get(i), 0);
         } else {
            // Only a newly added card gets the deal animation.
            cardsPanel.add(new CardPanel(cards.get(i), i * 180, compact));
         }
      }
      cardsPanel.revalidate();
      cardsPanel.repaint();

      String name = participant.getName().toUpperCase();
      if (participant.getHand().size() == 0) {
         label.setText(name);
      } else {
         label.setText(name + "   " + participant.getScoreText());
      }
   }

   // The big message in the middle. Green for a win, red for a loss.
   private void showMessage() {
      messageLabel.setText(game.getMessage());
      messageLabel.setForeground(GOLD);

      RoundResult lastResult = game.getLastResult();
      if (game.isGameOver()) {
         messageLabel.setText("Out of chips! Press NEW GAME");
         messageLabel.setForeground(LOSE_RED);
      } else if (!game.isRoundActive() && lastResult != null) {
         if (lastResult.isWin()) {
            messageLabel.setForeground(WIN_GREEN);
         } else if (lastResult.isLoss()) {
            messageLabel.setForeground(LOSE_RED);
         }
      }
   }

   private void showHistory() {
      ArrayList<RoundResult> history = game.getHistory();
      if (history.isEmpty()) {
         historyArea.setText("No rounds played yet.");
         return;
      }

      String text = "";
      for (int i = 0; i < history.size() && i < 12; i++) {
         text += history.get(i).toString() + "\n\n";
      }
      historyArea.setText(text);
   }

   // Turns buttons on or off depending on what the player is allowed to do right now.
   private void updateButtons() {
      boolean betting = !game.isRoundActive() && !game.isGameOver();

      chip10Button.setEnabled(betting);
      chip25Button.setEnabled(betting);
      chip50Button.setEnabled(betting);
      chip100Button.setEnabled(betting);
      clearButton.setEnabled(betting && pendingBet > 0);
      dealButton.setEnabled(betting);
      hitButton.setEnabled(game.isPlayerTurn());
      standButton.setEnabled(game.isPlayerTurn());
      doubleButton.setEnabled(game.canDoubleDown());
      splitButton.setEnabled(game.canSplit());
      newGameButton.setEnabled(game.isGameOver());
   }

   private void showSplitHand() {
      boolean visible = game.hasSplitHand();
      splitLabel.setVisible(visible);
      splitCardsPanel.setVisible(visible);
      if (visible) {
         ArrayList<Card> cards = game.getPlayer().getSplitHand().getCards();
         while (splitCardsPanel.getComponentCount() > cards.size()) {
            splitCardsPanel.remove(splitCardsPanel.getComponentCount() - 1);
         }
         for (int i = 0; i < cards.size(); i++) {
            if (i < splitCardsPanel.getComponentCount()) {
               ((CardPanel) splitCardsPanel.getComponent(i)).updateCard(cards.get(i), 0);
            } else {
               splitCardsPanel.add(new CardPanel(cards.get(i), i * 180, true));
            }
         }
         splitLabel.setText("SPLIT HAND   " + game.getPlayer().getSplitHand().getTotal()
               + "   Bet: " + game.getPlayer().getSplitBet());
      } else {
         splitLabel.setText("");
         splitCardsPanel.removeAll();
      }
      splitCardsPanel.revalidate();
      splitCardsPanel.repaint();
   }

   // Paints the felt, the betting arc, and the center rule text behind the
   // transparent seat panels. The seats are positioned in doLayout so the
   // table remains usable when the window is resized.
   private class CasinoTablePanel extends JPanel {
      private final Color FELT = new Color(18, 91, 67);
      private final Color FELT_DARK = new Color(11, 65, 51);
      private final Color TABLE_LINE = new Color(195, 211, 192);

      CasinoTablePanel() {
         setOpaque(true);
         setBackground(FELT_DARK);
      }

      @Override
      protected void paintComponent(java.awt.Graphics graphics) {
         super.paintComponent(graphics);
         java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
         g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
               java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

         int w = getWidth();
         int h = getHeight();
         g.setColor(FELT);
         g.fillRoundRect(12, 12, w - 24, h - 24, 70, 70);

         g.setColor(new Color(7, 52, 42));
         g.fillRoundRect(28, 28, w - 56, h - 56, 60, 60);
         g.setColor(FELT);
         g.fillRoundRect(34, 34, w - 68, h - 68, 56, 56);

         g.setColor(TABLE_LINE);
         g.setStroke(new java.awt.BasicStroke(2.2f));
         g.drawArc(55, 72, w - 110, h - 35, 12, 156);
         g.setColor(new Color(135, 165, 151));
         g.setStroke(new java.awt.BasicStroke(1.2f));
         g.drawArc(74, 88, w - 148, h - 60, 12, 156);

         g.dispose();
      }

      @Override
      public void doLayout() {
         int w = getWidth();
         int h = getHeight();
         int sideMargin = Math.max(15, w / 45);

         dealerSeat.setBounds(w / 2 - 155, 8, 310, 178);
         aiPlayersPanel.setBounds(sideMargin, 170, w - sideMargin * 2, 178);
         middlePanel.setBounds(w / 2 - 260, 330, 520, 100);
         aiPlayersPanel.setBounds(18, Math.max(425, h - 178), w - 36, 168);
      }
   }
}
