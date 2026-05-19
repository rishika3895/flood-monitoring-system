package nl.rish.monitoring.service;

import nl.rish.monitoring.model.WaterLevelReading;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WeatherSimulator {
    private final AlertEngine alertEngine;
    private final ExternalWeatherService externalWeatherService;

    @Value("${monitoring.simulation.stations}")
    private String stationsConfig;

    @Value("${external-data.enabled:false}")
    private boolean externalDataEnabled;

    @Value("${external-data.fallback-to-simulation:true}")
    private boolean fallbackEnabled;

    private List<String> stations;

    private final Map<String, Double> currentLevels = new ConcurrentHashMap<>();
    private final Map<String, Boolean> floodScenarios = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public WeatherSimulator(AlertEngine alertEngine, ExternalWeatherService externalWeatherService) {
        this.alertEngine = alertEngine;
        this.externalWeatherService = externalWeatherService;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        this.stations = List.of(stationsConfig.split(","));
    }

    @Scheduled(fixedDelayString = "${monitoring.simulation.interval-ms}")
    public void generateReadings() {
        boolean apiActive = externalDataEnabled && externalWeatherService.isApiAvailable();

        stations.forEach(stationId -> {
            boolean hasActiveFlood = Boolean.TRUE.equals(floodScenarios.get(stationId));
            boolean isDraining = currentLevels.getOrDefault(stationId, 1.5) > 1.5;

            boolean shouldSimulate = !apiActive || hasActiveFlood || isDraining;
            if (!shouldSimulate) return;

            double waterLevel = calculateWaterLevel(stationId);
            double rainfall = random.nextDouble() * 5.0;

            alertEngine.processReading(new WaterLevelReading(
                    stationId, waterLevel, rainfall, LocalDateTime.now(), null));
        });
    }

    private double calculateWaterLevel(String stationId) {
        double current = currentLevels.getOrDefault(stationId, 1.5 + random.nextDouble());

        if (Boolean.TRUE.equals(floodScenarios.get(stationId))) {
            current += 0.1;
            if (current > 3.5) floodScenarios.put(stationId, false);
        } else {
            if (current > 1.5) {
                current -= 0.02;
                current = Math.max(current, 1.5);
            } else {
                current += (random.nextDouble() - 0.5) * 0.05;
                current = Math.max(1.0, Math.min(current, 2.0));
            }
        }

        currentLevels.put(stationId, current);
        return current;
    }

    public void triggerFloodScenario(String stationId) {
        System.out.println("Triggering flood scenario for station: " + stationId);
        currentLevels.put(stationId, 1.5);
        floodScenarios.put(stationId, true);
    }

    public void resetStation(String stationId) {
        currentLevels.put(stationId, 1.5);
        floodScenarios.put(stationId, false);
        externalWeatherService.resetStationLevel(stationId);
    }
}
