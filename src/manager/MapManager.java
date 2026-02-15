package src.manager;

import java.sql.*;
import java.util.*;

public class MapManager {

    public List<Map<String, Object>> getMapSpots() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT spot_id, spot_type, current_vehicle_plate FROM parking_spots ORDER BY spot_id";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getString("spot_id"));
                map.put("type", rs.getString("spot_type"));
                String plate = rs.getString("current_vehicle_plate");
                map.put("plate", plate);
                map.put("occupied", plate != null && !plate.isEmpty());
                list.add(map);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}