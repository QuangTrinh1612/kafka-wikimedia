package com.wikimedia.stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class WikimediaStreamingApplicationTest {

    @Autowired
    private ApplicationContext context;
    
    @Test
    void contextLoads() {
        // Verify that the application context loads successfully
        assertNotNull(context);
    }
    
    @Test
    void kafkaTemplateIsAvailable() {
        // Verify that the KafkaTemplate bean is available
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = context.getBean(KafkaTemplate.class);
        
        assertNotNull(kafkaTemplate);
    }
    
    @Test
    void producerIsAvailable() {
        // Verify that the WikimediaChangesProducer bean is available
        WikimediaChangesProducer producer = context.getBean(WikimediaChangesProducer.class);
        assertNotNull(producer);
    }
    
    @Test
    void consumerIsAvailable() {
        // Verify that the WikimediaChangesConsumer bean is available
        WikimediaChangesConsumer consumer = context.getBean(WikimediaChangesConsumer.class);
        assertNotNull(consumer);
    }
}