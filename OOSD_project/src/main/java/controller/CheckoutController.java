package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Bill;
import model.Booking;
import model.Customer;
import model.Room;
import util.CSVHandler;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class CheckoutController {

    @FXML private ComboBox<Booking> cmbBooking;
    @FXML private Label lblCustomerDetails;
    @FXML private Label lblRoomDetails;
    @FXML private Label lblDays;
    @FXML private Label lblTotal;

    private ObservableList<Booking> activeBookings;
    private double calculatedTotal = 0.0;
    private Booking selectedBooking = null;

    @FXML
    public void initialize() {
        loadBookings();
        cmbBooking.setOnAction(e -> updateDetails());
    }

    private void loadBookings() {
        List<Booking> allBookings = CSVHandler.loadBookings();
        List<Bill> allBills = CSVHandler.loadBills();
        activeBookings = FXCollections.observableArrayList();

        for (Booking b : allBookings) {
            boolean billed = allBills.stream().anyMatch(bill -> bill.getBookingId() == b.getBookingId());
            if (!billed) {
                activeBookings.add(b);
            }
        }
        cmbBooking.setItems(activeBookings);
    }

    private void updateDetails() {
        selectedBooking = cmbBooking.getValue();
        if (selectedBooking == null) {
            clearDetails();
            return;
        }

        Customer c = CSVHandler.getCustomerById(selectedBooking.getCustomerId());
        Room r = CSVHandler.getRoomById(selectedBooking.getRoomId());

        if (c != null) {
            lblCustomerDetails.setText(c.getName() + " - " + c.getPhone());
        }
        if (r != null) {
            lblRoomDetails.setText("Room " + r.getRoomId() + " (" + r.getType() + ") - $" + r.getPrice() + "/day");
            long days = ChronoUnit.DAYS.between(selectedBooking.getCheckIn(), selectedBooking.getCheckOut());
            if (days <= 0) days = 1;
            lblDays.setText(String.valueOf(days));
            calculatedTotal = days * r.getPrice();
            lblTotal.setText(String.format("$%.2f", calculatedTotal));
        }
    }

    @FXML
    private void handleCheckout() {
        if (selectedBooking == null) {
            showAlert("Error", "Please select a booking.");
            return;
        }

        List<Bill> bills = CSVHandler.loadBills();
        int newBillId = bills.size() + 1;
        Bill newBill = new Bill(newBillId, selectedBooking.getBookingId(), calculatedTotal);
        bills.add(newBill);
        CSVHandler.saveBills(bills);
        saveBillToDB(newBill);

        List<Room> rooms = CSVHandler.loadRooms();
        for (Room r : rooms) {
            if (r.getRoomId() == selectedBooking.getRoomId()) {
                r.setStatus("Available");
                updateRoomStatusInDB(r.getRoomId(), "Available");
                break;
            }
        }
        CSVHandler.saveRooms(rooms);

        System.out.println("Checkout Completed");
        showAlert("Success", "Checkout successful. Total amount: $" + calculatedTotal);
        loadBookings();
        clearDetails();
        cmbBooking.getSelectionModel().clearSelection();
    }

    private void saveBillToDB(Bill bill) {
        String sql = "INSERT INTO billing (billId, bookingId, totalAmount) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bill.getBillId());
            pstmt.setInt(2, bill.getBookingId());
            pstmt.setDouble(3, bill.getTotalAmount());
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    private void updateRoomStatusInDB(int roomId, String status) {
        String sql = "UPDATE rooms SET status = ? WHERE roomId = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, roomId);
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    private void clearDetails() {
        lblCustomerDetails.setText("-");
        lblRoomDetails.setText("-");
        lblDays.setText("-");
        lblTotal.setText("-");
        calculatedTotal = 0.0;
        selectedBooking = null;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(title.equals("Success") ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
