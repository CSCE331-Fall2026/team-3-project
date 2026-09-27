#include "data_utils.h"
#include <bits/stdc++.h>

using namespace std;

string trim_quotes(const string &s) {
    if(s.size() >= 2 && s.front() == '"' && s.back() == '"') {
        return s.substr(1,s.size()-2);
    }

    return s;
}

vector<string> split_csv_line(const string &line) {
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

    while (getline(f, line)) {
        if (line.empty())
            continue;

        vector<string> fields = split_csv_line(line);

        if (fields.size() < 6)
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

string format_date(const tm &date) {
    char buffer[20];

    strftime(
        buffer,
        sizeof(buffer),
        "%Y-%m-%d",
        &date
    );

    return string(buffer);
}

tm add_days(tm date, int days) {
    date.tm_mday += days;
    mktime(&date);

    return date;
}