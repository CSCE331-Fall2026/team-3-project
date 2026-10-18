#include <bits/stdc++.h>
#include <string>
#include "data_utils.h"

using namespace std;
using rng_t = std::mt19937_64;


// Ornstein-Uhlenbeck step
double ou_step(rng_t &rng, double x, double theta, double mu, double sigma, double dt) {
    normal_distribution<double> nd(0.0, 1.0);
    double dW = nd(rng) * sqrt(dt);
    return x + theta * (mu - x) * dt + sigma * dW;
}

int sample_category(rng_t &rng, const vector<int> &items) {
    if (items.empty()) {
        return -1;
    }

    return items[
        uniform_int_distribution<int>(0, items.size() - 1)(rng)
    ];
}

//Shipment helper functions
long long generate_shipment_id(const tm &date, int sequence) {
    long long year = date.tm_year + 1900;
    long long month = date.tm_mon + 1;
    long long day = date.tm_mday;

    long long date_number = year * 10000 + month * 100 + day;

    return date_number * 1000 + sequence;
}

double calculate_inventory_quantity(int inventoryID, const map<int, deque<Shipment>> &shipments) {
    auto it = shipments.find(inventoryID);

    if (it == shipments.end()) {
        return 0.0;
    }

    double total = 0.0;

    for (const Shipment &shipment : it->second) {
        total += shipment.quantity;
    }

    return total;
}

double get_total_inventory(int inventoryID, const map<int, double> &base_inventory, const map<int, deque<Shipment>> &shipments) {
    double total = 0.0;

    auto base_it = base_inventory.find(inventoryID);

    if (base_it != base_inventory.end()) {
        total += base_it->second;
    }

    total += calculate_inventory_quantity(inventoryID,shipments);

    return total;
}

double calculate_reorder_quantity(const InventoryItem &item, double current_quantity) {
    double target_quantity = item.quantity * 4;

    if (current_quantity >= target_quantity) {
        return 0.0;
    }

    return (target_quantity - current_quantity);
}

double generate_wholesale_price(rng_t &rng, const InventoryItem &item) {
    double base_price;

    if (item.unit == "lbs") {
        base_price = 2.50;
    }
    else if (item.unit == "oz") {
        base_price = 0.15;
    }
    else {
        base_price = 0.30;
    }

    normal_distribution<double> price_noise(1.0,0.05);

    double price = base_price * price_noise(rng);

    if (price < 0.01) {
        price = 0.01;
    }

    return price;
}

Shipment create_shipment(const InventoryItem &item, double quantity, const tm &received_date, int sequence, rng_t &rng) {
    Shipment shipment;

    shipment.shipmentID = generate_shipment_id(received_date, sequence);

    shipment.inventoryID = item.id;

    shipment.quantity = quantity;

    shipment.wholesalePrice = generate_wholesale_price(rng, item);

    shipment.receivedDate = format_date(received_date);

    tm expiration = add_days(received_date, item.shelf_life);

    shipment.expirationDate = format_date(expiration);

    return shipment;
}

void print_shipment_insert(const Shipment &shipment) {
    cout << "INSERT INTO InventoryItem "
            "(ShipmentID, InventoryID, WholesalePrice, Quantity, "
            "ReceivedDate, ExpirationDate) VALUES (";

    cout << shipment.shipmentID << ","
         << shipment.inventoryID << ","
         << fixed << setprecision(2)
         << shipment.wholesalePrice << ","
         << fixed << setprecision(3)
         << shipment.quantity << ",'"
         << shipment.receivedDate << "','"
         << shipment.expirationDate << "');\n";
}

void print_shipment_update(const Shipment &shipment) {
    cout << "UPDATE InventoryItem "
            "SET Quantity = "
         << fixed << setprecision(3)
         << shipment.quantity
         << " WHERE ShipmentID = "
         << shipment.shipmentID
         << ";\n";
}

void print_shipment_delete(long long shipmentID) {
    cout << "DELETE FROM InventoryItem "
            "WHERE ShipmentID = "
         << shipmentID
         << ";\n";
}

void print_inventory_update(int inventoryID, double quantity) {
    cout << "UPDATE Inventory "
            "SET Quantity = "
         << fixed << setprecision(3)
         << quantity
         << " WHERE InventoryID = "
         << inventoryID
         << " AND Active = TRUE;\n";
}

bool consume_inventory_fifo(int inventoryID, double amount, map<int, double> &base_inventory, map<int, deque<Shipment>> &shipments) {
    double available = get_total_inventory(inventoryID, base_inventory, shipments);

    if (available < amount) {
        cerr << "Not enough inventory for InventoryID "
             << inventoryID
             << ". Needed: " << amount
             << ", Available: " << available
             << "\n";

        return false;
    }

    // Consume original starting inventory first
    double &base_qty = base_inventory[inventoryID];

    if (base_qty > 0) {
        double used = min(base_qty, amount);

        base_qty -= used;
        amount -= used;
    }

    // Then consume Sunday shipment lots FIFO
    auto &queue = shipments[inventoryID];

    while (amount > 0.000001 && !queue.empty()) {
        Shipment &oldest = queue.front();

        if (oldest.quantity <= amount) {
            amount -= oldest.quantity;
            queue.pop_front();
        }
        else {
            oldest.quantity -= amount;
            amount = 0;
        }
    }

    return true;
}

bool consume_menu_item(int menuItemID, int qty, const map<int, vector<MenuItemInventoryRow>> &recipe_map, map<int, double> &base_inventory, map<int, deque<Shipment>> &shipments) {
    auto it = recipe_map.find(menuItemID);

    if (it == recipe_map.end()) {
        return true; // Items without a recipe consume no inventory.
    }

    // First check that every ingredient is available
    for (const auto &ingredient : it->second) {

        double amount = ingredient.quantityRequired * qty;

        double available = get_total_inventory(ingredient.inventoryID, base_inventory, shipments);

        if (available < amount) {
            cerr << "Not enough InventoryID "
                 << ingredient.inventoryID
                 << " for MenuItemID "
                 << menuItemID << "\n";

            return false;
        }
    }

    // Everything is available, now actually consume it
    for (const auto &ingredient : it->second) {

        double amount = ingredient.quantityRequired * qty;

        consume_inventory_fifo(ingredient.inventoryID, amount, base_inventory, shipments);
    }

    return true;
}

void add_shipment(const Shipment &shipment, map<int, deque<Shipment>> &shipments
) {
    shipments[shipment.inventoryID].push_back(shipment);
}

void restock_inventory(const tm &day, const vector<InventoryItem> &inventory, const map<int, double> &base_inventory, map<int, deque<Shipment>> &shipments, rng_t &rng) {
    if (day.tm_wday != 0) {
        return;
    }

    int sequence = 1;

    for (const InventoryItem &item : inventory) {

        double current = get_total_inventory(item.id, base_inventory, shipments);

        double quantity_to_order = calculate_reorder_quantity(item, current);

        if (quantity_to_order <= 0) { 
            continue;
        }

        Shipment shipment = create_shipment(item, quantity_to_order, day, sequence++, rng);
        
        add_shipment(shipment, shipments);
        
    }
}

map<int, vector<MenuItemInventoryRow>> build_recipe_map(const vector<MenuItemInventoryRow> &recipes) {
    map<int, vector<MenuItemInventoryRow>> recipe_map;

    for (const auto &row : recipes) {
        recipe_map[row.menuItemID].push_back(row);
    }

    return recipe_map;
}

void print_final_inventory_updates(const vector<InventoryItem> &inventory, const map<int, double> &base_inventory, const map<int, deque<Shipment>> &shipments) {
    cout << "UPDATE Inventory AS i "
            "SET Quantity = v.Quantity "
            "FROM (VALUES\n";

    for (size_t i = 0; i < inventory.size(); ++i) {

        double quantity = get_total_inventory(inventory[i].id, base_inventory, shipments);

        cout << "("
             << inventory[i].id << ","
             << fixed << setprecision(3)
             << quantity
             << ")";

        if (i + 1 < inventory.size())
            cout << ",\n";
        else
            cout << "\n";
    }

    cout << ") AS v(InventoryID, Quantity) "
            "WHERE i.InventoryID = v.InventoryID AND i.Active = TRUE;\n";
}

void print_final_shipment_updates(const map<int, deque<Shipment>> &shipments) {
    for (const auto &entry : shipments) {
        for (const Shipment &shipment : entry.second) {
            print_shipment_update(shipment);
        }
    }
}void print_final_shipments(
    const map<int, deque<Shipment>> &shipments
) {
    vector<Shipment> remaining;

    for (const auto &entry : shipments) {
        for (const Shipment &shipment : entry.second) {
            remaining.push_back(shipment);
        }
    }

    if (remaining.empty()) {
        return;
    }

    cout << "INSERT INTO InventoryItem "
            "(ShipmentID, InventoryID, WholesalePrice, Quantity, "
            "ReceivedDate, ExpirationDate) VALUES\n";

    for (size_t i = 0; i < remaining.size(); ++i) {

        const Shipment &shipment = remaining[i];

        cout << "("
             << shipment.shipmentID << ","
             << shipment.inventoryID << ","
             << fixed << setprecision(2)
             << shipment.wholesalePrice << ","
             << fixed << setprecision(3)
             << shipment.quantity << ",'"
             << shipment.receivedDate << "','"
             << shipment.expirationDate << "')";

        if (i + 1 < remaining.size()) {
            cout << ",\n";
        }
        else {
            cout << ";\n";
        }
    }
}



int main(int argc, char** argv) {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    if (argc < 4) {
        cerr << "Usage: " << argv[0]
            << " MenuItems.csv Inventory.csv Recipes.csv "
            << "[days=7] [peak_day=3] [seed]\n";
        return 1;
    }

    string menu_path = argv[1];
    string inventory_path = argv[2];
    string recipe_path = argv[3];

    int days = 7;
    if (argc >= 5) days = stoi(argv[4]);

    int peak_day = 3;
    if (argc >= 6) peak_day = stoi(argv[5]);

    uint64_t seed =
        (uint64_t)chrono::high_resolution_clock::now()
            .time_since_epoch().count();

    if (argc >= 7)
        seed = stoull(argv[6]);

    rng_t rng(seed);
    auto menu = load_menu(menu_path);

    auto inventory = load_inventory(inventory_path);

    map<string, int> menu_name_to_id;
    map<string, int> inventory_name_to_id;

    for (const auto &item : menu)
        menu_name_to_id[item.name] = item.id;

    for (const auto &item : inventory)
        inventory_name_to_id[item.name] = item.id;

    auto recipes = load_recipes(
        recipe_path,
        menu_name_to_id,
        inventory_name_to_id
    );

    auto recipe_map = build_recipe_map(recipes);

    map<int, deque<Shipment>> shipments;
    map<int, double> base_inventory;

    for (const auto &item : inventory) {
        base_inventory[item.id] = item.quantity;
    }

    vector<int> entrees;
    vector<int> sides;
    vector<int> drinks;
    vector<int> appetizers;
    vector<int> extras;
    vector<int> cubMeals;
    vector<int> meals;

    for (size_t i = 0; i < menu.size(); ++i) {
        if (menu[i].category == "Entrees")
            entrees.push_back(i);
        else if (menu[i].category == "Sides" && menu[i].name.find("SOUP") == string::npos)
            sides.push_back(i);
        else if (menu[i].category == "Drinks")
            drinks.push_back(i);
        else if (menu[i].category == "Appetizers")
            appetizers.push_back(i);
        else if (menu[i].category == "Extras")
            extras.push_back(i);
        else if (meal_entree_count(menu[i].name) > 0)
            meals.push_back(i);
        else if (menu[i].category == "Meals")
            cubMeals.push_back(i);
    }


    // Simulation parameters
    double base_lambda_per_min = 0.2; // baseline orders per minute (adjust to scale)
    // OU intensity parameters (minute units: dt = 1)
    double theta = 0.05;   // speed of reversion
    double mu = 0.0;       // we'll use multiplicative noise around 0 (log-space)
    double sigma = 0.3;    // volatility of log-intensity
    // diurnal hourly multipliers (24 entries) - lunch and dinner peaks
    vector<double> hour_mult(24, 0.5);
    for (int h = 6; h <= 9; ++h) hour_mult[h] = 0.6; // breakfast light
    for (int h = 11; h <= 13; ++h) hour_mult[h] = 1.6; // lunch peak
    for (int h = 16; h <= 19; ++h) hour_mult[h] = 1.8; // dinner peak
    for (int h = 20; h <= 22; ++h) hour_mult[h] = 1.0;
    // weekly multiplier: peak day gets boost
    double peak_multiplier = 1.5;
    double other_day_multiplier = 1.0;

    // operating hours
    int open_h = 10, close_h = 22; // 10:00 to 22:00

    // SQL generation bookkeeping
    long long order_id = 1000000;     // start high to avoid collisions
    long long order_item_id = 2000000;

    // get today's date as end date
    time_t tnow = time(nullptr);
    tm tm_now = *localtime(&tnow);
    // simulate previous 'days' days ending yesterday (so neat)
    tm_now.tm_mday -= 1;
    mktime(&tm_now); // normalize

    tm start_day = tm_now; //go from first day to now
    start_day.tm_mday -= days - 1;
    mktime(&start_day);
    // For each simulated day
    for (int d = 0; d < days; ++d) {
        // current day
        tm day = start_day;
        day.tm_mday += d;
        mktime(&day);
        int wday = day.tm_wday; // 0=Sun..6=Sat
        double weekly_mult = (wday == peak_day) ? peak_multiplier : other_day_multiplier;
        //check for resock

        restock_inventory(day, inventory, base_inventory, shipments, rng);
        // initialize OU log-noise for intensity per day
        double log_noise = 0.0;

        // per-minute loop
        vector<string> orders_batch;
        vector<string> order_items_batch;
        vector<string> selections_batch;

        orders_batch.reserve(500);
        order_items_batch.reserve(1000);

        for (int h = open_h; h < close_h; ++h) {
            for (int minute = 0; minute < 60; ++minute) {
                // advance OU with dt=1 minute (t in minutes)
                log_noise = ou_step(rng, log_noise, theta, mu, sigma, 1.0);

                // compute lambda for this minute
                double hourFactor = hour_mult[h];
                double lambda = base_lambda_per_min * hourFactor * weekly_mult * exp(log_noise);
                if (lambda < 1e-6) lambda = 1e-6;

                // sample number of arrivals in this minute
                poisson_distribution<int> pois(lambda);
                int arrivals = pois(rng);
                for (int a = 0; a < arrivals; ++a) {
                    // decide order time within the minute uniformly
                    int sec = uniform_int_distribution<int>(0,59)(rng);
                    // sample with replacement if possible
                    vector<int> chosen_idx;

                    // 8% chance of a Cub Meal
                    bool cub_order =
                        uniform_int_distribution<int>(1, 100)(rng) <= 8;

                    if (cub_order && !cubMeals.empty()) {

                        chosen_idx.push_back(
                            sample_category(rng, cubMeals)
                        );
                    } 
                    else {

                        int meal = sample_category(rng, meals);
                        if (meal == -1 || sides.empty() || entrees.empty()) continue;
                        chosen_idx.push_back(meal);
                        chosen_idx.push_back(sample_category(rng, sides));
                        for (int i = 0; i < meal_entree_count(menu[meal].name); ++i)
                            chosen_idx.push_back(sample_category(rng, entrees));
                        // DRINK

                        if (uniform_int_distribution<int>(1, 100)(rng) <= 40) {
                            int idx = sample_category(rng, drinks);

                            if (idx != -1)
                                chosen_idx.push_back(idx);
                        }
                        // APPETIZER

                        if (uniform_int_distribution<int>(1, 100)(rng) <= 12) {
                            int idx = sample_category(rng, appetizers);

                            if (idx != -1)
                                chosen_idx.push_back(idx);
                        }
                        // EXTRA / DESSERT

                        if (uniform_int_distribution<int>(1, 100)(rng) <= 7) {
                            int idx = sample_category(rng, extras);

                            if (idx != -1)
                                chosen_idx.push_back(idx);
                        }
                    }
                    map<int, int> selection_counts;
                    int meal_idx = -1;
                    if (!cub_order && !chosen_idx.empty() && meal_entree_count(menu[chosen_idx[0]].name) > 0) {
                        meal_idx = chosen_idx[0];
                        int count = 1 + meal_entree_count(menu[meal_idx].name);
                        for (int i = 1; i <= count; ++i) selection_counts[chosen_idx[i]]++;
                        chosen_idx.erase(chosen_idx.begin() + 1, chosen_idx.begin() + 1 + count);
                    }
                    map<int, int> item_counts;

                    for (int idx : chosen_idx) { // turns multiple orders of same entree into one menu item with quantity x
                        item_counts[idx]++;
                    }
                    if (item_counts.empty()) continue;

                    // Check the whole order before consuming any stock. Different
                    // menu items can share ingredients, so per-item checks are insufficient.
                    map<int, double> required;
                    map<int, int> consumed_counts = item_counts;
                    for (const auto &entry : selection_counts) consumed_counts[entry.first] += entry.second;
                    for (const auto &entry : consumed_counts) {
                        auto recipe = recipe_map.find(menu[entry.first].id);
                        if (recipe == recipe_map.end()) continue; // unmapped items use no stock
                        for (const auto &ingredient : recipe->second)
                            required[ingredient.inventoryID] += ingredient.quantityRequired * entry.second;
                    }
                    bool available = true;
                    for (const auto &ingredient : required) {
                        if (get_total_inventory(ingredient.first, base_inventory, shipments) < ingredient.second) {
                            available = false;
                            break;
                        }
                    }
                    if (!available) continue;
                    for (const auto &ingredient : required) {
                        if (!consume_inventory_fifo(ingredient.first, ingredient.second, base_inventory, shipments)) {
                            cerr << "Failed to consume validated order inventory\n";
                            return 1;
                        }
                    }

                    // Meal components are included in the meal price; match cashier sales tax.
                    double total = 0.0;
                    for (const auto &entry : item_counts)
                        total += menu[entry.first].price * entry.second;
                    total *= 1.0825;
                    // format date/time
                    char buf_date[32], buf_time[32];
                    snprintf(buf_date, sizeof(buf_date), "%04d-%02d-%02d", day.tm_year+1900, day.tm_mon+1, day.tm_mday);
                    snprintf(buf_time, sizeof(buf_time), "%02d:%02d:%02d", h, minute, sec);

                    // create Order and OrderItem inserts as strings
                    // Order: (OrderID, OrderDate, OrderTime, Total)
                    ostringstream oorder;
                    oorder << "(" << order_id << ",'" << buf_date << "','" << buf_time << "'," << fixed << setprecision(2) << total << ")";
                    orders_batch.push_back(oorder.str());

                    // Batch item rows after their parent order rows.
                    for (auto &entry : item_counts) {
                        int idx = entry.first;
                        int q = entry.second;

                        int mid = menu[idx].id;

                        if (idx == meal_idx) {
                            for (const auto &selection : selection_counts) {
                                ostringstream row;
                                row << "(" << order_item_id << "," << menu[selection.first].id
                                    << "," << selection.second << ")";
                                selections_batch.push_back(row.str());
                            }
                        }
                        ostringstream oitem;
                        oitem << "("
                            << order_item_id++ << ","
                            << order_id << ","
                            << mid << ","
                            << q << ")";

                        order_items_batch.push_back(oitem.str());
                    }

                    order_id++;
                } // arrivals
                // flush batches every so often
                if (!orders_batch.empty() && orders_batch.size() >= 500) {
                    cout << "INSERT INTO Orders (OrderID, OrderDate, OrderTime, Total) VALUES\n";

                    for (size_t i = 0; i < orders_batch.size(); ++i) {
                        cout << orders_batch[i]
                            << (i + 1 < orders_batch.size() ? ",\n" : ";\n");
                    }

                    cout << "INSERT INTO OrderItem "
                            "(OrderItemID, OrderID, MenuItemID, Qty) VALUES\n";

                    for (size_t i = 0; i < order_items_batch.size(); ++i) {
                        cout << order_items_batch[i]
                            << (i + 1 < order_items_batch.size() ? ",\n" : ";\n");
                    }

                    if (!selections_batch.empty()) {
                        cout << "INSERT INTO OrderItemSelection (OrderItemID, MenuItemID, Qty) VALUES\n";
                        for (size_t i = 0; i < selections_batch.size(); ++i)
                            cout << selections_batch[i] << (i + 1 < selections_batch.size() ? ",\n" : ";\n");
                        selections_batch.clear();
                    }
                    orders_batch.clear();
                    order_items_batch.clear();
                }
            } // minutes
        } // hours
        // flush remaining orders for day
        if (!orders_batch.empty()) {
            cout << "INSERT INTO Orders (OrderID, OrderDate, OrderTime, Total) VALUES\n";

            for (size_t i = 0; i < orders_batch.size(); ++i) {
                cout << orders_batch[i]
                    << (i + 1 < orders_batch.size() ? ",\n" : ";\n");
            }

            cout << "INSERT INTO OrderItem "
                    "(OrderItemID, OrderID, MenuItemID, Qty) VALUES\n";

            for (size_t i = 0; i < order_items_batch.size(); ++i) {
                cout << order_items_batch[i]
                    << (i + 1 < order_items_batch.size() ? ",\n" : ";\n");
            }

            if (!selections_batch.empty()) {
                cout << "INSERT INTO OrderItemSelection (OrderItemID, MenuItemID, Qty) VALUES\n";
                for (size_t i = 0; i < selections_batch.size(); ++i)
                    cout << selections_batch[i] << (i + 1 < selections_batch.size() ? ",\n" : ";\n");
                selections_batch.clear();
            }
            orders_batch.clear();
            order_items_batch.clear();
        }
    } // days

    print_final_shipments(shipments);

    print_final_inventory_updates(inventory, base_inventory, shipments);

    cout << "COMMIT;\n";
    return 0;
}
