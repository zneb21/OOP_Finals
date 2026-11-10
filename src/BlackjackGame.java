import java.util.ArrayList;

// Controls one game of Blackjack: dealing, turns, and deciding who wins.
// This class has no GUI code, so the rules can be tested on their own.
public class BlackjackGame {
   public static final int STARTING_CHIPS = 1000;
   private static final int RESHUFFLE_LIMIT = 30;   // reserve enough cards for a full table round
   private static final int AI_PLAYER_COUNT = 4;

   private Deck deck;
   private Player player;
   private Dealer dealer;
   private ArrayList<AIPlayer> aiPlayers;
   private ArrayList<Participant> seats;
   private int humanSeat;
   private int activeSeat;
   private boolean vsAiTable;
   private ArrayList<RoundResult> history;
   private boolean roundActive;   // true from the deal until the round is settled
   private boolean playerTurn;
   private boolean splitTurn;
   private boolean aiTurn;
   private boolean dealerPhase;
   private int roundNumber;
   private String message;

   public BlackjackGame(String playerName) {
      this(playerName, false);
   }

   public BlackjackGame(String playerName, boolean vsAiTable) {
      this(playerName, vsAiTable, 2);
   }

   public BlackjackGame(String playerName, boolean vsAiTable, int humanSeat) {
      deck = new Deck();
      player = new Player(playerName, STARTING_CHIPS);
      dealer = new Dealer();
      this.vsAiTable = vsAiTable;
      this.humanSeat = vsAiTable ? Math.max(0, Math.min(4, humanSeat)) : 0;
      aiPlayers = new ArrayList<>();
      seats = new ArrayList<>();
      if (vsAiTable) {
         int aiNumber = 1;
         for (int seat = 0; seat < AI_PLAYER_COUNT + 1; seat++) {
            if (seat == this.humanSeat) {
               seats.add(player);
            } else {
               AIPlayer ai = new AIPlayer("AI " + aiNumber++);
               aiPlayers.add(ai);
               seats.add(ai);
            }
         }
      } else {
         seats.add(player);
      }
      history = new ArrayList<>();
      roundActive = false;
      playerTurn = false;
      splitTurn = false;
      aiTurn = false;
      dealerPhase = false;
      roundNumber = 0;
      message = "Place your bet to begin";
   }

   public Player getPlayer() {
      return player;
   }

   public Dealer getDealer() {
      return dealer;
   }

   public ArrayList<AIPlayer> getAiPlayers() {
      return new ArrayList<>(aiPlayers);
   }

   public ArrayList<Participant> getSeats() {
      return new ArrayList<>(seats);
   }

   public boolean isVsAiTable() {
      return vsAiTable;
   }

   public int getHumanSeat() {
      return humanSeat;
   }

   public boolean isAiTurn() {
      return aiTurn;
   }

   public Participant getActiveSeat() {
      return activeSeat >= 0 && activeSeat < seats.size() ? seats.get(activeSeat) : null;
   }

   public String getActiveSeatName() {
      Participant active = getActiveSeat();
      return roundActive && active != null ? active.getName() : "";
   }

   public boolean needsReshuffle() {
      return deck.cardsLeft() < RESHUFFLE_LIMIT;
   }

   public void reshuffle() {
      deck.reset();
      message = "Place your bet to begin";
   }

   public int getCardsLeft() {
      return deck.cardsLeft();
   }

   public boolean isRoundActive() {
      return roundActive;
   }

   public boolean isPlayerTurn() {
      return playerTurn;
   }

   public boolean isDealerTurn() {
      return roundActive && dealerPhase;
   }

   public boolean hasSplitHand() {
      return player.hasSplitHand();
   }

   public boolean isSplitTurn() {
      return playerTurn && splitTurn;
   }

   public boolean canSplit() {
      return playerTurn && !player.hasSplitHand() && player.getHand().size() == 2
            && player.getHand().getCards().get(0).getRank()
                  .equals(player.getHand().getCards().get(1).getRank())
            && player.getBet() <= player.getChips();
   }

   public int getRoundNumber() {
      return roundNumber;
   }

   public String getMessage() {
      return message;
   }

   // Newest round first. Returns a copy so the list cannot be changed from outside.
   public ArrayList<RoundResult> getHistory() {
      return new ArrayList<>(history);
   }

   // The result of the round that just finished, or null if no round was played yet.
   public RoundResult getLastResult() {
      if (history.isEmpty()) {
         return null;
      }
      return history.get(0);
   }

   public int getWins() {
      int count = 0;
      for (RoundResult result : history) {
         if (result.isWin()) {
            count++;
         }
      }
      return count;
   }

   public int getLosses() {
      int count = 0;
      for (RoundResult result : history) {
         if (result.isLoss()) {
            count++;
         }
      }
      return count;
   }

   public int getPushes() {
      return history.size() - getWins() - getLosses();
   }

   // The game is over when no round is running and the player has no chips left.
   public boolean isGameOver() {
      return !roundActive && player.getChips() == 0;
   }

   // Starts a new round. Throws InvalidBetException if the bet breaks the betting rules.
   public void startRound(int betAmount) throws InvalidBetException {
      if (roundActive) {
         return;
      }
      player.placeBet(betAmount);

      player.clearHand();
      dealer.clearHand();
      // The window shows the re-shuffling screen before calling this method
      // when the shoe is low, so this round always starts with a full shoe.

      // Deal the dealer separately so its second card can stay hidden. The
      // player and AI seats are dealt exactly two cards in the seat loop below.
      dealer.takeCard(deck.drawCard());
      Card holeCard = deck.drawCard();
      holeCard.setFaceUp(false);
      dealer.takeCard(holeCard);

      for (AIPlayer aiPlayer : aiPlayers) {
         aiPlayer.clearHand();
      }
      for (Participant seat : seats) {
         seat.takeCard(deck.drawCard());
         seat.takeCard(deck.drawCard());
      }

      roundNumber++;
      roundActive = true;
      playerTurn = false;
      splitTurn = false;
      aiTurn = false;
      dealerPhase = false;
      activeSeat = 0;
      message = "Players take turns by seat";

      // If anyone has Blackjack, the round ends right away.
      if (dealer.getHand().isBlackjack()) {
         dealerPhase = true;
         finishRound();
      } else {
         advanceSeat();
      }
   }

   private void advanceSeat() {
      playerTurn = false;
      aiTurn = false;
      while (activeSeat < seats.size()) {
         Participant current = seats.get(activeSeat);
         if (current == player) {
            if (player.getHand().isBlackjack()) {
               activeSeat++;
               continue;
            }
            playerTurn = true;
            message = player.getName() + "'s turn — Hit or Stand?";
            return;
         }
         if (current instanceof AIPlayer && !current.getHand().isBlackjack()) {
            aiTurn = true;
            message = current.getName() + "'s turn";
            return;
         }
         activeSeat++;
      }
      dealerPhase = true;
      dealer.revealHoleCard();
      message = "Dealer's turn";
   }

   // One card per tick lets the UI show each AI turn in seat order.
   public void playAiStep() {
      if (!roundActive || !aiTurn) return;
      AIPlayer current = (AIPlayer) seats.get(activeSeat);
      if (current.canHit()) {
         current.takeCard(deck.drawCard());
      }
      if (!current.canHit()) {
         activeSeat++;
         advanceSeat();
      }
   }

   public void hit() {
      if (!playerTurn) {
         return;
      }
      Hand activeHand = splitTurn ? player.getSplitHand() : player.getHand();
      activeHand.addCard(deck.drawCard());
      if (activeHand.isBust()) {
         if (!splitTurn && player.hasSplitHand()) {
            stand();
         } else {
            activeSeat++;
            advanceSeat();
         }
      }
   }

   // The player is done. The dealer shows the hole card and it becomes the dealer's turn.
   public void stand() {
      if (!playerTurn) {
         return;
      }
      if (!splitTurn && player.hasSplitHand()) {
         player.stand();
         splitTurn = true;
         message = "Play your split hand";
         return;
      }
      if (splitTurn) {
         player.standSplit();
      } else {
         player.stand();
      }
      playerTurn = false;
      activeSeat++;
      advanceSeat();
   }

   // Double down is only allowed on the first two cards.
   public boolean canDoubleDown() {
      Hand activeHand = splitTurn ? player.getSplitHand() : player.getHand();
      int activeBet = splitTurn ? player.getSplitBet() : player.getBet();
      return playerTurn && activeHand.size() == 2 && activeBet <= player.getChips();
   }

   public void split() throws InvalidBetException {
      if (!canSplit()) {
         return;
      }
      player.placeSplitBet();
      Card firstCard = player.getHand().getCards().get(0);
      Card secondCard = player.getHand().getCards().get(1);
      player.getHand().clear();
      player.takeCard(firstCard);
      player.getSplitHand().addCard(new Card(secondCard.getRank(), secondCard.getSuit()));
      player.takeCard(deck.drawCard());
      player.getSplitHand().addCard(deck.drawCard());
      message = "Play your first hand";
   }

   // Doubles the bet, takes exactly one more card, then stands.
   public void doubleDown() throws InvalidBetException {
      if (!canDoubleDown()) {
         return;
      }
      Hand activeHand = splitTurn ? player.getSplitHand() : player.getHand();
      if (splitTurn) {
         player.doubleSplitBet();
      } else {
         player.doubleBet();
      }
      activeHand.addCard(deck.drawCard());
      if (activeHand.isBust()) {
         if (!splitTurn && player.hasSplitHand()) stand();
         else { activeSeat++; advanceSeat(); }
      } else {
         stand();
      }
   }

   // The dealer's turn is done one card at a time, so the window can show each card.
   public boolean dealerShouldHit() {
      return isDealerTurn() && dealer.canHit();
   }

   public void dealerHit() {
      if (dealerShouldHit()) {
         dealer.takeCard(deck.drawCard());
      }
   }

   // Plays the whole dealer turn at once (used by TestCases).
   public void playDealerTurn() {
      if (!isDealerTurn()) {
         return;
      }
      while (dealerShouldHit()) {
         dealerHit();
      }
      finishRound();
   }

   // Compares the hands, pays the player, and saves the result.
   public void finishRound() {
      if (!roundActive || !dealerPhase) {
         return;
      }

      // A round may finish during the initial deal, after a bust, or after the
      // dealer has completed their turn. Ignore calls from any other phase.
      if (dealer.canHit() && !dealer.getHand().isBlackjack()) {
         return;
      }

      roundActive = false;
      playerTurn = false;
      dealer.revealHoleCard();

      Hand playerHand = player.getHand();
      Hand dealerHand = dealer.getHand();
      int totalChange = settleHand(playerHand, player.getBet(), dealerHand, !player.hasSplitHand());
      player.collectPayout(settlePayout(playerHand, player.getBet(), dealerHand, !player.hasSplitHand()));
      if (player.hasSplitHand()) {
         totalChange += settleHand(player.getSplitHand(), player.getSplitBet(), dealerHand, false);
         player.collectSplitPayout(settlePayout(player.getSplitHand(), player.getSplitBet(), dealerHand, false));
      }
      message = totalChange >= 0 ? "Round complete" : "Round complete - you lose";
      history.add(0, new RoundResult(roundNumber, message, totalChange));
   }

   private int settlePayout(Hand playerHand, int bet, Hand dealerHand, boolean allowBlackjackPayout) {
      if (playerHand.isBust() || dealerHand.isBlackjack()) return 0;
      if (allowBlackjackPayout && playerHand.isBlackjack()) return bet + (bet * 3) / 2;
      if (dealerHand.isBust() || playerHand.getTotal() > dealerHand.getTotal()) return bet * 2;
      if (playerHand.getTotal() == dealerHand.getTotal()) return bet;
      return 0;
   }

   private int settleHand(Hand playerHand, int bet, Hand dealerHand, boolean allowBlackjackPayout) {
      return settlePayout(playerHand, bet, dealerHand, allowBlackjackPayout) - bet;
   }

   // Starts over with full chips after the player runs out.
   public void resetGame() {
      player.resetChips(STARTING_CHIPS);
      player.clearHand();
      dealer.clearHand();
      for (AIPlayer aiPlayer : aiPlayers) {
         aiPlayer.clearHand();
      }
      deck.reset();
      history.clear();
      roundActive = false;
      playerTurn = false;
      splitTurn = false;
      aiTurn = false;
      dealerPhase = false;
      roundNumber = 0;
      message = "Place your bet to begin";
   }
}
