package src.ui;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.swing.*;
import src.model.BillData;
import src.manager.ParkingSystemFacade;

public class ExitPanel extends JPanel {
    private JTextField txtPlateSearch;
    private JTextArea txtBillArea;
    private JButton btnPay;
    private JComboBox<String> cmbPaymentMethod; 
    private ParkingSystemFacade facade = new ParkingSystemFacade();
    
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
        cmbPaymentMethod.setPreferredSize(new Dimension(75, cmbPaymentMethod.getPreferredSize().height));
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

        try {
            BillData bill = facade.calculateBill(plate);
            
            if (bill.found) {
                currentTicketID = bill.ticketID;
                currentSpotID = bill.spotID;
                totalAmountDue = bill.totalAmount;
                txtBillArea.setText(bill.billText);
                
                btnPay.setEnabled(true);
                cmbPaymentMethod.setEnabled(true);
            } else {
                JOptionPane.showMessageDialog(this, "Vehicle not found or already exited.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void processPayment() {
        String plate = txtPlateSearch.getText().trim();
        long exitTime = System.currentTimeMillis();
        String method = (String) cmbPaymentMethod.getSelectedItem(); 

        try {
            facade.processPayment(currentTicketID, currentSpotID, plate, totalAmountDue, method);
            generateReceipt(plate, exitTime, method); 
            txtBillArea.setText("Payment Successful!\nGate Opening...");
            txtPlateSearch.setText("");
            btnPay.setEnabled(false);
            cmbPaymentMethod.setEnabled(false);
        } catch (Exception ex) {
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