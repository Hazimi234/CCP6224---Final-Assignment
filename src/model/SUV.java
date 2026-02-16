package src.model;

public class SUV extends Vehicle {
    public SUV(String licensePlate, boolean isVip, boolean hasHandicappedCard) {
        // Pass "SUV" as the fixed type to the parent constructor
        super(licensePlate, "SUV", isVip, hasHandicappedCard);
    }

    @Override
    public boolean canFitInSpot(String spotType) {
        // SUV can fit in Regular, Reserved
        return spotType.equalsIgnoreCase("Regular") || 
               spotType.equalsIgnoreCase("Reserved");
    }
}