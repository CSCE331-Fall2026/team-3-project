import java.sql.SQLException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
      form.add(PandaTheme.label(labels[i], PandaTheme.BOLD, PandaTheme.BLACK), c);
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
      public void actionPerformed(ActionEvent e) { dispose(); }
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
