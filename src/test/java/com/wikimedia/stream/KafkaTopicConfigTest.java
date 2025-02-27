// File: src/test/java/com/wikimedia/stream/KafkaTopicConfigTest.java
package com.wikimedia.stream;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class KafkaTopicConfigTest {

    @Autowired
    private KafkaTopicConfig topicConfig;
    
    @Test
    void testTopicConfiguration() {
        NewTopic topic = topicConfig.wikimediaRecentChangeTopic();
        assertNotNull(topic);
        assertEquals(KafkaTopicConfig.TOPIC_NAME, topic.name());
        assertEquals(3, topic.numPartitions());
        assertEquals((short) 1, topic.replicationFactor());
    }
}