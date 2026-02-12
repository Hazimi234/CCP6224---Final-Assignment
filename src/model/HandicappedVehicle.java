package src.model;

public class HandicappedVehicle extends Vehicle {
    public HandicappedVehicle(String licensePlate, boolean isVip, boolean hasHandicappedCard) {
        super(licensePlate, "Handicapped Vehicle", isVip, hasHandicappedCard);
    }

    @Override
    public boolean canFitInSpot(String spotType) {
        return true;
    }
}