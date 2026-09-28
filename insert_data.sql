INSERT INTO Inventory (InventoryID, ItemName, Minimum_Threshold, Quantity)
VALUES (1, 'Chicken', 10, 1);

INSERT INTO Inventory (InventoryID, ItemName, Minimum_Threshold, Quantity)
VALUES (2, 'Beef', 15, 1);

INSERT INTO InventoryItem (ShipmentID, InventoryID, WholesalePrice, Quantity, ExpirationDate)
VALUES (1, 1, 5, 1, '2026-09-23');

INSERT INTO InventoryItem (ShipmentID, InventoryID, WholesalePrice, Quantity, ExpirationDate)
VALUES (1, 1, 5, 1, '2026-09-23');