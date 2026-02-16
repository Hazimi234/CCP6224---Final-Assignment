package src.model;

public abstract class Vehicle {
    // Protected attributes so subclasses (Car, SUV, etc.) can access them directly if needed
    protected String licensePlate;
    protected String vehicleType;
    protected boolean isVip;
    protected boolean hasHandicappedCard; // Special flag for the "Double Verification" logic

    // Constructor to initialize the vehicle's core details
    public Vehicle(String licensePlate, String vehicleType, boolean isVip, boolean hasHandicappedCard) {
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.isVip = isVip;
        this.hasHandicappedCard = hasHandicappedCard;
    }

    // This abstract method forces every subclass to define its own parking rules
    public abstract boolean canFitInSpot(String spotType);

    // Getters for accessing private data
    public String getLicensePlate() {
        return licensePlate;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public boolean isVip() {
        return isVip;
    }

    public boolean hasHandicappedCard() {
        return hasHandicappedCard;
    }
}