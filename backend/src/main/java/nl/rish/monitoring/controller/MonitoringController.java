package nl.rish.monitoring.controller;

import nl.rish.monitoring.entity.MonitoringStation;
import nl.rish.monitoring.repository.MonitoringStationRepository;
import nl.rish.monitoring.service.ExternalWeatherService;
import nl.rish.monitoring.service.WeatherSimulator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MonitoringController {
    private final MonitoringStationRepository stationRepository;
    private final WeatherSimulator weatherSimulator;
    private final ExternalWeatherService externalWeatherService;

    public MonitoringController(MonitoringStationRepository stationRepository,
                                WeatherSimulator weatherSimulator,
                                ExternalWeatherService externalWeatherService) {
        this.stationRepository = stationRepository;
        this.weatherSimulator = weatherSimulator;
        this.externalWeatherService = externalWeatherService;
    }

    @GetMapping("/stations")
    public List<MonitoringStation> getStations() {
        return stationRepository.findAll();
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("externalDataEnabled", externalWeatherService.isEnabled());
        status.put("apiAvailable", externalWeatherService.isApiAvailable());
        status.put("dataSource", externalWeatherService.isEnabled() && externalWeatherService.isApiAvailable()
                ? "OpenWeatherMap API" : "Simulation");
        return ResponseEntity.ok(status);
    }

    @PostMapping("/admin/trigger-flood/{stationId}")
    public ResponseEntity<Map<String, String>> triggerFlood(@PathVariable String stationId) {
        weatherSimulator.triggerFloodScenario(stationId);
        return ResponseEntity.ok(Map.of("message", "Flood scenario triggered for " + stationId));
    }

    @PostMapping("/admin/reset/{stationId}")
    public ResponseEntity<Map<String, String>> resetStation(@PathVariable String stationId) {
        weatherSimulator.resetStation(stationId);
        return ResponseEntity.ok(Map.of("message", "Station " + stationId + " reset"));
    }
}
