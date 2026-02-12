package src.model.strategy;

public interface FineStrategy {
    double calculateFine(double hoursOverstayed);
    String getName(); // To show in the Admin dropdown
}