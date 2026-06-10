package com.peerisland.orderengine.repository;

import com.peerisland.orderengine.domain.Order;
import com.peerisland.orderengine.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByStatus(OrderStatus status);
}
