package src.model;

public class Vehicle {
    private String licensePlate;
    private String vehicleType; 
    private boolean isVip; 
    private boolean hasHandicappedCard; // New "Double Verification" check

    public Vehicle(String licensePlate, String vehicleType, boolean isVip, boolean hasHandicappedCard) {
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.isVip = isVip;
        this.hasHandicappedCard = hasHandicappedCard;
    }

    // Physical fit check (Logic remains the same)
    public boolean canFitInSpot(String spotType) {
        if (vehicleType.equalsIgnoreCase("Motorcycle")) return spotType.equalsIgnoreCase("Compact");
        
        if (vehicleType.equalsIgnoreCase("Car")) {
            return spotType.equalsIgnoreCase("Compact") || 
                   spotType.equalsIgnoreCase("Regular") || 
                   spotType.equalsIgnoreCase("Reserved");
        }
        
        if (vehicleType.equalsIgnoreCase("SUV") || vehicleType.equalsIgnoreCase("Truck")) {
            return spotType.equalsIgnoreCase("Regular") || 
                   spotType.equalsIgnoreCase("Reserved");
        }
        
        if (vehicleType.equalsIgnoreCase("Handicapped Vehicle")) return true; 
        
        return false;
    }

    public String getLicensePlate() { return licensePlate; }
    public String getVehicleType() { return vehicleType; }
    public boolean isVip() { return isVip; }
    public boolean hasHandicappedCard() { return hasHandicappedCard; }
}