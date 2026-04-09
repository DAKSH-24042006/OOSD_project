package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
import java.util.List;

public class BookingController {

    @FXML private ComboBox<Customer> cmbCustomer;
    @FXML private ComboBox<Integer> cmbRoom;
    @FXML private DatePicker dpCheckIn;
    @FXML private DatePicker dpCheckOut;
    @FXML private ComboBox<String> cmbPaymentStatus;
    @FXML private ComboBox<String> cmbPaymentMethod;

    @FXML private TableView<Booking> tableBookings;
    @FXML private TableColumn<Booking, Integer> colBookingId;
    @FXML private TableColumn<Booking, String> colCustomer;
    @FXML private TableColumn<Booking, Integer> colRoom;
    @FXML private TableColumn<Booking, LocalDate> colCheckIn;
    @FXML private TableColumn<Booking, LocalDate> colCheckOut;
    @FXML private TableColumn<Booking, String> colBookingStatus;
    @FXML private TableColumn<Booking, String> colPaymentStatus;
    @FXML private TableColumn<Booking, String> colPaymentMethod;

    @FXML private TextField txtBookingSearch;
    @FXML private DatePicker dpFilterFrom;
    @FXML private DatePicker dpFilterTo;

    private ObservableList<Booking> bookingList;
    private FilteredList<Booking> filteredBookings;
    private Booking selectedBooking = null;

    @FXML
    public void initialize() {
        populateRoomList();
        populateCustomerList();
        cmbPaymentStatus.setItems(FXCollections.observableArrayList("Pending", "Partial", "Full"));
        cmbPaymentMethod.setItems(FXCollections.observableArrayList("Cash", "Card", "UPI"));

        cmbCustomer.setConverter(new javafx.util.StringConverter<Customer>() {
            @Override
            public String toString(Customer c) {
                return c == null ? "" : c.getName() + " (" + c.getPhone() + ")";
            }
            @Override
            public Customer fromString(String string) {
                return null;
            }
        });

        colBookingId.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        colCheckIn.setCellValueFactory(new PropertyValueFactory<>("checkIn"));
        colCheckOut.setCellValueFactory(new PropertyValueFactory<>("checkOut"));
        colBookingStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPaymentStatus.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        colPaymentMethod.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        colBookingStatus.setCellFactory(column -> new TableCell<Booking, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label label = new Label(item);
                    if ("Active".equalsIgnoreCase(item)) {
                        label.setStyle("-fx-background-color:#1a3a5c;-fx-text-fill:#74c0fc;-fx-background-radius:12;-fx-padding:3 10;-fx-font-weight:bold;");
                    } else if ("Checked Out".equalsIgnoreCase(item)) {
                        label.setStyle("-fx-background-color:#2a2a2a;-fx-text-fill:#adb5bd;-fx-background-radius:12;-fx-padding:3 10;-fx-font-weight:bold;");
                    } else if ("Cancelled".equalsIgnoreCase(item)) {
                        label.setStyle("-fx-background-color:#3b1a1a;-fx-text-fill:#ff6b6b;-fx-background-radius:12;-fx-padding:3 10;-fx-font-weight:bold;");
                    }
                    setGraphic(label);
                }
            }
        });

        loadData();

        txtBookingSearch.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        dpFilterFrom.setOnAction(e -> applyFilters());
        dpFilterTo.setOnAction(e -> applyFilters());

        tableBookings.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedBooking = newSelection;
                Customer c = CSVHandler.getCustomerById(selectedBooking.getCustomerId());
                if (c != null) {
                    cmbCustomer.setValue(c);
                }
                cmbRoom.setValue(selectedBooking.getRoomId());
                dpCheckIn.setValue(selectedBooking.getCheckIn());
                dpCheckOut.setValue(selectedBooking.getCheckOut());
                cmbPaymentStatus.setValue(selectedBooking.getPaymentStatus());
                cmbPaymentMethod.setValue(selectedBooking.getPaymentMethod());
            }
        });
    }

    private void loadData() {
        List<Booking> bookings = CSVHandler.loadBookings();
        List<Bill> bills = CSVHandler.loadBills();
        for (Booking b : bookings) {
            Customer c = CSVHandler.getCustomerById(b.getCustomerId());
            b.setCustomerName(c != null ? c.getName() : "Unknown");
            if (bills.stream().anyMatch(bill -> bill.getBookingId() == b.getBookingId())) {
                b.setStatus("Checked Out");
            }
        }
        bookingList = FXCollections.observableArrayList(bookings);
        filteredBookings = new FilteredList<>(bookingList, p -> true);
        SortedList<Booking> sortedBookings = new SortedList<>(filteredBookings);
        sortedBookings.comparatorProperty().bind(tableBookings.comparatorProperty());
        tableBookings.setItems(sortedBookings);
    }

    private void applyFilters() {
        filteredBookings.setPredicate(booking -> {
            String q = txtBookingSearch.getText() == null ? "" : txtBookingSearch.getText().toLowerCase();
            LocalDate from = dpFilterFrom.getValue();
            LocalDate to = dpFilterTo.getValue();

            boolean matchQ = booking.getCustomerName().toLowerCase().contains(q) || String.valueOf(booking.getBookingId()).contains(q);
            boolean matchFrom = from == null || !booking.getCheckIn().isBefore(from);
            boolean matchTo = to == null || !booking.getCheckOut().isAfter(to);

            return matchQ && matchFrom && matchTo;
        });
    }

    @FXML
    private void handleSaveBooking() {
        if (!validateInputs()) return;
        CSVHandler.getBookingsLock().lock();
        try {
            LocalDate in = dpCheckIn.getValue();
            LocalDate out = dpCheckOut.getValue();
            int roomId = cmbRoom.getValue();

            if (hasOverlap(roomId, in, out, -1)) {
                showAlert("Error", "Room already booked for these dates.");
                return;
            }

            Customer cust = cmbCustomer.getValue();
            Booking b = new Booking(CSVHandler.nextBookingId(), cust.getCustomerId(), roomId, in, out,
                    cmbPaymentStatus.getValue(), cmbPaymentMethod.getValue());
            
            bookingList.add(b);
            CSVHandler.saveBookings(bookingList);
            saveBookingToDB(b);

            Room r = CSVHandler.getRoomById(roomId);
            if (r != null) {
                r.setStatus("Booked");
                List<Room> all = CSVHandler.loadRooms();
                CSVHandler.saveRooms(all.stream().map(rm -> rm.getRoomId() == roomId ? r : rm).toList());
                updateRoomStatusInDB(roomId, "Booked");
            }

            loadData();
            clearForm();
        } finally {
            CSVHandler.getBookingsLock().unlock();
        }
    }

    @FXML
    private void handleUpdateBooking() {
        if (selectedBooking == null || !validateInputs()) return;
        CSVHandler.getBookingsLock().lock();
        try {
            LocalDate in = dpCheckIn.getValue();
            LocalDate out = dpCheckOut.getValue();
            int roomId = cmbRoom.getValue();

            if (hasOverlap(roomId, in, out, selectedBooking.getBookingId())) {
                showAlert("Error", "Room already booked for these dates.");
                return;
            }

            selectedBooking.setCheckIn(in);
            selectedBooking.setCheckOut(out);
            selectedBooking.setRoomId(roomId);
            selectedBooking.setPaymentStatus(cmbPaymentStatus.getValue());
            selectedBooking.setPaymentMethod(cmbPaymentMethod.getValue());

            CSVHandler.saveBookings(bookingList);
            updateBookingInDB(selectedBooking);
            loadData();
            clearForm();
        } finally {
            CSVHandler.getBookingsLock().unlock();
        }
    }

    @FXML
    private void handleCancelBooking() {
        if (selectedBooking == null || "Checked Out".equals(selectedBooking.getStatus())) {
            showAlert("Error", "Cannot cancel this booking.");
            return;
        }
        selectedBooking.setStatus("Cancelled");
        CSVHandler.saveBookings(bookingList);
        updateBookingInDB(selectedBooking);

        int roomId = selectedBooking.getRoomId();
        Room r = CSVHandler.getRoomById(roomId);
        if (r != null) {
            r.setStatus("Available");
            List<Room> all = CSVHandler.loadRooms();
            CSVHandler.saveRooms(all.stream().map(rm -> rm.getRoomId() == roomId ? r : rm).toList());
            updateRoomStatusInDB(roomId, "Available");
        }
        loadData();
        clearForm();
    }

    private boolean hasOverlap(int roomId, LocalDate in, LocalDate out, int excludeId) {
        return bookingList.stream()
                .filter(b -> b.getRoomId() == roomId && b.getBookingId() != excludeId 
                        && !"Cancelled".equalsIgnoreCase(b.getStatus()) 
                        && !"Checked Out".equalsIgnoreCase(b.getStatus()))
                .anyMatch(b -> in.isBefore(b.getCheckOut()) && out.isAfter(b.getCheckIn()));
    }

    private void populateCustomerList() {
        cmbCustomer.setItems(FXCollections.observableArrayList(CSVHandler.loadCustomers()));
    }

    private boolean validateInputs() {
        return cmbCustomer.getValue() != null && cmbRoom.getValue() != null && dpCheckIn.getValue() != null && dpCheckOut.getValue() != null;
    }


    private void populateRoomList() {
        cmbRoom.setItems(FXCollections.observableArrayList(CSVHandler.loadRooms().stream().map(Room::getRoomId).toList()));
    }

    private void saveBookingToDB(Booking b) {
        String sql = "INSERT INTO bookings (bookingId, customerId, roomId, checkIn, checkOut, paymentStatus, paymentMethod, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            synchronized (DBConnection.getDbMonitor()) {
                pstmt.setInt(1, b.getBookingId());
                pstmt.setInt(2, b.getCustomerId());
                pstmt.setInt(3, b.getRoomId());
                pstmt.setString(4, b.getCheckIn().toString());
                pstmt.setString(5, b.getCheckOut().toString());
                pstmt.setString(6, b.getPaymentStatus());
                pstmt.setString(7, b.getPaymentMethod());
                pstmt.setString(8, b.getStatus());
                pstmt.executeUpdate();
            }
        } catch (SQLException ignored) {}
    }

    private void updateBookingInDB(Booking b) {
        String sql = "UPDATE bookings SET checkIn=?, checkOut=?, paymentStatus=?, paymentMethod=?, status=? WHERE bookingId=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            synchronized (DBConnection.getDbMonitor()) {
                pstmt.setString(1, b.getCheckIn().toString());
                pstmt.setString(2, b.getCheckOut().toString());
                pstmt.setString(3, b.getPaymentStatus());
                pstmt.setString(4, b.getPaymentMethod());
                pstmt.setString(5, b.getStatus());
                pstmt.setInt(6, b.getBookingId());
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


    @FXML
    private void handleClearForm() {
        clearForm();
    }

    private void clearForm() {
        cmbCustomer.getSelectionModel().clearSelection();
        cmbRoom.getSelectionModel().clearSelection();
        dpCheckIn.setValue(null); dpCheckOut.setValue(null);
        cmbPaymentStatus.getSelectionModel().clearSelection();
        cmbPaymentMethod.getSelectionModel().clearSelection();
        selectedBooking = null;
        tableBookings.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content);
        alert.showAndWait();
    }
}
