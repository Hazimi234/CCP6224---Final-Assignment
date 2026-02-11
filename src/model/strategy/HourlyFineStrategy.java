package src.model.strategy;

public class HourlyFineStrategy implements FineStrategy {
    @Override
    public double calculateFine(double hoursOverstayed) {
        double billableHours = hoursOverstayed - 24;
        if (billableHours <= 0) return 0;
        return billableHours * 20.0; // RM 20 per hour
    }

    @Override
    public String getName() {
        return "Option C: Hourly Fine (RM 20/hr)";
    }
}