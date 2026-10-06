import java.sql.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/*
 * Manager GUI for team 3 project (DB calls live in InventoryDAO).
 *
 * Layout:
 *   - Nav bar: Inventory | Reports | Schedule (CardLayout), and "Switch Mode"
 *   - Inventory: table of Item / Amount / Unit / Status. Amount is editable in place;
 *                edited rows are highlighted until Save commits them all in one batch.
 *                "+ Add Item" opens a form for a new Inventory row.
 *                Reloads from the database on open, after Save/Add, and on Refresh.
 *   - Reports:   placeholder (Phase 4).
 *   - Schedule:  placeholder.
 *
 * Status is not stored: an item is "LOW" when Quantity <= Minimum_Threshold.
 */
public class ManagerGUI {

  static Connection conn = null;

  static final String[] PAGES = {"Inventory", "Reports", "Schedule"};

  JFrame frame;
  final Connection db;
  InventoryDAO dao;
  InventoryTableModel model;
  JTable table;
  JLabel unsavedLabel;
  CardLayout cards;
  JPanel pages;
  LinkedHashMap<String, JButton> navButtons = new LinkedHashMap<String, JButton>();

  public static void main(String args[]) {
    openConnection(new dbSetup());
    PandaTheme.useSystemLook();
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
    db = c;
    dao = new InventoryDAO(c);
  }

  void build() {
    frame = new JFrame("Panda Express - Manager");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(900, 640);
    frame.setLocationRelativeTo(null);

    cards = new CardLayout();
    pages = new JPanel(cards);
    pages.add(buildInventoryPanel(), "Inventory");
    pages.add(placeholder("Reports"), "Reports");
    pages.add(placeholder("Employee schedule"), "Schedule");

    JPanel root = new JPanel(new BorderLayout());
    root.setBackground(PandaTheme.WHITE);
    root.add(buildNavBar(), BorderLayout.NORTH);
    root.add(pages, BorderLayout.CENTER);

    frame.add(root);
    showPage("Inventory");
    frame.setVisible(true);
    reload();
  }

  // nav bar: Inventory | Reports | Schedule ... Switch Mode 
  JPanel buildNavBar() {
    JPanel nav = new JPanel(new BorderLayout());
    nav.setBackground(PandaTheme.GREY);

    JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    tabs.setOpaque(false);
    for (final String name : PAGES) {
      JButton b = PandaTheme.secondaryButton(name);
      b.setBorder(new EmptyBorder(10, 22, 10, 22));
      b.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) { showPage(name); }
      });
      navButtons.put(name, b);
      tabs.add(b);
    }
    nav.add(tabs, BorderLayout.WEST);

    JButton switchMode = PandaTheme.button("Switch Mode",
      PandaTheme.WHITE, PandaTheme.BLACK, PandaTheme.GREY_LIGHT);
    switchMode.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { switchMode(); }
    });
    JPanel right = new JPanel(new GridBagLayout());
    right.setOpaque(false);
    right.setBorder(new EmptyBorder(0, 0, 0, 12));
    right.add(switchMode);
    nav.add(right, BorderLayout.EAST);
    return nav;
  }

  void showPage(String name) {
    cards.show(pages, name);
    for (String n : navButtons.keySet()) {
      if (n.equals(name)) {
        PandaTheme.setBase(navButtons.get(n),
          PandaTheme.RED, PandaTheme.WHITE, PandaTheme.RED);
      } else {
        PandaTheme.setBase(navButtons.get(n),
          PandaTheme.GREY, PandaTheme.BLACK, PandaTheme.GREY_DARK);
      }
    }
  }

  void switchMode() {
    if (table.isEditing()) table.getCellEditor().stopCellEditing();
    if (!model.pending.isEmpty() && JOptionPane.showConfirmDialog(frame,
        "You have unsaved changes. Discard them and switch mode?", "Switch Mode",
        JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
      return;
    }
    frame.dispose();
    new ModeSelect(db).build();
  }

  JPanel placeholder(String title) {
    JPanel p = new JPanel(new BorderLayout());
    p.setBackground(PandaTheme.WHITE);
    p.setBorder(new EmptyBorder(16, 20, 16, 20));
    p.add(PandaTheme.label(title, PandaTheme.HEADING, PandaTheme.BLACK), BorderLayout.NORTH);
    return p;
  }

  // ---- Inventory page ----
  JPanel buildInventoryPanel() {
    JPanel panel = new JPanel(new BorderLayout(0, 12));
    panel.setBackground(PandaTheme.WHITE);
    panel.setBorder(new EmptyBorder(16, 20, 16, 20));

    JPanel top = new JPanel(new BorderLayout());
    top.setOpaque(false);
    top.add(PandaTheme.label("Inventory", PandaTheme.HEADING, PandaTheme.BLACK),
      BorderLayout.WEST);
    JButton add = PandaTheme.secondaryButton("+ Add Item");
    add.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { showAddDialog(); }
    });
    top.add(add, BorderLayout.EAST);

    model = new InventoryTableModel();
    table = buildTable();

    JScrollPane scroll = new JScrollPane(table);
    scroll.setBorder(new LineBorder(PandaTheme.GRID, 1));
    scroll.getViewport().setBackground(PandaTheme.WHITE);
    scroll.getVerticalScrollBar().setUnitIncrement(16);

    JPanel bottom = new JPanel(new BorderLayout());
    bottom.setOpaque(false);
    unsavedLabel = PandaTheme.label(" ", PandaTheme.SMALL, PandaTheme.MUTED);
    bottom.add(unsavedLabel, BorderLayout.WEST);

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    buttons.setOpaque(false);
    JButton refresh = PandaTheme.secondaryButton("Refresh");
    refresh.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (model.pending.isEmpty() || JOptionPane.showConfirmDialog(frame,
            "Discard unsaved changes?", "Refresh", JOptionPane.YES_NO_OPTION)
            == JOptionPane.YES_OPTION) {
          reload();
        }
      }
    });
    JButton save = PandaTheme.primaryButton("Save Changes");
    save.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        save();
      }
    });
    buttons.add(refresh);
    buttons.add(save);
    bottom.add(buttons, BorderLayout.EAST);

    model.addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent e) { updateUnsavedLabel(); }
    });

    panel.add(top, BorderLayout.NORTH);
    panel.add(scroll, BorderLayout.CENTER);
    panel.add(bottom, BorderLayout.SOUTH);
    return panel;
  }

  // Striped rows, light red highlight on unsaved rows, colored status.
  JTable buildTable() {
    JTable t = new JTable(model) {
      public Component prepareRenderer(TableCellRenderer r, int row, int col) {
        Component c = super.prepareRenderer(r, row, col);
        if (!isRowSelected(row)) {
          if (model.isPending(row)) c.setBackground(PandaTheme.RED_LIGHT);
          else c.setBackground(row % 2 == 0 ? PandaTheme.WHITE : PandaTheme.GREY_LIGHT);
        }
        return c;
      }
    };
    t.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    t.setFont(PandaTheme.BODY);
    t.setRowHeight(28);
    t.setForeground(PandaTheme.BLACK);
    t.setGridColor(PandaTheme.GRID);
    t.setShowVerticalLines(false);
    t.setSelectionBackground(PandaTheme.GREY);
    t.setSelectionForeground(PandaTheme.BLACK);
    t.setFillsViewportHeight(true);

    DefaultTableCellRenderer header = new DefaultTableCellRenderer() {
      public Component getTableCellRendererComponent(JTable tb, Object v,
          boolean sel, boolean focus, int row, int col) {
        super.getTableCellRendererComponent(tb, v, sel, focus, row, col);
        setBackground(PandaTheme.GREY);
        setForeground(PandaTheme.BLACK);
        setFont(PandaTheme.BOLD);
        setHorizontalAlignment(col == 1 ? SwingConstants.RIGHT
          : col == 4 ? SwingConstants.CENTER : SwingConstants.LEFT);
        setBorder(new EmptyBorder(0, 8, 0, col == 1 ? 12 : 8));
        return this;
      }
    };
    t.getTableHeader().setDefaultRenderer(header);
    t.getTableHeader().setReorderingAllowed(false);
    t.getTableHeader().setPreferredSize(new Dimension(0, 32));

    // left padding on text cells
    DefaultTableCellRenderer text = new DefaultTableCellRenderer() {
      public Component getTableCellRendererComponent(JTable tb, Object v,
          boolean sel, boolean focus, int row, int col) {
        super.getTableCellRendererComponent(tb, v, sel, focus, row, col);
        setBorder(new EmptyBorder(0, 8, 0, 8));
        return this;
      }
    };
    t.setDefaultRenderer(String.class, text);

    // amounts right-aligned with two decimals
    DefaultTableCellRenderer amount = new DefaultTableCellRenderer() {
      public Component getTableCellRendererComponent(JTable tb, Object v,
          boolean sel, boolean focus, int row, int col) {
        super.getTableCellRendererComponent(tb, v, sel, focus, row, col);
        if (v instanceof Double) setText(String.format("%.2f", (Double) v));
        setHorizontalAlignment(SwingConstants.RIGHT);
        setBorder(new EmptyBorder(0, 8, 0, 12));
        return this;
      }
    };
    t.getColumnModel().getColumn(1).setCellRenderer(amount);

    // status: red "LOW" / green "OK" with a dot
    DefaultTableCellRenderer status = new DefaultTableCellRenderer() {
      public Component getTableCellRendererComponent(JTable tb, Object v,
          boolean sel, boolean focus, int row, int col) {
        super.getTableCellRendererComponent(tb, v, sel, focus, row, col);
        boolean low = "LOW".equals(v);
        setText(low ? "●  LOW" : "●  OK");
        setForeground(low ? PandaTheme.RED : PandaTheme.OK_GREEN);
        setFont(PandaTheme.BOLD);
        setBorder(new EmptyBorder(0, 8, 0, 8));
        return this;
      }
    };
    t.getColumnModel().getColumn(3).setCellRenderer(status);

    // unsaved marker
    DefaultTableCellRenderer unsaved = new DefaultTableCellRenderer() {
      public Component getTableCellRendererComponent(JTable tb, Object v,
          boolean sel, boolean focus, int row, int col) {
        super.getTableCellRendererComponent(tb, v, sel, focus, row, col);
        setText("*".equals(v) ? "unsaved" : "");
        setForeground(PandaTheme.RED_DARK);
        setFont(PandaTheme.SMALL);
        setHorizontalAlignment(SwingConstants.CENTER);
        return this;
      }
    };
    t.getColumnModel().getColumn(4).setCellRenderer(unsaved);

    t.getColumnModel().getColumn(0).setPreferredWidth(280);
    t.getColumnModel().getColumn(1).setPreferredWidth(110);
    t.getColumnModel().getColumn(2).setPreferredWidth(90);
    t.getColumnModel().getColumn(3).setPreferredWidth(100);
    t.getColumnModel().getColumn(4).setPreferredWidth(90);
    return t;
  }

  void updateUnsavedLabel() {
    int n = model.pending.size();
    if (n == 0) {
      unsavedLabel.setText(model.getRowCount() + " items");
      unsavedLabel.setForeground(PandaTheme.MUTED);
    } else {
      unsavedLabel.setText(n + " unsaved change" + (n == 1 ? "" : "s"));
      unsavedLabel.setForeground(PandaTheme.RED_DARK);
    }
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

  // "+ Add Item" dialog
  void showAddDialog() {
    buildAddDialog().setVisible(true);
  }

  // Styled form for a new Inventory row. Errors show in red under the form;
  JDialog buildAddDialog() {
    final JDialog dialog = new JDialog(frame, "Add Inventory Item", true);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

    final JTextField name = PandaTheme.textField(18);
    final JTextField unit = PandaTheme.textField(18);
    final JTextField threshold = PandaTheme.textField(18);
    final JTextField quantity = PandaTheme.textField(18);
    final JTextField shelfLife = PandaTheme.textField(18);

    JPanel root = new JPanel(new BorderLayout(0, 12));
    root.setBackground(PandaTheme.WHITE);
    root.setBorder(new EmptyBorder(18, 20, 18, 20));
    root.add(PandaTheme.label("Add Inventory Item", PandaTheme.HEADING, PandaTheme.BLACK),
      BorderLayout.NORTH);

    JPanel form = new JPanel(new GridBagLayout());
    form.setOpaque(false);
    String[] labels = {"Item name", "Unit", "Minimum threshold", "Quantity", "Shelf life (days)"};
    JTextField[] fields = {name, unit, threshold, quantity, shelfLife};
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(5, 0, 5, 12);
    c.anchor = GridBagConstraints.WEST;
    for (int i = 0; i < labels.length; i++) {
      c.gridy = i;
      c.gridx = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE;
      form.add(formLabel(labels[i]), c);
      c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
      form.add(fields[i], c);
    }
    root.add(form, BorderLayout.CENTER);

    final JLabel error = PandaTheme.label(" ", PandaTheme.SMALL, PandaTheme.RED);
    JButton cancel = PandaTheme.secondaryButton("Cancel");
    JButton ok = PandaTheme.primaryButton("Add Item");
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    buttons.setOpaque(false);
    buttons.add(cancel);
    buttons.add(ok);
    JPanel bottom = new JPanel(new BorderLayout(0, 8));
    bottom.setOpaque(false);
    bottom.add(error, BorderLayout.NORTH);
    bottom.add(buttons, BorderLayout.SOUTH);
    root.add(bottom, BorderLayout.SOUTH);

    cancel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { dialog.dispose(); }
    });
    ok.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String n = name.getText().trim(), u = unit.getText().trim();
        Double t = parseNonNegative(threshold.getText());
        Double q = parseNonNegative(quantity.getText());
        Integer s = null;
        try {
          s = Integer.parseInt(shelfLife.getText().trim());
          if (s < 0) s = null;
        } catch (NumberFormatException ignored) {}

        String err = null;
        if (n.isEmpty()) err = "Item name is required.";
        else if (n.length() > 100) err = "Item name is too long (max 100).";
        else if (u.isEmpty()) err = "Unit is required.";
        else if (u.length() > 20) err = "Unit is too long (max 20).";
        else if (t == null) err = "Minimum threshold must be a non-negative number.";
        else if (q == null) err = "Quantity must be a non-negative number.";
        else if (s == null) err = "Shelf life must be a non-negative whole number.";

        if (err != null) {
          error.setText(err);
          return;
        }

        try {
          dao.insertItem(n, u, t, q, s);
          dialog.dispose();
          reload();
        } catch (SQLException ex) {
          error.setText("Add failed: " + ex.getMessage());
        }
      }
    });
    dialog.getRootPane().setDefaultButton(ok);

    dialog.add(root);
    dialog.pack();
    dialog.setLocationRelativeTo(frame);
    return dialog;
  }

  static JLabel formLabel(String text) {
    return PandaTheme.label(text, PandaTheme.BOLD, PandaTheme.BLACK);
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

  // Table model: Item | Amount | Unit | Status | Unsaved
  class InventoryTableModel extends AbstractTableModel {
    final String[] cols = {"Item", "Amount", "Unit", "Status", "Unsaved"};
    List<InventoryDAO.Item> items = new ArrayList<InventoryDAO.Item>();
    LinkedHashMap<Integer, Double> pending = new LinkedHashMap<Integer, Double>();

    void setItems(List<InventoryDAO.Item> newItems) {
      items = newItems;
      pending.clear();
      fireTableDataChanged();
    }

    boolean isPending(int r) { return pending.containsKey(items.get(r).id); }

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
        case 3: return it.isLow() ? "LOW" : "ok";
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
