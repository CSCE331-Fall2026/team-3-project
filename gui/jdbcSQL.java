import java.sql.*;

/*
CSCE 331
9-25-2019 Original
2/7/2020 Update for AWS
 */

public class jdbcSQL {

  // Database connection object
  static Connection conn = null;


  public static void openConnection(dbSetup credentials) {
    try {
      conn = DriverManager.getConnection(
        "jdbc:postgresql://csce-315-db.engr.tamu.edu/team3db",
        credentials.user, credentials.pswd);
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      System.exit(0);
    }
    System.out.println("Opened database successfully");
  }


  public static void closeConnection() {
    try {
      conn.close();
      System.out.println("Connection closed.");
    } catch (Exception e) {
      System.out.println("Connection NOT closed.");
    }
  }


  public static void main(String args[]) {
    // dbSetup hides the username and password
    dbSetup credentials = new dbSetup();

    // build the connection
    openConnection(credentials);

    try {
      // create an sql statement
      Statement stmt = conn.createStatement();
      String sqlStatement =
        "SELECT * FROM MenuItem";

      // send the sql statement to the db
      ResultSet result = stmt.executeQuery(sqlStatement);

      String data = String.format("%-30s", "Vendors") + "\tProducts\n";
      // output result
      System.out.println(String.format("%-30s", "Vendors") + "\tProducts");
      System.out.println("=========================================================");
      while (result.next()) {
        data += String.format("%-30s", result.getString("menuitemname")) + "\t" + result.getString("price") + "\n";
      }

    } catch (Exception e) {
      System.out.println("Error accessing Database.");
    }

    // close the connection
    closeConnection();    
  }
}
