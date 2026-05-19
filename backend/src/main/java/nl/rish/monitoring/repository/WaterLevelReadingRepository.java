package nl.rish.monitoring.repository;

import nl.rish.monitoring.entity.WaterLevelReadingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterLevelReadingRepository extends JpaRepository<WaterLevelReadingEntity, Long> {
}
