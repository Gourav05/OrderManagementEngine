package com.peerisland.orderengine;

import com.peerisland.orderengine.domain.Order;
import com.peerisland.orderengine.domain.OrderStatus;
import com.peerisland.orderengine.dto.request.CreateOrderRequest;
import com.peerisland.orderengine.dto.request.CreateOrderRequest.OrderItemRequest;
import com.peerisland.orderengine.dto.response.OrderResponse;
import com.peerisland.orderengine.exception.InvalidOrderStateException;
import com.peerisland.orderengine.mapper.OrderMapper;
import com.peerisland.orderengine.repository.OrderRepository;
import com.peerisland.orderengine.service.OrderServiceImpl;
import com.peerisland.orderengine.state.OrderStateMachine;
import com.peerisland.orderengine.util.ValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderStateMachine orderStateMachine;
    @Mock
    private ValidationUtil validationUtil;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new OrderServiceImpl(orderRepository, orderMapper, orderStateMachine, validationUtil);
    }

    @Test
    void createOrder_savesAndReturnsResponse() {
        CreateOrderRequest req = CreateOrderRequest.builder()
                .customerName("Tester")
                .items(List.of(OrderItemRequest.builder().productName("X").quantity(1).price(new BigDecimal("10.00")).build()))
                .build();

        Order entity = Order.builder().customerName("Tester").items(List.of()).totalAmount(new BigDecimal("10.00")).build();
        Order saved = Order.builder().id("id-1").customerName("Tester").items(List.of()).totalAmount(new BigDecimal("10.00")).createdAt(Instant.now()).build();
        OrderResponse resp = OrderResponse.builder().id("id-1").customerName("Tester").status(OrderStatus.PENDING).totalAmount(new BigDecimal("10.00")).build();

        when(orderMapper.toEntity(req)).thenReturn(entity);
        when(orderRepository.save(entity)).thenReturn(saved);
        when(orderMapper.toResponse(saved)).thenReturn(resp);

        OrderResponse result = service.createOrder(req);

        verify(validationUtil).validateCreateOrderRequest(req);
        verify(orderRepository).save(entity);
        assertEquals("id-1", result.getId());
    }

    @Test
    void updateOrderStatus_invalidTransition_throws() {
        Order order = Order.builder().id("id-2").customerName("C").status(OrderStatus.PENDING).build();
        when(orderRepository.findById("id-2")).thenReturn(Optional.of(order));
        when(orderStateMachine.canTransition(OrderStatus.PENDING, OrderStatus.DELIVERED)).thenReturn(false);

        assertThrows(InvalidOrderStateException.class, () -> service.updateOrderStatus("id-2", OrderStatus.DELIVERED));
    }

    @Test
    void processPendingOrderIds_promotesOrders() {
        Order o1 = Order.builder().id("a").status(OrderStatus.PENDING).build();
        Order o2 = Order.builder().id("b").status(OrderStatus.PENDING).build();
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(List.of(o1, o2));

        List<String> ids = service.processPendingOrderIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains("a") && ids.contains("b"));
        verify(orderRepository).saveAll(anyList());
    }
}
