package src.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;
import java.text.SimpleDateFormat; // NEW IMPORT
import java.util.Date;            // NEW IMPORT
import java.util.TimeZone;        // NEW IMPORT
import src.model.Vehicle; 

public class EntryPanel extends JPanel {
    private JTextField txtPlate;
    private JComboBox<String> cmbType;
    private JCheckBox chkVip;
    private JCheckBox chkHandicappedCard; 
    private JTable spotTable;
    private DefaultTableModel tableModel;
    private JButton btnPark;

    public EntryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- TOP: Input Form ---
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 5, 5)); 
        
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

        formPanel.add(new JLabel("Handicapped Card:"));
        chkHandicappedCard = new JCheckBox("Has Physical Card?");
        chkHandicappedCard.setEnabled(false); 
        formPanel.add(chkHandicappedCard);

        cmbType.addActionListener(e -> {
            String selected = (String) cmbType.getSelectedItem();
            if ("Handicapped Vehicle".equals(selected)) {
                chkHandicappedCard.setEnabled(true);
            } else {
                chkHandicappedCard.setSelected(false);
                chkHandicappedCard.setEnabled(false);
            }
        });

        JButton btnFind = new JButton("Find Available Spots");
        btnFind.addActionListener(e -> findSpots());
        formPanel.add(btnFind);
        
        add(formPanel, BorderLayout.NORTH);

        // --- CENTER: Spot Selection Table ---
        String[] columns = {"Spot ID", "Floor", "Type", "Rate (RM/hr)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
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
        boolean hasCard = chkHandicappedCard.isSelected();
        String plate = txtPlate.getText().trim();
        
        if (plate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a license plate first.");
            return;
        }

        Vehicle tempVehicle = new Vehicle(plate, type, isVip, hasCard);
        tableModel.setRowCount(0); 

        String sql = "SELECT * FROM parking_spots WHERE current_vehicle_plate IS NULL";
        
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String spotType = rs.getString("spot_type");
                
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
        boolean hasCard = chkHandicappedCard.isSelected(); 
        long entryTime = System.currentTimeMillis();

        String sqlVehicle = "INSERT OR REPLACE INTO vehicles (license_plate, vehicle_type, is_vip, has_handicapped_card) VALUES (?, ?, ?, ?)";
        String sqlSpot = "UPDATE parking_spots SET current_vehicle_plate = ? WHERE spot_id = ?";
        String sqlTicket = "INSERT INTO tickets (ticket_id, license_plate, spot_id, entry_time_millis) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:parking_lot.db")) {
            conn.setAutoCommit(false); 

            try (PreparedStatement pstmtV = conn.prepareStatement(sqlVehicle);
                 PreparedStatement pstmtS = conn.prepareStatement(sqlSpot);
                 PreparedStatement pstmtT = conn.prepareStatement(sqlTicket)) {

                // 1. Save Vehicle
                pstmtV.setString(1, plate);
                pstmtV.setString(2, type);
                pstmtV.setInt(3, isVip ? 1 : 0);
                pstmtV.setInt(4, hasCard ? 1 : 0); 
                pstmtV.executeUpdate();

                // 2. Update Spot
                pstmtS.setString(1, plate);
                pstmtS.setString(2, spotID);
                pstmtS.executeUpdate();

                // 3. Generate Ticket (UPDATED FOR MALAYSIA TIME)
                // ---------------------------------------------------------
                // Create a formatter for "DayMonthYear-HourMinuteSecond"
                SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy-HHmmss");
                
                // FORCE it to use Malaysia Time (Asia/Kuala_Lumpur)
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
                
                String myTime = sdf.format(new Date(entryTime)); // e.g., 09022026-143000
                
                String ticketID = "T-" + plate + "-" + myTime; 
                // ---------------------------------------------------------

                pstmtT.setString(1, ticketID);
                pstmtT.setString(2, plate);
                pstmtT.setString(3, spotID);
                pstmtT.setLong(4, entryTime);
                pstmtT.executeUpdate();

                conn.commit(); 

                JOptionPane.showMessageDialog(this, "Vehicle Parked!\nTicket: " + ticketID);
                txtPlate.setText("");
                tableModel.setRowCount(0);
                chkHandicappedCard.setSelected(false);
                chkHandicappedCard.setEnabled(false);

            } catch (SQLException e) {
                conn.rollback(); 
                throw e;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}