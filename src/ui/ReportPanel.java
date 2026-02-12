package src.ui;

import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ReportPanel extends JPanel {
    private JTabbedPane tabs;
    private Runnable[] refreshTasks = new Runnable[4];

    public ReportPanel() {
        setLayout(new BorderLayout());
        tabs = new JTabbedPane();

        tabs.addTab("Current Parked Vehicles", createParkedVehiclesPanel());
        tabs.addTab("Revenue Report", createRevenueReportPanel());
        tabs.addTab("Occupancy Report", createOccupancyReportPanel());
        tabs.addTab("Fines Report", createFinesReportPanel());

        add(tabs, BorderLayout.CENTER);

        // Refresh data when tab is switched
        tabs.addChangeListener(e -> refreshCurrentTab());
    }

    public void refreshCurrentTab() {
        int index = tabs.getSelectedIndex();
        if (index >= 0 && index < refreshTasks.length && refreshTasks[index] != null) {
            refreshTasks[index].run();
        }
    }

    // --- 1. Current Parked Vehicles ---
    private JPanel createParkedVehiclesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] cols = {"Spot ID", "License Plate", "Vehicle Type", "Entry Time"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);
        
        Runnable refreshAction = () -> {
            model.setRowCount(0);
            String sql = "SELECT s.spot_id, s.current_vehicle_plate, v.vehicle_type, t.entry_time_millis " +
                         "FROM parking_spots s " +
                         "JOIN vehicles v ON s.current_vehicle_plate = v.license_plate " +
                         "JOIN tickets t ON s.current_vehicle_plate = t.license_plate " +
                         "WHERE s.current_vehicle_plate IS NOT NULL AND t.exit_time_millis IS NULL";
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while(rs.next()) {
                    long time = rs.getLong("entry_time_millis");
                    String dateStr = new java.text.SimpleDateFormat("dd-MM HH:mm").format(new java.util.Date(time));
                    model.addRow(new Object[]{
                        rs.getString("spot_id"), rs.getString("current_vehicle_plate"), 
                        rs.getString("vehicle_type"), dateStr
                    });
                }
            } catch (SQLException ex) { ex.printStackTrace(); }
        };

        refreshTasks[0] = refreshAction;

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // --- 2. Revenue Report ---
    private JPanel createRevenueReportPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Filters
        JPanel filterPanel = new JPanel();
        JComboBox<String> cmbType = new JComboBox<>(new String[]{"All", "Parking Fee", "Fine"});
        JComboBox<String> cmbMethod = new JComboBox<>(new String[]{"All", "Cash", "Card"});
        JButton btnLoad = new JButton("Generate Report");
        
        filterPanel.add(new JLabel("Type:")); filterPanel.add(cmbType);
        filterPanel.add(new JLabel("Method:")); filterPanel.add(cmbMethod);
        filterPanel.add(btnLoad);

        String[] cols = {"Source", "Reference ID", "Amount (RM)", "Payment Method"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel lblTotal = new JLabel("Total: RM 0.00", SwingConstants.CENTER);
        lblTotal.setFont(new Font("Arial", Font.BOLD, 14));
        lblTotal.setOpaque(true);
        lblTotal.setBackground(new Color(200, 255, 200));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(lblTotal, BorderLayout.NORTH);
        topPanel.add(filterPanel, BorderLayout.CENTER);
        panel.add(topPanel, BorderLayout.NORTH);

        Runnable refreshAction = () -> {
            model.setRowCount(0);
            double total = 0;
            String typeFilter = (String) cmbType.getSelectedItem();
            String methodFilter = (String) cmbMethod.getSelectedItem();

            // Query Tickets
            if (typeFilter.equals("All") || typeFilter.equals("Parking Fee")) {
                String sql = "SELECT ticket_id, parking_fee, payment_method FROM tickets WHERE is_paid=1";
                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while(rs.next()) {
                        String method = rs.getString("payment_method");
                        if (method == null) method = "Unknown";
                        if (!methodFilter.equals("All") && !method.equalsIgnoreCase(methodFilter)) continue;

                        double amt = rs.getDouble("parking_fee");
                        total += amt;
                        model.addRow(new Object[]{"Parking Fee", rs.getString("ticket_id"), String.format("%.2f", amt), method});
                    }
                } catch (SQLException ex) { ex.printStackTrace(); }
            }

            // Query Fines
            if (typeFilter.equals("All") || typeFilter.equals("Fine")) {
                String sql = "SELECT fine_id, amount, payment_method FROM fines WHERE is_paid=1";
                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while(rs.next()) {
                        String method = rs.getString("payment_method");
                        if (method == null) method = "Unknown";
                        if (!methodFilter.equals("All") && !method.equalsIgnoreCase(methodFilter)) continue;

                        double amt = rs.getDouble("amount");
                        total += amt;
                        model.addRow(new Object[]{"Fine", rs.getString("fine_id"), String.format("%.2f", amt), method});
                    }
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
            lblTotal.setText("Total Revenue: RM " + String.format("%.2f", total));
        };

        refreshTasks[1] = refreshAction;
        btnLoad.addActionListener(e -> refreshAction.run());

        return panel;
    }

    // --- 3. Occupancy Report ---
    private JPanel createOccupancyReportPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel content = new JPanel(new GridLayout(2, 1));

        // Spot Type Table
        String[] typeCols = {"Spot Type", "Occupied", "Total Spots", "Occupancy %"};
        DefaultTableModel typeModel = new DefaultTableModel(typeCols, 0);
        JTable typeTable = new JTable(typeModel);
        JScrollPane typeScroll = new JScrollPane(typeTable);
        typeScroll.setBorder(BorderFactory.createTitledBorder("Breakdown by Spot Type"));

        // Floor Table
        String[] floorCols = {"Floor Level", "Occupied", "Total Spots", "Occupancy %"};
        DefaultTableModel floorModel = new DefaultTableModel(floorCols, 0);
        JTable floorTable = new JTable(floorModel);
        JScrollPane floorScroll = new JScrollPane(floorTable);
        floorScroll.setBorder(BorderFactory.createTitledBorder("Breakdown by Floor"));

        content.add(typeScroll);
        content.add(floorScroll);
        panel.add(content, BorderLayout.CENTER);

        Runnable refreshAction = () -> {
            typeModel.setRowCount(0);
            floorModel.setRowCount(0);

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                 Statement stmt = conn.createStatement()) {
                
                // Type Stats
                ResultSet rsType = stmt.executeQuery("SELECT spot_type, COUNT(*) as total, SUM(CASE WHEN current_vehicle_plate IS NOT NULL THEN 1 ELSE 0 END) as occupied FROM parking_spots GROUP BY spot_type");
                while(rsType.next()) {
                    int occ = rsType.getInt("occupied");
                    int tot = rsType.getInt("total");
                    double pct = (double)occ/tot * 100;
                    typeModel.addRow(new Object[]{rsType.getString("spot_type"), occ, tot, String.format("%.1f%%", pct)});
                }

                // Floor Stats
                ResultSet rsFloor = stmt.executeQuery("SELECT floor_level, COUNT(*) as total, SUM(CASE WHEN current_vehicle_plate IS NOT NULL THEN 1 ELSE 0 END) as occupied FROM parking_spots GROUP BY floor_level");
                while(rsFloor.next()) {
                    int occ = rsFloor.getInt("occupied");
                    int tot = rsFloor.getInt("total");
                    double pct = (double)occ/tot * 100;
                    floorModel.addRow(new Object[]{"Floor " + rsFloor.getInt("floor_level"), occ, tot, String.format("%.1f%%", pct)});
                }

            } catch (SQLException ex) { ex.printStackTrace(); }
        };

        refreshTasks[2] = refreshAction;

        return panel;
    }

    // --- 4. Fines Report ---
    private JPanel createFinesReportPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] cols = {"License Plate", "Amount (RM)", "Reason"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);
        
        Runnable refreshAction = () -> {
            model.setRowCount(0);
            String sql = "SELECT license_plate, amount, reason FROM fines WHERE is_paid = 0";
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while(rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("license_plate"), 
                        String.format("%.2f", rs.getDouble("amount")), 
                        rs.getString("reason")
                    });
                }
            } catch (SQLException ex) { ex.printStackTrace(); }
        };

        refreshTasks[3] = refreshAction;

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }
}