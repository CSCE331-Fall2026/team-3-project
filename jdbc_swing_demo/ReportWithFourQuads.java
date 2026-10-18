import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

/*
 * Reports page split into four quadrants.
 * Each quadrant is an empty placeholder panel to be filled in later
 * (e.g. sales report, excess report, restock report, ...).
 */



public class ReportWithFourQuads {
  public final JPanel panel = new JPanel(new BorderLayout(0, 12));
  public final JPanel[] quadrants = new JPanel[4];

  private final Connection conn;


  // Builds the reports page: heading on top, 2x2 grid of placeholder quadrants
  public ReportWithFourQuads(Connection conn) {
    this.conn = conn;
    JPanel heading = new JPanel();
    heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
    heading.setOpaque(false);
    heading.add(PandaTheme.label("Reports", PandaTheme.HEADING, PandaTheme.BLACK));

    JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
    grid.setOpaque(false);

    quadrants[0] = makeSalesReport("Sales Report");
    quadrants[1] = makeUsageReport("Usage Report");
    quadrants[2] = makeRestockReport("Restock Report");
    quadrants[3] = makeXReport("X-Report (Today)");

    grid.add(quadrants[0]);
    grid.add(quadrants[1]);
    grid.add(quadrants[2]);
    grid.add(quadrants[3]);

    panel.setBackground(PandaTheme.WHITE);
    panel.setBorder(new EmptyBorder(16, 20, 16, 20));
    panel.add(heading, BorderLayout.NORTH);
    panel.add(grid, BorderLayout.CENTER);
  }

  // Fills the quadrant for the sales report: quantity sold and revenue per menu item
  private JPanel makeSalesReport(String title) {
    JPanel q = new JPanel(new BorderLayout(0, 8));
    q.setBackground(PandaTheme.WHITE);
    q.setBorder(new CompoundBorder(
        new LineBorder(PandaTheme.GRID, 1),
        new EmptyBorder(12, 12, 12, 12)));

    LocalDate today = LocalDate.now();
    JSpinner startSpinner = new JSpinner(new SpinnerDateModel(
        java.util.Date.from(today.minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant()),
        null, null, java.util.Calendar.DAY_OF_MONTH));
    JSpinner endSpinner = new JSpinner(new SpinnerDateModel(
        java.util.Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant()),
        null, null, java.util.Calendar.DAY_OF_MONTH));
    startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, "yyyy-MM-dd"));
    endSpinner.setEditor(new JSpinner.DateEditor(endSpinner, "yyyy-MM-dd"));

    JButton runButton = new JButton("Run");
    JLabel status = PandaTheme.label(" ", PandaTheme.BOLD, PandaTheme.MUTED);

    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    controls.setOpaque(false);
    controls.add(new JLabel("From"));
    controls.add(startSpinner);
    controls.add(new JLabel("To"));
    controls.add(endSpinner);
    controls.add(runButton);

    JPanel header = new JPanel(new BorderLayout(0, 6));
    header.setOpaque(false);
    header.add(PandaTheme.label(title, PandaTheme.BOLD, PandaTheme.MUTED), BorderLayout.NORTH);
    header.add(controls, BorderLayout.CENTER);
    header.add(status, BorderLayout.SOUTH);
    q.add(header, BorderLayout.NORTH);

    DefaultTableModel tableModel = new DefaultTableModel(
        new String[] {"Item", "Qty Sold", "Revenue"}, 0) {
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    q.add(PandaTheme.scroll(new JTable(tableModel)), BorderLayout.CENTER);

    Runnable refresh = () -> {
        LocalDate startDate = ((java.util.Date) startSpinner.getValue())
            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = ((java.util.Date) endSpinner.getValue())
            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (startDate.isAfter(endDate)) {
            status.setText("Start must be on or before end");
            return;
        }
        tableModel.setRowCount(0);

        // revenue uses each item's current price
        int totalQty = 0;
        double totalRevenue = 0;
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT m.MenuItemName, SUM(oi.Qty) AS QtySold, " +
            "       SUM(oi.Qty * m.Price) AS Revenue " +
            "FROM Orders o " +
            "JOIN OrderItem oi ON oi.OrderID    = o.OrderID " +
            "JOIN MenuItem m   ON m.MenuItemID  = oi.MenuItemID " +
            "WHERE o.OrderDate BETWEEN ? AND ? " +
            "GROUP BY m.MenuItemID, m.MenuItemName " +
            "ORDER BY Revenue DESC")) {
            ps.setDate(1, java.sql.Date.valueOf(startDate));
            ps.setDate(2, java.sql.Date.valueOf(endDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int qty = rs.getInt("QtySold");
                    double revenue = rs.getDouble("Revenue");
                    tableModel.addRow(new Object[] {
                        rs.getString("MenuItemName"), qty, String.format("$%.2f", revenue)
                    });
                    totalQty += qty;
                    totalRevenue += revenue;
                }
            }
        } catch (SQLException ex) {
            status.setText("Query failed: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        status.setText(String.format("%d items sold, $%.2f revenue", totalQty, totalRevenue));
    };

    runButton.addActionListener(e -> refresh.run());
    refresh.run();

    return q;
  }

 // Fills the quadrant for the usage report
 private JPanel makeUsageReport(String title) {
    JPanel q = new JPanel(new BorderLayout(0, 8));
    q.setBackground(PandaTheme.WHITE);
    q.setBorder(new CompoundBorder(
        new LineBorder(PandaTheme.GRID, 1),
        new EmptyBorder(12, 12, 12, 12)));

    LocalDate today = LocalDate.now();
    JSpinner startSpinner = new JSpinner(new SpinnerDateModel(
        java.util.Date.from(today.minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant()),
        null, null, java.util.Calendar.DAY_OF_MONTH));
    JSpinner endSpinner = new JSpinner(new SpinnerDateModel(
        java.util.Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant()),
        null, null, java.util.Calendar.DAY_OF_MONTH));
    startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, "yyyy-MM-dd"));
    endSpinner.setEditor(new JSpinner.DateEditor(endSpinner, "yyyy-MM-dd"));

    JButton runButton = new JButton("Run");
    JLabel status = PandaTheme.label(" ", PandaTheme.BOLD, PandaTheme.MUTED);

    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    controls.setOpaque(false);
    controls.add(new JLabel("From"));
    controls.add(startSpinner);
    controls.add(new JLabel("To"));
    controls.add(endSpinner);
    controls.add(runButton);
    controls.add(status);

    JPanel header = new JPanel(new BorderLayout(0, 6));
    header.setOpaque(false);
    header.add(PandaTheme.label(title, PandaTheme.BOLD, PandaTheme.MUTED), BorderLayout.NORTH);
    header.add(controls, BorderLayout.SOUTH);
    q.add(header, BorderLayout.NORTH);

    BarGraph chart = new BarGraph("", new String[0], new double[0]);
    chart.setBackground(PandaTheme.WHITE);
    chart.setPreferredSize(new Dimension(500, 300));
    q.add(chart, BorderLayout.CENTER);

    Runnable refresh = () -> {
        LocalDate startDate = ((java.util.Date) startSpinner.getValue())
            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = ((java.util.Date) endSpinner.getValue())
            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (startDate.isAfter(endDate)) {
            status.setText("Start must be on or before end");
            return;
        }

        java.util.List<String> labels = new ArrayList<>();
        java.util.List<Double> values = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT i.InventoryID, i.ItemName, i.Unit, " +
            "       SUM(oi.Qty * mii.QuantityRequired) AS AmountUsed " +
            "FROM Orders o " +
            "JOIN (SELECT OrderID, MenuItemID, Qty FROM OrderItem UNION ALL " +
            "SELECT parent.OrderID, s.MenuItemID, parent.Qty * s.Qty " +
            "FROM OrderItemSelection s JOIN OrderItem parent ON parent.OrderItemID = s.OrderItemID) oi " +
            "ON oi.OrderID = o.OrderID " +
            "JOIN MenuItemInventory mii ON mii.MenuItemID = oi.MenuItemID " +
            "JOIN Inventory i           ON i.InventoryID  = mii.InventoryID " +
            "WHERE o.OrderDate BETWEEN ? AND ? " +
            "GROUP BY i.InventoryID, i.ItemName, i.Unit " +
            "ORDER BY AmountUsed DESC")) {
            ps.setDate(1, java.sql.Date.valueOf(startDate));
            ps.setDate(2, java.sql.Date.valueOf(endDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    labels.add(rs.getString("ItemName") + " (" + rs.getString("Unit") + ")");
                    values.add(rs.getDouble("AmountUsed"));
                }
            }
        } catch (SQLException ex) {
            status.setText("Query failed: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        chart.setData(labels.toArray(new String[0]),
            values.stream().mapToDouble(Double::doubleValue).toArray());
        status.setText(labels.size() + " items");
    };

    runButton.addActionListener(e -> refresh.run());
    refresh.run();

    return q;
  }

  // Fills the quadrant for the restock report
  private JPanel makeRestockReport(String title) {
    JPanel q = new JPanel(new BorderLayout(0, 8));
    q.setBackground(PandaTheme.WHITE);
    q.setBorder(new CompoundBorder(
        new LineBorder(PandaTheme.GRID, 1),
        new EmptyBorder(12, 12, 12, 12)));

    JButton runButton = new JButton("Run");
    JLabel status = PandaTheme.label(" ", PandaTheme.BOLD, PandaTheme.MUTED);

    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    controls.setOpaque(false);
    controls.add(runButton);
    controls.add(status);

    JPanel header = new JPanel(new BorderLayout(0, 6));
    header.setOpaque(false);
    header.add(PandaTheme.label(title, PandaTheme.BOLD, PandaTheme.MUTED), BorderLayout.NORTH);
    header.add(controls, BorderLayout.SOUTH);
    q.add(header, BorderLayout.NORTH);

    // makes a read-only table model for the restock report
    DefaultTableModel tableModel = new DefaultTableModel(
        new String[] {"Item", "Amount", "Minimum", "Unit"}, 0) {
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    q.add(PandaTheme.scroll(new JTable(tableModel)), BorderLayout.CENTER);

    Runnable refresh = () -> {
        tableModel.setRowCount(0);

        // checks the inventory table for items that are below their threshold
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT i.InventoryID, i.ItemName, i.Quantity, " +
            "       i." + InventoryDAO.THRESHOLD_COL + ", i.Unit " +
            "FROM Inventory i " +
            "WHERE i.Active = TRUE " +
            "AND i.Quantity < i." + InventoryDAO.THRESHOLD_COL + " " +
            "ORDER BY (i." + InventoryDAO.THRESHOLD_COL + " - i.Quantity) DESC");
            ResultSet rs = ps.executeQuery())  {
            while (rs.next()) {
                tableModel.addRow(new Object[] {
                    rs.getString("ItemName"),
                    rs.getDouble("Quantity"),
                    rs.getDouble(InventoryDAO.THRESHOLD_COL),
                    rs.getString("Unit")
                });
            }
        } catch (SQLException ex) {
            status.setText("Query failed: " + ex.getMessage());
            return;
        }
        
        // display the number of items that need restocking
        status.setText(tableModel.getRowCount() + " items need restocking");
    };

    runButton.addActionListener(e -> refresh.run());
    refresh.run();

    return q;
  }

  // Generates an X-Report showing hourly sales for the current day.
  // Read-only: does not modify orders, inventory, or register totals.
  private JPanel makeXReport(String title) {
      JPanel q = new JPanel(new BorderLayout(0, 8));
      q.setBackground(PandaTheme.WHITE);
      q.setBorder(new CompoundBorder(
          new LineBorder(PandaTheme.GRID, 1),
          new EmptyBorder(12, 12, 12, 12)));

      JButton runButton = new JButton("Run");
      JLabel status = PandaTheme.label(" ", PandaTheme.BOLD, PandaTheme.MUTED);

      JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
      controls.setOpaque(false);
      controls.add(runButton);
      controls.add(status);

      JPanel header = new JPanel(new BorderLayout(0, 6));
      header.setOpaque(false);
      header.add(PandaTheme.label(title, PandaTheme.BOLD, PandaTheme.MUTED),
          BorderLayout.NORTH);
      header.add(controls, BorderLayout.SOUTH);

      q.add(header, BorderLayout.NORTH);

      // Create line graph with 24 hours of data.
      XReportGraph chart = new XReportGraph();
      chart.setPreferredSize(new Dimension(500, 300));
      q.add(chart, BorderLayout.CENTER);

      Runnable refresh = () -> {
          // Reset sales before querying.
          double[] hourlySales = new double[24];

          LocalDate today = LocalDate.now();

          String sql =
            "SELECT EXTRACT(HOUR FROM OrderTime) AS Hour, " +
            "SUM(Total) AS TotalSales " +
            "FROM Orders " +
            "WHERE OrderDate = ? " +
            "GROUP BY EXTRACT(HOUR FROM OrderTime) " +
            "ORDER BY Hour";

          try (PreparedStatement ps = conn.prepareStatement(sql)) {

              ps.setDate(1, java.sql.Date.valueOf(today));

              try (ResultSet rs = ps.executeQuery()) {
                  while (rs.next()) {
                      int hour = rs.getInt("Hour");
                      double sales = rs.getDouble("TotalSales");

                      if (hour >= 0 && hour < 24) {
                          hourlySales[hour] = sales;
                      }
                  }
              }

              // Update graph.
              chart.setData(hourlySales);

              double total = 0;
              for (double sales : hourlySales) {
                  total += sales;
              }

              status.setText(String.format(
                  "Today's Sales: $%.2f", total));

          } catch (SQLException ex) {
              status.setText("Query failed: " + ex.getMessage());
              ex.printStackTrace();
          }
      };

      runButton.addActionListener(e -> refresh.run());

      // Generate report when page opens.
      refresh.run();

      return q;
  }
}
