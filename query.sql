/* Example queries for Sales DB */  

/* get product code, description, quantity on hand, and minimum threshold for all products */
SELECT p_code, p_descript, v_code, p_qoh, p_min 
    FROM product;

/* show products whose quantity is below the minimum threshold and below 2 * minimum threshold */
SELECT p_code, p_descript, v_code, p_qoh, p_min 
    FROM product 
    WHERE p_qoh <= p_min;
SELECT p_code, p_descript, v_code, p_qoh, p_min 
    FROM product 
    WHERE p_qoh <= 2*p_min;

/* computing total inventory value for each product and for full inventory */
SELECT p_code, p_descript, v_code, p_qoh, p_price, p_qoh * p_price AS inventory_value 
    FROM product;
SELECT SUM(p_qoh * p_price) AS total_inventory_value 
    FROM product;

/* compute the average price of all products, select products above the average price */
SELECT AVG(p_price) AS average_price 
    FROM product;
SELECT p_code, p_descript, p_price 
    FROM product 
    WHERE p_price > (SELECT AVG(p_price) FROM product);

/* use join to display vendor name and not just code in product listing */
SELECT p_code, p_descript, v_code, p_qoh, p_min 
    FROM product;
SELECT p.p_code, p.p_descript, v.v_name, p.p_qoh, p.p_min 
    FROM product p 
    JOIN vendor v ON p.v_code = v.v_code 
    ORDER BY v.v_name;
  /* JOIN drops rows that have null values, need LEFT JOIN if want those */
SELECT p.p_code, p.p_descript, v.v_name, p.p_qoh, p.p_min 
    FROM product p 
    LEFT JOIN vendor v ON p.v_code = v.v_code 
    ORDER BY v.v_name;

/* for each vendor, compute the average number of products and average price, here we want to ingore null vendors so using JOIN */
/* examples of ordering by different columns */
SELECT v.v_code, v.v_name, COUNT(*) AS num_products, AVG(p.p_price) AS avg_price 
    FROM product p 
    JOIN vendor v ON p.v_code = v.v_code 
    GROUP BY v.v_code, v.v_name 
    ORDER BY v.v_name;
SELECT v.v_code, v.v_name, COUNT(*) AS num_products, AVG(p.p_price) AS avg_price 
    FROM product p 
    JOIN vendor v ON p.v_code = v.v_code 
    GROUP BY v.v_code, v.v_name 
    ORDER BY num_products DESC;
/* only show vendors with multiple products */
SELECT v.v_code, v.v_name, COUNT(*) AS num_products, AVG(p.p_price) AS avg_price 
    FROM product p 
    JOIN vendor v ON p.v_code = v.v_code 
    GROUP BY v.v_code, v.v_name 
    HAVING COUNT(*) > 1 
    ORDER BY num_products DESC;

/* show products whos price is higher than the vendor's average */
SELECT p.p_code, p.p_descript, v.v_name, p.p_price 
    FROM product p 
    JOIN vendor v ON p.v_code = v.v_code 
    ORDER BY v.v_name;
SELECT v.v_code, v.v_name, AVG(p.p_price) AS avg_price 
    FROM product p 
    JOIN vendor v ON p.v_code = v.v_code 
    GROUP BY v.v_code, v.v_name 
    ORDER BY v.v_name;
SELECT p.p_code, p.p_descript, v.v_name, p.p_price 
    FROM product p 
    JOIN vendor v ON p.v_code = v.v_code 
    WHERE p.p_price > (SELECT AVG(p2.p_price) FROM product p2 WHERE p2.v_code = p.v_code) 
    ORDER BY v.v_name;

/* use a view on product/vendor so don't have to recompute join every time */
CREATE VIEW vendor_avg_price AS 
    SELECT v.v_code, v.v_name, AVG(p.p_price) AS avg_price 
        FROM product p 
        JOIN vendor v ON p.v_code = v.v_code 
        GROUP BY v.v_code, v.v_name;
SELECT * 
    FROM vendor_avg_price 
    ORDER BY v_name;
SELECT p.p_code, p.p_descript, v.v_name, p.p_price, va.avg_price 
    FROM product p 
    JOIN vendor v ON p.v_code = v.v_code 
    JOIN vendor_avg_price va ON va.v_code = v.v_code 
    WHERE p.p_price > va.avg_price 
    ORDER BY v.v_name;

