package src.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import src.manager.ParkingSystemFacade;

public class AdminStatsPanel extends JPanel {
    private JLabel lblTotalRevenue;
    private JLabel lblOccupancy;
    private JTable vehicleTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> cmbStrategy;
    private ParkingSystemFacade facade = new ParkingSystemFacade();
    
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
        loadSavedStrategy(); 
        refreshData();       
    }
    
    // Loads the saved fine strategy from the database and updates the combo box selection
    private void loadSavedStrategy() {
        isLoading = true; 
        String savedOpt = facade.loadSavedStrategy();
        
        if (savedOpt != null) {
            if (savedOpt.equals("Option A")) {
                cmbStrategy.setSelectedIndex(0);
            } else if (savedOpt.equals("Option B")) {
                cmbStrategy.setSelectedIndex(1);
            } else if (savedOpt.equals("Option C")) {
                cmbStrategy.setSelectedIndex(2);
            }
        }
        isLoading = false; 
    }

    // Updates the fine strategy in the database based on the selected option in the combo box
    private void updateStrategy() {
        String selectedFull = (String) cmbStrategy.getSelectedItem();
        String shortName = "Option A"; 

        if (selectedFull.contains("Option A")) {
            shortName = "Option A";
        } else if (selectedFull.contains("Option B")) {
            shortName = "Option B";
        } else {
            shortName = "Option C";
        }
        
        if (isLoading) return;

        facade.saveStrategy(shortName);
        JOptionPane.showMessageDialog(this, "System Updated!\nNow using: " + shortName);
    }


    public void refreshData() {
        facade.runComplianceScan(); 
        updateRevenue();
        updateVehicleList();
    }

    private void updateRevenue() {
        double total = facade.getTotalRevenue();
        lblTotalRevenue.setText("Total Revenue: RM " + String.format("%.2f", total));
    }

    private void updateVehicleList() {
        tableModel.setRowCount(0); 
        
        java.util.List<Object[]> vehicles = facade.getLiveVehicles();
        for (Object[] row : vehicles) {
            tableModel.addRow(row);
        }
        lblOccupancy.setText("Occupancy: " + vehicles.size() + " / 50");
    }
}