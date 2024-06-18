package org.example;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.sql.*;

public class WeatherPanelTest {

    @Test
    public void testNotNull() {
        WeatherPanel weatherPanel = new WeatherPanel();
        String city = "Wroclaw";
        WeatherInfo.Root actualWeatherInfo = weatherPanel.GetWeather(city);
        assertNotNull(actualWeatherInfo);
    }

    @Test
    public void testSavingToDB() throws SQLException {
        WeatherInfo.Root weatherInfo = new WeatherInfo.Root();
        WeatherInfo.Main main = new WeatherInfo.Main();
        main.setTemp(22.0);
        main.setFeels_like(18.0);
        weatherInfo.setMain(main);
        weatherInfo.setName("Czestochowa");
        weatherInfo.setDt(1716282033);

        WeatherPanel weatherPanel = new WeatherPanel();
        weatherPanel.saveToDatabase(weatherInfo);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:weather_info.db")) {
            String sql = "SELECT * FROM Weather WHERE name = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, "Czestochowa");
                ResultSet rs = statement.executeQuery();
                assertTrue(rs.next());
                assertEquals("Czestochowa", rs.getString("name"));
                assertEquals(1716282033, rs.getLong("date"));
                assertEquals(22.0, rs.getDouble("temp"));
                assertEquals(18.0, rs.getDouble("feels_like"));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

}