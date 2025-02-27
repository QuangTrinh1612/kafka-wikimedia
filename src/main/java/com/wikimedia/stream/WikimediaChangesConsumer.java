package com.wikimedia.stream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WikimediaChangesConsumer {
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    // For storing statistics
    private final Map<String, Integer> editsByWiki = new HashMap<>();
    private final Map<String, Integer> editsByUser = new HashMap<>();
    private final Map<String, Integer> editsByType = new HashMap<>();
    private int totalEdits = 0;

    public WikimediaChangesConsumer(SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = new ObjectMapper();
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_NAME, groupId = "wikimedia-consumer-group")
    public void consume(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            
            // Process and extract relevant data
            String wiki = rootNode.path("meta").path("domain").asText("unknown");
            String user = rootNode.path("user").asText("anonymous");
            String type = rootNode.path("type").asText("unknown");
            
            // Update statistics
            editsByWiki.put(wiki, editsByWiki.getOrDefault(wiki, 0) + 1);
            editsByUser.put(user, editsByUser.getOrDefault(user, 0) + 1);
            editsByType.put(type, editsByType.getOrDefault(type, 0) + 1);
            totalEdits++;
            
            // Create a simplified message to send to WebSocket clients
            Map<String, Object> dataToSend = new HashMap<>();
            dataToSend.put("wiki", wiki);
            dataToSend.put("user", user);
            dataToSend.put("type", type);
            dataToSend.put("title", rootNode.path("title").asText(""));
            dataToSend.put("timestamp", rootNode.path("meta").path("dt").asText(""));
            
            // Send individual event
            messagingTemplate.convertAndSend("/topic/changes", dataToSend);
            
            // Send statistics every 10 edits
            if (totalEdits % 10 == 0) {
                sendStatistics();
            }
        } catch (IOException e) {
            System.out.println("Error processing Wikimedia event");
        }
    }

    private void sendStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEdits", totalEdits);
        stats.put("editsByWiki", getTopEntries(editsByWiki, 10));
        stats.put("editsByUser", getTopEntries(editsByUser, 10));
        stats.put("editsByType", editsByType);
        
        messagingTemplate.convertAndSend("/topic/statistics", stats);
    }

    private Map<String, Integer> getTopEntries(Map<String, Integer> map, int limit) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
    }
}
