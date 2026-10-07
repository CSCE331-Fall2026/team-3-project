import java.sql.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Manager window: Inventory (edit amounts, add item, remove item - visual only),
 * Menu (edit prices; future orders only), Reports and Schedule placeholders.
 * LOW = Quantity <= Minimum_Threshold.
 */
public class ManagerGUI {

  static final String[] PAGES = {"Inventory", "Menu", "Reports", "Schedule"};

  JFrame frame;
  final Connection db;
  InventoryDAO dao;
  TablePage<InventoryDAO.Item> inventory;
  TablePage<InventoryDAO.MenuItem> menu;
  CardLayout cards;
  JPanel pages;
  LinkedHashMap<String, JButton> navButtons = new LinkedHashMap<String, JButton>();

  public static void main(String args[]) {
    ModeSelect.openConnection(new dbSetup());
    PandaTheme.useSystemLook();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() { new ManagerGUI(ModeSelect.conn).build(); }
    });
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
    pages.add(buildInventoryPage(), "Inventory");
    pages.add(buildMenuPage(), "Menu");
    pages.add(placeholder("Reports"), "Reports");
    pages.add(placeholder("Employee schedule"), "Schedule");

    JPanel root = new JPanel(new BorderLayout());
    root.setBackground(PandaTheme.WHITE);
    root.add(buildNavBar(), BorderLayout.NORTH);
    root.add(pages, BorderLayout.CENTER);

    frame.add(root);
    showPage("Inventory");
    frame.setVisible(true);
    inventory.reload();
    menu.reload();
  }

  // nav bar: Inventory | Menu | Reports | Schedule ... Switch Mode
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
    inventory.stopEditing();
    menu.stopEditing();
    if ((inventory.hasUnsaved() || menu.hasUnsaved()) && JOptionPane.showConfirmDialog(frame,
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

  // Inventory page: Item | Amount | Unit | Status | Unsaved
  JPanel buildInventoryPage() {
    TableEdit<InventoryDAO.Item> m = new TableEdit<InventoryDAO.Item>(
        new String[] {"Item", "Amount", "Unit", "Status", "Unsaved"}) {
      int id(InventoryDAO.Item it) { return it.id; }
      double value(InventoryDAO.Item it) { return it.quantity; }
      Object cell(InventoryDAO.Item it, int c) {
        if (c == 0) return it.name;
        if (c == 2) return it.unit;
        return it.isLow() ? "LOW" : "ok";
      }
    };
    inventory = new TablePage<InventoryDAO.Item>(frame, "Inventory", null, "items", m, "",
        new int[] {280, 110, 90, 100, 90}) {
      List<InventoryDAO.Item> load() throws SQLException { return dao.fetchAll(); }
      void store(Map<Integer, Double> changes) throws SQLException {
        dao.updateQuantities(changes);
      }
    };
    inventory.table.getColumnModel().getColumn(3).setCellRenderer(PandaTheme.statusRenderer());

    JButton remove = PandaTheme.secondaryButton("Remove Item");
    remove.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { removeSelected(); }
    });
    JButton add = PandaTheme.secondaryButton("+ Add Item");
    add.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        new AddItem(frame, dao, new Runnable() {
          public void run() { inventory.reload(); }
        }).setVisible(true);
      }
    });
    inventory.topButtons.add(remove);
    inventory.topButtons.add(add);
    return inventory.panel;
  }

  // Menu page: Menu Item | Price | Unsaved
  JPanel buildMenuPage() {
    TableEdit<InventoryDAO.MenuItem> m = new TableEdit<InventoryDAO.MenuItem>(
        new String[] {"Menu Item", "Price", "Unsaved"}) {
      int id(InventoryDAO.MenuItem it) { return it.id; }
      double value(InventoryDAO.MenuItem it) { return it.price; }
      Object cell(InventoryDAO.MenuItem it, int c) { return it.name; }
    };
    menu = new TablePage<InventoryDAO.MenuItem>(frame, "Menu Prices",
        "New prices apply to future orders only. Past orders keep the totals they were charged.",
        "menu items", m, "$", new int[] {420, 120, 90}) {
      List<InventoryDAO.MenuItem> load() throws SQLException { return dao.fetchMenuItems(); }
      void store(Map<Integer, Double> changes) throws SQLException {
        dao.updatePrices(changes);
      }
      boolean confirmSave(int n) {
        return JOptionPane.showConfirmDialog(frame,
          "Update " + n + " price(s)? New prices apply to future orders only.",
          "Save Prices", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
      }
    };
    JButton remove = PandaTheme.secondaryButton("Discontinue Item");

    remove.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        removeSelectedMenuItem();
      }
    });
    JButton add = PandaTheme.secondaryButton("+ Add Item");

    add.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        new AddMenuItem(frame, dao, new Runnable() {
          public void run() {
            menu.reload();
          }
        }).setVisible(true);
      }
    });

    menu.topButtons.add(remove);
    menu.topButtons.add(add);

    return menu.panel;
  }

  // Discontinue selected menu item
  void removeSelectedMenuItem() {
    menu.stopEditing();

    int selectedRow = menu.table.getSelectedRow();

    if (selectedRow < 0) {
      JOptionPane.showMessageDialog(
        frame,
        "Select a menu item to discontinue."
      );
      return;
    }

    int row = menu.table.convertRowIndexToModel(selectedRow);

    InventoryDAO.MenuItem item = menu.model.items.get(row);

    if (JOptionPane.showConfirmDialog(
        frame,
        "Discontinue \"" + item.name + "\"?",
        "Discontinue Menu Item",
        JOptionPane.YES_NO_OPTION
        ) != JOptionPane.YES_OPTION) {
      return;
    }

    try {
      dao.discontinueMenuItem(item.id);

      menu.reload();

      JOptionPane.showMessageDialog(
        frame,
        "\"" + item.name + "\" was discontinued."
      );

    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(
        frame,
        "Could not discontinue menu item:\n" + ex.getMessage(),
        "Database Error",
        JOptionPane.ERROR_MESSAGE
      );
    }
  }

  void removeSelected() {
    inventory.stopEditing();

    int selectedRow = inventory.table.getSelectedRow();

    if (selectedRow < 0) {
      JOptionPane.showMessageDialog(
        frame,
        "Select an item in the table to remove."
      );
      return;
    }

    int row = inventory.table.convertRowIndexToModel(selectedRow);

    InventoryDAO.Item item = inventory.model.items.get(row);

    if (JOptionPane.showConfirmDialog(
        frame,
        "Discontinue \"" + item.name + "\" from inventory?",
        "Discontinue Inventory Item",
        JOptionPane.YES_NO_OPTION
        ) != JOptionPane.YES_OPTION) {
      return;
    }

    try {
      dao.discontinueInventoryItem(item.id);

      inventory.reload();

      JOptionPane.showMessageDialog(
        frame,
        "\"" + item.name + "\" was discontinued from inventory."
      );

    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(
        frame,
        "Could not discontinue item:\n" + ex.getMessage(),
        "Database Error",
        JOptionPane.ERROR_MESSAGE
      );
    }
  }
}
