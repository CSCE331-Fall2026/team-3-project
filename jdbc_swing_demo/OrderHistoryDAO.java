/*
 * Read-only database queries for today's orders and their item quantities.
 */
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderHistoryDAO {
  private final Connection conn;

  // Uses the manager's existing database connection
  public OrderHistoryDAO(Connection conn) {
    this.conn = conn;
  }

  // Loads today's orders, newest first, using the database's checkout date
  public List<Object[]> fetchToday() throws SQLException {
    List<Object[]> orders = new ArrayList<Object[]>();
    try (Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(
          "SELECT OrderID, TO_CHAR(OrderTime, 'HH12:MI:SS AM') AS TimeMade, Total " +
          "FROM Orders WHERE OrderDate = CURRENT_DATE ORDER BY OrderTime DESC, OrderID DESC")) {
      while (rs.next()) {
        orders.add(new Object[] {rs.getInt("OrderID"), rs.getString("TimeMade"), rs.getDouble("Total")});
      }
    }
    return orders;
  }

  // Loads item names and quantities, including discontinued menu items
  public List<Object[]> fetchItems(int orderId) throws SQLException {
    List<Object[]> items = new ArrayList<Object[]>();
    try (PreparedStatement ps = conn.prepareStatement(
        "SELECT m.MenuItemName, SUM(oi.Qty) AS Qty FROM OrderItem oi " +
        "JOIN MenuItem m ON m.MenuItemID = oi.MenuItemID WHERE oi.OrderID = ? " +
        "GROUP BY m.MenuItemID, m.MenuItemName ORDER BY m.MenuItemName")) {
      ps.setInt(1, orderId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          items.add(new Object[] {rs.getString("MenuItemName"), rs.getLong("Qty")});
        }
      }
    }
    return items;
  }
}
