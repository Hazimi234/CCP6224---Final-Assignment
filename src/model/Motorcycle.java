package src.model;

public class Motorcycle extends Vehicle {
    public Motorcycle(String licensePlate, boolean isVip, boolean hasHandicappedCard) {
        // Pass "Motorcycle" as the fixed type to the parent constructor
        super(licensePlate, "Motorcycle", isVip, hasHandicappedCard);
    }

    @Override
    public boolean canFitInSpot(String spotType) {
        // Can only fit in Compact
        return spotType.equalsIgnoreCase("Compact");
    }
}