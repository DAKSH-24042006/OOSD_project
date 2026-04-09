package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.Bill;
import model.Booking;
import model.Customer;
import model.Room;
import util.CSVHandler;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CheckoutController {

    // Using raw ComboBox for max FXML compatibility
    @FXML private ComboBox cmbBooking;
    @FXML private Label lblCustomer;
    @FXML private Label lblRoom;
    @FXML private Label lblDays;
    @FXML private TextField txtExtra;
    @FXML private TextField txtDiscount;
    @FXML private Label lblTax;
    @FXML private Label lblTotal;

    private List<Booking> activeBookings = new ArrayList<>();
    private Booking selectedBooking;
    private double baseRate = 0;

    @FXML
    public void initialize() {
        System.out.println("DEBUG: CheckoutController.initialize() called.");
        // Body commented out for the first run to test injection
        /*
        try {
            loadActiveBookings();

            cmbBooking.setOnAction(e -> {
                try {
                    Object val = cmbBooking.getValue();
                    if (val == null) return;
                    Integer bid = (Integer) val;
                    
                    selectedBooking = CSVHandler.loadBookings().stream()
                        .filter(b -> b.getBookingId() == bid)
                        .findFirst().orElse(null);
                        
                    if (selectedBooking != null) {
                        Customer c = CSVHandler.getCustomerById(selectedBooking.getCustomerId());
                        lblCustomer.setText(c != null ? c.getName() : "Unknown Customer");
                        
                        Room r = CSVHandler.getRoomById(selectedBooking.getRoomId());
                        if (r != null) {
                            lblRoom.setText("Room " + r.getRoomId() + " (" + r.getType() + ")");
                            long days = ChronoUnit.DAYS.between(selectedBooking.getCheckIn(), selectedBooking.getCheckOut());
                            if (days <= 0) days = 1;
                            lblDays.setText(String.valueOf(days));
                            baseRate = r.getPrice() * days;
                        } else {
                            lblRoom.setText("Unknown Room");
                            lblDays.setText("-");
                            baseRate = 0;
                        }
                        calculateBill();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            if (txtExtra != null) txtExtra.textProperty().addListener((obs, oldV, newV) -> calculateBill());
            if (txtDiscount != null) txtDiscount.textProperty().addListener((obs, oldV, newV) -> calculateBill());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        */
    }

    private void loadActiveBookings() {
        // Method body commented out
    }

    private void calculateBill() {
        // Method body commented out
    }

    @FXML
    private void handleCheckout() {
        // Method body commented out
        System.out.println("DEBUG: handleCheckout clicked");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
