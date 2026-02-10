package src.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import src.model.strategy.*; 

public class AdminStatsPanel extends JPanel {
    private JLabel lblTotalRevenue;
    private JLabel lblOccupancy;
    private JTable vehicleTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> cmbStrategy;
    
    // Default strategy
    public static FineStrategy currentFineStrategy = new FixedFineStrategy();
    
    private boolean isLoading = false; 

    public AdminStatsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- TOP: Stats Dashboard ---
        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        
        lblTotalRevenue = new JLabel("Total Revenue: RM 0.00", SwingConstants.CENTER);
        lblTotalRevenue.setFont(new Font("Arial", Font.BOLD, 18));
        lblTotalRevenue.setOpaque(true);
        lblTotalRevenue.setBackground(new Color(200, 255, 200)); 
        
        lblOccupancy = new JLabel("Occupancy: 0 / 50", SwingConstants.CENTER);
        lblOccupancy.setFont(new Font("Arial", Font.BOLD, 18));
        lblOccupancy.setOpaque(true);
        lblOccupancy.setBackground(new Color(200, 200, 255)); 
        
        statsPanel.add(lblTotalRevenue);
        statsPanel.add(lblOccupancy);
        add(statsPanel, BorderLayout.NORTH);

        // --- CENTER: Live Vehicle Table ---
        String[] columns = {"Spot ID", "License Plate", "Vehicle Type", "Is VIP", "Status", "Unpaid Fines"};
        tableModel = new DefaultTableModel(columns, 0);
        vehicleTable = new JTable(tableModel);
        add(new JScrollPane(vehicleTable), BorderLayout.CENTER);

        // --- BOTTOM: Controls ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton btnRefresh = new JButton("Refresh Data");
        btnRefresh.addActionListener(e -> refreshData());
        bottomPanel.add(btnRefresh);

        bottomPanel.add(new JLabel("   |   Fine Policy Scheme: "));
        
        String[] strategies = {
            "Option A: Fixed Fine (RM 50)", 
            "Option B: Progressive Fine", 
            "Option C: Hourly Fine (RM 20/hr)"
        };
        cmbStrategy = new JComboBox<>(strategies);
        cmbStrategy.addActionListener(e -> updateStrategy());
        bottomPanel.add(cmbStrategy);

        add(bottomPanel, BorderLayout.SOUTH);

        // --- LOAD SAVED DATA ---
        refreshData();       
        loadSavedStrategy(); 
    }

    private void loadSavedStrategy() {
        isLoading = true; 
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = 'fine_strategy'";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                String savedOpt = rs.getString("setting_value");
                if (savedOpt.equals("Option A")) {
                    cmbStrategy.setSelectedIndex(0);
                    currentFineStrategy = new FixedFineStrategy();
                } else if (savedOpt.equals("Option B")) {
                    cmbStrategy.setSelectedIndex(1);
                    currentFineStrategy = new ProgressiveFineStrategy();
                } else if (savedOpt.equals("Option C")) {
                    cmbStrategy.setSelectedIndex(2);
                    currentFineStrategy = new HourlyFineStrategy();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            isLoading = false; 
        }
    }

    private void updateStrategy() {
        String selectedFull = (String) cmbStrategy.getSelectedItem();
        String shortName = "Option A"; 

        if (selectedFull.contains("Option A")) {
            currentFineStrategy = new FixedFineStrategy();
            shortName = "Option A";
        } else if (selectedFull.contains("Option B")) {
            currentFineStrategy = new ProgressiveFineStrategy();
            shortName = "Option B";
        } else {
            currentFineStrategy = new HourlyFineStrategy();
            shortName = "Option C";
        }
        
        if (isLoading) return;

        saveStrategyToDB(shortName);
        JOptionPane.showMessageDialog(this, "System Updated!\nNow using: " + currentFineStrategy.getName());
    }

    private void saveStrategyToDB(String val) {
        String sql = "UPDATE app_settings SET setting_value = ? WHERE setting_key = 'fine_strategy'";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, val);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void refreshData() {
        runComplianceScan(); // <--- NEW: Calculate and save fines on load/refresh
        updateRevenue();
        updateVehicleList();
    }

    private void updateRevenue() {
        String sql = "SELECT (SELECT IFNULL(SUM(parking_fee), 0) FROM tickets WHERE is_paid=1) + (SELECT IFNULL(SUM(amount), 0) FROM fines WHERE is_paid=1)";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                double total = rs.getDouble(1);
                lblTotalRevenue.setText("Total Revenue: RM " + String.format("%.2f", total));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateVehicleList() {
        tableModel.setRowCount(0); 
        int occupiedCount = 0;
        
        // Updated SQL: Fetch only DB fines, no live calculation needed
        String sql = "SELECT s.spot_id, s.current_vehicle_plate, v.vehicle_type, v.is_vip, " +
                     "(SELECT IFNULL(SUM(amount), 0) FROM fines f WHERE f.license_plate = s.current_vehicle_plate AND f.is_paid = 0) as db_fines " +
                     "FROM parking_spots s " +
                     "JOIN vehicles v ON s.current_vehicle_plate = v.license_plate " +
                     "WHERE s.current_vehicle_plate IS NOT NULL " +
                     "ORDER BY s.spot_id";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                occupiedCount++;
                String spot = rs.getString("spot_id");
                String plate = rs.getString("current_vehicle_plate");
                String type = rs.getString("vehicle_type");
                boolean vip = rs.getInt("is_vip") == 1;
                double dbFines = rs.getDouble("db_fines");

                tableModel.addRow(new Object[]{spot, plate, type, vip ? "YES" : "NO", "Parked", String.format("RM %.2f", dbFines)});
            }
            lblOccupancy.setText("Occupancy: " + occupiedCount + " / 50");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- NEW: Scan for violations and save to DB ---
    private void runComplianceScan() {
        String sql = "SELECT s.spot_type, s.current_vehicle_plate, v.is_vip, t.entry_time_millis " +
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
                
                // 1. Violation: Non-VIP in Reserved
                if ("Reserved".equalsIgnoreCase(spotType) && !isVip) {
                    updateOrInsertFine(conn, plate, 50.0, "Misuse of Reserved Spot");
                }
                
                // 2. Overstay: > 24 Hours
                double hours = Math.ceil((now - entryTime) / (1000.0 * 60 * 60));
                if (hours > 24 && currentFineStrategy != null) {
                    double fine = currentFineStrategy.calculateFine(hours);
                    if (fine > 0) {
                        updateOrInsertFine(conn, plate, fine, "Overstay Fine");
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- FIX: Update amount if fine increases, or Insert if new ---
    private void updateOrInsertFine(Connection conn, String plate, double amount, String reasonPrefix) {
        // Check if a fine with this reason (e.g., "Overstay Fine%") already exists and is unpaid
        String checkSql = "SELECT fine_id, amount FROM fines WHERE license_plate = ? AND is_paid = 0 AND reason LIKE ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setString(1, plate);
            pstmt.setString(2, reasonPrefix + "%");
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                // Fine exists. Always update to match current calculation.
                try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE fines SET amount = ? WHERE fine_id = ?")) {
                    updateStmt.setDouble(1, amount);
                    updateStmt.setInt(2, rs.getInt("fine_id"));
                    updateStmt.executeUpdate();
                }
                return; 
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // Insert if not exists
        String insertSql = "INSERT INTO fines (license_plate, amount, reason, is_paid) VALUES (?, ?, ?, 0)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, plate);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, reasonPrefix); // e.g., "Overstay Fine"
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}