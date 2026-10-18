#include <bits/stdc++.h>
#include <string>
#include "data_utils.h"
using namespace std;

int main(int argc, char **argv) {

    if (argc < 4) {
        cerr << "Usage: "
            << argv[0]
            << " MenuItems.csv Inventory.csv Recipes.csv\n";
        return 1;
    }

    string menu_path = argv[1];

    string inventory_path = argv[2];

    vector<InventoryItem> inventory = load_inventory(inventory_path);

    vector<MenuItem> menu = load_menu(menu_path);
    map<string, int> menu_name_to_id;
    map<string, int> inventory_name_to_id;

    for (const auto &item : menu) {
        menu_name_to_id[item.name] = item.id;
    }

    for (const auto &item : inventory) {
        inventory_name_to_id[item.name] = item.id;
    }
    vector<MenuItemInventoryRow> recipes = load_recipes(argv[3], menu_name_to_id, inventory_name_to_id);

    // CREATE TABLES
    cout << "CREATE TABLE Inventory ("
            "InventoryID INT PRIMARY KEY,"
            "ItemName VARCHAR(100) NOT NULL,"
            "Unit VARCHAR(20) NOT NULL,"
            "Minimum_Threshold DECIMAL(10,2) NOT NULL,"
            "Quantity DECIMAL(10,2) NOT NULL,"
            "ShelfLifeDays INT NOT NULL,"
            "Active BOOLEAN NOT NULL DEFAULT TRUE"
            ");\n\n";

    cout << "CREATE TABLE InventoryItem ("
            "ShipmentID BIGINT PRIMARY KEY,"
            "InventoryID INT NOT NULL,"
            "WholesalePrice DECIMAL(10,2) NOT NULL,"
            "Quantity DECIMAL(10,2) NOT NULL,"
            "ReceivedDate DATE NOT NULL,"
            "ExpirationDate DATE NOT NULL,"
            "FOREIGN KEY (InventoryID) REFERENCES Inventory(InventoryID)"
            ");\n\n";

    cout << "CREATE TABLE MenuItem ("
            "MenuItemID INT PRIMARY KEY,"
            "Category VARCHAR(20) NOT NULL,"
            "MenuItemName VARCHAR(150) NOT NULL,"
            "Price DECIMAL(10,2) NOT NULL,"
            "EntreeCount INT NOT NULL DEFAULT 0 CHECK (EntreeCount BETWEEN 0 AND 3),"
            "Active BOOLEAN NOT NULL DEFAULT TRUE"
            ");\n\n";

    cout << "CREATE TABLE MenuItemInventory ("
            "MenuItemID INT NOT NULL,"
            "InventoryID INT NOT NULL,"
            "QuantityRequired DECIMAL(10,3) NOT NULL,"
            "PRIMARY KEY (MenuItemID, InventoryID),"
            "FOREIGN KEY (MenuItemID) REFERENCES MenuItem(MenuItemID),"
            "FOREIGN KEY (InventoryID) REFERENCES Inventory(InventoryID)"
            ");\n\n";

    cout << "CREATE TABLE Orders ("
            "OrderID INT PRIMARY KEY,"
            "OrderDate DATE NOT NULL,"
            "OrderTime TIME NOT NULL,"
            "Total DECIMAL(10,2) NOT NULL"
            ");\n\n";

    cout << "CREATE TABLE OrderItem ("
            "OrderItemID INT PRIMARY KEY,"
            "OrderID INT NOT NULL,"
            "MenuItemID INT NOT NULL,"
            "Qty INT NOT NULL,"
            "FOREIGN KEY (OrderID) REFERENCES Orders(OrderID),"
            "FOREIGN KEY (MenuItemID) REFERENCES MenuItem(MenuItemID)"
            ");\n\n";

    cout << "CREATE TABLE OrderItemSelection ("
            "OrderItemID INT NOT NULL REFERENCES OrderItem(OrderItemID) ON DELETE CASCADE,"
            "MenuItemID INT NOT NULL REFERENCES MenuItem(MenuItemID),"
            "Qty INT NOT NULL CHECK (Qty > 0),"
            "PRIMARY KEY (OrderItemID, MenuItemID)"
            ");\n\n";

    cout << "BEGIN;\n";
    // INVENTORY

    cout << "INSERT INTO Inventory "
            "(InventoryID, ItemName, Unit, Minimum_Threshold, Quantity, ShelfLifeDays, Active) VALUES\n";

    for (size_t i = 0; i < inventory.size(); ++i) {
        cout << "("
            << inventory[i].id << ",'"
            << sql_escape(inventory[i].name) << "','"
            << sql_escape(inventory[i].unit) << "',"
            << inventory[i].minimum_threshold << ","
            << inventory[i].quantity << ","
            << inventory[i].shelf_life
            << ",TRUE)";

        if (i + 1 < inventory.size())
            cout << ",\n";
        else
            cout << ";\n";
    }
    // MENUITEM

    cout << "INSERT INTO MenuItem "
            "(MenuItemID, MenuItemName, Price, Category, EntreeCount, Active) VALUES\n";

    for (size_t i = 0; i < menu.size(); ++i) {
        cout << "("
            << menu[i].id
            << ",'"
            << sql_escape(menu[i].name)
            << "',"
            << fixed
            << setprecision(2)
            << menu[i].price
            << ",'"
            << sql_escape(menu[i].category)
            << "'," << meal_entree_count(menu[i].name) << ",TRUE"
            << ")";

        if (i + 1 < menu.size())
            cout << ",\n";
        else
            cout << ";\n";
    }

    if (!recipes.empty()) {
        cout << "INSERT INTO MenuItemInventory "
                "(MenuItemID, InventoryID, QuantityRequired) VALUES\n";
    }

    for (size_t i = 0; i < recipes.size(); ++i) {
        cout << "("
            << recipes[i].menuItemID << ","
            << recipes[i].inventoryID << ","
            << fixed << setprecision(3)
            << recipes[i].quantityRequired
            << ")";

        if (i + 1 < recipes.size())
            cout << ",\n";
        else
            cout << ";\n";
    }
    return 0;
}