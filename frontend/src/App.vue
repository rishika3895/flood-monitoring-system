<template>
  <div class="header">
    <h1>🌊 Flood Monitoring System</h1>
    <p>Real-time water level monitoring with intelligent alerting</p>
    <div class="data-source-badge" :class="dataSourceClass">
      {{ dataSourceIcon }} {{ dataSource }}
    </div>
  </div>
  
  <div class="main-content">
    <div class="map-container">
      <div id="map"></div>
    </div>
    
    <div class="sidebar">
      <div class="alerts">
        <div class="alerts-header">
          <h3>⚠️ Recent Alerts</h3>
          <button v-if="recentAlerts.length > 0" class="clear-btn" @click="clearAlerts">Clear</button>
        </div>
        <div 
          v-for="(alert, index) in recentAlerts" 
          :key="index"
          :class="['alert-item', alert.alertLevel.toLowerCase()]"
        >
          <div class="alert-station">{{ getStationName(alert.stationId) }}</div>
          <div class="alert-level">{{ alert.alertLevel }} Alert</div>
          <div class="alert-value">{{ alert.waterLevel.toFixed(2) }}m</div>
        </div>
        <div v-if="recentAlerts.length === 0" style="opacity: 0.6; font-size: 0.9rem;">
          No alerts yet - All stations normal
        </div>
      </div>
      
      <div class="status">
        <div class="status-row">
          <span :class="['status-indicator', wsConnected ? 'connected' : 'disconnected']"></span>
          {{ wsConnected ? 'Connected' : 'Disconnected' }}
        </div>
        <div class="status-row" v-if="lastUpdate">
          <span style="opacity: 0.7; font-size: 0.85rem;">
            Updated {{ lastUpdate }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import L from 'leaflet'
import axios from 'axios'
import type { Station, WaterLevelReading } from './types'
import { ALERT_COLORS } from './types'

const API_URL = 'http://localhost:8080/api'
const WS_URL = 'ws://localhost:8080/ws/monitoring'

const stations = ref<Station[]>([])
const recentAlerts = ref<WaterLevelReading[]>([])
const wsConnected = ref(false)
const dataSource = ref<string>('Loading...')
const lastUpdate = ref<string>('')

let map: L.Map
let ws: WebSocket
const markers = new Map<string, L.CircleMarker>()
let updateInterval: number

const dataSourceClass = computed(() => {
  if (dataSource.value.includes('API')) return 'live-data'
  if (dataSource.value.includes('Simulation')) return 'simulation-data'
  return ''
})

const dataSourceIcon = computed(() => {
  if (dataSource.value.includes('API')) return '🌐'
  if (dataSource.value.includes('Simulation')) return '⚙️'
  return '⏳'
})

onMounted(async () => {
  await initMap()
  await loadStations()
  await fetchDataSourceStatus()
  connectWebSocket()
  
  // Update "last updated" timestamp
  updateInterval = setInterval(() => {
    updateLastUpdateTime()
  }, 1000)
})

onUnmounted(() => {
  if (ws) ws.close()
  if (updateInterval) clearInterval(updateInterval)
})

async function initMap() {
  map = L.map('map').setView([51.9225, 4.47917], 8)
  
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
  }).addTo(map)
}

async function loadStations() {
  try {
    console.log('Loading stations from:', `${API_URL}/stations`)
    const response = await axios.get<Station[]>(`${API_URL}/stations`)
    console.log('Stations loaded:', response.data)
    stations.value = response.data
    
    stations.value.forEach(station => {
      console.log('Adding marker for:', station.name, station.latitude, station.longitude)
      const marker = L.circleMarker([station.latitude, station.longitude], {
        radius: 15,
        fillColor: ALERT_COLORS.GREEN,
        color: '#fff',
        weight: 2,
        opacity: 1,
        fillOpacity: 0.8
      }).addTo(map)
      
      marker.bindPopup(`<b>${station.name}</b><br>${station.stationId}`)
      markers.set(station.stationId, marker)
    })
  } catch (error) {
    console.error('Failed to load stations:', error)
  }
}

function connectWebSocket() {
  console.log('Connecting to WebSocket:', WS_URL)
  ws = new WebSocket(WS_URL)
  
  ws.onopen = () => {
    wsConnected.value = true
    console.log('WebSocket connected')
  }
  
  ws.onmessage = (event) => {
    console.log('WebSocket message received:', event.data)
    const reading: WaterLevelReading = JSON.parse(event.data)
    updateMarker(reading)
    
    if (reading.alertLevel !== 'GREEN') {
      addAlert(reading)
    }
  }
  
  ws.onclose = () => {
    wsConnected.value = false
    console.log('WebSocket disconnected, reconnecting in 3s...')
    setTimeout(connectWebSocket, 3000)
  }
  
  ws.onerror = (error) => {
    console.error('WebSocket error:', error)
  }
}

function updateMarker(reading: WaterLevelReading) {
  lastUpdateTimestamp = Date.now()
  
  const marker = markers.get(reading.stationId)
  if (marker) {
    marker.setStyle({
      fillColor: ALERT_COLORS[reading.alertLevel]
    })
    
    const station = stations.value.find(s => s.stationId === reading.stationId)
    if (station) {
      marker.setPopupContent(
        `<b>${station.name}</b><br>` +
        `Level: ${reading.waterLevel.toFixed(2)}m<br>` +
        `Rainfall: ${reading.rainfall.toFixed(1)}mm/h<br>` +
        `Alert: ${reading.alertLevel}`
      )
    }
  }
}

function addAlert(reading: WaterLevelReading) {
  recentAlerts.value.unshift(reading)
  if (recentAlerts.value.length > 5) {
    recentAlerts.value.pop()
  }
}

async function fetchDataSourceStatus() {
  try {
    const response = await axios.get(`${API_URL}/status`)
    dataSource.value = response.data.dataSource
    console.log('Data source:', dataSource.value)
  } catch (error) {
    console.error('Failed to fetch data source status:', error)
    dataSource.value = 'Unknown'
  }
}

let lastUpdateTimestamp = Date.now()

function updateLastUpdateTime() {
  const seconds = Math.floor((Date.now() - lastUpdateTimestamp) / 1000)
  if (seconds < 60) {
    lastUpdate.value = `${seconds}s ago`
  } else {
    const minutes = Math.floor(seconds / 60)
    lastUpdate.value = `${minutes}m ago`
  }
}

function getStationName(stationId: string): string {
  return stations.value.find(s => s.stationId === stationId)?.name || stationId
}

function clearAlerts() {
  recentAlerts.value = []
}
</script>
