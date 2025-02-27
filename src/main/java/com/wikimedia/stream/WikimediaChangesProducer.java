package com.wikimedia.stream;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.launchdarkly.eventsource.EventSource;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WikimediaChangesProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public WikimediaChangesProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void streamWikimediaChanges() {
        log.info("Starting Wikimedia Changes Producer ...");

        WikimediaChangesHandler eventHandler = new WikimediaChangesHandler(kafkaTemplate, KafkaTopicConfig.TOPIC_NAME);
        
        String url = "https://stream.wikimedia.org/v2/stream/recentchange";
        EventSource.Builder builder = new EventSource.Builder(eventHandler, URI.create(url));
        EventSource eventSource = builder.build();

        // Start the event source in a separate thread
        eventSource.start();

        // Keep the producer running for a set amount of time or use a different mechanism to control lifecycle
        try {
            TimeUnit.MINUTES.sleep(10);
        } catch (InterruptedException e) {
            log.error("Wikimedia stream interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            eventSource.close();
        }
    }
}