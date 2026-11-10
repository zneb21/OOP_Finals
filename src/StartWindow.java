import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

// Landing screen shown before the blackjack table.
public class StartWindow extends JFrame implements ActionListener {
   private static final Color BACKGROUND = new Color(14, 48, 35);
   private static final Color PANEL = new Color(20, 76, 53);
   private static final Color BAR = new Color(25, 25, 28);
   private static final Color GOLD = new Color(235, 195, 90);
   private static final Color MUTED = new Color(205, 225, 213);

   private JTextField nameField;
   private JRadioButton soloButton;
   private JRadioButton aiButton;
   private JComboBox<String> seatChoice;

   public StartWindow() {
      setTitle("Blackjack - Choose a Table");
      setSize(620, 520);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setLocationRelativeTo(null);
      setLayout(new BorderLayout());

      JPanel header = new JPanel(new BorderLayout());
      header.setBackground(BAR);
      header.setBorder(BorderFactory.createEmptyBorder(22, 30, 22, 30));
      JLabel title = label("BLACKJACK", 30, GOLD);
      title.setFont(new Font("Georgia", Font.BOLD, 32));
      header.add(title, BorderLayout.WEST);
      add(header, BorderLayout.NORTH);

      JPanel content = new JPanel(new GridLayout(0, 1, 0, 14));
      content.setBackground(BACKGROUND);
      content.setBorder(BorderFactory.createEmptyBorder(32, 70, 32, 70));

      JLabel welcome = label("Choose your table", 27, Color.WHITE);
      welcome.setHorizontalAlignment(SwingConstants.CENTER);
      content.add(welcome);

      JLabel subtitle = label("Enter your name and get ready to play.", 15, MUTED);
      subtitle.setHorizontalAlignment(SwingConstants.CENTER);
      content.add(subtitle);

      JPanel namePanel = new JPanel(new BorderLayout(12, 0));
      namePanel.setOpaque(false);
      namePanel.add(label("PLAYER NAME", 14, GOLD), BorderLayout.WEST);
      nameField = new JTextField("Player");
      nameField.setFont(new Font("Segoe UI", Font.PLAIN, 17));
      nameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GOLD, 1),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)));
      namePanel.add(nameField, BorderLayout.CENTER);
      content.add(namePanel);

      JPanel modes = new JPanel(new GridLayout(1, 2, 12, 0));
      modes.setOpaque(false);
      soloButton = modeButton("SOLO TABLE", "Practice a hand at your own pace.");
      aiButton = modeButton("VS AI TABLE", "Play against the computer dealer.");
      ButtonGroup group = new ButtonGroup();
      group.add(soloButton);
      group.add(aiButton);
      soloButton.setSelected(true);
      modes.add(soloButton);
      modes.add(aiButton);
      content.add(modes);

      JPanel seatPanel = new JPanel(new BorderLayout(12, 0));
      seatPanel.setOpaque(false);
      seatPanel.add(label("YOUR SEAT", 14, GOLD), BorderLayout.WEST);
      seatChoice = new JComboBox<>(new String[] {"Seat 1 (left end)", "Seat 2", "Seat 3 (center)", "Seat 4", "Seat 5 (right end)"});
      seatChoice.setFont(new Font("Segoe UI", Font.PLAIN, 15));
      seatPanel.add(seatChoice, BorderLayout.CENTER);
      content.add(seatPanel);
      seatPanel.setVisible(false);
      aiButton.addActionListener(event -> seatPanel.setVisible(aiButton.isSelected()));
      soloButton.addActionListener(event -> seatPanel.setVisible(false));

      JButton startButton = new JButton("START GAME");
      startButton.setActionCommand("start");
      startButton.addActionListener(this);
      startButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
      startButton.setForeground(Color.WHITE);
      startButton.setBackground(new Color(195, 145, 35));
      startButton.setFocusPainted(false);
      startButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
      startButton.setBorder(BorderFactory.createEmptyBorder(13, 20, 13, 20));
      content.add(startButton);

      add(content, BorderLayout.CENTER);
      getRootPane().setDefaultButton(startButton);
   }

   private JRadioButton modeButton(String title, String description) {
      JRadioButton button = new JRadioButton("<html><b>" + title + "</b><br><small>" + description + "</small></html>");
      button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
      button.setForeground(Color.WHITE);
      button.setBackground(PANEL);
      button.setFocusPainted(false);
      button.setOpaque(true);
      button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 145, 105), 1),
            BorderFactory.createEmptyBorder(14, 12, 14, 12)));
      button.setMargin(new Insets(0, 0, 0, 0));
      return button;
   }

   private JLabel label(String text, int size, Color color) {
      JLabel label = new JLabel(text);
      label.setFont(new Font("Segoe UI", Font.BOLD, size));
      label.setForeground(color);
      return label;
   }

   @Override
   public void actionPerformed(ActionEvent event) {
      if (!"start".equals(event.getActionCommand())) {
         return;
      }
      String name = nameField.getText().trim();
      if (name.isEmpty()) {
         JOptionPane.showMessageDialog(this, "Please enter your name.", "Missing Name", JOptionPane.WARNING_MESSAGE);
         nameField.requestFocusInWindow();
         return;
      }

      String mode = soloButton.isSelected() ? "Solo Table" : "Vs AI Table";
      int seat = seatChoice.getSelectedIndex();
      GameWindow window = new GameWindow(name, mode, seat);
      window.setVisible(true);
      dispose();
      window.showHowToPlay();
   }
}
