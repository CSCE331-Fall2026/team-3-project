#include <bits/stdc++.h>
#include <string>
using namespace std;

struct MenuItem {
    int id;
    string name;
    double price;
    string category;
};

struct InventoryItem {
    int id;
    string name;
    string unit;
    int quantity;
    int minimum_threshold;
    int shelf_life;
};

struct MenuItemInventoryRow {
    int menuItemID;
    int inventoryID;
    double quantityRequired;
};

static string trim_quotes(const string &s) {
    if(s.size() >= 2 && s.front() == '"' && s.back() == '"') {
        return s.substr(1,s.size()-2);
    }

    return s;
}

static vector<string> split_csv_line(const string &line) {
    vector<string> out;
    string cur;
    bool in_quotes = false;

    for (char c : line) {
        if (c == '"') {
            in_quotes = !in_quotes;
            cur += c;
        }
        else if (c == ',' && !in_quotes) {
            out.push_back(trim_quotes(cur));
            cur.clear();
        }
        else {
            cur += c;
        }
    }

    out.push_back(trim_quotes(cur));
    return out;
}

string sql_escape(const string &s) {
    string out;

    for (char c : s) {
        if (c == '\'')
            out += "''";
        else
            out += c;
    }

    return out;
}

string classify_item(const string &original_name) {
    string name = original_name;

    for (char &c : name)
        c = toupper((unsigned char)c);

    if (name.find("- CUB MEAL") != string::npos)
        return "EXCLUDE";

    if (name.find("CUB MEAL (") != string::npos)
        return "CUB_MEAL";

    if (name == "CHOW FUN" ||
        name == "CHOW MEIN" ||
        name == "FRIED RICE" ||
        name == "SUPER GREENS" ||
        name == "WHITE STEAMED RICE")
        return "SIDE";

    if (name.find("EGG ROLL") != string::npos ||
        name.find("POTSTICKER (") != string::npos ||
        name.find("CREAM CHEESE RANGOON") != string::npos ||
        name.find("SPRING ROLL") != string::npos)
        return "APPETIZER";

    if (name.find("SAUCE") != string::npos ||
        name == "HOT MUSTARD" ||
        name == "FORTUNE COOKIE" ||
        name == "TREE TOP APPLE CRISPS" ||
        name == "APPLE PIE ROLL")
        return "EXTRA";

    if (name.find("SOUP") != string::npos)
        return "SIDE";

    if (name.find("COCA-COLA") != string::npos ||
        name.find("COKE") != string::npos ||
        name.find("SPRITE") != string::npos ||
        name.find("FANTA") != string::npos ||
        name.find("DR PEPPER") != string::npos ||
        name.find("ROOT BEER") != string::npos ||
        name.find("TEA") != string::npos ||
        name.find("LEMONADE") != string::npos ||
        name.find("POWERADE") != string::npos ||
        name.find("GINGER ALE") != string::npos ||
        name.find("VITAMINWATER") != string::npos ||
        name.find("REFRESHER") != string::npos)
        return "DRINK";

    return "ENTREE";
}

double get_default_price(const string &category) {
    if (category == "ENTREE")
        return 6.50;

    if (category == "SIDE")
        return 5.00;

    if (category == "DRINK")
        return 2.75;

    if (category == "APPETIZER")
        return 3.00;

    if (category == "EXTRA")
        return 1.50;

    if (category == "CUB_MEAL")
        return 9.50;

    return 5.00;
}

vector<InventoryItem> load_inventory(const string &path) {
    ifstream f(path);

    if (!f) {
        cerr << "Failed to open inventory CSV: " << path << "\n";
        exit(1);
    }

    string header;
    getline(f, header);

    vector<InventoryItem> inventory;

    string line;
    int id = 1;

    while (getline(f, line)) {
        if (line.empty())
            continue;

        vector<string> fields = split_csv_line(line);

        if (fields.size() < 5)
            continue;

        InventoryItem item;

        item.id = stoi(fields[0]);
        item.name = fields[1];
        item.unit = fields[2];
        item.minimum_threshold = stoi(fields[3]);
        item.quantity = stoi(fields[4]);
        item.shelf_life = stoi(fields[5]);

        inventory.push_back(item);
    }

    return inventory;
}

vector<MenuItem> load_menu(const string &path) {
    ifstream f(path);

    if (!f) {
        cerr << "Failed to open menu CSV: " << path << "\n";
        exit(1);
    }

    string header;
    getline(f, header);

    vector<MenuItem> menu;

    string line;
    int id = 1;

    while (getline(f, line)) {
        if (line.empty())
            continue;

        vector<string> fields = split_csv_line(line);

        if (fields.empty())
            continue;

        string name = fields[0];

        string category = classify_item(name);

        if (category == "EXCLUDE")
            continue;

        MenuItem item;

        item.id = id++;
        item.name = name;
        item.category = category;
        item.price = std::stod(fields[1]);

        menu.push_back(item);
    }

    return menu;
}

vector<MenuItemInventoryRow> load_recipes(const string &path, const map<string, int> &menu_name_to_id, const map<string, int> &inventory_name_to_id) {
    ifstream f(path);

    if (!f) {
        cerr << "Failed to open recipes CSV: " << path << "\n";
        exit(1);
    }

    string header;
    getline(f, header);

    vector<MenuItemInventoryRow> rows;
    string line;

    while (getline(f, line)) {
        if (line.empty())
            continue;

        vector<string> fields = split_csv_line(line);

        if (fields.size() < 4)
            continue;

        string menu_name = fields[0];
        string inventory_name = fields[1];
        double quantity_required = stod(fields[2]);

        auto menu_it = menu_name_to_id.find(menu_name);
        auto inventory_it = inventory_name_to_id.find(inventory_name);

        if (menu_it == menu_name_to_id.end()) {
            cerr << "Menu item not found: "
                 << menu_name << "\n";
            continue;
        }

        if (inventory_it == inventory_name_to_id.end()) {
            cerr << "Inventory item not found: "
                 << inventory_name << "\n";
            continue;
        }

        MenuItemInventoryRow row;

        row.menuItemID = menu_it->second;
        row.inventoryID = inventory_it->second;
        row.quantityRequired = quantity_required;

        rows.push_back(row);
    }

    return rows;
}

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
    // CREATE TABLES
    cout << "CREATE TABLE Inventory ("
            "InventoryID INT PRIMARY KEY,"
            "ItemName VARCHAR(100) NOT NULL,"
            "Unit VARCHAR(20) NOT NULL,"
            "Minimum_Threshold DECIMAL(10,2) NOT NULL,"
            "Quantity DECIMAL(10,2) NOT NULL,"
            "ShelfLifeDays INT NOT NULL"
            ");\n\n";

    cout << "CREATE TABLE InventoryItem ("
            "ShipmentID INT PRIMARY KEY,"
            "InventoryID INT NOT NULL,"
            "WholesalePrice DECIMAL(10,2) NOT NULL,"
            "Quantity DECIMAL(10,2) NOT NULL,"
            "ReceivedDate DATE NOT NULL,"
            "ExpirationDate DATE NOT NULL,"
            "FOREIGN KEY (InventoryID) REFERENCES Inventory(InventoryID)"
            ");\n\n";

    cout << "CREATE TABLE MenuItem ("
            "MenuItemID INT PRIMARY KEY,"
            "MenuItemName VARCHAR(150) NOT NULL,"
            "Price DECIMAL(10,2) NOT NULL"
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

    cout << "BEGIN\n";
    // INVENTORY

    cout << "INSERT INTO Inventory "
            "(InventoryID, ItemName, Unit, Minimum_Threshold, Quantity, ShelfLifeDays) VALUES\n";

    for (size_t i = 0; i < inventory.size(); ++i) {
        cout << "("
            << inventory[i].id << ",'"
            << sql_escape(inventory[i].name) << "','"
            << sql_escape(inventory[i].unit) << "',"
            << inventory[i].minimum_threshold << ","
            << inventory[i].quantity << ","
            << inventory[i].shelf_life
            << ")";

        if (i + 1 < inventory.size())
            cout << ",\n";
        else
            cout << ";\n";
    }
    // MENUITEM

    cout << "INSERT INTO MenuItem "
            "(MenuItemID, MenuItemName, Price) VALUES\n";

    for (size_t i = 0; i < menu.size(); ++i) {

        cout << "("
             << menu[i].id
             << ",'"
             << sql_escape(menu[i].name)
             << "',"
             << fixed
             << setprecision(2)
             << menu[i].price
             << ")";

        if (i + 1 < menu.size())
            cout << ",\n";
        else
            cout << ";\n";
    }

    map<string, int> menu_name_to_id;
    map<string, int> inventory_name_to_id;

    for (const auto &item : menu) {
        menu_name_to_id[item.name] = item.id;
    }

    for (const auto &item : inventory) {
        inventory_name_to_id[item.name] = item.id;
    }
    vector<MenuItemInventoryRow> recipes = load_recipes(argv[3], menu_name_to_id, inventory_name_to_id);

    cout << "INSERT INTO MenuItemInventory "
            "(MenuItemID, InventoryID, QuantityRequired) VALUES\n";

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