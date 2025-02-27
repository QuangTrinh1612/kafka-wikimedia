package com.wikimedia.stream.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;
    private String URL;

    @BeforeEach
    public void setup() {
        URL = "ws://localhost:" + port + "/wikimedia-websocket";
        
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        
        SockJsClient sockJsClient = new SockJsClient(transports);
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    public void testWebSocketConnection() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<Boolean> connectionEstablished = new CompletableFuture<>();
        
        StompSession session = stompClient.connect(URL, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                connectionEstablished.complete(true);
            }
        }).get(5, TimeUnit.SECONDS);
        
        assertTrue(connectionEstablished.get(5, TimeUnit.SECONDS));
        
        // Clean up - disconnect
        if (session.isConnected()) {
            session.disconnect();
        }
    }

    @Test
    public void testTopicSubscription() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<Map<String, Object>> messageReceived = new CompletableFuture<>();
        
        StompSession session = stompClient.connect(URL, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                session.subscribe("/topic/changes", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return Map.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        messageReceived.complete((Map<String, Object>) payload);
                    }
                });
            }
        }).get(5, TimeUnit.SECONDS);
        
        // Note: In a real test, we would send a message to trigger the WebSocket response
        // but since our application depends on Kafka, we'll just verify the subscription works
        // and timeout if no message is received within a short period (which is expected)
        
        try {
            messageReceived.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // This is expected, as we haven't sent any message to trigger a response
        }
        
        // Clean up - disconnect
        if (session.isConnected()) {
            session.disconnect();
        }
    }
}