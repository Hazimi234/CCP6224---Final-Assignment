package src.model;

public class SUV extends Vehicle {
    public SUV(String licensePlate, boolean isVip, boolean hasHandicappedCard) {
        super(licensePlate, "SUV", isVip, hasHandicappedCard);
    }

    @Override
    public boolean canFitInSpot(String spotType) {
        return spotType.equalsIgnoreCase("Regular") || 
               spotType.equalsIgnoreCase("Reserved");
    }
}