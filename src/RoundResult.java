// The record of one finished round, shown in the "Recent Rounds" list.
public class RoundResult {
   private int roundNumber;
   private String message;
   private int chipChange;   // positive = won chips, negative = lost chips, 0 = push

   public RoundResult(int roundNumber, String message, int chipChange) {
      this.roundNumber = roundNumber;
      this.message = message;
      this.chipChange = chipChange;
   }

   public int getRoundNumber() {
      return roundNumber;
   }

   public String getMessage() {
      return message;
   }

   public int getChipChange() {
      return chipChange;
   }

   public boolean isWin() {
      return chipChange > 0;
   }

   public boolean isLoss() {
      return chipChange < 0;
   }

   @Override
   public String toString() {
      String change = String.valueOf(chipChange);
      if (chipChange > 0) {
         change = "+" + chipChange;
      }
      return "Round " + roundNumber + ": " + message + " (" + change + ")";
   }
}
