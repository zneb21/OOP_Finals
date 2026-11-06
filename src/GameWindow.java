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
   private JLabel messageLabel;
   private JLabel deckLabel;
   private JTextArea historyArea;

   // Areas where the cards are shown
   private JPanel dealerCardsPanel;
   private JPanel playerCardsPanel;

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
   private JButton newGameButton;
   private JButton howToPlayButton;

   private Timer dealerTimer;   // makes the dealer draw one card at a time

   public GameWindow(String playerName) {
      game = new BlackjackGame(playerName);
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
      info.add(balanceLabel);
      info.add(betLabel);
      info.add(statsLabel);

      bar.add(title, BorderLayout.WEST);
      bar.add(info, BorderLayout.EAST);
      return bar;
   }

   // The green table, split into 3 rows: dealer, message, player.
   private JPanel createTable() {
      JPanel table = new JPanel(new GridLayout(3, 1));
      table.setBackground(TABLE_GREEN);
      table.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

      dealerLabel = createLabel("DEALER", 18, Color.WHITE);
      dealerCardsPanel = createCardsArea();
      table.add(createHandArea(dealerLabel, dealerCardsPanel));

      JPanel middle = new JPanel(new GridLayout(2, 1));
      middle.setBackground(TABLE_GREEN);
      messageLabel = createLabel("", 30, GOLD);
      messageLabel.setFont(new Font("Georgia", Font.BOLD, 32));
      messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
      messageLabel.setVerticalAlignment(SwingConstants.BOTTOM);
      JLabel rulesLabel = createLabel("BLACKJACK PAYS 3 TO 2   |   DEALER STANDS ON 17", 14, SOFT_WHITE);
      rulesLabel.setHorizontalAlignment(SwingConstants.CENTER);
      rulesLabel.setVerticalAlignment(SwingConstants.TOP);
      rulesLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
      middle.add(messageLabel);
      middle.add(rulesLabel);
      table.add(middle);

      playerLabel = createLabel("", 18, Color.WHITE);
      playerCardsPanel = createCardsArea();
      table.add(createHandArea(playerLabel, playerCardsPanel));
      return table;
   }

   // One hand on the table: the name and score on top, the cards below.
   private JPanel createHandArea(JLabel label, JPanel cardsPanel) {
      JPanel area = new JPanel(new BorderLayout());
      area.setBackground(TABLE_GREEN);
      label.setHorizontalAlignment(SwingConstants.CENTER);
      area.add(label, BorderLayout.NORTH);
      area.add(cardsPanel, BorderLayout.CENTER);
      return area;
   }

   private JPanel createCardsArea() {
      JPanel cardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
      cardsPanel.setBackground(TABLE_GREEN);
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
      newGameButton = createButton("NEW GAME", new Color(200, 150, 40));
      howToPlayButton = createButton("HOW TO PLAY", new Color(95, 95, 100));
      actionArea.add(dealButton);
      actionArea.add(hitButton);
      actionArea.add(standButton);
      actionArea.add(doubleButton);
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
      try {
         game.startRound(pendingBet);
      } catch (InvalidBetException ex) {
         showWarning(ex.getMessage());
      }
   }

   private void doubleDown() {
      try {
         game.doubleDown();
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

      showHand(dealerLabel, dealerCardsPanel, game.getDealer());
      showHand(playerLabel, playerCardsPanel, player);
      showMessage();
      showHistory();
      updateButtons();

      if (game.isDealerTurn() && !dealerTimer.isRunning()) {
         dealerTimer.start();
      }
   }

   // Works for both the Player and the Dealer, because both are Participants.
   // getScoreText() runs the Dealer's version or the Player's version (polymorphism).
   private void showHand(JLabel label, JPanel cardsPanel, Participant participant) {
      cardsPanel.removeAll();
      for (Card card : participant.getHand().getCards()) {
         cardsPanel.add(new CardPanel(card));
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
      newGameButton.setEnabled(game.isGameOver());
   }
}
