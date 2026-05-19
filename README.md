# Flood Monitoring System

A real-time flood monitoring system built with Java 21 and Vue.js 3, designed to track water levels across multiple stations in the Netherlands. The system integrates with the OpenWeatherMap API to fetch live rainfall data and automatically converts it into water level readings. An alert engine continuously evaluates incoming data against configurable thresholds, triggering multi-level alerts (Yellow, Orange, Red) when water levels rise. Alerts and readings are pushed instantly to a live dashboard via WebSockets, where stations are visualized as color-coded markers on an interactive map. The system includes a built-in weather simulation engine with flood scenario triggers, and automatically falls back to simulation if the external API becomes unavailable.

## UML Diagram

![UML Diagram](Flood%20monitoring%20systemUML.jpeg)

## Architecture

- **Backend**: Java 21 (Spring Boot, Virtual Threads, WebSockets)
- **Frontend**: Vue.js 3 + TypeScript + Leaflet
- **Database**: MySQL
- **Real-time**: WebSocket communication
- **Data Source**: OpenWeatherMap API (with simulation fallback)

## Setup

### 1. Database
```bash
mysql -u root < schema.sql
```

### 2. Configure API Key
```bash
export OPENWEATHER_API_KEY=your_api_key_here
```

### 3. Start Backend
```bash
cd backend
./mvnw spring-boot:run
```

### 4. Start Frontend
```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173

## Alert Levels

| Level  | Water Level |
|--------|-------------|
| Green  | < 2.0m      |
| Yellow | ≥ 2.0m      |
| Orange | ≥ 2.5m      |
| Red    | ≥ 3.0m      |

## Admin Endpoints

```bash
# Trigger flood scenario
curl -X POST http://localhost:8080/api/admin/trigger-flood/STATION_001

# Reset station
curl -X POST http://localhost:8080/api/admin/reset/STATION_001
```
