package org.example;
import javax.swing.*;


public class WeatherApp {

    public WeatherApp() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Weather App");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new WeatherPanel());
            frame.pack();
            frame.setVisible(true);
        });
    }
}
