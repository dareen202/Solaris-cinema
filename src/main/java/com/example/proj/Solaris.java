package com.example.proj;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;

public class Solaris extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Solaris.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 800);

        stage.setTitle("Welcome to Solaris Cinema");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));

        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}