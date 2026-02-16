package src.model;

public class BillData {
    public String ticketID;
    public String spotID;
    public double totalAmount; // The final calculated cost (Fees + Fines)
    public String billText; // The full receipt text/invoice summary to display in the text area
    public boolean found; // A flag to check if the ticket was actually found in the database
}