package com.example.proj.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBHandle {

    public static Connection connect() {
        String dbName = "SolarisCinema";

        String connectionUrl =
                "jdbc:sqlserver://localhost:1433;" +
                        "databaseName=" + dbName + ";" +
                        "integratedSecurity=true;" +
                        "encrypt=true;" +
                        "trustServerCertificate=true;";

        try {
            Connection conn = DriverManager.getConnection(connectionUrl);
            System.out.println("Successfully connected to SQL Server!");
            return conn;
        } catch (SQLException e) {
            System.out.println("Connection failed!");
            e.printStackTrace();
            return null;
        }
    }
}