package nl.rish.monitoring.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class WaterLevelReading {
    private String stationId;
    private double waterLevel;
    private double rainfall;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private AlertLevel alertLevel;

    public WaterLevelReading() {}

    public WaterLevelReading(String stationId, double waterLevel, double rainfall,
                             LocalDateTime timestamp, AlertLevel alertLevel) {
        this.stationId = stationId;
        this.waterLevel = waterLevel;
        this.rainfall = rainfall;
        this.timestamp = timestamp;
        this.alertLevel = alertLevel;
    }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public double getWaterLevel() { return waterLevel; }
    public void setWaterLevel(double waterLevel) { this.waterLevel = waterLevel; }

    public double getRainfall() { return rainfall; }
    public void setRainfall(double rainfall) { this.rainfall = rainfall; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public AlertLevel getAlertLevel() { return alertLevel; }
    public void setAlertLevel(AlertLevel alertLevel) { this.alertLevel = alertLevel; }
}
