Populate example database from db_database_demo:
> psql -h csce-315-db.engr.tamu.edu -U admin_shawna -d hardware_store_sthomas -f create_sales.sql

Update username and password in dbSetup.java file, build, move source file to private directory keeping only .class file
Remind students not to commit dbSetup.java to repository!
> javac dbSetup.java

Update the database name in jdbcSQL.java and jdbcGUI.java, update the sql query, build, and run
(for Mac, replace the ; with a :)
> javac jdbcSQL.java

> java -cp ".;postgresql-42.2.8.jar" jdbcSQL

> javac jdbcGUI.java

> java -cp ".;postgresql-42.2.8.jar" jdbcGUI

## Cashier GUI

Cashier-only POS interface based on the cashier GUI sketch. Reads menu items
from `MenuItem`, checks stock via `MenuItemInventory`/`Inventory`, and on
Checkout writes the `Orders` + `OrderItem` rows and decremejava -cp ".;postgresql-42.2.8.jar" CashierGUInts `Inventory`
in a single transaction. INV./REP. buttons are manager-only placeholders.

Build and run (fill in credentials in `dbSetup.java` first; on Mac/Linux
replace the `;` with a `:`):

> javac -cp ".;postgresql-42.2.8.jar" dbSetup.java CashierGUI.java

> java -cp ".;postgresql-42.2.8.jar" CashierGUI

