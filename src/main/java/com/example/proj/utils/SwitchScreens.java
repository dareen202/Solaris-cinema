package com.example.proj.utils;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import java.io.IOException;

public class SwitchScreens {

    public static void switchScene(Event event, String fxmlFile, String title) {
        try {
            String fxmlPath = "/com/example/proj/" + fxmlFile;

            FXMLLoader loader = new FXMLLoader(SwitchScreens.class.getResource(fxmlPath));

            if (loader.getLocation() == null) {
                System.err.println("FATAL ERROR: Could not find FXML file at " + fxmlPath);
                return;
            }

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}