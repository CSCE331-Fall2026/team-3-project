
Shakir Islam - Project Manager

Madelyn Jaynes - Backend

Max Leal - Frontend

Vishanth Ramamurthy Yoganand - Frontend

JP Roque - Backend

inventoryItem allows for tracking specific shipments of each ingredient in inventory, including price, quantity and expiration date. This allows for making sure that different wholesale orders, with different expiration dates are handled accordingly.

inventory holds data on each ingredient type as a whole, including how much of that item is available and what the minimum threshold for that item is that determines when more needs to be ordered. This contains all the shipments of chicken for example, as well as how low the supply of chicken needs to get before we order more.

menuItem is for abstracting the joining of ingredients into one singular menuItem which is used in order item. It contains the intended price of the symphony of ingredients.

menuItemInventory is a junction table that allows multiple ingredients in the inventory to be combined into a single menuItem. For example, chicken and teriyaki sauce would both be in inventory, and menuItemInventory would allow them to be comined into teriyaki chicken in the menuItem table.

The orderItem is labeled by the OrderItem Id and contains the information for the OrderId (order it is in), the MenuItem (food it is) and the Qty of that particular item (Qty)

The order is pretty much the symphony of orderItems where it contains the total price, along with all the orderItems in which is in the order.




