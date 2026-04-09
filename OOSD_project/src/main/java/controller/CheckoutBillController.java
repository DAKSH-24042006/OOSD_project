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

public class CheckoutBillController {

    @FXML public ComboBox<Integer> bookingSelector;
    @FXML public Label customerDisplay;
    @FXML public Label roomDisplay;
    @FXML public Label daysDisplay;
    @FXML public TextField extraChargesInput;
    @FXML public TextField discountInput;
    @FXML public Label taxDisplay;
    @FXML public Label totalDisplay;

    private List<Booking> currentBookings = new ArrayList<>();
    private Booking selectedBooking;
    private double currentBaseRate = 0;

    @FXML
    public void initialize() {
        try {
            refreshBookingList();

            bookingSelector.setOnAction(e -> {
                Integer bid = bookingSelector.getValue();
                if (bid != null) {
                    processBookingSelection(bid);
                }
            });

            extraChargesInput.textProperty().addListener((obs, oldV, newV) -> runBillingCalculation());
            discountInput.textProperty().addListener((obs, oldV, newV) -> runBillingCalculation());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void refreshBookingList() {
        try {
            List<Bill> allBills = CSVHandler.loadBills();
            List<Booking> bookings = CSVHandler.loadBookings();
            
            currentBookings = bookings.stream()
                    .filter(b -> !"Cancelled".equalsIgnoreCase(b.getStatus()))
                    .filter(b -> allBills.stream().noneMatch(bill -> bill.getBookingId() == b.getBookingId()))
                    .collect(Collectors.toList());
                    
            List<Integer> ids = currentBookings.stream()
                    .map(Booking::getBookingId)
                    .collect(Collectors.toList());
            
            if (bookingSelector != null) {
                bookingSelector.setItems(FXCollections.observableArrayList(ids));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processBookingSelection(int bookingId) {
        selectedBooking = CSVHandler.getBookingById(bookingId);
        if (selectedBooking != null) {
            Customer c = CSVHandler.getCustomerById(selectedBooking.getCustomerId());
            if (customerDisplay != null) customerDisplay.setText(c != null ? c.getName() : "Unknown");
            
            Room r = CSVHandler.getRoomById(selectedBooking.getRoomId());
            if (r != null) {
                if (roomDisplay != null) roomDisplay.setText("Room " + r.getRoomId() + " (" + r.getType() + ")");
                long days = ChronoUnit.DAYS.between(selectedBooking.getCheckIn(), selectedBooking.getCheckOut());
                if (days <= 0) days = 1;
                if (daysDisplay != null) daysDisplay.setText(String.valueOf(days));
                currentBaseRate = r.getPrice() * days;
            } else {
                if (roomDisplay != null) roomDisplay.setText("N/A");
                if (daysDisplay != null) daysDisplay.setText("-");
                currentBaseRate = 0;
            }
            runBillingCalculation();
        }
    }

    private void runBillingCalculation() {
        try {
            double extra = 0;
            try { 
                if (extraChargesInput != null && !extraChargesInput.getText().isEmpty()) 
                    extra = Double.parseDouble(extraChargesInput.getText());
            } catch (Exception ignored) {}
            
            double discPercent = 0;
            try {
                if (discountInput != null && !discountInput.getText().isEmpty()) 
                    discPercent = Double.parseDouble(discountInput.getText());
            } catch (Exception ignored) {}
            
            double subtotal = currentBaseRate + extra;
            double discountAmount = subtotal * (discPercent / 100.0);
            double taxableBalance = subtotal - discountAmount;
            double calculatedTax = taxableBalance * 0.18;
            double finalTotal = taxableBalance + calculatedTax;

            if (taxDisplay != null) taxDisplay.setText(String.format("$%.2f", calculatedTax));
            if (totalDisplay != null) totalDisplay.setText(String.format("$%.2f", finalTotal));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onCompleteCheckout() {
        if (selectedBooking == null) return;
        
        try {
            double extra = extraChargesInput.getText().isEmpty() ? 0 : Double.parseDouble(extraChargesInput.getText());
            double disc = discountInput.getText().isEmpty() ? 0 : Double.parseDouble(discountInput.getText());
            String totalStr = totalDisplay.getText().replace("$", "");
            double total = totalStr.equals("-") ? 0 : Double.parseDouble(totalStr);
            String taxStr = taxDisplay.getText().replace("$", "");
            double tax = taxStr.equals("-") ? 0 : Double.parseDouble(taxStr);

            Bill newBill = new Bill(CSVHandler.nextBillId(), selectedBooking.getBookingId(), total, 
                                LocalDate.now().toString(), tax, disc, extra);
            
            List<Bill> history = CSVHandler.loadBills();
            history.add(newBill);
            CSVHandler.saveBills(history);
            persistBillToDB(newBill);

            Room room = CSVHandler.getRoomById(selectedBooking.getRoomId());
            if (room != null) {
                room.setStatus("Available");
                List<Room> allRooms = CSVHandler.loadRooms();
                CSVHandler.saveRooms(allRooms.stream()
                    .map(rm -> rm.getRoomId() == room.getRoomId() ? room : rm)
                    .collect(Collectors.toList()));
                syncRoomStatusToDB(room.getRoomId(), "Available");
            }

            displayMessage("Success", "Checkout handled. Bill ID: " + newBill.getBillId());
            resetForm();
            refreshBookingList();
        } catch (Exception e) {
            displayMessage("Error", "Action failed: " + e.getMessage());
        }
    }

    private void persistBillToDB(Bill b) {
        String sql = "INSERT INTO billing (billId, bookingId, totalAmount, billDate, tax, discount, extraCharges) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            synchronized (DBConnection.getDbMonitor()) {
                pstmt.setInt(1, b.getBillId());
                pstmt.setInt(2, b.getBookingId());
                pstmt.setDouble(3, b.getTotalAmount());
                pstmt.setString(4, b.getBillDate());
                pstmt.setDouble(5, b.getTax());
                pstmt.setDouble(6, b.getDiscount());
                pstmt.setDouble(7, b.getExtraCharges());
                pstmt.executeUpdate();
            }
        } catch (SQLException ignored) {}
    }

    private void syncRoomStatusToDB(int roomId, String status) {
        String sql = "UPDATE rooms SET status=? WHERE roomId=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            synchronized (DBConnection.getDbMonitor()) {
                pstmt.setString(1, status);
                pstmt.setInt(2, roomId);
                pstmt.executeUpdate();
            }
        } catch (SQLException ignored) {}
    }

    private void resetForm() {
        if (bookingSelector != null) bookingSelector.getSelectionModel().clearSelection();
        if (customerDisplay != null) customerDisplay.setText("-");
        if (roomDisplay != null) roomDisplay.setText("-");
        if (daysDisplay != null) daysDisplay.setText("-");
        if (extraChargesInput != null) extraChargesInput.setText("0.0");
        if (discountInput != null) discountInput.setText("0");
        if (taxDisplay != null) taxDisplay.setText("$0.00");
        if (totalDisplay != null) totalDisplay.setText("$0.00");
        selectedBooking = null;
    }

    private void displayMessage(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
