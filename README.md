
Shakir Islam - Project Manager

Madelyn Jaynes - Backend

Max Leal - Frontend

Vishanth Ramamurthy Yoganand - Frontend

JP Roque - Backend

inventoryItem allows for tracking specific shipments of each ingredient in inventory, including price, quantity, and expiration date. This allows for making sure that different wholesale orders, with different expiration dates, are handled accordingly. This information is mainly useful for managers rather than the cashier. It allows managers to track incoming inventory, monitor expiration, and compare costs over time. Keeping shipment information separate from the general `Inventory` table allows multiple batches of the same ingredient to be tracked separately, which is useful when items will expire at different times depending on when they came in.

inventory holds data on each ingredient type as a whole, including how much of that item is available and what the minimum threshold for that item is that determines when more needs to be ordered. This contains all the shipments of chicken, for example, as well as how low the supply of chicken needs to get before we order more. Although cashiers do not interact directly with this table in a normal transaction, inventory levels can be affected by the menu items they sell. For managers, this table is important because it supports reports about current inventory levels and restocking needs.

menuItem is for abstracting the joining of ingredients into one singular menuItem, which is used in an order item. It contains the intended price of the symphony of ingredients. For the cashier, this allows items to be selected quickly when creating an order without needing to interact directly with the individual inventory items. For managers, the table provides a consistent list of products that can be used in reports such as most popular items or looking to see which products are sold the most/least.

menuItemInventory is a junction table that allows multiple ingredients in the inventory to be combined into a single menuItem. For example, chicken and teriyaki sauce would both be in inventory, and menuItemInventory would allow them to be combined into teriyaki chicken in the menuItem table. It also supports manager reports by making it possible to track ingredient usage and estimate when ingredients will need to be reordered. It is not used by cashiers but supports the orders table.


The orderItem is labeled by the OrderItem ID and contains the information for the OrderId (order it is in), the MenuItem (food it is), and the Qty of that particular item (Qty). For the cashier, this table allows multiple menu items and quantities to be added to the same transaction. For managers, `OrderItem` provides detailed sales information that can be used to determine how many of each menu item were sold as well as to analyze sales patterns.

The order is pretty much the symphony of orderItems, where it contains the total price, along with all the orderItems in which is in the order. For the cashier, this table represents the final transaction created after the customer's items have been entered. For managers, it supports reports involving total sales, number of orders, revenue over a time period, and other sales trends.




