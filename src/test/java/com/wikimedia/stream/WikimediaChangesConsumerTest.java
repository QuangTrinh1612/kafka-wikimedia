package com.wikimedia.stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WikimediaChangesConsumerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplateMock;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> messageCaptor;
    
    private WikimediaChangesConsumer consumer;
    
    @BeforeEach
    void setUp() {
        consumer = new WikimediaChangesConsumer(messagingTemplateMock);
    }
    
    @Test
    void testConsume() throws Exception {
        // Create a sample JSON message
        String jsonMessage = "{\n" +
                "  \"meta\": {\n" +
                "    \"domain\": \"en.wikipedia.org\",\n" +
                "    \"dt\": \"2023-01-01T12:00:00Z\"\n" +
                "  },\n" +
                "  \"user\": \"TestUser\",\n" +
                "  \"type\": \"edit\",\n" +
                "  \"title\": \"Test Page\"\n" +
                "}";
        
        // Call the consume method
        consumer.consume(jsonMessage);
        
        // Verify that convertAndSend was called with the correct destination
        verify(messagingTemplateMock).convertAndSend(eq("/topic/changes"), messageCaptor.capture());
        
        // Verify the captured message contains the expected data
        Map<String, Object> capturedMessage = messageCaptor.getValue();
        assertEquals("en.wikipedia.org", capturedMessage.get("wiki"));
        assertEquals("TestUser", capturedMessage.get("user"));
        assertEquals("edit", capturedMessage.get("type"));
        assertEquals("Test Page", capturedMessage.get("title"));
        assertEquals("2023-01-01T12:00:00Z", capturedMessage.get("timestamp"));
    }
    
    @Test
    void testConsumeWithInvalidJson() {
        // Create an invalid JSON message
        String invalidJson = "this is not valid json";
        
        // Call the consume method, which should handle the exception internally
        consumer.consume(invalidJson);
        
        // Verify that no message was sent (because the processing failed)
        verify(messagingTemplateMock, never()).convertAndSend(eq("/topic/changes"), anyMap());
    }
    
    @Test
    void testSendStatistics() throws Exception {
        // We'll call consume multiple times to trigger the sendStatistics method
        for (int i = 0; i < 10; i++) {
            String jsonMessage = "{\n" +
                    "  \"meta\": {\n" +
                    "    \"domain\": \"en.wikipedia.org\",\n" +
                    "    \"dt\": \"2023-01-01T12:00:00Z\"\n" +
                    "  },\n" +
                    "  \"user\": \"TestUser" + i + "\",\n" +
                    "  \"type\": \"edit\",\n" +
                    "  \"title\": \"Test Page\"\n" +
                    "}";
            consumer.consume(jsonMessage);
        }
        
        // Verify that convertAndSend was called for statistics
        verify(messagingTemplateMock, atLeastOnce()).convertAndSend(eq("/topic/statistics"), messageCaptor.capture());
        
        // Verify the statistics contain the expected data
        Map<String, Object> capturedStats = messageCaptor.getValue();
        assertEquals(10, capturedStats.get("totalEdits"));
        
        // Verify the maps contain the expected entries
        assertTrue(capturedStats.containsKey("editsByWiki"));
        assertTrue(capturedStats.containsKey("editsByUser"));
        assertTrue(capturedStats.containsKey("editsByType"));
    }
}