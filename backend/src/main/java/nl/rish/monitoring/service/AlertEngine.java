package nl.rish.monitoring.service;

import nl.rish.monitoring.entity.Alert;
import nl.rish.monitoring.entity.WaterLevelReadingEntity;
import nl.rish.monitoring.model.AlertLevel;
import nl.rish.monitoring.model.WaterLevelReading;
import nl.rish.monitoring.repository.AlertRepository;
import nl.rish.monitoring.repository.WaterLevelReadingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;

@Service
public class AlertEngine {
    private final WaterLevelReadingRepository readingRepository;
    private final AlertRepository alertRepository;
    private final WebSocketService webSocketService;

    @Value("${monitoring.thresholds.yellow}")
    private double yellowThreshold;

    @Value("${monitoring.thresholds.orange}")
    private double orangeThreshold;

    @Value("${monitoring.thresholds.red}")
    private double redThreshold;

    // Java 21 Virtual Threads for concurrent processing
    private final java.util.concurrent.ExecutorService executor =
            Executors.newVirtualThreadPerTaskExecutor();

    public AlertEngine(WaterLevelReadingRepository readingRepository,
                       AlertRepository alertRepository,
                       WebSocketService webSocketService) {
        this.readingRepository = readingRepository;
        this.alertRepository = alertRepository;
        this.webSocketService = webSocketService;
    }

    public void processReading(WaterLevelReading reading) {
        executor.submit(() -> {
            try {
                AlertLevel level = determineAlertLevel(reading.getWaterLevel());
                reading.setAlertLevel(level);
                saveReading(reading);
                handleAlert(reading, level);
                webSocketService.broadcastReading(reading);
            } catch (Exception e) {
                System.err.println("Error processing reading for station " + reading.getStationId() + ": " + e.getMessage());
            }
        });
    }

    private AlertLevel determineAlertLevel(double waterLevel) {
        if (waterLevel >= redThreshold) return AlertLevel.RED;
        if (waterLevel >= orangeThreshold) return AlertLevel.ORANGE;
        if (waterLevel >= yellowThreshold) return AlertLevel.YELLOW;
        return AlertLevel.GREEN;
    }

    private void handleAlert(WaterLevelReading reading, AlertLevel level) {
        switch (level) {
            case YELLOW -> System.out.println("⚠️  YELLOW Alert - Station " + reading.getStationId() +
                    ": Water level " + reading.getWaterLevel() + "m");
            case ORANGE -> {
                System.out.println("🟠 ORANGE Alert - Station " + reading.getStationId() +
                        ": Water level " + reading.getWaterLevel() + "m");
                createAlert(reading, "Dashboard alert triggered");
            }
            case RED -> {
                System.err.println("🔴 RED Alert - Station " + reading.getStationId() +
                        ": Water level " + reading.getWaterLevel() + "m - CRITICAL!");
                createAlert(reading, "HIGH PRIORITY - Immediate action required");
            }
        }
    }

    private void saveReading(WaterLevelReading reading) {
        WaterLevelReadingEntity entity = new WaterLevelReadingEntity();
        entity.setStationId(reading.getStationId());
        entity.setWaterLevel(reading.getWaterLevel());
        entity.setRainfall(reading.getRainfall());
        entity.setTimestamp(reading.getTimestamp());
        entity.setAlertLevel(reading.getAlertLevel().name());
        readingRepository.save(entity);
    }

    private void createAlert(WaterLevelReading reading, String message) {
        Alert alert = new Alert();
        alert.setStationId(reading.getStationId());
        alert.setAlertLevel(reading.getAlertLevel().name());
        alert.setWaterLevel(reading.getWaterLevel());
        alert.setMessage(message);
        alert.setCreatedAt(LocalDateTime.now());
        alertRepository.save(alert);
    }
}
