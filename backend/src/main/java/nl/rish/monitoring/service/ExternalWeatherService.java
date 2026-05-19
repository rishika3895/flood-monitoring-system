package nl.rish.monitoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.rish.monitoring.external.OpenWeatherResponse;
import nl.rish.monitoring.model.WaterLevelReading;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExternalWeatherService {
    private final AlertEngine alertEngine;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${external-data.enabled:false}")
    private boolean enabled;

    @Value("${external-data.api-key:}")
    private String apiKey;

    @Value("${external-data.fallback-to-simulation:true}")
    private boolean fallbackToSimulation;

    private final Map<String, StationLocation> stationLocations = new HashMap<>();
    private final Map<String, Double> baseWaterLevels = new HashMap<>();

    private boolean apiAvailable = true;
    private int consecutiveFailures = 0;
    private static final int MAX_FAILURES = 3;

    public ExternalWeatherService(AlertEngine alertEngine, ObjectMapper objectMapper) {
        this.alertEngine = alertEngine;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();

        stationLocations.put("STATION_001", new StationLocation(51.9225, 4.47917, "Rotterdam"));
        stationLocations.put("STATION_002", new StationLocation(52.3676, 4.9041, "Amsterdam"));
        stationLocations.put("STATION_003", new StationLocation(50.8514, 5.6909, "Maastricht"));

        baseWaterLevels.put("STATION_001", 1.5);
        baseWaterLevels.put("STATION_002", 1.5);
        baseWaterLevels.put("STATION_003", 1.5);
    }

    @Scheduled(fixedDelayString = "${external-data.poll-interval-ms:60000}")
    public void fetchWeatherData() {
        if (!enabled) return;

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("⚠️  OpenWeatherMap API key not configured.");
            return;
        }

        stationLocations.forEach((stationId, location) -> {
            try {
                OpenWeatherResponse weather = fetchWeatherForLocation(location);
                WaterLevelReading reading = convertToWaterLevelReading(stationId, weather);
                alertEngine.processReading(reading);

                if (consecutiveFailures > 0) {
                    consecutiveFailures = 0;
                    apiAvailable = true;
                    System.out.println("✅ OpenWeatherMap API connection restored");
                }
            } catch (Exception e) {
                handleApiFailure(stationId, e);
            }
        });
    }

    private OpenWeatherResponse fetchWeatherForLocation(StationLocation location) throws Exception {
        String url = String.format(
                "https://api.openweathermap.org/data/2.5/weather?lat=%.4f&lon=%.4f&appid=%s&units=metric",
                location.latitude, location.longitude, apiKey);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API returned status: " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), OpenWeatherResponse.class);
    }

    private WaterLevelReading convertToWaterLevelReading(String stationId, OpenWeatherResponse weather) {
        double rainfall = weather.getRain() != null ? weather.getRain().getOneHour() : 0.0;
        double baseLevel = baseWaterLevels.get(stationId);
        double waterLevel = baseLevel;

        if (rainfall > 0) {
            double rainfallImpact = rainfall * 0.05;
            waterLevel = baseLevel + rainfallImpact;
            baseWaterLevels.put(stationId, Math.min(baseLevel + (rainfallImpact * 0.1), 3.5));
        } else {
            baseWaterLevels.put(stationId, Math.max(baseLevel - 0.02, 1.5));
        }

        return new WaterLevelReading(stationId, waterLevel, rainfall, LocalDateTime.now(), null);
    }

    private void handleApiFailure(String stationId, Exception e) {
        consecutiveFailures++;
        if (consecutiveFailures >= MAX_FAILURES && apiAvailable) {
            apiAvailable = false;
            System.err.println("❌ OpenWeatherMap API unavailable: " + e.getMessage());
            if (fallbackToSimulation) System.out.println("⚙️  Falling back to simulation mode");
        }
    }

    public boolean isApiAvailable() { return apiAvailable; }
    public boolean isEnabled() { return enabled; }

    public void resetStationLevel(String stationId) {
        baseWaterLevels.put(stationId, 1.5);
    }

    private static class StationLocation {
        final double latitude;
        final double longitude;
        final String name;

        StationLocation(double latitude, double longitude, String name) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.name = name;
        }
    }
}
