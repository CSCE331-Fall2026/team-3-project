import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


/*
 * Form for a new Inventory row. Errors show in red; closes on Cancel or a
 * successful insert, then runs onAdded.
 * 
 * @author Vishanth Yoganand
 */
public class AddItem extends JDialog {
  /*
  Functionality for button to add a menuitem to the database.

  * @param owner the user the window is centered on
  * @param dao database access to add items
  * @param onAdded to run after sucessful insert
  * @return void as it is the constructor
  */
  public AddItem(JFrame owner, final InventoryDAO dao, final Runnable onAdded) {
    super(owner, "Add Inventory Item", true);
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

    final JTextField name = PandaTheme.textField(18);
    final JComboBox<String> category = new JComboBox<String>(CashierGUI.CATEGORIES);
    category.setFont(PandaTheme.BODY);
    final JTextField price = PandaTheme.textField(18);

    JPanel root = new JPanel(new BorderLayout(0, 12));
    root.setBackground(PandaTheme.WHITE);
    root.setBorder(new EmptyBorder(18, 20, 18, 20));
    root.add(PandaTheme.label("Add Menu Item", PandaTheme.HEADING, PandaTheme.BLACK),
      BorderLayout.NORTH);

    JPanel form = new JPanel(new GridBagLayout());
    form.setOpaque(false);
    String[] labels = {"Item name", "Category", "Price"};
    JComponent[] fields = {name, category, price};
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(5, 0, 5, 12);
    c.anchor = GridBagConstraints.WEST;
    for (int i = 0; i < labels.length; i++) {
      c.gridy = i;
      c.gridx = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE;
      form.add(PandaTheme.label(labels[i], PandaTheme.BOLD, PandaTheme.BLACK), c);
      c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
      form.add(fields[i], c);
    }
    root.add(form, BorderLayout.CENTER);

    final List<InventoryDAO.Item> inventory;

    try {
      inventory = dao.fetchAll();
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(owner, "Could not load inventory. Try again.");
      dispose();
      throw new IllegalStateException();
    }
    
    final DefaultTableModel recipeModel = new DefaultTableModel(
        new String[] {"Ingredient", "Unit", "Qty per item"}, 0) {
      public Class<?> getColumnClass(int col) { return col == 2 ? Double.class : String.class; }
      public boolean isCellEditable(int r, int col) { return col == 2; }
    };
    for (InventoryDAO.Item it : inventory) {
      recipeModel.addRow(new Object[] {it.name, it.unit, null});
    }

    final JTable recipe = new JTable(recipeModel);
    recipe.setFont(PandaTheme.BODY);
    recipe.setRowHeight(24);
    recipe.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    recipe.getColumnModel().getColumn(0).setPreferredWidth(220);
    JScrollPane recipeScroll = PandaTheme.scroll(recipe);
    recipeScroll.setPreferredSize(new Dimension(420, 200));

    JPanel recipePanel = new JPanel(new BorderLayout(0, 6));
    recipePanel.setOpaque(false);
    recipePanel.add(PandaTheme.label("Ingredients (leave blank if not used)",
      PandaTheme.BOLD, PandaTheme.BLACK), BorderLayout.NORTH);
    recipePanel.add(recipeScroll, BorderLayout.CENTER);

    JPanel center = new JPanel(new BorderLayout(0, 12));
    center.setOpaque(false);
    center.add(form, BorderLayout.NORTH);
    center.add(recipePanel, BorderLayout.CENTER);
    root.add(center, BorderLayout.CENTER);

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
      public void actionPerformed(ActionEvent e) { dispose(); }
    });
    ok.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String n = name.getText().trim();
        String c = (String) category.getSelectedItem();
        Double p = parseNonNegative(price.getText());
        Integer s = null;

        String err = null;
        if (n.isEmpty()) err = "Item name is required.";
        else if (n.length() > 100) err = "Item name is too long (max 100).";
        else if ( p == 0) err = "Price must be greater than $0.00";

        Map<Integer, Double> ingredients = new LinkedHashMap<Integer, Double>();
        for (int r = 0; err == null && r < recipeModel.getRowCount(); r++) {
          Object v = recipeModel.getValueAt(r, 2);
          if (v == null) continue;
          double d = (Double) v;
          if (d == 0) continue;
          if (d < 0 || Double.isNaN(d) || Double.isInfinite(d)) {
            err = "Quantity for " + recipeModel.getValueAt(r, 0) + " must be a positive number.";
          } else {
            ingredients.put(inventory.get(r).id, Math.round(d * 1000) / 1000.0);
          }
        }

        if (err != null) {
          error.setText(err);
          return;
        }

        try {
          dao.insertMenuItem(n, c, p, ingredients);
          dispose();
          onAdded.run();
        } catch (SQLException ex) {
          error.setText("Add failed: " + ex.getMessage());
        }
      }
    });
    getRootPane().setDefaultButton(ok);

    add(root);
    pack();
    setLocationRelativeTo(owner);
  }

  /*
  * Parses a string to check if it contains a negative number.
  *
  * @param text containting a number
  * @return number if positive, null if negative
  */
  static Double parseNonNegative(String text) {
    try {
      double d = Double.parseDouble(text.trim());
      if (d < 0 || Double.isNaN(d) || Double.isInfinite(d)) return null;
      return d;
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
