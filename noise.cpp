#include <bits/stdc++.h>
#include <string>
using namespace std;
using rng_t = std::mt19937_64;

struct MenuItem {
    int id;
    string name;
    double price;
    double weight; // sampling weight
    string category;
};

static string trim_quotes(const string &s) {
    if (s.size() >= 2 && s.front() == '"' && s.back() == '"') return s.substr(1, s.size()-2);
    return s;
}

static vector<string> split_csv_line(const string &line) {
    vector<string> out;
    string cur;
    bool inq = false;
    for (size_t i = 0; i < line.size(); ++i) {
        char c = line[i];
        if (c == '"' ) {
            inq = !inq;
            cur += c; // keep quotes for later trimming
        } else if (c == ',' && !inq) {
            out.push_back(trim_quotes(cur));
            cur.clear();
        } else cur += c;
    }
    out.push_back(trim_quotes(cur));
    return out;
}

string classify_item(const string &original_name) {
    string name = original_name;

    for (char &c : name)
        c = toupper((unsigned char)c);

    // Individual Cub Meal nutrition portions are not separate POS items
    if (name.find("- CUB MEAL") != string::npos)
        return "EXCLUDE";

    // Full Cub Meal products
    if (name.find("CUB MEAL (") != string::npos)
        return "CUB_MEAL";

    // Sides
    if (name == "CHOW FUN" ||
        name == "CHOW MEIN" ||
        name == "FRIED RICE" ||
        name == "SUPER GREENS" ||
        name == "WHITE STEAMED RICE")
        return "SIDE";

    // Appetizers
    if (name.find("EGG ROLL") != string::npos ||
        name.find("POTSTICKER (") != string::npos ||
        name.find("CREAM CHEESE RANGOON") != string::npos ||
        name.find("SPRING ROLL") != string::npos)
        return "APPETIZER";

    // Extras / sauces / desserts
    if (name.find("SAUCE") != string::npos ||
        name == "HOT MUSTARD" ||
        name == "FORTUNE COOKIE" ||
        name == "TREE TOP APPLE CRISPS" ||
        name == "APPLE PIE ROLL")
        return "EXTRA";

    // Soup
    if (name.find("SOUP") != string::npos)
        return "SIDE";

    // Drinks
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

    // Everything remaining in this dataset is an entree
    return "ENTREE";
}

vector<MenuItem> load_menu(const string &path) {
    ifstream f(path);
    if (!f) {
        cerr << "Failed to open menu CSV: " << path << "\n";
        exit(1);
    }
    string header;
    getline(f, header);
    auto cols = split_csv_line(header);
    int price_col = -1;
    for (size_t i = 0; i < cols.size(); ++i) {
        string c = cols[i];
        for (auto &ch : c) ch = tolower((unsigned char)ch);
        if (c.find("price") != string::npos) price_col = (int)i;
    }
    vector<MenuItem> menu;

    string line;
    int id = 1;
    while (getline(f, line)) {
        if (line.empty()) continue;
        auto fields = split_csv_line(line);
        string name = fields.size() > 0 ? fields[0] : ("item" + to_string(id));
        double price = 5.0;
        if (price_col >= 0 && (size_t)price_col < fields.size()) {
            try { price = stod(fields[price_col]); } catch (...) { price = 5.0; }
        }
        MenuItem it;
        it.id = id++;
        it.name = name;
        it.price = price;
        it.category = classify_item(name);

        if (it.category == "EXCLUDE")
            continue;
        it.weight = max(0.1, 50.0 - it.price * 2.0);

        menu.push_back(it);
        }
        
    if (menu.empty()) {
        cerr << "No menu items parsed\n"; exit(1);
    }
    return menu;
}

// Ornstein-Uhlenbeck step
double ou_step(rng_t &rng, double x, double theta, double mu, double sigma, double dt) {
    normal_distribution<double> nd(0.0, 1.0);
    double dW = nd(rng) * sqrt(dt);
    return x + theta * (mu - x) * dt + sigma * dW;
}

// helper to sample index from weights
int sample_index(rng_t &rng, const vector<double> &weights) {
    discrete_distribution<int> dd(weights.begin(), weights.end());
    return dd(rng);
}

string sql_escape(const string &s) {
    string o; o.reserve(s.size());
    for (char c : s) {
        if (c == '\'') o += "''";
        else o += c;
    }
    return o;
}

int sample_category(rng_t &rng, const vector<int> &items) {
    if (items.empty())
        return -1;

    return items[
        uniform_int_distribution<int>(0, items.size() - 1)(rng)
    ];
}

int main(int argc, char** argv) {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    if (argc < 2) {
        cerr << "Usage: " << argv[0] << " menu.csv [days=7] [peak_day=3(Wed)] [seed]\n";
        return 1;
    }
    string menu_path = argv[1];
    int days = 7;
    if (argc >= 3) days = stoi(argv[2]);
    int peak_day = 3; // 0=Sun..6=Sat
    if (argc >= 4) peak_day = stoi(argv[3]);
    uint64_t seed = (uint64_t)chrono::high_resolution_clock::now().time_since_epoch().count();
    if (argc >= 5) seed = stoull(argv[4]);

    rng_t rng(seed);
    auto menu = load_menu(menu_path);

    vector<int> entrees;
    vector<int> sides;
    vector<int> drinks;
    vector<int> appetizers;
    vector<int> extras;
    vector<int> cubMeals;

    for (size_t i = 0; i < menu.size(); ++i) {
        if (menu[i].category == "ENTREE")
            entrees.push_back(i);
        else if (menu[i].category == "SIDE")
            sides.push_back(i);
        else if (menu[i].category == "DRINK")
            drinks.push_back(i);
        else if (menu[i].category == "APPETIZER")
            appetizers.push_back(i);
        else if (menu[i].category == "EXTRA")
            extras.push_back(i);
        else if (menu[i].category == "CUB_MEAL")
            cubMeals.push_back(i);
    }

    // normalize weights vector for sampling
    vector<double> weights;
    weights.reserve(menu.size());
    for (auto &m : menu) weights.push_back(m.weight);

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

    //cout << "BEGIN;\n";

    // For each simulated day
    for (int d = 0; d < days; ++d) {
        // current day
        tm day = tm_now;
        day.tm_mday -= d;
        mktime(&day);
        int wday = day.tm_wday; // 0=Sun..6=Sat
        double weekly_mult = (wday == peak_day) ? peak_multiplier : other_day_multiplier;

        // initialize OU log-noise for intensity per day
        double log_noise = 0.0;

        // per-minute loop
        vector<string> orders_batch;
        vector<string> order_items_batch;

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
                    // create order items: number of distinct items ~ 1 + Poisson(0.5)
                    int k = 1 + poisson_distribution<int>(0.5)(rng);
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

                        // SIDE
                        if (uniform_int_distribution<int>(1, 100)(rng) <= 95) {
                            int idx = sample_category(rng, sides);

                            if (idx != -1)
                                chosen_idx.push_back(idx);
                        }
                        // ENTREES

                        int entreeRoll =
                            uniform_int_distribution<int>(1, 100)(rng);

                        int numEntrees;

                        if (entreeRoll <= 45)
                            numEntrees = 1;
                        else if (entreeRoll <= 90)
                            numEntrees = 2;
                        else
                            numEntrees = 3;

                        for (int i = 0; i < numEntrees; ++i) {
                            int idx = sample_category(rng, entrees);

                            if (idx != -1)
                                chosen_idx.push_back(idx);
                        }
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
                    map<int, int> item_counts;

                    for (int idx : chosen_idx) { // turns multiple orders of same entree into one menu item with quantity x
                        item_counts[idx]++;
                    }
                    // compute order total with small per-order noise and occasional discounts
                    double total = 0.0;

                    for (auto &entry : item_counts) {
                        int idx = entry.first;
                        int qty = entry.second;

                        total += menu[idx].price * qty;
                    }
                    // apply small additive noise and occasional rounding / discount
                    normal_distribution<double> nd(0.0, 0.25);
                    total += nd(rng);
                    if (uniform_int_distribution<int>(0,999)(rng) < 10) { // 1% chance coupon
                        total *= 0.9;
                    }
                    if (total < 0.5) total = 0.5;
                    // format date/time
                    char buf_date[32], buf_time[32];
                    snprintf(buf_date, sizeof(buf_date), "%04d-%02d-%02d", day.tm_year+1900, day.tm_mon+1, day.tm_mday);
                    snprintf(buf_time, sizeof(buf_time), "%02d:%02d:%02d", h, minute, sec);

                    // create Order and OrderItem inserts as strings
                    // Order: (OrderID, OrderDate, OrderTime, Total)
                    ostringstream oorder;
                    oorder << "(" << order_id << ",'" << buf_date << "','" << buf_time << "'," << fixed << setprecision(2) << total << ")";
                    orders_batch.push_back(oorder.str());

                    // print OrderItem rows immediately grouped by Order later (we will batch separately)
                    // For convenience we'll also emit OrderItem rows now to separate container
                    // but to minimize memory we can stream; here we accumulate in strings for batched INSERTs
                    // Build OrderItem entries
                    // We'll collect them in a static vector printed after Orders to keep consistent IDs
                    // For simplicity, append to a global output stream below
                    // Create OrderItem inserts
                    for (auto &entry : item_counts) {
                        int idx = entry.first;
                        int q = entry.second;

                        int mid = menu[idx].id;

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
                            "(OrderItemID, OrderID, MenuItemID, MenuItemName, Qty) VALUES\n";

                    for (size_t i = 0; i < order_items_batch.size(); ++i) {
                        cout << order_items_batch[i]
                            << (i + 1 < order_items_batch.size() ? ",\n" : ";\n");
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

            orders_batch.clear();
            order_items_batch.clear();
        }
    } // days

    // cout << "COMMIT;\n";
    return 0;
}
