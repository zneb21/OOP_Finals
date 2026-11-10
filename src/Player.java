// The human player. Has chips and places bets.
public class Player extends Participant {
   private int chips;
   private int bet;
   private boolean standing;
   private Hand splitHand;
   private int splitBet;
   private boolean splitStanding;

   public Player(String name, int startingChips) {
      super(name);
      this.chips = startingChips;
      this.bet = 0;
      this.standing = false;
      this.splitHand = new Hand();
      this.splitBet = 0;
      this.splitStanding = false;
   }

   public int getChips() {
      return chips;
   }

   public int getBet() {
      return bet;
   }

   public Hand getSplitHand() {
      return splitHand;
   }

   public int getSplitBet() {
      return splitBet;
   }

   public boolean hasSplitHand() {
      return splitBet > 0;
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

   public void placeSplitBet() throws InvalidBetException {
      validateBet(bet);
      chips -= bet;
      splitBet = bet;
   }

   public void doubleSplitBet() throws InvalidBetException {
      if (splitBet > chips) {
         throw new InvalidBetException("Not enough chips to double down.");
      }
      chips -= splitBet;
      splitBet *= 2;
   }

   // Gives chips back after a round (0 if the player lost). The bet is then cleared.
   public void collectPayout(int payout) {
      chips += payout;
      bet = 0;
   }

   public void collectSplitPayout(int payout) {
      chips += payout;
      splitBet = 0;
   }

   public void resetChips(int amount) {
      chips = amount;
      bet = 0;
      splitBet = 0;
   }

   public void stand() {
      standing = true;
   }

   public void standSplit() {
      splitStanding = true;
   }

   public boolean isStanding() {
      return standing;
   }

   public boolean isSplitStanding() {
      return splitStanding;
   }

   @Override
   public void clearHand() {
      super.clearHand();
      standing = false;
      splitHand.clear();
      splitBet = 0;
      splitStanding = false;
   }

   // The player may keep hitting until they stand or reach 21.
   @Override
   public boolean canHit() {
      return !standing && getHand().getTotal() < 21;
   }
}
