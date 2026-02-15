package src.model;

public class Car extends Vehicle {
    public Car(String licensePlate, boolean isVip, boolean hasHandicappedCard) {
        super(licensePlate, "Car", isVip, hasHandicappedCard);
    }

    @Override
    public boolean canFitInSpot(String spotType) {
        return spotType.equalsIgnoreCase("Compact") || 
               spotType.equalsIgnoreCase("Regular") || 
               spotType.equalsIgnoreCase("Reserved");
    }
}