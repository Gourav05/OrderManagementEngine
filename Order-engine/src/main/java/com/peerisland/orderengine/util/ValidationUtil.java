package com.peerisland.orderengine.util;

import com.peerisland.orderengine.dto.request.CreateOrderRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ValidationUtil {

    public void validateCreateOrderRequest(CreateOrderRequest request) {
        boolean hasInvalidItem = request.getItems().stream()
                .anyMatch(item -> item.getQuantity() == null
                        || item.getQuantity() <= 0
                        || item.getPrice() == null
                        || item.getPrice().compareTo(BigDecimal.ZERO) <= 0);

        if (hasInvalidItem) {
            throw new IllegalArgumentException("Each order item must have a positive quantity and price");
        }
    }
}
