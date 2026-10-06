import java.sql.*;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/*
 * Manager GUI for team 3 project (view side of MVC; DB calls live in InventoryDAO).
 *
 * Tabs:
 *   - Inventory: table of Item / Amount / Unit / Status. Amount is editable in place;
 *                edited rows are marked "*" until Save commits them all in one batch.
 *                "+ add item" opens a form for a new Inventory row.
 *                Reloads from the database on open, after Save/Add, and on Refresh.
 *   - Reports:   placeholder (Phase 4).
 *   - Schedule:  placeholder (no employee/shift tables in the schema).
 *
 * Status is not stored: an item is "LOW" when Quantity <= Minimum_Threshold.
 */
public class ManagerGUI {

  static Connection conn = null;

  JFrame frame;
  InventoryDAO dao;
  InventoryTableModel model;

  public static void main(String args[]) {
    openConnection(new dbSetup());
    // use the operating system's normal look instead of Java's blue "Metal" theme
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) { /* fall back to the default look */ }
    SwingUtilities.invokeLater(new Runnable() {
      public void run() { new ManagerGUI(conn).build(); }
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

  public ManagerGUI(Connection c) {
    dao = new InventoryDAO(c);
  }

  void build() {
    frame = new JFrame("Manager GUI");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(800, 600);
    frame.setLocationRelativeTo(null);

    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Inventory", buildInventoryPanel());
    tabs.addTab("Reports", new JLabel("Reports"));
    tabs.addTab("Schedule", new JLabel("Employee schedule"));

    frame.add(tabs);
    frame.setVisible(true);
    reload();
  }

  // ---- Inventory tab ----
  JPanel buildInventoryPanel() {
    JPanel panel = new JPanel(new BorderLayout());

    JPanel top = new JPanel(new BorderLayout());
    top.add(new JLabel("Inventory"), BorderLayout.WEST);
    JButton add = new JButton("+ add item");
    add.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { showAddDialog(); }
    });
    top.add(add, BorderLayout.EAST);

    model = new InventoryTableModel();
    JTable table = new JTable(model);
    table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton refresh = new JButton("Refresh");
    refresh.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (model.pending.isEmpty() || JOptionPane.showConfirmDialog(frame,
            "Discard unsaved changes?", "Refresh", JOptionPane.YES_NO_OPTION)
            == JOptionPane.YES_OPTION) {
          reload();
        }
      }
    });
    JButton save = new JButton("Save");
    save.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        save();
      }
    });
    bottom.add(refresh);
    bottom.add(save);

    panel.add(top, BorderLayout.NORTH);
    panel.add(new JScrollPane(table), BorderLayout.CENTER);
    panel.add(bottom, BorderLayout.SOUTH);
    return panel;
  }

  void reload() {
    try {
      model.setItems(dao.fetchAll());
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(frame, "Error loading inventory: " + ex.getMessage());
    }
  }

  void save() {
    if (model.pending.isEmpty()) {
      JOptionPane.showMessageDialog(frame, "No changes to save.");
      return;
    }
    try {
      int n = model.pending.size();
      dao.updateQuantities(model.pending);
      reload();
      JOptionPane.showMessageDialog(frame, "Saved " + n + " item(s).");
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(frame, "Save failed: " + ex.getMessage());
    }
  }

  // ---- "+ add item" dialog ----
  void showAddDialog() {
    JTextField name = new JTextField();
    JTextField unit = new JTextField();
    JTextField threshold = new JTextField();
    JTextField quantity = new JTextField();
    JTextField shelfLife = new JTextField();

    JPanel form = new JPanel(new GridLayout(0, 2));
    form.add(new JLabel("Item name:"));        form.add(name);
    form.add(new JLabel("Unit:"));             form.add(unit);
    form.add(new JLabel("Minimum threshold:")); form.add(threshold);
    form.add(new JLabel("Quantity:"));         form.add(quantity);
    form.add(new JLabel("Shelf life (days):")); form.add(shelfLife);

    // keep re-showing the form until it is valid or cancelled
    while (true) {
      int choice = JOptionPane.showConfirmDialog(frame, form, "Add Inventory Item",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
      if (choice != JOptionPane.OK_OPTION) return;

      String err = null;
      String n = name.getText().trim(), u = unit.getText().trim();
      Double t = parseNonNegative(threshold.getText());
      Double q = parseNonNegative(quantity.getText());
      Integer s = null;
      try {
        s = Integer.parseInt(shelfLife.getText().trim());
        if (s < 0) s = null;
      } catch (NumberFormatException ignored) {}

      if (n.isEmpty()) err = "Item name is required.";
      else if (n.length() > 100) err = "Item name is too long (max 100).";
      else if (u.isEmpty()) err = "Unit is required.";
      else if (u.length() > 20) err = "Unit is too long (max 20).";
      else if (t == null) err = "Minimum threshold must be a non-negative number.";
      else if (q == null) err = "Quantity must be a non-negative number.";
      else if (s == null) err = "Shelf life must be a non-negative whole number.";

      if (err != null) {
        JOptionPane.showMessageDialog(frame, err);
        continue;
      }

      try {
        dao.insertItem(n, u, t, q, s);
        reload();
      } catch (SQLException ex) {
        JOptionPane.showMessageDialog(frame, "Add failed: " + ex.getMessage());
      }
      return;
    }
  }

  static Double parseNonNegative(String text) {
    try {
      double d = Double.parseDouble(text.trim());
      if (d < 0 || Double.isNaN(d) || Double.isInfinite(d)) return null;
      return d;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  // ---- Table model: Item | Amount | Unit | Status | Unsaved ----
  class InventoryTableModel extends AbstractTableModel {
    final String[] cols = {"Item", "Amount", "Unit", "Status", "Unsaved"};
    List<InventoryDAO.Item> items = new ArrayList<InventoryDAO.Item>();
    // InventoryID -> edited quantity not yet saved
    LinkedHashMap<Integer, Double> pending = new LinkedHashMap<Integer, Double>();

    void setItems(List<InventoryDAO.Item> newItems) {
      items = newItems;
      pending.clear();
      fireTableDataChanged();
    }

    public int getRowCount() { return items.size(); }
    public int getColumnCount() { return cols.length; }
    public String getColumnName(int c) { return cols[c]; }

    public Class<?> getColumnClass(int c) {
      return c == 1 ? Double.class : String.class;
    }

    public boolean isCellEditable(int r, int c) { return c == 1; }

    public Object getValueAt(int r, int c) {
      InventoryDAO.Item it = items.get(r);
      switch (c) {
        case 0: return it.name;
        case 1: return pending.containsKey(it.id) ? pending.get(it.id) : it.quantity;
        case 2: return it.unit;
        case 3: return it.isLow() ? "LOW" : "ok";  // based on saved quantity
        case 4: return pending.containsKey(it.id) ? "*" : "";
      }
      return null;
    }

    public void setValueAt(Object value, int r, int c) {
      if (c != 1 || value == null) return;
      double q = (Double) value;
      if (q < 0 || Double.isNaN(q) || Double.isInfinite(q)) {
        JOptionPane.showMessageDialog(frame, "Quantity must be a non-negative number.");
        return;
      }
      InventoryDAO.Item it = items.get(r);
      if (q == it.quantity) pending.remove(it.id);
      else pending.put(it.id, q);
      fireTableRowsUpdated(r, r);
    }
  }

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() { closeConnection(); }
    }));
  }
}
