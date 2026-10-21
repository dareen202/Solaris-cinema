package com.example.proj;

import com.example.proj.utils.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import java.time.LocalDate;
import java.sql.Time;

public class AdminController implements Initializable {
    @FXML private TextField titleTF;
    @FXML private TextField durationTF;
    @FXML private TextField ratingTF;
    @FXML private TextField posterTF;
    @FXML private TextArea descTA;
    @FXML private ListView<String> genreLV;
    @FXML private ScrollPane addmovieScrollPane;
    @FXML private ScrollPane moviesScrollPane;
    @FXML private ScrollPane screensScrollPane;
    @FXML private ScrollPane addShowScrollPane;
    @FXML private ScrollPane screenMoviesScrollPane;
    @FXML private FlowPane moviesFlowPane;
    @FXML private FlowPane screensContainer;
    @FXML private FlowPane screenMoviesFlowPane;
    @FXML private Label screenMoviesTitleLbl;
    @FXML private ComboBox<String> movieCB;
    @FXML private ComboBox<String> screenCB;
    @FXML private DatePicker showDateDP;
    @FXML private TextField startTimeTF;
    @FXML private TextField ticketPriceTF;

    private Map<String, Integer> genreMap = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        genreLV.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        loadGenresFromDatabase();
        loadMoviesIntoComboBox();
        loadScreensIntoComboBox();
        showDateDP.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        setVisible(moviesScrollPane, false);
        setVisible(screensScrollPane, false);
        setVisible(addShowScrollPane, false);
        setVisible(screenMoviesScrollPane, false);
        setVisible(addmovieScrollPane, true);
    }

    private void setVisible(ScrollPane pane, boolean visible) {
        pane.setVisible(visible);
        pane.setManaged(visible);
    }

    private void showOnly(ScrollPane toShow) {
        setVisible(addmovieScrollPane, false);
        setVisible(moviesScrollPane, false);
        setVisible(screensScrollPane, false);
        setVisible(addShowScrollPane, false);
        setVisible(screenMoviesScrollPane, false);
        setVisible(toShow, true);
    }

    private void loadGenresFromDatabase() {
        String query = "SELECT genre_id, genre_name FROM Genres";
        ObservableList<String> genreNames = FXCollections.observableArrayList();

        try (Connection conn = DBHandle.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                genreMap.put(rs.getString("genre_name"), rs.getInt("genre_id"));
                genreNames.add(rs.getString("genre_name"));
            }
            genreLV.setItems(genreNames);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onAddmovieBTNclick(ActionEvent event) {
        String title = titleTF.getText();
        String durationStr = durationTF.getText();
        String ratingStr = ratingTF.getText();
        String poster = posterTF.getText();
        String description = descTA.getText();
        ObservableList<String> selectedGenres = genreLV.getSelectionModel().getSelectedItems();

        if (title.isEmpty() || durationStr.isEmpty() || selectedGenres.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Title, Duration, and at least one Genre are required.");
            return;
        }

        try {
            int duration = Integer.parseInt(durationStr);
            double rating = ratingStr.isEmpty() ? 0.0 : Double.parseDouble(ratingStr);
            java.util.List<Integer> genreIds = new java.util.ArrayList<>();
            for (String name : selectedGenres) {
                genreIds.add(genreMap.get(name));
            }

            insertMovieWithGenres(title, duration, description, poster, rating, genreIds);

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Duration must be a whole number.");
        }
    }

    private void insertMovieWithGenres(String title, int duration, String desc, String poster, double rating, java.util.List<Integer> genreIds) {
        String insertMovieSQL = "INSERT INTO Movies (title, duration, description, poster, rating) VALUES (?, ?, ?, ?, ?)";
        String insertLinkSQL = "INSERT INTO MovieGenres (movie_id, genre_id) VALUES (?, ?)";

        Connection conn = null;

        try {
            conn = DBHandle.connect();
            if (conn == null) return;

            conn.setAutoCommit(false);

            try (PreparedStatement movieStmt = conn.prepareStatement(insertMovieSQL, Statement.RETURN_GENERATED_KEYS)) {
                movieStmt.setString(1, title);
                movieStmt.setInt(2, duration);
                movieStmt.setString(3, desc);
                movieStmt.setString(4, poster);
                movieStmt.setDouble(5, rating);
                movieStmt.executeUpdate();

                ResultSet rs = movieStmt.getGeneratedKeys();
                if (rs.next()) {
                    int newMovieId = rs.getInt(1);

                    try (PreparedStatement linkStmt = conn.prepareStatement(insertLinkSQL)) {
                        for (Integer id : genreIds) {
                            linkStmt.setInt(1, newMovieId);
                            linkStmt.setInt(2, id);
                            linkStmt.addBatch();
                        }
                        linkStmt.executeBatch();
                    }
                }
            }

            conn.commit();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Movie " + title + " added!");
            clearForm();

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    @FXML
    void onLogoutBTNclick(ActionEvent event) {
        SwitchScreens.switchScene(event, "login-view.fxml", "Solaris Cinema - Login");
    }

    @FXML
    void onviewMoviesBTNclick(ActionEvent event) {
        loadMoviesFromDatabase();
        showOnly(moviesScrollPane);
    }

    @FXML
    void onviewaddmovieBTNclick(ActionEvent event) {
        showOnly(addmovieScrollPane);
    }

    @FXML
    void onScreensBTNclick(ActionEvent event) {
        loadScreens();
        showOnly(screensScrollPane);
    }

    @FXML
    void onAddShowBTNclick(ActionEvent event) {
        showOnly(addShowScrollPane);
    }

    private void clearForm() {
        titleTF.clear();
        durationTF.clear();
        ratingTF.clear();
        posterTF.clear();
        descTA.clear();
        genreLV.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void loadScreens() {
        screensContainer.getChildren().clear();

        String query = "SELECT * FROM Screens";

        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet resultSet = pstmt.executeQuery()) {

            while (resultSet.next()) {
                String screenName = resultSet.getString("screen_name");
                int screenId = resultSet.getInt("screen_id");
                int totalSeats = resultSet.getInt("total_seats");

                VBox screenCard = new VBox();
                screenCard.setSpacing(12);
                screenCard.setPrefWidth(270);
                screenCard.setMaxWidth(270);
                screenCard.setStyle(
                        "-fx-background-color: #243b55;" +
                                "-fx-background-radius: 18;" +
                                "-fx-padding: 20;" +
                                "-fx-border-color: #adcfef;" +
                                "-fx-border-radius: 18;"
                );

                Label nameLabel = new Label(screenName);
                nameLabel.setStyle("-fx-text-fill: #adcfef; -fx-font-size: 24px; -fx-font-weight: bold;");

                Label idLabel = new Label("Screen ID: " + screenId);
                idLabel.setStyle("-fx-text-fill: #e3cdd8; -fx-font-size: 16px;");

                Label seatsLabel = new Label("Total Seats: " + totalSeats);
                seatsLabel.setStyle("-fx-text-fill: #e3cdd8; -fx-font-size: 16px;");

                Button moviesButton = new Button("Show Movies");
                moviesButton.setPrefWidth(160);
                moviesButton.setPrefHeight(45);
                moviesButton.setStyle(
                        "-fx-background-color: #d18bb9;" +
                                "-fx-text-fill: #1a304a;" +
                                "-fx-font-size: 16px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-background-radius: 12;"
                );
                moviesButton.setOnAction(e -> loadMoviesForScreen(screenId, screenName));

                screenCard.getChildren().addAll(nameLabel, idLabel, seatsLabel, moviesButton);
                screensContainer.getChildren().add(screenCard);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMoviesFromDatabase() {
        moviesFlowPane.getChildren().clear();

        String query = "SELECT m.movie_id, m.title, m.duration, m.description, m.poster, m.rating, " +
                "ISNULL(g.genres, 'Uncategorized') AS genres " +
                "FROM Movies m " +
                "LEFT JOIN (" +
                "    SELECT mg.movie_id, STRING_AGG(gen.genre_name, ', ') AS genres " +
                "    FROM MovieGenres mg " +
                "    JOIN Genres gen ON mg.genre_id = gen.genre_id " +
                "    GROUP BY mg.movie_id" +
                ") g ON m.movie_id = g.movie_id";

        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int movieId = rs.getInt("movie_id");
                String title = rs.getString("title");
                int duration = rs.getInt("duration");
                String desc = rs.getString("description");
                String posterUrl = rs.getString("poster");
                double rating = rs.getDouble("rating");
                String genres = rs.getString("genres");

                VBox movieCard = createMovieCard(movieId, title, duration, genres, rating, posterUrl, desc);
                moviesFlowPane.getChildren().add(movieCard);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox createMovieCard(int movieId, String title, int duration, String genres, double rating, String posterUrl, String desc) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #1a304a; -fx-background-radius: 15; -fx-border-color: rgba(211,140,185,0.2); -fx-border-radius: 15;");
        card.setPrefWidth(180);
        card.setMaxWidth(180);
        card.setMinHeight(300);

        ImageView poster = new ImageView();
        poster.setFitWidth(150);
        poster.setFitHeight(220);
        poster.setPreserveRatio(false);

        String resourcePath = "/images/" + posterUrl;
        try {
            if (posterUrl != null && !posterUrl.isEmpty()) {
                URL imageUrl = getClass().getResource(resourcePath);
                if (imageUrl != null) {
                    poster.setImage(new Image(imageUrl.toExternalForm(), true));
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading image: " + e.getMessage());
        }

        Label titleLbl = new Label(title);
        titleLbl.setTextFill(Color.web("#f2cde8"));
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLbl.setWrapText(true);
        titleLbl.setAlignment(Pos.CENTER);

        Label subtitleLbl = new Label(genres + " | ★ " + rating);
        subtitleLbl.setTextFill(Color.web("#adcdef"));
        subtitleLbl.setFont(Font.font("System", 12));

        Label durationLbl = new Label(duration + " mins");
        durationLbl.setTextFill(Color.web("#6c757d"));
        durationLbl.setFont(Font.font("System", 12));

        card.getChildren().addAll(poster, titleLbl, subtitleLbl, durationLbl);
        card.setOnMouseClicked(event -> {
            Session.selectedMovieId = movieId;
            SwitchScreens.switchScene(event, "movie-view.fxml", "Solaris Cinema - Movie Details");
        });
        card.setCursor(javafx.scene.Cursor.HAND);
        return card;
    }

    private void loadMoviesIntoComboBox() {
        String query = "SELECT title FROM Movies";

        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                movieCB.getItems().add(rs.getString("title"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadScreensIntoComboBox() {
        String query = "SELECT screen_name FROM Screens";

        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                screenCB.getItems().add(rs.getString("screen_name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @FXML
    void onReportsBTNclick(ActionEvent event) {
        SwitchScreens.switchScene(event, "reports-view.fxml", "Solaris Cinema - Sales & Reports");
    }
    @FXML
    void onSaveShowBTNclick(ActionEvent event) {
        String movieName = movieCB.getValue();
        String screenName = screenCB.getValue();
        LocalDate showDate = showDateDP.getValue();
        String startTime = startTimeTF.getText();
        String ticketPrice = ticketPriceTF.getText();

        if (movieName == null || screenName == null || showDate == null ||
                startTime.isEmpty() || ticketPrice.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please fill all fields.");
            return;
        }

        try {
            int movieId = getMovieId(movieName);
            int screenId = getScreenId(screenName);
            insertShow(movieId, screenId, showDate, startTime, Double.parseDouble(ticketPrice));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    private int getMovieId(String movieName) throws SQLException {
        String query = "SELECT movie_id FROM Movies WHERE title = ?";
        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, movieName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("movie_id");
        }
        return -1;
    }

    private int getScreenId(String screenName) throws SQLException {
        String query = "SELECT screen_id FROM Screens WHERE screen_name = ?";
        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, screenName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("screen_id");
        }
        return -1;
    }

    private void insertShow(int movieId, int screenId, LocalDate showDate, String startTime, double ticketPrice) {
        String query = """
            INSERT INTO Shows (movie_id, screen_id, show_date, start_time, ticket_price)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, movieId);
            pstmt.setInt(2, screenId);
            pstmt.setDate(3, java.sql.Date.valueOf(showDate));
            pstmt.setTime(4, Time.valueOf(startTime + ":00"));
            pstmt.setDouble(5, ticketPrice);
            pstmt.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Show added successfully!");
            clearShowForm();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void clearShowForm() {
        movieCB.setValue(null);
        screenCB.setValue(null);
        showDateDP.setValue(null);
        startTimeTF.clear();
        ticketPriceTF.clear();
    }

    private void loadMoviesForScreen(int screenId, String screenName) {
        screenMoviesFlowPane.getChildren().clear();
        screenMoviesTitleLbl.setText(screenName + " Movies");
        showOnly(screenMoviesScrollPane);

        String query =
                "SELECT m.movie_id, m.title, m.duration, m.description, m.poster, m.rating, " +
                        "s.show_date, s.start_time, s.ticket_price, " +
                        "ISNULL(g.genres, 'Uncategorized') AS genres " +
                        "FROM Shows s " +
                        "JOIN Movies m ON s.movie_id = m.movie_id " +
                        "LEFT JOIN (" +
                        "   SELECT mg.movie_id, STRING_AGG(gen.genre_name, ', ') AS genres " +
                        "   FROM MovieGenres mg " +
                        "   JOIN Genres gen ON mg.genre_id = gen.genre_id " +
                        "   GROUP BY mg.movie_id" +
                        ") g ON m.movie_id = g.movie_id " +
                        "WHERE s.screen_id = ? " +
                        "ORDER BY s.show_date, s.start_time";

        try (Connection conn = DBHandle.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, screenId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String title = rs.getString("title");
                String genres = rs.getString("genres");
                int duration = rs.getInt("duration");
                double rating = rs.getDouble("rating");
                String posterUrl = rs.getString("poster");
                Date showDate = rs.getDate("show_date");
                Time startTime = rs.getTime("start_time");
                double ticketPrice = rs.getDouble("ticket_price");

                VBox card = new VBox(10);
                card.setAlignment(Pos.TOP_CENTER);
                card.setPadding(new Insets(15));
                card.setPrefWidth(230);
                card.setStyle("-fx-background-color: #1a304a; -fx-background-radius: 15; -fx-border-color: rgba(211,140,185,0.2); -fx-border-radius: 15;");

                ImageView poster = new ImageView();
                poster.setFitWidth(180);
                poster.setFitHeight(250);
                poster.setPreserveRatio(false);

                try {
                    URL imageUrl = getClass().getResource("/images/" + posterUrl);
                    if (imageUrl != null) poster.setImage(new Image(imageUrl.toExternalForm(), true));
                } catch (Exception e) { e.printStackTrace(); }

                Label titleLbl = new Label(title);
                titleLbl.setWrapText(true);
                titleLbl.setTextFill(Color.web("#f2cde8"));
                titleLbl.setFont(Font.font("System", FontWeight.BOLD, 18));

                Label genresLbl = new Label(genres + " | ★ " + rating);
                genresLbl.setTextFill(Color.web("#adcdef"));

                Label durationLbl = new Label(duration + " mins");
                durationLbl.setTextFill(Color.web("#cccccc"));

                Label dateLbl = new Label("Date: " + showDate);
                dateLbl.setTextFill(Color.web("#d18bb9"));

                Label timeLbl = new Label("Time: " + startTime);
                timeLbl.setTextFill(Color.web("#d18bb9"));

                Label priceLbl = new Label("Price: $" + ticketPrice);
                priceLbl.setTextFill(Color.web("#d18bb9"));

                card.getChildren().addAll(poster, titleLbl, genresLbl, durationLbl, dateLbl, timeLbl, priceLbl);
                screenMoviesFlowPane.getChildren().add(card);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}