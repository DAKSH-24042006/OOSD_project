package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Room;
import util.CSVHandler;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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

    @FXML private TextField txtRoomSearch;
    @FXML private ComboBox<String> cmbFilterType;
    @FXML private ComboBox<String> cmbFilterStatus;
    @FXML private Button btnUpdateRoom;
    @FXML private Button btnDeleteRoom;

    private ObservableList<Room> roomList;
    private FilteredList<Room> filteredRooms;
    private Room selectedRoom = null;

    @FXML
    public void initialize() {
        cmbType.setItems(FXCollections.observableArrayList("Single", "Double", "Suite"));
        cmbBedType.setItems(FXCollections.observableArrayList("Single", "Double", "Queen", "King"));
        cmbAc.setItems(FXCollections.observableArrayList("AC", "Non-AC"));
        cmbWifi.setItems(FXCollections.observableArrayList("Yes", "No"));

        cmbFilterType.setItems(FXCollections.observableArrayList("All Types", "Single", "Double", "Suite"));
        cmbFilterStatus.setItems(FXCollections.observableArrayList("All Status", "Available", "Booked", "Maintenance"));

        colRoomId.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colFloor.setCellValueFactory(new PropertyValueFactory<>("floor"));
        colBedType.setCellValueFactory(new PropertyValueFactory<>("bedType"));
        colAc.setCellValueFactory(new PropertyValueFactory<>("ac"));
        colWifi.setCellValueFactory(new PropertyValueFactory<>("wifi"));

        colStatus.setCellFactory(column -> new TableCell<Room, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label label = new Label(item);
                    if ("Available".equalsIgnoreCase(item)) {
                        label.setStyle("-fx-background-color:#1a472a;-fx-text-fill:#69db7c;-fx-background-radius:12;-fx-padding:3 10;-fx-font-weight:bold;");
                    } else if ("Booked".equalsIgnoreCase(item)) {
                        label.setStyle("-fx-background-color:#3b1a1a;-fx-text-fill:#ff6b6b;-fx-background-radius:12;-fx-padding:3 10;-fx-font-weight:bold;");
                    } else if ("Maintenance".equalsIgnoreCase(item)) {
                        label.setStyle("-fx-background-color:#3d2e00;-fx-text-fill:#ffd43b;-fx-background-radius:12;-fx-padding:3 10;-fx-font-weight:bold;");
                    }
                    setGraphic(label);
                }
            }
        });

        loadRooms();

        txtRoomSearch.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        cmbFilterType.setOnAction(e -> applyFilters());
        cmbFilterStatus.setOnAction(e -> applyFilters());

        tableRooms.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedRoom = newSelection;
                txtRoomId.setText(String.valueOf(selectedRoom.getRoomId()));
                cmbType.setValue(selectedRoom.getType());
                txtPrice.setText(String.valueOf(selectedRoom.getPrice()));
                txtFloor.setText(String.valueOf(selectedRoom.getFloor()));
                cmbBedType.setValue(selectedRoom.getBedType());
                cmbAc.setValue(selectedRoom.getAc());
                cmbWifi.setValue(selectedRoom.getWifi());
            }
        });
    }

    private void loadRooms() {
        List<Room> rooms = CSVHandler.loadRooms();
        roomList = FXCollections.observableArrayList(rooms);
        filteredRooms = new FilteredList<>(roomList, p -> true);
        SortedList<Room> sortedRooms = new SortedList<>(filteredRooms);
        sortedRooms.comparatorProperty().bind(tableRooms.comparatorProperty());
        tableRooms.setItems(sortedRooms);
    }

    private void applyFilters() {
        filteredRooms.setPredicate(room -> {
            String q = txtRoomSearch.getText() == null ? "" : txtRoomSearch.getText().toLowerCase();
            String t = cmbFilterType.getValue();
            String s = cmbFilterStatus.getValue();

            boolean matchQ = String.valueOf(room.getRoomId()).contains(q) || room.getType().toLowerCase().contains(q);
            boolean matchT = t == null || t.equals("All Types") || room.getType().equals(t);
            boolean matchS = s == null || s.equals("All Status") || room.getStatus().equals(s);

            return matchQ && matchT && matchS;
        });
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

            if (type == null || bedType == null || ac == null || wifi == null) return;
            if (CSVHandler.getRoomById(roomId) != null) return;

            Room newRoom = new Room(roomId, type, price, "Available", floor, bedType, ac, wifi);
            roomList.add(newRoom);
            CSVHandler.saveRooms(roomList);
            saveRoomToDB(newRoom);
            clearForm();
        } catch (NumberFormatException e) {}
    }

    @FXML
    private void handleUpdateRoom() {
        if (selectedRoom == null) return;
        try {
            selectedRoom.setType(cmbType.getValue());
            selectedRoom.setPrice(Double.parseDouble(txtPrice.getText()));
            selectedRoom.setFloor(Integer.parseInt(txtFloor.getText()));
            selectedRoom.setBedType(cmbBedType.getValue());
            selectedRoom.setAc(cmbAc.getValue());
            selectedRoom.setWifi(cmbWifi.getValue());

            CSVHandler.saveRooms(roomList);
            updateRoomInDB(selectedRoom);
            tableRooms.refresh();
            clearForm();
        } catch (NumberFormatException e) {}
    }

    @FXML
    private void handleDeleteRoom() {
        if (selectedRoom == null) return;
        if ("Booked".equalsIgnoreCase(selectedRoom.getStatus())) {
            showAlert("Error", "Cannot delete a booked room.");
            return;
        }
        roomList.remove(selectedRoom);
        CSVHandler.saveRooms(roomList);
        deleteRoomFromDB(selectedRoom.getRoomId());
        clearForm();
    }

    @FXML
    private void handleClearForm() {
        clearForm();
        selectedRoom = null;
        tableRooms.getSelectionModel().clearSelection();
    }

    private void saveRoomToDB(Room room) {
        String sql = "INSERT INTO rooms (roomId, type, price, status, floor, bedType, ac, wifi) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        executeDBStatement(sql, room);
    }

    private void updateRoomInDB(Room room) {
        String sql = "UPDATE rooms SET type=?, price=?, status=?, floor=?, bedType=?, ac=?, wifi=? WHERE roomId=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            synchronized (DBConnection.getDbMonitor()) {
                pstmt.setString(1, room.getType());
                pstmt.setDouble(2, room.getPrice());
                pstmt.setString(3, room.getStatus());
                pstmt.setInt(4, room.getFloor());
                pstmt.setString(5, room.getBedType());
                pstmt.setString(6, room.getAc());
                pstmt.setString(7, room.getWifi());
                pstmt.setInt(8, room.getRoomId());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            showDBError(e);
        }
    }

    private void deleteRoomFromDB(int roomId) {
        String sql = "DELETE FROM rooms WHERE roomId=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            synchronized (DBConnection.getDbMonitor()) {
                pstmt.setInt(1, roomId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            showDBError(e);
        }
    }

    private void executeDBStatement(String sql, Room room) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            synchronized (DBConnection.getDbMonitor()) {
                pstmt.setInt(1, room.getRoomId());
                pstmt.setString(2, room.getType());
                pstmt.setDouble(3, room.getPrice());
                pstmt.setString(4, room.getStatus());
                pstmt.setInt(5, room.getFloor());
                pstmt.setString(6, room.getBedType());
                pstmt.setString(7, room.getAc());
                pstmt.setString(8, room.getWifi());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            showDBError(e);
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
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showDBError(SQLException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Database Error");
        alert.setHeaderText("A database error occurred.");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}
