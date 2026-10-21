package com.example.proj;

import com.example.proj.utils.DBHandle;
import com.example.proj.utils.Session;
import com.example.proj.utils.SwitchScreens;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {
    @FXML private TextField userTF;
    @FXML private PasswordField passTF;
    @FXML private TextField passVisibleTF;
    @FXML private Hyperlink showPassHL;

    private boolean isPasswordVisible = false;

    @FXML
    private void onloginBTNClick(ActionEvent event) {

        String username = userTF.getText().trim();
        String password = isPasswordVisible
                ? passVisibleTF.getText()
                : passTF.getText();

        if (username.isEmpty() || password.isEmpty()) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Login Error",
                    "Please enter both username and password."
            );

            return;
        }

        String role = authenticateUser(username, password);

        if (role != null) {
            Session.username = username;
            Session.role = role;
            if (role.equalsIgnoreCase("Admin")) {
                System.out.println("Login Successful! Welcome, Manager.");
                SwitchScreens.switchScene(
                        event,
                        "admin-view.fxml",
                        "Solaris Cinema - Admin Panel"
                );

            } else {
                System.out.println("Login Successful! Welcome, Member.");
                SwitchScreens.switchScene(
                        event,
                        "user-view.fxml",
                        "Solaris Cinema - User Panel"
                );
            }

        } else {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Login Failed",
                    "Invalid username or password."
            );
        }
    }

    private String authenticateUser(String user, String pass) {
        String query =
                "SELECT role FROM Users " +
                        "WHERE username = ? AND password = ?";

        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, user);
            pstmt.setString(2, pass);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("role");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleSignupLink(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource(
                        "/com/example/proj/signup-view.fxml"
                )
        );

        Scene scene = new Scene(fxmlLoader.load(), 600, 800);
        Stage stage =
                (Stage) ((Node) event.getSource())
                        .getScene()
                        .getWindow();

        stage.setTitle("Sign Up to Solaris");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void togglePassword(ActionEvent event) {

        if (isPasswordVisible) {
            passTF.setText(passVisibleTF.getText());
            passTF.setVisible(true);
            passVisibleTF.setVisible(false);
            showPassHL.setText("Show Password");
            isPasswordVisible = false;

        } else {
            passVisibleTF.setText(passTF.getText());
            passVisibleTF.setVisible(true);
            passTF.setVisible(false);
            showPassHL.setText("Hide Password");
            isPasswordVisible = true;
        }
    }
}