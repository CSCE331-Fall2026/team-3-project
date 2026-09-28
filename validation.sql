/* query 1 - 52 weeks of sales history */

SELECT
   'Week ' || EXTRACT(WEEK FROM OrderDate) ||
   ' has ' || COUNT(*) || ' orders' AS Weeks_of_Sales_History
FROM Orders
GROUP BY EXTRACT(WEEK FROM OrderDate)
ORDER BY EXTRACT(WEEK FROM OrderDate);

/* query 2 - Orders fall within operating hours */

SELECT
    CASE 
        WHEN EXTRACT(HOUR FROM OrderTime) = 12 THEN '12pm'
        WHEN EXTRACT(HOUR FROM OrderTime) > 12 THEN (EXTRACT(HOUR FROM OrderTime) - 12) || 'pm'
        ELSE EXTRACT(HOUR FROM OrderTime) || 'am'
    END ||
    ' has ' || COUNT(*) || ' orders totaling $' || SUM(Total) AS Hourly_Sales
FROM Orders
GROUP BY EXTRACT(HOUR FROM OrderTime)
ORDER BY EXTRACT(HOUR FROM OrderTime);

/* query 3 - 2 peak days */

SELECT
    EXTRACT(DAY FROM OrderDate) || ' ' || TRIM(TO_CHAR(OrderDate, 'Month')) ||
    ' has $' || SUM(Total) || ' of sales' AS Peak_Days
FROM Orders
GROUP BY OrderDate
ORDER BY SUM(Total) DESC
LIMIT 10;

/* query 4 - Inventory items for 20 menu items */

SELECT
    MenuItemName || ' uses ' || COUNT(Inventory.InventoryID) || ' items' AS inventory_per_menu_item
FROM MenuItem
LEFT JOIN MenuItemInventory ON MenuItem.MenuItemID = MenuItemInventory.MenuItemID
LEFT JOIN Inventory ON MenuItemInventory.InventoryID = Inventory.InventoryID
GROUP BY MenuItemName
ORDER BY MenuItemName;

/* query 5 - Best of the Worst */

SELECT 
    OrderDate || ' was the slowest day, and the best selling item was ' ||
    MenuItemName || ' with ' || SUM(Qty) || ' sold' AS best_of_the_worst
FROM Orders
JOIN OrderItem ON Orders.OrderID = OrderItem.OrderID
JOIN MenuItem ON OrderItem.MenuItemID = MenuItem.MenuItemID
WHERE OrderDate = (
    SELECT OrderDate FROM Orders
    GROUP BY OrderDate
    ORDER BY SUM(Total) ASC
    LIMIT 1
)
GROUP BY OrderDate, MenuItemName
ORDER BY SUM(Qty) DESC
LIMIT 1;

/* query 6 - Query to see if the menu items are fulfillable with current inventory */

SELECT m.MenuItemID,
       m.MenuItemName,
       BOOL_AND(i.Quantity >= mi.QuantityRequired) AS fulfillable
FROM MenuItem m
JOIN MenuItemInventory mi ON m.MenuItemID = mi.MenuItemID
JOIN Inventory i ON mi.InventoryID = i.InventoryID
GROUP BY m.MenuItemID, m.MenuItemName
ORDER BY m.MenuItemName;

/* query 7 - Best and worst selling items */

SELECT MenuItemName, SUM(Qty)
FROM OrderItem
JOIN MenuItem ON OrderItem.MenuItemID = MenuItem.MenuItemID
GROUP BY MenuItemName
ORDER BY SUM(Qty) DESC;

/* query 8 - Checks to see if used ingredients are more than shipped ingredients */

SELECT Inventory.ItemName, r.first_ship, u.first_order, r.total_received, u.total_used
FROM Inventory
JOIN (SELECT InventoryID, MIN(ReceivedDate) AS first_ship, SUM(Quantity) AS total_received
      FROM InventoryItem
      GROUP BY InventoryID) r ON Inventory.InventoryID = r.InventoryID
JOIN (SELECT MenuItemInventory.InventoryID, MIN(Orders.OrderDate) AS first_order, SUM(OrderItem.Qty * MenuItemInventory.QuantityRequired) AS total_used
      FROM Orders
      JOIN OrderItem ON Orders.OrderID = OrderItem.OrderID
      JOIN MenuItemInventory ON OrderItem.MenuItemID = MenuItemInventory.MenuItemID
      GROUP BY MenuItemInventory.InventoryID) u ON Inventory.InventoryID = u.InventoryID
WHERE u.total_used > r.total_received
ORDER BY r.first_ship DESC;


