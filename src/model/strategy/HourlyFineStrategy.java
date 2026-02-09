package src.model.strategy;

public class HourlyFineStrategy implements FineStrategy {
    @Override
    public double calculateFine(double hoursOverstayed) {
        if (hoursOverstayed <= 0) return 0;
        return hoursOverstayed * 20.0; // RM 20 per hour
    }

    @Override
    public String getName() {
        return "Option C: Hourly Fine (RM 20/hr)";
    }
}