#ifndef DATA_UTILS_H
#define DATA_UTILS_H

#include <string>
#include <vector>
#include <map>

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

struct Shipment {
    long long shipmentID;
    int inventoryID;
    double wholesalePrice;
    double quantity;
    string receivedDate;
    string expirationDate;
};

string trim_quotes(const string &s);
vector<string> split_csv_line(const string &line);
string sql_escape(const string &s);

string classify_item(const string &original_name);

vector<InventoryItem> load_inventory(const string &path);
vector<MenuItem> load_menu(const string &path);
vector<MenuItemInventoryRow> load_recipes(const string &path, const map<string, int> &menu_name_to_id, const map<string, int> &inventory_name_to_id);

string format_date(const tm &date);
tm add_days(tm date, int days);

#endif