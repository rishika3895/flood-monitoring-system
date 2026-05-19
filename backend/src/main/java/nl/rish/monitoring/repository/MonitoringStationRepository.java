package nl.rish.monitoring.repository;

import nl.rish.monitoring.entity.MonitoringStation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoringStationRepository extends JpaRepository<MonitoringStation, Long> {
}
