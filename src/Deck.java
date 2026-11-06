import java.util.ArrayList;
import java.util.Collections;

// A standard 52-card deck that can be shuffled and drawn from.
public class Deck {
   private static final String[] RANKS = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
   private static final String[] SUITS = {"Hearts", "Diamonds", "Clubs", "Spades"};

   private ArrayList<Card> cards;

   public Deck() {
      cards = new ArrayList<>();
      reset();
   }

   // Builds a fresh set of 52 cards and shuffles them.
   public void reset() {
      cards.clear();
      for (String suit : SUITS) {
         for (String rank : RANKS) {
            cards.add(new Card(rank, suit));
         }
      }
      Collections.shuffle(cards);
   }

   // Takes the top card. If the deck ever runs out, a new shuffled deck is used.
   public Card drawCard() {
      if (cards.isEmpty()) {
         reset();
      }
      return cards.remove(cards.size() - 1);
   }

   public int cardsLeft() {
      return cards.size();
   }
}
