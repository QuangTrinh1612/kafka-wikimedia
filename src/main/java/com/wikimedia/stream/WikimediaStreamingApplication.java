package com.wikimedia.stream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WikimediaStreamingApplication {
    public static void main(String[] args) {
        SpringApplication.run(WikimediaStreamingApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(WikimediaChangesProducer producer) {
        return args -> {
            // Start the producer in a separate thread
            new Thread(() -> producer.streamWikimediaChanges()).start();
        };
    }
}