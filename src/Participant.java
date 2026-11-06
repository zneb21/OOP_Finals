// Parent class for everyone sitting at the table.
// It is abstract because there is no "plain" participant: it is always a Player or a Dealer.
public abstract class Participant {
   private String name;
   private Hand hand;

   public Participant(String name) {
      this.name = name;
      this.hand = new Hand();
   }

   public String getName() {
      return name;
   }

   public Hand getHand() {
      return hand;
   }

   public void takeCard(Card card) {
      hand.addCard(card);
   }

   public void clearHand() {
      hand.clear();
   }

   // Each subclass has its own rule for when it may take another card.
   public abstract boolean canHit();

   // Text shown in the score badge on the table.
   public String getScoreText() {
      if (hand.isBlackjack()) {
         return "BLACKJACK";
      }
      if (hand.isBust()) {
         return "BUST " + hand.getTotal();
      }
      return String.valueOf(hand.getTotal());
   }
}
