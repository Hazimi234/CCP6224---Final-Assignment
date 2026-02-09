package src.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import src.model.strategy.*; 

public class AdminPanel extends JPanel {
    private JLabel lblTotalRevenue;
    private JLabel lblOccupancy;
    private JTable vehicleTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> cmbStrategy;
    
    // Default strategy
    public static FineStrategy currentFineStrategy = new FixedFineStrategy();
    
    // --- FIX 1: Add this flag to prevent the crash ---
    private boolean isLoading = false; 

    public AdminPanel() {
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
        String[] columns = {"Spot ID", "License Plate", "Vehicle Type", "Is VIP", "Status"};
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
        loadSavedStrategy(); // This will now run safely!
    }

    private void loadSavedStrategy() {
        // --- FIX 2: Turn ON the flag before loading ---
        isLoading = true; 

        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = 'fine_strategy'";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                String savedOpt = rs.getString("setting_value");
                // This triggers the action listener, but 'isLoading' will block the save!
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
            // --- FIX 3: Turn OFF the flag when done ---
            isLoading = false; 
        }
    }

    private void updateStrategy() {
        String selectedFull = (String) cmbStrategy.getSelectedItem();
        String shortName = "Option A"; 

        // Update the local logic variable
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
        
        // --- FIX 4: If we are just loading, STOP HERE. Do not save, do not popup. ---
        if (isLoading) {
            return;
        }

        // Only save to DB if the USER actually clicked it
        saveStrategyToDB(shortName);
        
        JOptionPane.showMessageDialog(this, "System Updated!\nNow using: " + currentFineStrategy.getName());
    }

    private void saveStrategyToDB(String val) {
        String sql = "UPDATE app_settings SET setting_value = ? WHERE setting_key = 'fine_strategy'";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, val);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void refreshData() {
        updateRevenue();
        updateVehicleList();
    }

    private void updateRevenue() {
        String sql = "SELECT " +
                     "(SELECT IFNULL(SUM(parking_fee), 0) FROM tickets WHERE is_paid=1) + " +
                     "(SELECT IFNULL(SUM(amount), 0) FROM fines WHERE is_paid=1)";
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
        String sql = "SELECT s.spot_id, s.current_vehicle_plate, v.vehicle_type, v.is_vip " +
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

                tableModel.addRow(new Object[]{spot, plate, type, vip ? "YES" : "NO", "Parked"});
            }
            lblOccupancy.setText("Occupancy: " + occupiedCount + " / 50");
        } catch (SQLException e) { e.printStackTrace(); }
    }
}