package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Room;
import util.CSVHandler;

import java.util.List;

public class RoomController {

    @FXML private TextField txtRoomId;
    @FXML private ComboBox<String> cmbType;
    @FXML private TextField txtPrice;
    @FXML private TextField txtFloor;
    @FXML private ComboBox<String> cmbBedType;
    @FXML private ComboBox<String> cmbAc;
    @FXML private ComboBox<String> cmbWifi;

    @FXML private TableView<Room> tableRooms;
    @FXML private TableColumn<Room, Integer> colRoomId;
    @FXML private TableColumn<Room, String> colType;
    @FXML private TableColumn<Room, Double> colPrice;
    @FXML private TableColumn<Room, String> colStatus;
    @FXML private TableColumn<Room, Integer> colFloor;
    @FXML private TableColumn<Room, String> colBedType;
    @FXML private TableColumn<Room, String> colAc;
    @FXML private TableColumn<Room, String> colWifi;

    private ObservableList<Room> roomList;

    @FXML
    public void initialize() {
        cmbType.setItems(FXCollections.observableArrayList("Single", "Double", "Suite"));
        cmbBedType.setItems(FXCollections.observableArrayList("Single", "Double", "Queen", "King"));
        cmbAc.setItems(FXCollections.observableArrayList("AC", "Non-AC"));
        cmbWifi.setItems(FXCollections.observableArrayList("Yes", "No"));

        colRoomId.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colFloor.setCellValueFactory(new PropertyValueFactory<>("floor"));
        colBedType.setCellValueFactory(new PropertyValueFactory<>("bedType"));
        colAc.setCellValueFactory(new PropertyValueFactory<>("ac"));
        colWifi.setCellValueFactory(new PropertyValueFactory<>("wifi"));

        loadRooms();
    }

    private void loadRooms() {
        List<Room> rooms = CSVHandler.loadRooms();
        roomList = FXCollections.observableArrayList(rooms);
        tableRooms.setItems(roomList);
    }

    @FXML
    private void handleAddRoom() {
        try {
            int roomId = Integer.parseInt(txtRoomId.getText());
            String type = cmbType.getValue();
            double price = Double.parseDouble(txtPrice.getText());
            int floor = Integer.parseInt(txtFloor.getText());
            String bedType = cmbBedType.getValue();
            String ac = cmbAc.getValue();
            String wifi = cmbWifi.getValue();

            if (type == null || bedType == null || ac == null || wifi == null) {
                 showAlert("Error", "All combobox fields must be selected.");
                 return;
            }

            if (CSVHandler.getRoomById(roomId) != null) {
                showAlert("Error", "Room ID already exists!");
                return;
            }

            Room newRoom = new Room(roomId, type, price, "Available", floor, bedType, ac, wifi);
            roomList.add(newRoom);
            CSVHandler.saveRooms(roomList);
            System.out.println("Room Added");
            showAlert("Success", "Room added successfully!");
            clearForm();
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid number format in numerical fields.");
        }
    }

    private void clearForm() {
        txtRoomId.clear();
        cmbType.getSelectionModel().clearSelection();
        txtPrice.clear();
        txtFloor.clear();
        cmbBedType.getSelectionModel().clearSelection();
        cmbAc.getSelectionModel().clearSelection();
        cmbWifi.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(title.equals("Success") ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
