# Flood Monitoring System

Real-time water level monitoring with alerting and visualization using OpenWeatherMap API.

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
