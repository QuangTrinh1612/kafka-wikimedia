package com.wikimedia.stream;

import org.springframework.kafka.core.KafkaTemplate;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.MessageEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WikimediaChangesHandler implements EventHandler {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public WikimediaChangesHandler(KafkaTemplate<String, String> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void onOpen() {
        log.info("Connection to Wikimedia stream opened");
    }

    @Override
    public void onClosed() {
        log.info("Connection to Wikimedia stream closed");
    }

    @Override
    public void onMessage(String event, MessageEvent messageEvent) {
        log.info("Received event: {}", event);
        // Send the event data to Kafka
        kafkaTemplate.send(topic, messageEvent.getData());
    }

    @Override
    public void onComment(String comment) {
        log.info("Received comment: {}", comment);
    }

    @Override
    public void onError(Throwable t) {
        log.error("Error in Wikimedia stream", t);
    }
}