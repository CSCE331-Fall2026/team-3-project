import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * Database calls for the manager GUI (model side of MVC).
 *
 *   fetchAll()          - every Inventory row, ordered by name
 *   updateQuantities()  - batch update of Quantity for a set of items (one transaction)
 *   insertItem()        - add a new Inventory row; InventoryID = MAX + 1
 *   fetchMenuItems()    - every MenuItem row, ordered by MenuItemID
 *   updatePrices()      - batch update of Price for a set of menu items (one transaction)
 *
 * A new price applies to orders placed after it is saved. Past orders are not
 * touched: their totals are already stored in Orders.Total.
 *
 * The threshold column is Minimum_Threshold in the live database
 * (see generate_base_data.cpp), even though the CHANGELOG calls it MinimumThreshold.
 */
public class InventoryDAO {

  static final String THRESHOLD_COL = "Minimum_Threshold";

  // One Inventory row.
  public static class Item {
    public int id;
    public String name;
    public String unit;
    public double threshold;
    public double quantity;
    public int shelfLifeDays;

    // "low" means at or below the minimum threshold
    public boolean isLow() { return quantity <= threshold; }
  }

  // One MenuItem row.
  public static class MenuItem {
    public int id;
    public String name;
    public String category;
    public double price;
  }

  private final Connection conn;

  public InventoryDAO(Connection conn) { this.conn = conn; }

  public List<Item> fetchAll() throws SQLException {
    List<Item> items = new ArrayList<Item>();
    Statement st = conn.createStatement();
    ResultSet rs = st.executeQuery(
      "SELECT InventoryID, ItemName, Unit, " + THRESHOLD_COL + ", Quantity, ShelfLifeDays " +
      "FROM Inventory " +
      "WHERE Active = TRUE " +
      "ORDER BY ItemName");
    while (rs.next()) {
      Item it = new Item();
      it.id = rs.getInt("InventoryID");
      it.name = rs.getString("ItemName");
      it.unit = rs.getString("Unit");
      it.threshold = rs.getDouble(THRESHOLD_COL);
      it.quantity = rs.getDouble("Quantity");
      it.shelfLifeDays = rs.getInt("ShelfLifeDays");
      items.add(it);
    }
    rs.close(); st.close();
    return items;
  }

  public void updateQuantities(Map<Integer, Double> changes) throws SQLException {
    if (changes.isEmpty()) return;
    try {
      conn.setAutoCommit(false);
      PreparedStatement ps = conn.prepareStatement(
        "UPDATE Inventory SET Quantity = ? WHERE InventoryID = ?");
      for (Map.Entry<Integer, Double> e : changes.entrySet()) {
        ps.setDouble(1, e.getValue());
        ps.setInt(2, e.getKey());
        ps.addBatch();
      }
      ps.executeBatch();
      ps.close();
      conn.commit();
    } catch (SQLException ex) {
      try { conn.rollback(); } catch (SQLException ignored) {}
      throw ex;
    } finally {
      try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
    }
  }

public int insertMenuItem(String name, String category, double price,
                            Map<Integer, Double> ingredients) throws SQLException {
    try {
      conn.setAutoCommit(false);

      int id;
      try (Statement st = conn.createStatement();
           ResultSet rs = st.executeQuery(
             "SELECT COALESCE(MAX(MenuItemID), 0) + 1 FROM MenuItem")) {
        rs.next();
        id = rs.getInt(1);
      }

      try (PreparedStatement ps = conn.prepareStatement(
             "INSERT INTO MenuItem (MenuItemID, MenuItemName, Category, Price) " +
             "VALUES (?, ?, ?, ?)")) {
        ps.setInt(1, id);
        ps.setString(2, name);
        ps.setString(3, category);
        ps.setDouble(4, price);
        ps.executeUpdate();
      }

      if (!ingredients.isEmpty()) {
        try (PreparedStatement ps = conn.prepareStatement(
               "INSERT INTO MenuItemInventory (MenuItemID, InventoryID, QuantityRequired) " +
               "VALUES (?, ?, ?)")) {
          for (Map.Entry<Integer, Double> e : ingredients.entrySet()) {
            ps.setInt(1, id);
            ps.setInt(2, e.getKey());
            ps.setDouble(3, e.getValue());
            ps.addBatch();
          }
          ps.executeBatch();
        }
      }

      conn.commit();
      return id;
    } catch (SQLException ex) {
      try { conn.rollback(); } catch (SQLException ignored) {}
      throw ex;
    } finally {
      try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
    }
  }

  // Returns the new InventoryID.
  public int insertMenuItem(String name, double price, String category)
    throws SQLException {

    try {
      conn.setAutoCommit(false);

      // Check whether item already exists
      PreparedStatement check = conn.prepareStatement(
        "SELECT MenuItemID, Active FROM MenuItem " +
        "WHERE LOWER(MenuItemName) = LOWER(?)"
      );

      check.setString(1, name);
      ResultSet rs = check.executeQuery();

      if (rs.next()) {
        int id = rs.getInt("MenuItemID");
        boolean active = rs.getBoolean("Active");

        rs.close();
        check.close();

        if (active) {
          throw new SQLException(
            "An active menu item named \"" + name + "\" already exists."
          );
        }

        // Reactivate discontinued item
        PreparedStatement reactivate = conn.prepareStatement(
          "UPDATE MenuItem " +
          "SET Price = ?, Category = ?, Active = TRUE " +
          "WHERE MenuItemID = ?"
        );

        reactivate.setDouble(1, price);
        reactivate.setString(2, category);
        reactivate.setInt(3, id);

        reactivate.executeUpdate();
        reactivate.close();

        conn.commit();
        return id;
      }

      rs.close();
      check.close();

      // New menu item
      Statement st = conn.createStatement();
      ResultSet next = st.executeQuery(
        "SELECT COALESCE(MAX(MenuItemID), 0) + 1 AS nextId FROM MenuItem"
      );

      next.next();
      int id = next.getInt("nextId");

      next.close();
      st.close();

      PreparedStatement insert = conn.prepareStatement(
        "INSERT INTO MenuItem " +
        "(MenuItemID, MenuItemName, Price, Category, Active) " +
        "VALUES (?, ?, ?, ?, TRUE)"
      );

      insert.setInt(1, id);
      insert.setString(2, name);
      insert.setDouble(3, price);
      insert.setString(4, category);

      insert.executeUpdate();
      insert.close();

      conn.commit();
      return id;

    } catch (SQLException ex) {
      try { conn.rollback(); }
      catch (SQLException ignored) {}

      throw ex;

    } finally {
      try { conn.setAutoCommit(true); }
      catch (SQLException ignored) {}
    }
  }

  public List<MenuItem> fetchMenuItems() throws SQLException {
    List<MenuItem> items = new ArrayList<MenuItem>();
    Statement st = conn.createStatement();
    ResultSet rs = st.executeQuery(
      "SELECT MenuItemID, MenuItemName, Price FROM MenuItem WHERE Active = TRUE ORDER BY MenuItemID");
    while (rs.next()) {
      MenuItem it = new MenuItem();
      it.id = rs.getInt("MenuItemID");
      it.name = rs.getString("MenuItemName");
      it.price = rs.getDouble("Price");
      items.add(it);
    }
    rs.close(); st.close();
    return items;
  }

  public void updatePrices(Map<Integer, Double> changes) throws SQLException {
    if (changes.isEmpty()) return;
    try {
      conn.setAutoCommit(false);
      PreparedStatement ps = conn.prepareStatement(
        "UPDATE MenuItem SET Price = ? WHERE MenuItemID = ?");
      for (Map.Entry<Integer, Double> e : changes.entrySet()) {
        ps.setDouble(1, e.getValue());
        ps.setInt(2, e.getKey());
        ps.addBatch();
      }
      ps.executeBatch();
      ps.close();
      conn.commit();
    } catch (SQLException ex) {
      try { conn.rollback(); } catch (SQLException ignored) {}
      throw ex;
    } finally {
      try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
    }
  }
  
  public void discontinueMenuItem(int menuItemId) throws SQLException {
    PreparedStatement ps = conn.prepareStatement(
      "UPDATE MenuItem SET Active = FALSE WHERE MenuItemID = ?"
    );

    ps.setInt(1, menuItemId);

    int rowsUpdated = ps.executeUpdate();
    ps.close();

    if (rowsUpdated == 0) {
      throw new SQLException(
        "No menu item found with ID " + menuItemId
      );
    }
  }

  public void discontinueInventoryItem(int inventoryId) throws SQLException {
    PreparedStatement ps = conn.prepareStatement(
      "UPDATE Inventory SET Active = FALSE WHERE InventoryID = ?"
    );

    ps.setInt(1, inventoryId);

    int rowsUpdated = ps.executeUpdate();
    ps.close();

    if (rowsUpdated == 0) {
      throw new SQLException(
        "No inventory item found with ID " + inventoryId
      );
    }
  }
}


