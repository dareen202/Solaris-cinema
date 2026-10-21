package com.example.proj;

import com.example.proj.utils.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class UserController implements Initializable {
    @FXML private ScrollPane moviesScrollPane;
    @FXML private FlowPane moviesFlowPane;
    @FXML private VBox profileVBox;
    @FXML private Label profileUsernameLabel;
    @FXML private Label profileRoleLabel;
    @FXML private VBox bookingsContainer;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        moviesScrollPane.setStyle("-fx-background: #0d1b2a; -fx-border-color: transparent;");
        loadMoviesFromDatabase();
        loadUserProfile();
    }

    @FXML
    void showMoviesView(ActionEvent event) {
        moviesScrollPane.setVisible(true);
        profileVBox.setVisible(false);
    }

    @FXML
    void showProfileView(ActionEvent event) {
        moviesScrollPane.setVisible(false);
        profileVBox.setVisible(true);
        loadUserProfile();
    }

    @FXML
    void handleLogout(ActionEvent event) {
        Session.username = "";
        SwitchScreens.switchScene(event, "login-view.fxml", "Solaris Cinema - Login");
    }

    private void loadUserProfile() {
        profileUsernameLabel.setText(Session.username);
        bookingsContainer.getChildren().clear();

        String query = "SELECT user_id, role FROM Users WHERE username = ?";
        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, Session.username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");
                profileRoleLabel.setText(rs.getString("role"));
                loadUserBookings(userId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            profileRoleLabel.setText("Unknown");
        }
    }

    private void loadUserBookings(int userId) {

        String query =
                "SELECT b.booking_id, " +
                        "m.title, " +
                        "s.show_date, " +
                        "s.start_time, " +
                        "st.seat_number, " +
                        "st.seat_type, " +
                        "CASE " +
                        "   WHEN st.seat_type = 'Premium' THEN s.ticket_price + 20 " +
                        "   WHEN st.seat_type = 'VIP' THEN s.ticket_price + 40 " +
                        "   ELSE s.ticket_price " +
                        "END AS final_price " +
                        "FROM Bookings b " +
                        "JOIN Shows s ON b.show_id = s.show_id " +
                        "JOIN Movies m ON s.movie_id = m.movie_id " +
                        "JOIN Seats st ON b.seat_id = st.seat_id " +
                        "WHERE b.user_id = ? " +
                        "AND b.status = 'BOOKED' " +
                        "ORDER BY b.booking_time DESC";
        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                HBox bookingRow = createBookingRow(
                        rs.getInt("booking_id"),
                        rs.getString("title"),
                        rs.getString("show_date"),
                        rs.getString("start_time"),
                        rs.getString("seat_number"),
                        //rs.getDouble("ticket_price")
                        rs.getDouble("final_price")
                );
                bookingsContainer.getChildren().add(bookingRow);
            }

            if (bookingsContainer.getChildren().isEmpty()) {
                Label noBookings = new Label("No bookings found.");
                noBookings.setTextFill(Color.GRAY);
                bookingsContainer.getChildren().add(noBookings);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox createBookingRow(int bookingId, String movie, String date, String time, String seat, double price) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: #1a304a; -fx-background-radius: 10; -fx-border-color: #d18bb9; -fx-border-width: 0.5;");

        VBox info = new VBox(2);
        Label mLabel = new Label(movie + " (Seat: " + seat + ")");
        mLabel.setStyle("-fx-text-fill: #f2cde8; -fx-font-weight: bold; -fx-font-size: 14px;");
        String formattedTime = (time != null && time.length() >= 5) ? time.substring(0, 5) : time;
        Label dLabel = new Label(" $" + price+"  "+ formattedTime + "   " + date );
        dLabel.setStyle("-fx-text-fill: #adcdef; -fx-font-size: 12px;");
        info.getChildren().addAll(mLabel, dLabel);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #a84242; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        cancelBtn.setOnAction(e -> handleCancelBooking(bookingId));

        row.getChildren().addAll(info, cancelBtn);
        HBox.setHgrow(info, Priority.ALWAYS);
        return row;
    }

    private void handleCancelBooking(int bookingId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setContentText("Confirm Cancellation");
        confirm.setTitle("Confirm");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try (Connection conn = DBHandle.connect();
                     PreparedStatement pstmt = conn.prepareStatement("UPDATE Bookings SET status = 'CANCELLED' WHERE booking_id = ?")) {
                    pstmt.setInt(1, bookingId);
                    pstmt.executeUpdate();
                    loadUserProfile(); // Refresh list
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void loadMoviesFromDatabase() {
        moviesFlowPane.getChildren().clear();
        String query = "SELECT m.movie_id, m.title, m.duration, m.description, m.poster, m.rating, " +
                "ISNULL(g.genres, 'Uncategorized') AS genres " +
                "FROM Movies m " +
                "LEFT JOIN (SELECT mg.movie_id, STRING_AGG(gen.genre_name, ', ') AS genres FROM MovieGenres mg " +
                "JOIN Genres gen ON mg.genre_id = gen.genre_id GROUP BY mg.movie_id) g ON m.movie_id = g.movie_id";

        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                moviesFlowPane.getChildren().add(createMovieCard(
                        rs.getInt("movie_id"), rs.getString("title"), rs.getInt("duration"),
                        rs.getString("genres"), rs.getDouble("rating"), rs.getString("poster"), rs.getString("description")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private VBox createMovieCard(int movieId, String title, int duration, String genres, double rating, String posterUrl, String desc) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #1a304a; -fx-background-radius: 15; -fx-border-color: rgba(211,140,185,0.2); -fx-border-radius: 15;");
        card.setPrefWidth(250);

        ImageView poster = new ImageView();
        poster.setFitWidth(220); poster.setFitHeight(300);
        try {
            URL imageUrl = getClass().getResource("/images/" + posterUrl);
            if (imageUrl != null) poster.setImage(new Image(imageUrl.toExternalForm(), true));
        } catch (Exception e) { System.out.println("Image error: " + e.getMessage()); }

        Label titleLbl = new Label(title);
        titleLbl.setTextFill(Color.web("#f2cde8")); titleLbl.setFont(Font.font("System", FontWeight.BOLD, 18));

        Button availableShowsBtn = new Button("Available Shows");
        availableShowsBtn.setStyle("-fx-background-color: #d18bb9; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;");
        availableShowsBtn.setOnAction(e -> {
            Session.selectedMovieId = movieId;
            Session.openShowsDirectly = true;
            SwitchScreens.switchScene(e, "shows-view.fxml", "Available Shows");
        });

        card.getChildren().addAll(poster, titleLbl, new Label(genres + " | ★ " + rating), new Label(duration + " mins"), availableShowsBtn);
        return card;
    }
}