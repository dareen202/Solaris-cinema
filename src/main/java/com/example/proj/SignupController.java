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

public class SignupController {

    @FXML private TextField userTF;
    @FXML private PasswordField passTF;
    @FXML private PasswordField conpassTF;

    @FXML
    void onSignUpBTNClick(ActionEvent event) {

        String username = userTF.getText().trim();
        String password = passTF.getText();
        String confirmPassword = conpassTF.getText();
        String role = "User";

        if (username.isEmpty()
                || password.isEmpty()
                || confirmPassword.isEmpty()) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Form Error!",
                    "All fields are required."
            );

            return;
        }

        // Validate passwords
        if (!password.equals(confirmPassword)) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Password Error",
                    "Passwords do not match!"
            );

            return;
        }

        if (isUsernameTaken(username)) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Registration Error",
                    "This username is already taken."
            );

            return;
        }

        boolean success = saveUserToSQLServer(
                username,
                password,
                role,
                event
        );

        if (success) {

            userTF.clear();
            passTF.clear();
            conpassTF.clear();
        }
    }

    private boolean isUsernameTaken(String username) {

        String query =
                "SELECT COUNT(*) FROM Users WHERE username = ?";

        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt =
                     conn.prepareStatement(query)) {

            pstmt.setString(1, username);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean saveUserToSQLServer(
            String user,
            String pass,
            String role,
            ActionEvent event
    ) {

        String query =
                "INSERT INTO Users (username, password, role) " +
                        "VALUES (?, ?, ?)";

        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt =
                     conn.prepareStatement(query)) {

            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            pstmt.setString(3, role);

            pstmt.executeUpdate();

            Session.username = user;
            Session.role = role;

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Registration complete!"
            );

            SwitchScreens.switchScene(
                    event,
                    "user-view.fxml",
                    "Solaris Cinema"
            );

            return true;

        } catch (SQLException e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    e.getMessage()
            );

            return false;
        }
    }

    @FXML
    private void handleLoginHL(ActionEvent event)
            throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource(
                        "/com/example/proj/login-view.fxml"
                )
        );

        Scene scene = new Scene(
                fxmlLoader.load(),
                600,
                800
        );

        Stage stage =
                (Stage) ((Node) event.getSource())
                        .getScene()
                        .getWindow();

        stage.setTitle("Welcome to Solaris");
        stage.setScene(scene);
        stage.show();
    }

    private void showAlert(
            Alert.AlertType type,
            String title,
            String content
    ) {

        Alert alert = new Alert(type);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        alert.showAndWait();
    }
}