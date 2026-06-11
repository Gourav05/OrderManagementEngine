package com.peerisland.orderengine.service;

import com.peerisland.orderengine.domain.Order;
import com.peerisland.orderengine.domain.OrderStatus;
import com.peerisland.orderengine.dto.request.CreateOrderRequest;
import com.peerisland.orderengine.dto.response.OrderResponse;
import com.peerisland.orderengine.exception.InvalidOrderStateException;
import com.peerisland.orderengine.exception.OrderNotFoundException;
import com.peerisland.orderengine.mapper.OrderMapper;
import com.peerisland.orderengine.repository.OrderRepository;
import com.peerisland.orderengine.state.OrderStateMachine;
import com.peerisland.orderengine.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_BY_ID_CACHE = "ordersById";
    private static final String ALL_ORDERS_CACHE = "allOrders";

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderStateMachine orderStateMachine;
    private final ValidationUtil validationUtil;

    @Override
    @Caching(
            put = {
                    @CachePut(value = ORDER_BY_ID_CACHE, key = "#result.id")
            },
            evict = {
                    @CacheEvict(value = ALL_ORDERS_CACHE, allEntries = true)
            }
    )
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.debug("Validating create order request for customerName={}", request.getCustomerName());
        validationUtil.validateCreateOrderRequest(request);
        Order savedOrder = orderRepository.save(orderMapper.toEntity(request));
        log.info("Order created orderId={} customerName={} itemCount={}",
                savedOrder.getId(), savedOrder.getCustomerName(), savedOrder.getItems().size());
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ORDER_BY_ID_CACHE, key = "#orderId")
    public OrderResponse getOrderById(String orderId) {
        log.debug("Fetching order by id orderId={}", orderId);
        return orderMapper.toResponse(findOrder(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_ORDERS_CACHE, key = "#status != null ? #status.name() : 'ALL'")
    public List<OrderResponse> getAllOrders(OrderStatus status) {
        log.debug("Fetching orders status={}", status);
        List<Order> orders = status == null
                ? orderRepository.findAll()
                : orderRepository.findByStatus(status);
        return orders.stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = ORDER_BY_ID_CACHE, key = "#orderId")
            },
            evict = {
                    @CacheEvict(value = ALL_ORDERS_CACHE, allEntries = true)
            }
    )
    public OrderResponse updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = findOrder(orderId);
        if (!orderStateMachine.canTransition(order.getStatus(), newStatus)) {
            log.warn("Invalid status transition attempted orderId={} currentStatus={} targetStatus={}",
                    orderId, order.getStatus(), newStatus);
            throw new InvalidOrderStateException(
                    "Invalid status transition from " + order.getStatus() + " to " + newStatus);
        }
        log.info("Updating order status orderId={} from={} to={}", orderId, order.getStatus(), newStatus);
        order.setStatus(newStatus);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = ORDER_BY_ID_CACHE, allEntries = true),
            @CacheEvict(value = ALL_ORDERS_CACHE, allEntries = true)
    })
    public java.util.List<String> processPendingOrderIds() {
        java.util.List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        log.debug("Processing pending orders count={}", pendingOrders.size());
        java.util.List<String> processedIds = new java.util.ArrayList<>();

        for (Order order : pendingOrders) {
            boolean success = false;
            int attempts = 0;
            while (!success && attempts < 3) {
                attempts++;
                try {
                    log.debug("Attempting to promote orderId={} attempt={}", order.getId(), attempts);
                    order.setStatus(OrderStatus.PROCESSING);
                    orderRepository.save(order);
                    processedIds.add(order.getId());
                    success = true;
                } catch (org.springframework.dao.OptimisticLockingFailureException ex) {
                    log.warn("Optimistic locking failure for orderId={} attempt={}", order.getId(), attempts);
                    // reload fresh entity and retry
                    java.util.Optional<Order> fresh = orderRepository.findById(order.getId());
                    if (fresh.isPresent()) {
                        order = fresh.get();
                    } else {
                        // order disappeared, stop retrying
                        break;
                    }
                }
            }
            if (!success) {
                log.error("Failed to promote orderId={} after {} attempts", order.getId(), 3);
            }
        }
        return processedIds;
    }

    @SuppressWarnings("unused")
    @Override
    public int processPendingOrders() {
        return processPendingOrderIds().size();
    }

    private Order findOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found orderId={}", orderId);
                    return new OrderNotFoundException(orderId);
                });
    }
}
