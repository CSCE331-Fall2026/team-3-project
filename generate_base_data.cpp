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
    int quantity;
    int minimum_threshold;
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
        item.price = get_default_price(category);

        menu.push_back(item);
    }

    return menu;
}

int main(int argc, char **argv) {

    if (argc < 2) {
        cerr << "Usage: "
             << argv[0]
             << " menu.csv\n";

        return 1;
    }

    string menu_path = argv[1];

    vector<MenuItem> menu = load_menu(menu_path);

    //
    // MENUITEM
    //

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

    return 0;
}