package src.manager;

import java.sql.*;
import java.util.*;

public class ReportManager {

    // Returns list of currently parked cars for Tab 1
    public List<Object[]> getParkedVehicles() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT s.spot_id, s.current_vehicle_plate, v.vehicle_type, t.entry_time_millis " +
                "FROM parking_spots s " +
                "JOIN vehicles v ON s.current_vehicle_plate = v.license_plate " +
                "JOIN tickets t ON s.current_vehicle_plate = t.license_plate " +
                "WHERE s.current_vehicle_plate IS NOT NULL AND t.exit_time_millis IS NULL";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                long time = rs.getLong("entry_time_millis");
                String dateStr = new java.text.SimpleDateFormat("dd-MM HH:mm").format(new java.util.Date(time));
                list.add(new Object[] {
                        rs.getString("spot_id"), rs.getString("current_vehicle_plate"),
                        rs.getString("vehicle_type"), dateStr
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    // Returns Revenue Data for Tab 2
    public Map<String, Object> getRevenueData(String typeFilter, String methodFilter) {
        Map<String, Object> result = new HashMap<>();
        List<Object[]> rows = new ArrayList<>();
        double total = 0;

        // 1. Calculate Revenue from Tickets
        if (typeFilter.equals("All") || typeFilter.equals("Parking Fee")) {
            String sql = "SELECT ticket_id, parking_fee, payment_method FROM tickets WHERE is_paid=1";
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String method = rs.getString("payment_method");
                    if (method == null)
                        method = "Unknown";
                    if (!methodFilter.equals("All") && !method.equalsIgnoreCase(methodFilter))
                        continue;
                    double amt = rs.getDouble("parking_fee");
                    total += amt;
                    rows.add(new Object[] { "Parking Fee", rs.getString("ticket_id"), String.format("%.2f", amt),
                            method });
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        // 2. Calculate Revenue from Fines
        if (typeFilter.equals("All") || typeFilter.equals("Fine")) {
            String sql = "SELECT fine_id, paid_amount, payment_method FROM fines WHERE paid_amount > 0";
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String method = rs.getString("payment_method");
                    if (method == null)
                        method = "Unknown";
                    if (!methodFilter.equals("All") && !method.equalsIgnoreCase(methodFilter))
                        continue;

                    double collected = rs.getDouble("paid_amount");
                    total += collected;

                    rows.add(
                            new Object[] { "Fine", rs.getString("fine_id"), String.format("%.2f", collected), method });
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        result.put("total", total);
        result.put("rows", rows);
        return result;
    }

    // Returns Occupancy stats for Tab 3
    public Map<String, List<Object[]>> getOccupancyData() {
        Map<String, List<Object[]>> result = new HashMap<>();
        List<Object[]> typeRows = new ArrayList<>();
        List<Object[]> floorRows = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                Statement stmt = conn.createStatement()) {

            // Group by Spot Type
            ResultSet rsType = stmt.executeQuery(
                    "SELECT spot_type, COUNT(*) as total, SUM(CASE WHEN current_vehicle_plate IS NOT NULL THEN 1 ELSE 0 END) as occupied FROM parking_spots GROUP BY spot_type");
            while (rsType.next()) {
                int occ = rsType.getInt("occupied");
                int tot = rsType.getInt("total");
                double pct = (double) occ / tot * 100;
                typeRows.add(new Object[] { rsType.getString("spot_type"), occ, tot, String.format("%.1f%%", pct) });
            }

            // Group by Floor Level
            ResultSet rsFloor = stmt.executeQuery(
                    "SELECT floor_level, COUNT(*) as total, SUM(CASE WHEN current_vehicle_plate IS NOT NULL THEN 1 ELSE 0 END) as occupied FROM parking_spots GROUP BY floor_level");
            while (rsFloor.next()) {
                int occ = rsFloor.getInt("occupied");
                int tot = rsFloor.getInt("total");
                double pct = (double) occ / tot * 100;
                floorRows.add(new Object[] { "Floor " + rsFloor.getInt("floor_level"), occ, tot,
                        String.format("%.1f%%", pct) });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        result.put("type", typeRows);
        result.put("floor", floorRows);
        return result;
    }

    // Returns Detailed Fine Report for Tab 4
    public List<Object[]> getFinesData() {
        List<Object[]> list = new ArrayList<>();
        // Query fetches the original amount vs paid amount to show status
        String sql = "SELECT license_plate, amount, paid_amount, reason, is_paid FROM fines";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                double total = rs.getDouble("amount");
                double paid = rs.getDouble("paid_amount");
                double balance = total - paid;
                boolean isPaid = rs.getInt("is_paid") == 1;

                // Determine readable status
                String status = "Unpaid";
                if (isPaid)
                    status = "Paid";
                else if (paid > 0)
                    status = "Partial";

                list.add(new Object[] {
                        rs.getString("license_plate"),
                        String.format("%.2f", total),
                        String.format("%.2f", paid),
                        String.format("%.2f", balance), // Outstanding Balance
                        status,
                        rs.getString("reason")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }
}