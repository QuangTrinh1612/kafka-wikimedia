package com.wikimedia.stream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class WikimediaStreamingApplication {
    public static void main(String[] args) {
        SpringApplication.run(WikimediaStreamingApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(WikimediaChangesProducer producer) {
        return args -> {
            System.out.println("Starting Wikimedia Stream Processing Application...");
            System.out.println("Web interface available at http://localhost:8081");
            
            // Start the producer in a separate thread
            new Thread(() -> {
                try {
                    System.out.println("Starting Wikimedia changes producer...");
                    producer.streamWikimediaChanges();
                } catch (Exception e) {
                    System.err.println("Error in Wikimedia producer: " + e.getMessage());
                    e.printStackTrace();
                }
            }, "wikimedia-producer-thread").start();
        };
    }
}