Populate example database from db_database_demo:
> psql -h csce-315-db.engr.tamu.edu -U admin_shawna -d hardware_store_sthomas -f create_sales.sql

Update username and password in dbSetup.java file, build, move source file to private directory keeping only .class file
Remind students not to commit dbSetup.java to repository!
> javac dbSetup.java

Update the database name in jdbcSQL.java and jdbcGUI.java, update the sql query, build, and run
(for Mac, replace the ; with a :)
> javac jdbcSQL.java

> java -cp ".;postgresql-42.2.8.jar" jdbcSQL

> javac jdbcGUI.java

> java -cp ".;postgresql-42.2.8.jar" jdbcGUI

