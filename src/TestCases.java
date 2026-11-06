// Simple console tests for the game rules. Run this file on its own (it does not open a window).
// The output can be copied into the Testing section of the documentation.
public class TestCases {
   private static int passed = 0;
   private static int failed = 0;

   public static void main(String[] args) {
      System.out.println("BLACKJACK TEST CASES");
      System.out.println("====================");

      // 1-3: hand totals and Aces
      Hand hand1 = new Hand();
      hand1.addCard(new Card("A", "Spades"));
      hand1.addCard(new Card("K", "Hearts"));
      check("Ace + King is Blackjack", "true", String.valueOf(hand1.isBlackjack()));

      Hand hand2 = new Hand();
      hand2.addCard(new Card("A", "Spades"));
      hand2.addCard(new Card("A", "Hearts"));
      hand2.addCard(new Card("9", "Clubs"));
      check("Ace + Ace + 9 totals 21", "21", String.valueOf(hand2.getTotal()));

      Hand hand3 = new Hand();
      hand3.addCard(new Card("K", "Spades"));
      hand3.addCard(new Card("Q", "Hearts"));
      hand3.addCard(new Card("5", "Clubs"));
      check("King + Queen + 5 is a bust", "true", String.valueOf(hand3.isBust()));

      // 4-5: dealer rule
      Dealer dealer = new Dealer();
      dealer.takeCard(new Card("10", "Spades"));
      dealer.takeCard(new Card("6", "Hearts"));
      check("Dealer hits on 16", "true", String.valueOf(dealer.canHit()));
      dealer.takeCard(new Card("A", "Clubs"));
      check("Dealer stands on 17", "false", String.valueOf(dealer.canHit()));

      // 6: dealer's hidden card
      Dealer dealer2 = new Dealer();
      dealer2.takeCard(new Card("10", "Spades"));
      Card hidden = new Card("7", "Hearts");
      hidden.setFaceUp(false);
      dealer2.takeCard(hidden);
      check("Dealer score hides the hole card", "10 + ?", dealer2.getScoreText());

      // 7-10: betting rules (custom exception)
      Player player = new Player("Tester", 100);
      check("Bet of 0 is rejected", "InvalidBetException", tryBet(player, 0));
      check("Bet over the chips is rejected", "InvalidBetException", tryBet(player, 500));
      check("Valid bet of 60 is accepted", "OK", tryBet(player, 60));
      check("Chips after betting 60 from 100", "40", String.valueOf(player.getChips()));

      String doubleResult;
      try {
         player.doubleBet();   // needs 60 more but only 40 left
         doubleResult = "OK";
      } catch (InvalidBetException e) {
         doubleResult = "InvalidBetException";
      }
      check("Double down without enough chips", "InvalidBetException", doubleResult);

      // 11-12: a full round through BlackjackGame
      BlackjackGame game = new BlackjackGame("Tester");
      try {
         game.startRound(100);
      } catch (InvalidBetException e) {
         System.out.println("Unexpected: " + e.getMessage());
      }
      check("Each side gets 2 cards on the deal", "2 and 2",
            game.getPlayer().getHand().size() + " and " + game.getDealer().getHand().size());
      game.stand();
      game.playDealerTurn();
      check("One round is saved in the history", "1", String.valueOf(game.getHistory().size()));

      System.out.println("====================");
      System.out.println("Passed: " + passed + "   Failed: " + failed);
   }

   private static String tryBet(Player player, int amount) {
      try {
         player.placeBet(amount);
         return "OK";
      } catch (InvalidBetException e) {
         return "InvalidBetException";
      }
   }

   private static void check(String name, String expected, String actual) {
      if (expected.equals(actual)) {
         passed++;
         System.out.println("[PASS] " + name);
      } else {
         failed++;
         System.out.println("[FAIL] " + name);
      }
      System.out.println("       expected: " + expected + "   actual: " + actual);
   }
}
