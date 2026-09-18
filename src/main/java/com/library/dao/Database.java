package com.library.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Singleton: only one Database object exists. It reads the JDBC driver, URL and login from the
 * external file config/db.properties, so they can be changed without touching the code.
 */
public class Database {

    private static Database instance;

    private final String url;
    private final String user;
    private final String password;

    private Database() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("config/db.properties")) {
            props.load(in);
        } catch (IOException e) {
            System.out.println("Note: config/db.properties not found, using default settings.");
        }

        String driver = props.getProperty("db.driver", "org.h2.Driver");
        // -Ddb.url=... (or the --memory option) overrides the URL from the file
        url = System.getProperty("db.url", props.getProperty("db.url", "jdbc:h2:file:./data/librarydb"));
        user = props.getProperty("db.user", "sa");
        password = props.getProperty("db.password", "");

        try {
            Class.forName(driver);   // load the JDBC driver
            createTables();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("JDBC driver not found: " + driver + " (is the H2 jar on the classpath?)");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not set up the database: " + e.getMessage());
        }
    }

    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private void createTables() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS items ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, item_type VARCHAR(10) NOT NULL, "
                    + "title VARCHAR(200) NOT NULL, creator VARCHAR(150), extra VARCHAR(50), category VARCHAR(50))");
            st.execute("CREATE TABLE IF NOT EXISTS members ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, "
                    + "email VARCHAR(150) NOT NULL UNIQUE, member_type VARCHAR(10) NOT NULL)");
            // An item is "issued" while it has a loan whose return_date is still empty.
            st.execute("CREATE TABLE IF NOT EXISTS loans ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, item_id INT NOT NULL, member_id INT NOT NULL, "
                    + "issue_date DATE DEFAULT CURRENT_DATE NOT NULL, return_date DATE, fine DOUBLE DEFAULT 0, "
                    + "FOREIGN KEY (item_id) REFERENCES items(id), FOREIGN KEY (member_id) REFERENCES members(id))");
        }
    }
}
