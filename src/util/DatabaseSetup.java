package src.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

public class DatabaseSetup {
    private static final String URL = "jdbc:sqlite:parking_lot.db";

    public static void createNewDatabase() {
        // 1. VEHICLES TABLE
        // Matches PDF: Plate, Type, plus VIP status for Reserved spots
        String sqlVehicles = "CREATE TABLE IF NOT EXISTS vehicles (\n"
                + " license_plate TEXT PRIMARY KEY,\n"
                + " vehicle_type TEXT NOT NULL,\n" 
                + " is_vip INTEGER DEFAULT 0\n"    // 1 = VIP (Allowed in Reserved Spots)
                + ");";

        // 2. PARKING SPOTS TABLE
        // Matches PDF: Spot ID, Type, Status (via current_vehicle), Rate
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

        // 3. TICKETS TABLE 
        // Matches PDF: Entry/Exit time, Fee calculation
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

        // 4. FINES TABLE
        // Matches PDF: Fines linked to plate
        String sqlFines = "CREATE TABLE IF NOT EXISTS fines (\n"
                + " fine_id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " license_plate TEXT,\n"
                + " amount REAL,\n"
                + " reason TEXT,\n"
                + " is_paid INTEGER DEFAULT 0,\n"
                + " FOREIGN KEY (license_plate) REFERENCES vehicles(license_plate)\n"
                + ");";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(sqlVehicles);
            stmt.execute(sqlSpots);
            stmt.execute(sqlTickets);
            stmt.execute(sqlFines);
            
            System.out.println("Database tables created successfully.");
            
            // Check if spots exist. If 0, create the 50 spots.
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM parking_spots");
            if (rs.next() && rs.getInt(1) == 0) {
                initializeSpots(stmt);
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Generates 5 Floors x 2 Rows x 5 Spots = 50 Spots
    private static void initializeSpots(Statement stmt) throws Exception {
        System.out.println("Initializing 50 parking spots...");
        
        for (int floor = 1; floor <= 5; floor++) {
            for (int row = 1; row <= 2; row++) {
                for (int spot = 1; spot <= 5; spot++) {
                    
                    // ID Format: F1-R1-S1
                    String spotID = String.format("F%d-R%d-S%d", floor, row, spot);
                    
                    // Default values
                    String type = "Regular"; 
                    double rate = 5.0;

                    // Floor 1: Compact (Motorcycles)
                    if (floor == 1) { 
                        type = "Compact"; 
                        rate = 2.0; 
                    }
                    // Floor 2: Mixed (Handicapped & Regular)
                    else if (floor == 2) {
                        if (row == 1 && spot <= 3) { // First 3 spots are Handicapped
                            type = "Handicapped"; 
                            rate = 2.0; 
                        } else { 
                            type = "Regular"; 
                            rate = 5.0; 
                        }
                    }
                    // Floor 3 & 4: Regular (Standard Cars/SUVs)
                    else if (floor == 3 || floor == 4) {
                        type = "Regular";
                        rate = 5.0;
                    }
                    // Floor 5: Reserved (VIP Only)
                    else if (floor == 5) {
                        type = "Reserved";
                        rate = 10.0;
                    }
                    
                    String sql = String.format(
                        "INSERT INTO parking_spots (spot_id, floor_level, row_number, spot_number, spot_type, hourly_rate) " +
                        "VALUES ('%s', %d, %d, %d, '%s', %.2f)", 
                        spotID, floor, row, spot, type, rate
                    );
                    
                    stmt.execute(sql);
                }
            }
        }
        System.out.println("Spots initialized.");
    }

    public static void main(String[] args) {
        createNewDatabase();
    }
}