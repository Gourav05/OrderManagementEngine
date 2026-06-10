package com.peerisland.orderengine;

import com.peerisland.orderengine.domain.OrderStatus;
import com.peerisland.orderengine.state.OrderStateMachine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStateMachineTest {

    private final OrderStateMachine sm = new OrderStateMachine();

    @Test
    void allowsValidTransition() {
        assertTrue(sm.canTransition(OrderStatus.PENDING, OrderStatus.PROCESSING));
    }

    @Test
    void rejectsInvalidTransition() {
        assertFalse(sm.canTransition(OrderStatus.PENDING, OrderStatus.DELIVERED));
    }

    @Test
    void isIdempotentForSameState() {
        assertTrue(sm.canTransition(OrderStatus.PROCESSING, OrderStatus.PROCESSING));
    }
}
