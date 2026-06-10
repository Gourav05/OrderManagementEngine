package com.peerisland.orderengine;

import com.peerisland.orderengine.domain.Order;
import com.peerisland.orderengine.dto.request.CreateOrderRequest;
import com.peerisland.orderengine.dto.request.CreateOrderRequest.OrderItemRequest;
import com.peerisland.orderengine.mapper.OrderMapper;
import com.peerisland.orderengine.dto.response.OrderResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    void mapsCreateRequestToEntityAndComputesTotal() {
        CreateOrderRequest req = CreateOrderRequest.builder()
                .customerName("Alice")
                .items(List.of(
                        OrderItemRequest.builder().productName("A").quantity(2).price(new BigDecimal("10.00")).build(),
                        OrderItemRequest.builder().productName("B").quantity(1).price(new BigDecimal("5.50")).build()
                ))
                .build();

        Order entity = mapper.toEntity(req);
        assertNotNull(entity);
        assertEquals(2, entity.getItems().size());
        assertEquals(new BigDecimal("25.50"), entity.getTotalAmount());
    }

    @Test
    void mapsEntityToResponse() {
        Order order = Order.builder()
                .customerName("Bob")
                .items(List.of())
                .totalAmount(new BigDecimal("0.00"))
                .build();

        OrderResponse resp = mapper.toResponse(order);
        assertNotNull(resp);
        assertEquals("Bob", resp.getCustomerName());
    }
}
