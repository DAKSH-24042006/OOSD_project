package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Customer;
import util.CSVHandler;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class CustomerController {

    @FXML private TextField txtName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private TextArea txtAddress;
    @FXML private TextField txtSearch;

    @FXML private TableView<Customer> tableCustomers;
    @FXML private TableColumn<Customer, Integer> colId;
    @FXML private TableColumn<Customer, String> colName;
    @FXML private TableColumn<Customer, String> colPhone;
    @FXML private TableColumn<Customer, String> colEmail;
    @FXML private TableColumn<Customer, String> colAddress;

    private ObservableList<Customer> customerList;
    private FilteredList<Customer> filteredCustomers;
    private Customer selectedCustomer = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));

        loadData();

        txtSearch.textProperty().addListener((obs, oldV, newV) -> {
            filteredCustomers.setPredicate(c -> {
                if (newV == null || newV.isEmpty()) return true;
                String lower = newV.toLowerCase();
                return c.getName().toLowerCase().contains(lower) || c.getPhone().contains(lower);
            });
        });

        tableCustomers.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                selectedCustomer = newV;
                txtName.setText(newV.getName());
                txtPhone.setText(newV.getPhone());
                txtEmail.setText(newV.getEmail());
                txtAddress.setText(newV.getAddress());
            }
        });
    }

    private void loadData() {
        List<Customer> customers = CSVHandler.loadCustomers();
        customerList = FXCollections.observableArrayList(customers);
        filteredCustomers = new FilteredList<>(customerList, p -> true);
        SortedList<Customer> sortedData = new SortedList<>(filteredCustomers);
        sortedData.comparatorProperty().bind(tableCustomers.comparatorProperty());
        tableCustomers.setItems(sortedData);
    }

    @FXML
    private void handleSave() {
        if (!validate()) return;
        CSVHandler.getCustomersLock().lock();
        try {
            Customer c = new Customer(CSVHandler.nextCustomerId(), txtName.getText(), txtPhone.getText(), txtEmail.getText(), txtAddress.getText());
            customerList.add(c);
            CSVHandler.saveCustomers(customerList);
            saveToDB(c);
            handleClear();
        } finally {
            CSVHandler.getCustomersLock().unlock();
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedCustomer == null || !validate()) return;
        CSVHandler.getCustomersLock().lock();
        try {
            selectedCustomer.setName(txtName.getText());
            selectedCustomer.setPhone(txtPhone.getText());
            selectedCustomer.setEmail(txtEmail.getText());
            selectedCustomer.setAddress(txtAddress.getText());
            
            CSVHandler.saveCustomers(customerList);
            updateInDB(selectedCustomer);
            tableCustomers.refresh();
            handleClear();
        } finally {
            CSVHandler.getCustomersLock().unlock();
        }
    }

    @FXML
    private void handleClear() {
        txtName.clear(); txtPhone.clear(); txtEmail.clear(); txtAddress.clear();
        selectedCustomer = null;
        tableCustomers.getSelectionModel().clearSelection();
    }

    private boolean validate() {
        return !txtName.getText().isEmpty() && txtPhone.getText().matches("\\d{10}") && txtEmail.getText().contains("@");
    }

    private void saveToDB(Customer c) {
        String sql = "INSERT INTO customers (customerId, name, phone, email, address) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            synchronized (DBConnection.getDbMonitor()) {
                pstmt.setInt(1, c.getCustomerId());
                pstmt.setString(2, c.getName());
                pstmt.setString(3, c.getPhone());
                pstmt.setString(4, c.getEmail());
                pstmt.setString(5, c.getAddress());
                pstmt.executeUpdate();
            }
        } catch (SQLException ignored) {}
    }

    private void updateInDB(Customer c) {
        String sql = "UPDATE customers SET name=?, phone=?, email=?, address=? WHERE customerId=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            synchronized (DBConnection.getDbMonitor()) {
                pstmt.setString(1, c.getName());
                pstmt.setString(2, c.getPhone());
                pstmt.setString(3, c.getEmail());
                pstmt.setString(4, c.getAddress());
                pstmt.setInt(5, c.getCustomerId());
                pstmt.executeUpdate();
            }
        } catch (SQLException ignored) {}
    }
}
