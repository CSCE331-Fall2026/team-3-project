
Shakir Islam - Project Manager

Madelyn Jaynes - Backend

Max Leal - Frontend

Vishanth Ramamurthy Yoganand - Frontend

JP Roque - Backend

# Database Design Justification
## InventoryItem
inventoryItem allows for tracking specific shipments of each ingredient in inventory, including price, quantity, and expiration date. This allows for making sure that different wholesale orders, with different expiration dates, are handled accordingly. This information is mainly useful for managers rather than the cashier. It allows managers to track incoming inventory, monitor expiration, and compare costs over time. Keeping shipment information separate from the general Inventory table allows multiple batches of the same ingredient to be tracked separately, which is useful when items will expire at different times depending on when they came in.

## Inventory
inventory holds data on each ingredient type as a whole, including how much of that item is available and what the minimum threshold for that item is that determines when more needs to be ordered. This contains all the shipments of chicken, for example, as well as how low the supply of chicken needs to get before we order more. Although cashiers do not interact directly with this table in a normal transaction, inventory levels can be affected by the menu items they sell. For managers, this table is important because it supports reports about current inventory levels and restocking needs.

## MenuItem
menuItem is for abstracting the joining of ingredients into one singular menuItem, which is used in an order item. It contains the intended price of the symphony of ingredients. For the cashier, this allows items to be selected quickly when creating an order without needing to interact directly with the individual inventory items. For managers, the table provides a consistent list of products that can be used in reports such as most popular items or looking to see which products are sold the most/least.

## MenuItemInventory
menuItemInventory is a junction table that allows multiple ingredients in the inventory to be combined into a single menuItem. For example, chicken and teriyaki sauce would both be in inventory, and menuItemInventory would allow them to be combined into teriyaki chicken in the menuItem table. It also supports manager reports by making it possible to track ingredient usage and estimate when ingredients will need to be reordered. It is not used by cashiers but supports the orders table.

## OrderItem
The orderItem is labeled by the OrderItem ID and contains the information for the OrderId (order it is in), the MenuItem (food it is), and the Qty of that particular item (Qty). For the cashier, this table allows multiple menu items and quantities to be added to the same transaction. For managers, OrderItem provides detailed sales information that can be used to determine how many of each menu item were sold as well as to analyze sales patterns.

## Orders
The order is pretty much the symphony of orderItems, where it contains the total price, along with all the orderItems in which is in the order. For the cashier, this table represents the final transaction created after the customer's items have been entered. For managers, it supports reports involving total sales, number of orders, revenue over a time period, and other sales trends.

## Data Generation
Run `g++ generate_base_data.cpp data_utils.cpp -o generate_base_data`\
`g++ generate_base_data.cpp data_utils.cpp -o generate_base_data` \
`g++ noise.cpp data_utils.cpp -o noise` \
`./generate_base_data MenuItems.csv Inventory.csv Recipes.csv > generated_data` \
`./noise MenuItems.csv Inventory.csv Recipes.csv 364 3 12345 >> generated_data` \
`generate_base_data.cpp` loads `Inventory`, `MenuItem`, and `MenuItemInventory` \
`noise.cpp` generates data for `Order`, `OrderItem`, and `InventoryItem` \
You should then run `psql -h csce-315-db.engr.tamu.edu -p 5432 -U team3 -d team3db -v ON_ERROR_STOP=1 -f generated_data` to enter the data into the database
You can run `psql -h csce-315-db.engr.tamu.edu -p 5432 -U team3 -d team3db -v ON_ERROR_STOP=1 -f drop_sales.sql` if you need to drop all tables from the database.

## Cashier GUI

Cashier-only POS interface based on the cashier GUI sketch. Reads menu items
from `MenuItem`, checks stock via `MenuItemInventory`/`Inventory`, and on
Checkout writes the `Orders` + `OrderItem` rows and decremejava -cp ".;postgresql-42.2.8.jar" CashierGUInts `Inventory`
in a single transaction. INV./REP. buttons are manager-only placeholders.

Build and run (fill in credentials in `dbSetup.java` first; on Mac/Linux
replace the `;` with a `:`):

Compile with:
> cd jdbc_swing_demo  
> javac -cp ".:postgresql-42.2.8.jar" dbSetup.java CashierGUI.java  
> cd ..  

Run with:
> cd jdbc_swing_demo  
> java -cp ".:postgresql-42.2.8.jar" CashierGUI  
> cd ..  

## Manager GUI

The Order History tab lists today's orders with their order ID, time, and total.
Click an order to open a popup showing item names and quantities. Refresh reloads
the list, and reopening the tab also loads the latest orders. Today follows the
database date used by cashier checkout.

`jdbc_swing_demo/OrderHistoryPage.java` builds the read-only history table and
item popup using the shared GUI theme. `jdbc_swing_demo/OrderHistoryDAO.java`
queries today's orders and each order's item quantities, including discontinued
menu items. Individual item prices are not displayed.

Compile with:
> cd jdbc_swing_demo  
> javac -cp ".:postgresql-42.2.8.jar" dbSetup.java InventoryDAO.java ManagerGUI.java  
> cd ..  

Run with:
> cd jdbc_swing_demo  
> java -cp ".:postgresql-42.2.8.jar" ManagerGUI  
> cd ..  

## Bowl, Plate, and Bigger Plate meals

The cashier opens on Meals. Bowl includes one side and one entree; Plate includes
one side and two entrees; Bigger Plate includes one side and three entrees.
Repeated entree choices are allowed. Soup is available a la carte, not as a meal
side. Prices start at $9.50, $11.00, and $12.50 in `MenuItems.csv`; these are project
prices, editable through the manager price editor. All entree choices are included
without premium surcharges. Existing a la carte items and Cub Meals remain available.

`Inventory.csv` contains Bowl Containers, Plate Containers, and Bigger Plate
Containers. `Recipes.csv` maps each meal to one container. Selected sides and
entrees consume their existing ingredient recipes in addition to the container.
The meal price is charged once, with the existing 8.25% sales tax.

Regenerate the schema as well as the data: `MenuItem` now has `EntreeCount`, and
`OrderItemSelection` records each meal's food choices separately from its priced
`OrderItem`. Order history shows those choices, and the product usage report
includes their ingredients. The noise generator creates matching meals and totals.
Old schema/data must be removed using your normal database reset process before
loading the generated SQL (`drop_sales.sql` includes the new table).

```bash
g++ -std=c++17 generate_base_data.cpp data_utils.cpp -o generate_base_data
g++ -std=c++17 noise.cpp data_utils.cpp -o noise
./generate_base_data MenuItems.csv Inventory.csv Recipes.csv > generated_data
./noise MenuItems.csv Inventory.csv Recipes.csv 364 3 12345 >> generated_data
# Load generated_data after resetting the database as usual.
cd jdbc_swing_demo
javac -cp ".:postgresql-42.2.8.jar" *.java
java -cp ".:postgresql-42.2.8.jar" CashierGUI
```
