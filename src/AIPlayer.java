// A computer-controlled table player. AI players use the same deck as the
// human player and follow a simple stand-on-17 strategy.
public class AIPlayer extends Participant {
   public AIPlayer(String name) {
      super(name);
   }

   @Override
   public boolean canHit() {
      return getHand().getTotal() < 17;
   }

   public void playTurn(Deck deck) {
      while (canHit()) {
         takeCard(deck.drawCard());
      }
   }
}
