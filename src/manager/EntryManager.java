package src.manager;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import src.model.Vehicle;

public class EntryManager {
    
    //check if vehicle is already parked
    public boolean isVehicleAlreadyParked(String plate) {
        String sql = "SELECT count(*) FROM parking_spots WHERE current_vehicle_plate = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plate);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //finds vehicle spots that is compatible with the vehicle
    public List<Vector<Object>> findMatchingSpots(Vehicle vehicle) {
        List<Vector<Object>> matches = new ArrayList<>();
        String sql = "SELECT * FROM parking_spots WHERE current_vehicle_plate IS NULL";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String spotType = rs.getString("spot_type");
                //if vehicle is allowed in the spot type
                if (vehicle.canFitInSpot(spotType)) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getString("spot_id"));
                    row.add(rs.getInt("floor_level"));
                    row.add(spotType);
                    row.add(rs.getDouble("hourly_rate"));
                    matches.add(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return matches;
    }

    //registers vehicle entry in the database
    public String parkVehicle(Vehicle vehicle, String spotID) throws SQLException {
        long entryTime = System.currentTimeMillis();
        String plate = vehicle.getLicensePlate();

        String sqlVehicle = "INSERT OR REPLACE INTO vehicles (license_plate, vehicle_type, is_vip, has_handicapped_card) VALUES (?, ?, ?, ?)";
        String sqlSpot = "UPDATE parking_spots SET current_vehicle_plate = ? WHERE spot_id = ?";
        String sqlTicket = "INSERT INTO tickets (ticket_id, license_plate, spot_id, entry_time_millis) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db")) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtV = conn.prepareStatement(sqlVehicle);
                 PreparedStatement pstmtS = conn.prepareStatement(sqlSpot);
                 PreparedStatement pstmtT = conn.prepareStatement(sqlTicket)) {

                // 1. Save Vehicle
                pstmtV.setString(1, plate);
                pstmtV.setString(2, vehicle.getVehicleType());
                pstmtV.setInt(3, vehicle.isVip() ? 1 : 0);
                pstmtV.setInt(4, vehicle.hasHandicappedCard() ? 1 : 0);
                pstmtV.executeUpdate();

                // 2. Update Spot
                pstmtS.setString(1, plate);
                pstmtS.setString(2, spotID);
                pstmtS.executeUpdate();

                // 3. Generate Ticket
                SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy-HHmmss");
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
                String ticketID = "T-" + plate + "-" + sdf.format(new Date(entryTime));

                pstmtT.setString(1, ticketID);
                pstmtT.setString(2, plate);
                pstmtT.setString(3, spotID);
                pstmtT.setLong(4, entryTime);
                pstmtT.executeUpdate();

                conn.commit();
                return ticketID;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}