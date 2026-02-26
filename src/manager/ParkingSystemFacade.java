package src.manager;

import java.sql.*;
import java.util.*;
import src.model.BillData;
import src.model.Vehicle;

public class ParkingSystemFacade {

    // Managers
    private EntryManager entryManager = new EntryManager();
    private ExitManager exitManager = new ExitManager();
    private AdminManager adminManager = new AdminManager();
    private ReportManager reportManager = new ReportManager();
    private MapManager mapManager = new MapManager();

    // --- ENTRY FACADE METHODS ---
    public boolean isVehicleAlreadyParked(String plate) {
        return entryManager.isVehicleAlreadyParked(plate);
    }

    public List<Vector<Object>> findMatchingSpots(Vehicle vehicle) {
        return entryManager.findMatchingSpots(vehicle);
    }

    public String parkVehicle(Vehicle vehicle, String spotID) throws SQLException {
        return entryManager.parkVehicle(vehicle, spotID);
    }

    // --- EXIT FACADE METHODS ---
    public BillData calculateBill(String plate) throws SQLException {
        return exitManager.calculateBill(plate);
    }

    public void processPayment(String ticketID, String spotID, String plate, double paidAmount, double feeCost, String method) throws SQLException {
        exitManager.processPayment(ticketID, spotID, plate, paidAmount, feeCost, method);
    }

    // --- ADMIN FACADE METHODS ---
    public String loadSavedStrategy() { return adminManager.loadSavedStrategy(); }
    public void saveStrategy(String val) { adminManager.saveStrategy(val); }
    public double getTotalRevenue() { return adminManager.getTotalRevenue(); }
    public List<Object[]> getLiveVehicles() { return adminManager.getLiveVehicles(); }
    public void runComplianceScan() { adminManager.runComplianceScan(); }
    public List<Map<String, Object>> getMapSpots() { return mapManager.getMapSpots(); }

    // --- REPORT FACADE METHODS ---
    public List<Object[]> getParkedVehiclesReport() { return reportManager.getParkedVehicles(); }
    public Map<String, Object> getRevenueReport(String type, String method) { return reportManager.getRevenueData(type, method); }
    public Map<String, List<Object[]>> getOccupancyReport() { return reportManager.getOccupancyData(); }
    public List<Object[]> getFinesReport() { return reportManager.getFinesData(); }
}