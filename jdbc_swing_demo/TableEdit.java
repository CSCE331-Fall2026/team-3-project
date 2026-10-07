import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/*
 * Table model with one editable number in column 1 and "Unsaved" as the last column.
 * Edits wait in pending (id -> new value) until saved.
 */
public abstract class TableEdit<T> extends AbstractTableModel {
  final String[] cols;
  List<T> items = new ArrayList<T>();
  LinkedHashMap<Integer, Double> pending = new LinkedHashMap<Integer, Double>();
  Component parent; 

  TableEdit(String[] cols) { this.cols = cols; }

  abstract int id(T item);
  abstract double value(T item);
  abstract Object cell(T item, int col);

  void setItems(List<T> newItems) {
    items = newItems;
    pending.clear();
    fireTableDataChanged();
  }

  boolean isPending(int r) { return pending.containsKey(id(items.get(r))); }

  public int getRowCount() { return items.size(); }
  public int getColumnCount() { return cols.length; }
  public String getColumnName(int c) { return cols[c]; }

  public Class<?> getColumnClass(int c) {
    return c == 1 ? Double.class : String.class;
  }

  public boolean isCellEditable(int r, int c) { return c == 1; }

  public Object getValueAt(int r, int c) {
    T it = items.get(r);
    int id = id(it);
    if (c == 1) return pending.containsKey(id) ? pending.get(id) : value(it);
    if (c == cols.length - 1) return pending.containsKey(id) ? "*" : "";
    return cell(it, c);
  }

  public void setValueAt(Object v, int r, int c) {
    if (c != 1 || v == null) return;
    double d = (Double) v;
    if (d < 0 || Double.isNaN(d) || Double.isInfinite(d)) {
      JOptionPane.showMessageDialog(parent, cols[1] + " must be a non-negative number.");
      return;
    }
    d = Math.round(d * 100) / 100.0;
    T it = items.get(r);
    if (d == value(it)) pending.remove(id(it));
    else pending.put(id(it), d);
    fireTableRowsUpdated(r, r);
  }
}
