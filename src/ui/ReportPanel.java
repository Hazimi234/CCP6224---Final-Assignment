package src.ui;

import java.awt.*;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import src.manager.ParkingSystemFacade;

public class ReportPanel extends JPanel {
    private JTabbedPane tabs;
    private ParkingSystemFacade facade = new ParkingSystemFacade();
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
            List<Object[]> data = facade.getParkedVehiclesReport();
            for (Object[] row : data) {
                model.addRow(row);
            }
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
        cmbMethod.setPreferredSize(new Dimension(75, cmbMethod.getPreferredSize().height));
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
            String typeFilter = (String) cmbType.getSelectedItem();
            String methodFilter = (String) cmbMethod.getSelectedItem();

            Map<String, Object> result = facade.getRevenueReport(typeFilter, methodFilter);
            double total = (double) result.get("total");
            List<Object[]> rows = (List<Object[]>) result.get("rows");
            
            for (Object[] row : rows) {
                model.addRow(row);
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

            Map<String, List<Object[]>> data = facade.getOccupancyReport();
            for (Object[] row : data.get("type")) {
                typeModel.addRow(row);
            }
            for (Object[] row : data.get("floor")) {
                floorModel.addRow(row);
            }
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
            List<Object[]> data = facade.getFinesReport();
            for (Object[] row : data) {
                model.addRow(row);
            }
        };

        refreshTasks[3] = refreshAction;

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }
}