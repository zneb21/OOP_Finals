// The house. Plays by a fixed rule instead of making choices.
public class Dealer extends Participant {
   private static final int STAND_VALUE = 17;

   public Dealer() {
      super("Dealer");
   }

   // House rule: the dealer must hit on 16 or less and stand on 17 or more.
   @Override
   public boolean canHit() {
      return getHand().getTotal() < STAND_VALUE;
   }

   public void revealHoleCard() {
      getHand().showAllCards();
   }

   // While one card is face down, only show the total of the visible cards.
   @Override
   public String getScoreText() {
      if (getHand().hasHiddenCard()) {
         return getHand().getVisibleTotal() + " + ?";
      }
      return super.getScoreText();
   }
}
