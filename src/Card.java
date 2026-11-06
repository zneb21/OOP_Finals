// One playing card, for example the Ace of Spades.
public class Card {
   private String rank;      // "A", "2" to "10", "J", "Q", "K"
   private String suit;      // "Hearts", "Diamonds", "Clubs", "Spades"
   private boolean faceUp;   // false = the card is shown from the back

   public Card(String rank, String suit) {
      this.rank = rank;
      this.suit = suit;
      this.faceUp = true;
   }

   public String getRank() {
      return rank;
   }

   public String getSuit() {
      return suit;
   }

   public boolean isFaceUp() {
      return faceUp;
   }

   public void setFaceUp(boolean faceUp) {
      this.faceUp = faceUp;
   }

   // Blackjack value of the card. An Ace counts as 11 here;
   // the Hand class lowers it to 1 when the total goes over 21.
   public int getValue() {
      if (isAce()) {
         return 11;
      }
      if (isFaceCard()) {
         return 10;
      }
      return Integer.parseInt(rank);
   }

   public boolean isAce() {
      return rank.equals("A");
   }

   public boolean isFaceCard() {
      return rank.equals("J") || rank.equals("Q") || rank.equals("K");
   }

   public boolean isRed() {
      return suit.equals("Hearts") || suit.equals("Diamonds");
   }

   // The suit symbol. Written as unicode escape codes so the file works in any editor (jGRASP, VS Code).
   public String getSuitSymbol() {
      if (suit.equals("Hearts")) {
         return "♥";
      } else if (suit.equals("Diamonds")) {
         return "♦";
      } else if (suit.equals("Clubs")) {
         return "♣";
      } else {
         return "♠";
      }
   }

   @Override
   public String toString() {
      return rank + " of " + suit;
   }
}
