import java.util.ArrayList;

// Controls one game of Blackjack: dealing, turns, and deciding who wins.
// This class has no GUI code, so the rules can be tested on their own.
public class BlackjackGame {
   public static final int STARTING_CHIPS = 1000;
   private static final int RESHUFFLE_LIMIT = 15;   // shuffle a new deck when fewer cards are left

   private Deck deck;
   private Player player;
   private Dealer dealer;
   private ArrayList<RoundResult> history;
   private boolean roundActive;   // true from the deal until the round is settled
   private boolean playerTurn;
   private int roundNumber;
   private String message;

   public BlackjackGame(String playerName) {
      deck = new Deck();
      player = new Player(playerName, STARTING_CHIPS);
      dealer = new Dealer();
      history = new ArrayList<>();
      roundActive = false;
      playerTurn = false;
      roundNumber = 0;
      message = "Place your bet to begin";
   }

   public Player getPlayer() {
      return player;
   }

   public Dealer getDealer() {
      return dealer;
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
      return roundActive && !playerTurn;
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
      if (deck.cardsLeft() < RESHUFFLE_LIMIT) {
         deck.reset();
      }

      // Deal two cards each. The dealer's second card (the "hole card") is face down.
      player.takeCard(deck.drawCard());
      dealer.takeCard(deck.drawCard());
      player.takeCard(deck.drawCard());
      Card holeCard = deck.drawCard();
      holeCard.setFaceUp(false);
      dealer.takeCard(holeCard);

      roundNumber++;
      roundActive = true;
      playerTurn = true;
      message = "Hit or Stand?";

      // If anyone has Blackjack, the round ends right away.
      if (player.getHand().isBlackjack() || dealer.getHand().isBlackjack()) {
         finishRound();
      }
   }

   public void hit() {
      if (!playerTurn) {
         return;
      }
      player.takeCard(deck.drawCard());
      if (player.getHand().isBust()) {
         finishRound();
      } else if (!player.canHit()) {
         stand();   // reached 21, nothing better to do
      }
   }

   // The player is done. The dealer shows the hole card and it becomes the dealer's turn.
   public void stand() {
      if (!playerTurn) {
         return;
      }
      player.stand();
      playerTurn = false;
      dealer.revealHoleCard();
      message = "Dealer's turn";
   }

   // Double down is only allowed on the first two cards.
   public boolean canDoubleDown() {
      return playerTurn && player.getHand().size() == 2;
   }

   // Doubles the bet, takes exactly one more card, then stands.
   public void doubleDown() throws InvalidBetException {
      if (!canDoubleDown()) {
         return;
      }
      player.doubleBet();
      player.takeCard(deck.drawCard());
      if (player.getHand().isBust()) {
         finishRound();
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
      while (dealerShouldHit()) {
         dealerHit();
      }
      finishRound();
   }

   // Compares the hands, pays the player, and saves the result.
   public void finishRound() {
      if (!roundActive) {
         return;
      }
      roundActive = false;
      playerTurn = false;
      dealer.revealHoleCard();

      Hand playerHand = player.getHand();
      Hand dealerHand = dealer.getHand();
      int bet = player.getBet();
      int payout;   // chips given back to the player, including the original bet

      if (playerHand.isBust()) {
         payout = 0;
         message = "Bust! You lose";
      } else if (playerHand.isBlackjack() && dealerHand.isBlackjack()) {
         payout = bet;
         message = "Both have Blackjack - Push";
      } else if (playerHand.isBlackjack()) {
         payout = bet + (bet * 3) / 2;   // Blackjack pays 3 to 2
         message = "Blackjack! You win";
      } else if (dealerHand.isBlackjack()) {
         payout = 0;
         message = "Dealer has Blackjack";
      } else if (dealerHand.isBust()) {
         payout = bet * 2;
         message = "Dealer busts! You win";
      } else if (playerHand.getTotal() > dealerHand.getTotal()) {
         payout = bet * 2;
         message = "You win!";
      } else if (playerHand.getTotal() < dealerHand.getTotal()) {
         payout = 0;
         message = "Dealer wins";
      } else {
         payout = bet;
         message = "Push - bet returned";
      }

      player.collectPayout(payout);
      history.add(0, new RoundResult(roundNumber, message, payout - bet));
   }

   // Starts over with full chips after the player runs out.
   public void resetGame() {
      player.resetChips(STARTING_CHIPS);
      player.clearHand();
      dealer.clearHand();
      deck.reset();
      history.clear();
      roundActive = false;
      playerTurn = false;
      roundNumber = 0;
      message = "Place your bet to begin";
   }
}
