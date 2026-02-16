package src.ui;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.swing.*;
import src.manager.AdminManager;
import src.manager.ParkingSystemFacade;
import src.model.BillData;
import src.model.strategy.HourlyFineStrategy;

//ExitPanel provides UI for vehicles leaving the parking lot

public class ExitPanel extends JPanel {
    private JTextField txtPlateSearch;
    private JTextArea txtBillArea;
    private JButton btnPay;
    private JComboBox<String> cmbPaymentMethod;
    private JTextField txtPayAmount;
    private JLabel lblMinPayment;
    
    private ParkingSystemFacade facade = new ParkingSystemFacade();
    
    private String currentTicketID;
    private String currentSpotID;
    private double minRequired = 0.0;
    private double parkingFeeOnly = 0.0;

    public ExitPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- TOP ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Enter License Plate to Exit:"));
        txtPlateSearch = new JTextField(15);
        searchPanel.add(txtPlateSearch);
        
        JButton btnSearch = new JButton("Calculate Bill");
        btnSearch.addActionListener(e -> calculateBill());
        searchPanel.add(btnSearch);
        
        add(searchPanel, BorderLayout.NORTH);

        // --- CENTER ---
        txtBillArea = new JTextArea();
        txtBillArea.setEditable(false);
        txtBillArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        txtBillArea.setBorder(BorderFactory.createTitledBorder("Payment Details"));
        add(new JScrollPane(txtBillArea), BorderLayout.CENTER);

        // --- BOTTOM ---
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        JPanel amountPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        amountPanel.add(new JLabel("Payment Amount (RM):"));
        txtPayAmount = new JTextField(8);
        txtPayAmount.setEnabled(false);
        amountPanel.add(txtPayAmount);
        
        lblMinPayment = new JLabel("(Min: RM 0.00)");
        lblMinPayment.setForeground(Color.RED);
        amountPanel.add(lblMinPayment);
        bottomPanel.add(amountPanel);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(new JLabel("Method:"));
        String[] methods = {"Cash", "Card"}; 
        cmbPaymentMethod = new JComboBox<>(methods);
        cmbPaymentMethod.setEnabled(false); //false until bill is calculated
        actionPanel.add(cmbPaymentMethod);

        btnPay = new JButton("CONFIRM PAYMENT & EXIT");
        btnPay.setFont(new Font("Arial", Font.BOLD, 14));
        btnPay.setBackground(new Color(255, 100, 100)); 
        btnPay.setEnabled(false); 
        btnPay.addActionListener(e -> processPayment());
        actionPanel.add(btnPay);
        
        bottomPanel.add(actionPanel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    //finds corresponding license plate, and calculates bill, and fills ui with breakdown of fees and minimum amount required to pay before exiting
    private void calculateBill() {
        String plate = txtPlateSearch.getText().trim();
        if (plate.isEmpty()) return;

        txtBillArea.setText("");
        btnPay.setEnabled(false);
        cmbPaymentMethod.setEnabled(false);
        txtPayAmount.setEnabled(false);
        txtPayAmount.setText("");

        try {
            BillData bill = facade.calculateBill(plate);
            
            if (bill.found) {
                currentTicketID = bill.ticketID;
                currentSpotID = bill.spotID;
                parkingFeeOnly = bill.parkingFee;
                
                // --- NEW MINIMUM PAYMENT LOGIC ---
                boolean isOptionC = (AdminManager.currentFineStrategy instanceof HourlyFineStrategy);
                
                if (isOptionC) {
                    // Option C: Min = Ticket + Past Fines (Current Fine is optional) 
                    minRequired = bill.parkingFee + bill.pastFines;
                    lblMinPayment.setText("(Min: RM " + String.format("%.2f", minRequired) + " [Ticket + Past Due])");
                } else {
                    // Option A/B: Min = Total (Everything)/ driver must pay the whole amount
                    minRequired = bill.totalAmount;
                    lblMinPayment.setText("(Min: RM " + String.format("%.2f", minRequired) + " [Full Bill])");
                }

                txtBillArea.setText(bill.billText);
                txtPayAmount.setText(String.format("%.2f", bill.totalAmount)); 
                
                btnPay.setEnabled(true);
                cmbPaymentMethod.setEnabled(true);
                txtPayAmount.setEnabled(true);
            } else {
                JOptionPane.showMessageDialog(this, "Vehicle not found or already exited.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    //checks input amount if passes minimum requirement, process the payment, vacates the spot & generates a receipt if successful 
    private void processPayment() {
        String plate = txtPlateSearch.getText().trim();
        long exitTime = System.currentTimeMillis();
        String method = (String) cmbPaymentMethod.getSelectedItem(); 
        
        double paidAmount = 0;
        try {
            paidAmount = Double.parseDouble(txtPayAmount.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Amount Entered!");
            return;
        }

        try {
            facade.processPayment(currentTicketID, currentSpotID, plate, paidAmount, parkingFeeOnly, method);
            
            generateReceipt(plate, exitTime, method, paidAmount); 
            
            //success reset
            txtBillArea.setText("Payment Successful!\nGate Opening...");
            txtPlateSearch.setText("");
            btnPay.setEnabled(false);
            cmbPaymentMethod.setEnabled(false);
            txtPayAmount.setEnabled(false);
            lblMinPayment.setText("(Min: RM 0.00)");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Payment Error: " + ex.getMessage());
        }
    }

    //display receipt upon success
    private void generateReceipt(String plate, long time, String method, double paid) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
        String timeStr = sdf.format(new Date(time));

        String msg = "RECEIPT\n" +
                     "--------------------\n" +
                     "Plate: " + plate + "\n" +
                     "Method: " + method + "\n" + 
                     "Paid: RM " + String.format("%.2f", paid) + "\n" +
                     "Time: " + timeStr + "\n" +
                     "--------------------\n" +
                     "Thank you!";
        
        JOptionPane.showMessageDialog(this, msg);
    }
}