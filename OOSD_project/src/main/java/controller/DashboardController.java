package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import model.Bill;
import model.Booking;
import model.Room;
import util.CSVHandler;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label lblTotalRooms;
    @FXML private Label lblActiveBookings;
    @FXML private Label lblTotalRevenue;
    @FXML private PieChart pieOccupancy;
    @FXML private BarChart<String, Number> barRevenue;

    @FXML
    public void initialize() {
        refreshDashboard();
    }

    private void refreshDashboard() {
        List<Room> rooms = CSVHandler.loadRooms();
        List<Booking> bookings = CSVHandler.loadBookings();
        List<Bill> bills = CSVHandler.loadBills();

        lblTotalRooms.setText(String.valueOf(rooms.size()));
        
        long active = bookings.stream()
                .filter(b -> "Active".equalsIgnoreCase(b.getStatus()))
                .filter(b -> bills.stream().noneMatch(bill -> bill.getBookingId() == b.getBookingId()))
                .count();
        lblActiveBookings.setText(String.valueOf(active));

        double totalRev = bills.stream().mapToDouble(Bill::getTotalAmount).sum();
        lblTotalRevenue.setText(String.format("$%.2f", totalRev));

        long available = rooms.stream().filter(r -> "Available".equalsIgnoreCase(r.getStatus())).count();
        long booked = rooms.stream().filter(r -> "Booked".equalsIgnoreCase(r.getStatus())).count();
        long maintenance = rooms.stream().filter(r -> "Maintenance".equalsIgnoreCase(r.getStatus())).count();

        pieOccupancy.setData(FXCollections.observableArrayList(
                new PieChart.Data("Available (" + available + ")", available),
                new PieChart.Data("Booked (" + booked + ")", booked),
                new PieChart.Data("Maintenance (" + maintenance + ")", maintenance)
        ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Last 7 Days Revenue");

        LocalDate today = LocalDate.now();
        Map<String, Double> revenueMap = bills.stream()
                .filter(b -> b.getBillDate() != null)
                .collect(Collectors.groupingBy(Bill::getBillDate, Collectors.summingDouble(Bill::getTotalAmount)));

        for (int i = 6; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            series.getData().add(new XYChart.Data<>(date, revenueMap.getOrDefault(date, 0.0)));
        }

        barRevenue.getData().clear();
        barRevenue.getData().add(series);
    }

    @FXML
    private void handleSync() {
        try {
            CSVHandler.syncAllToDB();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Synchronization Success");
            alert.setHeaderText(null);
            alert.setContentText("All data has been successfully synchronized to the MySQL database.");
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Synchronization Failed");
            alert.setHeaderText("Database Error");
            alert.setContentText("An error occurred during synchronization: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }
}
