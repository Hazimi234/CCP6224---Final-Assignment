package src.manager;

import java.sql.*;
import java.util.*;

public class MapManager {

    // Retrieves the status of all parking spots for the map visualization
    // Returns a List of Maps, where each Map represents one parking spot's data
    public List<Map<String, Object>> getMapSpots() {
        List<Map<String, Object>> list = new ArrayList<>();

        // SQL query to get ID, type, and the plate of the car (if any) currently in the spot
        // Ordered by spot_id so the grid draws in the correct sequence (F1... to F5...)
        String sql = "SELECT spot_id, spot_type, current_vehicle_plate FROM parking_spots ORDER BY spot_id";

        // Try-with-resources to automatically close the connection after query execution
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Create a small dictionary (Map) for this specific spot
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getString("spot_id"));
                map.put("type", rs.getString("spot_type"));

                // Get the plate number. If it is null or empty, the spot is free.
                String plate = rs.getString("current_vehicle_plate");
                map.put("plate", plate);

                // Boolean flag: True if a car is there, False if empty
                map.put("occupied", plate != null && !plate.isEmpty());

                list.add(map);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}