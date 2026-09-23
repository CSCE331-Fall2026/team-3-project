#include <iostream>
#include <random>


/*
 create a distribution of orders that could simulate realistic orders but not without edgecases (game days)

CREATE TABLE InventoryItem (
    ShipmentID INT PRIMARY KEY,
    InventoryID INT NOT NULL,
    WholesalePrice DECIMAL(10,2) NOT NULL,
    Quantity INT NOT NULL,
    ExpirationDate DATE NOT NULL,
    FOREIGN KEY (InventoryID) REFERENCES Inventory(InventoryID)
);

CREATE TABLE Inventory (
    InventoryID INT PRIMARY KEY,
    ItemName VARCHAR(50) NOT NULL,
    Minimum_Threshold INT NOT NULL DEFAULT 0,
    Quantity INT NOT NULL DEFAULT 0
);

CREATE TABLE MenuItem (
    MenuItemID INT PRIMARY KEY,
    MenuItemName VARCHAR(50) NOT NULL,
    Price DECIMAL(10,2) NOT NULL
);

CREATE TABLE MenuItemInventory (
    MenuItemID INT NOT NULL,
    InventoryID INT NOT NULL,
    QuantityRequired INT NOT NULL DEFAULT 1,
    PRIMARY KEY (MenuItemID, InventoryID),
    FOREIGN KEY (MenuItemID) REFERENCES MenuItem(MenuItemID),
    FOREIGN KEY (InventoryID) REFERENCES Inventory(InventoryID)
);

CREATE TABLE Orders (
    OrderID INT PRIMARY KEY,
    OrderDate DATE NOT NULL,
    OrderTime TIME NOT NULL,
    Total DECIMAL(10,2) NOT NULL
);

CREATE TABLE OrderItem (
    OrderItemID INT PRIMARY KEY,
    OrderID INT NOT NULL,
    MenuItemID INT NOT NULL,
    Qty INT NOT NULL,
    FOREIGN KEY (OrderID) REFERENCES Orders(OrderID),
    FOREIGN KEY (MenuItemID) REFERENCES MenuItem(MenuItemID)
);
*/
struct Order {



}
int main() {
  const char* entrees[] = {
    "Orange Chicken",
    "Kung Pao Chicken",
    ""
  } 




  return 0;
}
