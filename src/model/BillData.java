package src.model;

public class BillData {
    public String ticketID;
    public String spotID;
    
    public double totalAmount;   // Total (Fee + All Fines)
    public double parkingFee;    // Current Ticket Fee
    public double currentFine;   // Fine for THIS session (Option C calculates this)
    public double pastFines;     // Unpaid fines from PREVIOUS sessions
    
    public String billText;
    public boolean found;
}