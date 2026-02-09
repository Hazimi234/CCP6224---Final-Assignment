package src.model;

public class ParkingSpot {
    private String spotID;
    private String spotType;
    private String currentVehiclePlate; // Replaces the generic 'boolean occupied'
    private double hourlyRate;

    public ParkingSpot(String spotID, String spotType, String currentVehiclePlate, double hourlyRate) {
        this.spotID = spotID;
        this.spotType = spotType;
        this.currentVehiclePlate = currentVehiclePlate;
        this.hourlyRate = hourlyRate;
    }

    // Logic: It is occupied if the plate is NOT null
    public boolean isOccupied() {
        return currentVehiclePlate != null && !currentVehiclePlate.isEmpty();
    }

    // Getters
    public String getSpotID() { return spotID; }
    public String getSpotType() { return spotType; }
    public String getCurrentVehiclePlate() { return currentVehiclePlate; }
    public double getHourlyRate() { return hourlyRate; }
}