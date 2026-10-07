import java.sql.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/*
 * Entry screen: choose Cashier or Manager mode.
 *
 * Opens one database connection and hands it to whichever GUI is picked.
 * ManagerGUI has a "Switch Mode" button that comes back here;
 * CashierGUI does not (cashier side is handled separately).
 */
public class ModeSelect {

  static Connection conn = null;

  JFrame frame;
  final Connection db;

  public static void main(String args[]) {
    openConnection(new dbSetup());
    PandaTheme.useSystemLook();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() { new ModeSelect(conn).build(); }
    });
  }

  static void openConnection(dbSetup credentials) {
    try {
      conn = DriverManager.getConnection(
        "jdbc:postgresql://csce-315-db.engr.tamu.edu/team3db",
        credentials.user, credentials.pswd);
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      System.exit(0);
    }
  }

  static void closeConnection() {
    try { conn.close(); } catch (Exception e) { /* ignore */ }
  }

  public ModeSelect(Connection c) {
    db = c;
  }

  void build() {
    frame = new JFrame("Panda Express - Select Mode");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(460, 320);
    frame.setLocationRelativeTo(null);

    JPanel root = new JPanel(new BorderLayout());
    root.setBackground(PandaTheme.WHITE);

    // red header
    JPanel header = new JPanel();
    header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
    header.setBackground(PandaTheme.RED);
    header.setBorder(new EmptyBorder(18, 24, 18, 24));
    JLabel title = PandaTheme.label("PANDA EXPRESS", PandaTheme.TITLE, PandaTheme.WHITE);
    JLabel sub = PandaTheme.label("Point of Sale", PandaTheme.BODY, PandaTheme.WHITE);
    header.add(title);
    header.add(sub);

    // mode buttons
    JPanel body = new JPanel(new GridBagLayout());
    body.setBackground(PandaTheme.WHITE);
    body.setBorder(new EmptyBorder(16, 24, 24, 24));
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 1;
    c.insets = new Insets(6, 0, 6, 0);

    body.add(PandaTheme.label("Select a mode", PandaTheme.HEADING, PandaTheme.BLACK), c);

    JButton cashier = PandaTheme.primaryButton("Cashier");
    cashier.setFont(PandaTheme.HEADING);
    cashier.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { openCashier(); }
    });
    body.add(cashier, c);

    JButton manager = PandaTheme.secondaryButton("Manager");
    manager.setFont(PandaTheme.HEADING);
    manager.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { openManager(); }
    });
    body.add(manager, c);

    root.add(header, BorderLayout.NORTH);
    root.add(body, BorderLayout.CENTER);
    frame.add(root);
    frame.setVisible(true);
  }

  void openCashier() {
    frame.dispose();
    PandaTheme.useJavaLook();
    CashierGUI.conn = db;
    new CashierGUI().build();
  }

  void openManager() {
    frame.dispose();
    new ManagerGUI(db).build();
  }

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() { closeConnection(); }
    }));
  }
}
