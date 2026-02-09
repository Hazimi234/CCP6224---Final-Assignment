package src.model;

public class Vehicle {
    private String licensePlate;
    private String vehicleType; 
    private boolean isVip; 

    public Vehicle(String licensePlate, String vehicleType, boolean isVip) {
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.isVip = isVip;
    }

    // UPDATED: This now checks "Does it fit?", not "Is it legal?"
    // This allows non-VIPs to pick Reserved spots (and get fined later).
    public boolean canFitInSpot(String spotType) {
        if (vehicleType.equalsIgnoreCase("Motorcycle")) {
            return spotType.equalsIgnoreCase("Compact");
        }
        
        if (vehicleType.equalsIgnoreCase("Car")) {
            // Cars fit in Compact, Regular, AND Reserved
            return spotType.equalsIgnoreCase("Compact") || 
                   spotType.equalsIgnoreCase("Regular") || 
                   spotType.equalsIgnoreCase("Reserved");
        }
        
        if (vehicleType.equalsIgnoreCase("SUV") || vehicleType.equalsIgnoreCase("Truck")) {
            // SUVs fit in Regular AND Reserved
            return spotType.equalsIgnoreCase("Regular") || 
                   spotType.equalsIgnoreCase("Reserved");
        }
        
        if (vehicleType.equalsIgnoreCase("Handicapped Vehicle")) return true; 
        
        return false;
    }

    public String getLicensePlate() { return licensePlate; }
    public String getVehicleType() { return vehicleType; }
    public boolean isVip() { return isVip; }
}