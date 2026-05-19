export interface Station {
  id: number
  stationId: string
  name: string
  latitude: number
  longitude: number
}

export interface WaterLevelReading {
  stationId: string
  waterLevel: number
  rainfall: number
  timestamp: string
  alertLevel: 'GREEN' | 'YELLOW' | 'ORANGE' | 'RED'
}

export const ALERT_COLORS = {
  GREEN: '#4caf50',
  YELLOW: '#ffd54f',
  ORANGE: '#ff9800',
  RED: '#f44336'
}
