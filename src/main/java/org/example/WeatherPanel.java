/*
 * Created by JFormDesigner on Mon May 20 17:20:15 CEST 2024
 */

package org.example;

import java.awt.*;
import javax.swing.*;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;
import java.util.TimeZone;
import javax.swing.border.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * @author hania
 */
public class WeatherPanel extends JPanel {

    private static final String API_KEY = "a36c4942dc1564603b3dfbe5ccd2205d";
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather?q=%s&units=metric&appid=%s";
    private final Image backgroundImage;
    private static final String DB_URL = "jdbc:sqlite:weather_info.db";


    public WeatherPanel() {
        backgroundImage = new ImageIcon(Objects.requireNonNull(getClass().getResource("/new3.png"))).getImage();

        initComponents();
        comboBoxDBFilter.addItem("Temp > 25");
        comboBoxDBFilter.addItem("City: Warsaw");
        comboBoxDBFilter.addItem("Sort by newest");
        comboBoxDBFilter.addItem("Sort alphabetically");
        comboBoxDBFilter.addItem("Sort desc by temperature");

        // displaying current weather and adding data to database
        buttonweather.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String city = textFieldcity.getText();
                WeatherInfo.Root weatherInfo = GetWeather(city);
                displayWeather(weatherInfo);
                saveToDatabase(weatherInfo);
                displayDatabaseContent();
            }
        });

        // plot generation - comparison of temp and feels like temp during given day
        buttonPlot.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String city = textFieldPlotCity.getText();
                generatePlot(city);
            }
        });

        // display filtered database (getting city from user)
        buttonFilter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String city = textFieldDBFilter.getText();
                displayDatabaseFiltered(city);
            }
        });

        // displaying filtered database (choosing from a combobox)
        comboBoxDBFilter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                var x = comboBoxDBFilter.getItemAt(comboBoxDBFilter.getSelectedIndex());
                if(Objects.equals(x, "Temp > 25")){
                    displayDatabaseT25();
                }
                else if(Objects.equals(x, "City: Warsaw")){
                    displayDatabaseFiltered("Warsaw");
                }
                else if(Objects.equals(x, "Sort alphabetically")){
                    displayDatabaseAlphabetically();
                }
                else if(Objects.equals(x, "Sort desc by temperature")){
                    displayDatabaseTempReverse();
                }
                else if(Objects.equals(x, "Sort by newest")){
                    displayDatabaseNewest();
                }
            }
        });

        // displaying whole database (useful after filtering)
        buttonRestoreDB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                displayDatabaseContent();
            }
        });
    }

    // PLOT GENERATION - change of temp and feels like temp during latest measures

    private void generatePlot(String city) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        LinkedList<String[]> dataPoints = new LinkedList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM Weather WHERE name = ?";
            try (PreparedStatement p = conn.prepareStatement(sql)) {
                p.setString(1, city);
                ResultSet rs = p.executeQuery();
                while (rs.next()) {
                    long timestamp = rs.getLong("date") * 1000;
                    Date date = new Date(timestamp);
                    String hour = new SimpleDateFormat("HH").format(date);
                    String min = new SimpleDateFormat("mm").format(date);
                    double temp = rs.getDouble("temp");
                    double feels_like = rs.getDouble("feels_like");

                    String[] dataPoint = {date + " " + hour + ":" + min, Double.toString(temp), Double.toString(feels_like)};
                    dataPoints.add(dataPoint);

                    // 5 latest measures
                    if (dataPoints.size() > 6) {
                        dataPoints.removeFirst();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (String[] dataPoint : dataPoints) {
            dataset.addValue(Double.parseDouble(dataPoint[1]), "temperature", dataPoint[0]);
            dataset.addValue(Double.parseDouble(dataPoint[2]), "feels like", dataPoint[0]);
        }

        JFreeChart lineChart = ChartFactory.createLineChart(
                "temperature vs feels like",
                "date",
                "temperatures",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        ChartPanel chartPanel = new ChartPanel(lineChart);
        JFrame plotFrame = new JFrame();
        plotFrame.add(chartPanel);
        plotFrame.pack();
        plotFrame.setVisible(true);
    }

    // ADDING CURRENT WEATHER INTO DATABASE

    public void saveToDatabase(WeatherInfo.Root weatherInfo) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "INSERT INTO Weather(name, date, temp, feels_like) VALUES(?, ?, ?, ?)";
            try (PreparedStatement p = conn.prepareStatement(sql)) {
                p.setString(1, weatherInfo.getName());
                p.setLong(2, weatherInfo.getDt());
                p.setDouble(3, weatherInfo.getMain().getTemp());
                p.setDouble(4, weatherInfo.getMain().getFeels_like());
                p.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // DISPLAYING WHOLE DATABASE

    void displayDatabaseContent() {
        textAreaDB.setText("");
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM Weather";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    textAreaDB.append("city: " + (rs.getString("name") + " | "));
                    textAreaDB.append("date: " + (formatDate(rs.getLong("date"))) + " | ");
                    textAreaDB.append("temperature: " + (rs.getDouble("temp") + "°C | "));
                    textAreaDB.append("feels Like: " + (rs.getDouble("feels_like")) + ("°C"));
                    textAreaDB.append("\n");
                    }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DISPLAYING WHOLE DATABASE ALPHABETICALLY

    private void displayDatabaseAlphabetically() {
        textAreaDB.setText("");
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM Weather ORDER BY name";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    textAreaDB.append("city: " + (rs.getString("name") + " | "));
                    textAreaDB.append("date: " + (formatDate(rs.getLong("date"))) + " | ");
                    textAreaDB.append("temperature: " + (rs.getDouble("temp") + "°C | "));
                    textAreaDB.append("feels Like: " + (rs.getDouble("feels_like")) + ("°C"));
                    textAreaDB.append("\n");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DISPLAYING WHOLE DATABASE FROM NEWEST

    private void displayDatabaseNewest() {
        textAreaDB.setText("");
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM Weather ORDER BY date DESC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    textAreaDB.append("city: " + (rs.getString("name") + " | "));
                    textAreaDB.append("date: " + (formatDate(rs.getLong("date"))) + " | ");
                    textAreaDB.append("temperature: " + (rs.getDouble("temp") + "°C | "));
                    textAreaDB.append("feels Like: " + (rs.getDouble("feels_like")) + ("°C"));
                    textAreaDB.append("\n");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DISPLAYING WHOLE DATABASE ORDERED BY TEMPERATURE (DESCENDING)

    private void displayDatabaseTempReverse() {
        textAreaDB.setText("");
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM Weather ORDER BY temp DESC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    textAreaDB.append("city: " + (rs.getString("name") + " | "));
                    textAreaDB.append("date: " + (formatDate(rs.getLong("date"))) + " | ");
                    textAreaDB.append("temperature: " + (rs.getDouble("temp") + "°C | "));
                    textAreaDB.append("feels Like: " + (rs.getDouble("feels_like")) + ("°C"));
                    textAreaDB.append("\n");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DISPLAYING FILTERED DATABASE - FILTERING CRITERIUM IS CITY NAME FROM USER

    private void displayDatabaseFiltered(String city) {
        textAreaDB.setText("");
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM Weather WHERE name = ?";
            try (PreparedStatement p = conn.prepareStatement(sql)) {
                p.setString(1, city);
                ResultSet rs = p.executeQuery();
                while (rs.next()) {
                    textAreaDB.append("city: " + (rs.getString("name") + " | "));
                    textAreaDB.append("date: " + (formatDate(rs.getLong("date"))) + " | ");
                    textAreaDB.append("temperature: " + (rs.getDouble("temp") + "°C | "));
                    textAreaDB.append("feels Like: " + (rs.getDouble("feels_like")) + ("°C"));
                    textAreaDB.append("\n");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DISPLAYING FILTERED DATABASE - FILTERING CRITERIUM TEMP > 25

    private void displayDatabaseT25() {
        textAreaDB.setText("");
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM Weather WHERE temp > 25";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    textAreaDB.append("city: " + (rs.getString("name") + " | "));
                    textAreaDB.append("date: " + (formatDate(rs.getLong("date"))) + " | ");
                    textAreaDB.append("temperature: " + (rs.getDouble("temp") + "°C | "));
                    textAreaDB.append("feels Like: " + (rs.getDouble("feels_like")) + ("°C"));
                    textAreaDB.append("\n");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DATA FORMATTING

    private String formatDate(long timestamp) {
        long milliseconds = timestamp * 1000;
        long adjustedTimestamp = milliseconds + TimeZone.getDefault().getRawOffset();
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(adjustedTimestamp));
    }

    // USING CUSTOM IMAGE AS APP BACKGROUND

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // background image
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    // GETTING WEATHER FROM API

    WeatherInfo.Root GetWeather(String city) {
        WeatherInfo.Root info = new WeatherInfo.Root();
        try {
            String urlString = String.format(API_URL, city, API_KEY);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            // 200 = response is good
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                JsonReader jsonReader = Json.createReader(in);
                JsonObject jsonResponse = jsonReader.readObject();
                jsonReader.close();
                in.close();

                JsonArray weatherArray = jsonResponse.getJsonArray("weather");
                ArrayList<WeatherInfo.Weather> weatherList = new ArrayList<>();
                for (int i = 0; i < weatherArray.size(); i++) {
                    JsonObject weatherJson = weatherArray.getJsonObject(i);
                    WeatherInfo.Weather weather = new WeatherInfo.Weather();
                    weather.setMain(weatherJson.getString("main"));
                    weather.setDescription(weatherJson.getString("description"));
                    weather.setIcon(weatherJson.getString("icon"));
                    weatherList.add(weather);
                }
                info.setWeather(weatherList);

                WeatherInfo.Main main = new WeatherInfo.Main();
                JsonObject mainJson = jsonResponse.getJsonObject("main");
                main.setTemp(mainJson.getJsonNumber("temp").doubleValue());
                main.setFeels_like(mainJson.getJsonNumber("feels_like").doubleValue());
                main.setTemp_min(mainJson.getJsonNumber("temp_min").doubleValue());
                main.setTemp_max(mainJson.getJsonNumber("temp_max").doubleValue());
                main.setPressure(mainJson.getInt("pressure"));
                main.setHumidity(mainJson.getInt("humidity"));
                info.setMain(main);

                WeatherInfo.Wind wind = new WeatherInfo.Wind();
                JsonObject windJson = jsonResponse.getJsonObject("wind");
                wind.setSpeed(windJson.getJsonNumber("speed").doubleValue());
                info.setWind(wind);

                WeatherInfo.Sys sys = new WeatherInfo.Sys();
                JsonObject sysJson = jsonResponse.getJsonObject("sys");
                sys.setSunrise(sysJson.getJsonNumber("sunrise").longValue());
                sys.setSunset(sysJson.getJsonNumber("sunset").longValue());
                info.setSys(sys);

                WeatherInfo.Root root = new WeatherInfo.Root();
                JsonObject rootJson = jsonResponse.getJsonObject("root");

                info.setName(jsonResponse.getString("name"));
                info.setDt(jsonResponse.getJsonNumber("dt").longValue());

            } else {
                System.out.println("failed to get weather data");
                textArearesults.setText("");
                textArearesults.append("an error occurred,\ntry with different city or check\nyour internet connection");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("error occurred while getting weather data");
        }
        return info;
    }

    private void displayWeather(WeatherInfo.Root weatherInfo) {
        textArearesults.setText("");
        if (weatherInfo != null) {
            textArearesults.append(String.format("temperature: %.2f °C\n", weatherInfo.getMain().getTemp()));
            textArearesults.append(String.format("feels like: %.2f °C\n", weatherInfo.getMain().getFeels_like()));
            if (checkBoxhum.isSelected()) {
                textArearesults.append(String.format("humidity: %d%%\n", weatherInfo.getMain().getHumidity()));
            }
            if (checkBoxpressure.isSelected()) {
                textArearesults.append(String.format("pressure: %d hPa\n", weatherInfo.getMain().getPressure()));
            }
            if (checkBoxmin.isSelected()) {
                textArearesults.append(String.format("min Temperature: %.2f °C\n", weatherInfo.getMain().getTemp_min()));
            }
            if (checkBoxmax.isSelected()) {
                textArearesults.append(String.format("max temperature: %.2f °C\n", weatherInfo.getMain().getTemp_max()));
            }
            if (checkBoxsunrise.isSelected()) {
                textArearesults.append(String.format("sunrise: " + weatherInfo.getSys().getFormattedSunrise()) + "\n");
            }
            if (checkBoxsunset.isSelected()) {
                textArearesults.append(String.format("sunset: " + weatherInfo.getSys().getFormattedSunset()) + "\n");
            }
        } else {
            textArearesults.append("failed to get weather data :(");
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner Evaluation license - Hanna Grzebieluch
        labelcity = new JLabel();
        textAreaFilter = new JTextArea();
        textFieldcity = new JTextField();
        scrollPane2 = new JScrollPane();
        textAreaDB = new JTextArea();
        comboBoxDBFilter = new JComboBox<>();
        textFieldDBFilter = new JTextField();
        checkBoxhum = new JCheckBox();
        buttonRestoreDB = new JButton();
        buttonFilter = new JButton();
        checkBoxpressure = new JCheckBox();
        checkBoxmin = new JCheckBox();
        checkBoxmax = new JCheckBox();
        checkBoxsunrise = new JCheckBox();
        checkBoxsunset = new JCheckBox();
        buttonweather = new JButton();
        labelTitlePlot = new JLabel();
        labelCityPlot = new JLabel();
        scrollPane1 = new JScrollPane();
        textArearesults = new JTextArea();
        textFieldPlotCity = new JTextField();
        buttonPlot = new JButton();

        //======== this ========
        setPreferredSize(new Dimension(825, 500));
        setMinimumSize(new Dimension(300, 427));
        setBackground(new Color(0x00000000, true));
        setForeground(new Color(0x00000000, true));
        setBorder (new javax. swing. border. CompoundBorder( new javax .swing .border .TitledBorder (new javax. swing. border.
        EmptyBorder( 0, 0, 0, 0) , "JF\u006frm\u0044es\u0069gn\u0065r \u0045va\u006cua\u0074io\u006e", javax. swing. border. TitledBorder. CENTER, javax. swing
        . border. TitledBorder. BOTTOM, new java .awt .Font ("D\u0069al\u006fg" ,java .awt .Font .BOLD ,12 ),
        java. awt. Color. red) , getBorder( )) );  addPropertyChangeListener (new java. beans. PropertyChangeListener( )
        { @Override public void propertyChange (java .beans .PropertyChangeEvent e) {if ("\u0062or\u0064er" .equals (e .getPropertyName () ))
        throw new RuntimeException( ); }} );
        setLayout(new GridBagLayout());
        ((GridBagLayout)getLayout()).columnWidths = new int[] {29, 246, 20, 282, 158, 0};
        ((GridBagLayout)getLayout()).rowHeights = new int[] {29, 40, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 59, 0};
        ((GridBagLayout)getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
        ((GridBagLayout)getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

        //---- labelcity ----
        labelcity.setText("insert city name:");
        labelcity.setFont(labelcity.getFont().deriveFont(labelcity.getFont().getStyle() | Font.BOLD, labelcity.getFont().getSize() + 1f));
        labelcity.setBackground(new Color(0x3a3d4c));
        labelcity.setForeground(Color.white);
        labelcity.setVerticalAlignment(SwingConstants.TOP);
        add(labelcity, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- textAreaFilter ----
        textAreaFilter.setText("enter city name to get its historical data or choose filtering criteria from checkbox");
        textAreaFilter.setWrapStyleWord(true);
        textAreaFilter.setBackground(new Color(0x00000000, true));
        textAreaFilter.setForeground(Color.white);
        textAreaFilter.setFont(textAreaFilter.getFont().deriveFont(textAreaFilter.getFont().getStyle() | Font.BOLD, textAreaFilter.getFont().getSize() + 1f));
        textAreaFilter.setOpaque(false);
        textAreaFilter.setLineWrap(true);
        add(textAreaFilter, new GridBagConstraints(3, 1, 2, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 0), 0, 0));

        //---- textFieldcity ----
        textFieldcity.setPreferredSize(new Dimension(200, 26));
        textFieldcity.setBackground(Color.white);
        textFieldcity.setForeground(new Color(0xef729e));
        textFieldcity.setBorder(new BevelBorder(BevelBorder.LOWERED, Color.pink, Color.pink, Color.pink, Color.pink));
        textFieldcity.setOpaque(true);
        textFieldcity.setFont(textFieldcity.getFont().deriveFont(textFieldcity.getFont().getStyle() | Font.BOLD));
        add(textFieldcity, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //======== scrollPane2 ========
        {
            scrollPane2.setViewportView(textAreaDB);
        }
        add(scrollPane2, new GridBagConstraints(3, 4, 2, 4, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 0), 0, 0));

        //---- comboBoxDBFilter ----
        comboBoxDBFilter.setForeground(new Color(0xef729e));
        comboBoxDBFilter.setBackground(Color.white);
        add(comboBoxDBFilter, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- textFieldDBFilter ----
        textFieldDBFilter.setForeground(new Color(0xef729e));
        textFieldDBFilter.setBackground(Color.white);
        textFieldDBFilter.setBorder(new BevelBorder(BevelBorder.LOWERED, Color.pink, Color.pink, Color.pink, Color.pink));
        textFieldDBFilter.setFont(textFieldDBFilter.getFont().deriveFont(textFieldDBFilter.getFont().getStyle() | Font.BOLD));
        add(textFieldDBFilter, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 0), 0, 0));

        //---- checkBoxhum ----
        checkBoxhum.setText("humidity");
        checkBoxhum.setBackground(new Color(0x00000000, true));
        checkBoxhum.setForeground(Color.white);
        checkBoxhum.setRolloverEnabled(false);
        checkBoxhum.setContentAreaFilled(false);
        add(checkBoxhum, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- buttonRestoreDB ----
        buttonRestoreDB.setText("show full database");
        buttonRestoreDB.setBackground(new Color(0xef729e));
        buttonRestoreDB.setForeground(Color.white);
        buttonRestoreDB.setFont(buttonRestoreDB.getFont().deriveFont(buttonRestoreDB.getFont().getStyle() | Font.BOLD));
        buttonRestoreDB.setBorder(new BevelBorder(BevelBorder.LOWERED, Color.pink, Color.pink, Color.pink, Color.pink));
        add(buttonRestoreDB, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- buttonFilter ----
        buttonFilter.setText("filter database");
        buttonFilter.setBackground(new Color(0xef729e));
        buttonFilter.setBorder(new BevelBorder(BevelBorder.LOWERED, Color.pink, Color.pink, Color.pink, Color.pink));
        buttonFilter.setFont(buttonFilter.getFont().deriveFont(buttonFilter.getFont().getStyle() | Font.BOLD));
        buttonFilter.setForeground(Color.white);
        add(buttonFilter, new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 0), 0, 0));

        //---- checkBoxpressure ----
        checkBoxpressure.setText("pressure");
        checkBoxpressure.setBackground(new Color(0x00000000, true));
        checkBoxpressure.setForeground(Color.white);
        checkBoxpressure.setRolloverEnabled(false);
        checkBoxpressure.setContentAreaFilled(false);
        add(checkBoxpressure, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- checkBoxmin ----
        checkBoxmin.setText("min temperature");
        checkBoxmin.setBackground(new Color(0x00000000, true));
        checkBoxmin.setForeground(Color.white);
        checkBoxmin.setRolloverEnabled(false);
        checkBoxmin.setContentAreaFilled(false);
        add(checkBoxmin, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- checkBoxmax ----
        checkBoxmax.setText("max temperature");
        checkBoxmax.setForeground(Color.white);
        checkBoxmax.setBackground(new Color(0x00000000, true));
        checkBoxmax.setRolloverEnabled(false);
        checkBoxmax.setContentAreaFilled(false);
        add(checkBoxmax, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- checkBoxsunrise ----
        checkBoxsunrise.setText("sunrise");
        checkBoxsunrise.setBackground(new Color(0x00000000, true));
        checkBoxsunrise.setForeground(Color.white);
        checkBoxsunrise.setRolloverEnabled(false);
        checkBoxsunrise.setContentAreaFilled(false);
        add(checkBoxsunrise, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- checkBoxsunset ----
        checkBoxsunset.setText("sunset");
        checkBoxsunset.setBackground(new Color(0x00000000, true));
        checkBoxsunset.setForeground(Color.white);
        checkBoxsunset.setRolloverEnabled(false);
        checkBoxsunset.setContentAreaFilled(false);
        add(checkBoxsunset, new GridBagConstraints(1, 8, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- buttonweather ----
        buttonweather.setText("get weather");
        buttonweather.setPreferredSize(new Dimension(150, 26));
        buttonweather.setBackground(new Color(0xef729e));
        buttonweather.setForeground(Color.white);
        buttonweather.setBorder(new BevelBorder(BevelBorder.LOWERED, Color.pink, Color.pink, Color.pink, Color.pink));
        buttonweather.setFont(buttonweather.getFont().deriveFont(buttonweather.getFont().getSize() + 1f));
        add(buttonweather, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- labelTitlePlot ----
        labelTitlePlot.setText("insert data to generate your plot");
        labelTitlePlot.setFont(labelTitlePlot.getFont().deriveFont(labelTitlePlot.getFont().getStyle() | Font.BOLD, labelTitlePlot.getFont().getSize() + 1f));
        labelTitlePlot.setForeground(Color.white);
        add(labelTitlePlot, new GridBagConstraints(3, 9, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- labelCityPlot ----
        labelCityPlot.setText("insert city:");
        labelCityPlot.setForeground(Color.white);
        labelCityPlot.setFont(labelCityPlot.getFont().deriveFont(labelCityPlot.getFont().getSize() + 1f));
        add(labelCityPlot, new GridBagConstraints(3, 10, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //======== scrollPane1 ========
        {
            scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- textArearesults ----
            textArearesults.setPreferredSize(new Dimension(300, 20));
            textArearesults.setBackground(new Color(0x00000000, true));
            textArearesults.setForeground(new Color(0xef729e));
            textArearesults.setEditable(false);
            textArearesults.setOpaque(false);
            textArearesults.setCaretColor(new Color(0xff6666));
            textArearesults.setFont(textArearesults.getFont().deriveFont(textArearesults.getFont().getStyle() | Font.BOLD));
            scrollPane1.setViewportView(textArearesults);
        }
        add(scrollPane1, new GridBagConstraints(1, 10, 1, 7, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 5), 0, 0));

        //---- textFieldPlotCity ----
        textFieldPlotCity.setForeground(new Color(0xef729e));
        textFieldPlotCity.setBackground(Color.white);
        textFieldPlotCity.setBorder(new BevelBorder(BevelBorder.LOWERED, Color.pink, Color.pink, Color.pink, Color.pink));
        add(textFieldPlotCity, new GridBagConstraints(3, 11, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- buttonPlot ----
        buttonPlot.setText("generate plot");
        buttonPlot.setBackground(new Color(0xef729e));
        buttonPlot.setBorder(new BevelBorder(BevelBorder.LOWERED, Color.pink, Color.pink, Color.pink, Color.pink));
        buttonPlot.setFont(buttonPlot.getFont().deriveFont(buttonPlot.getFont().getSize() + 1f));
        buttonPlot.setForeground(Color.white);
        add(buttonPlot, new GridBagConstraints(3, 13, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on

    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner Evaluation license - Hanna Grzebieluch
    private JLabel labelcity;
    private JTextArea textAreaFilter;
    private JTextField textFieldcity;
    private JScrollPane scrollPane2;
    private JTextArea textAreaDB;
    private JComboBox<String> comboBoxDBFilter;
    private JTextField textFieldDBFilter;
    private JCheckBox checkBoxhum;
    private JButton buttonRestoreDB;
    private JButton buttonFilter;
    private JCheckBox checkBoxpressure;
    private JCheckBox checkBoxmin;
    private JCheckBox checkBoxmax;
    private JCheckBox checkBoxsunrise;
    private JCheckBox checkBoxsunset;
    private JButton buttonweather;
    private JLabel labelTitlePlot;
    private JLabel labelCityPlot;
    private JScrollPane scrollPane1;
    private JTextArea textArearesults;
    private JTextField textFieldPlotCity;
    private JButton buttonPlot;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
