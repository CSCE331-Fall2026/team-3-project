import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/*
 * Shared colors, fonts and widget styling for the manager side
 * (ModeSelect + ManagerGUI).
 *
 * RED and GREY are the same colors CashierGUI uses; the other shades are
 * lighter/darker versions of them for hover, stripes and highlights.
 *
 * Buttons are painted flat (no OS skin) so their colors show the same on
 * every look and feel.
 */
public class PandaTheme {

  // same as CashierGUI
  static final Color RED        = new Color(0xC8, 0x3A, 0x2E);
  static final Color GREY       = new Color(0xC7, 0xCE, 0xD6);
  static final Color WHITE      = Color.WHITE;
  static final Color BLACK      = Color.BLACK;

  // shades of the above
  static final Color RED_DARK   = new Color(0xA1, 0x2E, 0x24);  // red hover
  static final Color RED_LIGHT  = new Color(0xF6, 0xDC, 0xD9);  // unsaved rows
  static final Color GREY_DARK  = new Color(0xB0, 0xB8, 0xC1);  // grey hover, borders
  static final Color GREY_LIGHT = new Color(0xEE, 0xF1, 0xF4);  // row stripes
  static final Color GRID       = new Color(0xDD, 0xE2, 0xE7);
  static final Color MUTED      = new Color(0x5F, 0x67, 0x70);

  // status
  static final Color OK_GREEN   = new Color(0x2E, 0x7D, 0x32);

  static final String FONT = "Segoe UI";
  static final Font TITLE   = new Font(FONT, Font.BOLD, 22);
  static final Font HEADING = new Font(FONT, Font.BOLD, 18);
  static final Font BODY    = new Font(FONT, Font.PLAIN, 14);
  static final Font BOLD    = new Font(FONT, Font.BOLD, 14);
  static final Font SMALL   = new Font(FONT, Font.PLAIN, 12);

  // Windows/Mac native look for everything not styled by hand (dialogs, scrollbars).
  static void useSystemLook() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) { /* keep the current look */ }
  }

  // Java's default look; CashierGUI was designed against this one.
  static void useJavaLook() {
    try {
      UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (Exception e) { /* keep the current look */ }
  }

  // Flat colored button that darkens on hover.
  static JButton button(String text, Color bg, Color fg, Color hover) {
    // fill the background ourselves; the Windows look ignores setBackground on buttons
    final JButton b = new JButton(text) {
      protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
      }
    };
    b.setFont(BOLD);
    b.setForeground(fg);
    b.setBackground(bg);
    b.setContentAreaFilled(false);
    b.setFocusPainted(false);
    b.setBorderPainted(false);
    b.setBorder(new EmptyBorder(8, 18, 8, 18));
    b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    b.putClientProperty("baseColor", bg);
    b.putClientProperty("hoverColor", hover);
    b.addMouseListener(new MouseAdapter() {
      public void mouseEntered(MouseEvent e) {
        b.setBackground((Color) b.getClientProperty("hoverColor"));
      }
      public void mouseExited(MouseEvent e) {
        b.setBackground((Color) b.getClientProperty("baseColor"));
      }
    });
    return b;
  }

  // Red with white text (like the cashier's Checkout button).
  static JButton primaryButton(String text) {
    return button(text, RED, WHITE, RED_DARK);
  }

  // Grey with black text (like the cashier's menu item buttons).
  static JButton secondaryButton(String text) {
    return button(text, GREY, BLACK, GREY_DARK);
  }

  // Changes a button's resting color (used for the selected nav tab).
  static void setBase(JButton b, Color bg, Color fg, Color hover) {
    b.putClientProperty("baseColor", bg);
    b.putClientProperty("hoverColor", hover);
    b.setBackground(bg);
    b.setForeground(fg);
  }

  static JLabel label(String text, Font font, Color color) {
    JLabel l = new JLabel(text);
    l.setFont(font);
    l.setForeground(color);
    return l;
  }

  // Text field with a thin grey outline and some inner padding.
  static JTextField textField(int columns) {
    JTextField f = new JTextField(columns);
    f.setFont(BODY);
    f.setForeground(BLACK);
    f.setBackground(WHITE);
    f.setBorder(new CompoundBorder(new LineBorder(GREY_DARK, 1), new EmptyBorder(5, 8, 5, 8)));
    return f;
  }
}
