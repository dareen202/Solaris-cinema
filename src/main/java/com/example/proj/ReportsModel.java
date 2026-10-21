package com.example.proj;

import com.example.proj.utils.DBHandle;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportsModel {
    public Map<String, Integer> calculateSalesPerMovie() {
        Map<String, Integer> salesMap = new LinkedHashMap<>();
        String sql = "SELECT m.title, COUNT(b.booking_id) AS ticket_count " +
                "FROM Bookings b " +
                "JOIN Shows s ON b.show_id = s.show_id " +
                "JOIN Movies m ON s.movie_id = m.movie_id " +
                "WHERE b.status = 'BOOKED' "+
                "GROUP BY m.title " +
                "ORDER BY ticket_count DESC";
        try (Connection conn = DBHandle.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                salesMap.put(rs.getString("title"), rs.getInt("ticket_count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return salesMap;
    }

    public Map<String, Double> calculateRevenuePerMovie() {
        Map<String, Double> revenueMap = new LinkedHashMap<>();
        String sql = "SELECT m.title, SUM(s.ticket_price) AS total_revenue " +
                "FROM Bookings b " +
                "JOIN Shows s ON b.show_id = s.show_id " +
                "JOIN Movies m ON s.movie_id = m.movie_id " +
                "WHERE b.status = 'BOOKED' " +
                "GROUP BY m.title " +
                "ORDER BY total_revenue DESC";
        try (Connection conn = DBHandle.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                revenueMap.put(rs.getString("title"), rs.getDouble("total_revenue"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return revenueMap;
    }

    public DailySalesResult calculateDailySales(LocalDate date) {
        int totalTickets = 0;
        double totalRevenue = 0;
        Map<String, Integer> ticketsPerMovie = new LinkedHashMap<>();
        Map<String, Double> revenuePerMovie = new LinkedHashMap<>();

        String sql = "SELECT m.title, COUNT(b.booking_id) AS cnt, SUM(s.ticket_price) AS rev " +
                "FROM Bookings b " +
                "JOIN Shows s ON b.show_id = s.show_id " +
                "JOIN Movies m ON s.movie_id = m.movie_id " +
                "WHERE b.status = 'BOOKED' AND s.show_date = ? " +
                "GROUP BY m.title";
        try (Connection conn = DBHandle.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String title = rs.getString("title");
                int cnt = rs.getInt("cnt");
                double rev = rs.getDouble("rev");
                ticketsPerMovie.put(title, cnt);
                revenuePerMovie.put(title, rev);
                totalTickets += cnt;
                totalRevenue += rev;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new DailySalesResult(date, totalTickets, totalRevenue, ticketsPerMovie, revenuePerMovie);
    }


    public List<String> getMoviesForCombo() {
        List<String> movies = new ArrayList<>();
        String sql = "SELECT DISTINCT title FROM Movies ORDER BY title";
        try (Connection conn = DBHandle.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                movies.add(rs.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }

    public Map<String, Double> getTicketPricesPerMovie() {
        Map<String, Double> prices = new LinkedHashMap<>();
        String sql = "SELECT DISTINCT m.title, s.ticket_price " +
                "FROM Shows s JOIN Movies m ON s.movie_id = m.movie_id " +
                "ORDER BY m.title";
        try (Connection conn = DBHandle.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                prices.putIfAbsent(rs.getString("title"), rs.getDouble("ticket_price"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return prices;
    }

    public int setTicketPriceForMovie(String movieTitle, double newPrice) {
        String sql = "UPDATE Shows SET ticket_price = ? " +
                "WHERE show_id IN (SELECT show_id FROM Shows s " +
                "JOIN Movies m ON s.movie_id = m.movie_id WHERE m.title = ?)";
        try (Connection conn = DBHandle.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newPrice);
            ps.setString(2, movieTitle);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Map<LocalDate, Double> getDailyRevenueSummary(LocalDate from, LocalDate to) {
        Map<LocalDate, Double> summary = new TreeMap<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            summary.put(cursor, 0.0);
            cursor = cursor.plusDays(1);
        }
        String sql = "SELECT s.show_date, SUM(s.ticket_price) AS rev " +
                "FROM Bookings b " +
                "JOIN Shows s ON b.show_id = s.show_id " +
                "WHERE b.status = 'BOOKED' " +
                "  AND s.show_date BETWEEN ? AND ? " +
                "GROUP BY s.show_date";
        try (Connection conn = DBHandle.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(from));
            ps.setDate(2, java.sql.Date.valueOf(to));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                summary.put(rs.getDate("show_date").toLocalDate(), rs.getDouble("rev"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summary;
    }


    public static class DailySalesResult {
        public final LocalDate date;
        public final int totalTickets;
        public final double totalRevenue;
        public final Map<String, Integer> ticketsPerMovie;
        public final Map<String, Double> revenuePerMovie;

        public DailySalesResult(LocalDate date, int totalTickets, double totalRevenue,
                                Map<String, Integer> ticketsPerMovie,
                                Map<String, Double> revenuePerMovie) {
            this.date = date;
            this.totalTickets = totalTickets;
            this.totalRevenue = totalRevenue;
            this.ticketsPerMovie = ticketsPerMovie;
            this.revenuePerMovie = revenuePerMovie;
        }

        @Override
        public String toString() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            StringBuilder sb = new StringBuilder();
            sb.append("═══════════════════════════════════\n");
            sb.append("  Daily Sales Report — ").append(date.format(fmt)).append("\n");
            sb.append("═══════════════════════════════════\n");
            sb.append(String.format("  Total Tickets : %d%n", totalTickets));
            sb.append(String.format("  Total Revenue : EGP %.2f%n", totalRevenue));
            sb.append("───────────────────────────────────\n");
            ticketsPerMovie.forEach((movie, count) ->
                    sb.append(String.format("  %-22s %3d tickets%n", movie, count)));
            sb.append("═══════════════════════════════════\n");
            return sb.toString();
        }
    }
}