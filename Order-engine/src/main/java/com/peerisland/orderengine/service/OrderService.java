package com.peerisland.orderengine.service;

import com.peerisland.orderengine.domain.OrderStatus;
import com.peerisland.orderengine.dto.request.CreateOrderRequest;
import com.peerisland.orderengine.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(String orderId);

    List<OrderResponse> getAllOrders(OrderStatus status);

    OrderResponse updateOrderStatus(String orderId, OrderStatus newStatus);

    int processPendingOrders();
}
