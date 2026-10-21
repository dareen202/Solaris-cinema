package com.example.proj;

import com.example.proj.utils.DBHandle;
import com.example.proj.utils.Session;
import com.example.proj.utils.SwitchScreens;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.sql.Date;
import java.sql.Time;
public class MovieController implements Initializable {

    @FXML private ImageView posterIV;
    @FXML private Label titleLbl;
    @FXML private Label infoLbl;
    @FXML private Label descLbl;
    @FXML private ScrollPane showsScrollPane;
    @FXML private FlowPane showsFlowPane;

    private boolean autoShowShows = false;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadMovieDetails();
        if (Session.openShowsDirectly) {
            showsScrollPane.setVisible(true);
            showAvailableShows(null);
            Session.openShowsDirectly = false;
        }
    }


    private void loadMovieDetails() {
        if (Session.selectedMovieId == -1) return;

        String query = "SELECT m.title, m.duration, m.description, m.poster, m.rating, " +
                "ISNULL(g.genres, 'Uncategorized') AS genres " +
                "FROM Movies m " +
                "LEFT JOIN (" +
                "    SELECT mg.movie_id, STRING_AGG(gen.genre_name, ', ') AS genres " +
                "    FROM MovieGenres mg " +
                "    JOIN Genres gen ON mg.genre_id = gen.genre_id " +
                "    GROUP BY mg.movie_id" +
                ") g ON m.movie_id = g.movie_id " +
                "WHERE m.movie_id = ?";

        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, Session.selectedMovieId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                titleLbl.setText(rs.getString("title"));
                descLbl.setText(rs.getString("description"));

                String genres = rs.getString("genres");
                double rating = rs.getDouble("rating");
                int duration = rs.getInt("duration");
                infoLbl.setText(genres + " | ★ " + rating + " | " + duration + " mins");

                String posterUrl = rs.getString("poster");
                try {
                    if (posterUrl != null && !posterUrl.isEmpty()) {
                        URL imageUrl = getClass().getResource("/images/" + posterUrl);
                        if (imageUrl != null) {
                            posterIV.setImage(new Image(imageUrl.toExternalForm(), true));
                        }
                    }
                } catch (Exception e) {
                    System.out.println("No image found or path incorrect.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    void goBack(ActionEvent event) {
        if (Session.role.equalsIgnoreCase("Admin")) {
            SwitchScreens.switchScene(event, "admin-view.fxml", "Admin Panel");
        } else {
            SwitchScreens.switchScene(event, "user-view.fxml", "Solaris Cinema");
        }
    }

    @FXML
    void showAvailableShows(ActionEvent event) {

        showsFlowPane.getChildren().clear();
        showsScrollPane.setVisible(true);

        String query =
                "SELECT s.show_id, " +
                        "sc.screen_name, " +
                        "s.show_date, " +
                        "s.start_time, " +
                        "s.ticket_price " +
                        "FROM Shows s " +
                        "JOIN Screens sc ON s.screen_id = sc.screen_id " +
                        "WHERE s.movie_id = ? " +
                        "ORDER BY s.show_date, s.start_time";

        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, Session.selectedMovieId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String screenName = rs.getString("screen_name");
                Date showDate = rs.getDate("show_date");
                Time startTime = rs.getTime("start_time");
                double ticketPrice = rs.getDouble("ticket_price");
                VBox card = new VBox(10);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setPadding(new Insets(15));
                card.setPrefWidth(260);
                card.setStyle(
                        "-fx-background-color: #1a304a;" +
                                "-fx-background-radius: 15;" +
                                "-fx-border-color: rgba(211,140,185,0.2);" +
                                "-fx-border-radius: 15;"
                );

                Label screenLbl = new Label("Screen: " + screenName);
                screenLbl.setTextFill(Color.web("#f2cde8"));
                screenLbl.setFont(Font.font("System", FontWeight.BOLD, 18));
                Label dateLbl = new Label("Date: " + showDate.toString());
                dateLbl.setTextFill(Color.web("#adcdef"));
                dateLbl.setFont(Font.font(14));
                Label timeLbl = new Label("Start Time: " + startTime.toString());
                timeLbl.setTextFill(Color.web("#adcdef"));
                timeLbl.setFont(Font.font(14));

                Label priceLbl = new Label("Ticket Price: $" + ticketPrice);
                priceLbl.setTextFill(Color.web("#adcdef"));
                priceLbl.setFont(Font.font(14));

                Button bookBtn = new Button("Book Ticket");

                bookBtn.setStyle(
                        "-fx-background-color: #d18bb9;" +
                                "-fx-background-radius: 10;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;"
                );

                bookBtn.setPrefWidth(140);

                card.getChildren().addAll(
                        screenLbl,
                        dateLbl,
                        timeLbl,
                        priceLbl,
                        bookBtn
                );

                showsFlowPane.getChildren().add(card);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void enableAutoShowShows() {
        autoShowShows = true;
    }
}