import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import javax.swing.Timer;

// Shows one card on the screen and gives newly dealt cards a short flip animation.
public class CardPanel extends JPanel {
   private static final Color CARD_RED = new Color(200, 30, 45);
   private static final Color CARD_BLACK = new Color(30, 30, 35);
   private static final Color BACK_RED = new Color(120, 20, 35);
   private static final Color GOLD = new Color(235, 195, 90);

   private static final int ANIMATION_FRAMES = 10;
   private Card card;
   private boolean displayedFaceUp;
   private int animationFrame;
   private Timer animationTimer;

   public CardPanel(Card card) {
      this(card, 0);
   }

   public CardPanel(Card card, int animationDelay) {
      this(card, animationDelay, false);
   }

   public CardPanel(Card card, int animationDelay, boolean compact) {
      this.card = card;
      this.displayedFaceUp = card.isFaceUp();
      setPreferredSize(compact ? new Dimension(44, 60) : new Dimension(108, 148));
      setOpaque(false);
      startFlipAnimation(animationDelay);
   }

   // Updates the card without restarting the animation for an unchanged card.
   public void updateCard(Card newCard, int animationDelay) {
      boolean needsAnimation = card != newCard || displayedFaceUp != newCard.isFaceUp();
      card = newCard;
      if (needsAnimation) {
         displayedFaceUp = newCard.isFaceUp();
         if (animationTimer != null) {
            animationTimer.stop();
         }
         startFlipAnimation(animationDelay);
      }
      repaint();
   }

   private void startFlipAnimation(int animationDelay) {
      animationFrame = 0;
      animationTimer = new Timer(30, event -> {
         animationFrame++;
         repaint();
         if (animationFrame >= ANIMATION_FRAMES) {
            animationTimer.stop();
         }
      });
      animationTimer.setInitialDelay(animationDelay);
      animationTimer.start();
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);

      // A sine curve makes the card slow down naturally at the start and end.
      double progress = Math.min(1.0, animationFrame / (double) ANIMATION_FRAMES);
      double flipWidth = Math.max(0.06, Math.sin(progress * Math.PI / 2.0));
      boolean showFront = card.isFaceUp() && progress >= 0.5;

      Graphics2D g = (Graphics2D) graphics.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.scale(getWidth() / 108.0, getHeight() / 148.0);
      g.translate(getWidth() / 2.0, 0);
      g.scale(flipWidth, 1.0);
      g.translate(-getWidth() / 2.0, 0);

      if (showFront) {
         paintFront(g, card);
      } else {
         paintBack(g);
      }
      g.dispose();
   }

   private void paintFront(Graphics2D g, Card card) {
      Color color = card.isRed() ? CARD_RED : CARD_BLACK;
      g.setColor(new Color(0, 0, 0, 70));
      g.fillRoundRect(4, 5, 100, 140, 10, 10);
      g.setColor(Color.WHITE);
      g.fillRoundRect(2, 2, 100, 140, 10, 10);
      g.setColor(new Color(170, 170, 170));
      g.drawRoundRect(2, 2, 100, 140, 10, 10);

      g.setFont(new Font(Font.SERIF, Font.BOLD, 22));
      g.setColor(color);
      g.drawString(card.getRank(), 11, 28);
      g.drawString(card.getRank(), 77, 131);
      g.setFont(new Font(Font.SERIF, Font.BOLD, 48));
      drawCenteredString(g, card.getSuitSymbol(), 52, 88);
   }

   // A face-down card has a dark red back and gold border.
   private void paintBack(Graphics2D g) {
      g.setColor(new Color(0, 0, 0, 70));
      g.fillRoundRect(4, 5, 100, 140, 10, 10);
      g.setColor(BACK_RED);
      g.fillRoundRect(2, 2, 100, 140, 10, 10);
      g.setColor(Color.WHITE);
      g.drawRoundRect(2, 2, 100, 140, 10, 10);
      g.setColor(GOLD);
      g.drawRoundRect(8, 8, 88, 128, 7, 7);

      g.setFont(new Font(Font.SERIF, Font.BOLD, 44));
      g.setColor(GOLD);
      drawCenteredString(g, "?", 52, 90);
   }

   private void drawCenteredString(Graphics2D g, String text, int centerX, int baseline) {
      int width = g.getFontMetrics().stringWidth(text);
      g.drawString(text, centerX - width / 2, baseline);
   }
}
