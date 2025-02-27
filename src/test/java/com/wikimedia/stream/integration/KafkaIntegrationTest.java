package com.wikimedia.stream.integration;

import com.wikimedia.stream.KafkaTopicConfig;
import com.wikimedia.stream.WikimediaChangesConsumer;
import com.wikimedia.stream.WikimediaChangesHandler;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {KafkaTopicConfig.TOPIC_NAME})
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class KafkaIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private KafkaMessageListenerContainer<String, String> container;
    private BlockingQueue<ConsumerRecord<String, String>> consumerRecords;
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @BeforeAll
    void setUp() {
        // Consumer configuration
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new StringDeserializer());
        
        // Create container for consumer
        ContainerProperties containerProperties = new ContainerProperties(KafkaTopicConfig.TOPIC_NAME);
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        consumerRecords = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, String>) consumerRecords::add);
        container.start();
        
        // Wait for assignments
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
        
        // Producer configuration
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        DefaultKafkaProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(producerProps);
        
        kafkaTemplate = new KafkaTemplate<>(producerFactory);
    }
    
    @AfterAll
    void tearDown() {
        container.stop();
    }
    
    @Test
    void testKafkaIntegration() throws Exception {
        // Create a sample JSON message
        String sampleMessage = "{\n" +
                "  \"meta\": {\n" +
                "    \"domain\": \"en.wikipedia.org\",\n" +
                "    \"dt\": \"2023-01-01T12:00:00Z\"\n" +
                "  },\n" +
                "  \"user\": \"TestUser\",\n" +
                "  \"type\": \"edit\",\n" +
                "  \"title\": \"Test Page\"\n" +
                "}";
        
        // Send the message to Kafka using the WikimediaChangesHandler
        WikimediaChangesHandler handler = new WikimediaChangesHandler(kafkaTemplate, KafkaTopicConfig.TOPIC_NAME);
        com.launchdarkly.eventsource.MessageEvent messageEvent = new com.launchdarkly.eventsource.MessageEvent(sampleMessage);
        handler.onMessage("change", messageEvent);
        
        // Consume the message from Kafka
        ConsumerRecord<String, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);
        
        // Verify the message
        assertNotNull(received);
        assertEquals(sampleMessage, received.value());
        
        // Create a mock SimpMessagingTemplate
        SimpMessagingTemplate messagingTemplateMock = mock(SimpMessagingTemplate.class);
        
        // Create a consumer with the mock template
        WikimediaChangesConsumer consumer = new WikimediaChangesConsumer(messagingTemplateMock);
        
        // Process the message with the consumer (without exceptions)
        consumer.consume(received.value());
    }
}