import java.util.ArrayList;
import java.util.Collections;

// Two standard 52-card packs shuffled together (104 cards total).
public class Deck {
   private static final int NUMBER_OF_DECKS = 2;
   private static final String[] RANKS = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
   private static final String[] SUITS = {"Hearts", "Diamonds", "Clubs", "Spades"};

   private ArrayList<Card> cards;

   public Deck() {
      cards = new ArrayList<>();
      reset();
   }

   // Builds two fresh 52-card packs and shuffles them together.
   public void reset() {
      cards.clear();
      for (int deckNumber = 0; deckNumber < NUMBER_OF_DECKS; deckNumber++) {
         for (String suit : SUITS) {
            for (String rank : RANKS) {
               cards.add(new Card(rank, suit));
            }
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
