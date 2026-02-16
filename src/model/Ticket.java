package src.model;

import java.sql.Timestamp;

public class Ticket {
    private String ticketID;
    private String licensePlate;
    private String vehicleType;
    private String spotID;
    private long entryTimeMillis; // Easier to calculate duration with milliseconds

    public Ticket(String licensePlate, String vehicleType, String spotID, long entryTimeMillis) {
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.spotID = spotID;
        this.entryTimeMillis = entryTimeMillis;

        // Auto-generate a unique ID combining Plate + Time
        // Example Result: "T-ABC1234-1709923400"
        this.ticketID = "T-" + licensePlate + "-" + entryTimeMillis; 
    }

    // Getters
    public String getTicketID() { return ticketID; }
    public String getLicensePlate() { return licensePlate; }
    public String getVehicleType() { return vehicleType; }
    public String getSpotID() { return spotID; }
    public long getEntryTime() { return entryTimeMillis; }

    // Helper to get a readable string of the time (e.g., "2026-02-16 10:00:00")
    public String getFormattedEntryTime() {
        return new Timestamp(entryTimeMillis).toString();
    }
}