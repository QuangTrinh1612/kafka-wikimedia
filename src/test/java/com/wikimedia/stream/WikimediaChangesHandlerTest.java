package com.wikimedia.stream;

import com.launchdarkly.eventsource.MessageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WikimediaChangesHandlerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplateMock;
    
    private WikimediaChangesHandler handler;
    private final String topicName = "test-topic";
    
    @BeforeEach
    void setUp() {
        handler = new WikimediaChangesHandler(kafkaTemplateMock, topicName);
    }
    
    @Test
    void testOnMessage() {
        // Create a mock MessageEvent
        MessageEvent messageEvent = new MessageEvent("test data");
        
        // Call the onMessage method
        handler.onMessage("event", messageEvent);
        
        // Verify that the kafkaTemplate.send method was called with the correct parameters
        verify(kafkaTemplateMock, times(1)).send(eq(topicName), eq("test data"));
    }
    
    @Test
    void testOnError() {
        // Create a test exception
        Throwable testError = new RuntimeException("Test error");
        
        // Call the onError method
        handler.onError(testError);
        
        // Since the method only logs the error, there's not much to verify
        // This test simply ensures the method doesn't throw an exception
    }
}