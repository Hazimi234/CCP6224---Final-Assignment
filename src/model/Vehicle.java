package src.model;

public abstract class Vehicle {
    protected String licensePlate;
    protected String vehicleType; 
    protected boolean isVip; 
    protected boolean hasHandicappedCard; // New "Double Verification" check

    public Vehicle(String licensePlate, String vehicleType, boolean isVip, boolean hasHandicappedCard) {
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.isVip = isVip;
        this.hasHandicappedCard = hasHandicappedCard;
    }

    // Physical fit check (Logic remains the same)
    public abstract boolean canFitInSpot(String spotType);

    public String getLicensePlate() { return licensePlate; }
    public String getVehicleType() { return vehicleType; }
    public boolean isVip() { return isVip; }
    public boolean hasHandicappedCard() { return hasHandicappedCard; }
}