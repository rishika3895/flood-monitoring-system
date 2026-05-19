CREATE DATABASE IF NOT EXISTS rish_monitoring;
USE rish_monitoring;

CREATE TABLE monitoring_stations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    station_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE water_level_readings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    station_id VARCHAR(50) NOT NULL,
    water_level DOUBLE NOT NULL,
    rainfall DOUBLE,
    timestamp TIMESTAMP NOT NULL,
    alert_level VARCHAR(20),
    INDEX idx_station_time (station_id, timestamp),
    FOREIGN KEY (station_id) REFERENCES monitoring_stations(station_id)
);

CREATE TABLE alerts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    station_id VARCHAR(50) NOT NULL,
    alert_level VARCHAR(20) NOT NULL,
    water_level DOUBLE NOT NULL,
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_station_level (station_id, alert_level),
    FOREIGN KEY (station_id) REFERENCES monitoring_stations(station_id)
);

INSERT INTO monitoring_stations (station_id, name, latitude, longitude) VALUES
('STATION_001', 'Rotterdam Harbor', 51.9225, 4.47917),
('STATION_002', 'Amsterdam Canal', 52.3676, 4.9041),
('STATION_003', 'Maastricht River', 50.8514, 5.6909);
