package src.model;

public class HandicappedVehicle extends Vehicle {
    public HandicappedVehicle(String licensePlate, boolean isVip, boolean hasHandicappedCard) {
        // Pass "HandicappedVehicle" as the fixed type to the parent constructor
        super(licensePlate, "Handicapped Vehicle", isVip, hasHandicappedCard);
    }

    @Override
    public boolean canFitInSpot(String spotType) {
        // A handicapped vehicle typically has the right to park anywhere
        // Returning 'true' means it is physically capable of fitting in any spot type
        return true;
    }
}