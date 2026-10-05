import java.sql.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Cashier GUI for team 3 project (Panda Express style POS).
 *
 * Layout follows the cashier GUI sketch:
 *   - Top nav bar:  MENU | INV. | REP.  (INV./REP. are manager only)
 *   - Left:  category buttons (Sides, Entrees, Appetizers, Cub Meals, Drinks, Extras)
 *            whose item buttons are pulled live from the MenuItem table.
 *   - Right: running order ticket (qty, name, price), Subtotal / Tax / Total,
 *            and a red Checkout button.
 *
 * On Checkout (single transaction):
 *   1. INSERT INTO Orders (OrderID, OrderDate, OrderTime, Total)
 *   2. INSERT INTO OrderItem for each ticket line
 *   3. UPDATE Inventory ... FROM MenuItemInventory to decrement stock used
 * Items with a MenuItemInventory mapping are checked for availability before
 * they can be added to the ticket.
 */
public class CashierGUI {

  static Connection conn = null;

  static final double TAX_RATE = 0.0825;
  static final Color RED = new Color(0xC8, 0x3A, 0x2E);
  static final Color GREY = new Color(0xC7, 0xCE, 0xD6);

  // category name -> [min MenuItemID, max MenuItemID] in the MenuItem table
  static final LinkedHashMap<String, int[]> CATEGORIES = new LinkedHashMap<String, int[]>();
  static {
    CATEGORIES.put("Sides",      new int[] {1, 5});
    CATEGORIES.put("Entrees",    new int[] {6, 25});
    CATEGORIES.put("Appetizers", new int[] {26, 29});
    CATEGORIES.put("Cub Meals",  new int[] {30, 32});
    CATEGORIES.put("Extras",     new int[] {33, 43});
    CATEGORIES.put("Drinks",     new int[] {44, 154});
  }

  JFrame frame;
  JPanel itemsPanel;
  DefaultListModel<String> ticketModel;
  JLabel headerTotal, subtotalLabel, taxLabel, totalLabel;

  // MenuItemID -> [name, price, qty]
  LinkedHashMap<Integer, Object[]> orderLines = new LinkedHashMap<Integer, Object[]>();

  public static void main(String args[]) {
    openConnection(new dbSetup());
    SwingUtilities.invokeLater(new Runnable() {
      public void run() { new CashierGUI().build(); }
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

  void build() {
    frame = new JFrame("Cashier GUI");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(1100, 650);
    frame.setLocationRelativeTo(null);

    JPanel root = new JPanel(new BorderLayout(5, 5));
    root.setBorder(new EmptyBorder(5, 5, 5, 5));
    root.add(buildNavBar(), BorderLayout.NORTH);
    root.add(buildMenuArea(), BorderLayout.CENTER);
    root.add(buildTicket(), BorderLayout.EAST);

    frame.add(root);
    frame.setVisible(true);
    showCategory("Sides");
  }

  // ---- Top nav bar: MENU | INV. | REP. ----
  JPanel buildNavBar() {
    JPanel nav = new JPanel(new GridLayout(1, 3, 3, 3));
    JButton menu = navButton("MENU", true);
    JButton inv  = navButton("INV.", false);
    JButton rep  = navButton("REP.", false);

    inv.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(frame, "Inventory view is manager only.");
      }
    });
    rep.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(frame, "Reports view is manager only.");
      }
    });

    nav.add(menu); nav.add(inv); nav.add(rep);
    return nav;
  }

  JButton navButton(String text, boolean active) {
    JButton b = new JButton(text);
    b.setFont(new Font("SansSerif", Font.BOLD, 18));
    b.setBackground(active ? RED : GREY);
    b.setForeground(active ? Color.WHITE : Color.BLACK);
    b.setOpaque(true);
    b.setBorderPainted(false);
    return b;
  }

  // ---- Left: category row + scrollable grid of item buttons ----
  JPanel buildMenuArea() {
    JPanel area = new JPanel(new BorderLayout(5, 5));

    JPanel cats = new JPanel(new GridLayout(1, CATEGORIES.size(), 3, 3));
    for (String cat : CATEGORIES.keySet()) {
      JButton b = new JButton(cat);
      b.setFont(new Font("SansSerif", Font.BOLD, 14));
      b.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) { showCategory(cat); }
      });
      cats.add(b);
    }

    itemsPanel = new JPanel(new GridLayout(0, 4, 5, 5));
    JScrollPane scroll = new JScrollPane(itemsPanel);
    scroll.getVerticalScrollBar().setUnitIncrement(16);

    area.add(cats, BorderLayout.NORTH);
    area.add(scroll, BorderLayout.CENTER);
    return area;
  }

  void showCategory(String category) {
    itemsPanel.removeAll();
    int[] range = CATEGORIES.get(category);
    try {
      PreparedStatement ps = conn.prepareStatement(
        "SELECT MenuItemID, MenuItemName, Price FROM MenuItem " +
        "WHERE MenuItemID BETWEEN ? AND ? ORDER BY MenuItemID");
      ps.setInt(1, range[0]);
      ps.setInt(2, range[1]);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        final int id = rs.getInt("MenuItemID");
        final String name = rs.getString("MenuItemName");
        final double price = rs.getDouble("Price");
        JButton b = new JButton("<html><center>" + name + "<br>$" +
          String.format("%.2f", price) + "</center></html>");
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setBackground(GREY);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(150, 70));
        b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { addItem(id, name, price); }
        });
        itemsPanel.add(b);
      }
      rs.close(); ps.close();
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(frame, "Error loading menu: " + ex.getMessage());
    }
    itemsPanel.revalidate();
    itemsPanel.repaint();
  }

  // ---- Right: order ticket ----
  JPanel buildTicket() {
    JPanel ticket = new JPanel(new BorderLayout(5, 5));
    ticket.setPreferredSize(new Dimension(280, 0));
    ticket.setBackground(GREY);
    ticket.setBorder(new EmptyBorder(5, 5, 5, 5));

    headerTotal = new JLabel("Total: $0.00");
    headerTotal.setFont(new Font("SansSerif", Font.BOLD, 20));
    ticket.add(headerTotal, BorderLayout.NORTH);

    ticketModel = new DefaultListModel<String>();
    JList<String> list = new JList<String>(ticketModel);
    list.setFont(new Font("Monospaced", Font.PLAIN, 13));
    ticket.add(new JScrollPane(list), BorderLayout.CENTER);

    JPanel bottom = new JPanel();
    bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
    bottom.setOpaque(false);

    JButton remove = new JButton("Remove Selected");
    remove.setAlignmentX(Component.CENTER_ALIGNMENT);
    remove.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int idx = list.getSelectedIndex();
        if (idx >= 0) {
          int id = (Integer) orderLines.keySet().toArray()[idx];
          Object[] line = orderLines.get(id);
          int qty = (Integer) line[2];
          if (qty > 1) line[2] = qty - 1; else orderLines.remove(id);
          refreshTicket();
        }
      }
    });

    JPanel sums = new JPanel(new GridLayout(3, 1));
    sums.setOpaque(false);
    subtotalLabel = new JLabel("Subtotal: $0.00");
    taxLabel      = new JLabel("Tax: $0.00");
    totalLabel    = new JLabel("Total: $0.00");
    totalLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
    sums.add(subtotalLabel); sums.add(taxLabel); sums.add(totalLabel);

    JButton checkout = new JButton("Checkout");
    checkout.setAlignmentX(Component.CENTER_ALIGNMENT);
    checkout.setFont(new Font("SansSerif", Font.BOLD, 18));
    checkout.setBackground(RED);
    checkout.setForeground(Color.WHITE);
    checkout.setOpaque(true);
    checkout.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { checkout(); }
    });

    bottom.add(remove);
    bottom.add(Box.createVerticalStrut(5));
    bottom.add(sums);
    bottom.add(Box.createVerticalStrut(5));
    bottom.add(checkout);
    ticket.add(bottom, BorderLayout.SOUTH);
    return ticket;
  }

  void addItem(int id, String name, double price) {
    if (!isAvailable(id)) {
      JOptionPane.showMessageDialog(frame, name + " is out of stock.");
      return;
    }
    Object[] line = orderLines.get(id);
    if (line == null) orderLines.put(id, new Object[] {name, price, 1});
    else line[2] = (Integer) line[2] + 1;
    refreshTicket();
  }

  // An item is available if it has no inventory mapping, or every mapped
  // ingredient still has stock (contract: check inventory before selling).
  boolean isAvailable(int menuItemId) {
    try {
      PreparedStatement ps = conn.prepareStatement(
        "SELECT COUNT(*) AS needed, " +
        "COUNT(*) FILTER (WHERE i.Quantity >= mii.QuantityRequired) AS stocked " +
        "FROM MenuItemInventory mii " +
        "JOIN Inventory i ON i.InventoryID = mii.InventoryID " +
        "WHERE mii.MenuItemID = ?");
      ps.setInt(1, menuItemId);
      ResultSet rs = ps.executeQuery();
      rs.next();
      boolean ok = rs.getInt("needed") == 0 ||
                   rs.getInt("needed") == rs.getInt("stocked");
      rs.close(); ps.close();
      return ok;
    } catch (SQLException ex) {
      return true; // don't block sales on a lookup failure
    }
  }

  double subtotal() {
    double sum = 0;
    for (Object[] line : orderLines.values())
      sum += (Double) line[1] * (Integer) line[2];
    return sum;
  }

  void refreshTicket() {
    ticketModel.clear();
    for (Object[] line : orderLines.values()) {
      String name = (String) line[0];
      if (name.length() > 24) name = name.substring(0, 24);
      ticketModel.addElement(String.format("%dx %-24s $%.2f",
        (Integer) line[2], name, (Double) line[1] * (Integer) line[2]));
    }
    double sub = subtotal(), tax = sub * TAX_RATE, tot = sub + tax;
    headerTotal.setText(String.format("Total: $%.2f", tot));
    subtotalLabel.setText(String.format("Subtotal: $%.2f", sub));
    taxLabel.setText(String.format("Tax: $%.2f", tax));
    totalLabel.setText(String.format("Total: $%.2f", tot));
  }

  // ---- Checkout: write the order to the database in one transaction ----
  void checkout() {
    if (orderLines.isEmpty()) {
      JOptionPane.showMessageDialog(frame, "The order is empty.");
      return;
    }
    double sub = subtotal(), tot = sub * (1 + TAX_RATE);
    int confirm = JOptionPane.showConfirmDialog(frame,
      String.format("Charge $%.2f?", tot), "Checkout", JOptionPane.YES_NO_OPTION);
    if (confirm != JOptionPane.YES_OPTION) return;

    try {
      conn.setAutoCommit(false);

      Statement st = conn.createStatement();
      ResultSet rs = st.executeQuery(
        "SELECT (SELECT COALESCE(MAX(OrderID),0)+1 FROM Orders) AS oid, " +
        "       (SELECT COALESCE(MAX(OrderItemID),0) FROM OrderItem) AS oiid");
      rs.next();
      int orderId = rs.getInt("oid");
      int nextOrderItemId = rs.getInt("oiid");
      rs.close(); st.close();

      PreparedStatement insOrder = conn.prepareStatement(
        "INSERT INTO Orders (OrderID, OrderDate, OrderTime, Total) " +
        "VALUES (?, CURRENT_DATE, CURRENT_TIME, ?)");
      insOrder.setInt(1, orderId);
      insOrder.setDouble(2, tot);
      insOrder.executeUpdate();

      PreparedStatement insItem = conn.prepareStatement(
        "INSERT INTO OrderItem (OrderItemID, OrderID, MenuItemID, Qty) " +
        "VALUES (?, ?, ?, ?)");
      PreparedStatement decInv = conn.prepareStatement(
        "UPDATE Inventory i SET Quantity = i.Quantity - mii.QuantityRequired * ? " +
        "FROM MenuItemInventory mii " +
        "WHERE mii.MenuItemID = ? AND i.InventoryID = mii.InventoryID");
      for (Map.Entry<Integer, Object[]> e : orderLines.entrySet()) {
        int menuItemId = e.getKey();
        int qty = (Integer) e.getValue()[2];
        insItem.setInt(1, ++nextOrderItemId);
        insItem.setInt(2, orderId);
        insItem.setInt(3, menuItemId);
        insItem.setInt(4, qty);
        insItem.executeUpdate();
        decInv.setInt(1, qty);
        decInv.setInt(2, menuItemId);
        decInv.executeUpdate();
      }

      conn.commit();
      JOptionPane.showMessageDialog(frame,
        String.format("Order #%d submitted. Total: $%.2f\nThank you, have a nice day!",
          orderId, tot));
      orderLines.clear();
      refreshTicket();
    } catch (SQLException ex) {
      try { conn.rollback(); } catch (SQLException ignored) {}
      JOptionPane.showMessageDialog(frame, "Order failed: " + ex.getMessage());
    } finally {
      try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
    }
  }

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() { closeConnection(); }
    }));
  }
}
