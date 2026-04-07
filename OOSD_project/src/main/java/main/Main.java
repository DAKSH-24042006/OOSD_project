package main;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import util.CSVHandler;
import java.io.IOException;

public class Main extends Application {

    private BorderPane rootPane;

    @Override
    public void start(Stage primaryStage) {
        CSVHandler.ensureFilesExist();
        rootPane = new BorderPane();
        HBox navBar = new HBox(15);
        navBar.setAlignment(Pos.CENTER);
        navBar.setId("nav-bar");

        Button btnRoom = new Button("Rooms");
        Button btnBooking = new Button("Bookings");
        Button btnCheckout = new Button("Checkout");

        btnRoom.setOnAction(e -> loadPage("/view/room.fxml"));
        btnBooking.setOnAction(e -> loadPage("/view/booking.fxml"));
        btnCheckout.setOnAction(e -> loadPage("/view/checkout.fxml"));

        navBar.getChildren().addAll(btnRoom, btnBooking, btnCheckout);
        rootPane.setTop(navBar);

        loadPage("/view/room.fxml");

        Scene scene = new Scene(rootPane, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        
        primaryStage.setTitle("Hotel Management System");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void loadPage(String fxmlPath) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
            rootPane.setCenter(page);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
