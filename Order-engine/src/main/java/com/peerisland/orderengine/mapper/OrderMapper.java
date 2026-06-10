package com.peerisland.orderengine.mapper;

import com.peerisland.orderengine.domain.Order;
import com.peerisland.orderengine.domain.OrderItem;
import com.peerisland.orderengine.dto.request.CreateOrderRequest;
import com.peerisland.orderengine.dto.response.OrderItemResponse;
import com.peerisland.orderengine.dto.response.OrderResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderMapper {

    public Order toEntity(CreateOrderRequest request) {
        List<OrderItem> items = request.getItems().stream()
                .map(this::toEntity)
                .toList();

        return Order.builder()
                .customerName(request.getCustomerName())
                .items(items)
                .totalAmount(calculateTotal(items))
                .build();
    }

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream().map(this::toResponse).toList())
                .build();
    }

    private OrderItem toEntity(CreateOrderRequest.OrderItemRequest request) {
        return OrderItem.builder()
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .build();
    }

    private OrderItemResponse toResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
