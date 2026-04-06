package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Booking;
import model.Customer;
import model.Room;
import util.CSVHandler;

import java.time.LocalDate;
import java.util.List;

public class BookingController {

    @FXML private TextField txtBookingId;
    @FXML private ComboBox<Customer> cmbCustomer;
    @FXML private ComboBox<Room> cmbRoom;
    @FXML private DatePicker dpCheckIn;
    @FXML private DatePicker dpCheckOut;
    @FXML private ComboBox<String> cmbPaymentStatus;
    @FXML private ComboBox<String> cmbPaymentMethod;

    @FXML private TableView<Booking> tableBookings;
    @FXML private TableColumn<Booking, Integer> colBookingId;
    @FXML private TableColumn<Booking, String> colCustomerName;
    @FXML private TableColumn<Booking, Integer> colRoomId;
    @FXML private TableColumn<Booking, String> colDates;
    @FXML private TableColumn<Booking, String> colPaymentStatus;

    private ObservableList<Booking> bookingList;
    private List<Room> allRooms;

    @FXML
    public void initialize() {
        cmbPaymentStatus.setItems(FXCollections.observableArrayList("Pending", "Paid"));
        cmbPaymentMethod.setItems(FXCollections.observableArrayList("Cash", "Card", "UPI"));

        colBookingId.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        colCustomerName.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colRoomId.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        colDates.setCellValueFactory(new PropertyValueFactory<>("dates"));
        colPaymentStatus.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));

        loadData();
    }

    private void loadData() {
        List<Customer> customers = CSVHandler.loadCustomers();
        cmbCustomer.setItems(FXCollections.observableArrayList(customers));

        allRooms = CSVHandler.loadRooms();
        updateAvailableRooms();

        List<Booking> bookings = CSVHandler.loadBookings();
        for (Booking b : bookings) {
            Customer c = CSVHandler.getCustomerById(b.getCustomerId());
            if (c != null) b.setCustomerName(c.getName());
        }
        bookingList = FXCollections.observableArrayList(bookings);
        tableBookings.setItems(bookingList);
    }

    private void updateAvailableRooms() {
        ObservableList<Room> availableRooms = FXCollections.observableArrayList();
        for (Room r : allRooms) {
            if ("Available".equalsIgnoreCase(r.getStatus())) {
                availableRooms.add(r);
            }
        }
        cmbRoom.setItems(availableRooms);
    }

    @FXML
    private void handleSaveBooking() {
        try {
            int bookingId = Integer.parseInt(txtBookingId.getText());
            Customer selectedCustomer = cmbCustomer.getValue();
            Room selectedRoom = cmbRoom.getValue();
            LocalDate checkIn = dpCheckIn.getValue();
            LocalDate checkOut = dpCheckOut.getValue();
            String paymentStatus = cmbPaymentStatus.getValue();
            String paymentMethod = cmbPaymentMethod.getValue();

            if (selectedCustomer == null || selectedRoom == null || checkIn == null || checkOut == null || paymentStatus == null || paymentMethod == null) {
                showAlert("Error", "All fields must be filled!");
                return;
            }
            if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
                showAlert("Error", "Check-out date must be strictly after check-in date!");
                return;
            }
            if (CSVHandler.getBookingById(bookingId) != null) {
                showAlert("Error", "Booking ID already exists!");
                return;
            }

            Booking newBooking = new Booking(bookingId, selectedCustomer.getCustomerId(), selectedRoom.getRoomId(),
                    checkIn, checkOut, paymentStatus, paymentMethod);
            newBooking.setCustomerName(selectedCustomer.getName());

            bookingList.add(newBooking);
            CSVHandler.saveBookings(bookingList);

            selectedRoom.setStatus("Booked");
            CSVHandler.saveRooms(allRooms);

            System.out.println("Booking Created");
            showAlert("Success", "Booking created successfully!");
            clearForm();
            updateAvailableRooms();

        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid Booking ID format.");
        }
    }

    private void clearForm() {
        txtBookingId.clear();
        cmbCustomer.getSelectionModel().clearSelection();
        cmbRoom.getSelectionModel().clearSelection();
        dpCheckIn.setValue(null);
        dpCheckOut.setValue(null);
        cmbPaymentStatus.getSelectionModel().clearSelection();
        cmbPaymentMethod.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(title.equals("Success") ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
