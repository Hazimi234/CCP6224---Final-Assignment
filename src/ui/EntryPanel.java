package src.ui;

import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.table.DefaultTableModel; 
import src.manager.ParkingSystemFacade;
import src.model.*;

public class EntryPanel extends JPanel {
    private JTextField txtPlate;
    private JComboBox<String> cmbType;
    private JCheckBox chkVip;
    private JCheckBox chkHandicappedCard; 
    private JTable spotTable;
    private DefaultTableModel tableModel;
    private JButton btnPark;
    private ParkingSystemFacade facade = new ParkingSystemFacade(); //Facade to handle backend operations

    public EntryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- TOP: Input Form ---
        JPanel topContainer = new JPanel(new BorderLayout());
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
        btnFind.setPreferredSize(new Dimension(250, 35));
        btnFind.addActionListener(e -> findSpots());
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(btnFind);
        topContainer.add(formPanel, BorderLayout.CENTER);
        topContainer.add(btnPanel, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

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
    //Reads the form data, validates the license plate, checks for duplicates
    private void findSpots() {
        String plate = txtPlate.getText().trim();
        if (plate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a license plate first.");
            return;
        }


        if (facade.isVehicleAlreadyParked(plate)) {
            JOptionPane.showMessageDialog(this, 
                "ERROR: Vehicle " + plate + " is already inside the parking lot!", 
                "Duplicate Entry", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String type = (String) cmbType.getSelectedItem();
        boolean isVip = chkVip.isSelected();
        boolean hasCard = chkHandicappedCard.isSelected();
        
        Vehicle tempVehicle;
        switch (type) {
            case "Car": tempVehicle = new Car(plate, isVip, hasCard); break;
            case "Motorcycle": tempVehicle = new Motorcycle(plate, isVip, hasCard); break;
            case "SUV": tempVehicle = new SUV(plate, isVip, hasCard); break;
            case "Handicapped Vehicle": tempVehicle = new HandicappedVehicle(plate, isVip, hasCard); break;
            default: tempVehicle = new Car(plate, isVip, hasCard); break;
        }
        
        tableModel.setRowCount(0); 
        
        //Fetch matching spots from the database from the facade
        java.util.List<Vector<Object>> spots = facade.findMatchingSpots(tempVehicle);
        for (Vector<Object> row : spots) {
            tableModel.addRow(row);
        }
    }
    //Processes the final parking action. Registers the vehicle in the database, marks the selected spot as occupied, and generates a ticket.
    private void parkVehicle() {
        int selectedRow = spotTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a spot from the table.");
            return;
        }

        String plate = txtPlate.getText().trim().toUpperCase();
        
        //Double check for duplicates in case the user edited the text box after clicking "Find"
        if (facade.isVehicleAlreadyParked(plate)) {
            JOptionPane.showMessageDialog(this, 
                "ERROR: Vehicle " + plate + " is already inside!", 
                "Duplicate Entry", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String spotID = (String) tableModel.getValueAt(selectedRow, 0);
        String type = (String) cmbType.getSelectedItem();
        boolean isVip = chkVip.isSelected();
        boolean hasCard = chkHandicappedCard.isSelected(); 
        
        //Build the vehicle model
        Vehicle vehicle;
        switch (type) {
            case "Car": vehicle = new Car(plate, isVip, hasCard); break;
            case "Motorcycle": vehicle = new Motorcycle(plate, isVip, hasCard); break;
            case "SUV": vehicle = new SUV(plate, isVip, hasCard); break;
            case "Handicapped Vehicle": vehicle = new HandicappedVehicle(plate, isVip, hasCard); break;
            default: vehicle = new Car(plate, isVip, hasCard); break;
        }
        //Attempt to park the vehicle and show success message
        try {
            String ticketID = facade.parkVehicle(vehicle, spotID);
            JOptionPane.showMessageDialog(this, "Vehicle Parked!\nTicket: " + ticketID);
            //Reset form for next entry
            txtPlate.setText("");
            tableModel.setRowCount(0);
            chkHandicappedCard.setSelected(false);
            chkHandicappedCard.setEnabled(false);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}