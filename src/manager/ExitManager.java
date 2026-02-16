package src.manager;

import java.sql.*;
import java.util.*;
import src.model.BillData;
import src.model.strategy.HourlyFineStrategy;

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

                double finalRate = rate;
                if (vType.equals("Handicapped Vehicle") && hasCard) {
                    if (spotType.equals("Compact") || spotType.equals("Handicapped")) finalRate = 0.0;
                    else if (spotType.equals("Regular")) finalRate = 3.0;
                }
                data.parkingFee = hours * finalRate;
                data.currentFine = 0.0;
                data.pastFines = 0.0;

                StringBuilder sb = new StringBuilder();
                sb.append("Ticket ID:  ").append(data.ticketID).append("\n");
                sb.append("Plate No:   ").append(plate).append("\n");
                sb.append("Duration:   ").append((int)hours).append(" hours\n");
                sb.append("Parking Fee: RM ").append(String.format("%.2f", data.parkingFee)).append("\n");

                // Overstay Fine (Current)
                if (hours > 24) {
                    double fine = AdminManager.currentFineStrategy.calculateFine(hours);
                    if (fine > 0) {
                        sb.append("OVERSTAY FINE: RM ").append(String.format("%.2f", fine)).append("\n");
                        addInstantFine(conn, plate, fine, "Overstay Fine", data.ticketID);
                        data.currentFine += fine;
                    }
                }

                // Violation Fine (Current)
                if (spotType.equals("Reserved") && !isVip) {
                    sb.append("VIOLATION FINE: RM 50.00\n");
                    addInstantFine(conn, plate, 50.0, "Misuse of Reserved Spot", data.ticketID);
                    data.currentFine += 50.0;
                }

                // --- UPDATED: Calculate Past Fines using (Amount - PaidAmount) ---
                String tag = "[" + data.ticketID + "]";
                try (PreparedStatement psFines = conn.prepareStatement("SELECT amount, paid_amount, reason FROM fines WHERE license_plate = ? AND is_paid = 0")) {
                    psFines.setString(1, plate);
                    try (ResultSet rsFines = psFines.executeQuery()) {
                        while (rsFines.next()) {
                            double amt = rsFines.getDouble("amount");
                            double paid = rsFines.getDouble("paid_amount");
                            String rsn = rsFines.getString("reason");
                            double due = amt - paid;

                            if (rsn != null && rsn.contains(tag)) {
                                // Already in current fines
                            } else {
                                data.pastFines += due;
                            }
                        }
                    }
                }

                // Update currentFine from DB just in case partial payments occurred
                try (PreparedStatement psCur = conn.prepareStatement("SELECT SUM(amount - paid_amount) FROM fines WHERE license_plate = ? AND is_paid = 0 AND reason LIKE ?")) {
                    psCur.setString(1, plate);
                    psCur.setString(2, "%" + tag + "%");
                    try (ResultSet rsCur = psCur.executeQuery()) {
                        if (rsCur.next()) {
                            data.currentFine = rsCur.getDouble(1);
                        }
                    }
                }

                if (data.pastFines > 0) {
                     sb.append("PAST UNPAID FINES: RM ").append(String.format("%.2f", data.pastFines)).append("\n");
                }
                
                sb.append("----------------------------------\n");
                data.totalAmount = data.parkingFee + data.currentFine + data.pastFines;
                sb.append("TOTAL DUE:  RM ").append(String.format("%.2f", data.totalAmount)).append("\n");
                
                data.billText = sb.toString();
            }
        }
        return data;
    }

    private void addInstantFine(Connection conn, String plate, double amount, String reasonPrefix, String ticketID) throws SQLException {
        String taggedReason = reasonPrefix + " [" + ticketID + "]";
        String checkSql = "SELECT fine_id, amount FROM fines WHERE license_plate = ? AND is_paid = 0 AND reason = ?";
        
        try (PreparedStatement check = conn.prepareStatement(checkSql)) {
            check.setString(1, plate);
            check.setString(2, taggedReason);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                double existingAmt = rs.getDouble("amount");
                if (Math.abs(existingAmt - amount) > 0.01) {
                    try (PreparedStatement update = conn.prepareStatement("UPDATE fines SET amount = ? WHERE fine_id = ?")) {
                        update.setDouble(1, amount);
                        update.setInt(2, rs.getInt("fine_id"));
                        update.executeUpdate();
                    }
                }
                return;
            }
        }
        
        try (PreparedStatement insert = conn.prepareStatement("INSERT INTO fines (license_plate, amount, paid_amount, reason, is_paid) VALUES (?, ?, 0, ?, 0)")) {
            insert.setString(1, plate);
            insert.setDouble(2, amount);
            insert.setString(3, taggedReason);
            insert.executeUpdate();
        }
    }

    public void processPayment(String ticketID, String spotID, String plate, double paidAmount, double feeCost, String method) throws SQLException {
        long exitTime = System.currentTimeMillis();
        
        double pastFines = 0.0;
        double currentFines = 0.0;
        String tag = "[" + ticketID + "]";
        List<Integer> fineIds = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db")) {
            conn.setAutoCommit(false);
            
            try {
                // Get fines sorted by ID (oldest first)
                String sqlGetFines = "SELECT fine_id, amount, paid_amount, reason FROM fines WHERE license_plate = ? AND is_paid = 0 ORDER BY fine_id ASC";
                try (PreparedStatement ps = conn.prepareStatement(sqlGetFines)) {
                    ps.setString(1, plate);
                    ResultSet rs = ps.executeQuery();
                    while(rs.next()){
                        double amt = rs.getDouble("amount");
                        double paid = rs.getDouble("paid_amount");
                        double due = amt - paid;
                        
                        fineIds.add(rs.getInt("fine_id"));
                        
                        String rsn = rs.getString("reason");
                        if (rsn != null && rsn.contains(tag)) currentFines += due;
                        else pastFines += due;
                    }
                }

                // Validations
                boolean isOptionC = (AdminManager.currentFineStrategy instanceof HourlyFineStrategy);
                double minReq = 0.0;
                
                if (isOptionC) {
                    minReq = feeCost + pastFines;
                    if (paidAmount < (minReq - 0.01)) {
                         throw new SQLException("Partial Payment Error.\nMin Required: RM " + String.format("%.2f", minReq));
                    }
                } else {
                    minReq = feeCost + pastFines + currentFines;
                    if (paidAmount < (minReq - 0.01)) { 
                        throw new SQLException("Full Payment Error.\nTotal Required: RM " + String.format("%.2f", minReq));
                    }
                }

                // 1. Pay Ticket
                try (PreparedStatement pstT = conn.prepareStatement("UPDATE tickets SET exit_time_millis = ?, parking_fee = ?, is_paid = 1, payment_method = ? WHERE ticket_id = ?");
                     PreparedStatement pstS = conn.prepareStatement("UPDATE parking_spots SET current_vehicle_plate = NULL WHERE spot_id = ?")) {
                    pstT.setLong(1, exitTime);
                    pstT.setDouble(2, feeCost); 
                    pstT.setString(3, method);
                    pstT.setString(4, ticketID);
                    pstT.executeUpdate();
                    pstS.setString(1, spotID);
                    pstS.executeUpdate();
                }

                // 2. Pay Fines (Using paid_amount column)
                double remaining = paidAmount - feeCost;
                
                if (remaining > 0) {
                    for (int fid : fineIds) {
                        double amount = 0, paidAlready = 0;
                        try (PreparedStatement p = conn.prepareStatement("SELECT amount, paid_amount FROM fines WHERE fine_id=?")) {
                            p.setInt(1, fid);
                            ResultSet r = p.executeQuery();
                            if(r.next()) {
                                amount = r.getDouble(1);
                                paidAlready = r.getDouble(2);
                            }
                        }

                        double due = amount - paidAlready;
                        double paymentForThisFine = Math.min(remaining, due);

                        if (paymentForThisFine > 0) {
                            try (PreparedStatement pay = conn.prepareStatement(
                                "UPDATE fines SET paid_amount = paid_amount + ?, payment_method = ?, is_paid = (CASE WHEN paid_amount + ? >= amount THEN 1 ELSE 0 END) WHERE fine_id = ?")) {
                                pay.setDouble(1, paymentForThisFine);
                                pay.setString(2, method);
                                pay.setDouble(3, paymentForThisFine); // Check Logic
                                pay.setInt(4, fid);
                                pay.executeUpdate();
                            }
                            remaining -= paymentForThisFine;
                        }
                        
                        if (remaining <= 0.01) break;
                    }
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}