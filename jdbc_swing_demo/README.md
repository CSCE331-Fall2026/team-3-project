## Cashier GUI

Cashier-only POS interface based on the cashier GUI sketch. Reads menu items
from `MenuItem`, checks stock via `MenuItemInventory`/`Inventory`, and on
Checkout writes the `Orders` + `OrderItem` rows and decremejava -cp ".;postgresql-42.2.8.jar" CashierGUInts `Inventory`
in a single transaction. INV./REP. buttons are manager-only placeholders.

Build and run (fill in credentials in `dbSetup.java` first; on Mac/Linux
replace the `;` with a `:`):

> javac -cp ".:postgresql-42.2.8.jar" dbSetup.java CashierGUI.java

> java -cp ".:postgresql-42.2.8.jar" CashierGUI


## Manager GUI

> javac -cp ".:postgresql-42.2.8.jar" dbSetup.java InventoryDAO.java ManagerGUI.java

> java -cp ".:postgresql-42.2.8.jar" ManagerGUI