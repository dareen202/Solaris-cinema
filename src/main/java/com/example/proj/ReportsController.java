package com.example.proj;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.time.LocalDate;
import java.util.Map;
import java.util.ResourceBundle;
import com.example.proj.utils.SwitchScreens;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
public class ReportsController implements Initializable {

    @FXML private VBox tabOverview;
    @FXML private VBox tabPerMovie;
    @FXML private VBox tabDaily;
    @FXML private VBox tabPrice;
    @FXML private VBox tabAnalytics;
    @FXML private Button btnOverview;
    @FXML private Button btnPerMovie;
    @FXML private Button btnDaily;
    @FXML private Button btnPrice;
    @FXML private Button btnAnalytics;
    @FXML private Label lblTotalTickets;
    @FXML private Label lblTotalRevenue;
    @FXML private Label lblTopMovie;
    @FXML private ListView<String> listSalesPerMovie;
    @FXML private ListView<String> listRevenuePerMovie;
    @FXML private DatePicker datePicker;
    @FXML private TextArea areaDaily;
    @FXML private ListView<String> listPrices;
    @FXML private ComboBox<String> comboMovie;
    @FXML private TextField fieldPrice;
    @FXML private Label lblPriceResult;
    @FXML private PieChart pieChart;
    @FXML private LineChart<String, Number> barChart;

    private ReportsModel model;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        model = new ReportsModel();
        showTab(tabOverview);
        refreshOverview();
        refreshPerMovie();
        refreshPrices();
        loadComboMovies();
        datePicker.setValue(LocalDate.now());
        setActiveButton(btnOverview);
    }


    private void setActiveButton(Button active) {
        String normal = "-fx-background-color: transparent; -fx-border-color: #d18bb9; " +
                "-fx-border-radius: 10; -fx-text-fill: #d18bb9; -fx-cursor: hand;";
        String activeStyle = "-fx-background-color: #d18bb9; -fx-background-radius: 10; " +
                "-fx-text-fill: #112236; -fx-cursor: hand;";
        btnOverview.setStyle(normal);
        btnPerMovie.setStyle(normal);
        btnDaily.setStyle(normal);
        btnPrice.setStyle(normal);
        btnAnalytics.setStyle(normal);
        active.setStyle(activeStyle);
    }

    @FXML void onOverview()  { showTab(tabOverview);  refreshOverview(); setActiveButton(btnOverview); }
    @FXML void onPerMovie()  { showTab(tabPerMovie);  refreshPerMovie(); setActiveButton(btnPerMovie); }
    @FXML void onDaily()     { showTab(tabDaily);                        setActiveButton(btnDaily); }
    @FXML void onPrice()     { showTab(tabPrice);     refreshPrices();   setActiveButton(btnPrice); }
    @FXML void onAnalytics() { showTab(tabAnalytics); refreshCharts();   setActiveButton(btnAnalytics); }

    @FXML
    void onBack(ActionEvent event) {
        SwitchScreens.switchScene(event, "admin-view.fxml", "Solaris Cinema - Admin");
    }

    private void showTab(VBox active) {
        tabOverview.setVisible(false);  tabOverview.setManaged(false);
        tabPerMovie.setVisible(false);  tabPerMovie.setManaged(false);
        tabDaily.setVisible(false);     tabDaily.setManaged(false);
        tabPrice.setVisible(false);     tabPrice.setManaged(false);
        tabAnalytics.setVisible(false); tabAnalytics.setManaged(false);
        active.setVisible(true);
        active.setManaged(true);
    }

    private void refreshOverview() {
        Map<String, Integer> sales = model.calculateSalesPerMovie();
        int totalTickets = sales.values().stream().mapToInt(i -> i).sum();
        double totalRevenue = model.calculateRevenuePerMovie().values()
                .stream().mapToDouble(d -> d).sum();
        String topMovie = sales.isEmpty() ? "-" : sales.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");
        lblTotalTickets.setText(String.valueOf(totalTickets));
        lblTotalRevenue.setText(String.format("EGP %.0f", totalRevenue));
        lblTopMovie.setText(topMovie);
    }

    private void refreshPerMovie() {
        listSalesPerMovie.getItems().clear();
        listRevenuePerMovie.getItems().clear();
        model.calculateSalesPerMovie().forEach((movie, count) ->
                listSalesPerMovie.getItems().add(
                        String.format("%-28s %d tickets", movie, count)));
        model.calculateRevenuePerMovie().forEach((movie, revenue) ->
                listRevenuePerMovie.getItems().add(
                        String.format("%-28s EGP %.2f", movie, revenue)));
    }

    @FXML
    void onCalculateDaily() {
        LocalDate date = datePicker.getValue();
        if (date == null) { areaDaily.setText("Please select a date."); return; }
        areaDaily.setText(model.calculateDailySales(date).toString());
    }

    private void refreshPrices() {
        listPrices.getItems().clear();
        model.getTicketPricesPerMovie().forEach((movie, price) ->
                listPrices.getItems().add(
                        String.format("%-28s EGP %.0f", movie, price)));
    }

    private void loadComboMovies() {
        comboMovie.getItems().clear();
        comboMovie.getItems().addAll(model.getMoviesForCombo());
        if (!comboMovie.getItems().isEmpty())
            comboMovie.getSelectionModel().selectFirst();
    }

    @FXML
    void onUpdatePrice() {
        String movie = comboMovie.getValue();
        if (movie == null) { lblPriceResult.setText("Select a movie."); return; }
        try {
            double price = Double.parseDouble(fieldPrice.getText().trim());
            if (price < 0) throw new NumberFormatException();
            int updated = model.setTicketPriceForMovie(movie, price);
            lblPriceResult.setText("Updated " + updated + " shows for " + movie);
            refreshPrices();
        } catch (NumberFormatException e) {
            lblPriceResult.setText("Enter a valid positive number.");
        }
    }

    private void refreshCharts() {
        //PieChart
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        model.calculateSalesPerMovie().forEach((movie, count) ->
                pieData.add(new PieChart.Data(movie + " (" + count + ")", count)));
        pieChart.setData(pieData);
        pieChart.setLegendVisible(true);
        pieChart.setLabelsVisible(false);
        pieChart.setLegendSide(javafx.geometry.Side.BOTTOM);
        pieChart.setMinHeight(300);
        pieChart.setPadding(new javafx.geometry.Insets(10, 10, 20, 10));
        String base64Css = "data:text/css;base64,LmNoYXJ0LWxlZ2VuZC1pdGVtey1meC10ZXh0LWZpbGw6d2hpdGU7LWZ4LWZvbnQtc2l6ZToxMXB4O30gLmNoYXJ0LWxlZ2VuZHstZngtYmFja2dyb3VuZC1jb2xvcjp0cmFuc3BhcmVudDt9";
        pieChart.getStylesheets().clear();
        pieChart.getStylesheets().add(base64Css);

        // BarChart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue (EGP)");
        model.getDailyRevenueSummary(LocalDate.now().minusDays(6), LocalDate.now())
                .entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .forEach(e -> series.getData().add(new XYChart.Data<>(
                        e.getKey().format(java.time.format.DateTimeFormatter.ofPattern("MM/dd")),
                        e.getValue())));
        barChart.getData().clear();
        barChart.getData().add(series);
        barChart.getXAxis().setTickLabelRotation(-30);
        barChart.getData().clear();
        barChart.getData().add(series);
        barChart.getXAxis().setTickLabelRotation(0);
        barChart.getXAxis().setStyle("-fx-tick-label-font-size: 13px; -fx-tick-label-fill: #adcfef;");
        ((CategoryAxis) barChart.getXAxis()).setCategories(
                FXCollections.observableArrayList(
                        series.getData().stream()
                                .map(XYChart.Data::getXValue)
                                .collect(java.util.stream.Collectors.toList())
                )
        );
    }
}