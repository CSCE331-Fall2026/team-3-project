import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class AddMenuItem extends JDialog {

  private final InventoryDAO dao;
  private final Runnable onSuccess;

  private JTextField nameField;
  private JTextField priceField;
  private JComboBox<String> categoryBox;

  public AddMenuItem(JFrame parent, InventoryDAO dao, Runnable onSuccess) {
    super(parent, "Add Menu Item", true);

    this.dao = dao;
    this.onSuccess = onSuccess;

    build();

    pack();
    setLocationRelativeTo(parent);
  }

  private void build() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(new EmptyBorder(16, 16, 16, 16));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(6, 6, 6, 6);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    nameField = new JTextField(20);
    priceField = new JTextField(10);

    String[] categories = {
      "Sides",
      "Entrees",
      "Appetizers",
      "Meals",
      "Extras",
      "Drinks"
    };

    categoryBox = new JComboBox<String>(categories);

    gbc.gridx = 0;
    gbc.gridy = 0;
    panel.add(new JLabel("Name:"), gbc);

    gbc.gridx = 1;
    panel.add(nameField, gbc);

    gbc.gridx = 0;
    gbc.gridy = 1;
    panel.add(new JLabel("Price:"), gbc);

    gbc.gridx = 1;
    panel.add(priceField, gbc);

    gbc.gridx = 0;
    gbc.gridy = 2;
    panel.add(new JLabel("Category:"), gbc);

    gbc.gridx = 1;
    panel.add(categoryBox, gbc);

    JButton cancel = new JButton("Cancel");
    cancel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });

    JButton add = new JButton("Add Item");
    add.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        addItem();
      }
    });

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttons.add(cancel);
    buttons.add(add);

    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 2;
    panel.add(buttons, gbc);

    add(panel);
  }

  private void addItem() {
    String name = nameField.getText().trim();
    String priceText = priceField.getText().trim();
    String category = (String) categoryBox.getSelectedItem();

    if (name.isEmpty()) {
      JOptionPane.showMessageDialog(
        this,
        "Enter a menu item name."
      );
      return;
    }

    double price;

    try {
      price = Double.parseDouble(priceText);
    } catch (NumberFormatException ex) {
      JOptionPane.showMessageDialog(
        this,
        "Price must be a valid number."
      );
      return;
    }

    if (price < 0) {
      JOptionPane.showMessageDialog(
        this,
        "Price must be non-negative."
      );
      return;
    }

    try {
      dao.insertMenuItem(name, price, category);

      if (onSuccess != null) {
        onSuccess.run();
      }

      JOptionPane.showMessageDialog(
        this,
        "\"" + name + "\" was added to the menu."
      );

      dispose();

    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(
        this,
        "Could not add menu item:\n" + ex.getMessage(),
        "Database Error",
        JOptionPane.ERROR_MESSAGE
      );
    }
  }
}