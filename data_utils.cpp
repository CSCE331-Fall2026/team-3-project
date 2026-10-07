#include "data_utils.h"
#include <bits/stdc++.h>

using namespace std;

namespace {
[[noreturn]] void invalid_csv(const string &path, const string &message) {
    cerr << "Invalid CSV " << path << ": " << message << "\n";
    exit(1);
}

double nonnegative_number(const string &value, const string &path) {
    try {
        size_t end;
        double number = stod(value, &end);
        while (end < value.size() && isspace((unsigned char)value[end])) ++end;
        if (end != value.size() || !isfinite(number) || number < 0)
            invalid_csv(path, "invalid non-negative number: " + value);
        return number;
    } catch (const exception &) {
        invalid_csv(path, "invalid number: " + value);
    }
}

int nonnegative_integer(const string &value, const string &path) {
    double number = nonnegative_number(value, path);
    // InventoryItem currently stores whole units; never silently truncate fractions.
    if (number > numeric_limits<int>::max() || floor(number) != number)
        invalid_csv(path, "expected a whole number: " + value);
    return static_cast<int>(number);
}
}


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

    for (char &c : name) {
        c = toupper((unsigned char)c);
    }

    if (name.find("- CUB MEAL") != string::npos)
        return "EXCLUDE";
    if (name.find("CUB MEAL (") != string::npos)
        return "Meals";

    if (name == "CHOW FUN" ||
        name == "CHOW MEIN" ||
        name == "FRIED RICE" ||
        name == "SUPER GREENS" ||
        name == "WHITE STEAMED RICE")
        return "Sides";

    if (name.find("EGG ROLL") != string::npos ||
        name.find("POTSTICKER (") != string::npos ||
        name.find("CREAM CHEESE RANGOON") != string::npos ||
        name.find("SPRING ROLL") != string::npos)
        return "Appetizers";

    if (name.find("SAUCE") != string::npos ||
        name == "HOT MUSTARD" ||
        name == "FORTUNE COOKIE" ||
        name == "TREE TOP APPLE CRISPS" ||
        name == "APPLE PIE ROLL")
        return "Extras";

    if (name.find("SOUP") != string::npos)
        return "Sides";

    if (name.find("HI-C") != string::npos ||
        name.find("COCA-COLA") != string::npos ||
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
        return "Drinks";

    return "Entrees";
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
        if (line.find_first_not_of(" \t\r") == string::npos)
            continue;

        vector<string> fields = split_csv_line(line);

        if (fields.size() < 6)
            invalid_csv(path, "inventory row needs six fields");

        InventoryItem item;

        item.id = nonnegative_integer(fields[0], path);
        item.name = fields[1];
        item.unit = fields[2];
        item.minimum_threshold = nonnegative_integer(fields[3], path);
        item.quantity = nonnegative_integer(fields[4], path);
        item.shelf_life = nonnegative_integer(fields[5], path);

        inventory.push_back(item);
    }

    if (inventory.empty()) invalid_csv(path, "no inventory items");
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
        if (line.find_first_not_of(" \t\r") == string::npos)
            continue;

        vector<string> fields = split_csv_line(line);

        if (fields.size() < 2)
            invalid_csv(path, "menu row needs a name and price");

        string name = fields[0];

        string category = classify_item(name);

        if (category == "EXCLUDE")
            continue;

        MenuItem item;

        item.id = id++;
        item.name = name;
        item.category = category;
        item.price = nonnegative_number(fields[1], path);

        menu.push_back(item);
    }

    if (menu.empty()) invalid_csv(path, "no menu items");
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
        if (line.find_first_not_of(" \t\r") == string::npos)
            continue;

        vector<string> fields = split_csv_line(line);

        if (fields.size() < 4)
            invalid_csv(path, "recipe row needs four fields");

        string menu_name = fields[0];
        string inventory_name = fields[1];
        double quantity_required = nonnegative_number(fields[2], path);
        if (quantity_required <= 0)
            invalid_csv(path, "recipe quantities must be positive");

        auto menu_it = menu_name_to_id.find(menu_name);
        auto inventory_it = inventory_name_to_id.find(inventory_name);

        if (menu_it == menu_name_to_id.end()) {
            if (classify_item(menu_name) == "EXCLUDE") continue;
            invalid_csv(path, "menu item not found: " + menu_name);
        }

        if (inventory_it == inventory_name_to_id.end()) {
            invalid_csv(path, "inventory item not found: " + inventory_name);
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
