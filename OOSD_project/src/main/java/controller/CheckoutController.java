package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Bill;
import model.Booking;
import model.Room;
import util.CSVHandler;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class CheckoutController {

    @FXML private ComboBox<Integer> cmbBooking;
    @FXML private Label lblCustomer;
    @FXML private Label lblRoom;
    @FXML private Label lblDays;
    @FXML private TextField txtExtra;
    @FXML private TextField txtDiscount;
    @FXML private Label lblTax;
    @FXML private Label lblTotal;

    private List<Booking> activeBookings;
    private Booking selectedBooking;
    private double baseRate = 0;

    @FXML
    public void initialize() {
        loadActiveBookings();

        cmbBooking.setOnAction(e -> {
            Integer bid = cmbBooking.getValue();
            if (bid != null) {
                selectedBooking = CSVHandler.loadBookings().stream().filter(b -> b.getBookingId() == bid).findFirst().orElse(null);
                if (selectedBooking != null) {
                    lblCustomer.setText(CSVHandler.getCustomerById(selectedBooking.getCustomerId()).getName());
                    Room r = CSVHandler.getRoomById(selectedBooking.getRoomId());
                    lblRoom.setText("Room " + r.getRoomId() + " (" + r.getType() + ")");
                    long days = ChronoUnit.DAYS.between(selectedBooking.getCheckIn(), selectedBooking.getCheckOut());
                    if (days <= 0) days = 1;
                    lblDays.setText(String.valueOf(days));
                    baseRate = r.getPrice() * days;
                    calculateBill();
                }
            }
        });

        txtExtra.textProperty().addListener((obs, oldV, newV) -> calculateBill());
        txtDiscount.textProperty().addListener((obs, oldV, newV) -> calculateBill());
    }

    private void loadActiveBookings() {
        List<Bill> bills = CSVHandler.loadBills();
        activeBookings = CSVHandler.loadBookings().stream()
                .filter(b -> !"Cancelled".equalsIgnoreCase(b.getStatus()))
                .filter(b -> bills.stream().noneMatch(bill -> bill.getBookingId() == b.getBookingId()))
                .collect(Collectors.toList());
        cmbBooking.setItems(FXCollections.observableArrayList(activeBookings.stream().map(Booking::getBookingId).toList()));
    }

    private void calculateBill() {
        try {
            double extra = txtExtra.getText().isEmpty() ? 0 : Double.parseDouble(txtExtra.getText());
            double discPercent = txtDiscount.getText().isEmpty() ? 0 : Double.parseDouble(txtDiscount.getText());
            
            double subtotal = baseRate + extra;
            double discount = subtotal * (discPercent / 100.0);
            double taxable = subtotal - discount;
            double tax = taxable * 0.18;
            double total = taxable + tax;

            lblTax.setText(String.format("$%.2f", tax));
            lblTotal.setText(String.format("$%.2f", total));
        } catch (NumberFormatException e) {}
    }

    @FXML
    private void handleCheckout() {
        if (selectedBooking == null) return;
        
        CSVHandler.getBillsLock().lock();
        try {
            double extra = Double.parseDouble(txtExtra.getText());
            double disc = Double.parseDouble(txtDiscount.getText());
            double total = Double.parseDouble(lblTotal.getText().replace("$", ""));
            double tax = Double.parseDouble(lblTax.getText().replace("$", ""));

            Bill bill = new Bill(CSVHandler.nextBillId(), selectedBooking.getBookingId(), total, 
                                LocalDate.now().toString(), tax, disc, extra);
            
            List<Bill> allBills = CSVHandler.loadBills();
            allBills.add(bill);
            CSVHandler.saveBills(allBills);
            saveBillToDB(bill);

            Room r = CSVHandler.getRoomById(selectedBooking.getRoomId());
            if (r != null) {
                r.setStatus("Available");
                List<Room> rooms = CSVHandler.loadRooms();
                CSVHandler.saveRooms(rooms.stream().map(rm -> rm.getRoomId() == r.getRoomId() ? r : rm).toList());
                updateRoomStatusInDB(r.getRoomId(), "Available");
            }

            showAlert("Success", "Checkout completed. Bill ID: " + bill.getBillId());
            clearSelection();
            loadActiveBookings();
        } catch (Exception e) {
            showAlert("Error", "Checkout failed: " + e.getMessage());
        } finally {
            CSVHandler.getBillsLock().unlock();
        }
    }

    private void saveBillToDB(Bill b) {
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

    private void updateRoomStatusInDB(int roomId, String status) {
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

    private void clearSelection() {
        cmbBooking.getSelectionModel().clearSelection();
        lblCustomer.setText("-");
        lblRoom.setText("-");
        lblDays.setText("-");
        txtExtra.setText("0.0");
        txtDiscount.setText("0");
        lblTax.setText("$0.00");
        lblTotal.setText("$0.00");
        selectedBooking = null;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
