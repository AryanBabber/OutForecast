package com.example.outforecast;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Controller {
    private static final String API_KEY = "06a34bcb75cc41cd66533ecc14ae40f5";
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather";

    @FXML
    private TextField cityInput;
    @FXML
    private Label temperatureLabel;
    @FXML
    private Label conditionLabel;
    @FXML
    private Label humidityLabel;
    @FXML
    private Label windLabel;
    @FXML
    private Label locationLabel;
    @FXML
    private ImageView weatherIcon;
    @FXML
    private VBox weatherInfoBox;
    @FXML
    private Label statusLabel;
    @FXML
    private ToggleButton unitToggle;

    private final Map<String, Image> iconCache = new HashMap<>();
    private boolean isCelsius = true;
    private double currentTempCelsius = 0;

    @FXML
    public void initialize() {
        cityInput.setOnKeyPressed(this::handleEnterKeyPressed);
        unitToggle.setSelected(true);
        unitToggle.setText("°C");
    }

    private void handleEnterKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleSearch();
        }
    }

    @FXML
    private void handleSearch() {
        String city = cityInput.getText();
        if (city == null || city.trim().isEmpty()) {
            statusLabel.setText("Please enter a city name");
            weatherInfoBox.setVisible(false);
            return;
        }

        statusLabel.setText("Fetching weather data...");
        fetchWeatherData(city.trim());
    }

    @FXML
    private void handleUnitToggle() {
        isCelsius = unitToggle.isSelected();
        unitToggle.setText(isCelsius ? "°C" : "°F");

        if (weatherInfoBox.isVisible()) {
            updateTemperatureDisplay();
        }
    }

    private void updateTemperatureDisplay() {
        if (isCelsius) {
            temperatureLabel.setText(String.format("%.1f°C", currentTempCelsius));
        } else {
            double tempFahrenheit = (currentTempCelsius * 9/5) + 32;
            temperatureLabel.setText(String.format("%.1f°F", tempFahrenheit));
        }
    }

    private void fetchWeatherData(String city) {
        new Thread(() -> {
            try {
                String urlString = API_URL + "?q=" + city + "&appid=" + API_KEY + "&units=metric";
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    updateWeatherUI(response.toString());
                } else {
                    updateStatus("City not found. Please try again.", false);
                }
                connection.disconnect();
            } catch (Exception e) {
                updateStatus("Error: " + e.getMessage(), false);
            }
        }).start();
    }

    private void updateWeatherUI(String jsonData) {
        try {
            JSONObject json = new JSONObject(jsonData);
            JSONObject main = json.getJSONObject("main");
            JSONObject weather = json.getJSONArray("weather").getJSONObject(0);
            JSONObject wind = json.getJSONObject("wind");
            JSONObject sys = json.getJSONObject("sys");

            currentTempCelsius = main.getDouble("temp");
            int humidity = main.getInt("humidity");
            double windSpeed = wind.getDouble("speed");
            String cityName = json.getString("name");
            String country = sys.getString("country");
            String description = weather.getString("description");
            String iconCode = weather.getString("icon");

            javafx.application.Platform.runLater(() -> {
                locationLabel.setText(cityName + ", " + country);
                updateTemperatureDisplay();
                conditionLabel.setText(description);
                humidityLabel.setText("Humidity: " + humidity + "%");
                windLabel.setText("Wind: " + String.format("%.1f m/s", windSpeed));
                loadWeatherIcon(iconCode);
                weatherInfoBox.setVisible(true);
                statusLabel.setText("Updated: " + java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            });

        } catch (Exception e) {
            updateStatus("Error parsing weather data", false);
        }
    }

    private void loadWeatherIcon(String iconCode) {
        if (iconCache.containsKey(iconCode)) {
            weatherIcon.setImage(iconCache.get(iconCode));
            return;
        }

        String iconUrl = "http://openweathermap.org/img/wn/" + iconCode + "@2x.png";
        try {
            Image icon = new Image(iconUrl, true);
            iconCache.put(iconCode, icon);
            weatherIcon.setImage(icon);
        } catch (Exception e) {
            System.err.println("Failed to load weather icon: " + e.getMessage());
        }
    }

    private void updateStatus(String message, boolean showWeather) {
        javafx.application.Platform.runLater(() -> {
            statusLabel.setText(message);
            weatherInfoBox.setVisible(showWeather);
        });
    }
}