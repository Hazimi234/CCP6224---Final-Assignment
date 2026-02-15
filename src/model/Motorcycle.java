package src.model;

public class Motorcycle extends Vehicle {
    public Motorcycle(String licensePlate, boolean isVip, boolean hasHandicappedCard) {
        super(licensePlate, "Motorcycle", isVip, hasHandicappedCard);
    }

    @Override
    public boolean canFitInSpot(String spotType) {
        return spotType.equalsIgnoreCase("Compact");
    }
}