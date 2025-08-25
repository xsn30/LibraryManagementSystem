package com.example.qflslibrarymanagement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInit {
    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:library.db");
             Statement stmt = conn.createStatement()) {

            // 用户表
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id TEXT PRIMARY KEY," +
                    "name TEXT" +
                    ");");

            // 图书表
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS books (" +
                    "book_id TEXT PRIMARY KEY," +
                    "title TEXT," +
                    "author TEXT," +
                    "published_year INTEGER," +
                    "isbn TEXT," +
                    "genre TEXT," +
                    "status TEXT" +
                    ");");

            // 借阅记录表
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS loans (" +
                    "loan_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id TEXT," +
                    "book_id TEXT," +
                    "borrow_date TEXT," +
                    "return_date TEXT," +
                    "FOREIGN KEY(user_id) REFERENCES users(user_id)," +
                    "FOREIGN KEY(book_id) REFERENCES books(book_id)" +
                    ");");

            System.out.println("Database initialized successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}