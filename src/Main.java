import javax.swing.SwingUtilities;

// Starting point of the program. Run this file to play.
public class Main {
   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> {
         new StartWindow().setVisible(true);
      });
   }
}
