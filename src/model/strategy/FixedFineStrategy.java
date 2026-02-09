package src.model.strategy;

public class FixedFineStrategy implements FineStrategy {
    @Override
    public double calculateFine(double hoursOverstayed) {
        if (hoursOverstayed <= 0) return 0;
        return 50.0; // Flat fee regardless of how many hours
    }

    @Override
    public String getName() {
        return "Option A: Fixed Fine (RM 50)";
    }
}