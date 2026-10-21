package com.example.proj;

import com.example.proj.utils.DBHandle;
import com.example.proj.utils.Session;
import com.example.proj.utils.SwitchScreens;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import javafx.scene.paint.Color;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.sql.Date;
import java.sql.Time;

import java.util.ResourceBundle;

public class ShowsController implements Initializable {

    @FXML
    private FlowPane showsFlowPane;

    @FXML
    private Label movieTitleLbl;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        loadShows();
    }

    private void loadShows() {

        String query =
                "SELECT s.show_id, " +
                        "m.title, " +
                        "sc.screen_name, " +
                        "s.show_date, " +
                        "s.start_time, " +
                        "s.ticket_price " +
                        "FROM Shows s " +
                        "JOIN Movies m ON s.movie_id = m.movie_id " +
                        "JOIN Screens sc ON s.screen_id = sc.screen_id " +
                        "WHERE s.movie_id = ? " +
                        "ORDER BY s.show_date, s.start_time";

        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, Session.selectedMovieId);

            ResultSet rs = pstmt.executeQuery();

            boolean first = true;

            while (rs.next()) {

                int showId = rs.getInt("show_id");

                String movieTitle = rs.getString("title");

                String screenName = rs.getString("screen_name");

                Date showDate = rs.getDate("show_date");

                Time startTime = rs.getTime("start_time");

                double ticketPrice = rs.getDouble("ticket_price");

                if (first) {

                    movieTitleLbl.setText(movieTitle + " Shows");

                    first = false;
                }

                VBox card = new VBox(12);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setPadding(new Insets(20));
                card.setPrefWidth(380);

                card.setStyle(
                        "-fx-background-color: #243b55;" +
                                "-fx-background-radius: 18;" +
                                "-fx-padding: 20;" +
                                "-fx-border-color: #adcfef;" +
                                "-fx-border-radius: 18;"
                );

                Label screenLbl = new Label("Screen: " + screenName);
                screenLbl.setTextFill(Color.web("#adcfef"));
                screenLbl.setFont(Font.font("System", FontWeight.BOLD, 22));

                Label dateLbl = new Label("Date: " + showDate);
                dateLbl.setTextFill(Color.web("#e3cdd8"));
                dateLbl.setFont(Font.font(16));

                Label timeLbl = new Label("Start Time: " + startTime);
                timeLbl.setTextFill(Color.web("#e3cdd8"));
                timeLbl.setFont(Font.font(16));

                Label priceLbl = new Label("Ticket Price: $" + ticketPrice);
                priceLbl.setTextFill(Color.web("#e3cdd8"));
                priceLbl.setFont(Font.font(16));

                Button bookBtn = new Button("Book Ticket");

                bookBtn.setPrefWidth(160);
                bookBtn.setPrefHeight(45);

                bookBtn.setStyle(
                        "-fx-background-color: #d18bb9;" +
                                "-fx-text-fill: #1a304a;" +
                                "-fx-font-size: 16px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-background-radius: 12;"
                );

                bookBtn.setOnAction(e -> {

                    Session.selectedShowId = showId;
                    Session.selectedMovieTitle = movieTitle;
                    Session.selectedScreenName = screenName;
                    Session.selectedShowtime = showDate + " " + startTime;

                    SwitchScreens.switchScene(
                            e,
                            "seat-map-view.fxml",
                            "Seat Map"
                    );
                });

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

    @FXML
    void goBack(ActionEvent event) {

        SwitchScreens.switchScene(
                event,
                "user-view.fxml",
                "Solaris Cinema"
        );
    }
}
