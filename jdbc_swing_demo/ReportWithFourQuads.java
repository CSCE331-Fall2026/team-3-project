import java.awt.*;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/*
 * Reports page split into four quadrants.
 * Each quadrant is an empty placeholder panel to be filled in later
 * (e.g. sales report, excess report, restock report, ...).
 */
public class ReportWithFourQuads {
  public final JPanel panel = new JPanel(new BorderLayout(0, 12));
  public final JPanel[] quadrants = new JPanel[4];

  // Builds the reports page: heading on top, 2x2 grid of placeholder quadrants
  public ReportWithFourQuads() {
    JPanel heading = new JPanel();
    heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
    heading.setOpaque(false);
    heading.add(PandaTheme.label("Reports", PandaTheme.HEADING, PandaTheme.BLACK));

    JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
    grid.setOpaque(false);

    quadrants[i] = makeQuadrant("Report " + (i + 1));
    grid.add(quadrants[i]);

    panel.setBackground(PandaTheme.WHITE);
    panel.setBorder(new EmptyBorder(16, 20, 16, 20));
    panel.add(heading, BorderLayout.NORTH);
    panel.add(grid, BorderLayout.CENTER);
  }

  // Creates one empty bordered quadrant panel with a placeholder label
  private JPanel makeQuadrant(String title) {
    JPanel q = new JPanel(new BorderLayout());
    q.setBackground(PandaTheme.WHITE);
    q.setBorder(new CompoundBorder(
      new LineBorder(PandaTheme.GRID, 1),
      new EmptyBorder(12, 12, 12, 12)));
    q.add(PandaTheme.label(title, PandaTheme.BOLD, PandaTheme.MUTED), BorderLayout.NORTH);
    return q;
  }
}
