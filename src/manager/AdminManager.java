package src.manager;

import java.sql.*;
import java.util.*;
import src.model.strategy.*;

public class AdminManager {
    // Default if no setting found
    public static FineStrategy currentFineStrategy = new FixedFineStrategy();

    // Loads the current strategy from DB (Option A/B/C)
    public String loadSavedStrategy() {
        String strategyName = "Option A";
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = 'fine_strategy'";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                String savedOpt = rs.getString("setting_value");
                if (savedOpt.equals("Option B")) {
                    currentFineStrategy = new ProgressiveFineStrategy();
                    strategyName = "Option B";
                } else if (savedOpt.equals("Option C")) {
                    currentFineStrategy = new HourlyFineStrategy();
                    strategyName = "Option C";
                } else {
                    currentFineStrategy = new FixedFineStrategy();
                    strategyName = "Option A";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return strategyName;
    }

    // Saves new strategy and UPDATES existing fines immediately
    public void saveStrategy(String val) {
        if (val.contains("Option B"))
            currentFineStrategy = new ProgressiveFineStrategy();
        else if (val.contains("Option C"))
            currentFineStrategy = new HourlyFineStrategy();
        else
            currentFineStrategy = new FixedFineStrategy();

        String sql = "UPDATE app_settings SET setting_value = ? WHERE setting_key = 'fine_strategy'";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, val);
            pstmt.executeUpdate();

            // Trigger a scan so all currently parked cars get their fines updated
            // to match the new strategy immediately
            runComplianceScan();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Calculates Total Revenue for the Dashboard
    public double getTotalRevenue() {
        // SUM of (Paid Ticket Fees) + (Paid Fine Amounts)
        // Uses 'paid_amount' so partial payments count towards revenue
        String sql = "SELECT (SELECT IFNULL(SUM(parking_fee), 0) FROM tickets WHERE is_paid=1) + (SELECT IFNULL(SUM(paid_amount), 0) FROM fines)";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // Gets data for the Dashboard Table (Live Status)
    public List<Object[]> getLiveVehicles() {
        List<Object[]> list = new ArrayList<>();
        // Shows how much debt (amount - paid_amount) each parked car currently has
        String sql = "SELECT s.spot_id, s.current_vehicle_plate, v.vehicle_type, v.is_vip, " +
                "(SELECT IFNULL(SUM(amount - paid_amount), 0) FROM fines f WHERE f.license_plate = s.current_vehicle_plate AND f.is_paid = 0) as db_fines "
                +
                "FROM parking_spots s " +
                "JOIN vehicles v ON s.current_vehicle_plate = v.license_plate " +
                "WHERE s.current_vehicle_plate IS NOT NULL " +
                "ORDER BY s.spot_id";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[] {
                        rs.getString("spot_id"),
                        rs.getString("current_vehicle_plate"),
                        rs.getString("vehicle_type"),
                        rs.getInt("is_vip") == 1 ? "YES" : "NO",
                        "Parked",
                        String.format("RM %.2f", rs.getDouble("db_fines"))
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Scans for Overstay/Misuse and adds/updates fines
    public void runComplianceScan() {
        String sql = "SELECT s.spot_type, s.current_vehicle_plate, v.is_vip, t.entry_time_millis, t.ticket_id " +
                "FROM parking_spots s " +
                "JOIN vehicles v ON s.current_vehicle_plate = v.license_plate " +
                "JOIN tickets t ON s.current_vehicle_plate = t.license_plate " +
                "WHERE s.current_vehicle_plate IS NOT NULL AND t.exit_time_millis IS NULL";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            long now = System.currentTimeMillis();
            while (rs.next()) {
                String plate = rs.getString("current_vehicle_plate");
                String spotType = rs.getString("spot_type");
                boolean isVip = rs.getInt("is_vip") == 1;
                long entryTime = rs.getLong("entry_time_millis");
                String ticketID = rs.getString("ticket_id");

                // 1. Reserved Spot Check
                if ("Reserved".equalsIgnoreCase(spotType) && !isVip) {
                    updateOrInsertFine(conn, plate, 50.0, "Misuse of Reserved Spot", ticketID);
                }

                // 2. Overstay Check
                double hours = Math.ceil((now - entryTime) / (1000.0 * 60 * 60));
                if (hours > 24 && currentFineStrategy != null) {
                    double fine = currentFineStrategy.calculateFine(hours);
                    if (fine > 0) {
                        updateOrInsertFine(conn, plate, fine, "Overstay Fine", ticketID);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Updates fine amount if strategy changed, or inserts new fine if none exists
    private void updateOrInsertFine(Connection conn, String plate, double amount, String reasonPrefix, String ticketID)
            throws SQLException {
        // Use tagged reason to identify THIS session's fine
        String taggedReason = reasonPrefix + " [" + ticketID + "]";
        String checkSql = "SELECT fine_id, amount FROM fines WHERE license_plate = ? AND is_paid = 0 AND reason = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setString(1, plate);
            pstmt.setString(2, taggedReason);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // If fine exists but amount is different (Admin changed strategy), update it
                double currentDbAmount = rs.getDouble("amount");
                if (Math.abs(currentDbAmount - amount) > 0.01) {
                    try (PreparedStatement updateStmt = conn
                            .prepareStatement("UPDATE fines SET amount = ? WHERE fine_id = ?")) {
                        updateStmt.setDouble(1, amount);
                        updateStmt.setInt(2, rs.getInt("fine_id"));
                        updateStmt.executeUpdate();
                    }
                }
                return;
            }
        }

        // Insert new fine
        String insertSql = "INSERT INTO fines (license_plate, amount, paid_amount, reason, is_paid) VALUES (?, ?, 0, ?, 0)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, plate);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, taggedReason);
            pstmt.executeUpdate();
        }
    }
}