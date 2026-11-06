import java.util.ArrayList;

// The cards one participant is holding, and the rules for adding them up.
public class Hand {
   private ArrayList<Card> cards;

   public Hand() {
      cards = new ArrayList<>();
   }

   public void addCard(Card card) {
      cards.add(card);
   }

   // Returns a copy so other classes can read the cards but cannot add or remove them.
   public ArrayList<Card> getCards() {
      return new ArrayList<>(cards);
   }

   public int size() {
      return cards.size();
   }

   public void clear() {
      cards.clear();
   }

   // Total of every card in the hand.
   public int getTotal() {
      return calculateTotal(false);
   }

   // Total of only the face-up cards (what the player can see of the dealer's hand).
   public int getVisibleTotal() {
      return calculateTotal(true);
   }

   public boolean isBust() {
      return getTotal() > 21;
   }

   // Blackjack = exactly 21 with the first two cards.
   public boolean isBlackjack() {
      return cards.size() == 2 && getTotal() == 21;
   }

   public boolean hasHiddenCard() {
      for (Card card : cards) {
         if (!card.isFaceUp()) {
            return true;
         }
      }
      return false;
   }

   public void showAllCards() {
      for (Card card : cards) {
         card.setFaceUp(true);
      }
   }

   // Each Ace starts as 11. While the total is over 21, one Ace at a time is changed to 1.
   private int calculateTotal(boolean onlyFaceUp) {
      int total = 0;
      int aces = 0;
      for (Card card : cards) {
         // Skip face-down cards only when we want the visible total.
         if (card.isFaceUp() || !onlyFaceUp) {
            total += card.getValue();
            if (card.isAce()) {
               aces++;
            }
         }
      }
      while (total > 21 && aces > 0) {
         total -= 10;
         aces--;
      }
      return total;
   }
}
