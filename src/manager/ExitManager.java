package src.manager;

import java.sql.*;
import java.util.*;
import src.model.BillData;

public class ExitManager {

    public BillData calculateBill(String plate) throws SQLException {
        BillData data = new BillData();
        data.found = false;
        
        // Temp variables
        String spotType = "";
        String vType = "";
        boolean isVip = false;
        boolean hasCard = false;
        double rate = 0.0;
        long entryTime = 0;

        String sql = "SELECT t.ticket_id, t.entry_time_millis, t.spot_id, s.spot_type, s.hourly_rate, v.vehicle_type, v.is_vip, v.has_handicapped_card " +
                     "FROM tickets t " +
                     "JOIN parking_spots s ON t.spot_id = s.spot_id " +
                     "JOIN vehicles v ON t.license_plate = v.license_plate " +
                     "WHERE t.license_plate = ? AND t.exit_time_millis IS NULL";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plate);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    data.found = true;
                    data.ticketID = rs.getString("ticket_id");
                    data.spotID = rs.getString("spot_id");
                    entryTime = rs.getLong("entry_time_millis");
                    spotType = rs.getString("spot_type");
                    rate = rs.getDouble("hourly_rate");
                    vType = rs.getString("vehicle_type");
                    isVip = rs.getInt("is_vip") == 1;
                    hasCard = rs.getInt("has_handicapped_card") == 1;
                }
            }
            
            if (data.found) {
                long exitTime = System.currentTimeMillis();
                double hours = Math.ceil((exitTime - entryTime) / (1000.0 * 60 * 60));
                if (hours == 0) hours = 1;

                // Rate Logic
                double finalRate = rate;
                if (vType.equals("Handicapped Vehicle") && hasCard) {
                    if (spotType.equals("Compact") || spotType.equals("Handicapped")) finalRate = 0.0;
                    else if (spotType.equals("Regular")) finalRate = 3.0;
                }
                double parkingFee = hours * finalRate;
                double subTotal = parkingFee;

                StringBuilder sb = new StringBuilder();
                sb.append("Ticket ID:  ").append(data.ticketID).append("\n");
                sb.append("Plate No:   ").append(plate).append("\n");
                sb.append("Duration:   ").append((int)hours).append(" hours\n");
                sb.append("Parking Fee: RM ").append(String.format("%.2f", parkingFee)).append("\n");

                // Overstay Fine
                if (hours > 24) {
                    double fine = AdminManager.currentFineStrategy.calculateFine(hours);
                    if (fine > 0) {
                        sb.append("OVERSTAY FINE: RM ").append(String.format("%.2f", fine)).append("\n");
                        addInstantFine(conn, plate, fine, "Overstay Fine (" + (int)hours + "h)");
                        subTotal += fine;
                    }
                }

                // Violation Fine
                if (spotType.equals("Reserved") && !isVip) {
                    sb.append("VIOLATION FINE: RM 50.00\n");
                    addInstantFine(conn, plate, 50.0, "Misuse of Reserved Spot");
                    subTotal += 50.0;
                }

                // Unpaid Fines
                try (PreparedStatement psFines = conn.prepareStatement("SELECT SUM(amount) FROM fines WHERE license_plate = ? AND is_paid = 0")) {
                    psFines.setString(1, plate);
                    try (ResultSet rsFines = psFines.executeQuery()) {
                        if (rsFines.next()) {
                            double oldFines = rsFines.getDouble(1);
                            if (oldFines > 0) {
                                sb.append("Unpaid Fines: RM ").append(String.format("%.2f", oldFines)).append("\n");
                                subTotal += oldFines;
                            }
                        }
                    }
                }

                sb.append("----------------------------------\n");
                sb.append("TOTAL DUE:  RM ").append(String.format("%.2f", subTotal)).append("\n");
                
                data.totalAmount = subTotal;
                data.billText = sb.toString();
            }
        }
        return data;
    }

    private void addInstantFine(Connection conn, String plate, double amount, String reason) throws SQLException {
        // Check duplicate
        try (PreparedStatement check = conn.prepareStatement("SELECT count(*) FROM fines WHERE license_plate = ? AND is_paid = 0 AND reason LIKE ?")) {
            check.setString(1, plate);
            check.setString(2, reason.split("\\(")[0].trim() + "%");
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return;
            }
        }
        // Insert
        try (PreparedStatement insert = conn.prepareStatement("INSERT INTO fines (license_plate, amount, reason, is_paid) VALUES (?, ?, ?, 0)")) {
            insert.setString(1, plate);
            insert.setDouble(2, amount);
            insert.setString(3, reason);
            insert.executeUpdate();
        }
    }

    public void processPayment(String ticketID, String spotID, String plate, double amount, String method) throws SQLException {
        long exitTime = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db")) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstT = conn.prepareStatement("UPDATE tickets SET exit_time_millis = ?, parking_fee = ?, is_paid = 1, payment_method = ? WHERE ticket_id = ?");
                 PreparedStatement pstS = conn.prepareStatement("UPDATE parking_spots SET current_vehicle_plate = NULL WHERE spot_id = ?");
                 PreparedStatement pstF = conn.prepareStatement("UPDATE fines SET is_paid = 1, payment_method = ? WHERE license_plate = ? AND is_paid = 0")) {

                pstT.setLong(1, exitTime);
                pstT.setDouble(2, amount);
                pstT.setString(3, method);
                pstT.setString(4, ticketID);
                pstT.executeUpdate();

                pstS.setString(1, spotID);
                pstS.executeUpdate();

                pstF.setString(1, method);
                pstF.setString(2, plate);
                pstF.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}