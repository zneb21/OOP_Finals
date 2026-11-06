import javax.swing.JOptionPane;

// Starting point of the program. Run this file to play.
public class Main {
   public static void main(String[] args) {
      String name = JOptionPane.showInputDialog("Enter your name:");
      if (name == null || name.trim().isEmpty()) {
         name = "Player";
      }

      GameWindow window = new GameWindow(name.trim());
      window.setVisible(true);
      window.showHowToPlay();
   }
}
