import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * Database calls for the manager inventory panel (model side of MVC).
 *
 *   fetchAll()          - every Inventory row, ordered by name
 *   updateQuantities()  - batch update of Quantity for a set of items (one transaction)
 *   insertItem()        - add a new Inventory row; InventoryID = MAX + 1
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

  private final Connection conn;

  public InventoryDAO(Connection conn) { this.conn = conn; }

  public List<Item> fetchAll() throws SQLException {
    List<Item> items = new ArrayList<Item>();
    Statement st = conn.createStatement();
    ResultSet rs = st.executeQuery(
      "SELECT InventoryID, ItemName, Unit, " + THRESHOLD_COL + ", Quantity, ShelfLifeDays " +
      "FROM Inventory ORDER BY ItemName");
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

  // Returns the new InventoryID.
  public int insertItem(String name, String unit, double threshold,
                        double quantity, int shelfLifeDays) throws SQLException {
    try {
      conn.setAutoCommit(false);
      Statement st = conn.createStatement();
      ResultSet rs = st.executeQuery(
        "SELECT COALESCE(MAX(InventoryID), 0) + 1 AS nextId FROM Inventory");
      rs.next();
      int id = rs.getInt("nextId");
      rs.close(); st.close();

      PreparedStatement ps = conn.prepareStatement(
        "INSERT INTO Inventory (InventoryID, ItemName, Unit, " + THRESHOLD_COL +
        ", Quantity, ShelfLifeDays) VALUES (?, ?, ?, ?, ?, ?)");
      ps.setInt(1, id);
      ps.setString(2, name);
      ps.setString(3, unit);
      ps.setDouble(4, threshold);
      ps.setDouble(5, quantity);
      ps.setInt(6, shelfLifeDays);
      ps.executeUpdate();
      ps.close();
      conn.commit();
      return id;
    } catch (SQLException ex) {
      try { conn.rollback(); } catch (SQLException ignored) {}
      throw ex;
    } finally {
      try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
    }
  }
}
