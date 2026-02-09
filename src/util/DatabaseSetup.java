package src.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

public class DatabaseSetup {
    private static final String URL = "jdbc:sqlite:parking_lot.db";

    public static void createNewDatabase() {
        boolean isFirstRun = false; // Flag to track if we actually created new things

        String sqlVehicles = "CREATE TABLE IF NOT EXISTS vehicles (\n"
                + " license_plate TEXT PRIMARY KEY,\n"
                + " vehicle_type TEXT NOT NULL,\n" 
                + " is_vip INTEGER DEFAULT 0,\n"
                + " has_handicapped_card INTEGER DEFAULT 0\n"
                + ");";

        String sqlSpots = "CREATE TABLE IF NOT EXISTS parking_spots (\n"
                + " spot_id TEXT PRIMARY KEY,\n"
                + " floor_level INTEGER,\n"
                + " row_number INTEGER,\n"
                + " spot_number INTEGER,\n"
                + " spot_type TEXT,\n"           
                + " current_vehicle_plate TEXT,\n"
                + " hourly_rate REAL,\n"
                + " FOREIGN KEY (current_vehicle_plate) REFERENCES vehicles(license_plate)\n"
                + ");";

        String sqlTickets = "CREATE TABLE IF NOT EXISTS tickets (\n"
                + " ticket_id TEXT PRIMARY KEY,\n"
                + " license_plate TEXT,\n"
                + " spot_id TEXT,\n"
                + " entry_time_millis INTEGER,\n"
                + " exit_time_millis INTEGER,\n"
                + " parking_fee REAL DEFAULT 0.0,\n"
                + " is_paid INTEGER DEFAULT 0,\n"
                + " FOREIGN KEY (license_plate) REFERENCES vehicles(license_plate),\n"
                + " FOREIGN KEY (spot_id) REFERENCES parking_spots(spot_id)\n"
                + ");";

        String sqlFines = "CREATE TABLE IF NOT EXISTS fines (\n"
                + " fine_id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " license_plate TEXT,\n"
                + " amount REAL,\n"
                + " reason TEXT,\n"
                + " is_paid INTEGER DEFAULT 0,\n"
                + " FOREIGN KEY (license_plate) REFERENCES vehicles(license_plate)\n"
                + ");";

        // --- NEW: Settings Table to remember Fine Strategy ---
        String sqlSettings = "CREATE TABLE IF NOT EXISTS app_settings (\n"
                + " setting_key TEXT PRIMARY KEY,\n"
                + " setting_value TEXT\n"
                + ");";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(sqlVehicles);
            stmt.execute(sqlSpots);
            stmt.execute(sqlTickets);
            stmt.execute(sqlFines);
            stmt.execute(sqlSettings);
            
            // Check if we need to initialize spots (First Run Logic)
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM parking_spots");
            if (rs.next() && rs.getInt(1) == 0) {
                initializeSpots(stmt);
                isFirstRun = true;
            }

            // Check if we need to set default strategy
            ResultSet rsSet = stmt.executeQuery("SELECT count(*) FROM app_settings WHERE setting_key = 'fine_strategy'");
            if (rsSet.next() && rsSet.getInt(1) == 0) {
                stmt.execute("INSERT INTO app_settings (setting_key, setting_value) VALUES ('fine_strategy', 'Option A')");
            }

            if (isFirstRun) {
                System.out.println("System Setup Complete: Database initialized.");
            } else {
                System.out.println("System Ready: Connected to existing database.");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void initializeSpots(Statement stmt) throws Exception {
        System.out.println("Initializing 50 parking spots...");
        for (int floor = 1; floor <= 5; floor++) {
            for (int row = 1; row <= 2; row++) {
                for (int spot = 1; spot <= 5; spot++) {
                    String spotID = String.format("F%d-R%d-S%d", floor, row, spot);
                    String type = "Regular"; 
                    double rate = 5.0;

                    if (floor == 1) { type = "Compact"; rate = 2.0; }
                    else if (floor == 2) {
                        if (row == 1 && spot <= 3) { type = "Handicapped"; rate = 2.0; } 
                        else { type = "Regular"; rate = 5.0; }
                    }
                    else if (floor == 5) { type = "Reserved"; rate = 10.0; }
                    
                    String sql = String.format(
                        "INSERT INTO parking_spots (spot_id, floor_level, row_number, spot_number, spot_type, hourly_rate) " +
                        "VALUES ('%s', %d, %d, %d, '%s', %.2f)", 
                        spotID, floor, row, spot, type, rate
                    );
                    stmt.execute(sql);
                }
            }
        }
    }

    public static void main(String[] args) {
        createNewDatabase();
    }
}