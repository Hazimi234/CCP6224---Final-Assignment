package src.ui;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ExitPanel extends JPanel {
    private JTextField txtPlateSearch;
    private JTextArea txtBillArea;
    private JButton btnPay;
    private JComboBox<String> cmbPaymentMethod; 
    
    private String currentTicketID;
    private String currentSpotID;
    private double totalAmountDue = 0.0;

    public ExitPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- TOP: Search ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Enter License Plate to Exit:"));
        txtPlateSearch = new JTextField(15);
        searchPanel.add(txtPlateSearch);
        
        JButton btnSearch = new JButton("Calculate Bill");
        btnSearch.addActionListener(e -> calculateBill());
        searchPanel.add(btnSearch);
        
        add(searchPanel, BorderLayout.NORTH);

        // --- CENTER: Bill Display ---
        txtBillArea = new JTextArea();
        txtBillArea.setEditable(false);
        txtBillArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        txtBillArea.setBorder(BorderFactory.createTitledBorder("Payment Details"));
        add(new JScrollPane(txtBillArea), BorderLayout.CENTER);

        // --- BOTTOM: Payment Controls ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        bottomPanel.add(new JLabel("Payment Method:"));
        
        // --- UPDATED: Simplified Payment Methods ---
        String[] methods = {"Cash", "Card"}; 
        cmbPaymentMethod = new JComboBox<>(methods);
        cmbPaymentMethod.setEnabled(false); 
        bottomPanel.add(cmbPaymentMethod);

        btnPay = new JButton("CONFIRM PAYMENT & EXIT");
        btnPay.setFont(new Font("Arial", Font.BOLD, 14));
        btnPay.setBackground(new Color(255, 100, 100)); 
        btnPay.setEnabled(false); 
        btnPay.addActionListener(e -> processPayment());
        bottomPanel.add(btnPay);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void calculateBill() {
        String plate = txtPlateSearch.getText().trim();
        if (plate.isEmpty()) return;

        txtBillArea.setText("");
        totalAmountDue = 0.0;
        btnPay.setEnabled(false);
        cmbPaymentMethod.setEnabled(false);

        String sql = "SELECT t.ticket_id, t.entry_time_millis, t.spot_id, " +
                     "s.spot_type, s.hourly_rate, " +
                     "v.vehicle_type, v.is_vip, v.has_handicapped_card " +
                     "FROM tickets t " +
                     "JOIN parking_spots s ON t.spot_id = s.spot_id " +
                     "JOIN vehicles v ON t.license_plate = v.license_plate " +
                     "WHERE t.license_plate = ? AND t.exit_time_millis IS NULL";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plate);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                currentTicketID = rs.getString("ticket_id");
                currentSpotID = rs.getString("spot_id");
                long entryTime = rs.getLong("entry_time_millis");
                String spotType = rs.getString("spot_type");
                double rate = rs.getDouble("hourly_rate");
                String vType = rs.getString("vehicle_type");
                boolean isVip = rs.getInt("is_vip") == 1;
                boolean hasCard = rs.getInt("has_handicapped_card") == 1;

                long exitTime = System.currentTimeMillis();
                long durationMillis = exitTime - entryTime;
                double hours = Math.ceil(durationMillis / (1000.0 * 60 * 60)); 
                if (hours == 0) hours = 1; 

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
                String readableEntryTime = sdf.format(new Date(entryTime));

                // Logic: Handicapped Discounts
                double finalRate = rate;
                String discountMsg = "";

                if (vType.equals("Handicapped Vehicle") && hasCard) {
                    if (spotType.equals("Compact") || spotType.equals("Handicapped")) {
                        finalRate = 0.0; 
                        discountMsg = " (Handicapped Card: FREE)";
                    } else if (spotType.equals("Regular")) {
                        finalRate = 3.0; 
                        discountMsg = " (Handicapped Card: Discounted)";
                    }
                }

                double parkingFee = hours * finalRate;
                StringBuilder bill = new StringBuilder();
                bill.append("Ticket ID:  ").append(currentTicketID).append("\n");
                bill.append("Plate No:   ").append(plate).append("\n");
                bill.append("Entry Time: ").append(readableEntryTime).append("\n");
                bill.append("Spot:       ").append(currentSpotID).append(" (").append(spotType).append(")\n");
                bill.append("Duration:   ").append((int)hours).append(" hours\n");
                bill.append("Rate:       RM ").append(rate).append("/hr").append(discountMsg).append("\n");
                bill.append("Fee:        RM ").append(String.format("%.2f", parkingFee)).append("\n");
                bill.append("----------------------------------\n");

                double subTotal = parkingFee;

                // Violation Check
                if (spotType.equals("Reserved") && !isVip) {
                    double violationFine = 50.0;
                    bill.append("VIOLATION: Non-VIP in Reserved Spot!\n");
                    bill.append("Fine Added: RM ").append(violationFine).append("\n");
                    addInstantFine(plate, violationFine, "Misuse of Reserved Spot");
                    subTotal += violationFine;
                }

                // Previous Fines Check
                double oldFines = checkUnpaidFines(plate);
                if (oldFines > 0) {
                    bill.append("Previous Unpaid Fines: RM ").append(String.format("%.2f", oldFines)).append("\n");
                    subTotal += oldFines;
                }

                bill.append("==================================\n");
                bill.append("TOTAL DUE:  RM ").append(String.format("%.2f", subTotal)).append("\n");

                txtBillArea.setText(bill.toString());
                totalAmountDue = subTotal;
                btnPay.setEnabled(true);
                cmbPaymentMethod.setEnabled(true); 

            } else {
                JOptionPane.showMessageDialog(this, "Vehicle not found or already exited.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private double checkUnpaidFines(String plate) {
        String sql = "SELECT SUM(amount) FROM fines WHERE license_plate = ? AND is_paid = 0";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plate);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    private void addInstantFine(String plate, double amount, String reason) {
        String sql = "INSERT INTO fines (license_plate, amount, reason, is_paid) VALUES (?, ?, ?, 0)";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plate);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, reason);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void processPayment() {
        String plate = txtPlateSearch.getText().trim();
        long exitTime = System.currentTimeMillis();
        String method = (String) cmbPaymentMethod.getSelectedItem(); 

        String sqlUpdateTicket = "UPDATE tickets SET exit_time_millis = ?, parking_fee = ?, is_paid = 1 WHERE ticket_id = ?";
        String sqlClearSpot = "UPDATE parking_spots SET current_vehicle_plate = NULL WHERE spot_id = ?";
        String sqlPayFines = "UPDATE fines SET is_paid = 1 WHERE license_plate = ? AND is_paid = 0";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db")) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstT = conn.prepareStatement(sqlUpdateTicket);
                 PreparedStatement pstS = conn.prepareStatement(sqlClearSpot);
                 PreparedStatement pstF = conn.prepareStatement(sqlPayFines)) {

                pstT.setLong(1, exitTime);
                pstT.setDouble(2, totalAmountDue);
                pstT.setString(3, currentTicketID);
                pstT.executeUpdate();

                pstS.setString(1, currentSpotID);
                pstS.executeUpdate();

                pstF.setString(1, plate);
                pstF.executeUpdate();

                conn.commit();
                generateReceipt(plate, exitTime, method); 

                txtBillArea.setText("Payment Successful!\nGate Opening...");
                txtPlateSearch.setText("");
                btnPay.setEnabled(false);
                cmbPaymentMethod.setEnabled(false);

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Payment Error: " + ex.getMessage());
        }
    }

    private void generateReceipt(String plate, long time, String method) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
        String timeStr = sdf.format(new Date(time));

        String msg = "RECEIPT\n" +
                     "--------------------\n" +
                     "Plate: " + plate + "\n" +
                     "Method: " + method + "\n" + 
                     "Paid: RM " + String.format("%.2f", totalAmountDue) + "\n" +
                     "Time: " + timeStr + "\n" +
                     "--------------------\n" +
                     "Thank you!";
        
        JOptionPane.showMessageDialog(this, msg);
    }
}