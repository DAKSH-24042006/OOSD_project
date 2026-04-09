package main;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import util.CSVHandler;
import java.net.URL;

public class Main extends Application {

    private BorderPane rootPane;

    @Override
    public void start(Stage primaryStage) {
        CSVHandler.ensureFilesExist();
        rootPane = new BorderPane();
        HBox navBar = new HBox(15);
        navBar.setAlignment(Pos.CENTER);
        navBar.setId("nav-bar");

        Button btnDashboard = new Button("Dashboard");
        Button btnRoom = new Button("Rooms");
        Button btnBooking = new Button("Bookings");
        Button btnCustomer = new Button("Customers");
        Button btnCheckout = new Button("Checkout");

        btnDashboard.setOnAction(e -> loadPage("/view/dashboard.fxml"));
        btnRoom.setOnAction(e -> loadPage("/view/room.fxml"));
        btnBooking.setOnAction(e -> loadPage("/view/booking.fxml"));
        btnCustomer.setOnAction(e -> loadPage("/view/customer.fxml"));
        btnCheckout.setOnAction(e -> loadPage("/view/billing_process.fxml"));

        navBar.getChildren().addAll(btnDashboard, btnRoom, btnBooking, btnCustomer, btnCheckout);
        rootPane.setTop(navBar);

        loadPage("/view/dashboard.fxml");

        Scene scene = new Scene(rootPane, 1200, 850);
        URL cssUrl = Main.class.getResource("/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        
        primaryStage.setTitle("Red Bull Racing 2026 - Hotel Management System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadPage(String fxmlPath) {
        try {
            URL url = Main.class.getResource(fxmlPath);
            if (url == null) {
                throw new IllegalArgumentException("Invalid path: " + fxmlPath);
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent page = loader.load();
            rootPane.setCenter(page);
        } catch (Exception e) {
            System.err.println("CRITICAL FAILURE LOADING FXML: " + fxmlPath);
            e.printStackTrace();
            
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("error_log.txt", true))) {
                pw.println("--- ERROR LOADING " + fxmlPath + " ---");
                e.printStackTrace(pw);
                pw.println();
            } catch (java.io.IOException ignored) {}

            Throwable cause = e;
            while (cause.getCause() != null) cause = cause.getCause();
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Loading Page");
            alert.setHeaderText("Could not load: " + fxmlPath);
            alert.setContentText("Error Type: " + cause.getClass().getSimpleName() + "\nMessage: " + cause.getMessage() + "\n\nCheck error_log.txt for full stack trace.");
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
