// Custom exception for the betting rules:
// a bet must be more than 0 and cannot be more than the chips the player has.
public class InvalidBetException extends Exception {
   public InvalidBetException(String message) {
      super(message);
   }
}
