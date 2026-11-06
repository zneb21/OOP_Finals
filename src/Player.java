// The human player. Has chips and places bets.
public class Player extends Participant {
   private int chips;
   private int bet;
   private boolean standing;

   public Player(String name, int startingChips) {
      super(name);
      this.chips = startingChips;
      this.bet = 0;
      this.standing = false;
   }

   public int getChips() {
      return chips;
   }

   public int getBet() {
      return bet;
   }

   // Business rule check. Throws InvalidBetException if the amount is not allowed.
   public void validateBet(int amount) throws InvalidBetException {
      if (amount <= 0) {
         throw new InvalidBetException("Place a bet first by clicking the chips.");
      }
      if (amount > chips) {
         throw new InvalidBetException("Not enough chips. You only have " + chips + ".");
      }
   }

   // Moves the bet from the player's chips to the table.
   public void placeBet(int amount) throws InvalidBetException {
      validateBet(amount);
      chips -= amount;
      bet = amount;
   }

   // Double down: the player puts the same amount again on the table.
   public void doubleBet() throws InvalidBetException {
      if (bet > chips) {
         throw new InvalidBetException("Not enough chips to double down.");
      }
      chips -= bet;
      bet *= 2;
   }

   // Gives chips back after a round (0 if the player lost). The bet is then cleared.
   public void collectPayout(int payout) {
      chips += payout;
      bet = 0;
   }

   public void resetChips(int amount) {
      chips = amount;
      bet = 0;
   }

   public void stand() {
      standing = true;
   }

   public boolean isStanding() {
      return standing;
   }

   @Override
   public void clearHand() {
      super.clearHand();
      standing = false;
   }

   // The player may keep hitting until they stand or reach 21.
   @Override
   public boolean canHit() {
      return !standing && getHand().getTotal() < 21;
   }
}
