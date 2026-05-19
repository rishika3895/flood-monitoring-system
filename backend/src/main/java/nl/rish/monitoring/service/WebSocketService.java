package nl.rish.monitoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.rish.monitoring.model.WaterLevelReading;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketService {
    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public WebSocketService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void addSession(WebSocketSession session) {
        sessions.add(session);
        System.out.println("WebSocket session added: " + session.getId());
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
        System.out.println("WebSocket session removed: " + session.getId());
    }

    public void broadcastReading(WaterLevelReading reading) {
        try {
            String message = objectMapper.writeValueAsString(reading);
            TextMessage textMessage = new TextMessage(message);

            sessions.forEach(session -> {
                try {
                    if (session.isOpen()) {
                        synchronized (session) {
                            session.sendMessage(textMessage);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error sending to session " + session.getId() + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error broadcasting reading: " + e.getMessage());
        }
    }
}
