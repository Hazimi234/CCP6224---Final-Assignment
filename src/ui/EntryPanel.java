package src.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;
import src.model.Vehicle; // Import our updated model

public class EntryPanel extends JPanel {
    // Components
    private JTextField txtPlate;
    private JComboBox<String> cmbType;
    private JCheckBox chkVip;
    private JTable spotTable;
    private DefaultTableModel tableModel;
    private JButton btnPark;

    public EntryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- TOP: Input Form ---
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        
        formPanel.add(new JLabel("License Plate:"));
        txtPlate = new JTextField();
        formPanel.add(txtPlate);

        formPanel.add(new JLabel("Vehicle Type:"));
        String[] types = {"Car", "Motorcycle", "SUV", "Handicapped Vehicle"};
        cmbType = new JComboBox<>(types);
        formPanel.add(cmbType);

        formPanel.add(new JLabel("VIP Status:"));
        chkVip = new JCheckBox("Is VIP Holder?");
        formPanel.add(chkVip);

        JButton btnFind = new JButton("Find Available Spots");
        btnFind.addActionListener(e -> findSpots());
        formPanel.add(btnFind);
        
        add(formPanel, BorderLayout.NORTH);

        // --- CENTER: Spot Selection Table ---
        String[] columns = {"Spot ID", "Floor", "Type", "Rate (RM/hr)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override // Make table read-only
            public boolean isCellEditable(int row, int column) { return false; }
        };
        spotTable = new JTable(tableModel);
        spotTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(spotTable), BorderLayout.CENTER);

        // --- BOTTOM: Action Button ---
        btnPark = new JButton("PARK VEHICLE & GENERATE TICKET");
        btnPark.setFont(new Font("Arial", Font.BOLD, 14));
        btnPark.setBackground(new Color(100, 200, 100));
        btnPark.addActionListener(e -> parkVehicle());
        add(btnPark, BorderLayout.SOUTH);
    }

    private void findSpots() {
        String type = (String) cmbType.getSelectedItem();
        boolean isVip = chkVip.isSelected();
        String plate = txtPlate.getText().trim();
        
        if (plate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a license plate first.");
            return;
        }

        // Create a temporary vehicle just to check what fits (Physical check)
        Vehicle tempVehicle = new Vehicle(plate, type, isVip);

        tableModel.setRowCount(0); // Clear table

        String sql = "SELECT * FROM parking_spots WHERE current_vehicle_plate IS NULL";
        
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String spotType = rs.getString("spot_type");
                
                // Use our new "canFitInSpot" logic
                if (tempVehicle.canFitInSpot(spotType)) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getString("spot_id"));
                    row.add(rs.getInt("floor_level"));
                    row.add(spotType);
                    row.add(rs.getDouble("hourly_rate"));
                    tableModel.addRow(row);
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
        }
    }

    private void parkVehicle() {
        int selectedRow = spotTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a spot from the table.");
            return;
        }

        String spotID = (String) tableModel.getValueAt(selectedRow, 0);
        String plate = txtPlate.getText().trim().toUpperCase();
        String type = (String) cmbType.getSelectedItem();
        boolean isVip = chkVip.isSelected();
        long entryTime = System.currentTimeMillis();

        String sqlVehicle = "INSERT OR REPLACE INTO vehicles (license_plate, vehicle_type, is_vip) VALUES (?, ?, ?)";
        String sqlSpot = "UPDATE parking_spots SET current_vehicle_plate = ? WHERE spot_id = ?";
        String sqlTicket = "INSERT INTO tickets (ticket_id, license_plate, spot_id, entry_time_millis) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db")) {
            conn.setAutoCommit(false); // Start Transaction

            try (PreparedStatement pstmtV = conn.prepareStatement(sqlVehicle);
                 PreparedStatement pstmtS = conn.prepareStatement(sqlSpot);
                 PreparedStatement pstmtT = conn.prepareStatement(sqlTicket)) {

                // 1. Save Vehicle
                pstmtV.setString(1, plate);
                pstmtV.setString(2, type);
                pstmtV.setInt(3, isVip ? 1 : 0);
                pstmtV.executeUpdate();

                // 2. Update Spot
                pstmtS.setString(1, plate);
                pstmtS.setString(2, spotID);
                pstmtS.executeUpdate();

                // 3. Generate Ticket
                String ticketID = "T-" + plate + "-" + (entryTime / 1000); // Simple ID
                pstmtT.setString(1, ticketID);
                pstmtT.setString(2, plate);
                pstmtT.setString(3, spotID);
                pstmtT.setLong(4, entryTime);
                pstmtT.executeUpdate();

                conn.commit(); // Save all changes

                JOptionPane.showMessageDialog(this, 
                    "Vehicle Parked Successfully!\n\n" +
                    "Ticket ID: " + ticketID + "\n" +
                    "Spot: " + spotID + "\n" +
                    "Time: " + new java.util.Date(entryTime));

                // Clear form
                txtPlate.setText("");
                tableModel.setRowCount(0);

            } catch (SQLException e) {
                conn.rollback(); // Undo if error
                throw e;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error parking vehicle: " + ex.getMessage());
        }
    }
}