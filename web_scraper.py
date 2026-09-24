from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import csv

def scrape_website(url):
    chrome_options = Options()
    # chrome_options.add_argument("--headless")
    chrome_options.add_argument("--no-sandbox")
    chrome_options.add_argument("--disable-dev-shm-usage")

    driver = webdriver.Chrome(options=chrome_options)
    driver.get(url)

    try:
        # Wait up to 15 seconds for nutrition rows to appear
        WebDriverWait(driver, 15).until(
            EC.presence_of_all_elements_located(
                (By.CSS_SELECTOR, "tr.row-nutrition")
            )
        )
    except:
        print("Could not find nutrition rows.")
        print(driver.title)
        driver.quit()
        return

    rows = driver.find_elements(By.CSS_SELECTOR, "tr.row-nutrition")

    print("Rows found:", len(rows))

    scraped_data = []

    for row in rows:
        cells = row.find_elements(By.TAG_NAME, "td")

        if len(cells) == 0:
            continue

        values = [cell.text.strip() for cell in cells]

        print(values)

        scraped_data.append(values)

    driver.quit()

    with open("results.csv", "w", newline="", encoding="utf-8") as file:
        writer = csv.writer(file)

        writer.writerow([
            "Menu Item",
            "Portion",
            "Calories",
            "Calories From Fat",
            "Total Fat",
            "Saturated Fat",
            "Trans Fat",
            "Cholesterol",
            "Sodium",
            "Total Carbs",
            "Dietary Fiber",
            "Sugars",
            "Protein"
        ])

        writer.writerows(scraped_data)


if __name__ == "__main__":
    target_url = "https://www.pandaexpress.com/nutritioninformation"
    scrape_website(target_url)