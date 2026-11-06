import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

// Shows one card on the screen, like a small white playing card.
// The rank is at the top-left and bottom-right, and the suit is in the middle.
public class CardPanel extends JPanel {
   private static final Color CARD_RED = new Color(200, 30, 45);
   private static final Color CARD_BLACK = new Color(30, 30, 35);
   private static final Color BACK_RED = new Color(120, 20, 35);
   private static final Color GOLD = new Color(235, 195, 90);

   public CardPanel(Card card) {
      setPreferredSize(new Dimension(95, 135));
      setLayout(new BorderLayout());

      if (card.isFaceUp()) {
         showFront(card);
      } else {
         showBack();
      }
   }

   private void showFront(Card card) {
      setBackground(Color.WHITE);
      setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(170, 170, 170), 2),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));

      Color color = CARD_BLACK;
      if (card.isRed()) {
         color = CARD_RED;
      }

      JLabel topRank = createLabel(card.getRank(), 22, color, SwingConstants.LEFT);
      JLabel suit = createLabel(card.getSuitSymbol(), 48, color, SwingConstants.CENTER);
      JLabel bottomRank = createLabel(card.getRank(), 22, color, SwingConstants.RIGHT);

      add(topRank, BorderLayout.NORTH);
      add(suit, BorderLayout.CENTER);
      add(bottomRank, BorderLayout.SOUTH);
   }

   // A face-down card: dark red with a gold border and a question mark.
   private void showBack() {
      setBackground(BACK_RED);
      setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 3),
            BorderFactory.createLineBorder(GOLD, 2)));

      JLabel mark = createLabel("?", 44, GOLD, SwingConstants.CENTER);
      add(mark, BorderLayout.CENTER);
   }

   // "Serif" is used because Java always finds the suit symbols in this font.
   private JLabel createLabel(String text, int size, Color color, int alignment) {
      JLabel label = new JLabel(text, alignment);
      label.setFont(new Font(Font.SERIF, Font.BOLD, size));
      label.setForeground(color);
      return label;
   }
}
