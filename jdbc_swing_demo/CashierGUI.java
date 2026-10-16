import java.sql.*;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
 *
 * Styling comes from PandaTheme so the cashier side matches ManagerGUI.
 */
public class CashierGUI {

  static Connection conn = null;

  static final double TAX_RATE = 0.0825;

  // 4-digit PIN required to switch from the cashier side to the manager side
  static final String MANAGER_PIN = "1234";

  // Size suffixes used to group drinks ("NAME - SIZE"); MEDDIUM is a typo in the data
  static final String[] SIZES = {"KIDS", "SMALL", "MEDIUM", "MEDDIUM", "LARGE"};

  // List of menu item categories.
  static final String[] CATEGORIES = {"Sides", "Entrees", "Appetizers", "Meals", "Extras", "Drinks"};

  JFrame frame;
  JPanel itemsPanel;
  LinkedHashMap<String, JButton> catButtons = new LinkedHashMap<String, JButton>();
  DefaultListModel<String> ticketModel;
  JLabel headerTotal, subtotalLabel, taxLabel, totalLabel;

  // MenuItemID -> [name, price, qty]
  LinkedHashMap<Integer, Object[]> orderLines = new LinkedHashMap<Integer, Object[]>();

  public static void main(String args[]) {
    openConnection(new dbSetup());
    PandaTheme.useSystemLook();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() { new CashierGUI().build(); }
    });
  }

  // Opens the shared database connection, or exits if it fails
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

  // Closes the shared database connection, ignoring any exceptions
  static void closeConnection() {
    try { conn.close(); } catch (Exception e) { /* ignore */ }
  }

  // Builds the main GUI layout for the cashier interface
  void build() {
    frame = new JFrame("Panda Express - Cashier");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(1100, 650);
    frame.setLocationRelativeTo(null);

    JPanel root = new JPanel(new BorderLayout());
    root.setBackground(PandaTheme.WHITE);
    root.add(buildNavBar(), BorderLayout.NORTH);

    JPanel content = new JPanel(new BorderLayout(12, 0));
    content.setOpaque(false);
    content.setBorder(new EmptyBorder(16, 20, 16, 20));
    content.add(buildMenuArea(), BorderLayout.CENTER);
    content.add(buildTicket(), BorderLayout.EAST);
    root.add(content, BorderLayout.CENTER);

    frame.add(root);
    frame.setVisible(true);
    showCategory("Sides");
  }

  // ---- Top nav bar: MENU | INV. | REP. ----
  JPanel buildNavBar() {
    JPanel nav = new JPanel(new BorderLayout());
    nav.setBackground(PandaTheme.GREY);

    JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    tabs.setOpaque(false);

    JButton menu = PandaTheme.primaryButton("MENU");
    menu.setBorder(new EmptyBorder(10, 22, 10, 22));
    JButton inv = PandaTheme.secondaryButton("INV.");
    inv.setBorder(new EmptyBorder(10, 22, 10, 22));
    JButton rep = PandaTheme.secondaryButton("REP.");
    rep.setBorder(new EmptyBorder(10, 22, 10, 22));

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

    tabs.add(menu); tabs.add(inv); tabs.add(rep);
    nav.add(tabs, BorderLayout.WEST);

    JButton switchMode = PandaTheme.button("Switch Mode",
      PandaTheme.WHITE, PandaTheme.BLACK, PandaTheme.GREY_LIGHT);
    switchMode.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { switchToManager(); }
    });
    JPanel right = new JPanel(new GridBagLayout());
    right.setOpaque(false);
    right.setBorder(new EmptyBorder(0, 0, 0, 12));
    right.add(switchMode);
    nav.add(right, BorderLayout.EAST);
    return nav;
  }

  // Ask for the manager PIN, then open the manager side. Wrong PIN stays here.
  void switchToManager() {
    if (!orderLines.isEmpty() && JOptionPane.showConfirmDialog(frame,
        "The current order will be discarded. Switch to manager mode?", "Switch Mode",
        JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
      return;
    }
    JPasswordField pin = new JPasswordField(10);
    pin.setFont(PandaTheme.BODY);
    int choice = JOptionPane.showConfirmDialog(frame, pin, "Enter Manager PIN",
      JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (choice != JOptionPane.OK_OPTION) return;
    if (!MANAGER_PIN.equals(new String(pin.getPassword()))) {
      JOptionPane.showMessageDialog(frame, "Incorrect PIN.", "Manager PIN",
        JOptionPane.ERROR_MESSAGE);
      return;
    }
    frame.dispose();
    PandaTheme.useSystemLook();
    new ManagerGUI(conn).build(conn);
  }

  // ---- Left: category row + scrollable grid of item buttons ----
  JPanel buildMenuArea() {
    JPanel area = new JPanel(new BorderLayout(0, 12));
    area.setOpaque(false);

    JPanel cats = new JPanel(new GridLayout(1, CATEGORIES.length, 6, 0));
    cats.setOpaque(false);
    for (final String cat : CATEGORIES) {
      JButton b = PandaTheme.secondaryButton(cat);
      b.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) { showCategory(cat); }
      });
      catButtons.put(cat, b);
      cats.add(b);
    }

    itemsPanel = new JPanel(new GridLayout(0, 4, 8, 8));
    itemsPanel.setBackground(PandaTheme.WHITE);
    JScrollPane scroll = new JScrollPane(itemsPanel);
    scroll.setBorder(new LineBorder(PandaTheme.GRID, 1));
    scroll.getViewport().setBackground(PandaTheme.WHITE);
    scroll.getVerticalScrollBar().setUnitIncrement(16);

    area.add(cats, BorderLayout.NORTH);
    area.add(scroll, BorderLayout.CENTER);
    return area;
  }

  // Displays the menu items for the specified category
  void showCategory(String category) {
    for (String c : catButtons.keySet()) {
      if (c.equals(category)) {
        PandaTheme.setBase(catButtons.get(c),
          PandaTheme.RED, PandaTheme.WHITE, PandaTheme.RED_DARK);
      } else {
        PandaTheme.setBase(catButtons.get(c),
          PandaTheme.GREY, PandaTheme.BLACK, PandaTheme.GREY_DARK);
      }
    }
    List<Object[]> items = new ArrayList<Object[]>();
    try {
      PreparedStatement ps = conn.prepareStatement(
        "SELECT m.MenuItemID, m.MenuItemName, m.Price " +
        "FROM MenuItem m " +
        "WHERE m.Category = ? " +
        "AND m.Active = TRUE " +
        "AND NOT EXISTS (" +
        "  SELECT 1 " +
        "  FROM MenuItemInventory mii " +
        "  JOIN Inventory i ON i.InventoryID = mii.InventoryID " +
        "  WHERE mii.MenuItemID = m.MenuItemID " +
        "  AND i.Active = FALSE" +
        ") " +
        "ORDER BY m.MenuItemID"
      );

      ps.setString(1, category);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        items.add(new Object[] {rs.getInt("MenuItemID"),
          rs.getString("MenuItemName"), rs.getDouble("Price")});
      }
      rs.close(); ps.close();
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(frame, "Error loading menu: " + ex.getMessage());
    }
    if ("Drinks".equals(category)) showDrinkGrid(items);
    else showItemGrid(items);
  }

  // Flat grid: one button per menu item
  void showItemGrid(List<Object[]> items) {
    itemsPanel.removeAll();
    for (Object[] it : items) {
      itemsPanel.add(itemButton((Integer) it[0], (String) it[1], (Double) it[2],
        (String) it[1]));
    }
    itemsPanel.revalidate();
    itemsPanel.repaint();
  }

  // Drinks: one button per drink name; sizes are picked on the next screen.
  void showDrinkGrid(List<Object[]> items) {
    LinkedHashMap<String, List<Object[]>> drinks =
      new LinkedHashMap<String, List<Object[]>>();
    for (Object[] it : items) {
      String base = baseName((String) it[1]);
      if (!drinks.containsKey(base)) drinks.put(base, new ArrayList<Object[]>());
      drinks.get(base).add(it);
    }
    itemsPanel.removeAll();
    for (Map.Entry<String, List<Object[]>> e : drinks.entrySet()) {
      final String base = e.getKey();
      final List<Object[]> sizes = e.getValue();
      if (sizes.size() == 1) { // no size variants: add it directly
        Object[] it = sizes.get(0);
        itemsPanel.add(itemButton((Integer) it[0], (String) it[1], (Double) it[2],
          (String) it[1]));
      } else {
        JButton b = gridButton(base);
        b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e2) { showSizeGrid(base, sizes); }
        });
        itemsPanel.add(b);
      }
    }
    itemsPanel.revalidate();
    itemsPanel.repaint();
  }

  // Size picker for one drink: back button, then one button per size
  void showSizeGrid(String base, List<Object[]> sizes) {
    List<Object[]> sorted = new ArrayList<Object[]>(sizes);
    Collections.sort(sorted, new Comparator<Object[]>() {
      public int compare(Object[] a, Object[] b) {
        return sizeOrder((String) a[1]) - sizeOrder((String) b[1]);
      }
    });
    itemsPanel.removeAll();
    JButton back = gridButton("&lt; Back");
    back.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { showCategory("Drinks"); }
    });
    itemsPanel.add(back);
    for (Object[] it : sorted) {
      itemsPanel.add(itemButton((Integer) it[0], (String) it[1], (Double) it[2],
        sizeLabel((String) it[1])));
    }
    itemsPanel.revalidate();
    itemsPanel.repaint();
  }

  // Drink name without the size suffix ("COCA-COLA - SMALL" -> "COCA-COLA")
  static String baseName(String name) {
    int i = name.lastIndexOf(" - ");
    if (i < 0) return name;
    String suffix = name.substring(i + 3);
    for (String s : SIZES) {
      if (suffix.equals(s)) return name.substring(0, i);
    }
    return name;
  }

  // Title-cased size for the button label ("... - MEDDIUM" -> "Medium")
  static String sizeLabel(String name) {
    String s = name.substring(name.lastIndexOf(" - ") + 3);
    if (s.equals("MEDDIUM")) s = "MEDIUM";
    return s.charAt(0) + s.substring(1).toLowerCase();
  }

  // Returns the relative order of a size for sorting purposes
  static int sizeOrder(String name) {
    String s = name.substring(name.lastIndexOf(" - ") + 3);
    if (s.equals("KIDS")) return 0;
    if (s.equals("SMALL")) return 1;
    if (s.equals("MEDIUM") || s.equals("MEDDIUM")) return 2;
    if (s.equals("LARGE")) return 3;
    return 4;
  }

  // Creates a grid button with the specified label
  JButton gridButton(String label) {
    JButton b = PandaTheme.secondaryButton("<html><center>" + label + "</center></html>");
    b.setFont(PandaTheme.SMALL);
    b.setBorder(new EmptyBorder(4, 6, 4, 6));
    b.setPreferredSize(new Dimension(150, 70));
    return b;
  }

  // Item button: shows label + price, adds id/name to the ticket on click
  JButton itemButton(final int id, final String name, final double price, String label) {
    JButton b = gridButton(label + "<br>$" + String.format("%.2f", price));
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { addItem(id, name, price); }
    });
    return b;
  }

  // ---- Right: order ticket ----
  JPanel buildTicket() {
    JPanel ticket = new JPanel(new BorderLayout(0, 12));
    ticket.setPreferredSize(new Dimension(280, 0));
    ticket.setBackground(PandaTheme.GREY_LIGHT);
    ticket.setBorder(new CompoundBorder(new LineBorder(PandaTheme.GRID, 1),
      new EmptyBorder(12, 12, 12, 12)));

    headerTotal = PandaTheme.label("Total: $0.00", PandaTheme.HEADING, PandaTheme.BLACK);
    ticket.add(headerTotal, BorderLayout.NORTH);

    ticketModel = new DefaultListModel<String>();
    JList<String> list = new JList<String>(ticketModel);
    list.setFont(new Font("Monospaced", Font.PLAIN, 13));
    list.setBackground(PandaTheme.WHITE);
    JScrollPane listScroll = new JScrollPane(list);
    listScroll.setBorder(new LineBorder(PandaTheme.GRID, 1));
    ticket.add(listScroll, BorderLayout.CENTER);

    JPanel bottom = new JPanel();
    bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
    bottom.setOpaque(false);

    JButton remove = PandaTheme.secondaryButton("Remove Selected");
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
    subtotalLabel = PandaTheme.label("Subtotal: $0.00", PandaTheme.BODY, PandaTheme.BLACK);
    taxLabel      = PandaTheme.label("Tax: $0.00", PandaTheme.BODY, PandaTheme.BLACK);
    totalLabel    = PandaTheme.label("Total: $0.00",
      new Font(PandaTheme.FONT, Font.BOLD, 16), PandaTheme.BLACK);
    sums.add(subtotalLabel); sums.add(taxLabel); sums.add(totalLabel);

    JButton checkout = PandaTheme.primaryButton("Checkout");
    checkout.setAlignmentX(Component.CENTER_ALIGNMENT);
    checkout.setFont(PandaTheme.HEADING);
    checkout.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { checkout(); }
    });

    bottom.add(remove);
    bottom.add(Box.createVerticalStrut(8));
    bottom.add(sums);
    bottom.add(Box.createVerticalStrut(8));
    bottom.add(checkout);
    ticket.add(bottom, BorderLayout.SOUTH);
    return ticket;
  }

  // Adds an item to the order, updating the ticket display
  // If the item is not available, a message dialog is shown
  void addItem(int id, String name, double price) {
    Object[] line = orderLines.get(id);
    int inCart = (line == null) ? 0 : (Integer) line[2];
    if (!isAvailable(id, inCart + 1)) {
      JOptionPane.showMessageDialog(frame, name + " is out of stock.");
      return;
    }
    if (line == null) orderLines.put(id, new Object[] {name, price, 1});
    else line[2] = (Integer) line[2] + 1;
    refreshTicket();
  }

  // An item must be active, and every mapped ingredient must be active
  // with enough stock for the requested quantity. Active unmapped items are available
  boolean isAvailable(int menuItemId, int qtyWanted) {
    try (PreparedStatement ps = conn.prepareStatement(
          "SELECT EXISTS (" +
          "  SELECT 1 FROM MenuItem WHERE MenuItemID = ? AND Active = TRUE" +
          ") AND NOT EXISTS (" +
          "  SELECT 1 FROM MenuItemInventory mii " +
          "  JOIN Inventory i ON i.InventoryID = mii.InventoryID " +
          "  WHERE mii.MenuItemID = ? " +
          "  AND (i.Active = FALSE " +
          "       OR i.Quantity < mii.QuantityRequired * ?)" +
          ") AS ok"
      )) {

      ps.setInt(1, menuItemId);
      ps.setInt(2, menuItemId);
      ps.setInt(3, qtyWanted);
      try (ResultSet rs = ps.executeQuery()){
        rs.next();
        return rs.getBoolean("ok");
      }
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(
          frame,
          "Could not check inventory availability:\n" + ex.getMessage(),
          "Database Error",
          JOptionPane.ERROR_MESSAGE
        );
        return false;
      }
  }

  // Calculates the subtotal of all items in the current order
  double subtotal() {
    double sum = 0;
    for (Object[] line : orderLines.values())
      sum += (Double) line[1] * (Integer) line[2];
    return sum;
  }

  // Refreshes the ticket display, including subtotal, tax, and total
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
        "WHERE mii.MenuItemID = ? " +
        "AND i.InventoryID = mii.InventoryID " +
        "AND i.Active = TRUE");
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

      try(Statement check = conn.createStatement();
        ResultSet neg = check.executeQuery(
          "SELECT itemname FROM Inventory WHERE Quantity < 0")) {
            if(neg.next()) {
              conn.rollback();
              JOptionPane.showMessageDialog(frame, "Not enough " + neg.getString(1) + " to complete this order.");
              return;
            }
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
