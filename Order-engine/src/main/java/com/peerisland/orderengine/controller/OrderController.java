package com.peerisland.orderengine.controller;

import com.peerisland.orderengine.domain.OrderStatus;
import com.peerisland.orderengine.dto.request.CreateOrderRequest;
import com.peerisland.orderengine.dto.request.UpdateOrderStatusRequest;
import com.peerisland.orderengine.dto.response.OrderResponse;
import com.peerisland.orderengine.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Create order request received for customerName={}", request.getCustomerName());
        return orderService.createOrder(request);
    }

    @GetMapping
    public List<OrderResponse> getOrders(@RequestParam(required = false) OrderStatus status) {
        log.debug("Fetch orders request received status={}", status);
        return orderService.getAllOrders(status);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable String orderId) {
        log.debug("Fetch order request received for orderId={}", orderId);
        return orderService.getOrderById(orderId);
    }

    @PutMapping("/{orderId}/status")
    public OrderResponse updateStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderStatus status = request.getStatus();
        log.info("Update order status request received for orderId={} targetStatus={}", orderId, status);
        return orderService.updateOrderStatus(orderId, status);
    }


    @PatchMapping("/{orderId}/status")
    public OrderResponse patchStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderStatus status = request.getStatus();
        log.info("Patch order status request received for orderId={} targetStatus={}", orderId, status);
        return orderService.updateOrderStatus(orderId, status);
    }
}
