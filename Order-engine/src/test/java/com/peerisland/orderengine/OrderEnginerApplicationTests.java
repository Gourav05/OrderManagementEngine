package com.peerisland.orderengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peerisland.orderengine.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderEnginerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createOrderAndFetchIt() throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "customerName", "Alice",
                "items", List.of(
                        Map.of("productName", "Keyboard", "quantity", 2, "price", 49.99),
                        Map.of("productName", "Mouse", "quantity", 1, "price", 25.50)
                )
        ));

        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Alice"))
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING.name()))
                .andExpect(jsonPath("$.totalAmount").value(125.48))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String orderId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void rejectsInvalidStatusTransition() throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "customerName", "Bob",
                "items", List.of(Map.of("productName", "Monitor", "quantity", 1, "price", 299.99))
        ));

        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String orderId = objectMapper.readTree(response).get("id").asText();

        String updateBody = objectMapper.writeValueAsString(Map.of("status", OrderStatus.DELIVERED.name()));

        mockMvc.perform(put("/api/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status transition from PENDING to DELIVERED"));
    }

    @Test
    void listsOrdersWithOptionalStatusFilter() throws Exception {
        String pendingOrder = objectMapper.writeValueAsString(Map.of(
                "customerName", "Carol",
                "items", List.of(Map.of("productName", "Desk", "quantity", 1, "price", 199.99))
        ));
        String processingOrder = objectMapper.writeValueAsString(Map.of(
                "customerName", "Dave",
                "items", List.of(Map.of("productName", "Chair", "quantity", 1, "price", 89.99))
        ));

        String processingResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(processingOrder))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pendingOrder))
                .andExpect(status().isCreated());

        String processingOrderId = objectMapper.readTree(processingResponse).get("id").asText();
        String updateBody = objectMapper.writeValueAsString(Map.of("status", OrderStatus.PROCESSING.name()));

        mockMvc.perform(put("/api/orders/{orderId}/status", processingOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(OrderStatus.PROCESSING.name()));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[?(@.customerName == 'Carol')]").exists())
                .andExpect(jsonPath("$[?(@.customerName == 'Dave')]").exists());

        mockMvc.perform(get("/api/orders").param("status", OrderStatus.PROCESSING.name()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerName").value("Dave"))
                .andExpect(jsonPath("$[0].status").value(OrderStatus.PROCESSING.name()));
    }
}
