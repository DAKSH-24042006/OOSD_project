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

public class BillingProcessController {

    @FXML public ComboBox<Integer> bookingIdList;
    @FXML public Label guestNameLabel;
    @FXML public Label roomInfoLabel;
    @FXML public TextField stayDurationField;
    @FXML public TextField extraCostField;
    @FXML public TextField discountPercentField;
    @FXML public Label taxAmountLabel;
    @FXML public Label finalTotalLabel;

    private List<Booking> eligibleBookings = new ArrayList<>();
    private Booking activeSelection;
    private double calculatedBaseRate = 0;

    @FXML
    public void initialize() {
        try {
            updateBookingOptions();
            if (bookingIdList != null) {
                bookingIdList.setOnAction(e -> {
                    Integer bid = bookingIdList.getValue();
                    if (bid != null) {
                        onBookingChosen(bid);
                    }
                });
            }
            if (extraCostField != null) extraCostField.textProperty().addListener((obs, oldV, newV) -> updateCalculations());
            if (discountPercentField != null) discountPercentField.textProperty().addListener((obs, oldV, newV) -> updateCalculations());
            if (stayDurationField != null) stayDurationField.textProperty().addListener((obs, oldV, newV) -> updateCalculations());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateBookingOptions() {
        try {
            List<Bill> existingBills = CSVHandler.loadBills();
            List<Booking> allBookings = CSVHandler.loadBookings();
            eligibleBookings = allBookings.stream()
                    .filter(b -> !"Cancelled".equalsIgnoreCase(b.getStatus()))
                    .filter(b -> existingBills.stream().noneMatch(bill -> bill.getBookingId() == b.getBookingId()))
                    .collect(Collectors.toList());
            List<Integer> ids = eligibleBookings.stream()
                    .map(Booking::getBookingId)
                    .collect(Collectors.toList());
            if (bookingIdList != null) {
                bookingIdList.setItems(FXCollections.observableArrayList(ids));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onBookingChosen(int bid) {
        activeSelection = CSVHandler.getBookingById(bid);
        if (activeSelection != null) {
            Customer c = CSVHandler.getCustomerById(activeSelection.getCustomerId());
            if (guestNameLabel != null) guestNameLabel.setText(c != null ? c.getName() : "Unknown");
            Room r = CSVHandler.getRoomById(activeSelection.getRoomId());
            if (r != null) {
                if (roomInfoLabel != null) roomInfoLabel.setText("Room " + r.getRoomId() + " (" + r.getType() + ")");
                long days = ChronoUnit.DAYS.between(activeSelection.getCheckIn(), activeSelection.getCheckOut());
                if (days <= 0) days = 1;
                if (stayDurationField != null) stayDurationField.setText(String.valueOf(days));
            } else {
                if (roomInfoLabel != null) roomInfoLabel.setText("-");
                if (stayDurationField != null) stayDurationField.setText("0");
            }
            updateCalculations();
        }
    }

    private void updateCalculations() {
        try {
            double duration = 0;
            try {
                if (stayDurationField != null && !stayDurationField.getText().isEmpty()) 
                    duration = Double.parseDouble(stayDurationField.getText());
            } catch (Exception ignored) {}

            Room r = activeSelection != null ? CSVHandler.getRoomById(activeSelection.getRoomId()) : null;
            double baseRate = (r != null) ? r.getPrice() * duration : 0;

            double extra = 0;
            try { 
                if (extraCostField != null && !extraCostField.getText().isEmpty()) extra = Double.parseDouble(extraCostField.getText());
            } catch (Exception ignored) {}
            double discPercent = 0;
            try {
                if (discountPercentField != null && !discountPercentField.getText().isEmpty()) discPercent = Double.parseDouble(discountPercentField.getText());
            } catch (Exception ignored) {}
            double subtotal = baseRate + extra;
            double discAmount = subtotal * (discPercent / 100.0);
            double taxable = subtotal - discAmount;
            double tax = taxable * 0.18;
            double total = taxable + tax;
            if (taxAmountLabel != null) taxAmountLabel.setText(String.format("$%.2f", tax));
            if (finalTotalLabel != null) finalTotalLabel.setText(String.format("$%.2f", total));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void submitCheckout() {
        if (activeSelection == null) return;
        try {
            double extra = extraCostField.getText().isEmpty() ? 0 : Double.parseDouble(extraCostField.getText());
            double disc = discountPercentField.getText().isEmpty() ? 0 : Double.parseDouble(discountPercentField.getText());
            String totalStr = finalTotalLabel.getText().replace("$", "");
            double total = totalStr.equals("-") ? 0 : Double.parseDouble(totalStr);
            String taxStr = taxAmountLabel.getText().replace("$", "");
            double tax = taxStr.equals("-") ? 0 : Double.parseDouble(taxStr);
            Bill bill = new Bill(CSVHandler.nextBillId(), activeSelection.getBookingId(), total, LocalDate.now().toString(), tax, disc, extra);
            
            boolean dbSuccess = writeBillToDB(bill);
            if (!dbSuccess) return;

            Room room = CSVHandler.getRoomById(activeSelection.getRoomId());
            if (room != null) {
                boolean roomDbSuccess = updateDBRoomStatus(room.getRoomId(), "Available", "DIRTY");
                if (!roomDbSuccess) return;
                
                room.setStatus("Available");
                room.setCleaningStatus("DIRTY");
                List<Room> rooms = CSVHandler.loadRooms();
                CSVHandler.saveRooms(rooms.stream().map(rm -> rm.getRoomId() == room.getRoomId() ? room : rm).collect(Collectors.toList()));
            }

            List<Bill> all = CSVHandler.loadBills();
            all.add(bill);
            CSVHandler.saveBills(all);

            showMessage("Success", "Checkout process complete.");
            resetUI();
            updateBookingOptions();
        } catch (Exception e) {
            showMessage("Error", "Action failed: " + e.getMessage());
        }
    }

    private boolean writeBillToDB(Bill b) {
        String sql = "INSERT INTO billing (billId, bookingId, totalAmount, billDate, tax, discount, extraCharges) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            synchronized (DBConnection.getDbMonitor()) {
                pstmt.setInt(1, b.getBillId());
                pstmt.setInt(2, b.getBookingId());
                pstmt.setDouble(3, b.getTotalAmount());
                pstmt.setString(4, b.getBillDate());
                pstmt.setDouble(5, b.getTax());
                pstmt.setDouble(6, b.getDiscount());
                pstmt.setDouble(7, b.getExtraCharges());
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            javafx.application.Platform.runLater(() -> showMessage("Database Error", "Could not save bill to database: " + e.getMessage()));
            return false;
        }
    }

    private boolean updateDBRoomStatus(int roomId, String status, String cleaningStatus) {
        String sql = "UPDATE rooms SET status=?, cleaning_status=? WHERE roomId=?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            synchronized (DBConnection.getDbMonitor()) {
                pstmt.setString(1, status);
                pstmt.setString(2, cleaningStatus);
                pstmt.setInt(3, roomId);
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            javafx.application.Platform.runLater(() -> showMessage("Database Error", "Could not update room status: " + e.getMessage()));
            return false;
        }
    }

    private void resetUI() {
        if (bookingIdList != null) bookingIdList.getSelectionModel().clearSelection();
        if (guestNameLabel != null) guestNameLabel.setText("-");
        if (roomInfoLabel != null) roomInfoLabel.setText("-");
        if (stayDurationField != null) stayDurationField.setText("1");
        if (extraCostField != null) extraCostField.setText("0.0");
        if (discountPercentField != null) discountPercentField.setText("0");
        if (taxAmountLabel != null) taxAmountLabel.setText("$0.00");
        if (finalTotalLabel != null) finalTotalLabel.setText("$0.00");
        activeSelection = null;
    }

    private void showMessage(String title, String txt) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(txt);
        alert.showAndWait();
    }
}
