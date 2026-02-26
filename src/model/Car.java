package src.model;

public class Car extends Vehicle {
    public Car(String licensePlate, boolean isVip, boolean hasHandicappedCard) {
        // Pass "Car" as the fixed type to the parent constructor
        super(licensePlate, "Car", isVip, hasHandicappedCard);
    }

    @Override
    public boolean canFitInSpot(String spotType) {
        // Cars can fit in Compact, Regular, Reserved
        return spotType.equalsIgnoreCase("Compact") || 
               spotType.equalsIgnoreCase("Regular") || 
               spotType.equalsIgnoreCase("Reserved");
    }
}