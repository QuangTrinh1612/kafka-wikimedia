package com.wikimedia.stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class WebSocketConfigTest {

    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private WebSocketConfig webSocketConfig;
    
    @Test
    void testWebSocketConfigExists() {
        assertNotNull(webSocketConfig);
    }
    
    @Test
    void testEndpointExists() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc.perform(get("/wikimedia-websocket"))
                .andExpect(status().isOk());
    }

    // Note: For more comprehensive WebSocket testing, you'd use a WebSocketClient
    // to connect to the endpoint and verify message exchange, but that's beyond
    // the scope of a simple unit test.
}