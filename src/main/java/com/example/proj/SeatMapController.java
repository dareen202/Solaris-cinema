package com.example.proj;

import com.example.proj.utils.DBHandle;
import com.example.proj.utils.Session;
import com.example.proj.utils.SwitchScreens;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.sql.*;
import java.util.*;

public class SeatMapController implements Initializable {


    static final Color BG            = Color.rgb(12,  10,  20);
    static final Color TEXT_C        = Color.rgb(220, 215, 235);
    static final Color SUBTEXT       = Color.rgb(120, 110, 150);
    static final Color GOLD          = Color.rgb(255, 200,  60);
    static final Color SEAT_BOOKED   = Color.web("#E5C9D7");
    static final Color SEAT_PREMIUM  = Color.web("#26415E");
    static final Color SEAT_VIP      = Color.web("#83A6CE");
    static final Color SEAT_REGULAR  = Color.web("#0B1B32");
    static final Color SEAT_SELECTED = Color.web("#C48CB3");
    static final String S_BG   = "#0C0A14";
    static final String S_CARD = "#191426";
    static final String S_GREEN= "#32A05A";
    static final String S_RED  = "#A03232";

    static final int ST_AVAILABLE = 0;
    static final int ST_BOOKED    = 1;
    static final int ST_SELECTED  = 2;
    static final int ROWS = 10;
    static final int COLS = 10;
    static final int SW = 38, SH = 32, GX = 9, GY = 10, OX = 50, OY = 55;

    int[][] seats   = new int[ROWS][COLS];
    int[][] seatIds = new int[ROWS][COLS];
    Map<String, Double> prices = new HashMap<>();
    int screenId;

    @FXML private Label titleLbl;
    @FXML private Label subLbl;
    @FXML private ScrollPane scrollPane;
    @FXML private Label statusLbl;
    @FXML private Label selCountLbl;
    @FXML private Label selPriceLbl;
    @FXML private Label totalLbl;
    @FXML private Button bookBtn;
    @FXML private Button cancelBtn;
    @FXML private HBox legendBox;
    @FXML private StackPane canvasHolder;

    SeatCanvas seatCanvas;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        titleLbl.setText("🎬  " + Session.selectedMovieTitle);
        subLbl.setText(Session.selectedScreenName + "  ·  🕐 " + Session.selectedShowtime + "  ·  👤 " + Session.username);

        seatCanvas = new SeatCanvas();
        seatCanvas.onChange = this::refresh;
        canvasHolder.getChildren().add(seatCanvas);
        canvasHolder.setAlignment(Pos.CENTER);

        scrollPane.setContent(canvasHolder);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background:" + S_BG + ";-fx-background-color:" + S_BG + ";-fx-border-color:transparent;");

        bookBtn.setDisable(true);
        cancelBtn.setDisable(true);
        bookBtn.setOnAction(e -> onConfirm());
        cancelBtn.setOnAction(e -> onCancelSelected());

        buildLegend();
        initializeSeats(Session.selectedShowId);
    }

    void initializeSeats(int showId) {
        for (int r = 0; r < ROWS; r++) {
            Arrays.fill(seats[r], ST_AVAILABLE);
            Arrays.fill(seatIds[r], 0);
        }

        try (Connection con = DBHandle.connect()) {
            if (con == null) return;

            try (PreparedStatement ps = con.prepareStatement("SELECT screen_id FROM Shows WHERE show_id = ?")) {
                ps.setInt(1, showId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) screenId = rs.getInt("screen_id");
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT seat_id, seat_number FROM Seats WHERE screen_id = ?")) {
                ps.setInt(1, screenId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String sn = rs.getString("seat_number").toUpperCase();
                    int row = sn.charAt(0) - 'A';
                    int col = Integer.parseInt(sn.substring(1)) - 1;
                    if (row >= 0 && row < ROWS && col >= 0 && col < COLS) seatIds[row][col] = rs.getInt("seat_id");
                }
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT seat_id FROM Bookings WHERE show_id = ? AND status = 'BOOKED'")) {
                ps.setInt(1, showId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int sid = rs.getInt("seat_id");
                    outer: for (int r = 0; r < ROWS; r++)
                        for (int c = 0; c < COLS; c++)
                            if (seatIds[r][c] == sid) { seats[r][c] = ST_BOOKED; break outer; }
                }
            }

            double basePrice = 50.0;

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT ticket_price FROM Shows WHERE show_id = ?")) {

                ps.setInt(1, showId);

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    basePrice = rs.getDouble("ticket_price");
                }
            }

            prices.put("Regular", basePrice);
            prices.put("Premium", basePrice + 20);
            prices.put("VIP", basePrice + 40);
        } catch (SQLException e) {
            showAlert("DB Error", "Could not load seats:\n" + e.getMessage());
        }

        buildLegend();
        if (seatCanvas != null) seatCanvas.draw();
        refresh();
    }


    void onConfirm() {
        List<int[]> selected = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (seats[r][c] == ST_SELECTED) selected.add(new int[]{r, c});
            }
        }

        if (selected.isEmpty()) return;

        try (Connection con = DBHandle.connect()) {
            if (con == null) return;

            int userId = -1;
            try (PreparedStatement ps = con.prepareStatement("SELECT user_id FROM Users WHERE username = ?")) {
                ps.setString(1, Session.username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) userId = rs.getInt("user_id");
            }

            if (userId == -1) { showAlert("Error", "User not found."); return; }

            con.setAutoCommit(false);
            try {
                for (int[] rc : selected) {
                    int sId = seatIds[rc[0]][rc[1]];
                    if (sId == 0) throw new SQLException("Seat " + (char)('A'+rc[0]) + (rc[1]+1) + " not found in DB.");

                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO Bookings (user_id, show_id, seat_id, status) VALUES (?, ?, ?, 'BOOKED')")) {
                        ps.setInt(1, userId);
                        ps.setInt(2, Session.selectedShowId);
                        ps.setInt(3, sId);
                        ps.executeUpdate();
                    }
                    seats[rc[0]][rc[1]] = ST_BOOKED;
                }
                con.commit();
                showAlert("Success", selected.size() + " seat(s) booked successfully!");
            } catch (SQLException e) {
                con.rollback();
                showAlert("Booking Error", e.getMessage());
                return;
            }
        } catch (SQLException e) {
            showAlert("DB Error", e.getMessage());
            return;
        }

        seatCanvas.selR = -1; seatCanvas.selC = -1;
        seatCanvas.draw();
        refresh();
    }

    void onCancelSelected() {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                if (seats[r][c] == ST_SELECTED) seats[r][c] = ST_AVAILABLE;
        seatCanvas.selR = -1; seatCanvas.selC = -1;
        seatCanvas.draw();
        refresh();
    }

    @FXML void goBack(ActionEvent event) { SwitchScreens.switchScene(event, "shows-view.fxml", "Shows"); }

    String getSeatCategory(int row) { return row <= 1 ? "VIP" : row <= 4 ? "Premium" : "Regular"; }
    double getSeatPrice(int row) {
        return prices.getOrDefault(
                getSeatCategory(row),
                50.0
        );
    }
    void toggleSelect(int r, int c) {
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS || seats[r][c] == ST_BOOKED) return;
        seats[r][c] = seats[r][c] == ST_AVAILABLE ? ST_SELECTED : ST_AVAILABLE;
    }

    void refresh() {
        int selCount = 0; double selPrice = 0; int booked = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (seats[r][c] == ST_SELECTED) { selCount++; selPrice += getSeatPrice(r); }
                if (seats[r][c] == ST_BOOKED) booked++;
            }
        }
        selCountLbl.setText(selCount + " seat" + (selCount != 1 ? "s" : "") + " selected");
        selPriceLbl.setText(String.format("$%.2f", selPrice));
        bookBtn.setDisable(selCount == 0);
        cancelBtn.setDisable(selCount == 0);
        totalLbl.setText(String.format("Booked: %d / %d", booked, ROWS * COLS));

        if (seatCanvas != null && seatCanvas.selR >= 0 && seatCanvas.selC >= 0) {
            int r = seatCanvas.selR, c = seatCanvas.selC;
            String sid = (char) ('A' + r) + "" + (c + 1);
            int st = seats[r][c];
            String stTxt = st == ST_BOOKED ? "Booked" : st == ST_SELECTED ? "Selected" : "Available";
            statusLbl.setText("Seat " + sid + "  |  " + getSeatCategory(r) + "  |  $" + (int) getSeatPrice(r) + "  |  " + stTxt);
        } else {
            statusLbl.setText("Click a seat to select");
        }
    }

    void buildLegend() {
        legendBox.getChildren().clear();

        legendBox.getChildren().addAll(
                legendItem(
                        SEAT_REGULAR,
                        "Regular  $" + String.format("%.0f",
                                prices.getOrDefault("Regular", 0.0))
                ),

                legendItem(
                        SEAT_PREMIUM,
                        "Premium  $" + String.format("%.0f",
                                prices.getOrDefault("Premium", 0.0))
                ),

                legendItem(
                        SEAT_VIP,
                        "VIP  $" + String.format("%.0f",
                                prices.getOrDefault("VIP", 0.0))
                ),

                legendItem(SEAT_SELECTED, "Selected"),
                legendItem(SEAT_BOOKED, "Booked")
        );
    }

    static HBox legendItem(Color c, String text) {
        HBox box = new HBox(5); box.setAlignment(Pos.CENTER_LEFT);
        Canvas ic = new Canvas(14, 16);
        ic.getGraphicsContext2D().setFill(c);
        ic.getGraphicsContext2D().fillRoundRect(0, 3, 14, 10, 4, 4);
        Label lbl = new Label(text);
        lbl.setFont(Font.font("SansSerif", 10));
        lbl.setTextFill(SUBTEXT);
        box.getChildren().addAll(ic, lbl);
        return box;
    }

    static void showAlert(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            a.setTitle(title); a.setHeaderText(null); a.showAndWait();
        });
    }

    class SeatCanvas extends Canvas {
        int hoverR = -1, hoverC = -1, selR = -1, selC = -1;
        Runnable onChange;

        SeatCanvas() {
            super(OX * 2 + COLS * (SW + GX) - GX, OY + ROWS * (SH + GY) - GY + 24);
            setOnMouseMoved(this::onMove);
            setOnMouseClicked(this::onClick);
            draw();
        }

        void onMove(MouseEvent e) {
            int[] rc = hit(e.getX(), e.getY());
            int nr = rc != null ? rc[0] : -1, nc = rc != null ? rc[1] : -1;
            if (nr != hoverR || nc != hoverC) {
                hoverR = nr; hoverC = nc;
                setCursor(rc != null ? Cursor.HAND : Cursor.DEFAULT);
                draw();
            }
        }

        void onClick(MouseEvent e) {
            int[] rc = hit(e.getX(), e.getY());
            if (rc == null) { selR = -1; selC = -1; }
            else {
                selR = rc[0]; selC = rc[1];
                if (seats[rc[0]][rc[1]] != ST_BOOKED) toggleSelect(rc[0], rc[1]);
            }
            draw();
            if (onChange != null) onChange.run();
        }

        int[] hit(double px, double py) {
            for (int r = 0; r < ROWS; r++)
                for (int c = 0; c < COLS; c++) {
                    int x = OX + c * (SW + GX), y = OY + r * (SH + GY);
                    if (px >= x && px <= x + SW && py >= y && py <= y + SH) return new int[]{r, c};
                }
            return null;
        }

        void draw() {
            GraphicsContext g = getGraphicsContext2D();
            g.clearRect(0, 0, getWidth(), getHeight());
            g.setFill(BG); g.fillRect(0, 0, getWidth(), getHeight());

            int sW = COLS * (SW + GX) - GX;
            g.setStroke(Color.rgb(180, 50, 120, 0.5)); g.setLineWidth(2.5);
            g.strokeArc(OX - 30, 8, sW + 60, 36, 0, 180, javafx.scene.shape.ArcType.OPEN);
            g.setStroke(Color.rgb(180, 50, 120, 0.85)); g.setLineWidth(3);
            g.strokeLine(OX, 27, OX + sW, 27);
            g.setFill(Color.rgb(140, 120, 160));
            g.setFont(Font.font("SansSerif", FontWeight.BOLD, 10));
            String scr = "S C R E E N";
            g.fillText(scr, OX + (sW - scr.length() * 5.6) / 2, 42);

            char[] RL = "ABCDEFGHIJ".toCharArray();
            for (int r = 0; r < ROWS; r++) {
                int ly = OY + r * (SH + GY) + SH / 2 + 4;
                g.setFill(Color.rgb(90, 80, 120));
                g.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
                g.fillText(String.valueOf(RL[r]), OX - 16, ly);
                g.fillText(String.valueOf(RL[r]), OX + COLS * (SW + GX) + 5, ly);

                for (int c = 0; c < COLS; c++) {
                    int x = OX + c * (SW + GX), y = OY + r * (SH + GY);
                    int state = seats[r][c];
                    boolean isBooked = state == ST_BOOKED;
                    boolean isSel = state == ST_SELECTED;
                    boolean isHover = r == hoverR && c == hoverC;
                    boolean isVip = r <= 1;
                    boolean isPrem = r >= 2 && r <= 4;

                    Color base;
                    if (isSel) base = SEAT_SELECTED;
                    else if (isBooked) base = SEAT_BOOKED;
                    else if (isVip) base = SEAT_VIP;
                    else if (isPrem) base = SEAT_PREMIUM;
                    else base = SEAT_REGULAR;
                    if (isHover && !isBooked && !isSel) base = base.brighter();

                    g.setFill(base); roundRect(g, x, y, SW, SH - 7, 7);
                    g.setFill(base.darker()); roundRect(g, x + 4, y + SH - 10, SW - 8, 10, 5);
                    g.setFill(base.darker()); roundRect(g, x, y + 4, 4, SH - 12, 3);
                    roundRect(g, x + SW - 4, y + 4, 4, SH - 12, 3);

                    if (!isBooked) {
                        g.setFill(Color.rgb(255, 255, 255, 0.12));
                        roundRect(g, x + 7, y + 4, SW - 14, 6, 3);
                    }
                    if (isBooked) {
                        g.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
                        g.setFill(Color.rgb(0, 0, 0, 0.45));
                        g.fillText("✕", x + SW / 2.0 - 4, y + SH / 2.0 + 4);
                    }
                }
            }
        }

        void roundRect(GraphicsContext g, double x, double y, double w, double h, double r) {
            g.beginPath();
            g.moveTo(x + r, y); g.lineTo(x + w - r, y);
            g.quadraticCurveTo(x + w, y, x + w, y + r);
            g.lineTo(x + w, y + h - r);
            g.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
            g.lineTo(x + r, y + h);
            g.quadraticCurveTo(x, y + h, x, y + h - r);
            g.lineTo(x, y + r);
            g.quadraticCurveTo(x, y, x + r, y);
            g.closePath(); g.fill();
        }
    }
}