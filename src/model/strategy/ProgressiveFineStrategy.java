package src.model.strategy;

public class ProgressiveFineStrategy implements FineStrategy {
    @Override
    public double calculateFine(double hoursParked) {
        // According to "Fine Management" 
        // Fines are charged if the vehicle stays more than 24 hours.
        if (hoursParked <= 24) {
            return 0.0; // No fine if under 24 hours
        }

        double fine = 0.0;

        // Tier 1: The base fine for overstaying (First 24 hours of fineable time)
        // "First 24 hours: RM 50"
        if (hoursParked > 24) {
            fine += 50.0;
        }

        // Tier 2: "Hours 24-48: Additional RM 100" 
        if (hoursParked > 48) {
            fine += 100.0;
        }

        // Tier 3: "Hours 48-72: Additional RM 150" 
        if (hoursParked > 72) {
            fine += 150.0;
        }

        // Tier 4: "Above 72 hours: Additional RM 200"
        if (hoursParked > 96) {
             fine += 200.0; 
        }

        return fine;
    }

    @Override
    public String getName() {
        return "Option B: Progressive Scheme";
    }
}